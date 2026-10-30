package de.dh.pump.danai.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material3.Badge
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.dh.daps.ui.common.composables.AppAlertDialog
import de.dh.daps.ui.common.composables.DialogTitle
import de.dh.daps.ui.common.composables.FramedCard
import de.dh.daps.ui.common.composables.NormalButton
import de.dh.daps.ui.common.composables.NormalTextButton
import de.dh.daps.ui.common.composables.PrimaryButton
import de.dh.daps.ui.common.theme.AppPreview
import de.dh.pump.danai.R

/**
 * Maintenance mode ("Wartungsmodus") screen for Sooil Dana-i pumps.
 * Displayed when the pump is in active maintenance mode.
 */
@Composable
fun DanaIMaintenanceScreen(
    onEndMaintenance: (resetCatheterWear: Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    DanaIMaintenanceContent(
        onEndMaintenance = onEndMaintenance,
        modifier = modifier
    )
}

/**
 * Pure presentation component for the active Dana-i maintenance mode screen.
 */
@Composable
fun DanaIMaintenanceContent(
    onEndMaintenance: (resetCatheterWear: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var showResetDialog by remember { mutableStateOf(false) }

    if (showResetDialog) {
        DanaIMaintenanceResetCatheterDialog(
            onConfirmReset = {
                showResetDialog = false
                onEndMaintenance(true)
            },
            onConfirmNoReset = {
                showResetDialog = false
                onEndMaintenance(false)
            },
            onDismiss = { showResetDialog = false }
        )
    }

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(onClick = { showResetDialog = true }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(de.dh.daps.common.R.string.action_back)
                )
            }
            Text(
                text = stringResource(R.string.danai_maintenance_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        FramedCard(
            modifier = Modifier.fillMaxWidth(),
            containerColor = MaterialTheme.colorScheme.errorContainer,
            borderColor = MaterialTheme.colorScheme.error
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
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Build,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(28.dp)
                        )
                        Text(
                            text = stringResource(R.string.danai_maintenance_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }

                    Badge(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ) {
                        Text(
                            text = stringResource(R.string.danai_maintenance_status_active),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                HorizontalDivider()

                Text(
                    text = stringResource(R.string.danai_maintenance_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )

                Spacer(modifier = Modifier.height(8.dp))

                PrimaryButton(
                    onClick = { showResetDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Build,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.danai_maintenance_btn_end))
                }
            }
        }
    }
}

/**
 * Dialog asking whether to reset catheter wear duration when exiting Dana-i maintenance mode.
 */
@Composable
fun DanaIMaintenanceResetCatheterDialog(
    onConfirmReset: () -> Unit,
    onConfirmNoReset: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val titleText = stringResource(R.string.danai_maintenance_reset_catheter_dialog_title)
    AppAlertDialog(
        onDismissRequest = onDismiss,
        titleText = titleText,
        content = {
            Column(
                modifier = Modifier.padding(24.dp),
            ) {
                DialogTitle(text = titleText)

                Spacer(modifier = Modifier.height(16.dp))

                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.danai_maintenance_reset_catheter_dialog_subtitle),
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PrimaryButton(
                            onClick = onConfirmReset,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.danai_maintenance_reset_catheter_dialog_btn_reset))
                        }

                        NormalButton(
                            onClick = onConfirmNoReset,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.danai_maintenance_reset_catheter_dialog_btn_dont_reset))
                        }

                        NormalTextButton(
                            onClick = onDismiss,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.danai_maintenance_reset_catheter_dialog_btn_cancel))
                        }
                    }
                }
            }
        },
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
private fun DanaIMaintenanceContentPreview() {
    AppPreview {
        DanaIMaintenanceContent(
            onEndMaintenance = {}
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun DanaIMaintenanceResetCatheterDialogPreview() {
    AppPreview {
        DanaIMaintenanceResetCatheterDialog(
            onConfirmReset = {},
            onConfirmNoReset = {},
            onDismiss = {}
        )
    }
}