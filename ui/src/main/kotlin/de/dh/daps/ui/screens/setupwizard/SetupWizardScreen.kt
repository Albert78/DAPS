package de.dh.daps.ui.screens.setupwizard

import android.content.res.Configuration
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.dh.daps.common.model.InsulinType
import de.dh.daps.common.model.data.CarbsUnit
import de.dh.daps.common.model.data.GlucoseUnit
import de.dh.daps.common.model.data.Minutes
import de.dh.daps.ui.R
import de.dh.daps.ui.common.LocalCarbsUnit
import de.dh.daps.ui.common.LocalGlucoseUnit
import de.dh.daps.ui.common.carbsUnitLabel
import de.dh.daps.ui.common.composables.contentScrollIndicator
import de.dh.daps.ui.common.composables.screenTitle
import de.dh.daps.ui.common.glucoseUnitLabel
import de.dh.daps.ui.common.theme.AppPreview
import de.dh.daps.ui.screens.glucosesourcesetup.GlucoseSourceSetupContent
import de.dh.daps.ui.screens.insulinprofile.InsulinProfileDetailEditor
import de.dh.daps.ui.screens.insulintypes.InsulinTypeEditorContent
import de.dh.daps.ui.screens.pumpsetup.PumpSetupContent
import de.dh.daps.ui.screens.therapy.BgEditorContent

