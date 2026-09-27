package de.dh.daps.core.repository

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow

enum class BluetoothStatus {
    ENABLED,
    DISABLED,
    TURNING_ON,
    TURNING_OFF,
    UNAVAILABLE
}

/**
 * Repository for providing access to the Android system and phone device status.
 */
class DeviceStatusRepository(private val context: Context) {
    private val _isServiceRunning = MutableStateFlow(false)
    val isServiceRunning: StateFlow<Boolean> = _isServiceRunning.asStateFlow()

    fun setServiceRunning(running: Boolean) {
        _isServiceRunning.value = running
    }

    fun observeBatteryPercentage(): Flow<Int> = callbackFlow {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                intent?.let {
                    val level = it.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                    val scale = it.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                    if (level >= 0 && scale > 0) {
                        trySend((level * 100 / scale.toFloat()).toInt())
                    }
                }
            }
        }

        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val initialIntent = context.registerReceiver(receiver, filter)

        initialIntent?.let { intent ->
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            if (level >= 0 && scale > 0) {
                trySend((level * 100 / scale.toFloat()).toInt())
            }
        }

        awaitClose {
            try {
                context.unregisterReceiver(receiver)
            } catch (_: IllegalArgumentException) {
                // Receiver was not registered
            }
        }
    }

    fun observeBluetoothStatus(): Flow<BluetoothStatus> = callbackFlow {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val adapter = bluetoothManager?.adapter

        fun getCurrentStatus(): BluetoothStatus {
            if (adapter == null) return BluetoothStatus.UNAVAILABLE
            return when (adapter.state) {
                BluetoothAdapter.STATE_ON -> BluetoothStatus.ENABLED
                BluetoothAdapter.STATE_OFF -> BluetoothStatus.DISABLED
                BluetoothAdapter.STATE_TURNING_ON -> BluetoothStatus.TURNING_ON
                BluetoothAdapter.STATE_TURNING_OFF -> BluetoothStatus.TURNING_OFF
                else -> BluetoothStatus.DISABLED
            }
        }

        trySend(getCurrentStatus())

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                if (intent?.action == BluetoothAdapter.ACTION_STATE_CHANGED) {
                    trySend(getCurrentStatus())
                }
            }
        }

        val filter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        context.registerReceiver(receiver, filter)

        awaitClose {
            try {
                context.unregisterReceiver(receiver)
            } catch (_: IllegalArgumentException) {
                // Receiver was not registered
            }
        }
    }
}