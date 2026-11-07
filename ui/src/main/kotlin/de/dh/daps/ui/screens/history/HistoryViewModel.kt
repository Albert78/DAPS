package de.dh.daps.ui.screens.history

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import de.dh.daps.common.domain.model.data.BgReading
import de.dh.daps.common.domain.model.data.Timestamp
import de.dh.daps.common.domain.model.therapy.InsulinApplication
import de.dh.daps.common.domain.model.therapy.MealEntry
import de.dh.daps.core.SystemRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlin.time.Duration.Companion.milliseconds

data class HistoryUiState(
    val isLoading: Boolean,
    val isError: Boolean,
    val readings: List<BgReading> = listOf(),
    val insulinApplications: List<InsulinApplication> = listOf(),
    val meals: List<MealEntry> = listOf(),
    val dayOffset: Int = 0,
    val startTimestampMs: Long = 0L,
    val endTimestampMs: Long = 0L,
    val canNavigatePrevious: Boolean = true,
    val canNavigateNext: Boolean = false,
    val isLatestDay: Boolean = true
)

private data class RawHistoryData(
    val readings: List<BgReading>,
    val insulinApplications: List<InsulinApplication>,
    val meals: List<MealEntry>
)

/**
 * ViewModel for blood glucose history.
 */
@OptIn(FlowPreview::class)
class HistoryViewModel(
    systemRegistry: SystemRegistry
) : ViewModel() {
    private val glucoseRepository = systemRegistry.glucoseRepository
    private val treatmentRepository = systemRegistry.treatmentRepository

    private val _dayOffset = MutableStateFlow(0)
    val dayOffset: StateFlow<Int> = _dayOffset.asStateFlow()

    fun navigatePreviousDay() {
        _dayOffset.update { offset -> offset + 1 }
    }

    fun navigateNextDay() {
        _dayOffset.update { offset -> (offset - 1).coerceAtLeast(0) }
    }

    private val rawDataFlow = combine(
        glucoseRepository.observeBgReadings().distinctUntilChanged(),
        treatmentRepository.observeInsulinApplications().distinctUntilChanged(),
        treatmentRepository.observeMeals().distinctUntilChanged()
    ) { readings, insulin, meals ->
        RawHistoryData(readings, insulin, meals)
    }.debounce(300.milliseconds)

    val historyUiState: StateFlow<HistoryUiState> = combine(
        rawDataFlow,
        _dayOffset
    ) { (readings, insulin, meals), dayOffset ->
        val now = Timestamp.now()
        val endTimestamp = now.minusHours(24.0 * dayOffset)
        val startTimestamp = endTimestamp.minusHours(25.0)

        val filteredReadings = readings.filter { it.timestamp.ms in startTimestamp.ms..endTimestamp.ms }
        val filteredInsulin = insulin.filter { it.timestamp.ms in startTimestamp.ms..endTimestamp.ms }
        val filteredMeals = meals.filter { it.timestamp.ms in startTimestamp.ms..endTimestamp.ms }

        val canNavigatePrevious = readings.any { it.timestamp < startTimestamp } ||
                insulin.any { it.timestamp < startTimestamp } ||
                meals.any { it.timestamp < startTimestamp }
        val canNavigateNext = dayOffset > 0
        val isLatestDay = dayOffset == 0

        Log.d(TAG, "Updating history data for dayOffset=$dayOffset ($startTimestamp to $endTimestamp) with ${filteredReadings.size} readings, ${filteredInsulin.size} insulin apps, ${filteredMeals.size} meals")

        HistoryUiState(
            isLoading = false,
            isError = false,
            readings = filteredReadings,
            insulinApplications = filteredInsulin,
            meals = filteredMeals,
            dayOffset = dayOffset,
            startTimestampMs = startTimestamp.ms,
            endTimestampMs = endTimestamp.ms,
            canNavigatePrevious = canNavigatePrevious,
            canNavigateNext = canNavigateNext,
            isLatestDay = isLatestDay
        )
    }
    .flowOn(Dispatchers.Default)
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HistoryUiState(isLoading = true, isError = false)
    )

    companion object {
        val TAG = HistoryViewModel::class.simpleName

        class Factory(
            private val registry: SystemRegistry,
        ) : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                return HistoryViewModel(registry) as T
            }
        }
    }
}