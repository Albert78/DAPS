package de.dh.daps.core.alarms

import de.dh.daps.common.model.data.AlarmType
import de.dh.daps.common.model.data.Minutes
import de.dh.daps.common.model.data.Timestamp
import de.dh.daps.core.alarms.AlarmSnoozeManager.Companion.MAX_SAFETY_CRITICAL_SNOOZE_MINUTES
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Manages active snooze states and temporary silences for alarms.
 * Enforces safety boundaries for safety-critical alarms.
 */
class AlarmSnoozeManager {
    private val _snoozedAlarms = MutableStateFlow<Map<AlarmType, AlarmSnoozeState>>(emptyMap())
    val snoozedAlarms: StateFlow<Map<AlarmType, AlarmSnoozeState>> = _snoozedAlarms.asStateFlow()

    /**
     * Snoozes the specified [alarmType] for [minutes].
     * For safety-critical alarms, the duration is capped to [MAX_SAFETY_CRITICAL_SNOOZE_MINUTES].
     */
    fun snoozeAlarm(alarmType: AlarmType, minutes: Int) {
        val actualMinutes = if (alarmType.isSafetyCritical) {
            minutes.coerceAtMost(MAX_SAFETY_CRITICAL_SNOOZE_MINUTES)
        } else {
            minutes
        }
        val snoozedUntil = Timestamp.now() + Minutes(actualMinutes.toShort())
        _snoozedAlarms.update { current ->
            var updatedMap = current + (alarmType to AlarmSnoozeState(alarmType, snoozedUntil, actualMinutes))
            for (subsumed in alarmType.subsumedAlarms) {
                updatedMap += (subsumed to AlarmSnoozeState(subsumed, snoozedUntil, actualMinutes))
            }
            updatedMap
        }
    }

    /**
     * Returns true if the [alarmType] is currently actively snoozed.
     */
    fun isSnoozed(alarmType: AlarmType): Boolean {
        val state = _snoozedAlarms.value[alarmType] ?: return false
        return Timestamp.now() < state.snoozedUntil
    }

    /**
     * Gets the active [AlarmSnoozeState] if present and not expired.
     */
    fun getSnoozedState(alarmType: AlarmType): AlarmSnoozeState? {
        val state = _snoozedAlarms.value[alarmType] ?: return null
        return if (Timestamp.now() < state.snoozedUntil) {
            state
        } else {
            null
        }
    }

    /**
     * Clears snooze states for alarms that are no longer active in [activeAlarms]
     * or whose snooze duration has expired.
     */
    fun clearInactiveAndExpiredSnoozes(activeAlarms: Set<AlarmType>) {
        val now = Timestamp.now()
        _snoozedAlarms.update { current ->
            current.filter { (type, state) ->
                type in activeAlarms && now < state.snoozedUntil
            }
        }
    }

    /**
     * Removes the snooze state for [alarmType] and any subsumed alarms.
     */
    fun clearSnooze(alarmType: AlarmType) {
        _snoozedAlarms.update { current ->
            current - alarmType - alarmType.subsumedAlarms
        }
    }

    /**
     * Clears all active snoozes.
     */
    fun clearAllSnoozes() {
        _snoozedAlarms.value = emptyMap()
    }

    companion object {
        const val MAX_SAFETY_CRITICAL_SNOOZE_MINUTES = 30
    }
}