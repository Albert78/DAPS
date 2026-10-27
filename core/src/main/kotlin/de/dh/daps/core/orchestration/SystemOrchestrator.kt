package de.dh.daps.core.orchestration

import android.app.Notification
import android.content.Context
import android.content.Intent
import de.dh.daps.common.domain.calculation.CarbsInsulinCalculator
import de.dh.daps.common.domain.model.data.BgValue
import de.dh.daps.common.domain.model.data.Minutes
import de.dh.daps.common.domain.model.data.Tick
import de.dh.daps.common.domain.model.data.Timestamp
import de.dh.daps.common.domain.model.engine.ApsMode
import de.dh.daps.common.domain.model.data.BgReadingsInterval
import de.dh.daps.common.domain.model.therapy.MealEntry
import de.dh.daps.common.infrastructure.ReminderManager
import de.dh.daps.common.infrastructure.SystemWakeService
import de.dh.daps.common.infrastructure.TickHandler
import de.dh.daps.common.infrastructure.TickPriority
import de.dh.daps.common.infrastructure.TimeService
import de.dh.daps.common.infrastructure.WakeupHandler
import de.dh.daps.common.repository.AppPreferencesRepository
import de.dh.daps.core.aps.BolusCorrectionCalculator
import de.dh.daps.core.aps.Core
import de.dh.daps.core.aps.CoreIssue
import de.dh.daps.core.aps.CoreState
import de.dh.daps.core.aps.SimpleBolusCorrectionCalculator
import de.dh.daps.core.glucosesource.GlucoseSourceIssue
import de.dh.daps.core.glucosesource.GlucoseSourceManager
import de.dh.daps.core.infrastructure.AndroidNotifications
import de.dh.daps.core.pump.PumpIssue
import de.dh.daps.core.pump.PumpManager
import de.dh.daps.core.repository.DeviceStatusRepository
import de.dh.daps.core.repository.GlucoseRepository
import de.dh.daps.core.repository.SettingsRepository
import de.dh.daps.core.repository.SystemMetricsRepository
import de.dh.daps.core.repository.TreatmentRepository
import de.dh.daps.core.therapy.TherapyManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors

sealed interface ApsIssue {
    /**
     * No recent glucose value available, the loop cannot calculate new treatments.
     */
    data object StaleBG : ApsIssue

    /**
     * Active core issue preventing calculation or normal loop execution.
     */
    data class Core(val issue: CoreIssue) : ApsIssue

    /**
     * Active pump issue preventing insulin delivery or pump operation.
     */
    data class Pump(val issue: PumpIssue) : ApsIssue

    /**
     * Active glucose source issue preventing blood glucose readings.
     */
    data class GlucoseSource(val issue: GlucoseSourceIssue) : ApsIssue

    /**
     * Bluetooth is turned off on the phone, preventing communications with pump and CGM.
     */
    data object BluetoothDisabled : ApsIssue

    /**
     * Any other issue that prevents the core from working.
     */
    data class Other(val message: String? = null) : ApsIssue
}

/**
 * Orchestrates the different subsystems, manages the application mode, dispatches
 * notifications. It also handles persistence and provides a reactive state for other components to observe.
 */
interface SystemOrchestrator {
    /**
     * The current APS mode.
     */
    val apsMode: StateFlow<ApsMode>

    /**
     * The list of currently available APS modes based on configured hardware (CGM and Pump).
     */
    val availableApsModes: StateFlow<List<ApsMode>>

    /**
     * Active issues of the APS.
     */
    val apsIssues: StateFlow<Set<ApsIssue>>

    /**
     * Signal flow indicating whether the blood glucose value is stale.
     */
    val isBgStale: StateFlow<Boolean>

    /**
     * State of the core loop.
     */
    val coreState: StateFlow<CoreState>

    /**
     * Timestamp of the last successful core calculation completion.
     */
    val lastSuccessfulCoreCalculation: StateFlow<Timestamp>

