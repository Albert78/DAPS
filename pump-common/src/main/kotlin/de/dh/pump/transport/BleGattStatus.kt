package de.dh.pump.transport

import android.bluetooth.BluetoothGatt

/**
 * Standard Android GATT and HCI status codes returned by [android.bluetooth.BluetoothGattCallback].
 */
object BleGattStatus {
    /** GATT operation completed successfully (0x00). */
    const val GATT_SUCCESS = BluetoothGatt.GATT_SUCCESS

    /** Insufficient authentication for GATT operation (0x05). */
    const val GATT_INSUFFICIENT_AUTHENTICATION = BluetoothGatt.GATT_INSUFFICIENT_AUTHENTICATION

    /** Insufficient encryption for GATT operation (0x0F). */
    const val GATT_INSUFFICIENT_ENCRYPTION = BluetoothGatt.GATT_INSUFFICIENT_ENCRYPTION

    /**
     * Remote User Terminated Connection (0x13 / 19).
     * HCI status code indicating that the remote peripheral (e.g. pump) actively closed or rejected the BLE link.
     */
    const val GATT_CONN_TERMINATE_PEER_USER = 19

    /**
     * Internal Android GATT Error (0x85 / 133).
     * Generic Android BLE stack error, frequently caused by stale bonds, connection timeouts, or stack deadlocks.
     */
    const val GATT_ERROR = 133
}

/**
 * Exception thrown when a Bluetooth GATT operation or connection state change fails.
 */
class BleGattException(
    val status: Int,
    val newState: Int,
    message: String = "GATT operation failed: status=$status, state=$newState",
) : IllegalStateException(message)