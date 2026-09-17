package de.dh.daps.common.model.data

import de.dh.daps.common.model.ID_UNDEFINED

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