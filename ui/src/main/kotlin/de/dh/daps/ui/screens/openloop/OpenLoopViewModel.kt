package de.dh.daps.ui.screens.openloop

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import de.dh.daps.common.model.BasalStatus
import de.dh.daps.common.model.BolusStatus
import de.dh.daps.common.model.InsulinAmount
import de.dh.daps.common.model.MealEntry
import de.dh.daps.common.model.MealReminder
import de.dh.daps.common.model.PumpCapabilities
import de.dh.daps.common.model.data.Timestamp
import de.dh.daps.core.SystemRegistry
import de.dh.daps.core.aps.ApsRecommendation
import de.dh.daps.core.pump.PumpCommand
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn

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
    val maxBolusSize: InsulinAmount = InsulinAmount(25.0),
    val manualBolusAmount: Double = 1.0,
    val manualTempBasalPercent: Int = 100,
    val manualTempBasalDurationHours: Int = 1
)

@OptIn(ExperimentalCoroutinesApi::class)
class OpenLoopViewModel(
    systemRegistry: SystemRegistry
) : ViewModel() {
    private val recommendationManager = systemRegistry.recommendationManager
    private val treatmentRepository = systemRegistry.treatmentRepository
    private val pumpManager = systemRegistry.pumpManager

    private val _manualBolusAmount = MutableStateFlow(1.0)
    private val _manualTempBasalPercent = MutableStateFlow(100)
    private val _manualTempBasalDurationHours = MutableStateFlow(1)

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
        pumpData,
        combine(
            _manualBolusAmount,
            _manualTempBasalPercent,
            _manualTempBasalDurationHours
        ) { bolus, percent, duration -> Triple(bolus, percent, duration) }
    ) { flows ->
        @Suppress("UNCHECKED_CAST")
        val recommendations = flows[0] as List<ApsRecommendation>
        @Suppress("UNCHECKED_CAST")
        val mealReminders = flows[1] as List<MealReminder>
        @Suppress("UNCHECKED_CAST")
        val meals = flows[2] as List<MealEntry>
        val pData = flows[3] as PumpData
        @Suppress("UNCHECKED_CAST")
        val (bolusAmount, tempPercent, tempDuration) = flows[4] as Triple<Double, Int, Int>

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
            maxBolusSize = pData.capabilities?.maxBolusSize ?: InsulinAmount(25.0),
            manualBolusAmount = bolusAmount,
            manualTempBasalPercent = tempPercent,
            manualTempBasalDurationHours = tempDuration
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = OpenLoopUiState()
    )

    fun updateManualBolusAmount(amount: Double) {
        _manualBolusAmount.value = amount
    }

    fun updateManualTempBasalPercent(percent: Int) {
        _manualTempBasalPercent.value = percent
    }

    fun updateManualTempBasalDuration(hours: Int) {
        _manualTempBasalDurationHours.value = hours
    }

    fun applyBolusRecommendation(recommendation: ApsRecommendation.Bolus) {
        _manualBolusAmount.value = recommendation.amount.iu
    }

    fun applyTempBasalRecommendation(recommendation: ApsRecommendation.TempBasal) {
        _manualTempBasalPercent.value = recommendation.percent
        _manualTempBasalDurationHours.value = recommendation.durationInHours
    }

    fun removeRecommendation(recommendation: ApsRecommendation) {
        recommendationManager.removeRecommendation(recommendation)
    }

    fun clearAllRecommendations() {
        recommendationManager.clearRecommendations()
    }

    fun deliverBolus() {
        val amount = _manualBolusAmount.value
        if (amount > 0.0) {
            pumpManager.issueCommand(PumpCommand.DeliverBolus(InsulinAmount(amount)))
        }
    }

    fun cancelBolus() {
        pumpManager.issueCommand(PumpCommand.CancelBolus)
    }

    fun setTempBasal() {
        val percent = _manualTempBasalPercent.value
        val hours = _manualTempBasalDurationHours.value
        pumpManager.issueCommand(PumpCommand.SetTempBasal(percent = percent, durationHours = hours))
    }

    fun cancelTempBasal() {
        pumpManager.issueCommand(PumpCommand.CancelTempBasal)
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