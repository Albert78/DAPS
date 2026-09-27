package de.dh.daps.ui.common

import android.content.Context
import android.content.res.Resources
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import de.dh.daps.common.model.InsulinAmount
import de.dh.daps.common.model.data.BgDelta
import de.dh.daps.common.model.data.BgReadingsInterval
import de.dh.daps.common.model.data.BgValue
import de.dh.daps.common.model.data.CarbsUnit
import de.dh.daps.common.model.data.GlucoseUnit
import de.dh.daps.common.model.data.Minutes
import de.dh.daps.common.model.data.Timestamp
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import de.dh.daps.common.R as CommonR

data class AppFormatters(
    val shortDateTime: DateTimeFormatter,
    val shortDate: DateTimeFormatter,
    val longDateTime: DateTimeFormatter,
    val longDate: DateTimeFormatter,
)

// Provider for the formatters
val LocalAppFormatters = staticCompositionLocalOf<AppFormatters> {
    error("No AppFormatters provided")
}

val LocalGlucoseUnit = staticCompositionLocalOf<GlucoseUnit> {
    error("No GlucoseUnit provided")
}

val LocalCarbsUnit = staticCompositionLocalOf<CarbsUnit> {
    error("No CarbsUnit provided")
}

@Composable
fun rememberAppFormatters(): AppFormatters {
    val locale = LocalLocale.current.platformLocale

    val shortDateTimePattern = stringResource(CommonR.string.short_date_time_format)
    val shortDatePattern = stringResource(CommonR.string.short_date_format)
    val longDateTimePattern = stringResource(CommonR.string.long_date_time_format)
    val longDatePattern = stringResource(CommonR.string.long_date_format)

    return remember(locale, shortDateTimePattern, shortDatePattern, longDateTimePattern, longDatePattern) {
        AppFormatters(
            shortDateTime = DateTimeFormatter.ofPattern(shortDateTimePattern, locale),
            shortDate = DateTimeFormatter.ofPattern(shortDatePattern, locale),
            longDateTime = DateTimeFormatter.ofPattern(longDateTimePattern, locale),
            longDate = DateTimeFormatter.ofPattern(longDatePattern, locale)
        )
    }
}

/////////////////////////////////////////////// Time ///////////////////////////////////////////////

/**
 * Non-Composable / Backend function.
 * Formats a [Timestamp] as a time string ("HH:mm").
 *
 * @example time(Timestamp(1700000000000)) -> "14:30"
 */
fun time(timestamp: Timestamp): String {
    val localTime = Instant.ofEpochMilli(timestamp.ms)
        .atZone(ZoneId.systemDefault())
        .toLocalTime()
    return time(localTime)
}

/**
 * Non-Composable / Backend function.
 * Formats a [LocalTime] as a time string ("HH:mm").
 *
 * @example time(LocalTime.of(14, 30)) -> "14:30"
 */
fun time(time: LocalTime): String {
    return String.format(Locale.getDefault(), "%02d:%02d", time.hour, time.minute)
}

/**
 * Composable UI function.
 * Returns the localized time unit label (e.g. "Uhr" in German or empty/unit label in English).
 *
 * @example "Uhr" / ""
 */
@Composable
fun timeUnitLabel(): String {
    return stringResource(CommonR.string.time_unit)
}

/**
 * Composable UI function.
 * Formats a [Timestamp] as a time string with unit ("HH:mm Uhr").
 *
 * @example timeWithUnit(timestamp) -> "14:30 Uhr" / "14:30"
 */
@Composable
fun timeWithUnit(timestamp: Timestamp): String {
    return "${time(timestamp)} ${timeUnitLabel()}"
}

/**
 * Composable UI function.
 * Formats a [LocalTime] as a time string with unit ("HH:mm Uhr").
 *
 * @example timeWithUnit(time) -> "14:30 Uhr" / "14:30"
 */
@Composable
fun timeWithUnit(time: LocalTime): String {
    return "${time(time)} ${timeUnitLabel()}"
}

/**
 * Composable UI function.
 * Formats a [Timestamp] as a time string, optionally appending the unit.
 *
 * @example time(timestamp, withUnit = true) -> "14:30 Uhr" / "14:30"
 * @example time(timestamp, withUnit = false) -> "14:30"
 */
@Composable
fun time(timestamp: Timestamp, withUnit: Boolean): String {
    return if (withUnit) timeWithUnit(timestamp) else time(timestamp)
}

