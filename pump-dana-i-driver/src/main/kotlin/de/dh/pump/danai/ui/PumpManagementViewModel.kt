package de.dh.pump.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.dh.pump.danai.core.connection.DanaPumpScanner
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class PumpManagementViewModel(application: Application) : AndroidViewModel(application) {
    private val scanner = DanaPumpScanner(application, DanaIPumpManager.appLogger)

    val associatedPump = DanaIPumpManager.danaIPump
    val activeController = DanaIPumpManager.controller

    /**
     * Current list of pumps discovered during the most recent Bluetooth scan.
     */
    val scanResults: StateFlow<List<PumpDescriptor>> = scanner.scanResults
        .map { list ->
            list.map { scanResult ->
                PumpDescriptor(
                    name = scanResult.name,
                    address = scanResult.address,
                    device = scanResult.device
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    /**
     * Indicates whether a Bluetooth LE scan is currently active.
     */
    val isScanning: StateFlow<Boolean> = scanner.isScanning

    fun startDiscovery() {
        scanner.startScan(viewModelScope)
    }

    fun stopDiscovery() {
        scanner.stopScan()
    }

    fun connectAndHandshake(pump: PumpDescriptor) {
        // Use Application Context to prevent Context leaks
        DanaIPumpManager.createNewPumpLink(pump, getApplication())
    }

    fun reset() {
        DanaIPumpManager.forgetDevice()
    }

    override fun onCleared() {
        super.onCleared()
        // Ensure active BLE scan is stopped when ViewModel is destroyed
        scanner.stopScan()
    }
}