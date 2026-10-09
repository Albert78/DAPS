package de.dh.pump.danai.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.BluetoothSearching
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Router
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import de.dh.daps.common.model.InsulinPump
import de.dh.daps.common.model.PumpConnectionDescriptor
import de.dh.daps.ui.common.composables.FramedCard
import de.dh.daps.ui.common.composables.NormalTextButton
import de.dh.daps.ui.common.composables.PrimaryButton
import de.dh.daps.ui.common.composables.SecondaryButton
import de.dh.daps.ui.common.composables.WarningBanner
import de.dh.daps.ui.common.theme.AppPreview
import de.dh.pump.danai.R
import de.dh.pump.danai.core.connection.DanaScanResult

/**
 * Stateful setup container for Sooil Dana-i insulin pumps.
 *
 * Manages [DanaISetupViewModel] state and lifecycle, then delegates UI rendering
 * to the stateless [DanaISetupContent] component.
 */
@Composable
fun DanaISetupScreen(
    driver: DanaIInsulinPumpDriver,
    onConnected: (InsulinPump, PumpConnectionDescriptor) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DanaISetupViewModel = viewModel()
) {
    val scanResults by viewModel.scanResults.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val isConnecting by viewModel.isConnecting.collectAsState()
    val connectError by viewModel.connectError.collectAsState()

    // Automatically start BLE scan when screen opens
    LaunchedEffect(Unit) {
        viewModel.startDiscovery()
    }

    DanaISetupContent(
        scanResults = scanResults,
        isScanning = isScanning,
        isConnecting = isConnecting,
        connectError = connectError,
        onStartDiscovery = { viewModel.startDiscovery() },
        onPairClick = { scanResult ->
            val prefs = driver.getContext()?.preferences
            if (prefs != null) {
                viewModel.connectAndPair(
                    driver = driver,
                    scanResult = scanResult,
                    preferences = prefs,
                    onConnected = onConnected
                )
            }
        },
        onCancel = onCancel,
        modifier = modifier
    )
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
    isConnecting: Boolean,
    connectError: String?,
    onStartDiscovery: () -> Unit,
    onPairClick: (DanaScanResult) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
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

        connectError?.let { errorMsg ->
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

        if (scanResults.isEmpty() && !isScanning) {
            FramedCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
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
            Spacer(modifier = Modifier.weight(1f))
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(scanResults, key = { it.address }) { pump ->
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
                                enabled = !isConnecting,
                                onClick = { onPairClick(pump) }
                            ) {
                                if (isConnecting) {
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
            }
        }

        // Action controls at bottom
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SecondaryButton(
                    onClick = onStartDiscovery,
                    modifier = Modifier.weight(1f),
                    enabled = !isScanning && !isConnecting
                ) {
                    if (isScanning) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onSecondary
                            )
                            Text(stringResource(R.string.danai_pump_setup_btn_searching))
                        }
                    } else {
                        Text(stringResource(R.string.danai_pump_setup_btn_search))
                    }
                }
            }

            NormalTextButton(
                onClick = onCancel,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.danai_pump_setup_btn_cancel))
            }
        }
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
            isConnecting = false,
            connectError = null,
            onStartDiscovery = {},
            onPairClick = {},
            onCancel = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DanaISetupContentScanningPreview() {
    AppPreview {
        DanaISetupContent(
            scanResults = listOf(
                DanaScanResult.Fake(name = "DANA-i-123", address = "00:11:22:33:44:55")
            ),
            isScanning = true,
            isConnecting = false,
            connectError = null,
            onStartDiscovery = {},
            onPairClick = {},
            onCancel = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DanaISetupContentEmptyPreview() {
    AppPreview {
        DanaISetupContent(
            scanResults = emptyList(),
            isScanning = false,
            isConnecting = false,
            connectError = null,
            onStartDiscovery = {},
            onPairClick = {},
            onCancel = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DanaISetupContentErrorPreview() {
    AppPreview {
        DanaISetupContent(
            scanResults = emptyList(),
            isScanning = false,
            isConnecting = false,
            connectError = "Bluetooth-Kopplung fehlgeschlagen. Bitte versuche es erneut.",
            onStartDiscovery = {},
            onPairClick = {},
            onCancel = {}
        )
    }
}