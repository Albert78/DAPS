package de.dh.daps.core.alerts

import android.content.Intent
import android.util.Log
import de.dh.daps.common.domain.model.alarm.ActiveAlarmGroup
import de.dh.daps.common.domain.model.alarm.AlarmCategory
import de.dh.daps.common.domain.model.alarm.AlarmProfile
import de.dh.daps.common.domain.model.alarm.AlarmSeverity
import de.dh.daps.common.domain.model.alarm.AlarmSignalConfig
import de.dh.daps.common.domain.model.alarm.AlarmType
import de.dh.daps.common.domain.model.data.BgReading
import de.dh.daps.common.domain.model.data.BgValue
import de.dh.daps.common.domain.model.data.Timestamp
import de.dh.daps.common.domain.model.engine.ApsMode
import de.dh.daps.common.domain.model.therapy.CurrentTherapySettings
import de.dh.daps.common.domain.pump.InsulinPumpStatus
import de.dh.daps.common.domain.pump.PumpLifecycleState
import de.dh.daps.common.infrastructure.WakeupHandler
import de.dh.daps.core.glucosesource.GlucoseSourceIssue
import de.dh.daps.core.infrastructure.AndroidNotifications
import de.dh.daps.core.orchestration.ApsIssue
import de.dh.daps.core.orchestration.SystemOrchestrator
import de.dh.daps.core.pump.PumpIssue
import de.dh.daps.core.pump.PumpManager
import de.dh.daps.core.repository.GlucoseRepository
import de.dh.daps.core.repository.TherapyRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

/**
 * Continuously evaluates glucose values, pump/system issues, active alarm profile settings,
 * consumable lifecycle state (catheter/patch changes), and snooze states to trigger audio/haptics,
 * notification updates, and full-screen alarm screens.
 */
