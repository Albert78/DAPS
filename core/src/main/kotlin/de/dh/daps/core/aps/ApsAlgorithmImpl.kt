package de.dh.daps.core.aps

import android.util.Log
import de.dh.daps.common.METABOLIC_EVENTS_HISTORY_HOURS
import de.dh.daps.common.domain.calculation.CarbsInsulinCalculator
import de.dh.daps.common.domain.model.data.BgDelta
import de.dh.daps.common.domain.model.data.BgReading
import de.dh.daps.common.domain.model.data.BgValue
import de.dh.daps.common.domain.model.data.Minutes
import de.dh.daps.common.domain.model.data.Tick
import de.dh.daps.common.domain.model.data.Timestamp
import de.dh.daps.common.domain.model.engine.Timeline
import de.dh.daps.common.domain.model.therapy.DeferredBolus
import de.dh.daps.common.domain.model.therapy.FAST_KE_DEFAULT_PEAK
import de.dh.daps.common.domain.model.therapy.InsulinAmount
import de.dh.daps.common.domain.model.therapy.InsulinDose
import de.dh.daps.common.domain.model.therapy.InsulinType
import de.dh.daps.common.domain.model.therapy.MealType
import de.dh.daps.common.domain.model.therapy.convertToBgDeltaFromUnits
import de.dh.daps.common.domain.model.therapy.convertToCarbsFromBgDelta
import de.dh.daps.common.domain.model.therapy.convertToInsulinAmountFromBgDelta
import de.dh.daps.common.domain.model.therapy.convertToInsulinAmountFromCarbs
import de.dh.daps.common.domain.model.therapy.toActiveDoses
import de.dh.daps.core.aps.prediction.PredictionModel
import de.dh.daps.core.aps.prediction.RecentBgReadingsHistory
import de.dh.daps.core.aps.prediction.SampledBgReadings
import de.dh.daps.core.repository.GlucoseRepository
import de.dh.daps.core.repository.TreatmentRepository
import de.dh.daps.core.therapy.TherapyManager
import kotlin.math.ceil

