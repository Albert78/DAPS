package de.dh.daps.common.domain.model.therapy

import de.dh.daps.common.ID_UNDEFINED
import de.dh.daps.common.domain.model.alarm.AlertProfile
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
    val alertProfileOverrideId: Long? = null,
    val alertProfileOverride: AlertProfile? = null,
    val adjustmentHint: String? = null
) {
    /**
     * Resolves the alert profile override ID for this scheduled adjustment.
     */
    val effectiveAlertProfileOverrideId: Long?
        get() = alertProfileOverrideId ?: alertProfileOverride?.id
}