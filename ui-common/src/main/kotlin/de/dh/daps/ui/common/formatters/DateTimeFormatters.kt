package de.dh.daps.ui.common.formatters

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import de.dh.daps.common.domain.model.data.Minutes
import de.dh.daps.common.domain.model.data.Timestamp
import de.dh.daps.common.domain.pump.ExpirationDate
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.abs
import de.dh.daps.common.R as CommonR

internal const val NBSP = "\u00A0"

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
 * # Naming & Architecture Conventions for Time Formatters
 *
 * All time and date formatting functions in this file follow a strict systematic naming scheme:
 *
 * ## 1. Execution Context (UI vs. Backend)
 * - **@Composable functions:** Use concise, natural names (e.g., `time()`, `shortDate()`, `relativeTimeAgo()`).
 * - **Non-Composable / Backend functions (taking `Context`/`Resources`):** Prefixed with `format...`
 *   (e.g., `formatShortDate()`, `formatRelativeTimeAgo()`).
 *
 * ## 2. Information Scope (Target Types)
 * - `time...`     -> Pure Time ("14:30")
 * - `date...`     -> Pure Date ("14.11.2026")
 * - `dateTime...` -> Combined Date & Time ("14.11.2026, 14:30")
 *
 * ## 3. Detail Level / Length
 * - `short...` -> Compact representation for lists, cards, and top bars (e.g., `shortDate`, `shortDateTime`).
 * - `long...`  -> Detailed representation for dialogs, details, and reports (e.g., `longDate`, `longDateTime`).
 *
 * ## 4. Relative Time & Directions
 * - `relativeTime...`        -> Automatic direction detection (past or future).
 * - `relativeTimeAgo...`     -> Explicit past ("vor 5 Min.", "gestern").
 * - `relativeTimeUntil...`   -> Explicit future ("in 5 Min.", "morgen").
 * - `relativeTimeMinutes...` -> Signed offset ("-10 Min.", "Jetzt", "+15 Min.").
 * - `timeDuration...`        -> Duration / Time window ("innerhalb von 30 Min.").
 *
 * ## 5. Parameter Conventions
 * - Overloading is used for input types (`Timestamp`, `LocalDate`, `LocalDateTime`, `Long`, etc.).
 * - `withUnit: Boolean = false` -> Appends unit suffixes (e.g., "Uhr").
 * - `daysOnly: Boolean = false` -> Suppresses sub-day granularity (hours/minutes).
 * - `default: String = "-"`     -> Standard fallback for nullable or invalid timestamps.
 */

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
 * @example timeUnitLabel() -> "Uhr" / ""
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
    val label = timeUnitLabel()
    return if (label.isEmpty()) time(timestamp) else "${time(timestamp)}$NBSP$label"
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

/**
 * Composable UI function.
 * Formats a [Timestamp] as a pure time ("HH:mm") if it is less than 24 hours ago,
 * otherwise as a short date-time ("dd.MM.yy, HH:mm").
 *
 * @example smartTime(timestamp) -> "14:30" (if < 24h ago)
 * @example smartTime(timestamp) -> "18.10.26, 14:30" (if >= 24h ago)
 */
@Composable
fun smartTime(timestamp: Timestamp?, default: String = "-"): String {
    if (timestamp == null || !timestamp.isValid()) return default
    val diffMs = abs(System.currentTimeMillis() - timestamp.ms)
    return if (diffMs < 24 * 60 * 60 * 1000L) {
        time(timestamp)
    } else {
        shortDateTime(timestamp, default)
    }
}

/////////////////////////////////////////////// Long date time //////////////////////////////////////

/**
 * Composable UI function.
 * Formats a [LocalDateTime] using the localized long date-time pattern.
 *
 * @example longDateTime(now) -> "14.11.2026, 14:30" / "11/14/2026, 2:30 PM"
 */
@Composable
fun longDateTime(dateTime: LocalDateTime): String {
    return dateTime.format(LocalAppFormatters.current.longDateTime)
}

/**
 * Composable UI function.
 * Formats a nullable [LocalDateTime] using the localized long date-time pattern or returns a default string.
 *
 * @example longDateTime(now) -> "14.11.2026, 14:30" / "11/14/2026, 2:30 PM"
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
 * @example longDateTime(timestamp) -> "14.11.2026, 14:30" / "11/14/2026, 2:30 PM"
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
 * @example shortDateTime(now) -> "14.11.26, 14:30" / "Nov 14, 2:30 PM"
 * @example shortDateTime(null) -> "-"
 */
