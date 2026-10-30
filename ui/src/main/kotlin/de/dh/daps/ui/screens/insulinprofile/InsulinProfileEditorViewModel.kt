package de.dh.daps.ui.screens.insulinprofile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import de.dh.daps.common.DEFAULT_BASAL_UNITS_PER_HOUR
import de.dh.daps.common.DEFAULT_CR_GRAM_PER_UNIT
import de.dh.daps.common.DEFAULT_ISF_MGDL_PER_UNIT
import de.dh.daps.common.ID_UNDEFINED
import de.dh.daps.common.domain.model.data.Block
import de.dh.daps.common.domain.model.data.Minutes
import de.dh.daps.common.domain.model.therapy.InsulinProfile
import de.dh.daps.common.domain.model.therapy.InsulinType
import de.dh.daps.common.domain.model.therapy.ProfileInsulinSetting
import de.dh.daps.core.SystemRegistry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class InsulinProfileEditorUiState(
    val profile: InsulinProfile? = null,
    val insulinTypes: List<InsulinType> = emptyList(),
    val profiles: List<InsulinProfile> = emptyList(),
    val isLoading: Boolean = false,
    val isSaving: Boolean = false
)

class InsulinProfileEditorViewModel(
    private val registry: SystemRegistry,
    private val profileId: Long?,
    private val isCopy: Boolean,
    private val newName: String?
) : ViewModel() {

    private val therapyRepository = registry.therapyRepository
    private val _uiState = MutableStateFlow(InsulinProfileEditorUiState())
    val uiState: StateFlow<InsulinProfileEditorUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val profiles = therapyRepository.getAllInsulinProfiles()
            val insulinTypes = therapyRepository.getAllInsulinTypes()

            val profile = if (profileId != null && profileId != ID_UNDEFINED) {
                val source = profiles.find { it.id == profileId }
                if (source != null) {
                    if (isCopy) {
                        source.copy(
                            id = ID_UNDEFINED,
                            name = newName ?: "${source.name} (Copy)"
                        )
                    } else {
                        source
                    }
                } else {
                    createNewProfile(insulinTypes)
                }
            } else {
                createNewProfile(insulinTypes)
            }

            _uiState.update {
                it.copy(
                    profile = profile,
                    insulinTypes = insulinTypes,
                    profiles = profiles,
                    isLoading = false
                )
            }
        }
    }

    private fun createNewProfile(insulinTypes: List<InsulinType>): InsulinProfile {
        val initialSettings = insulinTypes.map { type ->
            ProfileInsulinSetting(
                insulinType = type,
                dia = type.dia,
                peak = type.peak
            )
        }
        return InsulinProfile(
            name = "",
            basalBlocks = listOf(
                Block(
                    Minutes.ofHours(24),
                    DEFAULT_BASAL_UNITS_PER_HOUR
                )
            ),
            isfBlocks = listOf(Block(Minutes.ofHours(24), DEFAULT_ISF_MGDL_PER_UNIT)),
            crBlocks = listOf(Block(Minutes.ofHours(24), DEFAULT_CR_GRAM_PER_UNIT)),
            insulinSettings = initialSettings
        )
    }

    fun isNameUnique(name: String, excludeId: Long): Boolean {
        val trimmedName = name.trim()
        return _uiState.value.profiles.none {
            it.name.trim().equals(trimmedName, ignoreCase = true) && it.id != excludeId
        }
    }

    fun save(profile: InsulinProfile, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            if (profile.id == ID_UNDEFINED) {
                therapyRepository.insertInsulinProfile(profile)
            } else {
                therapyRepository.updateInsulinProfile(profile)
            }
            onSuccess()
        }
    }

    companion object {
        class Factory(
            private val registry: SystemRegistry,
            private val profileId: Long?,
            private val isCopy: Boolean,
            private val newName: String?
        ) : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                return InsulinProfileEditorViewModel(registry, profileId, isCopy, newName) as T
            }
        }
    }
}