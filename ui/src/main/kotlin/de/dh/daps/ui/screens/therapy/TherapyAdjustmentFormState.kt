package de.dh.daps.ui.screens.therapy

import de.dh.daps.common.model.data.BgValue
import de.dh.daps.common.model.data.TherapyAdjustment
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * State representing the user input in the therapy adjustment form.
 */
data class TherapyAdjustmentFormState(
    val percentage: Int = 0,
    val targetBgOverride: BgValue? = null,
    val lowThresholdOverride: BgValue? = null,
    val alarmProfileOverrideId: Long? = null,
    val alarmProfileOverrideName: String? = null,
    val adjustmentHint: String? = null
)

/**
 * State holder managing form state, presets, and changes for therapy adjustments.
 */
class TherapyAdjustmentFormStateHolder {
    private val _formState = MutableStateFlow(TherapyAdjustmentFormState())
    val formState: StateFlow<TherapyAdjustmentFormState> = _formState.asStateFlow()

    private var initialFormState = TherapyAdjustmentFormState()

    fun initFormState(state: TherapyAdjustmentFormState) {
        initialFormState = state
        _formState.value = state
    }

    fun updateValues(
        percentage: Int,
        targetBg: BgValue?,
        lowThreshold: BgValue?,
        alarmProfileOverrideId: Long?,
        alarmProfileName: String? = null,
        hint: String? = null
    ) {
        _formState.update { current ->
            current.copy(
                percentage = percentage,
                targetBgOverride = targetBg,
                lowThresholdOverride = lowThreshold,
                alarmProfileOverrideId = alarmProfileOverrideId,
                alarmProfileOverrideName = if (alarmProfileOverrideId == null) null else (alarmProfileName ?: current.alarmProfileOverrideName),
                adjustmentHint = hint
            )
        }
    }

    fun applyPreset(preset: TherapyAdjustment) {
        _formState.update { current ->
            val hint = if (preset.percentage == 0 && preset.targetBgMgDl == null && preset.lowThresholdMgDl == null && preset.alarmProfileOverrideId == null) null else preset.name
            current.copy(
                percentage = preset.percentage,
                targetBgOverride = preset.targetBgMgDl?.let { BgValue.fromMgDl(it.toInt()) },
                lowThresholdOverride = preset.lowThresholdMgDl?.let { BgValue.fromMgDl(it.toInt()) },
                alarmProfileOverrideId = preset.alarmProfileOverrideId,
                adjustmentHint = hint
            )
        }
    }

    fun reset() {
        _formState.value = initialFormState
    }

    fun isDirty(): Boolean {
        return _formState.value != initialFormState
    }
}