@Composable
fun shortDateTime(dateTime: LocalDateTime?, default: String = "-"): String {
    return dateTime?.let {
        shortDateTime(dateTime)
    } ?: default
}

/**
 * Composable UI function.
 * Formats a nullable [Timestamp] using the localized short date-time pattern or returns a default string.
 *
 * @example shortDateTime(timestamp) -> "14.11.26, 14:30" / "Nov 14, 2:30 PM"
 * @example shortDateTime(null) -> "-"
 */
@Composable
fun shortDateTime(timestamp: Timestamp?, default: String = "-"): String {
    return if (timestamp != null && timestamp.isValid()) {
        val ldt = Instant.ofEpochMilli(timestamp.ms)
            .atZone(ZoneId.systemDefault())
            .toLocalDateTime()
        shortDateTime(ldt, default)
    } else default
}

/**
 * Non-Composable / Backend function.
 * Formats a [LocalDateTime] as a short date-time string using the localized pattern from string resources.
 *
 * @example formatShortDateTime(dateTime, context) -> "14.11.26, 14:30"
 */
fun formatShortDateTime(dateTime: LocalDateTime, context: Context): String {
    val pattern = context.getString(CommonR.string.short_date_time_format)
    return dateTime.format(DateTimeFormatter.ofPattern(pattern, Locale.getDefault()))
}

/**
 * Non-Composable / Backend function.
 * Formats a nullable [Timestamp] as a short date-time string using the localized pattern from string resources.
 *
 * @example formatShortDateTime(timestamp, context) -> "14.11.26, 14:30"
 * @example formatShortDateTime(null, context) -> "-"
 */
fun formatShortDateTime(timestamp: Timestamp?, context: Context, default: String = "-"): String {
    return if (timestamp != null && timestamp.isValid()) {
        val ldt = Instant.ofEpochMilli(timestamp.ms)
            .atZone(ZoneId.systemDefault())
            .toLocalDateTime()
        formatShortDateTime(ldt, context)
    } else default
}

/////////////////////////////////////////////// Long date //////////////////////////////////////

/**
 * Composable UI function.
 * Formats a [LocalDate] using the localized long date pattern.
 *
 * @example longDate(date) -> "14. November 2026" / "November 14, 2026"
 */
@Composable
fun longDate(date: LocalDate): String {
    return date.format(LocalAppFormatters.current.longDate)
}

/**
 * Composable UI function.
 * Formats a nullable [LocalDate] using the localized long date pattern or returns a default string.
 *
 * @example longDate(date) -> "14. November 2026" / "November 14, 2026"
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
 * @example longDate(dateTime) -> "14. November 2026" / "November 14, 2026"
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
 * @example shortDate(date) -> "14.11.2026" / "11/14/2026"
 */
@Composable
fun shortDate(date: LocalDate): String {
    return date.format(LocalAppFormatters.current.shortDate)
}

/**
 * Composable UI function.
 * Formats a nullable [LocalDate] using the localized short date pattern or returns a default string.
 *
 * @example shortDate(date) -> "14.11.2026" / "11/14/2026"
 * @example shortDate(null) -> "-"
 */
@Composable
fun shortDate(date: LocalDate?, default: String = "-"): String {
    return date?.let {
        shortDate(date)
    } ?: default
}

/**
 * Non-Composable / Backend function.
 * Formats a [LocalDate] as a short date string using the localized pattern from string resources.
 *
 * @example formatShortDate(date, context) -> "14.11.26"
 */
fun formatShortDate(date: LocalDate, context: Context): String {
    val pattern = context.getString(CommonR.string.short_date_format)
    return date.format(DateTimeFormatter.ofPattern(pattern, Locale.getDefault()))
}

/**
 * Non-Composable / Backend function.
 * Formats a nullable [LocalDate] as a short date string using the localized pattern from string resources.
 *
 * @example formatShortDate(date, context) -> "14.11.26"
 * @example formatShortDate(null, context) -> "-"
 */
fun formatShortDate(date: LocalDate?, context: Context, default: String = "-"): String {
    return date?.let { formatShortDate(it, context) } ?: default
}

