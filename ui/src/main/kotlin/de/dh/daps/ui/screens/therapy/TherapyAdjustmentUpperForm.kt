package de.dh.daps.ui.screens.therapy

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Adjust
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material.icons.filled.VerticalAlignBottom
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.dh.daps.common.model.ADJUSTMENT_PERCENTAGE_MAX
import de.dh.daps.common.model.ADJUSTMENT_PERCENTAGE_MIN
import de.dh.daps.common.model.LOW_THRESHOLD_MAX
import de.dh.daps.common.model.LOW_THRESHOLD_MIN
import de.dh.daps.common.model.TARGET_MAX
import de.dh.daps.common.model.TARGET_MIN
import de.dh.daps.common.model.data.AlarmProfile
import de.dh.daps.common.model.data.BgValue
import de.dh.daps.ui.R
import de.dh.daps.ui.common.ConfigurableDisplayStrategy
import de.dh.daps.ui.common.ModuloSteppingStrategy
import de.dh.daps.ui.common.composables.EditableValueStepper
import de.dh.daps.ui.common.composables.StepperDefaults
import de.dh.daps.ui.common.glucoseUnitLabel
import de.dh.daps.ui.common.glucoseValue
import de.dh.daps.ui.common.theme.NeutralGrey
import de.dh.daps.ui.common.theme.SoftBlue
import de.dh.daps.ui.common.theme.SoftRed

