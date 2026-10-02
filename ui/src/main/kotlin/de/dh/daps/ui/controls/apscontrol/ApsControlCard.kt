package de.dh.daps.ui.controls.apscontrol

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Adjust
import androidx.compose.material.icons.filled.VerticalAlignBottom
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.dh.daps.common.model.ApsMode
import de.dh.daps.common.model.InsulinAmount
import de.dh.daps.common.model.data.BgDelta
import de.dh.daps.common.model.data.BgValue
import de.dh.daps.common.model.data.Minutes
import de.dh.daps.ui.R
import de.dh.daps.ui.common.ConfigurableDisplayStrategy
import de.dh.daps.ui.common.composables.FramedCard
import de.dh.daps.ui.common.composables.NormalButton
import de.dh.daps.ui.common.composables.PrimaryButton
import de.dh.daps.ui.common.glucoseValue
import de.dh.daps.ui.common.icons.Icon_Alarms
import de.dh.daps.ui.common.icons.Icon_Insulin_Adjustment
import de.dh.daps.ui.common.icons.Icon_Insulin_Profile
import de.dh.daps.ui.common.theme.AppPreview
import de.dh.daps.ui.common.theme.NeutralGrey
import de.dh.daps.ui.common.theme.SoftBlue
import de.dh.daps.ui.common.theme.SoftGreen
import de.dh.daps.ui.common.theme.SoftRed
import de.dh.daps.ui.screens.therapy.ActiveTherapyStatusUiState
import de.dh.daps.ui.screens.therapy.InsulinProfileUiState
import de.dh.daps.ui.screens.therapy.TherapyAdjustmentUiState

