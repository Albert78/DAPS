package de.dh.pump.omnipod.protocol.response

import de.dh.pump.omnipod.protocol.definition.AlertType
import de.dh.pump.omnipod.protocol.definition.DeliveryStatus
import de.dh.pump.omnipod.protocol.definition.HasValue
import de.dh.pump.omnipod.protocol.definition.PodStatus
import de.dh.pump.omnipod.protocol.definition.byValue
import java.io.Serializable
import java.nio.ByteBuffer
import java.util.EnumSet
import kotlin.experimental.and

interface Response : Serializable {
    val responseType: ResponseType
    val encoded: ByteArray
}

abstract class ResponseBase(
    override val responseType: ResponseType,
    encoded: ByteArray
) : Response {
    override val encoded: ByteArray = encoded.copyOf(encoded.size)
}

enum class ResponseType(override val value: Byte) : HasValue {
    ACTIVATION_RESPONSE(0x01.toByte()),
    DEFAULT_STATUS_RESPONSE(0x1d.toByte()),
    ADDITIONAL_STATUS_RESPONSE(0x02.toByte()),
    NAK_RESPONSE(0x06.toByte()),
    UNKNOWN(0xff.toByte());

    enum class StatusResponseType(override val value: Byte) : HasValue {
        DEFAULT_STATUS_RESPONSE(0x00.toByte()),
        STATUS_RESPONSE_PAGE_1(0x01.toByte()),
        ALARM_STATUS(0x02.toByte()),
        UNKNOWN(0xff.toByte());
    }

    enum class ActivationResponseType(override val value: Byte) : HasValue {
        GET_VERSION_RESPONSE(0x15.toByte()),
        SET_UNIQUE_ID_RESPONSE(0x1b.toByte()),
        UNKNOWN(0xff.toByte());
    }
}

abstract class ActivationResponseBase(
    val activationResponseType: ResponseType.ActivationResponseType,
    encoded: ByteArray
) : ResponseBase(ResponseType.ACTIVATION_RESPONSE, encoded)

object AlertUtil {
    fun decodeAlertSet(encoded: Byte): EnumSet<AlertType> {
        val encodedInt = encoded.toInt() and 0xff
        val alertList = AlertType.entries
            .filter { it != AlertType.UNKNOWN }
            .filter { (it.value.toInt() and 0xff) and encodedInt != 0 }
            .toList()
        return if (alertList.isEmpty()) {
            EnumSet.noneOf(AlertType::class.java)
        } else {
            EnumSet.copyOf(alertList)
        }
    }
}

class DefaultStatusResponse(
    encoded: ByteArray
) : ResponseBase(ResponseType.DEFAULT_STATUS_RESPONSE, encoded) {
    val messageType: Byte = encoded[0]
    private val first4bytes = ByteBuffer.wrap(byteArrayOf(encoded[2], encoded[3], encoded[4], encoded[5])).int
    private val last4bytes = ByteBuffer.wrap(byteArrayOf(encoded[6], encoded[7], encoded[8], encoded[9])).int

    val podStatus: PodStatus = byValue((encoded[1] and 0x0f), PodStatus.UNKNOWN)
    val deliveryStatus: DeliveryStatus = byValue(((encoded[1].toInt() and 0xff) shr 4 and 0x0f).toByte(), DeliveryStatus.UNKNOWN)

    val totalPulsesDelivered: Short = (first4bytes ushr 11 ushr 4 and 0x1FFF).toShort()
    val sequenceNumberOfLastProgrammingCommand: Short = (first4bytes ushr 11 and 0X0F).toShort()
    val bolusPulsesRemaining: Short = (first4bytes and 0X7FF).toShort()

    val activeAlerts: EnumSet<AlertType> = AlertUtil.decodeAlertSet((last4bytes ushr 10 ushr 13 and 0xFF).toByte())
    val minutesSinceActivation: Short = (last4bytes ushr 10 and 0x1FFF).toShort()
    val reservoirPulsesRemaining: Short = (last4bytes and 0X3FF).toShort()
}

class VersionResponse(
    encoded: ByteArray
) : ActivationResponseBase(ResponseType.ActivationResponseType.GET_VERSION_RESPONSE, encoded) {
    val messageType: Byte = encoded[0]
    val messageLength: Short = (encoded[1].toInt() and 0xff).toShort()
    val firmwareVersionMajor: Short = (encoded[2].toInt() and 0xff).toShort()
    val firmwareVersionMinor: Short = (encoded[3].toInt() and 0xff).toShort()
    val firmwareVersionInterim: Short = (encoded[4].toInt() and 0xff).toShort()
    val bleVersionMajor: Short = (encoded[5].toInt() and 0xff).toShort()
    val bleVersionMinor: Short = (encoded[6].toInt() and 0xff).toShort()
    val bleVersionInterim: Short = (encoded[7].toInt() and 0xff).toShort()
    val productId: Short = (encoded[8].toInt() and 0xff).toShort()
    val podStatus: PodStatus = byValue((encoded[9] and 0xf), PodStatus.UNKNOWN)
    val lotNumber: Long = ByteBuffer.wrap(byteArrayOf(0, 0, 0, 0, encoded[10], encoded[11], encoded[12], encoded[13])).long
    val podSequenceNumber: Long = ByteBuffer.wrap(byteArrayOf(0, 0, 0, 0, encoded[14], encoded[15], encoded[16], encoded[17])).long
    val rssi: Byte = (encoded[18] and 0x3f)
    val uniqueIdReceivedInCommand: Long = ByteBuffer.wrap(byteArrayOf(0, 0, 0, 0, encoded[19], encoded[20], encoded[21], encoded[22])).long
}

class SetUniqueIdResponse(
    encoded: ByteArray
) : ActivationResponseBase(ResponseType.ActivationResponseType.SET_UNIQUE_ID_RESPONSE, encoded) {
    val messageType: Byte = encoded[0]
    val messageLength: Short = (encoded[1].toInt() and 0xff).toShort()
    val pulseVolumeInTenThousandthMicroLiter: Short = ByteBuffer.wrap(byteArrayOf(encoded[2], encoded[3])).short
    val pumpRate: Short = (encoded[4].toInt() and 0xff).toShort()
    val primePumpRate: Short = (encoded[5].toInt() and 0xff).toShort()
    val podStatus: PodStatus = byValue(encoded[16], PodStatus.UNKNOWN)
    val lotNumber: Long = ByteBuffer.wrap(byteArrayOf(0, 0, 0, 0, encoded[17], encoded[18], encoded[19], encoded[20])).long
    val podSequenceNumber: Long = ByteBuffer.wrap(byteArrayOf(0, 0, 0, 0, encoded[21], encoded[22], encoded[23], encoded[24])).long
    val uniqueIdReceivedInCommand: Long = ByteBuffer.wrap(byteArrayOf(0, 0, 0, 0, encoded[25], encoded[26], encoded[27], encoded[28])).long
}