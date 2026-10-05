package de.dh.daps.core.backup

import de.dh.daps.common.model.ApsMode
import de.dh.daps.common.model.InsulinAmount
import de.dh.daps.common.model.InsulinOrigin
import de.dh.daps.common.model.InsulinStatus
import de.dh.daps.common.model.data.BgDelta
import de.dh.daps.common.model.data.BgSampleKind
import de.dh.daps.common.model.data.BgValue
import de.dh.daps.common.model.data.Minutes
import de.dh.daps.common.model.data.Timestamp
import de.dh.daps.core.aps.CoreReasoning
import de.dh.daps.core.repository.db.entities.AlarmProfileEntity
import de.dh.daps.core.repository.db.entities.CoreInsightEntity
import de.dh.daps.core.repository.db.entities.CurrentSettingsEntity
import de.dh.daps.core.repository.db.entities.CurrentTherapySettingsEntity
import de.dh.daps.core.repository.db.entities.DBBgBlock
import de.dh.daps.core.repository.db.entities.DBBlock
import de.dh.daps.core.repository.db.entities.DataProviderEntity
import de.dh.daps.core.repository.db.entities.DeferredBolusEntity
import de.dh.daps.core.repository.db.entities.GlucoseReadingEntity
import de.dh.daps.core.repository.db.entities.InsulinEntity
import de.dh.daps.core.repository.db.entities.InsulinProfileEntity
import de.dh.daps.core.repository.db.entities.InsulinTypeEntity
import de.dh.daps.core.repository.db.entities.MealEntity
import de.dh.daps.core.repository.db.entities.MealReminderEntity
import de.dh.daps.core.repository.db.entities.MealTypeEntity
import de.dh.daps.core.repository.db.entities.ScheduledTherapyAdjustmentEntity
import de.dh.daps.core.repository.db.entities.SensorTypeEntity
import de.dh.daps.core.repository.db.entities.TherapyAdjustmentEntity
import kotlinx.serialization.Serializable

@Serializable
data class BackupOptions(
    val includeHistory: Boolean = true,
    val historyDaysFilter: Int? = null, // null = all, otherwise e.g. 30, 90 days
    val includeDiagnostics: Boolean = false,
    val includeDescriptors: Boolean = true
)

@Serializable
data class BackupManifestDto(
    val version: Int = 1,
    val appVersion: String = "1.0",
    val schemaVersion: Int = 2,
    val createdAtMs: Long = System.currentTimeMillis(),
    val includeHistory: Boolean = true,
    val includeDiagnostics: Boolean = false
)

@Serializable
data class AppPreferencesDto(
    val glucoseUnit: String,
    val carbsUnit: String,
    val glucoseSourceDescriptorJson: String? = null,
    val pumpDescriptorJson: String? = null
)

@Serializable
data class DBBlockDto(val duration: Short, val amount: Double)

@Serializable
data class DBBgBlockDto(val duration: Short, val target: Short, val lowThreshold: Short)

@Serializable
data class InsulinProfileDto(
    val id: Long,
    val name: String,
    val basalBlocks: List<DBBlockDto>,
    val isfBlocks: List<DBBlockDto>,
    val crBlocks: List<DBBlockDto>,
    val insulinTypeId: String,
    val insulinConcentration: Double,
    val diaMinutes: Short,
    val peakMinutes: Short
)

@Serializable
data class CurrentTherapySettingsDto(
    val id: Long,
    val insulinProfileId: Long,
    val defaultBgBlocks: List<DBBgBlockDto>,
    val insulinAdjustmentPercentage: Int,
    val targetBgOverride: Short? = null,
    val lowThresholdOverride: Short? = null,
    val alarmProfileOverrideId: Long? = null,
    val adjustmentHint: String? = null,
    val adjustmentEndTimeMs: Long? = null
)

@Serializable
data class TherapyAdjustmentPresetDto(
    val id: Long,
    val name: String,
    val percentage: Int,
    val targetBgOverride: Short? = null,
    val lowThresholdOverride: Short? = null,
    val alarmProfileOverrideId: Long? = null
)

