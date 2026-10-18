package de.dh.pump.util

import android.Manifest
import android.os.Build

/**
 * Helper utility for determining required Bluetooth permissions based on Android API level.
 */
object BluetoothPermissionUtils {
    /**
     * Returns the required permissions for BLE scanning and connecting.
     * On Android 12 (API level 31) and higher, uses [Manifest.permission.BLUETOOTH_SCAN]
     * and [Manifest.permission.BLUETOOTH_CONNECT].
     * On older Android versions, returns legacy Bluetooth and Location permissions.
     */
    val neededPermissions: List<String>
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            listOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
            )
        } else {
            listOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        }
}