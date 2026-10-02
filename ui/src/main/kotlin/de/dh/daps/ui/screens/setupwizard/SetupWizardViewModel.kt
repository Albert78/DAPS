package de.dh.daps.ui.screens.setupwizard

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import de.dh.daps.common.DEFAULT_BG_LOW_THRESHOLD_MGDL
import de.dh.daps.common.DEFAULT_BG_TARGET_MGDL
import de.dh.daps.common.DEFAULT_DIA_MINUTES
import de.dh.daps.common.DEFAULT_PEAK_MINUTES
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
import de.dh.daps.core.InitializationState
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
    MANUAL_UNITS,
    MANUAL_INSULIN_TYPE,
    MANUAL_INSULIN_PROFILE,
    MANUAL_BG_TARGETS,
    MANUAL_GLUCOSE_SOURCE,
    MANUAL_PUMP,
    MANUAL_SUMMARY;

    val manualStepIndex: Int?
        get() {
            val steps = entries.filter { it != MODE_SELECTION }
            val index = steps.indexOf(this)
            return if (index >= 0) index + 1 else null
        }

    val totalManualSteps: Int
        get() = entries.count { it != MODE_SELECTION }

    val progress: Pair<Int, Int>?
        get() = manualStepIndex?.let { Pair(it, totalManualSteps) }
}

