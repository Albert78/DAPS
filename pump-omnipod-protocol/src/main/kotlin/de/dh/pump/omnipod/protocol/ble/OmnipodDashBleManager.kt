package de.dh.pump.omnipod.protocol.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothStatusCodes
import android.content.Context
import de.dh.pump.omnipod.protocol.command.Command
import de.dh.pump.omnipod.protocol.definition.NakErrorType
import de.dh.pump.omnipod.protocol.response.NakResponse
import de.dh.pump.omnipod.protocol.response.ResponseParser
import de.dh.pump.omnipod.protocol.security.CommandAckError
import de.dh.pump.omnipod.protocol.security.CommandReceiveError
import de.dh.pump.omnipod.protocol.security.CommandReceiveSuccess
import de.dh.pump.omnipod.protocol.security.CommandSendSuccess
import de.dh.pump.omnipod.protocol.security.Session
import de.dh.pump.omnipod.protocol.state.OmnipodDashPodStateManager
import de.dh.pump.omnipod.protocol.transport.BleCommand
import de.dh.pump.omnipod.protocol.transport.BleCommandHello
import de.dh.pump.omnipod.protocol.transport.BleCommandType
import de.dh.pump.omnipod.protocol.transport.BleConfirmError
import de.dh.pump.omnipod.protocol.transport.BleConfirmIncorrectData
import de.dh.pump.omnipod.protocol.transport.BleConfirmResult
import de.dh.pump.omnipod.protocol.transport.BleConfirmSuccess
import de.dh.pump.omnipod.protocol.transport.BleSendErrorSending
import de.dh.pump.omnipod.protocol.transport.BleSendResult
import de.dh.pump.omnipod.protocol.transport.BleSendSuccess
import de.dh.pump.omnipod.protocol.transport.CmdBleIO
import de.dh.pump.omnipod.protocol.transport.DataBleIO
import de.dh.pump.omnipod.protocol.transport.MessageIO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.UUID
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

enum class PodBleConnectionState {
    DISCONNECTED, CONNECTING, CONNECTED, FAILED
}

