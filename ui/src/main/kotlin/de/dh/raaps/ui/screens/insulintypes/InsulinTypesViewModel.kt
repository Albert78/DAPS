package de.dh.raaps.ui.screens.insulintypes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import de.dh.raaps.common.model.InsulinType
import de.dh.raaps.core.SystemRegistry
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class InsulinTypesUiState(
    val insulinTypes: List<InsulinType> = emptyList(),
    val isLoading: Boolean = false
)

class InsulinTypesViewModel(
    registry: SystemRegistry
) : ViewModel() {

    private val treatmentRepository = registry.treatmentRepository

    val uiState: StateFlow<InsulinTypesUiState> = treatmentRepository.observeInsulinTypes()
        .map { InsulinTypesUiState(insulinTypes = it, isLoading = false) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = InsulinTypesUiState(isLoading = true)
        )

    fun deleteInsulinType(insulinType: InsulinType) {
        viewModelScope.launch {
            treatmentRepository.deleteInsulinType(insulinType)
        }
    }

    companion object {
        class Factory(private val registry: SystemRegistry) : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return InsulinTypesViewModel(registry) as T
            }
        }
    }
}