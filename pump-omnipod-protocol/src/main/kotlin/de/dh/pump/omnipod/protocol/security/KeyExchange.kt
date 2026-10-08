package de.dh.pump.omnipod.protocol.security

import de.dh.pump.protocol.toHex
import org.bouncycastle.crypto.engines.AESEngine
import org.bouncycastle.crypto.macs.CMac
import org.bouncycastle.crypto.params.KeyParameter

class KeyExchange(
    private val x25519: X25519KeyGenerator = X25519KeyGenerator(),
    randomByteGenerator: RandomByteGenerator = RandomByteGenerator()
) {

    val pdmNonce: ByteArray = randomByteGenerator.nextBytes(NONCE_SIZE)
    val pdmPrivate: ByteArray = x25519.generatePrivateKey()
    val pdmPublic: ByteArray = x25519.publicFromPrivate(pdmPrivate)

    var podPublic: ByteArray = ByteArray(PUBLIC_KEY_SIZE)
        private set
    var podNonce: ByteArray = ByteArray(NONCE_SIZE)
        private set

    val podConf = ByteArray(CMAC_SIZE)
    val pdmConf = ByteArray(CMAC_SIZE)
    var ltk = ByteArray(CMAC_SIZE)
        private set

    fun updatePodPublicData(payload: ByteArray) {
        require(payload.size == PUBLIC_KEY_SIZE + NONCE_SIZE) { "Invalid payload size for pod public data" }
        podPublic = payload.copyOfRange(0, PUBLIC_KEY_SIZE)
        podNonce = payload.copyOfRange(PUBLIC_KEY_SIZE, PUBLIC_KEY_SIZE + NONCE_SIZE)
        generateKeys()
    }

    fun validatePodConf(payload: ByteArray) {
        require(podConf.contentEquals(payload)) {
            "Received invalid podConf. Expected: ${podConf.toHex()}. Got: ${payload.toHex()}"
        }
    }

    private fun generateKeys() {
        val curveLTK = x25519.computeSharedSecret(pdmPrivate, podPublic)

        val firstKey = podPublic.copyOfRange(podPublic.size - 4, podPublic.size) +
            pdmPublic.copyOfRange(pdmPublic.size - 4, pdmPublic.size) +
            podNonce.copyOfRange(podNonce.size - 4, podNonce.size) +
            pdmNonce.copyOfRange(pdmNonce.size - 4, pdmNonce.size)

        val intermediateKey = ByteArray(CMAC_SIZE)
        aesCmac(firstKey, curveLTK, intermediateKey)

        val ltkData = byteArrayOf(2.toByte()) +
            INTERMEDIARY_KEY_MAGIC_STRING +
            podNonce +
            pdmNonce +
            byteArrayOf(0.toByte(), 1.toByte())
        aesCmac(intermediateKey, ltkData, ltk)

        val confData = byteArrayOf(1.toByte()) +
            INTERMEDIARY_KEY_MAGIC_STRING +
            podNonce +
            pdmNonce +
            byteArrayOf(0.toByte(), 1.toByte())
        val confKey = ByteArray(CMAC_SIZE)
        aesCmac(intermediateKey, confData, confKey)

        val pdmConfData = PDM_CONF_MAGIC_PREFIX + pdmNonce + podNonce
        aesCmac(confKey, pdmConfData, pdmConf)

        val podConfData = POD_CONF_MAGIC_PREFIX + podNonce + pdmNonce
        aesCmac(confKey, podConfData, podConf)
    }

    companion object {
        private const val PUBLIC_KEY_SIZE = 32
        private const val NONCE_SIZE = 16
        const val CMAC_SIZE = 16

        private val INTERMEDIARY_KEY_MAGIC_STRING = "TWIt".toByteArray()
        private val PDM_CONF_MAGIC_PREFIX = "KC_2_U".toByteArray()
        private val POD_CONF_MAGIC_PREFIX = "KC_2_V".toByteArray()
    }
}

@Suppress("DEPRECATION")
private fun aesCmac(key: ByteArray, data: ByteArray, result: ByteArray) {
    val mac = CMac(AESEngine())
    mac.init(KeyParameter(key))
    mac.update(data, 0, data.size)
    mac.doFinal(result, 0)
}