@Serializable
data class CurrentSettingsDto(
    val id: Long,
    val apsMode: String
)

@Serializable
data class AlarmProfileDto(
    val id: Long,
    val name: String,
    val isDefault: Boolean,
    val isActive: Boolean,
    val severityDefaultsJson: String,
    val customOverridesJson: String
)

@Serializable
data class MealTypeDto(
    val id: String,
    val name: String,
    val symbol: String? = null,
    val curveComponents: String,
    val catMinutes: Short,
    val sortOrder: Int = 0
)

@Serializable
data class InsulinTypeDto(
    val id: String,
    val name: String,
    val activeSubstance: String?,
    val peakMinutes: Short,
    val diaMinutes: Short,
    val defaultConcentration: Double = 1.0
)

@Serializable
data class SensorTypeDto(val id: Long, val name: String)

@Serializable
data class DataProviderDto(val id: Long, val name: String)

@Serializable
data class TherapyConfigDto(
    val insulinProfiles: List<InsulinProfileDto> = emptyList(),
    val currentTherapySettings: CurrentTherapySettingsDto? = null,
    val therapyAdjustments: List<TherapyAdjustmentPresetDto> = emptyList(),
    val currentSettings: CurrentSettingsDto? = null,
    val alarmProfiles: List<AlarmProfileDto> = emptyList(),
    val mealTypes: List<MealTypeDto> = emptyList(),
    val insulinTypes: List<InsulinTypeDto> = emptyList(),
    val sensorTypes: List<SensorTypeDto> = emptyList(),
    val dataProviders: List<DataProviderDto> = emptyList()
)

@Serializable
data class GlucoseReadingDto(
    val id: Long,
    val valueMgdl: Short,
    val sampleKind: String,
    val timestampMs: Long,
    val fkDataProvider: Long,
    val fkSourceSensor: Long
)

@Serializable
data class MealDto(
    val id: Long,
    val mealTypeId: String,
    val timestampMs: Long,
    val carbGrams: Double,
    val description: String = "",
    val administeredInsulinIu: Double = 0.0
)

@Serializable
data class InsulinApplicationDto(
    val id: Long,
    val insulinTypeId: String,
    val timestampMs: Long,
    val amountIu: Double,
    val origin: String,
    val basal: Boolean = false,
    val correction: Boolean = false,
    val meal: Boolean = false,
    val status: String,
    val pumpId: String? = null
)

@Serializable
data class DeferredBolusDto(
    val id: Long,
    val timestampMs: Long,
    val amountIu: Double,
    val mealId: Long? = null
)

@Serializable
data class ScheduledTherapyAdjustmentDto(
    val id: Long,
    val startTimeMs: Long,
    val endTimeMs: Long,
    val insulinAdjustmentPercentage: Int = 0,
    val targetBgOverride: Short? = null,
    val lowThresholdOverride: Short? = null,
    val alarmProfileOverrideId: Long? = null,
    val adjustmentHint: String? = null
)

@Serializable
data class MealReminderDto(
    val id: Long,
    val mealId: Long? = null,
    val mealTimestampMs: Long,
    val reminderTimestampMs: Long,
    val description: String = ""
)

@Serializable
data class MedicalHistoryDto(
    val glucoseReadings: List<GlucoseReadingDto> = emptyList(),
    val meals: List<MealDto> = emptyList(),
    val insulinApplications: List<InsulinApplicationDto> = emptyList(),
    val deferredBoluses: List<DeferredBolusDto> = emptyList(),
    val scheduledTherapyAdjustments: List<ScheduledTherapyAdjustmentDto> = emptyList(),
    val mealReminders: List<MealReminderDto> = emptyList()
)

@Serializable
data class CoreInsightDto(
    val id: Long,
    val timestampMs: Long,
    val bgOriginalMgdlScaled: Int,
    val bgFilteredMgdlScaled: Int,
    val deviationPerTickScaled: Int,
    val futureActiveInsulinIu: Double,
    val futureActiveCarbsGrams: Double,
    val predictedBgAtPeakScaled: Int,
    val targetBgScaled: Int,
    val isfScaled: Int,
    val cr: Double,
    val actionBolusIu: Double? = null,
    val actionTempBasalPercent: Int? = null,
    val actionTempBasalDurationInHours: Int? = null,
    val reasoning: String
)

