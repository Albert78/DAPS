package de.dh.pump.omnipod.dash

import android.content.Context
import de.dh.daps.common.model.BasalStatus
import de.dh.daps.common.model.BolusDeliveryState
import de.dh.daps.common.model.BolusEvent
import de.dh.daps.common.model.BolusStatus
import de.dh.daps.common.model.Expiration
import de.dh.daps.common.model.ExpirationDate
import de.dh.daps.common.model.InsulinAmount
import de.dh.daps.common.model.InsulinCategory
import de.dh.daps.common.model.InsulinConcentration
import de.dh.daps.common.model.InsulinHistory
import de.dh.daps.common.model.InsulinPump
import de.dh.daps.common.model.InsulinPumpStatus
import de.dh.daps.common.model.PumpAlerts
import de.dh.daps.common.model.PumpCapabilities
import de.dh.daps.common.model.PumpHardwareInformation
import de.dh.daps.common.model.ReplaceableComponentType
import de.dh.daps.common.model.data.InsulinProfile
import de.dh.daps.common.model.data.Timestamp
import de.dh.daps.common.ui.UiText
import de.dh.pump.PumpCommandException
import de.dh.pump.PumpStatus
import de.dh.pump.omnipod.dash.db.DashHistoryDatabase
import de.dh.pump.omnipod.dash.db.DashHistoryEntity
import de.dh.pump.omnipod.dash.db.DashHistoryEventType
import de.dh.pump.omnipod.dash.model.OmnipodInsulinHistoryPoint
import de.dh.pump.omnipod.protocol.ble.OmnipodDashBleManager
import de.dh.pump.omnipod.protocol.command.GetStatusCommand
import de.dh.pump.omnipod.protocol.command.ProgramBasalCommand
import de.dh.pump.omnipod.protocol.command.ProgramBolusCommand
import de.dh.pump.omnipod.protocol.command.ProgramTempBasalCommand
import de.dh.pump.omnipod.protocol.command.StopDeliveryCommand
import de.dh.pump.omnipod.protocol.definition.BasalProgram
import de.dh.pump.omnipod.protocol.definition.ProgramReminder
import de.dh.pump.omnipod.protocol.response.DefaultStatusResponse
import de.dh.pump.omnipod.protocol.state.OmnipodDashPodStateManager
import de.dh.pump.omnipod.protocol.util.PodPulseCalculator
import de.dh.pump.omnipod.protocol.util.ProfileToBasalScheduleConverter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Date
import java.util.concurrent.TimeUnit

data class OmnipodDashStatusData(
    override val pumpSuspended: Boolean = false,
    override val batteryRemainingPercent: Int = 100,
    override val reservoirRemainingUnits: InsulinAmount = InsulinAmount.ZERO,
    override val lastSyncTimestamp: Timestamp = Timestamp.now()
) : InsulinPumpStatus

