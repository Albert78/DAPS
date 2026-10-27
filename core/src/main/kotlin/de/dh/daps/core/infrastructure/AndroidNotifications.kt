package de.dh.daps.core.infrastructure

import android.app.Notification
import de.dh.daps.common.domain.model.alarm.ActiveAlarmGroup
import de.dh.daps.common.domain.model.alarm.AlarmType
import de.dh.daps.common.domain.model.data.BgValue
import de.dh.daps.common.domain.model.reminder.Reminder
import de.dh.daps.core.orchestration.ActiveRecommendations
import de.dh.daps.core.repository.GlucoseRepository

interface AndroidNotifications {
    fun createNotificationChannels()
    fun updateRecommendationNotifications(recommendations: ActiveRecommendations)

    fun showReminderNotification(reminder: Reminder)

    fun showAlarmNotification(alarmGroup: ActiveAlarmGroup, bgValue: BgValue? = null, isFullScreen: Boolean = true)
    fun showAlarmNotification(alarmType: AlarmType, bgValue: BgValue? = null, isFullScreen: Boolean = true)
    fun cancelAlarmNotification()

    fun createMainAppNotification(glucoseRepository: GlucoseRepository): Notification
    fun updateMainAppNotification(glucoseRepository: GlucoseRepository)

    companion object {
        const val FOREGROUND_NOTIFICATION_ID = 1
    }
}