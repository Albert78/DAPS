package de.dh.raaps.ui.screens.meals

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.dh.raaps.common.model.CarbCurveComponentData
import de.dh.raaps.common.model.ID_MEAL_STANDARD
import de.dh.raaps.common.model.MealType
import de.dh.raaps.common.model.data.Minutes
import de.dh.raaps.ui.R
import de.dh.raaps.ui.common.composables.PrimaryButton
import de.dh.raaps.ui.common.composables.contentScrollIndicator
import de.dh.raaps.ui.common.composables.screenTitle
import de.dh.raaps.ui.common.theme.AppTheme
import de.dh.raaps.common.R as CommonR

@Composable
fun MealTypeEditorScreen(
    viewModel: MealTypeEditorViewModel,
    onNavigateUp: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    MealTypeEditorContent(
        uiState = uiState,
        onNameChange = viewModel::onNameChange,
        onSymbolChange = viewModel::onSymbolChange,
        onCatChange = viewModel::onCatChange,
        onComponentsChange = viewModel::onComponentsChange,
        onSave = { viewModel.save(onNavigateUp) },
        onNavigateUp = onNavigateUp
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MealTypeEditorContent(
    uiState: MealTypeEditorUiState,
    onNameChange: (String) -> Unit,
    onSymbolChange: (String) -> Unit,
    onCatChange: (String) -> Unit,
    onComponentsChange: (List<CarbCurveComponentData>) -> Unit,
    onSave: () -> Unit,
    onNavigateUp: () -> Unit
) {
    Scaffold(
        modifier = Modifier.imePadding(),
        topBar = {
            TopAppBar(
                title = screenTitle(
                    if (uiState.id == null) stringResource(R.string.meal_type_editor_title_new)
                    else stringResource(R.string.meal_type_editor_title_edit)
                ),
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(id = CommonR.string.cd_navigate_up)
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = onSave,
                        enabled = uiState.isValid && !uiState.isSaving
                    ) {
                        Icon(imageVector = Icons.Default.Save, contentDescription = "Speichern")
                    }
                }
            )
        }
    ) { innerPadding ->
        val listState = rememberLazyListState()

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .contentScrollIndicator(listState),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                OutlinedTextField(
                    value = uiState.name,
                    onValueChange = onNameChange,
                    label = { Text(stringResource(R.string.meal_type_name_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
                )
            }

            item {
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        val previewMealType = remember(uiState.id, uiState.name, uiState.symbol) {
                            MealType(
                                id = uiState.id ?: "",
                                name = uiState.name,
                                symbol = uiState.symbol,
                                components = listOf(CarbCurveComponentData(100, Minutes(30))),
                                cat = Minutes(180)
                            )
                        }

                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(56.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                MealTypeIcon(
                                    mealType = previewMealType,
                                    modifier = Modifier.size(32.dp),
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }

                        if (previewMealType.isStandardMealType()) {
                            Text(
                                text = stringResource(R.string.meal_type_symbol_standard_is_fixed),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    hyphens = Hyphens.Auto
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f)
                            )
                        } else {
                            Spacer(modifier = Modifier.width(8.dp))

                            OutlinedTextField(
                                value = uiState.symbol ?: "",
                                onValueChange = { newValue ->
                                    if (newValue.length <= 1) {
                                        onSymbolChange(newValue)
                                    }
                                },
                                textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center),
                                singleLine = true,
                                modifier = Modifier.width(64.dp),
                                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters)
                            )

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.meal_type_symbol_label),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = stringResource(R.string.meal_type_symbol_subtitle),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = uiState.cat,
                    onValueChange = { newVal ->
                        if (newVal.all { it.isDigit() }) {
                            onCatChange(newVal)
                        }
                    },
                    label = { Text(stringResource(R.string.meal_type_cat_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = stringResource(R.string.meal_type_components_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    val totalWeight = uiState.components.sumOf { it.weight }
                    if (totalWeight != 100) {
                        Text(
                            text = stringResource(R.string.error_meal_type_weights_sum),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            itemsIndexed(uiState.components) { index, component ->
                ComponentItem(
                    component = component,
                    onUpdate = { updated ->
                        val newList = uiState.components.toMutableList()
                        newList[index] = updated
                        onComponentsChange(newList)
                    },
                    onDelete = {
                        val newList = uiState.components.toMutableList()
                        newList.removeAt(index)
                        onComponentsChange(newList)
                    }
                )
            }

            item {
                PrimaryButton(
                    onClick = {
                        val newList = uiState.components.toMutableList()
                        newList.add(CarbCurveComponentData(0, Minutes(60)))
                        onComponentsChange(newList)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Komponente hinzufügen")
                }
            }
        }
    }
}

@Composable
fun ComponentItem(
    component: CarbCurveComponentData,
    onUpdate: (CarbCurveComponentData) -> Unit,
    onDelete: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = if (component.weight == 0) "" else component.weight.toString(),
                onValueChange = { newVal ->
                    if (newVal.all { it.isDigit() }) {
                        val weight = if (newVal.isEmpty()) 0 else (newVal.toIntOrNull() ?: component.weight)
                        onUpdate(component.copy(weight = weight))
                    }
                },
                label = { Text(stringResource(R.string.meal_type_weight_label)) },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            OutlinedTextField(
                value = if (component.peakMinutes.value == 0.toShort()) "" else component.peakMinutes.value.toString(),
                onValueChange = { newVal ->
                    if (newVal.all { it.isDigit() }) {
                        val peak = if (newVal.isEmpty()) 0 else (newVal.toIntOrNull() ?: component.peakMinutes.value.toInt())
                        onUpdate(component.copy(peakMinutes = Minutes(peak.toShort())))
                    }
                },
                label = { Text(stringResource(R.string.meal_type_peak_label)) },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Löschen")
            }
        }
    }
}

@Preview(showBackground = true, name = "Light Mode")
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, name = "Dark Mode")
@Composable
fun MealTypeEditorPreview() {
    AppTheme {
        MealTypeEditorContent(
            uiState = MealTypeEditorUiState(
                name = "Normale Mahlzeit",
                cat = "180",
                components = listOf(
                    CarbCurveComponentData(60, Minutes(30)),
                    CarbCurveComponentData(40, Minutes(90))
                )
            ),
            onNameChange = {},
            onSymbolChange = {},
            onCatChange = {},
            onComponentsChange = {},
            onSave = {},
            onNavigateUp = {}
        )
    }
}

@Preview(showBackground = true, name = "Standard Meal Type Editor")
@Composable
fun StandardMealTypeEditorPreview() {
    AppTheme {
        MealTypeEditorContent(
            uiState = MealTypeEditorUiState(
                id = ID_MEAL_STANDARD,
                name = "Standard Mahlzeit",
                cat = "240",
                components = listOf(
                    CarbCurveComponentData(70, Minutes(75)),
                    CarbCurveComponentData(30, Minutes(150))
                )
            ),
            onNameChange = {},
            onSymbolChange = {},
            onCatChange = {},
            onComponentsChange = {},
            onSave = {},
            onNavigateUp = {}
        )
    }
}