package de.dh.daps.ui.screens.pumpsetup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
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
    val errorMessage: String? = null
)

class PumpSetupViewModel(
    registry: SystemRegistry
) : ViewModel() {

    private val selectedDriverState = MutableStateFlow<InsulinPumpDriver?>(null)
    private val errorMessageState = MutableStateFlow<String?>(null)

    init {
        selectedDriverState.value = registry.pumpDriverManager.getActiveDriver()

        viewModelScope.launch {
            var previousActiveDriver: InsulinPumpDriver? = registry.pumpDriverManager.getActiveDriver()
            registry.pumpDriverManager.activeDriver.collect { activeDriver ->
                if (activeDriver == null && previousActiveDriver != null) {
                    selectedDriverState.value = null
                }
                previousActiveDriver = activeDriver
            }
        }
    }

    val uiState: StateFlow<PumpSetupUiState> = combine(
        selectedDriverState,
        registry.pumpDriverManager.activeDriver,
        errorMessageState
    ) { selectedDriver, activeDriver, errorMessage ->
        PumpSetupUiState(
            availableDrivers = registry.pumpDriverManager.drivers,
            selectedDriver = selectedDriver,
            activeDriverId = activeDriver?.driverId,
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