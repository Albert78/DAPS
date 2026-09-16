package de.dh.daps.common.model.data

import de.dh.daps.common.model.ID_UNDEFINED

/**
 * Represents the current active therapy settings of the app.
 * It references the active [InsulinProfile].
 */
data class CurrentTherapySettings(
    var id: Long = ID_UNDEFINED,
    val insulinProfile: InsulinProfile,
    val defaultBgBlocks: List<BgBlock> = emptyList(),
    val insulinAdjustmentPercentage: Int = 0,
    val targetBgOverride: BgValue? = null,
    val lowThresholdOverride: BgValue? = null,
    val defaultAlarmProfile: AlarmProfile? = null,
    val alarmProfileOverride: AlarmProfile? = null,
    val adjustmentHint: String? = null,
    val adjustmentEndTime: Timestamp? = null
) {
    /**
     * Resolves the ID of the explicit alarm profile override, if one is set.
     */
    val alarmProfileOverrideId: Long?
        get() = alarmProfileOverride?.id

    /**
     * Resolves the effective alarm profile: the temporary override if set, otherwise the default alarm profile.
     */
    val effectiveAlarmProfile: AlarmProfile?
        get() = alarmProfileOverride ?: defaultAlarmProfile

    /**
     * Resolves the ID of the effective alarm profile.
     */
    val effectiveAlarmProfileId: Long?
        get() = effectiveAlarmProfile?.id

    /**
     * Calculates and caches the effective insulin profile considering [insulinAdjustmentPercentage].
     * Evaluated lazily once per instance.
     */
    val effectiveInsulinProfile: InsulinProfile by lazy {
        if (insulinAdjustmentPercentage == 0) {
            insulinProfile
        } else {
            val factor = (100.0 + insulinAdjustmentPercentage) / 100.0
            insulinProfile.copy(
                basalBlocks = insulinProfile.basalBlocks.map { it.copy(amount = it.amount * factor) },
                crBlocks = insulinProfile.crBlocks.map { it.copy(amount = it.amount / factor) },
                isfBlocks = insulinProfile.isfBlocks.map { it.copy(amount = (it.amount / factor)) }
            )
        }
    }
}