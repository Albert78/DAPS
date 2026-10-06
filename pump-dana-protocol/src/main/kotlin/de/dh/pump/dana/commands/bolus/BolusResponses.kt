package de.dh.pump.dana.commands.bolus

import de.dh.pump.PumpStatus
import de.dh.pump.dana.commands.DanaGlucoseUnits
import de.dh.pump.dana.commands.DanaRsResponse
import java.time.LocalTime

/**
 * One paired carbohydrate-ratio/correction-factor value for a specific hour.
 */
data class CirCfValue(
    val cir: Double,
    val cf: Double,
)

/**
 * Full 24-hour CIR/CF table. Correction factors are normalized according to the pump's glucose unit.
 */
data class Bolus24CirCfArrayResponse(
    override val status: PumpStatus,
    /**
     * The glucose unit (mg/dL or mmol/L) currently configured on the pump.
     */
    val units: DanaGlucoseUnits,
    /**
     * 24 hourly pairs of CIR and CF values.
     */
    val valuesByHour: List<CirCfValue>,
) : DanaRsResponse

data class MissedBolusWindow(
    /**
     * Hour when the window starts (0-23).
     */
    val startHour: Int,
    /**
     * Minute when the window starts (0-59).
     */
    val startMinute: Int,
    /**
     * Hour when the window ends (0-23).
     */
    val endHour: Int,
    /**
     * Minute when the window ends (0-59).
     */
    val endMinute: Int,
)

/**
 * Bolus feature flags and missed-bolus reminder windows.
 */
data class BolusOptionResponse(
    override val status: PumpStatus,
    /**
     * Whether the extended bolus feature is enabled in the pump menu.
     */
    val extendedBolusEnabled: Boolean,
    /**
     * Bolus calculator option index.
     */
    val bolusCalculationOption: Int,
    /**
     * Configuration bitmask for missed bolus reminders.
     */
    val missedBolusConfig: Int,
    /**
     * List of 4 defined time windows for bolus reminders.
     */
    val missedBolusWindows: List<MissedBolusWindow>,
) : DanaRsResponse

/**
 * Inputs and pump-calculated values used by the bolus calculator screen.
 */
data class BolusCalculationInformationResponse(
    override val status: PumpStatus,
    /**
     * Error code from the pump's calculation engine.
     */
    val errorCode: Int,
    /**
     * The glucose unit (mg/dL or mmol/L) currently configured on the pump.
     */
    val units: DanaGlucoseUnits,
    /**
     * Current blood glucose level (unit depends on [units]).
     */
    val currentBloodGlucose: Double,
    /**
     * Grams of carbohydrates entered.
     */
    val carbohydrateGrams: Int,
    /**
     * Current target blood glucose level (unit depends on [units]).
     */
    val currentTarget: Double,
    /**
     * Currently active carbohydrate ratio (CIR).
     */
    val currentCarbRatio: Int,
    /**
     * Currently active correction factor (CF).
     */
    val currentCorrectionFactor: Double,
    /**
     * Amount of insulin currently active in the body in Units (U).
     */
    val insulinOnBoardUnits: Double,
) : DanaRsResponse

/**
 * Compact CIR/CF profile used by older bolus calculator commands.
 */
data class BolusCirCfArrayResponse(
    override val status: PumpStatus,
    /**
     * Language ID.
     */
    val language: Int,
    /**
     * The glucose unit (mg/dL or mmol/L) currently configured on the pump.
     */
    val units: DanaGlucoseUnits,
    /**
     * List of 7 CIR values.
     */
    val cirValues: List<Int>,
    /**
     * List of 7 CF values.
     */
    val cfValues: List<Double>,
) : DanaRsResponse

/**
 * Current limits and last-delivery information for step bolus delivery.
 */
data class BolusStepBolusInformationResponse(
    override val status: PumpStatus,
    /**
     * Error code from the pump.
     */
    val errorCode: Int,
    /**
     * Type of bolus (e.g., 0 = step bolus).
     */
    val bolusType: Int,
    /**
     * Amount for a fresh bolus startup in Units (U).
     */
    val initialBolusAmountUnits: Double,

    /**
     * Gets the time when the bolus was injected, in UTC time.
     */
    val lastBolusTimeOfDayUTC: LocalTime,
    /**
     * Amount of the last delivered bolus in Units (U).
     */
    val lastBolusAmountUnits: Double,
    /**
     * Maximum allowed bolus amount in Units (U).
     */
    val maxBolusUnits: Double,
    /**
     * Incremental step size for bolus adjustments in Units (U).
     */
    val bolusStepUnits: Double,
) : DanaRsResponse

enum class BolusStartStatus(val code: Int) {
    OK(0x00),
    MAX_BOLUS_VIOLATION(0x10),
    COMMAND_ERROR(0x20),
    SPEED_ERROR(0x40),
    INSULIN_LIMIT_VIOLATION(0x80),
    UNKNOWN(-1);

    companion object {
        fun fromCode(code: Int): BolusStartStatus =
            entries.firstOrNull { it.code == code } ?: UNKNOWN
    }
}

/**
 * Result of a bolus start command.
 */
data class BolusStartResponse(
    override val status: PumpStatus,
    /**
     * The raw error code from the pump.
     */
    val errorCode: Int,
    /**
     * Normalized bolus start status.
     */
    val bolusStartStatus: BolusStartStatus,
) : DanaRsResponse