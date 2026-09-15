package de.dh.daps.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import de.dh.daps.common.model.data.AlarmType
import de.dh.daps.core.system.RegistryProvider

class AlarmBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val registry = (context.applicationContext as? RegistryProvider)?.registry ?: return
        val alarmTypeName = intent.getStringExtra(EXTRA_ALARM_TYPE)
        val alarmType = alarmTypeName?.let {
            try {
                AlarmType.valueOf(it)
            } catch (e: Exception) {
                null
            }
        }

        when (intent.action) {
            ACTION_SNOOZE_ALARM -> {
                val minutes = intent.getIntExtra(EXTRA_SNOOZE_MINUTES, 15)
                Log.d(TAG, "Snooze action received for $alarmType for $minutes minutes")
                if (alarmType != null) {
                    registry.alarmSnoozeManager.snoozeAlarm(alarmType, minutes)
                } else {
                    // Snooze all active firing alarms
                    val activeFiring = registry.alarmEvaluator.activeFiringAlarm.value
                    if (activeFiring != null) {
                        registry.alarmSnoozeManager.snoozeAlarm(activeFiring, minutes)
                    }
                }
                registry.alarmPlayerManager.stopAlarm()
            }

            ACTION_DISMISS_ALARM -> {
                Log.d(TAG, "Dismiss action received for $alarmType")
                if (alarmType != null) {
                    // Default to 15 min snooze on dismiss action if issue persists
                    registry.alarmSnoozeManager.snoozeAlarm(alarmType, 15)
                }
                registry.alarmPlayerManager.stopAlarm()
            }
        }
    }

    companion object {
        private const val TAG = "AlarmBroadcastReceiver"
        const val ACTION_SNOOZE_ALARM = "de.dh.daps.ACTION_SNOOZE_ALARM"
        const val ACTION_DISMISS_ALARM = "de.dh.daps.ACTION_DISMISS_ALARM"
        const val EXTRA_ALARM_TYPE = "extra_alarm_type"
        const val EXTRA_SNOOZE_MINUTES = "extra_snooze_minutes"

        fun createSnoozeIntent(context: Context, alarmType: AlarmType?, minutes: Int): Intent {
            return Intent(context, AlarmBroadcastReceiver::class.java).apply {
                action = ACTION_SNOOZE_ALARM
                if (alarmType != null) {
                    putExtra(EXTRA_ALARM_TYPE, alarmType.name)
                }
                putExtra(EXTRA_SNOOZE_MINUTES, minutes)
            }
        }

        fun createDismissIntent(context: Context, alarmType: AlarmType?): Intent {
            return Intent(context, AlarmBroadcastReceiver::class.java).apply {
                action = ACTION_DISMISS_ALARM
                if (alarmType != null) {
                    putExtra(EXTRA_ALARM_TYPE, alarmType.name)
                }
            }
        }
    }
}