package de.dh.pump.dana.commands.bolus

import de.dh.pump.PumpStatus
import de.dh.pump.dana.commands.DanaRsBolusSpeed
import de.dh.pump.dana.commands.DanaRsResponse

/**
 * Global bolus rate settings (limits and speed).
 */
data class BolusRateResponse(
    override val status: PumpStatus,
    val maxBolusUnits: Double,
    val bolusStepUnits: Double,
    val bolusSpeed: DanaRsBolusSpeed,
) : DanaRsResponse