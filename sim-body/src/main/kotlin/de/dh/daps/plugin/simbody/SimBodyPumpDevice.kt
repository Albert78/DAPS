package de.dh.daps.plugin.simbody

import android.util.Log
import de.dh.daps.common.model.InsulinAmount
import de.dh.daps.common.model.InsulinCategory
import de.dh.daps.common.model.data.InsulinProfile
import de.dh.daps.common.model.data.Timestamp
import de.dh.daps.common.model.data.getAmountForMinute
import de.dh.daps.plugin.simbody.SimBodyPumpDevice.Companion.BOLUS_SECONDS_PER_UNIT
import de.dh.daps.plugin.simbody.repository.db.PumpDao
import de.dh.daps.plugin.simbody.repository.db.PumpDeliveryType
import de.dh.daps.plugin.simbody.repository.db.PumpHistoryEntity
import de.dh.daps.plugin.simbody.repository.db.PumpStateEntity
import de.dh.pump.PumpCommandException
import de.dh.pump.PumpStatus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.time.Duration.Companion.milliseconds

/**
 * State snapshot of the bolus delivery inside the device hardware.
 */
sealed interface DeviceBolusState {
    data object Idle : DeviceBolusState

    data class Delivering(
        val bolusId: String?,
        val targetAmount: InsulinAmount,
        val deliveredAmount: InsulinAmount,
        val timestamp: Timestamp = Timestamp.now()
    ) : DeviceBolusState

    data class Completed(
        val bolusId: String?,
        val targetAmount: InsulinAmount,
        val deliveredAmount: InsulinAmount,
        val timestamp: Timestamp = Timestamp.now()
    ) : DeviceBolusState

    data class Stopped(
        val bolusId: String?,
        val targetAmount: InsulinAmount,
        val deliveredAmount: InsulinAmount,
        val reason: String? = null,
        val timestamp: Timestamp = Timestamp.now()
    ) : DeviceBolusState
}

/**
 * Represents the physical (simulated) insulin pump device.
 * This class holds the state of the hardware, including battery, insulin levels,
 * and error conditions like occlusions.
 *
 * It is responsible for reporting to the [BodyModel].
 */
