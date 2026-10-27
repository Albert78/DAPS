package de.dh.daps.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import de.dh.daps.common.domain.model.alarm.ActiveAlarmGroup
import de.dh.daps.common.domain.model.alarm.AlarmSignalConfig
import de.dh.daps.common.domain.model.alarm.AlarmType
import de.dh.daps.common.domain.model.cgm.GlucoseSource
import de.dh.daps.common.domain.model.engine.ApsMode
import de.dh.daps.common.domain.pump.InsulinPump
import de.dh.daps.core.SystemRegistry
import de.dh.daps.core.alerts.AlarmSnoozeState
import de.dh.daps.core.orchestration.ActiveRecommendations
import de.dh.daps.core.orchestration.ApsRecommendation
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
    val availableApsModes: List<ApsMode> = ApsMode.entries,
    val recommendations: List<ApsRecommendation> = emptyList(),
    val isMealCorrectionBolusAllowed: Boolean = false,
    val activeAlarmGroup: ActiveAlarmGroup? = null,
    val activeFiringConfig: AlarmSignalConfig? = null,
    val snoozedAlarms: Map<AlarmType, AlarmSnoozeState> = emptyMap(),
    val isGlucoseSourceConfigured: Boolean = true,
    val isPumpConfigured: Boolean = true
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
        systemOrchestrator.availableApsModes,
        systemRegistry.recommendationManager.recommendations,
        alarmEvaluator.activeAlarmGroup,
        alarmEvaluator.activeFiringConfig,
        alarmSnoozeManager.snoozedAlarms,
        systemRegistry.glucoseSourceManager.activeGlucoseSource,
        systemRegistry.pumpManager.activeInsulinPump
    ) { flows ->
        val state = flows[0] as DashboardUiState
        val mode = flows[1] as ApsMode
        @Suppress("UNCHECKED_CAST")
        val availableModes = flows[2] as List<ApsMode>
        val recommendations = flows[3] as ActiveRecommendations
        val activeAlarmGroup = flows[4] as ActiveAlarmGroup?
        val config = flows[5] as AlarmSignalConfig?
        @Suppress("UNCHECKED_CAST")
        val snoozedMap = flows[6] as Map<AlarmType, AlarmSnoozeState>
        val activeGlucoseSource = flows[7] as GlucoseSource?
        val activePump = flows[8] as InsulinPump?

        state.copy(
            apsMode = mode,
            availableApsModes = availableModes,
            recommendations = recommendations.all,
            isMealCorrectionBolusAllowed = systemOrchestrator.canOpenMealCorrectionBolus(),
            activeAlarmGroup = activeAlarmGroup,
            activeFiringConfig = config,
            snoozedAlarms = snoozedMap,
            isGlucoseSourceConfigured = activeGlucoseSource != null,
            isPumpConfigured = activePump != null
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardUiState())

    init {
        reload()
    }

    fun snoozeAllAlarms(minutes: Int) {
        val activeGroup = uiState.value.activeAlarmGroup
        if (activeGroup != null) {
            alarmSnoozeManager.snoozeAlarms(activeGroup.allAlarms, minutes)
        }
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