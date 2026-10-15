package de.dh.pump.danai.core.connection

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import de.dh.pump.dana.DanaBleProfiles
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
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Collections
import kotlin.coroutines.resume
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

    private var shouldVerifyCharacteristics = false
    private val probedAddresses = Collections.synchronizedSet(mutableSetOf<String>())

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
                                parentScope?.let { startScan(it, verifyCharacteristics = shouldVerifyCharacteristics) }
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
     * When [verifyCharacteristics] is true, each candidate device is probed via GATT to confirm
     * the presence of required Dana TX/RX characteristics before adding it to [scanResults].
     */
    @SuppressLint("MissingPermission")
    fun startScan(
        scope: CoroutineScope,
        duration: Duration = 15.seconds,
        verifyCharacteristics: Boolean = false,
    ) {
        parentScope = scope
        shouldVerifyCharacteristics = verifyCharacteristics

        // Stop any active scan and cancel all probing/timeout jobs
        stopScan()

        probedAddresses.clear()
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
            logger?.log("BLE Scan started for Dana pumps (verifyCharacteristics=$verifyCharacteristics).")
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
     * Stops the active BLE scan and cancels all associated background/probing jobs.
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

            if (probedAddresses.contains(device.address)) return
            probedAddresses.add(device.address)

            val item = DanaScanResult.Real(
                name = name,
                address = device.address,
                device = device,
            )

            if (shouldVerifyCharacteristics) {
                activeScanScope?.launch {
                    logger?.log("Probing GATT characteristics for $name (${device.address})...")
                    val isValid = withTimeoutOrNull(30.seconds) {
                        verifyPumpCharacteristics(device)
                    } ?: false

                    if (isValid) {
                        logger?.log("GATT probe SUCCESS: $name exposes required Dana TX/RX characteristics.")
                        addOrUpdateScanResult(item)
                    } else {
                        logger?.log("GATT probe FAILED: $name missing required Dana characteristics.")
                    }
                }
            } else {
                addOrUpdateScanResult(item)
            }
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

    @Suppress("DEPRECATION")
    @SuppressLint("MissingPermission")
    private suspend fun verifyPumpCharacteristics(device: BluetoothDevice): Boolean =
        suspendCancellableCoroutine { continuation ->
            val profile = DanaBleProfiles.danaI()
            var gatt: BluetoothGatt? = null

            val callback = object : BluetoothGattCallback() {
                override fun onConnectionStateChange(g: BluetoothGatt, status: Int, newState: Int) {
                    if (status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothProfile.STATE_CONNECTED) {
                        g.discoverServices()
                    } else {
                        try { g.close() } catch (_: Exception) {}
                        if (continuation.isActive) continuation.resume(false)
                    }
                }

                override fun onServicesDiscovered(g: BluetoothGatt, status: Int) {
                    val hasTx = g.services.any { s ->
                        s.characteristics.any { c -> c.uuid == profile.txCharacteristicUuid }
                    }
                    val hasRx = g.services.any { s ->
                        s.characteristics.any { c -> c.uuid == profile.rxCharacteristicUuid }
                    }

                    try {
                        g.disconnect()
                        g.close()
                    } catch (_: Exception) {}

                    if (continuation.isActive) continuation.resume(hasTx && hasRx)
                }
            }

            try {
                gatt = device.connectGatt(appContext, false, callback, BluetoothDevice.TRANSPORT_LE)
                if (gatt == null && continuation.isActive) {
                    continuation.resume(false)
                }
            } catch (_: Exception) {
                if (continuation.isActive) continuation.resume(false)
            }

            continuation.invokeOnCancellation {
                try {
                    gatt?.disconnect()
                    gatt?.close()
                } catch (_: Exception) {}
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