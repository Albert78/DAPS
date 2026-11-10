package de.dh.daps.ui.screens.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.dh.daps.ui.R
import de.dh.daps.ui.common.LocalCarbsUnit
import de.dh.daps.ui.common.LocalGlucoseUnit
import de.dh.daps.ui.common.composables.screenTitle
import de.dh.daps.ui.common.theme.AppPreview
import de.dh.daps.ui.controls.history.BgHistoryChartOrDefault
import de.dh.daps.ui.controls.history.BgOverviewChart
import de.dh.daps.ui.controls.history.HistoryDiagramData
import de.dh.daps.ui.controls.history.createSampleReadings
import de.dh.daps.ui.controls.history.rememberBgHistoryChartState
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun HistoryScreen(
    historyViewModel: HistoryViewModel
) {
    val historyUiState by historyViewModel.historyUiState.collectAsState()

    HistoryContent(
        historyUiState = historyUiState,
        onPreviousDayClick = { historyViewModel.navigatePreviousDay() },
        onNextDayClick = { historyViewModel.navigateNextDay() }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryContent(
    historyUiState: HistoryUiState,
    onPreviousDayClick: () -> Unit = {},
    onNextDayClick: () -> Unit = {}
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val chartState = key(historyUiState.dayOffset) {
        rememberBgHistoryChartState()
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = screenTitle(stringResource(id = R.string.history_screen_title)),
                scrollBehavior = scrollBehavior
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (historyUiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                val glucoseUnit = LocalGlucoseUnit.current
                val carbsUnit = LocalCarbsUnit.current
                val diagramData = HistoryDiagramData.create(
                    readings = historyUiState.readings,
                    glucoseUnit = glucoseUnit,
                    carbsUnit = carbsUnit,
                    insulinApplications = historyUiState.insulinApplications,
                    meals = historyUiState.meals,
                    isLatestDay = historyUiState.isLatestDay,
                    endTimestampMs = historyUiState.endTimestampMs
                )

                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    DateRangeHeader(
                        startMs = historyUiState.startTimestampMs,
                        endMs = historyUiState.endTimestampMs,
                        onPreviousDayClick = onPreviousDayClick,
                        onNextDayClick = onNextDayClick,
                        canNavigatePrevious = historyUiState.canNavigatePrevious,
                        canNavigateNext = historyUiState.canNavigateNext
                    )

                    BgHistoryChartOrDefault(
                        diagramData = diagramData,
                        showMarkers = true,
                        state = chartState,
                        modifier = Modifier.weight(1f)
                    )

                    if (diagramData != null) {
                        Spacer(modifier = Modifier.height(16.dp))
                        BgOverviewChart(
                            diagramData = diagramData,
                            state = chartState,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DateRangeHeader(
    startMs: Long?,
    endMs: Long?,
    onPreviousDayClick: () -> Unit,
    onNextDayClick: () -> Unit,
    canNavigatePrevious: Boolean,
    canNavigateNext: Boolean,
    modifier: Modifier = Modifier
) {
    val pattern = stringResource(id = R.string.history_date_range_format)
    val formatter = remember(pattern) { DateTimeFormatter.ofPattern(pattern, Locale.getDefault()) }
    val rangeText = remember(startMs, endMs, formatter) {
        if (startMs != null && endMs != null && startMs > 0 && endMs >= startMs) {
            val zoneId = ZoneId.systemDefault()
            val startDt = LocalDateTime.ofInstant(Instant.ofEpochMilli(startMs), zoneId)
            val endDt = LocalDateTime.ofInstant(Instant.ofEpochMilli(endMs), zoneId)
            "${startDt.format(formatter)} - ${endDt.format(formatter)}"
        } else {
            ""
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onPreviousDayClick,
            enabled = canNavigatePrevious
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = stringResource(id = R.string.cd_history_nav_previous_day)
            )
        }

        Text(
            text = rangeText,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )

        IconButton(
            onClick = onNextDayClick,
            enabled = canNavigateNext
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = stringResource(id = R.string.cd_history_nav_next_day)
            )
        }
    }
}

fun createSampleHistoryUiState(
    size: Int = 120,
    minsInterval: Short = 5,
    base: Double = 120.0,
    amplitude: Double = 20.0,
    noiseFactor: Double = 2.0,
    dayOffset: Int = 0
): HistoryUiState {
    val readings = createSampleReadings(
        size = size,
        minsInterval = minsInterval,
        base = base,
        amplitude = amplitude,
        noiseFactor = noiseFactor
    )
    val now = System.currentTimeMillis()
    val endMs = now - dayOffset * 24 * 3600 * 1000L
    val startMs = endMs - 25 * 3600 * 1000L
    return HistoryUiState(
        isLoading = false,
        isError = false,
        readings = readings,
        dayOffset = dayOffset,
        startTimestampMs = startMs,
        endTimestampMs = endMs,
        canNavigatePrevious = true,
        canNavigateNext = dayOffset > 0,
        isLatestDay = dayOffset == 0
    )
}

@Preview(showBackground = true)
@Composable
fun HistoryScreenPreview() {
    AppPreview {
        HistoryContent(
            historyUiState = createSampleHistoryUiState()
        )
    }
}