class ApsAlgorithmImpl(
    private val timeline: Timeline,
    private val treatmentRepository: TreatmentRepository,
    private val sampledBgReadings: SampledBgReadings,
    private val history: RecentBgReadingsHistory,
    private val predictionModel: PredictionModel,
    private val carbsInsulinCalculator: CarbsInsulinCalculator,
    private val therapyManager: TherapyManager
): ApsAlgorithm {
    // --- Time-based extensions for Tick to provide a Timestamp-like API ---
    private fun Tick.plusMinutes(minutes: Int): Tick = this + (minutes / timeline.tickDuration.value.toInt())
    private fun Tick.minusMinutes(minutes: Int): Tick = this - (minutes / timeline.tickDuration.value.toInt())
    private fun Tick.minus(minutes: Minutes): Tick = minusMinutes(minutes.value.toInt())

    override suspend fun onNewBgReading(reading: BgReading) {
        history.add(reading)
    }

    /**
     * Called when therapy settings changed.
     */
    override suspend fun updateTherapySettings() {
        predictionModel.invalidateTherapySettingsCache()
        recalculatePredictionModel()
    }

    /**
     * Called when meals occurred. This invalidates the cached effective carbs, which will be
     * re-evaluated on the next calculation.
     */
    override suspend fun updateMeals() {
        predictionModel.invalidateCarbsCache()
        recalculatePredictionModel()
    }

    /**
     * Called when insulin events occurred. This invalidates the cached effective insulin, which will be
     * re-evaluated on the next calculation.
     */
    override suspend fun updateInsulin() {
        predictionModel.invalidateInsulinCache()
        recalculatePredictionModel()
    }

    private suspend fun recalculatePredictionModel() {
        var currentBg = sampledBgReadings.calculateSavitzkyGolayEndBorder3()
        if (currentBg.isInvalid()) {
            currentBg = sampledBgReadings.calculatePTWMA(decayFactor = 0.7, maxNumReadings = 5)
        }
        val avgCurrentDeviationPerTick = calcAvgDeviationPerTick(DEVIATION_TIME_BASE)

        val meals = treatmentRepository.getMeals()
        val insulinApplications = treatmentRepository.getInsulinApplications()
        val settings = therapyManager.getCurrentTherapySettings()
        val pumpInsulinType = therapyManager.getPumpInsulinType()
        val setting = settings.insulinProfile.getSettingFor(pumpInsulinType)
        val dia = setting.dia
        val insulinPeak = setting.peak
        val activeDoses = insulinApplications.toActiveDoses()

        predictionModel.calculate(
            currentBg = currentBg,
            avgCurrentDeviationPerTick = avgCurrentDeviationPerTick,
            meals = meals,
            insulinDoses = activeDoses,
            dia = dia,
            insulinPeak = insulinPeak,
            therapyManager = therapyManager,
            carbsInsulinCalculator = carbsInsulinCalculator
        )
    }

    override suspend fun getAssumedBg(timestamp: Timestamp): BgValue {
        val tick = timeline.tick(timestamp)
        return predictionModel.withTickState(tick) { it.assumedBg } ?: BgValue.INVALID
    }

    override fun getBolusCorrectionCalculator(): BolusCorrectionCalculator {
        return BolusCorrectionCalculatorImpl()
    }

    /**
     * Smart bolus calculator that has access to the internal state of the APS algorithm.
     */
    private inner class BolusCorrectionCalculatorImpl : BolusCorrectionCalculator {
        override suspend fun calculateBolusProjections(mealTimestamp: Timestamp): BolusProjections {
            val assumedBg = getAssumedBg(mealTimestamp)
            if (assumedBg.isInvalid()) {
                return BolusProjections()
            }
            val historyLimit = mealTimestamp.minusHours(METABOLIC_EVENTS_HISTORY_HOURS)
            val insulinHistory = treatmentRepository.getInsulinApplications(from = historyLimit)
            val mealsHistory = treatmentRepository.getMeals(from = historyLimit)

            val settings = therapyManager.getCurrentTherapySettings()
            val pumpInsulinType = therapyManager.getPumpInsulinType()
            val setting = settings.insulinProfile.getSettingFor(pumpInsulinType)
            val dia = setting.dia
            val peak = setting.peak
            val lowThreshold = therapyManager.getBgSettings().second

            val mealTimeTick = timeline.tick(mealTimestamp)
            val impendingLow = predictionModel.findNext(
                startAt = mealTimeTick + 1,
                until = mealTimeTick.plusMinutes(BOLUS_CALCULATOR_LOW_PREDICTION_LOOKAHEAD.value.toInt()),
                predicate = { it.assumedBg.isValid() && it.assumedBg < lowThreshold },
                block = { ProjectedBg(bg = it.assumedBg, timestamp = timeline.timestamp(it.tick)) }
            )

            val projectedIob = carbsInsulinCalculator.iob(
                insulinDoses = insulinHistory.toActiveDoses(excludeBasal = true),
                timestamp = mealTimestamp,
                dia = dia,
                peak = peak
            )
            val projectedCob = carbsInsulinCalculator.cob(
                meals = mealsHistory,
                timestamp = mealTimestamp,
                includeFutureMeals = false
            )
            val sumFutureCarbs = mealsHistory.sumOf { if (it.timestamp > mealTimestamp) it.carbGrams else 0.0 }
            val deferredBoluses = treatmentRepository.getDeferredBoluses()
            val sumFutureDeferredBolus = deferredBoluses
                .fold(InsulinAmount.ZERO) { acc, next -> acc + next.amount }

            return BolusProjections(
                timestamp = mealTimestamp,
                bg = assumedBg,
                isProjected = mealTimestamp > Timestamp.now(),
                impendingLow = impendingLow,
                iob = projectedIob,
                cob = projectedCob,
                futureCarbs = sumFutureCarbs,
                deferredBolusAmount = sumFutureDeferredBolus
            )
        }

        override suspend fun calculateBolusParts(
            carbsGrams: Double,
            mealTimestamp: Timestamp,
            projectedBg: BgValue,
            impendingLow: ProjectedBg?,
            projectedIob: InsulinAmount,
            projectedCob: Double,
            futureCarbs: Double,
            deferredBolusAmount: InsulinAmount
        ) = BolusCalculationMath.calculateBolusParts(
            carbsGrams = carbsGrams,
            mealTimestamp = mealTimestamp,
            bg = projectedBg,
            cr = therapyManager.getCrFactor(mealTimestamp),
            isf = therapyManager.getIsfFactor(mealTimestamp),
            targetBg = therapyManager.getBgSettings(mealTimestamp).first,
            impendingLow = impendingLow,
            iob = projectedIob,
            cob = projectedCob,
            futureCarbs = futureCarbs,
            deferredBolusAmount = deferredBolusAmount
        )

        override suspend fun distributeInsulinPlan(
            manualBolus: InsulinAmount,
            insulinType: InsulinType,
            netCorrection: InsulinAmount,
            mealType: MealType?,
            imi: Minutes
        ) = BolusCalculationMath.distributeInsulinPlan(
            manualBolus,
            insulinType,
            netCorrection,
            mealType,
            imi
        )
    }


    /**
     * Calculates the systematic deviation per tick between actual blood glucose readings
     * and the prediction model over the last [pastTime] minutes.
     *
     * Deviations typically occur due to unannounced meals or variations in insulin/carb sensitivity
     * compared to the prediction model.
     *
     * A directional trend consistency check is performed to verify that the deviation has a clear
     * orientation (e.g. accelerating/rising glucose after unannounced meals or dropping glucose).
     * If directional consensus is met, an Exponential Moving Average (EMA) is computed to weight
     * recent ticks more heavily. Unordered fluctuations/noise are filtered out and result in [BgDelta.ZERO].
     */
    private suspend fun calcAvgDeviationPerTick(pastTime: Minutes): BgDelta {
        val endTick = timeline.getNowTick()
        val startTick = endTick.minus(pastTime)
        val numTicks = endTick.value - startTick.value

        if (numTicks <= 0) return BgDelta.ZERO

        val deviations = ArrayList<Double>(numTicks)

        // 1. Calculate per-tick deviation: d_t = (BG_t - BG_{t-1}) - BGI_t
        predictionModel.forEach(from = startTick + 1, to = endTick) { tick, state ->
            val bgPrev = sampledBgReadings.getAt(tick - 1)
            val bgCurr = sampledBgReadings.getAt(tick)

            if (bgPrev.isValid() && bgCurr.isValid()) {
                val actualDelta = (bgCurr - bgPrev).mgdl
                val predictedBgi = state.bgi.mgdl
                val tickDeviation = actualDelta - predictedBgi
                deviations.add(tickDeviation)
            }
        }

        val validCount = deviations.size
        val minRequiredTicks = (numTicks * 0.8).toInt().coerceAtLeast(1)
        if (validCount < minRequiredTicks) return BgDelta.ZERO

        // 2. Check trend consistency (sign ratio check instead of rigid standard deviation)
        val significantThreshold = 0.5 // Deviations below 0.5 mg/dL/tick are considered negligible
        val positiveCount = deviations.count { it > significantThreshold }
        val negativeCount = deviations.count { it < -significantThreshold }

        val minTrendRatio = 0.75 // At least 75% of ticks must point in the same direction
        val isConsistentRise = (positiveCount.toDouble() / validCount) >= minTrendRatio
        val isConsistentDrop = (negativeCount.toDouble() / validCount) >= minTrendRatio

        if (!isConsistentRise && !isConsistentDrop) {
            // No clear direction -> unordered noise
            return BgDelta.ZERO
        }

        // 3. Exponential Moving Average (EMA): Weight more recent ticks heavier
        var weightedSum = 0.0
        var totalWeight = 0.0
        var weight = 1.0
        val decayFactor = 0.8 // Older ticks lose weight exponentially per step

        // Iterate from most recent (end of list) to oldest ticks
        for (i in deviations.indices.reversed()) {
            weightedSum += deviations[i] * weight
            totalWeight += weight
            weight *= decayFactor
        }

        val avgDeviation = weightedSum / totalWeight
        return BgDelta.fromMgDl(avgDeviation)
    }

    /**
     * Calculates a list of artificial basal insulin doses which reflect a 100% basal coverage
     * of the last hours.
     */
    private suspend fun calculateArtificialNormalBasalDoses(): List<InsulinDose> {
        val insulinType = therapyManager.getPumpInsulinType()
        val pumpSetting = therapyManager.getCurrentTherapySettings().insulinProfile.getSettingFor(insulinType)
        val diaHours = (pumpSetting.dia.value + 59) / 60
        val now = Timestamp.now()
        // For the more distant past, it's sufficient to approximate the basal with one delivery per hour
        val hourlyDoses = (1..diaHours).map { hour ->
            val timestamp = now.minusHours(hour)
            val amount = therapyManager.getBasalPerHour(timestamp)
            InsulinDose(
                timestamp = timestamp,
                amount = amount,
                insulinType = insulinType
            )
        }
        // For the new past, we chose a more precise approach
        val quarterBasalAmount = therapyManager.getBasalPerHour(now) / 4.0
        val quarterDosesLastHour = (1..4).map { quarter ->
            InsulinDose(
                timestamp = now.minusMinutes(quarter * 15),
                amount = quarterBasalAmount,
                insulinType = insulinType
            )
        }
        return hourlyDoses + quarterDosesLastHour
    }

    override suspend fun recalculate(): CalculationResult {
        val now = Timestamp.now()
        history.prune(now.minus(BG_HISTORY_TIME))
        sampledBgReadings.sampleAvgValues()
        val nowTick = timeline.getNowTick()

        var insight = CoreInsight(
            timestamp = now,
            bgOriginal = sampledBgReadings.getAt(nowTick),
            bgFiltered = BgValue.INVALID,
            deviationPerTick = BgDelta.ZERO,
            futureActiveInsulin = InsulinAmount.ZERO,
            futureActiveCarbs = 0.0,
            predictedBgAtCorrection = BgValue.INVALID,
            targetBg = BgValue.INVALID,
            isf = BgDelta.ZERO,
            cr = 0.0,
            reasoning = CoreReasoning.INTERNAL_ERROR
        )

        try {
            Log.d(TAG, "Algorithm is calculating...")

            // Move the prediction window forward. It always covers roughly our BG readings history cache
            // to be able to calculate deviations between the static predictions and the actual readings.
            predictionModel.advanceToTick(nowTick.minus(PRESERVE_PREDICTIONS_PAST_TIME))

            // ------------------------------- BG Filtering & Validation -------------------------------

            val validFilteredBg = run {
                // Filter BG values to avoid big jumps caused by measurement errors.
                // If we have enough input values, we can use the better SavitzkyGolay filter, else fallback to PTWMA
                var filtered = sampledBgReadings.calculateSavitzkyGolayEndBorder3()
                if (filtered.isInvalid()) {
                    filtered = sampledBgReadings.calculatePTWMA(decayFactor = 0.7, maxNumReadings = 5)
                }

                // TODO: Add detection for noisy values.
                // If too noisy, return CalculationResult.coreIssues(NoisyValues)

                // ----------------------------- Switch off algorithm handling -----------------------------
                if (filtered.isInvalid()) {
                    var receivedValidValue = false
                    for (tick in nowTick.minusMinutes(SWITCH_OFF_ALGORITHM_INVALID_VALUES_THRESHOLD_IN_MINUTES)..nowTick) {
                        if (sampledBgReadings.getAt(tick).isValid()) {
                            receivedValidValue = true
                            break
                        }
                    }
                    if (!receivedValidValue) {
                        return@recalculate CalculationResult.coreIssue(
                            CoreIssue.NoRecentValues(SWITCH_OFF_ALGORITHM_INVALID_VALUES_THRESHOLD_IN_MINUTES),
                            CoreReasoning.INVALID_VALUES
                        ).withMetrics(insight)
                    }
                    // We cannot make any new predictions if we don't have fresh values.
                    // Fallback to safe basal
                    return@recalculate CalculationResult.safetyBasal(CoreReasoning.INVALID_VALUES).withMetrics(insight)
                }
                filtered
            }

            // Fill the "past" part of our tick states with real BG values from our input.
            // This is necessary at cold start and for each state when the algorithm didn't recalculate for some reason.
            predictionModel.rollingHistory.forEachS(to = nowTick) { tick, state ->
                val historicalBg = sampledBgReadings.getAt(tick)
                if (historicalBg.isValid()) {
                    state.assumedBg = historicalBg
                } else if (validFilteredBg.isValid()) {
                    state.assumedBg = validFilteredBg
                }
            }

            // TODO: We should check if there is much more insulin then carbs,
            //  If this is the situation, tell the user to manually check the situation.

            // TODO: We should check extremely high BG and switch off the algorithm.

            // TODO: Check fast dropping or raising BG and tell the user

            // ------------------------------ Calculate predictions ------------------------------------

            // Most of our calculations below are based on a static prediction model for insulin and carbs.
            // To react to dynamic changes (e.g. unannounced snacks), we calculate an average deviation of our
            // model predictions to the real blood glucose.
            // The deviation is the actual slope of bg values minus the bgi, which is the predicted slope.
            val avgCurrentDeviationPerTick = calcAvgDeviationPerTick(DEVIATION_TIME_BASE)
            // Assumption: That deviation will be continued in the future but will fade away

            val meals = treatmentRepository.getMeals()
            val insulinApplications = treatmentRepository.getInsulinApplications()
            val settings = therapyManager.getCurrentTherapySettings()
            val pumpInsulinType = therapyManager.getPumpInsulinType()
            val setting = settings.insulinProfile.getSettingFor(pumpInsulinType)
            val dia = setting.dia
            val insulinPeak = setting.peak
            val correctionHorizon = setting.peak

            val (targetBg, lowThreshold) = therapyManager.getMaxBgSettings(
                from = now,
                to = now + correctionHorizon * 2
            )

            val isfNow = therapyManager.getIsfFactor(now)
            val crNow = therapyManager.getCrFactor(now)

            insight = insight.copy(
                bgFiltered = validFilteredBg,
                deviationPerTick = avgCurrentDeviationPerTick,
                targetBg = targetBg,
                isf = isfNow,
                cr = crNow
            )

            // Cache block 1: Influence of meals and insulin - the data is reused in succeeding calculation
            // calls until another metabolic event occurs, which invalidates the cache block

            // Cache block 2: Profile data - the data is reused in succeeding calculation calls
            // until the profile changes, which invalidates the cache block

            val activeDoses = insulinApplications.toActiveDoses()

            // Prediction block:
            // Update predicted BGI, update predicted BG
            predictionModel.calculate(
                currentBg = validFilteredBg,
                avgCurrentDeviationPerTick = avgCurrentDeviationPerTick,
                meals = meals,
                insulinDoses = activeDoses,
                dia = dia,
                insulinPeak = insulinPeak,
                therapyManager = therapyManager,
                carbsInsulinCalculator = carbsInsulinCalculator
            )

            val correctionTicks = timeline.inTicks(correctionHorizon)

            // -------------------------------- Low handling -------------------------------------------

            // Get out of a current or impending low by suggesting carbs.

            // Find the next tick where the value falls below the minimum within the next LOW_WARNING_THRESHOLD minutes
            val impendingLowTick = predictionModel.findNext(
                startAt = nowTick,
                until = nowTick.plusMinutes(LOW_WARNING_LOOKAHEAD.value.toInt()),
                predicate = { it.assumedBg.isValid() && it.assumedBg < lowThreshold + LOW_BG_SAFETY_MARGIN },
                block = { it.tick }
            )

            if (impendingLowTick != null) {
                // We're too low or becoming too low soon.

                // Will we recover from low in the current situation?
                val recovery = predictionModel.findNext(
                    startAt = impendingLowTick,
                    until = impendingLowTick.plusMinutes(LOW_RECOVERY_LOOKAHEAD.value.toInt()),
                    predicate = { it.assumedBg.isValid() && it.assumedBg > lowThreshold + LOW_BG_SAFETY_MARGIN },
                    block = { true }
                ) ?: false

                if (!recovery) {
                    // Find out, how low we'll come to calculate the amount of suggested carbs.

                    // Correct the minimum BG for twice the peak time of fast KE -> Don't look into the future too much
                    val bgMin = predictionModel.findBgMin(
                        startAt = nowTick,
                        until = nowTick.plusMinutes(FAST_KE_DEFAULT_PEAK.value.toInt() * 2),
                        block = { it.assumedBg }
                    )
                    if (bgMin != null && bgMin.isValid()) {
                        val bgErrorAtMin = targetBg - bgMin
                        val neededLowCorrectionCarbsForMinInG = convertToCarbsFromBgDelta(
                            bgDelta = bgErrorAtMin,
                            isf = isfNow,
                            cr = crNow
                        )
                        val recentCarbsInG = meals.
                            filter { meal -> meal.timestamp > now.minusMinutes(20) && meal.timestamp < now.plusMinutes(15) }.
                            sumOf { meal -> meal.carbGrams }
                        val rawCarbsInGHint = neededLowCorrectionCarbsForMinInG - recentCarbsInG
                        val carbsInGHint = (ceil(rawCarbsInGHint / 5.0) * 5).toInt()
                        if (carbsInGHint > 0) {
                            return CalculationResult.carbsSuggestion(carbsInGHint = carbsInGHint).withMetrics(insight) // Stop further processing when we're currently low
                        }
                    }
                }
            }

            // Expected future carbs and insulin activity
            val correctionTick = nowTick + correctionTicks
            val evaluationTimestamp = timeline.timestamp(correctionTick) // The same timestamp as in prediction model
            val futureActiveCarbsAtCorrection = carbsInsulinCalculator.cob(meals = meals, timestamp = evaluationTimestamp, includeFutureMeals = true)

            val artificialNormalBasalDoses = calculateArtificialNormalBasalDoses()

            // Our activeDoses contain basal rates.
            // To calculate net IOB values, normal basal rates need to be subtracted.
            // It is not sufficient to just leave out the basal applications since applications
            // which differ from the normal rate (e.g. low basal) will reduce (or increase) our net IOB.
            val iobNow = carbsInsulinCalculator.iob(
                insulinDoses = activeDoses,
                timestamp = now,
                dia = dia,
                peak = insulinPeak
            )

            val iobAtCorrection = carbsInsulinCalculator.iob(
                insulinDoses = activeDoses,
                timestamp = evaluationTimestamp,
                dia = dia,
                peak = insulinPeak
            )
            val normalBasalIobAtCorrection = carbsInsulinCalculator.iob(
                insulinDoses = artificialNormalBasalDoses,
                timestamp = evaluationTimestamp,
                dia = dia,
                peak = insulinPeak
            )

            val netIobAtCorrection = iobAtCorrection - normalBasalIobAtCorrection

            val deferredBoluses = therapyManager.getDeferredBoluses()
            val sumDeferredBolusesAtCorrection = deferredBoluses.
                filter { it.timestamp >= evaluationTimestamp }.
                fold(InsulinAmount.ZERO) { acc, next -> acc + next.amount }

            insight = insight.copy(
                futureActiveInsulin = netIobAtCorrection + sumDeferredBolusesAtCorrection,
                futureActiveCarbs = futureActiveCarbsAtCorrection
            )

            val tickStateAtCorrectionValues = predictionModel.withTickState(nowTick + correctionTicks) {
                it.assumedBg to it.cumulatedBasalActivity
            } ?: return CalculationResult.safetyBasal().withMetrics(insight)

            val predictedBgAtCorrection = tickStateAtCorrectionValues.first.takeIf { it.isValid() }
                ?: return CalculationResult.safetyBasal().withMetrics(insight)
            val cumulatedBasalActivityUntilCorrection = tickStateAtCorrectionValues.second

            val crAtCorrection = therapyManager.getCrFactor(evaluationTimestamp)

            insight = insight.copy(predictedBgAtCorrection = predictedBgAtCorrection)
            val bgErrorAtCorrection = predictedBgAtCorrection - targetBg // < 0 if too low

            // Low protection for "lower than target" situations
            if (bgErrorAtCorrection < LOW_TEMP_BASAL_TRIGGER) {
                // Prediction is too low under normal basal;
                // Either BG is falling, or, raising too slow -> Lower basal rate and defer meals

                // cumulatedInsulinActivityUntilCorrection is what we expect to be active from past
                // injections, with normal basal from now until peak, and without any new meal or correction treatments.
                val cumulatedInsulinActivityUntilCorrection = iobNow - iobAtCorrection
                val insulinEffectUntilCorrection = -convertToBgDeltaFromUnits(cumulatedInsulinActivityUntilCorrection, isfNow)
                val normalBasalEffectUntilCorrection = -convertToBgDeltaFromUnits(cumulatedBasalActivityUntilCorrection, isfNow)

                // All predictions are based on the assumption that we deliver normal basal. So
                // to calculate the low temp scenario, we just subtract the normal basal effect.
                val lowTempBasalEffectUntilCorrection = insulinEffectUntilCorrection - normalBasalEffectUntilCorrection

                if (bgErrorAtCorrection + lowTempBasalEffectUntilCorrection < ZERO_TEMP_BASAL_TRIGGER) {
                    // Prediction is too low, even without basal -> Zero temp and defer ongoing meal boluses
                    return CalculationResult.zeroTemp(durationInHours = 1).withMetrics(insight)
                }
                // Else go on with decreased basal
                val needed = -(normalBasalEffectUntilCorrection - bgErrorAtCorrection).coerceAtMost(BgDelta.ZERO)
                val normalEffect = -normalBasalEffectUntilCorrection
                val percent = if (normalEffect.mgdl < 0.01)
                    0 // Safety guard
                else
                    ((needed.mgdl * 100.0 / normalEffect.mgdl).coerceIn(0.0, 100.0)).toInt()

                return CalculationResult.tempBasal(
                    percent = percent,
                    durationInHours = 1
                ).withMetrics(insight)
            }

            // *****************************************************************************************
            // At this point, we're sure not to become (too) low, it's safe to have a normal basal rate and
            // to administer meal boluses
            // *****************************************************************************************

            // -------------------------------- Meal & high handling -----------------------------------

            // Calculate scheduled meal boluses
            var dueMealBolusAmount = InsulinAmount.ZERO
            val dueDeferredBoluses: MutableList<DeferredBolus> = mutableListOf()
            for (deferredBolus in deferredBoluses) {
                if (deferredBolus.timestamp < now) {
                    dueMealBolusAmount += deferredBolus.amount
                    dueDeferredBoluses += deferredBolus
                }
            }
            val sumFutureDeferredBoluses = deferredBoluses.
                filter { it.timestamp >= now }.
                fold(InsulinAmount.ZERO) { acc, next -> acc + next.amount }

            val insulinEquivalentOfCarbsAtCorrection = convertToInsulinAmountFromCarbs(carbs = futureActiveCarbsAtCorrection, cr = crAtCorrection)

            // If we're above the threshold, don't correct too much
            val bgCorrectError = if (bgErrorAtCorrection > HIGH_CORRECTION_TARGET_SAFETY_THRESHOLD)
                bgErrorAtCorrection - HIGH_CORRECTION_TARGET_SAFETY_THRESHOLD
            else
                bgErrorAtCorrection

            val bgErrorCorrectionUnits = (convertToInsulinAmountFromBgDelta(bgCorrectError, isfNow) * AGGRESSIVENESS_ERROR_CORRECTION)
                .coerceAtLeast(InsulinAmount.ZERO)

            val pumpCapabilities = therapyManager.getPumpCapabilities()

            // Simplified calculation. We mix remaining insulin/carbs activity of now and peak.
            val futureInsulin = netIobAtCorrection + dueMealBolusAmount + sumFutureDeferredBoluses
            if (futureInsulin + InsulinAmount.EPSILON >= insulinEquivalentOfCarbsAtCorrection + bgErrorCorrectionUnits) {
                // Meals and BG error are covered by IOB/planned boluses.
                // Return to normal basal rate and wait for insulin/carbs to act.
                return if (dueDeferredBoluses.isEmpty()) {
                    CalculationResult.normalSafetyBasal().withMetrics(insight)
                } else {
                    if (pumpCapabilities != null && dueMealBolusAmount < pumpCapabilities.minBolusAmount) {
                        CalculationResult.normalSafetyBasal().withMetrics(insight)
                    }
                    // Administer due deferred bolus.
                    // It might be that this is too much for the COB but this is in the
                    // responsibility of the user.
                    CalculationResult.mealBolus(
                        bolusAmount = dueMealBolusAmount,
                        handledDeferredBoluses = dueDeferredBoluses,
                    ).withMetrics(insight)
                }
            } else {
                if (bgErrorAtCorrection <= HIGH_CORRECTION_BOLUS_TRIGGER) {
                    return CalculationResult.normalSafetyBasal().withMetrics(insight)
                }

                // A correction is necessary.

                // If we don't have insulin/carbs influence, we can just correct the BG error.
                // But IF we have insulin/carbs influence, we must not just add predicted
                // future insulin and carbs because:
                // 1) Insulin and carbs activity can be so different in time that we would risk low BG
                // 2) Future carbs should be covered by deferred boluses

                // A correction is always dangerous, so we simulate its impact up to DIA.
                // If any future BG value falls too low, we scale down the correction so that
                // the threshold is not violated.
                val candidateCorrection = bgErrorCorrectionUnits - netIobAtCorrection
                val correctionPart = if (candidateCorrection > InsulinAmount.ZERO) {
                    val diaTicks = timeline.inTicks(dia)
                    val endTick = nowTick + diaTicks
                    val bgThreshold = targetBg - CORRECTION_ALLOWED_DROP_BELOW_TARGET
                    var minAllowedCorrection = candidateCorrection
                    var cumulatedBgDrop = BgDelta.ZERO

                    predictionModel.forEach(from = nowTick + 1, to = endTick) { tick, state ->
                        val baselineBg = state.assumedBg
                        if (baselineBg.isValid()) {
                            val tickIsf = state.isf ?: therapyManager.getIsfFactor(timeline.timestamp(tick))
                            val intervalsSinceApplication = tick.value - nowTick.value
                            val effectiveInsulin = carbsInsulinCalculator.insulinCalculationCache.effectiveInsulin(
                                amount = candidateCorrection,
                                dia = dia,
                                peak = insulinPeak,
                                intervalsSinceApplication = intervalsSinceApplication
                            )
                            val bgDropInTick = convertToBgDeltaFromUnits(effectiveInsulin, tickIsf)
                            cumulatedBgDrop += bgDropInTick
                            val simulatedBg = baselineBg - cumulatedBgDrop

                            if (simulatedBg < bgThreshold) {
                                if (cumulatedBgDrop.mgdl > 0.0) {
                                    val maxAllowedBgDrop = (baselineBg - bgThreshold).coerceAtLeast(BgDelta.ZERO)
                                    val allowedFraction = maxAllowedBgDrop.mgdl / cumulatedBgDrop.mgdl
                                    val allowedCorrection = candidateCorrection * allowedFraction
                                    if (allowedCorrection < minAllowedCorrection) {
                                        minAllowedCorrection = allowedCorrection
                                    }
                                } else {
                                    minAllowedCorrection = InsulinAmount.ZERO
                                }
                            }
                        }
                    }

                    minAllowedCorrection.coerceAtLeast(InsulinAmount.ZERO)
                } else {
                    InsulinAmount.ZERO
                }
                val mealCorrectionBolusAmount = dueMealBolusAmount + correctionPart

                if (pumpCapabilities != null && mealCorrectionBolusAmount < pumpCapabilities.minBolusAmount) {
                    return CalculationResult.normalSafetyBasal().withMetrics(insight)
                }

                // We assume that the current blood glucose deviation is primarily caused by faster
                // carbs absorption than specified in the model (just a timing issue).
                // Before we generate additional correction insulin, to prevent overdoses, we bring
                // planned future doses forward to the present (“shifting”). If the user has indeed
                // underreported their intake, the system will gradually use up all deferred boluses
                // and only then increase the total dose.
                val deferredBolusUpdates = if (correctionPart > InsulinAmount.EPSILON) {
                    deferredBoluses.filter { it.timestamp >= now }.minByOrNull { it.timestamp }?.let { next ->
                        listOf(DeferredBolusUpdate(next.id, (next.amount - correctionPart).coerceAtLeast(InsulinAmount.ZERO)))
                    }
                } else null

                return CalculationResult.mealOrCorrectionBolus(
                    bolusAmount = mealCorrectionBolusAmount,
                    handledDeferredBoluses = dueDeferredBoluses,
                    correctionPart = correctionPart,
                    deferredBolusUpdates = deferredBolusUpdates
                ).withMetrics(insight)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error while recalculating", e)
            return CalculationResult.internalError("Error while recalculating", e).withMetrics(insight)
        }
    }

    companion object {
        val TAG = ApsAlgorithmImpl::class.simpleName!!

        const val PREDICTION_WINDOW_HOURS = 6
        val DEVIATION_TIME_BASE = Minutes(30)
        val BG_HISTORY_TIME = DEVIATION_TIME_BASE + Minutes(30)
        val PRESERVE_PREDICTIONS_PAST_TIME = DEVIATION_TIME_BASE

        suspend fun create(
            treatmentRepository: TreatmentRepository,
            glucoseRepository: GlucoseRepository,
            therapyManager: TherapyManager,
            timeline: Timeline,
            carbsInsulinCalculator: CarbsInsulinCalculator,
        ): ApsAlgorithm {
            val predictionModel = PredictionModel(
                predictionWindowHours = PREDICTION_WINDOW_HOURS,
                timeline = timeline
            )
            predictionModel.initializeToTick(Timestamp.now().minus(PRESERVE_PREDICTIONS_PAST_TIME))

            val history = RecentBgReadingsHistory(BG_HISTORY_TIME)
            val initialReadings = glucoseRepository.loadBgReadings(from = Timestamp.now().minus(BG_HISTORY_TIME))
            history.setAll(initialReadings)

            val sampledBgReadings = SampledBgReadings(timeline, history)

            return ApsAlgorithmImpl(
                timeline = timeline,
                treatmentRepository = treatmentRepository,
                sampledBgReadings = sampledBgReadings,
                history = history,
                predictionModel = predictionModel,
                carbsInsulinCalculator = carbsInsulinCalculator,
                therapyManager = therapyManager
            )
        }
    }
}