package de.dh.daps.ui.common.composables

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import de.dh.daps.common.model.BOLUS_MAX
import de.dh.daps.ui.common.SteppingStrategy
import de.dh.daps.ui.common.ValueDisplayStrategy
import de.dh.daps.ui.common.insulinSteppingStrategy
import de.dh.daps.ui.common.insulinUnitLabel
import java.util.Locale
import kotlin.math.round

/**
 * Specialized [EditableValueStepper] tailored for insulin amounts (e.g. bolus values in IU / IE).
 *
 * Always rounds the raw initial [currentValue] to two decimal places before passing it
 * to the underlying stepper control. Uses [insulinSteppingStrategy] and formats values with
 * two decimal places by default, while allowing users to override any parameter if needed.
 */
@Composable
fun InsulinAmountStepper(
    currentValue: Double,
    onValueChange: (Double) -> Unit,
    modifier: Modifier = Modifier,
    minValue: Double = 0.0,
    maxValue: Double = BOLUS_MAX,
    steppingStrategy: SteppingStrategy = insulinSteppingStrategy(),
    displayStrategy: ValueDisplayStrategy = remember {
        object : ValueDisplayStrategy {
            override fun format(value: Double): String =
                String.format(Locale.getDefault(), "%.2f", value)

            override fun color(value: Double): Color = Color.Unspecified
        }
    },
    suffix: String = " ${insulinUnitLabel()}",
    style: StepperStyle = StepperDefaults.defaultStyle()
) {
    val roundedValue = round(currentValue * 100.0) / 100.0

    EditableValueStepper(
        currentValue = roundedValue,
        onValueChange = onValueChange,
        modifier = modifier,
        minValue = minValue,
        maxValue = maxValue,
        steppingStrategy = steppingStrategy,
        displayStrategy = displayStrategy,
        suffix = suffix,
        style = style
    )
}