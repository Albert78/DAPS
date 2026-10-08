package de.dh.pump.omnipod.protocol.security

import org.bouncycastle.math.ec.rfc7748.X25519
import java.security.SecureRandom

class X25519KeyGenerator(private val secureRandom: SecureRandom = SecureRandom()) {

    fun generatePrivateKey(): ByteArray {
        val privateKey = ByteArray(X25519.POINT_SIZE)
        X25519.generatePrivateKey(secureRandom, privateKey)
        return privateKey
    }

    fun publicFromPrivate(privateKey: ByteArray): ByteArray {
        val publicKey = ByteArray(X25519.POINT_SIZE)
        X25519.generatePublicKey(privateKey, 0, publicKey, 0)
        return publicKey
    }

    fun computeSharedSecret(privateKey: ByteArray, publicKey: ByteArray): ByteArray {
        val secret = ByteArray(X25519.POINT_SIZE)
        X25519.calculateAgreement(privateKey, 0, publicKey, 0, secret, 0)
        return secret
    }
}