@Serializable
data class DiagnosticsDto(
    val insights: List<CoreInsightDto> = emptyList()
)

sealed class BackupResult {
    data class Success(val manifest: BackupManifestDto) : BackupResult()
    data class Error(val exception: Throwable) : BackupResult()
}

// Entity <-> DTO Mapping Functions

fun DBBlock.toDto() = DBBlockDto(duration, amount)
fun DBBlockDto.toEntity() = DBBlock(duration, amount)

fun DBBgBlock.toDto() = DBBgBlockDto(duration, target, lowThreshold)
fun DBBgBlockDto.toEntity() = DBBgBlock(duration, target, lowThreshold)

fun InsulinProfileEntity.toDto() = InsulinProfileDto(
    id = id,
    name = name,
    basalBlocks = basal_blocks.map { it.toDto() },
    isfBlocks = isf_blocks.map { it.toDto() },
    crBlocks = cr_blocks.map { it.toDto() },
    insulinTypeId = insulin_type_id,
    insulinConcentration = insulin_concentration,
    diaMinutes = dia.value,
    peakMinutes = peak.value
)

fun InsulinProfileDto.toEntity() = InsulinProfileEntity(
    id = id,
    name = name,
    basal_blocks = basalBlocks.map { it.toEntity() },
    isf_blocks = isfBlocks.map { it.toEntity() },
    cr_blocks = crBlocks.map { it.toEntity() },
    insulin_type_id = insulinTypeId,
    insulin_concentration = insulinConcentration,
    dia = Minutes(diaMinutes),
    peak = Minutes(peakMinutes)
)

fun CurrentTherapySettingsEntity.toDto() = CurrentTherapySettingsDto(
    id = id,
    insulinProfileId = insulin_profile_id,
    defaultBgBlocks = default_bg_blocks.map { it.toDto() },
    insulinAdjustmentPercentage = insulin_adjustment_percentage,
    targetBgOverride = target_bg_override,
    lowThresholdOverride = low_threshold_override,
    alarmProfileOverrideId = alarm_profile_override_id,
    adjustmentHint = adjustment_hint,
    adjustmentEndTimeMs = adjustment_end_time?.ms
)

fun CurrentTherapySettingsDto.toEntity() = CurrentTherapySettingsEntity(
    id = id,
    insulin_profile_id = insulinProfileId,
    default_bg_blocks = defaultBgBlocks.map { it.toEntity() },
    insulin_adjustment_percentage = insulinAdjustmentPercentage,
    target_bg_override = targetBgOverride,
    low_threshold_override = lowThresholdOverride,
    alarm_profile_override_id = alarmProfileOverrideId,
    adjustment_hint = adjustmentHint,
    adjustment_end_time = adjustmentEndTimeMs?.let { Timestamp(it) }
)

fun ScheduledTherapyAdjustmentEntity.toDto() = ScheduledTherapyAdjustmentDto(
    id = id,
    startTimeMs = start_time.ms,
    endTimeMs = end_time.ms,
    insulinAdjustmentPercentage = insulin_adjustment_percentage,
    targetBgOverride = target_bg_override,
    lowThresholdOverride = low_threshold_override,
    alarmProfileOverrideId = alarm_profile_override_id,
    adjustmentHint = adjustment_hint
)

fun ScheduledTherapyAdjustmentDto.toEntity() = ScheduledTherapyAdjustmentEntity(
    id = id,
    start_time = Timestamp(startTimeMs),
    end_time = Timestamp(endTimeMs),
    insulin_adjustment_percentage = insulinAdjustmentPercentage,
    target_bg_override = targetBgOverride,
    low_threshold_override = lowThresholdOverride,
    alarm_profile_override_id = alarmProfileOverrideId,
    adjustment_hint = adjustmentHint
)

