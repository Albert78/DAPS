package de.dh.pump.omnipod.protocol.ble.scan

import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.util.Log
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap

class ScanCollector(private val podId: Long) : ScanCallback() {

    private val foundMap = ConcurrentHashMap<String, ScanResult>()
    private var errorCode: Int = 0

    override fun onScanResult(callbackType: Int, result: ScanResult) {
        Log.d(TAG, "Scan found device: ${result.device.address}, RSSI: ${result.rssi}")
        foundMap[result.device.address] = result
    }

    override fun onScanFailed(errorCode: Int) {
        Log.w(TAG, "Scan failed with error code: $errorCode")
        this.errorCode = errorCode
        super.onScanFailed(errorCode)
    }

    @Throws(IllegalStateException::class)
    fun collect(): List<BleDiscoveredDevice> {
        if (errorCode != 0) {
            throw IllegalStateException("BLE Scan failed with error code $errorCode")
        }

        val discoveredDevices = mutableListOf<BleDiscoveredDevice>()
        for (result in foundMap.values) {
            val scanRecord = result.scanRecord ?: continue
            try {
                val device = BleDiscoveredDevice(result, scanRecord, podId)
                discoveredDevices.add(device)
                Log.d(TAG, "Successfully validated Pod: $device")
            } catch (e: DiscoveredInvalidPodException) {
                Log.d(TAG, "Ignored non-matching BLE device ${result.device.address}: ${e.message}")
            }
        }
        return Collections.unmodifiableList(discoveredDevices)
    }

    companion object {
        private const val TAG = "ScanCollector"
    }
}