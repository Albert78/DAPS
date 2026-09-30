package de.dh.daps.ui.screens.setupwizard

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
import androidx.compose.ui.unit.dp
import de.dh.daps.ui.common.composables.EditableValueStepper
import de.dh.daps.ui.common.composables.InsulinAmountStepper
import de.dh.daps.ui.common.composables.screenTitle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupWizardScreen(
    viewModel: SetupWizardViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            viewModel.importBackup(uri)
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        val error = uiState.errorMessage
        if (error != null) {
            snackbarHostState.showSnackbar(error)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = screenTitle("Willkommen bei DAPS"),
                navigationIcon = {
                    if (uiState.currentStep != SetupWizardStep.MODE_SELECTION) {
                        IconButton(onClick = { viewModel.goToPreviousStep() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Zurück"
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
                        text = "System wird eingerichtet...",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            } else {
                when (uiState.currentStep) {
                    SetupWizardStep.MODE_SELECTION -> {
                        ModeSelectionContent(
                            onSelectDemo = { viewModel.selectDemoData() },
                            onStartManual = { viewModel.startManualSetup() },
                            onSelectImport = { filePickerLauncher.launch("*/*") }
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
            .padding(16.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Ersteinrichtung erforderlich",
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Die Anwendungsdatenbank ist zurzeit leer. Bitte wähle aus, wie du deine Stammdaten initialisieren möchtest:",
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
                    Text("Demo-Daten verwenden", style = MaterialTheme.typography.titleMedium)
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "Befüllt das System mit vordefinierten Standard-Profilen, Insulintypen und Alarmeinstellungen, um direkt starten zu können.",
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = onSelectDemo,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Demo-Daten laden")
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
                    Text("Manuelle Einrichtung", style = MaterialTheme.typography.titleMedium)
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "Gehe Schritt für Schritt durch die Konfiguration deiner Ziel-Blutzuckerwerte und Basalrate.",
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = onStartManual,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Manuell einrichten")
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
                    Text("Daten importieren", style = MaterialTheme.typography.titleMedium)
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "Stelle eine früher gesicherte Backup-Datei (JSON/ZIP) wieder her.",
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = onSelectImport,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Backup-Datei auswählen")
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
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text("Schritt 1: Stammdaten-Typen", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(16.dp))
            Card {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Es werden Standard-Insulintypen (z.B. Rapid-acting, Fiasp) sowie Mahlzeitentypen zur Initialisierung angelegt.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Du kannst diese Einstellungen später jederzeit in den Stammdaten anpassen.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            OutlinedButton(onClick = onBack) {
                Text("Zurück")
            }
            Button(onClick = onNext) {
                Text("Weiter")
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text("Schritt 2: Blutzucker-Zielwerte", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(16.dp))

            Text("Ziel-Blutzucker (mg/dL)", style = MaterialTheme.typography.titleMedium)
            EditableValueStepper(
                currentValue = targetBg,
                onValueChange = { targetBg = it },
                suffix = " mg/dL",
                minValue = 40.0,
                maxValue = 400.0,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))

            Text("Hypo-Schwelle (mg/dL)", style = MaterialTheme.typography.titleMedium)
            EditableValueStepper(
                currentValue = lowThreshold,
                onValueChange = { lowThreshold = it },
                suffix = " mg/dL",
                minValue = 40.0,
                maxValue = 400.0,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            OutlinedButton(onClick = onBack) {
                Text("Zurück")
            }
            Button(onClick = { onNext(targetBg, lowThreshold) }) {
                Text("Weiter")
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text("Schritt 3: Basalrate", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(16.dp))

            Text("Standard Basalrate (U/h)", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            InsulinAmountStepper(
                currentValue = basalRate,
                onValueChange = { basalRate = it },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Gilt als ganztägiges Standardprofil. Detaillierte Zeitabschnitte können später unter 'Insulinprofile' konfiguriert werden.",
                style = MaterialTheme.typography.bodySmall
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            OutlinedButton(onClick = onBack) {
                Text("Zurück")
            }
            Button(onClick = { onNext(basalRate) }) {
                Text("Weiter")
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
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text("Schritt 4: Zusammenfassung", style = MaterialTheme.typography.titleLarge)
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
                        Text("Konfigurierte Parameter", style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(Modifier.height(12.dp))
                    Text("• Ziel-BZ: ${uiState.targetBgMgDl.toInt()} mg/dL")
                    Spacer(Modifier.height(4.dp))
                    Text("• Hypo-Schwelle: ${uiState.lowThresholdMgDl.toInt()} mg/dL")
                    Spacer(Modifier.height(4.dp))
                    Text("• Basalrate: ${"%.2f".format(uiState.basalRateUPerHour)} U/h")
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            OutlinedButton(onClick = onBack) {
                Text("Zurück")
            }
            Button(onClick = onComplete) {
                Text("Einrichtung abschließen")
            }
        }
    }
}