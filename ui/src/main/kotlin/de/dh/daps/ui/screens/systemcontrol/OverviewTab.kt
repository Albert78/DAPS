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
import de.dh.daps.common.model.data.Timestamp
import de.dh.daps.ui.common.composables.AppColorBlue
import de.dh.daps.ui.common.longDateTime
import de.dh.daps.ui.common.shortRelativeTimeAgo
import de.dh.daps.ui.common.theme.AppTheme
import de.dh.daps.ui.common.theme.ExtendedTheme
import de.dh.daps.ui.common.time
import java.time.Instant
import java.time.ZoneId

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

        // Section CGM
        SectionHeader(title = "Blutzucker-Sensor (CGM)")
        Spacer(modifier = Modifier.height(8.dp))

        OverviewCgmCard(state = uiState.cgm)

        Spacer(modifier = Modifier.height(16.dp))

        // Section Pump
        SectionHeader(title = "Insulinpumpe")
        Spacer(modifier = Modifier.height(8.dp))

        OverviewPumpCard(
            state = uiState.pump,
            onRefresh = onRefreshPumpStatus
        )
    }
}

@Composable
private fun AndroidSystemCard(
    state: AndroidSystemUiState
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
                text = state.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    ControlDetailRow(label = state.bluetoothStatus.label) {
                        StatusValueText(item = state.bluetoothStatus)
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    ControlDetailRow(label = state.phoneBatteryStatus.label) {
                        StatusValueText(item = state.phoneBatteryStatus)
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    ControlDetailRow(label = state.permissionsStatus.label) {
                        StatusValueText(item = state.permissionsStatus)
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    ControlDetailRow(label = state.dapsServiceStatus.label) {
                        StatusValueText(item = state.dapsServiceStatus)
                    }
                }
            }
        }
    }
}

@Composable
private fun ApsCard(
    state: ApsSystemUiState,
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
                text = state.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    ControlDetailRow(label = state.mode.label) {
                        StatusValueText(item = state.mode)
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    ControlDetailRow(label = state.lastCalculation.label) {
                        StatusValueText(item = state.lastCalculation)
                    }
                }
            }

            ControlDetailRow(label = state.status.label) {
                StatusValueText(item = state.status)
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
private fun OverviewCgmCard(
    state: OverviewCgmUiState
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
            Text(
                text = state.sensorName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    ControlDetailRow(label = state.lastConnection.label) {
                        StatusValueText(item = state.lastConnection)
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    ControlDetailRow(label = state.lastReading.label) {
                        StatusValueText(item = state.lastReading)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    ControlDetailRow(label = state.sensorExpiration.label) {
                        StatusValueText(
                            item = state.sensorExpiration,
                            isDateTime = true
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OverviewPumpCard(
    state: OverviewPumpUiState,
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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = state.pumpName,
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
                    ControlDetailRow(label = state.status.label) {
                        StatusValueText(item = state.status)
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    ControlDetailRow(label = state.lastBolus.label) {
                        StatusValueText(item = state.lastBolus)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    ControlDetailRow(label = state.batteryStatus.label) {
                        StatusValueText(item = state.batteryStatus)
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    ControlDetailRow(label = state.reservoirStatus.label) {
                        StatusValueText(item = state.reservoirStatus)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    ControlDetailRow(label = state.lastConnection.label) {
                        StatusValueText(item = state.lastConnection)
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    ControlDetailRow(label = state.nextPodChange.label) {
                        StatusValueText(
                            item = state.nextPodChange,
                            isDateTime = true
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusValueText(
    item: StatusValueItem,
    modifier: Modifier = Modifier,
    isDateTime: Boolean = false
) {
    val timestamp = item.timestamp
    val formattedValue = if (timestamp != null && timestamp.isValid()) {
        if (isDateTime) {
            val localDateTime = Instant.ofEpochMilli(timestamp.ms)
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime()
            longDateTime(localDateTime)
        } else {
            time(timestamp)
        }
    } else {
        item.value
    }

    val formattedRelative = if (timestamp != null && timestamp.isValid() && !isDateTime) {
        shortRelativeTimeAgo(timestamp)
    } else {
        item.relativeTime
    }

    StatusValueText(
        value = formattedValue,
        relativeTime = formattedRelative,
        status = item.status,
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

@Preview(showBackground = true, heightDp = 1200, name = "Light Mode")
@Preview(showBackground = true, heightDp = 1200, uiMode = Configuration.UI_MODE_NIGHT_YES, name = "Dark Mode")
@Composable
fun OverviewTabPreview() {
    AppTheme {
        Surface {
            OverviewTabContent(
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}