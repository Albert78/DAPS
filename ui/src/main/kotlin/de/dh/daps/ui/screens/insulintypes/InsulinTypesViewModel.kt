package de.dh.daps.ui.screens.insulintypes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import de.dh.daps.common.model.InsulinType
import de.dh.daps.core.SystemRegistry
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

data class InsulinTypesUiState(
    val insulinTypes: List<InsulinType> = emptyList(),
    val usedInsulinTypeIds: Set<String> = emptySet(),
    val isLoading: Boolean = false
) {
    fun isTypeInUse(type: InsulinType): Boolean {
        return type.id in usedInsulinTypeIds
    }

    fun canDeleteType(type: InsulinType): Boolean {
        return insulinTypes.size > 1 && !isTypeInUse(type)
    }
}

class InsulinTypesViewModel(
    registry: SystemRegistry
) : ViewModel() {

    private val treatmentRepository = registry.treatmentRepository

    val uiState: StateFlow<InsulinTypesUiState> = combine(
        treatmentRepository.observeInsulinTypes(),
        treatmentRepository.observeUsedInsulinTypeIds()
    ) { types, usedIds ->
        InsulinTypesUiState(
            insulinTypes = types,
            usedInsulinTypeIds = usedIds,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = InsulinTypesUiState(isLoading = true)
    )

    fun addInsulinType(insulinType: InsulinType) {
        viewModelScope.launch {
            val existingTypes = uiState.value.insulinTypes
            val toAdd = if (existingTypes.any { it.id == insulinType.id }) {
                insulinType.copy(id = UUID.randomUUID().toString())
            } else {
                insulinType
            }
            treatmentRepository.insertInsulinType(toAdd)
        }
    }

    fun deleteInsulinType(insulinType: InsulinType) {
        viewModelScope.launch {
            try {
                treatmentRepository.deleteInsulinType(insulinType)
            } catch (_: Exception) {
                // Ignore to prevent app crash if deletion fails due to constraint
            }
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