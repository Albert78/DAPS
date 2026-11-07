package de.dh.daps.common.domain.model.retention

import de.dh.daps.common.domain.model.data.Timestamp

/**
 * Supported retention durations in days, or [UNLIMITED] for indefinite storage.
 */
enum class RetentionOption(val days: Int?) {
    DAY_1(1),
    DAYS_3(3),
    DAYS_7(7),
    DAYS_14(14),
    DAYS_30(30),
    DAYS_90(90),
    DAYS_180(180),
    UNLIMITED(null);

    /**
     * Calculates the cutoff [Timestamp] relative to [now].
     * Returns `null` if retention is [UNLIMITED].
     */
    fun calculateCutoff(now: Timestamp): Timestamp? {
        val daysCount = days ?: return null
        return now.minusHours(daysCount * 24)
    }
}