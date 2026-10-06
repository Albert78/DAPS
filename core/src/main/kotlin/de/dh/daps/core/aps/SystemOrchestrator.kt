package de.dh.daps.core.aps

import android.app.Notification
import android.content.Intent
import android.util.Log
import de.dh.daps.AppPreferencesRepository
import de.dh.daps.common.model.ApsMode
import de.dh.daps.common.model.MealEntry
import de.dh.daps.common.model.calculation.CarbsInsulinCalculator
import de.dh.daps.common.model.data.BgReadingsInterval
import de.dh.daps.common.model.data.BgValue
import de.dh.daps.common.model.data.Tick
import de.dh.daps.common.model.data.Timestamp
import de.dh.daps.common.service.SystemWakeService
import de.dh.daps.common.service.TickHandler
import de.dh.daps.common.service.TickPriority
import de.dh.daps.common.service.TimeService
import de.dh.daps.common.service.WakeupHandler
import de.dh.daps.core.pump.PumpIssue
import de.dh.daps.core.pump.PumpManager
import de.dh.daps.core.repository.GlucoseRepository
import de.dh.daps.core.repository.SettingsRepository
import de.dh.daps.core.repository.SystemMetricsRepository
import de.dh.daps.core.repository.TreatmentRepository
import de.dh.daps.core.system.AndroidNotifications
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.Executors
import kotlin.time.Duration.Companion.milliseconds

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
    suspend fun startInitialization(
        treatmentRepository: TreatmentRepository,
        therapyManager: TherapyManager,
        recommendationManager: RecommendationManager,
        pumpManager: PumpManager,
        appPreferencesRepository: AppPreferencesRepository,
        carbsInsulinCalculator: CarbsInsulinCalculator,
        systemMetricsRepository: SystemMetricsRepository
    )

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
    private val glucoseSourceManager: GlucoseSourceManager,
    private val pumpManager: PumpManager,
    private val glucoseRepository: GlucoseRepository,
    private val wakeService: SystemWakeService,
    private val settingsRepository: SettingsRepository,
    private val timeService: TimeService,
    private val androidNotifications: AndroidNotifications,
    private val scope: CoroutineScope
) : SystemOrchestrator {
    // Threading: Single background thread to avoid race conditions in the core logic
    private var coreDispatcher: ExecutorCoroutineDispatcher? = null
    private var coreScope: CoroutineScope? = null
    private var initScope: CoroutineScope? = null

    private var notificationTickHandler: TickHandler? = null
    private var apsCoreTickHandler: TickHandler? = null

    private val _apsMode = MutableStateFlow(ApsMode.Suspend)
    override val apsMode: StateFlow<ApsMode> = _apsMode.asStateFlow()

    override val availableApsModes: StateFlow<List<ApsMode>> = combine(
        glucoseSourceManager.activeGlucoseSource,
        pumpManager.activeInsulinPump
    ) { glucoseSource, pump ->
        getAvailableApsModes(
            hasGlucoseSource = glucoseSource != null,
            hasPump = pump != null
        )
    }.stateIn(
        scope = scope,
        started = SharingStarted.Eagerly,
        initialValue = ApsMode.entries
    )

    private fun getAvailableApsModes(hasGlucoseSource: Boolean, hasPump: Boolean): List<ApsMode> =
        ApsMode.entries.filter { it.isAvailable(hasPump = hasPump, hasGlucoseSource = hasGlucoseSource) }

    private val _apsIssues = MutableStateFlow<Set<ApsIssue>>(emptySet())
    override val apsIssues: StateFlow<Set<ApsIssue>> = _apsIssues.asStateFlow()

    private val _isBgStale = MutableStateFlow(false)
    override val isBgStale: StateFlow<Boolean> = _isBgStale.asStateFlow()

    private val _coreState = MutableStateFlow<CoreState>(CoreState.Uninitialized)
    override val coreState: StateFlow<CoreState> = _coreState.asStateFlow()

    private val _lastSuccessfulCoreCalculation = MutableStateFlow(Timestamp.now())
    override val lastSuccessfulCoreCalculation: StateFlow<Timestamp> = _lastSuccessfulCoreCalculation.asStateFlow()

    private val _resumeRequestedAt = MutableStateFlow<Timestamp?>(null)
    override val resumeRequestedAt: StateFlow<Timestamp?> = _resumeRequestedAt.asStateFlow()

    // Computation Core: Pure logic and state, completely thread-agnostic
    private lateinit var core: Core

    private var therapyManager: TherapyManager? = null
    private var treatmentRepository: TreatmentRepository? = null
    private var carbsInsulinCalculator: CarbsInsulinCalculator? = null

    private inner class NotificationTickHandler : TickHandler {
        override suspend fun onTick(tick: Tick) {
            androidNotifications.updateMainAppNotification(glucoseRepository)
        }
    }

    private inner class SystemWakeupHandler : WakeupHandler {
        override fun onWakeup(wakeupId: UInt?, intent: Intent?) {
            if (wakeupId == WAKEUP_STALE_CHECK) {
                staleCheck()
            }
        }
    }

    /**
     * Executes the given block on the internal core thread asynchronously.
     */
    private fun inCoreThreadAsync(block: suspend CoroutineScope.() -> Unit): Job? {
        return coreScope?.launch {
            block()
        }
    }

    /**
     * Executes the given block on the internal core thread and waits for its completion.
     */
    private suspend fun <T> inCoreThreadSync(block: suspend CoroutineScope.() -> T): T? {
        val dispatcher = coreDispatcher ?: return null
        return withContext(dispatcher) {
            block()
        }
    }

    override suspend fun startInitialization(
        treatmentRepository: TreatmentRepository,
        therapyManager: TherapyManager,
        recommendationManager: RecommendationManager,
        pumpManager: PumpManager,
        appPreferencesRepository: AppPreferencesRepository,
        carbsInsulinCalculator: CarbsInsulinCalculator,
        systemMetricsRepository: SystemMetricsRepository
    ) {
        stop()

        val newInitScope = CoroutineScope(scope.coroutineContext + SupervisorJob())
        initScope = newInitScope

        val dispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
        coreDispatcher = dispatcher
        val newCoreScope = CoroutineScope(dispatcher + SupervisorJob())
        coreScope = newCoreScope

        this.therapyManager = therapyManager
        this.treatmentRepository = treatmentRepository
        this.carbsInsulinCalculator = carbsInsulinCalculator

        newInitScope.launch {
            combine(
                _isBgStale,
                coreState,
                pumpManager.pumpIssues
            ) { stale, state, pIssues ->
                val issues = mutableSetOf<ApsIssue>()
                if (stale) {
                    issues.add(ApsIssue.StaleBG)
                }
                if (state is CoreState.Active) {
                    state.issues.forEach { issues.add(ApsIssue.Core(it)) }
                }
                pIssues.forEach { issues.add(ApsIssue.Pump(it)) }

                // TODO: Depending on severity, switch APS mode to manual and issue alarm
                issues
            }.collect { combinedIssues ->
                _apsIssues.value = combinedIssues
            }
        }

        // Configure execution offset on timeService:
        // The 5-minute tick interval is centered around the BG reading (halfTickMs = 2.5 minutes).
        // Tick handlers should execute 20 seconds after the expected BG reading arrival.
        val halfTickMs = timeService.timeline.tickSizeMs / 2
        timeService.executionOffsetMs = halfTickMs + SystemOrchestrator.EXECUTION_DELAY_AFTER_BG_MS

        wakeService.registerHandler(WAKE_TAG, SystemWakeupHandler())

        newInitScope.launch {
            // Asynchronously restore the persisted APS mode on application startup.
            // Wait up to DEVICE_INITIALIZATION_TIMEOUT_MS for device drivers (pump and CGM) to finish reconnecting,
            // so we can evaluate if the previously saved APS mode is allowed by current hardware state.
            val currentSettings = runCatching { settingsRepository.getCurrentSettings() }.getOrNull()
            if (currentSettings != null) {
                val targetMode = currentSettings.apsMode
                // Wait until availableApsModes contains the target mode or timeout elapses
                val allowed = withTimeoutOrNull(DEVICE_INITIALIZATION_TIMEOUT_MS.milliseconds) {
                    availableApsModes.first { modes -> targetMode in modes }
                } ?: availableApsModes.value

                val initialMode = if (targetMode in allowed) {
                    targetMode
                } else {
                    val fallback = allowed.lastOrNull() ?: ApsMode.Suspend
                    Log.w(TAG, "Restored APS mode $targetMode is not allowed with current devices. Falling back to $fallback.")
                    settingsRepository.updateCurrentSettings(currentSettings.copy(apsMode = fallback))
                    fallback
                }
                _apsMode.value = initialMode
            }

            settingsRepository.observeCurrentSettings().drop(1).collect { settings ->
                if (settings != null) {
                    val allowed = availableApsModes.value
                    if (settings.apsMode in allowed) {
                        _apsMode.value = settings.apsMode
                    } else {
                        val fallback = allowed.lastOrNull() ?: ApsMode.Suspend
                        if (_apsMode.value != fallback) {
                            setApsMode(fallback)
                        }
                    }
                }
            }
        }

        newInitScope.launch {
            availableApsModes.collect { allowed ->
                val current = _apsMode.value
                if (current !in allowed) {
                    val fallback = allowed.lastOrNull() ?: ApsMode.Suspend
                    Log.w(TAG, "Current APS mode $current is no longer available. Downgrading to $fallback.")
                    setApsMode(fallback)
                }
            }
        }

        newInitScope.launch {
            glucoseRepository.currentBg.drop(1).collect { bg ->
                if (bg != null) {
                    _isBgStale.value = false
                    // Schedule stale check for the next window
                    val nextCheck = nextBgStaleCheckAt()
                    wakeService.scheduleWakeup(WAKE_TAG, WAKEUP_STALE_CHECK, nextCheck)

                    // First sync the timeline to our BG...
                    if (glucoseSourceManager.readingsInterval == BgReadingsInterval.FiveMinutes) {
                        // Center the 5-minute tick interval around the BG reading timestamp
                        // (bg.timestamp lies in the center: [bg.timestamp - 2.5m, bg.timestamp + 2.5m)).
                        // This ensures that timing fluctuations in incoming BG readings do not cause tick boundary jumps.
                        val halfTickMs = timeService.timeline.tickSizeMs / 2
                        val centeredTimestamp = Timestamp(bg.timestamp.ms - halfTickMs)
                        timeService.synchronize(centeredTimestamp)
                    } else {
                        // For reading intervals smaller than our 5-minute tick interval, it's ok
                        // if BG values arrive around the interval border. Even if both readings
                        // at the interval borders (start and end) go to the neighbor interval,
                        // we have enough values in the interval left.
                        timeService.synchronize(Timestamp(0))
                    }

                    // ...then add BG value
                    inCoreThreadAsync {
                        core.onNewBgReading(bg)
                    }
                    // Update notification immediately
                    androidNotifications.updateMainAppNotification(glucoseRepository)
                }
            }
        }

        androidNotifications.createNotificationChannels()
        val notifHandler = NotificationTickHandler()
        notificationTickHandler = notifHandler
        timeService.registerTickHandler(TickPriority.UI, notifHandler, "Notifications")

        newInitScope.launch {
            appPreferencesRepository.glucoseUnit.drop(1).collect {
                androidNotifications.updateMainAppNotification(glucoseRepository)
            }
        }

        newInitScope.launch {
            recommendationManager.recommendations.collect { recommendations ->
                val carbRec = recommendations.filterIsInstance<ApsRecommendation.Carbs>().firstOrNull()
                if (carbRec != null) {
                    androidNotifications.showRecommendationNotification(carbRec)
                } else {
                    androidNotifications.cancelRecommendationNotification()
                }

                val dueDeferredBoluses = recommendations
                    .filterIsInstance<ApsRecommendation.Bolus>()
                    .flatMap { it.includedDeferredBoluses.orEmpty() }

                if (dueDeferredBoluses.isNotEmpty()) {
                    androidNotifications.showDeferredBolusRecommendationNotification(dueDeferredBoluses)
                }
            }
        }

        newInitScope.launch {
            recommendationManager.dueMealReminders.collect { reminder ->
                androidNotifications.showMealReminderNotification(reminder)
            }
        }

        // Important implicit dependency to the ManualControlScreen:
        // The ManualControlScreen does not need to request a TreatmentLock because in AutoCorrection mode
        // we forward insulin callbacks directly to the TherapyManager here, ensuring the ManualControlScreen
        // does not conflict with the algorithm. In OpenLoop mode, on the other hand, we route all bolus
        // recommendations to the ManualControlScreen here. Acquiring the TreatmentLock in ManualControlScreen
        // is therefore not necessary.
        core = Core.createProductiveCore(
            therapyManager = therapyManager,
            treatmentRepository = treatmentRepository,
            timeline = timeService.timeline,
            carbsInsulinCalculator = carbsInsulinCalculator,
            glucoseRepository = glucoseRepository,

            onAcquireBusyState = { acquireBusyState() },
            onReleaseBusyState = { releaseBusyState() },

            onDeliverBolus = { treatmentLock, amount, handledDeferredBoluses, correctionPart, basalPart ->
                when (apsMode.value) {
                    ApsMode.AutoCorrection -> {
                        therapyManager.issueBolus(
                            treatmentLock = treatmentLock,
                            amount = amount,
                            handledDeferredBoluses = handledDeferredBoluses,
                            correctionPart = correctionPart,
                            basalPart = basalPart
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
                        ApsMode.AutoCorrection, ApsMode.OnlySuggestions -> {
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

    private fun acquireBusyState() {
        wakeService.acquireBusyState(WAKE_TAG)
    }

    private fun releaseBusyState() {
        wakeService.releaseBusyState(WAKE_TAG)
    }

    override fun createForegroundServiceNotification(): Notification {
        return androidNotifications.createMainAppNotification(glucoseRepository)
    }

    override fun stop() {
        initScope?.cancel()
        initScope = null

        notificationTickHandler?.let { timeService.unregisterTickHandler(it) }
        notificationTickHandler = null

        apsCoreTickHandler?.let { timeService.unregisterTickHandler(it) }
        apsCoreTickHandler = null

        coreScope?.cancel()
        coreScope = null

        coreDispatcher?.close()
        coreDispatcher = null

        wakeService.cancelWakeup(WAKE_TAG, WAKEUP_STALE_CHECK)
        wakeService.unregisterHandler(WAKE_TAG)

        _apsIssues.value = emptySet()
        _isBgStale.value = false
        _coreState.value = CoreState.Uninitialized
        _apsMode.value = ApsMode.Suspend
        _resumeRequestedAt.value = null
    }

    override suspend fun getAssumedBg(timestamp: Timestamp): BgValue {
        return if (::core.isInitialized && coreScope != null) {
            core.getAssumedBg(timestamp)
        } else {
            BgValue.INVALID
        }
    }

    override fun getBolusCorrectionCalculator(): BolusCorrectionCalculator {
        val tm = therapyManager
        val tr = treatmentRepository
        val cic = carbsInsulinCalculator

        return if (::core.isInitialized && coreScope != null) {
            core.getBolusCorrectionCalculator()
        } else if (tm != null && tr != null && cic != null) {
            SimpleBolusCorrectionCalculator(tm, glucoseRepository)
        } else {
            NoopAlgorithm().getBolusCorrectionCalculator()
        }
    }

    override fun canOpenMealCorrectionBolus(): Boolean = when (apsMode.value) {
        ApsMode.AutoCorrection, ApsMode.OnlySuggestions -> true
        ApsMode.Suspend -> false
    }

    override fun setApsMode(mode: ApsMode) {
        val allowed = availableApsModes.value
        val validMode = if (mode in allowed) {
            mode
        } else {
            val fallback = allowed.lastOrNull() ?: ApsMode.Suspend
            Log.w(TAG, "Attempted to set unavailable APS mode $mode. Using $fallback instead.")
            fallback
        }

        val previousMode = _apsMode.value
        _apsMode.value = validMode

        // Track when transitioning out of Suspend mode to grant a grace period in AlarmEvaluator
        // while the pump processes the asynchronous resume command.
        if (previousMode == ApsMode.Suspend && validMode != ApsMode.Suspend) {
            _resumeRequestedAt.value = Timestamp.now()
        } else if (validMode == ApsMode.Suspend) {
            _resumeRequestedAt.value = null
        }

        val shouldBeSuspended = (validMode == ApsMode.Suspend)
        therapyManager?.setSuspend(shouldBeSuspended)

        scope.launch {
            val currentSettings = settingsRepository.getCurrentSettings()
            if (currentSettings != null) {
                settingsRepository.updateCurrentSettings(currentSettings.copy(apsMode = validMode))
            }
        }
    }

    private fun staleCheck() {
        _isBgStale.value = isBgStale()
    }

    fun isBgStale(): Boolean {
        val lastDataTime = glucoseSourceManager.lastInputTimestamp.value
        return lastDataTime.isInvalid() || lastDataTime + STALE_BG_THRESHOLD < Timestamp.now()
    }

    private fun nextBgStaleCheckAt(): Timestamp {
        val lastDataTime = glucoseSourceManager.lastInputTimestamp.value
        return lastDataTime.ifValidOrNow() + STALE_BG_THRESHOLD
    }

    companion object {
        private val TAG = SystemOrchestratorImpl::class.simpleName
        const val WAKE_TAG = "SystemOrchestrator"
        const val WAKEUP_STALE_CHECK = 0u
        const val DEVICE_INITIALIZATION_TIMEOUT_MS = 1000L
    }
}