class SimBodyPumpDevice(
    private val bodyModel: BodyModel,
    initialProfile: InsulinProfile,
    private val pumpDao: PumpDao? = null
) {
    companion object {
        /**
         * Duration in seconds required to deliver 1 Unit (IU) of insulin in the simulated device.
         */
        const val BOLUS_SECONDS_PER_UNIT = 12.0
    }

    private val scope = CoroutineScope(Dispatchers.IO)

    private val _deviceBolusState = MutableStateFlow<DeviceBolusState>(DeviceBolusState.Idle)
    val deviceBolusState: StateFlow<DeviceBolusState> = _deviceBolusState.asStateFlow()

    private var activeBolusJob: Job? = null

    private val _batteryLevel = MutableStateFlow(0.85) // 0.0 to 1.0
    val batteryLevel: StateFlow<Double> = _batteryLevel.asStateFlow()

    private val _reservoirLevel = MutableStateFlow(InsulinAmount(180.0)) // Units
    val reservoirLevel: StateFlow<InsulinAmount> = _reservoirLevel.asStateFlow()

    private val _isOccluded = MutableStateFlow(false)
    val isOccluded: StateFlow<Boolean> = _isOccluded.asStateFlow()

    private val _isPrimed = MutableStateFlow(true)
    val isPrimed: StateFlow<Boolean> = _isPrimed.asStateFlow()

    private val _hasHardwareError = MutableStateFlow(false)
    val hasHardwareError: StateFlow<Boolean> = _hasHardwareError.asStateFlow()

    private val _isBroken = MutableStateFlow(false)
    val isBroken: StateFlow<Boolean> = _isBroken.asStateFlow()

    private val _activeProfile = MutableStateFlow(initialProfile)
    val activeProfile: StateFlow<InsulinProfile> = _activeProfile.asStateFlow()

    // Internal history storage
    private val _history = CopyOnWriteArrayList<HistoryEntry>()

    private val _tempBasalPercent = MutableStateFlow<Int?>(null)
    val tempBasalPercent: StateFlow<Int?> = _tempBasalPercent.asStateFlow()

    private val _tempBasalExpiry = MutableStateFlow<Timestamp?>(null)
    val tempBasalExpiry: StateFlow<Timestamp?> = _tempBasalExpiry.asStateFlow()

    private var lastBasalDeliveryTimestamp: Timestamp? = null // Initialize on first tick

    fun loadState() {
        val dao = pumpDao ?: return
        scope.launch {
            try {
                dao.getPumpState()?.let { state ->
                    _batteryLevel.value = state.batteryLevel
                    _reservoirLevel.value = InsulinAmount(state.reservoirLevel)
                    _isOccluded.value = state.isOccluded
                    _isPrimed.value = state.isPrimed
                    _hasHardwareError.value = state.hasHardwareError
                    _isBroken.value = state.isBroken
                    lastBasalDeliveryTimestamp = state.lastBasalDeliveryTimestamp
                    _tempBasalPercent.value = state.tempBasalPercent
                    _tempBasalExpiry.value = state.tempBasalExpiry
                }

                val threshold = Timestamp.now().minusHours(72)
                val loadedHistory = dao.getHistorySince(threshold).map {
                    HistoryEntry(
                        id = it.id.toString(),
                        timestamp = it.timestamp,
                        amount = it.amount,
                        category = if (it.deliveryType == PumpDeliveryType.Bolus) InsulinCategory.Bolus else InsulinCategory.Basal
                    )
                }
                _history.clear()
                _history.addAll(loadedHistory)
            } catch (e: Exception) {
                Log.e("SimBodyPumpDevice", "Error loading state: ${e.message}")
            }
        }
    }

    private fun persistState() {
        val dao = pumpDao ?: return
        scope.launch {
            dao.updatePumpState(
                PumpStateEntity(
                    batteryLevel = _batteryLevel.value,
                    reservoirLevel = _reservoirLevel.value.iu,
                    isOccluded = _isOccluded.value,
                    isPrimed = _isPrimed.value,
                    hasHardwareError = _hasHardwareError.value,
                    isBroken = _isBroken.value,
                    lastBasalDeliveryTimestamp = lastBasalDeliveryTimestamp ?: Timestamp.now(),
                    tempBasalPercent = _tempBasalPercent.value,
                    tempBasalExpiry = _tempBasalExpiry.value
                )
            )
        }
    }

    fun setBatteryLevel(level: Double) {
        _batteryLevel.value = level.coerceIn(0.0, 1.0)
        persistState()
    }

    fun setReservoirLevel(amount: InsulinAmount) {
        _reservoirLevel.value = amount.coerceAtLeast(InsulinAmount.ZERO)
        persistState()
    }

    fun setOcclusion(occluded: Boolean) {
        _isOccluded.value = occluded
        persistState()
    }

    fun setPrimed(primed: Boolean) {
        _isPrimed.value = primed
        persistState()
    }

    fun setHardwareError(error: Boolean) {
        _hasHardwareError.value = error
        persistState()
    }

    fun setBroken(broken: Boolean) {
        _isBroken.value = broken
        persistState()
    }

    fun setProfile(profile: InsulinProfile) {
        _activeProfile.value = profile
    }

    fun getProfileBasalRate(timestamp: Timestamp = Timestamp.now()): Double {
        val profile = _activeProfile.value
        return profile.basalBlocks.getAmountForMinute(timestamp.minutesSinceMidnight())
    }

    /**
     * Advances the internal device state.
     */
    fun advanceTo(currentTimestamp: Timestamp) {
        handleBasal(currentTimestamp)
    }

    /**
     * Implements active basal control: delivers 1/3 of the basal rate every 20 minutes.
     * These deliveries are recorded as insulin history points.
     */
    private fun handleBasal(currentTimestamp: Timestamp) {
        val twentyMinutesMs = 20 * 60 * 1000L

        val lastBasal = lastBasalDeliveryTimestamp ?: run {
            lastBasalDeliveryTimestamp = currentTimestamp
            persistState()
            return
        }

        var currentDelivery: Timestamp = lastBasal
        while (currentTimestamp - currentDelivery >= twentyMinutesMs) {
            val deliveryTimestamp = currentDelivery.plusMinutes(20)

            // Auto-reset TBR if expired
            _tempBasalExpiry.value?.let { expiry ->
                if (deliveryTimestamp >= expiry) {
                    _tempBasalPercent.value = null
                    _tempBasalExpiry.value = null
                    persistState()
                }
            }

            val profileRate = InsulinAmount(getProfileBasalRate(deliveryTimestamp))
            val currentPercent = _tempBasalPercent.value
            val rate = if (currentPercent != null) {
                profileRate * (currentPercent / 100.0)
            } else {
                profileRate
            }

            // Deliver 1/3 of the hourly rate (since we deliver every 20 minutes)
            val basalToDeliver = rate / 3.0

            deliverInternalBolus(
                basalToDeliver,
                deliveryTimestamp,
                if (_tempBasalPercent.value != null) PumpDeliveryType.Tbr else PumpDeliveryType.Basal
            )
            currentDelivery = deliveryTimestamp
            lastBasalDeliveryTimestamp = deliveryTimestamp
            persistState()
        }
    }

    private fun deliverInternalBolus(units: InsulinAmount, timestamp: Timestamp, type: PumpDeliveryType = PumpDeliveryType.Basal) {
        // Active delivery only works if hardware is OK
        if (isBroken.value || hasHardwareError.value || isOccluded.value || !isPrimed.value) {
            return
        }
        if (units < SimBodyInsulinPump.SIM_PUMP_MIN_BOLUS_INCREMENT) { // Check against PU
            return
        }
        if (reservoirLevel.value < units) {
            return
        }

        _reservoirLevel.value = (reservoirLevel.value - units).coerceAtLeast(InsulinAmount.ZERO)
        persistState()

        // Report to body
        bodyModel.bolus(units, timestamp = timestamp)

        // Record in history as an insulin delivery
        val tempId = UUID.randomUUID().toString()
        val entry = HistoryEntry(
            id = tempId,
            timestamp = timestamp,
            amount = units,
            category = if (type == PumpDeliveryType.Bolus) InsulinCategory.Bolus else InsulinCategory.Basal
        )
        _history.add(entry)

        pumpDao?.let { dao ->
            scope.launch {
                val dbId = dao.insertHistoryEntry(PumpHistoryEntity(
                    timestamp = entry.timestamp,
                    amount = entry.amount,
                    deliveryType = type
                ))
                val index = _history.indexOf(entry)
                if (index != -1) {
                    _history[index] = entry.copy(id = dbId.toString())
                }
            }
        }

        cleanupHistory()
    }

    private fun checkGeneralErrors(commandName: String) {
        if (isBroken.value || hasHardwareError.value) {
            val vendorMessage = when {
                isBroken.value -> "Pump is broken"
                hasHardwareError.value -> "Pump hardware error"
                else -> "Pump device error"
            }
            throw PumpCommandException(
                status = PumpStatus.DEVICE_ERROR,
                commandName = commandName,
                vendorMessage = vendorMessage
            )
        }
    }

    private fun checkOcclusion(commandName: String) {
        if (isOccluded.value) {
            throw PumpCommandException(
                status = PumpStatus.REJECTED,
                commandName = commandName,
                vendorMessage = "Occlusion detected"
            )
        }
    }

    /**
     * Starts an asynchronous bolus delivery process in the device.
     * The delivery process runs based on [BOLUS_SECONDS_PER_UNIT] per Unit and updates state periodically.
     *
     * @return The [Job] representing the device's async bolus delivery process.
     * @throws PumpCommandException if initial checks fail.
     */
    fun deliverBolus(amount: InsulinAmount, bolusId: String? = null): Job {
        checkGeneralErrors("deliverBolus")
        checkOcclusion("deliverBolus")
        if (!isPrimed.value) {
            throw PumpCommandException(
                status = PumpStatus.DEVICE_ERROR,
                commandName = "deliverBolus",
                vendorMessage = "Pump not primed"
            )
        }
        if (amount < SimBodyInsulinPump.SIM_PUMP_MIN_BOLUS_INCREMENT - InsulinAmount.EPSILON) {
            throw PumpCommandException(
                status = PumpStatus.INVALID_PARAMETER,
                commandName = "deliverBolus",
                vendorMessage = "Amount below minimum increment (${SimBodyInsulinPump.SIM_PUMP_MIN_BOLUS_INCREMENT.iu} IU)"
            )
        }
        if (reservoirLevel.value < amount) {
            throw PumpCommandException(
                status = PumpStatus.REJECTED,
                commandName = "deliverBolus",
                vendorMessage = "Insulin reservoir level (${reservoirLevel.value.iu} IU) is less than requested amount (${amount.iu} IU)"
            )
        }
        if (_deviceBolusState.value is DeviceBolusState.Delivering) {
            throw PumpCommandException(
                status = PumpStatus.REJECTED,
                commandName = "deliverBolus",
                vendorMessage = "Bolus delivery already in progress"
            )
        }

        val job = scope.launch {
            executeAsyncBolus(amount, bolusId)
        }
        activeBolusJob = job
        return job
    }

    /**
     * Cancels any active bolus delivery currently running in the device.
     */
    fun stopBolus() {
        activeBolusJob?.let { job ->
            if (job.isActive) {
                job.cancel()
            }
        }
    }

    private suspend fun executeAsyncBolus(targetAmount: InsulinAmount, bolusId: String?) {
        val startTimestamp = Timestamp.now()
        var deliveredAmount = InsulinAmount.ZERO

        _deviceBolusState.value = DeviceBolusState.Delivering(
            bolusId = bolusId,
            targetAmount = targetAmount,
            deliveredAmount = deliveredAmount,
            timestamp = startTimestamp
        )

        val tickIntervalMs = 250L
        val startTimeMs = System.currentTimeMillis()
        val totalDurationMs = maxOf((targetAmount.iu * BOLUS_SECONDS_PER_UNIT * 1000.0).toLong(), 250L)

        var unreportedInsulin = 0.0
        var lastDeliveredIu = 0.0

        try {
            while (true) {
                delay(tickIntervalMs.milliseconds)

                // Check hardware state mid-delivery
                if (isBroken.value || hasHardwareError.value || isOccluded.value) {
                    val reason = when {
                        isOccluded.value -> "Occlusion detected during bolus"
                        isBroken.value -> "Pump broken during bolus"
                        else -> "Hardware error during bolus"
                    }
                    finishBolusStopped(targetAmount, deliveredAmount, bolusId, reason, unreportedInsulin)
                    return
                }

                val elapsedMs = System.currentTimeMillis() - startTimeMs
                if (elapsedMs >= totalDurationMs) {
                    break
                }

                val progressFraction = (elapsedMs.toDouble() / totalDurationMs.toDouble()).coerceIn(0.0, 1.0)
                val currentDeliveredIu = (targetAmount.iu * progressFraction).coerceIn(0.0, targetAmount.iu)
                val stepIu = currentDeliveredIu - lastDeliveredIu

                if (stepIu > 0.0) {
                    val stepAmount = InsulinAmount(stepIu)
                    _reservoirLevel.value = (_reservoirLevel.value - stepAmount).coerceAtLeast(InsulinAmount.ZERO)
                    persistState()

                    unreportedInsulin += stepIu
                    if (unreportedInsulin >= 0.05) {
                        bodyModel.bolus(InsulinAmount(unreportedInsulin), timestamp = Timestamp.now())
                        unreportedInsulin = 0.0
                    }

                    deliveredAmount = InsulinAmount(currentDeliveredIu)
                    lastDeliveredIu = currentDeliveredIu

                    _deviceBolusState.value = DeviceBolusState.Delivering(
                        bolusId = bolusId,
                        targetAmount = targetAmount,
                        deliveredAmount = deliveredAmount,
                        timestamp = Timestamp.now()
                    )
                }
            }

            // Finish remaining insulin for exact match
            val remainingIu = targetAmount.iu - lastDeliveredIu
            if (remainingIu > 0.0) {
                val remainingAmount = InsulinAmount(remainingIu)
                _reservoirLevel.value = (_reservoirLevel.value - remainingAmount).coerceAtLeast(InsulinAmount.ZERO)
                persistState()
                unreportedInsulin += remainingIu
            }

            if (unreportedInsulin > 0.0) {
                bodyModel.bolus(InsulinAmount(unreportedInsulin), timestamp = Timestamp.now())
                unreportedInsulin = 0.0
            }

            deliveredAmount = targetAmount
            val completedTimestamp = Timestamp.now()

            recordBolusHistory(deliveredAmount, completedTimestamp)

            _deviceBolusState.value = DeviceBolusState.Completed(
                bolusId = bolusId,
                targetAmount = targetAmount,
                deliveredAmount = deliveredAmount,
                timestamp = completedTimestamp
            )

        } catch (e: CancellationException) {
            finishBolusStopped(targetAmount, deliveredAmount, bolusId, "Bolus stopped by user", unreportedInsulin)
            throw e
        } catch (e: Exception) {
            finishBolusStopped(targetAmount, deliveredAmount, bolusId, e.message ?: "Error during bolus delivery", unreportedInsulin)
        }
    }

    private fun finishBolusStopped(
        targetAmount: InsulinAmount,
        deliveredAmount: InsulinAmount,
        bolusId: String?,
        reason: String,
        unreportedInsulin: Double
    ) {
        if (unreportedInsulin > 0.0) {
            bodyModel.bolus(InsulinAmount(unreportedInsulin), timestamp = Timestamp.now())
        }
        val stoppedTimestamp = Timestamp.now()
        if (deliveredAmount > InsulinAmount.ZERO) {
            recordBolusHistory(deliveredAmount, stoppedTimestamp)
        }
        _deviceBolusState.value = DeviceBolusState.Stopped(
            bolusId = bolusId,
            targetAmount = targetAmount,
            deliveredAmount = deliveredAmount,
            reason = reason,
            timestamp = stoppedTimestamp
        )
    }

    private fun recordBolusHistory(amount: InsulinAmount, timestamp: Timestamp) {
        if (amount < SimBodyInsulinPump.SIM_PUMP_MIN_BOLUS_INCREMENT) {
            return
        }
        val tempId = UUID.randomUUID().toString()
        val entry = HistoryEntry(
            id = tempId,
            timestamp = timestamp,
            amount = amount,
            category = InsulinCategory.Bolus
        )
        _history.add(entry)

        pumpDao?.let { dao ->
            scope.launch {
                val dbId = dao.insertHistoryEntry(
                    PumpHistoryEntity(
                        timestamp = entry.timestamp,
                        amount = entry.amount,
                        deliveryType = PumpDeliveryType.Bolus
                    )
                )
                val index = _history.indexOf(entry)
                if (index != -1) {
                    _history[index] = entry.copy(id = dbId.toString())
                }
            }
        }
        cleanupHistory()
    }

    /**
     * Sets or clears temporary basal percentage.
     * Throws [PumpCommandException] if updating temporary basal fails.
     */
    fun updateTempBasalPercent(percent: Int?, durationHours: Int? = null) {
        checkGeneralErrors("updateTempBasalPercent")
        if ((percent != null) != (durationHours != null)) {
            throw PumpCommandException(
                status = PumpStatus.INVALID_PARAMETER,
                commandName = "updateTempBasalPercent",
                vendorMessage = "Percent and durationHours must both be specified or both be null"
            )
        }
        if (percent != null && percent !in 0..500) {
            throw PumpCommandException(
                status = PumpStatus.INVALID_PARAMETER,
                commandName = "updateTempBasalPercent",
                vendorMessage = "Percent must be between 1 and 500 (was $percent)"
            )
        }
        if (durationHours != null && durationHours !in 1..24) {
            throw PumpCommandException(
                status = PumpStatus.INVALID_PARAMETER,
                commandName = "updateTempBasalPercent",
                vendorMessage = "Duration must be between 1 and 24 hours (was $durationHours)"
            )
        }

        _tempBasalPercent.value = percent
        if (percent != null && durationHours != null) {
            _tempBasalExpiry.value = Timestamp.now().plusHours(durationHours)
        } else {
            _tempBasalExpiry.value = null
        }
        persistState()
    }

    fun getHistory(): List<HistoryEntry> = _history.toList()

    private fun cleanupHistory() {
        val threeDaysAgo = Timestamp.now().minusHours(72)
        _history.removeIf { it.timestamp < threeDaysAgo }

        pumpDao?.let { dao ->
            scope.launch {
                dao.deleteOldHistory(threeDaysAgo)
            }
        }
    }

    /**
     * Replaces the reservoir and resets levels.
     */
    fun replaceReservoir(amount: InsulinAmount = InsulinAmount(300.0)) {
        _reservoirLevel.value = amount
        _isPrimed.value = false // Need to prime after reservoir change
        persistState()
    }

    /**
     * Performs priming of the catheter.
     */
    fun primeCatheter(): Boolean {
        val primeAmount = InsulinAmount(10.0)
        if (reservoirLevel.value >= primeAmount) {
            _reservoirLevel.value -= primeAmount // Priming uses some insulin
            _isPrimed.value = true
            persistState()
            return true
        }
        return false
    }

    data class HistoryEntry(
        val id: String? = null,
        val timestamp: Timestamp,
        val amount: InsulinAmount,
        val category: InsulinCategory
    )
}