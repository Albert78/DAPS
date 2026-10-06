package de.dh.pump.dana.commands.options

import de.dh.pump.dana.commands.DanaGlucoseUnits

/**
 * Comprehensive set of user-configurable settings on a Dana pump.
 */
data class DanaRsUserOptions(
    /**
     * Hardware model index (used to determine feature support, e.g., >= 7 for Dana-i).
     */
    val hwModel: Int,
    /**
     * True for 24h clock, false for 12h clock.
     */
    val timeDisplayType24: Boolean,
    /**
     * Whether button scrolling is enabled.
     */
    val buttonScrollOnOff: Boolean,
    /**
     * Beep and alarm volume level (typically 1-5).
     */
    val beepAndAlarm: Int,
    /**
     * Time in seconds until the LCD turns off.
     */
    val lcdOnTimeSec: Int,
    /**
     * Time in seconds until the backlight turns off.
     */
    val backlightOnTimeSec: Int,
    /**
     * Selected menu language ID.
     */
    val selectedLanguage: Int,
    /**
     * Glucose measurement unit (mg/dL or mmol/L).
     */
    val units: DanaGlucoseUnits,
    /**
     * Automatic shutdown timer in hours (0 to disable).
     */
    val shutdownHour: Int,
    /**
     * Threshold for low reservoir warning in Units (U).
     */
    val lowReservoirRate: Int,
    /**
     * Default volume for cannula priming in Centi-Units (0.01 U).
     */
    val cannulaVolume: Int,
    /**
     * Assumed insulin amount after a reservoir refill in Units (U).
     */
    val refillAmount: Int,
    /**
     * Target blood glucose level for the bolus calculator (unit depends on [units]).
     */
    val target: Int,
)