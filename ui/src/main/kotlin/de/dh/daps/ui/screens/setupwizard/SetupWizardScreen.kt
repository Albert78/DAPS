package de.dh.daps.ui.screens.setupwizard

import android.content.res.Configuration
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import de.dh.daps.common.model.GlucoseSourceConnectionDescriptor
import de.dh.daps.common.model.GlucoseSourceDriver
import de.dh.daps.common.model.InsulinConcentration
import de.dh.daps.common.model.InsulinPumpDriver
import de.dh.daps.common.model.InsulinType
import de.dh.daps.common.model.PumpConnectionDescriptor
import de.dh.daps.common.model.data.BgBlock
import de.dh.daps.common.model.data.BgValue
import de.dh.daps.common.model.data.Block
import de.dh.daps.common.model.data.CarbsUnit
import de.dh.daps.common.model.data.GlucoseUnit
import de.dh.daps.common.model.data.InsulinProfile
import de.dh.daps.common.model.data.Minutes
import de.dh.daps.ui.R
import de.dh.daps.ui.common.LocalCarbsUnit
import de.dh.daps.ui.common.LocalGlucoseUnit
import de.dh.daps.ui.common.composables.LoadingScreen
import de.dh.daps.ui.common.theme.AppPreview
import de.dh.daps.ui.screens.glucosesourcesetup.GlucoseSourceSetupUiState
import de.dh.daps.ui.screens.glucosesourcesetup.GlucoseSourceSetupViewModel
import de.dh.daps.ui.screens.insulintypes.InsulinTypeEditorUiState
import de.dh.daps.ui.screens.pumpsetup.PumpSetupUiState
import de.dh.daps.ui.screens.pumpsetup.PumpSetupViewModel
import de.dh.daps.ui.screens.setupwizard.steps.BgTargetsStep
import de.dh.daps.ui.screens.setupwizard.steps.GlucoseSourceStep
import de.dh.daps.ui.screens.setupwizard.steps.InsulinProfileStep
import de.dh.daps.ui.screens.setupwizard.steps.InsulinTypeStep
import de.dh.daps.ui.screens.setupwizard.steps.ModeSelectionStep
import de.dh.daps.ui.screens.setupwizard.steps.PumpStep
import de.dh.daps.ui.screens.setupwizard.steps.SummaryStep
import de.dh.daps.ui.screens.setupwizard.steps.UnitsStep

@Composable
fun SetupWizardScreen(
    viewModel: SetupWizardViewModel,
    glucoseSourceSetupViewModel: GlucoseSourceSetupViewModel,
    pumpSetupViewModel: PumpSetupViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val cgmState by glucoseSourceSetupViewModel.uiState.collectAsState()
    val pumpState by pumpSetupViewModel.uiState.collectAsState()
    val cgmDescriptor by viewModel.activeGlucoseSourceDescriptor.collectAsState()
    val pumpDescriptor by viewModel.activePumpDescriptor.collectAsState()

    SetupWizardContent(
        uiState = uiState,
        cgmUiState = cgmState,
        pumpUiState = pumpState,
        cgmDescriptor = cgmDescriptor,
        pumpDescriptor = pumpDescriptor,
        onSelectDemo = viewModel::selectDemoData,
        onStartManual = viewModel::startManualSetup,
        onImportBackup = viewModel::importBackup,
        onSetGlucoseUnit = viewModel::setGlucoseUnit,
        onSetCarbsUnit = viewModel::setCarbsUnit,
        onToggleInsulinTypeSelection = viewModel::toggleInsulinTypeSelection,
        onSelectPrimaryInsulinType = viewModel::selectPrimaryInsulinType,
        onStartEditingInsulinType = viewModel::startEditingInsulinType,
        onCancelEditingInsulinType = viewModel::cancelEditingInsulinType,
        onUpdateInsulinTypeEditorName = viewModel::updateInsulinTypeEditorName,
        onUpdateInsulinTypeEditorActiveSubstance = viewModel::updateInsulinTypeEditorActiveSubstance,
        onUpdateInsulinTypeEditorPeak = viewModel::updateInsulinTypeEditorPeak,
        onUpdateInsulinTypeEditorDia = viewModel::updateInsulinTypeEditorDia,
        onUpdateInsulinTypeEditorConcentration = viewModel::updateInsulinTypeEditorConcentration,
        onSaveEditedInsulinType = viewModel::saveEditedInsulinType,
        onSetInsulinProfile = viewModel::setInsulinProfile,
        onSetBgBlocks = viewModel::setBgBlocks,
        onSelectCgmDriver = glucoseSourceSetupViewModel::selectDriver,
        onConnectGlucoseSource = { descriptor, onSuccess ->
            glucoseSourceSetupViewModel.connectGlucoseSource(descriptor, onSuccess)
        },
        onClearCgmError = glucoseSourceSetupViewModel::clearError,
        onSelectPumpDriver = pumpSetupViewModel::selectDriver,
        onConnectPump = { descriptor, onSuccess ->
            pumpSetupViewModel.connectPump(descriptor, onSuccess)
        },
        onClearPumpError = pumpSetupViewModel::clearError,
        onGoToNextStep = viewModel::goToNextStep,
        onGoToPreviousStep = viewModel::goToPreviousStep,
        onCompleteManualSetup = viewModel::completeManualSetup
    )
}

