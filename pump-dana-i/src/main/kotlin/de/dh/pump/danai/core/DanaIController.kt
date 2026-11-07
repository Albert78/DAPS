package de.dh.pump.danai.core

import de.dh.daps.common.domain.model.data.Minutes
import de.dh.daps.common.domain.model.data.Timestamp
import de.dh.daps.common.domain.model.data.getAmountForMinute
import de.dh.daps.common.domain.model.therapy.InsulinAmount
import de.dh.daps.common.domain.model.therapy.InsulinCategory
import de.dh.daps.common.domain.model.therapy.InsulinConcentration
import de.dh.daps.common.domain.model.therapy.InsulinHistoryPoint
import de.dh.daps.common.domain.model.therapy.InsulinProfile
import de.dh.daps.common.domain.pump.BasalStatus
import de.dh.daps.common.domain.pump.BolusDeliveryState
import de.dh.daps.common.domain.pump.BolusEvent
import de.dh.daps.common.domain.pump.BolusStatus
import de.dh.daps.common.domain.pump.PumpAlerts
import de.dh.daps.common.domain.pump.PumpCapabilities
import de.dh.pump.PumpCommandException
import de.dh.pump.PumpConnectionException
import de.dh.pump.PumpStatus
import de.dh.pump.PumpTimestamp
import de.dh.pump.client.singleframe.ProtocolFrame
import de.dh.pump.client.singleframe.PumpClient
import de.dh.pump.client.singleframe.PumpResponse
import de.dh.pump.dana.commands.DanaRsCommands
import de.dh.pump.dana.commands.aps.ApsHistoryEvent
import de.dh.pump.dana.commands.aps.ApsHistoryEventKind
import de.dh.pump.dana.commands.aps.ApsHistoryEventsResult
import de.dh.pump.dana.commands.general.DanaRsPumpErrorState
import de.dh.pump.dana.commands.options.DanaRsUserOptions
import de.dh.pump.dana.commands.options.OptionUserOptionsResponse
import de.dh.pump.dana.notifications.DanaAlarmNotification
import de.dh.pump.dana.notifications.DanaDeliveryCompleteNotification
import de.dh.pump.dana.notifications.DanaDeliveryRateDisplayNotification
import de.dh.pump.dana.notifications.DanaMissedBolusAlarmNotification
import de.dh.pump.dana.notifications.NotificationParsers
import de.dh.pump.dana.protocol.DanaRsBleEncryption
import de.dh.pump.danai.core.connection.DanaILink
import de.dh.pump.danai.core.connection.SessionState
import de.dh.pump.danai.core.model.DanaIBolusSpeed
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Calendar
import kotlin.time.Duration.Companion.seconds

/**
 * Throws a [PumpCommandException] if the response status is not [PumpStatus.OK].
 */
fun PumpResponse.ensureOk(commandName: String) {
    if (status != PumpStatus.OK) {
        throw PumpCommandException(status, commandName)
    }
}

/**
 * Orchestrator that connects the domain model [DanaIPump] with the technical [DanaILink].
 *
 * It handles command execution by automatically establishing connections and
 * updates the pump model with fresh data while a session is active.
 */
