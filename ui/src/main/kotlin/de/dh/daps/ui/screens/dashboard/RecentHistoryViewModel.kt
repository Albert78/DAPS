package de.dh.daps.ui.screens.dashboard

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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlin.time.Duration.Companion.milliseconds

data class RecentHistoryUiState(
    val isLoading: Boolean,
    val isError: Boolean,
    val readings: List<BgReading> = listOf(),
    val insulinApplications: List<InsulinApplication> = listOf(),
    val meals: List<MealEntry> = listOf(),
    val startTimestampMs: Long = 0L,
    val endTimestampMs: Long = 0L
)

private data class RawRecentHistoryData(
    val readings: List<BgReading>,
    val insulinApplications: List<InsulinApplication>,
    val meals: List<MealEntry>
)

/**
 * ViewModel for recent blood glucose history displayed on the dashboard.
 */
@OptIn(FlowPreview::class)
class RecentHistoryViewModel(
    systemRegistry: SystemRegistry
) : ViewModel() {
    private val glucoseRepository = systemRegistry.glucoseRepository
    private val treatmentRepository = systemRegistry.treatmentRepository

    private val rawDataFlow = combine(
        glucoseRepository.observeBgReadings().distinctUntilChanged(),
        treatmentRepository.observeInsulinApplications().distinctUntilChanged(),
        treatmentRepository.observeMeals().distinctUntilChanged()
    ) { readings, insulin, meals ->
        RawRecentHistoryData(readings, insulin, meals)
    }.debounce(300.milliseconds)

    val recentHistoryUiState: StateFlow<RecentHistoryUiState> = rawDataFlow
        .map { (readings, insulin, meals) ->
            val now = Timestamp.now()
            val endTimestamp = now
            val startTimestamp = now.minusHours(25.0)

            val filteredReadings = readings.filter { it.timestamp.ms in startTimestamp.ms..endTimestamp.ms }
            val filteredInsulin = insulin.filter { it.timestamp.ms in startTimestamp.ms..endTimestamp.ms }
            val filteredMeals = meals.filter { it.timestamp.ms in startTimestamp.ms..endTimestamp.ms }

            Log.d(TAG, "Updating recent history data ($startTimestamp to $endTimestamp) with ${filteredReadings.size} readings, ${filteredInsulin.size} insulin apps, ${filteredMeals.size} meals")

            RecentHistoryUiState(
                isLoading = false,
                isError = false,
                readings = filteredReadings,
                insulinApplications = filteredInsulin,
                meals = filteredMeals,
                startTimestampMs = startTimestamp.ms,
                endTimestampMs = endTimestamp.ms
            )
        }
        .flowOn(Dispatchers.Default)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = RecentHistoryUiState(isLoading = true, isError = false)
        )

    companion object {
        val TAG = RecentHistoryViewModel::class.simpleName

        class Factory(
            private val registry: SystemRegistry,
        ) : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                return RecentHistoryViewModel(registry) as T
            }
        }
    }
}