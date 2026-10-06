package de.dh.pump

import de.dh.pump.commands.PumpCommand
import de.dh.pump.commands.PumpResponse
import de.dh.pump.commands.PumpStreamCommand
import de.dh.pump.protocol.ByteReader
import de.dh.pump.protocol.ByteWriter
import de.dh.pump.protocol.CommandId
import de.dh.pump.protocol.FrameCodec
import de.dh.pump.protocol.ProtocolFrame
import de.dh.pump.protocol.PumpProtocolCodec
import de.dh.pump.transport.BleTransport
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Executes typed pump commands over a byte-oriented BLE transport.
 *
 * The client intentionally knows nothing about a concrete pump model. Command classes encode and
 * decode their own payloads, while the injected [PumpProtocolCodec] handles the outer packet format
 * used by a specific pump family. Requests are serialized because many pump BLE protocols only allow
 * one in-flight command and because command ordering is safety-relevant for write/control actions.
 */
class PumpClient(
    private val transport: BleTransport,
    private val scope: CoroutineScope,
    private val codec: PumpProtocolCodec = FrameCodec,
    private val defaultTimeout: Duration = 5.seconds,
    private val defaultStreamTimeout: Duration = 15.seconds,
) {
    private val transactionMutex = Mutex()
    private var nextSequence = 1

    private val _incomingFrames = MutableSharedFlow<ProtocolFrame>(extraBufferCapacity = 64)

    /**
     * Shared stream of all decoded frames received from the pump.
     */
    val incomingFrames = _incomingFrames.asSharedFlow()

    init {
        // Central collector job that decodes all incoming data
        scope.launch {
            while (true) {
                try {
                    val bytes = transport.notifications.receive()
                    val frames = codec.decodeFrames(bytes)
                    frames.forEach { _incomingFrames.emit(it) }
                } catch (_: Exception) {
                    // Transport closed or error
                    break
                }
            }
        }
    }

    suspend fun <R : PumpResponse> execute(
        command: PumpCommand<R>,
        timeout: Duration = defaultTimeout,
    ): R = transactionMutex.withLock {
        coroutineScope {
            val sequence = consumeSequence()
            val requestPayload = ByteWriter().also(command::encodePayload).toByteArray()
            val requestFrame = ProtocolFrame(
                sequence = sequence,
                commandId = command.commandId,
                flags = REQUEST_FLAGS,
                payload = requestPayload,
            )

            // Subscribe to the flow before writing
            val response = async {
                withTimeout(timeout) {
                    incomingFrames.first { it.matches(sequence, command.commandId) }
                }
            }

            transport.write(codec.encode(requestFrame))
            val responseFrame = response.await()
            val reader = ByteReader(responseFrame.payload)
            command.decodePayload(reader).also {
                reader.requireFullyConsumed(command.name)
            }
        }
    }

    suspend fun <C : PumpResponse, R> executeStream(
        command: PumpStreamCommand<C, R>,
        timeout: Duration = defaultStreamTimeout,
    ): R = transactionMutex.withLock {
        coroutineScope {
            val sequence = consumeSequence()
            val requestPayload = ByteWriter().also(command::encodePayload).toByteArray()
            val requestFrame = ProtocolFrame(
                sequence = sequence,
                commandId = command.commandId,
                flags = REQUEST_FLAGS,
                payload = requestPayload,
            )

            val completion = async {
                try {
                    withTimeout(timeout) {
                        incomingFrames
                            .filter { it.matches(sequence, command.commandId) }
                            .collect { frame ->
                                val reader = ByteReader(frame.payload)
                                val chunk = command.decodeChunk(reader)
                                reader.requireFullyConsumed("${command.name} chunk")
                                command.onChunk(chunk)
                                if (command.isComplete(chunk)) {
                                    // Stop collecting frames for this command
                                    throw StreamCompleteException()
                                }
                            }
                    }
                } catch (e: StreamCompleteException) {
                    // Normal termination
                }
            }

            transport.write(codec.encode(requestFrame))
            completion.await()
            command.result()
        }
    }

    private class StreamCompleteException : CancellationException()

    private companion object {
        const val REQUEST_FLAGS = 0x00
        const val NO_SEQUENCE = 0
    }

    private fun consumeSequence(): Int {
        val current = nextSequence
        nextSequence = if (current == 0xffff) 1 else current + 1
        return current
    }

    private fun ProtocolFrame.matches(sequence: Int, commandId: CommandId): Boolean {
        return (this.sequence == sequence || this.sequence == NO_SEQUENCE) && this.commandId == commandId
    }
}