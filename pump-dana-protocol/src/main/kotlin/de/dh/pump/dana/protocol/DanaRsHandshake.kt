package de.dh.pump.dana.protocol

import de.dh.pump.client.singleframe.ProtocolFrame
import java.nio.charset.StandardCharsets

data class DanaRsPairingSecrets(
    val rsV3PairingKey: ByteArray? = null,
    val rsV3RandomPairingKey: ByteArray? = null,
    val rsV3RandomSyncKey: Byte = 0,
    val ble5PairingKey: String? = null,
)

data class DanaRsHandshakeState(
    val encryptionType: DanaRsEncryptionType,
    val hardwareModel: Int? = null,
    val protocol: Int? = null,
    val ble5PairingKeyFromPump: String? = null,
    val connected: Boolean = false,
)

sealed interface DanaRsHandshakeResult {
    data class SendNext(val bytes: ByteArray, val state: DanaRsHandshakeState) : DanaRsHandshakeResult

    // Doesn't seem to be used in DanaI
    data class WaitingForPairing(val state: DanaRsHandshakeState) : DanaRsHandshakeResult
    data class Connected(val state: DanaRsHandshakeState) : DanaRsHandshakeResult
    data class Failed(val reason: String, val state: DanaRsHandshakeState?) : DanaRsHandshakeResult
}

/**
 * Small protocol-only state machine for the DanaRS/Dana-i encryption handshake.
 *
 * This class keeps only the packet decisions: which encryption generation was detected, which stored
 * keys should be installed into [DanaRsPacketCodec], and which handshake packet should be sent next.
 * Logging, preferences, pairing UI, and pump status updates belong in application-level code.
 */
