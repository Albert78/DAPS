package de.dh.daps.core

import android.app.Application
import android.content.Context
import de.dh.daps.AppPreferencesRepository
import de.dh.daps.common.METABOLIC_EVENTS_HISTORY_HOURS
import de.dh.daps.common.model.GlucoseSourceDriver
import de.dh.daps.common.model.InsulinPumpDriver
import de.dh.daps.common.model.PluginManager
import de.dh.daps.common.model.calculation.CarbsInsulinCalculator
import de.dh.daps.common.model.data.Minutes
import de.dh.daps.common.model.data.TimeService
import de.dh.daps.core.alarms.AlarmEvaluator
import de.dh.daps.core.alarms.AlarmPlayerManager
import de.dh.daps.core.alarms.AlarmPlayerManagerImpl
import de.dh.daps.core.alarms.AlarmSnoozeManager
import de.dh.daps.core.aps.GlucoseSourceDriverManager
import de.dh.daps.core.aps.GlucoseSourceManager
import de.dh.daps.core.aps.RecommendationManager
import de.dh.daps.core.aps.SystemOrchestrator
import de.dh.daps.core.aps.SystemOrchestratorImpl
import de.dh.daps.core.aps.TherapyManager
import de.dh.daps.core.backup.BackupOptions
import de.dh.daps.core.backup.BackupRepository
import de.dh.daps.core.backup.BackupRepositoryImpl
import de.dh.daps.core.backup.BackupResult
import de.dh.daps.core.device.DeviceConnectionManager
import de.dh.daps.core.pump.PumpDriverManager
import de.dh.daps.core.pump.PumpManager
import de.dh.daps.core.pump.PumpManagerImpl
import de.dh.daps.core.repository.AlarmRepository
import de.dh.daps.core.repository.AlarmRepositoryImpl
import de.dh.daps.core.repository.DatabaseInitializer
import de.dh.daps.core.repository.DeviceManagementRepository
import de.dh.daps.core.repository.DeviceStatusRepository
import de.dh.daps.core.repository.FoodRepository
import de.dh.daps.core.repository.GlucoseRepository
import de.dh.daps.core.repository.PermissionRepository
import de.dh.daps.core.repository.SettingsRepository
import de.dh.daps.core.repository.SystemMetricsRepository
import de.dh.daps.core.repository.TherapyRepository
import de.dh.daps.core.repository.TreatmentRepository
import de.dh.daps.core.repository.db.AppDatabase
import de.dh.daps.core.system.AndroidNotifications
import de.dh.daps.core.system.SystemWakeService
import de.dh.daps.core.system.SystemWakeServiceImpl
import de.dh.daps.core.system.TimeServiceImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Default implementation of the [SystemRegistry].
 * Manages the lifecycle and dependencies of all core components.
 */
