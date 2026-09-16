package de.dh.daps.core.system

import android.app.Notification
import de.dh.daps.common.model.data.AlarmType
import de.dh.daps.common.model.data.BgValue
import de.dh.daps.core.aps.ApsRecommendation
import de.dh.daps.core.repository.GlucoseRepository

interface AndroidNotifications {
    fun createNotificationChannels()
    fun showRecommendationNotification(recommendation: ApsRecommendation)
    fun cancelRecommendationNotification()

    fun showAlarmNotification(alarmType: AlarmType, bgValue: BgValue? = null)
    fun cancelAlarmNotification()

    fun createMainAppNotification(glucoseRepository: GlucoseRepository): Notification
    fun updateMainAppNotification(glucoseRepository: GlucoseRepository)

    companion object {
        const val FOREGROUND_NOTIFICATION_ID = 1
    }
}