package de.dh.daps.ui.controls.apscontrol

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.dh.daps.common.domain.model.engine.ApsMode
import de.dh.daps.ui.R
import de.dh.daps.ui.common.composables.AppAlertDialog
import de.dh.daps.ui.common.composables.NormalTextButton
import de.dh.daps.ui.common.composables.PrimaryButton
import de.dh.daps.ui.common.theme.AppPreview
import de.dh.daps.ui.common.theme.SoftBlue
import de.dh.daps.ui.common.theme.SoftGreen
import de.dh.daps.ui.common.theme.SoftRed

@Composable
fun ApsModeSelectionDialog(
    selectedMode: ApsMode,
    availableModes: List<ApsMode>,
    onModeChange: (ApsMode) -> Unit,
    onDismissRequest: () -> Unit
) {
    AppAlertDialog(
        onDismissRequest = onDismissRequest,
        titleText = stringResource(R.string.aps_mode_dialog_title),
        content = {
            val modes = ApsMode.entries
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                modes.forEachIndexed { index, mode ->
                    val isAvailable = mode in availableModes
                    ModeOption(
                        mode = mode,
                        isSelected = mode == selectedMode,
                        isAvailable = isAvailable,
                        onClick = {
                            onModeChange(mode)
                            onDismissRequest()
                        }
                    )
                    if (index < modes.size - 1) {
                        HorizontalDivider(
                            modifier = Modifier.padding(top = 8.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        },
        confirmButton = {
            NormalTextButton(onClick = onDismissRequest) {
                Text(stringResource(android.R.string.cancel))
            }
        }
    )
}

@Composable
private fun ModeOption(
    mode: ApsMode,
    isSelected: Boolean,
    isAvailable: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = if (!isAvailable) {
                when (mode) {
                    ApsMode.AutoCorrection -> stringResource(R.string.aps_mode_requires_glucose_source_and_pump)
                    else -> mode.toDescriptionString()
                }
            } else {
                mode.toDescriptionString()
            },
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            color = if (isAvailable) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
        PrimaryButton(
            onClick = onClick,
            enabled = isAvailable,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = when (mode) {
                    ApsMode.AutoCorrection -> SoftGreen
                    ApsMode.Manual -> SoftBlue
                    ApsMode.Suspend -> SoftRed
                }
            ),
            border = if (isSelected) {
                BorderStroke(6.dp, MaterialTheme.colorScheme.primary)
            } else null
        ) {
            Text(
                text = mode.toDisplayStringFull(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}

@Composable
private fun ApsMode.toDisplayStringFull(): String = stringResource(id = when (this) {
    ApsMode.Manual -> R.string.aps_mode_manual
    ApsMode.Suspend -> R.string.aps_mode_suspend
    ApsMode.AutoCorrection -> R.string.aps_mode_auto_correction
})

@Composable
private fun ApsMode.toDescriptionString(): String = stringResource(id = when (this) {
    ApsMode.Manual -> R.string.aps_mode_manual_desc
    ApsMode.Suspend -> R.string.aps_mode_suspend_desc
    ApsMode.AutoCorrection -> R.string.aps_mode_auto_correction_desc
})

@Preview(showBackground = true, showSystemUi = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, showSystemUi = true)
@Composable
private fun PreviewApsModeSelectionContent() {
    AppPreview(modifier = Modifier.padding(16.dp)) {
        ApsModeSelectionDialog(
            selectedMode = ApsMode.AutoCorrection,
            availableModes = ApsMode.entries,
            onModeChange = {},
            onDismissRequest = {}
        )
    }
}