/////////////////////////////////////////////// Time ago //////////////////////////////////////

/**
 * Composable UI function.
 * Formats a millisecond time difference into a relative past time description.
 *
 * @param daysOnly If true, skips sub-day time resolutions (hours, minutes, seconds) and formats as days ("heute", "gestern", "vor 3 Tagen").
 * @example relativeTimeAgo(3000) -> "Gerade eben" / "just now"
 * @example relativeTimeAgo(300000) -> "vor 5 Min." / "5 min ago"
 * @example relativeTimeAgo(7200000) -> "vor 2 Std." / "2 h ago"
 * @example relativeTimeAgo(86400000) -> "gestern" / "yesterday"
 * @example relativeTimeAgo(259200000) -> "vor 3 Tagen" / "3 days ago"
 */
@Composable
fun relativeTimeAgo(diffMs: Long, daysOnly: Boolean = false): String {
    val diffSec = diffMs / 1000
    val diffMin = diffMs / 60000
    val diffHours = diffMin / 60
    val diffDays = diffHours / 24
    return when {
        !daysOnly && diffSec < 5 -> stringResource(CommonR.string.time_ago_just_now)
        !daysOnly && diffSec < 61 -> stringResource(CommonR.string.time_ago_seconds_ago, diffSec)
        !daysOnly && diffMin < 1 -> stringResource(CommonR.string.time_ago_just_now)
        !daysOnly && diffMin < 91 -> stringResource(CommonR.string.time_ago_minutes_ago, diffMin)
        !daysOnly && diffHours < 24 -> stringResource(CommonR.string.time_ago_hours_ago, diffHours)
        else -> pluralStringResourceZero(CommonR.plurals.time_ago_days, CommonR.string.time_today, diffDays.toInt(), diffDays)
    }
}

/**
 * Composable UI function.
 * Formats a past [Timestamp] into a relative past time description.
 *
 * @example relativeTimeAgo(timestamp) -> "vor 5 Min." / "5 min ago"
 */
@Composable
fun relativeTimeAgo(timestamp: Timestamp, daysOnly: Boolean = false): String {
    val diffMs = System.currentTimeMillis() - timestamp.ms
    return relativeTimeAgo(diffMs, daysOnly = daysOnly)
}

/**
 * Composable UI function.
 * Formats a millisecond time difference into a relative future time description.
 *
 * @param daysOnly If true, skips sub-day time resolutions (hours, minutes, seconds) and formats as days ("heute", "morgen", "in 3 Tagen").
 * @example relativeTimeUntil(300000) -> "in 5 Min." / "in 5 min"
 * @example relativeTimeUntil(7200000) -> "in 2 Std." / "in 2 h"
 * @example relativeTimeUntil(86400000) -> "morgen" / "tomorrow"
 * @example relativeTimeUntil(259200000) -> "in 3 Tagen" / "in 3 days"
 */
@Composable
fun relativeTimeUntil(diffMs: Long, daysOnly: Boolean = false): String {
    val diffSec = diffMs / 1000
    val diffMin = diffMs / 60000
    val diffHours = diffMin / 60
    val diffDays = diffHours / 24
    return when {
        !daysOnly && diffSec < 5 -> stringResource(CommonR.string.time_until_just_now)
        !daysOnly && diffSec < 61 -> stringResource(CommonR.string.time_until_seconds, diffSec)
        !daysOnly && diffMin < 1 -> stringResource(CommonR.string.time_until_just_now)
        !daysOnly && diffMin < 91 -> stringResource(CommonR.string.time_until_minutes, diffMin)
        !daysOnly && diffHours < 24 -> stringResource(CommonR.string.time_until_hours, diffHours)
        else -> pluralStringResourceZero(CommonR.plurals.time_until_days, CommonR.string.time_today, diffDays.toInt(), diffDays)
    }
}

/**
 * Composable UI function.
 * Formats a future [Timestamp] into a relative future time description.
 *
 * @example relativeTimeUntil(timestamp) -> "in 5 Min." / "in 5 min"
 */
@Composable
fun relativeTimeUntil(timestamp: Timestamp, daysOnly: Boolean = false): String {
    val diffMs = timestamp.ms - System.currentTimeMillis()
    return relativeTimeUntil(diffMs, daysOnly = daysOnly)
}