/**
 * Composable UI function.
 * Formats a [LocalTime] as a time string, optionally appending the unit.
 *
 * @example time(time, withUnit = true) -> "14:30 Uhr" / "14:30"
 * @example time(time, withUnit = false) -> "14:30"
 */
@Composable
fun time(time: LocalTime, withUnit: Boolean): String {
    return if (withUnit) timeWithUnit(time) else time(time)
}

/////////////////////////////////////////////// Long date time //////////////////////////////////////

/**
 * Composable UI function.
 * Formats a [LocalDateTime] using the localized long date-time pattern.
 *
 * @example longDateTime(now) -> "14.11.2024, 14:30" / "11/14/2024, 2:30 PM"
 */
@Composable
fun longDateTime(dateTime: LocalDateTime): String {
    return dateTime.format(LocalAppFormatters.current.longDateTime)
}

/**
 * Composable UI function.
 * Formats a nullable [LocalDateTime] using the localized long date-time pattern or returns a default string.
 *
 * @example longDateTime(now) -> "14.11.2024, 14:30" / "11/14/2024, 2:30 PM"
 * @example longDateTime(null) -> "-"
 */
@Composable
fun longDateTime(dateTime: LocalDateTime?, default: String = "-"): String {
    return dateTime?.let {
        longDateTime(dateTime)
    } ?: default
}

/**
 * Composable UI function.
 * Formats a nullable [Timestamp] using the localized long date-time pattern or returns a default string.
 *
 * @example longDateTime(timestamp) -> "14.11.2024, 14:30" / "11/14/2024, 2:30 PM"
 * @example longDateTime(null) -> "-"
 */
@Composable
fun longDateTime(timestamp: Timestamp?, default: String = "-"): String {
    return if (timestamp != null && timestamp.isValid()) {
        val ldt = Instant.ofEpochMilli(timestamp.ms)
            .atZone(ZoneId.systemDefault())
            .toLocalDateTime()
        longDateTime(ldt, default)
    } else default
}

/////////////////////////////////////////////// Short date time //////////////////////////////////////

/**
 * Composable UI function.
 * Formats a [LocalDateTime] using the localized short date-time pattern.
 *
 * @example shortDateTime(now) -> "14.11. 14:30" / "Nov 14, 2:30 PM"
 */
@Composable
fun shortDateTime(dateTime: LocalDateTime): String {
    return dateTime.format(LocalAppFormatters.current.shortDateTime)
}

/**
 * Composable UI function.
 * Formats a nullable [LocalDateTime] using the localized short date-time pattern or returns a default string.
 *
 * @example shortDateTime(now) -> "14.11. 14:30" / "Nov 14, 2:30 PM"
 * @example shortDateTime(null) -> "-"
 */
@Composable
fun shortDateTime(dateTime: LocalDateTime?, default: String = "-"): String {
    return dateTime?.let {
        shortDateTime(dateTime)
    } ?: default
}

/////////////////////////////////////////////// Long date //////////////////////////////////////

/**
 * Composable UI function.
 * Formats a [LocalDate] using the localized long date pattern.
 *
 * @example longDate(date) -> "14. November 2024" / "November 14, 2024"
 */
@Composable
fun longDate(date: LocalDate): String {
    return date.format(LocalAppFormatters.current.longDate)
}

/**
 * Composable UI function.
 * Formats a nullable [LocalDate] using the localized long date pattern or returns a default string.
 *
 * @example longDate(date) -> "14. November 2024" / "November 14, 2024"
 * @example longDate(null) -> "-"
 */
@Composable
fun longDate(date: LocalDate?, default: String = "-"): String {
    return date?.let {
        longDate(date)
    } ?: default
}

/**
 * Composable UI function.
 * Formats a nullable [LocalDateTime] using the localized long date pattern or returns a default string.
 *
 * @example longDate(dateTime) -> "14. November 2024" / "November 14, 2024"
 * @example longDate(null) -> "-"
 */
@Composable
fun longDate(dateTime: LocalDateTime?, default: String = "-"): String {
    return longDate(dateTime?.toLocalDate(), default)
}

/////////////////////////////////////////////// Short date //////////////////////////////////////

/**
 * Composable UI function.
 * Formats a [LocalDate] using the localized short date pattern.
 *
 * @example shortDate(date) -> "14.11.2024" / "11/14/2024"
 */
