package de.dh.daps.ui.screens.systemcontrol

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import de.dh.daps.common.device.driver.GlucoseSourceDriver
import de.dh.daps.common.domain.model.cgm.GlucoseSource
import de.dh.daps.core.SystemRegistry
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

sealed interface GlucoseSourceSettingsUiState {
    data object Loading : GlucoseSourceSettingsUiState
    data object NoSettingsAvailable : GlucoseSourceSettingsUiState
    data class Content(
        val source: GlucoseSource,
        val driver: GlucoseSourceDriver
    ) : GlucoseSourceSettingsUiState
}

class GlucoseSourceSettingsViewModel(
    systemRegistry: SystemRegistry
) : ViewModel() {
    private val glucoseSourceManager = systemRegistry.glucoseSourceManager
    private val glucoseSourceDriverManager = systemRegistry.glucoseSourceDriverManager

    val uiState: StateFlow<GlucoseSourceSettingsUiState> = combine(
        glucoseSourceManager.activeGlucoseSource,
        glucoseSourceDriverManager.activeDriver
    ) { source, driver ->
        if (source != null && driver != null) {
            GlucoseSourceSettingsUiState.Content(source, driver)
        } else {
            GlucoseSourceSettingsUiState.NoSettingsAvailable
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = GlucoseSourceSettingsUiState.Loading
    )

    companion object {
        class Factory(private val registry: SystemRegistry) : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                return GlucoseSourceSettingsViewModel(registry) as T
            }
        }
    }
}