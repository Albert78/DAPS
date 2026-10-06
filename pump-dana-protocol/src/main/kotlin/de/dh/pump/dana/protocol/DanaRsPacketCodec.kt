package de.dh.pump.dana.protocol

import de.dh.pump.dana.DanaPacketCodec
import de.dh.pump.protocol.CommandId
import de.dh.pump.protocol.ProtocolException
import de.dh.pump.protocol.ProtocolFrame

/**
 * Stateful DanaRS/Dana-i packet codec.
 *
 * BLE notifications may split encrypted Dana packets across several chunks. The chunks are assembled
 * here, then passed through the Dana encryption/checksum implementation. The decoded frame payload
 * contains only command data; the Dana packet type and opcode are mapped to [ProtocolFrame.flags] and
 * [ProtocolFrame.commandId].
 */
class DanaRsPacketCodec(
    val encryption: DanaRsBleEncryption = DanaRsBleEncryption(),
) : DanaPacketCodec {
    private val readBuffer = ArrayList<Byte>(1024)

    /**
     * Enables second-level packet encryption after the DanaRSv3/Dana-i handshake has completed.
     *
     * During the handshake the encryption object still decrypts the normal Dana packet envelope. The
     * additional RSv3/BLE5 transform is only applied to command traffic after the session is connected.
     */
    var secondLevelEncryptionEnabled: Boolean = false

    fun reset() {
        readBuffer.clear()
        secondLevelEncryptionEnabled = false
        encryption.connectionState = 0
        encryption.securityVersion = DanaRsEncryptionType.ENCRYPTION_DEFAULT
    }

    fun setEncryptionType(type: DanaRsEncryptionType) {
        encryption.setEnhancedEncryption(type)
    }

    /**
     * Only used for DanaRS, not used for Dana-i.
     */
    fun setPairingKeys(
        pairingKey: ByteArray,
        randomPairingKey: ByteArray,
        randomSyncKey: Byte,
    ) {
        encryption.setPairingKeys(pairingKey, randomPairingKey, randomSyncKey)
    }

    /**
     * Only used for Dana-i, not used for DanaRS.
     */
    fun setBle5PairingKey(pairingKey: ByteArray) {
        encryption.setBle5Key(pairingKey)
    }

    override fun encode(frame: ProtocolFrame): ByteArray {
        val payload = frame.payload.takeIf { it.isNotEmpty() }
        var bytes = encryption.getEncryptedPacket(
            opcode = frame.commandId.value,
            bytes = payload,
            deviceName = null,
        )
        if (secondLevelEncryptionEnabled) {
            bytes = encryption.encryptSecondLevelPacket(bytes)
        }
        return bytes
    }

    override fun decode(bytes: ByteArray): ProtocolFrame {
        return decodeFrames(bytes).firstOrNull()
            ?: throw ProtocolException("No complete DanaRS packet available")
    }

    override fun decodeFrames(bytes: ByteArray): List<ProtocolFrame> {
        val incoming = if (secondLevelEncryptionEnabled) {
            encryption.decryptSecondLevelPacket(bytes)
        } else {
            bytes
        }
        append(incoming)

        val frames = mutableListOf<ProtocolFrame>()
        while (true) {
            val packet = removeNextPacket() ?: break
            val decrypted = encryption.getDecryptedPacket(packet)
                ?: throw ProtocolException("DanaRS packet checksum or length validation failed")
            if (decrypted.size < 2) {
                throw ProtocolException("DanaRS packet missing type/opcode")
            }

            val packetType = decrypted[0].toInt() and 0xff
            val opcode = decrypted[1].toInt() and 0xff
            val payload = decrypted.copyOfRange(2, decrypted.size)
            frames += ProtocolFrame(
                sequence = 0,
                commandId = CommandId(opcode),
                flags = packetType,
                payload = payload,
            )
        }
        return frames
    }

    fun encodePumpCheck(deviceName: String): ByteArray {
        require(deviceName.length == 10) { "Dana device name must be exactly 10 characters" }
        return encryption.getEncryptedPacket(
            opcode = DanaRsBleEncryption.DANAR_PACKET__OPCODE_ENCRYPTION__PUMP_CHECK,
            bytes = null,
            deviceName = deviceName,
        )
    }

    fun encodePasskeyCheck(pairingKey: ByteArray): ByteArray {
        return encryption.getEncryptedPacket(
            opcode = DanaRsBleEncryption.DANAR_PACKET__OPCODE_ENCRYPTION__CHECK_PASSKEY,
            bytes = pairingKey,
            deviceName = null,
        )
    }

    fun encodePasskeyRequest(): ByteArray {
        return encryption.getEncryptedPacket(
            opcode = DanaRsBleEncryption.DANAR_PACKET__OPCODE_ENCRYPTION__PASSKEY_REQUEST,
            bytes = null,
            deviceName = null,
        )
    }

    fun encodeTimeInformation(params: ByteArray? = null): ByteArray {
        return encryption.getEncryptedPacket(
            opcode = DanaRsBleEncryption.DANAR_PACKET__OPCODE_ENCRYPTION__TIME_INFORMATION,
            bytes = params,
            deviceName = null,
        )
    }

    fun encodeEasyMenuCheck(): ByteArray {
        return encryption.getEncryptedPacket(
            opcode = DanaRsBleEncryption.DANAR_PACKET__OPCODE_ENCRYPTION__GET_EASY_MENU_CHECK,
            bytes = null,
            deviceName = null,
        )
    }

    private fun append(bytes: ByteArray) {
        bytes.forEach { readBuffer += it }
    }

    private fun removeNextPacket(): ByteArray? {
        discardUntilPacketStart()
        if (readBuffer.size < MIN_PACKET_SIZE) return null

        val length = readBuffer[LENGTH_INDEX].toInt() and 0xff
        val totalLength = length + PACKET_OVERHEAD
        if (readBuffer.size < totalLength) return null

        if (!hasValidPacketEnd(totalLength)) {
            readBuffer.clear()
            throw ProtocolException("DanaRS packet end marker not found")
        }

        val packet = ByteArray(totalLength) { index -> readBuffer[index] }
        repeat(totalLength) { readBuffer.removeAt(0) }
        return packet
    }

    private fun discardUntilPacketStart() {
        val start = readBuffer.windowed(2).indexOfFirst { pair ->
            pair[0] == DANA_RS_PACKET_START && pair[1] == DANA_RS_PACKET_START ||
                pair[0] == DANA_I_PACKET_START && pair[1] == DANA_I_PACKET_START
        }
        if (start > 0) {
            repeat(start) { readBuffer.removeAt(0) }
        } else if (start < 0 && readBuffer.size > 1) {
            val keepLast = readBuffer.last()
            readBuffer.clear()
            readBuffer += keepLast
        }
    }

    private fun hasValidPacketEnd(totalLength: Int): Boolean {
        val end0 = readBuffer[totalLength - 2]
        val end1 = readBuffer[totalLength - 1]
        return end0 == DANA_RS_PACKET_END && end1 == DANA_RS_PACKET_END ||
            end0 == DANA_I_PACKET_END && end1 == DANA_I_PACKET_END
    }

    private companion object {
        const val LENGTH_INDEX = 2
        const val MIN_PACKET_SIZE = 7
        const val PACKET_OVERHEAD = 7

        val DANA_RS_PACKET_START: Byte = 0xA5.toByte()
        val DANA_RS_PACKET_END: Byte = 0x5A.toByte()
        val DANA_I_PACKET_START: Byte = 0xAA.toByte()
        val DANA_I_PACKET_END: Byte = 0xEE.toByte()
    }
}

@OptIn(ExperimentalUnsignedTypes::class)
fun DanaRsPacketCodec.getPairingSecrets(): DanaRsPairingSecrets {
    return DanaRsPairingSecrets(
        rsV3PairingKey = encryption.pairingKey?.asByteArray(),
        rsV3RandomPairingKey = encryption.randomPairingKey?.asByteArray(),
        rsV3RandomSyncKey = encryption.randomSyncKey.toByte(),
        ble5PairingKey = encryption.ble5PairingKey?.asByteArray()?.let { String(it) }
    )
}