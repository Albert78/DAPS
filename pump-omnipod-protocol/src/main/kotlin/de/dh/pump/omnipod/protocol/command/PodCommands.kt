package de.dh.pump.omnipod.protocol.command

import de.dh.pump.omnipod.protocol.command.program.BasalInsulinProgramElement
import de.dh.pump.omnipod.protocol.command.program.BolusShortInsulinProgramElement
import de.dh.pump.omnipod.protocol.command.program.ProgramBasalUtil
import de.dh.pump.omnipod.protocol.command.program.ProgramTempBasalUtil
import de.dh.pump.omnipod.protocol.command.program.ShortInsulinProgramElement
import de.dh.pump.omnipod.protocol.definition.AlertConfiguration
import de.dh.pump.omnipod.protocol.definition.AlertType
import de.dh.pump.omnipod.protocol.definition.BasalProgram
import de.dh.pump.omnipod.protocol.definition.BeepType
import de.dh.pump.omnipod.protocol.definition.Encodable
import de.dh.pump.omnipod.protocol.definition.ProgramReminder
import java.io.Serializable
import java.nio.ByteBuffer
import java.util.Date
import java.util.EnumSet
import kotlin.experimental.and
import kotlin.experimental.xor
import kotlin.math.roundToInt

enum class CommandType(val value: Byte) {
    SET_UNIQUE_ID(0x03.toByte()),
    GET_VERSION(0x07.toByte()),
    GET_STATUS(0x0e.toByte()),
    SILENCE_ALERTS(0x11.toByte()),
    PROGRAM_BASAL(0x13.toByte()),
    PROGRAM_TEMP_BASAL(0x16.toByte()),
    PROGRAM_BOLUS(0x17.toByte()),
    PROGRAM_ALERTS(0x19.toByte()),
    PROGRAM_INSULIN(0x1a.toByte()),
    DEACTIVATE(0x1c.toByte()),
    PROGRAM_BEEPS(0x1e.toByte()),
    STOP_DELIVERY(0x1f.toByte())
}

interface Command : Encodable, Serializable {
    val commandType: CommandType
    val sequenceNumber: Short
}

object MessageUtil {
    private val crc16table = shortArrayOf(
        0, -32763, -32753, 10, -32741, 30, 20, -32751, -32717, 54, 60, -32711, 40, -32723, -32729, 34,
        -32669, 102, 108, -32663, 120, -32643, -32649, 114, 80, -32683, -32673, 90, -32693, 78, 68, -32703,
        -32573, 198, 204, -32567, 216, -32547, -32553, 210, 240, -32523, -32513, 250, -32533, 238, 228, -32543,
        160, -32603, -32593, 170, -32581, 190, 180, -32591, -32621, 150, 156, -32615, 136, -32627, -32633, 130
    )

    fun createCrc(bArr: ByteArray): Short {
        var s: Short = 0
        for (b in bArr) {
            val b2 = (b xor (s and 255).toByte())
            var s2 = b2.toShort()
            if (b2 < 0) {
                s2 = ((b2 and Byte.MAX_VALUE) + 128).toShort()
            }
            s = (((s.toInt() shr 8).toShort() and 255) xor crc16table[s2.toInt() and 0x3f])
        }
        return s
    }

    fun calculateChecksum(bytes: ByteArray): Short {
        var sum: Short = 0
        for (b in bytes) {
            sum = (sum + (b.toInt() and 0xff)).toShort()
        }
        return sum
    }
}

abstract class HeaderEnabledCommand(
    override val commandType: CommandType,
    protected val uniqueId: Int,
    override val sequenceNumber: Short,
    protected val multiCommandFlag: Boolean
) : Command {
    companion object {
        fun appendCrc(command: ByteArray): ByteArray =
            ByteBuffer.allocate(command.size + 2)
                .put(command)
                .putShort(MessageUtil.createCrc(command))
                .array()

        fun encodeHeader(
            uniqueId: Int,
            sequenceNumber: Short,
            length: Short,
            multiCommandFlag: Boolean
        ): ByteArray =
            ByteBuffer.allocate(6)
                .putInt(uniqueId)
                .putShort((sequenceNumber.toInt() and 0x0f shl 10 or length.toInt() or ((if (multiCommandFlag) 1 else 0) shl 15)).toShort())
                .array()
    }
}

