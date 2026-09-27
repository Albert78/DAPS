package de.dh.daps.ui.screens.systemcontrol

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.dh.daps.common.model.data.BgReading
import de.dh.daps.common.model.data.BgReadingsInterval
import de.dh.daps.common.model.data.BgSampleKind
import de.dh.daps.common.model.data.BgValue
import de.dh.daps.common.model.data.Timestamp
import de.dh.daps.common.ui.UiText
import de.dh.daps.ui.common.glucoseValue
import de.dh.daps.ui.common.icons.Icon_Next
import de.dh.daps.ui.common.icons.Icon_Previous
import de.dh.daps.ui.common.longDateTime
import de.dh.daps.ui.common.readingsInterval
import de.dh.daps.ui.common.shortRelativeTimeAgo
import de.dh.daps.ui.common.shortRelativeTimeUntil
import de.dh.daps.ui.common.theme.AppTheme
import de.dh.daps.ui.common.time

@Composable
fun GlucoseSourceTabContent(
    modifier: Modifier = Modifier,
    uiState: GlucoseSourceTabUiState = GlucoseSourceTabUiState(),
    onChangeGlucoseSource: () -> Unit = {},
    @Suppress("UNUSED_PARAMETER") onStopSensor: () -> Unit = {}
) {
    Column(modifier = modifier) {
        GlucoseSourceOverviewCard(
            uiState = uiState,
            onChangeGlucoseSource = onChangeGlucoseSource
        )

        if (uiState.glucoseSourcePluginSection != null) {
            Spacer(modifier = Modifier.height(8.dp))
            SectionHeader(
                title = "Datenquelle"
            )
            Spacer(modifier = Modifier.height(8.dp))
            uiState.glucoseSourcePluginSection.invoke()
        }
    }
}

