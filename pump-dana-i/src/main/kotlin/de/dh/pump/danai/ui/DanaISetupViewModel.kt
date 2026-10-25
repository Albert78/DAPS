package de.dh.pump.danai.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import de.dh.pump.dana.commands.options.DanaRsUserOptions
import de.dh.pump.danai.DanaIInsulinPumpDriver
import de.dh.pump.danai.R
import de.dh.pump.danai.core.DanaIPumpAdapter
import de.dh.pump.danai.core.connection.DanaPumpScanner
import de.dh.pump.danai.core.connection.DanaScanError
import de.dh.pump.danai.core.connection.DanaScanResult
import de.dh.pump.danai.core.connection.PairingFailedException
import de.dh.pump.danai.core.connection.StaleBondException
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

    private val _isStaleBondError = MutableStateFlow(false)
    val isStaleBondError: StateFlow<Boolean> = _isStaleBondError.asStateFlow()

    private val _userOptions = MutableStateFlow<DanaRsUserOptions?>(null)
    val userOptions: StateFlow<DanaRsUserOptions?> = _userOptions.asStateFlow()

    private val _isLoadingOptions = MutableStateFlow(false)
    val isLoadingOptions: StateFlow<Boolean> = _isLoadingOptions.asStateFlow()

    private val _isSavingOptions = MutableStateFlow(false)
    val isSavingOptions: StateFlow<Boolean> = _isSavingOptions.asStateFlow()

    private val _optionsError = MutableStateFlow<String?>(null)
    val optionsError: StateFlow<String?> = _optionsError.asStateFlow()


    fun startDiscovery() {
        scanner.startScan(viewModelScope)
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
            _isStaleBondError.value = false
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
                if (e is StaleBondException) {
                    _isStaleBondError.value = true
                    _pairError.value = getApplication<Application>().getString(R.string.danai_setup_stale_bond_error)
                } else if (e is PairingFailedException) {
                    _isStaleBondError.value = true
                    _pairError.value = getApplication<Application>().getString(R.string.danai_setup_possible_stale_bond_error)
                } else {
                    _isStaleBondError.value = false
                    _pairError.value = e.message ?: getApplication<Application>().getString(R.string.danai_setup_connect_failed)
                }
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

    fun loadUserOptions(driver: DanaIInsulinPumpDriver) {
        viewModelScope.launch {
            _isLoadingOptions.value = true
            _optionsError.value = null
            try {
                val pumpAdapter = driver.activePump.value as? DanaIPumpAdapter
                val pump = pumpAdapter?.pump
                if (pump != null) {
                    val options = pump.getUserOptions()
                    _userOptions.value = options
                } else {
                    _optionsError.value = getApplication<Application>().getString(R.string.danai_unbind_dialog_pump_not_connected)
                }
            } catch (e: Exception) {
                _optionsError.value = e.message ?: getApplication<Application>().getString(R.string.danai_unbind_dialog_load_failed)
            } finally {
                _isLoadingOptions.value = false
            }
        }
    }

    fun saveAndUnbind(
        driver: DanaIInsulinPumpDriver,
        optionsToSave: DanaRsUserOptions?,
        onSuccess: (() -> Unit)? = null
    ) {
        viewModelScope.launch {
            _isSavingOptions.value = true
            _optionsError.value = null
            try {
                if (optionsToSave != null) {
                    val pumpAdapter = driver.activePump.value as? DanaIPumpAdapter
                    val pump = pumpAdapter?.pump
                    pump?.setUserOptions(optionsToSave)
                }
                driver.unbindDevice()
                onSuccess?.invoke()
            } catch (e: Exception) {
                _optionsError.value = e.message ?: getApplication<Application>().getString(R.string.danai_unbind_dialog_failed)
            } finally {
                _isSavingOptions.value = false
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