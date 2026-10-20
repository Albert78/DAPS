package de.dh.pump.util

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.util.Log

/**
 * Utility functions for managing Bluetooth device bonding (pairing) and bond removal across pump drivers.
 */
object BluetoothBondUtils {
    private const val TAG = "BluetoothBondUtils"

    /**
     * Checks whether the given Bluetooth device is currently bonded (paired).
     */
    @SuppressLint("MissingPermission")
    fun isBonded(device: BluetoothDevice): Boolean {
        return try {
            device.bondState == BluetoothDevice.BOND_BONDED
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException checking bondState for device ${device.address}", e)
            false
        } catch (e: Exception) {
            Log.e(TAG, "Error checking bondState for device ${device.address}", e)
            false
        }
    }
}