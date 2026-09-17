package de.dh.daps.ui.screens.therapyadjustments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import de.dh.daps.common.model.data.TherapyAdjustment
import de.dh.daps.core.SystemRegistry
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TherapyAdjustmentsUiState(
    val adjustments: List<TherapyAdjustment> = emptyList(),
    val isLoading: Boolean = false
)

class TherapyAdjustmentsViewModel(
    registry: SystemRegistry
) : ViewModel() {

    private val therapyManager = registry.therapyManager

    val uiState: StateFlow<TherapyAdjustmentsUiState> = therapyManager.observeAllTherapyAdjustments()
        .map { TherapyAdjustmentsUiState(adjustments = it, isLoading = false) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = TherapyAdjustmentsUiState(isLoading = true)
        )

    fun deleteAdjustment(adjustment: TherapyAdjustment) {
        viewModelScope.launch {
            therapyManager.deleteTherapyAdjustment(adjustment.id)
        }
    }

    companion object {
        class Factory(private val registry: SystemRegistry) : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return TherapyAdjustmentsViewModel(registry) as T
            }
        }
    }
}