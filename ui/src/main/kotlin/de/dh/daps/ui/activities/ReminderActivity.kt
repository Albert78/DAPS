package de.dh.daps.ui.activities

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import de.dh.daps.common.domain.model.alarm.getDefaultAlertProfiles
import de.dh.daps.common.domain.model.data.Timestamp
import de.dh.daps.common.domain.model.reminder.Reminder
import de.dh.daps.core.infrastructure.RegistryProvider
import de.dh.daps.ui.common.theme.AppTheme
import de.dh.daps.ui.screens.fullscreenalerts.ReminderScreen
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.minutes

class ReminderActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        turnOnScreenAndShowOnLock()

        val registry = (application as RegistryProvider).registry

        setContent {
            val allReminders by registry.reminderManager.activeReminders.collectAsState()
            val now = Timestamp.now()
            val dueReminders = allReminders.filter { it.isDue(now) }

            if (dueReminders.isEmpty()) {
                finish()
                return@setContent
            }

            AppTheme(darkTheme = true) {
                ReminderScreen(
                    reminders = dueReminders,
                    onSnoozeReminder = { reminder ->
                        lifecycleScope.launch {
                            val snoozeUntil = Timestamp(System.currentTimeMillis() + 15.minutes.inWholeMilliseconds)
                            registry.reminderManager.snoozeReminder(reminder.id, snoozeUntil)
                            val remaining = registry.reminderManager.activeReminders.value.filter { it.isDue() }
                            if (remaining.isEmpty()) {
                                finish()
                            }
                        }
                    },
                    onDismissReminder = { reminder ->
                        lifecycleScope.launch {
                            registry.reminderManager.dismissReminder(reminder.id)
                            val remaining = registry.reminderManager.activeReminders.value.filter { it.isDue() }
                            if (remaining.isEmpty()) {
                                finish()
                            }
                        }
                    },
                    onOpenReminder = { reminder ->
                        lifecycleScope.launch {
                            if (reminder.targetRoute.isNotBlank()) {
                                val intent = Intent(Intent.ACTION_VIEW, reminder.targetRoute.toUri()).apply {
                                    setClass(this@ReminderActivity, MainActivity::class.java)
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                                }
                                startActivity(intent)
                            }
                            registry.reminderManager.dismissReminder(reminder.id)
                            val remaining = registry.reminderManager.activeReminders.value.filter { it.isDue() }
                            if (remaining.isEmpty()) {
                                finish()
                            }
                        }
                    },
                    onClose = {
                        finish()
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val registry = (application as RegistryProvider).registry
        lifecycleScope.launch {
            val activeProfile = registry.therapyRepository.getCurrentTherapySettingsOrNull()?.effectiveAlertProfile
                ?: getDefaultAlertProfiles(this@ReminderActivity).first { it.isDefault }

            val dueReminders = registry.reminderManager.activeReminders.value.filter { it.isDue() }
            val firstDue = dueReminders.firstOrNull()
            if (firstDue != null) {
                val config = activeProfile.getConfigFor(firstDue)
                if (config.enabled) {
                    registry.alarmPlayerManager.playAlarm(config)
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        val registry = (application as RegistryProvider).registry
        registry.alarmPlayerManager.stopAlarm()
    }

    private fun turnOnScreenAndShowOnLock() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
        @Suppress("DEPRECATION")
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        )
    }

    companion object {
        fun start(context: Context, reminder: Reminder? = null) {
            val intent = createStartIntent(context, reminder).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            context.startActivity(intent)
        }

        fun createStartIntent(context: Context, reminder: Reminder? = null): Intent {
            return Intent(context, ReminderActivity::class.java).apply {
                if (reminder != null) {
                    putExtra("REMINDER_ID", reminder.id)
                }
            }
        }
    }
}