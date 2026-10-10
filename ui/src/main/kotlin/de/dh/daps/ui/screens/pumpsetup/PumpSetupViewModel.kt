package de.dh.daps.ui.screens.pumpsetup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import de.dh.daps.common.model.InsulinPump
import de.dh.daps.common.model.InsulinPumpDriver
import de.dh.daps.core.SystemRegistry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PumpSetupUiState(
    val availableDrivers: List<InsulinPumpDriver> = emptyList(),
    val selectedDriver: InsulinPumpDriver? = null,
    val activeDriverId: String? = null,
    val isConnecting: Boolean = false,
    val errorMessage: String? = null
)

class PumpSetupViewModel(
    private val registry: SystemRegistry
) : ViewModel() {

    private val selectedDriverState = MutableStateFlow<InsulinPumpDriver?>(
        registry.pumpDriverManager.getActiveDriver()
    )
    private val isConnectingState = MutableStateFlow(false)
    private val errorMessageState = MutableStateFlow<String?>(null)

    val uiState: StateFlow<PumpSetupUiState> = combine(
        selectedDriverState,
        registry.deviceManagementRepository.activePumpDriverId,
        isConnectingState,
        errorMessageState
    ) { selectedDriver, activeDriverId, isConnecting, errorMessage ->
        PumpSetupUiState(
            availableDrivers = registry.pumpDriverManager.drivers,
            selectedDriver = selectedDriver,
            activeDriverId = activeDriverId,
            isConnecting = isConnecting,
            errorMessage = errorMessage
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PumpSetupUiState(
            availableDrivers = registry.pumpDriverManager.drivers,
            selectedDriver = registry.pumpDriverManager.getActiveDriver()
        )
    )

    fun selectDriver(driver: InsulinPumpDriver?) {
        selectedDriverState.value = driver
        errorMessageState.value = null
    }

    fun onPumpConnected(
        driverId: String,
        insulinPump: InsulinPump,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            isConnectingState.value = true
            errorMessageState.value = null

            registry.deviceConnectionManager.setConnectedPump(driverId, insulinPump)
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
                return PumpSetupViewModel(registry) as T
            }
        }
    }
}