@Composable
fun shortDate(date: LocalDate): String {
    return date.format(LocalAppFormatters.current.shortDate)
}

/**
 * Composable UI function.
 * Formats a nullable [LocalDate] using the localized short date pattern or returns a default string.
 *
 * @example shortDate(date) -> "14.11.2024" / "11/14/2024"
 * @example shortDate(null) -> "-"
 */
@Composable
fun shortDate(date: LocalDate?, default: String = "-"): String {
    return date?.let {
        shortDate(date)
    } ?: default
}

/////////////////////////////////////////////// Time ago //////////////////////////////////////

/**
 * Composable UI function.
 * Formats a millisecond time difference into a relative past time description.
 *
 * @example shortRelativeTimeAgo(3000) -> "Gerade eben" / "just now"
 * @example shortRelativeTimeAgo(300000) -> "vor 5 Min." / "5 min ago"
 * @example shortRelativeTimeAgo(7200000) -> "vor 2 Std." / "2 h ago"
 */
@Composable
fun shortRelativeTimeAgo(diffMs: Long): String {
    val diffSec = diffMs / 1000
    val diffMin = diffMs / 60000
    return when {
        diffSec < 5 -> stringResource(CommonR.string.time_ago_just_now)
        diffSec < 61 -> stringResource(CommonR.string.time_ago_seconds_ago, diffSec)
        diffMin < 1 -> stringResource(CommonR.string.time_ago_just_now)
        diffMin < 91 -> stringResource(CommonR.string.time_ago_minutes_ago, diffMin)
        else -> {
            stringResource(CommonR.string.time_ago_hours_ago, diffMin / 60)
        }
    }
}

/**
 * Composable UI function.
 * Formats a past [Timestamp] into a relative past time description.
 *
 * @example shortRelativeTimeAgo(timestamp) -> "vor 5 Min." / "5 min ago"
 */
@Composable
fun shortRelativeTimeAgo(timestamp: Timestamp): String {
    val diffMs = System.currentTimeMillis() - timestamp.ms
    return shortRelativeTimeAgo(diffMs)
}

/**
 * Composable UI function.
 * Formats a millisecond time difference into a relative future time description.
 *
 * @example shortRelativeTimeUntil(300000) -> "in 5 Min." / "in 5 min"
 * @example shortRelativeTimeUntil(7200000) -> "in 2 Std." / "in 2 h"
 */
@Composable
fun shortRelativeTimeUntil(diffMs: Long): String {
    val diffSec = diffMs / 1000
    val diffMin = diffMs / 60000
    return when {
        diffSec < 5 -> stringResource(CommonR.string.time_until_just_now)
        diffSec < 61 -> stringResource(CommonR.string.time_until_seconds, diffSec)
        diffMin < 1 -> stringResource(CommonR.string.time_until_just_now)
        diffMin < 91 -> stringResource(CommonR.string.time_until_minutes, diffMin)
        else -> {
            stringResource(CommonR.string.time_until_hours, diffMin / 60)
        }
    }
}

/**
 * Composable UI function.
 * Formats a future [Timestamp] into a relative future time description.
 *
 * @example shortRelativeTimeUntil(timestamp) -> "in 5 Min." / "in 5 min"
 */
@Composable
fun shortRelativeTimeUntil(timestamp: Timestamp): String {
    val diffMs = timestamp.ms - System.currentTimeMillis()
    return shortRelativeTimeUntil(diffMs)
}

/**
 * Composable UI function.
 * Formats a signed minute offset into a relative time description.
 *
 * @example relativeTimeMinutes(0) -> "Jetzt" / "Now"
 * @example relativeTimeMinutes(15) -> "+15 Min." / "+15 min"
 * @example relativeTimeMinutes(-10) -> "-10 Min." / "-10 min"
 */
@Composable
fun relativeTimeMinutes(minutes: Int): String {
    return when {
        minutes == 0 -> stringResource(CommonR.string.relative_time_now)
        minutes > 0 -> stringResource(CommonR.string.relative_time_minutes_positive, minutes)
        else -> stringResource(CommonR.string.relative_time_minutes_negative, abs(minutes))
    }
}

/**
 * Composable UI function.
 * Formats a duration in [Minutes] into a descriptive "within X" string.
 *
 * @example withinTimeDescription(Minutes(30)) -> "innerhalb von 30 Min." / "within 30 min"
 * @example withinTimeDescription(Minutes(90)) -> "innerhalb von 1 Std. 30 Min." / "within 1 h 30 min"
 */
