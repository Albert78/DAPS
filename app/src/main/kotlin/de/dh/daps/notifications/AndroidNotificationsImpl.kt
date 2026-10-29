package de.dh.daps.notifications

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Context.NOTIFICATION_SERVICE
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService
import androidx.core.net.toUri
import de.dh.daps.R
import de.dh.daps.common.domain.model.alarm.ActiveAlarmGroup
import de.dh.daps.common.domain.model.alarm.AlarmType
import de.dh.daps.common.domain.model.alarm.AlertProfile
import de.dh.daps.common.domain.model.alarm.getDefaultAlertProfiles
import de.dh.daps.common.domain.model.data.BgDelta
import de.dh.daps.common.domain.model.data.BgValue
import de.dh.daps.common.domain.model.data.CarbsUnit
import de.dh.daps.common.domain.model.data.GlucoseUnit
import de.dh.daps.common.domain.model.reminder.Reminder
import de.dh.daps.common.domain.model.therapy.CurrentTherapySettings
import de.dh.daps.common.repository.AppPreferencesRepository
import de.dh.daps.core.alerts.getConfigFor
import de.dh.daps.core.infrastructure.AndroidNotifications
import de.dh.daps.core.orchestration.ActiveRecommendations
import de.dh.daps.core.orchestration.ApsRecommendation
import de.dh.daps.core.repository.GlucoseRepository
import de.dh.daps.core.repository.TherapyRepository
import de.dh.daps.ui.activities.AlarmActivity
import de.dh.daps.ui.activities.MainActivity
import de.dh.daps.ui.activities.RecommendationActivity
import de.dh.daps.ui.activities.ReminderActivity
import de.dh.daps.ui.common.formatCarbsValue
import de.dh.daps.ui.screens.permissions.canPostNotifications
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import de.dh.daps.common.R as CommonR
import de.dh.daps.ui.R as UiR

/**
 * Low-level notification manager for communicating with the Android system.
 */
