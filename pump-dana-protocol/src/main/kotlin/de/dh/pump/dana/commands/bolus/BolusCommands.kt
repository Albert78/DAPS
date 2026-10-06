package de.dh.pump.dana.commands.bolus

import de.dh.pump.PumpStatus
import de.dh.pump.dana.DanaPumpStatus
import de.dh.pump.dana.commands.DanaGlucoseUnits
import de.dh.pump.dana.commands.DanaRsAckPacketCommand
import de.dh.pump.dana.commands.DanaRsBolusSpeed
import de.dh.pump.dana.commands.DanaRsPacketCommand
import de.dh.pump.dana.commands.DanaRsPacketRegistry
import de.dh.pump.dana.commands.discardRemaining
import de.dh.pump.dana.commands.le16
import de.dh.pump.dana.commands.requireRemainingAtLeast
import de.dh.pump.protocol.ByteReader
import de.dh.pump.protocol.ByteWriter
import java.time.LocalTime
import kotlin.math.roundToInt

class BolusGet24CIRCFArrayCommand :
    DanaRsPacketCommand<Bolus24CirCfArrayResponse>(DanaRsPacketRegistry.BOLUS_GET_24_CIR_CF_ARRAY) {
    override fun decodePayload(reader: ByteReader): Bolus24CirCfArrayResponse {
        reader.requireRemainingAtLeast(97, name)
        val unitsRaw = reader.readUInt8()
        val units = DanaGlucoseUnits.fromWireValue(unitsRaw)
        val cirValues = List(24) { reader.readUInt16Le().toDouble() }
        val cfValues = List(24) {
            val raw = reader.readUInt16Le()
            if (units == DanaGlucoseUnits.MGDL) raw.toDouble() else raw / 100.0
        }
        reader.discardRemaining()
        return Bolus24CirCfArrayResponse(
            status = if (units == DanaGlucoseUnits.UNKNOWN) PumpStatus.INVALID_PARAMETER else PumpStatus.OK,
            units = units,
            valuesByHour = cirValues.zip(cfValues) { cir, cf -> CirCfValue(cir, cf) },
        )
    }
}

class BolusGetBolusOptionCommand :
    DanaRsPacketCommand<BolusOptionResponse>(DanaRsPacketRegistry.BOLUS_GET_BOLUS_OPTION) {
    override fun decodePayload(reader: ByteReader): BolusOptionResponse {
        reader.requireRemainingAtLeast(19, name)
        val extendedBolusEnabled = reader.readUInt8() == 1
        val bolusCalculationOption = reader.readUInt8()
        val missedBolusConfig = reader.readUInt8()
        val windows = List(4) {
            MissedBolusWindow(
                startHour = reader.readUInt8(),
                startMinute = reader.readUInt8(),
                endHour = reader.readUInt8(),
                endMinute = reader.readUInt8(),
            )
        }
        reader.discardRemaining()
        return BolusOptionResponse(
            status = if (extendedBolusEnabled) PumpStatus.OK else PumpStatus.REJECTED,
            extendedBolusEnabled = extendedBolusEnabled,
            bolusCalculationOption = bolusCalculationOption,
            missedBolusConfig = missedBolusConfig,
            missedBolusWindows = windows,
        )
    }
}

class BolusGetCalculationInformationCommand :
    DanaRsPacketCommand<BolusCalculationInformationResponse>(
        DanaRsPacketRegistry.BOLUS_GET_CALCULATION_INFORMATION) {
    override fun decodePayload(reader: ByteReader): BolusCalculationInformationResponse {
        reader.requireRemainingAtLeast(14, name)
        val errorCode = reader.readUInt8()
        var currentBloodGlucose = reader.readUInt16Le().toDouble()
        val carbohydrate = reader.readUInt16Le()
        var currentTarget = reader.readUInt16Le().toDouble()
        val currentCarbRatio = reader.readUInt16Le()
        var currentCorrectionFactor = reader.readUInt16Le().toDouble()
        val insulinOnBoard = reader.readUInt16Le() / 100.0
        val units = DanaGlucoseUnits.fromWireValue(reader.readUInt8())
        if (units == DanaGlucoseUnits.MMOL) {
            currentBloodGlucose /= 100.0
            currentTarget /= 100.0
            currentCorrectionFactor /= 100.0
        }
        reader.discardRemaining()
        return BolusCalculationInformationResponse(
            status = DanaPumpStatus.fromCode(errorCode),
            errorCode = errorCode,
            units = units,
            currentBloodGlucose = currentBloodGlucose,
            carbohydrateGrams = carbohydrate,
            currentTarget = currentTarget,
            currentCarbRatio = currentCarbRatio,
            currentCorrectionFactor = currentCorrectionFactor,
            insulinOnBoardUnits = insulinOnBoard,
        )
    }
}