/**
 * Composable UI function.
 * Formats a [Timestamp] into a relative time description (past or future automatically).
 *
 * @example relativeTime(pastTimestamp) -> "vor 5 Min." / "5 min ago"
 * @example relativeTime(futureTimestamp) -> "in 5 Min." / "in 5 min"
 */
@Composable
fun relativeTime(timestamp: Timestamp): String {
    val now = System.currentTimeMillis()
    return if (timestamp.ms < now) {
        relativeTimeAgo(timestamp)
    } else {
        relativeTimeUntil(timestamp)
    }
}

/**
 * Composable UI function.
 * Formats a [LocalDate] into a relative date description ("heute", "morgen", "gestern", "in 3 Tagen", "vor 3 Tagen").
 *
 * @example relativeTime(today) -> "heute"
 * @example relativeTime(tomorrow) -> "morgen"
 * @example relativeTime(yesterday) -> "gestern"
 */
@Composable
fun relativeTime(date: LocalDate): String {
    val today = LocalDate.now()
    val daysDiff = ChronoUnit.DAYS.between(today, date)
    return if (daysDiff < 0) {
        pluralStringResourceZero(
            CommonR.plurals.time_ago_days,
            CommonR.string.time_today,
            (-daysDiff).toInt(),
            -daysDiff
        )
    } else {
        pluralStringResourceZero(
            CommonR.plurals.time_until_days,
            CommonR.string.time_today,
            daysDiff.toInt(),
            daysDiff
        )
    }
}

/**
 * Composable UI function.
 * Formats an [ExpirationDate] into a relative time description (past or future automatically).
 * For [ExpirationDate.Approximate], only the calendar day offset is formatted ("heute", "morgen", "in 3 Tagen", etc.).
 *
 * @example relativeTime(expirationDate) -> "vor 2 Std." / "in 3 Tagen" / "heute"
 */
@Composable
fun relativeTime(expirationDate: ExpirationDate): String {
    return when (expirationDate) {
        is ExpirationDate.Hard -> relativeTime(expirationDate.dateTime)
        is ExpirationDate.Approximate -> relativeTime(expirationDate.date)
    }
}

/**
 * Non-Composable / Backend function.
 * Formats a millisecond time difference into a relative past time description using a [Context].
 *
 * @param daysOnly If true, skips sub-day time resolutions (hours, minutes, seconds) and formats as days ("heute", "gestern", "vor 3 Tagen").
 * @example formatRelativeTimeAgo(3000, context) -> "Gerade eben" / "just now"
 * @example formatRelativeTimeAgo(300000, context) -> "vor 5 Min." / "5 min ago"
 * @example formatRelativeTimeAgo(7200000, context) -> "vor 2 Std." / "2 h ago"
 * @example formatRelativeTimeAgo(86400000, context) -> "gestern" / "yesterday"
 * @example formatRelativeTimeAgo(259200000, context) -> "vor 3 Tagen" / "3 days ago"
 */
fun formatRelativeTimeAgo(diffMs: Long, context: Context, daysOnly: Boolean = false): String {
    val diffSec = diffMs / 1000
    val diffMin = diffMs / 60000
    val diffHours = diffMin / 60
    val diffDays = diffHours / 24
    return when {
        !daysOnly && diffSec < 5 -> context.getString(CommonR.string.time_ago_just_now)
        !daysOnly && diffSec < 61 -> context.getString(CommonR.string.time_ago_seconds_ago, diffSec)
        !daysOnly && diffMin < 1 -> context.getString(CommonR.string.time_ago_just_now)
        !daysOnly && diffMin < 91 -> context.getString(CommonR.string.time_ago_minutes_ago, diffMin)
        !daysOnly && diffHours < 24 -> context.getString(CommonR.string.time_ago_hours_ago, diffHours)
        else -> getQuantityStringZero(
            context.resources,
            CommonR.plurals.time_ago_days,
            CommonR.string.time_today,
            diffDays.toInt(),
            diffDays
        )
    }
}

/**
 * Non-Composable / Backend function.
 * Formats a past [Timestamp] into a relative past time description using a [Context].
 *
 * @example formatRelativeTimeAgo(timestamp, context) -> "vor 5 Min." / "5 min ago"
 */
fun formatRelativeTimeAgo(timestamp: Timestamp, context: Context, daysOnly: Boolean = false): String {
    val diffMs = System.currentTimeMillis() - timestamp.ms
    return formatRelativeTimeAgo(diffMs, context, daysOnly = daysOnly)
}

