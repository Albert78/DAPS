package de.dh.raaps.ui.screens.meals

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.dh.raaps.common.model.CARBS_KE_MAX
import de.dh.raaps.common.model.CARBS_KE_MIN
import de.dh.raaps.common.model.CarbCurveComponentData
import de.dh.raaps.common.model.ID_MEAL_FAST
import de.dh.raaps.common.model.ID_MEAL_SLOW
import de.dh.raaps.common.model.ID_MEAL_STANDARD
import de.dh.raaps.common.model.InsulinAmount
import de.dh.raaps.common.model.MealEntry
import de.dh.raaps.common.model.MealType
import de.dh.raaps.common.model.data.Minutes
import de.dh.raaps.common.model.data.Timestamp
import de.dh.raaps.ui.R
import de.dh.raaps.ui.common.DefaultSteppingStrategy
import de.dh.raaps.ui.common.ValueDisplayStrategy
import de.dh.raaps.ui.common.carbsKeUnitLabel
import de.dh.raaps.ui.common.composables.AbsoluteTimeStepper
import de.dh.raaps.ui.common.composables.AppColorBlue
import de.dh.raaps.ui.common.composables.EditableValueStepper
import de.dh.raaps.ui.common.composables.NormalTextButton
import de.dh.raaps.ui.common.insulinValue
import de.dh.raaps.ui.common.theme.AppTheme
import de.dh.raaps.ui.controls.meal.BolusPlanEditorDialog
import de.dh.raaps.ui.controls.meal.FoodTypeSelector
import de.dh.raaps.ui.controls.meal.PlannedBolusUiModel
import java.util.Locale
import de.dh.raaps.common.R as CommonR

private data class InitialMealValues(
    val carbsKe: Double,
    val timestamp: Timestamp,
    val mealType: MealType?,
    val pendingDeferredBoluses: List<PlannedBolusUiModel>
)

