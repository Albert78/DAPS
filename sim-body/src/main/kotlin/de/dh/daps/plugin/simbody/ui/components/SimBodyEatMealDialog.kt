package de.dh.daps.plugin.simbody.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.dh.daps.common.ID_MEAL_STANDARD
import de.dh.daps.common.domain.model.therapy.MealType
import de.dh.daps.plugin.simbody.BodyModel
import de.dh.daps.plugin.simbody.R
import de.dh.daps.ui.common.carbsUnitLabel
import de.dh.daps.ui.common.composables.AppAlertDialog
import de.dh.daps.ui.common.composables.CarbsValueStepper
import de.dh.daps.ui.common.composables.FoodTypeSelector
import de.dh.daps.ui.common.theme.AppPreview

@Composable
fun SimBodyEatMealDialog(
    onDismiss: () -> Unit,
    onConfirm: (Double, MealType) -> Unit
) {
    val context = LocalContext.current
    val mealTypes = remember(context) { BodyModel.getSimMealTypes(context) }
    var carbs by remember { mutableDoubleStateOf(0.0) }
    var selectedType by remember { mutableStateOf(mealTypes.find { it.id == ID_MEAL_STANDARD } ?: mealTypes.first()) }

    AppAlertDialog(
        titleText = stringResource(R.string.dialog_title_eat_meal),
        onDismissRequest = onDismiss,
        onConfirm = {
            if (carbs > 0) onConfirm(carbs, selectedType)
            onDismiss()
        },
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.label_carbs_g, carbsUnitLabel()), style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(16.dp))
            CarbsValueStepper(
                carbsGrams = carbs,
                onValueChange = { carbs = it },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))
            Text(stringResource(R.string.label_meal_type), style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(8.dp))
            FoodTypeSelector(
                mealTypes = mealTypes,
                selectedType = selectedType,
                onTypeSelected = { selectedType = it }
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun SimBodyEatMealDialogPreview() {
    AppPreview {
        SimBodyEatMealDialog(
            onDismiss = {},
            onConfirm = { _, _ -> }
        )
    }
}