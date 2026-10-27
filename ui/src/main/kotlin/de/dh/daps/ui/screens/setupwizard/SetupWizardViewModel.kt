package de.dh.daps.ui.screens.setupwizard

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import de.dh.daps.common.DEFAULT_BG_LOW_THRESHOLD_MGDL
import de.dh.daps.common.DEFAULT_BG_TARGET_MGDL
import de.dh.daps.common.DEFAULT_DIA_MINUTES
import de.dh.daps.common.DEFAULT_PEAK_MINUTES
import de.dh.daps.common.domain.model.data.BgValue
import de.dh.daps.common.domain.model.data.CarbsUnit
import de.dh.daps.common.domain.model.data.GlucoseUnit
import de.dh.daps.common.domain.model.data.Minutes
import de.dh.daps.common.domain.model.data.BgBlock
import de.dh.daps.common.domain.model.therapy.InsulinProfile
import de.dh.daps.common.domain.model.therapy.InsulinType
import de.dh.daps.common.domain.model.therapy.ProfileInsulinSetting
import de.dh.daps.common.domain.model.therapy.getDefaultInsulinProfile
import de.dh.daps.common.domain.model.therapy.getDefaultInsulinTypes
import de.dh.daps.common.domain.model.therapy.getDefaultMealTypes
import de.dh.daps.common.repository.setCarbsUnit
import de.dh.daps.common.repository.setGlucoseUnit
import de.dh.daps.core.InitializationState
import de.dh.daps.core.SetupOption
import de.dh.daps.core.SystemRegistry
import de.dh.daps.ui.R
import de.dh.daps.ui.screens.insulintypes.InsulinTypeEditorUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

enum class SetupWizardStep(val progress: Pair<Int, Int>) {
    MODE_SELECTION(1 to 5),
    MANUAL_UNITS(2 to 5),
    MANUAL_INSULIN_TYPE(3 to 5),
    MANUAL_INSULIN_PROFILE(4 to 5),
    MANUAL_BG_TARGETS(5 to 5),
    MANUAL_SUMMARY(5 to 5)
}

data class SetupWizardUiState(
    val currentStep: SetupWizardStep = SetupWizardStep.MODE_SELECTION,
    val isBusy: Boolean = false,
    val errorMessage: String? = null,
    val glucoseUnit: GlucoseUnit = GlucoseUnit.MG_DL,
    val carbsUnit: CarbsUnit = CarbsUnit.GRAMS,
    val availableInsulinTypes: List<InsulinType> = emptyList(),
    val selectedInsulinTypeIds: Set<String> = emptySet(),
    val isEditingInsulinType: Boolean = false,
    val insulinTypeEditorUiState: InsulinTypeEditorUiState = InsulinTypeEditorUiState(),
    val insulinProfile: InsulinProfile? = null,
    val bgBlocks: List<BgBlock> = emptyList()
) {
    val selectedInsulinTypes: List<InsulinType>
        get() = availableInsulinTypes.filter { it.id in selectedInsulinTypeIds }
}