class DanaRsHandshake(
    private val codec: DanaRsPacketCodec,
    private val secrets: DanaRsPairingSecrets = DanaRsPairingSecrets(),
) {
    private var state = DanaRsHandshakeState(DanaRsEncryptionType.ENCRYPTION_DEFAULT)

    fun start(deviceName: String): ByteArray = codec.encodePumpCheck(deviceName)

    fun onFrame(frame: ProtocolFrame): DanaRsHandshakeResult {
        if (frame.flags != DanaRsBleEncryption.DANAR_PACKET__TYPE_ENCRYPTION_RESPONSE.toInt()) {
            return DanaRsHandshakeResult.Failed("Expected Dana encryption response", state)
        }

        return when (frame.commandId.value) {
            DanaRsBleEncryption.DANAR_PACKET__OPCODE_ENCRYPTION__PUMP_CHECK -> processPumpCheck(frame.payload)
            DanaRsBleEncryption.DANAR_PACKET__OPCODE_ENCRYPTION__CHECK_PASSKEY -> processPasskeyCheck(frame.payload)
            DanaRsBleEncryption.DANAR_PACKET__OPCODE_ENCRYPTION__PASSKEY_REQUEST -> processPairingRequest(frame.payload)
            DanaRsBleEncryption.DANAR_PACKET__OPCODE_ENCRYPTION__PASSKEY_RETURN -> processPairingReturn(frame.payload)
            DanaRsBleEncryption.DANAR_PACKET__OPCODE_ENCRYPTION__TIME_INFORMATION -> processTimeInformation(frame.payload)
            DanaRsBleEncryption.DANAR_PACKET__OPCODE_ENCRYPTION__GET_EASY_MENU_CHECK -> {
                DanaRsHandshakeResult.SendNext(codec.encodeTimeInformation(byteArrayOf(0)), state)
            }
            else -> DanaRsHandshakeResult.Failed("Unknown Dana encryption opcode ${frame.commandId.value}", state)
        }
    }

    private fun processPumpCheck(payload: ByteArray): DanaRsHandshakeResult {
        if (payload.contentEquals("OK".toByteArray())) {
            return DanaRsHandshakeResult.Failed("Legacy DanaRS v1 is not supported", state)
        }

        if (payload.size == 7 && payload[0] == 'O'.code.toByte() && payload[1] == 'K'.code.toByte()) {
            // Dana-RS
            state = DanaRsHandshakeState(
                encryptionType = DanaRsEncryptionType.ENCRYPTION_RSv3,
                hardwareModel = payload[3].toInt() and 0xff,
                protocol = payload[5].toInt() and 0xff,
            )
            codec.setEncryptionType(DanaRsEncryptionType.ENCRYPTION_RSv3)
            val pairingKey = secrets.rsV3PairingKey
            val randomPairingKey = secrets.rsV3RandomPairingKey
            return if (pairingKey != null && randomPairingKey != null) {
                codec.setPairingKeys(pairingKey, randomPairingKey, secrets.rsV3RandomSyncKey)
                DanaRsHandshakeResult.SendNext(codec.encodeTimeInformation(byteArrayOf(0)), state)
            } else {
                DanaRsHandshakeResult.SendNext(codec.encodeTimeInformation(byteArrayOf(1)), state)
            }
        }

        if (payload.size == 12 && payload[0] == 'O'.code.toByte() && payload[1] == 'K'.code.toByte()) {
            // Dana-i
            val rawKey = payload.copyOfRange(6, 12)
            // Check if the key from the pump is empty (often 0x00 after battery replacement)
            val pairingKeyFromPump = if (rawKey.all { it == 0.toByte() }) null else String(rawKey, StandardCharsets.UTF_8)

            // Prefer using the stored key
            val pairingKey = secrets.ble5PairingKey?.takeIf { it.isNotBlank() } ?: pairingKeyFromPump

            state = DanaRsHandshakeState(
                encryptionType = DanaRsEncryptionType.ENCRYPTION_BLE5,
                hardwareModel = payload[3].toInt() and 0xff,
                protocol = payload[5].toInt() and 0xff,
                ble5PairingKeyFromPump = pairingKeyFromPump,
            )
            codec.setEncryptionType(DanaRsEncryptionType.ENCRYPTION_BLE5)

            return if (pairingKey != null) {
                codec.setBle5PairingKey(pairingKey.encodeToByteArray())
                DanaRsHandshakeResult.SendNext(codec.encodeTimeInformation(ByteArray(4)), state)
            } else {
                // If no key is available, request pairing (pump displays PIN)
                DanaRsHandshakeResult.SendNext(codec.encodePasskeyRequest(), state)
            }
        }

        val text = String(payload, StandardCharsets.UTF_8)
        return DanaRsHandshakeResult.Failed("Pump check failed: $text", state)
    }

    private fun processPasskeyCheck(payload: ByteArray): DanaRsHandshakeResult {
        return if (payload.firstOrNull() == 0x00.toByte()) {
            DanaRsHandshakeResult.SendNext(codec.encodeTimeInformation(), state)
        } else {
            DanaRsHandshakeResult.SendNext(codec.encodePasskeyRequest(), state)
        }
    }

    private fun processPairingRequest(payload: ByteArray): DanaRsHandshakeResult {
        return if (payload.firstOrNull() == 0x00.toByte()) {
            DanaRsHandshakeResult.WaitingForPairing(state)
        } else {
            DanaRsHandshakeResult.Failed("Dana pairing request was rejected", state)
        }
    }

    private fun processPairingReturn(payload: ByteArray): DanaRsHandshakeResult {
        return if (payload.size >= 2) {
            DanaRsHandshakeResult.SendNext(codec.encodeTimeInformation(), state)
        } else {
            DanaRsHandshakeResult.Failed("Dana pairing return did not contain a key", state)
        }
    }

    private fun processTimeInformation(payload: ByteArray): DanaRsHandshakeResult {
        return when (state.encryptionType) {
            DanaRsEncryptionType.ENCRYPTION_BLE5 -> markConnected()
            DanaRsEncryptionType.ENCRYPTION_RSv3 -> {
                if (payload.firstOrNull() == 0x00.toByte()) markConnected()
                else DanaRsHandshakeResult.SendNext(codec.encodeTimeInformation(byteArrayOf(1)), state)
            }
            DanaRsEncryptionType.ENCRYPTION_DEFAULT -> markConnected()
        }
    }

    private fun markConnected(): DanaRsHandshakeResult.Connected {
        codec.secondLevelEncryptionEnabled = true
        state = state.copy(connected = true)
        return DanaRsHandshakeResult.Connected(state)
    }
}