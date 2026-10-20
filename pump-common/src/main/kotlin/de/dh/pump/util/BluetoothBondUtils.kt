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

    /**
     * Initiates Bluetooth bonding (pairing) with the target device.
     *
     * @return `true` if bonding process was initiated or device is already bonded, `false` on failure.
     */
    @SuppressLint("MissingPermission")
    fun createBond(device: BluetoothDevice): Boolean {
        return try {
            if (device.bondState == BluetoothDevice.BOND_BONDED) {
                Log.d(TAG, "Device ${device.address} is already bonded")
                true
            } else {
                Log.d(TAG, "Initiating Bluetooth bond for device ${device.address}")
                device.createBond()
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException creating Bluetooth bond for device ${device.address}", e)
            false
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create Bluetooth bond for device ${device.address}", e)
            false
        }
    }

    /**
     * Removes the Bluetooth bond (unpairs) the target device from the Android OS.
     *
     * @return `true` if bond removal was invoked or device is already not bonded, `false` on failure.
     */
    @SuppressLint("MissingPermission")
    fun removeBond(device: BluetoothDevice): Boolean {
        return try {
            if (device.bondState == BluetoothDevice.BOND_NONE) {
                Log.d(TAG, "Device ${device.address} is already not bonded")
                true
            } else {
                Log.d(TAG, "Removing Bluetooth bond for device ${device.address}")
                val removeBondMethod = device.javaClass.getMethod("removeBond")
                val result = (removeBondMethod.invoke(device) as? Boolean) ?: false
                if (result) {
                    Log.d(TAG, "Successfully invoked removeBond for device ${device.address}")
                }
                result
            }
        } catch (e: Exception) {
            val cause = if (e is java.lang.reflect.InvocationTargetException) e.cause ?: e else e
            if (cause is SecurityException) {
                Log.w(TAG, "Cannot remove Bluetooth bond programmatically on Android OS: BLUETOOTH_PRIVILEGED permission required for ${device.address}")
            } else {
                Log.e(TAG, "Failed to remove Bluetooth bond for device ${device.address}", cause)
            }
            false
        }
    }
}