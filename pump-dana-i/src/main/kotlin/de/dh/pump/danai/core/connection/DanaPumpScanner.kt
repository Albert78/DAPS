package de.dh.pump.danai.core.connection

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import de.dh.pump.danai.core.DanaILogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Collections
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * Errors that can occur during BLE scanning.
 */
enum class DanaScanError {
    BLUETOOTH_DISABLED,
    SCANNER_UNAVAILABLE,
}

/**
 * Discovered Dana pump during Bluetooth scanning or UI previews.
 */
sealed interface DanaScanResult {
    val name: String
    val address: String

    /**
     * Real Bluetooth Low Energy scan result containing a non-nullable [BluetoothDevice].
     */
    data class Real(
        override val name: String,
        override val address: String,
        val device: BluetoothDevice,
    ) : DanaScanResult

    /**
     * Fake scan result used for previews or tests without requiring an Android [BluetoothDevice].
     */
    data class Fake(
        override val name: String,
        override val address: String,
    ) : DanaScanResult
}

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

    private val _scanError = MutableStateFlow<DanaScanError?>(null)
    val scanError: StateFlow<DanaScanError?> = _scanError.asStateFlow()

    private var parentScope: CoroutineScope? = null
    private var activeScanJob: Job? = null
    private var activeScanScope: CoroutineScope? = null
    private var isReceiverRegistered = false

    private val seenAddresses = Collections.synchronizedSet(mutableSetOf<String>())

    private val bluetoothReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == BluetoothAdapter.ACTION_STATE_CHANGED) {
                val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                if (state == BluetoothAdapter.STATE_ON) {
                    if (_scanError.value == DanaScanError.BLUETOOTH_DISABLED) {
                        parentScope?.launch {
                            // Give Android BLE stack time to fully initialize the scanner after STATE_ON
                            var attempts = 0
                            while (attempts < 5) {
                                val adapter = bluetoothManager?.adapter
                                if (adapter != null && adapter.isEnabled && adapter.bluetoothLeScanner != null) {
                                    break
                                }
                                delay(200.milliseconds)
                                attempts++
                            }
                            if (_scanError.value == DanaScanError.BLUETOOTH_DISABLED) {
                                parentScope?.let { startScan(it) }
                            }
                        }
                    }
                } else if (state == BluetoothAdapter.STATE_OFF || state == BluetoothAdapter.STATE_TURNING_OFF) {
                    stopScan()
                    _scanError.value = DanaScanError.BLUETOOTH_DISABLED
                }
            }
        }
    }

    private fun registerBluetoothReceiver() {
        if (!isReceiverRegistered) {
            val filter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
            appContext.registerReceiver(bluetoothReceiver, filter)
            isReceiverRegistered = true
        }
    }

    /**
     * Unregisters the Bluetooth state BroadcastReceiver if registered.
     */
    fun unregisterBluetoothReceiver() {
        if (isReceiverRegistered) {
            try {
                appContext.unregisterReceiver(bluetoothReceiver)
            } catch (_: IllegalArgumentException) {
            }
            isReceiverRegistered = false
        }
    }

    /**
     * Starts scanning for Dana pumps for [duration].
     *
     * Automatically stops scanning when [duration] elapses or when [stopScan] is called.
     */
    @SuppressLint("MissingPermission")
    fun startScan(
        scope: CoroutineScope,
        duration: Duration = 60.seconds,
    ) {
        parentScope = scope

        // Stop any active scan and cancel all background jobs
        stopScan()

        seenAddresses.clear()
        _scanError.value = null

        val adapter = bluetoothManager?.adapter
        if (adapter == null || !adapter.isEnabled) {
            _scanError.value = DanaScanError.BLUETOOTH_DISABLED
            registerBluetoothReceiver()
            logger?.log("BLE Scan cancelled: Bluetooth is disabled or not supported.")
            return
        }

        val scanner = adapter.bluetoothLeScanner
        if (scanner == null) {
            _scanError.value = DanaScanError.SCANNER_UNAVAILABLE
            logger?.log("BLE Scan cancelled: BluetoothLeScanner is unavailable.")
            return
        }

        val parentJob = scope.coroutineContext[Job]
        val scanJob = SupervisorJob(parentJob)
        val scanScope = CoroutineScope(scope.coroutineContext + scanJob)

        activeScanJob = scanJob
        activeScanScope = scanScope

        registerBluetoothReceiver()

        _scanResults.value = emptyList()
        _isScanning.value = true

        try {
            scanner.startScan(scanCallback)
            logger?.log("BLE Scan started for Dana pumps.")
        } catch (e: Exception) {
            _isScanning.value = false
            _scanError.value = DanaScanError.SCANNER_UNAVAILABLE
            logger?.log("Failed to start BLE scan: ${e.message}")
            scanJob.cancel()
            activeScanJob = null
            activeScanScope = null
            return
        }

        scanScope.launch {
            delay(duration)
            stopScan()
        }
    }

    /**
     * Stops the active BLE scan and cancels all associated background jobs.
     */
    @SuppressLint("MissingPermission")
    fun stopScan() {
        activeScanJob?.cancel()
        activeScanJob = null
        activeScanScope = null

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
            val name = (device.name ?: result.scanRecord?.deviceName).orEmpty().trim()

            if (!isValidDanaPumpDevice(name)) return

            if (seenAddresses.contains(device.address)) return
            seenAddresses.add(device.address)

            val item = DanaScanResult.Real(
                name = name,
                address = device.address,
                device = device,
            )

            addOrUpdateScanResult(item)
        }

        override fun onScanFailed(errorCode: Int) {
            logger?.log("BLE Scan failed with error code: $errorCode")
            stopScan()
        }
    }

    private fun addOrUpdateScanResult(item: DanaScanResult) {
        _scanResults.update { current ->
            val index = current.indexOfFirst { it.address == item.address }
            if (index >= 0) {
                current.toMutableList().apply { set(index, item) }
            } else {
                current + item
            }
        }
    }

    companion object {
        /**
         * Validates whether a discovered device matches the criteria of a Dana insulin pump.
         *
         * Dana pumps broadcast a 10-character alphanumeric serial number as their BLE device name
         * (e.g. "I123456789" or "D123456789").
         */
        internal fun isValidDanaPumpDevice(name: String): Boolean {
            return name.length == 10 && name.all { it.isLetterOrDigit() }
        }
    }
}