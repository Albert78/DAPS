package de.dh.pump.client.singleframe

/**
 * Generic request/response envelope used by single-frame synchronous clients.
 *
 * Real pump modules may map this model to a very different on-wire packet format. The important
 * contract for [PumpClient] is that the decoded frame exposes a
 * sequence number and command id so a notification can be correlated with the command that caused it.
 */
data class ProtocolFrame(
    val sequence: Int,
    val commandId: CommandId,
    val flags: Int,
    val payload: ByteArray,
) {
    override fun equals(other: Any?): Boolean {
        return other is ProtocolFrame &&
            sequence == other.sequence &&
            commandId == other.commandId &&
            flags == other.flags &&
            payload.contentEquals(other.payload)
    }

    override fun hashCode(): Int {
        var result = sequence
        result = 31 * result + commandId.hashCode()
        result = 31 * result + flags
        result = 31 * result + payload.contentHashCode()
        return result
    }
}