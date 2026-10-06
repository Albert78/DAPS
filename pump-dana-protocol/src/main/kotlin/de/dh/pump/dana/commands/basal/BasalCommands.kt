package de.dh.pump.dana.commands.basal

import de.dh.pump.PumpStatus
import de.dh.pump.dana.commands.DanaRsAckPacketCommand
import de.dh.pump.dana.commands.DanaRsPacketCommand
import de.dh.pump.dana.commands.DanaRsPacketRegistry
import de.dh.pump.dana.commands.discardRemaining
import de.dh.pump.dana.commands.le16
import de.dh.pump.dana.commands.requireRemainingAtLeast
import de.dh.pump.protocol.ByteReader
import de.dh.pump.protocol.ByteWriter
import kotlin.math.roundToInt

class BasalGetBasalRateCommand :
    DanaRsPacketCommand<BasalRateProfileResponse>(DanaRsPacketRegistry.BASAL_GET_BASAL_RATE) {
    override fun decodePayload(reader: ByteReader): BasalRateProfileResponse {
        reader.requireRemainingAtLeast(51, name)
        val maxBasal = reader.readUInt16Le() / 100.0
        val basalStep = reader.readUInt8() / 100.0
        val rates = List(24) { reader.readUInt16Le() / 100.0 }
        reader.discardRemaining()
        return BasalRateProfileResponse(
            status = PumpStatus.OK,
            maxBasalUnitsPerHour = maxBasal,
            basalStepUnits = basalStep,
            hourlyRatesUnits = rates,
            basalStepSupported = basalStep == 0.01,
        )
    }
}

class BasalGetProfileBasalRateCommand(
    /**
     * Index of the basal profile to retrieve (0-3).
     */
    private val profileNumber: Int,
) : DanaRsPacketCommand<BasalProfileBasalRateResponse>(
    DanaRsPacketRegistry.BASAL_GET_PROFILE_BASAL_RATE) {
    override fun encodePayload(writer: ByteWriter) {
        writer.writeUInt8(profileNumber)
    }

    override fun decodePayload(reader: ByteReader): BasalProfileBasalRateResponse {
        reader.requireRemainingAtLeast(48, name)
        val rates = List(24) { reader.readUInt16Le() / 100.0 }
        reader.discardRemaining()
        return BasalProfileBasalRateResponse(
            status = PumpStatus.OK,
            hourlyRatesUnits = rates
        )
    }
}

class BasalGetProfileNumberCommand :
    DanaRsPacketCommand<BasalProfileNumberResponse>(DanaRsPacketRegistry.BASAL_GET_PROFILE_NUMBER) {
    override fun decodePayload(reader: ByteReader): BasalProfileNumberResponse {
        reader.requireRemainingAtLeast(1, name)
        val activeProfile = reader.readUInt8()
        reader.discardRemaining()
        return BasalProfileNumberResponse(
            status = PumpStatus.OK,
            activeProfile = activeProfile,
        )
    }
}

class BasalSetCancelTemporaryBasalCommand : DanaRsAckPacketCommand(
    DanaRsPacketRegistry.BASAL_SET_CANCEL_TEMPORARY_BASAL)

class BasalSetTemporaryBasalCommand(
    /**
     * Basal rate ratio in percent (e.g., 150 = 150%).
     */
    private val ratioPercent: Int,
    /**
     * Duration of temporary basal in full hours.
     */
    private val durationHours: Int,
) : DanaRsAckPacketCommand(DanaRsPacketRegistry.BASAL_SET_TEMPORARY_BASAL) {
    override fun encodePayload(writer: ByteWriter) {
        writer.writeUInt8(ratioPercent)
        writer.writeUInt8(durationHours)
    }
}

class BasalSetProfileNumberCommand(
    /**
     * Index of the basal profile to activate (0-3).
     */
    private val profileNumber: Int,
) : DanaRsAckPacketCommand(DanaRsPacketRegistry.BASAL_SET_PROFILE_NUMBER) {
    override fun encodePayload(writer: ByteWriter) {
        writer.writeUInt8(profileNumber)
    }
}

class BasalSetSuspendOffCommand : DanaRsAckPacketCommand(
    DanaRsPacketRegistry.BASAL_SET_SUSPEND_OFF)

class BasalSetSuspendOnCommand : DanaRsAckPacketCommand(
    DanaRsPacketRegistry.BASAL_SET_SUSPEND_ON)

class BasalSetProfileBasalRateCommand(
    /**
     * Index of the profile to edit (0-3).
     */
    private val profileNumber: Int,
    /**
     * List of 24 hourly rates in Units (U).
     */
    private val hourlyRatesUnits: List<Double>,
) : DanaRsAckPacketCommand(DanaRsPacketRegistry.BASAL_SET_PROFILE_BASAL_RATE) {
    init {
        require(hourlyRatesUnits.size == 24) { "Dana basal profile requires 24 hourly rates" }
    }

    override fun encodePayload(writer: ByteWriter) {
        writer.writeUInt8(profileNumber)
        hourlyRatesUnits.forEach { rate ->
            // Written as Centi-Units (0.01 U)
            writer.writeBytes(le16((rate * 100.0).roundToInt()))
        }
    }
}