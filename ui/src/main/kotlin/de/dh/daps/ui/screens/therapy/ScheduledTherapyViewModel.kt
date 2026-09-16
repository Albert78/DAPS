package de.dh.daps.ui.screens.therapy

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import de.dh.daps.common.model.ID_UNDEFINED
import de.dh.daps.common.model.data.AlarmProfile
import de.dh.daps.common.model.data.BgValue
import de.dh.daps.common.model.data.GlucoseUnit
import de.dh.daps.common.model.data.ScheduledTherapyAdjustment
import de.dh.daps.common.model.data.Timestamp
import de.dh.daps.common.model.data.getBgForMinute
import de.dh.daps.core.SystemRegistry
import de.dh.daps.glucoseUnit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ScheduledTherapyUiState(
    val isLoading: Boolean = true,
    val baseTarget: BgValue = BgValue.fromMgDl(100),
    val baseLow: BgValue = BgValue.fromMgDl(70),
    val glucoseUnit: GlucoseUnit = GlucoseUnit.MG_DL,
    val availableAlarmProfiles: List<AlarmProfile> = emptyList(),
    val therapyAdjustmentPresets: List<TherapyAdjustment> = emptyList(),
    val existingScheduledAdjustment: ScheduledTherapyAdjustment? = null
)

/**
 * ViewModel for planning a future activity/therapy adjustment.
 */
class ScheduledTherapyViewModel(
    private val systemRegistry: SystemRegistry
) : ViewModel() {

    val formStateHolder = TherapyAdjustmentFormStateHolder()

    private val _uiState = MutableStateFlow(ScheduledTherapyUiState())
    val uiState: StateFlow<ScheduledTherapyUiState> = _uiState.asStateFlow()

    private val nowMs = System.currentTimeMillis()
    private val _startTime = MutableStateFlow(Timestamp(nowMs + 15 * 60 * 1000)) // Default: in 15 mins
    val startTime: StateFlow<Timestamp> = _startTime.asStateFlow()

    private val _endTime = MutableStateFlow(Timestamp(nowMs + 75 * 60 * 1000)) // Default: 1 hour duration
    val endTime: StateFlow<Timestamp> = _endTime.asStateFlow()

    private val therapyManager = systemRegistry.therapyManager
    private val appPreferencesRepository = systemRegistry.appPreferencesRepository

    private var isInitializedFromExisting = false

    // Hardcoded presets for now.
    // See also CurrentTherapyViewModel
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
            systemRegistry.alarmRepository.observeAllAlarmProfiles(),
            glucoseUnitFlow,
            therapyManager.observeScheduledTherapyAdjustment()
        ) { currentSettings, alarmProfiles, unit, existingScheduled ->
            val now = Timestamp.now()
            val baseBg = currentSettings.defaultBgBlocks.getBgForMinute(now.minutesSinceMidnight())

            if (!isInitializedFromExisting && existingScheduled != null) {
                isInitializedFromExisting = true
                _startTime.value = existingScheduled.startTime
                _endTime.value = existingScheduled.endTime
                val alarmProfileName = alarmProfiles.find { it.id == existingScheduled.alarmProfileOverrideId }?.name
                formStateHolder.initFormState(
                    TherapyAdjustmentFormState(
                        percentage = existingScheduled.percentage,
                        targetBgOverride = existingScheduled.targetBgOverride,
                        lowThresholdOverride = existingScheduled.lowThresholdOverride,
                        alarmProfileOverrideId = existingScheduled.alarmProfileOverrideId,
                        alarmProfileOverrideName = alarmProfileName,
                        adjustmentHint = existingScheduled.adjustmentHint
                    )
                )
            }

            _uiState.update {
                it.copy(
                    isLoading = false,
                    baseTarget = baseBg.first,
                    baseLow = baseBg.second,
                    glucoseUnit = unit,
                    availableAlarmProfiles = alarmProfiles,
                    therapyAdjustmentPresets = hardcodedPresets,
                    existingScheduledAdjustment = existingScheduled
                )
            }
        }.launchIn(viewModelScope)
    }

    fun setTimeRange(start: Timestamp, end: Timestamp) {
        _startTime.value = start
        _endTime.value = end
    }

    fun setStartTime(start: Timestamp) {
        _startTime.value = start
        if (_endTime.value <= start) {
            _endTime.value = Timestamp(start.ms + 60 * 60 * 1000)
        }
    }

    fun setEndTime(end: Timestamp) {
        _endTime.value = end
    }

    fun setFormValues(
        percentage: Int,
        targetBg: BgValue?,
        lowThreshold: BgValue?,
        alarmProfileOverrideId: Long?,
        hint: String? = null
    ) {
        val alarmProfiles = _uiState.value.availableAlarmProfiles
        val alarmProfileName = alarmProfiles.find { it.id == alarmProfileOverrideId }?.name
        formStateHolder.updateValues(
            percentage = percentage,
            targetBg = targetBg,
            lowThreshold = lowThreshold,
            alarmProfileOverrideId = alarmProfileOverrideId,
            alarmProfileName = alarmProfileName,
            hint = hint
        )
    }

    fun applyPreset(preset: TherapyAdjustment) {
        formStateHolder.applyPreset(preset)
    }

    fun saveScheduledAdjustment(onSuccess: () -> Unit) {
        val form = formStateHolder.formState.value
        viewModelScope.launch {
            val existingId = _uiState.value.existingScheduledAdjustment?.id ?: ID_UNDEFINED
            val adjustment = ScheduledTherapyAdjustment(
                id = existingId,
                startTime = _startTime.value,
                endTime = _endTime.value,
                percentage = form.percentage,
                targetBgOverride = form.targetBgOverride,
                lowThresholdOverride = form.lowThresholdOverride,
                alarmProfileOverrideId = form.alarmProfileOverrideId,
                adjustmentHint = form.adjustmentHint
            )
            therapyManager.saveScheduledTherapyAdjustment(adjustment)
            onSuccess()
        }
    }

    fun deleteScheduledAdjustment(onSuccess: () -> Unit) {
        viewModelScope.launch {
            therapyManager.deleteScheduledTherapyAdjustment()
            onSuccess()
        }
    }

    fun reset() {
        formStateHolder.reset()
    }

    companion object {
        class Factory(private val registry: SystemRegistry) : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                return ScheduledTherapyViewModel(registry) as T
            }
        }
    }
}