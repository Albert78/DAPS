package de.dh.daps.core.aps

import android.content.Intent
import android.util.Log
import de.dh.daps.AppPreferencesRepository
import de.dh.daps.common.model.ApsMode
import de.dh.daps.common.model.BolusDeliveryState
import de.dh.daps.common.model.BolusStatus
import de.dh.daps.common.model.DeferredBolus
import de.dh.daps.common.model.ID_UNDEFINED
import de.dh.daps.common.model.InsulinAmount
import de.dh.daps.common.model.InsulinHistory
import de.dh.daps.common.model.InsulinType
import de.dh.daps.common.model.MealEntry
import de.dh.daps.common.model.ToDo
import de.dh.daps.common.model.data.BgBlock
import de.dh.daps.common.model.data.BgDelta
import de.dh.daps.common.model.data.BgValue
import de.dh.daps.common.model.data.CurrentTherapySettings
import de.dh.daps.common.model.data.InsulinProfile
import de.dh.daps.common.model.data.ScheduledTherapyAdjustment
import de.dh.daps.common.model.data.Timestamp
import de.dh.daps.common.model.data.getAmountForMinute
import de.dh.daps.common.model.data.getBgForMinute
import de.dh.daps.core.pump.PumpCommand
import de.dh.daps.core.pump.PumpManager
import de.dh.daps.core.repository.AlarmRepository
import de.dh.daps.core.repository.TherapyRepository
import de.dh.daps.core.repository.TreatmentRepository
import de.dh.daps.core.system.SystemWakeService
import de.dh.daps.core.system.WakeupHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Recommendations for manual treatments, which are displayed as notifications to the user.
 */
sealed class ApsRecommendation {
    data class Carbs(val amountInGram: Int) : ApsRecommendation()
    data class Bolus(val amount: InsulinAmount) : ApsRecommendation()
}

/**
 * Represents an active lock on therapy-related operations.
 * This lock must be held and passed to critical functions in [TherapyManager].
 */
data class TreatmentLock(val tag: String)

sealed class LockResult {
    data object Success : LockResult()
    data class Busy(val owner: String) : LockResult()
}

/**
 * Central manager for all therapy-related operations and decisions in the APS system.
 *
 * This class serves as the main interface for both the automated system (APS Core) and the user
 * interface to execute treatments and manage therapy settings.
 *
 * ### Functional Areas:
 * - **Therapy Settings Management**: Access and modification of insulin profiles, factors (ISF, CR),
 *   basal rates, and blood glucose targets.
 * - **Insulin Delivery**: Execution of bolus and temporary basal rate commands via the [PumpManager].
 * - **Treatment Recommendations**: Generation of recommendations for manual carbs or bolus delivery.
 * - **Job Management**: Coordination and cleanup of pending insulin delivery tasks.
 *
 * ### Locking System (Concurrency Protection):
 * To prevent race conditions and conflicting treatments (e.g., the system setting a basal rate
 * while the user is delivering a bolus), critical functions require a [TreatmentLock].
 *
 * Callers must acquire this lock via [tryAcquire]. If successful, they receive a [TreatmentLock]
 * token that must be passed to all critical methods. These methods verify the lock's validity
 * via [checkLock] before execution.
 */
