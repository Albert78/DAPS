package de.dh.daps.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import de.dh.daps.common.model.ApsMode
import de.dh.daps.common.model.data.AlarmSoundConfig
import de.dh.daps.common.model.data.AlarmType
import de.dh.daps.core.SystemRegistry
import de.dh.daps.core.alarms.AlarmSnoozeState
import de.dh.daps.core.aps.ApsRecommendation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DashboardUiState(
    val isLoading: Boolean = false,
    val isError: Boolean = false,
    val apsMode: ApsMode = ApsMode.Suspend,
    // TODO: Get selectable modes from core
    val availableApsModes: List<ApsMode> = ApsMode.entries,
    val recommendations: List<ApsRecommendation> = emptyList(),
    val isMealCorrectionBolusAllowed: Boolean = false,
    val activeFiringAlarm: AlarmType? = null,
    val activeFiringConfig: AlarmSoundConfig? = null,
    val snoozedAlarms: Map<AlarmType, AlarmSnoozeState> = emptyMap()
)

/**
 * ViewModel for the main dashboard screen.
 */
class DashboardViewModel(
    private val systemRegistry: SystemRegistry
) : ViewModel() {
    private val systemOrchestrator = systemRegistry.systemOrchestrator
    private val alarmEvaluator = systemRegistry.alarmEvaluator
    private val alarmSnoozeManager = systemRegistry.alarmSnoozeManager
    private val _uiState = MutableStateFlow(DashboardUiState())

    val uiState: StateFlow<DashboardUiState> = combine(
        _uiState,
        systemOrchestrator.apsMode,
        systemRegistry.therapyManager.recommendations,
        alarmEvaluator.activeFiringAlarm,
        alarmEvaluator.activeFiringConfig,
        alarmSnoozeManager.snoozedAlarms
    ) { flows ->
        val state = flows[0] as DashboardUiState
        val mode = flows[1] as ApsMode
        @Suppress("UNCHECKED_CAST")
        val recommendations = flows[2] as List<ApsRecommendation>
        val activeFiring = flows[3] as AlarmType?
        val config = flows[4] as AlarmSoundConfig?
        @Suppress("UNCHECKED_CAST")
        val snoozedMap = flows[5] as Map<AlarmType, AlarmSnoozeState>

        state.copy(
            apsMode = mode,
            recommendations = recommendations,
            isMealCorrectionBolusAllowed = systemOrchestrator.canOpenMealCorrectionBolus(),
            activeFiringAlarm = activeFiring,
            activeFiringConfig = config,
            snoozedAlarms = snoozedMap
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardUiState())

    init {
        reload()
    }

    fun snoozeAlarm(alarmType: AlarmType, minutes: Int) {
        alarmSnoozeManager.snoozeAlarm(alarmType, minutes)
        systemRegistry.alarmPlayerManager.stopAlarm()
    }

    fun cancelSnooze(alarmType: AlarmType) {
        alarmSnoozeManager.clearSnooze(alarmType)
    }

    fun reload() {
        viewModelScope.launch {
            reload_suspend()
        }
    }

    private suspend fun reload_suspend() {
        _uiState.update { it.copy(isLoading = true) }
        try {
            updateUiModel()
            _uiState.update { it.copy(isLoading = false, isError = false) }
        } catch (e: Exception) {
            _uiState.update { it.copy(isLoading = false, isError = true) }
        }
    }

    private fun updateUiModel() {
        // Logic to update the dashboard based on repository data
    }

    fun setApsMode(mode: ApsMode) {
        systemOrchestrator.setApsMode(mode)
    }

    companion object {
        class Factory(private val registry: SystemRegistry) : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                return DashboardViewModel(registry) as T
            }
        }
    }
}