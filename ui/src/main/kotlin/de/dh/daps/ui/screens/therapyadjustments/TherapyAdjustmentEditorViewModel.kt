package de.dh.daps.ui.screens.therapyadjustments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import de.dh.daps.common.ADJUSTMENT_PERCENTAGE_MAX
import de.dh.daps.common.ADJUSTMENT_PERCENTAGE_MIN
import de.dh.daps.common.ID_UNDEFINED
import de.dh.daps.common.domain.model.alarm.AlertProfile
import de.dh.daps.common.domain.model.data.GlucoseUnit
import de.dh.daps.common.domain.model.therapy.TherapyAdjustment
import de.dh.daps.common.repository.glucoseUnit
import de.dh.daps.core.SystemRegistry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TherapyAdjustmentEditorUiState(
    val id: Long? = null,
    val name: String = "",
    val percentage: Int = 0,
    val targetBgMgDl: Short? = null,
    val lowThresholdMgDl: Short? = null,
    val alertProfileOverrideId: Long? = null,
    val availableAlertProfiles: List<AlertProfile> = emptyList(),
    val glucoseUnit: GlucoseUnit = GlucoseUnit.MG_DL,
    val isSaving: Boolean = false,
    val isLoading: Boolean = false
) {
    val isValid: Boolean
        get() {
            val nameValid = name.isNotBlank()
            val percentageValid = percentage in ADJUSTMENT_PERCENTAGE_MIN..ADJUSTMENT_PERCENTAGE_MAX
            val bgValid = if (targetBgMgDl != null && lowThresholdMgDl != null) {
                lowThresholdMgDl < targetBgMgDl
            } else true
            return nameValid && percentageValid && bgValid
        }
}

class TherapyAdjustmentEditorViewModel(
    registry: SystemRegistry,
    private val adjustmentId: Long?
) : ViewModel() {

    private val therapyManager = registry.therapyManager
    private val alertProfileRepository = registry.alertProfileRepository
    private val appPreferencesRepository = registry.appPreferencesRepository

    private val _uiState = MutableStateFlow(TherapyAdjustmentEditorUiState())
    val uiState: StateFlow<TherapyAdjustmentEditorUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val alertProfiles = alertProfileRepository.getAllAlertProfiles()
            val glucoseUnit = appPreferencesRepository.cachedPreferences.value.glucoseUnit
            _uiState.update {
                it.copy(
                    availableAlertProfiles = alertProfiles,
                    glucoseUnit = glucoseUnit
                )
            }
            if (adjustmentId != null && adjustmentId != ID_UNDEFINED) {
                loadAdjustment(adjustmentId)
            }
        }
    }

    private fun loadAdjustment(id: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val adjustment = therapyManager.getTherapyAdjustmentById(id)
            if (adjustment != null) {
                _uiState.update {
                    it.copy(
                        id = adjustment.id,
                        name = adjustment.name,
                        percentage = adjustment.percentage,
                        targetBgMgDl = adjustment.targetBgMgDl,
                        lowThresholdMgDl = adjustment.lowThresholdMgDl,
                        alertProfileOverrideId = adjustment.alertProfileOverrideId,
                        isLoading = false
                    )
                }
            } else {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun onNameChange(name: String) {
        _uiState.update { it.copy(name = name) }
    }

    fun onPercentageChange(percentage: Int) {
        _uiState.update { it.copy(percentage = percentage) }
    }

    fun onTargetBgChange(targetBgMgDl: Short?) {
        _uiState.update { it.copy(targetBgMgDl = targetBgMgDl) }
    }

    fun onLowThresholdChange(lowThresholdMgDl: Short?) {
        _uiState.update { it.copy(lowThresholdMgDl = lowThresholdMgDl) }
    }

    fun onAlertProfileOverrideChange(alertProfileId: Long?) {
        _uiState.update { it.copy(alertProfileOverrideId = alertProfileId) }
    }

    fun save(onSuccess: () -> Unit) {
        val state = _uiState.value
        if (!state.isValid) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val adjustment = TherapyAdjustment(
                id = state.id ?: ID_UNDEFINED,
                name = state.name.trim(),
                percentage = state.percentage,
                targetBgMgDl = state.targetBgMgDl,
                lowThresholdMgDl = state.lowThresholdMgDl,
                alertProfileOverrideId = state.alertProfileOverrideId
            )
            therapyManager.saveTherapyAdjustment(adjustment)
            onSuccess()
        }
    }

    companion object {
        class Factory(
            private val registry: SystemRegistry,
            private val adjustmentId: Long?
        ) : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                return TherapyAdjustmentEditorViewModel(registry, adjustmentId) as T
            }
        }
    }
}