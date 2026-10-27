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
    val heroAlarms: List<AlarmType>,
    val otherAlarms: List<AlarmType>,
    val highestSeverity: AlarmSeverity,
    val primaryAlarm: AlarmType
) {
    val allAlarms: List<AlarmType> get() = heroAlarms + otherAlarms
    val totalCount: Int get() = heroAlarms.size + otherAlarms.size
}