@Composable
fun withinTimeDescription(minutes: Minutes): String {
    val m = minutes.value.toInt()
    if (m <= 0) return stringResource(CommonR.string.within_time_just_now)
    val hours = m / 60
    val mins = m % 60

    val timeStr = when {
        hours == 0 -> stringResource(CommonR.string.duration_minutes_format, mins)
        mins == 0 -> stringResource(CommonR.string.duration_hours_format, hours)
        else -> stringResource(CommonR.string.duration_hours_and_minutes_format, hours, mins)
    }

    return stringResource(CommonR.string.within_time_format, timeStr)
}

/////////////////////////////////////////////// Glucose & Therapy //////////////////////////////////////

/**
 * Composable UI function.
 * Returns the localized unit label for blood glucose (e.g. "mg/dL" or "mmol/L").
 *
 * @example "mg/dL"
 */
@Composable
fun glucoseUnitLabel(unit: GlucoseUnit = LocalGlucoseUnit.current): String {
    return when (unit) {
        GlucoseUnit.MG_DL -> stringResource(CommonR.string.glucose_unit_mgdl)
        GlucoseUnit.MMOL -> stringResource(CommonR.string.glucose_unit_mmol)
    }
}

/**
 * Composable UI function.
 * Returns the localized unit label for Insulin Sensitivity Factor (ISF) (e.g. "mg/dL/IU" or "mmol/L/IU").
 *
 * @example "mg/dL/IU"
 */
@Composable
fun isfUnitLabel(unit: GlucoseUnit = LocalGlucoseUnit.current): String {
    return when (unit) {
        GlucoseUnit.MG_DL -> stringResource(CommonR.string.unit_mgdl_per_u)
        GlucoseUnit.MMOL -> stringResource(CommonR.string.unit_mmol_per_u)
    }
}

/**
 * Non-Composable / Backend function.
 * Formats a [BgReadingsInterval] for background services, notifications, etc.
 *
 * @example formatReadingsInterval(BgReadingsInterval.FiveMinutes, resources) -> "5 Minuten"
 * @example formatReadingsInterval(null, resources) -> "--"
 */
fun formatReadingsInterval(
    interval: BgReadingsInterval?,
    resources: Resources,
    default: String = "--"
): String {
    return when (interval) {
        BgReadingsInterval.OneMinute -> resources.getString(CommonR.string.bg_readings_interval_one_minute)
        BgReadingsInterval.FiveMinutes -> resources.getString(CommonR.string.bg_readings_interval_five_minutes)
        BgReadingsInterval.AdHoc -> resources.getString(CommonR.string.bg_readings_interval_adhoc)
        null -> default
    }
}

/**
 * Non-Composable / Backend function.
 * Formats a [BgReadingsInterval] using a [Context].
 *
 * @example formatReadingsInterval(BgReadingsInterval.FiveMinutes, context) -> "5 Minuten"
 */
fun formatReadingsInterval(
    interval: BgReadingsInterval?,
    context: Context,
    default: String = "--"
): String = formatReadingsInterval(interval, context.resources, default)

/**
 * Composable UI function.
 * Formats a [BgReadingsInterval] for display in Compose UI components.
 *
 * @example readingsInterval(BgReadingsInterval.FiveMinutes) -> "5 Minuten"
 * @example readingsInterval(null) -> "--"
 */
@Composable
fun readingsInterval(interval: BgReadingsInterval?, default: String = "--"): String {
    return when (interval) {
        BgReadingsInterval.OneMinute -> stringResource(CommonR.string.bg_readings_interval_one_minute)
        BgReadingsInterval.FiveMinutes -> stringResource(CommonR.string.bg_readings_interval_five_minutes)
        BgReadingsInterval.AdHoc -> stringResource(CommonR.string.bg_readings_interval_adhoc)
        null -> default
    }
}

/**
 * Composable UI function.
 * Formats a blood glucose value for UI display.
 *
 * @example glucoseValue(BgValue.fromMgDl(120)) -> "120"
 * @example glucoseValue(BgValue.fromMgDl(120), withUnit = true) -> "120 mg/dL"
 * @example glucoseValue(null) -> "-"
 */
@Composable
fun glucoseValue(
    value: BgValue?,
    unit: GlucoseUnit = LocalGlucoseUnit.current,
    default: String = "-",
    withUnit: Boolean = false
): String {
    if (value == null || value.isInvalid()) return default
    val valStr = value.toString(unit)
    return if (withUnit) {
        "$valStr ${glucoseUnitLabel(unit)}"
    } else valStr
}

