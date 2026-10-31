package de.dh.daps.ui.navigation

import android.Manifest
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import de.dh.daps.common.ID_UNDEFINED
import de.dh.daps.common.domain.model.therapy.getDefaultInsulinTypes
import de.dh.daps.common.navigation.AlertProfileEditorRoute
import de.dh.daps.common.navigation.AlertProfilesRoute
import de.dh.daps.common.navigation.AppDataRoute
import de.dh.daps.common.navigation.BgEditorRoute
import de.dh.daps.common.navigation.InsulinHistoryRoute
import de.dh.daps.common.navigation.RemindersRoute
import de.dh.daps.common.navigation.ReminderEditorRoute
import de.dh.daps.common.navigation.CoreDecisionsRoute
import de.dh.daps.common.navigation.CurrentTherapySettingsRoute
import de.dh.daps.common.navigation.DashboardRoute
import de.dh.daps.common.navigation.FeatureNavGraph
import de.dh.daps.common.navigation.FoodDatabaseRoute
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.dh.daps.common.navigation.GlucoseSourceSettingsRoute
import de.dh.daps.common.navigation.GlucoseSourceSetupRoute
import de.dh.daps.common.navigation.HistoricalMealRoute
import de.dh.daps.common.navigation.HistoryRoute
import de.dh.daps.common.navigation.InsulinProfileEditorRoute
import de.dh.daps.common.navigation.InsulinProfilesRoute
import de.dh.daps.common.navigation.InsulinTypeEditorRoute
import de.dh.daps.common.navigation.InsulinTypesRoute
import de.dh.daps.common.navigation.ManualControlRoute
import de.dh.daps.common.navigation.MasterDataRoute
import de.dh.daps.common.navigation.MealCorrectionBolusRoute
import de.dh.daps.common.navigation.MealTypeEditorRoute
import de.dh.daps.common.navigation.MealTypesRoute
import de.dh.daps.common.navigation.MealsRoute
import de.dh.daps.common.navigation.NavigationViewModel
import de.dh.daps.common.navigation.PermissionsRoute
import de.dh.daps.common.navigation.PreferencesMainRoute
import de.dh.daps.common.navigation.PumpSettingsRoute
import de.dh.daps.common.navigation.PumpSetupRoute
import de.dh.daps.common.navigation.ScheduledTherapyAdjustmentRoute
import de.dh.daps.common.navigation.SystemControlRoute
import de.dh.daps.ui.screens.systemcontrol.GlucoseSourceSettingsUiState
import de.dh.daps.ui.screens.systemcontrol.GlucoseSourceSettingsViewModel
import de.dh.daps.ui.screens.systemcontrol.PumpSettingsUiState
import de.dh.daps.ui.screens.systemcontrol.PumpSettingsViewModel
import de.dh.daps.ui.screens.insulinprofile.InsulinProfileEditorScreen
import de.dh.daps.ui.screens.insulinprofile.InsulinProfileEditorViewModel
import de.dh.daps.common.navigation.TherapyAdjustmentEditorRoute
import de.dh.daps.common.navigation.TherapyAdjustmentPresetsRoute
import de.dh.daps.common.navigation.TherapyAdjustmentRoute
import de.dh.daps.core.SystemRegistry
import de.dh.daps.common.repository.setUserDeclinedPermissions
import de.dh.daps.ui.R
import de.dh.daps.ui.common.treatmentlock.TreatmentLockScreen
import de.dh.daps.ui.common.treatmentlock.TreatmentLockViewModel
import de.dh.daps.ui.controls.history.HistoryViewModel
import de.dh.daps.ui.controls.state.SystemViewModel
import de.dh.daps.ui.screens.alertprofiles.AlertProfileEditorScreen
import de.dh.daps.ui.screens.alertprofiles.AlertProfileEditorViewModel
import de.dh.daps.ui.screens.alertprofiles.AlertProfilesScreen
import de.dh.daps.ui.screens.alertprofiles.AlertProfilesViewModel
import de.dh.daps.ui.screens.appdata.AppDataScreen
import de.dh.daps.ui.screens.appdata.AppDataViewModel
import de.dh.daps.ui.screens.insulinhistory.InsulinHistoryScreen
import de.dh.daps.ui.screens.insulinhistory.InsulinHistoryViewModel
import de.dh.daps.ui.screens.dashboard.DashboardScreen
import de.dh.daps.ui.screens.dashboard.DashboardViewModel
import de.dh.daps.ui.screens.fooddatabase.FoodDatabaseScreen
import de.dh.daps.ui.screens.driversetup.GlucoseSourceSetupScreen
import de.dh.daps.ui.screens.driversetup.GlucoseSourceSetupViewModel
import de.dh.daps.ui.screens.history.HistoryScreen
import de.dh.daps.ui.screens.insulinprofile.InsulinProfilesScreen
import de.dh.daps.ui.screens.insulinprofile.InsulinProfilesViewModel
import de.dh.daps.ui.screens.insulintypes.InsulinTypeEditorScreen
import de.dh.daps.ui.screens.insulintypes.InsulinTypeEditorViewModel
import de.dh.daps.ui.screens.insulintypes.InsulinTypesScreen
import de.dh.daps.ui.screens.insulintypes.InsulinTypesViewModel
import de.dh.daps.ui.screens.manualcontrol.ManualControlScreen
import de.dh.daps.ui.screens.manualcontrol.ManualControlViewModel
import de.dh.daps.ui.screens.masterdata.MasterDataScreen
import de.dh.daps.ui.screens.mealcorrectionbolus.MealCorrectionBolusScreen
import de.dh.daps.ui.screens.mealcorrectionbolus.MealCorrectionBolusViewModel
import de.dh.daps.ui.screens.meals.EditHistoricalMealScreen
import de.dh.daps.ui.screens.meals.EditHistoricalMealViewModel
import de.dh.daps.ui.screens.meals.MealsScreen
import de.dh.daps.ui.screens.meals.MealsViewModel
import de.dh.daps.ui.screens.mealtypes.MealTypeEditorScreen
import de.dh.daps.ui.screens.mealtypes.MealTypeEditorViewModel
import de.dh.daps.ui.screens.mealtypes.MealTypesScreen
import de.dh.daps.ui.screens.mealtypes.MealTypesViewModel
import de.dh.daps.ui.screens.permissions.PermissionsScreen
import de.dh.daps.ui.screens.permissions.PermissionsViewModel
import de.dh.daps.ui.screens.permissions.isPermissionsMissing
import de.dh.daps.ui.screens.permissions.openAppUseFullScreenSettings
import de.dh.daps.ui.screens.permissions.openAutoRevokeSettings
import de.dh.daps.ui.screens.permissions.openNotificationSettings
import de.dh.daps.ui.screens.permissions.requestIgnoreBatteryOptimizations
import de.dh.daps.common.navigation.PastRemindersRoute
import de.dh.daps.ui.screens.preferences.PreferencesScreen
import de.dh.daps.ui.screens.preferences.PreferencesViewModel
import de.dh.daps.ui.screens.driversetup.PumpSetupScreen
import de.dh.daps.ui.screens.driversetup.PumpSetupViewModel
import de.dh.daps.ui.screens.reminders.PastRemindersScreen
import de.dh.daps.ui.screens.reminders.PastRemindersViewModel
import de.dh.daps.ui.screens.reminders.RemindersScreen
import de.dh.daps.ui.screens.reminders.RemindersViewModel
import de.dh.daps.ui.screens.reminders.ReminderEditorScreen
import de.dh.daps.ui.screens.reminders.ReminderEditorViewModel
import de.dh.daps.ui.screens.systemcontrol.CoreDecisionsScreen
import de.dh.daps.ui.screens.systemcontrol.CoreDecisionsViewModel
import de.dh.daps.ui.screens.systemcontrol.SystemControlScreen
import de.dh.daps.ui.screens.systemcontrol.SystemControlViewModel
import de.dh.daps.ui.screens.therapy.BgEditorScreen
import de.dh.daps.ui.screens.therapy.BgEditorViewModel
import de.dh.daps.ui.screens.therapy.CurrentTherapySettingsScreen
import de.dh.daps.ui.screens.therapy.CurrentTherapyViewModel
import de.dh.daps.ui.screens.therapy.ScheduledTherapyAdjustmentScreen
import de.dh.daps.ui.screens.therapy.ScheduledTherapyViewModel
import de.dh.daps.ui.screens.therapy.TherapyAdjustmentScreen
import de.dh.daps.ui.screens.therapyadjustments.TherapyAdjustmentEditorScreen
import de.dh.daps.ui.screens.therapyadjustments.TherapyAdjustmentEditorViewModel
import de.dh.daps.ui.screens.therapyadjustments.TherapyAdjustmentsScreen
import de.dh.daps.ui.screens.therapyadjustments.TherapyAdjustmentsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainFeatureNavGraph(
    private val activity: ComponentActivity,
    private val navViewModel: NavigationViewModel,
    private val registry: SystemRegistry,
    private val extraDashboardContent: @Composable () -> Unit = {}
) : FeatureNavGraph {
    override fun getEntry(key: NavKey): NavEntry<NavKey>? {
        return when (key) {
            is DashboardRoute -> NavEntry(key) {
                val vm: DashboardViewModel =
                    viewModel(factory = DashboardViewModel.Companion.Factory(registry))
                val systemVM: SystemViewModel =
                    viewModel(factory = SystemViewModel.Companion.Factory(registry))
                val historyVM: HistoryViewModel =
                    viewModel(factory = HistoryViewModel.Companion.Factory(registry))
                val currentTherapyVM: CurrentTherapyViewModel =
                    viewModel(factory = CurrentTherapyViewModel.Companion.Factory(registry))
                val permissionsViewModel: PermissionsViewModel =
                    viewModel(factory = PermissionsViewModel.Companion.Factory(registry))

                LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
                    vm.reload()
                    permissionsViewModel.updateAppPermissions()
                }

                DashboardScreen(
                    viewModel = vm,
                    systemViewModel = systemVM,
                    historyViewModel = historyVM,
                    currentTherapyViewModel = currentTherapyVM,
                    permissionsViewModel = permissionsViewModel,
                    onFixPermissions = { navViewModel.push(PermissionsRoute) },
                    onNavigateToTherapySettings = { navViewModel.push(CurrentTherapySettingsRoute) },
                    onNavigateToMealCorrectionBolus = { navViewModel.push(MealCorrectionBolusRoute) },
                    onNavigateToSystemControl = { navViewModel.push(SystemControlRoute()) },
                    onNavigateToManualControl = { initialDialog -> navViewModel.push(ManualControlRoute(initialDialog = initialDialog)) },
                    onAdjustmentClick = { navViewModel.push(TherapyAdjustmentRoute) },
                    onHistoryChartClick = { navViewModel.push(HistoryRoute) },
                    onNavigateToGlucoseSourceSetup = { navViewModel.push(GlucoseSourceSetupRoute) },
                    onNavigateToPumpSetup = { navViewModel.push(PumpSetupRoute) },
                    extraContent = extraDashboardContent
                )
            }

            is InsulinProfilesRoute -> NavEntry(key) {
                val vm: InsulinProfilesViewModel = viewModel(
                    factory = InsulinProfilesViewModel.Companion.Factory(registry)
                )

                InsulinProfilesScreen(
                    viewModel = vm,
                    onNavigateUp = { navViewModel.pop() },
                    onNavigateToEditor = { profileId ->
                        navViewModel.push(InsulinProfileEditorRoute(profileId = profileId))
                    },
                    onNavigateToCopy = { profileId, newName ->
                        navViewModel.push(InsulinProfileEditorRoute(profileId = profileId, isCopy = true, newName = newName))
                    }
                )
            }

            is InsulinProfileEditorRoute -> NavEntry(key) {
                val vm: InsulinProfileEditorViewModel = viewModel(
                    factory = InsulinProfileEditorViewModel.Companion.Factory(registry, key.profileId, key.isCopy, key.newName)
                )

                InsulinProfileEditorScreen(
                    viewModel = vm,
                    onNavigateUp = { navViewModel.pop() }
                )
            }

            is CurrentTherapySettingsRoute -> NavEntry(key) {
                val currentTherapyVM: CurrentTherapyViewModel =
                    viewModel(factory = CurrentTherapyViewModel.Companion.Factory(registry))

                CurrentTherapySettingsScreen(
                    viewModel = currentTherapyVM,
                    onNavigateUp = { navViewModel.pop() },
                    onNavigateToInsulinProfileEditor = { navViewModel.push(InsulinProfilesRoute) },
                    onNavigateToBgEditor = { navViewModel.push(BgEditorRoute) },
                    onNavigateToTherapyAdjustment = { navViewModel.push(TherapyAdjustmentRoute) },
                    onNavigateToScheduledTherapyAdjustment = { navViewModel.push(ScheduledTherapyAdjustmentRoute) }
                )
            }

            is BgEditorRoute -> NavEntry(key) {
                val bgEditorVM: BgEditorViewModel =
                    viewModel(factory = BgEditorViewModel.Companion.Factory(registry))

                BgEditorScreen(
                    viewModel = bgEditorVM,
                    onNavigateUp = { navViewModel.pop() }
                )
            }

            is TherapyAdjustmentRoute -> NavEntry(key) {
                val currentTherapyVM: CurrentTherapyViewModel =
                    viewModel(factory = CurrentTherapyViewModel.Companion.Factory(registry))

                TherapyAdjustmentScreen(
                    viewModel = currentTherapyVM,
                    onNavigateUp = { navViewModel.pop() }
                )
            }

            is ScheduledTherapyAdjustmentRoute -> NavEntry(key) {
                val scheduledVM: ScheduledTherapyViewModel =
                    viewModel(factory = ScheduledTherapyViewModel.Companion.Factory(registry))

                ScheduledTherapyAdjustmentScreen(
                    viewModel = scheduledVM,
                    onNavigateUp = { navViewModel.pop() }
                )
            }

            is HistoryRoute -> NavEntry(key) {
                val historyVM: HistoryViewModel =
                    viewModel(factory = HistoryViewModel.Companion.Factory(registry))

                HistoryScreen(
                    historyViewModel = historyVM
                )
            }

            is MealCorrectionBolusRoute -> NavEntry(key) {
                val vm: MealCorrectionBolusViewModel = viewModel(
                    factory = MealCorrectionBolusViewModel.Companion.Factory(
                        registry = registry
                    )
                )
                val lockViewModel: TreatmentLockViewModel = viewModel(
                    factory = TreatmentLockViewModel.Companion.Factory(
                        tag = "MealCorrectionBolusScreen",
                        therapyManager = registry.therapyManager,
                        requirePumpSync = true
                    )
                )

                // Leave the screen and release therapy manager lock when the screen
                // stops (display off, app minimized/home, or switching apps). Using ON_STOP
                // ensures internal dialogs/overlays (which only trigger ON_PAUSE) keep the lock.
                LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
                    lockViewModel.releaseLock()
                    navViewModel.pop()
                }

                DisposableEffect(Unit) {
                    onDispose {
                        lockViewModel.releaseLock()
                    }
                }

                TreatmentLockScreen(
                    viewModel = lockViewModel,
                    title = stringResource(id = R.string.meal_add_screen_title),
                    onNavigateUp = { navViewModel.pop() }
                ) { treatmentLock ->
                    MealCorrectionBolusScreen(
                        viewModel = vm,
                        treatmentLock = treatmentLock,
                        onNavigateUp = { navViewModel.pop() }
                    )
                }
            }

            is HistoricalMealRoute -> NavEntry(key) {
                val vm: EditHistoricalMealViewModel = viewModel(
                    factory = EditHistoricalMealViewModel.Companion.Factory(registry, key.mealId)
                )
                EditHistoricalMealScreen(
                    viewModel = vm,
                    onNavigateUp = { navViewModel.pop() }
                )
            }

            is ManualControlRoute -> NavEntry(key) {
                val vm: ManualControlViewModel = viewModel(
                    factory = ManualControlViewModel.Companion.Factory(registry)
                )

                ManualControlScreen(
                    viewModel = vm,
                    initialDialog = key.initialDialog,
                    onNavigateUp = { navViewModel.pop() },
                    onNavigateToMealCorrectionBolus = {
                        navViewModel.replaceTop(MealCorrectionBolusRoute)
                    },
                    onEditMeal = { mealId ->
                        navViewModel.push(HistoricalMealRoute(mealId = mealId))
                    }
                )
            }

            is PermissionsRoute -> NavEntry(key) {
                val permissionsViewModel: PermissionsViewModel =
                    viewModel(factory = PermissionsViewModel.Companion.Factory(registry))

                permissionsViewModel.updateAppPermissions()

                LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
                    permissionsViewModel.updateAppPermissions()

                    activity.lifecycleScope.launch(Dispatchers.IO) {
                        registry.permissionsChangedHandler.onPermissionsChanged()
                    }
                }

                DisposableEffect(Unit) {
                    onDispose {
                        activity.lifecycleScope.launch(Dispatchers.IO) {
                            val userDeclinedPermissions = isPermissionsMissing(activity)
                            registry.appPreferencesRepository.setUserDeclinedPermissions(userDeclinedPermissions)
                            registry.permissionsChangedHandler.onPermissionsChanged()
                        }
                    }
                }

                val permissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { _ ->
                    permissionsViewModel.updateAppPermissions()
                }

                PermissionsScreen(
                    viewModel = permissionsViewModel,
                    onNavigateUp = { navViewModel.pop() },
                    onOpenNotificationSettings = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            openNotificationSettings(activity)
                        }
                    },
                    onOpenFullscreenSettings = { openAppUseFullScreenSettings(activity) },
                    onOpenBatteryOptimizationSettings = { requestIgnoreBatteryOptimizations(activity) },
                    onOpenAutoRevokeSettings = { openAutoRevokeSettings(activity) }
                )
            }

            is PreferencesMainRoute -> NavEntry(key) {
                val vm: PreferencesViewModel = viewModel(
                    factory = PreferencesViewModel.Companion.Factory(registry)
                )

                LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
                    vm.reload()
                }

                PreferencesScreen(
                    viewModel = vm,
                    onNavigateUp = { navViewModel.pop() }
                )
            }

            is AppDataRoute -> NavEntry(key) {
                val vm: AppDataViewModel = viewModel(
                    factory = AppDataViewModel.Companion.Factory(registry)
                )

                AppDataScreen(
                    viewModel = vm,
                    onNavigateUp = { navViewModel.pop() }
                )
            }

            is MealsRoute -> NavEntry(key) {
                val vm: MealsViewModel = viewModel(
                    factory = MealsViewModel.Companion.Factory(registry)
                )
                MealsScreen(
                    viewModel = vm,
                    onAddMeal = { navViewModel.push(HistoricalMealRoute(mealId = ID_UNDEFINED)) },
                    onEditMeal = { meal -> navViewModel.push(HistoricalMealRoute(mealId = meal.id)) },
                    onNavigateUp = { navViewModel.pop() }
                )
            }

            is MealTypesRoute -> NavEntry(key) {
                val vm: MealTypesViewModel = viewModel(
                    factory = MealTypesViewModel.Companion.Factory(registry)
                )
                MealTypesScreen(
                    viewModel = vm,
                    onNavigateToEditor = { id -> navViewModel.push(MealTypeEditorRoute(mealTypeId = id)) },
                    onNavigateUp = { navViewModel.pop() }
                )
            }

            is MealTypeEditorRoute -> NavEntry(key) {
                val vm: MealTypeEditorViewModel = viewModel(
                    factory = MealTypeEditorViewModel.Companion.Factory(registry, key.mealTypeId)
                )
                MealTypeEditorScreen(
                    viewModel = vm,
                    onNavigateUp = { navViewModel.pop() }
                )
            }

            is InsulinHistoryRoute -> NavEntry(key) {
                val vm: InsulinHistoryViewModel = viewModel(
                    factory = InsulinHistoryViewModel.Companion.Factory(registry)
                )
                InsulinHistoryScreen(
                    viewModel = vm,
                    onNavigateUp = { navViewModel.pop() }
                )
            }

            is RemindersRoute -> NavEntry(key) {
                val vm: RemindersViewModel = viewModel(
                    factory = RemindersViewModel.Companion.Factory(registry)
                )
                RemindersScreen(
                    viewModel = vm,
                    onNavigateUp = { navViewModel.pop() },
                    onNavigateToPastReminders = { navViewModel.push(PastRemindersRoute) },
                    onAddReminder = { navViewModel.push(ReminderEditorRoute()) },
                    onEditReminder = { id -> navViewModel.push(ReminderEditorRoute(reminderId = id)) }
                )
            }

            is ReminderEditorRoute -> NavEntry(key) {
                val vm: ReminderEditorViewModel = viewModel(
                    factory = ReminderEditorViewModel.Companion.Factory(registry, key.reminderId)
                )
                ReminderEditorScreen(
                    viewModel = vm,
                    onNavigateUp = { navViewModel.pop() }
                )
            }

            is PastRemindersRoute -> NavEntry(key) {
                val vm: PastRemindersViewModel = viewModel(
                    factory = PastRemindersViewModel.Companion.Factory(registry)
                )
                PastRemindersScreen(
                    viewModel = vm,
                    onNavigateUp = { navViewModel.pop() }
                )
            }

            is FoodDatabaseRoute -> NavEntry(key) {
                FoodDatabaseScreen(onNavigateUp = { navViewModel.pop() })
            }

            is SystemControlRoute -> NavEntry(key) {
                val vm: SystemControlViewModel = viewModel(
                    factory = SystemControlViewModel.Companion.Factory(registry)
                )
                SystemControlScreen(
                    viewModel = vm,
                    initialTab = key.initialTab,
                    onNavigateUp = { navViewModel.pop() },
                    onNavigateToCoreDecisions = { navViewModel.push(CoreDecisionsRoute) },
                    onNavigateToPumpSetup = { navViewModel.push(PumpSetupRoute) },
                    onNavigateToGlucoseSourceSetup = { navViewModel.push(GlucoseSourceSetupRoute) },
                    onNavigateToGlucoseSourceSettings = { navViewModel.push(GlucoseSourceSettingsRoute) },
                    onNavigateToPumpSettings = { navViewModel.push(PumpSettingsRoute) }
                )
            }

            is PumpSetupRoute -> NavEntry(key) {
                val vm: PumpSetupViewModel = viewModel(
                    factory = PumpSetupViewModel.Companion.Factory(registry)
                )
                PumpSetupScreen(
                    viewModel = vm,
                    onNavigateUp = { navViewModel.pop() }
                )
            }

            is GlucoseSourceSetupRoute -> NavEntry(key) {
                val vm: GlucoseSourceSetupViewModel = viewModel(
                    factory = GlucoseSourceSetupViewModel.Companion.Factory(registry)
                )
                GlucoseSourceSetupScreen(
                    viewModel = vm,
                    onNavigateUp = { navViewModel.pop() }
                )
            }

            is GlucoseSourceSettingsRoute -> NavEntry(key) {
                val vm: GlucoseSourceSettingsViewModel = viewModel(
                    factory = GlucoseSourceSettingsViewModel.Companion.Factory(registry)
                )
                val uiState by vm.uiState.collectAsStateWithLifecycle()

                when (val state = uiState) {
                    GlucoseSourceSettingsUiState.Loading -> {
                        // Wait for resolution
                    }
                    GlucoseSourceSettingsUiState.NoSettingsAvailable -> {
                        LaunchedEffect(Unit) {
                            navViewModel.pop()
                        }
                    }
                    is GlucoseSourceSettingsUiState.Content -> {
                        val settingsContent = state.driver.getSettingsScreen(
                            glucoseSource = state.source,
                            onClose = { navViewModel.pop() }
                        )
                        if (settingsContent != null) {
                            Surface(
                                color = MaterialTheme.colorScheme.background,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                settingsContent()
                            }
                        } else {
                            LaunchedEffect(Unit) {
                                navViewModel.pop()
                            }
                        }
                    }
                }
            }

            is PumpSettingsRoute -> NavEntry(key) {
                val vm: PumpSettingsViewModel = viewModel(
                    factory = PumpSettingsViewModel.Companion.Factory(registry)
                )
                val uiState by vm.uiState.collectAsStateWithLifecycle()

                when (val state = uiState) {
                    PumpSettingsUiState.Loading -> {
                        // Wait for resolution
                    }
                    PumpSettingsUiState.NoSettingsAvailable -> {
                        LaunchedEffect(Unit) {
                            navViewModel.pop()
                        }
                    }
                    is PumpSettingsUiState.Content -> {
                        val settingsContent = state.driver.getSettingsScreen(
                            pump = state.pump,
                            onClose = { navViewModel.pop() }
                        )
                        if (settingsContent != null) {
                            Surface(
                                color = MaterialTheme.colorScheme.background,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                settingsContent()
                            }
                        } else {
                            LaunchedEffect(Unit) {
                                navViewModel.pop()
                            }
                        }
                    }
                }
            }

            is CoreDecisionsRoute -> NavEntry(key) {
                val vm: CoreDecisionsViewModel = viewModel(
                    factory = CoreDecisionsViewModel.Companion.Factory(registry)
                )
                CoreDecisionsScreen(
                    viewModel = vm,
                    onNavigateUp = { navViewModel.pop() }
                )
            }

            is MasterDataRoute -> NavEntry(key) {
                MasterDataScreen(
                    onNavigateToInsulinTypes = { navViewModel.push(InsulinTypesRoute) },
                    onNavigateToInsulinProfileEditor = { navViewModel.push(InsulinProfilesRoute) },
                    onNavigateToBgEditor = { navViewModel.push(BgEditorRoute) },
                    onNavigateToMealTypes = { navViewModel.push(MealTypesRoute) },
                    onNavigateToAlertProfiles = { navViewModel.push(AlertProfilesRoute) },
                    onNavigateToTherapyAdjustments = { navViewModel.push(TherapyAdjustmentPresetsRoute) },
                    onNavigateUp = { navViewModel.pop() }
                )
            }

            is TherapyAdjustmentPresetsRoute -> NavEntry(key) {
                val vm: TherapyAdjustmentsViewModel = viewModel(
                    factory = TherapyAdjustmentsViewModel.Companion.Factory(registry)
                )
                TherapyAdjustmentsScreen(
                    viewModel = vm,
                    onNavigateToEditor = { id -> navViewModel.push(TherapyAdjustmentEditorRoute(adjustmentId = id)) },
                    onNavigateUp = { navViewModel.pop() }
                )
            }

            is TherapyAdjustmentEditorRoute -> NavEntry(key) {
                val vm: TherapyAdjustmentEditorViewModel = viewModel(
                    factory = TherapyAdjustmentEditorViewModel.Companion.Factory(registry, key.adjustmentId)
                )
                TherapyAdjustmentEditorScreen(
                    viewModel = vm,
                    onNavigateUp = { navViewModel.pop() }
                )
            }

            is InsulinTypesRoute -> NavEntry(key) {
                val context = LocalContext.current
                val defaultInsulinTypes = remember(context) { getDefaultInsulinTypes(context) }
                val vm: InsulinTypesViewModel = viewModel(
                    factory = InsulinTypesViewModel.Companion.Factory(registry, defaultInsulinTypes)
                )
                InsulinTypesScreen(
                    viewModel = vm,
                    onNavigateToEditor = { id -> navViewModel.push(InsulinTypeEditorRoute(insulinTypeId = id)) },
                    onNavigateUp = { navViewModel.pop() }
                )
            }

            is InsulinTypeEditorRoute -> NavEntry(key) {
                val vm: InsulinTypeEditorViewModel = viewModel(
                    factory = InsulinTypeEditorViewModel.Companion.Factory(registry, key.insulinTypeId)
                )
                InsulinTypeEditorScreen(
                    viewModel = vm,
                    onNavigateUp = { navViewModel.pop() }
                )
            }

            is AlertProfilesRoute -> NavEntry(key) {
                val vm: AlertProfilesViewModel = viewModel(
                    factory = AlertProfilesViewModel.Companion.Factory(registry)
                )
                AlertProfilesScreen(
                    viewModel = vm,
                    onNavigateToEditor = { id -> navViewModel.push(AlertProfileEditorRoute(profileId = id)) },
                    onNavigateUp = { navViewModel.pop() }
                )
            }

            is AlertProfileEditorRoute -> NavEntry(key) {
                val vm: AlertProfileEditorViewModel = viewModel(
                    factory = AlertProfileEditorViewModel.Companion.Factory(registry, key.profileId)
                )
                AlertProfileEditorScreen(
                    viewModel = vm,
                    onNavigateUp = { navViewModel.pop() }
                )
            }

            else -> null
        }
    }
}