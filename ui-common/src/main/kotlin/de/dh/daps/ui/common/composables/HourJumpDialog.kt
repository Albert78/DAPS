package de.dh.daps.ui.common.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.dh.daps.ui.common.R
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Direct Jump Dialog for rapid time-series / list navigation by hour.
 *
 * @param entriesByHour Ordered Map of [LocalDateTime] hours to entry count ([Int]).
 * @param currentVisibleHour Currently visible hour in the list to highlight in the dialog.
 * @param onHourSelected Callback triggered when user selects a target hour.
 * @param onDismissRequest Callback triggered when user cancels or dismisses the dialog.
 * @param modifier Modifier for the dialog layout.
 * @param title Custom title text for the dialog.
 */
@Composable
fun HourJumpDialog(
    entriesByHour: Map<LocalDateTime, Int>,
    currentVisibleHour: LocalDateTime?,
    onHourSelected: (LocalDateTime) -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    title: String = stringResource(id = R.string.jump_to_hour_title)
) {
    val hoursList = remember(entriesByHour) {
        entriesByHour.keys.toList()
    }
    val daysList = remember(hoursList) {
        hoursList.map { it.toLocalDate() }.distinct()
    }
    var selectedDate by remember(daysList, currentVisibleHour) {
        mutableStateOf(currentVisibleHour?.toLocalDate() ?: daysList.firstOrNull() ?: LocalDate.now())
    }

    val hoursForSelectedDate = remember(hoursList, selectedDate) {
        hoursList.filter { it.toLocalDate() == selectedDate }
    }

    val locale = LocalLocale.current.platformLocale
    val dateFormatter = remember(locale) { DateTimeFormatter.ofPattern("EEE, dd. MMM", locale) }
    val hourFormatter = remember(locale) { DateTimeFormatter.ofPattern("HH:mm", locale) }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        icon = { Icon(Icons.Default.Schedule, contentDescription = null) },
        title = { Text(text = title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (daysList.size > 1) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(daysList, key = { it }) { date ->
                            FilterChip(
                                selected = date == selectedDate,
                                onClick = { selectedDate = date },
                                label = { Text(date.format(dateFormatter)) }
                            )
                        }
                    }
                    HorizontalDivider(thickness = 0.5.dp)
                }

                LazyColumn(
                    modifier = Modifier.heightIn(max = 320.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(hoursForSelectedDate, key = { it }) { hour ->
                        val count = entriesByHour[hour] ?: 0
                        val isCurrent = hour == currentVisibleHour

                        val startTimeStr = hour.format(hourFormatter)
                        val endTimeStr = hour.plusHours(1).format(hourFormatter)

                        Surface(
                            onClick = { onHourSelected(hour) },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isCurrent) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "$startTimeStr – $endTimeStr Uhr",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isCurrent) MaterialTheme.colorScheme.onPrimaryContainer
                                    else MaterialTheme.colorScheme.onSurface
                                )
                                Badge(
                                    containerColor = if (isCurrent) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = if (isCurrent) MaterialTheme.colorScheme.onPrimary
                                    else MaterialTheme.colorScheme.onSecondaryContainer
                                ) {
                                    Text(
                                        text = stringResource(id = R.string.jump_to_hour_entries_count, count),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(id = android.R.string.cancel))
            }
        }
    )
}