abstract class NonceEnabledCommand(
    commandType: CommandType,
    uniqueId: Int,
    sequenceNumber: Short,
    multiCommandFlag: Boolean,
    protected val nonce: Int
) : HeaderEnabledCommand(commandType, uniqueId, sequenceNumber, multiCommandFlag)

class GetStatusCommand(
    uniqueId: Int,
    sequenceNumber: Short,
    val responseType: Byte = 0
) : HeaderEnabledCommand(CommandType.GET_STATUS, uniqueId, sequenceNumber, false) {
    override val encoded: ByteArray
        get() {
            val body = byteArrayOf(commandType.value, 1, responseType)
            val header = encodeHeader(uniqueId, sequenceNumber, body.size.toShort(), multiCommandFlag)
            return appendCrc(header + body)
        }
}

class GetVersionCommand(
    uniqueId: Int,
    sequenceNumber: Short
) : HeaderEnabledCommand(CommandType.GET_VERSION, uniqueId, sequenceNumber, false) {
    override val encoded: ByteArray
        get() {
            val body = byteArrayOf(commandType.value, 0)
            val header = encodeHeader(uniqueId, sequenceNumber, body.size.toShort(), multiCommandFlag)
            return appendCrc(header + body)
        }

    companion object {
        const val DEFAULT_UNIQUE_ID = 0xffffffff.toInt()
    }
}

class SetUniqueIdCommand(
    uniqueId: Int,
    sequenceNumber: Short,
    val podUniqueId: Int,
    val lotNumber: Int,
    val podSequenceNumber: Int
) : HeaderEnabledCommand(CommandType.SET_UNIQUE_ID, uniqueId, sequenceNumber, false) {
    override val encoded: ByteArray
        get() {
            val body = ByteBuffer.allocate(14)
                .put(commandType.value)
                .put(12)
                .putInt(podUniqueId)
                .putInt(lotNumber)
                .putInt(podSequenceNumber)
                .array()
            val header = encodeHeader(uniqueId, sequenceNumber, body.size.toShort(), multiCommandFlag)
            return appendCrc(header + body)
        }
}

class StopDeliveryCommand(
    uniqueId: Int,
    sequenceNumber: Short,
    nonce: Int,
    val stopBasal: Boolean,
    val stopBolus: Boolean,
    val stopTempBasal: Boolean
) : NonceEnabledCommand(CommandType.STOP_DELIVERY, uniqueId, sequenceNumber, false, nonce) {
    override val encoded: ByteArray
        get() {
            val stopFlags = ((if (stopBasal) 1 else 0) or
                ((if (stopBolus) 1 else 0) shl 1) or
                ((if (stopTempBasal) 1 else 0) shl 2)).toByte()
            val body = ByteBuffer.allocate(7)
                .put(commandType.value)
                .put(5)
                .putInt(nonce)
                .put(stopFlags)
                .array()
            val header = encodeHeader(uniqueId, sequenceNumber, body.size.toShort(), multiCommandFlag)
            return appendCrc(header + body)
        }
}

class DeactivateCommand(
    uniqueId: Int,
    sequenceNumber: Short,
    nonce: Int
) : NonceEnabledCommand(CommandType.DEACTIVATE, uniqueId, sequenceNumber, false, nonce) {
    override val encoded: ByteArray
        get() {
            val body = ByteBuffer.allocate(6)
                .put(commandType.value)
                .put(4)
                .putInt(nonce)
                .array()
            val header = encodeHeader(uniqueId, sequenceNumber, body.size.toShort(), multiCommandFlag)
            return appendCrc(header + body)
        }
}