@Composable
fun SetupWizardScreen(
    viewModel: SetupWizardViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.importBackup(uri)
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        val error = uiState.errorMessage
        if (error != null) {
            snackbarHostState.showSnackbar(error.asString(context))
        }
    }

    CompositionLocalProvider(
        LocalGlucoseUnit provides uiState.glucoseUnit,
        LocalCarbsUnit provides uiState.carbsUnit
    ) {
        if (uiState.isBusy) {
            Scaffold { innerPadding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.setup_wizard_busy_text),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        } else {
            when (uiState.currentStep) {
                SetupWizardStep.MODE_SELECTION -> {
                    WizardScaffold(
                        title = stringResource(R.string.setup_wizard_title),
                        showBackButton = false,
                        onBack = {},
                        snackbarHostState = snackbarHostState
                    ) {
                        ModeSelectionContent(
                            onSelectDemo = { viewModel.selectDemoData() },
                            onStartManual = { viewModel.startManualSetup() },
                            onSelectImport = { filePickerLauncher.launch(arrayOf("*/*")) }
                        )
                    }
                }

                SetupWizardStep.MANUAL_STEP_1_UNITS -> {
                    WizardScaffold(
                        title = stringResource(R.string.setup_wizard_step1_units_title),
                        showBackButton = true,
                        onBack = { viewModel.goToPreviousStep() },
                        snackbarHostState = snackbarHostState
                    ) {
                        UnitsStepContent(
                            glucoseUnit = uiState.glucoseUnit,
                            carbsUnit = uiState.carbsUnit,
                            onGlucoseUnitSelected = { viewModel.setGlucoseUnit(it) },
                            onCarbsUnitSelected = { viewModel.setCarbsUnit(it) },
                            onNext = { viewModel.goToNextStep() },
                            onBack = { viewModel.goToPreviousStep() }
                        )
                    }
                }

                SetupWizardStep.MANUAL_STEP_2_INSULIN_TYPE -> {
                    if (uiState.isEditingInsulinType) {
                        InsulinTypeEditorContent(
                            uiState = uiState.insulinTypeEditorUiState,
                            onNameChange = viewModel::updateInsulinTypeEditorName,
                            onPeakChange = viewModel::updateInsulinTypeEditorPeak,
                            onDiaChange = viewModel::updateInsulinTypeEditorDia,
                            onConcentrationChange = viewModel::updateInsulinTypeEditorConcentration,
                            onSave = viewModel::saveEditedInsulinType,
                            onNavigateUp = viewModel::cancelEditingInsulinType
                        )
                    } else {
                        WizardScaffold(
                            title = stringResource(R.string.setup_wizard_step2_insulin_title),
                            showBackButton = true,
                            onBack = { viewModel.goToPreviousStep() },
                            snackbarHostState = snackbarHostState
                        ) {
                            InsulinTypeStepContent(
                                availableTypes = uiState.availableInsulinTypes,
                                selectedType = uiState.selectedInsulinType,
                                onSelectType = viewModel::selectInsulinType,
                                onEditType = { viewModel.startEditingInsulinType(it) },
                                onAddNewType = { viewModel.startEditingInsulinType(null) },
                                onNext = { viewModel.goToNextStep() },
                                onBack = { viewModel.goToPreviousStep() }
                            )
                        }
                    }
                }

                SetupWizardStep.MANUAL_STEP_3_INSULIN_PROFILE -> {
                    val profile = uiState.insulinProfile
                    if (profile != null) {
                        InsulinProfileDetailEditor(
                            profile = profile,
                            insulinTypes = uiState.availableInsulinTypes,
                            onSave = { updatedProfile ->
                                viewModel.setInsulinProfile(updatedProfile)
                                viewModel.goToNextStep()
                            },
                            onCancel = { viewModel.goToPreviousStep() }
                        )
                    }
                }

                SetupWizardStep.MANUAL_STEP_4_BG_TARGETS -> {
                    BgEditorContent(
                        blocks = uiState.bgBlocks,
                        onBlocksChanged = { viewModel.setBgBlocks(it) },
                        onSave = { viewModel.goToNextStep() },
                        onNavigateUp = { viewModel.goToPreviousStep() },
                        originalBlocks = uiState.bgBlocks
                    )
                }

                SetupWizardStep.MANUAL_STEP_5_GLUCOSE_SOURCE -> {
                    val cgmState by viewModel.glucoseSourceSetupViewModel.uiState.collectAsState()
                    Box(modifier = Modifier.fillMaxSize()) {
                        GlucoseSourceSetupContent(
                            uiState = cgmState,
                            onNavigateUp = { viewModel.goToPreviousStep() },
                            onSelectDriver = { viewModel.glucoseSourceSetupViewModel.selectDriver(it) },
                            onConnectGlucoseSource = { descriptor ->
                                viewModel.glucoseSourceSetupViewModel.connectGlucoseSource(
                                    descriptor = descriptor,
                                    onSuccess = { viewModel.goToNextStep() }
                                )
                            },
                            onClearError = { viewModel.glucoseSourceSetupViewModel.clearError() }
                        )
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            OutlinedButton(onClick = { viewModel.goToPreviousStep() }) {
                                Text(stringResource(R.string.setup_wizard_btn_back))
                            }
                            Button(onClick = { viewModel.goToNextStep() }) {
                                Text(
                                    if (cgmState.activeSourceDescriptor != null)
                                        stringResource(R.string.setup_wizard_btn_next)
                                    else
                                        stringResource(R.string.setup_wizard_btn_skip)
                                )
                            }
                        }
                    }
                }

                SetupWizardStep.MANUAL_STEP_6_PUMP -> {
                    val pumpState by viewModel.pumpSetupViewModel.uiState.collectAsState()
                    Box(modifier = Modifier.fillMaxSize()) {
                        PumpSetupContent(
                            uiState = pumpState,
                            onNavigateUp = { viewModel.goToPreviousStep() },
                            onSelectDriver = { viewModel.pumpSetupViewModel.selectDriver(it) },
                            onConnectPump = { descriptor ->
                                viewModel.pumpSetupViewModel.connectPump(
                                    descriptor = descriptor,
                                    onSuccess = { viewModel.goToNextStep() }
                                )
                            },
                            onClearError = { viewModel.pumpSetupViewModel.clearError() }
                        )
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            OutlinedButton(onClick = { viewModel.goToPreviousStep() }) {
                                Text(stringResource(R.string.setup_wizard_btn_back))
                            }
                            Button(onClick = { viewModel.goToNextStep() }) {
                                Text(
                                    if (pumpState.activePumpDescriptor != null)
                                        stringResource(R.string.setup_wizard_btn_next)
                                    else
                                        stringResource(R.string.setup_wizard_btn_skip)
                                )
                            }
                        }
                    }
                }

                SetupWizardStep.MANUAL_STEP_7_SUMMARY -> {
                    val cgmState by viewModel.glucoseSourceSetupViewModel.uiState.collectAsState()
                    val pumpState by viewModel.pumpSetupViewModel.uiState.collectAsState()

                    WizardScaffold(
                        title = stringResource(R.string.setup_wizard_step7_title),
                        showBackButton = true,
                        onBack = { viewModel.goToPreviousStep() },
                        snackbarHostState = snackbarHostState
                    ) {
                        SummaryStepContent(
                            uiState = uiState,
                            cgmDisplayName = cgmState.activeSourceDescriptor?.displayName,
                            pumpDisplayName = pumpState.activePumpDescriptor?.displayName,
                            onComplete = { viewModel.completeManualSetup() },
                            onBack = { viewModel.goToPreviousStep() }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WizardScaffold(
    title: String,
    showBackButton: Boolean,
    onBack: () -> Unit,
    snackbarHostState: SnackbarHostState,
    content: @Composable (padding: PaddingValues) -> Unit
) {
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = screenTitle(title),
                navigationIcon = {
                    if (showBackButton) {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.setup_wizard_btn_back)
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        content(innerPadding)
    }
}

@Composable
private fun ModeSelectionContent(
    onSelectDemo: () -> Unit,
    onStartManual: () -> Unit,
    onSelectImport: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .contentScrollIndicator(scrollState)
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(R.string.setup_wizard_mode_card_title),
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.setup_wizard_mode_card_desc),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        OutlinedCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.setup_wizard_mode_demo_title),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.setup_wizard_mode_demo_desc),
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = onSelectDemo,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.setup_wizard_mode_demo_btn))
                }
            }
        }

        OutlinedCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Build,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.setup_wizard_mode_manual_title),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.setup_wizard_mode_manual_desc),
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = onStartManual,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.setup_wizard_mode_manual_btn))
                }
            }
        }

        OutlinedCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.FileDownload,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.setup_wizard_mode_import_title),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.setup_wizard_mode_import_desc),
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = onSelectImport,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.setup_wizard_mode_import_btn))
                }
            }
        }
    }
}

