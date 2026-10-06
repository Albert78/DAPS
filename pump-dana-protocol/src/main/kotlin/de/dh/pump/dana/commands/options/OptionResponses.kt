package de.dh.pump.dana.commands.options

import de.dh.pump.PumpStatus
import de.dh.pump.dana.commands.DanaGlucoseUnits
import de.dh.pump.dana.commands.DanaRsResponse
import java.time.LocalDateTime

/**
 * Pump-local clock value, decoded without applying a phone timezone conversion.
 */
data class OptionPumpTimeResponse(
    override val status: PumpStatus,
    val pumpTime: LocalDateTime,
) : DanaRsResponse

/**
 * UTC clock value plus the signed hour offset stored by newer Dana firmware.
 */
data class OptionPumpUtcAndTimeZoneResponse(
    override val status: PumpStatus,
    val pumpUtcTime: LocalDateTime,
    val zoneOffsetHours: Int,
) : DanaRsResponse

/**
 * User-visible pump settings returned by OPTION_GET_USER_OPTION.
 *
 * The language ids can be used as index in [DanaRsLanguage].
 *
 * Older firmware may omit the target value; in that case [target] is null and all known bytes are
 * still decoded.
 */
data class OptionUserOptionsResponse(
    override val status: PumpStatus,
    /**
     * True for 24h clock, false for 12h clock.
     */
    val timeDisplayType24: Boolean,
    /**
     * Whether button scrolling is enabled.
     */
    val buttonScrollOnOff: Boolean,
    /**
     * Beep and alarm volume level (1-5).
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
     * Volume used for cannula priming in Centi-Units (0.01 U).
     */
    val cannulaVolume: Int,
    /**
     * Expected current insulin amount in Units (U). Used to tell the pump the amount of insulin
     * after reservoir refill.
     */
    val refillAmount: Int,
    /**
     * List of language IDs supported by this pump firmware.
     */
    val selectableLanguages: List<Int>,
    /**
     * Target blood glucose level for the bolus calculator (unit depends on [units]).
     */
    val target: Int?,
) : DanaRsResponse