package de.dh.pump.omnipod.protocol.security

import de.dh.pump.protocol.toHex

data class PairResult(val ltk: ByteArray, val msgSeq: Byte) {
    init {
        require(ltk.size == 16) { "LTK length must be 16 bytes. Received LTK: ${ltk.toHex()}" }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PairResult

        if (!ltk.contentEquals(other.ltk)) return false
        if (msgSeq != other.msgSeq) return false

        return true
    }

    override fun hashCode(): Int {
        var result = ltk.contentHashCode()
        result = 31 * result + msgSeq.hashCode()
        return result
    }
}