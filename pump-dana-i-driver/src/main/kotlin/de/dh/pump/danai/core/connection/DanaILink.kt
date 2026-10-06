package de.dh.pump.danai.core.connection

import android.bluetooth.BluetoothDevice
import android.content.Context
import de.dh.pump.PumpClient
import de.dh.pump.PumpConnectionException
import de.dh.pump.commands.PumpCommand
import de.dh.pump.commands.PumpResponse
import de.dh.pump.commands.PumpStreamCommand
import de.dh.pump.danai.core.DanaILogger
import de.dh.daps.common.model.PluginPreferences
import de.dh.daps.common.model.data.Timestamp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Duration.Companion.seconds

/**
 * High-level status of the link to a Dana pump.
 *
 * This state machine abstracts the underlying technical session details into a
 * logical representation suitable for UI and application logic.
 */
sealed interface LinkStatus {
    /**
     * True if the link is currently performing an asynchronous operation
     * (connecting, authenticating, or disconnecting).
     * While busy, new connection attempts or command executions should be avoided.
     */
    val isBusy: Boolean get() = false

    /**
     * The link is inactive and ready to start a new connection.
     * This is the initial state and the state after a clean manual disconnect
     * or an unexpected physical link loss while connected.
     */
    object Idle : LinkStatus

    /**
     * The system is establishing a connection to the pump.
     * This state covers the entire setup process, including Bluetooth connection
     * and protocol handshake (authentication, pairing).
     *
     * Transition: Moves automatically to [Connected] on success or [Idle] on error.
     */
    object Connecting : LinkStatus {
        override val isBusy = true
    }

    /**
     * The connection is fully established and authenticated.
     * All pump commands (bolus, settings, status) can be performed in this state.
     *
     * Note: If the physical link is lost while in this state, the link will automatically
     * transition back to [Idle].
     */
    object Connected : LinkStatus

    /**
     * The connection is being closed.
     * This state persists while the Bluetooth stack performs the teardown.
     */
    object Disconnecting : LinkStatus {
        override val isBusy = true
    }
}

/**
 * Structured information about a connection error.
 */
data class LinkError(
    val message: String,
    val timestamp: Timestamp = Timestamp.now()
)

/**
 * Long-lived manager for the connection/link to a Dana pump.
 *
 * An instance persists across multiple connection attempts and maintains the overall
 * state of the pump link. It delegates the actual connection and handshake logic
 * to short-lived [DanaISession] instances. This separation ensures that the link
 * state is kept consistent even if individual sessions fail or are restarted.
 *
 * ### Normal Operation:
 * The methods [execute] and [executeStream] transparently use the existing active session
 * or automatically initiate a new connection if needed. This process is robust: Even if
 * a previous session is still in its teardown phase ([LinkStatus.Disconnecting]), the
 * manager will wait for the cleanup to complete before establishing a fresh connection.
 *
 * The overall status of this link can be watched via [status], the finer-grained state of the
 * connection session can be watched via [sessionState].
 *
 * ### Usage Notes for Consumers:
 * - **Unpredictable Disconnection**: Callers must expect a connection loss at any time
 *   (e.g., pump out of range). It is the caller's responsibility to buffer or hold
 *   back actions until the link is restored.
 * - **Manual Intervention**: The link might require user action to proceed. For instance,
 *   if the user removes the Bluetooth Bond in Android settings, the connection will
 *   stay in the [LinkStatus.Connecting] phase while waiting for the user to re-confirm
 *   a PIN on both devices. The detailed progress can be observed via [sessionState].
 */
