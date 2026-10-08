package de.dh.pump.omnipod.protocol.util

import kotlin.math.roundToInt

object PodPulseCalculator {
    const val PULSES_PER_UNIT = 20.0
    const val INSULIN_PER_PULSE_U = 0.05

    fun unitsToPulses(units: Double): Short {
        require(units >= 0.0) { "Units cannot be negative: $units" }
        return (units * PULSES_PER_UNIT).roundToInt().toShort()
    }

    fun pulsesToUnits(pulses: Short): Double {
        require(pulses >= 0) { "Pulses cannot be negative: $pulses" }
        return pulses * INSULIN_PER_PULSE_U
    }

    fun unitsPerHourToHalfHourPulses(rateUnitsPerHour: Double): Short {
        require(rateUnitsPerHour >= 0.0) { "Rate cannot be negative: $rateUnitsPerHour" }
        return (rateUnitsPerHour * (PULSES_PER_UNIT / 2.0)).roundToInt().toShort()
    }
}