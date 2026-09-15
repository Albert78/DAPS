package de.dh.daps.ui.screens.permissions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import de.dh.daps.core.SystemRegistry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * This view model is used to show the state of needed and granted permissions to the user.
 * It can be used from multiple activities, since the information about granted permissions is needed
 * on several places in the app (permissions screen, notification messages about missing permissions
 * in other screens).
 * This view model isn't implemented in the typical layer architecture (Repository - ViewModel - View),
 * since permission management (querying and requesting permissions, observing app permissions) is a
 * subject of the UI in Android. We request the current app permissions status in method
 * [updateAppPermissions] but we need the UI to call this method when permissions have changed.
 */
class PermissionsViewModel(
    private val systemRegistry: SystemRegistry
) : ViewModel() {
    private val _uiState = MutableStateFlow(PermissionsUiModel.loading())
    val uiState = _uiState.asStateFlow()

    init {
        updateAppPermissions()
    }

    /**
     * Updates the internal state of the system permissions with the current system settings for this app.
     */
    fun updateAppPermissions() {
        val appContext = systemRegistry.appContext

        val canSchedule = canScheduleExactAlarms(appContext)
        val canShowFullscreen = canShowFullscreenActivity(appContext)
        val canPost = canPostNotifications(appContext)
        val isIgnoring = isIgnoringBatteryOptimizations(appContext)
        val isAutoRevoke = isAutoRevokePermissions(appContext)

        val activeFunctions = RestrictedAppFunctions.getActiveAppFunctions()

        val allNeededPermissions = activeFunctions.flatMap { it.neededPermissions }.toSet()

        fun getStatus(permission: NeededPermission, isGranted: Boolean): PermissionStatus {
            return if (allNeededPermissions.contains(permission)) {
                if (isGranted) PermissionStatus.Granted else PermissionStatus.Denied
            } else {
                PermissionStatus.NotNeeded
            }
        }

        val alarmPermissionStatus = getStatus(NeededPermission.SCHEDULE_EXACT_ALARMS, canSchedule)
        val notificationPermissionStatus = getStatus(NeededPermission.POST_NOTIFICATIONS, canPost)
        val fullscreenPermissionStatus = getStatus(NeededPermission.SHOW_FULLSCREEN_ACTIVITY, canShowFullscreen)
        val ignoreBatteryOptimizationPermissionStatus = getStatus(NeededPermission.IGNORE_BATTERY_OPTIMIZATIONS, isIgnoring)
        // For auto-revoke, the permission status is "granted" if the app is exempted, which means isAutoRevokePermissions() is false.
        val autoRevokePermissionsPermissionStatus = getStatus(NeededPermission.MANAGE_AUTO_REVOKE, !isAutoRevoke)

        updateUiModel(
            alarmPermissionStatus = alarmPermissionStatus,
            notificationPermissionStatus = notificationPermissionStatus,
            fullscreenPermissionStatus = fullscreenPermissionStatus,
            ignoreBatteryOptimizationPermissionStatus = ignoreBatteryOptimizationPermissionStatus,
            autoRevokePermissionsPermissionStatus = autoRevokePermissionsPermissionStatus
        )
    }

    private fun updateUiModel(
        alarmPermissionStatus: PermissionStatus,
        notificationPermissionStatus: PermissionStatus,
        fullscreenPermissionStatus: PermissionStatus,
        ignoreBatteryOptimizationPermissionStatus: PermissionStatus,
        autoRevokePermissionsPermissionStatus: PermissionStatus
    ) {
        _uiState.update {
            PermissionsUiModel.create(
                alarmPermissionStatus,
                notificationPermissionStatus,
                fullscreenPermissionStatus,
                ignoreBatteryOptimizationPermissionStatus,
                autoRevokePermissionsPermissionStatus,
                resources = systemRegistry.appContext.resources
            )
        }
    }

    companion object {
        class Factory(private val registry: SystemRegistry) : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                return PermissionsViewModel(registry) as T
            }
        }
    }
}