@Composable
fun SetupWizardContent(
    uiState: SetupWizardUiState,
    cgmUiState: GlucoseSourceSetupUiState = GlucoseSourceSetupUiState(),
    pumpUiState: PumpSetupUiState = PumpSetupUiState(),
    cgmDescriptor: GlucoseSourceConnectionDescriptor? = null,
    pumpDescriptor: PumpConnectionDescriptor? = null,
    onSelectDemo: () -> Unit = {},
    onStartManual: () -> Unit = {},
    onImportBackup: (Uri) -> Unit = {},
    onSetGlucoseUnit: (GlucoseUnit) -> Unit = {},
    onSetCarbsUnit: (CarbsUnit) -> Unit = {},
    onToggleInsulinTypeSelection: (InsulinType) -> Unit = {},
    onSelectPrimaryInsulinType: (InsulinType) -> Unit = {},
    onStartEditingInsulinType: (InsulinType?) -> Unit = {},
    onCancelEditingInsulinType: () -> Unit = {},
    onUpdateInsulinTypeEditorName: (String) -> Unit = {},
    onUpdateInsulinTypeEditorActiveSubstance: (String) -> Unit = {},
    onUpdateInsulinTypeEditorPeak: (String) -> Unit = {},
    onUpdateInsulinTypeEditorDia: (String) -> Unit = {},
    onUpdateInsulinTypeEditorConcentration: (InsulinConcentration) -> Unit = {},
    onSaveEditedInsulinType: () -> Unit = {},
    onSetInsulinProfile: (InsulinProfile) -> Unit = {},
    onSetBgBlocks: (List<BgBlock>) -> Unit = {},
    onSelectCgmDriver: (GlucoseSourceDriver?) -> Unit = {},
    onConnectGlucoseSource: (GlucoseSourceConnectionDescriptor, onSuccess: () -> Unit) -> Unit = { _, _ -> },
    onClearCgmError: () -> Unit = {},
    onSelectPumpDriver: (InsulinPumpDriver?) -> Unit = {},
    onConnectPump: (PumpConnectionDescriptor, onSuccess: () -> Unit) -> Unit = { _, _ -> },
    onClearPumpError: () -> Unit = {},
    onGoToNextStep: () -> Unit = {},
    onGoToPreviousStep: () -> Unit = {},
    onCompleteManualSetup: () -> Unit = {}
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(uiState.errorMessage) {
        val error = uiState.errorMessage
        if (error != null) {
            snackbarHostState.showSnackbar(error.asString(context))
        }
    }

    val canGoBack = uiState.currentStep != SetupWizardStep.MODE_SELECTION || uiState.isEditingInsulinType
    BackHandler(enabled = canGoBack) {
        if (uiState.isEditingInsulinType) {
            onCancelEditingInsulinType()
        } else {
            onGoToPreviousStep()
        }
    }

    CompositionLocalProvider(
        LocalGlucoseUnit provides uiState.glucoseUnit,
        LocalCarbsUnit provides uiState.carbsUnit
    ) {
        if (uiState.isBusy) {
            Scaffold { innerPadding ->
                LoadingScreen(
                    message = stringResource(R.string.setup_wizard_busy_text),
                    modifier = Modifier.padding(innerPadding)
                )
            }
        } else {
            AnimatedContent(
                targetState = uiState.currentStep,
                transitionSpec = {
                    if (targetState.ordinal > initialState.ordinal) {
                        slideInHorizontally { width -> width } + fadeIn() togetherWith
                                slideOutHorizontally { width -> -width } + fadeOut()
                    } else {
                        slideInHorizontally { width -> -width } + fadeIn() togetherWith
                                slideOutHorizontally { width -> width } + fadeOut()
                    }
                },
                label = "SetupWizardStepTransition"
            ) { step ->
                when (step) {
                    SetupWizardStep.MODE_SELECTION -> {
                        ModeSelectionStep(
                            onSelectDemo = onSelectDemo,
                            onStartManual = onStartManual,
                            onImportBackup = onImportBackup,
                            snackbarHostState = snackbarHostState
                        )
                    }

                    SetupWizardStep.MANUAL_UNITS -> {
                        UnitsStep(
                            glucoseUnit = uiState.glucoseUnit,
                            carbsUnit = uiState.carbsUnit,
                            onGlucoseUnitSelected = onSetGlucoseUnit,
                            onCarbsUnitSelected = onSetCarbsUnit,
                            onNext = onGoToNextStep,
                            onBack = onGoToPreviousStep,
                            snackbarHostState = snackbarHostState,
                            stepProgress = step.progress
                        )
                    }

                    SetupWizardStep.MANUAL_INSULIN_TYPE -> {
                        InsulinTypeStep(
                            availableTypes = uiState.availableInsulinTypes,
                            selectedTypeIds = uiState.selectedInsulinTypeIds,
                            primaryTypeId = uiState.primaryInsulinTypeId,
                            isEditing = uiState.isEditingInsulinType,
                            editorUiState = uiState.insulinTypeEditorUiState,
                            onToggleTypeSelection = onToggleInsulinTypeSelection,
                            onSelectPrimaryType = onSelectPrimaryInsulinType,
                            onStartEditing = onStartEditingInsulinType,
                            onCancelEditing = onCancelEditingInsulinType,
                            onUpdateName = onUpdateInsulinTypeEditorName,
                            onUpdateActiveSubstance = onUpdateInsulinTypeEditorActiveSubstance,
                            onUpdatePeak = onUpdateInsulinTypeEditorPeak,
                            onUpdateDia = onUpdateInsulinTypeEditorDia,
                            onUpdateConcentration = onUpdateInsulinTypeEditorConcentration,
                            onSaveEditedType = onSaveEditedInsulinType,
                            onNext = onGoToNextStep,
                            onBack = onGoToPreviousStep,
                            snackbarHostState = snackbarHostState,
                            stepProgress = step.progress
                        )
                    }

                    SetupWizardStep.MANUAL_INSULIN_PROFILE -> {
                        val profile = uiState.insulinProfile
                        if (profile != null) {
                            InsulinProfileStep(
                                profile = profile,
                                availableInsulinTypes = uiState.selectedInsulinTypes,
                                onSaveProfile = onSetInsulinProfile,
                                onNext = onGoToNextStep,
                                onBack = onGoToPreviousStep,
                                snackbarHostState = snackbarHostState,
                                stepProgress = step.progress
                            )
                        }
                    }

                    SetupWizardStep.MANUAL_BG_TARGETS -> {
                        BgTargetsStep(
                            bgBlocks = uiState.bgBlocks,
                            onSetBgBlocks = onSetBgBlocks,
                            onNext = onGoToNextStep,
                            onBack = onGoToPreviousStep,
                            snackbarHostState = snackbarHostState,
                            stepProgress = step.progress
                        )
                    }

                    SetupWizardStep.MANUAL_GLUCOSE_SOURCE -> {
                        GlucoseSourceStep(
                            uiState = cgmUiState,
                            onSelectDriver = onSelectCgmDriver,
                            onConnectGlucoseSource = onConnectGlucoseSource,
                            onClearError = onClearCgmError,
                            onNext = onGoToNextStep,
                            onBack = onGoToPreviousStep,
                            snackbarHostState = snackbarHostState,
                            stepProgress = step.progress
                        )
                    }

                    SetupWizardStep.MANUAL_PUMP -> {
                        PumpStep(
                            uiState = pumpUiState,
                            onSelectDriver = onSelectPumpDriver,
                            onConnectPump = onConnectPump,
                            onClearError = onClearPumpError,
                            onNext = onGoToNextStep,
                            onBack = onGoToPreviousStep,
                            snackbarHostState = snackbarHostState,
                            stepProgress = step.progress
                        )
                    }

                    SetupWizardStep.MANUAL_SUMMARY -> {
                        SummaryStep(
                            uiState = uiState,
                            cgmDisplayName = cgmDescriptor?.displayName,
                            pumpDisplayName = pumpDescriptor?.displayName,
                            onComplete = onCompleteManualSetup,
                            onBack = onGoToPreviousStep,
                            snackbarHostState = snackbarHostState,
                            stepProgress = step.progress
                        )
                    }
                }
            }
        }
    }
}