/**
 * Composable UI function.
 * Formats an Insulin Sensitivity Factor (ISF) value for UI display.
 *
 * @example isfValue(BgDelta.fromMgDl(40)) -> "40"
 * @example isfValue(BgDelta.fromMgDl(40), withUnit = true) -> "40 mg/dL/IU" / "40 mg/dL/I.E."
 * @example isfValue(null) -> "-"
 */
@Composable
fun isfValue(
    value: BgDelta?,
    unit: GlucoseUnit = LocalGlucoseUnit.current,
    default: String = "-",
    withUnit: Boolean = false
): String {
    val valStr = value?.toString(unit) ?: return default
    return if (withUnit) {
        val unitStr = when (unit) {
            GlucoseUnit.MG_DL -> stringResource(CommonR.string.unit_mgdl_per_u)
            GlucoseUnit.MMOL -> stringResource(CommonR.string.unit_mmol_per_u)
        }
        "$valStr $unitStr"
    } else valStr
}

/**
 * Composable UI function.
 * Formats a blood glucose delta value with +/- sign for UI display.
 *
 * @example deltaValue(BgDelta.fromMgDl(5)) -> "+5"
 * @example deltaValue(BgDelta.fromMgDl(-12)) -> "-12"
 * @example deltaValue(BgDelta.fromMgDl(5), withUnit = true) -> "+5 mg/dL"
 * @example deltaValue(null) -> "-"
 */
@Composable
fun deltaValue(
    value: BgDelta?,
    unit: GlucoseUnit = LocalGlucoseUnit.current,
    default: String = "-",
    withUnit: Boolean = false
): String {
    val valStr = value?.toDiff(unit) ?: return default
    return if (withUnit) {
        val unitStr = when (unit) {
            GlucoseUnit.MG_DL -> stringResource(CommonR.string.glucose_unit_mgdl)
            GlucoseUnit.MMOL -> stringResource(CommonR.string.glucose_unit_mmol)
        }
        "$valStr $unitStr"
    } else valStr
}

/**
 * Composable UI function.
 * Formats a Carb Ratio (CR) value for UI display.
 *
 * @example crValue(12.0) -> "12.0 g/IU" / "12.0 g/E"
 * @example crValue(12.0, withUnit = false) -> "12.0"
 * @example crValue(null) -> "-"
 */
@Composable
fun crValue(value: Double?, default: String = "-", withUnit: Boolean = true): String {
    return value?.let {
        val valStr = String.format(Locale.getDefault(), "%.1f", it)
        if (withUnit) "$valStr " + stringResource(CommonR.string.unit_g_per_u)
        else valStr
    } ?: default
}

/**
 * Non-Composable / Backend function.
 * Returns the unit label for insulin ("IU" / "I.E.").
 *
 * @example "IU" / "I.E."
 */
fun formatInsulinUnitLabel(resources: Resources): String {
    return resources.getString(CommonR.string.insulin_unit)
}

/**
 * Non-Composable / Backend function.
 * Returns the unit label for insulin ("IU" / "I.E.").
 *
 * @example "IU" / "I.E."
 */
fun formatInsulinUnitLabel(context: Context): String = formatInsulinUnitLabel(context.resources)

/**
 * Non-Composable / Backend function.
 * Formats an insulin value in units (IU / I.E.) for background services, notifications, etc.
 *
 * @example formatInsulinValue(1.5, resources) -> "1.50 IU" / "1.50 I.E."
 * @example formatInsulinValue(1.5, resources, withUnit = false) -> "1.50"
 * @example formatInsulinValue(1.5, resources, signed = true) -> "+1.50 IU" / "+1.50 I.E."
 * @example formatInsulinValue(null, resources) -> "-"
 */
fun formatInsulinValue(
    value: Double?,
    resources: Resources,
    default: String = "-",
    withUnit: Boolean = true,
    signed: Boolean = false
): String {
    return value?.let {
        val format = if (signed) "%+.2f" else "%.2f"
        val valStr = String.format(Locale.getDefault(), format, it)
        if (withUnit) "$valStr ${formatInsulinUnitLabel(resources)}"
        else valStr
    } ?: default
}

