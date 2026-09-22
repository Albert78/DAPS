package de.dh.daps.ui.screens.manualcontrol

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import de.dh.daps.common.model.BasalStatus
import de.dh.daps.common.model.BolusStatus
import de.dh.daps.common.model.DeferredBolus
import de.dh.daps.common.model.InsulinAmount
import de.dh.daps.common.model.InsulinApplication
import de.dh.daps.common.model.InsulinStatus
import de.dh.daps.common.model.MealEntry
import de.dh.daps.common.model.data.BgReading
import de.dh.daps.common.model.data.Timestamp
import de.dh.daps.common.model.toActiveDoses
import de.dh.daps.core.SystemRegistry
import de.dh.daps.core.aps.ApsRecommendation
import de.dh.daps.core.aps.LockResult
import de.dh.daps.core.aps.TreatmentLock
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

/**
 * Contains metabolic and therapy context information (current glucose, IOB, COB, last and next meal).
 */
data class ManualControlContextInfoUiModel(
    val lastBgReading: BgReading? = null,
    val iob: InsulinAmount = InsulinAmount.ZERO,
    val cob: Double = 0.0,
    val lastPastMeal: MealEntry? = null,
    val nextPlannedMeal: MealEntry? = null,
    val hasNextPlannedMealReminder: Boolean = false,
    val lastBolus: InsulinApplication? = null,
)

/**
 * Represents the status and capabilities of the connected insulin pump.
 */
data class ManualControlPumpUiModel(
    val isConnected: Boolean = false,
    val model: String? = null,
    val basalStatus: BasalStatus? = null,
    val bolusStatus: BolusStatus? = null,
    val minBolusAmount: InsulinAmount = InsulinAmount(0.05),
    val maxBolusSize: InsulinAmount = InsulinAmount(25.0)
)

/**
 * Overall UI state for the Manual Control screen.
 */
