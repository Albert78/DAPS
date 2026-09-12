package de.dh.raaps.ui.controls.meal

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.dh.raaps.common.model.CarbCurveComponentData
import de.dh.raaps.common.model.ID_MEAL_FAST
import de.dh.raaps.common.model.ID_MEAL_SLOW
import de.dh.raaps.common.model.ID_MEAL_STANDARD
import de.dh.raaps.common.model.MealType
import de.dh.raaps.common.model.data.Minutes
import de.dh.raaps.ui.R
import de.dh.raaps.ui.common.theme.AppTheme
import de.dh.raaps.ui.screens.mealtypes.MealTypeIcon
import de.dh.raaps.common.R as CommonR

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodTypeSelector(
    mealTypes: List<MealType>,
    selectedType: MealType?,
    onTypeSelected: (MealType) -> Unit,
    isMandatory: Boolean = false,
    initialExpanded: Boolean = false
) {
    var expanded by remember { mutableStateOf(initialExpanded) }

    val isError = isMandatory && selectedType == null

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(
            1.dp,
            if (isError) MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
            else MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (isError) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
            else MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
        )
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 4.dp)
                .fillMaxWidth()
        ) {
            if (!expanded) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Always show icons of all meal types, highlight selected
                    mealTypes.forEach { type ->
                        val isSelected = type == selectedType
                        IconButton(
                            onClick = { onTypeSelected(type) },
                            modifier = Modifier.size(40.dp)
                        ) {
                            MealTypeIcon(
                                mealType = type,
                                modifier = Modifier.size(24.dp),
                                tint = if (isSelected)
                                    MaterialTheme.colorScheme.primary
                                else if (isError)
                                    MaterialTheme.colorScheme.error.copy(alpha = 0.6f)
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            )
                        }
                    }

                    if (isError) {
                        Text(
                            text = "!",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    IconButton(onClick = { expanded = true }) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = stringResource(CommonR.string.cd_expand),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.food_type_selector_food_type_label),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                    IconButton(onClick = { expanded = false }) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowUp,
                            contentDescription = stringResource(CommonR.string.cd_collapse),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                FlowRow(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    mealTypes.forEach { type ->
                        FilterChip(
                            selected = (type == selectedType),
                            onClick = {
                                onTypeSelected(type)
                                expanded = false
                            },
                            label = { Text(type.name) },
                            leadingIcon = {
                                MealTypeIcon(
                                    mealType = type,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

private fun getPreviewMealTypes(): List<MealType> = listOf(
    MealType(
        id = ID_MEAL_STANDARD,
        name = "Standard",
        components = listOf(CarbCurveComponentData(100, Minutes(60))),
        cat = Minutes(180)
    ),
    MealType(
        id = ID_MEAL_FAST,
        name = "Schnell",
        components = listOf(CarbCurveComponentData(100, Minutes(30))),
        cat = Minutes(120)
    ),
    MealType(
        id = ID_MEAL_SLOW,
        name = "Langsam",
        components = listOf(CarbCurveComponentData(100, Minutes(90))),
        cat = Minutes(240)
    ),
    MealType(
        id = "custom_m",
        name = "Müsli",
        symbol = "M",
        components = listOf(CarbCurveComponentData(100, Minutes(45))),
        cat = Minutes(150)
    ),
    MealType(
        id = "custom_4",
        name = "Pizza",
        symbol = "4",
        components = listOf(CarbCurveComponentData(100, Minutes(120))),
        cat = Minutes(300)
    )
)

@Preview(name = "Light Mode", showBackground = true)
@Preview(name = "Dark Mode", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun FoodTypeSelectorPreview() {
    val sampleMealTypes = getPreviewMealTypes()
    var selectedType by remember { mutableStateOf<MealType?>(sampleMealTypes[3]) }

    AppTheme {
        Surface {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                FoodTypeSelector(
                    mealTypes = sampleMealTypes,
                    selectedType = selectedType,
                    onTypeSelected = { selectedType = it }
                )
            }
        }
    }
}

@Preview(name = "Expanded State", showBackground = true)
@Composable
private fun FoodTypeSelectorExpandedPreview() {
    val sampleMealTypes = getPreviewMealTypes()
    var selectedType by remember { mutableStateOf<MealType?>(sampleMealTypes[3]) }

    AppTheme {
        Surface {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                FoodTypeSelector(
                    mealTypes = sampleMealTypes,
                    selectedType = selectedType,
                    onTypeSelected = { selectedType = it },
                    initialExpanded = true
                )
            }
        }
    }
}