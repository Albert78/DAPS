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
import de.dh.daps.common.domain.model.alarm.ActiveAlarm
import de.dh.daps.common.domain.model.alarm.ActiveAlarmGroup
import de.dh.daps.common.domain.model.alarm.AlarmSeverity
import de.dh.daps.common.domain.model.alarm.AlarmType
import de.dh.daps.common.domain.model.data.BgValue
import de.dh.daps.common.domain.model.data.GlucoseUnit
import de.dh.daps.ui.R
import de.dh.daps.ui.common.composables.FramedCard
import de.dh.daps.ui.common.composables.PrimaryButton
import de.dh.daps.ui.common.composables.contentScrollIndicator
import de.dh.daps.ui.common.descriptionResId
import de.dh.daps.ui.common.theme.AppPreview
import de.dh.daps.ui.common.titleResId

@Composable
fun FullScreenAlarmScreen(
    alarmGroup: ActiveAlarmGroup,
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
                            val bgValue = when (heroAlarm) {
                                is ActiveAlarm.CriticalLowBg -> heroAlarm.bgValue
                                is ActiveAlarm.LowBg -> heroAlarm.bgValue
                                is ActiveAlarm.HighBg -> heroAlarm.bgValue
                                else -> null
                            }
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
fun getAlarmTypeTitle(alarmType: AlarmType): String = stringResource(id = alarmType.titleResId)

@Composable
fun getAlarmTypeTitle(alarm: ActiveAlarm): String = stringResource(id = alarm.titleResId)

@Composable
fun getAlarmTypeDescription(alarm: ActiveAlarm): String = when (alarm) {
    is ActiveAlarm.CgmSignalLoss -> stringResource(id = R.string.aps_issue_stale_bg, alarm.minutes)
    else -> alarm.descriptionResId?.let { stringResource(id = it) } ?: stringResource(id = alarm.titleResId)
}

@Preview(showBackground = true, name = "Critical Low BG Alarm")
@Composable
fun FullScreenAlarmScreenCriticalPreview() {
    AppPreview {
        val criticalAlarm = ActiveAlarm.CriticalLowBg(BgValue.fromMgDl(55))
        val group = ActiveAlarmGroup(
            heroAlarms = listOf(criticalAlarm),
            otherAlarms = emptyList(),
            highestSeverity = AlarmSeverity.CRITICAL,
            primaryAlarm = criticalAlarm
        )
        FullScreenAlarmScreen(
            alarmGroup = group,
            glucoseUnit = GlucoseUnit.MG_DL,
            onSnoozeAll = {}
        )
    }
}

@Preview(showBackground = true, name = "Multi Alarm Preview")
@Composable
fun FullScreenAlarmScreenMultiPreview() {
    AppPreview {
        val group = ActiveAlarmGroup(
            heroAlarms = listOf(ActiveAlarm.Generic(AlarmType.PUMP_OCCLUSION), ActiveAlarm.HighBg(BgValue.fromMgDl(280))),
            otherAlarms = listOf(ActiveAlarm.Generic(AlarmType.PUMP_LOW_BATTERY)),
            highestSeverity = AlarmSeverity.CRITICAL,
            primaryAlarm = ActiveAlarm.Generic(AlarmType.PUMP_OCCLUSION)
        )
        FullScreenAlarmScreen(
            alarmGroup = group,
            glucoseUnit = GlucoseUnit.MG_DL,
            onSnoozeAll = {}
        )
    }
}