package de.dh.pump.omnipod.protocol.ble.scan

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanSettings
import android.os.ParcelUuid
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

class PodScanner(private val bluetoothAdapter: BluetoothAdapter) {

    @SuppressLint("MissingPermission")
    suspend fun scanForPod(
        serviceUuidString: String = SCAN_FOR_SERVICE_UUID,
        podId: Long = POD_ID_NOT_ACTIVATED,
        scanDurationMs: Long = SCAN_DURATION_MS
    ): BleDiscoveredDevice = withContext(Dispatchers.IO) {
        val scanner = bluetoothAdapter.bluetoothLeScanner
            ?: throw IllegalStateException("BluetoothLeScanner unavailable. Is Bluetooth enabled?")

        val scanFilter = ScanFilter.Builder()
            .setServiceUuid(ParcelUuid.fromString(serviceUuidString))
            .build()

        val scanSettings = ScanSettings.Builder()
            .setLegacy(false)
            .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        val collector = ScanCollector(podId)
        Log.d(TAG, "Starting BLE scan for Omnipod Dash with service UUID $serviceUuidString")

        try {
            scanner.startScan(listOf(scanFilter), scanSettings, collector)
            delay(scanDurationMs)
            scanner.flushPendingScanResults(collector)
        } finally {
            try {
                scanner.stopScan(collector)
            } catch (e: Exception) {
                Log.w(TAG, "Error stopping BLE scan", e)
            }
        }

        val collected = collector.collect()
        if (collected.isEmpty()) {
            throw IllegalStateException("No Omnipod Dash Pod found in range.")
        } else if (collected.size > 1) {
            Log.w(TAG, "Found multiple pods (${collected.size}), selecting the first one: ${collected[0]}")
        }
        collected[0]
    }

    companion object {
        const val SCAN_FOR_SERVICE_UUID = "00004024-0000-1000-8000-00805f9b34fb"
        const val POD_ID_NOT_ACTIVATED = 0xFFFFFFFFL
        private const val SCAN_DURATION_MS = 5000L
        private const val TAG = "PodScanner"
    }
}