package de.dh.daps.ui.screens.systemcontrol

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import de.dh.daps.core.SystemRegistry
import de.dh.daps.core.aps.CoreInsight
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class CoreDecisionsUiState(
    val coreInsights: List<CoreInsight> = emptyList()
)

class CoreDecisionsViewModel(
    systemRegistry: SystemRegistry
) : ViewModel() {
    private val systemMetricsRepository = systemRegistry.systemMetricsRepository

    val uiState: StateFlow<CoreDecisionsUiState> = systemMetricsRepository.observeInsights()
        .map { insights -> CoreDecisionsUiState(coreInsights = insights) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = CoreDecisionsUiState()
        )

    companion object {
        class Factory(private val registry: SystemRegistry) : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                return CoreDecisionsViewModel(registry) as T
            }
        }
    }
}