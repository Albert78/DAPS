package de.dh.pump.danai.ui

import android.app.Application
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.BluetoothSearching
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Router
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import de.dh.daps.common.model.DeviceLifecycleState
import de.dh.daps.ui.common.composables.FramedCard
import de.dh.daps.ui.common.composables.NormalButton
import de.dh.daps.ui.common.composables.NormalTextButton
import de.dh.daps.ui.common.composables.PrimaryButton
import de.dh.daps.ui.common.composables.SecondaryButton
import de.dh.daps.ui.common.composables.WarningBanner
import de.dh.daps.ui.common.theme.AppPreview
import de.dh.pump.danai.DanaIInsulinPumpDriver
import de.dh.pump.danai.R
import de.dh.pump.danai.core.connection.DanaScanError
import de.dh.pump.danai.core.connection.DanaScanResult
import de.dh.daps.common.R as CommonR

/**
 * Stateful setup container for Sooil Dana-i insulin pumps.
 *
 * Manages [DanaISetupViewModel] state and lifecycle, branching between [DeviceLifecycleState.DeviceBound]
 * and the setup/scan workflow.
 */
@Composable
fun DanaISetupScreen(
    driver: DanaIInsulinPumpDriver,
    onDriverReset: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DanaISetupViewModel = viewModel(
        factory = DanaISetupViewModel.Companion.Factory(
            LocalContext.current.applicationContext as Application
        )
    )
) {
    val lifecycleState by driver.lifecycleState.collectAsStateWithLifecycle()
    val scanResults by viewModel.scanResults.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val scanError by viewModel.scanError.collectAsState()
    val pairingAddress by viewModel.pairingAddress.collectAsState()
    val pairError by viewModel.pairError.collectAsState()

    when (val state = lifecycleState) {
        is DeviceLifecycleState.DeviceBound -> {
            DanaIBoundContent(
                displayName = state.displayName.asString(),
                deviceId = state.deviceId,
                onUnbind = {
                    viewModel.unbind(driver) {
                        onDriverReset()
                    }
                },
                modifier = modifier
            )
        }
        else -> {
            // Automatically start BLE scan when screen opens in setup/unbound state
            LaunchedEffect(Unit) {
                viewModel.startDiscovery()
            }

            DanaISetupContent(
                scanResults = scanResults,
                isScanning = isScanning,
                scanError = scanError,
                pairingAddress = pairingAddress,
                pairError = pairError,
                onStartDiscovery = { viewModel.startDiscovery() },
                onPairClick = { scanResult ->
                    viewModel.pairAndBind(
                        driver = driver,
                        scanResult = scanResult
                    )
                },
                modifier = modifier
            )
        }
    }
}

/**
 * Stateless view for an already bound Dana-i insulin pump.
 */
