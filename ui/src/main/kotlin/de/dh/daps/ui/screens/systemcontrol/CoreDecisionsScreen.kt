package de.dh.daps.ui.screens.systemcontrol

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.dh.daps.common.domain.model.data.GlucoseUnit
import de.dh.daps.common.domain.model.therapy.InsulinAmount
import de.dh.daps.core.aps.CoreInsight
import de.dh.daps.core.aps.CoreReasoning
import de.dh.daps.ui.R
import de.dh.daps.ui.common.LocalGlucoseUnit
import de.dh.daps.ui.common.carbsValue
import de.dh.daps.ui.common.composables.FastScrollbar
import de.dh.daps.ui.common.composables.animateScrollToItemFast
import de.dh.daps.ui.common.composables.contentScrollIndicator
import de.dh.daps.ui.common.composables.screenTitle
import de.dh.daps.ui.common.crValue
import de.dh.daps.ui.common.glucoseValue
import de.dh.daps.ui.common.insulinValue
import de.dh.daps.ui.common.isfValue
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Date
import de.dh.daps.common.R as CommonR

@Composable
fun CoreDecisionsScreen(
    viewModel: CoreDecisionsViewModel,
    onNavigateUp: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    CoreDecisionsContent(
        uiState = uiState,
        onNavigateUp = onNavigateUp
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoreDecisionsContent(
    uiState: CoreDecisionsUiState,
    onNavigateUp: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val locale = LocalLocale.current.platformLocale
    val badgeFormatter = remember(locale) {
        DateTimeFormatter.ofPattern("dd.MM. HH:00", locale)
    }

    val groupedEntries = remember(uiState.coreInsights) {
        uiState.coreInsights.groupBy { insight ->
            Instant.ofEpochMilli(insight.timestamp.ms)
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime()
                .truncatedTo(ChronoUnit.HOURS)
        }
    }
    val hoursList = remember(groupedEntries) {
        groupedEntries.keys.toList()
    }
    val hourHeaderIndices = remember(groupedEntries) {
        val map = mutableMapOf<LocalDateTime, Int>()
        var currentIndex = 0
        groupedEntries.forEach { (hour, entries) ->
            map[hour] = currentIndex
            currentIndex += 1 + entries.size
        }
        map
    }
    val itemIndexToHour = remember(groupedEntries) {
        val list = mutableListOf<LocalDateTime>()
        groupedEntries.forEach { (hour, entries) ->
            list.add(hour)
            entries.forEach { _ ->
                list.add(hour)
            }
        }
        list
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = screenTitle(stringResource(id = R.string.core_decisions_screen_title)),
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(id = CommonR.string.cd_navigate_up)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        val listState = rememberLazyListState()
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .contentScrollIndicator(
                        scrollableState = listState,
                        showTopGradient = false,
                        topIconPadding = 48.dp
                    )
            ) {
                if (uiState.coreInsights.isEmpty()) {
                    item {
                        Text(
                            text = stringResource(id = R.string.core_decisions_no_insights),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                } else {
                    groupedEntries.entries.forEachIndexed { index, (hour, entries) ->
                        val previousHour = if (index > 0) hoursList[index - 1] else null
                        val nextHour = if (index < hoursList.size - 1) hoursList[index + 1] else null

                        val onPreviousClick: (() -> Unit)? = previousHour?.let { targetHour ->
                            {
                                hourHeaderIndices[targetHour]?.let { targetIndex ->
                                    coroutineScope.launch {
                                        listState.animateScrollToItemFast(targetIndex)
                                    }
                                }
                            }
                        }

                        val onNextClick: (() -> Unit)? = nextHour?.let { targetHour ->
                            {
                                hourHeaderIndices[targetHour]?.let { targetIndex ->
                                    coroutineScope.launch {
                                        listState.animateScrollToItemFast(targetIndex)
                                    }
                                }
                            }
                        }

                        stickyHeader {
                            HourHeader(
                                hour = hour,
                                count = entries.size,
                                onPreviousHourClick = onPreviousClick,
                                onNextHourClick = onNextClick
                            )
                        }
                        items(entries, key = { it.timestamp.ms }) { insight ->
                            Spacer(modifier = Modifier.height(8.dp))
                            InsightCard(
                                insight = insight,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }

            if (uiState.coreInsights.isNotEmpty()) {
                FastScrollbar(
                    listState = listState,
                    getBadgeText = { index ->
                        itemIndexToHour.getOrNull(index)?.format(badgeFormatter)
                    },
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxHeight()
                        .padding(vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
fun HourHeader(
    hour: LocalDateTime,
    count: Int,
    onPreviousHourClick: (() -> Unit)? = null,
    onNextHourClick: (() -> Unit)? = null
) {
    val locale = LocalLocale.current.platformLocale
    val dateFormatter = remember(locale) { DateTimeFormatter.ofPattern("EEEE, dd. MMMM yyyy", locale) }
    val timeFormatter = remember(locale) { DateTimeFormatter.ofPattern("HH:mm", locale) }

    val dateStr = remember(hour, dateFormatter) { hour.format(dateFormatter) }
    val startTimeStr = remember(hour, timeFormatter) { hour.format(timeFormatter) }
    val endTimeStr = remember(hour, timeFormatter) { hour.plusHours(1).format(timeFormatter) }

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = dateStr,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "$startTimeStr – $endTimeStr Uhr ($count)",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { onPreviousHourClick?.invoke() },
                    enabled = onPreviousHourClick != null
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowUp,
                        contentDescription = stringResource(id = R.string.cd_history_nav_previous_hour)
                    )
                }
                IconButton(
                    onClick = { onNextHourClick?.invoke() },
                    enabled = onNextHourClick != null
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = stringResource(id = R.string.cd_history_nav_next_hour)
                    )
                }
            }
        }
    }
}

@Composable
fun InsightCard(
    insight: CoreInsight,
    modifier: Modifier = Modifier
) {
    val locale = LocalLocale.current.platformLocale
    val dateTimeFormat = remember(locale) { SimpleDateFormat("HH:mm:ss", locale) }
    val glucoseUnit = LocalGlucoseUnit.current
    val unitStr = when (glucoseUnit) {
        GlucoseUnit.MG_DL -> stringResource(CommonR.string.glucose_unit_mgdl)
        GlucoseUnit.MMOL -> stringResource(CommonR.string.glucose_unit_mmol)
    }
    val devStr = when (glucoseUnit) {
        GlucoseUnit.MG_DL -> String.format(locale, "%+.1f", insight.deviationPerTick.mgdl)
        GlucoseUnit.MMOL -> String.format(locale, "%+.2f", insight.deviationPerTick.mmol)
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = dateTimeFormat.format(Date(insight.timestamp.ms)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = insight.reasoning.name,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (insight.reasoning == CoreReasoning.INTERNAL_ERROR) Color.Red else MaterialTheme.colorScheme.secondary
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                MetricItem(stringResource(id = R.string.core_insight_label_bg), "${glucoseValue(insight.bgFiltered, glucoseUnit)} ${stringResource(
                    CommonR.string.bg_raw_format, glucoseValue(insight.bgOriginal, glucoseUnit))}")
                Spacer(modifier = Modifier.width(16.dp))
                MetricItem(stringResource(id = R.string.core_insight_label_iob), insulinValue(insight.futureActiveInsulin))
                Spacer(modifier = Modifier.width(16.dp))
                MetricItem(stringResource(id = R.string.core_insight_label_cob), carbsValue(insight.futureActiveCarbs))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                MetricItem(stringResource(id = R.string.core_insight_label_pred_peak), "${glucoseValue(insight.predictedBgAtCorrection, glucoseUnit)} $unitStr")
                Spacer(modifier = Modifier.width(16.dp))
                MetricItem(stringResource(id = R.string.core_insight_label_dev), devStr)
                Spacer(modifier = Modifier.width(16.dp))
                MetricItem(stringResource(id = R.string.core_insight_label_target), "${glucoseValue(insight.targetBg, glucoseUnit)} $unitStr")
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                MetricItem(stringResource(id = R.string.core_insight_label_isf), isfValue(insight.isf, glucoseUnit))
                Spacer(modifier = Modifier.width(16.dp))
                MetricItem(stringResource(id = R.string.core_insight_label_cr), crValue(insight.cr))
            }

            val actionText = when {
                insight.actionBolus != null && insight.actionBolus!! > InsulinAmount.ZERO -> {
                    stringResource(id = R.string.core_insight_action_bolus, insulinValue(insight.actionBolus))
                }
                insight.actionTempBasalPercent != null -> {
                    stringResource(
                        id = R.string.core_insight_action_temp_basal,
                        insight.actionTempBasalPercent!!,
                        insight.actionTempBasalDurationInHours ?: 0
                    )
                }
                else -> null
            }

            actionText?.let {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), thickness = 0.5.dp)
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
        }
    }
}

@Composable
fun MetricItem(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = MaterialTheme.typography.labelSmall.fontSize * 0.8f)
        )
        Text(text = value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
    }
}