/**
 * Non-Composable / Backend function.
 * Formats an insulin value in units (IU / I.E.) using a [Context].
 *
 * @example formatInsulinValue(1.5, context) -> "1.50 IU" / "1.50 I.E."
 */
fun formatInsulinValue(
    value: Double?,
    context: Context,
    default: String = "-",
    withUnit: Boolean = true,
    signed: Boolean = false
): String = formatInsulinValue(value, context.resources, default = default, withUnit = withUnit, signed = signed)

/**
 * Non-Composable / Backend function.
 * Formats an [InsulinAmount] in units (IU / I.E.) for background services, notifications, etc.
 *
 * @example formatInsulinValue(amount, resources) -> "1.50 IU" / "1.50 I.E."
 */
fun formatInsulinValue(
    amount: InsulinAmount?,
    resources: Resources,
    default: String = "-",
    withUnit: Boolean = true,
    signed: Boolean = false
): String = formatInsulinValue(amount?.iu, resources, default = default, withUnit = withUnit, signed = signed)

/**
 * Non-Composable / Backend function.
 * Formats an [InsulinAmount] in units (IU / I.E.) using a [Context].
 *
 * @example formatInsulinValue(amount, context) -> "1.50 IU" / "1.50 I.E."
 */
fun formatInsulinValue(
    amount: InsulinAmount?,
    context: Context,
    default: String = "-",
    withUnit: Boolean = true,
    signed: Boolean = false
): String = formatInsulinValue(amount?.iu, context, default = default, withUnit = withUnit, signed = signed)

/**
 * Composable UI function.
 * Returns the unit label for insulin ("IU" / "I.E.") in Compose UI components.
 *
 * @example "IU" / "I.E."
 */
@Composable
fun insulinUnitLabel(): String {
    return stringResource(CommonR.string.insulin_unit)
}

/**
 * Composable UI function.
 * Formats an insulin value in units (IU / I.E.) for display in Compose UI components.
 *
 * @example insulinValue(1.5) -> "1.50 IU" / "1.50 I.E."
 * @example insulinValue(1.5, withUnit = false) -> "1.50"
 * @example insulinValue(1.5, signed = true) -> "+1.50 IU" / "+1.50 I.E."
 * @example insulinValue(null) -> "-"
 */
@Composable
fun insulinValue(value: Double?, default: String = "-", withUnit: Boolean = true, signed: Boolean = false): String {
    return formatInsulinValue(value, context = LocalContext.current, default = default, withUnit = withUnit, signed = signed)
}

/**
 * Composable UI function.
 * Formats an [InsulinAmount] in units (IU / I.E.) for display in Compose UI components.
 *
 * @example insulinValue(amount) -> "1.50 IU" / "1.50 I.E."
 */
@Composable
fun insulinValue(amount: InsulinAmount?, default: String = "-", withUnit: Boolean = true, signed: Boolean = false): String {
    return insulinValue(amount?.iu, default = default, withUnit = withUnit, signed = signed)
}

/**
 * Default step size for insulin amounts in units (IU).
 */
const val INSULIN_STEP_SIZE = 0.1

/**
 * Creates a [SteppingStrategy] tailored for insulin amounts, using [ModuloSteppingStrategy]
 * with the standard [INSULIN_STEP_SIZE] (0.1 IU) by default.
 */
fun insulinSteppingStrategy(step: Double = INSULIN_STEP_SIZE): SteppingStrategy = ModuloSteppingStrategy(step)

/**
 * Non-Composable / Backend function.
 * Returns the unit label for grams ("g").
 *
 * @example "g"
 */
fun formatCarbsGramsUnitLabel(resources: Resources): String {
    return resources.getString(CommonR.string.unit_g)
}

/**
 * Non-Composable / Backend function.
 * Returns the unit label for grams ("g").
 *
 * @example "g"
 */
fun formatCarbsGramsUnitLabel(context: Context): String = formatCarbsGramsUnitLabel(context.resources)

/**
 * Non-Composable / Backend function.
 * Returns the unit label for carbohydrate units ("KE").
 *
 * @example "KE"
 */
fun formatCarbsKeUnitLabel(resources: Resources): String {
    return resources.getString(CommonR.string.unit_ke)
}

/**
 * Non-Composable / Backend function.
 * Returns the unit label for carbohydrate units ("KE").
 *
 * @example "KE"
 */
fun formatCarbsKeUnitLabel(context: Context): String = formatCarbsKeUnitLabel(context.resources)