class TherapyManager(
    private val therapyRepository: TherapyRepository,
    private val treatmentRepository: TreatmentRepository,
    private val appPreferencesRepository: AppPreferencesRepository,
    private val pumpManager: PumpManager,
    private val systemOrchestrator: SystemOrchestrator,
    private val alarmRepository: AlarmRepository,
    private val scope: CoroutineScope,
    private val wakeService: SystemWakeService? = null
) {
    private val mutex = Mutex()
    private val executionMutex = Mutex()
    private var currentExecutionOwner: String? = null

    private val _recommendations = MutableStateFlow<List<ApsRecommendation>>(emptyList())
    val recommendations: StateFlow<List<ApsRecommendation>> = _recommendations.asStateFlow()

    val currentTherapySettingsFlow: Flow<CurrentTherapySettings> = therapyRepository.observeCurrentTherapySettings()

    /**
     * Wires up the therapy manager with external components, sync history.
     */
    fun startInitialization() {
        pumpManager.setOnHistoryUpdateListener { history ->
            updatePumpHistory(history)
        }

        pumpManager.setOnBolusStatusUpdateListener { bolusStatus ->
            updateBolusStatus(bolusStatus)
        }

        pumpManager.issueCommand(PumpCommand.SyncHistory)

        wakeService?.registerHandler(WAKEUP_TAG_ADJUSTMENT, object : WakeupHandler {
            override fun onWakeup(wakeupId: UInt?, intent: Intent?) {
                scope.launch {
                    checkAndApplyTherapyAdjustmentTiming()
                }
            }
        })

        scope.launch {
            checkAndApplyTherapyAdjustmentTiming()
        }

        scope.launch {
            currentTherapySettingsFlow
                .map { it.effectiveInsulinProfile }
                .distinctUntilChanged()
                .collect { effectiveProfile ->
                    pumpManager.issueCommand(
                        PumpCommand.SetProfile(effectiveProfile)
                    )
                }
        }
    }

    suspend fun checkAndApplyTherapyAdjustmentTiming() {
        mutex.withLock {
            var currentSettings = runCatching { getCurrentTherapySettings() }.getOrNull() ?: return@withLock
            val now = Timestamp.now()

            // 1. Check active therapy adjustment expiration
            val activeEndTime = currentSettings.adjustmentEndTime
            if (activeEndTime != null && now >= activeEndTime) {
                // Adjustment expired -> reset to neutral / standard
                therapyRepository.updateCurrentTherapySettings(
                    insulinProfileId = currentSettings.insulinProfile.id,
                    defaultBgBlocks = currentSettings.defaultBgBlocks,
                    insulinAdjustmentPercentage = 0,
                    targetBgOverride = null,
                    lowThresholdOverride = null,
                    alarmProfileOverrideId = null,
                    adjustmentHint = null,
                    adjustmentEndTime = null
                )
                Log.d(TAG, "Therapy adjustment expired and reset to neutral")
                currentSettings = getCurrentTherapySettings()
            } else if (activeEndTime != null && now < activeEndTime) {
                // Adjustment is active -> schedule end wakeup if needed
                wakeService?.scheduleWakeup(WAKEUP_TAG_ADJUSTMENT, WAKEUP_ID_END, activeEndTime)
                Log.d(TAG, "Therapy adjustment is active, scheduled end wakeup for $activeEndTime")
            }

            // 2. Check scheduled therapy adjustment timing & transition
            val scheduled = runCatching { getScheduledTherapyAdjustment() }.getOrNull()
            if (scheduled != null) {
                if (now >= scheduled.endTime) {
                    // Scheduled adjustment expired before activation
                    therapyRepository.deleteAllScheduledTherapyAdjustments()
                    Log.d(TAG, "Scheduled therapy adjustment expired before activation and was removed")
                } else if (now >= scheduled.startTime) {
                    // Start time reached -> activate scheduled adjustment as current adjustment
                    therapyRepository.updateCurrentTherapySettings(
                        insulinProfileId = currentSettings.insulinProfile.id,
                        defaultBgBlocks = currentSettings.defaultBgBlocks,
                        insulinAdjustmentPercentage = scheduled.percentage,
                        targetBgOverride = scheduled.targetBgOverride,
                        lowThresholdOverride = scheduled.lowThresholdOverride,
                        alarmProfileOverrideId = scheduled.effectiveAlarmProfileOverrideId,
                        adjustmentHint = scheduled.adjustmentHint,
                        adjustmentEndTime = scheduled.endTime
                    )
                    therapyRepository.deleteAllScheduledTherapyAdjustments()
                    wakeService?.scheduleWakeup(WAKEUP_TAG_ADJUSTMENT, WAKEUP_ID_END, scheduled.endTime)
                    Log.d(TAG, "Scheduled therapy adjustment activated (valid until ${scheduled.endTime})")
                } else {
                    // Scheduled adjustment is pending in the future -> schedule start wakeup
                    wakeService?.scheduleWakeup(WAKEUP_TAG_ADJUSTMENT, WAKEUP_ID_START, scheduled.startTime)
                    Log.d(TAG, "Scheduled therapy adjustment pending, scheduled start wakeup for ${scheduled.startTime}")
                }
            }
        }
    }

    // -----------------------------------------------------------------------------------------
    // --- Section: Therapy Settings Management ---
    // -----------------------------------------------------------------------------------------

    suspend fun getCurrentTherapySettings(): CurrentTherapySettings = therapyRepository.getCurrentTherapySettings()

    /**
     * Gets the planned basal rate at the given timestamp.
     */
    suspend fun getBasalPerHour(timestamp: Timestamp): InsulinAmount {
        val settings = getCurrentTherapySettings()
        val baseBasal = settings.effectiveInsulinProfile.basalBlocks.getAmountForMinute(timestamp.minutesSinceMidnight())
        return InsulinAmount(baseBasal)
    }

    /**
     * Gets the carbohydrate to insulin ratio to be used for calculations at the given timestamp.
     * The CR is a measure of how many grams of carbohydrates are covered by one unit of insulin.
     * Unit: Grams of carbs.
     */
    suspend fun getCrFactor(timestamp: Timestamp): Double {
        val settings = getCurrentTherapySettings()
        return settings.effectiveInsulinProfile.crBlocks.getAmountForMinute(timestamp.minutesSinceMidnight())
    }

    /**
     * Gets the insulin sensitivity factor to be used for calculations for the given timestamp.
     * The ISF (sometimes also called the correction factor, CF) is a measure of how much a single
     * unit of insulin lowers blood glucose levels.
     * Unit: Blood glucose delta.
     */
    suspend fun getIsfFactor(timestamp: Timestamp): BgDelta {
        val settings = getCurrentTherapySettings()
        val amount = settings.effectiveInsulinProfile.isfBlocks.getAmountForMinute(timestamp.minutesSinceMidnight())
        return BgDelta.fromMgDl(amount.toInt())
    }

    suspend fun getBgSettings(timestamp: Timestamp = Timestamp.now()): Pair<BgValue, BgValue> {
        val settings = getCurrentTherapySettings()
        val defaultBg = settings.defaultBgBlocks.getBgForMinute(timestamp.minutesSinceMidnight())
        return Pair(
            settings.targetBgOverride ?: defaultBg.first,
            settings.lowThresholdOverride ?: defaultBg.second
        )
    }

    suspend fun updateDefaultBgBlocks(blocks: List<BgBlock>) {
        mutex.withLock {
            val currentSettings = getCurrentTherapySettings()
            therapyRepository.updateCurrentTherapySettings(
                insulinProfileId = currentSettings.insulinProfile.id,
                defaultBgBlocks = blocks,
                insulinAdjustmentPercentage = currentSettings.insulinAdjustmentPercentage,
                targetBgOverride = currentSettings.targetBgOverride,
                lowThresholdOverride = currentSettings.lowThresholdOverride,
                alarmProfileOverrideId = currentSettings.alarmProfileOverrideId,
                adjustmentHint = currentSettings.adjustmentHint
            )
        }
    }

    suspend fun getPumpInsulinType(): InsulinType {
        val currentSettings = getCurrentTherapySettings()
        return currentSettings.insulinProfile.insulinType
    }

    suspend fun getPumpCapabilities() = pumpManager.insulinPump?.pumpCapabilities?.value

    suspend fun getAllInsulinProfiles() = therapyRepository.getAllInsulinProfiles()

    fun observeAllInsulinProfiles() = therapyRepository.observeAllInsulinProfiles()

    /**
     * Updates the current therapy settings based on a selected profile.
     * This will create a copy of the profile's therapy data as the active configuration.
     */
    suspend fun selectInsulinProfile(profile: InsulinProfile) {
        mutex.withLock {
            val currentSettings = getCurrentTherapySettings()
            therapyRepository.updateCurrentTherapySettings(
                insulinProfileId = profile.id,
                defaultBgBlocks = currentSettings.defaultBgBlocks,
                insulinAdjustmentPercentage = currentSettings.insulinAdjustmentPercentage,
                targetBgOverride = currentSettings.targetBgOverride,
                lowThresholdOverride = currentSettings.lowThresholdOverride,
                alarmProfileOverrideId = currentSettings.alarmProfileOverrideId,
                adjustmentHint = currentSettings.adjustmentHint
            )
        }
    }

    suspend fun setTherapyAdjustment(
        percentage: Int,
        targetBg: BgValue?,
        lowThreshold: BgValue?,
        alarmProfileOverrideId: Long? = null,
        adjustmentHint: String? = null,
        adjustmentEndTime: Timestamp? = null
    ) {
        mutex.withLock {
            val currentSettings = getCurrentTherapySettings()

            therapyRepository.updateCurrentTherapySettings(
                insulinProfileId = currentSettings.insulinProfile.id,
                defaultBgBlocks = currentSettings.defaultBgBlocks,
                insulinAdjustmentPercentage = percentage,
                targetBgOverride = targetBg,
                lowThresholdOverride = lowThreshold,
                alarmProfileOverrideId = alarmProfileOverrideId,
                adjustmentHint = adjustmentHint,
                adjustmentEndTime = adjustmentEndTime
            )

            // Schedule system wakeup if needed
            val now = Timestamp.now()
            if (adjustmentEndTime != null && adjustmentEndTime > now) {
                wakeService?.scheduleWakeup(WAKEUP_TAG_ADJUSTMENT, WAKEUP_ID_END, adjustmentEndTime)
                Log.d(TAG, "Scheduled adjustment end wakeup for $adjustmentEndTime")
            }
        }
    }

    suspend fun setInsulinAdjustmentPercentage(percentage: Int) {
        mutex.withLock {
            val currentSettings = getCurrentTherapySettings()
            therapyRepository.updateCurrentTherapySettings(
                insulinProfileId = currentSettings.insulinProfile.id,
                defaultBgBlocks = currentSettings.defaultBgBlocks,
                insulinAdjustmentPercentage = percentage,
                targetBgOverride = currentSettings.targetBgOverride,
                lowThresholdOverride = currentSettings.lowThresholdOverride,
                alarmProfileOverrideId = currentSettings.alarmProfileOverrideId,
                adjustmentHint = currentSettings.adjustmentHint
            )
        }
    }

    suspend fun setTargetBgOverride(target: BgValue?) {
        mutex.withLock {
            val currentSettings = getCurrentTherapySettings()
            therapyRepository.updateCurrentTherapySettings(
                insulinProfileId = currentSettings.insulinProfile.id,
                defaultBgBlocks = currentSettings.defaultBgBlocks,
                insulinAdjustmentPercentage = currentSettings.insulinAdjustmentPercentage,
                targetBgOverride = target,
                lowThresholdOverride = currentSettings.lowThresholdOverride,
                alarmProfileOverrideId = currentSettings.alarmProfileOverrideId,
                adjustmentHint = currentSettings.adjustmentHint
            )
        }
    }

    suspend fun setLowThresholdOverride(threshold: BgValue?) {
        mutex.withLock {
            val currentSettings = getCurrentTherapySettings()
            therapyRepository.updateCurrentTherapySettings(
                insulinProfileId = currentSettings.insulinProfile.id,
                defaultBgBlocks = currentSettings.defaultBgBlocks,
                insulinAdjustmentPercentage = currentSettings.insulinAdjustmentPercentage,
                targetBgOverride = currentSettings.targetBgOverride,
                lowThresholdOverride = threshold,
                alarmProfileOverrideId = currentSettings.alarmProfileOverrideId,
                adjustmentHint = currentSettings.adjustmentHint
            )
        }
    }

    // --- Scheduled Therapy Adjustments ---

    fun observeScheduledTherapyAdjustment(): Flow<ScheduledTherapyAdjustment?> =
        therapyRepository.observeAllScheduledTherapyAdjustments().map { it.firstOrNull() }

    fun observeScheduledTherapyAdjustments(): Flow<List<ScheduledTherapyAdjustment>> =
        therapyRepository.observeAllScheduledTherapyAdjustments()

    suspend fun getScheduledTherapyAdjustment(): ScheduledTherapyAdjustment? =
        therapyRepository.getAllScheduledTherapyAdjustments().firstOrNull()

    suspend fun saveScheduledTherapyAdjustment(adjustment: ScheduledTherapyAdjustment): Long {
        // Enforce singleton: clear any existing scheduled adjustment first, then insert new record
        therapyRepository.deleteAllScheduledTherapyAdjustments()
        val newAdjustment = adjustment.copy(id = ID_UNDEFINED)
        val id = therapyRepository.saveScheduledTherapyAdjustment(newAdjustment)
        checkAndApplyTherapyAdjustmentTiming()
        return id
    }

    suspend fun deleteScheduledTherapyAdjustment() {
        therapyRepository.deleteAllScheduledTherapyAdjustments()
    }

    // -----------------------------------------------------------------------------------------
    // --- Section: Critical Insulin & Carb Management (Requires Lock) ---
    // -----------------------------------------------------------------------------------------

    /**
     * Verifies that the provided lock matches the current execution owner.
     * @throws IllegalStateException if the lock is invalid or not held.
     */
    private fun checkLock(treatmentLock: TreatmentLock) {
        if (currentExecutionOwner != treatmentLock.tag) {
            throw IllegalStateException("Execution lock not held by ${treatmentLock.tag} (current owner: $currentExecutionOwner)")
        }
    }

    /**
     * Triggered when the history of actual bolus and basal values was updated.
     */
    suspend fun updatePumpHistory(history: InsulinHistory) {
        val cts = getCurrentTherapySettings()
        treatmentRepository.mergeInsulinHistory(history, cts.insulinProfile.insulinType)
    }

    /**
     * Triggered when the bolus status of the pump was updated.
     */
    suspend fun updateBolusStatus(bolusStatus: BolusStatus) {
        if (bolusStatus.state != BolusDeliveryState.COMPLETED) return

        val appId = bolusStatus.bolusId?.toLongOrNull() ?: return

        val confirmed = treatmentRepository.confirmScheduledBolus(
            id = appId,
            timestamp = bolusStatus.timestamp,
            deliveredAmount = bolusStatus.deliveredAmount
        )

        if (confirmed) {
            Log.i(TAG, "Confirmed scheduled bolus application #$appId")
        }
    }

    /**
     * Initiates a bolus delivery.
     *
     * Depending on the current [ApsMode], this will either directly command the pump
     * or record a recommendation for the user.
     *
     * @param treatmentLock The lock held by the caller.
     * @param amount The amount of insulin to deliver.
     * @param handledDeferredBoluses Optional deferred boluses that were handled by this delivery.
     */
    suspend fun issueBolus(
        treatmentLock: TreatmentLock,
        amount: InsulinAmount,
        meal: MealEntry? = null,
        handledDeferredBoluses: List<DeferredBolus>? = null,
        correctionPart: InsulinAmount = InsulinAmount.ZERO,
        basalPart: InsulinAmount = InsulinAmount.ZERO
    ) {
        checkLock(treatmentLock)

        // Record insulin administration in meals
        val isMealBolus = meal != null || (handledDeferredBoluses?.any { it.mealId != null } == true)
        if (meal != null && meal.id != ID_UNDEFINED) {
            val mealInsulin = (amount - correctionPart - basalPart).coerceAtLeast(InsulinAmount.ZERO)
            if (mealInsulin > InsulinAmount.ZERO) {
                treatmentRepository.addAdministeredInsulinToMeal(meal.id, mealInsulin)
            }
        }
        handledDeferredBoluses?.forEach { deferredBolus ->
            val mealId = deferredBolus.mealId
            if (mealId != null && mealId != ID_UNDEFINED && (meal == null || meal.id != mealId)) {
                treatmentRepository.addAdministeredInsulinToMeal(mealId, deferredBolus.amount)
            }
        }
        // Remove deferred boluses
        handledDeferredBoluses?.let {
            treatmentRepository.removeDeferredBoluses(it)
        }
        // Preserve delivery metadata until history sync
        val cts = getCurrentTherapySettings()
        val scheduledEntry = treatmentRepository.addScheduledPumpInsulinEntry(
            timestamp = Timestamp.now(),
            amount = amount,
            insulinType = cts.insulinProfile.insulinType,
            basal = basalPart > InsulinAmount.ZERO,
            correction = correctionPart > InsulinAmount.ZERO,
            meal = isMealBolus
        )
        val bolusId = if (scheduledEntry.id != 0L) scheduledEntry.id.toString() else null

        val minBolusIncrement = getPumpCapabilities()?.minBolusAmount
        if (minBolusIncrement != null && amount < minBolusIncrement) {
            Log.i(TAG, "Skipping bolus which is too low for pump (amount=$amount, minBolusIncrement=$minBolusIncrement)")
            return
        }
        when (systemOrchestrator.apsMode.value) {
            ApsMode.Suspend -> return
            ApsMode.BasalOnly -> recommendBolus(treatmentLock, amount)
            ApsMode.AutoCorrection -> {
                pumpManager.issueCommand(PumpCommand.DeliverBolus(amount, bolusId))
            }
        }
    }

    /**
     * Sets a temporary basal rate on the pump.
     *
     * @param treatmentLock The lock held by the caller.
     * @param durationInHours The duration for the temporary basal rate.
     * @param percent The relative basal rate in percent.
     */
    fun setTempBasal(treatmentLock: TreatmentLock, durationInHours: Int, percent: Int) {
        checkLock(treatmentLock)
        when (systemOrchestrator.apsMode.value) {
            ApsMode.Suspend -> return
            ApsMode.BasalOnly -> return
            ApsMode.AutoCorrection -> {
                pumpManager.issueCommand(
                    PumpCommand.SetTempBasal(
                        percent = percent,
                        durationHours = durationInHours
                    )
                )
            }
        }
    }

    /**
     * Cancels any active temporary basal rate on the pump.
     */
    fun clearTempBasal(treatmentLock: TreatmentLock) {
        checkLock(treatmentLock)
        when (systemOrchestrator.apsMode.value) {
            ApsMode.Suspend -> return
            ApsMode.BasalOnly -> return
            ApsMode.AutoCorrection -> {
                pumpManager.issueCommand(
                    PumpCommand.CancelTempBasal
                )
            }
        }
    }

    /**
     * Clears all currently active therapy recommendations.
     */
    fun clearRecommendations(treatmentLock: TreatmentLock) {
        checkLock(treatmentLock)
        _recommendations.value = emptyList()
    }

    /**
     * Records a recommendation for carb intake.
     */
    fun recommendCarbs(treatmentLock: TreatmentLock, amountInGram: Int) {
        checkLock(treatmentLock)
        _recommendations.value += ApsRecommendation.Carbs(amountInGram)
    }

    /**
     * Records a recommendation for bolus delivery.
     */
    fun recommendBolus(treatmentLock: TreatmentLock, amount: InsulinAmount) {
        checkLock(treatmentLock)
        _recommendations.value += ApsRecommendation.Bolus(amount)
    }

    /**
     * Schedules a reminder for the user to eat their meal.
     *
     * @param mealTimestamp The time when the meal is planned to be eaten.
     */
    fun scheduleMealReminder(mealTimestamp: Timestamp) {
        ToDo.toBeImplemented("Schedule meal reminder")
        // TODO: Implement meal reminder notification logic
        Log.i(TAG, "Scheduled meal reminder for $mealTimestamp")
    }

    suspend fun addDeferredBolus(treatmentLock: TreatmentLock, deferredBolus: DeferredBolus) {
        checkLock(treatmentLock)
        treatmentRepository.addDeferredBolus(deferredBolus)
    }

    suspend fun getDeferredBoluses(): List<DeferredBolus> {
        return treatmentRepository.getDeferredBoluses()
    }

    suspend fun updateDeferredBolus(treatmentLock: TreatmentLock, deferredBolus: DeferredBolus) {
        checkLock(treatmentLock)
        treatmentRepository.updateDeferredBolus(deferredBolus)
    }

    suspend fun removeDeferredBolus(treatmentLock: TreatmentLock, deferredBolus: DeferredBolus) {
        checkLock(treatmentLock)
        treatmentRepository.removeDeferredBolus(deferredBolus)
    }

    suspend fun applyDeferredBolusUpdates(treatmentLock: TreatmentLock, updates: List<DeferredBolusUpdate>) {
        checkLock(treatmentLock)
        val minBolusIncrement = pumpManager.insulinPump?.pumpCapabilities?.value?.minBolusIncrement ?: InsulinAmount.ZERO
        val allDeferred = treatmentRepository.getDeferredBoluses()

        for (update in updates) {
            val bolus = allDeferred.find { it.id == update.id } ?: continue
            if (update.newAmount > minBolusIncrement) {
                treatmentRepository.updateDeferredBolus(bolus.copy(amount = update.newAmount))
            } else {
                treatmentRepository.removeDeferredBolus(bolus)
            }
        }
    }

    /**
     * Causes the insulin pump to execute its pending jobs and to do a history sync.
     * @return Number of pending jobs.
     */
    suspend fun waitForPumpSync(treatmentLock: TreatmentLock): Int {
        checkLock(treatmentLock)
        pumpManager.issueCommand(command = PumpCommand.SyncHistory)
        pumpManager.waitForJobsOrError()
        return pumpManager.getPendingJobsCount()
    }

    /**
     * Tries to acquire a lock for a specific execution block.
     * The lock must be acquired, if either
     * - the following process needs consistent data for the ongoing process (e.g. MealCorrectionBolus screen,
     *   where the bolus decision needs a stable carbs and insulin situation until commit)
     * or
     * - the following process needs to change critical data which might interfere with a
     *   potential ongoing other process (like issuing a bolus will interfere with an ongoing
     *   core calculation).
     * Most of the critical functions require the lock in their method signature.
     * If the lock is already held by another system part, returns [LockResult.Busy].
     * Otherwise, executes the block and returns [LockResult.Success].
     */
    suspend fun tryAcquire(tag: String, block: suspend (TreatmentLock) -> Unit): LockResult {
        if (!executionMutex.tryLock()) {
            return LockResult.Busy(currentExecutionOwner ?: "Unknown")
        }
        val treatmentLock = TreatmentLock(tag)
        currentExecutionOwner = tag
        return try {
            block(treatmentLock)
            LockResult.Success
        } finally {
            currentExecutionOwner = null
            executionMutex.unlock()
        }
    }

    companion object {
        val TAG = TherapyManager::class.simpleName
        private const val WAKEUP_TAG_ADJUSTMENT = "TherapyAdjustment"
        private val WAKEUP_ID_START = 1u
        private val WAKEUP_ID_END = 2u
    }
}