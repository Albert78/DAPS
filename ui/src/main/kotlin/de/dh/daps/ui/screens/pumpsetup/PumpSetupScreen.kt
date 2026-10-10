package de.dh.daps.ui.screens.pumpsetup

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Router
import androidx.compose.material3.Badge
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.dh.daps.common.model.DeviceLifecycleState
import de.dh.daps.common.model.InsulinPump
import de.dh.daps.common.model.InsulinPumpDriver
import de.dh.daps.common.ui.UiText
import de.dh.daps.ui.R
import de.dh.daps.ui.common.composables.FramedCard
import de.dh.daps.ui.common.composables.NormalTextButton
import de.dh.daps.ui.common.composables.PrimaryButton
import de.dh.daps.ui.common.composables.ScopedDriverSetupContainer
import de.dh.daps.ui.common.composables.screenTitle
import de.dh.daps.ui.common.theme.AppTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import de.dh.daps.common.R as CommonR

@Composable
fun PumpSetupScreen(
    viewModel: PumpSetupViewModel,
    onNavigateUp: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    PumpSetupContent(
        uiState = uiState,
        onNavigateUp = onNavigateUp,
        onSelectDriver = viewModel::selectDriver,
        onClearError = viewModel::clearError
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PumpSetupContent(
    uiState: PumpSetupUiState,
    onNavigateUp: () -> Unit,
    onSelectDriver: (InsulinPumpDriver?) -> Unit,
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
    val titleText = selectedDriver?.displayName?.asString() ?: stringResource(id = R.string.pump_setup_screen_title)

    BackHandler(enabled = selectedDriver != null) {
        // Intercept back gesture so driver setup workflow cannot be exited via back gesture
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = screenTitle(titleText),
                navigationIcon = {
                    if (selectedDriver == null) {
                        IconButton(onClick = onNavigateUp) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(id = CommonR.string.cd_navigate_up)
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
                            onNavigateUp = {
                                if (uiState.availableDrivers.size > 1) {
                                    onSelectDriver(null)
                                } else {
                                    onNavigateUp()
                                }
                            }
                        )
                    }
                }
            } else {
                PumpDriverSelectionContent(
                    uiState = uiState,
                    onSelectDriver = onSelectDriver
                )
            }
        }
    }
}

