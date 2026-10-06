package de.dh.daps.ui.screens.glucosesourcesetup

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
import androidx.compose.material.icons.filled.Sensors
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
import de.dh.daps.common.model.GlucoseSource
import de.dh.daps.common.model.GlucoseSourceConnectionDescriptor
import de.dh.daps.common.model.GlucoseSourceDriver
import de.dh.daps.common.ui.UiText
import de.dh.daps.ui.R
import de.dh.daps.ui.common.composables.screenTitle
import de.dh.daps.ui.common.theme.AppTheme
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
        onConnectGlucoseSource = { descriptor ->
            viewModel.connectGlucoseSource(descriptor, onSuccess = onNavigateUp)
        },
        onClearError = viewModel::clearError
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlucoseSourceSetupContent(
    uiState: GlucoseSourceSetupUiState,
    onNavigateUp: () -> Unit,
    onSelectDriver: (GlucoseSourceDriver?) -> Unit,
    onConnectGlucoseSource: (GlucoseSourceConnectionDescriptor) -> Unit,
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
                            onConnectGlucoseSource(descriptor)
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
                GlucoseSourceDriverSelectionContent(
                    uiState = uiState,
                    onSelectDriver = onSelectDriver
                )
            }

            if (uiState.isConnecting) {
                LoadingOverlayCard(
                    message = stringResource(id = R.string.glucose_source_setup_connecting)
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
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
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

        uiState.activeSourceDescriptor?.let { activeDesc ->
            item {
                ActiveGlucoseSourceCard(activeDesc = activeDesc)
                Spacer(modifier = Modifier.height(16.dp))
            }
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
                val isActive = uiState.activeSourceDescriptor?.driverId == driver.driverId
                GlucoseSourceDriverCard(
                    driver = driver,
                    isActive = isActive,
                    onClick = { onSelectDriver(driver) }
                )
            }
        }
    }
}

@Composable
private fun ActiveGlucoseSourceCard(activeDesc: GlucoseSourceConnectionDescriptor) {
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
                imageVector = Icons.Default.Sensors,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(id = R.string.glucose_source_setup_active_source_header),
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
                    text = activeDesc.sourceId,
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
private fun GlucoseSourceDriverCard(
    driver: GlucoseSourceDriver,
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
                imageVector = Icons.Default.Sensors,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = driver.displayName.asString(),
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
                                text = stringResource(id = R.string.glucose_source_setup_active_badge),
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

private class PreviewGlucoseSourceDriver(
    override val driverId: String,
    override val displayName: UiText
) : GlucoseSourceDriver {
    @Composable
    override fun SetupScreen(
        onConnected: (GlucoseSource, GlucoseSourceConnectionDescriptor) -> Unit,
        onCancel: () -> Unit
    ) {
    }

    override suspend fun connect(descriptor: GlucoseSourceConnectionDescriptor): Result<GlucoseSource> {
        return Result.failure(UnsupportedOperationException())
    }
}

@Preview(showBackground = true, name = "Keine Quelle verbunden - Light")
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, name = "Keine Quelle verbunden - Dark")
@Composable
fun GlucoseSourceSetupNoActiveSourcePreview() {
    AppTheme {
        GlucoseSourceSetupContent(
            uiState = GlucoseSourceSetupUiState(
                availableDrivers = listOf(
                    PreviewGlucoseSourceDriver("de.dh.daps.plugin.glucose.receiver", UiText.DynamicString("xDrip+ Broadcast Receiver")),
                    PreviewGlucoseSourceDriver("de.dh.daps.plugin.simbody", UiText.DynamicString("SimBody Virtual Glucose Source"))
                ),
                activeSourceDescriptor = null
            ),
            onNavigateUp = {},
            onSelectDriver = {},
            onConnectGlucoseSource = {},
            onClearError = {}
        )
    }
}

@Preview(showBackground = true, name = "Mehrere Treiber verfügbar - Light")
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, name = "Mehrere Treiber verfügbar - Dark")
@Composable
fun GlucoseSourceSetupMultipleDriversPreview() {
    AppTheme {
        GlucoseSourceSetupContent(
            uiState = GlucoseSourceSetupUiState(
                availableDrivers = listOf(
                    PreviewGlucoseSourceDriver("de.dh.daps.plugin.simbody", UiText.DynamicString("SimBody Virtual Glucose Source")),
                    PreviewGlucoseSourceDriver("de.dh.daps.plugin.glucose.receiver", UiText.DynamicString("xDrip+ Broadcast Receiver")),
                    PreviewGlucoseSourceDriver("de.dh.daps.plugin.sample", UiText.DynamicString("Sample Glucose Source Driver mit langem Namen für automatischen Zeilenumbruch"))
                ),
                activeSourceDescriptor = GlucoseSourceConnectionDescriptor(
                    driverId = "de.dh.daps.plugin.simbody",
                    sourceId = "simbody-sensor-01",
                    displayName = "SimBody Virtual Glucose Sensor"
                )
            ),
            onNavigateUp = {},
            onSelectDriver = {},
            onConnectGlucoseSource = {},
            onClearError = {}
        )
    }
}

@Preview(showBackground = true, name = "Quelle verbunden, keine Treiber - Light")
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, name = "Quelle verbunden, keine Treiber - Dark")
@Composable
fun GlucoseSourceSetupActiveSourceNoDriversPreview() {
    AppTheme {
        GlucoseSourceSetupContent(
            uiState = GlucoseSourceSetupUiState(
                availableDrivers = emptyList(),
                activeSourceDescriptor = GlucoseSourceConnectionDescriptor(
                    driverId = "de.dh.daps.plugin.simbody",
                    sourceId = "simbody-sensor-01",
                    displayName = "SimBody Virtual Glucose Sensor"
                )
            ),
            onNavigateUp = {},
            onSelectDriver = {},
            onConnectGlucoseSource = {},
            onClearError = {}
        )
    }
}

@Preview(showBackground = true, name = "Keine Quelle, keine Treiber - Light")
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, name = "Keine Quelle, keine Treiber - Dark")
@Composable
fun GlucoseSourceSetupNoActiveSourceNoDriversPreview() {
    AppTheme {
        GlucoseSourceSetupContent(
            uiState = GlucoseSourceSetupUiState(
                availableDrivers = emptyList(),
                activeSourceDescriptor = null
            ),
            onNavigateUp = {},
            onSelectDriver = {},
            onConnectGlucoseSource = {},
            onClearError = {}
        )
    }
}