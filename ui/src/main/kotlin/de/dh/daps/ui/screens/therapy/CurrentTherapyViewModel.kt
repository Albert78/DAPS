package de.dh.daps.ui.screens.therapy

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import de.dh.daps.common.model.InsulinAmount
import de.dh.daps.common.model.data.AlarmProfile
import de.dh.daps.common.model.data.BgBlock
import de.dh.daps.common.model.data.BgDelta
import de.dh.daps.common.model.data.BgValue
import de.dh.daps.common.model.data.CurrentTherapySettings
import de.dh.daps.common.model.data.GlucoseUnit
import de.dh.daps.common.model.data.InsulinProfile
import de.dh.daps.common.model.data.Minutes
import de.dh.daps.common.model.data.ScheduledTherapyAdjustment
import de.dh.daps.common.model.data.Timestamp
import de.dh.daps.common.model.data.getBgForMinute
import de.dh.daps.core.SystemRegistry
import de.dh.daps.glucoseUnit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale

data class InsulinProfileUiState(
    val name: String = "",
    val activeProfileId: Long? = null,
    val isfRange: String = "",
    val crRange: String = "",
    val basalRange: String = "",
    val dia: Minutes = Minutes(0),
    val peak: Minutes = Minutes(0)
)

data class TherapyAdjustmentUiState(
    val percentage: Int = 0,
    val targetBgOverride: BgValue? = null,
    val lowThresholdOverride: BgValue? = null,
    val alarmProfileOverrideId: Long? = null,
    val alarmProfileOverrideName: String? = null,
    val adjustmentHint: String? = null,
    val adjustmentEndTime: Timestamp? = null
)

data class ActiveTherapyStatusUiState(
    val profile: InsulinProfileUiState = InsulinProfileUiState(),
    val adjustment: TherapyAdjustmentUiState = TherapyAdjustmentUiState(),
    val currentIsf: BgDelta = BgDelta.ZERO,
    val currentCr: Double = 0.0,
    val currentBasal: InsulinAmount = InsulinAmount.ZERO,
    val target: BgValue = BgValue.fromMgDl(0),
    val lowThreshold: BgValue = BgValue.fromMgDl(0),
    val baseTarget: BgValue = BgValue.fromMgDl(0),
    val baseLow: BgValue = BgValue.fromMgDl(0)
)

data class TherapyAdjustment(
    val name: String,
    val percentage: Int = 0,
    val targetBgMgDl: Short? = null,
    val lowThresholdMgDl: Short? = null,
    val alarmProfileOverrideId: Long? = null
)

data class CurrentTherapyUiState(
    val isLoading: Boolean = true,
    val activeTherapyStatus: ActiveTherapyStatusUiState = ActiveTherapyStatusUiState(),
    val scheduledTherapyAdjustment: ScheduledTherapyAdjustment? = null,
    val glucoseUnit: GlucoseUnit = GlucoseUnit.MG_DL,
    val availableInsulinProfiles: List<InsulinProfile> = emptyList(),
    val availableAlarmProfiles: List<AlarmProfile> = emptyList(),
    val defaultBgBlocks: List<BgBlock> = emptyList(),
    val therapyAdjustmentPresets: List<TherapyAdjustment> = emptyList()
)

/**
 * ViewModel for viewing and selecting the current therapy settings.
 */
