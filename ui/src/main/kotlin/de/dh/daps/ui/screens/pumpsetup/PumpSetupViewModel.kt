package de.dh.daps.ui.screens.pumpsetup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import de.dh.daps.common.model.InsulinPumpDriver
import de.dh.daps.common.model.PumpConnectionDescriptor
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
    val activePumpDescriptor: PumpConnectionDescriptor? = null,
    val isConnecting: Boolean = false,
    val errorMessage: String? = null
)

class PumpSetupViewModel(
    private val registry: SystemRegistry,
    private val initialDriverId: String? = null
) : ViewModel() {

    private val selectedDriverState = MutableStateFlow<InsulinPumpDriver?>(
        initialDriverId?.let { registry.pumpDriverManager.getDriver(it) }
    )
    private val isConnectingState = MutableStateFlow(false)
    private val errorMessageState = MutableStateFlow<String?>(null)

    val uiState: StateFlow<PumpSetupUiState> = combine(
        selectedDriverState,
        registry.deviceManagementRepository.pumpDescriptor,
        isConnectingState,
        errorMessageState
    ) { selectedDriver, activeDescriptor, isConnecting, errorMessage ->
        PumpSetupUiState(
            availableDrivers = registry.pumpDriverManager.drivers,
            selectedDriver = selectedDriver,
            activePumpDescriptor = activeDescriptor,
            isConnecting = isConnecting,
            errorMessage = errorMessage
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PumpSetupUiState(
            availableDrivers = registry.pumpDriverManager.drivers,
            selectedDriver = initialDriverId?.let { registry.pumpDriverManager.getDriver(it) }
        )
    )

    fun selectDriver(driver: InsulinPumpDriver?) {
        selectedDriverState.value = driver
        errorMessageState.value = null
    }

    fun connectPump(
        descriptor: PumpConnectionDescriptor,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            isConnectingState.value = true
            errorMessageState.value = null

            registry.deviceConnectionManager.connectPump(descriptor)
                .onSuccess {
                    isConnectingState.value = false
                    onSuccess()
                }
                .onFailure { error ->
                    isConnectingState.value = false
                    errorMessageState.value = error.localizedMessage ?: "Verbindung zur Pumpe fehlgeschlagen."
                }
        }
    }

    fun clearError() {
        errorMessageState.value = null
    }

    companion object {
        class Factory(
            private val registry: SystemRegistry,
            private val initialDriverId: String? = null
        ) : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                return PumpSetupViewModel(registry, initialDriverId) as T
            }
        }
    }
}