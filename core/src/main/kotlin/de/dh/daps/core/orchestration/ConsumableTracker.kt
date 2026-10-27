package de.dh.daps.core.orchestration

import de.dh.daps.common.domain.model.data.Timestamp
import de.dh.daps.common.domain.pump.InsulinPumpStatus
import de.dh.daps.common.domain.pump.PatchPhase
import de.dh.daps.common.domain.pump.PumpLifecycleState
import de.dh.daps.common.infrastructure.REMINDER_SOURCE_CATHETER_CHANGE
import de.dh.daps.common.infrastructure.REMINDER_SOURCE_PATCH_CHANGE
import de.dh.daps.common.infrastructure.ReminderManager
import de.dh.daps.common.infrastructure.SystemWakeService
import de.dh.daps.common.infrastructure.clearConsumableReminder
import de.dh.daps.core.pump.PumpIssue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/**
 * Tracks consumable lifecycles (catheters, patch pumps), evaluates warnings,
 * syncs advance reminders, and manages wakeups.
 */
class ConsumableTracker {
    private val _consumableIssues = MutableStateFlow<Set<ApsIssue>>(emptySet())
    val consumableIssues: StateFlow<Set<ApsIssue>> = _consumableIssues.asStateFlow()

    var lastPumpStatus: InsulinPumpStatus? = null
        private set
    var lastWarningTime: LocalTime? = null
        private set

    fun updatePumpStatusAndWarningTime(status: InsulinPumpStatus?, warningTime: LocalTime) {
        lastPumpStatus = status
        lastWarningTime = warningTime
    }

    suspend fun evaluateConsumableLifecycle(
        lifecycleState: PumpLifecycleState?,
        warningTime: LocalTime,
        reminderManager: ReminderManager?,
        wakeService: SystemWakeService,
        wakeTag: String,
        wakeupConsumableCheckId: UInt
    ) {
        if (lifecycleState == null) {
            _consumableIssues.value = emptySet()
            reminderManager?.clearConsumableReminder(REMINDER_SOURCE_CATHETER_CHANGE)
            reminderManager?.clearConsumableReminder(REMINDER_SOURCE_PATCH_CHANGE)
            wakeService.cancelWakeup(wakeTag, wakeupConsumableCheckId)
            return
        }

        // 1. Sync advance reminder (1 day before)
        syncConsumableReminder(lifecycleState, warningTime, reminderManager)

        // 2. Evaluate active warning issue & next wakeup timestamp
        val now = Timestamp.now()
        val zoneId = ZoneId.systemDefault()
        val effectiveNow = now.plusMs(CONSUMABLE_REMINDER_TOLERANCE_MS)
        val currentZoneDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(effectiveNow.ms), zoneId)

        val newIssues = mutableSetOf<ApsIssue>()
        var nextWakeupMs: Long? = null

        when (lifecycleState) {
            is PumpLifecycleState.Durable -> {
                val expirationDate = lifecycleState.approximateCatheterExpiration
                if (expirationDate != null) {
                    val warnTriggerDateTime = expirationDate.atTime(warningTime)
                    if (currentZoneDateTime >= warnTriggerDateTime) {
                        newIssues.add(ApsIssue.Pump(PumpIssue.CatheterChangeWarn))
                    } else {
                        val warnMs = warnTriggerDateTime.atZone(zoneId).toInstant().toEpochMilli()
                        if (warnMs > effectiveNow.ms) {
                            nextWakeupMs = warnMs
                        }
                    }
                }
            }
            is PumpLifecycleState.Patch -> {
                if (lifecycleState.phase == PatchPhase.IN_GRACE_PERIOD || lifecycleState.phase == PatchPhase.EXPIRED) {
                    newIssues.add(ApsIssue.Pump(PumpIssue.PatchChangeWarn))
                } else if (lifecycleState.phase == PatchPhase.ACTIVE) {
                    val activeUntil = lifecycleState.activeUntil
                    if (activeUntil.isValid() && activeUntil.ms > 0) {
                        if (effectiveNow >= activeUntil) {
                            newIssues.add(ApsIssue.Pump(PumpIssue.PatchChangeWarn))
                        } else if (activeUntil.ms > effectiveNow.ms) {
                            nextWakeupMs = activeUntil.ms
                        }
                    }
                }
            }
        }

        _consumableIssues.value = newIssues

        if (nextWakeupMs != null && nextWakeupMs > now.ms) {
            wakeService.scheduleWakeup(wakeTag, wakeupConsumableCheckId, Timestamp(nextWakeupMs))
        } else {
            wakeService.cancelWakeup(wakeTag, wakeupConsumableCheckId)
        }
    }

    private suspend fun syncConsumableReminder(
        lifecycleState: PumpLifecycleState,
        warningTime: LocalTime,
        reminderManager: ReminderManager?
    ) {
        val manager = reminderManager ?: return
        val zoneId = ZoneId.systemDefault()
        when (lifecycleState) {
            is PumpLifecycleState.Durable -> {
                val expirationDate = lifecycleState.approximateCatheterExpiration
                if (expirationDate != null) {
                    val triggerDateTime = expirationDate.minusDays(1).atTime(warningTime)
                    val triggerMs = triggerDateTime.atZone(zoneId).toInstant().toEpochMilli()

                    manager.syncConsumableReminder(
                        sourceId = REMINDER_SOURCE_CATHETER_CHANGE,
                        reminderTimestamp = Timestamp(triggerMs)
                    )
                } else {
                    manager.clearConsumableReminder(REMINDER_SOURCE_CATHETER_CHANGE)
                }
                manager.clearConsumableReminder(REMINDER_SOURCE_PATCH_CHANGE)
            }
            is PumpLifecycleState.Patch -> {
                val activeUntil = lifecycleState.activeUntil
                if (lifecycleState.phase == PatchPhase.ACTIVE && activeUntil.isValid() && activeUntil.ms > 0) {
                    val activeUntilZoneDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(activeUntil.ms), zoneId)
                    val activeUntilDate = activeUntilZoneDateTime.toLocalDate()
                    val triggerDateTime = activeUntilDate.minusDays(1).atTime(warningTime)
                    val triggerMs = triggerDateTime.atZone(zoneId).toInstant().toEpochMilli()

                    manager.syncConsumableReminder(
                        sourceId = REMINDER_SOURCE_PATCH_CHANGE,
                        reminderTimestamp = Timestamp(triggerMs)
                    )
                } else {
                    manager.clearConsumableReminder(REMINDER_SOURCE_PATCH_CHANGE)
                }
                manager.clearConsumableReminder(REMINDER_SOURCE_CATHETER_CHANGE)
            }
        }
    }

    fun reset() {
        _consumableIssues.value = emptySet()
        lastPumpStatus = null
        lastWarningTime = null
    }

    companion object {
        const val CONSUMABLE_REMINDER_TOLERANCE_MS = 1_000L
    }
}