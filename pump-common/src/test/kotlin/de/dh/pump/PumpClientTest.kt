package de.dh.pump

import de.dh.pump.commands.AckResponse
import de.dh.pump.commands.CommandKind
import de.dh.pump.commands.PumpCommand
import de.dh.pump.protocol.ByteReader
import de.dh.pump.protocol.ByteWriter
import de.dh.pump.protocol.CommandId
import de.dh.pump.protocol.ProtocolFrame
import de.dh.pump.protocol.PumpProtocolCodec
import de.dh.pump.transport.BleTransport
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.BUFFERED
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Test
import kotlin.time.Duration

/**
 * Tests showing how to use the generic PumpClient with a custom Transport and Codec.
 */
class PumpClientTest {

    private val testCodec = TestCodec()

    @Test
    fun clientExecutesCommandAndParsesResponse() = runBlocking {
        val fakeTransport = FakeTransport(testCodec)
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val client = PumpClient(fakeTransport, scope, codec = testCodec)

        val command = SimpleCommand("TestCommand", 0x11)

        // Mock the pump response in a background task
        val responseFrame = ProtocolFrame(
            sequence = 1,
            commandId = command.commandId,
            flags = 0,
            payload = byteArrayOf(0x00) // OK status
        )
        val responseBytes = testCodec.encode(responseFrame)

        fakeTransport.onSend = { _ ->
            fakeTransport.emitIncoming(responseBytes)
        }

        val result = client.execute(command)

        Assert.assertEquals(PumpStatus.OK, result.status)
        Assert.assertEquals(0x11, fakeTransport.lastSentOpcode)
    }

    private class SimpleCommand(override val name: String, opcode: Int) : PumpCommand<AckResponse> {
        override val commandId = CommandId(opcode)
        override val kind = CommandKind.READ
        override fun encodePayload(writer: ByteWriter) {}
        override fun decodePayload(reader: ByteReader) =
            AckResponse(if (reader.readUInt8() == 0) PumpStatus.OK else PumpStatus.REJECTED)
    }

    private class TestCodec : PumpProtocolCodec {
        override fun encode(frame: ProtocolFrame): ByteArray {
            return ByteWriter()
                .writeUInt16Le(frame.sequence)
                .writeUInt16Le(frame.commandId.value)
                .writeUInt8(frame.flags)
                .writeBytes(frame.payload)
                .toByteArray()
        }

        override fun decode(bytes: ByteArray): ProtocolFrame {
            val reader = ByteReader(bytes)
            val sequence = reader.readUInt16Le()
            val commandId = CommandId(reader.readUInt16Le())
            val flags = reader.readUInt8()
            val payload = reader.readBytes(bytes.size - 5)
            return ProtocolFrame(sequence, commandId, flags, payload)
        }
    }

    private class FakeTransport(private val codec: PumpProtocolCodec) : BleTransport {
        private val _incoming = Channel<ByteArray>(BUFFERED)
        override val notifications = _incoming

        var lastSentOpcode: Int = -1
        var onSend: ((ByteArray) -> Unit)? = null

        override suspend fun write(bytes: ByteArray) {
            try {
                val frame = codec.decode(bytes)
                lastSentOpcode = frame.commandId.value
            } catch (_: Exception) {}
            onSend?.invoke(bytes)
        }

        fun emitIncoming(bytes: ByteArray) {
            _incoming.trySend(bytes)
        }

        override suspend fun close(timeout: Duration) {}
    }
}