class ProgramBolusCommand(
    val interlockCommand: ProgramInsulinCommand,
    uniqueId: Int,
    sequenceNumber: Short,
    multiCommandFlag: Boolean,
    val programReminder: ProgramReminder,
    val numberOfTenthPulses: Short,
    val delayUntilFirstTenthPulseInUsec: Int
) : HeaderEnabledCommand(CommandType.PROGRAM_BOLUS, uniqueId, sequenceNumber, multiCommandFlag) {
    constructor(
        uniqueId: Int,
        sequenceNumber: Short,
        nonce: Int,
        pulseCount: Short
    ) : this(
        createFromPulses(uniqueId, sequenceNumber, nonce, pulseCount).interlockCommand,
        uniqueId,
        sequenceNumber,
        false,
        ProgramReminder(false, false, 0),
        (pulseCount * 10).toShort(),
        25000
    )

    override val encoded: ByteArray
        get() {
            val bolusCommand = ByteBuffer.allocate(15)
                .put(commandType.value)
                .put(13.toByte())
                .put(programReminder.encoded)
                .putShort(numberOfTenthPulses)
                .putInt(delayUntilFirstTenthPulseInUsec)
                .putShort(0.toShort()) // Extended bolus pulses
                .putInt(0) // Delay between tenth extended pulses in usec
                .array()
            val interlockBytes = interlockCommand.encoded
            val header: ByteArray = encodeHeader(
                uniqueId,
                sequenceNumber,
                (bolusCommand.size + interlockBytes.size).toShort(),
                multiCommandFlag
            )
            return appendCrc(
                ByteBuffer.allocate(header.size + interlockBytes.size + bolusCommand.size)
                    .put(header)
                    .put(interlockBytes)
                    .put(bolusCommand)
                    .array()
            )
        }

    companion object {
        fun create(
            uniqueId: Int,
            sequenceNumber: Short,
            nonce: Int,
            units: Double,
            programReminder: ProgramReminder = ProgramReminder(false, false, 0),
            delayBetweenPulsesInEighthSeconds: Byte = 2,
            multiCommandFlag: Boolean = false
        ): ProgramBolusCommand {
            val numberOfPulses = (units * 20).roundToInt().toShort()
            val byte10And11 = (numberOfPulses * delayBetweenPulsesInEighthSeconds).toShort()
            val shortElement = BolusShortInsulinProgramElement(numberOfPulses)
            val checksum = MessageUtil.calculateChecksum(
                ByteBuffer.allocate(7)
                    .put(0x01.toByte())
                    .putShort(byte10And11)
                    .putShort(numberOfPulses)
                    .putShort(numberOfPulses)
                    .array()
            )
            val interlockCommand = ProgramInsulinCommand(
                uniqueId = uniqueId,
                sequenceNumber = sequenceNumber,
                multiCommandFlag = multiCommandFlag,
                nonce = nonce,
                insulinProgramElements = listOf(shortElement),
                checksum = checksum,
                byte9 = 0x01.toByte(),
                byte10And11 = byte10And11,
                byte12And13 = numberOfPulses,
                deliveryType = ProgramInsulinCommand.DeliveryType.BOLUS
            )
            val delayUntilFirstTenthPulseInUsec = (delayBetweenPulsesInEighthSeconds.toInt() / 8.0 * 100000).toInt()
            return ProgramBolusCommand(
                interlockCommand = interlockCommand,
                uniqueId = uniqueId,
                sequenceNumber = sequenceNumber,
                multiCommandFlag = multiCommandFlag,
                programReminder = programReminder,
                numberOfTenthPulses = (numberOfPulses * 10).toShort(),
                delayUntilFirstTenthPulseInUsec = delayUntilFirstTenthPulseInUsec
            )
        }

        fun createFromPulses(
            uniqueId: Int,
            sequenceNumber: Short,
            nonce: Int,
            pulseCount: Short,
            programReminder: ProgramReminder = ProgramReminder(false, false, 0),
            delayBetweenPulsesInEighthSeconds: Byte = 2,
            multiCommandFlag: Boolean = false
        ): ProgramBolusCommand = create(
            uniqueId = uniqueId,
            sequenceNumber = sequenceNumber,
            nonce = nonce,
            units = pulseCount.toDouble() / 20.0,
            programReminder = programReminder,
            delayBetweenPulsesInEighthSeconds = delayBetweenPulsesInEighthSeconds,
            multiCommandFlag = multiCommandFlag
        )
    }
}

