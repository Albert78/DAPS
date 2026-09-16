package de.dh.daps.common.model.data

import de.dh.daps.common.model.ID_UNDEFINED

/**
 * Represents a planned/scheduled therapy adjustment for a future activity or time window.
 */
data class ScheduledTherapyAdjustment(
    val id: Long = ID_UNDEFINED,
    val startTime: Timestamp,
    val endTime: Timestamp,
    val percentage: Int = 0,
    val targetBgOverride: BgValue? = null,
    val lowThresholdOverride: BgValue? = null,
    val alarmProfileOverrideId: Long? = null,
    val alarmProfileOverride: AlarmProfile? = null,
    val adjustmentHint: String? = null
) {
    /**
     * Resolves the alarm profile override ID for this scheduled adjustment.
     */
    val effectiveAlarmProfileOverrideId: Long?
        get() = alarmProfileOverrideId ?: alarmProfileOverride?.id
}