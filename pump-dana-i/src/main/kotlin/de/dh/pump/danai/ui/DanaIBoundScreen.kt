package de.dh.pump.danai.ui

import android.app.Application
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Router
import androidx.compose.material3.Badge
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import de.dh.daps.common.device.state.DeviceLifecycleState
import de.dh.daps.common.domain.model.data.Timestamp
import de.dh.daps.common.domain.pump.PumpLifecycleState
import de.dh.daps.ui.common.composables.DateTimePickerDialog
import de.dh.daps.ui.common.composables.FramedCard
import de.dh.daps.ui.common.composables.NormalButton
import de.dh.daps.ui.common.composables.PrimaryButton
import de.dh.daps.ui.common.formatters.shortDateTime
import de.dh.daps.ui.common.theme.AppPreview
import de.dh.pump.dana.commands.options.DanaRsUserOptions
import de.dh.pump.danai.DanaIInsulinPumpDriver
import de.dh.pump.danai.R
import de.dh.pump.danai.core.DanaIPumpAdapter
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Stateful screen container for an already bound Dana-i insulin pump.
 */
@Composable
fun DanaIBoundScreen(
    driver: DanaIInsulinPumpDriver,
    state: DeviceLifecycleState.DeviceBound,
    onDriverReset: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DanaIBoundViewModel = viewModel(
        factory = DanaIBoundViewModel.Companion.Factory(
            LocalContext.current.applicationContext as Application
        )
    )
) {
    val userOptions by viewModel.userOptions.collectAsStateWithLifecycle()
    val isLoadingOptions by viewModel.isLoadingOptions.collectAsStateWithLifecycle()
    val isSavingOptions by viewModel.isSavingOptions.collectAsStateWithLifecycle()
    val optionsError by viewModel.optionsError.collectAsStateWithLifecycle()

    val activePump by driver.activePump.collectAsStateWithLifecycle()
    val pumpAdapter = activePump as? DanaIPumpAdapter
    val isMaintenanceActive = pumpAdapter?.connectionState?.collectAsStateWithLifecycle()?.value?.isMaintenanceMode ?: false

    val pumpStatus by (pumpAdapter?.pumpStatus)?.collectAsStateWithLifecycle() ?: remember { mutableStateOf(null) }
    val catheterLastChanged = (pumpStatus?.lifecycleState as? PumpLifecycleState.Durable)?.catheterLastChanged
    val coroutineScope = rememberCoroutineScope()

    if (isMaintenanceActive) {
        DanaIMaintenanceScreen(
            onEndMaintenance = { resetCatheterWear ->
                coroutineScope.launch {
                    if (resetCatheterWear) {
                        pumpAdapter.saveCatheterLastChanged(Timestamp.now())
                    }
                    pumpAdapter.setMaintenanceMode(false)
                }
            },
            modifier = modifier
        )
    } else {
        DanaIBoundContent(
            displayName = state.displayName.asString(),
            deviceId = state.deviceId,
            userOptions = userOptions,
            isLoadingOptions = isLoadingOptions,
            isSavingOptions = isSavingOptions,
            optionsError = optionsError,
            catheterLastChanged = catheterLastChanged,
            onLoadOptions = { viewModel.loadUserOptions(driver) },
            onStartMaintenance = {
                pumpAdapter?.setMaintenanceMode(true)
            },
            onSaveAndUnbind = { options ->
                viewModel.saveAndUnbind(driver, options) {
                    onDriverReset()
                }
            },
            onUnbindOnly = {
                viewModel.unbind(driver) {
                    onDriverReset()
                }
            },
            onSaveCatheterLastChanged = { newTimestamp ->
                coroutineScope.launch {
                    pumpAdapter?.saveCatheterLastChanged(newTimestamp)
                }
            },
            modifier = modifier
        )
    }
}

/**
 * View for an already bound Dana-i insulin pump with options, maintenance access, and unbind capabilities.
 */