class BolusGetCIRCFArrayCommand :
    DanaRsPacketCommand<BolusCirCfArrayResponse>(DanaRsPacketRegistry.BOLUS_GET_CIR_CF_ARRAY) {
    override fun decodePayload(reader: ByteReader): BolusCirCfArrayResponse {
        reader.requireRemainingAtLeast(30, name)
        val language = reader.readUInt8()
        val units = DanaGlucoseUnits.fromWireValue(reader.readUInt8())
        val cirValues = List(7) { reader.readUInt16Le() }
        val cfValues = List(7) {
            val raw = reader.readUInt16Le()
            if (units == DanaGlucoseUnits.MGDL) raw.toDouble() else raw / 100.0
        }
        reader.discardRemaining()
        return BolusCirCfArrayResponse(
            status = if (units == DanaGlucoseUnits.UNKNOWN) PumpStatus.INVALID_PARAMETER else PumpStatus.OK,
            language = language,
            units = units,
            cirValues = cirValues,
            cfValues = cfValues,
        )
    }
}

class BolusGetStepBolusInformationCommand :
    DanaRsPacketCommand<BolusStepBolusInformationResponse>(
        DanaRsPacketRegistry.BOLUS_GET_STEP_BOLUS_INFORMATION) {
    override fun decodePayload(reader: ByteReader): BolusStepBolusInformationResponse {
        reader.requireRemainingAtLeast(11, name)
        val errorCode = reader.readUInt8()
        val bolusType = reader.readUInt8()
        val initialBolusAmount = reader.readUInt16Le() / 100.0
        val hour = reader.readUInt8()
        val minute = reader.readUInt8()
        val lastBolusAmount = reader.readUInt16Le() / 100.0
        val maxBolus = reader.readUInt16Le() / 100.0
        val bolusStep = reader.readUInt8() / 100.0
        reader.discardRemaining()
        return BolusStepBolusInformationResponse(
            status = DanaPumpStatus.fromCode(errorCode),
            errorCode = errorCode,
            bolusType = bolusType,
            initialBolusAmountUnits = initialBolusAmount,
            lastBolusTimeOfDayUTC = LocalTime.of(hour, minute),
            lastBolusAmountUnits = lastBolusAmount,
            maxBolusUnits = maxBolus,
            bolusStepUnits = bolusStep,
        )
    }
}

class BolusSetStepBolusStartCommand(
    /**
     * Amount of insulin to deliver in Units (U).
     */
    private val amountUnits: Double,
    /**
     * Speed of bolus delivery.
     */
    private val speed: DanaRsBolusSpeed,
) : DanaRsPacketCommand<BolusStartResponse>(DanaRsPacketRegistry.BOLUS_SET_STEP_BOLUS_START) {
    override fun encodePayload(writer: ByteWriter) {
        // Written as Centi-Units (0.01 U)
        writer.writeBytes(le16((amountUnits * 100.0).roundToInt()))
        writer.writeUInt8(speed.wireValue)
    }

    override fun decodePayload(reader: ByteReader): BolusStartResponse {
        val errorCode = reader.readUInt8()
        return BolusStartResponse(
            status = if (errorCode == 0x00) PumpStatus.OK else PumpStatus.REJECTED,
            errorCode = errorCode,
            bolusStartStatus = BolusStartStatus.fromCode(errorCode),
        )
    }
}

