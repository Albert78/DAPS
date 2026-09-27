package de.dh.daps.ui.screens.systemcontrol

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import de.dh.daps.common.model.GlucoseSourceStatus
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.dh.daps.common.model.InsulinAmount
import de.dh.daps.common.model.data.BgReading
import de.dh.daps.common.model.data.Timestamp
import de.dh.daps.common.ui.UiText
import de.dh.daps.ui.common.LocalGlucoseUnit
import de.dh.daps.ui.common.composables.AppColorBlue
import de.dh.daps.ui.common.glucoseUnitLabel
import de.dh.daps.ui.common.insulinValue
import de.dh.daps.ui.common.shortDateTime
import de.dh.daps.ui.common.shortRelativeTimeAgo
import de.dh.daps.ui.common.shortRelativeTimeUntil
import de.dh.daps.ui.common.theme.AppTheme
import de.dh.daps.ui.common.theme.ExtendedTheme
import de.dh.daps.ui.common.time

@Composable
fun OverviewTabContent(
    modifier: Modifier = Modifier,
    uiState: OverviewTabUiState = OverviewTabUiState(),
    onRefreshPumpStatus: () -> Unit = {},
    onNavigateToCoreDecisions: () -> Unit = {}
) {
    Column(
        modifier = modifier
    ) {
        // Section Algorithm
        SectionHeader(title = "APS-System")
        Spacer(modifier = Modifier.height(8.dp))

        ApsCard(
            state = uiState.apsSystem,
            onNavigateToCoreDecisions = onNavigateToCoreDecisions
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Section System
        SectionHeader(title = "System")
        Spacer(modifier = Modifier.height(8.dp))

        AndroidSystemCard(state = uiState.androidSystem)

        Spacer(modifier = Modifier.height(16.dp))

        // Section Glucose Source
        SectionHeader(title = "Blutzucker-Quelle")
        Spacer(modifier = Modifier.height(8.dp))

        OverviewGlucoseSourceCard(state = uiState.glucoseSource)

        Spacer(modifier = Modifier.height(16.dp))

        // Section Pump
        SectionHeader(title = "Insulinpumpe")
        Spacer(modifier = Modifier.height(8.dp))

        OverviewPumpCard(
            state = uiState.insulinPump,
            onRefresh = onRefreshPumpStatus
        )
    }
}

@Composable
private fun AndroidSystemCard(
    state: OverviewAndroidSystemUiState
) {
    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        border = BorderStroke(2.dp, AppColorBlue.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Android",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    ControlDetailRow(label = "Bluetooth-Status") {
                        StatusMetricText(metric = state.bluetoothStatus)
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    ControlDetailRow(label = "Batteriestatus Telefon") {
                        StatusMetricText(metric = state.phoneBattery)
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    ControlDetailRow(label = "Berechtigungen") {
                        StatusMetricText(
                            metric = state.permissionsStatus,
                            formatValue = { count -> if (count == 0) "Alle erteilt" else "$count fehlend" }
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    ControlDetailRow(label = "DAPS-System-Service") {
                        StatusMetricText(metric = state.dapsServiceStatus)
                    }
                }
            }
        }
    }
}

@Composable
private fun ApsCard(
    state: OverviewApsSystemUiState,
    onNavigateToCoreDecisions: () -> Unit = {}
) {
    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        border = BorderStroke(2.dp, AppColorBlue.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "APS-System",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
            )

            ControlDetailRow(label = "Status") {
                StatusMetricText(metric = state.status)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    ControlDetailRow(label = "APS-Modus") {
                        StatusMetricText(metric = state.mode)
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    ControlDetailRow(label = "Letzte Berechnung") {
                        StatusMetricText(metric = state.lastCalculation)
                    }
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(top = 4.dp),
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onNavigateToCoreDecisions,
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(
                        text = "Verlaufsprotokoll anzeigen",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun OverviewGlucoseSourceCard(
    state: OverviewGlucoseSourceUiState?
) {
    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        border = BorderStroke(2.dp, AppColorBlue.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            if (state == null) {
                Text(
                    text = "Keine Blutzucker-Quelle konfiguriert",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    text = state.sensorName.asString(),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        ControlDetailRow(label = "Status") {
                            StatusMetricText(metric = state.status)
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        ControlDetailRow(label = "Ablaufdatum Sensor") {
                            StatusMetricText(
                                metric = state.sensorExpiration,
                                isDateTime = true
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        ControlDetailRow(label = "Letzte Verbindung") {
                            StatusMetricText(metric = state.lastConnection)
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        ControlDetailRow(label = "Letzter Messwert") {
                            StatusMetricText(metric = state.lastReading)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OverviewPumpCard(
    state: OverviewPumpUiState?,
    onRefresh: () -> Unit = {}
) {
    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        border = BorderStroke(2.dp, AppColorBlue.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            if (state == null) {
                Text(
                    text = "Keine Insulinpumpe konfiguriert",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = state.pumpName.asString(),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onRefresh) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Pumpenstatus aktualisieren",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        ControlDetailRow(label = "Status") {
                            StatusMetricText(metric = state.state)
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        ControlDetailRow(label = "Nächster Kanülenwechsel") {
                            StatusMetricText(
                                metric = state.nextCannulaChange,
                                isDateTime = true
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        ControlDetailRow(label = "Batteriestatus") {
                            StatusMetricText(metric = state.battery)
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        ControlDetailRow(label = "Reservoir-Füllstand") {
                            StatusMetricText(metric = state.reservoir)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        ControlDetailRow(label = "Letzte Verbindung") {
                            StatusMetricText(metric = state.lastConnection)
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        ControlDetailRow(label = "Letzter Bolus") {
                            StatusMetricText(metric = state.lastBolus)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun <T> StatusMetricText(
    metric: StatusMetric<T>,
    modifier: Modifier = Modifier,
    isDateTime: Boolean = false,
    formatValue: ((T) -> String)? = null
) {
    val glucoseUnit = LocalGlucoseUnit.current
    val timestamp = metric.value as? Timestamp

    val formattedValue = if (metric.value != null && formatValue != null) {
        formatValue(metric.value)
    } else {
        when (val v = metric.value) {
            is Boolean -> if (v) "Aktiv" else "Inaktiv"
            is OverviewPumpState -> when (v) {
                OverviewPumpState.ACTIVE -> "Aktiv"
                OverviewPumpState.SUSPENDED -> "Suspended"
                OverviewPumpState.ERROR -> "Fehler"
            }
            is GlucoseSourceStatus -> when (v) {
                GlucoseSourceStatus.Ok -> "OK"
                GlucoseSourceStatus.Expired -> "Abgelaufen"
                GlucoseSourceStatus.Error -> "Fehler"
            }
            is Int -> "$v%"
            is InsulinAmount -> insulinValue(v)
            is BgReading -> "${v.value.toString(glucoseUnit)} ${glucoseUnitLabel(glucoseUnit)}"
            is Timestamp -> if (v.isValid()) {
                if (isDateTime) {
                    shortDateTime(v)
                } else {
                    time(v)
                }
            } else "--"
            is String -> v
            null -> "--"
            else -> v.toString()
        }
    }
    val relativeTime = if (timestamp != null && timestamp.isValid()) {
        if (isDateTime) {
            val rel = shortRelativeTimeUntil(timestamp)
            if (rel.isNotEmpty()) "($rel)" else null
        } else {
            shortRelativeTimeAgo(timestamp)
        }
    } else null

    StatusValueText(
        value = formattedValue,
        relativeTime = relativeTime,
        status = metric.status,
        modifier = modifier
    )
}

@Composable
private fun StatusValueText(
    value: String,
    modifier: Modifier = Modifier,
    relativeTime: String? = null,
    status: ValueStatus? = null
) {
    Column(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (status != null) {
                val (icon, tint) = when (status) {
                    ValueStatus.GOOD -> Icons.Default.Check to ExtendedTheme.semanticColors.good
                    ValueStatus.WARNING -> Icons.Default.Warning to ExtendedTheme.semanticColors.warning
                    ValueStatus.BAD -> Icons.Default.Error to ExtendedTheme.semanticColors.bad
                }
                Icon(
                    imageVector = icon,
                    contentDescription = status.name,
                    tint = tint,
                    modifier = Modifier.size(16.dp)
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
        if (relativeTime != null) {
            Text(
                text = relativeTime,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = if (status != null) 20.dp else 0.dp)
            )
        }
    }
}

internal fun sampleOverviewTabUiState() = OverviewTabUiState(
    androidSystem = OverviewAndroidSystemUiState(
        bluetoothStatus = StatusMetric(true, status = ValueStatus.GOOD),
        phoneBattery = StatusMetric(82, status = ValueStatus.GOOD),
        permissionsStatus = StatusMetric(0, status = ValueStatus.GOOD),
        dapsServiceStatus = StatusMetric(true, status = ValueStatus.GOOD)
    ),
    apsSystem = OverviewApsSystemUiState(
        mode = StatusMetric("Auto-Korrektur", status = ValueStatus.GOOD),
        lastCalculation = StatusMetric(Timestamp(System.currentTimeMillis() - 120_000), status = ValueStatus.GOOD),
        status = StatusMetric("Aktiv", status = ValueStatus.GOOD)
    ),
    glucoseSource = OverviewGlucoseSourceUiState(
        sensorName = UiText.DynamicString("SimBody Virtueller Glukosesensor"),
        status = StatusMetric(GlucoseSourceStatus.Ok, status = ValueStatus.GOOD),
        lastConnection = StatusMetric(Timestamp(System.currentTimeMillis() - 60_000), status = ValueStatus.GOOD),
        lastReading = StatusMetric(Timestamp(System.currentTimeMillis() - 120_000), status = ValueStatus.GOOD),
        sensorExpiration = StatusMetric(Timestamp(System.currentTimeMillis() + 864_000_000), status = ValueStatus.GOOD)
    ),
    insulinPump = OverviewPumpUiState(
        pumpName = UiText.DynamicString("SimBody Virtuelle Insulinpumpe"),
        state = StatusMetric(OverviewPumpState.ACTIVE, status = ValueStatus.GOOD),
        lastBolus = StatusMetric(Timestamp(System.currentTimeMillis() - 600_000), status = ValueStatus.GOOD),
        battery = StatusMetric(85, status = ValueStatus.GOOD),
        reservoir = StatusMetric(InsulinAmount(140.0), status = ValueStatus.GOOD),
        lastConnection = StatusMetric(Timestamp(System.currentTimeMillis() - 60_000), status = ValueStatus.GOOD),
        nextCannulaChange = StatusMetric(Timestamp(System.currentTimeMillis() + 172_800_000), status = ValueStatus.GOOD)
    )
)

@Preview(showBackground = true, heightDp = 1200, name = "Light Mode")
@Preview(showBackground = true, heightDp = 1200, uiMode = Configuration.UI_MODE_NIGHT_YES, name = "Dark Mode")
@Composable
fun OverviewTabPreview() {
    AppTheme {
        Surface {
            OverviewTabContent(
                uiState = sampleOverviewTabUiState(),
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}