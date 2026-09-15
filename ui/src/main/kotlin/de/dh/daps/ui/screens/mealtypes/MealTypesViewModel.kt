package de.dh.daps.ui.screens.mealtypes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import de.dh.daps.common.model.MealType
import de.dh.daps.core.SystemRegistry
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class MealTypesUiState(
    val mealTypes: List<MealType> = emptyList(),
    val isLoading: Boolean = false
)

class MealTypesViewModel(
    registry: SystemRegistry
) : ViewModel() {

    private val treatmentRepository = registry.treatmentRepository

    val uiState: StateFlow<MealTypesUiState> = treatmentRepository.observeMealTypes()
        .map { MealTypesUiState(mealTypes = it, isLoading = false) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = MealTypesUiState(isLoading = true)
        )

    fun deleteMealType(mealType: MealType) {
        if (mealType.isStandardMealType()) return
        viewModelScope.launch {
            treatmentRepository.deleteMealType(mealType)
        }
    }

    companion object {
        class Factory(private val registry: SystemRegistry) : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return MealTypesViewModel(registry) as T
            }
        }
    }
}