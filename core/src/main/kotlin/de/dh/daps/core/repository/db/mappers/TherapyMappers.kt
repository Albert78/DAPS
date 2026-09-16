package de.dh.daps.core.repository.db.mappers

import de.dh.daps.common.model.InsulinConcentration
import de.dh.daps.common.model.InsulinType
import de.dh.daps.common.model.data.AlarmProfile
import de.dh.daps.common.model.data.BgBlock
import de.dh.daps.common.model.data.BgValue
import de.dh.daps.common.model.data.Block
import de.dh.daps.common.model.data.CurrentTherapySettings
import de.dh.daps.common.model.data.InsulinProfile
import de.dh.daps.common.model.data.Minutes
import de.dh.daps.common.model.data.ScheduledTherapyAdjustment
import de.dh.daps.core.repository.db.entities.CurrentTherapySettingsEntity
import de.dh.daps.core.repository.db.entities.DBBgBlock
import de.dh.daps.core.repository.db.entities.DBBlock
import de.dh.daps.core.repository.db.entities.InsulinProfileEntity
import de.dh.daps.core.repository.db.entities.ScheduledTherapyAdjustmentEntity

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

fun InsulinProfile.toEntity() = InsulinProfileEntity(
    id = this.id,
    name = this.name,
    basal_blocks = this.basalBlocks.map { it.toDb() },
    isf_blocks = this.isfBlocks.map { it.toDb() },
    cr_blocks = this.crBlocks.map { it.toDb() },
    insulin_type_id = this.insulinType.id,
    insulin_concentration = this.insulinConcentration.factor,
    dia = this.dia,
    peak = this.peak
)

fun InsulinProfileEntity.toModel(insulinType: InsulinType) = InsulinProfile(
    id = this.id,
    name = this.name,
    basalBlocks = this.basal_blocks.map { it.toModel() },
    isfBlocks = this.isf_blocks.map { it.toModel() },
    crBlocks = this.cr_blocks.map { it.toModel() },
    insulinType = insulinType,
    insulinConcentration = InsulinConcentration(this.insulin_concentration),
    dia = this.dia,
    peak = this.peak
)

fun CurrentTherapySettings.toEntity() = CurrentTherapySettingsEntity(
    id = this.id,
    insulin_profile_id = this.insulinProfile.id,
    default_bg_blocks = this.defaultBgBlocks.map { it.toDb() },
    insulin_adjustment_percentage = this.insulinAdjustmentPercentage,
    target_bg_override = this.targetBgOverride?.mgdlInt?.toShort(),
    low_threshold_override = this.lowThresholdOverride?.mgdlInt?.toShort(),
    alarm_profile_override_id = this.alarmProfileOverrideId,
    adjustment_hint = this.adjustmentHint,
    adjustment_end_time = this.adjustmentEndTime
)

fun CurrentTherapySettingsEntity.toModel(
    profile: InsulinProfile,
    defaultAlarmProfile: AlarmProfile? = null,
    alarmProfileOverride: AlarmProfile? = null
): CurrentTherapySettings {
    return CurrentTherapySettings(
        id = this.id,
        insulinProfile = profile,
        defaultBgBlocks = this.default_bg_blocks.map { it.toModel() },
        insulinAdjustmentPercentage = this.insulin_adjustment_percentage,
        targetBgOverride = this.target_bg_override?.let { BgValue.fromMgDl(it) },
        lowThresholdOverride = this.low_threshold_override?.let { BgValue.fromMgDl(it) },
        defaultAlarmProfile = defaultAlarmProfile,
        alarmProfileOverride = alarmProfileOverride,
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
    alarm_profile_override_id = this.effectiveAlarmProfileOverrideId,
    adjustment_hint = this.adjustmentHint
)

fun ScheduledTherapyAdjustmentEntity.toModel(alarmProfile: AlarmProfile? = null) = ScheduledTherapyAdjustment(
    id = this.id,
    startTime = this.start_time,
    endTime = this.end_time,
    percentage = this.insulin_adjustment_percentage,
    targetBgOverride = this.target_bg_override?.let { BgValue.fromMgDl(it) },
    lowThresholdOverride = this.low_threshold_override?.let { BgValue.fromMgDl(it) },
    alarmProfileOverrideId = this.alarm_profile_override_id,
    alarmProfileOverride = alarmProfile,
    adjustmentHint = this.adjustment_hint
)