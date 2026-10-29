package de.dh.daps.ui.screens.fullscreenalerts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.dh.daps.common.domain.model.alarm.ActiveAlarmGroup
import de.dh.daps.common.domain.model.alarm.AlarmCategory
import de.dh.daps.common.domain.model.alarm.AlarmSeverity
import de.dh.daps.common.domain.model.alarm.AlarmType
import de.dh.daps.common.domain.model.data.BgValue
import de.dh.daps.common.domain.model.data.GlucoseUnit
import de.dh.daps.ui.R
import de.dh.daps.ui.common.composables.FramedCard
import de.dh.daps.ui.common.composables.PrimaryButton
import de.dh.daps.ui.common.composables.contentScrollIndicator
import de.dh.daps.ui.common.theme.AppPreview

@Composable
fun FullScreenAlarmScreen(
    alarmType: AlarmType,
    bgValue: BgValue?,
    glucoseUnit: GlucoseUnit,
    onSnooze: (minutes: Int) -> Unit
) {
    val group = ActiveAlarmGroup(
        heroAlarms = listOf(alarmType),
        otherAlarms = emptyList(),
        highestSeverity = alarmType.defaultSeverity,
        primaryAlarm = alarmType
    )
    FullScreenAlarmScreen(
        alarmGroup = group,
        bgValue = bgValue,
        glucoseUnit = glucoseUnit,
        onSnoozeAll = onSnooze
    )
}

