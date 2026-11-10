package de.dh.daps.common.domain.model.therapy

import de.dh.daps.common.domain.model.alarm.AlertProfile
import de.dh.daps.common.domain.model.data.BgValue
import de.dh.daps.common.domain.model.data.Minutes
import de.dh.daps.common.domain.model.data.Timestamp

/**
 * Self-contained, immutable snapshot of therapy profile and active overlays.
 * Allows synchronous, thread-safe resolution of effective settings without database or manager lookups.
 */
data class TherapySnapshot(
    val insulinSettings: List<ProfileInsulinSetting> = emptyList(),
    val dayProfile: DayProfileTimeline,
    val defaultAlertProfile: AlertProfile,
    val activeOverlays: List<TherapyOverlay> = emptyList()
) {
    /**
     * Resolves the [ProfileInsulinSetting] (DIA and Peak) for the given [insulinType].
     * Falls back to default DIA/Peak defined on the [InsulinType] if no profile-specific override exists.
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

    /**
     * Resolves the effective therapy settings for the given [timestamp].
     */
    fun getSettingsAt(timestamp: Timestamp): EffectiveTherapySettings {
        val minute = timestamp.minutesSinceMidnight()
        val baseSegment = dayProfile.getAt(minute)
        val overlay = activeOverlays.firstOrNull { it.isActiveAt(timestamp) }
        return baseSegment.applyOverlay(overlay, defaultAlertProfile)
    }

    /**
     * Resolves the effective [AlertProfile] for the given [timestamp].
     */
    fun getAlertProfileAt(timestamp: Timestamp): AlertProfile {
        val overlay = activeOverlays.firstOrNull { it.isActiveAt(timestamp) }
        return overlay?.alertProfileOverride ?: defaultAlertProfile
    }

    /**
     * Gets the maximum target BG and maximum low threshold BG settings within the interval [from, to].
     */
    fun getMaxBgSettings(from: Timestamp, to: Timestamp): Pair<BgValue, BgValue> {
        if (from >= to) {
            val settings = getSettingsAt(from)
            return Pair(settings.targetBg, settings.lowThreshold)
        }

        var (maxTarget, maxLow) = getSettingsAt(from).let { Pair(it.targetBg, it.lowThreshold) }
        var current = from.plusMinutes(1)

        while (current < to) {
            val settings = getSettingsAt(current)
            if (settings.targetBg > maxTarget) {
                maxTarget = settings.targetBg
            }
            if (settings.lowThreshold > maxLow) {
                maxLow = settings.lowThreshold
            }
            current = current.plusMinutes(1)
        }

        val settingsTo = getSettingsAt(to)
        if (settingsTo.targetBg > maxTarget) {
            maxTarget = settingsTo.targetBg
        }
        if (settingsTo.lowThreshold > maxLow) {
            maxLow = settingsTo.lowThreshold
        }

        return Pair(maxTarget, maxLow)
    }
}