@Composable
private fun UnitsStepContent(
    glucoseUnit: GlucoseUnit,
    carbsUnit: CarbsUnit,
    onGlucoseUnitSelected: (GlucoseUnit) -> Unit,
    onCarbsUnitSelected: (CarbsUnit) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .contentScrollIndicator(scrollState)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.setup_wizard_step1_units_desc),
                    style = MaterialTheme.typography.bodyMedium
                )

                OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Science,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.pref_glucose_unit_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(Modifier.height(8.dp))

                        Column(Modifier.selectableGroup()) {
                            GlucoseUnit.entries.forEach { unit ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .selectable(
                                            selected = (unit == glucoseUnit),
                                            onClick = { onGlucoseUnitSelected(unit) },
                                            role = Role.RadioButton
                                        )
                                        .padding(horizontal = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = (unit == glucoseUnit),
                                        onClick = null
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Text(
                                        text = glucoseUnitLabel(unit),
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                            }
                        }
                    }
                }

                OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Restaurant,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.pref_carbs_unit_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(Modifier.height(8.dp))

                        Column(Modifier.selectableGroup()) {
                            CarbsUnit.entries.forEach { unit ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .selectable(
                                            selected = (unit == carbsUnit),
                                            onClick = { onCarbsUnitSelected(unit) },
                                            role = Role.RadioButton
                                        )
                                        .padding(horizontal = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = (unit == carbsUnit),
                                        onClick = null
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Text(
                                        text = carbsUnitLabel(unit),
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            OutlinedButton(onClick = onBack) {
                Text(stringResource(R.string.setup_wizard_btn_back))
            }
            Button(onClick = onNext) {
                Text(stringResource(R.string.setup_wizard_btn_next))
            }
        }
    }
}

@Composable
private fun InsulinTypeStepContent(
    availableTypes: List<InsulinType>,
    selectedType: InsulinType?,
    onSelectType: (InsulinType) -> Unit,
    onEditType: (InsulinType) -> Unit,
    onAddNewType: () -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .contentScrollIndicator(scrollState)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.setup_wizard_step2_insulin_desc),
                    style = MaterialTheme.typography.bodyMedium
                )

                Column(
                    modifier = Modifier.selectableGroup(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    availableTypes.forEach { type ->
                        val isSelected = type.id == selectedType?.id
                        OutlinedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = isSelected,
                                    onClick = { onSelectType(type) },
                                    role = Role.RadioButton
                                ),
                            colors = if (isSelected) {
                                CardDefaults.outlinedCardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                )
                            } else CardDefaults.outlinedCardColors()
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(16.dp)
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = null
                                )
                                Spacer(Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = type.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = "DIA: ${type.dia.value} min, Peak: ${type.peak.value} min (U${(type.defaultConcentration.factor * 100).toInt()})",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(onClick = { onEditType(type) }) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }

                OutlinedButton(
                    onClick = onAddNewType,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.setup_wizard_step2_insulin_custom_btn))
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            OutlinedButton(onClick = onBack) {
                Text(stringResource(R.string.setup_wizard_btn_back))
            }
            Button(
                onClick = onNext,
                enabled = selectedType != null
            ) {
                Text(stringResource(R.string.setup_wizard_btn_next))
            }
        }
    }
}

