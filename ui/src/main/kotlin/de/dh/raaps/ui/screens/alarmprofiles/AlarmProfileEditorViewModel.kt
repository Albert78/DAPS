package de.dh.raaps.ui.screens.alarmprofiles

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import de.dh.raaps.common.model.ID_UNDEFINED
import de.dh.raaps.common.model.data.AlarmProfile
import de.dh.raaps.common.model.data.AlarmSeverity
import de.dh.raaps.common.model.data.AlarmSoundConfig
import de.dh.raaps.common.model.data.VibrationMode
import de.dh.raaps.core.SystemRegistry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AlarmProfileEditorUiState(
    val profileId: Long? = null,
    val name: String = "",
    val severityDefaults: Map<AlarmSeverity, AlarmSoundConfig> = mapOf(
        AlarmSeverity.CRITICAL to AlarmSoundConfig(volume = 100, vibrationMode = VibrationMode.CONTINUOUS, overrideDnd = true),
        AlarmSeverity.WARNING to AlarmSoundConfig(volume = 70, vibrationMode = VibrationMode.LONG, overrideDnd = false),
        AlarmSeverity.INFO to AlarmSoundConfig(volume = 40, vibrationMode = VibrationMode.SHORT, overrideDnd = false)
    ),
    val isDefault: Boolean = false,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val existingNames: List<String> = emptyList()
) {
    val isValid: Boolean
        get() {
            val trimmedName = name.trim()
            if (trimmedName.isEmpty()) return false
            val isUnique = existingNames.none { it.equals(trimmedName, ignoreCase = true) }
            return isUnique
        }
}

class AlarmProfileEditorViewModel(
    registry: SystemRegistry,
    private val profileId: Long?
) : ViewModel() {

    private val alarmRepository = registry.alarmRepository
    private val alarmPlayerManager = registry.alarmPlayerManager
    private val _uiState = MutableStateFlow(AlarmProfileEditorUiState(profileId = profileId))
    val uiState: StateFlow<AlarmProfileEditorUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val allProfiles = alarmRepository.getAllAlarmProfiles()
            val otherNames = allProfiles
                .filter { it.id != profileId }
                .map { it.name.trim() }

            val targetProfile = if (profileId != null && profileId != ID_UNDEFINED) {
                alarmRepository.getAlarmProfileById(profileId)
            } else null

            if (targetProfile != null) {
                _uiState.update {
                    it.copy(
                        name = targetProfile.name,
                        severityDefaults = targetProfile.severityDefaults,
                        isDefault = targetProfile.isDefault,
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

    fun onSeverityConfigChange(severity: AlarmSeverity, config: AlarmSoundConfig) {
        _uiState.update { state ->
            val updatedMap = state.severityDefaults.toMutableMap().apply {
                put(severity, config)
            }
            state.copy(severityDefaults = updatedMap)
        }
    }

    fun save(onSuccess: () -> Unit) {
        val state = _uiState.value
        if (!state.isValid) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val profile = AlarmProfile(
                id = state.profileId ?: ID_UNDEFINED,
                name = state.name.trim(),
                isDefault = state.isDefault,
                severityDefaults = state.severityDefaults
            )

            if (profile.id == ID_UNDEFINED) {
                alarmRepository.insertAlarmProfile(profile)
            } else {
                alarmRepository.updateAlarmProfile(profile)
            }
            onSuccess()
        }
    }

    fun playPreviewSound(config: AlarmSoundConfig) {
        alarmPlayerManager.playPreview(config, durationMs = 3000L)
    }

    override fun onCleared() {
        alarmPlayerManager.stopAlarm()
    }

    companion object {
        class Factory(private val registry: SystemRegistry, private val profileId: Long?) : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                return AlarmProfileEditorViewModel(registry, profileId) as T
            }
        }
    }
}