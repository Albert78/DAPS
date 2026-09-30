package de.dh.daps.ui.screens.setupwizard

import android.content.res.Configuration
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.dh.daps.ui.R
import de.dh.daps.ui.common.composables.EditableValueStepper
import de.dh.daps.ui.common.composables.InsulinAmountStepper
import de.dh.daps.ui.common.composables.contentScrollIndicator
import de.dh.daps.ui.common.composables.screenTitle
import de.dh.daps.ui.common.theme.AppPreview

@OptIn(ExperimentalMaterial3Api::class)
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

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = screenTitle(stringResource(R.string.setup_wizard_title)),
                navigationIcon = {
                    if (uiState.currentStep != SetupWizardStep.MODE_SELECTION) {
                        IconButton(onClick = { viewModel.goToPreviousStep() }) {
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (uiState.isBusy) {
                Column(
                    modifier = Modifier.fillMaxSize(),
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
            } else {
                when (uiState.currentStep) {
                    SetupWizardStep.MODE_SELECTION -> {
                        ModeSelectionContent(
                            onSelectDemo = { viewModel.selectDemoData() },
                            onStartManual = { viewModel.startManualSetup() },
                            onSelectImport = { filePickerLauncher.launch(arrayOf("*/*")) }
                        )
                    }
                    SetupWizardStep.MANUAL_STEP_1_TYPES -> {
                        TypesStepContent(
                            onNext = { viewModel.goToNextStepFromTypes() },
                            onBack = { viewModel.goToPreviousStep() }
                        )
                    }
                    SetupWizardStep.MANUAL_STEP_2_BG_TARGETS -> {
                        BgTargetsStepContent(
                            initialTargetBg = uiState.targetBgMgDl,
                            initialLowThreshold = uiState.lowThresholdMgDl,
                            onNext = { target, low -> viewModel.setBgTargets(target, low) },
                            onBack = { viewModel.goToPreviousStep() }
                        )
                    }
                    SetupWizardStep.MANUAL_STEP_3_BASAL_RATE -> {
                        BasalRateStepContent(
                            initialBasalRate = uiState.basalRateUPerHour,
                            onNext = { rate -> viewModel.setBasalRate(rate) },
                            onBack = { viewModel.goToPreviousStep() }
                        )
                    }
                    SetupWizardStep.MANUAL_STEP_4_SUMMARY -> {
                        SummaryStepContent(
                            uiState = uiState,
                            onComplete = { viewModel.completeManualSetup() },
                            onBack = { viewModel.goToPreviousStep() }
                        )
                    }
                }
            }
        }
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
private fun TypesStepContent(
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
                    .verticalScroll(scrollState)
            ) {
                Text(
                    text = stringResource(R.string.setup_wizard_step1_title),
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(Modifier.height(16.dp))
                Card {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = stringResource(R.string.setup_wizard_step1_card_text1),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.setup_wizard_step1_card_text2),
                            style = MaterialTheme.typography.bodySmall
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
            Button(onClick = onNext) {
                Text(stringResource(R.string.setup_wizard_btn_next))
            }
        }
    }
}

@Composable
private fun BgTargetsStepContent(
    initialTargetBg: Double,
    initialLowThreshold: Double,
    onNext: (Double, Double) -> Unit,
    onBack: () -> Unit
) {
    var targetBg by remember { mutableDoubleStateOf(initialTargetBg) }
    var lowThreshold by remember { mutableDoubleStateOf(initialLowThreshold) }
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
                    .verticalScroll(scrollState)
            ) {
                Text(
                    text = stringResource(R.string.setup_wizard_step2_title),
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(Modifier.height(16.dp))

                Text(
                    text = stringResource(R.string.setup_wizard_step2_target_bg_label),
                    style = MaterialTheme.typography.titleMedium
                )
                EditableValueStepper(
                    currentValue = targetBg,
                    onValueChange = { targetBg = it },
                    suffix = stringResource(R.string.setup_wizard_unit_mgdl_suffix),
                    minValue = 40.0,
                    maxValue = 400.0,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(16.dp))

                Text(
                    text = stringResource(R.string.setup_wizard_step2_low_threshold_label),
                    style = MaterialTheme.typography.titleMedium
                )
                EditableValueStepper(
                    currentValue = lowThreshold,
                    onValueChange = { lowThreshold = it },
                    suffix = stringResource(R.string.setup_wizard_unit_mgdl_suffix),
                    minValue = 40.0,
                    maxValue = 400.0,
                    modifier = Modifier.fillMaxWidth()
                )
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
            Button(onClick = { onNext(targetBg, lowThreshold) }) {
                Text(stringResource(R.string.setup_wizard_btn_next))
            }
        }
    }
}

@Composable
private fun BasalRateStepContent(
    initialBasalRate: Double,
    onNext: (Double) -> Unit,
    onBack: () -> Unit
) {
    var basalRate by remember { mutableDoubleStateOf(initialBasalRate) }
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
                    .verticalScroll(scrollState)
            ) {
                Text(
                    text = stringResource(R.string.setup_wizard_step3_title),
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(Modifier.height(16.dp))

                Text(
                    text = stringResource(R.string.setup_wizard_step3_basal_label),
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.height(8.dp))
                InsulinAmountStepper(
                    currentValue = basalRate,
                    onValueChange = { basalRate = it },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.setup_wizard_step3_basal_desc),
                    style = MaterialTheme.typography.bodySmall
                )
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
            Button(onClick = { onNext(basalRate) }) {
                Text(stringResource(R.string.setup_wizard_btn_next))
            }
        }
    }
}

@Composable
private fun SummaryStepContent(
    uiState: SetupWizardUiState,
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
                    .verticalScroll(scrollState)
            ) {
                Text(
                    text = stringResource(R.string.setup_wizard_step4_title),
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(Modifier.height(16.dp))

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
                                text = stringResource(R.string.setup_wizard_step4_card_title),
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                        Text(
                            stringResource(
                                R.string.setup_wizard_step4_target_bg_format,
                                uiState.targetBgMgDl.toInt()
                            )
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            stringResource(
                                R.string.setup_wizard_step4_low_threshold_format,
                                uiState.lowThresholdMgDl.toInt()
                            )
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            stringResource(
                                R.string.setup_wizard_step4_basal_rate_format,
                                "%.2f".format(uiState.basalRateUPerHour)
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

@Preview(showBackground = true, name = "BG Targets Step")
@Composable
fun SetupWizardBgTargetsStepPreview() {
    AppPreview {
        BgTargetsStepContent(
            initialTargetBg = 100.0,
            initialLowThreshold = 70.0,
            onNext = { _, _ -> },
            onBack = {}
        )
    }
}

@Preview(showBackground = true, name = "Summary Step")
@Composable
fun SetupWizardSummaryStepPreview() {
    AppPreview {
        SummaryStepContent(
            uiState = SetupWizardUiState(
                targetBgMgDl = 100.0,
                lowThresholdMgDl = 70.0,
                basalRateUPerHour = 1.0
            ),
            onComplete = {},
            onBack = {}
        )
    }
}