class CurrentTherapyViewModel(
    private val systemRegistry: SystemRegistry
) : ViewModel() {
    private val _uiState = MutableStateFlow(CurrentTherapyUiState())
    val uiState: StateFlow<CurrentTherapyUiState> = _uiState

    private val therapyManager = systemRegistry.therapyManager
    private val appPreferencesRepository = systemRegistry.appPreferencesRepository

    // Hardcoded presets for now.
    // See also ScheduledTherapyViewModel
    // TODO: Make these user-editable in the future (e.g. via a database table or preferences).
    private val hardcodedPresets = listOf(
        TherapyAdjustment("Neutral"),
        TherapyAdjustment("Fahrrad fahren", percentage = -30, targetBgMgDl = 150, lowThresholdMgDl = 100),
        TherapyAdjustment("Klettern", percentage = -40, targetBgMgDl = 160, lowThresholdMgDl = 110),
        TherapyAdjustment("Alkohol", percentage = -15, targetBgMgDl = 120, lowThresholdMgDl = 80),
        TherapyAdjustment("Krank", percentage = 30, targetBgMgDl = 100, lowThresholdMgDl = 70),
        TherapyAdjustment("Stress", percentage = 20, targetBgMgDl = 115, lowThresholdMgDl = 75)
    )

    init {
        val glucoseUnitFlow = appPreferencesRepository.cachedPreferences.map { it.glucoseUnit }
        combine(
            therapyManager.currentTherapySettingsFlow,
            therapyManager.observeAllInsulinProfiles(),
            systemRegistry.alarmRepository.observeAllAlarmProfiles(),
            glucoseUnitFlow,
            therapyManager.observeScheduledTherapyAdjustment()
        ) { currentSettings, profiles, alarmProfiles, unit, scheduledAdjustment ->
            updateState(currentSettings, profiles, alarmProfiles, unit, scheduledAdjustment)
        }.launchIn(viewModelScope)
    }

    private suspend fun updateState(
        currentSettings: CurrentTherapySettings,
        profiles: List<InsulinProfile>,
        alarmProfiles: List<AlarmProfile>,
        unit: GlucoseUnit,
        scheduledAdjustment: ScheduledTherapyAdjustment?
    ) {
        val now = Timestamp.now()
        val isf = therapyManager.getIsfFactor(now)
        val cr = therapyManager.getCrFactor(now)
        val basal = therapyManager.getBasalPerHour(now)
        val bgSettings = therapyManager.getBgSettings(now)

        val crValues = currentSettings.insulinProfile.crBlocks.map { it.amount }
        val isfValues = currentSettings.insulinProfile.isfBlocks.map { it.amount }
        val basalValues = currentSettings.insulinProfile.basalBlocks.map { it.amount }

        val minuteSinceMidnight = now.minutesSinceMidnight()
        val baseBg = currentSettings.defaultBgBlocks.getBgForMinute(minuteSinceMidnight)

        val profileUiState = InsulinProfileUiState(
            name = currentSettings.insulinProfile.name,
            activeProfileId = currentSettings.insulinProfile.id,
            isfRange = formatIsfRange(isfValues, unit),
            crRange = formatRange(crValues, "%.1f"),
            basalRange = formatRange(basalValues, "%.2f"),
            dia = currentSettings.insulinProfile.dia,
            peak = currentSettings.insulinProfile.peak
        )

        val adjustmentUiState = TherapyAdjustmentUiState(
            percentage = currentSettings.insulinAdjustmentPercentage,
            targetBgOverride = currentSettings.targetBgOverride,
            lowThresholdOverride = currentSettings.lowThresholdOverride,
            alarmProfileOverrideId = currentSettings.alarmProfileOverrideId,
            alarmProfileOverrideName = currentSettings.alarmProfileOverride?.name,
            adjustmentHint = currentSettings.adjustmentHint,
            adjustmentEndTime = currentSettings.adjustmentEndTime
        )

        val activeTherapyStatus = ActiveTherapyStatusUiState(
            profile = profileUiState,
            adjustment = adjustmentUiState,
            currentIsf = isf,
            currentCr = cr,
            currentBasal = basal,
            target = bgSettings.first,
            lowThreshold = bgSettings.second,
            baseTarget = baseBg.first,
            baseLow = baseBg.second
        )

        val isInitialLoad = _uiState.value.isLoading

        _uiState.update {
            it.copy(
                isLoading = false,
                activeTherapyStatus = activeTherapyStatus,
                scheduledTherapyAdjustment = scheduledAdjustment,
                glucoseUnit = unit,
                availableInsulinProfiles = profiles,
                availableAlarmProfiles = alarmProfiles,
                defaultBgBlocks = currentSettings.defaultBgBlocks,
                therapyAdjustmentPresets = hardcodedPresets
            )
        }
        if (isInitialLoad) {
            initDraftAdjustment()
        }
    }

    private fun formatRange(values: List<Double>, format: String): String {
        if (values.isEmpty()) return "-"
        val min = values.minOrNull() ?: 0.0
        val max = values.maxOrNull() ?: 0.0
        return if (min == max) {
            String.format(Locale.getDefault(), format, min)
        } else {
            val fMin = String.format(Locale.getDefault(), format, min)
            val fMax = String.format(Locale.getDefault(), format, max)
            "$fMin – $fMax"
        }
    }

    private fun formatIsfRange(values: List<Double>, unit: GlucoseUnit): String {
        if (values.isEmpty()) return "-"
        val min = values.minOrNull() ?: 0.0
        val max = values.maxOrNull() ?: 0.0

        fun formatVal(v: Double): String {
            return BgDelta.fromMgDl(v).toString(unit)
        }

        return if (min == max) {
            formatVal(min)
        } else {
            "${formatVal(min)} – ${formatVal(max)}"
        }
    }

    fun updateDefaultBgBlocks(blocks: List<BgBlock>) {
        viewModelScope.launch {
            therapyManager.updateDefaultBgBlocks(blocks)
        }
    }

    fun selectInsulinProfile(profile: InsulinProfile) {
        viewModelScope.launch {
            therapyManager.selectInsulinProfile(profile)
        }
    }

    val formStateHolder = TherapyAdjustmentFormStateHolder()

    private val _draftEndTime = MutableStateFlow<Timestamp?>(null)

    val draftAdjustment: StateFlow<TherapyAdjustmentUiState?> = combine(
        formStateHolder.formState,
        _draftEndTime
    ) { form, endTime ->
        TherapyAdjustmentUiState(
            percentage = form.percentage,
            targetBgOverride = form.targetBgOverride,
            lowThresholdOverride = form.lowThresholdOverride,
            alarmProfileOverrideId = form.alarmProfileOverrideId,
            alarmProfileOverrideName = form.alarmProfileOverrideName,
            adjustmentHint = form.adjustmentHint,
            adjustmentEndTime = endTime
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun initDraftAdjustment() {
        val active = _uiState.value.activeTherapyStatus.adjustment
        _draftEndTime.value = active.adjustmentEndTime
        formStateHolder.initFormState(
            TherapyAdjustmentFormState(
                percentage = active.percentage,
                targetBgOverride = active.targetBgOverride,
                lowThresholdOverride = active.lowThresholdOverride,
                alarmProfileOverrideId = active.alarmProfileOverrideId,
                alarmProfileOverrideName = active.alarmProfileOverrideName,
                adjustmentHint = active.adjustmentHint
            )
        )
    }

    fun setDraftValues(
        percentage: Int,
        targetBg: BgValue?,
        lowThreshold: BgValue?,
        alarmProfileOverrideId: Long?,
        adjustmentHint: String? = null,
        adjustmentEndTime: Timestamp? = null
    ) {
        val alarmProfiles = _uiState.value.availableAlarmProfiles
        val alarmProfileName = alarmProfiles.find { it.id == alarmProfileOverrideId }?.name
        formStateHolder.updateValues(
            percentage = percentage,
            targetBg = targetBg,
            lowThreshold = lowThreshold,
            alarmProfileOverrideId = alarmProfileOverrideId,
            alarmProfileName = alarmProfileName,
            hint = adjustmentHint
        )
        if (adjustmentEndTime != null) {
            _draftEndTime.value = adjustmentEndTime
        }
    }

    fun applyPreset(preset: TherapyAdjustment) {
        formStateHolder.applyPreset(preset)
    }

    fun setDraftEndTime(endTime: Timestamp?) {
        _draftEndTime.value = endTime
    }

    fun applyDraftAdjustment() {
        val form = formStateHolder.formState.value
        val endTime = _draftEndTime.value
        viewModelScope.launch {
            therapyManager.setTherapyAdjustment(
                percentage = form.percentage,
                targetBg = form.targetBgOverride,
                lowThreshold = form.lowThresholdOverride,
                alarmProfileOverrideId = form.alarmProfileOverrideId,
                adjustmentHint = form.adjustmentHint,
                adjustmentEndTime = endTime
            )
        }
    }

    fun resetDraft() {
        formStateHolder.reset()
        _draftEndTime.value = _uiState.value.activeTherapyStatus.adjustment.adjustmentEndTime
    }

    fun isDraftDirty(): Boolean {
        val active = _uiState.value.activeTherapyStatus.adjustment
        return formStateHolder.isDirty() || _draftEndTime.value != active.adjustmentEndTime
    }

    fun setTherapyAdjustment(
        percentage: Int,
        targetBg: BgValue?,
        lowThreshold: BgValue?,
        alarmProfileOverrideId: Long? = null,
        adjustmentHint: String? = null,
        adjustmentEndTime: Timestamp? = null
    ) {
        viewModelScope.launch {
            therapyManager.setTherapyAdjustment(
                percentage = percentage,
                targetBg = targetBg,
                lowThreshold = lowThreshold,
                alarmProfileOverrideId = alarmProfileOverrideId,
                adjustmentHint = adjustmentHint,
                adjustmentEndTime = adjustmentEndTime
            )
        }
    }

    companion object {
        class Factory(private val registry: SystemRegistry) : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                return CurrentTherapyViewModel(registry) as T
            }
        }
    }
}