@Composable
fun ApsControlCard(
    modifier: Modifier = Modifier,
    activeTherapyStatus: ActiveTherapyStatusUiState,
    selectedMode: ApsMode,
    availableModes: List<ApsMode>,
    onModeChange: (ApsMode) -> Unit,
    onAdjustmentClick: () -> Unit,
    onProfileClick: () -> Unit,
    onManualControlClick: () -> Unit = {}
) {
    val insulinAdjustmentPercentage = activeTherapyStatus.adjustment.percentage
    val adjustmentHint = activeTherapyStatus.adjustment.adjustmentHint
    val isSuspended = selectedMode == ApsMode.Suspend
    val displayStrategy = ConfigurableDisplayStrategy(
        positiveColor = SoftRed,
        negativeColor = SoftBlue,
        neutralColor = NeutralGrey,
        positivePrefix = "+",
        suffix = "%",
        neutralLabel = stringResource(R.string.aps_control_adjustment_neutral)
    )

    FramedCard(
        modifier = modifier.height(IntrinsicSize.Min)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Info Column (left)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onProfileClick() }
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 1. Insulinprofile
                val nameText = if (insulinAdjustmentPercentage != 0) {
                    "${activeTherapyStatus.profile.name} (${displayStrategy.format(insulinAdjustmentPercentage.toDouble())})"
                } else {
                    activeTherapyStatus.profile.name
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icon_Insulin_Profile,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = nameText.ifEmpty { "-" },
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                }

                // 2. (Temp-)Basalrate aus dem PumpManager-Basalstatus
                CompositionLocalProvider(
                    LocalContentColor provides if (isSuspended) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f) else LocalContentColor.current
                ) {
                    val basalStatus = activeTherapyStatus.basalStatus
                    val isTempBasal = basalStatus?.isTempBasal == true
                    val rateValue = if (isSuspended || basalStatus?.isSuspended == true) {
                        0.0
                    } else {
                        basalStatus?.activeRate?.iu ?: activeTherapyStatus.currentBasal.iu
                    }
                    val basalValue = String.format(LocalLocale.current.platformLocale, "%.1f", rateValue)
                    val basalLabelRes = if (isTempBasal) R.string.aps_control_temp_basal_label else R.string.aps_control_basal_label

                    Surface(
                        color = if (isSuspended) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.secondaryContainer,
                        shape = MaterialTheme.shapes.extraSmall,
                    ) {
                        Text(
                            text = " ${stringResource(basalLabelRes, basalValue)} ",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }

                // 3. BG target and low threshold
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Target
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Adjust,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = glucoseValue(activeTherapyStatus.target),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Low
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.VerticalAlignBottom,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = glucoseValue(activeTherapyStatus.lowThreshold),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // 4. Active alarm profile
                val alarmProfileName = activeTherapyStatus.activeAlarmProfileName
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icon_Alarms,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = alarmProfileName ?: "-",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            VerticalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                thickness = 1.dp,
                color = Color.Gray.copy(alpha = 0.3f)
            )

            // Buttons Column (right)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Mode Button
                var showModeDialog by remember { mutableStateOf(false) }
                Box {
                    PrimaryButton(
                        onClick = { showModeDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = when (selectedMode) {
                                ApsMode.AutoCorrection -> SoftGreen
                                ApsMode.OnlySuggestions -> SoftBlue
                                ApsMode.Suspend -> SoftRed
                            }
                        )
                    ) {
                        Text(
                            text = selectedMode.toDisplayStringShort(),
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center
                        )
                    }

                    if (showModeDialog) {
                        ApsModeSelectionDialog(
                            selectedMode = selectedMode,
                            availableModes = availableModes,
                            onModeChange = onModeChange,
                            onDismissRequest = { showModeDialog = false }
                        )
                    }
                }

                if (selectedMode != ApsMode.AutoCorrection) {
                    NormalButton(
                        onClick = onManualControlClick,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = stringResource(R.string.aps_control_button_manual_control),
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    // Adjustment Button
                    val isNeutral = insulinAdjustmentPercentage == 0
                    val adjustmentText = buildString {
                        if (adjustmentHint != null) {
                            append(adjustmentHint)
                            append(" (")
                        }
                        append(displayStrategy.format(insulinAdjustmentPercentage.toDouble()))
                        if (adjustmentHint != null) {
                            append(")")
                        }
                    }

                    if (isNeutral && adjustmentHint == null) {
                        NormalButton(
                            onClick = onAdjustmentClick,
                            enabled = !isSuspended,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icon_Insulin_Adjustment, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(adjustmentText, style = MaterialTheme.typography.titleMedium)
                        }
                    } else {
                        PrimaryButton(
                            onClick = onAdjustmentClick,
                            enabled = !isSuspended,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = displayStrategy.color(insulinAdjustmentPercentage.toDouble())
                            )
                        ) {
                            Icon(Icon_Insulin_Adjustment, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                adjustmentText,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ApsMode.toDisplayStringShort(): String = stringResource(id = when (this) {
    ApsMode.OnlySuggestions -> R.string.aps_mode_only_suggestions_short
    ApsMode.Suspend -> R.string.aps_mode_suspend_short
    ApsMode.AutoCorrection -> R.string.aps_mode_auto_correction_short
})

private fun createSampleTherapyStatus() = ActiveTherapyStatusUiState(
    profile = InsulinProfileUiState(
        name = "Normal",
        activeProfileId = null,
        isfRange = "50",
        crRange = "12.0",
        basalRange = "0.80",
        dia = Minutes(300),
        peak = Minutes(75)
    ),
    adjustment = TherapyAdjustmentUiState(
        percentage = 0,
        targetBgOverride = null,
        lowThresholdOverride = null,
        adjustmentHint = null
    ),
    currentIsf = BgDelta.fromMgDl(50),
    currentCr = 12.0,
    currentBasal = InsulinAmount(0.8),
    activeAlarmProfileName = "Kino / Diskret",
    target = BgValue.fromMgDl(100),
    lowThreshold = BgValue.fromMgDl(70),
    baseTarget = BgValue.fromMgDl(110),
    baseLow = BgValue.fromMgDl(70)
)

@Preview(name = "AutoCorrection - Light", showBackground = true)
@Preview(name = "AutoCorrection - Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewApsControlCardAutoCorrection() {
    AppPreview {
        ApsControlCard(
            modifier = Modifier.padding(16.dp),
            activeTherapyStatus = createSampleTherapyStatus(),
            selectedMode = ApsMode.AutoCorrection,
            availableModes = ApsMode.entries,
            onModeChange = {},
            onAdjustmentClick = {},
            onProfileClick = {}
        )
    }
}

@Preview(name = "OnlySuggestions - Light", showBackground = true)
@Preview(name = "OnlySuggestions - Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewApsControlCardOnlySuggestions() {
    AppPreview {
        ApsControlCard(
            modifier = Modifier.padding(16.dp),
            activeTherapyStatus = createSampleTherapyStatus(),
            selectedMode = ApsMode.OnlySuggestions,
            availableModes = ApsMode.entries,
            onModeChange = {},
            onAdjustmentClick = {},
            onProfileClick = {}
        )
    }
}

@Preview(name = "Suspend - Light", showBackground = true)
@Preview(name = "Suspend - Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewApsControlCardSuspend() {
    AppPreview {
        ApsControlCard(
            modifier = Modifier.padding(16.dp),
            activeTherapyStatus = createSampleTherapyStatus(),
            selectedMode = ApsMode.Suspend,
            availableModes = ApsMode.entries,
            onModeChange = {},
            onAdjustmentClick = {},
            onProfileClick = {}
        )
    }
}