@Composable
fun EditHistoricalMealScreen(
    viewModel: EditHistoricalMealViewModel,
    onNavigateUp: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    EditHistoricalMealContent(
        uiState = uiState,
        onNavigateUp = onNavigateUp,
        onDelete = { viewModel.deleteMeal(onNavigateUp) },
        onCarbsChange = { viewModel.onCarbsChange(it) },
        onTimestampChange = { viewModel.onTimestampChange(it) },
        onMealTypeChange = { viewModel.onMealTypeChange(it) },
        onOpenBolusPlanSheet = { viewModel.onOpenBolusPlanSheet() },
        onCloseBolusPlanSheet = { viewModel.onCloseBolusPlanSheet() },
        onAddDeferredBolus = { viewModel.onAddDeferredBolus() },
        onUpdateDeferredBolusTime = { index, time -> viewModel.onUpdateDeferredBolusTime(index, time) },
        onUpdateDeferredBolusAmount = { index, amount -> viewModel.onUpdateDeferredBolusAmount(index, amount) },
        onRemoveDeferredBolus = { index -> viewModel.onRemoveDeferredBolus(index) },
        onSave = { viewModel.saveChanges(onNavigateUp) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditHistoricalMealContent(
    uiState: EditHistoricalMealUiState,
    onNavigateUp: () -> Unit,
    onDelete: () -> Unit,
    onCarbsChange: (Double) -> Unit,
    onTimestampChange: (Timestamp) -> Unit,
    onMealTypeChange: (MealType) -> Unit,
    onOpenBolusPlanSheet: () -> Unit,
    onCloseBolusPlanSheet: () -> Unit,
    onAddDeferredBolus: () -> Unit,
    onUpdateDeferredBolusTime: (Int, Timestamp) -> Unit,
    onUpdateDeferredBolusAmount: (Int, InsulinAmount) -> Unit,
    onRemoveDeferredBolus: (Int) -> Unit,
    onSave: () -> Unit
) {
    var showDiscardConfirmation by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    val initialValues = remember(uiState.isLoading) {
        if (!uiState.isLoading) {
            InitialMealValues(
                carbsKe = uiState.editedCarbsKe,
                timestamp = uiState.editedTimestamp,
                mealType = uiState.editedMealType,
                pendingDeferredBoluses = uiState.pendingDeferredBoluses
            )
        } else null
    }

    val hasChanges = remember(
        uiState.editedCarbsKe,
        uiState.editedTimestamp,
        uiState.editedMealType,
        uiState.pendingDeferredBoluses,
        initialValues,
        uiState.isAddMode
    ) {
        if (initialValues == null) false
        else if (uiState.isAddMode) {
            uiState.editedCarbsKe > 0.0 ||
                    uiState.editedMealType != null ||
                    uiState.pendingDeferredBoluses.isNotEmpty() ||
                    uiState.editedTimestamp != initialValues.timestamp
        } else {
            uiState.editedCarbsKe != initialValues.carbsKe ||
                    uiState.editedTimestamp != initialValues.timestamp ||
                    uiState.editedMealType != initialValues.mealType ||
                    uiState.pendingDeferredBoluses != initialValues.pendingDeferredBoluses
        }
    }

    fun handleBack() {
        if (hasChanges) {
            showDiscardConfirmation = true
        } else {
            onNavigateUp()
        }
    }

    BackHandler(onBack = ::handleBack)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(
                            if (uiState.isAddMode) R.string.edit_historical_meal_add_screen_title
                            else R.string.edit_historical_meal_edit_screen_title
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = ::handleBack) {
                        Icon(
                            imageVector = if (hasChanges) Icons.Default.Close else Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(
                                id = if (hasChanges) CommonR.string.cd_cancel
                                else CommonR.string.cd_navigate_up
                            )
                        )
                    }
                },
                actions = {
                    if (!uiState.isAddMode) {
                        IconButton(onClick = { showDeleteConfirmation = true }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = stringResource(id = CommonR.string.cd_delete),
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    IconButton(onClick = onSave, enabled = uiState.isFormValid && !uiState.isSaving) {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = stringResource(id = CommonR.string.action_save)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (uiState.isLoading) {
                // Show nothing or a skeleton
            } else if (!uiState.isAddMode && uiState.meal == null) {
                Text(text = stringResource(R.string.meal_not_found))
            } else {
                EditMealCard(
                    carbsKe = uiState.editedCarbsKe,
                    timestamp = uiState.editedTimestamp,
                    mealType = uiState.editedMealType,
                    mealTypes = uiState.mealTypes,
                    pendingDeferredBoluses = uiState.pendingDeferredBoluses,
                    insulinAdministered = uiState.insulinAdministered,
                    onCarbsChange = onCarbsChange,
                    onTimestampChange = onTimestampChange,
                    onMealTypeChange = onMealTypeChange,
                    onOpenBolusPlanSheet = onOpenBolusPlanSheet
                )
            }
        }

        if (uiState.isBolusPlanSheetOpen) {
            val dialogTitle = stringResource(
                if (uiState.insulinAdministered || !uiState.isAddMode) {
                    R.string.bolus_plan_editor_title_pending
                } else {
                    R.string.bolus_plan_editor_title_planning
                }
            )

            BolusPlanEditorDialog(
                title = dialogTitle,
                insulinAdministered = uiState.insulinAdministered,
                plannedBoluses = uiState.pendingDeferredBoluses,
                baseTime = uiState.editedTimestamp,
                onUpdateBolusTime = onUpdateDeferredBolusTime,
                onUpdateBolusAmount = onUpdateDeferredBolusAmount,
                onAddDeferredBolus = onAddDeferredBolus,
                onRemoveDeferredBolus = onRemoveDeferredBolus,
                onDismissRequest = onCloseBolusPlanSheet,
                onConfirm = onCloseBolusPlanSheet
            )
        }
    }

    if (showDiscardConfirmation) {
        AlertDialog(
            onDismissRequest = { showDiscardConfirmation = false },
            title = { Text(stringResource(id = R.string.edit_historical_meal_discard_title)) },
            text = { Text(stringResource(id = R.string.edit_historical_meal_discard_message)) },
            confirmButton = {
                NormalTextButton(
                    onClick = {
                        showDiscardConfirmation = false
                        onSave()
                    },
                    enabled = uiState.isFormValid && !uiState.isSaving
                ) {
                    Text(stringResource(id = CommonR.string.action_save))
                }
            },
            dismissButton = {
                NormalTextButton(onClick = {
                    showDiscardConfirmation = false
                    onNavigateUp()
                }) {
                    Text(stringResource(id = R.string.discard_confirm_button))
                }
            }
        )
    }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text(stringResource(id = R.string.delete_meal_title)) },
            text = { Text(stringResource(id = R.string.delete_meal_message)) },
            confirmButton = {
                NormalTextButton(onClick = {
                    showDeleteConfirmation = false
                    onDelete()
                }) {
                    Text(stringResource(id = CommonR.string.action_delete))
                }
            },
            dismissButton = {
                NormalTextButton(onClick = { showDeleteConfirmation = false }) {
                    Text(stringResource(id = android.R.string.cancel))
                }
            }
        )
    }
}

@Composable
fun EditMealCard(
    carbsKe: Double,
    timestamp: Timestamp,
    mealType: MealType?,
    mealTypes: List<MealType>,
    pendingDeferredBoluses: List<PlannedBolusUiModel>,
    insulinAdministered: Boolean = false,
    onCarbsChange: (Double) -> Unit,
    onTimestampChange: (Timestamp) -> Unit,
    onMealTypeChange: (MealType) -> Unit,
    onOpenBolusPlanSheet: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(2.dp, AppColorBlue.copy(alpha = 0.3f)),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = stringResource(R.string.edit_historical_meal_meal_time_label),
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.height(8.dp))

                AbsoluteTimeStepper(
                    currentTime = timestamp,
                    onTimeChange = onTimestampChange,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = stringResource(R.string.edit_historical_meal_carbs_label),
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.height(8.dp))
                EditableValueStepper(
                    currentValue = carbsKe,
                    onValueChange = onCarbsChange,
                    minValue = CARBS_KE_MIN,
                    maxValue = CARBS_KE_MAX,
                    steppingStrategy = DefaultSteppingStrategy(0.5),
                    displayStrategy = object : ValueDisplayStrategy {
                        override fun format(value: Double): String =
                            String.format(Locale.getDefault(), "%.1f", value)

                        override fun color(value: Double): Color = Color.Unspecified
                    },
                    suffix = " ${carbsKeUnitLabel()}"
                )
            }

            FoodTypeSelector(
                mealTypes = mealTypes,
                selectedType = mealType,
                onTypeSelected = onMealTypeChange,
                isMandatory = true
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))

            if (insulinAdministered) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = stringResource(R.string.bolus_plan_editor_insulin_already_administered),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Verzögerte Boli Sektion
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenBolusPlanSheet() }
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.bolus_plan_editor_title_pending),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    val totalAmount = pendingDeferredBoluses.fold(InsulinAmount.ZERO) { acc, next -> acc + next.amount }
                    val count = pendingDeferredBoluses.size
                    val text = if (count == 0) {
                        stringResource(R.string.bolus_plan_editor_no_deferred_boluses)
                    } else {
                        "$count (${insulinValue(totalAmount.iu)})"
                    }
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                OutlinedButton(onClick = onOpenBolusPlanSheet) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(text = stringResource(R.string.action_edit))
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun EditHistoricalMealContentPreview() {
    val sampleMealTypes = listOf(
        MealType(id = ID_MEAL_FAST, name = "Schnell", components = listOf(CarbCurveComponentData(100, Minutes(30))), cat = Minutes(120)),
        MealType(id = ID_MEAL_STANDARD, name = "Standard", components = listOf(CarbCurveComponentData(100, Minutes(60))), cat = Minutes(180)),
        MealType(id = ID_MEAL_SLOW, name = "Langsam", components = listOf(CarbCurveComponentData(100, Minutes(90))), cat = Minutes(240)),
    )
    val sampleMealType = sampleMealTypes[1]
    val sampleMeal = MealEntry(
        timestamp = Timestamp.now(),
        carbGrams = 40.0,
        mealType = sampleMealType,
        insulinAdministered = true
    )
    val sampleUiState = EditHistoricalMealUiState(
        isLoading = false,
        isAddMode = false,
        meal = sampleMeal,
        editedCarbsKe = 4.0,
        editedTimestamp = Timestamp.now(),
        editedMealType = sampleMealType,
        mealTypes = sampleMealTypes,
        pendingDeferredBoluses = listOf(
            PlannedBolusUiModel(amount = InsulinAmount(1.5), timestamp = Timestamp.now() + Minutes(30))
        ),
        insulinAdministered = true,
        isSaving = false,
        isFormValid = true
    )

    AppTheme {
        EditHistoricalMealContent(
            uiState = sampleUiState,
            onNavigateUp = {},
            onDelete = {},
            onCarbsChange = {},
            onTimestampChange = {},
            onMealTypeChange = {},
            onOpenBolusPlanSheet = {},
            onCloseBolusPlanSheet = {},
            onAddDeferredBolus = {},
            onUpdateDeferredBolusTime = { _, _ -> },
            onUpdateDeferredBolusAmount = { _, _ -> },
            onRemoveDeferredBolus = {},
            onSave = {}
        )
    }
}