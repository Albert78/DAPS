package de.dh.pump.omnipod.protocol.security

import java.security.SecureRandom

class RandomByteGenerator {

    private val secureRandom = SecureRandom()

    fun nextBytes(length: Int): ByteArray = ByteArray(length).also(secureRandom::nextBytes)
}