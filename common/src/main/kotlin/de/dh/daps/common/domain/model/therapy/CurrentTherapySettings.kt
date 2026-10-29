package de.dh.daps.common.domain.model.therapy

import de.dh.daps.common.ID_UNDEFINED
import de.dh.daps.common.domain.model.alarm.AlertProfile
import de.dh.daps.common.domain.model.data.BgBlock
import de.dh.daps.common.domain.model.data.BgValue
import de.dh.daps.common.domain.model.data.Timestamp

/**
 * Represents the current active therapy settings of the application.
 * References the active [InsulinProfile], default blood glucose target blocks,
 * active percentage adjustment, overrides, and default/override alert profile.
 */
data class CurrentTherapySettings(
    val id: Long = ID_UNDEFINED,
    val insulinProfile: InsulinProfile,
    val defaultBgBlocks: List<BgBlock> = emptyList(),
    val insulinAdjustmentPercentage: Int = 0,
    val targetBgOverride: BgValue? = null,
    val lowThresholdOverride: BgValue? = null,
    val defaultAlertProfile: AlertProfile? = null,
    val alertProfileOverride: AlertProfile? = null,
    val adjustmentHint: String? = null,
    val adjustmentEndTime: Timestamp? = null
) {
    /**
     * Resolves the ID of the explicit alert profile override, if one is set.
     */
    val alertProfileOverrideId: Long?
        get() = alertProfileOverride?.id

    /**
     * Effective alert profile (override if active, otherwise default).
     */
    val effectiveAlertProfile: AlertProfile?
        get() = alertProfileOverride ?: defaultAlertProfile

    /**
     * ID of the effective alert profile.
     */
    val effectiveAlertProfileId: Long?
        get() = effectiveAlertProfile?.id

    /**
     * Calculates and caches the effective insulin profile considering [insulinAdjustmentPercentage].
     * Evaluated lazily once per instance.
     * Calculates the effective [InsulinProfile] by applying the [insulinAdjustmentPercentage]
     * to the basal rates, CR and ISF blocks of [insulinProfile].
     */
    val effectiveInsulinProfile: InsulinProfile
        get() {
            if (insulinAdjustmentPercentage == 0) return insulinProfile
            val factor = 1.0 + (insulinAdjustmentPercentage / 100.0)
            return insulinProfile.copy(
                basalBlocks = insulinProfile.basalBlocks.map { it.copy(amount = it.amount * factor) },
                crBlocks = insulinProfile.crBlocks.map { it.copy(amount = it.amount / factor) },
                isfBlocks = insulinProfile.isfBlocks.map { it.copy(amount = it.amount / factor) }
            )
        }
}