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
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.dh.daps.common.model.InsulinAmount
import de.dh.daps.common.model.data.Timestamp
import de.dh.daps.common.ui.UiText
import de.dh.daps.ui.R
import de.dh.daps.ui.common.icons.PumpReservoir
import de.dh.daps.ui.common.insulinValue
import de.dh.daps.ui.common.shortRelativeTimeAgo
import de.dh.daps.ui.common.theme.AppTheme
import de.dh.daps.ui.common.time

@Composable
fun PumpTabContent(
    modifier: Modifier = Modifier,
    uiState: PumpTabUiState? = null,
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

        if (uiState != null) {
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
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PumpOverviewCard(
    uiState: PumpTabUiState?,
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
                Text(
                    text = uiState?.pumpName?.asString() ?: uiState?.pumpModel ?: "Keine Insulinpumpe aktiv",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(16.dp))
                OutlinedIconButton(
                    onClick = onChangePumpDriver
                ) {
                    Icon(
                        imageVector = Icons.Default.SwapHoriz,
                        contentDescription = if (uiState != null) "Insulinpumpe wechseln" else "Insulinpumpe einrichten"
                    )
                }
            }

            if (uiState != null) {
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
                            label = "Pumpenmodell",
                            reserveIconSpace = true
                        ) {
                            Text(
                                text = uiState.pumpModel ?: "--",
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

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

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outlineVariant
                )

                val statusText = when {
                    uiState.isSuspended -> stringResource(R.string.system_control_pump_state_suspended)
                    else -> stringResource(R.string.system_control_pump_state_active)
                }
                val statusColor = when {
                    uiState.isSuspended -> MaterialTheme.colorScheme.tertiary
                    else -> MaterialTheme.colorScheme.secondary
                }

                val lastConnTimestamp = uiState.lastConnectionTimestamp
                val lastConnTimeText = if (lastConnTimestamp.isValid()) {
                    time(lastConnTimestamp)
                } else {
                    "--"
                }

                val lastConnRelativeText = if (lastConnTimestamp.isValid()) {
                    shortRelativeTimeAgo(lastConnTimestamp)
                } else {
                    null
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        ControlDetailRow(
                            label = "Status",
                            icon = Icons.Default.Settings
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

                val reservoirDisplay = insulinValue(uiState.reservoirRemaining)

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
                                text = "${uiState.batteryPercent}%",
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
    @Suppress("UNUSED_PARAMETER") uiState: PumpTabUiState,
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
                label = stringResource(R.string.system_control_pump_action_maintainance_disconnect),
                onClick = onDisconnectForMaintenance
            )
            ActionButton(
                icon = Icons.Default.Refresh,
                label = stringResource(R.string.system_control_pump_job_type_refresh_status),
                onClick = onRefreshPumpStatus
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
                text = stringResource(R.string.system_control_pump_jobs_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            if (pendingJobs.isEmpty()) {
                Text(
                    text = stringResource(R.string.system_control_pump_jobs_empty),
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
                                text = job.title.asString(),
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            job.errorMessage?.let { error ->
                                Text(
                                    text = error.asString(),
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
                                contentDescription = stringResource(R.string.system_control_pump_job_cancel),
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

internal fun samplePumpTabUiState() = PumpTabUiState(
    pumpName = UiText.DynamicString("SimBody Virtuelle Insulinpumpe"),
    pumpModel = "Simulator",
    manufacturer = "DAPS",
    serialNumber = "SIM-001",
    pumpConnected = true,
    batteryPercent = 85,
    reservoirRemaining = InsulinAmount(140.0),
    lastConnectionTimestamp = Timestamp(System.currentTimeMillis() - 60_000),
    pendingJobs = listOf(
        PumpJobItem(
            id = "job_1",
            title = UiText.StringResource(R.string.system_control_pump_job_type_bolus, "1,50")
        )
    )
)

@Preview(showBackground = true, name = "Pump Tab - Light Mode")
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, name = "Pump Tab - Dark Mode")
@Composable
fun PumpTabPreview() {
    AppTheme {
        Surface {
            PumpTabContent(
                uiState = samplePumpTabUiState(),
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
                uiState = null,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}