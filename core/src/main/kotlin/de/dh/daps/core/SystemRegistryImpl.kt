package de.dh.daps.core

import android.app.Application
import android.content.Context
import de.dh.daps.common.repository.AppPreferencesRepository
import de.dh.daps.common.METABOLIC_EVENTS_HISTORY_HOURS
import de.dh.daps.common.device.driver.GlucoseSourceDriver
import de.dh.daps.common.device.driver.InsulinPumpDriver
import de.dh.daps.common.infrastructure.Plugin
import de.dh.daps.common.infrastructure.PluginContext
import de.dh.daps.common.infrastructure.PluginManager
import de.dh.daps.common.infrastructure.ScopedPluginPreferences
import de.dh.daps.common.domain.calculation.CarbsInsulinCalculator
import de.dh.daps.common.domain.model.therapy.Minutes
import de.dh.daps.common.infrastructure.ReminderManager
import de.dh.daps.common.infrastructure.SystemWakeService
import de.dh.daps.common.infrastructure.TimeService
import de.dh.daps.core.alarms.AlarmEvaluator
import de.dh.daps.core.alarms.AlarmPlayerManager
import de.dh.daps.core.alarms.AlarmPlayerManagerImpl
import de.dh.daps.core.alarms.AlarmSnoozeManager
import de.dh.daps.core.backup.AppDataManagementRepository
import de.dh.daps.core.backup.AppDataManagementRepositoryImpl
import de.dh.daps.core.backup.BackupOptions
import de.dh.daps.core.backup.BackupResult
import de.dh.daps.core.glucosesource.GlucoseSourceDriverManager
import de.dh.daps.core.glucosesource.GlucoseSourceManager
import de.dh.daps.core.infrastructure.AndroidNotifications
import de.dh.daps.core.infrastructure.PluginContextImpl
import de.dh.daps.core.infrastructure.ReminderManagerImpl
import de.dh.daps.core.infrastructure.SystemWakeServiceImpl
import de.dh.daps.core.infrastructure.TimeServiceImpl
import de.dh.daps.core.orchestration.DeviceConnectionManager
import de.dh.daps.core.orchestration.RecommendationManager
import de.dh.daps.core.orchestration.SystemOrchestrator
import de.dh.daps.core.orchestration.SystemOrchestratorImpl
import de.dh.daps.core.pump.PumpDriverManager
import de.dh.daps.core.pump.PumpManager
import de.dh.daps.core.pump.PumpManagerImpl
import de.dh.daps.core.repository.AlarmRepository
import de.dh.daps.core.repository.AlarmRepositoryImpl
import de.dh.daps.core.repository.DatabaseInitializer
import de.dh.daps.core.repository.DeviceStatusRepository
import de.dh.daps.core.repository.FoodRepository
import de.dh.daps.core.repository.GlucoseRepository
import de.dh.daps.core.repository.PermissionRepository
import de.dh.daps.core.repository.SettingsRepository
import de.dh.daps.core.repository.SystemMetricsRepository
import de.dh.daps.core.repository.TherapyRepository
import de.dh.daps.core.repository.TreatmentRepository
import de.dh.daps.core.repository.db.AppDatabase
import de.dh.daps.core.therapy.TherapyManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

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
    override val settingsRepository: SettingsRepository,
    override val systemMetricsRepository: SystemMetricsRepository,
    override val deviceStatusRepository: DeviceStatusRepository,
    override val permissionRepository: PermissionRepository,
    override val appPreferencesRepository: AppPreferencesRepository,
    override val appDataManagementRepository: AppDataManagementRepository,
    override val glucoseSourceManager: GlucoseSourceManager,
    override val glucoseSourceDriverManager: GlucoseSourceDriverManager,
    override val therapyManager: TherapyManager,
    override val recommendationManager: RecommendationManager,
    override val reminderManager: ReminderManager,
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
    private val pluginContexts = ConcurrentHashMap<String, PluginContext>()

    fun getPluginContext(plugin: Plugin): PluginContext {
        return pluginContexts.getOrPut(plugin.pluginId) {
            PluginContextImpl(
                appContext = appContext,
                pluginManager = pluginManager,
                timeService = timeService,
                carbsInsulinCalculator = carbsInsulinCalculator,
                preferences = ScopedPluginPreferences(plugin.pluginId, appPreferencesRepository),
                wakeService = wakeService,
                reminderManager = reminderManager
            )
        }
    }

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
                        pluginManager.getPlugins().forEach { plugin ->
                            runCatching { plugin.onSeedDefaultData() }
                        }
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
                            ?: throw IllegalStateException("Unable to open the selected file '$option.uri'")
                        val result = inputStream.use { stream ->
                            appDataManagementRepository.importBackup(
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
                // 1. Stop background engines and unbind active devices
                deviceConnectionManager.unbindGlucoseSource()
                deviceConnectionManager.unbindPump()
                glucoseSourceManager.stop()
                pumpManager.stop()
                therapyManager.stop()
                systemOrchestrator.stop()
                alarmEvaluator.stop()
                alarmPlayerManager.stopAlarm()
                alarmSnoozeManager.clearAllSnoozes()
                recommendationManager.stop()
                reminderManager.stop()
                timeService.stop()

                // 2. Wipe database and app preferences
                appDataManagementRepository.clearAllData()
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
        deviceConnectionManager.restoreConnections()

        therapyManager.startInitialization()
        reminderManager.start()

        systemOrchestrator.startInitialization()

        alarmEvaluator.start()

        pluginManager.getPlugins().forEach { plugin ->
            plugin.initialize(getPluginContext(plugin))
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
            val settingsRepository = SettingsRepository(appDatabase)
            val systemMetricsRepository = SystemMetricsRepository(appDatabase)
            val deviceStatusRepository = DeviceStatusRepository(application)
            val permissionRepository = PermissionRepository(application, pluginManager)

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

            val pumpManager = PumpManagerImpl(
                scope = scope,
                wakeService = wakeService
            )
            val pumpDriverManager = PumpDriverManager(
                drivers = pluginManager.getPlugins().filterIsInstance<InsulinPumpDriver>(),
                scope = scope
            )

            val glucoseSourceManager = GlucoseSourceManager(
                glucoseRepository = glucoseRepository
            )
            val glucoseSourceDriverManager = GlucoseSourceDriverManager(
                drivers = pluginManager.getPlugins().filterIsInstance<GlucoseSourceDriver>(),
                scope = scope
            )

            val deviceConnectionManager = DeviceConnectionManager(
                glucoseSourceDriverManager = glucoseSourceDriverManager,
                pumpDriverManager = pumpDriverManager,
                glucoseSourceManager = glucoseSourceManager,
                pumpManager = pumpManager,
                scope = scope
            )

            val reminderManager = ReminderManagerImpl(
                reminderDao = appDatabase.reminderDao(),
                wakeService = wakeService,
                timeService = timeService,
                scope = scope
            )

            val recommendationManager = RecommendationManager()

            val therapyManager = TherapyManager(
                therapyRepository = therapyRepository,
                treatmentRepository = treatmentRepository,
                pumpManager = pumpManager,
                scope = scope,
                wakeService = wakeService
            )
            val carbsInsulinCalculator = CarbsInsulinCalculator(timeService.tickInterval)

            val systemOrchestrator = SystemOrchestratorImpl(
                // Infrastructure & System Services
                context = application,
                scope = scope,
                timeService = timeService,
                wakeService = wakeService,
                androidNotifications = androidNotifications,

                // Repositories (Data & Persistence Layer)
                glucoseRepository = glucoseRepository,
                treatmentRepository = treatmentRepository,
                settingsRepository = settingsRepository,
                deviceStatusRepository = deviceStatusRepository,
                systemMetricsRepository = systemMetricsRepository,
                appPreferencesRepository = appPreferencesRepository,

                // Domain Managers & Calculators
                glucoseSourceManager = glucoseSourceManager,
                pumpManager = pumpManager,
                therapyManager = therapyManager,
                recommendationManager = recommendationManager,
                reminderManager = reminderManager,
                carbsInsulinCalculator = carbsInsulinCalculator
            )

            val alarmSnoozeManager = AlarmSnoozeManager()
            val alarmEvaluator = AlarmEvaluator(
                glucoseRepository = glucoseRepository,
                systemOrchestrator = systemOrchestrator,
                therapyRepository = therapyRepository,
                alarmSnoozeManager = alarmSnoozeManager,
                androidNotifications = androidNotifications,
                pumpManager = pumpManager,
                scope = scope
            )

            val permissionsHandler = PermissionsChangedHandler {
                permissionRepository.refreshPermissions()
                pluginManager.triggerUpdatesAfterPermissionsChange()
                onPermissionsChanged()
            }

            val appDataManagementRepository = AppDataManagementRepositoryImpl(
                context = application,
                appDatabase = appDatabase,
                preferencesRepository = appPreferencesRepository,
                treatmentRepository = treatmentRepository,
                therapyRepository = therapyRepository,
                settingsRepository = settingsRepository,
                alarmRepository = alarmRepository,
                glucoseRepository = glucoseRepository,
                pluginManager = pluginManager
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
                settingsRepository = settingsRepository,
                systemMetricsRepository = systemMetricsRepository,
                deviceStatusRepository = deviceStatusRepository,
                permissionRepository = permissionRepository,
                appPreferencesRepository = appPreferencesRepository,
                appDataManagementRepository = appDataManagementRepository,
                therapyManager = therapyManager,
                recommendationManager = recommendationManager,
                reminderManager = reminderManager,
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
                plugin.setup(registryInstance.getPluginContext(plugin))
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