class OmnipodDashPump(
    private val podStateManager: OmnipodDashPodStateManager,
    private val bleManager: OmnipodDashBleManager,
    private val context: Context? = null
) : InsulinPump {

    override val insulinPumpId: String = PUMP_ID
    override val insulinPumpDisplayName: UiText = UiText.StringResource(R.string.omnipod_dash_pump_display_name)
    override var insulinConcentration: InsulinConcentration = InsulinConcentration.U100
    override var requestedHistoryStart: Timestamp = Timestamp.now().minusHours(InsulinPump.DEFAULT_REQUESTED_HISTORY_HOURS)

    private var sequenceNumber: Short = 1
    private var lastSyncTimestamp: Timestamp = Timestamp.now().minusHours(24)

    private var activeBolusJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _startDate = MutableStateFlow<Timestamp?>(null)
    override val startDate: StateFlow<Timestamp?> = _startDate.asStateFlow()

    private val _isExpired = MutableStateFlow(false)
    override val isExpired: StateFlow<Boolean> = _isExpired.asStateFlow()

    private val _expirations = MutableStateFlow<List<Expiration>>(emptyList())
    override val expirations: StateFlow<List<Expiration>> = _expirations.asStateFlow()

    private val _hardwareInformation = MutableStateFlow<PumpHardwareInformation?>(
        PumpHardwareInformation(
            manufacturer = "Insulet",
            model = "Omnipod Dash",
            serialNumber = podStateManager.lotNumber?.toString() ?: "-"
        )
    )
    override val hardwareInformation: StateFlow<PumpHardwareInformation?> = _hardwareInformation.asStateFlow()

    private val _pumpCapabilities = MutableStateFlow(
        PumpCapabilities(
            minBasalRate = InsulinAmount(0.05),
            supportsZeroBasal = true,
            minBasalIncrement = InsulinAmount(0.05),
            minBolusAmount = InsulinAmount(0.05),
            minBolusIncrement = InsulinAmount(0.05),
            maxBolusSize = InsulinAmount(30.0)
        )
    )
    override val pumpCapabilities: StateFlow<PumpCapabilities> = _pumpCapabilities.asStateFlow()

    private val _isConnected = MutableStateFlow(true)
    override val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _pumpStatus = MutableStateFlow<InsulinPumpStatus>(OmnipodDashStatusData())
    override val pumpStatus: StateFlow<InsulinPumpStatus> = _pumpStatus.asStateFlow()

    private val _alerts = MutableStateFlow(PumpAlerts())
    override val alerts: StateFlow<PumpAlerts> = _alerts.asStateFlow()

    private val _basalStatus = MutableStateFlow(BasalStatus())
    override val basalStatus: StateFlow<BasalStatus> = _basalStatus.asStateFlow()

    private val _bolusStatus = MutableStateFlow(BolusStatus())
    override val bolusStatus: StateFlow<BolusStatus> = _bolusStatus.asStateFlow()

    private val _bolusEvents = MutableSharedFlow<BolusEvent>(extraBufferCapacity = 64)
    override val bolusEvents: SharedFlow<BolusEvent> = _bolusEvents.asSharedFlow()

    private val _history = MutableStateFlow<InsulinHistory?>(null)
    override val history: StateFlow<InsulinHistory?> = _history.asStateFlow()

    private val db: DashHistoryDatabase? by lazy {
        context?.let { DashHistoryDatabase.getInstance(it) }
    }

    init {
        updateExpirationState()
    }

    private fun nextSequenceNumber(): Short {
        val seq = sequenceNumber
        sequenceNumber = ((sequenceNumber + 1) and 0x0F).toShort()
        return seq
    }

    private fun getNonce(): Int {
        return podStateManager.lastNonce ?: 0x49534141
    }

    private fun updateExpirationState() {
        val activationTime = podStateManager.activatedAtTimestampMs
        if (activationTime != null && activationTime > 0) {
            val start = Timestamp(activationTime)
            _startDate.value = start
            val expiresAtMs = activationTime + TimeUnit.HOURS.toMillis(POD_DURATION_HOURS)
            val expiresAt = Timestamp(expiresAtMs)
            val graceAt = Timestamp(activationTime + TimeUnit.HOURS.toMillis(POD_DURATION_HOURS + POD_GRACE_HOURS))
            val nowMs = System.currentTimeMillis()

            _isExpired.value = nowMs >= graceAt.ms
            _expirations.value = listOf(
                Expiration(
                    type = ReplaceableComponentType.Patch,
                    date = ExpirationDate.Hard(expiresAt),
                    graceUntil = graceAt
                )
            )
        }
    }

    override suspend fun bolus(amount: InsulinAmount, bolusId: String?) {
        if (_bolusStatus.value.state == BolusDeliveryState.DELIVERING) {
            throw PumpCommandException(
                status = PumpStatus.REJECTED,
                commandName = "bolus",
                vendorMessage = "A bolus delivery is already in progress"
            )
        }

        val seq = nextSequenceNumber()
        val pulseCount = PodPulseCalculator.unitsToPulses(amount.iu)

        podStateManager.createActiveCommand(
            sequenceNumber = seq,
            commandType = OmnipodDashPodStateManager.ActiveCommandType.BOLUS,
            requestedUnits = amount.iu,
            pulseCount = pulseCount
        )
        podStateManager.saveToPreferences()

        val cmd = ProgramBolusCommand(
            uniqueId = podStateManager.uniqueId ?: 0,
            sequenceNumber = seq,
            nonce = getNonce(),
            pulseCount = pulseCount
        )

        try {
            bleManager.sendCommand(cmd).getOrThrow()
            podStateManager.clearActiveCommand()
            podStateManager.saveToPreferences()

            activeBolusJob?.cancel()

            val now = Timestamp.now()
            _bolusStatus.value = BolusStatus(
                state = BolusDeliveryState.DELIVERING,
                bolusId = bolusId,
                targetAmount = amount,
                deliveredAmount = InsulinAmount.ZERO,
                timestamp = now
            )
            _bolusEvents.emit(
                BolusEvent.Started(
                    bolusId = bolusId,
                    targetAmount = amount,
                    timestamp = now
                )
            )
            logEvent(DashHistoryEventType.BOLUS, "Programmed bolus of ${amount.iu} U", amount.iu)

            val initialTotalPulses = podStateManager.pulsesDelivered
            activeBolusJob = scope.launch {
                pollBolusProgress(amount, bolusId, initialTotalPulses)
            }
        } catch (e: Exception) {
            throw e
        }
    }

    override suspend fun stopBolus() {
        activeBolusJob?.cancel()
        activeBolusJob = null

        val seq = nextSequenceNumber()
        podStateManager.createActiveCommand(
            sequenceNumber = seq,
            commandType = OmnipodDashPodStateManager.ActiveCommandType.STOP_DELIVERY
        )
        podStateManager.saveToPreferences()

        val cmd = StopDeliveryCommand(
            uniqueId = podStateManager.uniqueId ?: 0,
            sequenceNumber = seq,
            nonce = getNonce(),
            stopBasal = false,
            stopBolus = true,
            stopTempBasal = false
        )

        try {
            bleManager.sendCommand(cmd).getOrThrow()
            podStateManager.clearActiveCommand()
            podStateManager.saveToPreferences()

            var finalDelivered = _bolusStatus.value.deliveredAmount
            val current = _bolusStatus.value

            try {
                val statusCmd = GetStatusCommand(
                    uniqueId = podStateManager.uniqueId ?: 0,
                    sequenceNumber = nextSequenceNumber()
                )
                val responseBytes = bleManager.sendCommand(statusCmd).getOrNull()
                if (responseBytes != null && responseBytes.size >= 10) {
                    val statusResponse = DefaultStatusResponse(responseBytes)
                    podStateManager.updateFromDefaultStatusResponse(statusResponse)
                    val remainingPulses = statusResponse.bolusPulsesRemaining
                    val targetPulses = PodPulseCalculator.unitsToPulses(current.targetAmount.iu)
                    val deliveredPulses = (targetPulses - remainingPulses).coerceAtLeast(0).toShort()
                    finalDelivered = InsulinAmount(PodPulseCalculator.pulsesToUnits(deliveredPulses))
                }
            } catch (_: Exception) {}

            val now = Timestamp.now()
            _bolusStatus.value = current.copy(
                state = BolusDeliveryState.STOPPED,
                deliveredAmount = finalDelivered
            )
            _bolusEvents.emit(
                BolusEvent.Stopped(
                    bolusId = current.bolusId,
                    targetAmount = current.targetAmount,
                    deliveredAmount = finalDelivered,
                    timestamp = now
                )
            )
            logEvent(DashHistoryEventType.STOP_BOLUS, "Stopped running bolus at ${finalDelivered.iu} U of ${current.targetAmount.iu} U", finalDelivered.iu)
        } catch (e: Exception) {
            throw e
        }
    }

    override suspend fun tempBasal(percent: Int, durationHours: Int) {
        val seq = nextSequenceNumber()
        val pulseCount = (percent * 1).toShort()
        val durationHalfHours = (durationHours * 2).toShort()

        podStateManager.createActiveCommand(
            sequenceNumber = seq,
            commandType = OmnipodDashPodStateManager.ActiveCommandType.TEMP_BASAL,
            requestedUnits = percent / 100.0,
            pulseCount = pulseCount
        )
        podStateManager.saveToPreferences()

        val cmd = ProgramTempBasalCommand(
            uniqueId = podStateManager.uniqueId ?: 0,
            sequenceNumber = seq,
            nonce = getNonce(),
            pulseCount = pulseCount,
            durationHalfHours = durationHalfHours
        )

        try {
            bleManager.sendCommand(cmd).getOrThrow()
            podStateManager.clearActiveCommand()
            podStateManager.saveToPreferences()

            _basalStatus.value = BasalStatus(
                activeRate = InsulinAmount(percent / 100.0),
                isTempBasal = true,
                tempBasalPercent = percent,
                isSuspended = false
            )
            logEvent(DashHistoryEventType.TEMP_BASAL, "Started temp basal $percent% for $durationHours h", 0.0)
        } catch (e: Exception) {
            throw e
        }
    }

    override suspend fun cancelTempBasal() {
        val seq = nextSequenceNumber()
        podStateManager.createActiveCommand(
            sequenceNumber = seq,
            commandType = OmnipodDashPodStateManager.ActiveCommandType.STOP_DELIVERY
        )
        podStateManager.saveToPreferences()

        val cmd = StopDeliveryCommand(
            uniqueId = podStateManager.uniqueId ?: 0,
            sequenceNumber = seq,
            nonce = getNonce(),
            stopBasal = false,
            stopBolus = false,
            stopTempBasal = true
        )

        try {
            bleManager.sendCommand(cmd).getOrThrow()
            podStateManager.clearActiveCommand()
            podStateManager.saveToPreferences()

            _basalStatus.value = BasalStatus(isTempBasal = false)
            logEvent(DashHistoryEventType.CANCEL_TEMP_BASAL, "Cancelled active temp basal", 0.0)
        } catch (e: Exception) {
            throw e
        }
    }

    override suspend fun setSuspend(suspended: Boolean) {
        val seq = nextSequenceNumber()
        podStateManager.createActiveCommand(
            sequenceNumber = seq,
            commandType = OmnipodDashPodStateManager.ActiveCommandType.STOP_DELIVERY
        )
        podStateManager.saveToPreferences()

        val cmd = StopDeliveryCommand(
            uniqueId = podStateManager.uniqueId ?: 0,
            sequenceNumber = seq,
            nonce = getNonce(),
            stopBasal = suspended,
            stopBolus = suspended,
            stopTempBasal = suspended
        )

        try {
            bleManager.sendCommand(cmd).getOrThrow()
            podStateManager.clearActiveCommand()
            podStateManager.saveToPreferences()

            _basalStatus.value = _basalStatus.value.copy(isSuspended = suspended)
            _pumpStatus.value = OmnipodDashStatusData(pumpSuspended = suspended)
            logEvent(
                if (suspended) DashHistoryEventType.SUSPEND else DashHistoryEventType.RESUME,
                "Delivery suspended: $suspended",
                0.0
            )
        } catch (e: Exception) {
            throw e
        }
    }

    override suspend fun setProfile(profile: InsulinProfile) {
        val seq = nextSequenceNumber()
        val schedulePulses = ProfileToBasalScheduleConverter.convertProfileToHalfHourPulses(profile)
        val currentRate = profile.basalBlocks.firstOrNull()?.amount ?: 0.0

        podStateManager.createActiveCommand(
            sequenceNumber = seq,
            commandType = OmnipodDashPodStateManager.ActiveCommandType.BASAL_SCHEDULE
        )
        podStateManager.saveToPreferences()

        val basalProgram = createBasalProgramFromProfile(profile)
        val cmd = ProgramBasalCommand.create(
            uniqueId = podStateManager.uniqueId ?: 0,
            sequenceNumber = seq,
            nonce = getNonce(),
            basalProgram = basalProgram,
            programReminder = ProgramReminder(atStart = false, atEnd = false, atInterval = 0),
            currentTime = Date()
        )

        try {
            bleManager.sendCommand(cmd).getOrThrow()
            podStateManager.clearActiveCommand()
            podStateManager.saveToPreferences()

            _basalStatus.value = _basalStatus.value.copy(
                activeRate = InsulinAmount(currentRate),
                isTempBasal = false,
                isSuspended = false
            )
            logEvent(DashHistoryEventType.PROFILE, "Set active basal profile: ${profile.name} (${schedulePulses.size} segments)", 0.0)
        } catch (e: Exception) {
            throw e
        }
    }

    override suspend fun syncHistory() {
        refreshStatus()
        db?.let { historyDb ->
            val cutoff = requestedHistoryStart
            val entities = historyDb.historyDao().getEventsSince(cutoff.ms)

            val points = entities.map { entity ->
                OmnipodInsulinHistoryPoint(
                    timestamp = Timestamp(entity.timestampMs),
                    amount = InsulinAmount(entity.unitsDelivered),
                    category = when (entity.eventType) {
                        DashHistoryEventType.BOLUS -> InsulinCategory.Bolus
                        else -> InsulinCategory.Basal
                    },
                    pumpId = entity.id.toString()
                )
            }.sortedBy { it.timestamp }

            if (points.isNotEmpty()) {
                _history.value = InsulinHistory(
                    from = points.first().timestamp,
                    to = points.last().timestamp,
                    points = points
                )
                lastSyncTimestamp = Timestamp.now()
            } else {
                _history.value = null
            }
        }
    }

    override suspend fun refreshStatus() {
        val cmd = GetStatusCommand(
            uniqueId = podStateManager.uniqueId ?: 0,
            sequenceNumber = nextSequenceNumber()
        )
        val sendResult = bleManager.sendCommand(cmd)
        val responseBytes = sendResult.getOrThrow()

        if (responseBytes.size >= 10) {
            val statusResponse = DefaultStatusResponse(responseBytes)
            podStateManager.updateFromDefaultStatusResponse(statusResponse)

            val unconfirmedCmd = podStateManager.activeCommand
            if (unconfirmedCmd != null) {
                handleActiveCommandRecovery(unconfirmedCmd, statusResponse)
            }
        }

        _pumpStatus.value = OmnipodDashStatusData(
            pumpSuspended = podStateManager.isSuspended,
            lastSyncTimestamp = Timestamp.now()
        )
    }

    private suspend fun handleActiveCommandRecovery(
        cmd: OmnipodDashPodStateManager.ActiveCommand,
        response: DefaultStatusResponse
    ) {
        val pulsesDeliveredAtStart = cmd.pulsesDeliveredAtStart ?: 0
        val totalPulsesNow = response.totalPulsesDelivered
        val deliveredPulseDelta = totalPulsesNow - pulsesDeliveredAtStart

        val commandExecuted = deliveredPulseDelta > 0 || response.podStatus.isRunning()

        if (commandExecuted) {
            when (cmd.commandType) {
                OmnipodDashPodStateManager.ActiveCommandType.BOLUS -> {
                    val deliveredUnits = PodPulseCalculator.pulsesToUnits(deliveredPulseDelta.toShort())
                    logEvent(
                        DashHistoryEventType.BOLUS,
                        "Recovered unconfirmed bolus execution ($deliveredUnits U delivered)",
                        deliveredUnits
                    )
                }
                OmnipodDashPodStateManager.ActiveCommandType.TEMP_BASAL -> {
                    logEvent(
                        DashHistoryEventType.TEMP_BASAL,
                        "Recovered unconfirmed temp basal execution",
                        0.0
                    )
                }
                OmnipodDashPodStateManager.ActiveCommandType.STOP_DELIVERY -> {
                    logEvent(
                        DashHistoryEventType.SUSPEND,
                        "Recovered unconfirmed stop delivery execution",
                        0.0
                    )
                }
                OmnipodDashPodStateManager.ActiveCommandType.BASAL_SCHEDULE -> {
                    logEvent(
                        DashHistoryEventType.PROFILE,
                        "Recovered unconfirmed basal profile update execution",
                        0.0
                    )
                }
            }
        }
        podStateManager.clearActiveCommand()
        podStateManager.saveToPreferences()
    }

    private fun createBasalProgramFromProfile(profile: InsulinProfile): BasalProgram {
        val pulses = ProfileToBasalScheduleConverter.convertProfileToHalfHourPulses(profile)
        val segments = pulses.mapIndexed { index, pulseCount ->
            BasalProgram.Segment(
                startSlotIndex = (index).toShort(),
                endSlotIndex = (index + 1).toShort(),
                basalRateInHundredthUnitsPerHour = ((pulseCount.toDouble() / 20.0) * 100).toInt()
            )
        }
        return de.dh.pump.omnipod.protocol.definition.BasalProgram(segments)
    }

    private suspend fun pollBolusProgress(
        targetAmount: InsulinAmount,
        bolusId: String?,
        initialTotalPulses: Short?
    ) {
        val targetPulses = PodPulseCalculator.unitsToPulses(targetAmount.iu)
        var isDelivering = true
        var noProgressCount = 0
        val maxNoProgress = 60 // 60 * 2s = 120s timeout safeguard
        var lastDeliveredPulses: Short = 0

        while (isDelivering && scope.isActive) {
            delay(BOLUS_POLL_INTERVAL_MS)

            if (_bolusStatus.value.state != BolusDeliveryState.DELIVERING) {
                break
            }

            try {
                val statusCmd = GetStatusCommand(
                    uniqueId = podStateManager.uniqueId ?: 0,
                    sequenceNumber = nextSequenceNumber()
                )
                val responseBytes = bleManager.sendCommand(statusCmd).getOrNull()

                if (responseBytes != null && responseBytes.size >= 10) {
                    val statusResponse = DefaultStatusResponse(responseBytes)
                    podStateManager.updateFromDefaultStatusResponse(statusResponse)

                    val remainingPulses = statusResponse.bolusPulsesRemaining
                    val activeBolus = statusResponse.deliveryStatus.bolusDeliveringActive()

                    val deliveredPulses = if (initialTotalPulses != null && initialTotalPulses > 0) {
                        (statusResponse.totalPulsesDelivered - initialTotalPulses).coerceAtLeast(0).toShort()
                    } else {
                        (targetPulses - remainingPulses).coerceAtLeast(0).toShort()
                    }

                    val deliveredUnits = PodPulseCalculator.pulsesToUnits(deliveredPulses)
                    val deliveredAmount = InsulinAmount(deliveredUnits.coerceAtMost(targetAmount.iu))
                    val now = Timestamp.now()

                    if (deliveredPulses != lastDeliveredPulses) {
                        lastDeliveredPulses = deliveredPulses
                        noProgressCount = 0

                        _bolusStatus.value = BolusStatus(
                            state = BolusDeliveryState.DELIVERING,
                            bolusId = bolusId,
                            targetAmount = targetAmount,
                            deliveredAmount = deliveredAmount,
                            timestamp = now
                        )

                        _bolusEvents.emit(
                            BolusEvent.Progress(
                                bolusId = bolusId,
                                targetAmount = targetAmount,
                                deliveredAmount = deliveredAmount,
                                timestamp = now
                            )
                        )
                    } else {
                        noProgressCount++
                    }

                    if (!activeBolus || remainingPulses <= 0) {
                        isDelivering = false
                    }
                } else {
                    noProgressCount++
                }
            } catch (_: Exception) {
                noProgressCount++
            }

            if (noProgressCount >= maxNoProgress) {
                isDelivering = false
            }
        }

        if (_bolusStatus.value.state == BolusDeliveryState.DELIVERING) {
            val finalDelivered = if (lastDeliveredPulses >= targetPulses) {
                targetAmount
            } else {
                InsulinAmount(PodPulseCalculator.pulsesToUnits(lastDeliveredPulses))
            }
            val now = Timestamp.now()

            _bolusStatus.value = BolusStatus(
                state = BolusDeliveryState.COMPLETED,
                bolusId = bolusId,
                targetAmount = targetAmount,
                deliveredAmount = finalDelivered,
                timestamp = now
            )

            _bolusEvents.emit(
                BolusEvent.Completed(
                    bolusId = bolusId,
                    targetAmount = targetAmount,
                    deliveredAmount = finalDelivered,
                    timestamp = now
                )
            )

            logEvent(
                DashHistoryEventType.BOLUS,
                "Completed bolus delivery of ${finalDelivered.iu} U (target ${targetAmount.iu} U)",
                finalDelivered.iu
            )
        }
    }

    override fun stop() {
        _isConnected.value = false
        activeBolusJob?.cancel()
        scope.cancel()
        bleManager.disconnect()
    }

    private suspend fun logEvent(eventType: DashHistoryEventType, detailText: String, units: Double) {
        cleanupOldEvents()
        db?.historyDao()?.insertEvent(
            DashHistoryEntity(
                timestampMs = System.currentTimeMillis(),
                eventType = eventType,
                detailText = detailText,
                unitsDelivered = units
            )
        )
    }

    private suspend fun cleanupOldEvents() {
        val cutoffMs = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(HISTORY_CLEANUP_DAYS)
        db?.historyDao()?.deleteEventsOlderThan(cutoffMs)
    }

    companion object {
        const val PUMP_ID = "de.dh.daps.plugin.omnipod.dash"
        private const val POD_DURATION_HOURS = 72L
        private const val POD_GRACE_HOURS = 8L
        private const val HISTORY_CLEANUP_DAYS = 3L
        private const val BOLUS_POLL_INTERVAL_MS = 2000L
    }
}