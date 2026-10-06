package de.dh.pump.danai.core.connection

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.content.Context
import de.dh.pump.PumpClient
import de.dh.pump.dana.DanaBleProfiles
import de.dh.pump.dana.protocol.DanaRsHandshake
import de.dh.pump.dana.protocol.DanaRsHandshakeResult
import de.dh.pump.dana.protocol.DanaRsHandshakeState
import de.dh.pump.dana.protocol.DanaRsPacketCodec
import de.dh.pump.dana.protocol.DanaRsPairingSecrets
import de.dh.pump.danai.core.DanaILogger
import de.dh.pump.danai.core.DanaPumpPreferences
import de.dh.daps.common.model.PluginPreferences
import de.dh.pump.transport.AndroidBleTransport
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.util.UUID
import kotlin.time.Duration.Companion.milliseconds

/**
 * Low-level state of the Bluetooth connection to a Dana pump.
 */
sealed interface SessionState {
    /**
     * True if the session has reached a steady state (Connected, Failed, or Disconnected)
     * and is no longer automatically transitioning.
     */
    val isStable: Boolean get() = false

    /**
     * True if the session is in a terminal state (Failed or Disconnected)
     * and can no longer be used.
     * It's not necessary to call [DanaISession.disconnect] in this state.
     */
    val isClosed: Boolean get() = false

    /**
     * Establishing the physical Bluetooth connection.
     */
    object Connecting : SessionState

    /**
     * Physical link is up, now performing the protocol handshake.
     */
    object Authenticating : SessionState

    /**
     * Handshake is waiting for user interaction (Bluetooth bonding).
     * This state can persist until the user confirms the pairing dialog.
     */
    object Pairing : SessionState

    /**
     * Handshake successful. The pump is fully operational.
     * Operations: All commands can be performed via the client.
     * The session can be closed via [DanaISession.disconnect] to release BLE resources.
     */
    data class Connected(val client: PumpClient) : SessionState {
        override val isStable = true
    }

    /**
     * The session is being closed.
     */
    object Disconnecting : SessionState

    /**
     * An error occurred during connection or handshake.
     * Operations: Inspect [message] and [error].
     */
    data class Failed(val message: String, val error: Throwable? = null) : SessionState {
        override val isStable = true
        override val isClosed = true
    }

    /**
     * The session has ended (manual disconnect or link loss).
     * Operations: None.
     */
    object Disconnected : SessionState {
        override val isStable = true
        override val isClosed = true
    }
}

/**
 * Represents a single session for a specific Bluetooth connection with the Dana-i pump.
 *
 * This class handles the short-lived lifecycle of a single connection attempt and its
 * subsequent active state. It performs the entire setup process (physical connect and
 * protocol handshake) within its [runLifecycle] method to allow external observers
 * to track the detailed progress which can take a significant amount of time (some seconds up to
 * minutes, if a pairing is necessary).
 */
