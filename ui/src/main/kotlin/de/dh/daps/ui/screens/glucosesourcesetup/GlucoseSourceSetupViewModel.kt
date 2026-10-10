package de.dh.daps.ui.screens.glucosesourcesetup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
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
    val errorMessage: String? = null
)

class GlucoseSourceSetupViewModel(
    registry: SystemRegistry
) : ViewModel() {

    private val selectedDriverState = MutableStateFlow<GlucoseSourceDriver?>(null)
    private val errorMessageState = MutableStateFlow<String?>(null)

    init {
        viewModelScope.launch {
            var previousActiveDriver: GlucoseSourceDriver? = registry.glucoseSourceDriverManager.getActiveDriver()
            registry.glucoseSourceDriverManager.activeDriver.collect { activeDriver ->
                if (activeDriver == null && previousActiveDriver != null) {
                    selectedDriverState.value = null
                }
                previousActiveDriver = activeDriver
            }
        }
    }

    val uiState: StateFlow<GlucoseSourceSetupUiState> = combine(
        selectedDriverState,
        registry.glucoseSourceDriverManager.activeDriver,
        errorMessageState
    ) { selectedDriver, activeDriver, errorMessage ->
        val driverToShow = activeDriver ?: selectedDriver
        GlucoseSourceSetupUiState(
            availableDrivers = registry.glucoseSourceDriverManager.drivers,
            selectedDriver = driverToShow,
            activeDriverId = activeDriver?.driverId,
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