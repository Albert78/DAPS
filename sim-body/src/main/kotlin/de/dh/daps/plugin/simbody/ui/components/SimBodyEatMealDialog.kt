package de.dh.daps.plugin.simbody.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.dh.daps.common.model.ID_MEAL_STANDARD
import de.dh.daps.common.model.MealType
import de.dh.daps.plugin.simbody.BodyModel
import de.dh.daps.plugin.simbody.R
import de.dh.daps.ui.common.carbsGramsSteppingStrategy
import de.dh.daps.ui.common.carbsGramsUnitLabel
import de.dh.daps.ui.common.composables.EditableValueStepper
import de.dh.daps.ui.common.composables.NormalTextButton
import de.dh.daps.ui.common.theme.AppPreview
import de.dh.daps.ui.controls.meal.FoodTypeSelector

@Composable
fun SimBodyEatMealDialog(
    onDismiss: () -> Unit,
    onConfirm: (Double, MealType) -> Unit
) {
    val mealTypes = BodyModel.SIM_MEAL_TYPES
    var carbs by remember { mutableDoubleStateOf(0.0) }
    var selectedType by remember { mutableStateOf(mealTypes.find { it.id == ID_MEAL_STANDARD } ?: mealTypes.first()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dialog_title_eat_meal)) },
        text = {
            SimBodyEatMealDialogContent(
                carbs = carbs,
                onCarbsChange = { carbs = it },
                mealTypes = mealTypes,
                selectedType = selectedType,
                onTypeSelected = { selectedType = it }
            )
        },
        confirmButton = {
            NormalTextButton(onClick = {
                if (carbs > 0) onConfirm(carbs, selectedType)
                onDismiss()
            }) {
                Text(stringResource(R.string.btn_ok))
            }
        },
        dismissButton = {
            NormalTextButton(onClick = onDismiss) {
                Text(stringResource(R.string.btn_cancel))
            }
        }
    )
}

@Composable
fun SimBodyEatMealDialogContent(
    carbs: Double,
    onCarbsChange: (Double) -> Unit,
    mealTypes: List<MealType>,
    selectedType: MealType,
    onTypeSelected: (MealType) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(stringResource(R.string.label_carbs_g, carbsGramsUnitLabel()), style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(16.dp))
        EditableValueStepper(
            currentValue = carbs,
            onValueChange = onCarbsChange,
            minValue = 0.0,
            maxValue = 200.0,
            steppingStrategy = carbsGramsSteppingStrategy(),
            suffix = carbsGramsUnitLabel(),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))
        Text(stringResource(R.string.label_meal_type), style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(8.dp))
        FoodTypeSelector(
            mealTypes = mealTypes,
            selectedType = selectedType,
            onTypeSelected = onTypeSelected
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SimBodyEatMealDialogPreview() {
    val mealTypes = BodyModel.SIM_MEAL_TYPES
    AppPreview {
        SimBodyEatMealDialogContent(
            carbs = 30.0,
            onCarbsChange = {},
            mealTypes = mealTypes,
            selectedType = mealTypes.first(),
            onTypeSelected = {}
        )
    }
}