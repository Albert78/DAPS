package de.dh.pump.omnipod.protocol.security

sealed class SessionNegotiationResponse

data class SessionKeys(
    val ck: ByteArray,
    val nonce: Nonce,
    var msgSequenceNumber: Byte
) : SessionNegotiationResponse() {

    init {
        require(ck.size == 16) { "CK must be 16 bytes long" }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SessionKeys

        if (!ck.contentEquals(other.ck)) return false
        if (nonce != other.nonce) return false
        if (msgSequenceNumber != other.msgSequenceNumber) return false

        return true
    }

    override fun hashCode(): Int {
        var result = ck.contentHashCode()
        result = 31 * result + nonce.hashCode()
        result = 31 * result + msgSequenceNumber.hashCode()
        return result
    }
}

data class SessionNegotiationResynchronization(
    val synchronizedEapSqn: EapSqn,
    val msgSequenceNumber: Byte
) : SessionNegotiationResponse()