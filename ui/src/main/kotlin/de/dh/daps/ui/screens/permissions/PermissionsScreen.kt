package de.dh.daps.ui.screens.permissions

import android.Manifest
import android.content.res.Configuration
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.dh.daps.ui.R
import de.dh.daps.ui.common.composables.contentScrollIndicator
import de.dh.daps.ui.common.composables.screenTitle
import de.dh.daps.ui.common.icons.Icon_Check_No
import de.dh.daps.ui.common.icons.Icon_Check_Yes
import de.dh.daps.ui.common.icons.Icon_Info
import de.dh.daps.ui.common.icons.Icon_Screen_Back
import de.dh.daps.ui.common.theme.AppPreview
import de.dh.daps.common.R as CommonR

@Composable
fun PermissionsScreen(
    viewModel: PermissionsViewModel,
    onNavigateUp: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
    onOpenFullscreenSettings: () -> Unit,
    onOpenBatteryOptimizationSettings: () -> Unit,
    onOpenAutoRevokeSettings: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    val requestPermissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        viewModel.updateAppPermissions()
    }

    PermissionsScreenContent(
        uiModel = uiState,
        onNavigateUp = onNavigateUp,
        onOpenNotificationSettings = onOpenNotificationSettings,
        onOpenFullscreenSettings = onOpenFullscreenSettings,
        onOpenBatteryOptimizationSettings = onOpenBatteryOptimizationSettings,
        onOpenAutoRevokeSettings = onOpenAutoRevokeSettings,
        onRequestPluginPermissions = { permissions ->
            requestPermissionsLauncher.launch(permissions.toTypedArray())
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionsScreenContent(
    uiModel: PermissionsUiModel,
    onNavigateUp: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
    onOpenFullscreenSettings: () -> Unit,
    onOpenBatteryOptimizationSettings: () -> Unit,
    onOpenAutoRevokeSettings: () -> Unit,
    onRequestPluginPermissions: (Collection<String>) -> Unit = {}
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val scrollState = rememberScrollState()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = screenTitle(stringResource(id = R.string.permissions_title)),
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(
                            imageVector = Icon_Screen_Back,
                            contentDescription = stringResource(CommonR.string.cd_navigate_up)
                        )
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .contentScrollIndicator(scrollableState = scrollState)
                    .verticalScroll(scrollState)
            ) {
                // Intro Text
                Text(
                    text = stringResource(id = R.string.permissions_intro_text),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(16.dp)
                )

                if (uiModel.numPermissionsMissing == 0) {
                    Text(
                        text = uiModel.permissionsMissingText,
                        style = MaterialTheme.typography.bodyLarge,
                        color = colorResource(id = CommonR.color.permission_granted_color),
                        modifier = Modifier.padding(16.dp)
                    )
                } else {
                    Text(
                        text = uiModel.permissionsMissingText,
                        style = MaterialTheme.typography.bodyLarge,
                        color = colorResource(id = CommonR.color.permission_not_granted_color),
                        modifier = Modifier.padding(16.dp)
                    )
                }

                HorizontalDivider()

                // Notifications
                PermissionItem(
                    description = stringResource(id = R.string.permission_notifications_desc),
                    grantedText = stringResource(id = R.string.permission_granted_text),
                    notGrantedText = stringResource(id = R.string.permission_not_granted_text),
                    status = uiModel.notificationPermissionStatus,
                    onClick = onOpenNotificationSettings
                )

                HorizontalDivider()

                // Fullscreen Intent
                PermissionItem(
                    description = stringResource(id = R.string.permission_can_show_fullscreen_activity_desc),
                    grantedText = stringResource(id = R.string.permission_granted_text),
                    notGrantedText = stringResource(id = R.string.permission_not_granted_text),
                    status = uiModel.fullscreenPermissionStatus,
                    onClick = onOpenFullscreenSettings
                )

                HorizontalDivider()

                // Battery Optimization
                PermissionItem(
                    description = stringResource(id = R.string.ignore_battery_optimization_permissions_desc),
                    grantedText = stringResource(id = R.string.ignore_battery_optimization_permissions_off_label),
                    notGrantedText = stringResource(id = R.string.ignore_battery_optimization_permissions_label),
                    status = uiModel.ignoreBatteryOptimizationPermissionStatus,
                    onClick = onOpenBatteryOptimizationSettings
                )

                HorizontalDivider()

                // Auto Revoke
                PermissionItem(
                    description = stringResource(id = R.string.auto_revokePermissions_desc),
                    grantedText = stringResource(id = R.string.auto_revoke_permissions_off_label),
                    notGrantedText = stringResource(id = R.string.auto_revoke_permissions_label),
                    status = uiModel.autoRevokePermissionsPermissionStatus,
                    onClick = onOpenAutoRevokeSettings
                )

                if (uiModel.pluginPermissions.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(id = R.string.plugin_permissions_title),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                    Text(
                        text = stringResource(id = R.string.plugin_permissions_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )

                    HorizontalDivider()

                    val groupedByPlugin = uiModel.pluginPermissions.groupBy { it.pluginId }

                    groupedByPlugin.forEach { (pluginId, permissions) ->
                        val pluginName = permissions.firstOrNull()?.pluginDisplayName?.asString() ?: pluginId

                        Column(modifier = Modifier.padding(vertical = 8.dp)) {
                            Text(
                                text = pluginName,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                            )

                            permissions.forEach { perm ->
                                val permDescription = formatPermissionDescription(perm.permissionString)
                                PermissionItem(
                                    description = permDescription,
                                    grantedText = stringResource(id = R.string.permission_granted_text),
                                    notGrantedText = stringResource(id = R.string.permission_not_granted_text),
                                    status = perm.status,
                                    onClick = {
                                        if (perm.status is PermissionStatus.Denied) {
                                            onRequestPluginPermissions(listOf(perm.permissionString))
                                        }
                                    }
                                )
                            }
                        }
                        HorizontalDivider()
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                ) {
                    Row(
                        modifier = Modifier
                            .padding(start = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icon_Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface
                        )

                        Text(
                            text = stringResource(id = R.string.boot_receiver_desc),
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier
                                .padding(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun formatPermissionDescription(permissionString: String): String {
    return when (permissionString) {
        Manifest.permission.BLUETOOTH_SCAN -> stringResource(id = R.string.permission_bluetooth_scan_desc)
        Manifest.permission.BLUETOOTH_CONNECT -> stringResource(id = R.string.permission_bluetooth_connect_desc)
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION -> stringResource(id = R.string.permission_location_desc)
        else -> permissionString.substringAfterLast(".")
    }
}

@Composable
fun PermissionItem(
    description: String,
    grantedText: String,
    notGrantedText: String,
    status: PermissionStatus,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        when (status) {
            is PermissionStatus.Granted -> {
                Icon(
                    imageVector = Icon_Check_Yes,
                    tint = colorResource(CommonR.color.permission_granted_color),
                    contentDescription = stringResource(R.string.cd_permission_granted),
                    modifier = Modifier
                        .size(50.dp)
                        .padding(end = 10.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = description, style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = grantedText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colorResource(CommonR.color.permission_granted_color)
                    )
                }
            }
            is PermissionStatus.Denied -> {
                Icon(
                    imageVector = Icon_Check_No,
                    tint = colorResource(CommonR.color.permission_not_granted_color),
                    contentDescription = stringResource(R.string.cd_permission_not_granted),
                    modifier = Modifier
                        .size(50.dp)
                        .padding(end = 10.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = description, style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = notGrantedText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colorResource(CommonR.color.permission_not_granted_color)
                    )
                }
            }
            is PermissionStatus.NotNeeded -> {
                Icon(
                    imageVector = Icon_Check_Yes,
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), // Dark gray
                    contentDescription = stringResource(R.string.cd_permission_not_needed),
                    modifier = Modifier
                        .size(50.dp)
                        .padding(end = 10.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = description,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f) // Dunkelgrau
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.permission_not_needed),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f) // Dunkelgrau
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "Light mode", heightDp = 900)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, name = "Dark Mode", heightDp = 900)
@Composable
fun PermissionsScreenPreview() {
    AppPreview {
        PermissionsScreenContent(
            uiModel = PermissionsUiModel.create(
                alarmPermissionStatus = PermissionStatus.create(isGranted = true, isNeeded = true),
                notificationPermissionStatus = PermissionStatus.create(isGranted = false, isNeeded = true),
                fullscreenPermissionStatus = PermissionStatus.create(isGranted = false, isNeeded = true),
                ignoreBatteryOptimizationPermissionStatus = PermissionStatus.create(isGranted = true, isNeeded = true),
                autoRevokePermissionsPermissionStatus = PermissionStatus.create(isGranted = true, isNeeded = true),
                resources = LocalResources.current
            ),
            onNavigateUp = {},
            onOpenNotificationSettings = {},
            onOpenFullscreenSettings = {},
            onOpenBatteryOptimizationSettings = {},
            onOpenAutoRevokeSettings = {}
        )
    }
}

@Composable
fun PermissionsItemPreview(status: PermissionStatus) {
    AppPreview {
        PermissionItem(
            description = "Dies ist die Beschreibung der Berechtigung",
            grantedText = "Berechtigung erteilt",
            notGrantedText = "Berechtigung nicht freigegeben",
            status = status,
            onClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PermissionsItemGrantedPreview() {
    PermissionsItemPreview(PermissionStatus.create(isGranted = true, isNeeded = true))
}

@Preview(showBackground = true)
@Composable
fun PermissionsItemNotGrantedPreview() {
    PermissionsItemPreview(PermissionStatus.create(isGranted = false, isNeeded = true))
}

@Preview(showBackground = true)
@Composable
fun PermissionsItemNotNeededPreview() {
    PermissionsItemPreview(PermissionStatus.create(isGranted = false, isNeeded = false))
}