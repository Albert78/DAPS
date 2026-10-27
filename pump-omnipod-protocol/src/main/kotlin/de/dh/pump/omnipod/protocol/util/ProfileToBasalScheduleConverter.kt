package de.dh.pump.omnipod.protocol.util

import de.dh.daps.common.domain.model.therapy.InsulinProfile
import de.dh.daps.common.domain.model.therapy.Minutes
import de.dh.daps.common.domain.model.therapy.getAmountForMinute

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