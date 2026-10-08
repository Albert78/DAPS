package de.dh.pump.omnipod.protocol.util

import de.dh.daps.common.model.data.InsulinProfile
import de.dh.daps.common.model.data.Minutes
import de.dh.daps.common.model.data.getAmountForMinute

object ProfileToBasalScheduleConverter {
    const val SEGMENTS_PER_DAY = 48

    fun convertProfileToHalfHourPulses(profile: InsulinProfile): ShortArray {
        val pulses = ShortArray(SEGMENTS_PER_DAY)
        for (i in 0 until SEGMENTS_PER_DAY) {
            val minuteSinceMidnight = Minutes((i * 30).toShort())
            val rateInUnitsPerHour = profile.basalBlocks.getAmountForMinute(minuteSinceMidnight)
            pulses[i] = PodPulseCalculator.unitsPerHourToHalfHourPulses(rateInUnitsPerHour)
        }
        return pulses
    }
}