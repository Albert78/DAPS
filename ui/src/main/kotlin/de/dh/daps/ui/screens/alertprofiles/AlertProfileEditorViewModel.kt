package de.dh.daps.ui.screens.alertprofiles

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import de.dh.daps.common.ID_UNDEFINED
import de.dh.daps.common.domain.model.alarm.AlarmSeverity
import de.dh.daps.common.domain.model.alarm.AlarmType
import de.dh.daps.common.domain.model.alarm.AlertDisplayMode
import de.dh.daps.common.domain.model.alarm.AlertProfile
import de.dh.daps.common.domain.model.alarm.AlertSignalConfig
import de.dh.daps.common.domain.model.alarm.RecommendationType
import de.dh.daps.common.domain.model.alarm.SoundConfig
import de.dh.daps.common.domain.model.alarm.VibrationMode
import de.dh.daps.common.domain.model.reminder.ReminderType
import de.dh.daps.core.SystemRegistry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AlertProfileEditorUiState(
    val profileId: Long? = null,
    val name: String = "",
    val isDefault: Boolean = false,

    // 1. Alarms
    val alarmDefaults: Map<AlarmSeverity, AlertSignalConfig> = mapOf(
        AlarmSeverity.CRITICAL to AlertSignalConfig(
            displayMode = AlertDisplayMode.FullScreen(SoundConfig(volume = 100)),
            vibrationMode = VibrationMode.CONTINUOUS,
            overrideDnd = true
        ),
        AlarmSeverity.WARNING to AlertSignalConfig(
            displayMode = AlertDisplayMode.FullScreen(SoundConfig(volume = 80)),
            vibrationMode = VibrationMode.LONG,
            overrideDnd = false
        ),
        AlarmSeverity.INFO to AlertSignalConfig(
            displayMode = AlertDisplayMode.NotificationOnly,
            vibrationMode = VibrationMode.SHORT,
            overrideDnd = false
        )
    ),
    val alarmOverrides: Map<AlarmType, AlertSignalConfig> = emptyMap(),

    // 2. APS Recommendations
    val recommendationDefault: AlertSignalConfig = AlertSignalConfig(
        enabled = true,
        displayMode = AlertDisplayMode.NotificationOnly,
        vibrationMode = VibrationMode.SHORT,
        overrideDnd = false
    ),
    val recommendationOverrides: Map<RecommendationType, AlertSignalConfig> = emptyMap(),

    // 3. Reminders
    val reminderDefault: AlertSignalConfig = AlertSignalConfig(
        enabled = true,
        displayMode = AlertDisplayMode.NotificationOnly,
        vibrationMode = VibrationMode.SHORT,
        overrideDnd = false
    ),
    val reminderOverrides: Map<ReminderType, AlertSignalConfig> = emptyMap(),

    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val existingNames: List<String> = emptyList()
) {
    val severityDefaults: Map<AlarmSeverity, AlertSignalConfig>
        get() = alarmDefaults

    val isValid: Boolean
        get() {
            val trimmedName = name.trim()
            if (trimmedName.isEmpty()) return false
            return existingNames.none { it.equals(trimmedName, ignoreCase = true) }
        }
}

