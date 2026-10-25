package de.dh.daps.ui.screens.systemcontrol

import android.content.res.Configuration
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Battery5Bar
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import de.dh.daps.common.model.ExpirationDate
import de.dh.daps.common.model.InsulinAmount
import de.dh.daps.common.model.InsulinConcentration
import de.dh.daps.common.model.PatchDeactivationReason
import de.dh.daps.common.model.PatchPhase
import de.dh.daps.common.model.PumpLifecycleState
import de.dh.daps.common.model.data.Timestamp
import de.dh.daps.common.ui.UiText
import de.dh.daps.core.pump.JobErrorCode
import de.dh.daps.core.pump.PumpCoordinatorState
import de.dh.daps.core.pump.PumpSyncStatus
import de.dh.daps.core.pump.RetryReason
import de.dh.daps.core.pump.SyncStep
import de.dh.daps.ui.R
import de.dh.daps.ui.common.composables.FramedCard
import de.dh.daps.ui.common.composables.PrimaryButton
import de.dh.daps.ui.common.icons.Icon_Pump_Device
import de.dh.daps.ui.common.icons.PumpReservoir
import de.dh.daps.ui.common.insulinValue
import de.dh.daps.ui.common.shortDate
import de.dh.daps.ui.common.shortDateTime
import de.dh.daps.ui.common.shortRelativeTime
import de.dh.daps.ui.common.smartTime
import de.dh.daps.ui.common.theme.AppTheme
import de.dh.daps.ui.common.theme.ExtendedTheme
import java.time.LocalDate

