package de.dh.daps.ui.screens.systemcontrol

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import de.dh.daps.common.model.InsulinAmount
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.dh.daps.common.ui.UiText
import de.dh.daps.ui.R
import de.dh.daps.ui.common.composables.contentScrollIndicator
import de.dh.daps.ui.common.composables.screenTitle
import de.dh.daps.ui.common.theme.AppPreview
import de.dh.daps.common.R as CommonR

const val SYSTEM_CONTROL_TAB_OVERVIEW = 0
const val SYSTEM_CONTROL_TAB_GLUCOSE_SOURCE = 1
const val SYSTEM_CONTROL_TAB_PUMP = 2

@Composable
fun SystemControlScreen(
    onNavigateUp: () -> Unit,
    onNavigateToCoreDecisions: () -> Unit,
    onNavigateToPumpSetup: () -> Unit = {},
    onNavigateToGlucoseSourceSetup: () -> Unit = {},
    viewModel: SystemControlViewModel,
    initialTab: Int = SYSTEM_CONTROL_TAB_OVERVIEW
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    SystemControlContent(
        uiState = uiState,
        initialTab = initialTab,
        onNavigateUp = onNavigateUp,
        onNavigateToCoreDecisions = onNavigateToCoreDecisions,
        onNavigateToPumpSetup = onNavigateToPumpSetup,
        onNavigateToGlucoseSourceSetup = onNavigateToGlucoseSourceSetup,
        onStopGlucoseSource = viewModel::stopActiveGlucoseSource,
        onDisconnectForMaintenance = viewModel::disconnectPumpForMaintenance,
        onCancelPumpJob = viewModel::cancelPumpJob,
        onRefreshPumpStatus = viewModel::refreshPumpStatus
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SystemControlContent(
    uiState: SystemControlUiState,
    initialTab: Int = SYSTEM_CONTROL_TAB_OVERVIEW,
    onNavigateUp: () -> Unit,
    onNavigateToCoreDecisions: () -> Unit,
    onNavigateToPumpSetup: () -> Unit = {},
    onNavigateToGlucoseSourceSetup: () -> Unit = {},
    onStopGlucoseSource: () -> Unit = {},
    onDisconnectForMaintenance: () -> Unit = {},
    onCancelPumpJob: (String) -> Unit = {},
    onRefreshPumpStatus: () -> Unit = {}
) {
    var selectedTabIndex by rememberSaveable { mutableIntStateOf(initialTab) }

    val tabs = listOf(
        stringResource(id = R.string.system_control_tab_overview),
        stringResource(id = R.string.system_control_tab_glucose_source),
        stringResource(id = R.string.system_control_tab_pump)
    )

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = screenTitle(stringResource(id = R.string.system_control_screen_title)),
                    navigationIcon = {
                        IconButton(onClick = onNavigateUp) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(id = CommonR.string.cd_navigate_up)
                            )
                        }
                    }
                )
                PrimaryTabRow(
                    selectedTabIndex = selectedTabIndex,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary,
                    divider = {}
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            text = {
                                Text(
                                    text = title,
                                    maxLines = 1,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        )
                    }
                }
                HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    ) { innerPadding ->
        val listState = rememberLazyListState()
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .contentScrollIndicator(listState)
                .padding(start = 16.dp, end = 16.dp, top = 16.dp)
        ) {
            item {
                when (selectedTabIndex) {
                    SYSTEM_CONTROL_TAB_OVERVIEW -> OverviewTabContent(
                        uiState = uiState.overviewUiState,
                        onRefreshPumpStatus = onRefreshPumpStatus,
                        onNavigateToCoreDecisions = onNavigateToCoreDecisions
                    )
                    SYSTEM_CONTROL_TAB_GLUCOSE_SOURCE -> GlucoseSourceTabContent(
                        uiState = uiState.glucoseSourceTabUiState,
                        onChangeGlucoseSource = onNavigateToGlucoseSourceSetup,
                        onStopSensor = onStopGlucoseSource
                    )
                    SYSTEM_CONTROL_TAB_PUMP -> PumpTabContent(
                        uiState = uiState.pumpTabUiState,
                        onChangePumpDriver = onNavigateToPumpSetup,
                        onRefreshPumpStatus = onRefreshPumpStatus,
                        onDisconnectForMaintenance = onDisconnectForMaintenance,
                        onCancelPumpJob = onCancelPumpJob
                    )
                }
            }
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Preview(showBackground = true, name = "Overview Tab")
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, name = "Overview Tab - Dark Mode")
@Composable
fun SystemControlOverviewPreview() {
    AppPreview {
        SystemControlContent(
            uiState = previewUiState(),
            initialTab = SYSTEM_CONTROL_TAB_OVERVIEW,
            onNavigateUp = {},
            onNavigateToCoreDecisions = {},
            onStopGlucoseSource = {},
            onDisconnectForMaintenance = {},
            onCancelPumpJob = {},
            onRefreshPumpStatus = {}
        )
    }
}

@Preview(showBackground = true, name = "Glucose Source Tab")
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, name = "Glucose Source Tab - Dark Mode")
@Composable
fun SystemControlGlucosePreview() {
    AppPreview {
        SystemControlContent(
            uiState = previewUiState(),
            initialTab = SYSTEM_CONTROL_TAB_GLUCOSE_SOURCE,
            onNavigateUp = {},
            onNavigateToCoreDecisions = {},
            onStopGlucoseSource = {},
            onDisconnectForMaintenance = {},
            onCancelPumpJob = {},
            onRefreshPumpStatus = {}
        )
    }
}

@Preview(showBackground = true, name = "Pump Tab")
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, name = "Pump Tab - Dark Mode")
@Composable
fun SystemControlPumpPreview() {
    AppPreview {
        SystemControlContent(
            uiState = previewUiState(),
            initialTab = SYSTEM_CONTROL_TAB_PUMP,
            onNavigateUp = {},
            onNavigateToCoreDecisions = {},
            onStopGlucoseSource = {},
            onDisconnectForMaintenance = {},
            onCancelPumpJob = {},
            onRefreshPumpStatus = {}
        )
    }
}

private fun previewUiState() = SystemControlUiState(
    overviewUiState = OverviewTabUiState(),
    glucoseSourceTabUiState = GlucoseSourceTabUiState(
        glucoseSourceName = UiText.DynamicString("Dexcom G6"),
        sensorTypeName = "G6-Sensor",
        readingsIntervalText = "5 Minuten",
        lastBgValueText = "124 mg/dl",
        lastReadingTimeText = "12:32:40",
        lastReadingRelativeTimeText = "vor 2 Min.",
        hasNextPrediction = true,
        nextReadingTimeText = "12:37:40",
        nextReadingRelativeTimeText = "in 3 Min.",
        glucoseSourcePluginSection = {
            GlucoseSourcePluginExampleCard(
                sensorCode = "8132",
                transmitterSerialNumber = "8G1234",
                estimatedExpirationDateText = "24.10.2024, 18:30",
                onStopSensor = {}
            )
        }
    ),
    pumpTabUiState = PumpTabUiState(
        pumpModel = "DANA-i",
        manufacturer = "SOOIL",
        serialNumber = "12345678",
        pumpConnected = true,
        batteryPercent = 85,
        reservoirRemaining = InsulinAmount(140.0),
        lastConnectionTimeText = "12:34:56",
        lastConnectionRelativeTimeText = "vor 1 Min.",
        pendingJobs = listOf(
            PumpJobItem(
                id = "job_1",
                title = UiText.StringResource(R.string.system_control_pump_job_type_bolus, "1,50")
            )
        )
    )
)