@Composable
private fun SummaryStepContent(
    uiState: SetupWizardUiState,
    cgmDisplayName: String?,
    pumpDisplayName: String?,
    onComplete: () -> Unit,
    onBack: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .contentScrollIndicator(scrollState)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.setup_wizard_step7_card_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(Modifier.height(12.dp))

                        Text(
                            stringResource(
                                R.string.setup_wizard_summary_units_format,
                                glucoseUnitLabel(uiState.glucoseUnit),
                                carbsUnitLabel(uiState.carbsUnit)
                            )
                        )
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                        val insulin = uiState.selectedInsulinType
                        if (insulin != null) {
                            Text(
                                stringResource(
                                    R.string.setup_wizard_summary_insulin_type_format,
                                    insulin.name,
                                    insulin.dia.value.toInt(),
                                    insulin.peak.value.toInt()
                                )
                            )
                        }

                        val profile = uiState.insulinProfile
                        if (profile != null) {
                            Text(
                                stringResource(
                                    R.string.setup_wizard_summary_profile_format,
                                    profile.name
                                )
                            )
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                        Text(
                            stringResource(
                                R.string.setup_wizard_summary_cgm_format,
                                cgmDisplayName ?: stringResource(R.string.setup_wizard_summary_not_connected)
                            )
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            stringResource(
                                R.string.setup_wizard_summary_pump_format,
                                pumpDisplayName ?: stringResource(R.string.setup_wizard_summary_not_connected)
                            )
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            OutlinedButton(onClick = onBack) {
                Text(stringResource(R.string.setup_wizard_btn_back))
            }
            Button(onClick = onComplete) {
                Text(stringResource(R.string.setup_wizard_step4_complete_btn))
            }
        }
    }
}

@Preview(showBackground = true, name = "Mode Selection - Light")
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, name = "Mode Selection - Dark")
@Composable
fun SetupWizardModeSelectionPreview() {
    AppPreview {
        ModeSelectionContent(
            onSelectDemo = {},
            onStartManual = {},
            onSelectImport = {}
        )
    }
}

@Preview(showBackground = true, name = "Units Step")
@Composable
fun SetupWizardUnitsStepPreview() {
    AppPreview {
        UnitsStepContent(
            glucoseUnit = GlucoseUnit.MG_DL,
            carbsUnit = CarbsUnit.GRAMS,
            onGlucoseUnitSelected = {},
            onCarbsUnitSelected = {},
            onNext = {},
            onBack = {}
        )
    }
}

@Preview(showBackground = true, name = "Insulin Type Step")
@Composable
fun SetupWizardInsulinTypeStepPreview() {
    val sampleType = InsulinType(
        name = "NovoRapid",
        dia = Minutes.ofHours(5),
        peak = Minutes(75)
    )
    AppPreview {
        InsulinTypeStepContent(
            availableTypes = listOf(sampleType),
            selectedType = sampleType,
            onSelectType = {},
            onEditType = {},
            onAddNewType = {},
            onNext = {},
            onBack = {}
        )
    }
}