package de.dh.pump.omnipod.protocol.transport

import java.nio.ByteBuffer

data class MessagePacket(
    val type: MessageType,
    val source: Id,
    val destination: Id,
    val payload: ByteArray,
    val sequenceNumber: Byte,
    val ack: Boolean = false,
    val ackNumber: Byte = 0.toByte(),
    val eqos: Short = 0.toShort(),
    val priority: Boolean = false,
    val lastMessage: Boolean = false,
    val gateway: Boolean = false,
    val sas: Boolean = true,
    val tfs: Boolean = false,
    val version: Short = 0.toShort()
) {

    fun asByteArray(forEncryption: Boolean = false): ByteArray {
        val bb = ByteBuffer.allocate(16 + payload.size)
        bb.put(MAGIC_PATTERN.toByteArray())

        val f1 = Flag()
        f1.set(0, this.version.toInt() and 4 != 0)
        f1.set(1, this.version.toInt() and 2 != 0)
        f1.set(2, this.version.toInt() and 1 != 0)
        f1.set(3, this.sas)
        f1.set(4, this.tfs)
        f1.set(5, this.eqos.toInt() and 4 != 0)
        f1.set(6, this.eqos.toInt() and 2 != 0)
        f1.set(7, this.eqos.toInt() and 1 != 0)

        val f2 = Flag()
        f2.set(0, this.ack)
        f2.set(1, this.priority)
        f2.set(2, this.lastMessage)
        f2.set(3, this.gateway)
        f2.set(4, this.type.value.toInt() and 8 != 0)
        f2.set(5, this.type.value.toInt() and 4 != 0)
        f2.set(6, this.type.value.toInt() and 2 != 0)
        f2.set(7, this.type.value.toInt() and 1 != 0)

        bb.put(f1.value.toByte())
        bb.put(f2.value.toByte())
        bb.put(this.sequenceNumber)
        bb.put(this.ackNumber)
        val size = payload.size -
            if (type == MessageType.ENCRYPTED && !forEncryption) 8 else 0
        bb.put((size ushr 3).toByte())
        bb.put((size shl 5).toByte())

        bb.put(this.source.address)
        bb.put(this.destination.address)

        bb.put(this.payload)

        val ret = ByteArray(bb.position())
        bb.flip()
        bb.get(ret)

        return ret
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MessagePacket

        if (type != other.type) return false
        if (source != other.source) return false
        if (destination != other.destination) return false
        if (!payload.contentEquals(other.payload)) return false
        if (sequenceNumber != other.sequenceNumber) return false
        if (ack != other.ack) return false
        if (ackNumber != other.ackNumber) return false
        if (eqos != other.eqos) return false
        if (priority != other.priority) return false
        if (lastMessage != other.lastMessage) return false
        if (gateway != other.gateway) return false
        if (sas != other.sas) return false
        if (tfs != other.tfs) return false
        if (version != other.version) return false

        return true
    }

    override fun hashCode(): Int {
        var result = type.hashCode()
        result = 31 * result + source.hashCode()
        result = 31 * result + destination.hashCode()
        result = 31 * result + payload.contentHashCode()
        result = 31 * result + sequenceNumber.hashCode()
        result = 31 * result + ack.hashCode()
        result = 31 * result + ackNumber.hashCode()
        result = 31 * result + eqos.hashCode()
        result = 31 * result + priority.hashCode()
        result = 31 * result + lastMessage.hashCode()
        result = 31 * result + gateway.hashCode()
        result = 31 * result + sas.hashCode()
        result = 31 * result + tfs.hashCode()
        result = 31 * result + version.hashCode()
        return result
    }

    companion object {

        private const val MAGIC_PATTERN = "TW"
        private const val HEADER_SIZE = 16

        fun parse(payload: ByteArray): MessagePacket {
            require(payload.size >= HEADER_SIZE) { "Payload too short for MessagePacket header" }

            if (payload.copyOfRange(0, 2).decodeToString() != MAGIC_PATTERN) {
                throw IllegalArgumentException("Could not parse message: invalid magic pattern")
            }
            val f1 = Flag(payload[2].toInt() and 0xff)
            val sas = f1.get(3) != 0
            val tfs = f1.get(4) != 0
            val version = ((f1.get(0) shl 2) or (f1.get(1) shl 1) or (f1.get(2) shl 0)).toShort()
            val eqos = (f1.get(7) or (f1.get(6) shl 1) or (f1.get(5) shl 2)).toShort()

            val f2 = Flag(payload[3].toInt() and 0xff)
            val ack = f2.get(0) != 0
            val priority = f2.get(1) != 0
            val lastMessage = f2.get(2) != 0
            val gateway = f2.get(3) != 0
            val type = MessageType.byValue(
                (f2.get(7) or (f2.get(6) shl 1) or (f2.get(5) shl 2) or (f2.get(4) shl 3)).toByte()
            )
            if (version.toInt() != 0) {
                throw IllegalArgumentException("Unsupported MessagePacket version: $version")
            }
            val sequenceNumber = payload[4]
            val ackNumber = payload[5]
            val size = (payload[6].toInt() and 0xff shl 3) or ((payload[7].toInt() and 0xff) ushr 5)
            require(payload.size >= size + HEADER_SIZE) { "Payload smaller than specified size $size" }

            val payloadEnd = 16 + size +
                if (type == MessageType.ENCRYPTED) 8
                else 0

            return MessagePacket(
                type = type,
                ack = ack,
                eqos = eqos,
                priority = priority,
                lastMessage = lastMessage,
                gateway = gateway,
                sas = sas,
                tfs = tfs,
                version = version,
                sequenceNumber = sequenceNumber,
                ackNumber = ackNumber,
                source = Id(payload.copyOfRange(8, 12)),
                destination = Id(payload.copyOfRange(12, 16)),
                payload = payload.copyOfRange(16, payloadEnd)
            )
        }
    }
}