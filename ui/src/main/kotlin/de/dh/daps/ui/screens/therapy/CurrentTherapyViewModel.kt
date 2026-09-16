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
import de.dh.daps.common.model.data.TherapyAdjustmentTiming
import de.dh.daps.common.model.data.Timestamp
import de.dh.daps.common.model.data.getBgForMinute
import de.dh.daps.core.SystemRegistry
import de.dh.daps.glucoseUnit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
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
    val activeAlarmProfileId: Long? = null,
    val activeAlarmProfileName: String? = null,
    val adjustmentHint: String? = null,
    val timing: TherapyAdjustmentTiming = TherapyAdjustmentTiming()
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
    val activeAlarmProfileId: Long? = null
)

data class CurrentTherapyUiState(
    val isLoading: Boolean = true,
    val activeTherapyStatus: ActiveTherapyStatusUiState = ActiveTherapyStatusUiState(),
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
            glucoseUnitFlow
        ) { currentSettings, profiles, alarmProfiles, unit ->
            updateState(currentSettings, profiles, alarmProfiles, unit)
        }.launchIn(viewModelScope)
    }

    private suspend fun updateState(
        currentSettings: CurrentTherapySettings,
        profiles: List<InsulinProfile>,
        alarmProfiles: List<AlarmProfile>,
        unit: GlucoseUnit
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
            activeAlarmProfileId = currentSettings.activeAlarmProfileId,
            activeAlarmProfileName = currentSettings.activeAlarmProfile?.name,
            adjustmentHint = currentSettings.adjustmentHint,
            timing = currentSettings.adjustmentTiming
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

        _uiState.update {
            it.copy(
                isLoading = false,
                activeTherapyStatus = activeTherapyStatus,
                glucoseUnit = unit,
                availableInsulinProfiles = profiles,
                availableAlarmProfiles = alarmProfiles,
                defaultBgBlocks = currentSettings.defaultBgBlocks,
                therapyAdjustmentPresets = hardcodedPresets
            )
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

    private val _draftAdjustment = MutableStateFlow<TherapyAdjustmentUiState?>(null)
    val draftAdjustment: StateFlow<TherapyAdjustmentUiState?> = _draftAdjustment

    fun initDraftAdjustment() {
        _draftAdjustment.value = _uiState.value.activeTherapyStatus.adjustment
    }

    fun setDraftValues(
        percentage: Int,
        targetBg: BgValue?,
        lowThreshold: BgValue?,
        activeAlarmProfileId: Long?,
        adjustmentHint: String? = null,
        timing: TherapyAdjustmentTiming? = null
    ) {
        val currentDraft = _draftAdjustment.value ?: _uiState.value.activeTherapyStatus.adjustment
        val alarmProfiles = _uiState.value.availableAlarmProfiles
        val alarmProfileName = alarmProfiles.find { it.id == activeAlarmProfileId }?.name
        _draftAdjustment.value = currentDraft.copy(
            percentage = percentage,
            targetBgOverride = targetBg,
            lowThresholdOverride = lowThreshold,
            activeAlarmProfileId = activeAlarmProfileId,
            activeAlarmProfileName = if (activeAlarmProfileId != null) alarmProfileName else null,
            adjustmentHint = adjustmentHint ?: currentDraft.adjustmentHint,
            timing = timing ?: currentDraft.timing
        )
    }

    fun setDraftTiming(timing: TherapyAdjustmentTiming) {
        val currentDraft = _draftAdjustment.value ?: _uiState.value.activeTherapyStatus.adjustment
        _draftAdjustment.value = currentDraft.copy(timing = timing)
    }

    fun applyDraftAdjustment() {
        val draft = _draftAdjustment.value ?: return
        viewModelScope.launch {
            therapyManager.setTherapyAdjustment(
                percentage = draft.percentage,
                targetBg = draft.targetBgOverride,
                lowThreshold = draft.lowThresholdOverride,
                activeAlarmProfileId = draft.activeAlarmProfileId,
                adjustmentHint = draft.adjustmentHint,
                timing = draft.timing
            )
            _draftAdjustment.value = null
        }
    }

    fun resetDraft() {
        _draftAdjustment.value = null
    }

    fun isDraftDirty(): Boolean {
        val draft = _draftAdjustment.value ?: return false
        val active = _uiState.value.activeTherapyStatus.adjustment
        return draft != active
    }

    fun setTherapyAdjustment(
        percentage: Int,
        targetBg: BgValue?,
        lowThreshold: BgValue?,
        activeAlarmProfileId: Long? = null,
        adjustmentHint: String? = null,
        timing: TherapyAdjustmentTiming = TherapyAdjustmentTiming()
    ) {
        viewModelScope.launch {
            therapyManager.setTherapyAdjustment(
                percentage = percentage,
                targetBg = targetBg,
                lowThreshold = lowThreshold,
                activeAlarmProfileId = activeAlarmProfileId,
                adjustmentHint = adjustmentHint,
                timing = timing
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