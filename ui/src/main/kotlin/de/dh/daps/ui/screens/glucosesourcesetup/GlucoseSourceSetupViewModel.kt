package de.dh.daps.ui.screens.glucosesourcesetup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import de.dh.daps.common.model.GlucoseSource
import de.dh.daps.common.model.GlucoseSourceDriver
import de.dh.daps.core.SystemRegistry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class GlucoseSourceSetupUiState(
    val availableDrivers: List<GlucoseSourceDriver> = emptyList(),
    val selectedDriver: GlucoseSourceDriver? = null,
    val activeDriverId: String? = null,
    val isConnecting: Boolean = false,
    val errorMessage: String? = null
)

class GlucoseSourceSetupViewModel(
    private val registry: SystemRegistry
) : ViewModel() {

    private val selectedDriverState = MutableStateFlow<GlucoseSourceDriver?>(
        registry.glucoseSourceDriverManager.getActiveDriver()
    )
    private val isConnectingState = MutableStateFlow(false)
    private val errorMessageState = MutableStateFlow<String?>(null)

    val uiState: StateFlow<GlucoseSourceSetupUiState> = combine(
        selectedDriverState,
        registry.deviceManagementRepository.activeGlucoseSourceDriverId,
        isConnectingState,
        errorMessageState
    ) { selectedDriver, activeDriverId, isConnecting, errorMessage ->
        GlucoseSourceSetupUiState(
            availableDrivers = registry.glucoseSourceDriverManager.drivers,
            selectedDriver = selectedDriver,
            activeDriverId = activeDriverId,
            isConnecting = isConnecting,
            errorMessage = errorMessage
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = GlucoseSourceSetupUiState(
            availableDrivers = registry.glucoseSourceDriverManager.drivers,
            selectedDriver = registry.glucoseSourceDriverManager.getActiveDriver()
        )
    )

    fun selectDriver(driver: GlucoseSourceDriver?) {
        selectedDriverState.value = driver
        errorMessageState.value = null
    }

    fun onGlucoseSourceConnected(
        driverId: String,
        glucoseSource: GlucoseSource,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            isConnectingState.value = true
            errorMessageState.value = null

            registry.deviceConnectionManager.setConnectedGlucoseSource(driverId, glucoseSource)
            isConnectingState.value = false
            onSuccess()
        }
    }

    fun clearError() {
        errorMessageState.value = null
    }

    companion object {
        class Factory(
            private val registry: SystemRegistry
        ) : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                return GlucoseSourceSetupViewModel(registry) as T
            }
        }
    }
}