data class ManualControlUiState(
    val contextInfo: ManualControlContextInfoUiModel = ManualControlContextInfoUiModel(),
    val recommendations: List<ApsRecommendation> = emptyList(),
    val pump: ManualControlPumpUiModel = ManualControlPumpUiModel(),
    val showLockError: Boolean = false,
    val lockErrorOwner: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
class ManualControlViewModel(
    systemRegistry: SystemRegistry
) : ViewModel() {
    private val recommendationManager = systemRegistry.recommendationManager
    private val treatmentRepository = systemRegistry.treatmentRepository
    private val pumpManager = systemRegistry.pumpManager
    private val therapyManager = systemRegistry.therapyManager
    private val glucoseRepository = systemRegistry.glucoseRepository
    private val carbsInsulinCalculator = systemRegistry.carbsInsulinCalculator

    private val lockErrorFlow = MutableStateFlow<String?>(null)

    private val contextInfoFlow: Flow<ManualControlContextInfoUiModel> = combine(
        glucoseRepository.currentBg,
        treatmentRepository.observeInsulinApplications(),
        treatmentRepository.observeMeals(),
        therapyManager.currentTherapySettingsFlow
    ) { currentBg, insulin, meals, settings ->
        val now = Timestamp.now()
        val iob = carbsInsulinCalculator.iob(
            insulinDoses = insulin.toActiveDoses(),
            timestamp = now,
            dia = settings.insulinProfile.dia,
            peak = settings.insulinProfile.peak
        )
        val cob = carbsInsulinCalculator.cob(
            meals = meals,
            timestamp = now,
            includeFutureMeals = false
        )

        // Pure MealEntry objects (excluding MealReminders)
        val pastMealCutoff = now.minusHours(PAST_MEAL_LOOKBACK_HOURS)
        val pastMeal = meals
            .filter { it.timestamp in pastMealCutoff..now }
            .maxByOrNull { it.timestamp }
        val nextMeal = meals
            .filter { it.timestamp > now }
            .minByOrNull { it.timestamp }

        val lastBolus = insulin
            .filter { !it.basal && it.status != InsulinStatus.Cancelled && it.status != InsulinStatus.Invalidated && it.timestamp <= now.plusMinutes(5) }
            .maxByOrNull { it.timestamp }

        ManualControlContextInfoUiModel(
            lastBgReading = currentBg,
            iob = iob,
            cob = cob,
            lastPastMeal = pastMeal,
            nextPlannedMeal = nextMeal,
            lastBolus = lastBolus
        )
    }

    private val pumpFlow: Flow<ManualControlPumpUiModel> = pumpManager.activeInsulinPump.flatMapLatest { pump ->
        if (pump == null) {
            flowOf(ManualControlPumpUiModel())
        } else {
            combine(
                pump.isConnected,
                pump.hardwareInformation,
                pump.basalStatus,
                pump.bolusStatus,
                pump.pumpCapabilities
            ) { connected, hardware, basal, bolus, capabilities ->
                ManualControlPumpUiModel(
                    isConnected = connected,
                    model = hardware?.model,
                    basalStatus = basal,
                    bolusStatus = bolus,
                    minBolusAmount = capabilities.minBolusAmount,
                    maxBolusSize = capabilities.maxBolusSize
                )
            }
        }
    }

    val uiState: StateFlow<ManualControlUiState> = combine(
        contextInfoFlow,
        recommendationManager.recommendations,
        recommendationManager.mealReminders,
        pumpFlow,
        lockErrorFlow
    ) { contextInfo, recommendations, mealReminders, pump, lockErrorOwner ->
        val nextMeal = contextInfo.nextPlannedMeal
        val hasNextPlannedMealReminder = if (nextMeal != null) {
            mealReminders.any { reminder ->
                (reminder.mealId != null && reminder.mealId == nextMeal.id) ||
                        reminder.mealTimestamp == nextMeal.timestamp
            }
        } else false

        val filteredRecommendations = recommendations.filter { rec ->
            when (rec) {
                is ApsRecommendation.TempBasal -> rec.shouldDisplayForManualControl(pump.basalStatus)
                else -> true
            }
        }

        ManualControlUiState(
            contextInfo = contextInfo.copy(hasNextPlannedMealReminder = hasNextPlannedMealReminder),
            recommendations = filteredRecommendations,
            pump = pump,
            showLockError = lockErrorOwner != null,
            lockErrorOwner = lockErrorOwner
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ManualControlUiState()
    )

    fun dismissLockError() {
        lockErrorFlow.value = null
    }

    fun dismissRecommendation(recommendation: ApsRecommendation) {
        recommendationManager.removeRecommendation(recommendation)
    }

    private suspend fun acquireTreatmentLockAndExecute(
        tag: String = TAG,
        timeoutMs: Long = 5000L,
        block: suspend (TreatmentLock) -> Unit
    ): Boolean {
        val startTime = System.currentTimeMillis()
        var lastOwner: String? = null
        while (System.currentTimeMillis() - startTime < timeoutMs) {
            val lockResult = therapyManager.tryAcquire(tag) { lock ->
                block(lock)
            }
            when (lockResult) {
                is LockResult.Success -> return true
                is LockResult.Busy -> lastOwner = lockResult.owner
            }
            delay(100L.milliseconds)
        }
        lockErrorFlow.value = lastOwner ?: "System"
        return false
    }

    fun deliverBolus(
        amount: InsulinAmount,
        handledDeferredBoluses: List<DeferredBolus>? = null,
        correctionPart: InsulinAmount = InsulinAmount.ZERO,
        basalPart: InsulinAmount = InsulinAmount.ZERO,
        recommendationToDismiss: ApsRecommendation? = null
    ) {
        viewModelScope.launch {
            val acquired = acquireTreatmentLockAndExecute { treatmentLock ->
                therapyManager.issueBolus(
                    treatmentLock = treatmentLock,
                    amount = amount,
                    handledDeferredBoluses = handledDeferredBoluses,
                    correctionPart = correctionPart,
                    basalPart = basalPart
                )
            }
            if (acquired) {
                recommendationToDismiss?.let {
                    recommendationManager.removeRecommendation(it)
                }
            }
        }
    }

    fun cancelBolus() {
        viewModelScope.launch {
            acquireTreatmentLockAndExecute { treatmentLock ->
                therapyManager.cancelBolus(treatmentLock)
            }
        }
    }

    fun setTempBasal(
        durationHours: Int,
        percent: Int,
        recommendationToDismiss: ApsRecommendation? = null
    ) {
        viewModelScope.launch {
            val acquired = acquireTreatmentLockAndExecute { treatmentLock ->
                therapyManager.clearTempBasal(treatmentLock)
                therapyManager.setTempBasal(
                    treatmentLock = treatmentLock,
                    durationInHours = durationHours,
                    percent = percent
                )
            }
            if (acquired) {
                recommendationToDismiss?.let {
                    recommendationManager.removeRecommendation(it)
                }
            }
        }
    }

    fun cancelTempBasal() {
        viewModelScope.launch {
            acquireTreatmentLockAndExecute { treatmentLock ->
                therapyManager.clearTempBasal(treatmentLock)
            }
        }
    }

    companion object {
        private const val TAG = "ManualControlScreen"
        const val PAST_MEAL_LOOKBACK_HOURS = 12

        class Factory(private val registry: SystemRegistry) : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ManualControlViewModel(registry) as T
            }
        }
    }
}

private fun ApsRecommendation.TempBasal.shouldDisplayForManualControl(
    basalStatus: BasalStatus?
): Boolean {
    val currentPercent = if (basalStatus != null && basalStatus.isTempBasal) (basalStatus.tempBasalPercent ?: 100) else 100
    val proposedPercent = this.percent

    return proposedPercent != currentPercent
}