package de.dh.pump.omnipod.protocol.security

import de.dh.pump.protocol.hexToByteArray
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

class Milenage(
    private val k: ByteArray,
    val sqn: ByteArray,
    randParam: ByteArray? = null,
    val auts: ByteArray = ByteArray(AUTS_SIZE),
    val amf: ByteArray = MILENAGE_AMF
) {

    init {
        require(k.size == KEY_SIZE) { "Milenage key must be $KEY_SIZE bytes long." }
        require(sqn.size == SQN) { "Milenage SQN must be $SQN bytes long." }
        require(auts.size == AUTS_SIZE) { "Milenage AUTS must be $AUTS_SIZE bytes long." }
        require(amf.size == MILENAGE_AMF.size) { "Milenage AMF must be ${MILENAGE_AMF.size} bytes long." }
    }

    private val secretKeySpec = SecretKeySpec(k, "AES")
    private val cipher: Cipher = Cipher.getInstance("AES/ECB/NoPadding").apply {
        init(Cipher.ENCRYPT_MODE, secretKeySpec)
    }

    val rand: ByteArray = randParam ?: ByteArray(KEY_SIZE).also { SecureRandom().nextBytes(it) }

    private val opc = cipher.doFinal(MILENAGE_OP) xor MILENAGE_OP
    private val randOpcEncrypted = cipher.doFinal(rand xor opc)
    private val randOpcEncryptedXorOpc = randOpcEncrypted xor opc
    private val resAkInput = randOpcEncryptedXorOpc.copyOfRange(0, KEY_SIZE).also {
        it[15] = (it[15].toInt() xor 1).toByte()
    }

    private val resAk = cipher.doFinal(resAkInput) xor opc

    val res: ByteArray = resAk.copyOfRange(8, 16)
    private val ak: ByteArray = resAk.copyOfRange(0, 6)

    private val ckInput = ByteArray(KEY_SIZE).also { input ->
        for (i in 0..15) {
            input[(i + 12) % 16] = randOpcEncryptedXorOpc[i]
        }
        input[15] = (input[15].toInt() xor 2).toByte()
    }

    val ck: ByteArray = cipher.doFinal(ckInput) xor opc

    private val sqnAmf = sqn + amf + sqn + amf
    private val sqnAmfXorOpc = sqnAmf xor opc
    private val macAInput = ByteArray(KEY_SIZE).also { input ->
        for (i in 0..15) {
            input[(i + 8) % 16] = sqnAmfXorOpc[i]
        }
    }

    private val macAFull = cipher.doFinal(macAInput xor randOpcEncrypted) xor opc
    private val macA = macAFull.copyOfRange(0, 8)
    val macS: ByteArray = macAFull.copyOfRange(8, 16)

    val autn: ByteArray = (ak xor sqn) + amf + macA

    private val akStarInput = ByteArray(KEY_SIZE).also { input ->
        for (i in 0..15) {
            input[(i + 4) % 16] = randOpcEncryptedXorOpc[i]
        }
        input[15] = (input[15].toInt() xor 8).toByte()
    }

    private val akStarFull = cipher.doFinal(akStarInput) xor opc
    private val akStar = akStarFull.copyOfRange(0, 6)

    private val seqXorAkStar = auts.copyOfRange(0, 6)
    val synchronizationSqn: ByteArray = akStar xor seqXorAkStar
    val receivedMacS: ByteArray = auts.copyOfRange(6, 14)

    companion object {
        val RESYNC_AMF: ByteArray = "0000".hexToByteArray()
        private val MILENAGE_OP = "cdc202d5123e20f62b6d676ac72cb318".hexToByteArray()
        private val MILENAGE_AMF = "b9b9".hexToByteArray()
        const val KEY_SIZE = 16
        const val AUTS_SIZE = 14
        private const val SQN = 6
    }
}

private infix fun ByteArray.xor(other: ByteArray): ByteArray {
    val out = ByteArray(size)
    for (i in indices) out[i] = (this[i].toInt() xor other[i].toInt()).toByte()
    return out
}