package de.dh.pump.danai.core

import de.dh.daps.common.model.BasalStatus
import de.dh.daps.common.model.BolusDeliveryState
import de.dh.daps.common.model.BolusEvent
import de.dh.daps.common.model.BolusStatus
import de.dh.daps.common.model.InsulinAmount
import de.dh.daps.common.model.InsulinCategory
import de.dh.daps.common.model.InsulinConcentration
import de.dh.daps.common.model.InsulinHistoryPoint
import de.dh.daps.common.model.PumpAlerts
import de.dh.daps.common.model.PumpCapabilities
import de.dh.daps.common.model.data.InsulinProfile
import de.dh.daps.common.model.data.Minutes
import de.dh.daps.common.model.data.PumpTimestamp
import de.dh.daps.common.model.data.Timestamp
import de.dh.daps.common.model.data.getAmountForMinute
import de.dh.pump.PumpClient
import de.dh.pump.PumpCommandException
import de.dh.pump.PumpConnectionException
import de.dh.pump.PumpStatus
import de.dh.pump.commands.PumpResponse
import de.dh.pump.dana.commands.DanaRsCommands
import de.dh.pump.dana.commands.general.DanaRsPumpErrorState
import de.dh.pump.dana.commands.history.DanaRsHistoryRecord
import de.dh.pump.dana.notifications.DanaAlarmNotification
import de.dh.pump.dana.notifications.DanaDeliveryCompleteNotification
import de.dh.pump.dana.notifications.DanaDeliveryRateDisplayNotification
import de.dh.pump.dana.notifications.DanaMissedBolusAlarmNotification
import de.dh.pump.dana.notifications.NotificationParsers
import de.dh.pump.dana.protocol.DanaRsBleEncryption
import de.dh.pump.danai.core.connection.DanaILink
import de.dh.pump.danai.core.connection.SessionState
import de.dh.pump.danai.core.model.DanaIBolusSpeed
import de.dh.pump.protocol.ProtocolFrame
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.ZoneOffset
import java.util.Calendar

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
    private val pump: DanaIPump,
    private val link: DanaILink,
    private val logger: DanaILogger,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main),
) {
    private val commands = DanaRsCommands()
    private val actionMutex = Mutex()
    private var sessionHandlerJob: Job? = null
    private var lastBasalHistoryTimestamp: Timestamp = Timestamp.now().minusHours(24)
    private var lastBolusHistoryTimestamp: Timestamp = Timestamp.now().minusHours(24)

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
                    targetAmount = InsulinAmount.fromPumpUnits(targetAmount, pump.insulinConcentration),
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
            targetAmount = context?.targetAmount?.toPumpUnits(pump.insulinConcentration),
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

    init {
        restoreActiveBolusContext()

        // Wire up the pump's action callbacks
        pump.onBolus = { amount, speed, bolusId -> executeBolusStart(amount, speed, bolusId) }
        pump.onTempBasal = { percent, duration -> executeTempBasal(percent, duration) }
        pump.onStopBolus = { executeStopBolus() }
        pump.onCancelTempBasal = { executeCancelTempBasal() }
        pump.onSetSuspend = { suspended -> executeSetSuspend(suspended) }
        pump.onSetProfile = { profile -> executeSetProfile(profile) }
        pump.onSyncHistory = { executeSyncHistory() }
        pump.onRefreshStatus = { executeRefreshStatus() }
        pump.onExecute = { command -> withConnection { it.execute(command) } }
        pump.onExecuteStream = { command -> withConnection { client -> client.executeStream(command) as Any } }

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
            pump.setConnected(true)
        } else if (state.isClosed) {
            stopSessionHandler()
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
                updatePumpStatus(client)
            } catch (e: Exception) {
                logger.log("Initial status update failed: ${e.message}")
            }

            // We don't want to poll the status here, since this must be done from an upper layer
//            while (true) {
//                delay(30.seconds)
//                try {
//                    updatePumpStatus(client)
//                } catch (e: Exception) {
//                    logger.log("Session poll failed: ${e.message}")
//                }
//            }
        }
    }

    private suspend fun stopSessionHandler() {
        sessionHandlerJob?.cancelAndJoin()
        sessionHandlerJob = null
    }

    /**
     * Helper that ensures an active technical connection and provides the client.
     */
    private suspend fun <T> withConnection(block: suspend (PumpClient) -> T): T = actionMutex.withLock {
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
                reservoirRemainingUnits = InsulinAmount.fromPumpUnits(status.reservoirRemainingUnits, pump.insulinConcentration),
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
                reservoirLow = InsulinAmount.fromPumpUnits(status.reservoirRemainingUnits, pump.insulinConcentration) <= WARNING_THRESHOLD_RESERVOIR_REMAINING_UNITS,
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
                activeRate = InsulinAmount.fromPumpUnits(actualRate, pump.insulinConcentration),
                isTempBasal = status.tempBasalInProgress,
                tempBasalPercent = if (status.tempBasalInProgress) status.tempBasalPercent else null,
                isSuspended = status.pumpSuspended,
            ),
        )

        if (pump.pumpCapabilities.value == null) {
            val basalInfo = client.execute(commands.basalGetBasalRate())
            val bolusInfo = client.execute(commands.bolusGetStepBolusInformation())

            val minBolusIncrement =
                InsulinAmount.fromPumpUnits(bolusInfo.bolusStepUnits, pump.insulinConcentration)
            pump.updatePumpCapabilities(
                PumpCapabilities(
                    minBasalRate = DANA_I_MIN_BASAL_RATE,
                    supportsZeroBasal = true,
                    // TODO: Check if the values from the commands really have this meaning
                    minBasalIncrement = if (basalInfo.basalStepSupported) InsulinAmount.fromPumpUnits(basalInfo.basalStepUnits, pump.insulinConcentration) else DANA_I_MIN_BASAL_INCREMENT,
                    minBolusAmount = minBolusIncrement,
                    minBolusIncrement = minBolusIncrement,
                    maxBolusSize = InsulinAmount.fromPumpUnits(bolusInfo.maxBolusUnits, pump.insulinConcentration)
                )
            )

            pump.updateHourlyBasalRates(basalInfo.hourlyRatesUnits)
        }

        // Check offline bolus reconciliation
        val isDelivering = status.errorStates.contains(DanaRsPumpErrorState.ORDER_DELIVERING)
        val context = activeBolusContext

        if (context != null && !isDelivering) {
            logger.log("Active bolus (${context.bolusId}) finished while offline/disconnected. Reconciling with history...")
            performBolusHistorySync(client)

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

    private suspend fun performBasalHistorySync(client: PumpClient) {
        logger.log("Syncing basal history...")

        try {
            val queryPumpTime = PumpTimestamp.fromSystemTimestamp(lastBasalHistoryTimestamp, pump.pumpTimeOffsetMs)
            val history = client.executeStream(commands.historyBasal(queryPumpTime))
            val points = mapBasalRecordsToHistoryPoints(
                records = history.records,
                pumpTimeOffsetMs = pump.pumpTimeOffsetMs,
                concentration = pump.insulinConcentration
            )

            if (points.isNotEmpty()) {
                pump.updateBasalHistory(points)
                val maxSystemTimestamp = history.records.maxOf { it.timestamp }.toSystemTimestamp(pump.pumpTimeOffsetMs)
                lastBasalHistoryTimestamp = maxSystemTimestamp.plusMs(1)
            }
            logger.log("Basal history sync completed: ${history.records.size} records converted to ${points.size} history points.")
        } catch (e: Exception) {
            logger.log("Basal history sync failed: ${e.message}")
        }
    }

    private fun mapBasalRecordsToHistoryPoints(
        records: List<DanaRsHistoryRecord>,
        pumpTimeOffsetMs: Long,
        concentration: InsulinConcentration
    ): List<InsulinHistoryPoint> {
        if (records.isEmpty()) return emptyList()

        val sortedRecords = records.sortedBy { it.timestamp }
        val points = ArrayList<InsulinHistoryPoint>()

        for (i in sortedRecords.indices) {
            val record = sortedRecords[i]
            val startTime = record.timestamp.toSystemTimestamp(pumpTimeOffsetMs)
            val unitsPerHour = record.value ?: 0.0
            val nextTime = if (i + 1 < sortedRecords.size) {
                sortedRecords[i + 1].timestamp.toSystemTimestamp(pumpTimeOffsetMs)
            } else {
                startTime.plusHours(1)
            }

            if (unitsPerHour <= 0.09) {
                val cal = Calendar.getInstance().apply {
                    timeInMillis = startTime.ms
                    set(Calendar.MINUTE, 56)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val targetTimestamp = if (cal.timeInMillis < startTime.ms) startTime else Timestamp(cal.timeInMillis)

                points.add(
                    DanaInsulinHistoryPoint(
                        timestamp = targetTimestamp,
                        amount = InsulinAmount.fromPumpUnits(unitsPerHour, concentration),
                        category = InsulinCategory.Basal,
                        pumpId = "${record.recordCode}_1"
                    )
                )
            } else {
                val intervalMs = 4 * 60 * 1000L
                val amountPerPulse = InsulinAmount.fromPumpUnits(
                    unitsPerHour / 15.0,
                    concentration
                )
                var currentPulseTime = startTime
                var seq = 1

                while (currentPulseTime < nextTime) {
                    points.add(
                        DanaInsulinHistoryPoint(
                            timestamp = currentPulseTime,
                            amount = amountPerPulse,
                            category = InsulinCategory.Basal,
                            pumpId = "${record.recordCode}_$seq"
                        )
                    )
                    currentPulseTime = currentPulseTime.plusMs(intervalMs)
                    seq++
                }
            }
        }
        return points
    }

    private suspend fun performBolusHistorySync(client: PumpClient) {
        logger.log("Syncing bolus history...")

        try {
            val queryPumpTime = PumpTimestamp.fromSystemTimestamp(lastBolusHistoryTimestamp, pump.pumpTimeOffsetMs)
            val history = client.executeStream(commands.historyBolus(queryPumpTime))
            val points = history.records.map { record ->
                DanaInsulinHistoryPoint(
                    timestamp = record.timestamp.toSystemTimestamp(pump.pumpTimeOffsetMs),
                    amount = InsulinAmount.fromPumpUnits(record.value ?: 0.0, pump.insulinConcentration),
                    category = InsulinCategory.Bolus,
                    pumpId = record.recordCode.toString()
                )
            }
            if (points.isNotEmpty()) {
                pump.updateBolusHistory(points)
                val maxSystemTimestamp = history.records.maxOf { it.timestamp }.toSystemTimestamp(pump.pumpTimeOffsetMs)
                lastBolusHistoryTimestamp = maxSystemTimestamp.plusMs(1)
            }
            logger.log("Bolus history sync completed: ${points.size} records.")
        } catch (e: Exception) {
            logger.log("Bolus history sync failed: ${e.message}")
        }
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
                val delivered = InsulinAmount.fromPumpUnits(notification.deliveredInsulinUnits, pump.insulinConcentration)
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

                performBolusHistorySync(client)
                updatePumpStatus(client)
            }

            is DanaDeliveryRateDisplayNotification -> {
                val context = activeBolusContext
                val bolusId = context?.bolusId
                val targetAmount = context?.targetAmount ?: InsulinAmount.ZERO
                val delivered = InsulinAmount.fromPumpUnits(notification.deliveredInsulinUnits, pump.insulinConcentration)
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
        val pumpUnits = amount.toPumpUnits(pump.insulinConcentration)
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
        performBasalHistorySync(client)
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

        if (profile.insulinConcentration != pump.insulinConcentration) {
            pump.insulinConcentration = profile.insulinConcentration
        }

        val hourlyRates = (0..23).map { hour ->
            val amount = profile.basalBlocks.getAmountForMinute(Minutes.ofHours(hour))
            InsulinAmount(amount).toPumpUnits(pump.insulinConcentration)
        }

        val activeProfileRes = client.execute(commands.basalGetProfileNumber())
        val activeProfileNum = activeProfileRes.activeProfile

        val cmd = commands.basalSetProfileBasalRate(activeProfileNum, hourlyRates)
        client.execute(cmd).ensureOk(cmd.name)

        updatePumpStatus(client)
    }

    private suspend fun executeSyncHistory() = withConnection { client ->
        updateTimeOffset(client)
        performBasalHistorySync(client)
        performBolusHistorySync(client)
    }

    private suspend fun executeRefreshStatus() = withConnection { client ->
        updatePumpStatus(client)
    }

    /**
     * Terminates all background activities of this controller.
     */
    fun stop() {
        scope.launch { stopSessionHandler() }
        // Unwire callbacks to avoid leaks
        pump.onBolus = null
        pump.onTempBasal = null
        pump.onStopBolus = null
        pump.onCancelTempBasal = null
        pump.onSetProfile = null
        pump.onSyncHistory = null
        pump.onRefreshStatus = null
        pump.onExecute = null
        pump.onExecuteStream = null
    }
}