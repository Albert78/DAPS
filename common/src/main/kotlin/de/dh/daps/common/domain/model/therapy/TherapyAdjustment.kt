package de.dh.daps.common.domain.model.therapy

import de.dh.daps.common.ID_UNDEFINED

/**
 * Predefined or user-configured therapy adjustment profile/preset.
 */
data class TherapyAdjustment(
    val id: Long = ID_UNDEFINED,
    val name: String,
    val percentage: Int = 0,
    val targetBgMgDl: Short? = null,
    val lowThresholdMgDl: Short? = null,
    val alarmProfileOverrideId: Long? = null
)