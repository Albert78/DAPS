package de.dh.daps.ui.common.composables

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import de.dh.daps.common.model.CARBS_GRAMS_MAX
import de.dh.daps.common.model.CARBS_KE_MAX
import de.dh.daps.common.model.CARBS_KE_MIN
import de.dh.daps.common.model.data.CarbsUnit
import de.dh.daps.ui.common.LocalCarbsUnit
import de.dh.daps.ui.common.carbsGramsSteppingStrategy
import de.dh.daps.ui.common.carbsKeSteppingStrategy
import de.dh.daps.ui.common.carbsUnitLabel

@Composable
fun CarbsValueStepper(
    carbsGrams: Double,
    onValueChange: (Double) -> Unit,
    modifier: Modifier = Modifier,
    unit: CarbsUnit = LocalCarbsUnit.current,
    style: StepperStyle = StepperDefaults.defaultStyle()
) {
    val unitLabel = carbsUnitLabel(unit)

    when (unit) {
        CarbsUnit.GRAMS -> {
            EditableValueStepper(
                currentValue = carbsGrams,
                onValueChange = { grams -> onValueChange(grams) },
                modifier = modifier,
                minValue = 0.0,
                maxValue = CARBS_GRAMS_MAX,
                steppingStrategy = carbsGramsSteppingStrategy(),
                suffix = " $unitLabel",
                style = style
            )
        }
        CarbsUnit.KE -> {
            val carbsKe = carbsGrams / 10.0
            EditableValueStepper(
                currentValue = carbsKe,
                onValueChange = { ke -> onValueChange(ke * 10.0) },
                modifier = modifier,
                minValue = CARBS_KE_MIN,
                maxValue = CARBS_KE_MAX,
                steppingStrategy = carbsKeSteppingStrategy(),
                suffix = " $unitLabel",
                style = style
            )
        }
    }
}