class SystemRegistryImpl(
    override val appContext: Context,
    override val glucoseRepository: GlucoseRepository,
    override val therapyRepository: TherapyRepository,
    override val alarmRepository: AlarmRepository,
    override val alarmPlayerManager: AlarmPlayerManager,
    override val alarmSnoozeManager: AlarmSnoozeManager,
    override val alarmEvaluator: AlarmEvaluator,
    override val treatmentRepository: TreatmentRepository,
    override val foodRepository: FoodRepository,
    override val deviceManagementRepository: DeviceManagementRepository,
    override val settingsRepository: SettingsRepository,
    override val systemMetricsRepository: SystemMetricsRepository,
    override val deviceStatusRepository: DeviceStatusRepository,
    override val permissionRepository: PermissionRepository,
    override val appPreferencesRepository: AppPreferencesRepository,
    override val backupRepository: BackupRepository,
    override val glucoseSourceManager: GlucoseSourceManager,
    override val glucoseSourceDriverManager: GlucoseSourceDriverManager,
    override val therapyManager: TherapyManager,
    override val recommendationManager: RecommendationManager,
    override val systemOrchestrator: SystemOrchestrator,
    override val pluginManager: PluginManager,
    override val wakeService: SystemWakeService,
    override val timeService: TimeService,
    override val pumpManager: PumpManager,
    override val pumpDriverManager: PumpDriverManager,
    override val deviceConnectionManager: DeviceConnectionManager,
    override val carbsInsulinCalculator: CarbsInsulinCalculator,
    override val permissionsChangedHandler: PermissionsChangedHandler,
) : SystemRegistry {

    private val _initializationState = MutableStateFlow(InitializationState.REQUIRES_SETUP)
    override val initializationState: StateFlow<InitializationState> = _initializationState.asStateFlow()

    override suspend fun isDatabaseInitialized(): Boolean {
        return DatabaseInitializer.isInitialized(therapyRepository, settingsRepository)
    }

    override suspend fun completeInitialization(option: SetupOption) {
        withContext(Dispatchers.Default) {
            _initializationState.value = InitializationState.INITIALIZING
            runCatching {
                when (option) {
                    is SetupOption.SeedDemoData -> {
                        DatabaseInitializer.initializeDefaultData(
                            appContext,
                            treatmentRepository,
                            therapyRepository,
                            settingsRepository,
                            alarmRepository
                        )
                    }
                    is SetupOption.ManualSetupCompleted -> {
                        DatabaseInitializer.ensureMinimumSettings(
                            appContext,
                            treatmentRepository,
                            therapyRepository,
                            settingsRepository,
                            alarmRepository
                        )
                    }
                    is SetupOption.ImportBackup -> {
                        val inputStream = appContext.contentResolver.openInputStream(option.uri)
                            ?: throw IllegalStateException("Unable to open the selected file")
                        val result = inputStream.use { stream ->
                            backupRepository.importBackup(
                                stream,
                                BackupOptions(
                                    includeHistory = true,
                                    includeDiagnostics = true,
                                    includeDescriptors = true
                                )
                            )
                        }
                        if (result is BackupResult.Error) {
                            throw result.exception
                        }
                        DatabaseInitializer.ensureMinimumSettings(
                            appContext,
                            treatmentRepository,
                            therapyRepository,
                            settingsRepository,
                            alarmRepository
                        )
                    }
                }

                // Reload caches after database setup
                treatmentRepository.load()
                therapyRepository.clearCache()
                glucoseRepository.initialize()

                // Start active engine and services
                startCoreEngine()
            }.onFailure { exception ->
                _initializationState.value = InitializationState.REQUIRES_SETUP
                throw exception
            }
        }
    }

    override suspend fun resetToFactorySettings() {
        withContext(Dispatchers.Default) {
            _initializationState.value = InitializationState.INITIALIZING
            runCatching {
                // 1. Stop background engines and disconnect active devices
                deviceConnectionManager.disconnectGlucoseSource()
                deviceConnectionManager.disconnectPump()
                glucoseSourceManager.stop()
                systemOrchestrator.stop()
                alarmEvaluator.stop()
                alarmPlayerManager.stopAlarm()
                alarmSnoozeManager.clearAllSnoozes()

                // 2. Wipe database and app preferences
                backupRepository.clearAllData()
                appPreferencesRepository.editPreferences { mutablePreferences ->
                    mutablePreferences.clear()
                }

                // 3. Reload in-memory state
                treatmentRepository.load()
                therapyRepository.clearCache()
                glucoseRepository.initialize()
            }.also {
                _initializationState.value = InitializationState.REQUIRES_SETUP
            }
        }
    }

    suspend fun startCoreEngine() {
        // TODO: Check result, handle errors
        deviceConnectionManager.restoreConnections()

        therapyManager.startInitialization()

        systemOrchestrator.startInitialization(
            treatmentRepository = treatmentRepository,
            therapyManager = therapyManager,
            recommendationManager = recommendationManager,
            pumpManager = pumpManager,
            appPreferencesRepository = appPreferencesRepository,
            carbsInsulinCalculator = carbsInsulinCalculator,
            systemMetricsRepository = systemMetricsRepository
        )

        alarmEvaluator.start()

        pluginManager.getPlugins().forEach { plugin ->
            plugin.initialize(this)
        }

        _initializationState.value = InitializationState.READY
    }

    companion object {
        /**
         * Factory method to create and initialize the [SystemRegistry].
         * This encapsulates the complex setup of all repositories, managers, and the APS core.
         */
        fun create(
            application: Application,
            scope: CoroutineScope,
            pluginManager: PluginManager,
            androidNotifications: AndroidNotifications,
            onPermissionsChanged: () -> Unit,
        ): SystemRegistry {
            // Phase 1: Create all repositories and managers

            val appPreferencesRepository = AppPreferencesRepository(context = application, scope = scope)
            val appDatabase = AppDatabase.getInstance(application)

            // Repositories
            val glucoseRepository = GlucoseRepository(appDatabase)
            val therapyRepository = TherapyRepository(appDatabase)
            val alarmRepository = AlarmRepositoryImpl(appDatabase.alarmProfileDao())
            val alarmPlayerManager = AlarmPlayerManagerImpl(application)
            val treatmentRepository = TreatmentRepository(
                historySize = Minutes.ofHours(METABOLIC_EVENTS_HISTORY_HOURS),
                appDatabase = appDatabase
            )
            val foodRepository = FoodRepository(appDatabase)
            val deviceManagementRepository = DeviceManagementRepository(
                appDatabase = appDatabase,
                appPreferencesRepository = appPreferencesRepository,
                scope = scope
            )
            val settingsRepository = SettingsRepository(appDatabase)
            val systemMetricsRepository = SystemMetricsRepository(appDatabase)
            val deviceStatusRepository = DeviceStatusRepository(application)
            val permissionRepository = PermissionRepository(application)

            // Managers
            val wakeService = SystemWakeServiceImpl(
                context = application,
                systemMetricsRepository = systemMetricsRepository,
                scope = scope
            )
            val timeService = TimeServiceImpl(
                wakeService = wakeService,
                systemMetricsRepository = systemMetricsRepository,
                scope = scope
            )
            val pumpManager = PumpManagerImpl(scope = scope, wakeService = wakeService)
            val pumpDriverManager = PumpDriverManager(
                drivers = pluginManager.getPlugins().filterIsInstance<InsulinPumpDriver>()
            )

            val glucoseSourceManager = GlucoseSourceManager(
                glucoseRepository = glucoseRepository
            )
            val glucoseSourceDriverManager = GlucoseSourceDriverManager(
                drivers = pluginManager.getPlugins().filterIsInstance<GlucoseSourceDriver>()
            )

            val deviceConnectionManager = DeviceConnectionManager(
                deviceManagementRepository = deviceManagementRepository,
                glucoseSourceDriverManager = glucoseSourceDriverManager,
                pumpDriverManager = pumpDriverManager,
                glucoseSourceManager = glucoseSourceManager,
                pumpManager = pumpManager
            )

            val recommendationManager = RecommendationManager(
                mealReminderDao = appDatabase.mealReminderDao(),
                wakeService = wakeService,
                scope = scope
            )

            val therapyManager = TherapyManager(
                therapyRepository = therapyRepository,
                treatmentRepository = treatmentRepository,
                pumpManager = pumpManager,
                scope = scope,
                wakeService = wakeService
            )
            val carbsInsulinCalculator = CarbsInsulinCalculator(timeService.tickInterval)

            val systemOrchestrator = SystemOrchestratorImpl(
                glucoseSourceManager = glucoseSourceManager,
                glucoseRepository = glucoseRepository,
                wakeService = wakeService,
                settingsRepository = settingsRepository,
                timeService = timeService,
                androidNotifications = androidNotifications,
                scope = scope
            )

            val alarmSnoozeManager = AlarmSnoozeManager()
            val alarmEvaluator = AlarmEvaluator(
                glucoseRepository = glucoseRepository,
                systemOrchestrator = systemOrchestrator,
                therapyRepository = therapyRepository,
                alarmSnoozeManager = alarmSnoozeManager,
                androidNotifications = androidNotifications,
                scope = scope
            )

            val permissionsHandler = PermissionsChangedHandler {
                permissionRepository.refreshPermissions()
                pluginManager.triggerUpdatesAfterPermissionsChange()
                onPermissionsChanged()
            }

            val backupRepository = BackupRepositoryImpl(
                context = application,
                appDatabase = appDatabase,
                preferencesRepository = appPreferencesRepository,
                treatmentRepository = treatmentRepository,
                therapyRepository = therapyRepository,
                settingsRepository = settingsRepository,
                alarmRepository = alarmRepository,
                glucoseRepository = glucoseRepository
            )

            // Wire up registry
            val registryInstance = SystemRegistryImpl(
                appContext = application,
                glucoseRepository = glucoseRepository,
                therapyRepository = therapyRepository,
                alarmRepository = alarmRepository,
                alarmPlayerManager = alarmPlayerManager,
                alarmSnoozeManager = alarmSnoozeManager,
                alarmEvaluator = alarmEvaluator,
                treatmentRepository = treatmentRepository,
                foodRepository = foodRepository,
                deviceManagementRepository = deviceManagementRepository,
                settingsRepository = settingsRepository,
                systemMetricsRepository = systemMetricsRepository,
                deviceStatusRepository = deviceStatusRepository,
                permissionRepository = permissionRepository,
                appPreferencesRepository = appPreferencesRepository,
                backupRepository = backupRepository,
                therapyManager = therapyManager,
                recommendationManager = recommendationManager,
                glucoseSourceManager = glucoseSourceManager,
                glucoseSourceDriverManager = glucoseSourceDriverManager,
                systemOrchestrator = systemOrchestrator,
                pluginManager = pluginManager,
                wakeService = wakeService,
                timeService = timeService,
                pumpManager = pumpManager,
                pumpDriverManager = pumpDriverManager,
                deviceConnectionManager = deviceConnectionManager,
                carbsInsulinCalculator = carbsInsulinCalculator,
                permissionsChangedHandler = permissionsHandler
            )

            // Provide PluginContext to all registered plugins early (setup)
            pluginManager.getPlugins().forEach { plugin ->
                plugin.setup(registryInstance)
            }

            // Phase 2: Asynchronously check database initialization state and conditionally start core engines
            registryInstance._initializationState.value = InitializationState.INITIALIZING
            scope.launch(Dispatchers.Default) {
                runCatching {
                    glucoseRepository.initialize()
                    treatmentRepository.load()
                    val isDbInitialized = registryInstance.isDatabaseInitialized()

                    if (isDbInitialized) {
                        registryInstance.startCoreEngine()
                    } else {
                        registryInstance._initializationState.value = InitializationState.REQUIRES_SETUP
                    }
                }.onFailure {
                    registryInstance._initializationState.value = InitializationState.REQUIRES_SETUP
                }
            }

            return registryInstance
        }
    }
}