@Composable
private fun PumpDriverSelectionContent(
    uiState: PumpSetupUiState,
    onSelectDriver: (InsulinPumpDriver) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(id = R.string.pump_setup_select_driver_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        val activeDriver = uiState.availableDrivers.find { it.driverId == uiState.activeDriverId }
        activeDriver?.let { driver ->
            item {
                ActivePumpCard(driver = driver)
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        item {
            Text(
                text = stringResource(id = R.string.pump_setup_available_drivers_header),
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
                                text = stringResource(id = R.string.pump_setup_no_drivers_found),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = stringResource(id = R.string.pump_setup_no_drivers_description),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        } else {
            items(uiState.availableDrivers, key = { it.driverId }) { driver ->
                val isActive = uiState.activeDriverId == driver.driverId
                DriverCard(
                    driver = driver,
                    isActive = isActive,
                    onClick = { onSelectDriver(driver) }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = stringResource(id = R.string.pump_setup_more_drivers_in_development),
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
private fun ActivePumpCard(driver: InsulinPumpDriver) {
    val lifecycleState by driver.lifecycleState.collectAsStateWithLifecycle()
    val (displayName, deviceId) = when (val state = lifecycleState) {
        is DeviceLifecycleState.DeviceBound -> Pair(state.displayName, state.deviceId)
        else -> Pair(driver.displayName, null)
    }

    FramedCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Router,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(id = R.string.pump_setup_active_pump_header),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = displayName.asString(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (!deviceId.isNullOrEmpty()) {
                    Text(
                        text = deviceId,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun DriverCard(
    driver: InsulinPumpDriver,
    isActive: Boolean,
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
                imageVector = Icons.Default.Router,
                contentDescription = null,
                tint = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
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
            if (isActive) {
                Badge(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    Text(
                        text = stringResource(id = R.string.pump_setup_active_badge),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private class PreviewPumpDriver(
    override val driverId: String,
    override val displayName: UiText,
    initialState: DeviceLifecycleState = DeviceLifecycleState.NoDevice,
    private val setupScreenContent: @Composable () -> Unit = {}
) : InsulinPumpDriver {
    override val lifecycleState: StateFlow<DeviceLifecycleState> =
        MutableStateFlow(initialState)

    override suspend fun initState() {}

    @Composable
    override fun SetupScreen(
        onNavigateUp: () -> Unit
    ) {
        setupScreenContent()
    }

    override suspend fun attach(): Result<InsulinPump> {
        return Result.failure(UnsupportedOperationException())
    }
}

@Composable
internal fun OmnipodDashStep1PreviewContent() {
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
                    text = "Insulet Omnipod Dash",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Schritt 1 von 3",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                LinearProgressIndicator(
                    progress = { 1f / 3f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                )
            }
        }

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
                    text = "Schritt 1: Pod befüllen & primen/entlüften",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Befülle den Pod mit mindestens 85 Einheiten Insulin, bis du 2 Pieptöne hörst. Platziere den Pod neben deinem DAPS-Gerät und tippe auf 'Pod primen'.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PrimaryButton(
                    onClick = {},
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Pod primen")
                }
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

@Preview(showBackground = true, name = "Keine Pumpe verbunden - Light")
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, name = "Keine Pumpe verbunden - Dark")
@Composable
fun PumpSetupNoActivePumpPreview() {
    AppTheme {
        PumpSetupContent(
            uiState = PumpSetupUiState(
                availableDrivers = listOf(
                    PreviewPumpDriver("de.dh.daps.plugin.sample", UiText.DynamicString("Sample Pump Driver")),
                    PreviewPumpDriver("de.dh.daps.plugin.simbody", UiText.DynamicString("SimBody Virtual Pump Driver"))
                ),
                activeDriverId = null
            ),
            onNavigateUp = {},
            onSelectDriver = {},
            onClearError = {}
        )
    }
}

@Preview(showBackground = true, name = "Mehrere Treiber verfügbar - Light")
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, name = "Mehrere Treiber verfügbar - Dark")
@Composable
fun PumpSetupMultipleDriversPreview() {
    val simBodyDriver = PreviewPumpDriver(
        "de.dh.daps.plugin.simbody",
        UiText.DynamicString("SimBody Virtual Pump Driver"),
        DeviceLifecycleState.DeviceBound("simbody-virtual-pump-01", UiText.DynamicString("SimBody Virtual Insulin Pump"))
    )
    AppTheme {
        PumpSetupContent(
            uiState = PumpSetupUiState(
                availableDrivers = listOf(
                    simBodyDriver,
                    PreviewPumpDriver("de.dh.daps.plugin.sample", UiText.DynamicString("Sample Pump Driver")),
                    PreviewPumpDriver("de.dh.daps.plugin.danai", UiText.DynamicString("Sooil Dana-i Driver mit langem Namen für automatischen Zeilenumbruch"))
                ),
                activeDriverId = "de.dh.daps.plugin.simbody"
            ),
            onNavigateUp = {},
            onSelectDriver = {},
            onClearError = {}
        )
    }
}

@Preview(showBackground = true, name = "Pumpe verbunden, keine Treiber - Light")
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, name = "Pumpe verbunden, keine Treiber - Dark")
@Composable
fun PumpSetupActivePumpNoDriversPreview() {
    AppTheme {
        PumpSetupContent(
            uiState = PumpSetupUiState(
                availableDrivers = emptyList(),
                activeDriverId = "de.dh.daps.plugin.simbody"
            ),
            onNavigateUp = {},
            onSelectDriver = {},
            onClearError = {}
        )
    }
}

@Preview(showBackground = true, name = "Keine Pumpe, keine Treiber - Light")
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, name = "Keine Pumpe, keine Treiber - Dark")
@Composable
fun PumpSetupNoActivePumpNoDriversPreview() {
    AppTheme {
        PumpSetupContent(
            uiState = PumpSetupUiState(
                availableDrivers = emptyList(),
                activeDriverId = null
            ),
            onNavigateUp = {},
            onSelectDriver = {},
            onClearError = {}
        )
    }
}

@Preview(showBackground = true, name = "Treiber-Einrichtung Schritt 1 (Omnipod Dash) - Light")
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, name = "Treiber-Einrichtung Schritt 1 (Omnipod Dash) - Dark")
@Composable
fun PumpSetupDriverSetupOmnipodDashPreview() {
    val omnipodDriver = PreviewPumpDriver(
        driverId = "de.dh.pump.omnipod.dash",
        displayName = UiText.DynamicString("Insulet Omnipod Dash"),
        setupScreenContent = { OmnipodDashStep1PreviewContent() }
    )
    AppTheme {
        PumpSetupContent(
            uiState = PumpSetupUiState(
                availableDrivers = listOf(omnipodDriver),
                selectedDriver = omnipodDriver
            ),
            onNavigateUp = {},
            onSelectDriver = {},
            onClearError = {}
        )
    }
}