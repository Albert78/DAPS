package de.dh.daps.ui.screens.openloop

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import de.dh.daps.common.model.BasalStatus
import de.dh.daps.common.model.BolusStatus
import de.dh.daps.common.model.DeferredBolus
import de.dh.daps.common.model.InsulinAmount
import de.dh.daps.common.model.MealEntry
import de.dh.daps.common.model.MealReminder
import de.dh.daps.common.model.PumpCapabilities
import de.dh.daps.common.model.data.Timestamp
import de.dh.daps.core.SystemRegistry
import de.dh.daps.core.aps.ApsRecommendation
import de.dh.daps.core.aps.TreatmentLock
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface PlannedMealItem {
    data class Entry(val meal: MealEntry) : PlannedMealItem
    data class Reminder(val reminder: MealReminder) : PlannedMealItem

    val timestamp: Timestamp
        get() = when (this) {
            is Entry -> meal.timestamp
            is Reminder -> reminder.mealTimestamp
        }
}

data class OpenLoopUiState(
    val recommendations: List<ApsRecommendation> = emptyList(),
    val lastPastMeal: MealEntry? = null,
    val nextPlannedMeal: PlannedMealItem? = null,
    val isPumpConnected: Boolean = false,
    val pumpModel: String? = null,
    val activeBasalStatus: BasalStatus? = null,
    val activeBolusStatus: BolusStatus? = null,
    val minBolusAmount: InsulinAmount = InsulinAmount(0.05),
    val maxBolusSize: InsulinAmount = InsulinAmount(25.0)
)

@OptIn(ExperimentalCoroutinesApi::class)
class OpenLoopViewModel(
    systemRegistry: SystemRegistry
) : ViewModel() {
    private val recommendationManager = systemRegistry.recommendationManager
    private val treatmentRepository = systemRegistry.treatmentRepository
    private val pumpManager = systemRegistry.pumpManager
    private val therapyManager = systemRegistry.therapyManager

    private val pumpData = pumpManager.activeInsulinPump.flatMapLatest { pump ->
        if (pump == null) {
            flowOf(PumpData())
        } else {
            combine(
                pump.isConnected,
                pump.hardwareInformation,
                pump.basalStatus,
                pump.bolusStatus,
                pump.pumpCapabilities
            ) { connected, hardware, basal, bolus, capabilities ->
                PumpData(
                    connected = connected,
                    model = hardware?.model,
                    basalStatus = basal,
                    bolusStatus = bolus,
                    capabilities = capabilities
                )
            }
        }
    }

    val uiState: StateFlow<OpenLoopUiState> = combine(
        recommendationManager.recommendations,
        recommendationManager.mealReminders,
        treatmentRepository.observeMeals(),
        pumpData
    ) { recommendations, mealReminders, meals, pData ->
        val now = Timestamp.now()

        // Last meal in the past (timestamp <= now)
        val pastMeal = meals.filter { it.timestamp <= now }.maxByOrNull { it.timestamp }

        // Next planned meal (timestamp > now or mealReminder.mealTimestamp > now)
        val futureEntries = meals.filter { it.timestamp > now }.map { PlannedMealItem.Entry(it) }
        val futureReminders = mealReminders.filter { it.mealTimestamp > now }.map { PlannedMealItem.Reminder(it) }
        val nextMeal = (futureEntries + futureReminders).minByOrNull { it.timestamp }

        OpenLoopUiState(
            recommendations = recommendations,
            lastPastMeal = pastMeal,
            nextPlannedMeal = nextMeal,
            isPumpConnected = pData.connected,
            pumpModel = pData.model,
            activeBasalStatus = pData.basalStatus,
            activeBolusStatus = pData.bolusStatus,
            minBolusAmount = pData.capabilities?.minBolusAmount ?: InsulinAmount(0.05),
            maxBolusSize = pData.capabilities?.maxBolusSize ?: InsulinAmount(25.0)
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = OpenLoopUiState()
    )

    fun deliverBolus(
        treatmentLock: TreatmentLock,
        amount: InsulinAmount,
        handledDeferredBoluses: List<DeferredBolus>? = null,
        correctionPart: InsulinAmount = InsulinAmount.ZERO,
        basalPart: InsulinAmount = InsulinAmount.ZERO,
        recommendationToDismiss: ApsRecommendation? = null
    ) {
        viewModelScope.launch {
            therapyManager.issueBolus(
                treatmentLock = treatmentLock,
                amount = amount,
                handledDeferredBoluses = handledDeferredBoluses,
                correctionPart = correctionPart,
                basalPart = basalPart
            )
            recommendationToDismiss?.let {
                recommendationManager.removeRecommendation(it)
            }
        }
    }

    fun cancelBolus(treatmentLock: TreatmentLock) {
        viewModelScope.launch {
            therapyManager.cancelBolus(treatmentLock)
        }
    }

    fun setTempBasal(
        treatmentLock: TreatmentLock,
        durationHours: Int,
        percent: Int,
        recommendationToDismiss: ApsRecommendation? = null
    ) {
        viewModelScope.launch {
            therapyManager.setTempBasal(
                treatmentLock = treatmentLock,
                durationInHours = durationHours,
                percent = percent
            )
            recommendationToDismiss?.let {
                recommendationManager.removeRecommendation(it)
            }
        }
    }

    fun cancelTempBasal(treatmentLock: TreatmentLock) {
        viewModelScope.launch {
            therapyManager.clearTempBasal(treatmentLock)
        }
    }

    private data class PumpData(
        val connected: Boolean = false,
        val model: String? = null,
        val basalStatus: BasalStatus? = null,
        val bolusStatus: BolusStatus? = null,
        val capabilities: PumpCapabilities? = null
    )

    companion object {
        class Factory(private val registry: SystemRegistry) : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return OpenLoopViewModel(registry) as T
            }
        }
    }
}