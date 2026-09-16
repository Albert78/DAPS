package de.dh.daps.ui.screens.alarmprofiles

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import de.dh.daps.common.model.data.AlarmProfile
import de.dh.daps.core.SystemRegistry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AlarmProfilesUiState(
    val profiles: List<AlarmProfile> = emptyList(),
    val defaultProfile: AlarmProfile? = null,
    val alarmProfileOverride: AlarmProfile? = null,
    val isLoading: Boolean = false,
    val showDeleteConfirmation: AlarmProfile? = null
) {
    val activeProfile: AlarmProfile?
        get() = alarmProfileOverride ?: defaultProfile
}

class AlarmProfilesViewModel(
    systemRegistry: SystemRegistry
) : ViewModel() {

    private val _uiState = MutableStateFlow(AlarmProfilesUiState(isLoading = true))
    val uiState = _uiState.asStateFlow()

    private val alarmRepository = systemRegistry.alarmRepository
    private val therapyManager = systemRegistry.therapyManager

    init {
        observeData()
    }

    private fun observeData() {
        viewModelScope.launch {
            combine(
                alarmRepository.observeAllAlarmProfiles(),
                alarmRepository.observeDefaultAlarmProfile(),
                therapyManager.currentTherapySettingsFlow
            ) { profiles, defaultProf, currentSettings ->
                Triple(profiles, defaultProf, currentSettings.alarmProfileOverride)
            }.collect { (profiles, defaultProf, overrideProf) ->
                _uiState.update {
                    it.copy(
                        profiles = profiles,
                        defaultProfile = defaultProf,
                        alarmProfileOverride = overrideProf,
                        isLoading = false
                    )
                }
            }
        }
    }

    fun setDefaultProfile(profile: AlarmProfile) {
        viewModelScope.launch {
            alarmRepository.setDefaultAlarmProfile(profile.id)
        }
    }

    fun setActiveProfile(profile: AlarmProfile) {
        setDefaultProfile(profile)
    }

    fun confirmDelete(profile: AlarmProfile) {
        _uiState.update { it.copy(showDeleteConfirmation = profile) }
    }

    fun cancelDelete() {
        _uiState.update { it.copy(showDeleteConfirmation = null) }
    }

    fun deleteProfile(profile: AlarmProfile) {
        viewModelScope.launch {
            alarmRepository.deleteAlarmProfile(profile.id)
            cancelDelete()
        }
    }

    companion object {
        class Factory(private val registry: SystemRegistry) : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                return AlarmProfilesViewModel(registry) as T
            }
        }
    }
}