@Composable
fun PumpTabContent(
    modifier: Modifier = Modifier,
    uiState: PumpTabUiState = PumpTabUiState.Loading,
    onSetupPumpDriver: () -> Unit = {},
    onRefreshPumpStatus: () -> Unit = {},
    onCancelPumpJob: (String) -> Unit = {},
    onResetPumpCommunication: () -> Unit = {}
) {
    var showSettingsDialog by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        val mainHeadlineText = when (uiState) {
            is PumpTabUiState.Loading -> stringResource(R.string.system_control_pump_plugin_section_title)
            is PumpTabUiState.NoneConfigured -> stringResource(R.string.system_control_pump_none_active)
            is PumpTabUiState.Content -> uiState.pumpName.asString()
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = mainHeadlineText,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            if (uiState is PumpTabUiState.Content) {
                Spacer(modifier = Modifier.width(16.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val hasSettings = uiState.pumpSettingsScreen != null
                    val context = LocalContext.current
                    val noSettingsMessage = stringResource(R.string.system_control_pump_no_settings_toast)

                    OutlinedIconButton(
                        onClick = {
                            if (hasSettings) {
                                showSettingsDialog = true
                            } else {
                                Toast.makeText(context, noSettingsMessage, Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = if (hasSettings) {
                            IconButtonDefaults.outlinedIconButtonColors()
                        } else {
                            IconButtonDefaults.outlinedIconButtonColors(
                                contentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            )
                        },
                        border = if (hasSettings) {
                            IconButtonDefaults.outlinedIconButtonBorder(enabled = true)
                        } else {
                            IconButtonDefaults.outlinedIconButtonBorder(enabled = false)
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = stringResource(R.string.cd_system_control_pump_settings),
                            tint = if (hasSettings) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            }
                        )
                    }
                    OutlinedIconButton(onClick = onSetupPumpDriver) {
                        Icon(
                            imageVector = Icon_Pump_Device,
                            contentDescription = stringResource(R.string.cd_system_control_pump_setup)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (uiState is PumpTabUiState.NoneConfigured) {
            PrimaryButton(
                onClick = onSetupPumpDriver,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icon_Pump_Device,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.cd_system_control_pump_setup),
                    fontWeight = FontWeight.Bold
                )
            }
        } else {
            SectionHeader(
                title = stringResource(R.string.system_control_tab_overview)
            )
            Spacer(modifier = Modifier.height(8.dp))

            PumpOverviewCard(
                uiState = uiState,
                onRefreshPumpStatus = onRefreshPumpStatus
            )
        }

        if (uiState is PumpTabUiState.Content) {
            Spacer(modifier = Modifier.height(16.dp))
            PumpLifecycleSection(
                lifecycleState = uiState.lifecycleState,
                reservoirRemaining = uiState.reservoirRemaining,
                tdd = uiState.tdd,
                estimatedReservoirRunout = uiState.estimatedReservoirRunout
            )

            Spacer(modifier = Modifier.height(16.dp))
            SectionHeader(
                title = stringResource(R.string.system_control_pump_technical_status)
            )
            Spacer(modifier = Modifier.height(8.dp))

            PumpTechnicalStatusCard(
                coordinatorState = uiState.coordinatorState,
                syncStatus = uiState.syncStatus,
                nextWakeupTime = uiState.nextWakeupTime,
                pendingJobs = uiState.pendingJobs,
                onCancelJob = onCancelPumpJob,
                onResetPumpCommunication = onResetPumpCommunication
            )

            if (uiState.pumpPluginSection != null) {
                Spacer(modifier = Modifier.height(16.dp))
                SectionHeader(
                    title = stringResource(R.string.system_control_pump_plugin_section_title)
                )
                Spacer(modifier = Modifier.height(8.dp))
                uiState.pumpPluginSection.invoke()
            }
        }

        if (showSettingsDialog && uiState is PumpTabUiState.Content && uiState.pumpSettingsScreen != null) {
            Dialog(
                onDismissRequest = { showSettingsDialog = false },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.background,
                    modifier = Modifier.fillMaxSize()
                ) {
                    uiState.pumpSettingsScreen.invoke {
                        showSettingsDialog = false
                    }
                }
            }
        }
    }
}

@Composable
fun PumpOverviewCard(
    uiState: PumpTabUiState,
    modifier: Modifier = Modifier,
    onRefreshPumpStatus: () -> Unit = {}
) {
    FramedCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            when (uiState) {
                is PumpTabUiState.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                is PumpTabUiState.NoneConfigured -> {
                    Text(
                        text = stringResource(R.string.system_control_pump_none_active),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                is PumpTabUiState.Content -> {
                    val notAvailableText = stringResource(R.string.system_control_value_not_available)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            ControlDetailRow(
                                label = stringResource(R.string.system_control_pump_manufacturer_label),
                                reserveIconSpace = true
                            ) {
                                Text(
                                    text = uiState.manufacturer ?: notAvailableText,
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            ControlDetailRow(
                                label = stringResource(R.string.system_control_pump_model_label),
                                reserveIconSpace = true
                            ) {
                                Text(
                                    text = uiState.pumpModel ?: notAvailableText,
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            ControlDetailRow(
                                label = stringResource(R.string.system_control_pump_serial_number_label),
                                reserveIconSpace = true
                            ) {
                                Text(
                                    text = uiState.serialNumber ?: notAvailableText,
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            ControlDetailRow(
                                label = "",
                                reserveIconSpace = true
                            ) {
                                OutlinedButton(
                                    onClick = onRefreshPumpStatus,
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Refresh,
                                            contentDescription = stringResource(R.string.system_control_pump_job_type_refresh_status),
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "Aktualisieren",
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                }
                            }
                        }
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant
                    )

                    val isExpired = when (val lifecycle = uiState.lifecycleState) {
                        is PumpLifecycleState.Patch -> lifecycle.isExpired
                        is PumpLifecycleState.Durable -> false
                    }
                    val statusText = when {
                        isExpired -> stringResource(R.string.overview_glucose_source_status_expired)
                        uiState.isSuspended -> stringResource(R.string.system_control_pump_state_suspended)
                        else -> stringResource(R.string.system_control_pump_state_active)
                    }
                    val statusColor = when {
                        isExpired -> MaterialTheme.colorScheme.error
                        uiState.isSuspended -> ExtendedTheme.semanticColors.warning
                        else -> ExtendedTheme.semanticColors.good
                    }

                    val lastConnTimestamp = uiState.lastConnectionTimestamp
                    val lastConnTimeText = if (lastConnTimestamp.isValid()) {
                        smartTime(lastConnTimestamp)
                    } else {
                        notAvailableText
                    }

                    val lastConnRelativeText = if (lastConnTimestamp.isValid()) {
                        val rel = shortRelativeTime(lastConnTimestamp)
                        if (rel.isNotEmpty()) "($rel)" else null
                    } else {
                        null
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            ControlDetailRow(
                                label = stringResource(R.string.system_control_pump_status_label),
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
                            if (uiState.pumpConnected) {
                                ControlDetailRow(
                                    label = stringResource(R.string.system_control_pump_connection_status_label),
                                    icon = Icons.Outlined.Link
                                ) {
                                    Text(
                                        text = stringResource(R.string.system_control_pump_status_connected),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.secondary,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            } else {
                                ControlDetailRow(
                                    label = stringResource(R.string.system_control_pump_last_conn_label),
                                    icon = Icons.Outlined.Link
                                ) {
                                    Column {
                                        Text(
                                            text = lastConnTimeText,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (lastConnRelativeText != null) {
                                            Text(
                                                text = lastConnRelativeText,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
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
                                label = stringResource(R.string.system_control_pump_battery_label),
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
                                label = stringResource(R.string.system_control_pump_reservoir_label),
                                icon = Icons.Outlined.PumpReservoir
                            ) {
                                Text(
                                    text = reservoirDisplay,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                val volumeMl = uiState.reservoirRemaining.toMl(uiState.insulinConcentration)
                                Text(
                                    text = stringResource(R.string.system_control_pump_reservoir_detail_format, volumeMl, uiState.insulinConcentration.uValue),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PumpLifecycleSection(
    lifecycleState: PumpLifecycleState,
    modifier: Modifier = Modifier,
    reservoirRemaining: InsulinAmount? = null,
    tdd: InsulinAmount? = null,
    estimatedReservoirRunout: Timestamp? = null
) {
    Column(modifier = modifier) {
        SectionHeader(
            title = stringResource(R.string.system_control_pump_expirations_title)
        )
        Spacer(modifier = Modifier.height(8.dp))

        when (lifecycleState) {
            is PumpLifecycleState.Patch -> PatchPumpLifecycleSection(state = lifecycleState)
            is PumpLifecycleState.Durable -> DurablePumpLifecycleSection(
                state = lifecycleState,
                reservoirRemaining = reservoirRemaining,
                tdd = tdd,
                estimatedReservoirRunout = estimatedReservoirRunout
            )
        }
    }
}

@Composable
fun PatchPumpLifecycleSection(
    state: PumpLifecycleState.Patch,
    modifier: Modifier = Modifier
) {
    FramedCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.overview_pump_patch_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                val (phaseText, phaseColor) = when (state.phase) {
                    PatchPhase.PRIMING -> "Vorbereitung" to MaterialTheme.colorScheme.primary
                    PatchPhase.ACTIVE -> "Aktiv" to ExtendedTheme.semanticColors.good
                    PatchPhase.IN_GRACE_PERIOD -> "Nachfrist" to ExtendedTheme.semanticColors.warning
                    PatchPhase.EXPIRED -> stringResource(R.string.overview_glucose_source_status_expired) to MaterialTheme.colorScheme.error
                }

                Surface(
                    color = phaseColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = phaseText,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = phaseColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (state.isOperating) {
                val progress = state.activeProgress()
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Aktivitätsdauer",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${(progress * 100).toInt()}%",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = if (state.isInGracePeriod) ExtendedTheme.semanticColors.warning else MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    ControlDetailRow(
                        label = stringResource(R.string.system_control_source_expiration_label),
                        reserveIconSpace = true
                    ) {
                        Column {
                            Text(
                                text = if (state.activeUntil.isValid()) shortDateTime(state.activeUntil) else stringResource(R.string.system_control_value_not_available),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (state.activeUntil.isValid()) {
                                val rel = shortRelativeTime(state.activeUntil)
                                if (rel.isNotEmpty()) {
                                    Text(
                                        text = "($rel)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    if (state.graceUntil.isValid()) {
                        ControlDetailRow(
                            label = stringResource(R.string.system_control_expiration_grace_until_label),
                            reserveIconSpace = true
                        ) {
                            Column {
                                Text(
                                    text = shortDateTime(state.graceUntil),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.tertiary,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                val graceRel = shortRelativeTime(state.graceUntil)
                                if (graceRel.isNotEmpty()) {
                                    Text(
                                        text = "($graceRel)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }

            val reason = state.deactivationReason
            if (state.isExpired && reason != null) {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outlineVariant
                )
                Text(
                    text = "Deaktivierungsgrund: ${reason.name.replace('_', ' ')}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun DurablePumpLifecycleSection(
    state: PumpLifecycleState.Durable,
    modifier: Modifier = Modifier,
    reservoirRemaining: InsulinAmount? = null,
    tdd: InsulinAmount? = null,
    estimatedReservoirRunout: Timestamp? = null
) {
    FramedCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Text(
                text = stringResource(R.string.system_control_pump_catheter_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    ControlDetailRow(
                        label = stringResource(R.string.system_control_pump_last_changed_label),
                        reserveIconSpace = true
                    ) {
                        val lastChanged = state.catheterLastChanged
                        if (lastChanged != null) {
                            Text(
                                text = shortDateTime(lastChanged),
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        } else {
                            Text(
                                text = stringResource(R.string.system_control_value_not_available),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    ControlDetailRow(
                        label = stringResource(R.string.overview_pump_next_expiration_label),
                        reserveIconSpace = true
                    ) {
                        val exp = state.approximateCatheterExpiration
                        if (exp != null) {
                            Column {
                                Text(
                                    text = shortDate(exp),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                val rel = shortRelativeTime(ExpirationDate.Approximate(exp))
                                if (rel.isNotEmpty()) {
                                    Text(
                                        text = "($rel)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        } else {
                            Text(
                                text = stringResource(R.string.system_control_value_not_available),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
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

            Text(
                text = stringResource(R.string.system_control_pump_reservoir_label),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    ControlDetailRow(
                        label = stringResource(R.string.system_control_pump_reservoir_level_label),
                        reserveIconSpace = true
                    ) {
                        if (reservoirRemaining != null) {
                            Text(
                                text = insulinValue(reservoirRemaining),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                        } else {
                            Text(
                                text = stringResource(R.string.system_control_value_not_available),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    ControlDetailRow(
                        label = stringResource(R.string.system_control_pump_tdd_label),
                        reserveIconSpace = true
                    ) {
                        if (tdd != null) {
                            Text(
                                text = insulinValue(tdd),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                        } else {
                            Text(
                                text = stringResource(R.string.system_control_value_not_available),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            ControlDetailRow(
                label = stringResource(R.string.system_control_pump_reservoir_estimated_runout_label),
                reserveIconSpace = true
            ) {
                if (estimatedReservoirRunout != null && estimatedReservoirRunout.isValid()) {
                    Column {
                        Text(
                            text = shortDateTime(estimatedReservoirRunout),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        val rel = shortRelativeTime(estimatedReservoirRunout)
                        if (rel.isNotEmpty()) {
                            Text(
                                text = "($rel)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                } else {
                    Text(
                        text = stringResource(R.string.system_control_value_not_available),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}



@Composable
fun PumpTechnicalStatusCard(
    coordinatorState: PumpCoordinatorState,
    modifier: Modifier = Modifier,
    syncStatus: PumpSyncStatus = PumpSyncStatus.InSync(null),
    nextWakeupTime: Timestamp? = null,
    pendingJobs: List<PumpJobItem> = emptyList(),
    onCancelJob: (String) -> Unit = {},
    onResetPumpCommunication: () -> Unit = {}
) {
    FramedCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Text(
                text = stringResource(R.string.system_control_pump_communication_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            val (statusText, statusColor) = when (coordinatorState) {
                PumpCoordinatorState.Idle -> stringResource(R.string.system_control_pump_coordinator_state_idle) to MaterialTheme.colorScheme.onSurface
                PumpCoordinatorState.Running -> stringResource(R.string.system_control_pump_coordinator_state_running) to MaterialTheme.colorScheme.primary
                PumpCoordinatorState.Error -> stringResource(R.string.system_control_pump_coordinator_state_error) to MaterialTheme.colorScheme.error
            }

            if (coordinatorState == PumpCoordinatorState.Error) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.system_control_pump_coordinator_error_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.system_control_pump_communication_status_label),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = statusColor
                    )
                }

                if (coordinatorState == PumpCoordinatorState.Error) {
                    PrimaryButton(
                        onClick = onResetPumpCommunication
                    ) {
                        Text(
                            text = stringResource(R.string.system_control_pump_reset_button),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant
            )

            // Section: Synchronisationsverlauf (PumpSyncStatus)
            Text(
                text = stringResource(R.string.system_control_pump_sync_status_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            when (syncStatus) {
                is PumpSyncStatus.InSync -> {
                    Text(
                        text = stringResource(R.string.system_control_pump_sync_status_in_sync),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = ExtendedTheme.semanticColors.good
                    )
                    val scheduledTime = syncStatus.nextScheduledWakeup ?: nextWakeupTime
                    if (scheduledTime != null && scheduledTime.isValid()) {
                        val formattedTime = smartTime(scheduledTime)
                        val relText = shortRelativeTime(scheduledTime)
                        val fullText = if (relText.isNotEmpty()) "$formattedTime ($relText)" else formattedTime
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${stringResource(R.string.system_control_pump_next_wakeup_label)}: $fullText",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                is PumpSyncStatus.Reconciling -> {
                    val stepText = when (syncStatus.step) {
                        SyncStep.RefreshingStatus -> stringResource(R.string.system_control_pump_sync_step_refreshing_status)
                        SyncStep.ReconcilingSuspend -> stringResource(R.string.system_control_pump_sync_step_reconciling_suspend)
                        SyncStep.ReconcilingTempBasal -> stringResource(R.string.system_control_pump_sync_step_reconciling_temp_basal)
                        SyncStep.ReconcilingProfile -> stringResource(R.string.system_control_pump_sync_step_reconciling_profile)
                        SyncStep.ExecutingBolus -> stringResource(R.string.system_control_pump_sync_step_executing_bolus)
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = stepText,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                is PumpSyncStatus.Retrying -> {
                    val reasonStr = when (syncStatus.reason) {
                        RetryReason.PUMP_BUSY -> stringResource(R.string.system_control_pump_retry_reason_busy)
                        RetryReason.CONNECTION_FAILED -> stringResource(R.string.system_control_pump_retry_reason_connection)
                    }
                    val statusFormatted = stringResource(
                        R.string.system_control_pump_retrying_status,
                        reasonStr,
                        syncStatus.currentAttempt,
                        syncStatus.maxAttempts
                    )
                    Text(
                        text = statusFormatted,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = ExtendedTheme.semanticColors.warning
                    )
                    if (syncStatus.nextAttemptAt.isValid()) {
                        val formattedNext = smartTime(syncStatus.nextAttemptAt)
                        val relNext = shortRelativeTime(syncStatus.nextAttemptAt)
                        val fullNext = if (relNext.isNotEmpty()) "$formattedNext ($relNext)" else formattedNext
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${stringResource(R.string.system_control_pump_next_retry_label)}: $fullNext",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                is PumpSyncStatus.Error -> {
                    val sinceText = if (syncStatus.errorSince.isValid()) {
                        val smartT = smartTime(syncStatus.errorSince)
                        val relT = shortRelativeTime(syncStatus.errorSince)
                        if (relT.isNotEmpty()) "$smartT ($relT)" else smartT
                    } else null

                    Column {
                        Text(
                            text = stringResource(R.string.system_control_pump_coordinator_state_error),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                        if (sinceText != null) {
                            Text(
                                text = "Seit: $sinceText",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
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

            Text(
                text = stringResource(R.string.system_control_pump_bolus_jobs_title),
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



internal fun samplePumpTabUiState(): PumpTabUiState = PumpTabUiState.Content(
    pumpName = UiText.DynamicString("SimBody Virtuelle Insulinpumpe"),
    batteryPercent = 85,
    reservoirRemaining = InsulinAmount(86.0),
    insulinConcentration = InsulinConcentration.U100,
    lastConnectionTimestamp = Timestamp(System.currentTimeMillis() - 60_000),
    manufacturer = "DAPS",
    pumpModel = "Simulator",
    serialNumber = "SIM-001",
    pumpConnected = true,
    isSuspended = false,
    lifecycleState = PumpLifecycleState.Patch(
        phase = PatchPhase.ACTIVE,
        activationTime = Timestamp.now(),
        activeUntil = Timestamp(System.currentTimeMillis() + 172_800_000),
        graceUntil = Timestamp(System.currentTimeMillis() + 180_000_000)
    ),
    estimatedReservoirRunout = Timestamp(System.currentTimeMillis() + (86.0 / 25.0 * 24 * 3600 * 1000).toLong()),
    coordinatorState = PumpCoordinatorState.Idle,
    syncStatus = PumpSyncStatus.InSync(Timestamp(System.currentTimeMillis() + 300_000)),
    nextWakeupTime = Timestamp(System.currentTimeMillis() + 300_000),
    pendingJobs = listOf(
        PumpJobItem(
            id = "job_1",
            title = UiText.StringResource(R.string.system_control_pump_job_type_bolus, "1,50")
        )
    ),
    pumpPluginSection = {
        PumpPluginExampleCard()
    }
)

@Composable
fun PumpPluginExampleCard(
    modifier: Modifier = Modifier,
    maintenanceStatus: String = "Optimal",
    activeProfileName: String = "Standard",
    errorMemory: String = "Keine Fehler",
    onRunSelfTest: () -> Unit = {},
    onPrimeCatheter: () -> Unit = {}
) {
    FramedCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    ControlDetailRow(
                        label = "Wartungsstatus"
                    ) {
                        Text(
                            text = maintenanceStatus,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    ControlDetailRow(
                        label = "Aktives Profil"
                    ) {
                        Text(
                            text = activeProfileName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            ControlDetailRow(
                label = "Fehlerspeicher"
            ) {
                Text(
                    text = errorMemory,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PrimaryButton(
                    onClick = onRunSelfTest,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Selbsttest",
                        fontWeight = FontWeight.Bold
                    )
                }
                PrimaryButton(
                    onClick = onPrimeCatheter,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Katheter füllen",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
@Preview(showBackground = true, heightDp = 1200, name = "Pump Tab - Light Mode")
@Preview(showBackground = true, heightDp = 1200, uiMode = Configuration.UI_MODE_NIGHT_YES, name = "Pump Tab - Dark Mode")
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

@Preview(showBackground = true, name = "Pump Tab - Not Configured")
@Composable
fun PumpTabNotConfiguredPreview() {
    AppTheme {
        Surface {
            PumpTabContent(
                uiState = PumpTabUiState.NoneConfigured,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@Preview(showBackground = true, name = "Pump Tab - Loading")
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, name = "Pump Tab - Loading - Dark Mode")
@Composable
fun PumpTabLoadingPreview() {
    AppTheme {
        Surface {
            PumpTabContent(
                uiState = PumpTabUiState.Loading,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@Preview(showBackground = true, name = "Technical Status Card - Error")
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, name = "Technical Status Card - Error - Dark Mode")
@Composable
fun PumpTechnicalStatusErrorCardPreview() {
    AppTheme {
        Surface {
            PumpTechnicalStatusCard(
                coordinatorState = PumpCoordinatorState.Error,
                syncStatus = PumpSyncStatus.Error(Timestamp.now(), JobErrorCode.ConnectionFailed("Timeout")),
                nextWakeupTime = null,
                pendingJobs = listOf(
                    PumpJobItem(
                        id = "job_1",
                        title = UiText.StringResource(R.string.system_control_pump_job_type_bolus, "1,50"),
                        errorMessage = UiText.StringResource(R.string.system_control_pump_job_error_connection_failed, ": Timeout")
                    )
                ),
                onCancelJob = {},
                onResetPumpCommunication = {},
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@Preview(showBackground = true, name = "Patch Pump Lifecycle - Active")
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, name = "Patch Pump Lifecycle - Active - Dark Mode")
@Composable
fun PatchPumpLifecycleSectionActivePreview() {
    AppTheme {
        Surface {
            PumpLifecycleSection(
                lifecycleState = PumpLifecycleState.Patch(
                    phase = PatchPhase.ACTIVE,
                    activationTime = Timestamp.now(),
                    activeUntil = Timestamp(System.currentTimeMillis() + 172_800_000),
                    graceUntil = Timestamp(System.currentTimeMillis() + 180_000_000)
                ),
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@Preview(showBackground = true, name = "Patch Pump Lifecycle - Grace Period")
@Composable
fun PatchPumpLifecycleSectionGracePreview() {
    AppTheme {
        Surface {
            PumpLifecycleSection(
                lifecycleState = PumpLifecycleState.Patch(
                    phase = PatchPhase.IN_GRACE_PERIOD,
                    activationTime = Timestamp(System.currentTimeMillis() - 250_000_000),
                    activeUntil = Timestamp(System.currentTimeMillis() - 3_600_000),
                    graceUntil = Timestamp(System.currentTimeMillis() + 25_200_000)
                ),
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@Preview(showBackground = true, name = "Patch Pump Lifecycle - Expired")
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, name = "Patch Pump Lifecycle - Expired - Dark Mode")
@Composable
fun PatchPumpLifecycleSectionExpiredPreview() {
    AppTheme {
        Surface {
            PumpLifecycleSection(
                lifecycleState = PumpLifecycleState.Patch(
                    phase = PatchPhase.EXPIRED,
                    activationTime = Timestamp(System.currentTimeMillis() - 260_000_000),
                    activeUntil = Timestamp(System.currentTimeMillis() - 7_200_000),
                    graceUntil = Timestamp(System.currentTimeMillis() - 1_800_000),
                    deactivationReason = PatchDeactivationReason.HARD_TIME_LIMIT
                ),
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@Preview(showBackground = true, name = "Durable Pump Lifecycle - Active")
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, name = "Durable Pump Lifecycle - Active - Dark Mode")
@Composable
fun DurablePumpLifecycleSectionActivePreview() {
    AppTheme {
        Surface {
            val reachMs = (86.0 / 25.0 * 24 * 3600 * 1000).toLong()
            PumpLifecycleSection(
                lifecycleState = PumpLifecycleState.Durable(
                    approximateCatheterExpiration = LocalDate.now().plusDays(2),
                    catheterLastChanged = Timestamp(System.currentTimeMillis() - 86_400_000)
                ),
                reservoirRemaining = InsulinAmount(86.0),
                tdd = InsulinAmount(25.0),
                estimatedReservoirRunout = Timestamp(System.currentTimeMillis() + reachMs),
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@Preview(showBackground = true, name = "Durable Pump Lifecycle - Expired")
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, name = "Durable Pump Lifecycle - Expired - Dark Mode")
@Composable
fun DurablePumpLifecycleSectionExpiredPreview() {
    AppTheme {
        Surface {
            PumpLifecycleSection(
                lifecycleState = PumpLifecycleState.Durable(
                    approximateCatheterExpiration = LocalDate.now().minusDays(1),
                    catheterLastChanged = Timestamp(System.currentTimeMillis() - 4 * 86_400_000)
                ),
                reservoirRemaining = null,
                tdd = null,
                estimatedReservoirRunout = null,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}