private val previewInsulinType = InsulinType(
    id = "1",
    name = "NovoRapid",
    activeSubstance = "Insulin aspart",
    dia = Minutes.ofHours(5),
    peak = Minutes(75)
)

private val previewInsulinProfile = InsulinProfile(
    id = 1L,
    name = "Standard",
    basalBlocks = listOf(Block(Minutes.ofHours(24), 1.0)),
    isfBlocks = listOf(Block(Minutes.ofHours(24), 40.0)),
    crBlocks = listOf(Block(Minutes.ofHours(24), 10.0)),
    insulinType = previewInsulinType,
    insulinConcentration = InsulinConcentration.U100,
    dia = Minutes.ofHours(5),
    peak = Minutes(75)
)

private val previewBgBlocks = listOf(
    BgBlock(
        duration = Minutes.ofHours(24),
        target = BgValue.fromMgDl(100.0),
        lowThreshold = BgValue.fromMgDl(70.0)
    )
)

@Preview(showBackground = true, name = "Mode Selection")
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, name = "1 - Mode Selection (Dark)")
@Composable
fun SetupWizardModeSelectionPreview() {
    AppPreview {
        SetupWizardContent(
            uiState = SetupWizardUiState(
                currentStep = SetupWizardStep.MODE_SELECTION
            )
        )
    }
}

