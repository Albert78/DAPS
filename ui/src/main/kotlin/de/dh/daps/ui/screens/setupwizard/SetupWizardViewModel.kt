package de.dh.daps.ui.screens.setupwizard

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import de.dh.daps.common.DEFAULT_BG_LOW_THRESHOLD_MGDL
import de.dh.daps.common.DEFAULT_BG_TARGET_MGDL
import de.dh.daps.common.model.GlucoseSourceConnectionDescriptor
import de.dh.daps.common.model.InsulinConcentration
import de.dh.daps.common.model.InsulinType
import de.dh.daps.common.model.PumpConnectionDescriptor
import de.dh.daps.common.model.data.BgBlock
import de.dh.daps.common.model.data.BgValue
import de.dh.daps.common.model.data.CarbsUnit
import de.dh.daps.common.model.data.GlucoseUnit
import de.dh.daps.common.model.data.InsulinProfile
import de.dh.daps.common.model.data.Minutes
import de.dh.daps.common.model.getDefaultInsulinProfile
import de.dh.daps.common.model.getDefaultInsulinTypes
import de.dh.daps.common.model.getDefaultMealTypes
import de.dh.daps.common.ui.UiText
import de.dh.daps.core.SetupOption
import de.dh.daps.core.SystemRegistry
import de.dh.daps.setCarbsUnit
import de.dh.daps.setGlucoseUnit
import de.dh.daps.ui.R
import de.dh.daps.ui.screens.insulintypes.InsulinTypeEditorUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

enum class SetupWizardStep {
    MODE_SELECTION,
    MANUAL_STEP_1_UNITS,
    MANUAL_STEP_2_INSULIN_TYPE,
    MANUAL_STEP_3_INSULIN_PROFILE,
    MANUAL_STEP_4_BG_TARGETS,
    MANUAL_STEP_5_GLUCOSE_SOURCE,
    MANUAL_STEP_6_PUMP,
    MANUAL_STEP_7_SUMMARY
}

data class SetupWizardUiState(
    val currentStep: SetupWizardStep = SetupWizardStep.MODE_SELECTION,
    val isBusy: Boolean = false,
    val errorMessage: UiText? = null,
    val glucoseUnit: GlucoseUnit = GlucoseUnit.MG_DL,
    val carbsUnit: CarbsUnit = CarbsUnit.GRAMS,
    val availableInsulinTypes: List<InsulinType> = emptyList(),
    val selectedInsulinType: InsulinType? = null,
    val isEditingInsulinType: Boolean = false,
    val insulinTypeEditorUiState: InsulinTypeEditorUiState = InsulinTypeEditorUiState(),
    val insulinProfile: InsulinProfile? = null,
    val bgBlocks: List<BgBlock> = emptyList()
)

