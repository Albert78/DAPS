package de.dh.pump.omnipod.protocol.command.program

import de.dh.pump.omnipod.protocol.definition.Encodable
import java.io.Serializable
import java.nio.ByteBuffer
import kotlin.experimental.and

interface ShortInsulinProgramElement : Encodable, Serializable

class BolusShortInsulinProgramElement(
    val numberOfPulses: Short
) : ShortInsulinProgramElement {
    override val encoded: ByteArray
        get() = ByteBuffer.allocate(2).putShort(numberOfPulses).array()
}

class BasalShortInsulinProgramElement(
    val numberOfSlots: Byte,
    val pulsesPerSlot: Short,
    val extraAlternatePulse: Boolean
) : ShortInsulinProgramElement {

    override val encoded: ByteArray
        get() {
            val firstByte = (
                ((numberOfSlots.toInt() - 1) and 0x0f) shl 4
                    or ((if (extraAlternatePulse) 1 else 0) shl 3)
                    or (pulsesPerSlot.toInt() ushr 8 and 0x03)
                ).toByte()
            return ByteBuffer.allocate(2)
                .put(firstByte)
                .put((pulsesPerSlot.toInt() and 0xff).toByte())
                .array()
        }

    override fun toString(): String {
        return "BasalShortInsulinProgramElement(numberOfSlots=$numberOfSlots, pulsesPerSlot=$pulsesPerSlot, extraAlternatePulse=$extraAlternatePulse)"
    }
}

open class BasalInsulinProgramElement(
    val startSlotIndex: Byte,
    val numberOfSlots: Byte,
    val totalTenthPulses: Short
) : Encodable, Serializable {

    override val encoded: ByteArray
        get() = ByteBuffer.allocate(6)
            .putShort(totalTenthPulses)
            .putInt(if (totalTenthPulses.toInt() == 0) Int.MIN_VALUE or delayBetweenTenthPulsesInUsec else delayBetweenTenthPulsesInUsec)
            .array()

    val durationInSeconds: Short
        get() = (numberOfSlots.toInt() * 1800).toShort()

    val delayBetweenTenthPulsesInUsec: Int
        get() = if (totalTenthPulses.toInt() == 0) {
            ProgramBasalUtil.MAX_DELAY_BETWEEN_TENTH_PULSES_IN_USEC_AND_USECS_IN_BASAL_SLOT
        } else (ProgramBasalUtil.MAX_DELAY_BETWEEN_TENTH_PULSES_IN_USEC_AND_USECS_IN_BASAL_SLOT.toLong() * numberOfSlots / totalTenthPulses.toDouble()).toInt()

    override fun toString(): String {
        return "BasalInsulinProgramElement(startSlotIndex=$startSlotIndex, numberOfSlots=$numberOfSlots, totalTenthPulses=$totalTenthPulses, delayBetweenTenthPulsesInUsec=$delayBetweenTenthPulsesInUsec)"
    }
}

data class CurrentSlot(
    val index: Byte,
    val eighthSecondsRemaining: Short,
    val pulsesRemaining: Short
) : Serializable

class CurrentBasalInsulinProgramElement(
    val index: Byte,
    val delayUntilNextTenthPulseInUsec: Int,
    val remainingTenthPulses: Short
) : Serializable