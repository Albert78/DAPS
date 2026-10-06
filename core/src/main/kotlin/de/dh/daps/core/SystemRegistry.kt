package de.dh.daps.core

import android.content.Context
import android.net.Uri
import de.dh.daps.AppPreferencesRepository
import de.dh.daps.common.model.PluginManager
import de.dh.daps.common.model.calculation.CarbsInsulinCalculator
import de.dh.daps.common.model.data.TimeService
import de.dh.daps.core.alarms.AlarmEvaluator
import de.dh.daps.core.alarms.AlarmPlayerManager
import de.dh.daps.core.alarms.AlarmSnoozeManager
import de.dh.daps.core.aps.GlucoseSourceDriverManager
import de.dh.daps.core.aps.GlucoseSourceManager
import de.dh.daps.core.aps.RecommendationManager
import de.dh.daps.core.aps.SystemOrchestrator
import de.dh.daps.core.aps.TherapyManager
import de.dh.daps.core.backup.AppDataManagementRepository
import de.dh.daps.core.device.DeviceConnectionManager
import de.dh.daps.core.pump.PumpDriverManager
import de.dh.daps.core.pump.PumpManager
import de.dh.daps.core.repository.AlarmRepository
import de.dh.daps.core.repository.DeviceManagementRepository
import de.dh.daps.core.repository.DeviceStatusRepository
import de.dh.daps.core.repository.FoodRepository
import de.dh.daps.core.repository.GlucoseRepository
import de.dh.daps.core.repository.PermissionRepository
import de.dh.daps.core.repository.SettingsRepository
import de.dh.daps.core.repository.SystemMetricsRepository
import de.dh.daps.core.repository.TherapyRepository
import de.dh.daps.core.repository.TreatmentRepository
import de.dh.daps.core.system.SystemWakeService
import kotlinx.coroutines.flow.StateFlow

/**
 * Functional interface for handling permission change events.
 */
fun interface PermissionsChangedHandler {
    /**
     * Called when the application permissions have changed and dependent services
     * need to be updated.
     */
    fun onPermissionsChanged()
}

/**
 * State representing the initialization progress of the system.
 */
enum class InitializationState {
    /** System requires setup/master data before background engines can start. */
    REQUIRES_SETUP,
    /** System initialization is in progress. */
    INITIALIZING,
    /** System is fully initialized and core engines are running. */
    READY
}

/**
 * Option chosen in the setup wizard for initial data setup.
 */
sealed interface SetupOption {
    /** Initialize database with default/demo data. */
    object SeedDemoData : SetupOption
    /** Manual setup completed by the user in the wizard. */
    object ManualSetupCompleted : SetupOption
    /** Restore database and settings from a backup file URI. */
    data class ImportBackup(val uri: Uri) : SetupOption
}

/**
 * Central registry for all core services, repositories, and coordinators.
 * This acts as the single source of truth for component access within the application.
 */
interface SystemRegistry {
    /**
     * The global application context.
     */
    val appContext: Context

    // Data Repositories

    /**
     * Repository for data providers, sensors and glucose values. The glucose source plugin
     * might have its own database, if needed.
     */
    val glucoseRepository: GlucoseRepository

    /**
     * Repository for therapy settings, profiles, and active therapy configurations.
     */
    val therapyRepository: TherapyRepository

    /**
     * Repository for alarm configurations and active alarm profile.
     */
    val alarmRepository: AlarmRepository

    /**
     * Central engine for audio playback, haptics/vibration, and safety audio focus handling.
     */
    val alarmPlayerManager: AlarmPlayerManager

    /**
     * Manages snooze state and temporary silences for alarms.
     */
    val alarmSnoozeManager: AlarmSnoozeManager

    /**
     * Evaluates condition triggers and orchestrates alarm firing.
     */
    val alarmEvaluator: AlarmEvaluator

    /**
     * Repository for active metabolic treatments, including insulin applications and carb intake.
     */
    val treatmentRepository: TreatmentRepository

    /**
     * Repository for managing known food items and nutritional data.
     */
    val foodRepository: FoodRepository

    /**
     * Repository for tracking device-related events, such as cannula or reservoir changes.
     */
    val deviceManagementRepository: DeviceManagementRepository

    /**
     * Repository for general system and application settings.
     */
    val settingsRepository: SettingsRepository

    /**
     * Repository for algorithm internal metrics and decision reasoning history.
     */
    val systemMetricsRepository: SystemMetricsRepository

    /**
     * Repository for Android device system status metrics (e.g. battery level, Bluetooth status).
     */
    val deviceStatusRepository: DeviceStatusRepository

    /**
     * Repository for checking and observing system permissions.
     */
    val permissionRepository: PermissionRepository

    /**
     * Repository for lightweight application preferences and key-value pairs.
     */
    val appPreferencesRepository: AppPreferencesRepository

    /**
     * Repository for app data management, backup, restore and reset operations.
     */
    val appDataManagementRepository: AppDataManagementRepository

    // System Managers and Services

    /**
     * Central coordinator for managing system plugins (e.g., pump or glucose source drivers).
     */
    val pluginManager: PluginManager

    /**
     * Manages system wakeups and wake locks to ensure critical background tasks are executed.
     */
    val wakeService: SystemWakeService

    /**
     * Provides the system-wide time reference and handles synchronized ticking for background processes.
     */
    val timeService: TimeService

    // Domain Managers and Services

    /**
     * The mathematical core for calculating insulin-on-board (IOB) and carbs-on-board (COB).
     */
    val carbsInsulinCalculator: CarbsInsulinCalculator

    /**
     * Manages the active glucose data source and processes incoming blood glucose readings.
     */
    val glucoseSourceManager: GlucoseSourceManager

    /**
     * Manages registered CGM / blood glucose driver plugins.
     */
    val glucoseSourceDriverManager: GlucoseSourceDriverManager

    /**
     * Central interface for monitoring and interacting with the insulin pump hardware.
     */
    val pumpManager: PumpManager

    /**
     * Manages registered insulin pump driver plugins.
     */
    val pumpDriverManager: PumpDriverManager

    /**
     * Central manager responsible for connecting, disconnecting, and restoring device connections.
     */
    val deviceConnectionManager: DeviceConnectionManager

    /**
     * Core coordinator for therapy logic, combining data from various sources to generate APS recommendations.
     */
    val therapyManager: TherapyManager

    /**
     * Central manager for treatment recommendations and meal reminders.
     */
    val recommendationManager: RecommendationManager

    /**
     * Manages the overall application state, including the active APS mode and system-wide issues.
     */
    val systemOrchestrator: SystemOrchestrator

    // Other stuff

    /**
     * Handler for permission change events.
     */
    val permissionsChangedHandler: PermissionsChangedHandler

    // System Initialization & Lifecycle

    /**
     * Observable initialization state of the system registry and database.
     */
    val initializationState: StateFlow<InitializationState>

    /**
     * Checks if the database is already populated with required master data.
     */
    suspend fun isDatabaseInitialized(): Boolean

    /**
     * Completes system initialization with the chosen setup option and starts core engines.
     */
    suspend fun completeInitialization(option: SetupOption)

    /**
     * Resets the system to factory settings by stopping active services, clearing database
     * tables and preferences, and setting the initialization state back to [InitializationState.REQUIRES_SETUP].
     */
    suspend fun resetToFactorySettings()
}