fun TherapyAdjustmentEntity.toDto() = TherapyAdjustmentPresetDto(
    id = id,
    name = name,
    percentage = percentage,
    targetBgOverride = target_bg_override,
    lowThresholdOverride = low_threshold_override,
    alarmProfileOverrideId = alarm_profile_override_id
)

fun TherapyAdjustmentPresetDto.toEntity() = TherapyAdjustmentEntity(
    id = id,
    name = name,
    percentage = percentage,
    target_bg_override = targetBgOverride,
    low_threshold_override = lowThresholdOverride,
    alarm_profile_override_id = alarmProfileOverrideId
)

fun CurrentSettingsEntity.toDto() = CurrentSettingsDto(
    id = id,
    apsMode = aps_mode.name
)

fun CurrentSettingsDto.toEntity() = CurrentSettingsEntity(
    id = id,
    aps_mode = runCatching { ApsMode.valueOf(apsMode) }.getOrDefault(ApsMode.OnlySuggestions)
)

fun AlarmProfileEntity.toDto() = AlarmProfileDto(
    id = id,
    name = name,
    isDefault = is_default,
    isActive = is_active,
    severityDefaultsJson = severity_defaults_json,
    customOverridesJson = custom_overrides_json
)

fun AlarmProfileDto.toEntity() = AlarmProfileEntity(
    id = id,
    name = name,
    is_default = isDefault,
    is_active = isActive,
    severity_defaults_json = severityDefaultsJson,
    custom_overrides_json = customOverridesJson
)

fun MealTypeEntity.toDto() = MealTypeDto(
    id = id,
    name = name,
    symbol = symbol,
    curveComponents = curve_components,
    catMinutes = cat.value,
    sortOrder = sortOrder
)

fun MealTypeDto.toEntity() = MealTypeEntity(
    id = id,
    name = name,
    symbol = symbol,
    curve_components = curveComponents,
    cat = Minutes(catMinutes),
    sortOrder = sortOrder
)

fun InsulinTypeEntity.toDto() = InsulinTypeDto(
    id = id,
    name = name,
    activeSubstance = active_substance,
    peakMinutes = peak.value,
    diaMinutes = dia.value,
    defaultConcentration = default_concentration
)

fun InsulinTypeDto.toEntity() = InsulinTypeEntity(
    id = id,
    name = name,
    active_substance = activeSubstance,
    peak = Minutes(peakMinutes),
    dia = Minutes(diaMinutes),
    default_concentration = defaultConcentration
)

fun SensorTypeEntity.toDto() = SensorTypeDto(id = id, name = name)
fun SensorTypeDto.toEntity() = SensorTypeEntity(id = id, name = name)

fun DataProviderEntity.toDto() = DataProviderDto(id = id, name = name)
fun DataProviderDto.toEntity() = DataProviderEntity(id = id, name = name)

fun GlucoseReadingEntity.toDto() = GlucoseReadingDto(
    id = id,
    valueMgdl = value_mgdl,
    sampleKind = sample_kind.name,
    timestampMs = timestamp.ms,
    fkDataProvider = fk_data_provider,
    fkSourceSensor = fk_source_sensor
)

fun GlucoseReadingDto.toEntity() = GlucoseReadingEntity(
    id = id,
    value_mgdl = valueMgdl,
    sample_kind = runCatching { BgSampleKind.valueOf(sampleKind) }.getOrDefault(BgSampleKind.Value),
    timestamp = Timestamp(timestampMs),
    fk_data_provider = fkDataProvider,
    fk_source_sensor = fkSourceSensor
)

fun MealEntity.toDto() = MealDto(
    id = id,
    mealTypeId = meal_type_id,
    timestampMs = timestamp.ms,
    carbGrams = carbGrams,
    description = description,
    administeredInsulinIu = administeredInsulinAmount.iu
)

fun MealDto.toEntity() = MealEntity(
    id = id,
    meal_type_id = mealTypeId,
    timestamp = Timestamp(timestampMs),
    carbGrams = carbGrams,
    description = description,
    administeredInsulinAmount = InsulinAmount(administeredInsulinIu)
)

