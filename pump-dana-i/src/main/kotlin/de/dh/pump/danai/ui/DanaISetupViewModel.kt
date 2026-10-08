package de.dh.pump.danai.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.dh.daps.common.model.InsulinPump
import de.dh.daps.common.model.PluginPreferences
import de.dh.daps.common.model.PumpConnectionDescriptor
import de.dh.pump.danai.core.connection.DanaPumpScanner
import de.dh.pump.danai.core.connection.DanaScanResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

import de.dh.pump.danai.R

/**
 * ViewModel managing BLE scanning and pairing workflow for the Dana-i setup screen.
 */
class DanaISetupViewModel(application: Application) : AndroidViewModel(application) {
    private val scanner = DanaPumpScanner(application)

    val scanResults: StateFlow<List<DanaScanResult>> = scanner.scanResults
    val isScanning: StateFlow<Boolean> = scanner.isScanning

    private val _isConnecting = MutableStateFlow(false)
    val isConnecting: StateFlow<Boolean> = _isConnecting.asStateFlow()

    private val _connectError = MutableStateFlow<String?>(null)
    val connectError: StateFlow<String?> = _connectError.asStateFlow()

    fun startDiscovery() {
        scanner.startScan(viewModelScope)
    }

    fun stopDiscovery() {
        scanner.stopScan()
    }

    fun connectAndPair(
        driver: DanaIInsulinPumpDriver,
        scanResult: DanaScanResult,
        preferences: PluginPreferences,
        onConnected: (InsulinPump, PumpConnectionDescriptor) -> Unit
    ) {
        viewModelScope.launch {
            _isConnecting.value = true
            _connectError.value = null
            scanner.stopScan()

            try {
                val (pump, descriptor) = driver.createAndConnectPump(
                    deviceName = scanResult.name,
                    deviceAddress = scanResult.address,
                    context = getApplication(),
                    preferences = preferences
                )
                _isConnecting.value = false
                onConnected(pump, descriptor)
            } catch (e: Exception) {
                _isConnecting.value = false
                _connectError.value = e.message ?: getApplication<Application>().getString(R.string.danai_pump_setup_connect_failed)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        scanner.stopScan()
    }
}