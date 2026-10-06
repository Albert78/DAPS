package de.dh.pump.transport

import android.bluetooth.BluetoothGattCharacteristic
import java.util.UUID
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * BLE service layout and timing policy for a pump connection.
 *
 * Some pump protocols identify their GATT endpoint by characteristic UUIDs rather than by a stable
 * service UUID. When [serviceUuid] is null, the transport scans all discovered services and selects
 * the configured TX/RX characteristics wherever they are found.
 */
data class BlePumpConfig(
    val serviceUuid: UUID? = null,
    val txCharacteristicUuid: UUID,
    val rxCharacteristicUuid: UUID,
    val connectionTimeout: Duration = 10.seconds,
    val operationTimeout: Duration = 5.seconds,
    val writeType: Int = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT,
    val maxWriteChunkSize: Int = 20,
) {
    companion object {
        /** Standard GATT descriptor used to enable notifications or indications on a characteristic. */
        val CLIENT_CHARACTERISTIC_CONFIG_UUID: UUID =
            UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
    }
}