class AndroidNotificationsImpl(
    val context: Context,
    private val therapyRepository: TherapyRepository,
    private val appPreferencesRepository: AppPreferencesRepository,
    private val scope: CoroutineScope
): AndroidNotifications {
    private val manager = context.getSystemService<NotificationManager>()!!

    private val currentTherapySettings: StateFlow<CurrentTherapySettings?> =
        therapyRepository.observeCurrentTherapySettingsOrNull()
            .stateIn(
                scope = scope,
                started = SharingStarted.Eagerly,
                initialValue = null
            )

    override fun createNotificationChannels() {
        // Channel for the foreground service (BG values)
        val serviceName = context.getString(UiR.string.aps_service_notification_channel_name)
        val serviceImportance = NotificationManager.IMPORTANCE_LOW
        val serviceChannel = NotificationChannel(SERVICE_CHANNEL_ID, serviceName, serviceImportance)
        serviceChannel.setShowBadge(false)

        // Channel for recommendations (User interaction required)
        val recName = context.getString(UiR.string.recommendation_notification_channel_name)
        val recImportance = NotificationManager.IMPORTANCE_HIGH
        val recChannel = NotificationChannel(RECOMMENDATION_CHANNEL_ID, recName, recImportance)
        recChannel.enableVibration(true)
        recChannel.setShowBadge(true)

        // Channel for algorithm issues (Manual intervention required)
        val issueName = context.getString(UiR.string.algorithm_issue_notification_channel_name)
        val issueImportance = NotificationManager.IMPORTANCE_HIGH
        val issueChannel = NotificationChannel(ALGORITHM_ISSUE_CHANNEL_ID, issueName, issueImportance)
        issueChannel.enableVibration(true)
        issueChannel.setShowBadge(true)

        val notificationManager = context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(serviceChannel)
        notificationManager.createNotificationChannel(recChannel)
        notificationManager.createNotificationChannel(issueChannel)
    }

    private fun getActiveAlertProfile(): AlertProfile {
        return currentTherapySettings.value?.effectiveAlertProfile
            ?: getDefaultAlertProfiles(context).first { it.isDefault }
    }

    private fun getGlucoseUnit(): GlucoseUnit {
        return appPreferencesRepository.glucoseUnit.value
    }

    private fun getCarbsUnit(): CarbsUnit {
        return appPreferencesRepository.carbsUnit.value
    }

    fun getBgValueString(sample: BgValue?, unit: GlucoseUnit, forceSign: Boolean): String? {
        if (sample == null) return null
        val valStr = sample.toString(unit)
        val unitStr = when (unit) {
            GlucoseUnit.MG_DL -> context.getString(CommonR.string.glucose_unit_mgdl)
            GlucoseUnit.MMOL -> context.getString(CommonR.string.glucose_unit_mmol)
        }
        return if (forceSign) {
            val sign = if (sample.mgdl > 0) "+" else ""
            "$sign$valStr $unitStr"
        } else {
            "$valStr $unitStr"
        }
    }

    fun getBgDeltaString(delta: BgValue?, unit: GlucoseUnit): String? {
        if (delta == null) return null
        val deltaValue = BgDelta.fromMgDl(delta.mgdl)
        val valStr = deltaValue.toDiff(unit)
        val unitStr = when (unit) {
            GlucoseUnit.MG_DL -> context.getString(CommonR.string.glucose_unit_mgdl)
            GlucoseUnit.MMOL -> context.getString(CommonR.string.glucose_unit_mmol)
        }
        return context.getString(CommonR.string.bg_delta_format, valStr, unitStr)
    }

    override fun createMainAppNotification(glucoseRepository: GlucoseRepository): Notification {
        val data = MainAppNotificationData.create(glucoseRepository)
        Log.d(TAG, "Build notification for ${data.lastBgSample}")
        val unit = getGlucoseUnit()
        val bgValueStr = getBgValueString(data.lastBgSample?.value, unit, false)
        val title = bgValueStr ?: context.getString(UiR.string.aps_service_notification_content_no_value_yet)
        val details = getBgDeltaString(data.getBgDelta(), unit)

        val dashboardIntent = MainActivity.createStartDashboardIntent(context)
        val goToEventPendingIntent = PendingIntent.getActivity(
            context, 0,
            dashboardIntent, PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, SERVICE_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(details)
            .setSmallIcon(R.mipmap.ic_launcher) // Use app icon for now
            .setContentIntent(goToEventPendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setAllowSystemGeneratedContextualActions(false)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun updateMainAppNotification(glucoseRepository: GlucoseRepository) {
        val notification: Notification = createMainAppNotification(glucoseRepository)
        notify(AndroidNotifications.FOREGROUND_NOTIFICATION_ID, notification)
    }

    override fun updateRecommendationNotifications(recommendations: ActiveRecommendations) {
        val activeProfile = getActiveAlertProfile()

        val carbs = recommendations.carbs
        if (carbs != null) {
            val config = activeProfile.getConfigFor(carbs)
            if (config.enabled) {
                showCarbsRecommendationNotification(carbs, isFullScreen = config.isFullScreen)
            } else {
                manager.cancel(RECOMMENDATION_CARBS_NOTIFICATION_ID)
            }
        } else {
            manager.cancel(RECOMMENDATION_CARBS_NOTIFICATION_ID)
        }

        val bolus = recommendations.bolus
        if (bolus != null) {
            val config = activeProfile.getConfigFor(bolus)
            if (config.enabled) {
                showBolusRecommendationNotification(bolus, isFullScreen = config.isFullScreen)
            } else {
                manager.cancel(RECOMMENDATION_BOLUS_NOTIFICATION_ID)
            }
        } else {
            manager.cancel(RECOMMENDATION_BOLUS_NOTIFICATION_ID)
        }

        val tempBasal = recommendations.tempBasal
        if (tempBasal != null) {
            val config = activeProfile.getConfigFor(tempBasal)
            if (config.enabled) {
                showTempBasalRecommendationNotification(tempBasal, isFullScreen = config.isFullScreen)
            } else {
                manager.cancel(RECOMMENDATION_TEMP_BASAL_NOTIFICATION_ID)
            }
        } else {
            manager.cancel(RECOMMENDATION_TEMP_BASAL_NOTIFICATION_ID)
        }
    }

    private fun showCarbsRecommendationNotification(
        carbs: ApsRecommendation.Carbs,
        isFullScreen: Boolean
    ) {
        val title = context.getString(UiR.string.recommendation_title_carbs)
        val text = context.getString(
            UiR.string.recommendation_text_carbs,
            formatCarbsValue(carbs.amountInGram.toDouble(), getCarbsUnit(), context)
        )
        val intent = MainActivity.createRecommendationIntent(context, carbs)
        val pendingIntent = PendingIntent.getActivity(
            context,
            RECOMMENDATION_CARBS_NOTIFICATION_ID,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val builder = NotificationCompat.Builder(context, RECOMMENDATION_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_RECOMMENDATION)

        if (isFullScreen) {
            val fullScreenIntent = RecommendationActivity.createStartIntent(context)
            val fullScreenPendingIntent = PendingIntent.getActivity(
                context,
                RECOMMENDATION_CARBS_NOTIFICATION_ID,
                fullScreenIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            builder.setFullScreenIntent(fullScreenPendingIntent, true)
        }

        notify(RECOMMENDATION_CARBS_NOTIFICATION_ID, builder.build())
    }

    private fun showBolusRecommendationNotification(
        bolus: ApsRecommendation.Bolus,
        isFullScreen: Boolean
    ) {
        val hasDeferredPart = !bolus.includedDeferredBoluses.isNullOrEmpty()
        val title = if (hasDeferredPart) {
            context.getString(UiR.string.notification_deferred_bolus_title)
        } else {
            context.getString(UiR.string.recommendation_title_bolus)
        }
        val text = if (hasDeferredPart) {
            context.getString(UiR.string.notification_deferred_bolus_text, bolus.amount.iu)
        } else {
            context.getString(UiR.string.recommendation_text_bolus, bolus.amount.iu)
        }
        val intent = MainActivity.createRecommendationIntent(context, bolus)
        val pendingIntent = PendingIntent.getActivity(
            context,
            RECOMMENDATION_BOLUS_NOTIFICATION_ID,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val builder = NotificationCompat.Builder(context, RECOMMENDATION_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_RECOMMENDATION)

        if (isFullScreen) {
            val fullScreenIntent = RecommendationActivity.createStartIntent(context)
            val fullScreenPendingIntent = PendingIntent.getActivity(
                context,
                RECOMMENDATION_BOLUS_NOTIFICATION_ID,
                fullScreenIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            builder.setFullScreenIntent(fullScreenPendingIntent, true)
        }

        notify(RECOMMENDATION_BOLUS_NOTIFICATION_ID, builder.build())
    }

    private fun showTempBasalRecommendationNotification(
        tempBasal: ApsRecommendation.TempBasal,
        isFullScreen: Boolean
    ) {
        val title = context.getString(UiR.string.recommendation_title_temp_basal)
        val text = context.getString(
            UiR.string.recommendation_text_temp_basal,
            tempBasal.percent,
            tempBasal.durationInHours
        )
        val intent = MainActivity.createRecommendationIntent(context, tempBasal)
        val pendingIntent = PendingIntent.getActivity(
            context,
            RECOMMENDATION_TEMP_BASAL_NOTIFICATION_ID,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val builder = NotificationCompat.Builder(context, RECOMMENDATION_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_RECOMMENDATION)

        if (isFullScreen) {
            val fullScreenIntent = RecommendationActivity.createStartIntent(context)
            val fullScreenPendingIntent = PendingIntent.getActivity(
                context,
                RECOMMENDATION_TEMP_BASAL_NOTIFICATION_ID,
                fullScreenIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            builder.setFullScreenIntent(fullScreenPendingIntent, true)
        }

        notify(RECOMMENDATION_TEMP_BASAL_NOTIFICATION_ID, builder.build())
    }

    override fun showReminderNotification(reminder: Reminder) {
        val activeProfile = getActiveAlertProfile()
        val config = activeProfile.getConfigFor(reminder)
        if (!config.enabled) return

        val title = reminder.title.ifBlank { context.getString(CommonR.string.notification_reminder_title) }
        val text = reminder.description

        val pendingIntent = if (reminder.targetRoute.isNotBlank()) {
            val routeIntent = Intent(Intent.ACTION_VIEW, reminder.targetRoute.toUri()).apply {
                setClass(context, MainActivity::class.java)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            PendingIntent.getActivity(
                context,
                reminder.id.hashCode(),
                routeIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        } else {
            val intent = ReminderActivity.createStartIntent(context, reminder)
            PendingIntent.getActivity(
                context,
                reminder.id.hashCode(),
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        }

        val builder = NotificationCompat.Builder(context, RECOMMENDATION_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_EVENT)

        if (config.isFullScreen) {
            val fullScreenIntent = ReminderActivity.createStartIntent(context, reminder).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val fullScreenPendingIntent = PendingIntent.getActivity(
                context,
                reminder.id.hashCode(),
                fullScreenIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            builder.setFullScreenIntent(fullScreenPendingIntent, true)
        }

        val notificationId = REMINDER_NOTIFICATION_ID + (reminder.id % 10000).toInt()
        notify(notificationId, builder.build())
    }

    override fun showAlarmNotification(alarmType: AlarmType, bgValue: BgValue?, isFullScreen: Boolean) {
        val group = ActiveAlarmGroup(
            heroAlarms = listOf(alarmType),
            otherAlarms = emptyList(),
            highestSeverity = alarmType.defaultSeverity,
            primaryAlarm = alarmType
        )
        showAlarmNotification(group, bgValue, isFullScreen)
    }

    override fun showAlarmNotification(
        alarmGroup: ActiveAlarmGroup,
        bgValue: BgValue?,
        isFullScreen: Boolean
    ) {
        val activeProfile = getActiveAlertProfile()
        val config = activeProfile.getConfigFor(alarmGroup.primaryAlarm)
        if (!config.enabled) {
            return
        }

        val unit = getGlucoseUnit()
        val heroTitles = alarmGroup.heroAlarms.joinToString(", ") { getAlarmTitle(it) }
        val title = if (alarmGroup.otherAlarms.isNotEmpty()) {
            "$heroTitles (+${alarmGroup.otherAlarms.size})"
        } else {
            heroTitles
        }

        val primaryContentText = getAlarmContentText(alarmGroup.primaryAlarm, bgValue, unit)

        val alarmActivityIntent = Intent(context, AlarmActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context, 0,
            alarmActivityIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val fullScreenPendingIntent = PendingIntent.getActivity(
            context, 0,
            alarmActivityIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Snooze all active alarms when snooze action is selected from notification
        val snooze15Intent = AlarmBroadcastReceiver.createSnoozeIntent(context, null, 15)
        val snooze15PendingIntent = PendingIntent.getBroadcast(
            context, 15, snooze15Intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snooze30Intent = AlarmBroadcastReceiver.createSnoozeIntent(context, null, 30)
        val snooze30PendingIntent = PendingIntent.getBroadcast(
            context, 30, snooze30Intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val inboxStyle = NotificationCompat.InboxStyle()
        alarmGroup.heroAlarms.forEach { hero ->
            val desc = getAlarmContentText(hero, bgValue, unit)
            val lineText = if (desc.isNotBlank()) "${getAlarmTitle(hero)}: $desc" else getAlarmTitle(hero)
            inboxStyle.addLine(lineText)
        }
        if (alarmGroup.otherAlarms.isNotEmpty()) {
            inboxStyle.addLine("+ ${alarmGroup.otherAlarms.size} weitere Alarme")
        }

        val builder = NotificationCompat.Builder(context, ALGORITHM_ISSUE_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(primaryContentText)
            .setStyle(inboxStyle)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(contentPendingIntent)
            .addAction(
                R.mipmap.ic_launcher,
                context.getString(UiR.string.alarm_action_snooze_15),
                snooze15PendingIntent
            )
            .addAction(
                R.mipmap.ic_launcher,
                context.getString(UiR.string.alarm_action_snooze_30),
                snooze30PendingIntent
            )
            .setDeleteIntent(snooze15PendingIntent)
            .setAutoCancel(false)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)

        if (isFullScreen && config.isFullScreen) {
            builder.setFullScreenIntent(fullScreenPendingIntent, true)
        }

        val notification = builder.build()

        notify(ALGORITHM_ISSUE_NOTIFICATION_ID, notification)
    }

    private fun getAlarmTitle(alarmType: AlarmType): String = when (alarmType) {
        AlarmType.CRITICAL_LOW_BG -> context.getString(UiR.string.alarm_type_critical_low_bg)
        AlarmType.LOW_BG -> context.getString(UiR.string.alarm_type_low_bg)
        AlarmType.HIGH_BG -> context.getString(UiR.string.alarm_type_high_bg)
        AlarmType.PUMP_OCCLUSION -> context.getString(UiR.string.alarm_type_pump_occlusion)
        AlarmType.PUMP_SUSPENDED -> context.getString(UiR.string.alarm_type_pump_suspended)
        AlarmType.PUMP_LOW_INSULIN -> context.getString(UiR.string.alarm_type_pump_low_insulin)
        AlarmType.PUMP_LOW_BATTERY -> context.getString(UiR.string.alarm_type_pump_low_battery)
        AlarmType.PUMP_STALE_BOND -> context.getString(UiR.string.alarm_type_pump_stale_bond)
        AlarmType.PUMP_CATHETER_CHANGE_WARN -> context.getString(UiR.string.alarm_type_pump_catheter_change_warn)
        AlarmType.PUMP_PATCH_CHANGE_WARN -> context.getString(UiR.string.alarm_type_pump_patch_change_warn)
        AlarmType.CGM_SIGNAL_LOSS -> context.getString(UiR.string.alarm_type_cgm_signal_loss)
        AlarmType.CGM_EXPIRED -> context.getString(UiR.string.alarm_type_cgm_expired)
        AlarmType.CGM_SENSOR_ERROR -> context.getString(UiR.string.alarm_type_cgm_sensor_error)
        AlarmType.CGM_STALE_BOND -> context.getString(UiR.string.alarm_type_cgm_stale_bond)
        AlarmType.SYSTEM_BATTERY_LOW -> context.getString(UiR.string.alarm_type_system_battery_low)
        AlarmType.SYSTEM_BLUETOOTH_OFF -> context.getString(UiR.string.alarm_type_system_bluetooth_off)
        AlarmType.SYSTEM_THERAPY_SETTINGS_MISSING -> context.getString(UiR.string.alarm_type_system_therapy_settings_missing)
    }

    private fun getAlarmContentText(alarmType: AlarmType, bgValue: BgValue?, unit: GlucoseUnit): String = when (alarmType) {
        AlarmType.CRITICAL_LOW_BG, AlarmType.LOW_BG, AlarmType.HIGH_BG -> {
            if (bgValue != null) {
                getBgValueString(bgValue, unit, false) ?: ""
            } else {
                ""
            }
        }
        AlarmType.CGM_SIGNAL_LOSS -> context.getString(UiR.string.core_issue_no_recent_values, 15)
        AlarmType.CGM_EXPIRED -> context.getString(UiR.string.glucose_source_issue_expired)
        AlarmType.CGM_SENSOR_ERROR -> context.getString(UiR.string.glucose_source_issue_sensor_error)
        AlarmType.CGM_STALE_BOND -> context.getString(UiR.string.glucose_source_issue_stale_bluetooth_bond)
        AlarmType.PUMP_OCCLUSION -> context.getString(UiR.string.pump_issue_occlusion)
        AlarmType.PUMP_SUSPENDED -> context.getString(UiR.string.pump_issue_suspended)
        AlarmType.PUMP_LOW_INSULIN -> context.getString(UiR.string.pump_issue_low_insulin)
        AlarmType.PUMP_LOW_BATTERY -> context.getString(UiR.string.pump_issue_low_battery)
        AlarmType.PUMP_STALE_BOND -> context.getString(UiR.string.pump_issue_stale_bluetooth_bond)
        AlarmType.PUMP_CATHETER_CHANGE_WARN -> context.getString(UiR.string.pump_issue_catheter_change_warn)
        AlarmType.PUMP_PATCH_CHANGE_WARN -> context.getString(UiR.string.pump_issue_patch_change_warn)
        AlarmType.SYSTEM_BATTERY_LOW -> context.getString(UiR.string.alarm_type_system_battery_low)
        AlarmType.SYSTEM_BLUETOOTH_OFF -> context.getString(UiR.string.alarm_type_system_bluetooth_off_desc)
        AlarmType.SYSTEM_THERAPY_SETTINGS_MISSING -> context.getString(UiR.string.alarm_type_system_therapy_settings_missing_desc)
    }

    override fun cancelAlarmNotification() {
        manager.cancel(ALGORITHM_ISSUE_NOTIFICATION_ID)
    }

    private fun notify(notificationId: Int, notification: Notification) {
        if (!canPostNotifications(context)) {
            Log.w(TAG, "Missing permissions to show notification")
            return
        }
        try {
            manager.notify(notificationId, notification)
        } catch (e: SecurityException) {
            // Fallback to log message below
        }
    }

    companion object {
        val TAG = AndroidNotificationsImpl::class.simpleName
        const val RECOMMENDATION_CARBS_NOTIFICATION_ID = 2
        const val RECOMMENDATION_BOLUS_NOTIFICATION_ID = 3
        const val RECOMMENDATION_TEMP_BASAL_NOTIFICATION_ID = 4
        const val ALGORITHM_ISSUE_NOTIFICATION_ID = 5
        const val REMINDER_NOTIFICATION_ID = 6
        const val SERVICE_CHANNEL_ID = "aps_service_channel"
        const val RECOMMENDATION_CHANNEL_ID = "aps_recommendation_channel"
        const val ALGORITHM_ISSUE_CHANNEL_ID = "aps_algorithm_issue_channel"
    }
}