@Composable
fun FullScreenAlarmScreen(
    alarmGroup: ActiveAlarmGroup,
    bgValue: BgValue?,
    glucoseUnit: GlucoseUnit,
    onSnoozeAll: (minutes: Int) -> Unit
) {
    val isCritical = alarmGroup.highestSeverity == AlarmSeverity.CRITICAL
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

    val scrollState = rememberScrollState()

    val locale = LocalConfiguration.current.locales[0]

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = backgroundColor
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .contentScrollIndicator(scrollState)
                .verticalScroll(scrollState)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Header Icon & Title
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = stringResource(id = R.string.full_screen_alarm_title),
                    style = MaterialTheme.typography.labelLarge,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(72.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Hero Alarms Section
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                alarmGroup.heroAlarms.forEach { heroAlarm ->
                    val rawTitle = getAlarmTypeTitle(heroAlarm)
                    val displayTitle = if (heroAlarm.isSafetyCritical) rawTitle.uppercase(locale) else rawTitle
                    Text(
                        text = displayTitle,
                        style = MaterialTheme.typography.headlineMedium,
                        color = contentColor,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center
                    )

                    FramedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            if (heroAlarm.category == AlarmCategory.GLUCOSE && bgValue != null && bgValue.isValid()) {
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
                                    text = getAlarmTypeDescription(heroAlarm),
                                    style = MaterialTheme.typography.bodyLarge,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            // Other Alarms Section (if present)
            if (alarmGroup.otherAlarms.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = stringResource(id = R.string.full_screen_other_alarms_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = contentColor,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    alarmGroup.otherAlarms.forEach { otherAlarm ->
                        FramedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            shape = RoundedCornerShape(16.dp),
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                val rawTitle = getAlarmTypeTitle(otherAlarm)
                                val displayTitle = if (otherAlarm.isSafetyCritical) rawTitle.uppercase(locale) else rawTitle
                                Text(
                                    text = displayTitle,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = getAlarmTypeDescription(otherAlarm),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action Buttons
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(id = R.string.alarm_action_snooze_all),
                    style = MaterialTheme.typography.titleMedium,
                    color = contentColor,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                PrimaryButton(
                    onClick = { onSnoozeAll(15) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(id = R.string.alarm_action_snooze_15),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                PrimaryButton(
                    onClick = { onSnoozeAll(30) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(id = R.string.alarm_action_snooze_30),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                PrimaryButton(
                    onClick = { onSnoozeAll(60) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(id = R.string.alarm_action_snooze_60),
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
    AlarmType.PUMP_SUSPENDED -> stringResource(id = R.string.alarm_type_pump_suspended)
    AlarmType.PUMP_LOW_INSULIN -> stringResource(id = R.string.alarm_type_pump_low_insulin)
    AlarmType.PUMP_LOW_BATTERY -> stringResource(id = R.string.alarm_type_pump_low_battery)
    AlarmType.PUMP_STALE_BOND -> stringResource(id = R.string.alarm_type_pump_stale_bond)
    AlarmType.PUMP_CATHETER_CHANGE_WARN -> stringResource(id = R.string.alarm_type_pump_catheter_change_warn)
    AlarmType.PUMP_PATCH_CHANGE_WARN -> stringResource(id = R.string.alarm_type_pump_patch_change_warn)
    AlarmType.CGM_SIGNAL_LOSS -> stringResource(id = R.string.alarm_type_cgm_signal_loss)
    AlarmType.CGM_EXPIRED -> stringResource(id = R.string.alarm_type_cgm_expired)
    AlarmType.CGM_SENSOR_ERROR -> stringResource(id = R.string.alarm_type_cgm_sensor_error)
    AlarmType.CGM_STALE_BOND -> stringResource(id = R.string.alarm_type_cgm_stale_bond)
    AlarmType.SYSTEM_BATTERY_LOW -> stringResource(id = R.string.alarm_type_system_battery_low)
    AlarmType.SYSTEM_BLUETOOTH_OFF -> stringResource(id = R.string.alarm_type_system_bluetooth_off)
    AlarmType.SYSTEM_THERAPY_SETTINGS_MISSING -> stringResource(id = R.string.alarm_type_system_therapy_settings_missing)
}

@Composable
fun getAlarmTypeDescription(alarmType: AlarmType): String = when (alarmType) {
    AlarmType.CRITICAL_LOW_BG -> stringResource(id = R.string.alarm_type_critical_low_bg)
    AlarmType.LOW_BG -> stringResource(id = R.string.alarm_type_low_bg)
    AlarmType.HIGH_BG -> stringResource(id = R.string.alarm_type_high_bg)
    AlarmType.PUMP_OCCLUSION -> stringResource(id = R.string.pump_issue_occlusion)
    AlarmType.PUMP_SUSPENDED -> stringResource(id = R.string.pump_issue_suspended)
    AlarmType.PUMP_LOW_INSULIN -> stringResource(id = R.string.pump_issue_low_insulin)
    AlarmType.PUMP_LOW_BATTERY -> stringResource(id = R.string.pump_issue_low_battery)
    AlarmType.PUMP_STALE_BOND -> stringResource(id = R.string.pump_issue_stale_bluetooth_bond)
    AlarmType.PUMP_CATHETER_CHANGE_WARN -> stringResource(id = R.string.pump_issue_catheter_change_warn)
    AlarmType.PUMP_PATCH_CHANGE_WARN -> stringResource(id = R.string.pump_issue_patch_change_warn)
    AlarmType.CGM_SIGNAL_LOSS -> stringResource(id = R.string.core_issue_no_recent_values, 15)
    AlarmType.CGM_EXPIRED -> stringResource(id = R.string.glucose_source_issue_expired)
    AlarmType.CGM_SENSOR_ERROR -> stringResource(id = R.string.glucose_source_issue_sensor_error)
    AlarmType.CGM_STALE_BOND -> stringResource(id = R.string.glucose_source_issue_stale_bluetooth_bond)
    AlarmType.SYSTEM_BATTERY_LOW -> stringResource(id = R.string.alarm_type_system_battery_low)
    AlarmType.SYSTEM_BLUETOOTH_OFF -> stringResource(id = R.string.alarm_type_system_bluetooth_off_desc)
    AlarmType.SYSTEM_THERAPY_SETTINGS_MISSING -> stringResource(id = R.string.alarm_type_system_therapy_settings_missing_desc)
}

@Preview(showBackground = true, name = "Critical Low BG Alarm")
@Composable
fun FullScreenAlarmScreenCriticalPreview() {
    AppPreview {
        FullScreenAlarmScreen(
            alarmType = AlarmType.CRITICAL_LOW_BG,
            bgValue = BgValue.fromMgDl(55),
            glucoseUnit = GlucoseUnit.MG_DL,
            onSnooze = {}
        )
    }
}

@Preview(showBackground = true, name = "Multi Alarm Preview")
@Composable
fun FullScreenAlarmScreenMultiPreview() {
    AppPreview {
        val group = ActiveAlarmGroup(
            heroAlarms = listOf(AlarmType.PUMP_OCCLUSION, AlarmType.HIGH_BG),
            otherAlarms = listOf(AlarmType.PUMP_LOW_BATTERY),
            highestSeverity = AlarmSeverity.CRITICAL,
            primaryAlarm = AlarmType.PUMP_OCCLUSION
        )
        FullScreenAlarmScreen(
            alarmGroup = group,
            bgValue = BgValue.fromMgDl(280),
            glucoseUnit = GlucoseUnit.MG_DL,
            onSnoozeAll = {}
        )
    }
}