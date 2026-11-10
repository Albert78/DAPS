package de.dh.daps.core.orchestration

import de.dh.daps.common.domain.model.data.Minutes
import de.dh.daps.common.domain.model.data.Timestamp
import de.dh.daps.core.aps.CoreIssue
import de.dh.daps.core.aps.CoreState
import de.dh.daps.core.aps.STALE_BG_THRESHOLD
import de.dh.daps.core.glucosesource.GlucoseSourceIssue
import de.dh.daps.core.glucosesource.GlucoseSourceManager
import de.dh.daps.core.pump.PumpIssue
import de.dh.daps.core.pump.PumpManager
import de.dh.daps.core.repository.BluetoothStatus
import de.dh.daps.core.repository.DeviceStatusRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * Aggregates system health metrics, device issues, Bluetooth status,
 * and stale BG readings into a single reactive issue stream.
 */
class ApsIssueAggregator {
    private val _apsIssues = MutableStateFlow<Set<ApsIssue>>(emptySet())
    val apsIssues: StateFlow<Set<ApsIssue>> = _apsIssues.asStateFlow()

    private val _staleSince = MutableStateFlow<Timestamp?>(null)

    fun staleCheck(glucoseSourceManager: GlucoseSourceManager) {
        val lastDataTime = glucoseSourceManager.lastInputTimestamp.value
        val isStale = lastDataTime.isInvalid() || lastDataTime + STALE_BG_THRESHOLD < Timestamp.now()
        _staleSince.value = if (isStale) lastDataTime else null
    }

    fun nextBgStaleCheckAt(glucoseSourceManager: GlucoseSourceManager): Timestamp {
        val lastDataTime = glucoseSourceManager.lastInputTimestamp.value
        return lastDataTime.ifValidOrNow() + STALE_BG_THRESHOLD
    }

    fun onNewBgReadingArrived() {
        _staleSince.value = null
    }

    fun startObserving(
        scope: CoroutineScope,
        coreState: StateFlow<CoreState>,
        pumpManager: PumpManager,
        glucoseSourceManager: GlucoseSourceManager,
        deviceStatusRepository: DeviceStatusRepository,
        consumableIssues: StateFlow<Set<ApsIssue>>
    ) {
        scope.launch {
            combine(
                _staleSince,
                coreState,
                pumpManager.pumpIssues,
                glucoseSourceManager.glucoseSourceIssues,
                deviceStatusRepository.observeBluetoothStatus(),
                consumableIssues
            ) { flows ->
                val staleSince = flows[0] as Timestamp?
                val state = flows[1] as CoreState
                @Suppress("UNCHECKED_CAST")
                val pIssues = flows[2] as Set<PumpIssue>
                @Suppress("UNCHECKED_CAST")
                val gIssues = flows[3] as Set<GlucoseSourceIssue>
                val btStatus = flows[4] as BluetoothStatus
                @Suppress("UNCHECKED_CAST")
                val cIssues = flows[5] as Set<ApsIssue>

                val issues = mutableSetOf<ApsIssue>()
                if (btStatus == BluetoothStatus.DISABLED || btStatus == BluetoothStatus.UNAVAILABLE) {
                    issues.add(ApsIssue.BluetoothDisabled)
                }

                val hasCoreNoRecentValues = state is CoreState.Active && state.issues.any { it is CoreIssue.NoRecentValues }

                if (staleSince != null && !hasCoreNoRecentValues) {
                    val staleMinutes = if (staleSince.isValid()) {
                        Minutes.timeDifference(staleSince, Timestamp.now()).value.toInt().coerceAtLeast(STALE_BG_THRESHOLD.value.toInt())
                    } else {
                        STALE_BG_THRESHOLD.value.toInt()
                    }
                    issues.add(ApsIssue.StaleBG(staleMinutes))
                }
                if (state is CoreState.Active) {
                    state.issues.forEach { issues.add(ApsIssue.Core(it)) }
                }
                pIssues.forEach { issues.add(ApsIssue.Pump(it)) }
                gIssues.forEach { issues.add(ApsIssue.GlucoseSource(it)) }
                cIssues.forEach { issues.add(it) }

                issues
            }.collect { combinedIssues ->
                _apsIssues.value = combinedIssues
            }
        }
    }

    fun reset() {
        _apsIssues.value = emptySet()
        _staleSince.value = null
    }
}