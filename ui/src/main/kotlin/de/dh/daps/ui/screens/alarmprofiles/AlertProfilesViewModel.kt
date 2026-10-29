package de.dh.daps.ui.screens.alarmprofiles

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import de.dh.daps.common.domain.model.alarm.AlertProfile
import de.dh.daps.core.SystemRegistry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AlertProfilesUiState(
    val profiles: List<AlertProfile> = emptyList(),
    val defaultProfile: AlertProfile? = null,
    val alertProfileOverride: AlertProfile? = null,
    val isLoading: Boolean = false,
    val showDeleteConfirmation: AlertProfile? = null
) {
    val activeProfile: AlertProfile?
        get() = alertProfileOverride ?: defaultProfile
}

class AlertProfilesViewModel(
    systemRegistry: SystemRegistry
) : ViewModel() {

    private val _uiState = MutableStateFlow(AlertProfilesUiState(isLoading = true))
    val uiState = _uiState.asStateFlow()

    private val alertProfileRepository = systemRegistry.alertProfileRepository
    private val therapyManager = systemRegistry.therapyManager

    init {
        observeData()
    }

    private fun observeData() {
        viewModelScope.launch {
            combine(
                alertProfileRepository.observeAllAlertProfiles(),
                alertProfileRepository.observeDefaultAlertProfile(),
                therapyManager.currentTherapySettingsFlow
            ) { profiles, defaultProf, currentSettings ->
                Triple(profiles, defaultProf, currentSettings.alertProfileOverride)
            }.collect { (profiles, defaultProf, overrideProf) ->
                _uiState.update {
                    it.copy(
                        profiles = profiles,
                        defaultProfile = defaultProf,
                        alertProfileOverride = overrideProf,
                        isLoading = false
                    )
                }
            }
        }
    }

    fun confirmDelete(profile: AlertProfile) {
        _uiState.update { it.copy(showDeleteConfirmation = profile) }
    }

    fun cancelDelete() {
        _uiState.update { it.copy(showDeleteConfirmation = null) }
    }

    fun deleteProfile(profile: AlertProfile) {
        viewModelScope.launch {
            alertProfileRepository.deleteAlertProfile(profile.id)
            cancelDelete()
        }
    }

    companion object {
        class Factory(private val registry: SystemRegistry) : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                return AlertProfilesViewModel(registry) as T
            }
        }
    }
}