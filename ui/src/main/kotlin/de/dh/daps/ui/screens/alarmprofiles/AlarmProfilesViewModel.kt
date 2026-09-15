package de.dh.raaps.ui.screens.alarmprofiles

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import de.dh.raaps.common.model.data.AlarmProfile
import de.dh.raaps.core.SystemRegistry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AlarmProfilesUiState(
    val profiles: List<AlarmProfile> = emptyList(),
    val activeProfile: AlarmProfile? = null,
    val isLoading: Boolean = false,
    val showDeleteConfirmation: AlarmProfile? = null
)

class AlarmProfilesViewModel(
    systemRegistry: SystemRegistry
) : ViewModel() {

    private val _uiState = MutableStateFlow(AlarmProfilesUiState(isLoading = true))
    val uiState = _uiState.asStateFlow()

    private val alarmRepository = systemRegistry.alarmRepository

    init {
        observeData()
    }

    private fun observeData() {
        viewModelScope.launch {
            combine(
                alarmRepository.observeAllAlarmProfiles(),
                alarmRepository.observeActiveAlarmProfile()
            ) { profiles, active ->
                profiles to active
            }.collect { (profiles, active) ->
                _uiState.update {
                    it.copy(
                        profiles = profiles,
                        activeProfile = active,
                        isLoading = false
                    )
                }
            }
        }
    }

    fun setActiveProfile(profile: AlarmProfile) {
        viewModelScope.launch {
            alarmRepository.setActiveAlarmProfile(profile.id)
        }
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