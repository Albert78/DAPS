package de.dh.daps.core.alarms

import android.content.Context
import android.util.Log
import de.dh.daps.common.model.data.AlarmProfile
import de.dh.daps.common.model.data.AlarmSeverity
import de.dh.daps.common.model.data.AlarmSoundConfig
import de.dh.daps.common.model.data.AlarmType
import de.dh.daps.common.model.data.BgReading
import de.dh.daps.core.aps.ApsIssue
import de.dh.daps.core.aps.SystemOrchestrator
import de.dh.daps.core.pump.PumpIssue
import de.dh.daps.core.repository.AlarmRepository
import de.dh.daps.core.repository.GlucoseRepository
import de.dh.daps.core.system.AndroidNotifications
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * Continuously evaluates glucose values, pump/system issues, active alarm profile settings,
 * and snooze states to trigger audio/haptics, notification updates, and full-screen alarm screens.
 */
class AlarmEvaluator(
    private val context: Context,
    private val glucoseRepository: GlucoseRepository,
    private val systemOrchestrator: SystemOrchestrator,
    private val alarmRepository: AlarmRepository,
    private val alarmSnoozeManager: AlarmSnoozeManager,
    private val alarmPlayerManager: AlarmPlayerManager,
    private val androidNotifications: AndroidNotifications,
    private val scope: CoroutineScope
) {

    private val _activeAlarms = MutableStateFlow<Set<AlarmType>>(emptySet())
    val activeAlarms: StateFlow<Set<AlarmType>> = _activeAlarms.asStateFlow()

    private val _activeFiringAlarm = MutableStateFlow<AlarmType?>(null)
    val activeFiringAlarm: StateFlow<AlarmType?> = _activeFiringAlarm.asStateFlow()

    private val _activeFiringConfig = MutableStateFlow<AlarmSoundConfig?>(null)
    val activeFiringConfig: StateFlow<AlarmSoundConfig?> = _activeFiringConfig.asStateFlow()

    fun start() {
        scope.launch {
            combine(
                glucoseRepository.currentBg,
                systemOrchestrator.apsIssues,
                alarmRepository.observeActiveAlarmProfile(),
                alarmSnoozeManager.snoozedAlarms
            ) { currentBgReading, apsIssues, activeProfile, snoozedMap ->
                EvaluationInput(
                    bgReading = currentBgReading,
                    apsIssues = apsIssues,
                    activeProfile = activeProfile,
                    snoozedMap = snoozedMap
                )
            }.collect { input ->
                evaluate(input)
            }
        }
    }

    private fun evaluate(input: EvaluationInput) {
        val activeSet = mutableSetOf<AlarmType>()

        // 1. Evaluate Glucose thresholds
        val bg = input.bgReading?.value
        if (bg != null && bg.isValid()) {
            val mgdl = bg.mgdl
            if (mgdl < CRITICAL_LOW_THRESHOLD_MGDL) {
                activeSet.add(AlarmType.CRITICAL_LOW_BG)
            } else if (mgdl < LOW_THRESHOLD_MGDL) {
                activeSet.add(AlarmType.LOW_BG)
            } else if (mgdl > HIGH_THRESHOLD_MGDL) {
                activeSet.add(AlarmType.HIGH_BG)
            }
        }

        // 2. Evaluate Issues
        input.apsIssues.forEach { issue ->
            when (issue) {
                is ApsIssue.StaleBG -> activeSet.add(AlarmType.CGM_SIGNAL_LOSS)
                is ApsIssue.Pump -> {
                    when (issue.issue) {
                        is PumpIssue.Inoperative -> activeSet.add(AlarmType.PUMP_OCCLUSION)
                        is PumpIssue.LowInsulin -> activeSet.add(AlarmType.PUMP_LOW_INSULIN)
                        is PumpIssue.LowBattery -> activeSet.add(AlarmType.PUMP_LOW_BATTERY)
                        else -> {}
                    }
                }
                else -> {}
            }
        }

        _activeAlarms.value = activeSet

        // 3. Filter unsnoozed active alarms
        val unsnoozedActiveAlarms = activeSet.filter { alarmType ->
            !alarmSnoozeManager.isSnoozed(alarmType)
        }

        if (unsnoozedActiveAlarms.isNotEmpty()) {
            // Priority sort: Severity CRITICAL > WARNING > INFO, then safety critical
            val primaryAlarm = unsnoozedActiveAlarms.maxWithOrNull(
                compareBy<AlarmType> { severityRank(it.defaultSeverity) }
                    .thenBy { if (it.isSafetyCritical) 1 else 0 }
            ) ?: unsnoozedActiveAlarms.first()

            val activeProfile = input.activeProfile
            val config = activeProfile?.getConfigFor(primaryAlarm) ?: AlarmSoundConfig()

            val previousFiring = _activeFiringAlarm.value
            _activeFiringAlarm.value = primaryAlarm
            _activeFiringConfig.value = config

            // Play audio/vibration if not already playing or if primary alarm changed
            if (previousFiring != primaryAlarm || !alarmPlayerManager.isPlaying()) {
                Log.d(TAG, "Triggering alarm playback for $primaryAlarm with config $config")
                alarmPlayerManager.playAlarm(config, isSafetyCritical = primaryAlarm.isSafetyCritical)
            }
        } else {
            // No unsnoozed alarms active
            if (_activeFiringAlarm.value != null) {
                Log.d(TAG, "Stopping active alarm sound/vibration: all alarms cleared or snoozed")
                alarmPlayerManager.stopAlarm()
            }
            _activeFiringAlarm.value = null
            _activeFiringConfig.value = null
        }
    }

    private fun severityRank(severity: AlarmSeverity): Int = when (severity) {
        AlarmSeverity.CRITICAL -> 3
        AlarmSeverity.WARNING -> 2
        AlarmSeverity.INFO -> 1
    }

    private data class EvaluationInput(
        val bgReading: BgReading?,
        val apsIssues: Set<ApsIssue>,
        val activeProfile: AlarmProfile?,
        val snoozedMap: Map<AlarmType, AlarmSnoozeState>
    )

    companion object {
        private const val TAG = "AlarmEvaluator"
        const val CRITICAL_LOW_THRESHOLD_MGDL = 54.0
        const val LOW_THRESHOLD_MGDL = 70.0
        const val HIGH_THRESHOLD_MGDL = 250.0
    }
}