/**
 * Non-Composable / Backend function.
 * Formats a carbohydrate value explicitly in grams ("g") for background services, notifications, etc.
 *
 * @example formatCarbsGramsValue(20.0, resources) -> "20 g"
 * @example formatCarbsGramsValue(20.0, resources, signed = true) -> "+20 g"
 * @example formatCarbsGramsValue(null, resources) -> "-"
 */
fun formatCarbsGramsValue(
    value: Double?,
    resources: Resources,
    default: String = "-",
    withUnit: Boolean = true,
    signed: Boolean = false
): String {
    return value?.let {
        val format = if (signed) "%+.0f" else "%.0f"
        val valStr = String.format(Locale.getDefault(), format, it)
        if (withUnit) "$valStr ${formatCarbsGramsUnitLabel(resources)}"
        else valStr
    } ?: default
}

/**
 * Non-Composable / Backend function.
 * Formats a value explicitly in carbohydrate units ("KE") for background services, notifications, etc.
 * The value must already be in KE (e.g. 2.0 KE for 20g).
 *
 * @example formatCarbsKeValue(2.0, resources) -> "2 KE"
 * @example formatCarbsKeValue(1.5, resources) -> "1.5 KE"
 * @example formatCarbsKeValue(null, resources) -> "-"
 */
fun formatCarbsKeValue(
    value: Double?,
    resources: Resources,
    default: String = "-",
    withUnit: Boolean = true,
    signed: Boolean = false
): String {
    return value?.let {
        var format = if (signed) "%+" else "%"
        format += if (value % 1.0 == 0.0) {
            ".0f"
        } else {
            ".1f"
        }
        val valStr = String.format(Locale.getDefault(), format, it)
        if (withUnit) "$valStr ${formatCarbsKeUnitLabel(resources)}"
        else valStr
    } ?: default
}

/**
 * Non-Composable / Backend function.
 * Formats the given carbohydrate value (in grams) dynamically according to the selected [CarbsUnit]
 * (GRAMS or KE) for background services, notifications, etc.
 *
 * @example formatCarbsValue(20.0, CarbsUnit.GRAMS, resources) -> "20 g"
 * @example formatCarbsValue(20.0, CarbsUnit.KE, resources) -> "2 KE"
 * @example formatCarbsValue(15.0, CarbsUnit.KE, resources) -> "1.5 KE"
 * @example formatCarbsValue(null, CarbsUnit.GRAMS, resources) -> "-"
 */
fun formatCarbsValue(
    valueInGrams: Double?,
    unit: CarbsUnit,
    resources: Resources,
    default: String = "-",
    withUnit: Boolean = true,
    signed: Boolean = false
): String {
    return valueInGrams?.let {
        when (unit) {
            CarbsUnit.GRAMS -> formatCarbsGramsValue(valueInGrams, resources = resources, default = default, withUnit = withUnit, signed = signed)
            CarbsUnit.KE -> {
                val keValue = valueInGrams / 10.0
                formatCarbsKeValue(keValue, resources = resources, default = default, withUnit = withUnit, signed = signed)
            }
        }
    } ?: default
}

/**
 * Non-Composable / Backend function.
 * Formats the given carbohydrate value (in grams) dynamically according to the selected [CarbsUnit] using a [Context].
 *
 * @example formatCarbsValue(20.0, CarbsUnit.GRAMS, context) -> "20 g"
 * @example formatCarbsValue(20.0, CarbsUnit.KE, context) -> "2 KE"
 */
fun formatCarbsValue(
    valueInGrams: Double?,
    unit: CarbsUnit,
    context: Context,
    default: String = "-",
    withUnit: Boolean = true,
    signed: Boolean = false
): String = formatCarbsValue(valueInGrams, unit, context.resources, default, withUnit, signed)

/**
 * Non-Composable / Backend function.
 * Formats a carbohydrate value explicitly in grams ("g") using a [Context].
 *
 * @example formatCarbsGramsValue(20.0, context) -> "20 g"
 */
fun formatCarbsGramsValue(
    value: Double?,
    context: Context,
    default: String = "-",
    withUnit: Boolean = true,
    signed: Boolean = false
): String = formatCarbsGramsValue(value, context.resources, default, withUnit, signed)

/**
 * Non-Composable / Backend function.
 * Formats a value explicitly in KE ("KE") using a [Context].
 *
 * @example formatCarbsKeValue(2.0, context) -> "2 KE"
 */
