package de.dh.raaps.ui.controls.meal

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import de.dh.raaps.common.model.InsulinAmount
import de.dh.raaps.common.model.data.Timestamp
import de.dh.raaps.ui.R
import de.dh.raaps.ui.common.DefaultSteppingStrategy
import de.dh.raaps.ui.common.ValueDisplayStrategy
import de.dh.raaps.ui.common.composables.AppColorBlue
import de.dh.raaps.ui.common.composables.EditableValueStepper
import de.dh.raaps.ui.common.composables.PrimaryButton
import de.dh.raaps.ui.common.composables.StepperDefaults
import de.dh.raaps.ui.common.composables.TimeStepper
import de.dh.raaps.ui.common.composables.contentScrollIndicator
import de.dh.raaps.ui.common.insulinValue
import de.dh.raaps.ui.common.theme.AppTheme
import de.dh.raaps.ui.common.time
import java.util.Locale
import kotlin.math.round
import de.dh.raaps.common.R as CommonR

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BolusPlanEditorDialog(
    title: String,
    administeredInsulinAmount: InsulinAmount = InsulinAmount.ZERO,
    plannedBoluses: List<PlannedBolusUiModel>,
    baseTime: Timestamp = Timestamp.now(),
    onUpdateBolusTime: (index: Int, newTimestamp: Timestamp) -> Unit,
    onUpdateBolusAmount: (index: Int, newAmount: InsulinAmount) -> Unit,
    onAddDeferredBolus: () -> Unit,
    onRemoveDeferredBolus: (index: Int) -> Unit,
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit = onDismissRequest
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    )
                }
            ) { innerPadding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    BolusPlanEditorContent(
                        title = null,
                        administeredInsulinAmount = administeredInsulinAmount,
                        plannedBoluses = plannedBoluses,
                        baseTime = baseTime,
                        onUpdateBolusTime = onUpdateBolusTime,
                        onUpdateBolusAmount = onUpdateBolusAmount,
                        onAddDeferredBolus = onAddDeferredBolus,
                        onRemoveDeferredBolus = onRemoveDeferredBolus,
                        onConfirm = onConfirm
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BolusPlanEditorSheet(
    title: String,
    administeredInsulinAmount: InsulinAmount = InsulinAmount.ZERO,
    plannedBoluses: List<PlannedBolusUiModel>,
    baseTime: Timestamp = Timestamp.now(),
    onUpdateBolusTime: (index: Int, newTimestamp: Timestamp) -> Unit,
    onUpdateBolusAmount: (index: Int, newAmount: InsulinAmount) -> Unit,
    onAddDeferredBolus: () -> Unit,
    onRemoveDeferredBolus: (index: Int) -> Unit,
    onDismissRequest: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState()
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState
    ) {
        BolusPlanEditorContent(
            title = title,
            administeredInsulinAmount = administeredInsulinAmount,
            plannedBoluses = plannedBoluses,
            baseTime = baseTime,
            onUpdateBolusTime = onUpdateBolusTime,
            onUpdateBolusAmount = onUpdateBolusAmount,
            onAddDeferredBolus = onAddDeferredBolus,
            onRemoveDeferredBolus = onRemoveDeferredBolus,
            onConfirm = onDismissRequest
        )
    }
}

@Composable
fun BolusPlanEditorContent(
    title: String?,
    administeredInsulinAmount: InsulinAmount = InsulinAmount.ZERO,
    plannedBoluses: List<PlannedBolusUiModel>,
    baseTime: Timestamp = Timestamp.now(),
    onUpdateBolusTime: (index: Int, newTimestamp: Timestamp) -> Unit,
    onUpdateBolusAmount: (index: Int, newAmount: InsulinAmount) -> Unit,
    onAddDeferredBolus: () -> Unit,
    onRemoveDeferredBolus: (index: Int) -> Unit,
    onConfirm: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (!title.isNullOrEmpty()) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        if (administeredInsulinAmount > InsulinAmount.ZERO) {
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
                        text = stringResource(R.string.x_ie_insulin_already_administered, insulinValue(administeredInsulinAmount.iu, withUnit = false)),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        if (plannedBoluses.isEmpty()) {
            Text(
                text = stringResource(R.string.bolus_plan_editor_no_deferred_boluses),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 12.dp)
            )
        } else {
            val listState = rememberLazyListState()
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f, fill = false)
                    .contentScrollIndicator(listState),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                itemsIndexed(plannedBoluses) { index, item ->
                    BolusPlanItemCard(
                        index = index,
                        item = item,
                        baseTime = baseTime,
                        onTimeChange = { newTime -> onUpdateBolusTime(index, newTime) },
                        onAmountChange = { newAmount -> onUpdateBolusAmount(index, newAmount) },
                        onDelete = { onRemoveDeferredBolus(index) }
                    )
                }
            }
        }

        val totalAmount = plannedBoluses.fold(InsulinAmount.ZERO) { acc, next -> acc + next.amount }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.bolus_plan_editor_total_label, insulinValue(totalAmount.iu)),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            OutlinedButton(
                onClick = onAddDeferredBolus,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(text = stringResource(R.string.bolus_plan_editor_add_deferred_bolus))
            }
        }

        Spacer(Modifier.height(4.dp))

        PrimaryButton(
            onClick = onConfirm,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = stringResource(R.string.bolus_plan_editor_done))
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun BolusPlanItemCard(
    index: Int,
    item: PlannedBolusUiModel,
    baseTime: Timestamp,
    onTimeChange: (Timestamp) -> Unit,
    onAmountChange: (InsulinAmount) -> Unit,
    onDelete: () -> Unit
) {
    val sharedStepperStyle = StepperDefaults.smallStyle().copy(
        buttonSize = 40.dp,
        spacing = 4.dp,
        valueWidth = 110.dp
    )

    val diffMin = round((item.timestamp.ms - baseTime.ms) / 60000.0).toInt()
    val relationText = when {
        diffMin > 0 -> stringResource(R.string.bolus_plan_editor_after_meal)
        diffMin < 0 -> stringResource(R.string.bolus_plan_editor_before_meal)
        else -> stringResource(R.string.bolus_plan_editor_at_meal)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, AppColorBlue.copy(alpha = 0.3f)),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.label.ifEmpty {
                        stringResource(R.string.bolus_plan_editor_deferred_bolus_n, index + 1)
                    },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = stringResource(CommonR.string.cd_delete),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))

            // 1. Zeit / Verzögerung
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.bolus_plan_editor_time_label),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .padding(end = 8.dp)
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    TimeStepper(
                        currentTime = item.timestamp,
                        baseTime = baseTime,
                        onTimeChange = onTimeChange,
                        showPreposition = false,
                        style = sharedStepperStyle
                    )
                    Text(
                        text = "$relationText (${time(item.timestamp)})",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.08f))

            // 2. Menge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.bolus_plan_editor_amount_label),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .padding(end = 8.dp)
                )
                EditableValueStepper(
                    currentValue = item.amount.iu,
                    onValueChange = { onAmountChange(InsulinAmount(it)) },
                    minValue = 0.1,
                    maxValue = 30.0,
                    steppingStrategy = DefaultSteppingStrategy(0.1),
                    displayStrategy = object : ValueDisplayStrategy {
                        override fun format(value: Double): String =
                            String.format(Locale.getDefault(), "%.1f", value)

                        override fun color(value: Double): Color = Color.Unspecified
                    },
                    suffix = " IE",
                    style = sharedStepperStyle
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun BolusPlanEditorContentPreview() {
    val now = Timestamp.now()
    val sampleBoluses = listOf(
        PlannedBolusUiModel(
            amount = InsulinAmount(1.5),
            timestamp = Timestamp(now.ms + 30 * 60 * 1000)
        ),
        PlannedBolusUiModel(
            amount = InsulinAmount(1.0),
            timestamp = Timestamp(now.ms + 60 * 60 * 1000)
        )
    )

    AppTheme {
        BolusPlanEditorContent(
            title = stringResource(R.string.bolus_plan_editor_title_pending),
            administeredInsulinAmount = InsulinAmount(2.5),
            plannedBoluses = sampleBoluses,
            baseTime = now,
            onUpdateBolusTime = { _, _ -> },
            onUpdateBolusAmount = { _, _ -> },
            onAddDeferredBolus = {},
            onRemoveDeferredBolus = {},
            onConfirm = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun BolusPlanEditorDialogPreview() {
    val now = Timestamp.now()
    val sampleBoluses = listOf(
        PlannedBolusUiModel(
            amount = InsulinAmount(1.5),
            timestamp = Timestamp(now.ms + 30 * 60 * 1000)
        ),
        PlannedBolusUiModel(
            amount = InsulinAmount(1.0),
            timestamp = Timestamp(now.ms + 60 * 60 * 1000)
        )
    )

    AppTheme {
        BolusPlanEditorDialog(
            title = stringResource(R.string.bolus_plan_editor_title_pending),
            administeredInsulinAmount = InsulinAmount(2.5),
            plannedBoluses = sampleBoluses,
            baseTime = now,
            onUpdateBolusTime = { _, _ -> },
            onUpdateBolusAmount = { _, _ -> },
            onAddDeferredBolus = {},
            onRemoveDeferredBolus = {},
            onDismissRequest = {}
        )
    }
}