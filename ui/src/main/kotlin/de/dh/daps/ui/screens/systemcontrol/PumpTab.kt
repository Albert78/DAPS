package de.dh.daps.ui.screens.systemcontrol

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Battery5Bar
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.dh.daps.ui.common.icons.PumpReservoir
import de.dh.daps.ui.common.insulinValue
import de.dh.daps.ui.common.shortRelativeTimeAgo
import de.dh.daps.ui.common.theme.AppTheme
import de.dh.daps.ui.common.time

@Composable
fun PumpTabContent(
    modifier: Modifier = Modifier,
    uiState: PumpTabUiState = PumpTabUiState(),
    onChangePumpDriver: () -> Unit = {},
    onRefreshPumpStatus: () -> Unit = {},
    onDisconnectForMaintenance: () -> Unit = {},
    onCancelPumpJob: (String) -> Unit = {}
) {
    Column(modifier = modifier) {
        PumpOverviewCard(
            uiState = uiState,
            onChangePumpDriver = onChangePumpDriver
        )

        PumpJobsCard(
            pendingJobs = uiState.pendingJobs,
            onCancelJob = onCancelPumpJob
        )

        PumpActionsCard(
            uiState = uiState,
            onDisconnectForMaintenance = onDisconnectForMaintenance,
            onRefreshPumpStatus = onRefreshPumpStatus
        )

        if (uiState.pumpPluginSection != null) {
            Spacer(modifier = Modifier.height(24.dp))
            SectionHeader(
                title = "Insulinpumpe"
            )
            Spacer(modifier = Modifier.height(8.dp))
            uiState.pumpPluginSection.invoke()
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PumpOverviewCard(
    uiState: PumpTabUiState,
    modifier: Modifier = Modifier,
    onChangePumpDriver: () -> Unit = {}
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    ControlDetailRow(
                        label = "Pumpenmodell",
                        icon = Icons.Default.Info
                    ) {
                        Text(
                            text = uiState.pumpModel ?: "Nicht verbunden",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                OutlinedButton(
                    onClick = onChangePumpDriver
                ) {
                    Icon(
                        imageVector = Icons.Default.SwapHoriz,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (uiState.pumpModel != null) "Wechseln..." else "Auswählen...",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }

            if (uiState.pumpConnected) {
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        ControlDetailRow(
                            label = "Hersteller",
                            reserveIconSpace = true
                        ) {
                            Text(
                                text = uiState.manufacturer ?: "--",
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        ControlDetailRow(
                            label = "Seriennummer",
                            reserveIconSpace = true
                        ) {
                            Text(
                                text = uiState.serialNumber ?: "--",
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant
            )

            val statusText = when {
                !uiState.pumpConnected -> "Nicht verbunden"
                uiState.isSuspended -> "Unterbrochen"
                else -> "Aktiv"
            }
            val statusColor = when {
                !uiState.pumpConnected -> MaterialTheme.colorScheme.error
                uiState.isSuspended -> MaterialTheme.colorScheme.tertiary
                else -> MaterialTheme.colorScheme.secondary
            }

            val lastConnTimestamp = uiState.lastConnectionTimestamp
            val lastConnTimeText = if (lastConnTimestamp != null && lastConnTimestamp.isValid()) {
                time(lastConnTimestamp)
            } else {
                uiState.lastConnectionTimeText
            }

            val lastConnRelativeText = if (lastConnTimestamp != null && lastConnTimestamp.isValid()) {
                shortRelativeTimeAgo(lastConnTimestamp)
            } else {
                uiState.lastConnectionRelativeTimeText
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    ControlDetailRow(
                        label = "Status",
                        icon = if (uiState.pumpConnected) Icons.Default.Settings else Icons.Default.Cancel
                    ) {
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = statusColor,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    ControlDetailRow(
                        label = "Letzte Verbindung",
                        icon = Icons.Default.Sync
                    ) {
                        FlowRow(
                            verticalArrangement = Arrangement.Center,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = lastConnTimeText,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (!lastConnRelativeText.isNullOrEmpty()) {
                                val relativeText = lastConnRelativeText
                                val formattedRelative = if (relativeText.startsWith("(") && relativeText.endsWith(")")) {
                                    relativeText
                                } else {
                                    "($relativeText)"
                                }
                                Text(
                                    text = formattedRelative,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.secondary,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            if (uiState.pumpConnected) {
                val reservoirDisplay = if (uiState.reservoirUnits != null) {
                    insulinValue(uiState.reservoirUnits)
                } else {
                    uiState.reservoirText
                }

                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        ControlDetailRow(
                            label = "Batterie",
                            icon = Icons.Default.Battery5Bar
                        ) {
                            Text(
                                text = uiState.batteryPercentText,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        ControlDetailRow(
                            label = "Reservoir",
                            icon = Icons.Outlined.PumpReservoir
                        ) {
                            Text(
                                text = reservoirDisplay,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PumpActionsCard(
    uiState: PumpTabUiState,
    modifier: Modifier = Modifier,
    onDisconnectForMaintenance: () -> Unit = {},
    onRefreshPumpStatus: () -> Unit = {}
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ActionButton(
                icon = Icons.Default.Build,
                label = "Zur Wartung trennen",
                onClick = onDisconnectForMaintenance,
                enabled = uiState.pumpConnected
            )
            ActionButton(
                icon = Icons.Default.Refresh,
                label = "Status aktualisieren",
                onClick = onRefreshPumpStatus,
                enabled = uiState.pumpConnected
            )
        }
    }
}

@Composable
fun PumpJobsCard(
    pendingJobs: List<PumpJobItem>,
    onCancelJob: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Text(
                text = "Ausstehende Befehle",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            if (pendingJobs.isEmpty()) {
                Text(
                    text = "Alle Pumpenjobs sind abgearbeitet",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                pendingJobs.forEach { job ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = if (job.hasError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = job.title,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            job.errorMessage?.let { error ->
                                Text(
                                    text = error,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.error,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        IconButton(
                            onClick = { onCancelJob(job.id) },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Cancel,
                                contentDescription = "Befehl abbrechen",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.padding(4.dp)
    ) {
        IconButton(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        )
    }
}

@Preview(showBackground = true, name = "Pump Tab - Light Mode")
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, name = "Pump Tab - Dark Mode")
@Composable
fun PumpTabPreview() {
    AppTheme {
        Surface {
            PumpTabContent(
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@Preview(showBackground = true, name = "Pump Tab - Disconnected")
@Composable
fun PumpTabDisconnectedPreview() {
    AppTheme {
        Surface {
            PumpTabContent(
                uiState = PumpTabUiState(
                    pumpModel = null,
                    pumpConnected = false,
                    lastConnectionTimeText = "--",
                    lastConnectionRelativeTimeText = null,
                    pendingJobs = emptyList(),
                    pumpPluginSection = null
                ),
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}