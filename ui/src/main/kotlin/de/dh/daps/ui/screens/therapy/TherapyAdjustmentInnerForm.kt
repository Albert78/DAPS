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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Adjust
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material.icons.filled.VerticalAlignBottom
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import android.content.res.Configuration
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.dh.daps.common.R as CommonR
import de.dh.daps.common.model.ADJUSTMENT_PERCENTAGE_MAX
import de.dh.daps.common.model.ADJUSTMENT_PERCENTAGE_MIN
import de.dh.daps.common.model.LOW_THRESHOLD_MAX
import de.dh.daps.common.model.LOW_THRESHOLD_MIN
import de.dh.daps.common.model.TARGET_MAX
import de.dh.daps.common.model.TARGET_MIN
import de.dh.daps.common.model.data.AlarmProfile
import de.dh.daps.common.model.data.BgValue
import de.dh.daps.common.model.data.GlucoseUnit
import de.dh.daps.ui.R
import de.dh.daps.ui.common.ConfigurableDisplayStrategy
import de.dh.daps.ui.common.LocalGlucoseUnit
import de.dh.daps.ui.common.ModuloSteppingStrategy
import de.dh.daps.ui.common.composables.EditableValueStepper
import de.dh.daps.ui.common.composables.StepperDefaults
import de.dh.daps.ui.common.glucoseUnitLabel
import de.dh.daps.ui.common.glucoseValue
import de.dh.daps.ui.common.theme.AppTheme
import de.dh.daps.ui.common.theme.NeutralGrey
import de.dh.daps.ui.common.theme.SoftBlue
import de.dh.daps.ui.common.theme.SoftRed

