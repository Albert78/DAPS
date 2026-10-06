package de.dh.daps.ui.screens.permissions

import android.content.res.Resources
import de.dh.daps.common.ui.UiText
import de.dh.daps.ui.R
import de.dh.daps.ui.common.DisplayTextUtils

sealed class PermissionStatus {
    /**
     * The permission is needed and granted for this app.
     */
    object Granted : PermissionStatus()

    /**
     * The permission is currently not necessary for this app (e.g. calendar sync not configured).
     */
    object NotNeeded : PermissionStatus()

    /**
     * The permission is necessary but not granted for this app.
     */
    object Denied : PermissionStatus()

    /**
     * Returns if the state of this permission is ok, i.e. granted or not needed.
     */
    fun isSatisfied(): Boolean = this is Granted || this is NotNeeded

    companion object {
        fun create(
            isGranted: Boolean,
            isNeeded: Boolean
        ): PermissionStatus = when {
            isGranted -> Granted
            isNeeded -> NotNeeded
            else -> Denied
        }
    }
}

data class PluginPermissionUiModel(
    val pluginId: String,
    val pluginDisplayName: UiText,
    val permissionString: String,
    val status: PermissionStatus
)

data class PermissionsUiModel(
    val isLoading: Boolean,
    val alarmPermissionStatus: PermissionStatus,
    val notificationPermissionStatus: PermissionStatus,
    val fullscreenPermissionStatus: PermissionStatus,
    val ignoreBatteryOptimizationPermissionStatus: PermissionStatus,
    val autoRevokePermissionsPermissionStatus: PermissionStatus,
    val pluginPermissions: List<PluginPermissionUiModel> = emptyList(),
    val numPermissionsMissing: Int,
    val permissionsMissingText: String
) {
    val isPermissionsConfigComplete: Boolean = numPermissionsMissing == 0

    companion object {
        fun create(
            alarmPermissionStatus: PermissionStatus,
            notificationPermissionStatus: PermissionStatus,
            fullscreenPermissionStatus: PermissionStatus,
            ignoreBatteryOptimizationPermissionStatus: PermissionStatus,
            autoRevokePermissionsPermissionStatus: PermissionStatus,
            pluginPermissions: List<PluginPermissionUiModel> = emptyList(),
            resources: Resources
        ): PermissionsUiModel {
            val numMissing = getNumPermissionsMissing(
                alarmPermissionStatus,
                notificationPermissionStatus,
                fullscreenPermissionStatus,
                ignoreBatteryOptimizationPermissionStatus,
                autoRevokePermissionsPermissionStatus,
                pluginPermissions
            )

            val permissionsText = DisplayTextUtils.getQuantityStringZero(
                resources,
                R.plurals.permissions_activity_start_permissions_missing,
                R.string.permissions_activity_start_all_permissions_granted,
                numMissing,
                numMissing
            )

            return PermissionsUiModel(
                isLoading = false,
                alarmPermissionStatus = alarmPermissionStatus,
                notificationPermissionStatus = notificationPermissionStatus,
                fullscreenPermissionStatus = fullscreenPermissionStatus,
                ignoreBatteryOptimizationPermissionStatus = ignoreBatteryOptimizationPermissionStatus,
                autoRevokePermissionsPermissionStatus = autoRevokePermissionsPermissionStatus,
                pluginPermissions = pluginPermissions,
                numPermissionsMissing = numMissing,
                permissionsMissingText = permissionsText
            )
        }

        fun loading(): PermissionsUiModel {
            return PermissionsUiModel(
                isLoading = true,
                alarmPermissionStatus = PermissionStatus.NotNeeded,
                notificationPermissionStatus = PermissionStatus.NotNeeded,
                fullscreenPermissionStatus = PermissionStatus.NotNeeded,
                ignoreBatteryOptimizationPermissionStatus = PermissionStatus.NotNeeded,
                autoRevokePermissionsPermissionStatus = PermissionStatus.NotNeeded,
                pluginPermissions = emptyList(),
                numPermissionsMissing = 0,
                permissionsMissingText = ""
            )
        }

        fun allMissing(resources: Resources): PermissionsUiModel {
            return PermissionsUiModel.Companion.create(
                alarmPermissionStatus = PermissionStatus.Denied,
                notificationPermissionStatus = PermissionStatus.Denied,
                fullscreenPermissionStatus = PermissionStatus.Denied,
                ignoreBatteryOptimizationPermissionStatus = PermissionStatus.Denied,
                autoRevokePermissionsPermissionStatus = PermissionStatus.Denied,
                pluginPermissions = emptyList(),
                resources = resources
            )
        }

        fun allGranted(resources: Resources): PermissionsUiModel {
            return create(
                alarmPermissionStatus = PermissionStatus.Granted,
                notificationPermissionStatus = PermissionStatus.Granted,
                fullscreenPermissionStatus = PermissionStatus.Granted,
                ignoreBatteryOptimizationPermissionStatus = PermissionStatus.Granted,
                autoRevokePermissionsPermissionStatus = PermissionStatus.Granted,
                pluginPermissions = emptyList(),
                resources = resources
            )
        }

        private fun getNumPermissionsMissing(
            alarmPermissionStatus: PermissionStatus,
            notificationPermissionStatus: PermissionStatus,
            fullscreenPermissionStatus: PermissionStatus,
            ignoreBatteryOptimizationPermissionStatus: PermissionStatus,
            autoRevokePermissionsPermissionStatus: PermissionStatus,
            pluginPermissions: List<PluginPermissionUiModel> = emptyList()
        ): Int {
            val permissions = listOf(
                alarmPermissionStatus,
                notificationPermissionStatus,
                fullscreenPermissionStatus,
                ignoreBatteryOptimizationPermissionStatus,
                autoRevokePermissionsPermissionStatus
            )
            val missingCore = permissions.count { !it.isSatisfied() }
            val missingPlugins = pluginPermissions.count { !it.status.isSatisfied() }
            return missingCore + missingPlugins
        }
    }
}