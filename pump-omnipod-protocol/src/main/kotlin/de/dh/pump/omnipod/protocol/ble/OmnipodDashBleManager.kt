package de.dh.pump.omnipod.protocol.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import de.dh.pump.omnipod.protocol.command.Command
import de.dh.pump.omnipod.protocol.state.OmnipodDashPodStateManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.util.UUID

enum class PodBleConnectionState {
    DISCONNECTED, CONNECTING, CONNECTED, FAILED
}

class OmnipodDashBleManager(
    private val context: Context,
    private val podStateManager: OmnipodDashPodStateManager
) {
    private val _connectionState = MutableStateFlow(PodBleConnectionState.DISCONNECTED)
    val connectionState: StateFlow<PodBleConnectionState> = _connectionState

    private var bluetoothGatt: BluetoothGatt? = null
    private var cmdCharacteristic: BluetoothGattCharacteristic? = null
    private var dataCharacteristic: BluetoothGattCharacteristic? = null

    private val gattCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED && status == BluetoothGatt.GATT_SUCCESS) {
                _connectionState.value = PodBleConnectionState.CONNECTING
                gatt.discoverServices()
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                _connectionState.value = PodBleConnectionState.DISCONNECTED
                closeGatt()
            } else {
                _connectionState.value = PodBleConnectionState.FAILED
            }
        }

        @SuppressLint("MissingPermission")
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val service = gatt.getService(CharacteristicType.SERVICE_UUID)
                if (service != null) {
                    cmdCharacteristic = service.getCharacteristic(CharacteristicType.CMD.uuid)
                    dataCharacteristic = service.getCharacteristic(CharacteristicType.DATA.uuid)
                    if (cmdCharacteristic != null && dataCharacteristic != null) {
                        enableIndications(gatt, cmdCharacteristic!!)
                        enableIndications(gatt, dataCharacteristic!!)
                        _connectionState.value = PodBleConnectionState.CONNECTED
                        return
                    }
                }
            }
            _connectionState.value = PodBleConnectionState.FAILED
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            // Received indication payload
        }
    }

    @SuppressLint("MissingPermission")
    suspend fun connect(deviceAddress: String): Result<Unit> = withContext(Dispatchers.IO) {
        _connectionState.value = PodBleConnectionState.CONNECTING
        try {
            val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
                ?: return@withContext Result.failure(IllegalStateException("BluetoothManager unavailable"))
            val adapter = bluetoothManager.adapter
                ?: return@withContext Result.failure(IllegalStateException("BluetoothAdapter unavailable"))
            val device = adapter.getRemoteDevice(deviceAddress)
                ?: run {
                    _connectionState.value = PodBleConnectionState.FAILED
                    return@withContext Result.failure(IllegalArgumentException("Device not found: $deviceAddress"))
                }
            podStateManager.bluetoothAddress = deviceAddress
            bluetoothGatt = device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
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

    @SuppressLint("MissingPermission")
    private fun enableIndications(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
        gatt.setCharacteristicNotification(characteristic, true)
        val descriptor = characteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG_UUID)
        if (descriptor != null) {
            descriptor.value = BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
            gatt.writeDescriptor(descriptor)
        }
    }

    @SuppressLint("MissingPermission")
    fun disconnect() {
        bluetoothGatt?.disconnect()
        closeGatt()
        _connectionState.value = PodBleConnectionState.DISCONNECTED
    }

    @SuppressLint("MissingPermission")
    private fun closeGatt() {
        bluetoothGatt?.close()
        bluetoothGatt = null
        cmdCharacteristic = null
        dataCharacteristic = null
    }

    companion object {
        private val CLIENT_CHARACTERISTIC_CONFIG_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
    }
}