fun InsulinEntity.toDto() = InsulinApplicationDto(
    id = id,
    insulinTypeId = insulin_type_id,
    timestampMs = timestamp.ms,
    amountIu = amount.iu,
    origin = origin.name,
    basal = basal,
    correction = correction,
    meal = meal,
    status = status.name,
    pumpId = pump_id
)

fun InsulinApplicationDto.toEntity() = InsulinEntity(
    id = id,
    insulin_type_id = insulinTypeId,
    timestamp = Timestamp(timestampMs),
    amount = InsulinAmount(amountIu),
    origin = runCatching { InsulinOrigin.valueOf(origin) }.getOrDefault(InsulinOrigin.Manual),
    basal = basal,
    correction = correction,
    meal = meal,
    status = runCatching { InsulinStatus.valueOf(status) }.getOrDefault(InsulinStatus.Confirmed),
    pump_id = pumpId
)

fun DeferredBolusEntity.toDto() = DeferredBolusDto(
    id = id,
    timestampMs = timestamp.ms,
    amountIu = amount,
    mealId = meal_id
)

fun DeferredBolusDto.toEntity() = DeferredBolusEntity(
    id = id,
    timestamp = Timestamp(timestampMs),
    amount = amountIu,
    meal_id = mealId
)

fun MealReminderEntity.toDto() = MealReminderDto(
    id = id,
    mealId = meal_id,
    mealTimestampMs = meal_timestamp.ms,
    reminderTimestampMs = reminder_timestamp.ms,
    description = description
)

fun MealReminderDto.toEntity() = MealReminderEntity(
    id = id,
    meal_id = mealId,
    meal_timestamp = Timestamp(mealTimestampMs),
    reminder_timestamp = Timestamp(reminderTimestampMs),
    description = description
)

fun CoreInsightEntity.toDto() = CoreInsightDto(
    id = id,
    timestampMs = timestamp.ms,
    bgOriginalMgdlScaled = bgOriginal.scaled.toInt(),
    bgFilteredMgdlScaled = bgFiltered.scaled.toInt(),
    deviationPerTickScaled = deviationPerTick.scaled.toInt(),
    futureActiveInsulinIu = futureActiveInsulin.iu,
    futureActiveCarbsGrams = futureActiveCarbs,
    predictedBgAtPeakScaled = predictedBgAtPeak.scaled.toInt(),
    targetBgScaled = targetBg.scaled.toInt(),
    isfScaled = isf.scaled.toInt(),
    cr = cr,
    actionBolusIu = actionBolus?.iu,
    actionTempBasalPercent = actionTempBasalPercent,
    actionTempBasalDurationInHours = actionTempBasalDurationInHours,
    reasoning = reasoning.name
)

fun CoreInsightDto.toEntity() = CoreInsightEntity(
    id = id,
    timestamp = Timestamp(timestampMs),
    bgOriginal = BgValue.fromMgDlScaled(bgOriginalMgdlScaled),
    bgFiltered = BgValue.fromMgDlScaled(bgFilteredMgdlScaled),
    deviationPerTick = BgDelta.fromMgDlScaled(deviationPerTickScaled),
    futureActiveInsulin = InsulinAmount(futureActiveInsulinIu),
    futureActiveCarbs = futureActiveCarbsGrams,
    predictedBgAtPeak = BgValue.fromMgDlScaled(predictedBgAtPeakScaled),
    targetBg = BgValue.fromMgDlScaled(targetBgScaled),
    isf = BgDelta.fromMgDlScaled(isfScaled),
    cr = cr,
    actionBolus = actionBolusIu?.let { InsulinAmount(it) },
    actionTempBasalPercent = actionTempBasalPercent,
    actionTempBasalDurationInHours = actionTempBasalDurationInHours,
    reasoning = runCatching { CoreReasoning.valueOf(reasoning) }.getOrDefault(CoreReasoning.INTERNAL_ERROR)
)