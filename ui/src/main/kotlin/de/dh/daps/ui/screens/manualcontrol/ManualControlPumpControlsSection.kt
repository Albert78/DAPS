package de.dh.daps.ui.screens.manualcontrol

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.dh.daps.common.model.BasalStatus
import de.dh.daps.common.model.BolusDeliveryState
import de.dh.daps.common.model.InsulinAmount
import de.dh.daps.common.model.InsulinApplication
import de.dh.daps.common.model.InsulinOrigin
import de.dh.daps.common.model.InsulinType
import de.dh.daps.common.model.data.Minutes
import de.dh.daps.common.model.data.Timestamp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import de.dh.daps.ui.R
import de.dh.daps.ui.common.composables.NormalButton
import de.dh.daps.ui.common.composables.PrimaryButton
import de.dh.daps.ui.common.icons.Icon_Basal
import de.dh.daps.ui.common.icons.Icon_Temp_Basal
import de.dh.daps.ui.common.icons.Syringe
import de.dh.daps.ui.common.insulinValue
import de.dh.daps.ui.common.theme.AppPreview
import de.dh.daps.ui.common.theme.SoftRed
import de.dh.daps.ui.common.timeWithUnit

@Composable
fun ManualControlPumpControlsSection(
    pump: ManualControlPumpUiModel,
    lastBolus: InsulinApplication? = null,
    onOpenBolusDialog: (ManualControlDialog.Bolus) -> Unit,
    onCancelBolus: () -> Unit,
    onOpenTempBasalDialog: (ManualControlDialog.TempBasal) -> Unit,
    onCancelTempBasal: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = stringResource(id = R.string.manual_control_pump_controls_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        // Card 1: Bolus (Primary Color Scheme - Cool Blue tone)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Syringe,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(id = R.string.manual_control_bolus_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        val lastBolusText = if (lastBolus != null) {
                            stringResource(
                                id = R.string.manual_control_last_bolus_format,
                                insulinValue(lastBolus.amount.iu),
                                timeWithUnit(lastBolus.timestamp)
                            )
                        } else {
                            stringResource(id = R.string.manual_control_no_last_bolus)
                        }
                        Text(
                            text = lastBolusText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                val activeBolus = pump.bolusStatus
                if (activeBolus?.state == BolusDeliveryState.DELIVERING) {
                    Text(
                        text = stringResource(
                            id = R.string.manual_control_bolus_delivering,
                            activeBolus.deliveredAmount.iu,
                            activeBolus.targetAmount.iu,
                            activeBolus.progressPercent
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { activeBolus.progressPercent / 100f },
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    NormalButton(
                        onClick = onCancelBolus,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = SoftRed
                        ),
                        border = BorderStroke(1.dp, SoftRed)
                    ) {
                        Icon(imageVector = Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(id = R.string.manual_control_cancel_bolus))
                    }
                } else {
                    Text(
                        text = stringResource(id = R.string.manual_control_no_active_bolus),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    PrimaryButton(
                        onClick = {
                            onOpenBolusDialog(
                                ManualControlDialog.Bolus(
                                    initialAmount = pump.minBolusAmount.iu.coerceAtLeast(1.0)
                                )
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Outlined.Syringe, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(id = R.string.manual_control_deliver_bolus))
                    }
                }
            }
        }

        // Card 2: Basal
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                val activeBasal = pump.basalStatus
                val isTempActive = activeBasal?.isTempBasal == true

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icon_Basal,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(id = R.string.manual_control_basal_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        val basalSubtitle = if (isTempActive) {
                            stringResource(id = R.string.manual_control_subtitle_temp_basal)
                        } else {
                            stringResource(id = R.string.manual_control_subtitle_normal_basal)
                        }
                        Text(
                            text = basalSubtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (activeBasal != null) {
                    val activeRateFormatted = insulinValue(activeBasal.activeRate.iu)
                    if (activeBasal.isSuspended) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.errorContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = stringResource(id = R.string.manual_control_basal_suspended),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        PrimaryButton(
                            onClick = {
                                onOpenTempBasalDialog(
                                    ManualControlDialog.TempBasal(
                                        initialPercent = 100,
                                        initialDurationHours = 1
                                    )
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(imageVector = Icon_Temp_Basal, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(id = R.string.manual_control_set_temp_basal))
                        }
                    } else if (activeBasal.isTempBasal) {
                        val percent = activeBasal.tempBasalPercent ?: 100
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = stringResource(id = R.string.manual_control_temp_basal_active, percent),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = stringResource(id = R.string.manual_control_basal_rate_format, activeRateFormatted),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                activeBasal.tempBasalExpiry?.let { expiry ->
                                    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
                                    val endTimeStr = timeFormat.format(Date(expiry.ms))
                                    val diffMs = expiry.ms - System.currentTimeMillis()
                                    val totalMinutes = (diffMs / 60_000).coerceAtLeast(0)
                                    val hours = totalMinutes / 60
                                    val minutes = totalMinutes % 60
                                    val expiryText = if (hours > 0) {
                                        stringResource(id = R.string.manual_control_temp_basal_expiry_hours_info, endTimeStr, hours, minutes)
                                    } else {
                                        stringResource(id = R.string.manual_control_temp_basal_expiry_info, endTimeStr, minutes)
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = expiryText,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        NormalButton(
                            onClick = onCancelTempBasal,
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = SoftRed
                            ),
                            border = BorderStroke(1.dp, SoftRed)
                        ) {
                            Icon(imageVector = Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(id = R.string.manual_control_cancel_temp_basal))
                        }
                    } else {
                        Text(
                            text = stringResource(id = R.string.manual_control_basal_rate_format, activeRateFormatted),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        PrimaryButton(
                            onClick = {
                                onOpenTempBasalDialog(
                                    ManualControlDialog.TempBasal(
                                        initialPercent = 100,
                                        initialDurationHours = 1
                                    )
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(imageVector = Icon_Temp_Basal, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(id = R.string.manual_control_set_temp_basal))
                        }
                    }
                } else {
                    Text(
                        text = stringResource(id = R.string.manual_control_basal_rate_format, "--"),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    PrimaryButton(
                        onClick = {
                            onOpenTempBasalDialog(
                                ManualControlDialog.TempBasal(
                                    initialPercent = 100,
                                    initialDurationHours = 1
                                )
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icon_Temp_Basal, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(id = R.string.manual_control_set_temp_basal))
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "Pump Controls Preview")
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, name = "Pump Controls - Dark Mode")
@Composable
fun ManualControlPumpControlsPreview() {
    AppPreview(modifier = Modifier.padding(16.dp)) {
        ManualControlPumpControlsSection(
            pump = ManualControlPumpUiModel(
                isConnected = true,
                basalStatus = BasalStatus(
                    isSuspended = false,
                    activeRate = InsulinAmount(0.5),
                    isTempBasal = false,
                    tempBasalPercent = 80
                )
            ),
            lastBolus = InsulinApplication(
                timestamp = Timestamp.now().minusHours(1),
                amount = InsulinAmount(2.5),
                insulinType = InsulinType(name = "NovoRapid", peak = Minutes(75), dia = Minutes(300)),
                origin = InsulinOrigin.Pump
            ),
            onOpenBolusDialog = {},
            onCancelBolus = {},
            onOpenTempBasalDialog = {},
            onCancelTempBasal = {}
        )
    }
}

@Preview(showBackground = true, name = "Pump Controls - Temp Basal Active")
@Composable
fun ManualControlPumpControlsTempBasalActivePreview() {
    AppPreview(modifier = Modifier.padding(16.dp)) {
        ManualControlPumpControlsSection(
            pump = ManualControlPumpUiModel(
                isConnected = true,
                basalStatus = BasalStatus(
                    isSuspended = false,
                    activeRate = InsulinAmount(0.6),
                    isTempBasal = true,
                    tempBasalPercent = 120,
                    tempBasalExpiry = Timestamp.now().plusMinutes(45)
                )
            ),
            lastBolus = InsulinApplication(
                timestamp = Timestamp.now().minusHours(1),
                amount = InsulinAmount(2.5),
                insulinType = InsulinType(name = "NovoRapid", peak = Minutes(75), dia = Minutes(300)),
                origin = InsulinOrigin.Pump
            ),
            onOpenBolusDialog = {},
            onCancelBolus = {},
            onOpenTempBasalDialog = {},
            onCancelTempBasal = {}
        )
    }
}