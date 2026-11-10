package de.dh.daps.common.domain.model.alarm

/**
 * Groups currently active unsnoozed alarms by severity level.
 *
 * @property heroAlarms Active alarms matching the highest active severity level.
 * @property otherAlarms Active alarms with lower severity levels.
 * @property highestSeverity The highest active [AlarmSeverity].
 * @property primaryAlarm The primary alarm used for signal configurations (audio/vibration/fullscreen).
 */
data class ActiveAlarmGroup(
    val heroAlarms: List<ActiveAlarm>,
    val otherAlarms: List<ActiveAlarm>,
    val highestSeverity: AlarmSeverity,
    val primaryAlarm: ActiveAlarm
) {
    val allAlarms: List<ActiveAlarm> get() = heroAlarms + otherAlarms
    val allAlarmTypes: List<AlarmType> get() = allAlarms.map { it.type }
    val totalCount: Int get() = heroAlarms.size + otherAlarms.size
}