/**
 * Non-Composable / Backend function.
 * Formats a millisecond time difference into a relative future time description using a [Context].
 *
 * @param daysOnly If true, skips sub-day time resolutions (hours, minutes, seconds) and formats as days ("heute", "morgen", "in 3 Tagen").
 * @example formatRelativeTimeUntil(300000, context) -> "in 5 Min." / "in 5 min"
 * @example formatRelativeTimeUntil(7200000, context) -> "in 2 Std." / "in 2 h"
 * @example formatRelativeTimeUntil(86400000, context) -> "morgen" / "tomorrow"
 * @example formatRelativeTimeUntil(259200000, context) -> "in 3 Tagen" / "in 3 days"
 */
fun formatRelativeTimeUntil(diffMs: Long, context: Context, daysOnly: Boolean = false): String {
    val diffSec = diffMs / 1000
    val diffMin = diffMs / 60000
    val diffHours = diffMin / 60
    val diffDays = diffHours / 24
    return when {
        !daysOnly && diffSec < 5 -> context.getString(CommonR.string.time_until_just_now)
        !daysOnly && diffSec < 61 -> context.getString(CommonR.string.time_until_seconds, diffSec)
        !daysOnly && diffMin < 1 -> context.getString(CommonR.string.time_until_just_now)
        !daysOnly && diffMin < 91 -> context.getString(CommonR.string.time_until_minutes, diffMin)
        !daysOnly && diffHours < 24 -> context.getString(CommonR.string.time_until_hours, diffHours)
        else -> getQuantityStringZero(
            context.resources,
            CommonR.plurals.time_until_days,
            CommonR.string.time_today,
            diffDays.toInt(),
            diffDays
        )
    }
}

/**
 * Non-Composable / Backend function.
 * Formats a future [Timestamp] into a relative future time description using a [Context].
 *
 * @example formatRelativeTimeUntil(timestamp, context) -> "in 5 Min." / "in 5 min"
 */
fun formatRelativeTimeUntil(timestamp: Timestamp, context: Context, daysOnly: Boolean = false): String {
    val diffMs = timestamp.ms - System.currentTimeMillis()
    return formatRelativeTimeUntil(diffMs, context, daysOnly = daysOnly)
}

/**
 * Non-Composable / Backend function.
 * Formats a [Timestamp] into a relative time description (past or future automatically) using a [Context].
 *
 * @example formatRelativeTime(pastTimestamp, context) -> "vor 5 Min." / "5 min ago"
 * @example formatRelativeTime(futureTimestamp, context) -> "in 5 Min." / "in 5 min"
 */
fun formatRelativeTime(timestamp: Timestamp, context: Context): String {
    val now = System.currentTimeMillis()
    return if (timestamp.ms < now) {
        formatRelativeTimeAgo(timestamp, context)
    } else {
        formatRelativeTimeUntil(timestamp, context)
    }
}

/**
 * Non-Composable / Backend function.
 * Formats a [LocalDate] into a relative date description ("heute", "morgen", "gestern", "in 3 Tagen", "vor 3 Tagen") using a [Context].
 *
 * @example formatRelativeTime(today, context) -> "heute"
 * @example formatRelativeTime(tomorrow, context) -> "morgen"
 * @example formatRelativeTime(yesterday, context) -> "gestern"
 */
fun formatRelativeTime(date: LocalDate, context: Context): String {
    val today = LocalDate.now()
    val daysDiff = ChronoUnit.DAYS.between(today, date)
    return if (daysDiff < 0) {
        val quantity = (-daysDiff).toInt()
        getQuantityStringZero(
            context.resources,
            CommonR.plurals.time_ago_days,
            CommonR.string.time_today,
            quantity,
            quantity
        )
    } else {
        val quantity = daysDiff.toInt()
        getQuantityStringZero(
            context.resources,
            CommonR.plurals.time_until_days,
            CommonR.string.time_today,
            quantity,
            quantity
        )
    }
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
 * @example timeDuration(minutes) -> "innerhalb von 30 Min." / "within 30 min"
 * @example timeDuration(minutes) -> "innerhalb von 1 Std. 30 Min." / "within 1 h 30 min"
 */
@Composable
fun timeDuration(minutes: Minutes): String {
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