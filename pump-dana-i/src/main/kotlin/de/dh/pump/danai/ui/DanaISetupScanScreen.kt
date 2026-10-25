package de.dh.pump.danai.ui

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Router
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.dh.daps.ui.common.composables.FramedCard
import de.dh.daps.ui.common.composables.PrimaryButton
import de.dh.daps.ui.common.composables.WarningBanner
import de.dh.daps.ui.common.theme.AppPreview
import de.dh.pump.danai.R
import de.dh.pump.danai.core.connection.DanaScanError
import de.dh.pump.danai.core.connection.DanaScanResult

/**
 * Discovery and pairing UI component for Dana-i insulin pumps.
 */
@Composable
fun DanaISetupContent(
    scanResults: List<DanaScanResult>,
    isScanning: Boolean,
    scanError: DanaScanError?,
    pairingAddress: String?,
    pairError: String?,
    isStaleBondError: Boolean,
    onStartDiscovery: () -> Unit,
    onPairClick: (DanaScanResult) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column {
            Text(
                text = stringResource(R.string.danai_setup_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(R.string.danai_setup_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        scanError?.let { err ->
            val errorText = when (err) {
                DanaScanError.BLUETOOTH_DISABLED -> stringResource(R.string.danai_setup_bluetooth_disabled)
                DanaScanError.SCANNER_UNAVAILABLE -> stringResource(R.string.danai_setup_scanner_unavailable)
            }
            WarningBanner(
                warningText = errorText,
                actionText = stringResource(R.string.danai_setup_btn_enable_bluetooth),
                onActionClick = { openBluetoothSettings(context) }
            )
        }

        pairError?.let { err ->
            WarningBanner(
                warningText = err,
                actionText = if (isStaleBondError) stringResource(R.string.danai_setup_btn_open_bluetooth_settings) else null,
                onActionClick = {
                    if (isStaleBondError) {
                        openBluetoothSettings(context)
                    }
                }
            )
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
                    text = stringResource(R.string.danai_setup_available_pumps),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                HorizontalDivider()

                if (scanResults.isEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (isScanning) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                            Text(
                                text = stringResource(R.string.danai_setup_searching_pumps),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Bluetooth,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = stringResource(R.string.danai_setup_no_pumps_found),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(scanResults) { result ->
                            DanaIScanResultRow(
                                result = result,
                                isPairing = (pairingAddress == result.address),
                                onPairClick = { onPairClick(result) }
                            )
                        }
                    }
                }

                HorizontalDivider()

                PrimaryButton(
                    onClick = onStartDiscovery,
                    enabled = !isScanning && pairingAddress == null,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isScanning) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = stringResource(R.string.danai_setup_searching_pumps))
                    } else {
                        Icon(
                            imageVector = Icons.Default.Bluetooth,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = stringResource(R.string.danai_setup_btn_search))
                    }
                }
            }
        }
    }
}

@Composable
private fun DanaIScanResultRow(
    result: DanaScanResult,
    isPairing: Boolean,
    onPairClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isPairing, onClick = onPairClick)
            .padding(vertical = 8.dp),
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
                modifier = Modifier.size(28.dp)
            )
            Column {
                Text(
                    text = result.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = result.address,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        PrimaryButton(
            onClick = onPairClick,
            enabled = !isPairing
        ) {
            if (isPairing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Text(stringResource(R.string.danai_setup_btn_pair))
            }
        }
    }
}

private fun openBluetoothSettings(context: Context) {
    val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }
    context.startActivity(intent)
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
            isStaleBondError = false,
            onStartDiscovery = {},
            onPairClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DanaISetupContentResultsPreview() {
    AppPreview {
        DanaISetupContent(
            scanResults = listOf(DanaScanResult.Fake("DANA-1234", "00:11:22:33:44:55")),
            isScanning = false,
            scanError = null,
            pairingAddress = null,
            pairError = stringResource(R.string.danai_setup_stale_bond_error),
            isStaleBondError = true,
            onStartDiscovery = {},
            onPairClick = {}
        )
    }
}