@Composable
fun GlucoseSourceOverviewCard(
    uiState: GlucoseSourceTabUiState,
    modifier: Modifier = Modifier,
    onChangeGlucoseSource: () -> Unit = {}
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    ControlDetailRow(
                        label = "Glukose-Quelle",
                        icon = Icons.Default.Info
                    ) {
                        Text(
                            text = uiState.glucoseSourceName?.asString() ?: "Nicht verbunden",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                OutlinedButton(
                    onClick = onChangeGlucoseSource
                ) {
                    Icon(
                        imageVector = Icons.Default.SwapHoriz,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (uiState.glucoseSourceName != null) "Wechseln..." else "Einrichten...",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }

            if (uiState.glucoseSourceName != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        ControlDetailRow(
                            label = "Sensor-Typ",
                            reserveIconSpace = true
                        ) {
                            Text(
                                text = uiState.sensorTypeName ?: "--",
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        ControlDetailRow(
                            label = "Messintervall",
                            reserveIconSpace = true
                        ) {
                            val intervalText = readingsInterval(uiState.readingsInterval)
                            Text(
                                text = intervalText,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 20.dp),
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outlineVariant
                )

                val lastReading = uiState.lastBgReading
                val bgValueText = if (lastReading != null) {
                    glucoseValue(lastReading.value, withUnit = true)
                } else {
                    "--"
                }

                val lastTimeText = if (lastReading != null && lastReading.timestamp.isValid()) {
                    time(lastReading.timestamp)
                } else {
                    "--"
                }

                val lastRelativeTime = if (lastReading != null && lastReading.timestamp.isValid()) {
                    shortRelativeTimeAgo(lastReading.timestamp)
                } else {
                    null
                }

                ControlDetailRow(
                    label = "Letzter Messwert",
                    icon = Icon_Previous
                ) {
                    GlucoseFragments(
                        value = bgValueText,
                        time = lastTimeText,
                        extra = lastRelativeTime,
                        stackVertical = true
                    )
                }

                if (uiState.hasNextPrediction) {
                    val nextPred = uiState.nextPredictedTimestamp
                    val nextTimeText = if (nextPred != null && nextPred.isValid()) {
                        time(nextPred)
                    } else {
                        "--"
                    }

                    val nextRelativeTime = if (nextPred != null && nextPred.isValid()) {
                        shortRelativeTimeUntil(nextPred)
                    } else {
                        null
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    ControlDetailRow(
                        label = "Nächste Messung",
                        icon = Icon_Next
                    ) {
                        GlucoseFragments(
                            value = "--",
                            time = nextTimeText,
                            extra = nextRelativeTime,
                            stackVertical = true
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GlucoseFragments(
    value: String,
    time: String,
    extra: String?,
    stackVertical: Boolean = false
) {
    if (stackVertical) {
        Column {
            FlowRow(
                verticalArrangement = Arrangement.Center,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = time,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (extra != null) {
                    Text(
                        text = "($extra)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if (value.isNotEmpty() && value != "--") {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    } else {
        FlowRow(
            verticalArrangement = Arrangement.Center,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (value.isNotEmpty() && value != "--") {
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = time,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.align(Alignment.CenterVertically),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (extra != null) {
                Text(
                    text = "($extra)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.align(Alignment.CenterVertically),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun GlucoseSourcePluginExampleCard(
    sensorCode: String?,
    transmitterSerialNumber: String?,
    onStopSensor: () -> Unit,
    modifier: Modifier = Modifier,
    estimatedExpirationTimestamp: Timestamp? = null
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Text(
                text = "Sensor-Steuerung",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    ControlDetailRow(
                        label = "Sensor-Code"
                    ) {
                        Text(
                            text = sensorCode ?: "--",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    ControlDetailRow(
                        label = "Transmitter-Seriennummer"
                    ) {
                        Text(
                            text = transmitterSerialNumber ?: "--",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            val expDateDisplay = longDateTime(estimatedExpirationTimestamp)

            ControlDetailRow(
                label = "Geschätztes Ablaufdatum"
            ) {
                Text(
                    text = expDateDisplay,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onStopSensor,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Stop,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Sensor stoppen",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

private fun sampleGlucoseSourceTabUiState() = GlucoseSourceTabUiState(
    glucoseSourceName = UiText.DynamicString("Dexcom G6"),
    manufacturer = "Dexcom",
    serialNumber = "SN-98765432",
    sensorTypeName = "G6-Sensor",
    readingsInterval = BgReadingsInterval.FiveMinutes,
    lastBgReading = BgReading(
        value = BgValue.fromMgDl(124),
        sampleKind = BgSampleKind.Value,
        timestamp = Timestamp(System.currentTimeMillis() - 120_000)
    ),
    hasNextPrediction = true,
    nextPredictedTimestamp = Timestamp(System.currentTimeMillis() + 180_000),
    sensorCode = "8132",
    transmitterSerialNumber = "8G1234",
    estimatedExpirationTimestamp = Timestamp(System.currentTimeMillis() + 864000000),
    glucoseSourcePluginSection = {
        GlucoseSourcePluginExampleCard(
            sensorCode = "8132",
            transmitterSerialNumber = "8G1234",
            estimatedExpirationTimestamp = Timestamp(System.currentTimeMillis() + 864000000),
            onStopSensor = {}
        )
    }
)

@Preview(showBackground = true, name = "Glucose Source Tab - Light Mode")
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, name = "Glucose Source Tab - Dark Mode")
@Composable
fun GlucoseSourceTabPreview() {
    AppTheme {
        Surface {
            GlucoseSourceTabContent(
                uiState = sampleGlucoseSourceTabUiState(),
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@Preview(showBackground = true, name = "Glucose Source Tab - Disconnected")
@Composable
fun GlucoseSourceTabDisconnectedPreview() {
    AppTheme {
        Surface {
            GlucoseSourceTabContent(
                uiState = GlucoseSourceTabUiState(
                    glucoseSourceName = null,
                    sensorTypeName = null,
                    readingsInterval = null,
                    lastBgReading = null,
                    hasNextPrediction = false,
                    sensorCode = null,
                    transmitterSerialNumber = null,
                    estimatedExpirationTimestamp = null
                ),
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}