class OmnipodDashBleManager(
    private val context: Context,
    private val podStateManager: OmnipodDashPodStateManager
) {
    private val commandMutex = Mutex()

    private val _connectionState = MutableStateFlow(PodBleConnectionState.DISCONNECTED)
    val connectionState: StateFlow<PodBleConnectionState> = _connectionState

    var session: Session? = null

    private var bluetoothGatt: BluetoothGatt? = null
    private var cmdCharacteristic: BluetoothGattCharacteristic? = null
    private var dataCharacteristic: BluetoothGattCharacteristic? = null

    private val cmdQueue = LinkedBlockingQueue<ByteArray>()
    private val dataQueue = LinkedBlockingQueue<ByteArray>()

    val cmdBleIO: CmdBleIO = object : CmdBleIO {
        override fun receivePacket(timeoutMs: Long): ByteArray? {
            return try {
                cmdQueue.poll(timeoutMs, TimeUnit.MILLISECONDS)
            } catch (_: InterruptedException) {
                null
            }
        }

        @SuppressLint("MissingPermission")
        override fun sendAndConfirmPacket(payload: ByteArray): BleSendResult {
            val characteristic = cmdCharacteristic ?: return BleSendErrorSending("CMD characteristic null")
            val gatt = bluetoothGatt ?: return BleSendErrorSending("GATT null")
            val status = gatt.writeCharacteristic(characteristic, payload, characteristic.writeType)
            val success = status == BluetoothStatusCodes.SUCCESS
            return if (success) BleSendSuccess else BleSendErrorSending("writeCharacteristic failed: $status")
        }

        override fun flushIncomingQueue(): Boolean {
            val foundRTS = cmdQueue.any { it.isNotEmpty() && it[0] == BleCommandType.RTS.value }
            cmdQueue.clear()
            return foundRTS
        }

        override fun readyToRead(): BleSendResult = BleSendSuccess

        override fun peekCommand(): ByteArray? = cmdQueue.peek()

        override fun hello(): BleSendResult = sendAndConfirmPacket(BleCommandHello(4242).data)

        override fun expectCommandType(expected: BleCommand, timeoutMs: Long): BleConfirmResult {
            val packet = receivePacket(timeoutMs) ?: return BleConfirmError("Timeout waiting for $expected")
            if (packet.contentEquals(expected.data)) {
                return BleConfirmSuccess
            }
            return BleConfirmIncorrectData(packet)
        }
    }

    val dataBleIO: DataBleIO = object : DataBleIO {
        override fun receivePacket(timeoutMs: Long): ByteArray? {
            return try {
                dataQueue.poll(timeoutMs, TimeUnit.MILLISECONDS)
            } catch (_: InterruptedException) {
                null
            }
        }

        @SuppressLint("MissingPermission")
        override fun sendAndConfirmPacket(payload: ByteArray): BleSendResult {
            val characteristic = dataCharacteristic ?: return BleSendErrorSending("DATA characteristic null")
            val gatt = bluetoothGatt ?: return BleSendErrorSending("GATT null")
            val status = gatt.writeCharacteristic(characteristic, payload, characteristic.writeType)
            val success = status == BluetoothStatusCodes.SUCCESS
            return if (success) BleSendSuccess else BleSendErrorSending("writeCharacteristic failed: $status")
        }

        override fun flushIncomingQueue(): Boolean {
            dataQueue.clear()
            return false
        }

        override fun readyToRead(): BleSendResult = BleSendSuccess
    }

    val messageIO = MessageIO(cmdBleIO, dataBleIO)

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

        @Deprecated("Deprecated in Java")
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            @Suppress("DEPRECATION")
            val value = characteristic.value ?: return
            handleCharacteristicChanged(characteristic.uuid, value)
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            handleCharacteristicChanged(characteristic.uuid, value)
        }

        private fun handleCharacteristicChanged(uuid: UUID, value: ByteArray) {
            val bytes = value.copyOf()
            when (uuid) {
                CharacteristicType.CMD.uuid -> cmdQueue.offer(bytes)
                CharacteristicType.DATA.uuid -> dataQueue.offer(bytes)
            }
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
            bluetoothGatt = device.connectGatt(
                context,
                false,
                gattCallback,
                BluetoothDevice.TRANSPORT_LE
            )
            Result.success(Unit)
        } catch (e: Exception) {
            _connectionState.value = PodBleConnectionState.FAILED
            Result.failure(e)
        }
    }

    @SuppressLint("MissingPermission")
    suspend fun sendCommand(command: Command, maxRetries: Int = 1): Result<ByteArray> = commandMutex.withLock {
        withContext(Dispatchers.IO) {
            var attempt = 0
            var lastError: Throwable? = null

            while (attempt <= maxRetries) {
                if (_connectionState.value != PodBleConnectionState.CONNECTED) {
                    return@withContext Result.failure(IllegalStateException("BLE Not Connected"))
                }
                try {
                    val payload = command.encoded
                    val activeSession = session
                    val responseBytes = if (activeSession != null) {
                        val sendResult = activeSession.sendCommand(payload)
                        if (sendResult !is CommandSendSuccess) {
                            lastError = IllegalStateException("Session command send failed: $sendResult")
                            attempt++
                            continue
                        }
                        when (val receiveResult = activeSession.readAndAckResponse()) {
                            is CommandReceiveSuccess -> receiveResult.payload
                            is CommandAckError -> receiveResult.payload
                            is CommandReceiveError -> {
                                lastError = IllegalStateException("Failed to receive response: ${receiveResult.msg}")
                                attempt++
                                continue
                            }
                        }
                    } else {
                        cmdQueue.clear()
                        val characteristic = cmdCharacteristic
                        val gatt = bluetoothGatt
                        if (characteristic != null && gatt != null) {
                            val status = gatt.writeCharacteristic(characteristic, payload, characteristic.writeType)
                            if (status != BluetoothStatusCodes.SUCCESS) {
                                lastError = IllegalStateException("Failed to write GATT characteristic, status: $status")
                                attempt++
                                continue
                            }
                            val bytes = cmdQueue.poll(5000, TimeUnit.MILLISECONDS)
                            if (bytes == null) {
                                lastError = IllegalStateException("Timeout waiting for response indication")
                                attempt++
                                continue
                            }
                            bytes
                        } else {
                            return@withContext Result.failure(IllegalStateException("Characteristics not initialized"))
                        }
                    }

                    try {
                        val parsed = ResponseParser.parseResponse(responseBytes)
                        if (parsed is NakResponse) {
                            if (parsed.nakErrorType == NakErrorType.ILLEGAL_SECURITY_CODE) {
                                podStateManager.resyncNonceFromNak(parsed)
                                if (attempt < maxRetries) {
                                    attempt++
                                    continue
                                }
                            }
                            return@withContext Result.failure(
                                IllegalStateException("Command rejected with NAK: ${parsed.nakErrorType} (SyncCount: ${parsed.securityNakSyncCount})")
                            )
                        }
                    } catch (_: Exception) {
                        // Not a NAK response or unparseable, return raw bytes
                    }

                    return@withContext Result.success(responseBytes)
                } catch (e: Exception) {
                    lastError = e
                    attempt++
                }
            }
            Result.failure(lastError ?: IllegalStateException("Command execution failed after $maxRetries retries"))
        }
    }

    @SuppressLint("MissingPermission")
    private fun enableIndications(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
        gatt.setCharacteristicNotification(characteristic, true)
        val descriptor = characteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG_UUID)
        if (descriptor != null) {
            gatt.writeDescriptor(descriptor, BluetoothGattDescriptor.ENABLE_INDICATION_VALUE)
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
        session = null
        cmdQueue.clear()
        dataQueue.clear()
    }

    companion object {
        private val CLIENT_CHARACTERISTIC_CONFIG_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
    }
}