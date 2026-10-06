package de.dh.pump.transport

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothProfile
import android.content.Context
import androidx.annotation.RequiresPermission
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.BUFFERED
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import java.util.UUID
import kotlin.time.Duration

/**
 * Android BluetoothGatt implementation of [BleTransport].
 *
 * **Device Binding:** Each instance is strictly bound to an established GATT connection.
 * If the connection is lost, the instance transitions to [TransportState.Disconnected]
 * and cannot be reused.
 *
 * **Implementation Note - Separation of Concerns:**
 * The creation of an [AndroidBleTransport] involves a multi-step handshake: Establishing the
 * physical link, negotiating MTU size, discovering services, and enabling notifications.
 *
 * To keep the operational class "lean and green", we isolate this transient setup logic:
 * 1. A temporary orchestrator ([GattSetup]) manages the asynchronous state machine during connection.
 * 2. A [GattCallbackProxy] allows swapping the GATT callback delegate from "Setup mode"
 *    to "Operational mode" once the connection is ready.
 *
 * This ensures that the final [AndroidBleTransport] instance only represents a fully
 * initialized transport, free from the state-leakage and complexity of the connection process.
 */
class AndroidBleTransport private constructor(
    private val gatt: BluetoothGatt,
    private val config: BlePumpConfig,
    private val txCharacteristic: BluetoothGattCharacteristic,
    private val rxCharacteristic: BluetoothGattCharacteristic,
    private val gattCallbackProxy: GattCallbackProxy,
) : BleTransport {
    private val operationMutex = Mutex()
    private val inbound = Channel<ByteArray>(BUFFERED)

    private val _state = MutableStateFlow<TransportState>(TransportState.Connected)
    val state: StateFlow<TransportState> = _state.asStateFlow()

    private var characteristicWriteResult: CompletableDeferred<Int>? = null

    override val notifications: Channel<ByteArray> = inbound

    init {
        // Switch the GATT proxy to our operational handler
        gattCallbackProxy.delegate = OperationalHandler()
    }

    @SuppressLint("MissingPermission")
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override suspend fun write(bytes: ByteArray) {
        operationMutex.withLock {
            require(config.maxWriteChunkSize > 0) { "maxWriteChunkSize must be > 0" }
            var offset = 0
            while (offset < bytes.size) {
                val nextOffset = minOf(offset + config.maxWriteChunkSize, bytes.size)
                writeChunk(bytes.copyOfRange(offset, nextOffset))
                offset = nextOffset
            }
        }
    }

    @SuppressLint("MissingPermission")
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private suspend fun writeChunk(bytes: ByteArray) {
        val writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        characteristicWriteResult = CompletableDeferred()

        check(
            GattCompat.writeCharacteristic(
                gatt = gatt,
                characteristic = txCharacteristic,
                value = bytes,
                writeType = writeType,
            ),
        ) { "Could not start characteristic write" }

        val status = withTimeout(config.operationTimeout) {
            characteristicWriteResult?.await()
        }
        check(status == BluetoothGatt.GATT_SUCCESS) { "Write failed with status $status" }
    }

    @SuppressLint("MissingPermission")
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override suspend fun close(timeout: Duration) {
        operationMutex.withLock {
            if (_state.value is TransportState.Disconnected) return

            val disconnected = CompletableDeferred<Unit>()
            val disconnectHandler = object : BluetoothGattCallback() {
                override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                    if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                        disconnected.complete(Unit)
                    }
                }
            }

            gattCallbackProxy.delegate = disconnectHandler
            try {
                gatt.disconnect()
                withTimeout(timeout) {
                    disconnected.await()
                }
            } catch (_: Exception) {
                // Timeout or other error during disconnect - we continue to close the gatt
            } finally {
                gatt.close()
                gattCallbackProxy.delegate = null
                inbound.close()

                // Without this, a following connect won't work on many Android devices
                delay(1000)
                _state.value = TransportState.Disconnected()
            }
        }
    }

    sealed interface TransportState {
        object Connected : TransportState
        data class Disconnected(val cause: Throwable? = null) : TransportState
    }

    private inner class OperationalHandler : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_DISCONNECTED || status != BluetoothGatt.GATT_SUCCESS) {
                val error = IllegalStateException("Connection lost (status=$status, state=$newState)")
                characteristicWriteResult?.completeExceptionally(error)
                _state.value = TransportState.Disconnected(error)
                gattCallbackProxy.delegate = null
            }
        }

        override fun onCharacteristicWrite(gatt: BluetoothGatt, char: BluetoothGattCharacteristic, status: Int) {
            characteristicWriteResult?.complete(status)
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt, char: BluetoothGattCharacteristic, value: ByteArray) {
            inbound.trySend(value)
        }

        @Deprecated("Deprecated in API 33")
        override fun onCharacteristicChanged(gatt: BluetoothGatt, char: BluetoothGattCharacteristic) {
            @Suppress("DEPRECATION")
            char.value?.let { inbound.trySend(it) }
        }
    }

    /**
     * Helper to proxy GATT callbacks to a swappable delegate.
     */
    private class GattCallbackProxy : BluetoothGattCallback() {
        @Volatile
        var delegate: BluetoothGattCallback? = null

        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            delegate?.onConnectionStateChange(gatt, status, newState)
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            delegate?.onServicesDiscovered(gatt, status)
        }

        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
            delegate?.onMtuChanged(gatt, mtu, status)
        }

        override fun onCharacteristicWrite(gatt: BluetoothGatt, char: BluetoothGattCharacteristic, status: Int) {
            delegate?.onCharacteristicWrite(gatt, char, status)
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt, char: BluetoothGattCharacteristic, value: ByteArray) {
            delegate?.onCharacteristicChanged(gatt, char, value)
        }

        @Deprecated("Deprecated in Java")
        @Suppress("DEPRECATION")
        override fun onCharacteristicChanged(gatt: BluetoothGatt, char: BluetoothGattCharacteristic) {
            delegate?.onCharacteristicChanged(gatt, char)
        }

        override fun onDescriptorWrite(gatt: BluetoothGatt, desc: BluetoothGattDescriptor, status: Int) {
            delegate?.onDescriptorWrite(gatt, desc, status)
        }
    }

    companion object {
        /**
         * Initiates a new connection to a [BluetoothDevice] and performs the full GATT setup.
         *
         * The setup process includes:
         * 1. Establishing the physical BLE connection.
         * 2. Requesting a high MTU (512) for efficient encrypted data transfer.
         * 3. Discovering available GATT services and characteristics.
         * 4. Identifying the specific TX and RX characteristics required for the pump.
         * 5. Enabling BLE notifications on the RX characteristic.
         *
         * @return A fully initialized [AndroidBleTransport] instance ready for communication.
         * @throws IllegalStateException if any step of the setup fails or the device is not a supported pump.
         */
        @SuppressLint("MissingPermission")
        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        suspend fun connect(
            context: Context,
            device: BluetoothDevice,
            config: BlePumpConfig,
        ): AndroidBleTransport {
            val gattCallback = GattCallbackProxy()
            val setup = GattSetup(config)
            gattCallback.delegate = setup

            // Start the physical connection process
            val gatt = device.connectGatt(context.applicationContext, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
                ?: throw IllegalStateException("Could not initiate GATT connection")

            // The above call device.connectGatt() is deprecated; for API 37 use:
//            val settings = BluetoothGattConnectionSettings.Builder()
//                .setTransport(BluetoothDevice.TRANSPORT_LE)
//                .setAutoConnectEnabled(false)
//                .build()
//
//            val gatt = device.connectGatt(settings, context.mainExecutor, gattCallback)
//                ?: throw IllegalStateException("Could not initiate GATT connection")

            try {
                return withTimeout(config.connectionTimeout) {
                    // Orchestrate the multi-step GATT handshake
                    setup.execute(gatt)

                    // Wrap the established connection into a transport instance
                    AndroidBleTransport(gatt, config, setup.tx!!, setup.rx!!, gattCallback)
                }
            } catch (e: Exception) {
                // Ensure the connection is cleaned up if setup fails at any point
                gatt.disconnect()
                gatt.close()
                throw e
            }
        }

        /**
         * Internal orchestrator that handles the asynchronous multi-step GATT setup.
         *
         * This class acts as a temporary [BluetoothGattCallback] during the connection phase.
         * It collects all necessary GATT objects (MTU, Services, Characteristics) and
         * ensures they are correctly configured before handing them over to the transport.
         */
        private class GattSetup(private val config: BlePumpConfig) : BluetoothGattCallback() {
            private val connection = CompletableDeferred<Unit>()
            private val services = CompletableDeferred<Unit>()
            private val mtu = CompletableDeferred<Int>()
            private val descriptorWrite = CompletableDeferred<Int>()

            var tx: BluetoothGattCharacteristic? = null
            var rx: BluetoothGattCharacteristic? = null

            /**
             * Executes the setup sequence in a linear, suspendable fashion.
             */
            @SuppressLint("MissingPermission")
            suspend fun execute(gatt: BluetoothGatt) {
                // Step 1: Wait for the physical BLE connection to be established
                connection.await()

                // Step 2: Request MTU. Larger MTU is required for Dana-i encrypted packets
                gatt.requestMtu(512)
                mtu.await()

                // Step 3: Discover all services provided by the pump
                gatt.discoverServices()
                services.await()

                // Step 4: Identify the specific TX (write) and RX (notify) characteristics
                val serviceList = config.serviceUuid?.let { listOfNotNull(gatt.getService(it)) } ?: gatt.services
                tx = findChar(serviceList, config.txCharacteristicUuid)
                rx = findChar(serviceList, config.rxCharacteristicUuid)

                // Step 5: Enable notifications on the RX characteristic so the pump can send data
                enableNotifications(gatt, rx!!, config)
            }

            private fun findChar(services: List<BluetoothGattService>, uuid: UUID) =
                services.flatMap { it.characteristics }.find { it.uuid == uuid }
                    ?: throw IllegalStateException("Characteristic $uuid not found")

            @SuppressLint("MissingPermission")
            private suspend fun enableNotifications(gatt: BluetoothGatt, char: BluetoothGattCharacteristic, config: BlePumpConfig) {
                gatt.setCharacteristicNotification(char, true)
                val desc = char.getDescriptor(BlePumpConfig.CLIENT_CHARACTERISTIC_CONFIG_UUID)
                    ?: throw IllegalStateException("CCCD not found")

                GattCompat.writeDescriptor(gatt, desc, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
                val status = withTimeout(config.operationTimeout) { descriptorWrite.await() }
                if (status != BluetoothGatt.GATT_SUCCESS) throw IllegalStateException("Descriptor write failed: $status")
            }

            override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                if (status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothProfile.STATE_CONNECTED) {
                    connection.complete(Unit)
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED || status != BluetoothGatt.GATT_SUCCESS) {
                    val error = IllegalStateException("Connect failed: status=$status, state=$newState")
                    connection.completeExceptionally(error)
                    services.completeExceptionally(error)
                    mtu.completeExceptionally(error)
                    descriptorWrite.completeExceptionally(error)
                }
            }

            override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                if (status == BluetoothGatt.GATT_SUCCESS) services.complete(Unit)
                else services.completeExceptionally(IllegalStateException("Discovery failed: $status"))
            }

            override fun onMtuChanged(gatt: BluetoothGatt, m: Int, status: Int) {
                if (status == BluetoothGatt.GATT_SUCCESS) mtu.complete(m)
                else mtu.completeExceptionally(IllegalStateException("MTU failed: $status"))
            }

            override fun onDescriptorWrite(gatt: BluetoothGatt, d: BluetoothGattDescriptor, status: Int) {
                descriptorWrite.complete(status)
            }
        }
    }
}