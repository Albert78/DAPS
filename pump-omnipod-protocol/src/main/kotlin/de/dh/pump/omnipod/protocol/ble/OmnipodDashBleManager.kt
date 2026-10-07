package de.dh.pump.omnipod.protocol.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import de.dh.pump.omnipod.protocol.command.Command
import de.dh.pump.omnipod.protocol.response.Response
import de.dh.pump.omnipod.protocol.state.OmnipodDashPodStateManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext

enum class PodBleConnectionState {
    DISCONNECTED, CONNECTING, CONNECTED, FAILED
}

class OmnipodDashBleManager(
    private val context: Context,
    private val podStateManager: OmnipodDashPodStateManager
) {
    private val _connectionState = MutableStateFlow(PodBleConnectionState.DISCONNECTED)
    val connectionState: StateFlow<PodBleConnectionState> = _connectionState

    @SuppressLint("MissingPermission")
    suspend fun connect(deviceAddress: String): Result<Unit> = withContext(Dispatchers.IO) {
        _connectionState.value = PodBleConnectionState.CONNECTING
        try {
            val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
                ?: return@withContext Result.failure(IllegalStateException("BluetoothManager unavailable"))
            val adapter = bluetoothManager.adapter
                ?: return@withContext Result.failure(IllegalStateException("BluetoothAdapter unavailable"))
            val device = adapter.getRemoteDevice(deviceAddress)
            if (device == null) {
                _connectionState.value = PodBleConnectionState.FAILED
                return@withContext Result.failure(IllegalArgumentException("Device not found: $deviceAddress"))
            }
            podStateManager.bluetoothAddress = deviceAddress
            _connectionState.value = PodBleConnectionState.CONNECTED
            Result.success(Unit)
        } catch (e: Exception) {
            _connectionState.value = PodBleConnectionState.FAILED
            Result.failure(e)
        }
    }

    suspend fun sendCommand(command: Command): Result<ByteArray> = withContext(Dispatchers.IO) {
        if (_connectionState.value != PodBleConnectionState.CONNECTED) {
            return@withContext Result.failure(IllegalStateException("BLE Not Connected"))
        }
        try {
            val payload = command.encoded
            Result.success(payload)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun disconnect() {
        _connectionState.value = PodBleConnectionState.DISCONNECTED
    }
}