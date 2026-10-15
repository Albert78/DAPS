package de.dh.pump.danai.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import de.dh.pump.danai.DanaIInsulinPumpDriver
import de.dh.pump.danai.R
import de.dh.pump.danai.core.connection.DanaPumpScanner
import de.dh.pump.danai.core.connection.DanaScanError
import de.dh.pump.danai.core.connection.DanaScanResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel managing BLE scanning and pairing workflow for the Dana-i setup screen.
 */
class DanaISetupViewModel(application: Application) : AndroidViewModel(application) {
    private val scanner = DanaPumpScanner(application)

    val scanResults: StateFlow<List<DanaScanResult>> = scanner.scanResults
    val isScanning: StateFlow<Boolean> = scanner.isScanning
    val scanError: StateFlow<DanaScanError?> = scanner.scanError

    private val _pairingAddress = MutableStateFlow<String?>(null)
    val pairingAddress: StateFlow<String?> = _pairingAddress.asStateFlow()

    private val _pairError = MutableStateFlow<String?>(null)
    val pairError: StateFlow<String?> = _pairError.asStateFlow()

    fun startDiscovery() {
        scanner.startScan(viewModelScope, verifyCharacteristics = true)
    }

    fun stopDiscovery() {
        scanner.stopScan()
    }

    fun pairAndBind(
        driver: DanaIInsulinPumpDriver,
        scanResult: DanaScanResult,
        onSuccess: (() -> Unit)? = null
    ) {
        viewModelScope.launch {
            _pairingAddress.value = scanResult.address
            _pairError.value = null
            scanner.stopScan()

            try {
                driver.bindPumpDevice(
                    deviceName = scanResult.name,
                    deviceAddress = scanResult.address,
                )
                _pairingAddress.value = null
                onSuccess?.invoke()
            } catch (e: Exception) {
                _pairingAddress.value = null
                _pairError.value = e.message ?: getApplication<Application>().getString(R.string.danai_pump_setup_connect_failed)
            }
        }
    }

    fun unbind(
        driver: DanaIInsulinPumpDriver,
        onSuccess: (() -> Unit)? = null
    ) {
        viewModelScope.launch {
            try {
                driver.unbindDevice()
                onSuccess?.invoke()
            } catch (e: Exception) {
                _pairError.value = e.message
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        scanner.stopScan()
        scanner.unregisterBluetoothReceiver()
    }

    companion object {
        class Factory(private val application: Application) : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return DanaISetupViewModel(application) as T
            }
        }
    }
}