class AlertProfileEditorViewModel(
    registry: SystemRegistry,
    private val profileId: Long?
) : ViewModel() {

    private val alertProfileRepository = registry.alertProfileRepository
    private val alarmPlayerManager = registry.alarmPlayerManager
    private val _uiState = MutableStateFlow(AlertProfileEditorUiState(profileId = profileId))
    val uiState: StateFlow<AlertProfileEditorUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val allProfiles = alertProfileRepository.getAllAlertProfiles()
            val otherNames = allProfiles
                .filter { it.id != profileId }
                .map { it.name.trim() }

            val targetProfile = if (profileId != null && profileId != ID_UNDEFINED) {
                alertProfileRepository.getAlertProfileById(profileId)
            } else null

            if (targetProfile != null) {
                _uiState.update {
                    it.copy(
                        name = targetProfile.name,
                        isDefault = targetProfile.isDefault,
                        alarmDefaults = targetProfile.alarmDefaults.ifEmpty { it.alarmDefaults },
                        alarmOverrides = targetProfile.alarmOverrides,
                        recommendationDefault = targetProfile.recommendationDefault,
                        recommendationOverrides = targetProfile.recommendationOverrides,
                        reminderDefault = targetProfile.reminderDefault,
                        reminderOverrides = targetProfile.reminderOverrides,
                        existingNames = otherNames,
                        isLoading = false
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        existingNames = otherNames,
                        isLoading = false
                    )
                }
            }
        }
    }

    fun onNameChange(name: String) {
        _uiState.update { it.copy(name = name) }
    }

    fun onIsDefaultChange(isDefault: Boolean) {
        _uiState.update { it.copy(isDefault = isDefault) }
    }

    // 1. Alarms Section Operations
    fun onAlarmDefaultChange(severity: AlarmSeverity, config: AlertSignalConfig) {
        _uiState.update { state ->
            val updatedMap = state.alarmDefaults.toMutableMap().apply {
                put(severity, config)
            }
            state.copy(alarmDefaults = updatedMap)
        }
    }

    fun onSeverityConfigChange(severity: AlarmSeverity, config: AlertSignalConfig) {
        onAlarmDefaultChange(severity, config)
    }

    fun onAlarmOverrideAdd(type: AlarmType, config: AlertSignalConfig = AlertSignalConfig()) {
        _uiState.update { state ->
            val updatedMap = state.alarmOverrides.toMutableMap().apply {
                put(type, config)
            }
            state.copy(alarmOverrides = updatedMap)
        }
    }

    fun onAlarmOverrideChange(type: AlarmType, config: AlertSignalConfig) {
        onAlarmOverrideAdd(type, config)
    }

    fun onAlarmOverrideRemove(type: AlarmType) {
        _uiState.update { state ->
            val updatedMap = state.alarmOverrides.toMutableMap().apply {
                remove(type)
            }
            state.copy(alarmOverrides = updatedMap)
        }
    }

    // 2. Recommendations Section Operations
    fun onRecommendationDefaultChange(config: AlertSignalConfig) {
        _uiState.update { it.copy(recommendationDefault = config) }
    }

    fun onRecommendationOverrideAdd(type: RecommendationType, config: AlertSignalConfig = AlertSignalConfig()) {
        _uiState.update { state ->
            val updatedMap = state.recommendationOverrides.toMutableMap().apply {
                put(type, config)
            }
            state.copy(recommendationOverrides = updatedMap)
        }
    }

    fun onRecommendationOverrideChange(type: RecommendationType, config: AlertSignalConfig) {
        onRecommendationOverrideAdd(type, config)
    }

    fun onRecommendationOverrideRemove(type: RecommendationType) {
        _uiState.update { state ->
            val updatedMap = state.recommendationOverrides.toMutableMap().apply {
                remove(type)
            }
            state.copy(recommendationOverrides = updatedMap)
        }
    }

    // 3. Reminders Section Operations
    fun onReminderDefaultChange(config: AlertSignalConfig) {
        _uiState.update { it.copy(reminderDefault = config) }
    }

    fun onReminderOverrideAdd(type: ReminderType, config: AlertSignalConfig = AlertSignalConfig()) {
        _uiState.update { state ->
            val updatedMap = state.reminderOverrides.toMutableMap().apply {
                put(type, config)
            }
            state.copy(reminderOverrides = updatedMap)
        }
    }

    fun onReminderOverrideChange(type: ReminderType, config: AlertSignalConfig) {
        onReminderOverrideAdd(type, config)
    }

    fun onReminderOverrideRemove(type: ReminderType) {
        _uiState.update { state ->
            val updatedMap = state.reminderOverrides.toMutableMap().apply {
                remove(type)
            }
            state.copy(reminderOverrides = updatedMap)
        }
    }

    fun save(onSuccess: () -> Unit) {
        val state = _uiState.value
        if (!state.isValid) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val profile = AlertProfile(
                id = state.profileId ?: ID_UNDEFINED,
                name = state.name.trim(),
                isDefault = state.isDefault,
                alarmDefaults = state.alarmDefaults,
                alarmOverrides = state.alarmOverrides,
                recommendationDefault = state.recommendationDefault,
                recommendationOverrides = state.recommendationOverrides,
                reminderDefault = state.reminderDefault,
                reminderOverrides = state.reminderOverrides
            )

            val savedId = if (profile.id == ID_UNDEFINED) {
                alertProfileRepository.insertAlertProfile(profile)
            } else {
                alertProfileRepository.updateAlertProfile(profile)
                profile.id
            }

            if (state.isDefault) {
                alertProfileRepository.setDefaultAlertProfile(savedId)
            }

            onSuccess()
        }
    }

    fun playPreviewSound(config: AlertSignalConfig) {
        alarmPlayerManager.playPreview(config, durationMs = 3000L)
    }

    fun updatePreviewVolume(volume: Int) {
        alarmPlayerManager.updateVolume(volume)
    }

    fun stopPreviewSound() {
        alarmPlayerManager.stopAlarm()
    }

    override fun onCleared() {
        alarmPlayerManager.stopAlarm()
    }

    companion object {
        class Factory(private val registry: SystemRegistry, private val profileId: Long?) : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                return AlertProfileEditorViewModel(registry, profileId) as T
            }
        }
    }
}