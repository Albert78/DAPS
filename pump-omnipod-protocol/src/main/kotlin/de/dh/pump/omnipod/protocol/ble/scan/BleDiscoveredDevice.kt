package de.dh.pump.omnipod.protocol.ble.scan

import android.bluetooth.le.ScanRecord
import android.bluetooth.le.ScanResult
import android.os.ParcelUuid

class DiscoveredInvalidPodException(message: String, cause: Throwable? = null) : Exception(message, cause)

class BleDiscoveredDevice(
    val scanResult: ScanResult,
    val scanRecord: ScanRecord,
    val podId: Long
) {
    val address: String get() = scanResult.device.address
    val sequenceNo: Int
    val lotNo: Long

    @Throws(DiscoveredInvalidPodException::class)
    private fun validateServiceUUIDs() {
        val serviceUuids = scanRecord.serviceUuids
            ?: throw DiscoveredInvalidPodException("No service UUIDs found in scan record")
        if (serviceUuids.size != 9) {
            throw DiscoveredInvalidPodException("Expected 9 service UUIDs in advertisement, got ${serviceUuids.size}")
        }
        if (extractUUID16(serviceUuids[0]) != MAIN_SERVICE_UUID) {
            throw DiscoveredInvalidPodException(
                "First service UUID expected to be 4024, got ${extractUUID16(serviceUuids[0])}"
            )
        }
        if (extractUUID16(serviceUuids[2]) != UNKNOWN_THIRD_SERVICE_UUID) {
            throw DiscoveredInvalidPodException(
                "Third service UUID expected to be 000a, got ${extractUUID16(serviceUuids[2])}"
            )
        }
    }

    @Throws(DiscoveredInvalidPodException::class)
    private fun validatePodId() {
        val serviceUUIDs = scanRecord.serviceUuids ?: return
        val hexPodId = extractUUID16(serviceUUIDs[3]) + extractUUID16(serviceUUIDs[4])
        val foundPodId = hexPodId.toLong(16)
        if (this.podId != foundPodId) {
            throw DiscoveredInvalidPodException(
                "Pod ID mismatch: expected ${this.podId}, found $foundPodId ($hexPodId)"
            )
        }
    }

    private fun parseLotNo(): Long {
        val serviceUUIDs = scanRecord.serviceUuids!!
        val lotSeq = extractUUID16(serviceUUIDs[5]) +
                extractUUID16(serviceUUIDs[6]) +
                extractUUID16(serviceUUIDs[7])
        return lotSeq.substring(0, 10).toLong(16)
    }

    private fun parseSeqNo(): Int {
        val serviceUUIDs = scanRecord.serviceUuids!!
        val lotSeq = extractUUID16(serviceUUIDs[7]) +
                extractUUID16(serviceUUIDs[8])
        return lotSeq.substring(2).toInt(16)
    }

    init {
        validateServiceUUIDs()
        validatePodId()
        lotNo = parseLotNo()
        sequenceNo = parseSeqNo()
    }

    override fun toString(): String {
        return "BleDiscoveredDevice(address='$address', podId=$podId, lotNo=$lotNo, sequenceNo=$sequenceNo)"
    }

    companion object {
        const val MAIN_SERVICE_UUID = "4024"
        const val UNKNOWN_THIRD_SERVICE_UUID = "000a"

        private fun extractUUID16(uuid: ParcelUuid): String {
            return uuid.toString().substring(4, 8)
        }
    }
}