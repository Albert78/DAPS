package de.dh.daps.ui.screens.manualcontrol

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.dh.daps.common.model.DeferredBolus
import de.dh.daps.common.model.InsulinAmount
import de.dh.daps.core.aps.ApsRecommendation
import de.dh.daps.ui.R
import de.dh.daps.ui.common.ConfigurableDisplayStrategy
import de.dh.daps.ui.common.DefaultSteppingStrategy
import de.dh.daps.ui.common.composables.EditableValueStepper
import de.dh.daps.ui.common.composables.InsulinAmountStepper
import de.dh.daps.ui.common.composables.NormalTextButton
import de.dh.daps.ui.common.composables.PrimaryButton
import de.dh.daps.ui.common.composables.SecondaryButton
import de.dh.daps.ui.common.theme.AppPreview

sealed interface ManualControlDialog {
    data class Bolus(
        val initialAmount: InsulinAmount,
        val includedDeferredBoluses: List<DeferredBolus>? = null,
        val correctionPart: InsulinAmount = InsulinAmount.ZERO,
        val basalPart: InsulinAmount = InsulinAmount.ZERO,
        val recommendationToDismiss: ApsRecommendation.Bolus? = null,
    ) : ManualControlDialog

    data class TempBasal(
        val initialPercent: Int,
        val initialDurationHours: Int,
        val recommendationToDismiss: ApsRecommendation.TempBasal? = null
    ) : ManualControlDialog
}

@Composable
fun DeliverBolusDialog(
    dialogData: ManualControlDialog.Bolus,
    minBolusAmount: InsulinAmount,
    maxBolusSize: InsulinAmount,
    onDismiss: () -> Unit,
    onConfirm: (
        amount: InsulinAmount,
        handledDeferredBoluses: List<DeferredBolus>?,
        correctionPart: InsulinAmount,
        basalPart: InsulinAmount,
        recommendationToDismiss: ApsRecommendation.Bolus?
    ) -> Unit
) {
    var amountState by remember { mutableDoubleStateOf(dialogData.initialAmount.iu) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(id = R.string.manual_control_dialog_deliver_bolus_title)) },
        text = {
            DeliverBolusDialogContent(
                amountState = amountState,
                onAmountChange = { amountState = it },
                includedDeferredBoluses = dialogData.includedDeferredBoluses,
                minBolusAmount = minBolusAmount,
                maxBolusSize = maxBolusSize
            )
        },
        confirmButton = {
            PrimaryButton(
                onClick = {
                    onConfirm(
                        InsulinAmount(amountState),
                        dialogData.includedDeferredBoluses,
                        dialogData.correctionPart,
                        dialogData.basalPart,
                        dialogData.recommendationToDismiss
                    )
                }
            ) {
                Text(stringResource(id = R.string.manual_control_dialog_confirm_deliver))
            }
        },
        dismissButton = {
            NormalTextButton(onClick = onDismiss) {
                Text(stringResource(id = R.string.manual_control_dialog_cancel))
            }
        }
    )
}

@Composable
fun DeliverBolusDialogContent(
    amountState: Double,
    onAmountChange: (Double) -> Unit,
    includedDeferredBoluses: List<DeferredBolus>?,
    minBolusAmount: InsulinAmount,
    maxBolusSize: InsulinAmount,
    modifier: Modifier = Modifier
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
    ) {
        includedDeferredBoluses?.takeIf { it.isNotEmpty() }?.let { deferredList ->
            val totalDeferred = deferredList.sumOf { it.amount.iu }
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Text(
                    text = pluralStringResource(
                        id = R.plurals.manual_control_deferred_boluses_info,
                        count = deferredList.size,
                        deferredList.size,
                        totalDeferred
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(10.dp)
                )
            }
        }

        InsulinAmountStepper(
            currentValue = amountState,
            onValueChange = onAmountChange,
            minValue = minBolusAmount.iu,
            maxValue = maxBolusSize.iu,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun SetTempBasalDialog(
    dialogData: ManualControlDialog.TempBasal,
    onDismiss: () -> Unit,
    onConfirm: (durationHours: Int, percent: Int, recommendationToDismiss: ApsRecommendation.TempBasal?) -> Unit
) {
    var percent by remember { mutableIntStateOf(dialogData.initialPercent) }
    var durationHours by remember { mutableIntStateOf(dialogData.initialDurationHours) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(id = R.string.manual_control_dialog_set_temp_basal_title)) },
        text = {
            SetTempBasalDialogContent(
                percent = percent,
                onPercentChange = { percent = it },
                durationHours = durationHours,
                onDurationHoursChange = { durationHours = it }
            )
        },
        confirmButton = {
            SecondaryButton(
                onClick = {
                    onConfirm(durationHours, percent, dialogData.recommendationToDismiss)
                }
            ) {
                Text(stringResource(id = R.string.manual_control_dialog_confirm_set))
            }
        },
        dismissButton = {
            NormalTextButton(onClick = onDismiss) {
                Text(stringResource(id = R.string.manual_control_dialog_cancel))
            }
        }
    )
}

@Composable
fun SetTempBasalDialogContent(
    percent: Int,
    onPercentChange: (Int) -> Unit,
    durationHours: Int,
    onDurationHoursChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
    ) {
        Text(
            text = stringResource(id = R.string.manual_control_rate_percent_label),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        EditableValueStepper(
            currentValue = percent.toDouble(),
            onValueChange = { onPercentChange(it.toInt().coerceIn(0, 500)) },
            minValue = 0.0,
            maxValue = 500.0,
            steppingStrategy = DefaultSteppingStrategy(step = 10.0),
            displayStrategy = ConfigurableDisplayStrategy(suffix = "%"),
            modifier = Modifier.fillMaxWidth()
        )

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf(0, 25, 50, 80, 120, 150, 200).forEach { preset ->
                AssistChip(
                    onClick = { onPercentChange(preset) },
                    label = { Text("%d%%".format(preset)) }
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = stringResource(id = R.string.manual_control_duration_hours_label),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        EditableValueStepper(
            currentValue = durationHours.toDouble(),
            onValueChange = { onDurationHoursChange(it.toInt().coerceAtLeast(1)) },
            minValue = 1.0,
            maxValue = 24.0,
            steppingStrategy = DefaultSteppingStrategy(step = 1.0),
            displayStrategy = ConfigurableDisplayStrategy(suffix = " ${stringResource(id = R.string.manual_control_unit_hours_short)}"),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Preview(showBackground = true, name = "Deliver Bolus Dialog")
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, name = "Deliver Bolus Dialog - Dark Mode")
@Composable
fun DeliverBolusDialogPreview() {
    AppPreview {
        DeliverBolusDialogContent(
            amountState = 1.5,
            onAmountChange = {},
            includedDeferredBoluses = null,
            minBolusAmount = InsulinAmount(0.05),
            maxBolusSize = InsulinAmount(25.0),
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(showBackground = true, name = "Set Temp Basal Dialog")
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, name = "Set Temp Basal Dialog - Dark Mode")
@Composable
fun SetTempBasalDialogPreview() {
    AppPreview {
        SetTempBasalDialogContent(
            percent = 120,
            onPercentChange = {},
            durationHours = 2,
            onDurationHoursChange = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}