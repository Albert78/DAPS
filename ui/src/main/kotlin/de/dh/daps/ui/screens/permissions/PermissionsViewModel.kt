package de.dh.daps.ui.screens.permissions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import de.dh.daps.core.SystemRegistry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * This view model is used to show the state of needed and granted permissions to the user.
 * It can be used from multiple activities, since the information about granted permissions is needed
 * on several places in the app (permissions screen, notification messages about missing permissions
 * in other screens).
 */
class PermissionsViewModel(
    private val systemRegistry: SystemRegistry
) : ViewModel() {
    private val _uiState = MutableStateFlow(PermissionsUiModel.loading())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            systemRegistry.permissionRepository.permissionSummary.collect {
                refreshUiState()
            }
        }
    }

    /**
     * Triggers a refresh of the system permissions in [de.dh.daps.core.repository.PermissionRepository].
     */
    fun updateAppPermissions() {
        systemRegistry.permissionRepository.refreshPermissions()
        systemRegistry.pluginManager.triggerUpdatesAfterPermissionsChange()
    }

    private fun refreshUiState() {
        val permSummary = systemRegistry.permissionRepository.permissionSummary.value

        val activeFunctions = RestrictedAppFunctions.getActiveAppFunctions()
        val allNeededPermissions = activeFunctions.flatMap { it.neededPermissions }.toSet()

        fun getStatus(permission: NeededPermission, isGranted: Boolean): PermissionStatus {
            return if (allNeededPermissions.contains(permission)) {
                if (isGranted) PermissionStatus.Granted else PermissionStatus.Denied
            } else {
                PermissionStatus.NotNeeded
            }
        }

        val alarmPermissionStatus = getStatus(NeededPermission.SCHEDULE_EXACT_ALARMS, permSummary.canScheduleExactAlarms)
        val notificationPermissionStatus = getStatus(NeededPermission.POST_NOTIFICATIONS, permSummary.canPostNotifications)
        val fullscreenPermissionStatus = getStatus(NeededPermission.SHOW_FULLSCREEN_ACTIVITY, permSummary.canShowFullscreenActivity)
        val ignoreBatteryOptimizationPermissionStatus = getStatus(NeededPermission.IGNORE_BATTERY_OPTIMIZATIONS, permSummary.isIgnoringBatteryOptimizations)
        val autoRevokePermissionsPermissionStatus = getStatus(NeededPermission.MANAGE_AUTO_REVOKE, permSummary.isAutoRevokeExempted)

        val pluginPermissions = permSummary.pluginPermissions.map { status ->
            PluginPermissionUiModel(
                pluginId = status.pluginId,
                pluginDisplayName = status.pluginDisplayName,
                permissionString = status.permissionString,
                status = if (status.isGranted) PermissionStatus.Granted else PermissionStatus.Denied
            )
        }

        updateUiModel(
            alarmPermissionStatus = alarmPermissionStatus,
            notificationPermissionStatus = notificationPermissionStatus,
            fullscreenPermissionStatus = fullscreenPermissionStatus,
            ignoreBatteryOptimizationPermissionStatus = ignoreBatteryOptimizationPermissionStatus,
            autoRevokePermissionsPermissionStatus = autoRevokePermissionsPermissionStatus,
            pluginPermissions = pluginPermissions
        )
    }

    private fun updateUiModel(
        alarmPermissionStatus: PermissionStatus,
        notificationPermissionStatus: PermissionStatus,
        fullscreenPermissionStatus: PermissionStatus,
        ignoreBatteryOptimizationPermissionStatus: PermissionStatus,
        autoRevokePermissionsPermissionStatus: PermissionStatus,
        pluginPermissions: List<PluginPermissionUiModel>
    ) {
        _uiState.update {
            PermissionsUiModel.create(
                alarmPermissionStatus,
                notificationPermissionStatus,
                fullscreenPermissionStatus,
                ignoreBatteryOptimizationPermissionStatus,
                autoRevokePermissionsPermissionStatus,
                pluginPermissions = pluginPermissions,
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