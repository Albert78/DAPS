package de.dh.daps.ui.screens.setupwizard

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import de.dh.daps.common.DEFAULT_BG_LOW_THRESHOLD_MGDL
import de.dh.daps.common.DEFAULT_BG_TARGET_MGDL
import de.dh.daps.common.model.data.BgBlock
import de.dh.daps.common.model.data.BgValue
import de.dh.daps.common.model.data.Block
import de.dh.daps.common.model.data.Minutes
import de.dh.daps.common.model.getDefaultInsulinProfile
import de.dh.daps.common.model.getDefaultInsulinTypes
import de.dh.daps.common.model.getDefaultMealTypes
import de.dh.daps.common.ui.UiText
import de.dh.daps.core.SetupOption
import de.dh.daps.core.SystemRegistry
import de.dh.daps.ui.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class SetupWizardStep {
    MODE_SELECTION,
    MANUAL_STEP_1_TYPES,
    MANUAL_STEP_2_BG_TARGETS,
    MANUAL_STEP_3_BASAL_RATE,
    MANUAL_STEP_4_SUMMARY
}

data class SetupWizardUiState(
    val currentStep: SetupWizardStep = SetupWizardStep.MODE_SELECTION,
    val isBusy: Boolean = false,
    val errorMessage: UiText? = null,
    val targetBgMgDl: Double = DEFAULT_BG_TARGET_MGDL.toDouble(),
    val lowThresholdMgDl: Double = DEFAULT_BG_LOW_THRESHOLD_MGDL.toDouble(),
    val basalRateUPerHour: Double = 1.0,
    val profileName: String = "Standard"
)

class SetupWizardViewModel(
    private val registry: SystemRegistry
) : ViewModel() {

    private val _uiState = MutableStateFlow(SetupWizardUiState())
    val uiState: StateFlow<SetupWizardUiState> = _uiState.asStateFlow()

    fun selectDemoData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, errorMessage = null) }
            runCatching {
                registry.completeInitialization(SetupOption.SeedDemoData)
            }.onFailure { error ->
                val message = error.localizedMessage?.let { UiText.DynamicString(it) }
                    ?: UiText.StringResource(R.string.setup_wizard_error_load_demo_data)
                _uiState.update { it.copy(isBusy = false, errorMessage = message) }
            }
        }
    }

    fun importBackup(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, errorMessage = null) }
            runCatching {
                registry.completeInitialization(SetupOption.ImportBackup(uri))
            }.onFailure { error ->
                val message = error.localizedMessage?.let { UiText.DynamicString(it) }
                    ?: UiText.StringResource(R.string.setup_wizard_error_import_backup)
                _uiState.update { it.copy(isBusy = false, errorMessage = message) }
            }
        }
    }

    fun startManualSetup() {
        _uiState.update { it.copy(currentStep = SetupWizardStep.MANUAL_STEP_1_TYPES) }
    }

    fun setBgTargets(targetBg: Double, lowThreshold: Double) {
        _uiState.update {
            it.copy(
                targetBgMgDl = targetBg,
                lowThresholdMgDl = lowThreshold,
                currentStep = SetupWizardStep.MANUAL_STEP_3_BASAL_RATE
            )
        }
    }

    fun setBasalRate(rate: Double) {
        _uiState.update {
            it.copy(
                basalRateUPerHour = rate,
                currentStep = SetupWizardStep.MANUAL_STEP_4_SUMMARY
            )
        }
    }

    fun goToNextStepFromTypes() {
        _uiState.update { it.copy(currentStep = SetupWizardStep.MANUAL_STEP_2_BG_TARGETS) }
    }

    fun goToPreviousStep() {
        _uiState.update { state ->
            val prevStep = when (state.currentStep) {
                SetupWizardStep.MODE_SELECTION -> SetupWizardStep.MODE_SELECTION
                SetupWizardStep.MANUAL_STEP_1_TYPES -> SetupWizardStep.MODE_SELECTION
                SetupWizardStep.MANUAL_STEP_2_BG_TARGETS -> SetupWizardStep.MANUAL_STEP_1_TYPES
                SetupWizardStep.MANUAL_STEP_3_BASAL_RATE -> SetupWizardStep.MANUAL_STEP_2_BG_TARGETS
                SetupWizardStep.MANUAL_STEP_4_SUMMARY -> SetupWizardStep.MANUAL_STEP_3_BASAL_RATE
            }
            state.copy(currentStep = prevStep)
        }
    }

    fun completeManualSetup() {
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, errorMessage = null) }
            runCatching {
                val state = _uiState.value
                val context = registry.appContext

                // 1. Types
                val defaultInsulinTypes = getDefaultInsulinTypes(context)
                defaultInsulinTypes.forEach {
                    registry.treatmentRepository.insertInsulinType(it)
                }
                getDefaultMealTypes(context).forEach {
                    registry.treatmentRepository.insertMealType(it)
                }

                val primaryInsulinType = defaultInsulinTypes.firstOrNull()
                    ?: throw IllegalStateException(context.getString(R.string.setup_wizard_error_no_insulin_type))

                // 2. Profile
                val baseProfile = getDefaultInsulinProfile(context, primaryInsulinType)
                val customProfile = baseProfile.copy(
                    name = state.profileName,
                    basalBlocks = listOf(
                        Block(Minutes.ofHours(24), state.basalRateUPerHour)
                    )
                )

                val insertedProfileId = registry.therapyRepository.insertInsulinProfile(customProfile)

                // 3. Therapy settings
                registry.therapyRepository.updateCurrentTherapySettings(
                    insulinProfileId = insertedProfileId,
                    defaultBgBlocks = listOf(
                        BgBlock(
                            duration = Minutes.ofHours(24),
                            target = BgValue.fromMgDl(state.targetBgMgDl.toInt().toShort()),
                            lowThreshold = BgValue.fromMgDl(state.lowThresholdMgDl.toInt().toShort())
                        )
                    )
                )

                // 4. Complete system initialization
                registry.completeInitialization(SetupOption.ManualSetupCompleted)
            }.onFailure { error ->
                val message = error.localizedMessage?.let { UiText.DynamicString(it) }
                    ?: UiText.StringResource(R.string.setup_wizard_error_manual_setup)
                _uiState.update { it.copy(isBusy = false, errorMessage = message) }
            }
        }
    }

    class Factory(private val registry: SystemRegistry) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SetupWizardViewModel(registry) as T
        }
    }
}