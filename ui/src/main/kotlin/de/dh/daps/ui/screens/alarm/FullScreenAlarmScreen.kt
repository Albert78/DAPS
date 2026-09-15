package de.dh.daps.ui.screens.alarm

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.dh.daps.common.model.data.AlarmSeverity
import de.dh.daps.common.model.data.AlarmType
import de.dh.daps.common.model.data.BgValue
import de.dh.daps.common.model.data.GlucoseUnit
import de.dh.daps.ui.R
import de.dh.daps.ui.common.composables.PrimaryButton
import de.dh.daps.ui.common.theme.AppTheme

@Composable
fun FullScreenAlarmScreen(
    alarmType: AlarmType,
    bgValue: BgValue?,
    glucoseUnit: GlucoseUnit,
    onSnooze: (minutes: Int) -> Unit,
    onDismiss: () -> Unit
) {
    val isCritical = alarmType.defaultSeverity == AlarmSeverity.CRITICAL
    val backgroundColor = if (isCritical) {
        MaterialTheme.colorScheme.errorContainer
    } else {
        MaterialTheme.colorScheme.tertiaryContainer
    }
    val contentColor = if (isCritical) {
        MaterialTheme.colorScheme.onErrorContainer
    } else {
        MaterialTheme.colorScheme.onTertiaryContainer
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = backgroundColor
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Header Icon & Title
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(72.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(id = R.string.full_screen_alarm_title),
                    style = MaterialTheme.typography.labelLarge,
                    color = contentColor,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = getAlarmTypeTitle(alarmType),
                    style = MaterialTheme.typography.headlineMedium,
                    color = contentColor,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
                )
            }

            // Central Data Card (Glucose value or status)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (bgValue != null && bgValue.isValid()) {
                        Text(
                            text = bgValue.toString(glucoseUnit),
                            style = MaterialTheme.typography.displayLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = when (glucoseUnit) {
                                GlucoseUnit.MG_DL -> "mg/dL"
                                GlucoseUnit.MMOL -> "mmol/L"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            text = getAlarmTypeDescription(alarmType),
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Action Buttons
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(id = R.string.alarm_action_snooze),
                    style = MaterialTheme.typography.titleSmall,
                    color = contentColor,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    OutlinedButton(
                        onClick = { onSnooze(15) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(id = R.string.alarm_action_snooze_15))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedButton(
                        onClick = { onSnooze(30) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(id = R.string.alarm_action_snooze_30))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedButton(
                        onClick = { onSnooze(60) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(id = R.string.alarm_action_snooze_60))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                PrimaryButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(id = R.string.alarm_action_dismiss),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
    }
}

@Composable
fun getAlarmTypeTitle(alarmType: AlarmType): String = when (alarmType) {
    AlarmType.CRITICAL_LOW_BG -> stringResource(id = R.string.alarm_type_critical_low_bg)
    AlarmType.LOW_BG -> stringResource(id = R.string.alarm_type_low_bg)
    AlarmType.HIGH_BG -> stringResource(id = R.string.alarm_type_high_bg)
    AlarmType.PUMP_OCCLUSION -> stringResource(id = R.string.alarm_type_pump_occlusion)
    AlarmType.PUMP_LOW_INSULIN -> stringResource(id = R.string.alarm_type_pump_low_insulin)
    AlarmType.PUMP_LOW_BATTERY -> stringResource(id = R.string.alarm_type_pump_low_battery)
    AlarmType.CGM_SIGNAL_LOSS -> stringResource(id = R.string.alarm_type_cgm_signal_loss)
    AlarmType.SYSTEM_BATTERY_LOW -> stringResource(id = R.string.alarm_type_system_battery_low)
}

@Composable
fun getAlarmTypeDescription(alarmType: AlarmType): String = when (alarmType) {
    AlarmType.CRITICAL_LOW_BG -> stringResource(id = R.string.alarm_type_critical_low_bg)
    AlarmType.LOW_BG -> stringResource(id = R.string.alarm_type_low_bg)
    AlarmType.HIGH_BG -> stringResource(id = R.string.alarm_type_high_bg)
    AlarmType.PUMP_OCCLUSION -> stringResource(id = R.string.pump_issue_inoperative)
    AlarmType.PUMP_LOW_INSULIN -> stringResource(id = R.string.pump_issue_low_insulin)
    AlarmType.PUMP_LOW_BATTERY -> stringResource(id = R.string.pump_issue_low_battery)
    AlarmType.CGM_SIGNAL_LOSS -> stringResource(id = R.string.core_issue_no_recent_values, 15)
    AlarmType.SYSTEM_BATTERY_LOW -> stringResource(id = R.string.alarm_type_system_battery_low)
}

@Preview(showBackground = true, name = "Critical Low BG Alarm")
@Composable
fun FullScreenAlarmScreenCriticalPreview() {
    AppTheme {
        FullScreenAlarmScreen(
            alarmType = AlarmType.CRITICAL_LOW_BG,
            bgValue = BgValue.fromMgDl(55),
            glucoseUnit = GlucoseUnit.MG_DL,
            onSnooze = {},
            onDismiss = {}
        )
    }
}

@Preview(showBackground = true, name = "High BG Alarm")
@Composable
fun FullScreenAlarmScreenHighBgPreview() {
    AppTheme {
        FullScreenAlarmScreen(
            alarmType = AlarmType.HIGH_BG,
            bgValue = BgValue.fromMgDl(240),
            glucoseUnit = GlucoseUnit.MG_DL,
            onSnooze = {},
            onDismiss = {}
        )
    }
}