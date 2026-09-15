package de.dh.daps.ui.screens.insulintypes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import de.dh.daps.common.model.InsulinConcentration
import de.dh.daps.common.model.InsulinType
import de.dh.daps.common.model.data.Minutes
import de.dh.daps.core.SystemRegistry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class InsulinTypeEditorUiState(
    val id: String? = null,
    val name: String = "",
    val peak: String = "50",
    val dia: String = "300",
    val concentration: InsulinConcentration = InsulinConcentration.U100,
    val isSaving: Boolean = false,
    val isLoading: Boolean = false
) {
    val isValid: Boolean
        get() {
            val nameValid = name.isNotBlank()
            val peakInt = peak.toIntOrNull() ?: 0
            val diaInt = dia.toIntOrNull() ?: 0
            val timesValid = peakInt > 0 && diaInt > peakInt
            return nameValid && timesValid
        }
}

class InsulinTypeEditorViewModel(
    registry: SystemRegistry,
    private val insulinTypeId: String?
) : ViewModel() {

    private val treatmentRepository = registry.treatmentRepository
    private val _uiState = MutableStateFlow(InsulinTypeEditorUiState())
    val uiState: StateFlow<InsulinTypeEditorUiState> = _uiState.asStateFlow()

    init {
        if (insulinTypeId != null) {
            loadInsulinType(insulinTypeId)
        }
    }

    private fun loadInsulinType(id: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val type = treatmentRepository.getAllInsulinTypes().find { it.id == id }
            if (type != null) {
                _uiState.update {
                    it.copy(
                        id = type.id,
                        name = type.name,
                        peak = type.peak.value.toString(),
                        dia = type.dia.value.toString(),
                        concentration = type.defaultConcentration,
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

    fun onPeakChange(peak: String) {
        _uiState.update { it.copy(peak = peak) }
    }

    fun onDiaChange(dia: String) {
        _uiState.update { it.copy(dia = dia) }
    }

    fun onConcentrationChange(concentration: InsulinConcentration) {
        _uiState.update { it.copy(concentration = concentration) }
    }

    fun save(onSuccess: () -> Unit) {
        val state = _uiState.value
        if (!state.isValid) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val insulinType = InsulinType(
                id = state.id ?: UUID.randomUUID().toString(),
                name = state.name.trim(),
                peak = Minutes((state.peak.toIntOrNull() ?: 50).toShort()),
                dia = Minutes((state.dia.toIntOrNull() ?: 300).toShort()),
                defaultConcentration = state.concentration
            )
            treatmentRepository.insertInsulinType(insulinType)
            onSuccess()
        }
    }

    companion object {
        class Factory(private val registry: SystemRegistry, private val insulinTypeId: String?) : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                return InsulinTypeEditorViewModel(registry, insulinTypeId) as T
            }
        }
    }
}