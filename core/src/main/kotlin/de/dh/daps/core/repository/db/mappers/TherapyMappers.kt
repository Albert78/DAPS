package de.dh.daps.core.repository.db.mappers

import de.dh.daps.common.domain.model.alarm.AlertProfile
import de.dh.daps.common.domain.model.data.BgValue
import de.dh.daps.common.domain.model.data.Minutes
import de.dh.daps.common.domain.model.data.BgBlock
import de.dh.daps.common.domain.model.data.Block
import de.dh.daps.common.domain.model.therapy.CurrentTherapySettings
import de.dh.daps.common.domain.model.therapy.InsulinProfile
import de.dh.daps.common.domain.model.therapy.InsulinType
import de.dh.daps.common.domain.model.therapy.ProfileInsulinSetting
import de.dh.daps.common.domain.model.therapy.ScheduledTherapyAdjustment
import de.dh.daps.common.domain.model.therapy.TherapyAdjustment
import de.dh.daps.core.repository.db.entities.CurrentTherapySettingsEntity
import de.dh.daps.core.repository.db.entities.DBBgBlock
import de.dh.daps.core.repository.db.entities.DBBlock
import de.dh.daps.core.repository.db.entities.DBInsulinSetting
import de.dh.daps.core.repository.db.entities.InsulinProfileEntity
import de.dh.daps.core.repository.db.entities.ScheduledTherapyAdjustmentEntity
import de.dh.daps.core.repository.db.entities.TherapyAdjustmentEntity

// Therapy Converters
fun Block.toDb() = DBBlock(
    duration = this.duration.value,
    amount = this.amount
)

fun DBBlock.toModel() = Block(
    duration = Minutes(this.duration),
    amount = this.amount
)

fun BgBlock.toDb() = DBBgBlock(
    duration = this.duration.value,
    target = this.target.mgdlInt.toShort(),
    lowThreshold = this.lowThreshold.mgdlInt.toShort()
)

fun DBBgBlock.toModel() = BgBlock(
    duration = Minutes(this.duration),
    target = BgValue.fromMgDl(this.target),
    lowThreshold = BgValue.fromMgDl(this.lowThreshold)
)

fun ProfileInsulinSetting.toDb() = DBInsulinSetting(
    insulinTypeId = this.insulinType.id,
    dia = this.dia.value,
    peak = this.peak.value
)

fun InsulinProfile.toEntity() = InsulinProfileEntity(
    id = this.id,
    name = this.name,
    basal_blocks = this.basalBlocks.map { it.toDb() },
    isf_blocks = this.isfBlocks.map { it.toDb() },
    cr_blocks = this.crBlocks.map { it.toDb() },
    insulin_settings = this.insulinSettings.map { it.toDb() }
)

fun InsulinProfileEntity.toModel(availableInsulinTypes: List<InsulinType>): InsulinProfile {
    val typesMap = availableInsulinTypes.associateBy { it.id }
    val settings = this.insulin_settings.mapNotNull { dbSetting ->
        val type = typesMap[dbSetting.insulinTypeId]
        if (type != null) {
            ProfileInsulinSetting(
                insulinType = type,
                dia = Minutes(dbSetting.dia),
                peak = Minutes(dbSetting.peak)
            )
        } else null
    }
    return InsulinProfile(
        id = this.id,
        name = this.name,
        basalBlocks = this.basal_blocks.map { it.toModel() },
        isfBlocks = this.isf_blocks.map { it.toModel() },
        crBlocks = this.cr_blocks.map { it.toModel() },
        insulinSettings = settings
    )
}

fun CurrentTherapySettings.toEntity() = CurrentTherapySettingsEntity(
    id = this.id,
    insulin_profile_id = this.insulinProfile.id,
    default_bg_blocks = this.defaultBgBlocks.map { it.toDb() },
    insulin_adjustment_percentage = this.insulinAdjustmentPercentage,
    target_bg_override = this.targetBgOverride?.mgdlInt?.toShort(),
    low_threshold_override = this.lowThresholdOverride?.mgdlInt?.toShort(),
    alarm_profile_override_id = this.alertProfileOverrideId,
    adjustment_hint = this.adjustmentHint,
    adjustment_end_time = this.adjustmentEndTime
)

fun CurrentTherapySettingsEntity.toModel(
    profile: InsulinProfile,
    defaultAlertProfile: AlertProfile? = null,
    alertProfileOverride: AlertProfile? = null
): CurrentTherapySettings {
    return CurrentTherapySettings(
        id = this.id,
        insulinProfile = profile,
        defaultBgBlocks = this.default_bg_blocks.map { it.toModel() },
        insulinAdjustmentPercentage = this.insulin_adjustment_percentage,
        targetBgOverride = this.target_bg_override?.let { BgValue.fromMgDl(it) },
        lowThresholdOverride = this.low_threshold_override?.let { BgValue.fromMgDl(it) },
        defaultAlertProfile = defaultAlertProfile,
        alertProfileOverride = alertProfileOverride,
        adjustmentHint = this.adjustment_hint,
        adjustmentEndTime = this.adjustment_end_time
    )
}

fun ScheduledTherapyAdjustment.toEntity() = ScheduledTherapyAdjustmentEntity(
    id = this.id,
    start_time = this.startTime,
    end_time = this.endTime,
    insulin_adjustment_percentage = this.percentage,
    target_bg_override = this.targetBgOverride?.mgdlInt?.toShort(),
    low_threshold_override = this.lowThresholdOverride?.mgdlInt?.toShort(),
    alarm_profile_override_id = this.effectiveAlertProfileOverrideId,
    adjustment_hint = this.adjustmentHint
)

fun ScheduledTherapyAdjustmentEntity.toModel(alertProfile: AlertProfile? = null) = ScheduledTherapyAdjustment(
    id = this.id,
    startTime = this.start_time,
    endTime = this.end_time,
    percentage = this.insulin_adjustment_percentage,
    targetBgOverride = this.target_bg_override?.let { BgValue.fromMgDl(it) },
    lowThresholdOverride = this.low_threshold_override?.let { BgValue.fromMgDl(it) },
    alertProfileOverrideId = this.alarm_profile_override_id,
    alertProfileOverride = alertProfile,
    adjustmentHint = this.adjustment_hint
)

fun TherapyAdjustment.toEntity() = TherapyAdjustmentEntity(
    id = this.id,
    name = this.name,
    percentage = this.percentage,
    target_bg_override = this.targetBgMgDl,
    low_threshold_override = this.lowThresholdMgDl,
    alarm_profile_override_id = this.alertProfileOverrideId
)

fun TherapyAdjustmentEntity.toModel() = TherapyAdjustment(
    id = this.id,
    name = this.name,
    percentage = this.percentage,
    targetBgMgDl = this.target_bg_override,
    lowThresholdMgDl = this.low_threshold_override,
    alertProfileOverrideId = this.alarm_profile_override_id
)