@Preview(showBackground = true, name = "Units")
@Composable
fun SetupWizardUnitsStepPreview() {
    AppPreview {
        SetupWizardContent(
            uiState = SetupWizardUiState(
                currentStep = SetupWizardStep.MANUAL_UNITS,
                glucoseUnit = GlucoseUnit.MG_DL,
                carbsUnit = CarbsUnit.GRAMS
            )
        )
    }
}

@Preview(showBackground = true, name = "Insulin Type")
@Composable
fun SetupWizardInsulinTypeStepPreview() {
    AppPreview {
        SetupWizardContent(
            uiState = SetupWizardUiState(
                currentStep = SetupWizardStep.MANUAL_INSULIN_TYPE,
                availableInsulinTypes = listOf(previewInsulinType),
                selectedInsulinTypeIds = setOf(previewInsulinType.id),
                primaryInsulinTypeId = previewInsulinType.id
            )
        )
    }
}

@Preview(showBackground = true, name = "Edit Insulin Type")
@Composable
fun SetupWizardInsulinTypeEditPreview() {
    AppPreview {
        SetupWizardContent(
            uiState = SetupWizardUiState(
                currentStep = SetupWizardStep.MANUAL_INSULIN_TYPE,
                isEditingInsulinType = true,
                insulinTypeEditorUiState = InsulinTypeEditorUiState(
                    id = previewInsulinType.id,
                    name = previewInsulinType.name,
                    peak = previewInsulinType.peak.value.toString(),
                    dia = previewInsulinType.dia.value.toString(),
                    concentration = previewInsulinType.defaultConcentration
                )
            )
        )
    }
}

@Preview(showBackground = true, name = "Insulin Profile")
@Composable
fun SetupWizardInsulinProfileStepPreview() {
    AppPreview {
        SetupWizardContent(
            uiState = SetupWizardUiState(
                currentStep = SetupWizardStep.MANUAL_INSULIN_PROFILE,
                availableInsulinTypes = listOf(previewInsulinType),
                selectedInsulinTypeIds = setOf(previewInsulinType.id),
                primaryInsulinTypeId = previewInsulinType.id,
                insulinProfile = previewInsulinProfile
            )
        )
    }
}

@Preview(showBackground = true, name = "BG Targets")
@Composable
fun SetupWizardBgTargetsStepPreview() {
    AppPreview {
        SetupWizardContent(
            uiState = SetupWizardUiState(
                currentStep = SetupWizardStep.MANUAL_BG_TARGETS,
                bgBlocks = previewBgBlocks
            )
        )
    }
}

@Preview(showBackground = true, name = "Glucose Source")
@Composable
fun SetupWizardGlucoseSourceStepPreview() {
    AppPreview {
        SetupWizardContent(
            uiState = SetupWizardUiState(
                currentStep = SetupWizardStep.MANUAL_GLUCOSE_SOURCE
            )
        )
    }
}

@Preview(showBackground = true, name = "Pump")
@Composable
fun SetupWizardPumpStepPreview() {
    AppPreview {
        SetupWizardContent(
            uiState = SetupWizardUiState(
                currentStep = SetupWizardStep.MANUAL_PUMP
            )
        )
    }
}

@Preview(showBackground = true, name = "Summary")
@Composable
fun SetupWizardSummaryStepPreview() {
    AppPreview {
        SetupWizardContent(
            uiState = SetupWizardUiState(
                currentStep = SetupWizardStep.MANUAL_SUMMARY,
                glucoseUnit = GlucoseUnit.MG_DL,
                carbsUnit = CarbsUnit.GRAMS,
                selectedInsulinTypeIds = setOf(previewInsulinType.id),
                primaryInsulinTypeId = previewInsulinType.id,
                insulinProfile = previewInsulinProfile,
                bgBlocks = previewBgBlocks
            )
        )
    }
}

@Preview(showBackground = true, name = "Initializing / Busy")
@Composable
fun SetupWizardBusyPreview() {
    AppPreview {
        SetupWizardContent(
            uiState = SetupWizardUiState(isBusy = true)
        )
    }
}