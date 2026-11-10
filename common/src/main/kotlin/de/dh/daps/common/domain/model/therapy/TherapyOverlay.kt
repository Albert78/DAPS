package de.dh.daps.common.domain.model.therapy

import de.dh.daps.common.domain.model.alarm.AlertProfile
import de.dh.daps.common.domain.model.data.BgDelta
import de.dh.daps.common.domain.model.data.BgValue
import de.dh.daps.common.domain.model.data.Timestamp

/**
 * Effective therapy settings resolved for a specific moment.
 */
data class EffectiveTherapySettings(
    val targetBg: BgValue,
    val lowThreshold: BgValue,
    val basalRate: InsulinAmount,
    val isf: BgDelta,
    val cr: Double,
    val alertProfile: AlertProfile
)

/**
 * An overlay representing temporary modifications to therapy settings over a specific time range.
 */
data class TherapyOverlay(
    val startTime: Timestamp,
    val endTime: Timestamp,
    val insulinAdjustmentPercentage: Int = 0,
    val targetBgOverride: BgValue? = null,
    val lowThresholdOverride: BgValue? = null,
    val alertProfileOverride: AlertProfile? = null,
    val adjustmentHint: String? = null
) {
    fun isActiveAt(timestamp: Timestamp): Boolean =
        timestamp in startTime..<endTime
}

fun DayProfileSegment.applyOverlay(
    overlay: TherapyOverlay?,
    defaultAlertProfile: AlertProfile
): EffectiveTherapySettings {
    val activeAlertProfile = overlay?.alertProfileOverride ?: defaultAlertProfile
    if (overlay == null) {
        return EffectiveTherapySettings(
            targetBg = targetBg,
            lowThreshold = lowThreshold,
            basalRate = basalRate,
            isf = isf,
            cr = cr,
            alertProfile = activeAlertProfile
        )
    }

    val percentage = overlay.insulinAdjustmentPercentage
    val factor = if (percentage != 0) 1.0 + (percentage / 100.0) else 1.0

    val effectiveBasal = if (factor != 1.0) basalRate * factor else basalRate
    val effectiveIsf = if (factor != 1.0) BgDelta.fromMgDl((isf.mgdl / factor).toInt()) else isf
    val effectiveCr = if (factor != 1.0) cr / factor else cr

    return EffectiveTherapySettings(
        targetBg = overlay.targetBgOverride ?: targetBg,
        lowThreshold = overlay.lowThresholdOverride ?: lowThreshold,
        basalRate = effectiveBasal,
        isf = effectiveIsf,
        cr = effectiveCr,
        alertProfile = activeAlertProfile
    )
}

fun CurrentTherapySettings.toOverlayOrNull(now: Timestamp = Timestamp.now()): TherapyOverlay? {
    val endTime = adjustmentEndTime ?: return null
    if (now >= endTime) return null
    return TherapyOverlay(
        startTime = now,
        endTime = endTime,
        insulinAdjustmentPercentage = insulinAdjustmentPercentage,
        targetBgOverride = targetBgOverride,
        lowThresholdOverride = lowThresholdOverride,
        alertProfileOverride = alertProfileOverride,
        adjustmentHint = adjustmentHint
    )
}

fun ScheduledTherapyAdjustment.toOverlay(): TherapyOverlay = TherapyOverlay(
    startTime = startTime,
    endTime = endTime,
    insulinAdjustmentPercentage = percentage,
    targetBgOverride = targetBgOverride,
    lowThresholdOverride = lowThresholdOverride,
    alertProfileOverride = alertProfileOverride,
    adjustmentHint = adjustmentHint
)