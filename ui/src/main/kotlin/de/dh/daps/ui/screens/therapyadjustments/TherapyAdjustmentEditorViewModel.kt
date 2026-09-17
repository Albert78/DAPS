package de.dh.daps.ui.screens.therapyadjustments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import de.dh.daps.common.model.ADJUSTMENT_PERCENTAGE_MAX
import de.dh.daps.common.model.ADJUSTMENT_PERCENTAGE_MIN
import de.dh.daps.common.model.ID_UNDEFINED
import de.dh.daps.common.model.data.AlarmProfile
import de.dh.daps.common.model.data.GlucoseUnit
import de.dh.daps.common.model.data.TherapyAdjustment
import de.dh.daps.core.SystemRegistry
import de.dh.daps.glucoseUnit
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
    val alarmProfileOverrideId: Long? = null,
    val availableAlarmProfiles: List<AlarmProfile> = emptyList(),
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
    private val alarmRepository = registry.alarmRepository
    private val appPreferencesRepository = registry.appPreferencesRepository

    private val _uiState = MutableStateFlow(TherapyAdjustmentEditorUiState())
    val uiState: StateFlow<TherapyAdjustmentEditorUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val alarmProfiles = alarmRepository.getAllAlarmProfiles()
            val glucoseUnit = appPreferencesRepository.cachedPreferences.value.glucoseUnit
            _uiState.update {
                it.copy(
                    availableAlarmProfiles = alarmProfiles,
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
                        alarmProfileOverrideId = adjustment.alarmProfileOverrideId,
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

    fun onAlarmProfileOverrideChange(alarmProfileId: Long?) {
        _uiState.update { it.copy(alarmProfileOverrideId = alarmProfileId) }
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
                alarmProfileOverrideId = state.alarmProfileOverrideId
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