fun formatCarbsKeValue(
    value: Double?,
    context: Context,
    default: String = "-",
    withUnit: Boolean = true,
    signed: Boolean = false
): String = formatCarbsKeValue(value, context.resources, default, withUnit, signed)

/**
 * Composable UI function.
 * Returns the unit label for the configured or provided [CarbsUnit] in Compose UI components.
 *
 * @example "g" (for CarbsUnit.GRAMS) or "KE" (for CarbsUnit.KE)
 */
@Composable
fun carbsUnitLabel(unit: CarbsUnit = LocalCarbsUnit.current): String {
    return when (unit) {
        CarbsUnit.GRAMS -> stringResource(CommonR.string.unit_g)
        CarbsUnit.KE -> stringResource(CommonR.string.unit_ke)
    }
}

/**
 * Composable UI function.
 * Formats a carbohydrate value (provided in grams) dynamically according to the provided or CompositionLocal
 * configured [CarbsUnit] (GRAMS or KE) for display in Compose UI components.
 *
 * @example carbsValue(20.0) -> "20 g" (with CarbsUnit.GRAMS)
 * @example carbsValue(20.0) -> "2 KE" (with CarbsUnit.KE)
 * @example carbsValue(15.0) -> "1.5 KE" (with CarbsUnit.KE)
 * @example carbsValue(null) -> "-"
 */
@Composable
fun carbsValue(
    valueInGrams: Double?,
    unit: CarbsUnit = LocalCarbsUnit.current,
    default: String = "-",
    withUnit: Boolean = true,
    signed: Boolean = false
): String {
    return formatCarbsValue(
        valueInGrams = valueInGrams,
        unit = unit,
        context = LocalContext.current,
        default = default,
        withUnit = withUnit,
        signed = signed
    )
}

/**
 * Composable UI function.
 * Returns the unit label for KE ("KE") in Compose UI components.
 *
 * @example "KE"
 */
@Composable
fun carbsKeUnitLabel(): String {
    return stringResource(CommonR.string.unit_ke)
}

/**
 * Composable UI function.
 * Formats a value in KE (e.g. 2.0 for 2 KE) for display in Compose UI components.
 *
 * @example carbsKeValue(2.0) -> "2 KE"
 * @example carbsKeValue(1.5) -> "1.5 KE"
 * @example carbsKeValue(null) -> "-"
 */
@Composable
fun carbsKeValue(value: Double?, default: String = "-", withUnit: Boolean = true, signed: Boolean = false): String {
    return formatCarbsKeValue(
        value = value,
        context = LocalContext.current,
        default = default,
        withUnit = withUnit,
        signed = signed
    )
}

/**
 * Composable UI function.
 * Returns the unit label for grams ("g") in Compose UI components.
 *
 * @example "g"
 */
@Composable
fun carbsGramsUnitLabel(): String {
    return stringResource(CommonR.string.unit_g)
}

/**
 * Composable UI function.
 * Formats a carbohydrate value in grams for display in Compose UI components.
 *
 * @example carbsGramsValue(20.0) -> "20 g"
 * @example carbsGramsValue(null) -> "-"
 */
@Composable
fun carbsGramsValue(value: Double?, default: String = "-", withUnit: Boolean = true, signed: Boolean = false): String {
    return formatCarbsGramsValue(
        value = value,
        context = LocalContext.current,
        default = default,
        withUnit = withUnit,
        signed = signed
    )
}

/**
 * Default step size for carbohydrate units (KE / Bread Units).
 */
const val CARBS_KE_STEP_SIZE = 0.5

/**
 * Creates a [SteppingStrategy] tailored for carbohydrate units (KE), using [ModuloSteppingStrategy]
 * with the standard [CARBS_KE_STEP_SIZE] (0.5 KE) by default.
 */
fun carbsKeSteppingStrategy(step: Double = CARBS_KE_STEP_SIZE): SteppingStrategy = ModuloSteppingStrategy(step)

/**
 * Default step size for carbohydrate grams (g).
 */
const val CARBS_GRAMS_STEP_SIZE = 5.0

/**
 * Creates a [SteppingStrategy] tailored for carbohydrate grams, using [ModuloSteppingStrategy]
 * with the standard [CARBS_GRAMS_STEP_SIZE] (5.0 g) by default.
 */
fun carbsGramsSteppingStrategy(step: Double = CARBS_GRAMS_STEP_SIZE): SteppingStrategy = ModuloSteppingStrategy(step)