package de.dh.daps.ui.screens.systemcontrol

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.dh.daps.common.model.Expiration
import de.dh.daps.common.model.ExpirationDate
import de.dh.daps.common.model.ReplaceableComponentType
import de.dh.daps.common.model.data.BgReading
import de.dh.daps.common.model.data.BgReadingsInterval
import de.dh.daps.common.model.data.BgSampleKind
import de.dh.daps.common.model.data.BgValue
import de.dh.daps.common.model.data.Timestamp
import de.dh.daps.common.ui.UiText
import de.dh.daps.ui.R
import de.dh.daps.ui.common.composables.FramedCard
import de.dh.daps.ui.common.glucoseValue
import de.dh.daps.ui.common.icons.Icon_Next
import de.dh.daps.ui.common.icons.Icon_Previous
import de.dh.daps.ui.common.readingsInterval
import de.dh.daps.ui.common.shortDate
import de.dh.daps.ui.common.shortDateTime
import de.dh.daps.ui.common.shortRelativeTimeAgo
import de.dh.daps.ui.common.shortRelativeTimeUntil
import de.dh.daps.ui.common.theme.AppTheme
import de.dh.daps.ui.common.theme.ExtendedTheme
import de.dh.daps.ui.common.time
import java.time.ZoneId

@Composable
fun SourceTabContent(
    modifier: Modifier = Modifier,
    uiState: SourceTabUiState = SourceTabUiState.Loading,
    onChangeGlucoseSource: () -> Unit = {},
    @Suppress("UNUSED_PARAMETER") onStopSensor: () -> Unit = {}
) {
    Column(modifier = modifier) {
        val mainHeadlineText = when (uiState) {
            is SourceTabUiState.Loading -> stringResource(R.string.overview_section_glucose_source)
            is SourceTabUiState.NoneConfigured -> stringResource(R.string.system_control_source_none_active)
            is SourceTabUiState.Content -> uiState.glucoseSourceName.asString()
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = mainHeadlineText,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            if (uiState !is SourceTabUiState.Loading) {
                Spacer(modifier = Modifier.width(16.dp))
                OutlinedIconButton(onClick = onChangeGlucoseSource) {
                    Icon(
                        imageVector = Icons.Default.SwapHoriz,
                        contentDescription = stringResource(
                            if (uiState is SourceTabUiState.NoneConfigured) R.string.system_control_source_setup
                            else R.string.system_control_source_change
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        SectionHeader(
            title = stringResource(R.string.system_control_tab_overview)
        )
        Spacer(modifier = Modifier.height(8.dp))

        SourceOverviewCard(
            uiState = uiState
        )

        if (uiState is SourceTabUiState.Content && uiState.glucoseSourcePluginSection != null) {
            Spacer(modifier = Modifier.height(8.dp))
            SectionHeader(
                title = stringResource(R.string.system_control_source_plugin_section_title)
            )
            Spacer(modifier = Modifier.height(8.dp))
            uiState.glucoseSourcePluginSection.invoke()
        }
    }
}

@Composable
fun SourceOverviewCard(
    uiState: SourceTabUiState,
    modifier: Modifier = Modifier
) {
    FramedCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            when (uiState) {
                is SourceTabUiState.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                is SourceTabUiState.NoneConfigured -> {
                    Text(
                        text = stringResource(R.string.system_control_source_none_active),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                is SourceTabUiState.Content -> {
                    val notAvailableText = stringResource(R.string.system_control_value_not_available)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            ControlDetailRow(
                                label = stringResource(R.string.system_control_source_manufacturer_label),
                                reserveIconSpace = true
                            ) {
                                Text(
                                    text = uiState.manufacturer ?: notAvailableText,
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            ControlDetailRow(
                                label = stringResource(R.string.system_control_source_model_label),
                                reserveIconSpace = true
                            ) {
                                Text(
                                    text = uiState.model ?: notAvailableText,
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
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
                            ControlDetailRow(
                                label = stringResource(R.string.system_control_source_serial_number_label),
                                reserveIconSpace = true
                            ) {
                                Text(
                                    text = uiState.serialNumber ?: notAvailableText,
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            ControlDetailRow(
                                label = stringResource(R.string.system_control_source_measurement_interval_label),
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

                    Spacer(modifier = Modifier.height(12.dp))

                    val startDateText = if (uiState.startDate != null && uiState.startDate.isValid()) {
                        shortDateTime(uiState.startDate)
                    } else {
                        notAvailableText
                    }
                    val startDateRel = if (uiState.startDate != null && uiState.startDate.isValid()) {
                        val rel = shortRelativeTimeAgo(uiState.startDate)
                        if (rel.isNotEmpty()) "($rel)" else null
                    } else {
                        null
                    }

                    val (expDateText, expDateRel) = when (val expDate = uiState.expiration?.date) {
                        is ExpirationDate.Hard -> {
                            val dt = shortDateTime(expDate.dateTime)
                            val rel = if (expDate.dateTime.ms < System.currentTimeMillis()) {
                                shortRelativeTimeAgo(expDate.dateTime)
                            } else {
                                shortRelativeTimeUntil(expDate.dateTime)
                            }
                            dt to (if (rel.isNotEmpty()) "($rel)" else null)
                        }
                        is ExpirationDate.Approximate -> {
                            val d = shortDate(expDate.date)
                            val targetMs = expDate.date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                            val diffMs = targetMs - System.currentTimeMillis()
                            val rel = if (diffMs < 0) {
                                shortRelativeTimeAgo(-diffMs)
                            } else {
                                shortRelativeTimeUntil(diffMs)
                            }
                            d to (if (rel.isNotEmpty()) "($rel)" else null)
                        }
                        null -> notAvailableText to null
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            ControlDetailRow(
                                label = stringResource(R.string.system_control_source_start_time_label),
                                reserveIconSpace = true
                            ) {
                                Column {
                                    Text(
                                        text = startDateText,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (startDateRel != null) {
                                        Text(
                                            text = startDateRel,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            ControlDetailRow(
                                label = stringResource(R.string.system_control_source_expiration_label),
                                reserveIconSpace = true
                            ) {
                                Column {
                                    Text(
                                        text = expDateText,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (expDateRel != null) {
                                        Text(
                                            text = expDateRel,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant
                    )

                    val statusText = when {
                        uiState.isExpired -> stringResource(R.string.overview_glucose_source_status_expired)
                        else -> stringResource(R.string.label_active)
                    }
                    val statusColor = when {
                        uiState.isExpired -> MaterialTheme.colorScheme.error
                        else -> ExtendedTheme.semanticColors.good
                    }

                    val lastReading = uiState.lastBgReading
                    val bgValueText = if (lastReading != null) {
                        glucoseValue(lastReading.value, withUnit = true)
                    } else {
                        notAvailableText
                    }

                    val lastTimeText = if (lastReading != null && lastReading.timestamp.isValid()) {
                        time(lastReading.timestamp)
                    } else {
                        notAvailableText
                    }

                    val lastRelativeTime = if (lastReading != null && lastReading.timestamp.isValid()) {
                        shortRelativeTimeAgo(lastReading.timestamp)
                    } else {
                        null
                    }

                    ControlDetailRow(
                        label = stringResource(R.string.overview_status_label),
                        icon = Icons.Default.Settings
                    ) {
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = statusColor,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            ControlDetailRow(
                                label = stringResource(R.string.system_control_source_last_reading_label),
                                icon = Icon_Previous
                            ) {
                                GlucoseFragments(
                                    value = bgValueText,
                                    time = lastTimeText,
                                    extra = lastRelativeTime,
                                    stackVertical = true
                                )
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            if (uiState.hasNextPrediction) {
                                val nextPred = uiState.nextPredictedTimestamp
                                val nextTimeText = if (nextPred != null && nextPred.isValid()) {
                                    time(nextPred)
                                } else {
                                    notAvailableText
                                }

                                val nextRelativeTime = if (nextPred != null && nextPred.isValid()) {
                                    shortRelativeTimeUntil(nextPred)
                                } else {
                                    null
                                }

                                ControlDetailRow(
                                    label = stringResource(R.string.system_control_source_next_reading_label),
                                    icon = Icon_Next
                                ) {
                                    GlucoseFragments(
                                        value = notAvailableText,
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
    modifier: Modifier = Modifier
) {
    FramedCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Text(
                text = stringResource(R.string.system_control_source_control_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(16.dp))

            val notAvailableText = stringResource(R.string.system_control_value_not_available)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    ControlDetailRow(
                        label = stringResource(R.string.system_control_source_sensor_code_label)
                    ) {
                        Text(
                            text = sensorCode ?: notAvailableText,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    ControlDetailRow(
                        label = stringResource(R.string.system_control_source_transmitter_sn_label)
                    ) {
                        Text(
                            text = transmitterSerialNumber ?: notAvailableText,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
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
                    text = stringResource(R.string.system_control_source_action_stop_sensor),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

internal fun sampleSourceTabUiState(): SourceTabUiState = SourceTabUiState.Content(
    glucoseSourceName = UiText.DynamicString("SimBody Virtueller Glukosesensor"),
    manufacturer = "DAPS",
    model = "SimBody Glukosesensor",
    serialNumber = "12345",
    readingsInterval = BgReadingsInterval.FiveMinutes,
    startDate = Timestamp(System.currentTimeMillis() - 86_400_000),
    lastBgReading = BgReading(
        value = BgValue.fromMgDl(124),
        sampleKind = BgSampleKind.Value,
        timestamp = Timestamp(System.currentTimeMillis() - 120_000)
    ),
    hasNextPrediction = true,
    nextPredictedTimestamp = Timestamp(System.currentTimeMillis() + 180_000),
    expiration = Expiration(
        type = ReplaceableComponentType.Sensor,
        date = ExpirationDate.Hard(Timestamp(System.currentTimeMillis() + 864000000))
    ),
    glucoseSourcePluginSection = {
        GlucoseSourcePluginExampleCard(
            sensorCode = "8132",
            transmitterSerialNumber = "8G1234",
            onStopSensor = {}
        )
    }
)

internal fun sampleSourceTabExpiredUiState(): SourceTabUiState = SourceTabUiState.Content(
    glucoseSourceName = UiText.DynamicString("SimBody Virtueller Glukosesensor"),
    manufacturer = "DAPS",
    model = "SimBody Glukosesensor",
    serialNumber = "12345",
    readingsInterval = BgReadingsInterval.FiveMinutes,
    startDate = Timestamp(System.currentTimeMillis() - 10 * 86_400_000L),
    lastBgReading = BgReading(
        value = BgValue.fromMgDl(110),
        sampleKind = BgSampleKind.Value,
        timestamp = Timestamp(System.currentTimeMillis() - 900_000)
    ),
    hasNextPrediction = false,
    nextPredictedTimestamp = null,
    expiration = Expiration(
        type = ReplaceableComponentType.Sensor,
        date = ExpirationDate.Hard(Timestamp(System.currentTimeMillis() - 7_200_000L)),
        graceUntil = Timestamp(System.currentTimeMillis() - 1_800_000L)
    ),
    isExpired = true,
    glucoseSourcePluginSection = {
        GlucoseSourcePluginExampleCard(
            sensorCode = "8132",
            transmitterSerialNumber = "8G1234",
            onStopSensor = {}
        )
    }
)

@Preview(showBackground = true, name = "Source Tab - Light Mode")
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, name = "Source Tab - Dark Mode")
@Composable
fun SourceTabPreview() {
    AppTheme {
        Surface {
            SourceTabContent(
                uiState = sampleSourceTabUiState(),
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@Preview(showBackground = true, name = "Source Tab - Expired")
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, name = "Source Tab - Expired - Dark Mode")
@Composable
fun SourceTabExpiredPreview() {
    AppTheme {
        Surface {
            SourceTabContent(
                uiState = sampleSourceTabExpiredUiState(),
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@Preview(showBackground = true, name = "Source Tab - Loading")
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, name = "Source Tab - Loading - Dark Mode")
@Composable
fun SourceTabLoadingPreview() {
    AppTheme {
        Surface {
            SourceTabContent(
                uiState = SourceTabUiState.Loading,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@Preview(showBackground = true, name = "Source Tab - Disconnected")
@Composable
fun SourceTabDisconnectedPreview() {
    AppTheme {
        Surface {
            SourceTabContent(
                uiState = SourceTabUiState.NoneConfigured,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}