class ProgramTempBasalCommand(
    val interlockCommand: ProgramInsulinCommand,
    uniqueId: Int,
    sequenceNumber: Short,
    multiCommandFlag: Boolean,
    val programReminder: ProgramReminder,
    val insulinProgramElements: List<BasalInsulinProgramElement>
) : HeaderEnabledCommand(CommandType.PROGRAM_TEMP_BASAL, uniqueId, sequenceNumber, multiCommandFlag) {
    constructor(
        uniqueId: Int,
        sequenceNumber: Short,
        nonce: Int,
        pulseCount: Short,
        durationHalfHours: Short
    ) : this(
        create(
            uniqueId = uniqueId,
            sequenceNumber = sequenceNumber,
            nonce = nonce,
            rateInUnitsPerHour = (pulseCount.toDouble() / 10.0) * 2.0,
            durationInMinutes = (durationHalfHours * 30).toShort()
        ).interlockCommand,
        uniqueId,
        sequenceNumber,
        false,
        ProgramReminder(false, false, 0),
        create(
            uniqueId = uniqueId,
            sequenceNumber = sequenceNumber,
            nonce = nonce,
            rateInUnitsPerHour = (pulseCount.toDouble() / 10.0) * 2.0,
            durationInMinutes = (durationHalfHours * 30).toShort()
        ).insulinProgramElements
    )

    private fun getBodyLength(): Byte = (insulinProgramElements.size * 6 + 8).toByte()
    private fun getLength(): Short = (getBodyLength() + 2).toShort()

    override val encoded: ByteArray
        get() {
            val firstProgramElement = insulinProgramElements[0]
            val remainingTenthPulsesInFirstElement: Short
            val delayUntilNextTenthPulseInUsec: Int
            if (firstProgramElement.totalTenthPulses.toInt() == 0) {
                remainingTenthPulsesInFirstElement = firstProgramElement.numberOfSlots.toShort()
                delayUntilNextTenthPulseInUsec =
                    ProgramBasalUtil.MAX_DELAY_BETWEEN_TENTH_PULSES_IN_USEC_AND_USECS_IN_BASAL_SLOT
            } else {
                remainingTenthPulsesInFirstElement = firstProgramElement.totalTenthPulses
                delayUntilNextTenthPulseInUsec =
                    (firstProgramElement.numberOfSlots.toLong() * 1800.0 / remainingTenthPulsesInFirstElement * 1000000).toInt()
            }
            val buffer = ByteBuffer.allocate(getLength().toInt())
                .put(commandType.value)
                .put(getBodyLength())
                .put(programReminder.encoded)
                .put(0x00.toByte()) // Current slot index
                .putShort(remainingTenthPulsesInFirstElement)
                .putInt(delayUntilNextTenthPulseInUsec)
            for (element in insulinProgramElements) {
                buffer.put(element.encoded)
            }
            val tempBasalCommand = buffer.array()
            val interlockBytes = interlockCommand.encoded
            val header: ByteArray = encodeHeader(
                uniqueId,
                sequenceNumber,
                (tempBasalCommand.size + interlockBytes.size).toShort(),
                multiCommandFlag
            )
            return appendCrc(
                ByteBuffer.allocate(header.size + interlockBytes.size + tempBasalCommand.size)
                    .put(header)
                    .put(interlockBytes)
                    .put(tempBasalCommand)
                    .array()
            )
        }

    companion object {
        fun create(
            uniqueId: Int,
            sequenceNumber: Short,
            nonce: Int,
            rateInUnitsPerHour: Double,
            durationInMinutes: Short,
            programReminder: ProgramReminder = ProgramReminder(false, false, 0),
            multiCommandFlag: Boolean = false
        ): ProgramTempBasalCommand {
            require(durationInMinutes % 30 == 0) { "durationInMinutes must be divisible by 30" }
            val durationInSlots = (durationInMinutes / 30).toByte()
            val pulsesPerSlot = ProgramTempBasalUtil.mapTempBasalToPulsesPerSlot(durationInSlots, rateInUnitsPerHour)
            val tenthPulsesPerSlot = ProgramTempBasalUtil.mapTempBasalToTenthPulsesPerSlot(durationInSlots.toInt(), rateInUnitsPerHour)
            val shortInsulinProgramElements = ProgramTempBasalUtil.mapPulsesPerSlotToShortInsulinProgramElements(pulsesPerSlot)
            val insulinProgramElements = ProgramTempBasalUtil.mapTenthPulsesPerSlotToLongInsulinProgramElements(tenthPulsesPerSlot)
            val checksum = ProgramTempBasalUtil.calculateChecksum(durationInSlots, pulsesPerSlot[0], pulsesPerSlot)
            val interlockCommand = ProgramInsulinCommand(
                uniqueId = uniqueId,
                sequenceNumber = sequenceNumber,
                multiCommandFlag = multiCommandFlag,
                nonce = nonce,
                insulinProgramElements = shortInsulinProgramElements,
                checksum = checksum,
                byte9 = durationInSlots,
                byte10And11 = 0x3840.toShort(),
                byte12And13 = pulsesPerSlot[0],
                deliveryType = ProgramInsulinCommand.DeliveryType.TEMP_BASAL
            )
            return ProgramTempBasalCommand(
                interlockCommand = interlockCommand,
                uniqueId = uniqueId,
                sequenceNumber = sequenceNumber,
                multiCommandFlag = multiCommandFlag,
                programReminder = programReminder,
                insulinProgramElements = insulinProgramElements
            )
        }
    }
}