class DanaILink(
    val device: BluetoothDevice,
    val deviceName: String,
    val preferences: PluginPreferences,
    private val logger: DanaILogger,
    private val context: Context,
    private val txUuid: String = DEFAULT_TX_UUID,
    private val rxUuid: String = DEFAULT_RX_UUID
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _status = MutableStateFlow<LinkStatus>(LinkStatus.Idle)

    /**
     * Observable status of this link.
     */
    val status = _status.asStateFlow()

    private val _lastError = MutableStateFlow<LinkError?>(null)

    /**
     * Information about the most recent failed connection attempt.
     * This is cleared automatically when a new connection attempt starts.
     */
    val lastError = _lastError.asStateFlow()

    private val _sessionState = MutableStateFlow<SessionState>(SessionState.Disconnected)

    /**
     * Observable state of the current session.
     * This persists across different connection attempts.
     */
    val sessionState = _sessionState.asStateFlow()

    private val _session = MutableStateFlow<DanaISession?>(null)
    val session = _session.asStateFlow()

    private val connectionMutex = Mutex()

    /**
     * Ensures a valid connection exists and returns the active client.
     * This is the main entry point for all command executions.
     */
    private suspend fun ensureConnectedClient(): PumpClient {
        connect()
        return getConnectedClientOrThrow()
    }

    /**
     * Synchronized method to initiate a new session if no active one exists.
     */
    private suspend fun startConnection() {
        // If the link is currently disconnecting, we must wait for it to finish
        // before we can reliably start a new connection attempt.
        status.first { it !is LinkStatus.Disconnecting }

        connectionMutex.withLock {
            val currentSessionState = _session.value?.state

            // Return if session is already active or in progress (i.e. state is not terminal)
            if (_session.value != null && currentSessionState?.isClosed == false) {
                return@withLock
            }

            // Clear previous error and set state to Connecting immediately
            _lastError.value = null
            _status.value = LinkStatus.Connecting
            _sessionState.value = SessionState.Connecting

            lateinit var newSession: DanaISession
            newSession = DanaISession(
                device = device,
                deviceName = deviceName,
                txUuid = txUuid,
                rxUuid = rxUuid,
                preferences = preferences,
                logger = logger,
                context = context,
                onSessionStateChanged = { sState -> handleSessionStateChange(newSession, sState) }
            )
            _session.value = newSession

            // Start the actual connection and handshake process
            scope.launch { newSession.runLifecycle() }
        }
    }

    /**
     * Handles state changes of a specific session and maps them to the global link state.
     */
    private fun handleSessionStateChange(session: DanaISession, sState: SessionState) {
        scope.launch {
            connectionMutex.withLock {
                // Only allow the "active" session to update the global link status.
                // This prevents old, terminating sessions from interfering with new attempts.
                if (_session.value != session) return@withLock

                _sessionState.value = sState

                if (sState.isClosed) {
                    if (sState is SessionState.Failed) {
                        _lastError.value = LinkError(sState.message)
                    }
                    _status.value = LinkStatus.Idle
                    _session.value = null
                } else {
                    // Map non-terminal states
                    _status.value = when (sState) {
                        is SessionState.Connecting,
                        is SessionState.Authenticating,
                        is SessionState.Pairing -> LinkStatus.Connecting
                        is SessionState.Connected -> LinkStatus.Connected
                        is SessionState.Disconnecting -> LinkStatus.Disconnecting
                        else -> _status.value
                    }
                }
            }
        }
    }

    /**
     * Extracts the active client from the current session or throws an exception if not available.
     */
    private fun getConnectedClientOrThrow(): PumpClient {
        val currentSession = _session.value ?: throw PumpConnectionException("No active session")
        val state = currentSession.state
        if (state !is SessionState.Connected) {
            throw PumpConnectionException("Session not connected (Current state: $state)")
        }
        return state.client
    }

    /**
     * Suspends until the link is fully connected.
     *
     * This method initiates a connection if necessary and waits until the state
     * becomes [LinkStatus.Connected]. If the connection fails or is interrupted,
     * an [Exception] is thrown.
     *
     * This function is cancelable. If the calling coroutine is canceled, the
     * waiting process stops immediately while the connection process continues.
     *
     * @throws Exception if the connection fails or reaches a terminal error state.
     */
    suspend fun connect() {
        startConnection()

        // Wait until the link state reaches a stable state (not busy anymore)
        val finalState = status.first { !it.isBusy }

        if (finalState !is LinkStatus.Connected) {
            val message = _lastError.value?.message ?: "Connection interrupted"
            throw PumpConnectionException(message)
        }
    }

    /**
     * Manually closes the active session and releases all resources.
     *
     * The link state will transition to [LinkStatus.Disconnecting] and finally to [LinkStatus.Idle].
     */
    suspend fun disconnect() {
        // We do not hold the connectionMutex during the entire disconnect process.
        // Otherwise, bridgeSessionState would be unable to update the link status
        // to 'Disconnecting' because it also requires the lock for status updates.
        val currentSession = _session.value
        currentSession?.disconnect()
    }

    /**
     * Executes a single command on the pump.
     *
     * If no active session exists, this method will automatically attempt to connect first.
     * It suspends until the command is completed or a timeout occurs.
     *
     * @param command The command to send.
     * @return The response from the pump.
     * @throws Exception if the connection fails or the command execution times out.
     */
    suspend fun <R : PumpResponse> execute(command: PumpCommand<R>): R {
        val client = ensureConnectedClient()

        logger.log("Sending command: ${command.javaClass.simpleName}")
        return client.execute(command, timeout = 20.seconds)
    }

    /**
     * Executes a stream command (e.g., history download) on the pump.
     *
     * If no active session exists, this method will automatically attempt to connect first.
     * It suspends until the entire stream operation is completed.
     *
     * @param command The stream command to send.
     * @return The aggregated result of the stream.
     * @throws Exception if the connection fails or the operation is interrupted.
     */
    suspend fun <C : PumpResponse, R> executeStream(command: PumpStreamCommand<C, R>): R {
        val client = ensureConnectedClient()

        return client.executeStream(command, timeout = 60.seconds)
    }

    companion object {
        const val DEFAULT_TX_UUID = "0000fff2-0000-1000-8000-00805f9b34fb"
        const val DEFAULT_RX_UUID = "0000fff1-0000-1000-8000-00805f9b34fb"
    }
}