class DanaIController(
    val pump: DanaIPump,
    val link: DanaILink,
    private val logger: DanaILogger,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main),
) {
    private val commands = DanaRsCommands()
    private val actionMutex = Mutex()
    private var sessionHandlerJob: Job? = null
    private var lastApsHistoryTimestamp: Timestamp = Timestamp.now().minusHours(24)
    private var basalTickerJob: Job? = null
    private var lastAppliedProfile: InsulinProfile? = null

    private data class ActiveBolusContext(
        val bolusId: String?,
        val targetAmount: InsulinAmount,
        val startTime: Timestamp = Timestamp.now(),
    )

    private var activeBolusContext: ActiveBolusContext? = null
    private val danaPreferences = DanaPumpPreferences(link.preferences)

    private fun restoreActiveBolusContext() {
        scope.launch {
            val targetAmount = danaPreferences.getActiveBolusTargetAmount()
            val startTime = danaPreferences.getActiveBolusStartTime()
            val bolusId = danaPreferences.getActiveBolusId()

            if (targetAmount != null && startTime != null) {
                val restored = ActiveBolusContext(
                    bolusId = bolusId,
                    targetAmount = InsulinAmount.fromPumpUnits(targetAmount, pump.insulinConcentration.value),
                    startTime = Timestamp(startTime),
                )
                activeBolusContext = restored
                logger.log("Restored active bolus context from storage: bolusId=${restored.bolusId}, targetAmount=${restored.targetAmount}")
                pump.updateBolusStatus(
                    BolusStatus(
                        state = BolusDeliveryState.DELIVERING,
                        bolusId = restored.bolusId,
                        targetAmount = restored.targetAmount,
                        deliveredAmount = InsulinAmount.ZERO,
                        timestamp = restored.startTime,
                    )
                )
            }
        }
    }

    private suspend fun setActiveBolusContext(context: ActiveBolusContext?) {
        activeBolusContext = context
        danaPreferences.saveActiveBolus(
            bolusId = context?.bolusId,
            targetAmount = context?.targetAmount?.toPumpUnits(pump.insulinConcentration.value),
            startTime = context?.startTime?.ms,
        )
    }

    /**
     * The device name (usually the serial number) of the associated pump.
     */
    val deviceName = link.deviceName

    /**
     * The underlying Bluetooth device object.
     */
    val device = link.device

    /**
     * Technical link status.
     */
    val linkStatus = link.status

    /**
     * Technical session state.
     */
    val sessionState = link.sessionState

    /**
     * Information about the most recent technical link error.
     */
    val lastError = link.lastError

    /**
     * Flow indicating whether connection failed due to a stale OS Bluetooth bond.
     */
    val requiresStaleBondRemoval = link.requiresStaleBondRemoval

    init {
        restoreActiveBolusContext()

        // Wire up the pump's action callbacks
        pump.onBolus = { amount, speed, bolusId -> executeBolusStart(amount, speed, bolusId) }
        pump.onTempBasal = { percent, duration -> executeTempBasal(percent, duration) }
        pump.onStopBolus = { executeStopBolus() }
        pump.onCancelTempBasal = { executeCancelTempBasal() }
        pump.onSetSuspend = { suspended -> executeSetSuspend(suspended) }
        pump.onSetProfile = { profile -> executeSetProfile(profile) }
        pump.onSyncHistory = { start -> executeSyncHistory(start) }
        pump.onRefreshStatus = { executeRefreshStatus() }
        pump.onGetUserOptions = { executeGetUserOptions() }
        pump.onSetUserOptions = { options -> executeSetUserOptions(options) }
        pump.onExecute = { command -> withConnection { it.execute(command) } }
        pump.onExecuteStream = { command -> withConnection { client -> client.executeStream(command) as Any } }
        pump.onDisconnectForMaintenance = {
            scope.launch {
                try {
                    setMaintenanceUserOptions(true)
                } catch (e: Exception) {
                    logger.log("Failed to configure options for maintenance mode: ${e.message}")
                } finally {
                    disconnect()
                }
            }
        }
        pump.onConnectFromMaintenance = {
            scope.launch {
                try {
                    connect()
                    setMaintenanceUserOptions(false)
                    executeRefreshStatus()
                    val profileToRestore = lastAppliedProfile
                    if (profileToRestore != null) {
                        logger.log("Restoring basal profile after maintenance mode...")
                        executeSetProfile(profileToRestore)
                    }
                } catch (e: Exception) {
                    logger.log("Failed to reconnect after maintenance: ${e.message}")
                }
            }
        }

        // Observe technical sessions to handle notifications and polling while connected
        scope.launch {
            link.sessionState.collect { state ->
                handleSessionStateChange(state)
            }
        }
    }

    /**
     * Manually initiates a technical connection.
     */
    suspend fun connect() {
        link.connect()
    }

    /**
     * Manually terminates the technical connection.
     */
    suspend fun disconnect() {
        link.disconnect()
    }

    private suspend fun handleSessionStateChange(state: SessionState) {
        if (state is SessionState.Connected) {
            startSessionHandler(state.client)
            startBasalTicker()
            pump.setConnected(true)
        } else if (state.isClosed) {
            stopSessionHandler()
            stopBasalTicker()
            pump.setConnected(false)
        }
    }

    private fun startSessionHandler(client: PumpClient) {
        sessionHandlerJob?.cancel()
        sessionHandlerJob = scope.launch {
            // Notification Listener
            launch {
                client.incomingFrames
                    .filter { it.flags == DanaRsBleEncryption.DANAR_PACKET__TYPE_NOTIFY }
                    .collect { frame ->
                        processNotificationFrame(frame, client)
                    }
            }

            // Initial Status Update
            try {
                updateHardwareStatus(client)
                syncPumpTime(client)
                updateUserOptions(client)
                updatePumpStatus(client)
            } catch (e: Exception) {
                logger.log("Initial status update failed: ${e.message}")
            }
        }
    }

    private suspend fun stopSessionHandler() {
        sessionHandlerJob?.cancelAndJoin()
        sessionHandlerJob = null
    }

    /**
     * Helper that ensures an active technical connection and provides the client.
     */
    private suspend fun <T> withConnection(ignoreMaintenanceMode: Boolean = false, block: suspend (PumpClient) -> T): T = actionMutex.withLock {
        if (pump.isMaintenanceMode.value && !ignoreMaintenanceMode) {
            throw PumpConnectionException("Pump is in maintenance mode")
        }
        link.connect()
        val state = link.sessionState.value
        if (state !is SessionState.Connected) throw PumpConnectionException("Failed to connect to pump device")
        block(state.client)
    }

    private suspend fun updateHardwareStatus(client: PumpClient) {
        if (pump.hardware.value != null) return

        logger.log("Fetching hardware information...")
        val pumpCheck = client.execute(commands.generalGetPumpCheck())
        val shippingVersion = client.execute(commands.generalGetShippingVersion())
        val shippingInfo = client.execute(commands.generalGetShippingInformation())

        pump.updateHardware(
            DanaPumpHardwareData(
                hardwareModel = pumpCheck.hardwareModel,
                protocol = pumpCheck.protocol,
                productCode = pumpCheck.productCode,
                bleModel = shippingVersion.bleModel,
                serialNumber = shippingInfo.serialNumber,
                shippingCountry = shippingInfo.shippingCountry,
                shippingDate = shippingInfo.shippingDate,
            ),
        )
    }

    private suspend fun syncPumpTime(client: PumpClient) {
        try {
            val sysTime = System.currentTimeMillis()
            val zoneOffsetSeconds = ZonedDateTime.now().offset.totalSeconds / 3600
            val response = client.execute(commands.optionSetPumpUtcAndTimeZone(sysTime, zoneOffsetSeconds))
            if (response.status == PumpStatus.OK) {
                logger.log("Successfully synchronized pump time with system time.")
            }
            updateTimeOffset(client)
        } catch (e: Exception) {
            logger.log("Failed to sync pump time: ${e.message}")
        }
    }

    private suspend fun updateTimeOffset(client: PumpClient) {
        try {
            val sysTime = System.currentTimeMillis()
            val response = client.execute(commands.optionGetPumpUtcAndTimeZone())
            if (response.status == PumpStatus.OK) {
                val pumpUtcMillis = response.pumpUtcTime.toInstant(ZoneOffset.UTC).toEpochMilli()
                pump.pumpTimeOffsetMs = pumpUtcMillis - sysTime
                logger.log("Updated pump time offset: ${pump.pumpTimeOffsetMs} ms (Pump UTC: ${response.pumpUtcTime})")
            }
        } catch (e: Exception) {
            logger.log("Failed to query pump time offset: ${e.message}")
        }
    }

    private suspend fun updatePumpStatus(client: PumpClient) {
        updateTimeOffset(client)
        val status = client.execute(commands.generalInitialScreenInformation())

        // Update general status data
        pump.updateStatus(
            DanaPumpStatusData(
                batteryRemainingPercent = status.batteryRemainingPercent,
                reservoirRemainingUnits = InsulinAmount.fromPumpUnits(status.reservoirRemainingUnits, pump.insulinConcentration.value),
                dailyTotalUnits = status.dailyTotalUnits,
                lastSyncTimestamp = Timestamp.now(),
            ),
        )

        // Update error states
        val domainErrors = status.errorStates.map { protocolError ->
            when (protocolError) {
                DanaRsPumpErrorState.SUSPENDED -> DanaPumpError.Suspended
                DanaRsPumpErrorState.DAILY_MAX -> DanaPumpError.DailyTotalMaxExceeded
                DanaRsPumpErrorState.BOLUS_BLOCK -> DanaPumpError.BolusBlocked
                DanaRsPumpErrorState.ORDER_DELIVERING -> DanaPumpError.CommandInProgress
                DanaRsPumpErrorState.NO_PRIME -> DanaPumpError.NotPrimed
            }
        }.toSet()
        pump.updateErrorStates(domainErrors)

        // Update alerts
        pump.updateAlerts(
            PumpAlerts(
                batteryLow = status.batteryRemainingPercent <= WARNING_THRESHOLD_BATTERY_REMAINING_PERCENT,
                reservoirLow = InsulinAmount.fromPumpUnits(status.reservoirRemainingUnits, pump.insulinConcentration.value) <= WARNING_THRESHOLD_RESERVOIR_REMAINING_UNITS,
            ),
        )

        // Calculate actual active rate based on TBR percentage
        val actualRate = if (status.tempBasalInProgress) {
            status.currentBasalUnitsPerHour * (status.tempBasalPercent / 100.0)
        } else {
            status.currentBasalUnitsPerHour
        }

        pump.updateBasal(
            BasalStatus(
                activeRate = InsulinAmount.fromPumpUnits(actualRate, pump.insulinConcentration.value),
                isTempBasal = status.tempBasalInProgress,
                tempBasalPercent = if (status.tempBasalInProgress) status.tempBasalPercent else null,
                isSuspended = status.pumpSuspended,
            ),
        )

        if (pump.pumpCapabilities.value == null) {
            val basalInfo = client.execute(commands.basalGetBasalRate())
            val bolusInfo = client.execute(commands.bolusGetStepBolusInformation())

            val minBolusIncrement =
                InsulinAmount.fromPumpUnits(bolusInfo.bolusStepUnits, pump.insulinConcentration.value)
            pump.updatePumpCapabilities(
                PumpCapabilities(
                    minBasalRate = DANA_I_MIN_BASAL_RATE,
                    supportsZeroBasal = true,
                    // TODO: Check if the values from the commands really have this meaning
                    minBasalIncrement = if (basalInfo.basalStepSupported) InsulinAmount.fromPumpUnits(basalInfo.basalStepUnits, pump.insulinConcentration.value) else DANA_I_MIN_BASAL_INCREMENT,
                    minBolusAmount = minBolusIncrement,
                    minBolusIncrement = minBolusIncrement,
                    maxBolusSize = InsulinAmount.fromPumpUnits(bolusInfo.maxBolusUnits, pump.insulinConcentration.value)
                )
            )

            pump.updateHourlyBasalRates(basalInfo.hourlyRatesUnits)
        }

        // Check offline bolus reconciliation
        val isDelivering = status.errorStates.contains(DanaRsPumpErrorState.ORDER_DELIVERING)
        val context = activeBolusContext

        if (context != null && !isDelivering) {
            logger.log("Active bolus (${context.bolusId}) finished while offline/disconnected. Reconciling with history...")
            performApsHistorySync(client)

            val matchingRecord = pump.history.value?.points?.find { record ->
                record.category == InsulinCategory.Bolus && record.timestamp >= context.startTime.minusSeconds(60)
            }

            val now = Timestamp.now()
            if (matchingRecord != null) {
                val delivered = matchingRecord.amount
                if (delivered >= context.targetAmount) {
                    val statusObj = BolusStatus(
                        BolusDeliveryState.COMPLETED,
                        context.bolusId,
                        context.targetAmount,
                        delivered,
                        now
                    )
                    pump.updateBolusStatus(statusObj)
                    pump.emitBolusEvent(BolusEvent.Completed(context.bolusId, context.targetAmount, delivered, now))
                } else {
                    val statusObj = BolusStatus(
                        BolusDeliveryState.STOPPED,
                        context.bolusId,
                        context.targetAmount,
                        delivered,
                        now
                    )
                    pump.updateBolusStatus(statusObj)
                    pump.emitBolusEvent(BolusEvent.Stopped(context.bolusId, context.targetAmount, delivered, now))
                }
            } else {
                val statusObj = BolusStatus(
                    BolusDeliveryState.COMPLETED,
                    context.bolusId,
                    context.targetAmount,
                    context.targetAmount,
                    now
                )
                pump.updateBolusStatus(statusObj)
                pump.emitBolusEvent(BolusEvent.Completed(context.bolusId, context.targetAmount, context.targetAmount, now))
            }

            setActiveBolusContext(null)
        }
    }

    private fun startBasalTicker() {
        stopBasalTicker()
        basalTickerJob = scope.launch {
            logger.log("Starting synthetic basal ticker...")
            while (true) {
                val nowMs = System.currentTimeMillis()
                val zdt = ZonedDateTime.ofInstant(Instant.ofEpochMilli(nowMs), ZoneId.systemDefault())
                val currentMinute = zdt.minute

                val nextSlotMinute = ((currentMinute / 4) + 1) * 4
                val targetZdt = if (nextSlotMinute >= 60) {
                    zdt.plusHours(1).withMinute(0).withSecond(0).withNano(0)
                } else {
                    zdt.withMinute(nextSlotMinute).withSecond(0).withNano(0)
                }
                val delayMs = maxOf(500L, targetZdt.toInstant().toEpochMilli() - nowMs)

                kotlinx.coroutines.delay(delayMs)
                onBasalTickerTick()
            }
        }
    }

    private fun stopBasalTicker() {
        basalTickerJob?.cancel()
        basalTickerJob = null
    }

    private fun onBasalTickerTick() {
        try {
            val now = Timestamp.now()
            val currentStatus = pump.basalStatus.value

            if (currentStatus.isSuspended) {
                return
            }

            val activeRateUnits = currentStatus.activeRate.toPumpUnits(pump.insulinConcentration.value)
            if (activeRateUnits <= 0.0) {
                return
            }

            val zdt = ZonedDateTime.ofInstant(Instant.ofEpochMilli(now.ms), ZoneId.systemDefault())
            val isLowRate = activeRateUnits < 0.09

            if (isLowRate) {
                if (zdt.minute == 56) {
                    val slotTimestamp = normalizeToBasalSlot(now, isLowRateAt56 = true)
                    val pulseAmount = InsulinAmount.fromPumpUnits(activeRateUnits, pump.insulinConcentration.value)
                    val pulseId = formatBasalPulseId(slotTimestamp)
                    val point = DanaInsulinHistoryPoint(
                        timestamp = slotTimestamp,
                        amount = pulseAmount,
                        category = InsulinCategory.Basal,
                        pumpId = pulseId
                    )
                    logger.log("Basal ticker: Emitting low-rate pulse (minute 56) $activeRateUnits U (pumpId=$pulseId)")
                    pump.updateBasalHistory(listOf(point))
                }
            } else {
                val slotTimestamp = normalizeToBasalSlot(now, isLowRateAt56 = false)
                val pulseAmount = InsulinAmount.fromPumpUnits(activeRateUnits / 15.0, pump.insulinConcentration.value)
                val pulseId = formatBasalPulseId(slotTimestamp)
                val point = DanaInsulinHistoryPoint(
                    timestamp = slotTimestamp,
                    amount = pulseAmount,
                    category = InsulinCategory.Basal,
                    pumpId = pulseId
                )
                logger.log("Basal ticker: Emitting 4-min pulse ${pulseAmount.iu} U (pumpId=$pulseId)")
                pump.updateBasalHistory(listOf(point))
            }
        } catch (e: Exception) {
            logger.log("Error in basal ticker: ${e.message}")
        }
    }

    private suspend fun performApsHistorySync(client: PumpClient, start: Timestamp = pump.lastRequestedStart) {
        logger.log("Syncing APS history...")

        try {
            val startTimestamp = maxOf(lastApsHistoryTimestamp, start)
            val queryPumpTime = PumpTimestamp.fromSystemTimestamp(startTimestamp, pump.pumpTimeOffsetMs)

            client.execute(commands.generalSetHistoryUploadMode(1))
            val result = try {
                client.executeStream(
                    commands.apsHistoryEvents(
                        fromTime = queryPumpTime,
                        zoneId = ZoneOffset.UTC,
                        useUtcLayout = true
                    )
                ) as ApsHistoryEventsResult
            } finally {
                client.execute(commands.generalSetHistoryUploadMode(0))
            }

            val events = result.events.sortedBy { it.timestamp }
            logger.log("Fetched ${events.size} APS history events.")

            val bolusPoints = events.mapNotNull { event ->
                if (isBolusEvent(event.kind)) {
                    val sysTime = event.timestamp.toSystemTimestamp(pump.pumpTimeOffsetMs)
                    val amount = event.insulinUnits ?: 0.0
                    if (amount > 0.0) {
                        DanaInsulinHistoryPoint(
                            timestamp = sysTime,
                            amount = InsulinAmount.fromPumpUnits(amount, pump.insulinConcentration.value),
                            category = InsulinCategory.Bolus,
                            pumpId = event.pumpId.toString()
                        )
                    } else null
                } else null
            }
            if (bolusPoints.isNotEmpty()) {
                pump.updateBolusHistory(bolusPoints, start)
            }

            val basalPoints = reconstructBasalPulsesFromApsEvents(
                events = events,
                startTime = startTimestamp,
                endTime = Timestamp.now(),
                pumpTimeOffsetMs = pump.pumpTimeOffsetMs,
                concentration = pump.insulinConcentration.value,
                currentBasalStatus = pump.basalStatus.value,
                hourlyBasalRates = pump.hourlyBasalRates.value
            )
            if (basalPoints.isNotEmpty()) {
                pump.updateBasalHistory(basalPoints, start)
            }

            if (events.isNotEmpty()) {
                val maxPumpTime = events.maxOf { it.timestamp }.toSystemTimestamp(pump.pumpTimeOffsetMs)
                lastApsHistoryTimestamp = maxPumpTime.plusMs(1)
            }
            logger.log("APS history sync completed: ${bolusPoints.size} boluses, ${basalPoints.size} basal pulses.")
        } catch (e: Exception) {
            logger.log("APS history sync failed: ${e.message}")
        }
    }

    private fun isBolusEvent(kind: ApsHistoryEventKind): Boolean = when (kind) {
        ApsHistoryEventKind.BOLUS,
        ApsHistoryEventKind.DUAL_BOLUS,
        ApsHistoryEventKind.EXTENDED_START,
        ApsHistoryEventKind.DUAL_EXTENDED_START -> true
        else -> false
    }

    internal fun reconstructBasalPulsesFromApsEvents(
        events: List<ApsHistoryEvent>,
        startTime: Timestamp,
        endTime: Timestamp,
        pumpTimeOffsetMs: Long,
        concentration: InsulinConcentration,
        currentBasalStatus: BasalStatus,
        hourlyBasalRates: List<Double>
    ): List<InsulinHistoryPoint> {
        if (startTime >= endTime) return emptyList()

        val systemEvents = events.map { event ->
            event to event.timestamp.toSystemTimestamp(pumpTimeOffsetMs)
        }.sortedBy { it.second }

        var currentBaseRateUnits: Double? = null
        var activeTempPercent: Int? = null
        var tempEndTime: Timestamp? = null
        var isSuspended = false

        // Initialize state from past events prior to startTime if any
        for ((event, eventTime) in systemEvents) {
            if (eventTime > startTime) break
            when (event.kind) {
                ApsHistoryEventKind.PROFILE_CHANGE -> {
                    currentBaseRateUnits = event.currentRateUnitsPerHour ?: (event.param2 / 100.0)
                }
                ApsHistoryEventKind.TEMP_START -> {
                    activeTempPercent = event.ratioPercent ?: event.param1
                    val durationMin = event.durationMinutes ?: event.param2
                    tempEndTime = eventTime.plusMinutes(durationMin)
                }
                ApsHistoryEventKind.TEMP_STOP -> {
                    activeTempPercent = null
                    tempEndTime = null
                }
                ApsHistoryEventKind.SUSPEND_ON -> {
                    isSuspended = true
                }
                ApsHistoryEventKind.SUSPEND_OFF -> {
                    isSuspended = false
                }
                else -> {}
            }
        }

        val points = mutableListOf<InsulinHistoryPoint>()
        var slot = normalizeToBasalSlot(startTime)
        val endSlot = normalizeToBasalSlot(endTime)
        var eventIndex = 0

        while (slot <= endSlot) {
            // Apply all events that occurred up to slot
            while (eventIndex < systemEvents.size && systemEvents[eventIndex].second <= slot) {
                val (event, eventTime) = systemEvents[eventIndex]
                when (event.kind) {
                    ApsHistoryEventKind.PROFILE_CHANGE -> {
                        currentBaseRateUnits = event.currentRateUnitsPerHour ?: (event.param2 / 100.0)
                    }
                    ApsHistoryEventKind.TEMP_START -> {
                        activeTempPercent = event.ratioPercent ?: event.param1
                        val durationMin = event.durationMinutes ?: event.param2
                        tempEndTime = eventTime.plusMinutes(durationMin)
                    }
                    ApsHistoryEventKind.TEMP_STOP -> {
                        activeTempPercent = null
                        tempEndTime = null
                    }
                    ApsHistoryEventKind.SUSPEND_ON -> {
                        isSuspended = true
                    }
                    ApsHistoryEventKind.SUSPEND_OFF -> {
                        isSuspended = false
                    }
                    else -> {}
                }
                eventIndex++
            }

            // Check temp basal expiry
            if (tempEndTime != null && slot >= tempEndTime) {
                activeTempPercent = null
                tempEndTime = null
            }

            val slotZdt = ZonedDateTime.ofInstant(Instant.ofEpochMilli(slot.ms), ZoneId.systemDefault())
            val baseRate = currentBaseRateUnits ?: hourlyBasalRates.getOrNull(slotZdt.hour) ?: currentBasalStatus.activeRate.toPumpUnits(concentration)

            val activeRate = if (isSuspended) {
                0.0
            } else if (activeTempPercent != null) {
                baseRate * (activeTempPercent / 100.0)
            } else {
                baseRate
            }

            if (activeRate >= 0.09) {
                val pulseAmount = InsulinAmount.fromPumpUnits(activeRate / 15.0, concentration)
                val pulseId = formatBasalPulseId(slot)
                points.add(
                    DanaInsulinHistoryPoint(
                        timestamp = slot,
                        amount = pulseAmount,
                        category = InsulinCategory.Basal,
                        pumpId = pulseId
                    )
                )
            } else if (activeRate > 0.0) {
                if (slotZdt.minute == 56) {
                    val slot56Timestamp = normalizeToBasalSlot(slot, isLowRateAt56 = true)
                    val pulseAmount = InsulinAmount.fromPumpUnits(activeRate, concentration)
                    val pulseId = formatBasalPulseId(slot56Timestamp)
                    points.add(
                        DanaInsulinHistoryPoint(
                            timestamp = slot56Timestamp,
                            amount = pulseAmount,
                            category = InsulinCategory.Basal,
                            pumpId = pulseId
                        )
                    )
                }
            }

            slot = slot.plusMs(4 * 60 * 1000L)
        }

        return points
    }

    private suspend fun processNotificationFrame(frame: ProtocolFrame, client: PumpClient) {
        val notification = NotificationParsers.parse(frame) ?: return

        when (notification) {
            is DanaAlarmNotification -> {
                logger.log("Pump Alarm received: ${notification.alarmCode} (0x${"%02X".format(notification.alarmCode.code)})")
                updatePumpStatus(client)
            }

            is DanaDeliveryCompleteNotification -> {
                val context = activeBolusContext
                val bolusId = context?.bolusId
                val delivered = InsulinAmount.fromPumpUnits(notification.deliveredInsulinUnits, pump.insulinConcentration.value)
                val targetAmount = context?.targetAmount ?: delivered
                val now = Timestamp.now()

                logger.log("Delivery complete notification (${delivered.iu} U, bolusId=$bolusId). Syncing history...")

                val completedStatus = BolusStatus(
                    state = BolusDeliveryState.COMPLETED,
                    bolusId = bolusId,
                    targetAmount = targetAmount,
                    deliveredAmount = delivered,
                    timestamp = now,
                )
                pump.updateBolusStatus(completedStatus)
                pump.emitBolusEvent(BolusEvent.Completed(bolusId, targetAmount, delivered, now))

                setActiveBolusContext(null)

                performApsHistorySync(client)
                updatePumpStatus(client)
            }

            is DanaDeliveryRateDisplayNotification -> {
                val context = activeBolusContext
                val bolusId = context?.bolusId
                val targetAmount = context?.targetAmount ?: InsulinAmount.ZERO
                val delivered = InsulinAmount.fromPumpUnits(notification.deliveredInsulinUnits, pump.insulinConcentration.value)
                val now = Timestamp.now()

                logger.log("Delivery progress: ${delivered.iu} U (bolusId=$bolusId)")

                val progressStatus = BolusStatus(
                    state = BolusDeliveryState.DELIVERING,
                    bolusId = bolusId,
                    targetAmount = targetAmount,
                    deliveredAmount = delivered,
                    timestamp = now,
                )
                pump.updateBolusStatus(progressStatus)
                pump.emitBolusEvent(BolusEvent.Progress(bolusId, targetAmount, delivered, now))
            }

            is DanaMissedBolusAlarmNotification -> {
                // Reminder-timer if bolus windows are configured in the pump.
                // Ignored in APS system.
                logger.log("Missed bolus alarm: ${notification.startHour}:${notification.startMinute} - ${notification.endHour}:${notification.endMinute}")
            }
        }
    }

    /**
     * Executes a bolus delivery asynchronously.
     *
     * @throws PumpConnectionException if the technical connection fails.
     * @throws PumpCommandException if the pump rejects the bolus request.
     */
    private suspend fun executeBolusStart(
        amount: InsulinAmount,
        speed: DanaIBolusSpeed,
        bolusId: String? = null,
    ) = withConnection { client ->
        val pumpUnits = amount.toPumpUnits(pump.insulinConcentration.value)
        logger.log("Action: Executing Bolus $pumpUnits U (bolusId=$bolusId)")
        val now = Timestamp.now()
        val context = ActiveBolusContext(bolusId, amount, now)
        setActiveBolusContext(context)

        val status = BolusStatus(
            state = BolusDeliveryState.DELIVERING,
            bolusId = bolusId,
            targetAmount = amount,
            deliveredAmount = InsulinAmount.ZERO,
            timestamp = now,
        )
        pump.updateBolusStatus(status)
        pump.emitBolusEvent(BolusEvent.Started(bolusId, amount, now))

        try {
            val cmd = commands.bolusSetStepBolusStart(pumpUnits, speed.toDanaRsBolusSpeed())
            client.execute(cmd).ensureOk(cmd.name)
            // Attention: Don't issue more commands on the pump since complex commands will cause the bolus
            // on the pump to be canceled (e.g. performHistorySync).
            // Instead, rely on DELIVERY_COMPLETE notification.
        } catch (e: Exception) {
            setActiveBolusContext(null)
            val errorNow = Timestamp.now()
            val stoppedStatus = BolusStatus(
                state = BolusDeliveryState.STOPPED,
                bolusId = bolusId,
                targetAmount = amount,
                deliveredAmount = InsulinAmount.ZERO,
                timestamp = errorNow,
            )
            pump.updateBolusStatus(stoppedStatus)
            pump.emitBolusEvent(BolusEvent.Stopped(bolusId, amount, InsulinAmount.ZERO, errorNow))
            throw e
        }
    }

    /**
     * Sets a temporary basal rate.
     *
     * @throws PumpConnectionException if the technical connection fails.
     * @throws PumpCommandException if the pump rejects the request.
     */
    private suspend fun executeTempBasal(percent: Int, durationHours: Int) = withConnection { client ->
        logger.log("Action: Executing Temp Basal $percent% ($durationHours h)")

        try {
            // If a Temp Basal is already running, we must cancel it first
            logger.log("Try to cancel Temp Basal first...")
            client.execute(commands.basalSetCancelTemporaryBasal())
        } catch (_: Exception) {
            // Ignore
        }

        val cmd = commands.basalSetTemporaryBasal(percent, durationHours)
        client.execute(cmd).ensureOk(cmd.name)
        performApsHistorySync(client)
        updatePumpStatus(client)
    }

    /**
     * Stops any currently running bolus.
     *
     * @throws PumpConnectionException if the technical connection fails.
     * @throws PumpCommandException if the pump rejects the stop request.
     */
    private suspend fun executeStopBolus() = withConnection { client ->
        logger.log("Action: Stopping Bolus")
        val context = activeBolusContext
        val bolusId = context?.bolusId
        val targetAmount = context?.targetAmount ?: InsulinAmount.ZERO
        val currentDelivered = pump.bolusStatus.value.deliveredAmount
        val now = Timestamp.now()

        val cmd = commands.bolusSetStepBolusStop()
        client.execute(cmd).ensureOk(cmd.name)

        val stoppedStatus = BolusStatus(
            state = BolusDeliveryState.STOPPED,
            bolusId = bolusId,
            targetAmount = targetAmount,
            deliveredAmount = currentDelivered,
            timestamp = now,
        )
        pump.updateBolusStatus(stoppedStatus)
        pump.emitBolusEvent(BolusEvent.Stopped(bolusId, targetAmount, currentDelivered, now))

        setActiveBolusContext(null)
        updatePumpStatus(client)
    }

    /**
     * Cancels the active temporary basal rate.
     *
     * @throws PumpConnectionException if the technical connection fails.
     * @throws PumpCommandException if the pump rejects the cancel request.
     */
    private suspend fun executeCancelTempBasal() = withConnection { client ->
        logger.log("Action: Canceling Temp Basal")
        val cmd = commands.basalSetCancelTemporaryBasal()
        client.execute(cmd).ensureOk(cmd.name)
        performApsHistorySync(client)
        updatePumpStatus(client)
    }

    /**
     * Sets the suspend state of the pump hardware.
     *
     * @throws PumpConnectionException if the technical connection fails.
     * @throws PumpCommandException if the pump rejects the request.
     */
    private suspend fun executeSetSuspend(suspended: Boolean) = withConnection { client ->
        logger.log("Action: Setting Suspend to $suspended")
        val cmd = if (suspended) commands.basalSetSuspendOn() else commands.basalSetSuspendOff()
        client.execute(cmd).ensureOk(cmd.name)
        performApsHistorySync(client)
        updatePumpStatus(client)
    }

    /**
     * Sets the active therapy profile on the pump.
     *
     * @throws PumpConnectionException if the technical connection fails.
     * @throws PumpCommandException if the pump rejects the profile request.
     */
    private suspend fun executeSetProfile(profile: InsulinProfile) = withConnection { client ->
        logger.log("Action: Setting profile '${profile.name}'")

        val hourlyRates = (0..23).map { hour ->
            val amount = profile.basalBlocks.getAmountForMinute(Minutes.ofHours(hour))
            InsulinAmount(amount).toPumpUnits(pump.insulinConcentration.value)
        }

        val activeProfileRes = client.execute(commands.basalGetProfileNumber())
        val activeProfileNum = activeProfileRes.activeProfile

        val cmdSPBR = commands.basalSetProfileBasalRate(activeProfileNum, hourlyRates)
        client.execute(cmdSPBR, timeout = 15.seconds).ensureOk(cmdSPBR.name)

        // It seems to be necessary to issue this command after setting the set basal rate command
        val cmdSPN = commands.basalSetProfileNumber(activeProfileNum)
        client.execute(cmdSPN).ensureOk(cmdSPN.name)

        lastAppliedProfile = profile

        updatePumpStatus(client)
    }

    private suspend fun executeSyncHistory(start: Timestamp) = withConnection { client ->
        updateTimeOffset(client)
        performApsHistorySync(client, start)
    }

    private suspend fun executeRefreshStatus() = withConnection { client ->
        updatePumpStatus(client)
    }

    private suspend fun updateUserOptions(client: PumpClient) {
        try {
            logger.log("Fetching pump user options...")
            val response = client.execute(commands.optionGetUserOption())
            if (response.status == PumpStatus.OK) {
                val hwModel = pump.hardware.value?.hardwareModel ?: 7
                var currentOptions = response.toUserOptions(hwModel)
                logger.log("Fetched pump user options: LCD=${currentOptions.lcdOnTimeSec}s, Backlight=${currentOptions.backlightOnTimeSec}s")

                val targetBacklightTime = CONNECTED_BACKLIGHT_ON_TIME_SEC
                if (currentOptions.backlightOnTimeSec != targetBacklightTime) {
                    val updatedOptions = currentOptions.copy(
                        backlightOnTimeSec = targetBacklightTime,
                    )
                    logger.log("Auto-configuring pump backlight setting upon connection: Backlight=$targetBacklightTime s (Off)")
                    val setRes = client.execute(commands.optionSetUserOption(updatedOptions))
                    if (setRes.status == PumpStatus.OK) {
                        currentOptions = updatedOptions
                    } else {
                        logger.log("Failed to auto-configure backlight setting: status=${setRes.status}")
                    }
                }
                pump.updateUserOptions(currentOptions)
            }
        } catch (e: Exception) {
            logger.log("Failed to update user options: ${e.message}")
        }
    }

    private suspend fun setMaintenanceUserOptions(maintenanceModeOn: Boolean) = withConnection(ignoreMaintenanceMode = true) { client ->
        logger.log("Configuring pump user options for maintenance mode...")
        val currentOptions = pump.userOptions.value ?: try {
            val response = client.execute(commands.optionGetUserOption())
            if (response.status == PumpStatus.OK) {
                val hwModel = pump.hardware.value?.hardwareModel ?: 7
                response.toUserOptions(hwModel)
            } else null
        } catch (e: Exception) {
            logger.log("Failed to fetch user options for maintenance mode: ${e.message}")
            null
        }

        if (currentOptions != null) {
            val maintenanceOptions = if (maintenanceModeOn)
                currentOptions.copy(backlightOnTimeSec = MAINTENANCE_BACKLIGHT_ON_TIME_SEC)
            else
                currentOptions.copy(backlightOnTimeSec = 0)
            logger.log("Setting pump backlight to $MAINTENANCE_BACKLIGHT_ON_TIME_SEC s for maintenance mode...")
            val response = client.execute(commands.optionSetUserOption(maintenanceOptions))
            if (response.status == PumpStatus.OK) {
                pump.updateUserOptions(maintenanceOptions)
            } else {
                logger.log("Failed to set backlight for maintenance mode: status=${response.status}")
            }
        }
    }

    private suspend fun executeGetUserOptions(): DanaRsUserOptions = withConnection { client ->
        logger.log("Action: Getting user options")
        val response = client.execute(commands.optionGetUserOption())
        response.ensureOk("OPTION_GET_USER_OPTION")
        val hwModel = pump.hardware.value?.hardwareModel ?: 7
        val options = response.toUserOptions(hwModel)
        pump.updateUserOptions(options)
        options
    }

    private suspend fun executeSetUserOptions(options: DanaRsUserOptions) = withConnection { client ->
        logger.log("Action: Setting user options (LCD=${options.lcdOnTimeSec}s, Backlight=${options.backlightOnTimeSec}s)")
        val cmd = commands.optionSetUserOption(options)
        val response = client.execute(cmd)
        response.ensureOk(cmd.name)
        pump.updateUserOptions(options)
    }

    private fun OptionUserOptionsResponse.toUserOptions(hwModel: Int = 7): DanaRsUserOptions {
        return DanaRsUserOptions(
            hwModel = hwModel,
            timeDisplayType24 = timeDisplayType24,
            buttonScrollOnOff = buttonScrollOnOff,
            beepAndAlarm = beepAndAlarm,
            lcdOnTimeSec = lcdOnTimeSec,
            backlightOnTimeSec = backlightOnTimeSec,
            selectedLanguage = selectedLanguage,
            units = units,
            shutdownHour = shutdownHour,
            lowReservoirRate = lowReservoirRate,
            cannulaVolume = cannulaVolume,
            refillAmount = refillAmount,
            target = target ?: 100,
        )
    }

    /**
     * Terminates all background activities of this controller.
     */
    fun stop() {
        stopBasalTicker()
        scope.launch { stopSessionHandler() }
        // Unwire callbacks to avoid leaks
        pump.onBolus = null
        pump.onTempBasal = null
        pump.onStopBolus = null
        pump.onCancelTempBasal = null
        pump.onSetProfile = null
        pump.onSyncHistory = null
        pump.onRefreshStatus = null
        pump.onGetUserOptions = null
        pump.onSetUserOptions = null
        pump.onExecute = null
        pump.onExecuteStream = null
    }

    companion object {
        private val BASAL_PULSE_ID_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH:mm")

        fun formatBasalPulseId(timestamp: Timestamp): String {
            val zdt = ZonedDateTime.ofInstant(Instant.ofEpochMilli(timestamp.ms), ZoneId.systemDefault())
            return "basal_pulse_${zdt.format(BASAL_PULSE_ID_FORMATTER)}"
        }

        fun normalizeToBasalSlot(timestamp: Timestamp, isLowRateAt56: Boolean = false): Timestamp {
            val zdt = ZonedDateTime.ofInstant(Instant.ofEpochMilli(timestamp.ms), ZoneId.systemDefault())
            val normalizedZdt = if (isLowRateAt56) {
                zdt.withMinute(56).withSecond(0).withNano(0)
            } else {
                val minute = (zdt.minute / 4) * 4
                zdt.withMinute(minute).withSecond(0).withNano(0)
            }
            return Timestamp(normalizedZdt.toInstant().toEpochMilli())
        }
    }
}