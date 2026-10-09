package de.dh.daps.ui.common.composables

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import de.dh.daps.common.CARBS_GRAMS_MAX
import de.dh.daps.common.CARBS_GRAMS_MIN
import de.dh.daps.common.model.data.CarbsUnit
import de.dh.daps.ui.common.LocalCarbsUnit
import de.dh.daps.ui.common.ValueDisplayStrategy
import de.dh.daps.ui.common.carbsGramsSteppingStrategy
import de.dh.daps.ui.common.carbsKeSteppingStrategy
import de.dh.daps.ui.common.carbsUnitLabel
import java.util.Locale
import kotlin.math.round

@Composable
fun CarbsValueStepper(
    carbsGrams: Double,
    onValueChange: (Double) -> Unit,
    modifier: Modifier = Modifier,
    minValue: Double = CARBS_GRAMS_MIN,
    maxValue: Double = CARBS_GRAMS_MAX,
    unit: CarbsUnit = LocalCarbsUnit.current,
    style: StepperStyle = StepperDefaults.defaultStyle()
) {
    val unitLabel = carbsUnitLabel(unit)
    val roundedGrams = round(carbsGrams)

    when (unit) {
        CarbsUnit.GRAMS -> {
            val displayStrategyGrams = remember {
                object : ValueDisplayStrategy {
                    override fun format(value: Double): String =
                        String.format(Locale.getDefault(), "%.0f", value)

                    override fun color(value: Double): Color = Color.Unspecified
                }
            }

            EditableValueStepper(
                currentValue = roundedGrams,
                onValueChange = { grams -> onValueChange(round(grams)) },
                modifier = modifier,
                minValue = minValue,
                maxValue = maxValue,
                steppingStrategy = carbsGramsSteppingStrategy(),
                displayStrategy = displayStrategyGrams,
                suffix = " $unitLabel",
                style = style
            )
        }
        CarbsUnit.KE -> {
            val displayStrategyKe = remember {
                object : ValueDisplayStrategy {
                    override fun format(value: Double): String =
                        String.format(Locale.getDefault(), "%.1f", value)

                    override fun color(value: Double): Color = Color.Unspecified
                }
            }

            val carbsKe = roundedGrams / 10.0
            EditableValueStepper(
                currentValue = carbsKe,
                onValueChange = { ke -> onValueChange(round(ke * 10.0)) },
                modifier = modifier,
                minValue = minValue / 10.0,
                maxValue = maxValue / 10.0,
                steppingStrategy = carbsKeSteppingStrategy(),
                displayStrategy = displayStrategyKe,
                suffix = " $unitLabel",
                style = style
            )
        }
    }
}