/**
 * Shared reusable form component for the inner section of therapy adjustments
 * (Insulin percentage adjustment, BG target/low overrides, alarm profile).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TherapyAdjustmentInnerForm(
    formState: TherapyAdjustmentFormState,
    baseTarget: BgValue,
    baseLow: BgValue,
    availableAlarmProfiles: List<AlarmProfile>,
    onValuesChange: (percentage: Int, targetBg: BgValue?, lowThreshold: BgValue?, alarmProfileId: Long?, adjustmentHint: String?) -> Unit,
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
    val currentAlarmProfileId = formState.alarmProfileOverrideId
    val currentHint = formState.adjustmentHint

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Description / Reason Section
        val hintActive = !currentHint.isNullOrBlank()
        AdjustmentSection(
            icon = Icons.Default.Edit,
            title = stringResource(R.string.therapy_adjustment_description_label),
            description = stringResource(R.string.therapy_adjustment_description_description),
            isActive = hintActive,
            onClear = {
                onValuesChange(currentPercentage, currentTarget, currentLow, currentAlarmProfileId, null)
            },
            useCardWrapper = false
        ) {
            OutlinedTextField(
                value = currentHint ?: "",
                onValueChange = { text ->
                    val newHint = text.ifBlank { null }
                    onValuesChange(currentPercentage, currentTarget, currentLow, currentAlarmProfileId, newHint)
                },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(stringResource(R.string.therapy_adjustment_description_placeholder))
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        // Insulin Adjustment Section
        val insulinAdjustmentActive = currentPercentage != 0
        AdjustmentSection(
            icon = Icons.Default.UnfoldMore,
            title = stringResource(R.string.therapy_adjustment_insulin_adjustment_label),
            description = stringResource(R.string.therapy_adjustment_insulin_adjustment_description),
            isActive = insulinAdjustmentActive,
            onClear = {
                onValuesChange(0, currentTarget, currentLow, currentAlarmProfileId, currentHint)
            },
            accentColor = if (currentPercentage > 0) SoftRed else SoftBlue
        ) {
            EditableValueStepper(
                currentValue = currentPercentage.toDouble(),
                onValueChange = { onValuesChange(it.toInt(), currentTarget, currentLow, currentAlarmProfileId, currentHint) },
                minValue = ADJUSTMENT_PERCENTAGE_MIN.toDouble(),
                maxValue = ADJUSTMENT_PERCENTAGE_MAX.toDouble(),
                steppingStrategy = steppingStrategyInsulin,
                displayStrategy = displayStrategyInsulin,
                suffix = if (currentPercentage != 0) "%" else ""
            )
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        // BG Override Section
        val bgOverrideActive = currentTarget != null || currentLow != null
        AdjustmentSection(
            icon = Icons.Default.Adjust,
            title = stringResource(R.string.therapy_adjustment_bg_adjustment_label),
            description = stringResource(R.string.therapy_adjustment_bg_adjustment_description),
            isActive = bgOverrideActive,
            onClear = {
                onValuesChange(currentPercentage, null, null, currentAlarmProfileId, currentHint)
            },
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
                            onValuesChange(currentPercentage, baseTarget, currentLow, currentAlarmProfileId, currentHint)
                        } else {
                            onValuesChange(currentPercentage, null, currentLow, currentAlarmProfileId, currentHint)
                        }
                    },
                    accentColor = MaterialTheme.colorScheme.primary
                ) {
                    if (currentTarget != null) {
                        EditableValueStepper(
                            currentValue = currentTarget.mgdl,
                            onValueChange = {
                                val newValue = if (it == 0.0) null else BgValue.fromMgDl(it.toInt())
                                onValuesChange(currentPercentage, newValue, currentLow, currentAlarmProfileId, currentHint)
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
                            onValuesChange(currentPercentage, currentTarget, baseLow, currentAlarmProfileId, currentHint)
                        } else {
                            onValuesChange(currentPercentage, currentTarget, null, currentAlarmProfileId, currentHint)
                        }
                    },
                    accentColor = MaterialTheme.colorScheme.error
                ) {
                    if (currentLow != null) {
                        EditableValueStepper(
                            currentValue = currentLow.mgdl,
                            onValueChange = {
                                val newValue = if (it == 0.0) null else BgValue.fromMgDl(it.toInt())
                                onValuesChange(currentPercentage, currentTarget, newValue, currentAlarmProfileId, currentHint)
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
            title = stringResource(R.string.therapy_adjustment_alarm_profile_label),
            description = stringResource(R.string.therapy_adjustment_alarm_profile_description),
            isActive = alarmProfileActive,
            onClear = {
                onValuesChange(currentPercentage, currentTarget, currentLow, null, currentHint)
            },
            useCardWrapper = false
        ) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = (currentAlarmProfileId == null),
                    onClick = { onValuesChange(currentPercentage, currentTarget, currentLow, null, currentHint) },
                    label = { Text(stringResource(R.string.aps_control_adjustment_standard)) }
                )
                availableAlarmProfiles.forEach { profile ->
                    FilterChip(
                        selected = (currentAlarmProfileId == profile.id),
                        onClick = { onValuesChange(currentPercentage, currentTarget, currentLow, profile.id, currentHint) },
                        label = { Text(profile.name) }
                    )
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
    isActive: Boolean = false,
    onClear: (() -> Unit)? = null,
    useCardWrapper: Boolean = true,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    content: @Composable () -> Unit
) {
    var isExpanded by remember { mutableStateOf(isActive) }

    LaunchedEffect(isActive) {
        isExpanded = isActive
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    isExpanded = !isExpanded
                }
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (isActive) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.weight(1f))
            if (isActive && onClear != null) {
                IconButton(
                    onClick = onClear,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(CommonR.string.cd_delete),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
        if (isExpanded) {
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

@Preview(showBackground = true, name = "Light Mode - All Active", heightDp = 850)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, name = "Dark Mode - All Active", heightDp = 850)
@Composable
private fun TherapyAdjustmentInnerFormPreview() {
    AppTheme {
        CompositionLocalProvider(LocalGlucoseUnit provides GlucoseUnit.MG_DL) {
            Surface(modifier = Modifier.padding(16.dp)) {
                TherapyAdjustmentInnerForm(
                    formState = TherapyAdjustmentFormState(
                        percentage = -15,
                        targetBgOverride = BgValue.fromMgDl(130),
                        lowThresholdOverride = BgValue.fromMgDl(85),
                        alarmProfileOverrideId = 1L,
                        adjustmentHint = "Fahrrad fahren"
                    ),
                    baseTarget = BgValue.fromMgDl(100),
                    baseLow = BgValue.fromMgDl(70),
                    availableAlarmProfiles = listOf(
                        AlarmProfile(id = 1L, name = "Sport"),
                        AlarmProfile(id = 2L, name = "Schlafen")
                    ),
                    onValuesChange = { _, _, _, _, _ -> }
                )
            }
        }
    }
}

@Preview(showBackground = true, name = "Light Mode - Collapsed Inactive")
@Composable
private fun TherapyAdjustmentInnerFormCollapsedPreview() {
    AppTheme {
        CompositionLocalProvider(LocalGlucoseUnit provides GlucoseUnit.MG_DL) {
            Surface(modifier = Modifier.padding(16.dp)) {
                TherapyAdjustmentInnerForm(
                    formState = TherapyAdjustmentFormState(
                        percentage = 0,
                        targetBgOverride = null,
                        lowThresholdOverride = null,
                        alarmProfileOverrideId = null,
                        adjustmentHint = null
                    ),
                    baseTarget = BgValue.fromMgDl(100),
                    baseLow = BgValue.fromMgDl(70),
                    availableAlarmProfiles = listOf(
                        AlarmProfile(id = 1L, name = "Sport")
                    ),
                    onValuesChange = { _, _, _, _, _ -> }
                )
            }
        }
    }
}