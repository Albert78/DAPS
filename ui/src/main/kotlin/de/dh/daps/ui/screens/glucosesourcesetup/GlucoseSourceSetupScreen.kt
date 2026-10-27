package de.dh.daps.ui.screens.glucosesourcesetup

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.dh.daps.common.device.driver.GlucoseSourceDriver
import de.dh.daps.common.device.state.DeviceLifecycleState
import de.dh.daps.common.domain.model.cgm.GlucoseSource
import de.dh.daps.common.ui.UiText
import de.dh.daps.ui.R
import de.dh.daps.ui.common.composables.FramedCard
import de.dh.daps.ui.common.composables.NormalTextButton
import de.dh.daps.ui.common.composables.PrimaryButton
import de.dh.daps.ui.common.composables.ScopedDriverSetupContainer
import de.dh.daps.ui.common.composables.contentScrollIndicator
import de.dh.daps.ui.common.composables.screenTitle
import de.dh.daps.ui.common.theme.AppPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import de.dh.daps.common.R as CommonR

@Composable
fun GlucoseSourceSetupScreen(
    viewModel: GlucoseSourceSetupViewModel,
    onNavigateUp: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    GlucoseSourceSetupContent(
        uiState = uiState,
        onNavigateUp = onNavigateUp,
        onSelectDriver = viewModel::selectDriver,
        onClearError = viewModel::clearError
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlucoseSourceSetupContent(
    uiState: GlucoseSourceSetupUiState,
    onNavigateUp: () -> Unit,
    onSelectDriver: (GlucoseSourceDriver?) -> Unit,
    onClearError: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }

    val errorMessage = uiState.errorMessage
    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            snackbarHostState.showSnackbar(errorMessage)
            onClearError()
        }
    }

    val selectedDriver = uiState.selectedDriver
    val titleText = selectedDriver?.displayName?.asString() ?: stringResource(id = R.string.glucose_source_setup_screen_title)

    val selectedDriverState = selectedDriver?.lifecycleState?.collectAsStateWithLifecycle()
    val driverLifecycleState = selectedDriverState?.value

    val isNoDeviceState = selectedDriver != null && (driverLifecycleState is DeviceLifecycleState.NoDevice)

    val handleBack = {
        if (isNoDeviceState) {
            onSelectDriver(null)
        } else {
            onNavigateUp()
        }
    }

    BackHandler(enabled = true) {
        handleBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = screenTitle(titleText),
                navigationIcon = {
                    if (selectedDriver == null || isNoDeviceState) {
                        IconButton(onClick = handleBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(id = CommonR.string.cd_navigate_up)
                            )
                        }
                    } else {
                        IconButton(onClick = handleBack) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = stringResource(id = CommonR.string.cd_close)
                            )
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (selectedDriver != null) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    ScopedDriverSetupContainer(driverId = selectedDriver.driverId) {
                        selectedDriver.SetupScreen(
                            onDriverReset = {
                                onSelectDriver(null)
                            }
                        )
                    }
                }
            } else {
                GlucoseSourceDriverSelectionContent(
                    uiState = uiState,
                    onSelectDriver = onSelectDriver
                )
            }
        }
    }
}

@Composable
private fun GlucoseSourceDriverSelectionContent(
    uiState: GlucoseSourceSetupUiState,
    onSelectDriver: (GlucoseSourceDriver) -> Unit
) {
    val listState = rememberLazyListState()
    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .contentScrollIndicator(listState)
            .padding(horizontal = 16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(id = R.string.glucose_source_setup_select_driver_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        item {
            Text(
                text = stringResource(id = R.string.glucose_source_setup_available_drivers_header),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (uiState.availableDrivers.isEmpty()) {
            item {
                FramedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = stringResource(id = R.string.glucose_source_setup_no_drivers_found),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = stringResource(id = R.string.glucose_source_setup_no_drivers_description),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        } else {
            items(uiState.availableDrivers, key = { it.driverId }) { driver ->
                DriverCard(
                    driver = driver,
                    onClick = { onSelectDriver(driver) }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = stringResource(id = R.string.glucose_source_setup_more_drivers_in_development),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun DriverCard(
    driver: GlucoseSourceDriver,
    onClick: () -> Unit
) {
    FramedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Sensors,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = driver.displayName.asString(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private class PreviewGlucoseDriver(
    override val driverId: String,
    override val displayName: UiText,
    initialState: DeviceLifecycleState = DeviceLifecycleState.NoDevice,
    private val setupScreenContent: @Composable () -> Unit = {}
) : GlucoseSourceDriver {
    override val lifecycleState: StateFlow<DeviceLifecycleState> =
        MutableStateFlow(initialState)

    override val activeGlucoseSource: StateFlow<GlucoseSource?> =
        MutableStateFlow(null)

    override suspend fun initState() {}

    @Composable
    override fun SetupScreen(
        onDriverReset: () -> Unit
    ) {
        setupScreenContent()
    }
}

@Composable
internal fun GlucoseSourceSamplePreviewContent() {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        FramedCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Dexcom G6 CGM",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Verbindung mit dem Sensor herstellen",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Text(
                    text = "Gib die Transmitterseriennummer deines Dexcom G6 Transmitters ein, um die Bluetooth-Verkopplung zu starten.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PrimaryButton(
                onClick = {},
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Sensor verbinden")
            }

            NormalTextButton(
                onClick = {},
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Einrichtung abbrechen")
            }
        }
    }
}

@Preview(showBackground = true, name = "Treiber-Auswahlliste - Light")
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, name = "Treiber-Auswahlliste - Dark")
@Composable
fun GlucoseSourceSetupDriverSelectionPreview() {
    AppPreview {
        GlucoseSourceSetupContent(
            uiState = GlucoseSourceSetupUiState(
                availableDrivers = listOf(
                    PreviewGlucoseDriver("de.dh.daps.plugin.glucose.sample", UiText.DynamicString("Sample CGM Driver")),
                    PreviewGlucoseDriver("de.dh.daps.plugin.simbody.cgm", UiText.DynamicString("SimBody Virtual CGM Driver"))
                )
            ),
            onNavigateUp = {},
            onSelectDriver = {},
            onClearError = {}
        )
    }
}

@Preview(showBackground = true, name = "Keine Treiber verfügbar - Light")
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, name = "Keine Treiber verfügbar - Dark")
@Composable
fun GlucoseSourceSetupNoDriversPreview() {
    AppPreview {
        GlucoseSourceSetupContent(
            uiState = GlucoseSourceSetupUiState(
                availableDrivers = emptyList()
            ),
            onNavigateUp = {},
            onSelectDriver = {},
            onClearError = {}
        )
    }
}

@Preview(showBackground = true, name = "Treiber-Einrichtung (Dexcom G6) - Light")
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, name = "Treiber-Einrichtung (Dexcom G6) - Dark")
@Composable
fun GlucoseSourceSetupDriverSetupPreview() {
    val sampleDriver = PreviewGlucoseDriver(
        driverId = "de.dh.daps.plugin.glucose.dexcom",
        displayName = UiText.DynamicString("Dexcom G6 CGM"),
        setupScreenContent = { GlucoseSourceSamplePreviewContent() }
    )
    AppPreview {
        GlucoseSourceSetupContent(
            uiState = GlucoseSourceSetupUiState(
                availableDrivers = listOf(sampleDriver),
                selectedDriver = sampleDriver
            ),
            onNavigateUp = {},
            onSelectDriver = {},
            onClearError = {}
        )
    }
}