class SetupWizardViewModel(
    val registry: SystemRegistry
) : ViewModel() {
    val activeGlucoseSourceDescriptor: StateFlow<GlucoseSourceConnectionDescriptor?> =
        registry.deviceManagementRepository.glucoseSourceDescriptor

    val activePumpDescriptor: StateFlow<PumpConnectionDescriptor?> =
        registry.deviceManagementRepository.pumpDescriptor

    private val _uiState = MutableStateFlow(SetupWizardUiState())
    val uiState: StateFlow<SetupWizardUiState> = _uiState.asStateFlow()

    init {
        val context = registry.appContext
        val defaultTypes = getDefaultInsulinTypes(context)
        val primaryType = defaultTypes.firstOrNull()

        val initialProfile = if (primaryType != null) {
            getDefaultInsulinProfile(context, primaryType)
        } else null

        val initialBgBlocks = listOf(
            BgBlock(
                duration = Minutes.ofHours(24),
                target = BgValue.fromMgDl(DEFAULT_BG_TARGET_MGDL),
                lowThreshold = BgValue.fromMgDl(DEFAULT_BG_LOW_THRESHOLD_MGDL)
            )
        )

        _uiState.update {
            it.copy(
                availableInsulinTypes = defaultTypes,
                selectedInsulinType = primaryType,
                insulinProfile = initialProfile,
                bgBlocks = initialBgBlocks
            )
        }
    }

    fun selectDemoData() {
        viewModelScope.launch(Dispatchers.IO) {
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
        viewModelScope.launch(Dispatchers.IO) {
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
        _uiState.update { it.copy(currentStep = SetupWizardStep.MANUAL_STEP_1_UNITS) }
    }

    fun setGlucoseUnit(unit: GlucoseUnit) {
        _uiState.update { it.copy(glucoseUnit = unit) }
    }

    fun setCarbsUnit(unit: CarbsUnit) {
        _uiState.update { it.copy(carbsUnit = unit) }
    }

    fun selectInsulinType(type: InsulinType) {
        _uiState.update { state ->
            val updatedProfile = state.insulinProfile?.copy(
                insulinType = type,
                dia = type.dia,
                peak = type.peak,
                insulinConcentration = type.defaultConcentration
            )
            state.copy(
                selectedInsulinType = type,
                insulinProfile = updatedProfile
            )
        }
    }

    fun startEditingInsulinType(typeToEdit: InsulinType? = null) {
        val type = typeToEdit ?: _uiState.value.selectedInsulinType
        _uiState.update { state ->
            state.copy(
                isEditingInsulinType = true,
                insulinTypeEditorUiState = InsulinTypeEditorUiState(
                    id = type?.id,
                    name = type?.name ?: "",
                    peak = type?.peak?.value?.toString() ?: "50",
                    dia = type?.dia?.value?.toString() ?: "300",
                    concentration = type?.defaultConcentration ?: InsulinConcentration.U100
                )
            )
        }
    }

    fun cancelEditingInsulinType() {
        _uiState.update { it.copy(isEditingInsulinType = false) }
    }

    fun updateInsulinTypeEditorName(name: String) {
        _uiState.update { state ->
            state.copy(insulinTypeEditorUiState = state.insulinTypeEditorUiState.copy(name = name))
        }
    }

    fun updateInsulinTypeEditorPeak(peak: String) {
        _uiState.update { state ->
            state.copy(insulinTypeEditorUiState = state.insulinTypeEditorUiState.copy(peak = peak))
        }
    }

    fun updateInsulinTypeEditorDia(dia: String) {
        _uiState.update { state ->
            state.copy(insulinTypeEditorUiState = state.insulinTypeEditorUiState.copy(dia = dia))
        }
    }

    fun updateInsulinTypeEditorConcentration(concentration: InsulinConcentration) {
        _uiState.update { state ->
            state.copy(insulinTypeEditorUiState = state.insulinTypeEditorUiState.copy(concentration = concentration))
        }
    }

    fun saveEditedInsulinType() {
        val editorState = _uiState.value.insulinTypeEditorUiState
        if (!editorState.isValid) return

        val newType = InsulinType(
            id = editorState.id ?: UUID.randomUUID().toString(),
            name = editorState.name.trim(),
            peak = Minutes((editorState.peak.toIntOrNull() ?: 50).toShort()),
            dia = Minutes((editorState.dia.toIntOrNull() ?: 300).toShort()),
            defaultConcentration = editorState.concentration
        )

        _uiState.update { state ->
            val updatedTypes = (state.availableInsulinTypes.filterNot { it.id == newType.id } + newType)
            val updatedProfile = state.insulinProfile?.copy(
                insulinType = newType,
                dia = newType.dia,
                peak = newType.peak,
                insulinConcentration = newType.defaultConcentration
            )
            state.copy(
                availableInsulinTypes = updatedTypes,
                selectedInsulinType = newType,
                insulinProfile = updatedProfile,
                isEditingInsulinType = false
            )
        }
    }

    fun setInsulinProfile(profile: InsulinProfile) {
        _uiState.update { it.copy(insulinProfile = profile) }
    }

    fun setBgBlocks(blocks: List<BgBlock>) {
        _uiState.update { it.copy(bgBlocks = blocks) }
    }

    fun goToNextStep() {
        _uiState.update { state ->
            val nextStep = when (state.currentStep) {
                SetupWizardStep.MODE_SELECTION -> SetupWizardStep.MANUAL_STEP_1_UNITS
                SetupWizardStep.MANUAL_STEP_1_UNITS -> SetupWizardStep.MANUAL_STEP_2_INSULIN_TYPE
                SetupWizardStep.MANUAL_STEP_2_INSULIN_TYPE -> SetupWizardStep.MANUAL_STEP_3_INSULIN_PROFILE
                SetupWizardStep.MANUAL_STEP_3_INSULIN_PROFILE -> SetupWizardStep.MANUAL_STEP_4_BG_TARGETS
                SetupWizardStep.MANUAL_STEP_4_BG_TARGETS -> SetupWizardStep.MANUAL_STEP_5_GLUCOSE_SOURCE
                SetupWizardStep.MANUAL_STEP_5_GLUCOSE_SOURCE -> SetupWizardStep.MANUAL_STEP_6_PUMP
                SetupWizardStep.MANUAL_STEP_6_PUMP -> SetupWizardStep.MANUAL_STEP_7_SUMMARY
                SetupWizardStep.MANUAL_STEP_7_SUMMARY -> SetupWizardStep.MANUAL_STEP_7_SUMMARY
            }
            state.copy(currentStep = nextStep)
        }
    }

    fun goToPreviousStep() {
        _uiState.update { state ->
            val prevStep = when (state.currentStep) {
                SetupWizardStep.MODE_SELECTION -> SetupWizardStep.MODE_SELECTION
                SetupWizardStep.MANUAL_STEP_1_UNITS -> SetupWizardStep.MODE_SELECTION
                SetupWizardStep.MANUAL_STEP_2_INSULIN_TYPE -> SetupWizardStep.MANUAL_STEP_1_UNITS
                SetupWizardStep.MANUAL_STEP_3_INSULIN_PROFILE -> SetupWizardStep.MANUAL_STEP_2_INSULIN_TYPE
                SetupWizardStep.MANUAL_STEP_4_BG_TARGETS -> SetupWizardStep.MANUAL_STEP_3_INSULIN_PROFILE
                SetupWizardStep.MANUAL_STEP_5_GLUCOSE_SOURCE -> SetupWizardStep.MANUAL_STEP_4_BG_TARGETS
                SetupWizardStep.MANUAL_STEP_6_PUMP -> SetupWizardStep.MANUAL_STEP_5_GLUCOSE_SOURCE
                SetupWizardStep.MANUAL_STEP_7_SUMMARY -> SetupWizardStep.MANUAL_STEP_6_PUMP
            }
            state.copy(currentStep = prevStep)
        }
    }

    fun completeManualSetup() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isBusy = true, errorMessage = null) }
            runCatching {
                val state = _uiState.value
                val context = registry.appContext

                // 1. Save preferences
                registry.appPreferencesRepository.setGlucoseUnit(state.glucoseUnit)
                registry.appPreferencesRepository.setCarbsUnit(state.carbsUnit)

                // 2. Insert Insulin Types and Meal Types
                val selectedType = state.selectedInsulinType
                    ?: throw IllegalStateException(context.getString(R.string.setup_wizard_error_no_insulin_type))

                val allTypesToInsert = (state.availableInsulinTypes + selectedType).distinctBy { it.id }
                allTypesToInsert.forEach {
                    registry.treatmentRepository.insertInsulinType(it)
                }
                getDefaultMealTypes(context).forEach {
                    registry.treatmentRepository.insertMealType(it)
                }

                // 3. Insert Insulin Profile
                val profileToInsert = (state.insulinProfile ?: getDefaultInsulinProfile(context, selectedType)).copy(
                    insulinType = selectedType,
                    dia = selectedType.dia,
                    peak = selectedType.peak
                )
                val insertedProfileId = registry.therapyRepository.insertInsulinProfile(profileToInsert)

                // 4. Save Therapy Settings with configured BgBlocks
                registry.therapyRepository.updateCurrentTherapySettings(
                    insulinProfileId = insertedProfileId,
                    defaultBgBlocks = state.bgBlocks.ifEmpty {
                        listOf(
                            BgBlock(
                                duration = Minutes.ofHours(24),
                                target = BgValue.fromMgDl(DEFAULT_BG_TARGET_MGDL),
                                lowThreshold = BgValue.fromMgDl(DEFAULT_BG_LOW_THRESHOLD_MGDL)
                            )
                        )
                    }
                )

                // 5. Complete initialization
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