data class SetupWizardUiState(
    val currentStep: SetupWizardStep = SetupWizardStep.MODE_SELECTION,
    val isBusy: Boolean = false,
    val errorMessage: UiText? = null,
    val glucoseUnit: GlucoseUnit = GlucoseUnit.MG_DL,
    val carbsUnit: CarbsUnit = CarbsUnit.GRAMS,
    val availableInsulinTypes: List<InsulinType> = emptyList(),
    val selectedInsulinTypeIds: Set<String> = emptySet(),
    val primaryInsulinTypeId: String? = null,
    val isEditingInsulinType: Boolean = false,
    val insulinTypeEditorUiState: InsulinTypeEditorUiState = InsulinTypeEditorUiState(),
    val insulinProfile: InsulinProfile? = null,
    val bgBlocks: List<BgBlock> = emptyList()
) {
    val selectedInsulinTypes: List<InsulinType>
        get() = availableInsulinTypes.filter { it.id in selectedInsulinTypeIds }

    val selectedInsulinType: InsulinType?
        get() = availableInsulinTypes.firstOrNull { it.id == primaryInsulinTypeId }
            ?: selectedInsulinTypes.firstOrNull()
}

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
        resetState()
        viewModelScope.launch {
            registry.initializationState.collect { state ->
                if (state == InitializationState.REQUIRES_SETUP) {
                    resetState()
                }
            }
        }
    }

    fun resetState() {
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

        _uiState.value = SetupWizardUiState(
            currentStep = SetupWizardStep.MODE_SELECTION,
            isBusy = false,
            errorMessage = null,
            glucoseUnit = GlucoseUnit.MG_DL,
            carbsUnit = CarbsUnit.GRAMS,
            availableInsulinTypes = defaultTypes,
            selectedInsulinTypeIds = primaryType?.let { setOf(it.id) } ?: emptySet(),
            primaryInsulinTypeId = primaryType?.id,
            insulinProfile = initialProfile,
            bgBlocks = initialBgBlocks
        )
    }

    fun selectDemoData() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isBusy = true, errorMessage = null) }
            runCatching {
                registry.completeInitialization(SetupOption.SeedDemoData)
            }.onSuccess {
                resetState()
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
            }.onSuccess {
                resetState()
            }.onFailure { error ->
                val message = error.localizedMessage?.let { UiText.DynamicString(it) }
                    ?: UiText.StringResource(R.string.setup_wizard_error_import_backup)
                _uiState.update { it.copy(isBusy = false, errorMessage = message) }
            }
        }
    }

    fun startManualSetup() {
        _uiState.update { it.copy(currentStep = SetupWizardStep.MANUAL_UNITS) }
    }

    fun setGlucoseUnit(unit: GlucoseUnit) {
        _uiState.update { it.copy(glucoseUnit = unit) }
    }

    fun setCarbsUnit(unit: CarbsUnit) {
        _uiState.update { it.copy(carbsUnit = unit) }
    }

    fun toggleInsulinTypeSelection(type: InsulinType) {
        _uiState.update { state ->
            val newSelectedIds = if (type.id in state.selectedInsulinTypeIds) {
                state.selectedInsulinTypeIds - type.id
            } else {
                state.selectedInsulinTypeIds + type.id
            }

            val newPrimaryId = if (state.primaryInsulinTypeId in newSelectedIds) {
                state.primaryInsulinTypeId
            } else {
                newSelectedIds.firstOrNull()
            }

            val newPrimaryType = state.availableInsulinTypes.firstOrNull { it.id == newPrimaryId }

            val updatedProfile = if (newPrimaryType != null && state.insulinProfile != null) {
                state.insulinProfile.copy(
                    insulinType = newPrimaryType,
                    dia = newPrimaryType.dia,
                    peak = newPrimaryType.peak,
                    insulinConcentration = newPrimaryType.defaultConcentration
                )
            } else state.insulinProfile

            state.copy(
                selectedInsulinTypeIds = newSelectedIds,
                primaryInsulinTypeId = newPrimaryId,
                insulinProfile = updatedProfile
            )
        }
    }

    fun selectPrimaryInsulinType(type: InsulinType) {
        _uiState.update { state ->
            val newSelectedIds = state.selectedInsulinTypeIds + type.id
            val updatedProfile = state.insulinProfile?.copy(
                insulinType = type,
                dia = type.dia,
                peak = type.peak,
                insulinConcentration = type.defaultConcentration
            )
            state.copy(
                selectedInsulinTypeIds = newSelectedIds,
                primaryInsulinTypeId = type.id,
                insulinProfile = updatedProfile
            )
        }
    }

    fun selectInsulinType(type: InsulinType) {
        selectPrimaryInsulinType(type)
    }

    fun startEditingInsulinType(typeToEdit: InsulinType? = null) {
        _uiState.update { state ->
            state.copy(
                isEditingInsulinType = true,
                insulinTypeEditorUiState = InsulinTypeEditorUiState(
                    id = typeToEdit?.id,
                    name = typeToEdit?.name ?: "",
                    peak = typeToEdit?.peak?.value?.toString() ?: DEFAULT_PEAK_MINUTES.toString(),
                    dia = typeToEdit?.dia?.value?.toString() ?: DEFAULT_DIA_MINUTES.toString(),
                    concentration = typeToEdit?.defaultConcentration ?: InsulinConcentration.U100
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
            peak = Minutes((editorState.peak.toIntOrNull() ?: DEFAULT_PEAK_MINUTES).toShort()),
            dia = Minutes((editorState.dia.toIntOrNull() ?: DEFAULT_DIA_MINUTES).toShort()),
            defaultConcentration = editorState.concentration
        )

        _uiState.update { state ->
            val updatedTypes = (state.availableInsulinTypes.filterNot { it.id == newType.id } + newType)
            val newSelectedIds = state.selectedInsulinTypeIds + newType.id
            val isFirstSelection = state.primaryInsulinTypeId == null || state.selectedInsulinTypeIds.isEmpty()
            val newPrimaryId = if (isFirstSelection) newType.id else state.primaryInsulinTypeId

            val primaryType = updatedTypes.firstOrNull { it.id == newPrimaryId }
            val updatedProfile = if (primaryType != null && state.insulinProfile != null) {
                state.insulinProfile.copy(
                    insulinType = primaryType,
                    dia = primaryType.dia,
                    peak = primaryType.peak,
                    insulinConcentration = primaryType.defaultConcentration
                )
            } else state.insulinProfile

            state.copy(
                availableInsulinTypes = updatedTypes,
                selectedInsulinTypeIds = newSelectedIds,
                primaryInsulinTypeId = newPrimaryId,
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
                SetupWizardStep.MODE_SELECTION -> SetupWizardStep.MANUAL_UNITS
                SetupWizardStep.MANUAL_UNITS -> SetupWizardStep.MANUAL_INSULIN_TYPE
                SetupWizardStep.MANUAL_INSULIN_TYPE -> SetupWizardStep.MANUAL_INSULIN_PROFILE
                SetupWizardStep.MANUAL_INSULIN_PROFILE -> SetupWizardStep.MANUAL_BG_TARGETS
                SetupWizardStep.MANUAL_BG_TARGETS -> SetupWizardStep.MANUAL_GLUCOSE_SOURCE
                SetupWizardStep.MANUAL_GLUCOSE_SOURCE -> SetupWizardStep.MANUAL_PUMP
                SetupWizardStep.MANUAL_PUMP -> SetupWizardStep.MANUAL_SUMMARY
                SetupWizardStep.MANUAL_SUMMARY -> SetupWizardStep.MANUAL_SUMMARY
            }
            state.copy(currentStep = nextStep)
        }
    }

    fun goToPreviousStep() {
        _uiState.update { state ->
            val prevStep = when (state.currentStep) {
                SetupWizardStep.MODE_SELECTION -> SetupWizardStep.MODE_SELECTION
                SetupWizardStep.MANUAL_UNITS -> SetupWizardStep.MODE_SELECTION
                SetupWizardStep.MANUAL_INSULIN_TYPE -> SetupWizardStep.MANUAL_UNITS
                SetupWizardStep.MANUAL_INSULIN_PROFILE -> SetupWizardStep.MANUAL_INSULIN_TYPE
                SetupWizardStep.MANUAL_BG_TARGETS -> SetupWizardStep.MANUAL_INSULIN_PROFILE
                SetupWizardStep.MANUAL_GLUCOSE_SOURCE -> SetupWizardStep.MANUAL_BG_TARGETS
                SetupWizardStep.MANUAL_PUMP -> SetupWizardStep.MANUAL_GLUCOSE_SOURCE
                SetupWizardStep.MANUAL_SUMMARY -> SetupWizardStep.MANUAL_PUMP
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

                // Save preferences
                registry.appPreferencesRepository.setGlucoseUnit(state.glucoseUnit)
                registry.appPreferencesRepository.setCarbsUnit(state.carbsUnit)

                // Insert Insulin Types and Meal Types
                val selectedTypes = state.selectedInsulinTypes
                val primaryType = state.selectedInsulinType
                    ?: throw IllegalStateException(context.getString(R.string.setup_wizard_error_no_insulin_type))

                selectedTypes.forEach {
                    registry.treatmentRepository.insertInsulinType(it)
                }
                getDefaultMealTypes(context).forEach {
                    registry.treatmentRepository.insertMealType(it)
                }

                // Insert Insulin Profile
                val profileToInsert = (state.insulinProfile ?: getDefaultInsulinProfile(context, primaryType)).copy(
                    insulinType = primaryType,
                    dia = primaryType.dia,
                    peak = primaryType.peak
                )
                val insertedProfileId = registry.therapyRepository.insertInsulinProfile(profileToInsert)

                // Save Therapy Settings with configured BgBlocks
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

                // Complete initialization
                registry.completeInitialization(SetupOption.ManualSetupCompleted)
            }.onSuccess {
                resetState()
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