class AlarmEvaluator(
    private val glucoseRepository: GlucoseRepository,
    private val systemOrchestrator: SystemOrchestrator,
    private val therapyRepository: TherapyRepository,
    private val alarmSnoozeManager: AlarmSnoozeManager,
    private val androidNotifications: AndroidNotifications,
    private val pumpManager: PumpManager,
    private val scope: CoroutineScope
) : WakeupHandler {
    private val _activeAlarms = MutableStateFlow<Set<AlarmType>>(emptySet())
    val activeAlarms: StateFlow<Set<AlarmType>> = _activeAlarms.asStateFlow()

    private val _activeAlarmGroup = MutableStateFlow<ActiveAlarmGroup?>(null)
    val activeAlarmGroup: StateFlow<ActiveAlarmGroup?> = _activeAlarmGroup.asStateFlow()

    private val _activeFiringConfig = MutableStateFlow<AlarmSignalConfig?>(null)
    val activeFiringConfig: StateFlow<AlarmSignalConfig?> = _activeFiringConfig.asStateFlow()

    private var lastEvaluatedBgValue: BgValue? = null
    private var lastInput: EvaluationInput? = null

    private var evaluationJob: Job? = null
    private var gracePeriodJob: Job? = null

    @OptIn(ExperimentalCoroutinesApi::class)
    fun start() {
        stop()

        val pumpStatusFlow = pumpManager.activeInsulinPump.flatMapLatest { pump ->
            pump?.pumpStatus ?: flowOf(null)
        }

        evaluationJob = scope.launch {
            combine(
                glucoseRepository.currentBg,
                systemOrchestrator.apsIssues,
                therapyRepository.observeCurrentTherapySettingsOrNull(),
                alarmSnoozeManager.snoozedAlarms,
                systemOrchestrator.apsMode,
                systemOrchestrator.resumeRequestedAt,
                pumpStatusFlow
            ) { flows ->
                val therapySettings = flows[2] as CurrentTherapySettings?
                @Suppress("UNCHECKED_CAST")
                EvaluationInput(
                    bgReading = flows[0] as BgReading?,
                    apsIssues = flows[1] as Set<ApsIssue>,
                    currentTherapySettings = therapySettings,
                    activeProfile = therapySettings?.effectiveAlarmProfile,
                    snoozedMap = flows[3] as Map<AlarmType, AlarmSnoozeState>,
                    apsMode = flows[4] as ApsMode,
                    resumeRequestedAt = flows[5] as Timestamp?,
                    pumpStatus = flows[6] as InsulinPumpStatus?
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
        _activeAlarmGroup.value = null
        _activeFiringConfig.value = null
        lastEvaluatedBgValue = null
        lastInput = null
        androidNotifications.cancelAlarmNotification()
    }

    override fun onWakeup(wakeupId: UInt?, intent: Intent?) {
        val input = lastInput ?: return
        scope.launch {
            evaluate(input)
        }
    }

    private fun evaluate(input: EvaluationInput) {
        lastInput = input
        gracePeriodJob?.cancel()
        gracePeriodJob = null

        val activeSet = mutableSetOf<AlarmType>()

        // Evaluate Therapy Settings
        if (input.currentTherapySettings == null) {
            activeSet.add(AlarmType.SYSTEM_THERAPY_SETTINGS_MISSING)
        }

        // Evaluate Glucose thresholds
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

        // Evaluate Issues
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
                is ApsIssue.BluetoothDisabled -> activeSet.add(AlarmType.SYSTEM_BLUETOOTH_OFF)
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
                        is PumpIssue.StaleBluetoothBond -> activeSet.add(AlarmType.PUMP_STALE_BOND)
                        is PumpIssue.CatheterChangeWarn -> activeSet.add(AlarmType.PUMP_CATHETER_CHANGE_WARN)
                        is PumpIssue.PatchChangeWarn -> activeSet.add(AlarmType.PUMP_PATCH_CHANGE_WARN)
                        is PumpIssue.Expired -> {
                            if (input.pumpStatus?.lifecycleState is PumpLifecycleState.Durable) {
                                activeSet.add(AlarmType.PUMP_CATHETER_CHANGE_WARN)
                            } else {
                                activeSet.add(AlarmType.PUMP_PATCH_CHANGE_WARN)
                            }
                        }
                        else -> {}
                    }
                }
                is ApsIssue.GlucoseSource -> {
                    when (issue.issue) {
                        is GlucoseSourceIssue.Expired -> activeSet.add(AlarmType.CGM_EXPIRED)
                        is GlucoseSourceIssue.SensorError -> activeSet.add(AlarmType.CGM_SENSOR_ERROR)
                        is GlucoseSourceIssue.StaleBluetoothBond -> activeSet.add(AlarmType.CGM_STALE_BOND)
                        else -> {}
                    }
                }
                else -> {}
            }
        }

        _activeAlarms.value = activeSet

        // Sync snooze states: remove snoozes for resolved or expired alarms
        alarmSnoozeManager.clearInactiveAndExpiredSnoozes(activeSet)

        // Filter unsnoozed active alarms
        val unsnoozedActiveAlarms = activeSet.filter { alarmType ->
            !alarmSnoozeManager.isSnoozed(alarmType)
        }

        if (unsnoozedActiveAlarms.isNotEmpty()) {
            val highestSeverity = unsnoozedActiveAlarms.maxByOrNull { severityRank(it.defaultSeverity) }?.defaultSeverity
                ?: AlarmSeverity.INFO

            val heroAlarms = unsnoozedActiveAlarms
                .filter { it.defaultSeverity == highestSeverity }
                .sortedWith(
                    compareByDescending<AlarmType> { if (it.isSafetyCritical) 1 else 0 }
                        .thenBy { it.name }
                )

            val otherAlarms = unsnoozedActiveAlarms
                .filter { it.defaultSeverity != highestSeverity }
                .sortedWith(
                    compareByDescending<AlarmType> { severityRank(it.defaultSeverity) }
                        .thenByDescending { if (it.isSafetyCritical) 1 else 0 }
                        .thenBy { it.name }
                )

            val primaryAlarm = heroAlarms.first()
            val alarmGroup = ActiveAlarmGroup(
                heroAlarms = heroAlarms,
                otherAlarms = otherAlarms,
                highestSeverity = highestSeverity,
                primaryAlarm = primaryAlarm
            )

            val activeProfile = input.activeProfile
            val config = activeProfile?.getConfigFor(primaryAlarm) ?: AlarmSignalConfig()

            val bgValue = input.bgReading?.value
            val isGlucoseAlarmActive = alarmGroup.allAlarms.any { it.category == AlarmCategory.GLUCOSE }
            val effectiveBgValue = if (isGlucoseAlarmActive) bgValue else null

            val previousGroup = _activeAlarmGroup.value
            val previousConfig = _activeFiringConfig.value
            val previousBgValue = lastEvaluatedBgValue

            _activeAlarmGroup.value = alarmGroup
            _activeFiringConfig.value = config
            lastEvaluatedBgValue = if (isGlucoseAlarmActive) bgValue else null

            // Only re-post notification if active alarm group, signal config, or BG reading (if glucose alarm) actually changed
            val bgChanged = isGlucoseAlarmActive && (bgValue != previousBgValue)
            if (alarmGroup != previousGroup || config != previousConfig || bgChanged) {
                Log.d(TAG, "Posting notification for active alarm group $alarmGroup (isFullScreen=${config.isFullScreen})")
                androidNotifications.showAlarmNotification(alarmGroup, effectiveBgValue, isFullScreen = config.isFullScreen)
            }
        } else {
            // No unsnoozed alarms active
            if (_activeAlarmGroup.value != null) {
                Log.d(TAG, "Cancelling alarm notification: all alarms cleared or snoozed")
                androidNotifications.cancelAlarmNotification()
            }
            _activeAlarmGroup.value = null
            _activeFiringConfig.value = null
            lastEvaluatedBgValue = null
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
        val currentTherapySettings: CurrentTherapySettings?,
        val activeProfile: AlarmProfile?,
        val snoozedMap: Map<AlarmType, AlarmSnoozeState>,
        val apsMode: ApsMode = ApsMode.Suspend,
        val resumeRequestedAt: Timestamp? = null,
        val pumpStatus: InsulinPumpStatus? = null
    )

    companion object {
        private const val TAG = "AlarmEvaluator"
        const val CRITICAL_LOW_THRESHOLD_MGDL = 54.0
        const val LOW_THRESHOLD_MGDL = 70.0
        const val HIGH_THRESHOLD_MGDL = 250.0
        const val PUMP_RESUME_GRACE_PERIOD_MS = 10_000L

        const val WAKE_TAG_ALARM_EVALUATOR = "AlarmEvaluator"
        val WAKEUP_ID_ALARM_EVALUATOR: UInt = 600u
    }
}