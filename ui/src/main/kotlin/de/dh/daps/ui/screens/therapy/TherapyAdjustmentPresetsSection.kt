package de.dh.daps.ui.screens.therapy

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.dh.daps.common.model.data.BgValue
import de.dh.daps.common.model.data.GlucoseUnit
import de.dh.daps.ui.R
import de.dh.daps.ui.common.ConfigurableDisplayStrategy
import de.dh.daps.ui.common.LocalGlucoseUnit
import de.dh.daps.ui.common.glucoseValue
import de.dh.daps.ui.common.theme.AppTheme
import de.dh.daps.ui.common.theme.NeutralGrey
import de.dh.daps.ui.common.theme.SoftBlue
import de.dh.daps.ui.common.theme.SoftRed

/**
 * Shared reusable quick-selection (presets) component for therapy adjustments.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TherapyAdjustmentPresetsSection(
    presets: List<TherapyAdjustment>,
    onPresetApplied: (TherapyAdjustment) -> Unit,
    modifier: Modifier = Modifier
) {
    if (presets.isEmpty()) return

    val displayStrategyInsulin = remember {
        ConfigurableDisplayStrategy(
            positiveColor = SoftRed,
            negativeColor = SoftBlue,
            neutralColor = NeutralGrey,
            positivePrefix = "+"
        )
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = stringResource(R.string.therapy_adjustment_presets_title),
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

@Preview(showBackground = true, name = "Presets Section")
@Composable
private fun TherapyAdjustmentPresetsSectionPreview() {
    AppTheme {
        CompositionLocalProvider(LocalGlucoseUnit provides GlucoseUnit.MG_DL) {
            Surface(modifier = Modifier.padding(16.dp)) {
                TherapyAdjustmentPresetsSection(
                    presets = listOf(
                        TherapyAdjustment("Fahrrad fahren", percentage = -30, targetBgMgDl = 150, lowThresholdMgDl = 100),
                        TherapyAdjustment("Stress", percentage = 20, targetBgMgDl = 115, lowThresholdMgDl = 75)
                    ),
                    onPresetApplied = {}
                )
            }
        }
    }
}