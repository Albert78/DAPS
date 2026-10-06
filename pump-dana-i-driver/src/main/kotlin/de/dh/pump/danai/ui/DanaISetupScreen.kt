package de.dh.pump.danai.ui

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
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import de.dh.daps.common.model.InsulinPump
import de.dh.daps.common.model.PumpConnectionDescriptor
import de.dh.pump.danai.R

/**
 * Setup and pairing screen for Sooil Dana-i insulin pumps.
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

    val pluginContext = driver.getContext()

    // Automatically start BLE scan when screen opens
    LaunchedEffect(Unit) {
        viewModel.startDiscovery()
    }

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        Text(
            text = stringResource(R.string.danai_pump_setup_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(16.dp))

        connectError?.let { errorMsg ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = errorMsg,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Text(stringResource(R.string.danai_pump_setup_available_pumps), style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))

        if (scanResults.isEmpty() && !isScanning) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Text(
                    text = stringResource(R.string.danai_pump_setup_no_pumps_found),
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(scanResults) { pump ->
                    ListItem(
                        headlineContent = { Text(pump.name) },
                        supportingContent = { Text(pump.address) },
                        trailingContent = {
                            Button(
                                enabled = !isConnecting,
                                onClick = {
                                    val prefs = pluginContext?.preferences
                                    if (prefs != null) {
                                        viewModel.connectAndPair(
                                            driver = driver,
                                            scanResult = pump,
                                            preferences = prefs,
                                            onConnected = onConnected
                                        )
                                    }
                                }
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
                        },
                        leadingContent = {
                            Icon(
                                if (isScanning) Icons.Default.Refresh else Icons.Default.Bluetooth,
                                contentDescription = null
                            )
                        }
                    )
                    HorizontalDivider()
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f)
            ) {
                Text(stringResource(R.string.danai_pump_setup_btn_cancel))
            }

            Spacer(Modifier.width(8.dp))

            Button(
                onClick = { viewModel.startDiscovery() },
                modifier = Modifier.weight(1f),
                enabled = !isScanning && !isConnecting
            ) {
                if (isScanning) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.danai_pump_setup_btn_searching))
                } else {
                    Text(stringResource(R.string.danai_pump_setup_btn_search))
                }
            }
        }
    }
}