@Composable
fun DanaIBoundContent(
    displayName: String,
    deviceId: String?,
    userOptions: DanaRsUserOptions?,
    isLoadingOptions: Boolean,
    isSavingOptions: Boolean,
    optionsError: String?,
    onLoadOptions: () -> Unit,
    onStartMaintenance: () -> Unit,
    onSaveAndUnbind: (DanaRsUserOptions?) -> Unit,
    onUnbindOnly: () -> Unit,
    modifier: Modifier = Modifier,
    catheterLastChanged: Timestamp? = null,
    onSaveCatheterLastChanged: ((Timestamp?) -> Unit)? = null
) {
    var showUnbindDialog by remember { mutableStateOf(false) }
    var showCatheterDatePicker by remember { mutableStateOf(false) }

    LaunchedEffect(showUnbindDialog) {
        if (showUnbindDialog) {
            onLoadOptions()
        }
    }

    FramedCard(
        modifier = modifier.fillMaxWidth()
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
                        text = stringResource(R.string.danai_bound_badge),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            HorizontalDivider()

            Text(
                text = stringResource(R.string.danai_bound_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            HorizontalDivider()

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
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Column {
                        Text(
                            text = stringResource(R.string.danai_bound_catheter_last_changed_label),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = shortDateTime(catheterLastChanged),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(
                    onClick = { showCatheterDatePicker = true }
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = stringResource(R.string.cd_danai_bound_catheter_edit),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            HorizontalDivider()

            PrimaryButton(
                onClick = onStartMaintenance,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Build,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.danai_bound_btn_start_maintenance))
            }

            NormalButton(
                onClick = { showUnbindDialog = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f),
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
            ) {
                Text(stringResource(R.string.danai_bound_btn_unbind))
            }
        }
    }

    if (showCatheterDatePicker) {
        val now = LocalDateTime.now()
        val minDateTime = now.minusDays(7)
        val initialDateTime = remember(catheterLastChanged) {
            catheterLastChanged?.let { ts ->
                val ldt = LocalDateTime.ofInstant(Instant.ofEpochMilli(ts.ms), ZoneId.systemDefault())
                when {
                    ldt.isBefore(minDateTime) -> minDateTime
                    ldt.isAfter(now) -> now
                    else -> ldt
                }
            } ?: now
        }

        DateTimePickerDialog(
            dialogTitle = stringResource(R.string.danai_bound_catheter_dialog_title),
            initialDateTime = initialDateTime,
            minDate = minDateTime.toLocalDate(),
            maxDate = now.toLocalDate(),
            onDismissRequest = { showCatheterDatePicker = false },
            onDateTimeSelected = { selectedLdt ->
                showCatheterDatePicker = false
                val clampedLdt = when {
                    selectedLdt.isBefore(minDateTime) -> minDateTime
                    selectedLdt.isAfter(now) -> now
                    else -> selectedLdt
                }
                val timestamp = Timestamp(clampedLdt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli())
                onSaveCatheterLastChanged?.invoke(timestamp)
            }
        )
    }

    if (showUnbindDialog) {
        DanaIPumpUnbindDialog(
            userOptions = userOptions,
            isLoading = isLoadingOptions,
            isSaving = isSavingOptions,
            error = optionsError,
            onSaveAndUnbind = { options ->
                showUnbindDialog = false
                onSaveAndUnbind(options)
            },
            onUnbindOnly = {
                showUnbindDialog = false
                onUnbindOnly()
            },
            onDismiss = { showUnbindDialog = false }
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DanaIBoundContentPreview() {
    AppPreview {
        DanaIBoundContent(
            displayName = "Dana-i (DANA-1234)",
            deviceId = "00:11:22:33:44:55",
            userOptions = null,
            isLoadingOptions = false,
            isSavingOptions = false,
            optionsError = null,
            catheterLastChanged = Timestamp(System.currentTimeMillis() - 86_400_000),
            onLoadOptions = {},
            onStartMaintenance = {},
            onSaveAndUnbind = {},
            onUnbindOnly = {}
        )
    }
}