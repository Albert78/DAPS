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
import de.dh.daps.R
import de.dh.daps.common.ID_UNDEFINED
import de.dh.daps.common.model.DeferredBolus
import de.dh.daps.common.model.MealReminder
import de.dh.daps.common.model.data.AlarmType
import de.dh.daps.common.model.data.BgDelta
import de.dh.daps.common.model.data.BgValue
import de.dh.daps.common.model.data.CarbsUnit
import de.dh.daps.common.model.data.GlucoseUnit
import de.dh.daps.common.navigation.ManualControlInitialDialog
import de.dh.daps.core.aps.ApsRecommendation
import de.dh.daps.core.repository.GlucoseRepository
import de.dh.daps.core.system.AndroidNotifications
import de.dh.daps.core.system.RegistryProvider
import de.dh.daps.ui.activities.AlarmActivity
import de.dh.daps.ui.activities.MainActivity
import de.dh.daps.ui.common.formatCarbsValue
import de.dh.daps.ui.screens.permissions.canPostNotifications
import de.dh.daps.common.R as CommonR
import de.dh.daps.ui.R as UiR

/**
 * Low-level notification manager for communicating with the Android system.
 */
class AndroidNotificationsImpl(
    val context: Context
): AndroidNotifications {
    private val manager = context.getSystemService<NotificationManager>()!!

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

    private fun getGlucoseUnit(): GlucoseUnit {
        val registry = (context.applicationContext as? RegistryProvider)?.registry
        return registry?.appPreferencesRepository?.glucoseUnit?.value ?: GlucoseUnit.MG_DL
    }

    private fun getCarbsUnit(): CarbsUnit {
        val registry = (context.applicationContext as? RegistryProvider)?.registry
        return registry?.appPreferencesRepository?.carbsUnit?.value ?: CarbsUnit.GRAMS
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

    override fun showRecommendationNotification(recommendation: ApsRecommendation) {
        val title = when (recommendation) {
            is ApsRecommendation.Carbs -> context.getString(UiR.string.recommendation_title_carbs)
            is ApsRecommendation.Bolus -> context.getString(UiR.string.recommendation_title_bolus)
            is ApsRecommendation.TempBasal -> context.getString(UiR.string.recommendation_title_temp_basal)
        }
        val text = when (recommendation) {
            is ApsRecommendation.Carbs -> context.getString(
                UiR.string.recommendation_text_carbs,
                formatCarbsValue(recommendation.amountInGram.toDouble(), getCarbsUnit(), context)
            )
            is ApsRecommendation.Bolus -> context.getString(
                UiR.string.recommendation_text_bolus,
                recommendation.amount.iu
            )
            is ApsRecommendation.TempBasal -> context.getString(
                UiR.string.recommendation_text_temp_basal,
                recommendation.percent,
                recommendation.durationInHours
            )
        }

        val intent = when (recommendation) {
            is ApsRecommendation.Carbs -> {
                MainActivity.createMealCorrectionBolusIntent(context, recommendation.amountInGram.toDouble())
            }
            is ApsRecommendation.Bolus -> {
                MainActivity.createManualControlIntent(context, ManualControlInitialDialog.BOLUS)
            }
            is ApsRecommendation.TempBasal -> {
                MainActivity.createManualControlIntent(context, ManualControlInitialDialog.TEMP_BASAL)
            }
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            RECOMMENDATION_NOTIFICATION_ID,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, RECOMMENDATION_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_RECOMMENDATION)
            .build()

        // Use the same ID for all recommendations, we only support one at a time
        notify(RECOMMENDATION_NOTIFICATION_ID, notification)
    }

    override fun cancelRecommendationNotification() {
        manager.cancel(RECOMMENDATION_NOTIFICATION_ID)
    }

    override fun showMealReminderNotification(mealReminder: MealReminder) {
        val title = context.getString(UiR.string.notification_meal_reminder_title)
        val text = if (mealReminder.description.isNotBlank()) {
            context.getString(UiR.string.notification_meal_reminder_text_with_description, mealReminder.description)
        } else {
            context.getString(UiR.string.notification_meal_reminder_text_default)
        }

        val mealId = mealReminder.mealId ?: ID_UNDEFINED
        val mealIntent = MainActivity.createEditMealIntent(context, mealId)
        val pendingIntent = PendingIntent.getActivity(
            context,
            mealId.hashCode(),
            mealIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, RECOMMENDATION_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .build()

        notify(MEAL_REMINDER_NOTIFICATION_ID, notification)
    }

    override fun showDeferredBolusRecommendationNotification(dueDeferredBoluses: List<DeferredBolus>) {
        val totalAmount = dueDeferredBoluses.sumOf { it.amount.iu }
        val title = context.getString(UiR.string.notification_deferred_bolus_title)
        val text = context.getString(UiR.string.notification_deferred_bolus_text, totalAmount)

        val dashboardIntent = MainActivity.createStartDashboardIntent(context)
        val pendingIntent = PendingIntent.getActivity(
            context, 0,
            dashboardIntent, PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, RECOMMENDATION_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_RECOMMENDATION)
            .build()

        notify(DEFERRED_BOLUS_NOTIFICATION_ID, notification)
    }

    override fun showAlarmNotification(alarmType: AlarmType, bgValue: BgValue?, isFullScreen: Boolean) {
        val unit = getGlucoseUnit()
        val title = getAlarmTitle(alarmType)
        val contentText = getAlarmContentText(alarmType, bgValue, unit)

        val dashboardIntent = MainActivity.createStartDashboardIntent(context)
        val pendingIntent = PendingIntent.getActivity(
            context, 0,
            dashboardIntent, PendingIntent.FLAG_IMMUTABLE
        )

        val alarmActivityIntent = Intent(context, AlarmActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            context, 0,
            alarmActivityIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val snooze15Intent = AlarmBroadcastReceiver.createSnoozeIntent(context, alarmType, 15)
        val snooze15PendingIntent = PendingIntent.getBroadcast(
            context, 15, snooze15Intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snooze30Intent = AlarmBroadcastReceiver.createSnoozeIntent(context, alarmType, 30)
        val snooze30PendingIntent = PendingIntent.getBroadcast(
            context, 30, snooze30Intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, ALGORITHM_ISSUE_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(contentText)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
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
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)

        if (isFullScreen) {
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
        AlarmType.CGM_SIGNAL_LOSS -> context.getString(UiR.string.alarm_type_cgm_signal_loss)
        AlarmType.SYSTEM_BATTERY_LOW -> context.getString(UiR.string.alarm_type_system_battery_low)
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
        AlarmType.PUMP_OCCLUSION -> context.getString(UiR.string.pump_issue_occlusion)
        AlarmType.PUMP_SUSPENDED -> context.getString(UiR.string.pump_issue_suspended)
        AlarmType.PUMP_LOW_INSULIN -> context.getString(UiR.string.pump_issue_low_insulin)
        AlarmType.PUMP_LOW_BATTERY -> context.getString(UiR.string.pump_issue_low_battery)
        AlarmType.SYSTEM_BATTERY_LOW -> context.getString(UiR.string.alarm_type_system_battery_low)
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
        const val RECOMMENDATION_NOTIFICATION_ID = 2
        const val ALGORITHM_ISSUE_NOTIFICATION_ID = 3
        const val MEAL_REMINDER_NOTIFICATION_ID = 4
        const val DEFERRED_BOLUS_NOTIFICATION_ID = 5
        const val SERVICE_CHANNEL_ID = "aps_service_channel"
        const val RECOMMENDATION_CHANNEL_ID = "aps_recommendation_channel"
        const val ALGORITHM_ISSUE_CHANNEL_ID = "aps_algorithm_issue_channel"
    }
}