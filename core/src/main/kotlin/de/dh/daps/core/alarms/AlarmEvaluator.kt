package de.dh.daps.core.alarms

import android.util.Log
import de.dh.daps.common.model.ApsMode
import de.dh.daps.common.model.data.AlarmProfile
import de.dh.daps.common.model.data.AlarmSeverity
import de.dh.daps.common.model.data.AlarmSignalConfig
import de.dh.daps.common.model.data.AlarmType
import de.dh.daps.common.model.data.BgReading
import de.dh.daps.common.model.data.Timestamp
import de.dh.daps.core.aps.ApsIssue
import de.dh.daps.core.aps.SystemOrchestrator
import de.dh.daps.core.pump.PumpIssue
import de.dh.daps.core.repository.GlucoseRepository
import de.dh.daps.core.repository.TherapyRepository
import de.dh.daps.core.system.AndroidNotifications
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

/**
 * Continuously evaluates glucose values, pump/system issues, active alarm profile settings,
 * and snooze states to trigger audio/haptics, notification updates, and full-screen alarm screens.
 */
class AlarmEvaluator(
    private val glucoseRepository: GlucoseRepository,
    private val systemOrchestrator: SystemOrchestrator,
    private val therapyRepository: TherapyRepository,
    private val alarmSnoozeManager: AlarmSnoozeManager,
    private val androidNotifications: AndroidNotifications,
    private val scope: CoroutineScope
) {

    private val _activeAlarms = MutableStateFlow<Set<AlarmType>>(emptySet())
    val activeAlarms: StateFlow<Set<AlarmType>> = _activeAlarms.asStateFlow()

    private val _activeFiringAlarm = MutableStateFlow<AlarmType?>(null)
    val activeFiringAlarm: StateFlow<AlarmType?> = _activeFiringAlarm.asStateFlow()

    private val _activeFiringConfig = MutableStateFlow<AlarmSignalConfig?>(null)
    val activeFiringConfig: StateFlow<AlarmSignalConfig?> = _activeFiringConfig.asStateFlow()

    private var evaluationJob: Job? = null
    private var gracePeriodJob: Job? = null

    fun start() {
        stop()
        evaluationJob = scope.launch {
            combine(
                glucoseRepository.currentBg,
                systemOrchestrator.apsIssues,
                therapyRepository.observeCurrentTherapySettings().map { it.effectiveAlarmProfile },
                alarmSnoozeManager.snoozedAlarms,
                systemOrchestrator.apsMode,
                systemOrchestrator.resumeRequestedAt
            ) { flows ->
                @Suppress("UNCHECKED_CAST")
                EvaluationInput(
                    bgReading = flows[0] as BgReading?,
                    apsIssues = flows[1] as Set<ApsIssue>,
                    activeProfile = flows[2] as AlarmProfile?,
                    snoozedMap = flows[3] as Map<AlarmType, AlarmSnoozeState>,
                    apsMode = flows[4] as ApsMode,
                    resumeRequestedAt = flows[5] as Timestamp?
                )
            }.collect { input ->
                evaluate(input)
            }
        }
    }

    fun stop() {
        evaluationJob?.cancel()
        evaluationJob = null
        gracePeriodJob?.cancel()
        gracePeriodJob = null
        _activeAlarms.value = emptySet()
        _activeFiringAlarm.value = null
        _activeFiringConfig.value = null
        androidNotifications.cancelAlarmNotification()
    }

    private fun evaluate(input: EvaluationInput) {
        gracePeriodJob?.cancel()
        gracePeriodJob = null

        val activeSet = mutableSetOf<AlarmType>()

        // 1. Evaluate Glucose thresholds
        val bg = input.bgReading?.value
        if (bg != null && bg.isValid()) {
            val mgdl = bg.mgdl
            if (mgdl < LOW_THRESHOLD_MGDL) {
                activeSet.add(AlarmType.LOW_BG)
                if (mgdl < CRITICAL_LOW_THRESHOLD_MGDL) {
                    activeSet.add(AlarmType.CRITICAL_LOW_BG)
                }
            } else if (mgdl > HIGH_THRESHOLD_MGDL) {
                activeSet.add(AlarmType.HIGH_BG)
            }
        }

        // 2. Evaluate Issues
        // When switching out of ApsMode.Suspend to an active mode, the pump resume command is executed asynchronously
        // on the physical device. During this transient phase, the pump remains reported as suspended (`PumpIssue.Inoperative`)
        // until the status update arrives. To prevent false alarm flashing on the UI, we grant a grace period
        // (PUMP_RESUME_GRACE_PERIOD_MS) during which the PUMP_SUSPENDED alarm is suppressed.
        val now = Timestamp.now()
        val isWithinResumeGracePeriod = input.resumeRequestedAt?.let { ts ->
            now < ts.plusMs(PUMP_RESUME_GRACE_PERIOD_MS)
        } ?: false

        if (input.apsMode != ApsMode.Suspend && input.resumeRequestedAt != null) {
            val remainingMs = PUMP_RESUME_GRACE_PERIOD_MS - (now.ms - input.resumeRequestedAt.ms)
            if (remainingMs > 0 && input.apsIssues.any { it is ApsIssue.Pump && it.issue is PumpIssue.Inoperative }) {
                gracePeriodJob = scope.launch {
                    delay(remainingMs.milliseconds)
                    evaluate(input)
                }
            }
        }

        input.apsIssues.forEach { issue ->
            when (issue) {
                is ApsIssue.StaleBG -> activeSet.add(AlarmType.CGM_SIGNAL_LOSS)
                is ApsIssue.Pump -> {
                    when (issue.issue) {
                        is PumpIssue.Inoperative -> {
                            if (input.apsMode != ApsMode.Suspend && !isWithinResumeGracePeriod) {
                                activeSet.add(AlarmType.PUMP_SUSPENDED)
                            }
                        }
                        is PumpIssue.Occlusion -> activeSet.add(AlarmType.PUMP_OCCLUSION)
                        is PumpIssue.LowInsulin -> activeSet.add(AlarmType.PUMP_LOW_INSULIN)
                        is PumpIssue.LowBattery -> activeSet.add(AlarmType.PUMP_LOW_BATTERY)
                        else -> {}
                    }
                }
                else -> {}
            }
        }

        _activeAlarms.value = activeSet

        // Sync snooze states: remove snoozes for resolved or expired alarms
        alarmSnoozeManager.clearInactiveAndExpiredSnoozes(activeSet)

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
            val config = activeProfile?.getConfigFor(primaryAlarm) ?: AlarmSignalConfig()

            _activeFiringAlarm.value = primaryAlarm
            _activeFiringConfig.value = config

            Log.d(TAG, "Posting notification for active alarm $primaryAlarm (isFullScreen=${config.isFullScreen})")
            androidNotifications.showAlarmNotification(primaryAlarm, input.bgReading?.value, isFullScreen = config.isFullScreen)
        } else {
            // No unsnoozed alarms active
            if (_activeFiringAlarm.value != null) {
                Log.d(TAG, "Cancelling alarm notification: all alarms cleared or snoozed")
                androidNotifications.cancelAlarmNotification()
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
        val snoozedMap: Map<AlarmType, AlarmSnoozeState>,
        val apsMode: ApsMode = ApsMode.Suspend,
        val resumeRequestedAt: Timestamp? = null
    )

    companion object {
        private const val TAG = "AlarmEvaluator"
        const val CRITICAL_LOW_THRESHOLD_MGDL = 54.0
        const val LOW_THRESHOLD_MGDL = 70.0
        const val HIGH_THRESHOLD_MGDL = 250.0
        const val PUMP_RESUME_GRACE_PERIOD_MS = 10_000L
    }
}