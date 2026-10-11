package de.dh.pump.omnipod.protocol.response

import de.dh.pump.omnipod.protocol.definition.AlarmType
import de.dh.pump.omnipod.protocol.definition.AlertType
import de.dh.pump.omnipod.protocol.definition.DeliveryStatus
import de.dh.pump.omnipod.protocol.definition.HasValue
import de.dh.pump.omnipod.protocol.definition.NakErrorType
import de.dh.pump.omnipod.protocol.definition.PodConstants
import de.dh.pump.omnipod.protocol.definition.PodStatus
import de.dh.pump.omnipod.protocol.definition.byValue
import java.io.Serializable
import java.nio.ByteBuffer
import java.util.EnumSet
import java.util.Locale
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

abstract class AdditionalStatusResponseBase(
    val statusResponseType: ResponseType.StatusResponseType,
    encoded: ByteArray
) : ResponseBase(ResponseType.ADDITIONAL_STATUS_RESPONSE, encoded)

class NakResponse(
    encoded: ByteArray
) : ResponseBase(ResponseType.NAK_RESPONSE, encoded) {

    val messageType: Byte = encoded[0]
    val messageLength: Short = encoded[1].toShort()
    val nakErrorType: NakErrorType = byValue(encoded[2], NakErrorType.UNKNOWN)
    var alarmType: AlarmType? = null
        private set
    var podStatus: PodStatus? = null
        private set
    var securityNakSyncCount: Short = 0
        private set

    init {
        val byte3 = encoded[3]
        val byte4 = encoded[4]
        if (nakErrorType == NakErrorType.ILLEGAL_SECURITY_CODE) {
            securityNakSyncCount = ((byte3.toInt() shl 8 or (byte4.toInt() and 0xff)).toShort())
            alarmType = null
            podStatus = null
        } else {
            securityNakSyncCount = 0
            alarmType = byValue(byte3, AlarmType.UNKNOWN)
            podStatus = byValue(byte4, PodStatus.UNKNOWN)
        }
    }
}

class AlarmStatusResponse(
    encoded: ByteArray
) : AdditionalStatusResponseBase(ResponseType.StatusResponseType.ALARM_STATUS, encoded) {

    val messageType: Byte = encoded[0]
    val messageLength: Short = (encoded[1].toInt() and 0xff).toShort()
    val additionalStatusResponseType: Byte = encoded[2]
    val podStatus: PodStatus = byValue((encoded[3] and 0x0f), PodStatus.UNKNOWN)
    val deliveryStatus: DeliveryStatus = byValue((encoded[4] and 0x0f), DeliveryStatus.UNKNOWN)
    val bolusPulsesRemaining: Short = (ByteBuffer.wrap(byteArrayOf(encoded[5], encoded[6])).short and 2047)
    val sequenceNumberOfLastProgrammingCommand: Short = (encoded[7] and 0x0f).toShort()
    val totalPulsesDelivered: Short = ByteBuffer.wrap(byteArrayOf(encoded[8], encoded[9])).short
    val alarmType: AlarmType = byValue(encoded[10], AlarmType.UNKNOWN)
    val alarmTime: Short = ByteBuffer.wrap(byteArrayOf(encoded[11], encoded[12])).short
    val reservoirPulsesRemaining: Short = ByteBuffer.wrap(byteArrayOf(encoded[13], encoded[14])).short
    val minutesSinceActivation: Short = ByteBuffer.wrap(byteArrayOf(encoded[15], encoded[16])).short
    val activeAlerts: EnumSet<AlertType> = AlertUtil.decodeAlertSet(encoded[17])
    val occlusionAlarm: Boolean
    val pulseInfoInvalid: Boolean
    val podStatusWhenAlarmOccurred: PodStatus
    val immediateBolusWhenAlarmOccurred: Boolean
    val occlusionType: Byte
    val occurredWhenFetchingImmediateBolusActiveInformation: Boolean
    val rssi: Short
    val receiverLowerGain: Short
    val podStatusWhenAlarmOccurred2: PodStatus
    val returnAddressOfPodAlarmHandlerCaller: Short
    val rawErrorEventByte: Byte = encoded[19]

    init {
        val alarmFlags = encoded[18]
        occlusionAlarm = (alarmFlags.toInt() and 1) == 1
        pulseInfoInvalid = (alarmFlags.toInt() shr 1 and 1) == 1
        val byte19 = encoded[19]
        val byte20 = encoded[20]
        podStatusWhenAlarmOccurred = byValue((byte19 and 0x0f), PodStatus.UNKNOWN)
        immediateBolusWhenAlarmOccurred = (byte19.toInt() shr 4 and 1) == 1
        occlusionType = ((byte19.toInt() shr 5 and 3).toByte())
        occurredWhenFetchingImmediateBolusActiveInformation = (byte19.toInt() shr 7 and 1) == 1
        rssi = (byte20 and 0x3f).toShort()
        receiverLowerGain = ((byte20.toInt() shr 6 and 0x03).toShort())
        podStatusWhenAlarmOccurred2 = byValue((encoded[21] and 0x0f), PodStatus.UNKNOWN)
        returnAddressOfPodAlarmHandlerCaller = ByteBuffer.wrap(byteArrayOf(encoded[22], encoded[23])).short
    }

    val pdmRef: String?
        get() {
            val tt = alarmType.pdmFaultCategory?.tt ?: return null
            val vvv = rawErrorEventByte.toInt() and 0xff
            val alarmMinutes = alarmTime.toInt() and 0xffff
            val activeMinutes = minutesSinceActivation.toInt() and 0xffff
            val hh = (if (alarmMinutes == 0 || alarmMinutes == 0xffff) activeMinutes else alarmMinutes) / 60
            val iii = (totalPulsesDelivered.toInt() * PodConstants.POD_PULSE_BOLUS_UNITS).toInt()
            val rr = (reservoirPulsesRemaining.toInt() * PodConstants.POD_PULSE_BOLUS_UNITS).toInt()
            val fff = alarmType.value.toInt() and 0xff
            return java.lang.String.format(Locale.ROOT, "%02d-%03d%02d-%03d%02d-%03d", tt, vvv, hh, iii, rr, fff)
        }
}