@Composable
fun DanaIBoundContent(
    displayName: String,
    deviceId: String?,
    onUnbind: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showUnbindDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxSize(),
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Router,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                        Column {
                            Text(
                                text = displayName,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            if (!deviceId.isNullOrEmpty()) {
                                Text(
                                    text = deviceId,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    Badge(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ) {
                        Text(
                            text = stringResource(R.string.danai_pump_bound_badge),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                HorizontalDivider()

                Text(
                    text = stringResource(R.string.danai_pump_bound_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            NormalButton(
                onClick = { showUnbindDialog = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.danai_pump_btn_unbind))
            }
        }
    }

    if (showUnbindDialog) {
        AlertDialog(
            onDismissRequest = { showUnbindDialog = false },
            title = { Text(stringResource(R.string.danai_pump_unbind_dialog_title)) },
            text = { Text(stringResource(R.string.danai_pump_unbind_dialog_message)) },
            confirmButton = {
                PrimaryButton(
                    onClick = {
                        showUnbindDialog = false
                        onUnbind()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text(stringResource(R.string.danai_pump_unbind_dialog_confirm))
                }
            },
            dismissButton = {
                NormalTextButton(onClick = { showUnbindDialog = false }) {
                    Text(stringResource(CommonR.string.action_cancel))
                }
            }
        )
    }
}

/**
 * Pure presentation composable for the Dana-i setup screen.
 *
 * Fully decoupled from ViewModel and driver logic for easy previewing and UI testing.
 */
@Composable
fun DanaISetupContent(
    scanResults: List<DanaScanResult>,
    isScanning: Boolean,
    scanError: DanaScanError?,
    pairingAddress: String?,
    pairError: String?,
    onStartDiscovery: () -> Unit,
    onPairClick: (DanaScanResult) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isPairingAnyPump = pairingAddress != null

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Driver header & subtitle
        FramedCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = stringResource(R.string.danai_pump_setup_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(R.string.danai_pump_setup_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        scanError?.let { error ->
            val warningText = when (error) {
                DanaScanError.BLUETOOTH_DISABLED -> stringResource(R.string.danai_pump_setup_bluetooth_disabled)
                DanaScanError.SCANNER_UNAVAILABLE -> stringResource(R.string.danai_pump_setup_scanner_unavailable)
            }
            val actionText = if (error == DanaScanError.BLUETOOTH_DISABLED) {
                stringResource(R.string.danai_pump_setup_btn_enable_bluetooth)
            } else {
                null
            }

            WarningBanner(
                warningText = warningText,
                actionText = actionText,
                onActionClick = {
                    if (error == DanaScanError.BLUETOOTH_DISABLED) {
                        try {
                            context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
                        } catch (_: Exception) {
                            context.startActivity(Intent(Settings.ACTION_SETTINGS))
                        }
                    }
                }
            )
        }

        pairError?.let { errorMsg ->
            WarningBanner(
                warningText = errorMsg,
                onActionClick = {}
            )
        }

        Text(
            text = stringResource(R.string.danai_pump_setup_available_pumps),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        if (scanResults.isEmpty()) {
            FramedCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (isScanning) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = stringResource(R.string.danai_pump_setup_searching_pumps),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = stringResource(R.string.danai_pump_setup_no_pumps_found),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.weight(1f))
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(scanResults, key = { it.address }) { pump ->
                    val isPairingThisPump = pairingAddress == pump.address

                    FramedCard(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = if (isScanning) Icons.AutoMirrored.Filled.BluetoothSearching else Icons.Default.Router,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = pump.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = pump.address,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            PrimaryButton(
                                enabled = !isPairingAnyPump,
                                onClick = { onPairClick(pump) }
                            ) {
                                if (isPairingThisPump) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                } else {
                                    Text(stringResource(R.string.danai_pump_setup_btn_pair))
                                }
                            }
                        }
                    }
                }

                if (isScanning) {
                    item {
                        FramedCard(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = stringResource(R.string.danai_pump_setup_searching_pumps),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // Action controls at bottom
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SecondaryButton(
                onClick = onStartDiscovery,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isScanning && !isPairingAnyPump
            ) {
                Text(stringResource(R.string.danai_pump_setup_btn_search))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DanaIBoundContentPreview() {
    AppPreview {
        DanaIBoundContent(
            displayName = "Dana-i (DANA-1234)",
            deviceId = "00:11:22:33:44:55",
            onUnbind = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DanaISetupContentPreview() {
    AppPreview {
        DanaISetupContent(
            scanResults = listOf(
                DanaScanResult.Fake(name = "DANA-i-123", address = "00:11:22:33:44:55"),
                DanaScanResult.Fake(name = "DANA-i-456", address = "AA:BB:CC:DD:EE:FF")
            ),
            isScanning = false,
            scanError = null,
            pairingAddress = null,
            pairError = null,
            onStartDiscovery = {},
            onPairClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DanaISetupContentScanningPreview() {
    AppPreview {
        DanaISetupContent(
            scanResults = emptyList(),
            isScanning = true,
            scanError = null,
            pairingAddress = null,
            pairError = null,
            onStartDiscovery = {},
            onPairClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DanaISetupContentBluetoothDisabledPreview() {
    AppPreview {
        DanaISetupContent(
            scanResults = emptyList(),
            isScanning = false,
            scanError = DanaScanError.BLUETOOTH_DISABLED,
            pairingAddress = null,
            pairError = null,
            onStartDiscovery = {},
            onPairClick = {}
        )
    }
}