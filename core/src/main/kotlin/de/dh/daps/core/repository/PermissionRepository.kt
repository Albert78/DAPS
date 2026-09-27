package de.dh.daps.core.repository

import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.PowerManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Represents a snapshot of the current system permission state for the app.
 */
data class PermissionSummary(
    val canScheduleExactAlarms: Boolean,
    val canPostNotifications: Boolean,
    val canShowFullscreenActivity: Boolean,
    val isIgnoringBatteryOptimizations: Boolean,
    val isAutoRevokeExempted: Boolean,
    val isAllGranted: Boolean,
    val numMissing: Int,
)

/**
 * Repository for checking and observing system permissions required by the app.
 */
class PermissionRepository(private val context: Context) {
    private val _permissionSummary = MutableStateFlow(checkPermissions())
    val permissionSummary: StateFlow<PermissionSummary> = _permissionSummary.asStateFlow()

    fun refreshPermissions() {
        _permissionSummary.value = checkPermissions()
    }

    fun checkPermissions(): PermissionSummary {
        val scheduleExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            alarmManager?.canScheduleExactAlarms() == true
        } else {
            true
        }

        val postNotifications = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.areNotificationsEnabled() == true
        } else {
            true
        }

        val showFullscreen = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.canUseFullScreenIntent() == true
        } else {
            true
        }

        val ignoreBattery = run {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            powerManager?.isIgnoringBatteryOptimizations(context.packageName) == true
        }

        val autoRevokeExempted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            context.packageManager.isAutoRevokeWhitelisted
        } else {
            true
        }

        val checks = listOf(scheduleExact, postNotifications, showFullscreen, ignoreBattery, autoRevokeExempted)
        val missingCount = checks.count { !it }
        val allGranted = missingCount == 0

        return PermissionSummary(
            canScheduleExactAlarms = scheduleExact,
            canPostNotifications = postNotifications,
            canShowFullscreenActivity = showFullscreen,
            isIgnoringBatteryOptimizations = ignoreBattery,
            isAutoRevokeExempted = autoRevokeExempted,
            isAllGranted = allGranted,
            numMissing = missingCount
        )
    }
}