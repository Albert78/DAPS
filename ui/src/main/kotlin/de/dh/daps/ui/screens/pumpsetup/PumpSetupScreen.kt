package de.dh.daps.ui.screens.pumpsetup

import android.content.res.Configuration
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Router
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import de.dh.daps.ui.common.composables.LoadingOverlayCard
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.dh.daps.common.model.InsulinPump
import de.dh.daps.common.model.InsulinPumpDriver
import de.dh.daps.common.model.PumpConnectionDescriptor
import de.dh.daps.common.ui.UiText
import de.dh.daps.ui.R
import de.dh.daps.ui.common.composables.screenTitle
import de.dh.daps.ui.common.theme.AppTheme
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
        onConnectPump = { descriptor ->
            viewModel.connectPump(descriptor, onSuccess = onNavigateUp)
        },
        onClearError = viewModel::clearError
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PumpSetupContent(
    uiState: PumpSetupUiState,
    onNavigateUp: () -> Unit,
    onSelectDriver: (InsulinPumpDriver?) -> Unit,
    onConnectPump: (PumpConnectionDescriptor) -> Unit,
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
    val titleText = selectedDriver?.driverDisplayName?.asString() ?: stringResource(id = R.string.pump_setup_screen_title)

    Scaffold(
        topBar = {
            TopAppBar(
                title = screenTitle(titleText),
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (selectedDriver != null && uiState.availableDrivers.size > 1) {
                                onSelectDriver(null)
                            } else {
                                onNavigateUp()
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(id = CommonR.string.cd_navigate_up)
                        )
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
                    selectedDriver.SetupScreen(
                        onConnected = { _, descriptor ->
                            onConnectPump(descriptor)
                        },
                        onCancel = {
                            if (uiState.availableDrivers.size > 1) {
                                onSelectDriver(null)
                            } else {
                                onNavigateUp()
                            }
                        }
                    )
                }
            } else {
                PumpDriverSelectionContent(
                    uiState = uiState,
                    onSelectDriver = onSelectDriver
                )
            }

            if (uiState.isConnecting) {
                LoadingOverlayCard(
                    message = stringResource(id = R.string.pump_setup_connecting)
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

        uiState.activePumpDescriptor?.let { activeDesc ->
            item {
                ActivePumpCard(activeDesc = activeDesc)
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
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
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
                val isActive = uiState.activePumpDescriptor?.driverId == driver.driverId
                DriverCard(
                    driver = driver,
                    isActive = isActive,
                    onClick = { onSelectDriver(driver) }
                )
            }
        }
    }
}

@Composable
private fun ActivePumpCard(activeDesc: PumpConnectionDescriptor) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        )
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
                    text = activeDesc.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = activeDesc.deviceId,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
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
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Router,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = driver.driverDisplayName.asString(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (isActive) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Badge(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ) {
                            Text(
                                text = stringResource(id = R.string.pump_setup_active_badge),
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Text(
                    text = driver.driverId,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
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

private class PreviewPumpDriver(
    override val driverId: String,
    override val driverDisplayName: UiText
) : InsulinPumpDriver {
    @Composable
    override fun SetupScreen(
        onConnected: (InsulinPump, PumpConnectionDescriptor) -> Unit,
        onCancel: () -> Unit
    ) {
    }

    override suspend fun connect(descriptor: PumpConnectionDescriptor): Result<InsulinPump> {
        return Result.failure(UnsupportedOperationException())
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
                activePumpDescriptor = null
            ),
            onNavigateUp = {},
            onSelectDriver = {},
            onConnectPump = {},
            onClearError = {}
        )
    }
}

@Preview(showBackground = true, name = "Mehrere Treiber verfügbar - Light")
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, name = "Mehrere Treiber verfügbar - Dark")
@Composable
fun PumpSetupMultipleDriversPreview() {
    AppTheme {
        PumpSetupContent(
            uiState = PumpSetupUiState(
                availableDrivers = listOf(
                    PreviewPumpDriver("de.dh.daps.plugin.simbody", UiText.DynamicString("SimBody Virtual Pump Driver")),
                    PreviewPumpDriver("de.dh.daps.plugin.sample", UiText.DynamicString("Sample Pump Driver")),
                    PreviewPumpDriver("de.dh.daps.plugin.ypso", UiText.DynamicString("Ypsomed YpsoPump Driver mit langem Namen für automatischen Zeilenumbruch"))
                ),
                activePumpDescriptor = PumpConnectionDescriptor(
                    driverId = "de.dh.daps.plugin.simbody",
                    deviceId = "simbody-virtual-pump-01",
                    displayName = "SimBody Virtual Insulin Pump"
                )
            ),
            onNavigateUp = {},
            onSelectDriver = {},
            onConnectPump = {},
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
                activePumpDescriptor = PumpConnectionDescriptor(
                    driverId = "de.dh.daps.plugin.simbody",
                    deviceId = "simbody-virtual-pump-01",
                    displayName = "SimBody Virtual Insulin Pump"
                )
            ),
            onNavigateUp = {},
            onSelectDriver = {},
            onConnectPump = {},
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
                activePumpDescriptor = null
            ),
            onNavigateUp = {},
            onSelectDriver = {},
            onConnectPump = {},
            onClearError = {}
        )
    }
}