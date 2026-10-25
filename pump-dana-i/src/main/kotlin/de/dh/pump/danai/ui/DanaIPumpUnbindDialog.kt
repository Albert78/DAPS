package de.dh.pump.danai.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.dh.daps.ui.common.composables.NormalTextButton
import de.dh.daps.ui.common.composables.PrimaryButton
import de.dh.daps.ui.common.composables.WarningBanner
import de.dh.daps.ui.common.theme.AppPreview
import de.dh.pump.dana.commands.DanaGlucoseUnits
import de.dh.pump.dana.commands.options.DanaRsUserOptions
import de.dh.pump.danai.R

/**
 * Dialog allowing users to configure display backlight before unbinding.
 */
@Composable
fun DanaIPumpUnbindDialog(
    userOptions: DanaRsUserOptions?,
    isLoading: Boolean,
    isSaving: Boolean,
    error: String?,
    onSaveAndUnbind: (DanaRsUserOptions) -> Unit,
    onUnbindOnly: () -> Unit,
    onDismiss: () -> Unit,
) {
    var setLightEnabled by remember { mutableStateOf(true) }
    var backlightTime by remember(userOptions) { mutableIntStateOf(userOptions?.backlightOnTimeSec ?: 30) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.danai_unbind_dialog_title)) },
        text = {
            DanaIPumpUnbindDialogContent(
                isLoading = isLoading,
                isSaving = isSaving,
                error = error,
                setLightEnabled = setLightEnabled,
                onSetLightEnabledChange = { setLightEnabled = it },
                backlightTime = backlightTime,
                onBacklightTimeChange = { backlightTime = it }
            )
        },
        confirmButton = {
            PrimaryButton(
                onClick = {
                    if (setLightEnabled && userOptions != null) {
                        onSaveAndUnbind(userOptions.copy(backlightOnTimeSec = backlightTime))
                    } else {
                        onUnbindOnly()
                    }
                },
                enabled = !isSaving && !isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                )
            ) {
                Text(stringResource(R.string.danai_unbind_dialog_btn_confirm))
            }
        },
        dismissButton = {
            NormalTextButton(onClick = onDismiss, enabled = !isSaving && !isLoading) {
                Text(stringResource(R.string.danai_unbind_dialog_btn_cancel))
            }
        }
    )
}

/**
 * Pure presentation content for the unbind dialog.
 *
 * Extracted into a standalone composable so it can be previewed without an [AlertDialog] host window.
 */
@Composable
fun DanaIPumpUnbindDialogContent(
    isLoading: Boolean,
    isSaving: Boolean,
    error: String?,
    setLightEnabled: Boolean,
    onSetLightEnabledChange: (Boolean) -> Unit,
    backlightTime: Int,
    onBacklightTimeChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val lightTimeOptions = remember { listOf(0, 10, 30, 60) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = stringResource(R.string.danai_unbind_dialog_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (isLoading || isSaving) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(vertical = 12.dp)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
                Text(
                    text = if (isSaving) stringResource(R.string.danai_unbind_dialog_saving) else stringResource(R.string.danai_unbind_dialog_loading),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        error?.let { err ->
            WarningBanner(warningText = err, onActionClick = {})
        }

        HorizontalDivider()

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Checkbox(
                checked = setLightEnabled,
                onCheckedChange = onSetLightEnabledChange,
                enabled = !isSaving && !isLoading
            )
            Text(
                text = stringResource(R.string.danai_unbind_dialog_enable_set_light),
                style = MaterialTheme.typography.bodyMedium
            )
        }

        if (setLightEnabled) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = stringResource(R.string.danai_unbind_dialog_backlight_time),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    lightTimeOptions.forEach { sec ->
                        FilterChip(
                            selected = (backlightTime == sec),
                            onClick = { onBacklightTimeChange(sec) },
                            enabled = !isSaving && !isLoading,
                            label = {
                                Text(
                                    if (sec == 0) stringResource(R.string.danai_unbind_dialog_off)
                                    else stringResource(R.string.danai_unbind_dialog_seconds_format, sec)
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun DanaIPumpUnbindDialogPreview() {
    AppPreview {
        DanaIPumpUnbindDialog(
            userOptions = DanaRsUserOptions(
                hwModel = 7,
                timeDisplayType24 = true,
                buttonScrollOnOff = true,
                beepAndAlarm = 3,
                lcdOnTimeSec = 10,
                backlightOnTimeSec = 30,
                selectedLanguage = 0,
                units = DanaGlucoseUnits.MGDL,
                shutdownHour = 0,
                lowReservoirRate = 10,
                cannulaVolume = 50,
                refillAmount = 300,
                target = 100
            ),
            isLoading = false,
            isSaving = false,
            error = null,
            onSaveAndUnbind = { _ -> },
            onUnbindOnly = { },
            onDismiss = { }
        )
    }
}