class DanaISession(
    private val device: BluetoothDevice,
    private val deviceName: String,
    private val txUuid: String,
    private val rxUuid: String,
    preferences: PluginPreferences,
    private val logger: DanaILogger,
    private val context: Context,
    private val onSessionStateChanged: (SessionState) -> Unit
) {
    private val danaPreferences = DanaPumpPreferences(preferences)

    /**
     * The current state of the session.
     * Updates trigger the callback and signal session termination if the state is closed.
     */
    var state: SessionState = SessionState.Connecting
        private set(value) {
            if (field == value) return
            field = value
            onSessionStateChanged(value)
            if (value.isClosed) {
                closedDeferred.complete(Unit)
            }
        }

    private val closedDeferred = CompletableDeferred<Unit>()
    private val codec = DanaRsPacketCodec()
    private var transport: AndroidBleTransport? = null

    /**
     * Executes the session lifecycle.
     *
     * This method orchestrates the entire process: Establishing the physical link,
     * performing the handshake, and maintaining the active connection until the
     * physical link is lost or the session is manually disconnected.
     *
     * @throws Exception if the connection, handshake or the session fails.
     */
    @SuppressLint("MissingPermission")
    suspend fun runLifecycle() = coroutineScope {
        try {
            val profile = DanaBleProfiles.danaI(
                txCharacteristicUuid = UUID.fromString(txUuid.trim()),
                rxCharacteristicUuid = UUID.fromString(rxUuid.trim()),
            )

            logger.log("Connecting to ${device.address}...")
            val transport = withContext(Dispatchers.Main) {
                AndroidBleTransport.connect(
                    context = context,
                    device = device,
                    config = profile.toBlePumpConfig(),
                )
            }
            this@DanaISession.transport = transport

            // Watch for physical disconnect in background
            val monitoringJob = launch {
                transport.state.collect { transportState ->
                    if (transportState is AndroidBleTransport.TransportState.Disconnected) {
                        state = SessionState.Disconnected
                    }
                }
            }

            try {
                logger.log("Physical link established. Starting handshake...")
                try {
                    performHandshake(transport)
                } catch (error: Exception) {
                    if (!state.isClosed) {
                        state = SessionState.Failed("Handshake failed", error)
                    }
                    throw error
                }

                // IMPORTANT: We pass 'this' coroutineScope to the client factory
                val client = PumpClient(
                    transport = transport,
                    scope = this@coroutineScope,
                    codec = codec,
                )
                logger.log("Session authenticated")

                state = SessionState.Connected(client)

                // Keep session alive until a terminal state is reached
                closedDeferred.await()
            } finally {
                monitoringJob.cancel()
            }
        } catch (e: Exception) {
            if (!state.isClosed && state !is SessionState.Failed) {
                state = SessionState.Failed(e.message ?: "Session failed", e)
            }
        } finally {
            // Ensure we show 'Disconnecting' if the session ends for any other reason (e.g. cancellation)
            if (!state.isClosed && state !is SessionState.Disconnecting) {
                state = SessionState.Disconnecting
            }

            cleanup()

            // Final terminal state
            state = SessionState.Disconnected
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun performHandshake(transport: AndroidBleTransport): DanaRsHandshakeState {
        val pairingKey = danaPreferences.getBle5PairingKey()
        val handshake = DanaRsHandshake(
            codec = codec,
            secrets = DanaRsPairingSecrets(
                // The BLE 5 pairing key serves as a second-level application key.
                //
                // Situation A: Normal Reconnect (Bonding intact)
                // If the Bluetooth bond is active and the pump's state is stable, it will automatically
                // send the current key in the 'PUMP_CHECK' response. In this case, providing a
                // stored key here is optional as the protocol will simply adopt the one from the pump.
                //
                // Situation B: Recovery after Power Loss (e.g. Battery replacement)
                // When the pump loses power, its internal application state is reset. While the
                // Bluetooth bond remains valid on the OS level, the pump will NOT proactively send
                // the pairing key. To recover the session seamlessly, the phone MUST prove it
                // knows the previous key by providing it here.
                //
                // Situation C: Missing Key / Fresh Setup
                // If no key is provided here AND the pump does not send one (Situation B without a
                // stored key), the handshake will automatically transition to Situation C:
                // It triggers a 'Passkey Request' to the pump, which forces a new Bluetooth bonding
                // process and a subsequent, manual key exchange.
                ble5PairingKey = pairingKey
            ),
        )
        val notificationChannel = transport.notifications

        state = SessionState.Authenticating

        // Start handshake
        transport.write(handshake.start(deviceName))

        // Process handshake packets
        while (true) {
            if (device.bondState == BluetoothDevice.BOND_BONDING) {
                state = SessionState.Pairing
            }

            val timeout = if (state is SessionState.Pairing) DANA_PAIRING_TIMEOUT else DANA_HANDSHAKE_TIMEOUT
            val bytes = withTimeout(timeout.milliseconds) {
                notificationChannel.receive()
            }

            val frames = codec.decodeFrames(bytes)
            for (frame in frames) {
                when (val res = handshake.onFrame(frame)) {
                    is DanaRsHandshakeResult.SendNext -> {
                        transport.write(res.bytes)
                    }
                    is DanaRsHandshakeResult.WaitingForPairing -> {
                        state = SessionState.Pairing
                    }
                    is DanaRsHandshakeResult.Connected -> {
                        danaPreferences.saveHandshakeData(
                            pairingKey = res.state.ble5PairingKeyFromPump ?: pairingKey,
                            hardwareModel = res.state.hardwareModel,
                            protocol = res.state.protocol
                        )
                        return res.state
                    }
                    is DanaRsHandshakeResult.Failed -> {
                        throw Exception("Handshake failed: ${res.reason}")
                    }
                }
            }
        }
    }

    suspend fun disconnect() {
        if (state.isClosed) return
        state = SessionState.Disconnecting
        cleanup()
        state = SessionState.Disconnected
    }

    @SuppressLint("MissingPermission")
    private suspend fun cleanup() = withContext(Dispatchers.Main) {
        transport?.close()
        transport = null
    }

    companion object {
        private const val DANA_HANDSHAKE_TIMEOUT = 5000L
        private const val DANA_PAIRING_TIMEOUT = 60000L
    }
}