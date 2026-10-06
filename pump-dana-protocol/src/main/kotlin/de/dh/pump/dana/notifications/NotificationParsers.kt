package de.dh.pump.dana.notifications

import de.dh.pump.dana.commands.requireRemainingAtLeast
import de.dh.pump.dana.protocol.DanaRsBleEncryption
import de.dh.pump.protocol.ByteReader
import de.dh.pump.client.singleframe.ProtocolFrame

/**
 * Decoders for DanaRS/Dana-i notification packets.
 */
object NotificationParsers {
    /**
     * Entry point to parse an incoming protocol frame into a typed Dana notification.
     * Returns null if the frame is not a notification or the opcode is unknown.
     */
    fun parse(frame: ProtocolFrame): DanaNotification? {
        if (frame.flags != DanaRsBleEncryption.DANAR_PACKET__TYPE_NOTIFY) {
            return null
        }

        val reader = ByteReader(frame.payload)
        return when (frame.commandId.value) {
            DanaRsBleEncryption.DANAR_PACKET__OPCODE_NOTIFY__ALARM ->
                parseAlarm(reader)

            DanaRsBleEncryption.DANAR_PACKET__OPCODE_NOTIFY__DELIVERY_COMPLETE ->
                parseDeliveryComplete(reader)

            DanaRsBleEncryption.DANAR_PACKET__OPCODE_NOTIFY__DELIVERY_RATE_DISPLAY ->
                parseDeliveryRateDisplay(reader)

            DanaRsBleEncryption.DANAR_PACKET__OPCODE_NOTIFY__MISSED_BOLUS_ALARM ->
                parseMissedBolusAlarm(reader)

            else -> null
        }
    }

    private fun parseAlarm(reader: ByteReader): DanaAlarmNotification {
        reader.requireRemainingAtLeast(1, "NOTIFY__ALARM")
        val alarmCode = reader.readUInt8()
        return DanaAlarmNotification(alarmCode = DanaAlarmCode.fromCode(alarmCode))
    }

    private fun parseDeliveryComplete(reader: ByteReader): DanaDeliveryCompleteNotification {
        reader.requireRemainingAtLeast(2, "NOTIFY__DELIVERY_COMPLETE")
        val deliveredInsulinUnits = reader.readUInt16Le() / 100.0
        return DanaDeliveryCompleteNotification(deliveredInsulinUnits = deliveredInsulinUnits)
    }

    private fun parseDeliveryRateDisplay(reader: ByteReader): DanaDeliveryRateDisplayNotification {
        reader.requireRemainingAtLeast(2, "NOTIFY__DELIVERY_RATE_DISPLAY")
        val deliveredInsulinUnits = reader.readUInt16Le() / 100.0
        return DanaDeliveryRateDisplayNotification(deliveredInsulinUnits = deliveredInsulinUnits)
    }

    private fun parseMissedBolusAlarm(reader: ByteReader): DanaMissedBolusAlarmNotification {
        reader.requireRemainingAtLeast(4, "NOTIFY__MISSED_BOLUS_ALARM")
        val startHour = reader.readUInt8()
        val startMinute = reader.readUInt8()
        val endHour = reader.readUInt8()
        val endMinute = reader.readUInt8()
        return DanaMissedBolusAlarmNotification(
            startHour = startHour,
            startMinute = startMinute,
            endHour = endHour,
            endMinute = endMinute
        )
    }
}