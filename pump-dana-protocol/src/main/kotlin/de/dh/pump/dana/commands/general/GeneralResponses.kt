package de.dh.pump.dana.commands.general

import de.dh.pump.PumpStatus
import de.dh.pump.dana.commands.DanaRsResponse
import java.time.LocalDate

enum class DanaRsPumpErrorState(val code: Int) {
    SUSPENDED(0x01),
    DAILY_MAX(0x02),
    BOLUS_BLOCK(0x04),
    ORDER_DELIVERING(0x08),
    NO_PRIME(0x10);

    companion object {
        fun parseFlags(code: Int): Set<DanaRsPumpErrorState> {
            return entries.filter { (it.code and code) == it.code }.toSet()
        }
    }
}

/**
 * Snapshot shown by the pump's initial screen command.
 *
 * Most numeric insulin values are transmitted as centi-units. The parser exposes them as pump units
 * to keep application code independent from the wire scaling.
 */
data class GeneralInitialScreenInformationResponse(
    override val status: PumpStatus,
    /**
     * Whether insulin delivery is currently suspended.
     */
    val pumpSuspended: Boolean,
    /**
     * Whether a temporary basal rate is currently active.
     */
    val tempBasalInProgress: Boolean,
    /**
     * Whether an extended bolus is currently being delivered.
     */
    val extendedBolusInProgress: Boolean,
    /**
     * Whether a dual bolus is currently being delivered.
     */
    val dualBolusInProgress: Boolean,
    /**
     * Total insulin delivered today in Units (U).
     */
    val dailyTotalUnits: Double,
    /**
     * Maximum daily total limit in Units (U).
     */
    val maxDailyTotalUnits: Int,
    /**
     * Remaining insulin in the reservoir in Units (U).
     */
    val reservoirRemainingUnits: Double,
    /**
     * Currently active basal rate in Units per Hour (U/h).
     */
    val currentBasalUnitsPerHour: Double,
    /**
     * Currently active temporary basal ratio in percent.
     */
    val tempBasalPercent: Int,
    /**
     * Remaining battery level in percent.
     */
    val batteryRemainingPercent: Int,
    /**
     * Delivery rate of the active extended bolus in Units per Hour (U/h).
     */
    val extendedBolusAbsoluteRate: Double,
    /**
     * Active insulin on board in Units (U).
     */
    val insulinOnBoardUnits: Double,
    /**
     * Current technical error or warning state of the pump (multiple flags possible).
     */
    val errorStates: Set<DanaRsPumpErrorState>,
) : DanaRsResponse

/**
 * Firmware capability information returned during the normal DanaRS/Dana-i pump check.
 */
data class GeneralPumpCheckResponse(
    override val status: PumpStatus,
    val hardwareModel: Int,
    val protocol: Int,
    val productCode: Int,
    val supportedFirmware: Boolean,
) : DanaRsResponse

/**
 * Manufacturing metadata reported by the pump.
 */
data class GeneralShippingInformationResponse(
    override val status: PumpStatus,
    val serialNumber: String,
    val shippingCountry: String,
    val shippingDate: LocalDate,
) : DanaRsResponse

data class GeneralShippingVersionResponse(
    override val status: PumpStatus,
    val bleModel: String,
) : DanaRsResponse

data class GeneralUserTimeChangeFlagResponse(
    override val status: PumpStatus,
    val flag: Int,
    val changedByUser: Boolean,
) : DanaRsResponse

/**
 * Rolling bolus averages reported by the review command.
 */
data class ReviewBolusAverageResponse(
    override val status: PumpStatus,
    val threeDayAverageUnits: Double,
    val sevenDayAverageUnits: Double,
    val fourteenDayAverageUnits: Double,
    val twentyOneDayAverageUnits: Double,
    val twentyEightDayAverageUnits: Double,
) : DanaRsResponse

data class ReviewPumpDecRatioResponse(
    override val status: PumpStatus,
    val ratioPercent: Int,
) : DanaRsResponse