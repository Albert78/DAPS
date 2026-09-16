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
    val activeAlarmProfileId: Long? = null,
    val activeAlarmProfile: AlarmProfile? = null,
    val adjustmentHint: String? = null
) {
    fun isCurrentlyActive(now: Timestamp = Timestamp.now()): Boolean {
        return now >= startTime && now < endTime
    }
}