    /**
     * Timestamp of when resume was requested (transition out of ApsMode.Suspend).
     * Null when APS mode is Suspend or no resume transition is active.
     */
    val resumeRequestedAt: StateFlow<Timestamp?>

    /**
     * Updates the APS mode and persists the change.
     */
    fun setApsMode(mode: ApsMode)

    /**
     * Starts the initialization of the system core.
     */
    suspend fun startInitialization()

    /**
     * Creates a notification for the foreground service.
     */
    fun createForegroundServiceNotification(): Notification

    /**
     * Gracefully stops the system.
     */
    fun stop()

    /**
     * Returns the predicted blood glucose value for the given timestamp.
     */
    suspend fun getAssumedBg(timestamp: Timestamp): BgValue

    /**
     * Returns the bolus correction calculator.
     */
    fun getBolusCorrectionCalculator(): BolusCorrectionCalculator

    /**
     * Returns whether the meal bolus screen can be opened.
     */
    fun canOpenMealCorrectionBolus(): Boolean

    companion object {
        const val EXECUTION_DELAY_AFTER_BG_MS = 20_000L
    }
}

/**
 * Implementation of [SystemOrchestrator].
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SystemOrchestratorImpl(
    // Infrastructure & System Services
    private val context: Context,
    private val scope: CoroutineScope,
    private val timeService: TimeService,
    private val wakeService: SystemWakeService,
    private val androidNotifications: AndroidNotifications,

    // Repositories (Data & Persistence Layer)
    private val glucoseRepository: GlucoseRepository,
    private val treatmentRepository: TreatmentRepository,
    private val settingsRepository: SettingsRepository,
    private val deviceStatusRepository: DeviceStatusRepository,
    private val systemMetricsRepository: SystemMetricsRepository,
    private val appPreferencesRepository: AppPreferencesRepository,

    // Domain Managers & Calculators
    private val glucoseSourceManager: GlucoseSourceManager,
    private val pumpManager: PumpManager,
    private val therapyManager: TherapyManager,
    private val recommendationManager: RecommendationManager,
    private val reminderManager: ReminderManager,
    private val carbsInsulinCalculator: CarbsInsulinCalculator,

    // Internal Helper Components (with Default Values)
    private val modeManager: ApsModeManager = ApsModeManager(
        glucoseSourceManager = glucoseSourceManager,
        pumpManager = pumpManager,
        settingsRepository = settingsRepository,
        scope = scope
    ),
    private val issueAggregator: ApsIssueAggregator = ApsIssueAggregator(),
    private val consumableTracker: ConsumableTracker = ConsumableTracker(context),
    private val notificationController: SystemNotificationController = SystemNotificationController(
        androidNotifications = androidNotifications
    )
) : SystemOrchestrator {
    private var coreDispatcher: ExecutorCoroutineDispatcher? = null
    private var coreScope: CoroutineScope? = null
    private var initScope: CoroutineScope? = null

    private var notificationTickHandler: TickHandler? = null
    private var apsCoreTickHandler: TickHandler? = null

    override val apsMode: StateFlow<ApsMode> = modeManager.apsMode
    override val availableApsModes: StateFlow<List<ApsMode>> = modeManager.availableApsModes
    override val resumeRequestedAt: StateFlow<Timestamp?> = modeManager.resumeRequestedAt

    override val apsIssues: StateFlow<Set<ApsIssue>> = issueAggregator.apsIssues
    override val isBgStale: StateFlow<Boolean> = issueAggregator.isBgStale

    private val _coreState = MutableStateFlow<CoreState>(CoreState.Uninitialized)
    override val coreState: StateFlow<CoreState> = _coreState.asStateFlow()

    private val _lastSuccessfulCoreCalculation = MutableStateFlow(Timestamp.now())
    override val lastSuccessfulCoreCalculation: StateFlow<Timestamp> = _lastSuccessfulCoreCalculation.asStateFlow()

    private lateinit var core: Core

    private inner class SystemWakeupHandler : WakeupHandler {
        override fun onWakeup(wakeupId: UInt?, intent: Intent?) {
            when (wakeupId) {
                WAKEUP_STALE_CHECK -> issueAggregator.staleCheck(glucoseSourceManager)
                WAKEUP_DAILY_MAINTENANCE -> performDailyMaintenance()
                WAKEUP_CONSUMABLE_CHECK -> reevaluateConsumables()
            }
        }
    }

    private fun inCoreThreadAsync(block: suspend CoroutineScope.() -> Unit): Job? {
        return coreScope?.launch {
            block()
        }
    }

    private suspend fun <T> inCoreThreadSync(block: suspend CoroutineScope.() -> T): T? {
        val dispatcher = coreDispatcher ?: return null
        return withContext(dispatcher) {
            block()
        }
    }

    override suspend fun startInitialization() {
        stop()

        val newInitScope = CoroutineScope(scope.coroutineContext + SupervisorJob())
        initScope = newInitScope

        val dispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
        coreDispatcher = dispatcher
        val newCoreScope = CoroutineScope(dispatcher + SupervisorJob())
        coreScope = newCoreScope

        performDailyMaintenance()

        issueAggregator.startObserving(
            scope = newInitScope,
            coreState = coreState,
            pumpManager = pumpManager,
            glucoseSourceManager = glucoseSourceManager,
            deviceStatusRepository = deviceStatusRepository,
            consumableIssues = consumableTracker.consumableIssues
        )

        val halfTickMs = timeService.timeline.tickSizeMs / 2
        timeService.executionOffsetMs = halfTickMs + SystemOrchestrator.EXECUTION_DELAY_AFTER_BG_MS

        wakeService.registerHandler(WAKE_TAG, SystemWakeupHandler())

        modeManager.startObservingSettingsAndAvailability(
            initScope = newInitScope,
            deviceInitializationTimeoutMs = DEVICE_INITIALIZATION_TIMEOUT_MS
        )

        newInitScope.launch {
            glucoseRepository.currentBg.drop(1).collect { bg ->
                if (bg != null) {
                    issueAggregator.onNewBgReadingArrived()
                    val nextCheck = issueAggregator.nextBgStaleCheckAt(glucoseSourceManager)
                    wakeService.scheduleWakeup(WAKE_TAG, WAKEUP_STALE_CHECK, nextCheck)

                    if (glucoseSourceManager.readingsInterval == BgReadingsInterval.FiveMinutes) {
                        val centeredTimestamp = Timestamp(bg.timestamp.ms - halfTickMs)
                        timeService.synchronize(centeredTimestamp)
                    } else {
                        timeService.synchronize(Timestamp(0))
                    }

                    inCoreThreadAsync {
                        core.onNewBgReading(bg)
                    }
                    notificationController.updateMainAppNotification(glucoseRepository)
                }
            }
        }

        notificationController.createNotificationChannels()
        val notifHandler = notificationController.createTickHandler(glucoseRepository)
        notificationTickHandler = notifHandler
        notificationController.registerTickHandler(timeService, notifHandler)

        notificationController.startObserving(
            scope = newInitScope,
            glucoseRepository = glucoseRepository,
            appPreferencesRepository = appPreferencesRepository,
            recommendationManager = recommendationManager,
            reminderManager = reminderManager
        )

        newInitScope.launch {
            pumpManager.activeInsulinPump.flatMapLatest { pump ->
                pump?.pumpStatus ?: flowOf(null)
            }.combine(appPreferencesRepository.catheterPatchWarningTime) { status, warningTime ->
                Pair(status, warningTime)
            }.collect { (status, warningTime) ->
                consumableTracker.updatePumpStatusAndWarningTime(status, warningTime)
                consumableTracker.evaluateConsumableLifecycle(
                    lifecycleState = status?.lifecycleState,
                    warningTime = warningTime,
                    reminderManager = reminderManager,
                    wakeService = wakeService,
                    wakeTag = WAKE_TAG,
                    wakeupConsumableCheckId = WAKEUP_CONSUMABLE_CHECK
                )
            }
        }

        core = Core.createProductiveCore(
            therapyManager = therapyManager,
            treatmentRepository = treatmentRepository,
            timeline = timeService.timeline,
            carbsInsulinCalculator = carbsInsulinCalculator,
            glucoseRepository = glucoseRepository,

            withBusyState = { block -> wakeService.withBusyStateSuspend(WAKE_TAG, block) },

            onDeliverBolus = { treatmentLock, amount, handledDeferredBoluses, correctionPart, basalPart ->
                when (apsMode.value) {
                    ApsMode.AutoCorrection -> {
                        therapyManager.issueBolus(
                            treatmentLock = treatmentLock,
                            amount = amount,
                            handledDeferredBoluses = handledDeferredBoluses,
                            correctionPart = correctionPart,
                            basalPart = basalPart,
                            expiresAt = Timestamp.now().plus(AUTOMATED_BOLUS_EXPIRATION)
                        )
                    }
                    else -> {
                        val associatedMeal = if (!handledDeferredBoluses.isNullOrEmpty()) {
                            val tr = treatmentRepository
                            val mealIds = handledDeferredBoluses.mapNotNull { it.mealId }.distinct()
                            var latestMeal: MealEntry? = null
                            if (mealIds.isNotEmpty()) {
                                val meals = mealIds.mapNotNull { tr.getMeal(it) }
                                latestMeal = meals.maxByOrNull { it.timestamp }
                            }
                            if (latestMeal == null) {
                                latestMeal = tr.getMeals().maxByOrNull { it.timestamp }
                            }
                            latestMeal
                        } else null

                        recommendationManager.addBolusRecommendation(
                            amount = amount,
                            includedDeferredBoluses = handledDeferredBoluses,
                            associatedMeal = associatedMeal,
                            correctionPart = correctionPart,
                            basalPart = basalPart
                        )
                    }
                }
            },
            onApplyDeferredBolusUpdates = { treatmentLock, updates -> therapyManager.applyDeferredBolusUpdates(treatmentLock, updates) },
            onSetTempBasal = { treatmentLock, durationInHours, percent ->
                when (apsMode.value) {
                    ApsMode.AutoCorrection -> therapyManager.setTempBasal(treatmentLock, durationInHours, percent)
                    else -> recommendationManager.addTempBasalRecommendation(durationInHours, percent)
                }
            },
            onClearTempBasal = { treatmentLock ->
                when (apsMode.value) {
                    ApsMode.AutoCorrection -> therapyManager.clearTempBasal(treatmentLock)
                    else -> recommendationManager.clearTempBasalRecommendation()
                }
            },
            onCarbsHint = { amountInGram -> recommendationManager.addCarbsRecommendation(amountInGram) },
            onClearRecommendations = { recommendationManager.clearRecommendations() },
            onWaitForPumpSync = { treatmentLock -> therapyManager.waitForPumpSync(treatmentLock) },
            systemMetricsRepository = systemMetricsRepository,
            scope = newInitScope
        )

        newInitScope.launch {
            core.coreState.collect { state ->
                _coreState.value = state
            }
        }
        newInitScope.launch {
            core.lastSuccessfulCoreCalculation.collect { timestamp ->
                _lastSuccessfulCoreCalculation.value = timestamp
            }
        }

        inCoreThreadAsync {
            core.initialize()

            launch {
                apsMode.collect { mode ->
                    when (mode) {
                        ApsMode.AutoCorrection, ApsMode.Manual -> {
                            core.activate()
                            core.processCalculation()
                        }
                        ApsMode.Suspend -> {
                            core.suspend()
                        }
                    }
                }
            }
            launch {
                therapyManager.currentTherapySettingsFlow.drop(1).collect { _ ->
                    core.onTherapySettingsChanged()
                }
            }
            launch {
                treatmentRepository.observeMeals().drop(1).collect { _ ->
                    core.onMealsChanged()
                }
            }
            launch {
                treatmentRepository.observeInsulinApplications().drop(1).collect { _ ->
                    core.onInsulinChanged()
                }
            }
        }

        val apsHandler = object : TickHandler {
            override suspend fun onTick(tick: Tick) {
                inCoreThreadSync {
                    core.processCalculation()
                }
            }
        }
        apsCoreTickHandler = apsHandler
        timeService.registerTickHandler(TickPriority.APS, apsHandler, "APS Core")
    }

    override fun createForegroundServiceNotification(): Notification {
        return notificationController.createForegroundServiceNotification(glucoseRepository)
    }

    override fun stop() {
        initScope?.cancel()
        initScope = null

        notificationTickHandler?.let { notificationController.unregisterTickHandler(timeService, it) }
        notificationTickHandler = null

        apsCoreTickHandler?.let { timeService.unregisterTickHandler(it) }
        apsCoreTickHandler = null

        coreScope?.cancel()
        coreScope = null

        coreDispatcher?.close()
        coreDispatcher = null

        wakeService.cancelWakeup(WAKE_TAG, WAKEUP_STALE_CHECK)
        wakeService.cancelWakeup(WAKE_TAG, WAKEUP_DAILY_MAINTENANCE)
        wakeService.cancelWakeup(WAKE_TAG, WAKEUP_CONSUMABLE_CHECK)
        wakeService.unregisterHandler(WAKE_TAG)

        issueAggregator.reset()
        consumableTracker.reset()
        modeManager.reset()
        _coreState.value = CoreState.Uninitialized
    }

    override suspend fun getAssumedBg(timestamp: Timestamp): BgValue {
        return if (::core.isInitialized && coreScope != null) {
            core.getAssumedBg(timestamp)
        } else {
            BgValue.INVALID
        }
    }

    override fun getBolusCorrectionCalculator(): BolusCorrectionCalculator {
        return if (::core.isInitialized && coreScope != null) {
            core.getBolusCorrectionCalculator()
        } else {
            SimpleBolusCorrectionCalculator(therapyManager, glucoseRepository)
        }
    }

    override fun canOpenMealCorrectionBolus(): Boolean = when (apsMode.value) {
        ApsMode.AutoCorrection, ApsMode.Manual -> true
        ApsMode.Suspend -> false
    }

    override fun setApsMode(mode: ApsMode) {
        modeManager.setApsMode(mode, therapyManager)
    }

    private fun performDailyMaintenance() {
        scope.launch {
            reminderManager.purgeOldReminders(3)
        }
        scheduleDailyMaintenance()
    }

    private fun scheduleDailyMaintenance() {
        val nextMaintenance = timeService.currentTime.plusHours(24)
        wakeService.scheduleWakeup(WAKE_TAG, WAKEUP_DAILY_MAINTENANCE, nextMaintenance)
    }

    private fun reevaluateConsumables() {
        val status = consumableTracker.lastPumpStatus
        val warningTime = consumableTracker.lastWarningTime ?: return
        initScope?.launch {
            consumableTracker.evaluateConsumableLifecycle(
                lifecycleState = status?.lifecycleState,
                warningTime = warningTime,
                reminderManager = reminderManager,
                wakeService = wakeService,
                wakeTag = WAKE_TAG,
                wakeupConsumableCheckId = WAKEUP_CONSUMABLE_CHECK
            )
        }
    }

    companion object {
        const val WAKE_TAG = "SystemOrchestrator"
        const val WAKEUP_STALE_CHECK = 0u
        const val WAKEUP_DAILY_MAINTENANCE = 1u
        const val WAKEUP_CONSUMABLE_CHECK = 2u
        const val DEVICE_INITIALIZATION_TIMEOUT_MS = 1000L
        val AUTOMATED_BOLUS_EXPIRATION = Minutes(4)
    }
}