package de.dh.pump.dana.commands.options

import de.dh.pump.PumpStatus
import de.dh.pump.dana.commands.DanaGlucoseUnits
import de.dh.pump.dana.commands.DanaRsAckPacketCommand
import de.dh.pump.dana.commands.DanaRsPacketCommand
import de.dh.pump.dana.commands.DanaRsPacketRegistry
import de.dh.pump.dana.commands.discardRemaining
import de.dh.pump.dana.commands.encodeDanaDateTime
import de.dh.pump.dana.commands.encodeDanaUtcDateTime
import de.dh.pump.dana.commands.readDanaLocalDateTime
import de.dh.pump.dana.commands.readSignedInt8
import de.dh.pump.dana.commands.requireRemainingAtLeast
import de.dh.pump.protocol.ByteReader
import de.dh.pump.protocol.ByteWriter
import de.dh.daps.common.model.data.PumpTimestamp
import java.time.ZoneId

/**
 * Doesn't seem to work on Dana-i.
 */
class OptionGetPumpTimeCommand :
    DanaRsPacketCommand<OptionPumpTimeResponse>(DanaRsPacketRegistry.OPTION_GET_PUMP_TIME) {
    override fun decodePayload(reader: ByteReader): OptionPumpTimeResponse {
        reader.requireRemainingAtLeast(6, name)
        val pumpTime = reader.readDanaLocalDateTime()
        reader.discardRemaining()
        return OptionPumpTimeResponse(
            status = PumpStatus.OK,
            pumpTime = pumpTime,
        )
    }
}

class OptionGetPumpUtcAndTimeZoneCommand :
    DanaRsPacketCommand<OptionPumpUtcAndTimeZoneResponse>(DanaRsPacketRegistry.OPTION_GET_PUMP_UTC_AND_TIME_ZONE) {
    override fun decodePayload(reader: ByteReader): OptionPumpUtcAndTimeZoneResponse {
        reader.requireRemainingAtLeast(7, name)
        val pumpUtcTime = reader.readDanaLocalDateTime()
        val zoneOffset = reader.readSignedInt8()
        reader.discardRemaining()
        return OptionPumpUtcAndTimeZoneResponse(
            status = PumpStatus.OK,
            pumpUtcTime = pumpUtcTime,
            zoneOffsetHours = zoneOffset,
        )
    }
}

class OptionGetUserOptionCommand :
    DanaRsPacketCommand<OptionUserOptionsResponse>(DanaRsPacketRegistry.OPTION_GET_USER_OPTION) {
    override fun decodePayload(reader: ByteReader): OptionUserOptionsResponse {
        reader.requireRemainingAtLeast(18, name)
        val timeDisplayType24 = reader.readUInt8() == 0
        val buttonScrollOnOff = reader.readUInt8() == 1
        val beepAndAlarm = reader.readUInt8()
        val lcdOnTimeSec = reader.readUInt8()
        val backlightOnTimeSec = reader.readUInt8()
        val selectedLanguage = reader.readUInt8()
        val units = DanaGlucoseUnits.fromWireValue(reader.readUInt8())
        val shutdownHour = reader.readUInt8()
        val lowReservoirRate = reader.readUInt8()
        val cannulaVolume = reader.readUInt16Le()
        val refillAmount = reader.readUInt16Le()
        val selectableLanguages = List(5) { reader.readUInt8() }
        val target = if (reader.remaining >= 2) reader.readUInt16Le() else null
        reader.discardRemaining()
        return OptionUserOptionsResponse(
            status = if (lcdOnTimeSec >= 5) PumpStatus.OK else PumpStatus.INVALID_PARAMETER,
            timeDisplayType24 = timeDisplayType24,
            buttonScrollOnOff = buttonScrollOnOff,
            beepAndAlarm = beepAndAlarm,
            lcdOnTimeSec = lcdOnTimeSec,
            backlightOnTimeSec = backlightOnTimeSec,
            selectedLanguage = selectedLanguage,
            units = units,
            shutdownHour = shutdownHour,
            lowReservoirRate = lowReservoirRate,
            cannulaVolume = cannulaVolume,
            refillAmount = refillAmount,
            selectableLanguages = selectableLanguages,
            target = target,
        )
    }
}

/**
 * Doesn't seem to work on Dana-i.
 */
class OptionSetPumpTimeCommand(
    private val time: PumpTimestamp,
    private val zoneId: ZoneId = ZoneId.systemDefault(),
) : DanaRsAckPacketCommand(DanaRsPacketRegistry.OPTION_SET_PUMP_TIME) {
    override fun encodePayload(writer: ByteWriter) {
        writer.writeBytes(
            encodeDanaDateTime(
                time,
                zoneId
            )
        )
    }
}

class OptionSetPumpUtcAndTimeZoneCommand(
    /**
     * Time on the pump's clock.
     */
    private val time: PumpTimestamp,

    /**
     * Current time offset to UTC of the given time, in the time zone which is configured in the pump.
     */
    private val zoneOffset: Int,
) : DanaRsAckPacketCommand(DanaRsPacketRegistry.OPTION_SET_PUMP_UTC_AND_TIME_ZONE) {
    override fun encodePayload(writer: ByteWriter) {
        writer.writeBytes(
            encodeDanaUtcDateTime(
                time
            )
        )
        writer.writeBytes(byteArrayOf(zoneOffset.toByte()))
    }
}

class OptionSetUserOptionCommand(
    /**
     * Set of user options including display, alerts, and thresholds.
     */
    private val options: DanaRsUserOptions,
) : DanaRsAckPacketCommand(DanaRsPacketRegistry.OPTION_SET_USER_OPTION) {
    override fun encodePayload(writer: ByteWriter) {
        // 0 = 24h, 1 = 12h
        writer.writeUInt8(if (options.timeDisplayType24) 0 else 1)
        // 0 = Off, 1 = On
        writer.writeUInt8(if (options.buttonScrollOnOff) 1 else 0)
        // Level 1-5
        writer.writeUInt8(options.beepAndAlarm)
        // Time in seconds
        writer.writeUInt8(options.lcdOnTimeSec)
        // Time in seconds
        writer.writeUInt8(options.backlightOnTimeSec)
        // Language ID
        writer.writeUInt8(options.selectedLanguage)
        // 0 = mg/dL, 1 = mmol/L
        writer.writeUInt8(options.units.wireValue)
        // Time in hours
        writer.writeUInt8(options.shutdownHour)
        // Amount in Units (U)
        writer.writeUInt8(options.lowReservoirRate)
        // Amount in Centi-Units (0.01 U)
        writer.writeUInt8(options.cannulaVolume and 0xff)
        writer.writeUInt8((options.cannulaVolume ushr 8) and 0xff)
        // Amount in Units (U)
        writer.writeUInt8(options.refillAmount and 0xff)
        writer.writeUInt8((options.refillAmount ushr 8) and 0xff)
        if (options.hwModel >= 7) {
            // Amount in Units (depends on [units] setting)
            writer.writeUInt8(options.target and 0xff)
            writer.writeUInt8((options.target ushr 8) and 0xff)
        }
    }
}