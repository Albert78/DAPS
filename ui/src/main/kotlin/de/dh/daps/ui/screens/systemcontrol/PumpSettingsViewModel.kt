package de.dh.daps.ui.screens.systemcontrol

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import de.dh.daps.common.model.InsulinPump
import de.dh.daps.common.model.InsulinPumpDriver
import de.dh.daps.core.SystemRegistry
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

sealed interface PumpSettingsUiState {
    data object Loading : PumpSettingsUiState
    data object NoSettingsAvailable : PumpSettingsUiState
    data class Content(
        val pump: InsulinPump,
        val driver: InsulinPumpDriver
    ) : PumpSettingsUiState
}

class PumpSettingsViewModel(
    systemRegistry: SystemRegistry
) : ViewModel() {
    private val pumpManager = systemRegistry.pumpManager
    private val pumpDriverManager = systemRegistry.pumpDriverManager

    val uiState: StateFlow<PumpSettingsUiState> = combine(
        pumpManager.activeInsulinPump,
        pumpDriverManager.activeDriver
    ) { pump, driver ->
        if (pump != null && driver != null) {
            PumpSettingsUiState.Content(pump, driver)
        } else {
            PumpSettingsUiState.NoSettingsAvailable
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PumpSettingsUiState.Loading
    )

    companion object {
        class Factory(private val registry: SystemRegistry) : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                return PumpSettingsViewModel(registry) as T
            }
        }
    }
}