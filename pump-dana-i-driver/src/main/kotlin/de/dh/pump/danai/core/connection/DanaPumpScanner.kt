package de.dh.pump.danai.core.connection

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import de.dh.pump.danai.core.DanaILogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Discovered Dana pump during Bluetooth scanning.
 */
data class DanaScanResult(
    val name: String,
    val address: String,
    val device: BluetoothDevice,
)

/**
 * Handles Bluetooth Low Energy (BLE) scanning specifically for Dana insulin pumps.
 *
 * Encapsulates Android system Bluetooth scanning operations away from UI components
 * and ViewModels to provide a clean separation of concerns and thread-safe state flows.
 */
class DanaPumpScanner(
    context: Context,
    private val logger: DanaILogger? = null,
) {
    private val appContext = context.applicationContext
    private val bluetoothManager = appContext.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager

    private val _scanResults = MutableStateFlow<List<DanaScanResult>>(emptyList())
    val scanResults: StateFlow<List<DanaScanResult>> = _scanResults.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private var scanTimeoutJob: Job? = null

    /**
     * Starts scanning for Dana pumps for [duration].
     *
     * Automatically stops scanning when [duration] elapses or when [stopScan] is called.
     */
    @SuppressLint("MissingPermission")
    fun startScan(scope: CoroutineScope, duration: Duration = 15.seconds) {
        val adapter = bluetoothManager?.adapter
        if (adapter == null || !adapter.isEnabled) {
            logger?.log("BLE Scan cancelled: Bluetooth is disabled or not supported.")
            return
        }

        val scanner = adapter.bluetoothLeScanner
        if (scanner == null) {
            logger?.log("BLE Scan cancelled: BluetoothLeScanner is unavailable.")
            return
        }

        // Cancel any active scan / timeout job
        stopScan()

        _scanResults.value = emptyList()
        _isScanning.value = true

        try {
            scanner.startScan(scanCallback)
            logger?.log("BLE Scan started for Dana pumps.")
        } catch (e: Exception) {
            _isScanning.value = false
            logger?.log("Failed to start BLE scan: ${e.message}")
            return
        }

        scanTimeoutJob = scope.launch {
            delay(duration)
            stopScan()
        }
    }

    /**
     * Stops the active BLE scan.
     */
    @SuppressLint("MissingPermission")
    fun stopScan() {
        scanTimeoutJob?.cancel()
        scanTimeoutJob = null

        if (_isScanning.value) {
            _isScanning.value = false
            try {
                bluetoothManager?.adapter?.bluetoothLeScanner?.stopScan(scanCallback)
                logger?.log("BLE Scan stopped.")
            } catch (e: Exception) {
                logger?.log("Error stopping BLE scan: ${e.message}")
            }
        }
    }

    private val scanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device ?: return
            val name = device.name.orEmpty()

            // Dana pumps always have a 10-character serial number as their name
            if (name.length != 10) return

            val item = DanaScanResult(
                name = name,
                address = device.address,
                device = device,
            )

            _scanResults.update { current ->
                val index = current.indexOfFirst { it.address == item.address }
                if (index >= 0) {
                    current.toMutableList().apply { set(index, item) }
                } else {
                    current + item
                }
            }
        }

        override fun onScanFailed(errorCode: Int) {
            logger?.log("BLE Scan failed with error code: $errorCode")
            _isScanning.value = false
        }
    }
}