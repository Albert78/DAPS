package de.dh.pump.dana

import android.bluetooth.BluetoothGattCharacteristic
import de.dh.pump.transport.BlePumpConfig
import java.util.UUID

/**
 * BLE identity and GATT layout for a Dana pump family.
 *
 * Dana endpoints are identified by their UART-style read/write characteristics. The service UUID is
 * optional because compatible devices may expose the same characteristics under different services.
 */
data class DanaBleProfile(
    val model: DanaPumpModel,
    val serviceUuid: UUID? = null,
    val txCharacteristicUuid: UUID,
    val rxCharacteristicUuid: UUID,
) {
    fun toBlePumpConfig(): BlePumpConfig {
        return BlePumpConfig(
            serviceUuid = serviceUuid,
            txCharacteristicUuid = txCharacteristicUuid,
            rxCharacteristicUuid = rxCharacteristicUuid,
            writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE,
        )
    }
}

object DanaBleProfiles {
    /**
     * Creates a Dana-i profile using the known read/write characteristic UUIDs.
     */
    fun danaI(
        serviceUuid: UUID? = null,
        txCharacteristicUuid: UUID = UUID.fromString(DANA_UART_WRITE_UUID),
        rxCharacteristicUuid: UUID = UUID.fromString(DANA_UART_READ_UUID),
    ): DanaBleProfile {
        return DanaBleProfile(
            model = DanaPumpModel.DANA_I,
            serviceUuid = serviceUuid,
            txCharacteristicUuid = txCharacteristicUuid,
            rxCharacteristicUuid = rxCharacteristicUuid,
        )
    }

    private const val DANA_UART_READ_UUID = "0000fff1-0000-1000-8000-00805f9b34fb"
    private const val DANA_UART_WRITE_UUID = "0000fff2-0000-1000-8000-00805f9b34fb"
}