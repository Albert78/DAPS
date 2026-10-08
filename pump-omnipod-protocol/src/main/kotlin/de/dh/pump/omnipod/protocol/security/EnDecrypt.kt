package de.dh.pump.omnipod.protocol.security

import de.dh.pump.omnipod.protocol.transport.MessagePacket
import org.bouncycastle.crypto.engines.AESEngine
import org.bouncycastle.crypto.modes.CCMBlockCipher
import org.bouncycastle.crypto.params.AEADParameters
import org.bouncycastle.crypto.params.KeyParameter

@Suppress("DEPRECATION")
class EnDecrypt(private val nonce: Nonce, private val ck: ByteArray) {

    private fun createCipher(): CCMBlockCipher = CCMBlockCipher(AESEngine())

    fun decrypt(msg: MessagePacket): MessagePacket {
        val payload = msg.payload
        val header = msg.asByteArray().copyOfRange(0, 16)

        val n = nonce.increment(false)
        val cipher = createCipher()
        cipher.init(
            false,
            AEADParameters(
                KeyParameter(ck),
                MAC_SIZE * 8, // in bits
                n,
                header
            )
        )
        val decryptedPayload = ByteArray(payload.size - MAC_SIZE)
        cipher.processPacket(payload, 0, payload.size, decryptedPayload, 0)
        return msg.copy(payload = decryptedPayload)
    }

    fun encrypt(headerMessage: MessagePacket): MessagePacket {
        val payload = headerMessage.payload
        val header = headerMessage.asByteArray(true).copyOfRange(0, 16)

        val n = nonce.increment(true)
        val encryptedPayload = ByteArray(payload.size + MAC_SIZE)

        val cipher = createCipher()
        cipher.init(
            true,
            AEADParameters(
                KeyParameter(ck),
                MAC_SIZE * 8, // in bits
                n,
                header
            )
        )
        cipher.processPacket(payload, 0, payload.size, encryptedPayload, 0)

        return headerMessage.copy(payload = encryptedPayload)
    }

    companion object {
        private const val MAC_SIZE = 8
    }
}