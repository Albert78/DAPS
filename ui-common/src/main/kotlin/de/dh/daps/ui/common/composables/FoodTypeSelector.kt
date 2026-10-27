package de.dh.daps.ui.common.composables

import android.content.res.Configuration
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import de.dh.daps.common.domain.model.data.Minutes
import de.dh.daps.common.domain.model.therapy.CarbCurveComponentData
import de.dh.daps.common.domain.model.therapy.MealType
import de.dh.daps.common.domain.model.therapy.getDefaultFastMealType
import de.dh.daps.common.domain.model.therapy.getDefaultSlowMealType
import de.dh.daps.common.domain.model.therapy.getDefaultStandardMealType
import de.dh.daps.ui.common.R
import de.dh.daps.ui.common.theme.AppPreview
import de.dh.daps.common.R as CommonR

@Composable
fun FoodTypeSelector(
    mealTypes: List<MealType>,
    selectedType: MealType?,
    onTypeSelected: (MealType) -> Unit,
    isMandatory: Boolean = false,
    initialExpanded: Boolean = false,
    maxExpandedHeight: Dp = 200.dp
) {
    var expanded by remember { mutableStateOf(initialExpanded) }

    val isError = isMandatory && selectedType == null

    FramedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        borderColor = if (isError) MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
            else MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f),
        borderWidth = 1.dp,
        containerColor = if (isError) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
            else MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 4.dp)
                .fillMaxWidth()
        ) {
            if (!expanded) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val scrollState = rememberScrollState()

                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .horizontalContentScrollIndicator(scrollState)
                            .horizontalScroll(scrollState),
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

                val verticalScrollState = rememberScrollState()

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = maxExpandedHeight)
                        .contentScrollIndicator(
                            scrollableState = verticalScrollState,
                            indicatorHeight = 24.dp,
                            iconSize = 24.dp,
                            topIconPadding = 2.dp,
                            bottomIconPadding = 2.dp
                        )
                        .verticalScroll(verticalScrollState)
                        .padding(bottom = 8.dp)
                ) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
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
}



@Composable
private fun getPreviewMealTypes(): List<MealType> {
    val context = LocalContext.current
    return listOf(
        getDefaultFastMealType(context),
        getDefaultStandardMealType(context),
        getDefaultSlowMealType(context),
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
}

@Preview(name = "Light Mode", showBackground = true)
@Preview(name = "Dark Mode", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun FoodTypeSelectorPreview() {
    val sampleMealTypes = getPreviewMealTypes()
    var selectedType by remember { mutableStateOf<MealType?>(sampleMealTypes[3]) }

    AppPreview {
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

@Preview(name = "Scrollable Meal Types", showBackground = true)
@Composable
private fun FoodTypeSelectorScrollablePreview() {
    val sampleMealTypes = getPreviewMealTypes() + getPreviewMealTypes() + getPreviewMealTypes()
    var selectedType by remember { mutableStateOf<MealType?>(sampleMealTypes[0]) }

    AppPreview {
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

@Preview(name = "Expanded State", showBackground = true)
@Composable
private fun FoodTypeSelectorExpandedPreview() {
    val sampleMealTypes = getPreviewMealTypes()
    var selectedType by remember { mutableStateOf<MealType?>(sampleMealTypes[3]) }

    AppPreview {
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

@Preview(name = "Expanded Scrollable Meal Types", showBackground = true)
@Composable
private fun FoodTypeSelectorExpandedScrollablePreview() {
    val sampleMealTypes = getPreviewMealTypes() + getPreviewMealTypes() + getPreviewMealTypes()
    var selectedType by remember { mutableStateOf<MealType?>(sampleMealTypes[0]) }

    AppPreview {
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