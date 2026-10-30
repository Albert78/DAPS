package de.dh.daps.ui.screens.insulinprofile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import de.dh.daps.common.domain.model.therapy.InsulinProfile
import de.dh.daps.common.domain.model.therapy.InsulinType
import de.dh.daps.core.SystemRegistry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class InsulinProfilesUiState(
    val profiles: List<InsulinProfile> = emptyList(),
    val insulinTypes: List<InsulinType> = emptyList(),
    val activeProfileId: Long? = null,
    val isLoading: Boolean = false
) {
    fun isProfileActive(profile: InsulinProfile): Boolean {
        return profile.id == activeProfileId
    }

    fun canDeleteProfile(profile: InsulinProfile): Boolean {
        return profiles.size > 1 && !isProfileActive(profile)
    }
}

/**
 * ViewModel for managing therapy profiles (list and deletion).
 */
class InsulinProfilesViewModel(
    private val systemRegistry: SystemRegistry
) : ViewModel() {

    private val _uiState = MutableStateFlow(InsulinProfilesUiState(isLoading = true))
    val uiState = _uiState.asStateFlow()

    private val therapyRepository = systemRegistry.therapyRepository
    private val therapyManager = systemRegistry.therapyManager

    init {
        observeData()
    }

    private fun observeData() {
        viewModelScope.launch {
            combine(
                therapyRepository.observeAllInsulinProfiles(),
                therapyManager.currentTherapySettingsFlow
            ) { profiles, currentSettings ->
                val insulinTypes = therapyRepository.getAllInsulinTypes()
                InsulinProfilesUiState(
                    profiles = profiles,
                    insulinTypes = insulinTypes,
                    activeProfileId = currentSettings.insulinProfile.id,
                    isLoading = false
                )
            }.collect { newState ->
                _uiState.update { newState }
            }
        }
    }

    fun deleteInsulinProfile(profile: InsulinProfile) {
        viewModelScope.launch {
            if (uiState.value.canDeleteProfile(profile)) {
                therapyRepository.deleteInsulinProfile(profile)
            }
        }
    }

    companion object {
        class Factory(private val registry: SystemRegistry) : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                return InsulinProfilesViewModel(registry) as T
            }
        }
    }
}