class ProgramAlertsCommand(
    uniqueId: Int,
    sequenceNumber: Short,
    nonce: Int,
    val alertConfigurations: List<AlertConfiguration>,
    multiCommandFlag: Boolean = false
) : NonceEnabledCommand(CommandType.PROGRAM_ALERTS, uniqueId, sequenceNumber, multiCommandFlag, nonce) {

    private fun getLength(): Short = (alertConfigurations.size * 6 + 6).toShort()
    private fun getBodyLength(): Byte = (alertConfigurations.size * 6 + 4).toByte()

    override val encoded: ByteArray
        get() {
            val byteBuffer = ByteBuffer.allocate(getLength() + 6)
                .put(encodeHeader(uniqueId, sequenceNumber, getLength(), multiCommandFlag))
                .put(commandType.value)
                .put(getBodyLength())
                .putInt(nonce)
            for (configuration in alertConfigurations) {
                byteBuffer.put(configuration.encoded)
            }
            return appendCrc(byteBuffer.array())
        }
}

class ProgramBeepsCommand(
    uniqueId: Int,
    sequenceNumber: Short,
    val immediateBeepType: BeepType,
    val basalReminder: ProgramReminder,
    val tempBasalReminder: ProgramReminder,
    val bolusReminder: ProgramReminder,
    multiCommandFlag: Boolean = false
) : HeaderEnabledCommand(CommandType.PROGRAM_BEEPS, uniqueId, sequenceNumber, multiCommandFlag) {

    override val encoded: ByteArray
        get() = appendCrc(
            ByteBuffer.allocate(6 + 6)
                .put(encodeHeader(uniqueId, sequenceNumber, 6, multiCommandFlag))
                .put(commandType.value)
                .put(4.toByte())
                .put(immediateBeepType.value)
                .put(basalReminder.encoded)
                .put(tempBasalReminder.encoded)
                .put(bolusReminder.encoded)
                .array()
        )
}

class SilenceAlertsCommand(
    uniqueId: Int,
    sequenceNumber: Short,
    nonce: Int,
    val alertTypes: EnumSet<AlertType>,
    multiCommandFlag: Boolean = false
) : NonceEnabledCommand(CommandType.SILENCE_ALERTS, uniqueId, sequenceNumber, multiCommandFlag, nonce) {

    override val encoded: ByteArray
        get() {
            var alertByte: Byte = 0
            for (alert in alertTypes) {
                alertByte = (alertByte.toInt() or (alert.value.toInt() and 0xff)).toByte()
            }
            val body = ByteBuffer.allocate(7)
                .put(commandType.value)
                .put(5)
                .putInt(nonce)
                .put(alertByte)
                .array()
            val header = encodeHeader(uniqueId, sequenceNumber, 7, multiCommandFlag)
            return appendCrc(header + body)
        }
}

class ProgramInsulinCommand(
    uniqueId: Int,
    sequenceNumber: Short,
    multiCommandFlag: Boolean,
    nonce: Int,
    val insulinProgramElements: List<ShortInsulinProgramElement>,
    val checksum: Short,
    val byte9: Byte,
    val byte10And11: Short,
    val byte12And13: Short,
    val deliveryType: DeliveryType
) : NonceEnabledCommand(CommandType.PROGRAM_INSULIN, uniqueId, sequenceNumber, multiCommandFlag, nonce) {

    enum class DeliveryType(val value: Byte) {
        BASAL(0x00.toByte()), TEMP_BASAL(0x01.toByte()), BOLUS(0x02.toByte())
    }

    fun getLength(): Short = (insulinProgramElements.size * 2 + 14).toShort()
    fun getBodyLength(): Byte = (insulinProgramElements.size * 2 + 12).toByte()

    override val encoded: ByteArray
        get() {
            val buffer = ByteBuffer.allocate(getLength().toInt())
                .put(commandType.value)
                .put(getBodyLength())
                .putInt(nonce)
                .put(deliveryType.value)
                .putShort(checksum)
                .put(byte9)
                .putShort(byte10And11)
                .putShort(byte12And13)
            for (element in insulinProgramElements) {
                buffer.put(element.encoded)
            }
            return buffer.array()
        }
}