class SetupWizardViewModel(
    val registry: SystemRegistry
) : ViewModel() {
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
        val initialProfile = getDefaultInsulinProfile(context, defaultTypes)

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
            selectedInsulinTypeIds = emptySet(),
            insulinProfile = initialProfile,
            bgBlocks = initialBgBlocks
        )
    }

    fun selectDemoData() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isBusy = true, errorMessage = null) }
            runCatching {
                registry.completeInitialization(SetupOption.SeedDemoData)
            }.onFailure { ex ->
                _uiState.update {
                    it.copy(
                        isBusy = false,
                        errorMessage = ex.message ?: registry.appContext.getString(R.string.setup_wizard_error_load_demo_data)
                    )
                }
            }
        }
    }

    fun startManualSetup() {
        goToNextStep()
    }

    fun importBackup(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isBusy = true, errorMessage = null) }
            runCatching {
                registry.completeInitialization(SetupOption.ImportBackup(uri))
            }.onFailure { ex ->
                _uiState.update {
                    it.copy(
                        isBusy = false,
                        errorMessage = ex.message ?: registry.appContext.getString(R.string.setup_wizard_error_import_backup)
                    )
                }
            }
        }
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

            val updatedProfile = if (state.insulinProfile != null) {
                val selectedTypes = state.availableInsulinTypes.filter { it.id in newSelectedIds }
                val currentSettingsMap = state.insulinProfile.insulinSettings.associateBy { it.insulinType.id }
                val newSettings = selectedTypes.map { selectedType ->
                    currentSettingsMap[selectedType.id] ?: ProfileInsulinSetting(
                        insulinType = selectedType,
                        dia = selectedType.dia,
                        peak = selectedType.peak
                    )
                }
                state.insulinProfile.copy(insulinSettings = newSettings)
            } else state.insulinProfile

            state.copy(
                selectedInsulinTypeIds = newSelectedIds,
                insulinProfile = updatedProfile
            )
        }
    }

    fun startEditingInsulinType(typeToEdit: InsulinType? = null) {
        _uiState.update { state ->
            state.copy(
                isEditingInsulinType = true,
                insulinTypeEditorUiState = InsulinTypeEditorUiState(
                    id = typeToEdit?.id,
                    name = typeToEdit?.name ?: "",
                    activeSubstance = typeToEdit?.activeSubstance,
                    peak = typeToEdit?.peak?.value?.toString() ?: "",
                    dia = typeToEdit?.dia?.value?.toString() ?: ""
                )
            )
        }
    }

    fun cancelEditingInsulinType() {
        _uiState.update { state ->
            state.copy(
                isEditingInsulinType = false,
                insulinTypeEditorUiState = InsulinTypeEditorUiState()
            )
        }
    }

    fun updateInsulinTypeEditorName(name: String) {
        _uiState.update { state ->
            state.copy(
                insulinTypeEditorUiState = state.insulinTypeEditorUiState.copy(name = name)
            )
        }
    }

    fun updateInsulinTypeEditorActiveSubstance(substance: String) {
        _uiState.update { state ->
            state.copy(
                insulinTypeEditorUiState = state.insulinTypeEditorUiState.copy(activeSubstance = substance)
            )
        }
    }

    fun updateInsulinTypeEditorPeak(peak: String) {
        _uiState.update { state ->
            state.copy(
                insulinTypeEditorUiState = state.insulinTypeEditorUiState.copy(peak = peak)
            )
        }
    }

    fun updateInsulinTypeEditorDia(dia: String) {
        _uiState.update { state ->
            state.copy(
                insulinTypeEditorUiState = state.insulinTypeEditorUiState.copy(dia = dia)
            )
        }
    }

    fun saveEditedInsulinType() {
        val editorState = _uiState.value.insulinTypeEditorUiState
        if (!editorState.isValid) return

        val newType = InsulinType(
            id = editorState.id ?: UUID.randomUUID().toString(),
            name = editorState.name.trim(),
            activeSubstance = editorState.activeSubstance?.trim()?.ifEmpty { null },
            peak = Minutes((editorState.peak.toIntOrNull() ?: DEFAULT_PEAK_MINUTES).toShort()),
            dia = Minutes((editorState.dia.toIntOrNull() ?: DEFAULT_DIA_MINUTES).toShort())
        )

        _uiState.update { state ->
            val updatedTypes = (state.availableInsulinTypes.filterNot { it.id == newType.id } + newType)
            val newSelectedIds = state.selectedInsulinTypeIds + newType.id

            val updatedProfile = if (state.insulinProfile != null) {
                val selectedTypes = updatedTypes.filter { it.id in newSelectedIds }
                val currentSettingsMap = state.insulinProfile.insulinSettings.associateBy { it.insulinType.id }
                val newSettings = selectedTypes.map { type ->
                    currentSettingsMap[type.id] ?: ProfileInsulinSetting(insulinType = type, dia = type.dia, peak = type.peak)
                }
                state.insulinProfile.copy(insulinSettings = newSettings)
            } else state.insulinProfile

            state.copy(
                availableInsulinTypes = updatedTypes,
                selectedInsulinTypeIds = newSelectedIds,
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
                SetupWizardStep.MANUAL_BG_TARGETS -> SetupWizardStep.MANUAL_SUMMARY
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
                SetupWizardStep.MANUAL_SUMMARY -> SetupWizardStep.MANUAL_BG_TARGETS
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
                if (selectedTypes.isEmpty()) {
                    throw IllegalStateException(context.getString(R.string.setup_wizard_error_no_insulin_type))
                }

                selectedTypes.forEach {
                    registry.treatmentRepository.insertInsulinType(it)
                }
                getDefaultMealTypes(context).forEach {
                    registry.treatmentRepository.insertMealType(it)
                }

                // Insert Insulin Profile
                val profileToInsert = state.insulinProfile ?: getDefaultInsulinProfile(context, selectedTypes)
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

                registry.completeInitialization(SetupOption.ManualSetupCompleted)
            }.onFailure { ex ->
                _uiState.update {
                    it.copy(
                        isBusy = false,
                        errorMessage = ex.message ?: registry.appContext.getString(R.string.setup_wizard_error_manual_setup)
                    )
                }
            }
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    companion object {
        fun Factory(registry: SystemRegistry): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return SetupWizardViewModel(registry) as T
            }
        }
    }
}