/**
 * Shared reusable form component for the top section of therapy adjustments
 * (Insulin percentage adjustment, BG target/low overrides, alarm profile, presets).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TherapyAdjustmentUpperForm(
    formState: TherapyAdjustmentFormState,
    baseTarget: BgValue,
    baseLow: BgValue,
    availableAlarmProfiles: List<AlarmProfile>,
    presets: List<TherapyAdjustment>,
    onValuesChange: (percentage: Int, targetBg: BgValue?, lowThreshold: BgValue?, alarmProfileId: Long?) -> Unit,
    onPresetApplied: (TherapyAdjustment) -> Unit,
    modifier: Modifier = Modifier
) {
    val steppingStrategyInsulin = remember { ModuloSteppingStrategy(5.0) }
    val steppingStrategyBg = remember { ModuloSteppingStrategy(5.0) }

    val displayStrategyInsulin = ConfigurableDisplayStrategy(
        positiveColor = SoftRed,
        negativeColor = SoftBlue,
        neutralColor = NeutralGrey,
        positivePrefix = "+",
        neutralLabel = stringResource(R.string.aps_control_adjustment_neutral)
    )

    val currentPercentage = formState.percentage
    val currentTarget = formState.targetBgOverride
    val currentLow = formState.lowThresholdOverride
    val currentAlarmProfileId = formState.activeAlarmProfileId

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Insulin Adjustment Section
        val insulinAdjustmentActive = currentPercentage != 0
        AdjustmentSection(
            icon = Icons.Default.UnfoldMore,
            title = stringResource(R.string.aps_control_therapy_adjustment_dialog_insulin_adjustment_label),
            description = stringResource(R.string.aps_control_therapy_adjustment_dialog_insulin_adjustment_description),
            isActive = insulinAdjustmentActive,
            accentColor = if (currentPercentage > 0) SoftRed else SoftBlue
        ) {
            EditableValueStepper(
                currentValue = currentPercentage.toDouble(),
                onValueChange = { onValuesChange(it.toInt(), currentTarget, currentLow, currentAlarmProfileId) },
                minValue = ADJUSTMENT_PERCENTAGE_MIN.toDouble(),
                maxValue = ADJUSTMENT_PERCENTAGE_MAX.toDouble(),
                steppingStrategy = steppingStrategyInsulin,
                displayStrategy = displayStrategyInsulin,
                suffix = if (currentPercentage != 0) "%" else ""
            )
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        // BG Override Section
        AdjustmentSection(
            icon = Icons.Default.Adjust,
            title = stringResource(R.string.aps_control_therapy_adjustment_dialog_bg_adjustment_label),
            description = stringResource(R.string.aps_control_therapy_adjustment_dialog_bg_adjustment_description),
            useCardWrapper = false
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Max),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Target BG Tile
                AdjustmentTile(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    icon = Icons.Default.Adjust,
                    label = stringResource(R.string.current_therapy_target_label),
                    active = currentTarget != null,
                    onActiveChange = { active ->
                        if (active) {
                            onValuesChange(currentPercentage, baseTarget, currentLow, currentAlarmProfileId)
                        } else {
                            onValuesChange(currentPercentage, null, currentLow, currentAlarmProfileId)
                        }
                    },
                    accentColor = MaterialTheme.colorScheme.primary
                ) {
                    if (currentTarget != null) {
                        EditableValueStepper(
                            currentValue = currentTarget.mgdl,
                            onValueChange = {
                                val newValue = if (it == 0.0) null else BgValue.fromMgDl(it.toInt())
                                onValuesChange(currentPercentage, newValue, currentLow, currentAlarmProfileId)
                            },
                            minValue = TARGET_MIN.toDouble(),
                            maxValue = TARGET_MAX.toDouble(),
                            steppingStrategy = steppingStrategyBg,
                            suffix = glucoseUnitLabel(),
                            style = StepperDefaults.compactStyle()
                        )
                    } else {
                        StandardValueDisplay(baseTarget)
                    }
                }

                // Low Threshold Tile
                AdjustmentTile(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    icon = Icons.Default.VerticalAlignBottom,
                    label = stringResource(R.string.current_therapy_low_threshold_label),
                    active = currentLow != null,
                    onActiveChange = { active ->
                        if (active) {
                            onValuesChange(currentPercentage, currentTarget, baseLow, currentAlarmProfileId)
                        } else {
                            onValuesChange(currentPercentage, currentTarget, null, currentAlarmProfileId)
                        }
                    },
                    accentColor = MaterialTheme.colorScheme.error
                ) {
                    if (currentLow != null) {
                        EditableValueStepper(
                            currentValue = currentLow.mgdl,
                            onValueChange = {
                                val newValue = if (it == 0.0) null else BgValue.fromMgDl(it.toInt())
                                onValuesChange(currentPercentage, currentTarget, newValue, currentAlarmProfileId)
                            },
                            minValue = LOW_THRESHOLD_MIN.toDouble(),
                            maxValue = LOW_THRESHOLD_MAX.toDouble(),
                            steppingStrategy = steppingStrategyBg,
                            suffix = glucoseUnitLabel(),
                            style = StepperDefaults.compactStyle()
                        )
                    } else {
                        StandardValueDisplay(baseLow)
                    }
                }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        // Alarm Profile Section
        val alarmProfileActive = currentAlarmProfileId != null
        AdjustmentSection(
            icon = Icons.Default.Notifications,
            title = stringResource(R.string.aps_control_therapy_adjustment_alarm_profile_label),
            description = stringResource(R.string.aps_control_therapy_adjustment_alarm_profile_description),
            isActive = alarmProfileActive,
            useCardWrapper = false
        ) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = (currentAlarmProfileId == null),
                    onClick = { onValuesChange(currentPercentage, currentTarget, currentLow, null) },
                    label = { Text(stringResource(R.string.aps_control_adjustment_standard)) }
                )
                availableAlarmProfiles.forEach { profile ->
                    FilterChip(
                        selected = (currentAlarmProfileId == profile.id),
                        onClick = { onValuesChange(currentPercentage, currentTarget, currentLow, profile.id) },
                        label = { Text(profile.name) }
                    )
                }
            }
        }

        if (presets.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                Text(
                    text = stringResource(R.string.aps_control_therapy_adjustment_dialog_presets_title),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    presets.forEach { preset ->
                        SuggestionChip(
                            onClick = { onPresetApplied(preset) },
                            label = {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = preset.name,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(
                                            text = displayStrategyInsulin.format(preset.percentage.toDouble()),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = displayStrategyInsulin.color(preset.percentage.toDouble())
                                        )
                                        if (preset.targetBgMgDl != null) {
                                            val targetValue = BgValue.fromMgDl(preset.targetBgMgDl.toInt())
                                            Text(
                                                text = "• ${glucoseValue(targetValue, withUnit = true)}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AdjustmentSection(
    icon: ImageVector,
    title: String,
    description: String,
    useCardWrapper: Boolean = true,
    isActive: Boolean = false,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    content: @Composable () -> Unit
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isActive) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (isActive) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(12.dp))
        if (useCardWrapper) {
            val containerColor = if (isActive) accentColor.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.4f)
            val borderColor = if (isActive) accentColor else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
            val borderWidth = if (isActive) 2.dp else 1.dp

            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.outlinedCardColors(containerColor = containerColor),
                border = BorderStroke(borderWidth, borderColor)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp, horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    content()
                }
            }
        } else {
            content()
        }
    }
}

@Composable
private fun AdjustmentTile(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    active: Boolean,
    onActiveChange: (Boolean) -> Unit,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    content: @Composable () -> Unit
) {
    val containerColor = if (active) accentColor.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.4f)
    val borderColor = if (active) accentColor else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
    val borderWidth = if (active) 2.dp else 1.dp
    val contentColor = if (active) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
    val iconTint = if (active) accentColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)

    OutlinedCard(
        modifier = modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null
        ) {
            onActiveChange(!active)
        },
        colors = CardDefaults.outlinedCardColors(
            containerColor = containerColor,
        ),
        border = BorderStroke(borderWidth, borderColor)
    ) {
        Column(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth()
                .fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = iconTint
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = contentColor,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Normal
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = false) { },
                contentAlignment = Alignment.Center
            ) {
                content()
            }
        }
    }
}

@Composable
private fun StandardValueDisplay(
    value: BgValue
) {
    Column(
        modifier = Modifier.padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = glucoseValue(value, withUnit = true),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = stringResource(R.string.aps_control_adjustment_standard),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}