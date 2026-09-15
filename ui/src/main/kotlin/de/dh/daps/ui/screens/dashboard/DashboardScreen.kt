package de.dh.raaps.ui.screens.dashboard

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.ui.text.font.FontWeight
import de.dh.raaps.common.model.data.AlarmSeverity
import de.dh.raaps.common.model.data.Timestamp
import de.dh.raaps.ui.screens.alarm.getAlarmTypeTitle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.dh.raaps.common.model.ApsMode
import de.dh.raaps.common.model.InsulinAmount
import de.dh.raaps.common.model.data.AlarmType
import de.dh.raaps.common.model.data.BgDelta
import de.dh.raaps.common.model.data.BgValue
import de.dh.raaps.common.model.data.GlucoseUnit
import de.dh.raaps.common.model.data.Minutes
import de.dh.raaps.core.aps.ApsRecommendation
import de.dh.raaps.ui.R
import de.dh.raaps.ui.common.LocalGlucoseUnit
import de.dh.raaps.ui.common.composables.ExpandableInfoCard
import de.dh.raaps.ui.common.composables.PrimaryButton
import de.dh.raaps.ui.common.composables.WarningBanner
import de.dh.raaps.ui.common.composables.screenTitle
import de.dh.raaps.ui.common.icons.Icon_Menu_Permissions
import de.dh.raaps.ui.common.icons.Icon_Settings
import de.dh.raaps.ui.common.theme.AppTheme
import de.dh.raaps.ui.controls.apscontrol.ApsControlCard
import de.dh.raaps.ui.controls.history.HistoryAndImpactChartOrDefault
import de.dh.raaps.ui.controls.history.HistoryAndImpactDiagramData
import de.dh.raaps.ui.controls.history.HistoryUiState
import de.dh.raaps.ui.controls.history.HistoryViewModel
import de.dh.raaps.ui.controls.history.rememberBgHistoryChartState
import de.dh.raaps.ui.controls.state.CurrentBgUiState
import de.dh.raaps.ui.controls.state.CurrentStateView
import de.dh.raaps.ui.controls.state.SystemViewModel
import de.dh.raaps.ui.controls.state.createSampleGoodBgUiState
import de.dh.raaps.ui.screens.history.createSampleHistoryUiState
import de.dh.raaps.ui.screens.permissions.PermissionStatus
import de.dh.raaps.ui.screens.permissions.PermissionsUiModel
import de.dh.raaps.ui.screens.permissions.PermissionsViewModel
import de.dh.raaps.ui.screens.therapy.ActiveTherapyStatusUiState
import de.dh.raaps.ui.screens.therapy.CurrentTherapyUiState
import de.dh.raaps.ui.screens.therapy.CurrentTherapyViewModel
import de.dh.raaps.ui.screens.therapy.InsulinProfileUiState
import de.dh.raaps.ui.screens.therapy.TherapyAdjustmentUiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import de.dh.raaps.common.R as CommonR

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    systemViewModel: SystemViewModel,
    historyViewModel: HistoryViewModel,
    currentTherapyViewModel: CurrentTherapyViewModel,
    permissionsViewModel: PermissionsViewModel,
    onFixPermissions: () -> Unit,
    onNavigateToPermissions: () -> Unit,
    onNavigateToPreferences: () -> Unit,
    onNavigateToTherapySettings: () -> Unit,
    onNavigateToMealCorrectionBolus: () -> Unit,
    onNavigateToSystemControl: () -> Unit,
    onAdjustmentClick: () -> Unit,
    onHistoryChartClick: () -> Unit,
    extraContent: @Composable () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val currentBgUiState by systemViewModel.currentBgUiState.collectAsState()
    val historyUiState by historyViewModel.historyUiState.collectAsState()
    val iob by systemViewModel.iob.collectAsState()
    val cob by systemViewModel.cob.collectAsState()
    val currentTherapyUiState by currentTherapyViewModel.uiState.collectAsState()
    val permissionsUiState by permissionsViewModel.uiState.collectAsState()

    DashboardContent(
        dashboardUiState = uiState,
        currentBgUiState = currentBgUiState,
        historyUiState = historyUiState,
        iob = iob,
        cob = cob,
        currentTherapyUiState = currentTherapyUiState,
        permissionsUiState = permissionsUiState,
        onFixPermissionsClick = onFixPermissions,
        onNavigateToPermissions = onNavigateToPermissions,
        onNavigateToPreferences = onNavigateToPreferences,
        onNavigateToTherapySettings = onNavigateToTherapySettings,
        onNavigateToMealCorrectionBolus = onNavigateToMealCorrectionBolus,
        isMealCorrectionBolusAllowed = uiState.isMealCorrectionBolusAllowed,
        onNavigateToSystemControl = onNavigateToSystemControl,
        onHistoryChartClick = onHistoryChartClick,
        onApsModeSelect = { viewModel.setApsMode(it) },
        onAdjustmentClick = onAdjustmentClick,
        onSnoozeAlarm = { type, min -> viewModel.snoozeAlarm(type, min) },
        onCancelSnooze = { type -> viewModel.cancelSnooze(type) },
        extraContent = extraContent
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardContent(
    dashboardUiState: DashboardUiState,
    currentBgUiState: CurrentBgUiState,
    historyUiState: HistoryUiState,
    iob: InsulinAmount,
    cob: Double,
    currentTherapyUiState: CurrentTherapyUiState,
    permissionsUiState: PermissionsUiModel,
    onFixPermissionsClick: () -> Unit,
    onNavigateToPermissions: () -> Unit,
    onNavigateToPreferences: () -> Unit,
    onNavigateToTherapySettings: () -> Unit,
    onNavigateToMealCorrectionBolus: () -> Unit,
    isMealCorrectionBolusAllowed: Boolean,
    onNavigateToSystemControl: () -> Unit,
    onHistoryChartClick: (() -> Unit)?,
    onApsModeSelect: (ApsMode) -> Unit,
    onAdjustmentClick: () -> Unit,
    onSnoozeAlarm: (AlarmType, Int) -> Unit = { _, _ -> },
    onCancelSnooze: (AlarmType) -> Unit = {},
    extraContent: @Composable () -> Unit = {}
) {
    var menuExpanded by remember { mutableStateOf(false) }

    var carbsVisible by remember { mutableStateOf(true) }
    var insulinVisible by remember { mutableStateOf(true) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = screenTitle(
                    text = stringResource(id = R.string.dashboard_screen_title),
                    iconPainter = painterResource(id = R.drawable.ic_app_logo)
                ),
                navigationIcon = {
                    Spacer(modifier = Modifier.width(48.dp))
                },
                actions = {
                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(imageVector = Icons.Default.MoreVert, contentDescription = stringResource(id = CommonR.string.cd_more_options))
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(id = R.string.menu_item_permissions_label)) },
                                leadingIcon = { Icon(imageVector = Icon_Menu_Permissions, contentDescription = null) },
                                onClick = {
                                    menuExpanded = false
                                    onNavigateToPermissions()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(id = R.string.menu_item_preferences_label)) },
                                leadingIcon = { Icon(imageVector = Icon_Settings, contentDescription = null) },
                                onClick = {
                                    menuExpanded = false
                                    onNavigateToPreferences()
                                }
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Permissions warning header
            if (!permissionsUiState.isPermissionsConfigComplete) {
                WarningBanner(
                    warningText = stringResource(id = R.string.dashboard_permissions_missing),
                    actionText = stringResource(id = R.string.dashboard_fix_permissions_link),
                    onActionClick = onFixPermissionsClick
                )
            }

            // Active firing alarm banner
            dashboardUiState.activeFiringAlarm?.let { firingAlarm ->
                ActiveFiringAlarmBanner(
                    alarmType = firingAlarm,
                    onSnooze = { minutes -> onSnoozeAlarm(firingAlarm, minutes) }
                )
            }

            // Snoozed alarms banners
            dashboardUiState.snoozedAlarms.forEach { (alarmType, snoozeState) ->
                SnoozedAlarmBanner(
                    alarmType = alarmType,
                    snoozedUntil = snoozeState.snoozedUntil,
                    onCancelSnooze = { onCancelSnooze(alarmType) }
                )
            }

            CurrentStateView(
                currentBgUiState = currentBgUiState,
                iob = iob,
                cob = cob,
                modifier = Modifier.fillMaxWidth(),
                carbsVisible = carbsVisible,
                onCarbsToggle = { carbsVisible = it },
                insulinVisible = insulinVisible,
                onInsulinToggle = { insulinVisible = it },
                onSystemClick = onNavigateToSystemControl
            )

            dashboardUiState.recommendations.forEach { recommendation ->
                Spacer(modifier = Modifier.height(8.dp))
                val (infoText, detailText) = when (recommendation) {
                    is ApsRecommendation.Carbs -> {
                        stringResource(R.string.recommendation_carbs_info_title, recommendation.amountInGram) to
                                stringResource(R.string.recommendation_carbs_info_text, recommendation.amountInGram)
                    }

                    is ApsRecommendation.Bolus -> {
                        stringResource(R.string.recommendation_bolus_info_title, recommendation.amount.iu) to
                                stringResource(R.string.recommendation_bolus_info_text, recommendation.amount.iu)
                    }
                }
                ExpandableInfoCard(
                    infoText = infoText,
                    detailText = detailText,
                    initiallyExpanded = false,
                    expandable = true
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.dashboard_history_title),
                style = MaterialTheme.typography.bodyLarge
            )

            Box(
                modifier = Modifier.fillMaxWidth()
            ) {
                val chartState = rememberBgHistoryChartState()
                val glucoseUnit = LocalGlucoseUnit.current
                if (historyUiState.isLoading || currentTherapyUiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else {
                    HistoryAndImpactChartOrDefault(
                        diagramData = HistoryAndImpactDiagramData.create(
                            readings = historyUiState.readings,
                            glucoseUnit = glucoseUnit,
                            insulinApplications = historyUiState.insulinApplications,
                            meals = historyUiState.meals,
                            dia = currentTherapyUiState.activeTherapyStatus.profile.dia,
                            peak = currentTherapyUiState.activeTherapyStatus.profile.peak
                        ),
                        state = chartState,
                        onChartClick = onHistoryChartClick,
                        showCarbs = carbsVisible,
                        showInsulin = insulinVisible
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            ApsControlCard(
                modifier = Modifier.fillMaxWidth(),
                activeTherapyStatus = currentTherapyUiState.activeTherapyStatus,
                selectedMode = dashboardUiState.apsMode,
                availableModes = dashboardUiState.availableApsModes,
                onModeChange = onApsModeSelect,
                onAdjustmentClick = onAdjustmentClick,
                onProfileClick = onNavigateToTherapySettings
            )

            Spacer(modifier = Modifier.height(16.dp))

            PrimaryButton(
                onClick = onNavigateToMealCorrectionBolus,
                enabled = isMealCorrectionBolusAllowed,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(R.string.dashboard_meal_correction_bolus_button),
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Spacer(modifier = Modifier.height(40.dp))

            extraContent()
        }
    }
}

@Preview(showBackground = true, name = "Light Mode")
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, name = "Dark Mode")
@Composable
fun DashboardPreview() {
    AppTheme {
        CompositionLocalProvider(LocalGlucoseUnit provides GlucoseUnit.MG_DL) {
            DashboardContent(
                dashboardUiState = DashboardUiState(isLoading = false, isError = false),
                currentBgUiState = createSampleGoodBgUiState(),
                historyUiState = createSampleHistoryUiState(),
                iob = InsulinAmount(1.57),
                cob = 12.0,
                currentTherapyUiState = CurrentTherapyUiState(
                    activeTherapyStatus = ActiveTherapyStatusUiState(
                        profile = InsulinProfileUiState(
                            name = "Normal",
                            activeProfileId = null,
                            isfRange = "50",
                            crRange = "10.0",
                            basalRange = "0.50",
                            dia = Minutes(300),
                            peak = Minutes(75)
                        ),
                        adjustment = TherapyAdjustmentUiState(
                            percentage = 0,
                            targetBgOverride = null,
                            lowThresholdOverride = null,
                            adjustmentHint = null
                        ),
                        currentIsf = BgDelta.fromMgDl(50),
                        currentCr = 10.0,
                        currentBasal = InsulinAmount(0.5),
                        target = BgValue.fromMgDl(110),
                        lowThreshold = BgValue.fromMgDl(70),
                        baseTarget = BgValue.fromMgDl(110),
                        baseLow = BgValue.fromMgDl(70)
                    )
                ),
                permissionsUiState = PermissionsUiModel(
                    isLoading = false,
                    alarmPermissionStatus = PermissionStatus.Granted,
                    notificationPermissionStatus = PermissionStatus.Granted,
                    fullscreenPermissionStatus = PermissionStatus.Granted,
                    ignoreBatteryOptimizationPermissionStatus = PermissionStatus.Granted,
                    autoRevokePermissionsPermissionStatus = PermissionStatus.Granted,
                    numPermissionsMissing = 0,
                    permissionsMissingText = ""
                ),
                onFixPermissionsClick = {},
                onNavigateToPermissions = {},
                onNavigateToPreferences = {},
                onNavigateToTherapySettings = {},
                onNavigateToSystemControl = {},
                onHistoryChartClick = {},
                onApsModeSelect = {},
                onAdjustmentClick = {},
                onNavigateToMealCorrectionBolus = {},
                isMealCorrectionBolusAllowed = true
            )
        }
    }
}

@Preview(showBackground = true, name = "Light Mode")
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, name = "Dark Mode")
@Composable
fun DashboardPermissionsWarningPreview() {
    AppTheme {
        CompositionLocalProvider(LocalGlucoseUnit provides GlucoseUnit.MG_DL) {
            DashboardContent(
                dashboardUiState = DashboardUiState(isLoading = false, isError = false),
                currentBgUiState = createSampleGoodBgUiState(),
                historyUiState = createSampleHistoryUiState(),
                iob = InsulinAmount(1.57),
                cob = 12.0,
                currentTherapyUiState = CurrentTherapyUiState(
                    activeTherapyStatus = ActiveTherapyStatusUiState(
                        profile = InsulinProfileUiState(
                            name = "Normal",
                            activeProfileId = null,
                            isfRange = "50",
                            crRange = "10.0",
                            basalRange = "0.50",
                            dia = Minutes(300),
                            peak = Minutes(75)
                        ),
                        adjustment = TherapyAdjustmentUiState(
                            percentage = 0,
                            targetBgOverride = null,
                            lowThresholdOverride = null,
                            adjustmentHint = null
                        ),
                        currentIsf = BgDelta.fromMgDl(50),
                        currentCr = 10.0,
                        currentBasal = InsulinAmount(0.5),
                        target = BgValue.fromMgDl(110),
                        lowThreshold = BgValue.fromMgDl(70),
                        baseTarget = BgValue.fromMgDl(110),
                        baseLow = BgValue.fromMgDl(70)
                    )
                ),
                permissionsUiState = PermissionsUiModel(
                    isLoading = false,
                    alarmPermissionStatus = PermissionStatus.Granted,
                    notificationPermissionStatus = PermissionStatus.Denied,
                    fullscreenPermissionStatus = PermissionStatus.Granted,
                    ignoreBatteryOptimizationPermissionStatus = PermissionStatus.Granted,
                    autoRevokePermissionsPermissionStatus = PermissionStatus.Granted,
                    numPermissionsMissing = 1,
                    permissionsMissingText = "1 permission missing"
                ),
                onFixPermissionsClick = {},
                onNavigateToPermissions = {},
                onNavigateToPreferences = {},
                onNavigateToTherapySettings = {},
                onNavigateToSystemControl = {},
                onHistoryChartClick = {},
                onApsModeSelect = {},
                onAdjustmentClick = {},
                onNavigateToMealCorrectionBolus = {},
                isMealCorrectionBolusAllowed = true
            )
        }
    }
}

@Composable
fun ActiveFiringAlarmBanner(
    alarmType: AlarmType,
    onSnooze: (Int) -> Unit
) {
    val isCritical = alarmType.defaultSeverity == AlarmSeverity.CRITICAL
    val containerColor = if (isCritical) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.tertiaryContainer
    val contentColor = if (isCritical) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onTertiaryContainer

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = getAlarmTypeTitle(alarmType),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = contentColor
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(onClick = { onSnooze(15) }) {
                    Text(stringResource(R.string.alarm_action_snooze_15))
                }
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedButton(onClick = { onSnooze(30) }) {
                    Text(stringResource(R.string.alarm_action_snooze_30))
                }
            }
        }
    }
}

@Composable
fun SnoozedAlarmBanner(
    alarmType: AlarmType,
    snoozedUntil: Timestamp,
    onCancelSnooze: () -> Unit
) {
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val timeStr = timeFormat.format(Date(snoozedUntil.ms))
    val text = stringResource(
        R.string.alarm_snoozed_until_format,
        getAlarmTypeTitle(alarmType),
        timeStr
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "⚠️ $text",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onCancelSnooze) {
                Text(stringResource(R.string.alarm_action_unsnooze))
            }
        }
    }
}