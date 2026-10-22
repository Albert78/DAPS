package de.dh.daps.common.model.data

import de.dh.daps.common.ID_UNDEFINED
import de.dh.daps.common.model.InsulinType

/**
 * Specific DIA and Peak settings for an [InsulinType] within an [InsulinProfile].
 */
data class ProfileInsulinSetting(
    val insulinType: InsulinType,
    val dia: Minutes,
    val peak: Minutes
)

/**
 * A therapy profile that defines a set of therapy factors.
 * Profiles are used to switch between different metabolic states (e.g. Normal, Sport, Illness).
 */
data class InsulinProfile(
    var id: Long = ID_UNDEFINED,
    val name: String,
    val basalBlocks: List<Block>,
    val isfBlocks: List<Block>,
    val crBlocks: List<Block>,
    val insulinSettings: List<ProfileInsulinSetting> = emptyList()
) {
    /**
     * Resolves the [ProfileInsulinSetting] for the given [insulinType].
     * If a specific configuration exists in [insulinSettings], it is returned.
     * Otherwise, fallback to a setting using default DIA and Peak from [insulinType].
     */
    fun getSettingFor(insulinType: InsulinType): ProfileInsulinSetting {
        return insulinSettings.firstOrNull { it.insulinType.id == insulinType.id }
            ?: ProfileInsulinSetting(
                insulinType = insulinType,
                dia = insulinType.dia,
                peak = insulinType.peak
            )
    }

    /**
     * Resolves DIA for [insulinType] (customized in profile or fallback to insulinType.dia).
     */
    fun getDiaFor(insulinType: InsulinType): Minutes = getSettingFor(insulinType).dia

    /**
     * Resolves Peak for [insulinType] (customized in profile or fallback to insulinType.peak).
     */
    fun getPeakFor(insulinType: InsulinType): Minutes = getSettingFor(insulinType).peak
}