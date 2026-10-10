package de.dh.daps.ui.screens.glucosesourcesetup

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.dh.daps.common.model.DeviceLifecycleState
import de.dh.daps.common.model.GlucoseSource
import de.dh.daps.common.model.GlucoseSourceDriver
import de.dh.daps.common.ui.UiText
import de.dh.daps.ui.R
import de.dh.daps.ui.common.composables.FramedCard
import de.dh.daps.ui.common.composables.LoadingOverlayCard
import de.dh.daps.ui.common.composables.ScopedDriverSetupContainer
import de.dh.daps.ui.common.composables.screenTitle
import de.dh.daps.ui.common.theme.AppTheme
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
        onBindGlucoseSource = { _, source ->
            viewModel.onGlucoseSourceBound(source, onSuccess = onNavigateUp)
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
    onBindGlucoseSource: (String, GlucoseSource) -> Unit,
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
                            onBound = { source ->
                                onBindGlucoseSource(selectedDriver.driverId, source)
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
                }
            } else {
                GlucoseSourceDriverSelectionContent(
                    uiState = uiState,
                    onSelectDriver = onSelectDriver
                )
            }

            if (uiState.isBinding) {
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

        val activeDriver = uiState.availableDrivers.find { it.driverId == uiState.activeDriverId }
        activeDriver?.let { driver ->
            item {
                ActiveGlucoseSourceCard(driver = driver)
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
private fun ActiveGlucoseSourceCard(driver: GlucoseSourceDriver) {
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
    driver: GlucoseSourceDriver,
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
                imageVector = Icons.Default.Sensors,
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
                        text = stringResource(id = R.string.glucose_source_setup_active_badge),
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

private class PreviewGlucoseDriver(
    override val driverId: String,
    override val displayName: UiText,
    initialState: DeviceLifecycleState = DeviceLifecycleState.NoDevice,
) : GlucoseSourceDriver {
    override val lifecycleState: StateFlow<DeviceLifecycleState> =
        MutableStateFlow(initialState)

    @Composable
    override fun SetupScreen(
        onBound: (GlucoseSource) -> Unit,
        onCancel: () -> Unit
    ) {
    }

    override suspend fun attach(): Result<GlucoseSource> {
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
                    PreviewGlucoseDriver("de.dh.daps.plugin.glucose.sample", UiText.DynamicString("Sample CGM Driver")),
                    PreviewGlucoseDriver("de.dh.daps.plugin.simbody.cgm", UiText.DynamicString("SimBody Virtual CGM Driver"))
                ),
                activeDriverId = null
            ),
            onNavigateUp = {},
            onSelectDriver = {},
            onBindGlucoseSource = { _, _ -> },
            onClearError = {}
        )
    }
}

@Preview(showBackground = true, name = "Mehrere Treiber verfügbar - Light")
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, name = "Mehrere Treiber verfügbar - Dark")
@Composable
fun GlucoseSourceSetupMultipleDriversPreview() {
    val simBodyDriver = PreviewGlucoseDriver(
        "de.dh.daps.plugin.simbody.cgm",
        UiText.DynamicString("SimBody Virtual CGM Driver"),
        DeviceLifecycleState.DeviceBound("simbody-virtual-cgm-01", UiText.DynamicString("SimBody Virtual CGM"))
    )
    AppTheme {
        GlucoseSourceSetupContent(
            uiState = GlucoseSourceSetupUiState(
                availableDrivers = listOf(
                    simBodyDriver,
                    PreviewGlucoseDriver("de.dh.daps.plugin.glucose.sample", UiText.DynamicString("Sample CGM Driver"))
                ),
                activeDriverId = "de.dh.daps.plugin.simbody.cgm"
            ),
            onNavigateUp = {},
            onSelectDriver = {},
            onBindGlucoseSource = { _, _ -> },
            onClearError = {}
        )
    }
}