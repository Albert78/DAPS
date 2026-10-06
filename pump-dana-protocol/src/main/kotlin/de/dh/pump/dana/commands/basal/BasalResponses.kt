package de.dh.pump.dana.commands.basal

import de.dh.pump.PumpStatus
import de.dh.pump.dana.commands.DanaRsResponse

/**
 * Active basal profile returned as 24 hourly rates plus pump-side limits.
 */
data class BasalRateProfileResponse(
    override val status: PumpStatus,
    /**
     * Maximum allowed basal rate in Units per Hour (U/h).
     */
    val maxBasalUnitsPerHour: Double,
    /**
     * Incremental step size for basal adjustments in Units (U).
     */
    val basalStepUnits: Double,
    /**
     * List of 24 hourly basal rates in Units per Hour (U/h).
     */
    val hourlyRatesUnits: List<Double>,
    /**
     * Whether the pump hardware supports the current basal step size.
     */
    val basalStepSupported: Boolean,
) : DanaRsResponse

/**
 * 24 hourly rates for a specific basal profile.
 */
data class BasalProfileBasalRateResponse(
    override val status: PumpStatus,
    /**
     * List of 24 hourly basal rates in Units per Hour (U/h).
     */
    val hourlyRatesUnits: List<Double>,
) : DanaRsResponse

/**
 * The one-byte active profile index reported by the pump.
 */
data class BasalProfileNumberResponse(
    override val status: PumpStatus,
    /**
     * Index of the currently active basal profile (0-3).
     */
    val activeProfile: Int,
) : DanaRsResponse