class ProgramBasalCommand(
    val interlockCommand: ProgramInsulinCommand,
    uniqueId: Int,
    sequenceNumber: Short,
    multiCommandFlag: Boolean,
    val insulinProgramElements: List<BasalInsulinProgramElement>,
    val programReminder: ProgramReminder,
    val currentInsulinProgramElementIndex: Byte,
    val remainingTenthPulsesInCurrentInsulinProgramElement: Short,
    val delayUntilNextTenthPulseInUsec: Int
) : HeaderEnabledCommand(CommandType.PROGRAM_BASAL, uniqueId, sequenceNumber, multiCommandFlag) {

    val length: Short get() = (insulinProgramElements.size * 6 + 10).toShort()
    private val bodyLength: Byte get() = (insulinProgramElements.size * 6 + 8).toByte()

    override val encoded: ByteArray
        get() {
            val buffer = ByteBuffer.allocate(length.toInt())
                .put(commandType.value)
                .put(bodyLength)
                .put(programReminder.encoded)
                .put(currentInsulinProgramElementIndex)
                .putShort(remainingTenthPulsesInCurrentInsulinProgramElement)
                .putInt(delayUntilNextTenthPulseInUsec)
            for (element in insulinProgramElements) {
                buffer.put(element.encoded)
            }
            val basalCommand = buffer.array()
            val interlockBytes = interlockCommand.encoded
            val header = encodeHeader(
                uniqueId,
                sequenceNumber,
                (basalCommand.size + interlockBytes.size).toShort(),
                multiCommandFlag
            )
            return appendCrc(
                ByteBuffer.allocate(basalCommand.size + interlockBytes.size + header.size)
                    .put(header)
                    .put(interlockBytes)
                    .put(basalCommand)
                    .array()
            )
        }

    companion object {
        fun create(
            uniqueId: Int,
            sequenceNumber: Short,
            nonce: Int,
            basalProgram: BasalProgram,
            programReminder: ProgramReminder,
            currentTime: Date = Date(),
            multiCommandFlag: Boolean = false
        ): ProgramBasalCommand {
            val pulsesPerSlot = ProgramBasalUtil.mapBasalProgramToPulsesPerSlot(basalProgram)
            val currentSlot = ProgramBasalUtil.calculateCurrentSlot(pulsesPerSlot, currentTime)
            val checksum = ProgramBasalUtil.calculateChecksum(pulsesPerSlot, currentSlot)
            val longElements = ProgramBasalUtil.mapTenthPulsesPerSlotToLongInsulinProgramElements(
                ProgramBasalUtil.mapBasalProgramToTenthPulsesPerSlot(basalProgram)
            )
            val shortElements = ProgramBasalUtil.mapPulsesPerSlotToShortInsulinProgramElements(pulsesPerSlot)
            val currentLongElement = ProgramBasalUtil.calculateCurrentLongInsulinProgramElement(longElements, currentTime)

            val interlock = ProgramInsulinCommand(
                uniqueId = uniqueId,
                sequenceNumber = sequenceNumber,
                multiCommandFlag = multiCommandFlag,
                nonce = nonce,
                insulinProgramElements = shortElements,
                checksum = checksum,
                byte9 = currentSlot.index,
                byte10And11 = currentSlot.eighthSecondsRemaining,
                byte12And13 = currentSlot.pulsesRemaining,
                deliveryType = ProgramInsulinCommand.DeliveryType.BASAL
            )

            return ProgramBasalCommand(
                interlockCommand = interlock,
                uniqueId = uniqueId,
                sequenceNumber = sequenceNumber,
                multiCommandFlag = multiCommandFlag,
                insulinProgramElements = longElements,
                programReminder = programReminder,
                currentInsulinProgramElementIndex = currentLongElement.index,
                remainingTenthPulsesInCurrentInsulinProgramElement = currentLongElement.remainingTenthPulses,
                delayUntilNextTenthPulseInUsec = currentLongElement.delayUntilNextTenthPulseInUsec
            )
        }
    }
}