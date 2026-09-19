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
import de.dh.daps.common.model.data.BgReading
import de.dh.daps.common.model.data.Timestamp
import de.dh.daps.common.model.toActiveDoses
import de.dh.daps.core.SystemRegistry
import de.dh.daps.core.aps.ApsRecommendation
import de.dh.daps.core.aps.TreatmentLock
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Beinhaltet den Kontext der aktuellen Therapie- und Stoffwechsellage.
 */
data class OpenLoopContextInfoUiModel(
    val lastBgReading: BgReading? = null,
    val iob: InsulinAmount = InsulinAmount.ZERO,
    val cob: Double = 0.0,
    val lastPastMeal: MealEntry? = null,
    val nextPlannedMeal: MealEntry? = null
)

/**
 * Status und Fähigkeiten der verbundenen Insulinpumpe.
 */
data class OpenLoopPumpUiModel(
    val isConnected: Boolean = false,
    val model: String? = null,
    val basalStatus: BasalStatus? = null,
    val bolusStatus: BolusStatus? = null,
    val minBolusAmount: InsulinAmount = InsulinAmount(0.05),
    val maxBolusSize: InsulinAmount = InsulinAmount(25.0)
)

/**
 * Gesamtzustand des OpenLoop-Screens.
 */
data class OpenLoopUiState(
    val contextInfo: OpenLoopContextInfoUiModel = OpenLoopContextInfoUiModel(),
    val recommendations: List<ApsRecommendation> = emptyList(),
    val activeMealReminders: List<MealReminder> = emptyList(),
    val pump: OpenLoopPumpUiModel = OpenLoopPumpUiModel()
)

@OptIn(ExperimentalCoroutinesApi::class)
class OpenLoopViewModel(
    systemRegistry: SystemRegistry
) : ViewModel() {
    private val recommendationManager = systemRegistry.recommendationManager
    private val treatmentRepository = systemRegistry.treatmentRepository
    private val pumpManager = systemRegistry.pumpManager
    private val therapyManager = systemRegistry.therapyManager
    private val glucoseRepository = systemRegistry.glucoseRepository
    private val carbsInsulinCalculator = systemRegistry.carbsInsulinCalculator

    private val contextInfoFlow: Flow<OpenLoopContextInfoUiModel> = combine(
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
        val pastMeal = meals.filter { it.timestamp <= now }.maxByOrNull { it.timestamp }
        val nextMeal = meals.filter { it.timestamp > now }.minByOrNull { it.timestamp }

        OpenLoopContextInfoUiModel(
            lastBgReading = currentBg,
            iob = iob,
            cob = cob,
            lastPastMeal = pastMeal,
            nextPlannedMeal = nextMeal
        )
    }

    private val pumpFlow: Flow<OpenLoopPumpUiModel> = pumpManager.activeInsulinPump.flatMapLatest { pump ->
        if (pump == null) {
            flowOf(OpenLoopPumpUiModel())
        } else {
            combine(
                pump.isConnected,
                pump.hardwareInformation,
                pump.basalStatus,
                pump.bolusStatus,
                pump.pumpCapabilities
            ) { connected, hardware, basal, bolus, capabilities ->
                OpenLoopPumpUiModel(
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

    val uiState: StateFlow<OpenLoopUiState> = combine(
        contextInfoFlow,
        recommendationManager.recommendations,
        recommendationManager.mealReminders,
        pumpFlow
    ) { contextInfo, recommendations, mealReminders, pump ->
        OpenLoopUiState(
            contextInfo = contextInfo,
            recommendations = recommendations,
            activeMealReminders = mealReminders,
            pump = pump
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

    companion object {
        class Factory(private val registry: SystemRegistry) : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return OpenLoopViewModel(registry) as T
            }
        }
    }
}