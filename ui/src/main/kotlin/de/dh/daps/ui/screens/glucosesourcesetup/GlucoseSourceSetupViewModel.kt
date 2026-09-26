package de.dh.daps.ui.screens.glucosesourcesetup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import de.dh.daps.common.model.GlucoseSourceConnectionDescriptor
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
    val activeSourceDescriptor: GlucoseSourceConnectionDescriptor? = null,
    val isConnecting: Boolean = false,
    val errorMessage: String? = null
)

class GlucoseSourceSetupViewModel(
    private val registry: SystemRegistry,
    initialDriverId: String? = null
) : ViewModel() {

    private val selectedDriverState = MutableStateFlow<GlucoseSourceDriver?>(
        initialDriverId?.let { registry.glucoseSourceDriverManager.getDriver(it) }
    )
    private val isConnectingState = MutableStateFlow(false)
    private val errorMessageState = MutableStateFlow<String?>(null)

    val uiState: StateFlow<GlucoseSourceSetupUiState> = combine(
        selectedDriverState,
        registry.deviceManagementRepository.glucoseSourceDescriptor,
        isConnectingState,
        errorMessageState
    ) { selectedDriver, activeDescriptor, isConnecting, errorMessage ->
        GlucoseSourceSetupUiState(
            availableDrivers = registry.glucoseSourceDriverManager.drivers,
            selectedDriver = selectedDriver,
            activeSourceDescriptor = activeDescriptor,
            isConnecting = isConnecting,
            errorMessage = errorMessage
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = GlucoseSourceSetupUiState(
            availableDrivers = registry.glucoseSourceDriverManager.drivers,
            selectedDriver = initialDriverId?.let { registry.glucoseSourceDriverManager.getDriver(it) }
        )
    )

    fun selectDriver(driver: GlucoseSourceDriver?) {
        selectedDriverState.value = driver
        errorMessageState.value = null
    }

    fun connectGlucoseSource(
        descriptor: GlucoseSourceConnectionDescriptor,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            isConnectingState.value = true
            errorMessageState.value = null

            registry.deviceConnectionManager.connectGlucoseSource(descriptor)
                .onSuccess {
                    isConnectingState.value = false
                    onSuccess()
                }
                .onFailure { error ->
                    isConnectingState.value = false
                    errorMessageState.value = error.localizedMessage ?: "Verbindung zur Glukose-Quelle fehlgeschlagen."
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
                return GlucoseSourceSetupViewModel(registry, initialDriverId) as T
            }
        }
    }
}