package de.dh.daps.common.domain.model.therapy

import de.dh.daps.common.ID_UNDEFINED
import de.dh.daps.common.domain.model.alarm.AlarmProfile
import de.dh.daps.common.domain.model.data.BgValue
import de.dh.daps.common.domain.model.data.Timestamp

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