class BolusSetStepBolusStopCommand : DanaRsAckPacketCommand(
    DanaRsPacketRegistry.BOLUS_SET_STEP_BOLUS_STOP)

class BolusSetExtendedBolusCommand(
    /**
     * Total amount of insulin to deliver over the duration in Units (U).
     */
    private val amountUnits: Double,
    /**
     * Duration of delivery in 30-minute increments (e.g., 2 = 1 hour).
     */
    private val durationHalfHours: Int,
) : DanaRsAckPacketCommand(DanaRsPacketRegistry.BOLUS_SET_EXTENDED_BOLUS) {
    override fun encodePayload(writer: ByteWriter) {
        // Written as Centi-Units (0.01 U)
        writer.writeBytes(le16((amountUnits * 100.0).roundToInt()))
        writer.writeUInt8(durationHalfHours)
    }
}

class BolusSetExtendedBolusCancelCommand : DanaRsAckPacketCommand(
    DanaRsPacketRegistry.BOLUS_SET_EXTENDED_BOLUS_CANCEL)

class BolusSet24CIRCFArrayCommand(
    private val ic: IntArray,
    private val cf: IntArray,
) : DanaRsAckPacketCommand(DanaRsPacketRegistry.BOLUS_SET_24_CIR_CF_ARRAY) {
    init {
        require(ic.size == 24 && cf.size == 24) { "Dana CIR/CF array requires 24 IC and 24 CF values" }
    }

    override fun encodePayload(writer: ByteWriter) {
        ic.forEach { writer.writeBytes(le16(it)) }
        cf.forEach { writer.writeBytes(le16(it)) }
    }
}

/**
 * Doesn't seem to work on Dana-i.
 */
class BolusGetBolusRateCommand :
    DanaRsPacketCommand<BolusRateResponse>(DanaRsPacketRegistry.BOLUS_GET_BOLUS_RATE) {
    override fun decodePayload(reader: ByteReader): BolusRateResponse {
        reader.requireRemainingAtLeast(4, name)
        val maxBolus = reader.readUInt16Le() / 100.0
        val bolusStep = reader.readUInt8() / 100.0
        val speedRaw = reader.readUInt8()

        reader.discardRemaining()
        return BolusRateResponse(
            status = PumpStatus.OK,
            maxBolusUnits = maxBolus,
            bolusStepUnits = bolusStep,
            bolusSpeed = DanaRsBolusSpeed.fromWireValue(speedRaw),
        )
    }
}
/**
 * Doesn't seem to work on Dana-i.
 */
class BolusSetBolusRateCommand(
    private val maxBolusUnits: Double,
    private val bolusStepUnits: Double,
    private val speed: DanaRsBolusSpeed,
) : DanaRsAckPacketCommand(DanaRsPacketRegistry.BOLUS_SET_BOLUS_RATE) {
    override fun encodePayload(writer: ByteWriter) {
        writer.writeBytes(le16((maxBolusUnits * 100.0).roundToInt()))
        writer.writeUInt8((bolusStepUnits * 100.0).roundToInt())
        writer.writeUInt8(speed.wireValue)
    }
}

class BolusSetBolusOptionCommand(
    private val extendedBolusEnabled: Boolean,
    private val bolusCalculationOption: Int,
    private val missedBolusConfig: Int,
    private val missedBolusWindows: List<MissedBolusWindow>,
) : DanaRsAckPacketCommand(DanaRsPacketRegistry.BOLUS_SET_BOLUS_OPTION) {
    init {
        require(missedBolusWindows.size == 4) { "Dana bolus options require four missed-bolus windows" }
    }

    override fun encodePayload(writer: ByteWriter) {
        writer.writeUInt8(if (extendedBolusEnabled) 1 else 0)
        writer.writeUInt8(bolusCalculationOption)
        writer.writeUInt8(missedBolusConfig)
        missedBolusWindows.forEach { window ->
            writer.writeUInt8(window.startHour)
            writer.writeUInt8(window.startMinute)
            writer.writeUInt8(window.endHour)
            writer.writeUInt8(window.endMinute)
        }
    }
}