package de.dh.raaps.ui.screens.therapy

import android.content.res.Configuration
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Adjust
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material.icons.filled.VerticalAlignBottom
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.FilterChip
import de.dh.raaps.common.model.data.AlarmProfile
import de.dh.raaps.common.model.ADJUSTMENT_PERCENTAGE_MAX
import de.dh.raaps.common.model.ADJUSTMENT_PERCENTAGE_MIN
import de.dh.raaps.common.model.LOW_THRESHOLD_MAX
import de.dh.raaps.common.model.LOW_THRESHOLD_MIN
import de.dh.raaps.common.model.TARGET_MAX
import de.dh.raaps.common.model.TARGET_MIN
import de.dh.raaps.common.model.data.BgValue
import de.dh.raaps.common.model.data.GlucoseUnit
import de.dh.raaps.ui.R
import de.dh.raaps.ui.common.ConfigurableDisplayStrategy
import de.dh.raaps.ui.common.LocalGlucoseUnit
import de.dh.raaps.ui.common.ModuloSteppingStrategy
import de.dh.raaps.ui.common.composables.EditableValueStepper
import de.dh.raaps.ui.common.composables.StepperDefaults
import de.dh.raaps.ui.common.composables.contentScrollIndicator
import de.dh.raaps.ui.common.composables.screenTitle
import de.dh.raaps.ui.common.glucoseUnitLabel
import de.dh.raaps.ui.common.glucoseValue
import de.dh.raaps.ui.common.theme.AppTheme
import de.dh.raaps.ui.common.theme.NeutralGrey
import de.dh.raaps.ui.common.theme.SoftBlue
import de.dh.raaps.ui.common.theme.SoftRed
import de.dh.raaps.common.R as CommonR

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TherapyAdjustmentScreen(
    viewModel: CurrentTherapyViewModel,
    onNavigateUp: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val activeTherapyStatus = uiState.activeTherapyStatus

    TherapyAdjustmentContent(
        currentPercentage = activeTherapyStatus.adjustment.percentage,
        currentTarget = activeTherapyStatus.adjustment.targetBgOverride,
        currentLow = activeTherapyStatus.adjustment.lowThresholdOverride,
        currentAlarmProfileId = activeTherapyStatus.adjustment.activeAlarmProfileId,
        baseTarget = activeTherapyStatus.baseTarget,
        baseLow = activeTherapyStatus.baseLow,
        onValuesChange = { p, t, l, a ->
            viewModel.setTherapyAdjustment(p, t, l, a, null)
        },
        availableAlarmProfiles = uiState.availableAlarmProfiles,
        presets = uiState.therapyAdjustmentPresets,
        onPresetApplied = { p, t, l, a, n ->
            // Avoid "neutral" adjustment hint for neutral settings
            val hint = if (p == 0 && t == null && l == null && a == null) null else n
            viewModel.setTherapyAdjustment(p, t, l, a, hint)
            onNavigateUp()
        },
        onNavigateUp = onNavigateUp
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TherapyAdjustmentContent(
    currentPercentage: Int,
    currentTarget: BgValue?,
    currentLow: BgValue?,
    currentAlarmProfileId: Long?,
    baseTarget: BgValue,
    baseLow: BgValue,
    onValuesChange: (Int, BgValue?, BgValue?, Long?) -> Unit,
    onPresetApplied: (Int, BgValue?, BgValue?, Long?, String?) -> Unit,
    onNavigateUp: () -> Unit,
    availableAlarmProfiles: List<AlarmProfile> = emptyList(),
    presets: List<TherapyAdjustment> = emptyList()
) {
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            MediumTopAppBar(
                title = screenTitle(stringResource(id = R.string.aps_control_therpay_adjustment_dialog_title)),
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(id = CommonR.string.cd_navigate_up)
                        )
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
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

            val focusManager = LocalFocusManager.current

            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .contentScrollIndicator(scrollState)
                    .verticalScroll(scrollState)
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) {
                        focusManager.clearFocus()
                    }
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Spacer(modifier = Modifier.height(8.dp))

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
                        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Target BG Tile
                        AdjustmentTile(
                            modifier = Modifier.weight(1f).fillMaxHeight(),
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
                            modifier = Modifier.weight(1f).fillMaxHeight(),
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
                                    onClick = {
                                        onPresetApplied(
                                            preset.percentage,
                                            preset.targetBgMgDl?.let { BgValue.fromMgDl(it.toInt()) },
                                            preset.lowThresholdMgDl?.let { BgValue.fromMgDl(it.toInt()) },
                                            preset.activeAlarmProfileId,
                                            preset.name
                                        )
                                    },
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
            indication = null // Subtler or no ripple to avoid visual clutter in small tiles
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
                    .clickable(enabled = false) { /* stop propagation if needed */ },
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

@Preview(showBackground = true, name = "Light Mode")
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, name = "Dark Mode")
@Composable
private fun TherapyAdjustmentPreviewValues() {
    AppTheme {
        CompositionLocalProvider(LocalGlucoseUnit provides GlucoseUnit.MG_DL) {
            Surface {
                TherapyAdjustmentContent(
                    currentPercentage = -10,
                    currentTarget = BgValue.fromMgDl(120),
                    currentLow = BgValue.fromMgDl(80),
                    currentAlarmProfileId = null,
                    baseTarget = BgValue.fromMgDl(100),
                    baseLow = BgValue.fromMgDl(70),
                    onValuesChange = { _, _, _, _ -> },
                    onPresetApplied = { _, _, _, _, _ -> },
                    onNavigateUp = {},
                    presets = listOf(
                        TherapyAdjustment("Fahrrad fahren", percentage = -30, targetBgMgDl = 150, lowThresholdMgDl = 100),
                        TherapyAdjustment("Stress", percentage = 20, targetBgMgDl = 115, lowThresholdMgDl = 75)
                    )
                )
            }
        }
    }
}

@Preview(showBackground = true, name = "Light Mode")
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, name = "Dark Mode")
@Composable
private fun TherapyAdjustmentPreviewEmpty() {
    AppTheme {
        CompositionLocalProvider(LocalGlucoseUnit provides GlucoseUnit.MG_DL) {
            Surface {
                TherapyAdjustmentContent(
                    currentPercentage = 0,
                    currentTarget = null,
                    currentLow = null,
                    currentAlarmProfileId = null,
                    baseTarget = BgValue.fromMgDl(100),
                    baseLow = BgValue.fromMgDl(70),
                    onValuesChange = { _, _, _, _ -> },
                    onPresetApplied = { _, _, _, _, _ -> },
                    onNavigateUp = {},
                    presets = listOf(
                        TherapyAdjustment("Fahrrad fahren", percentage = -30, targetBgMgDl = 150, lowThresholdMgDl = 100),
                        TherapyAdjustment("Stress", percentage = 20, targetBgMgDl = 115, lowThresholdMgDl = 75)
                    )
                )
            }
        }
    }
}