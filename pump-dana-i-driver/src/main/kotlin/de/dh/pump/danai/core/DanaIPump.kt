package de.dh.pump.danai.core

import de.dh.pump.PumpCommandException
import de.dh.pump.PumpConnectionException
import de.dh.pump.commands.PumpCommand
import de.dh.pump.commands.PumpResponse
import de.dh.pump.commands.PumpStreamCommand
import de.dh.pump.danai.core.model.DanaIBolusSpeed
import de.dh.daps.common.model.BasalStatus
import de.dh.daps.common.model.BolusEvent
import de.dh.daps.common.model.BolusStatus
import de.dh.daps.common.model.InsulinAmount
import de.dh.daps.common.model.InsulinCategory
import de.dh.daps.common.model.InsulinConcentration
import de.dh.daps.common.model.InsulinHistory
import de.dh.daps.common.model.InsulinHistoryPoint
import de.dh.daps.common.model.InsulinPumpStatus
import de.dh.daps.common.model.PumpAlerts
import de.dh.daps.common.model.PumpCapabilities
import de.dh.daps.common.model.data.InsulinProfile
import de.dh.daps.common.model.data.Timestamp
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate

/**
 * Domain data representing the static hardware information of a Dana-i pump.
 */
data class DanaPumpHardwareData(
    val hardwareModel: Int,
    val protocol: Int,
    val productCode: Int,
    val bleModel: String,
    val serialNumber: String,
    val shippingCountry: String,
    val shippingDate: LocalDate,
)

/**
 * Domain data representing the general status of a Dana-i pump.
 */
data class DanaPumpStatusData(
    override val pumpSuspended: Boolean = false,
    override val batteryRemainingPercent: Int = 0,
    override val reservoirRemainingUnits: InsulinAmount = InsulinAmount.ZERO,
    val dailyTotalUnits: Double = 0.0,
    override val lastSyncTimestamp: Timestamp = Timestamp.INVALID,
) : InsulinPumpStatus

/**
 * Snapshot of an insulin delivery point.
 */
data class DanaInsulinHistoryPoint(
    override val timestamp: Timestamp,
    override val amount: InsulinAmount,
    override val category: InsulinCategory,
    override val pumpId: String? = null
) : InsulinHistoryPoint

/**
 * Domain-level error states representing pump restrictions.
 * See page 84 in manual.
 */
enum class DanaPumpError {
    Suspended,
    DailyTotalMaxExceeded,

    /**
     * Bolus is blocked for some time because of bolus block function (see Dana-i Professional Manual).
     */
    BolusBlocked,
    CommandInProgress,
    NotPrimed,
}

/**
 * High-level domain model for a Dana-i pump.
 *
 * This class holds the last known status, provides flows for alerts, basal delivery,
 * and bolus results, and allows issuing functional commands (orders) to the pump.
 */
class DanaIPump {
    /**
     * Concentration of the insulin loaded in the pump. Default is [InsulinConcentration.U100].
     */
    var insulinConcentration: InsulinConcentration = InsulinConcentration.U100

    /**
     * Difference in milliseconds between pump UTC time and system time (pumpUtcMillis - systemMillis).
     */
    var pumpTimeOffsetMs: Long = 0L

    private val _hardware = MutableStateFlow<DanaPumpHardwareData?>(null)

    /**
     * Static hardware information of the pump. Null if not yet retrieved.
     */
    val hardware = _hardware.asStateFlow()

    private val _isConnected = MutableStateFlow<Boolean>(false)
    val isConnected = _isConnected.asStateFlow()

    private val _pumpCapabilities = MutableStateFlow<PumpCapabilities?>(null)

    /**
     * Pump capabilities information. Null if not yet retrieved.
     */
    var pumpCapabilities = _pumpCapabilities.asStateFlow()

    private val _status = MutableStateFlow(DanaPumpStatusData())

    /**
     * General status and data of the pump (Battery, Reservoir, etc.).
     * Set in [refreshStatus].
     */
    val status = _status.asStateFlow()

    private val _alerts = MutableStateFlow(PumpAlerts())

    /**
     * Current state of normalized pump alerts. If one or more alerts are set, the user should check the pump.
     * Set in [refreshStatus].
     */
    val alerts = _alerts.asStateFlow()

    private val _errorStates = MutableStateFlow<Set<DanaPumpError>>(emptySet())

    /**
     * Dana specific errors and restrictions from the pump. Can provide more detailed description
     * for the general alerts.
     * Set in [refreshStatus].
     */
    val errorStates = _errorStates.asStateFlow()

    private val _basalStatus = MutableStateFlow(BasalStatus())

    /**
     * Current state of the basal insulin delivery.
     */
    val basalStatus = _basalStatus.asStateFlow()

    private val _hourlyBasalRates = MutableStateFlow<List<Double>>(emptyList())

    val hourlyBasalRates = _hourlyBasalRates.asStateFlow()

    private var currentBasalPoints = emptyList<InsulinHistoryPoint>()
    private var currentBolusPoints = emptyList<InsulinHistoryPoint>()

    private val _history = MutableStateFlow<InsulinHistory?>(null)

    /**
     * History of insulin deliveries (basal and bolus) for the last 24 hours.
     * Sorted by timestamp ascending.
     * Updated by [syncHistory].
     */
    val history: StateFlow<InsulinHistory?> = _history.asStateFlow()

    private val _bolusStatus = MutableStateFlow(BolusStatus())

    /**
     * Current status of active or recent bolus delivery.
     */
    val bolusStatus = _bolusStatus.asStateFlow()

    private val _bolusEvents = MutableSharedFlow<BolusEvent>(extraBufferCapacity = 64)

    /**
     * Real-time event stream for bolus delivery lifecycle updates.
     */
    val bolusEvents: SharedFlow<BolusEvent> = _bolusEvents.asSharedFlow()

    // Internal actions to be implemented by the Controller
    internal var onBolus: (suspend (InsulinAmount, DanaIBolusSpeed, String?) -> Unit)? = null
    internal var onTempBasal: (suspend (Int, Int) -> Unit)? = null
    internal var onStopBolus: (suspend () -> Unit)? = null
    internal var onCancelTempBasal: (suspend () -> Unit)? = null
    internal var onSetSuspend: (suspend (Boolean) -> Unit)? = null
    internal var onSetProfile: (suspend (InsulinProfile) -> Unit)? = null
    internal var onSyncHistory: (suspend () -> Unit)? = null
    internal var onRefreshStatus: (suspend () -> Unit)? = null
    internal var onExecute: (suspend (PumpCommand<*>) -> PumpResponse)? = null
    internal var onExecuteStream: (suspend (PumpStreamCommand<*, *>) -> Any)? = null

    /**
     * Initiates a bolus delivery with an optional tracking [bolusId].
     *
     * @throws PumpConnectionException if the technical connection fails.
     * @throws PumpCommandException if the command is rejected by the pump hardware.
     */
    suspend fun bolus(
        amount: InsulinAmount,
        speed: DanaIBolusSpeed = DanaIBolusSpeed.U12_SECONDS,
        bolusId: String? = null,
    ) {
        onBolus?.invoke(amount, speed, bolusId)
    }

    /**
     * Starts a temporary basal rate in percent.
     *
     * @throws PumpConnectionException if the technical connection fails.
     * @throws PumpCommandException if the command is rejected by the pump hardware.
     */
    suspend fun tempBasal(percent: Int, durationHours: Int) {
        onTempBasal?.invoke(percent, durationHours)
    }

    /**
     * Immediately stops any currently running bolus delivery.
     *
     * @throws PumpConnectionException if the technical connection fails.
     * @throws PumpCommandException if the command is rejected by the pump hardware.
     */
    suspend fun stopBolus() {
        onStopBolus?.invoke()
    }

    /**
     * Cancels the currently active temporary basal rate.
     *
     * @throws PumpConnectionException if the technical connection fails.
     * @throws PumpCommandException if the command is rejected by the pump hardware.
     */
    suspend fun cancelTempBasal() {
        onCancelTempBasal?.invoke()
    }

    /**
     * Sets the suspend state of the pump.
     *
     * @param suspended True to suspend insulin delivery, false to resume.
     * @throws PumpConnectionException if the technical connection fails.
     * @throws PumpCommandException if the command is rejected by the pump hardware.
     */
    suspend fun setSuspend(suspended: Boolean) {
        onSetSuspend?.invoke(suspended)
    }

    /**
     * Sets the active therapy profile on the pump.
     *
     * @throws PumpConnectionException if the technical connection fails.
     */
    suspend fun setProfile(profile: InsulinProfile) {
        onSetProfile?.invoke(profile)
    }

    /**
     * Performs a synchronization of the pump's history events.
     *
     * @throws Exception if the command cannot be sent or the pump connection fails.
     */
    suspend fun syncHistory() {
        onSyncHistory?.invoke()
    }

    /**
     * Refreshes the last known data and status from the pump.
     *
     * @throws Exception if the command cannot be sent or the pump connection fails.
     */
    suspend fun refreshStatus() {
        onRefreshStatus?.invoke()
    }

    /**
     * Executes a low-level command on the pump.
     *
     * @throws IllegalStateException if no pump controller is wired to this pump instance.
     * @throws Exception if the command execution fails on the transport or protocol level.
     */
    @Suppress("UNCHECKED_CAST")
    suspend fun <R : PumpResponse> execute(command: PumpCommand<R>): R {
        val executor = onExecute ?: throw IllegalStateException("No executor wired")
        return executor.invoke(command) as R
    }

    /**
     * Executes a low-level stream command on the pump.
     *
     * @throws IllegalStateException if no pump controller is wired to this pump instance.
     * @throws Exception if the command execution fails on the transport or protocol level.
     */
    @Suppress("UNCHECKED_CAST")
    suspend fun <C : PumpResponse, R> executeStream(command: PumpStreamCommand<C, R>): R {
        val executor = onExecuteStream ?: throw IllegalStateException("No stream executor wired")
        return executor.invoke(command) as R
    }

    internal fun setConnected(value: Boolean) {
        _isConnected.value = value
    }

    /**
     * Updates the general status data.
     */
    internal fun updateStatus(data: DanaPumpStatusData) {
        _status.value = data
    }

    /**
     * Updates the hardware information.
     */
    internal fun updateHardware(data: DanaPumpHardwareData) {
        _hardware.value = data
    }

    internal fun updatePumpCapabilities(capabilities: PumpCapabilities) {
        _pumpCapabilities.value = capabilities
    }

    /**
     * Updates the current alerts.
     */
    internal fun updateAlerts(alerts: PumpAlerts) {
        _alerts.value = alerts
    }

    /**
     * Updates the current basal status.
     * Updates the basal history with new points and prunes records older than 24 hours.
     */
    internal fun updateBasal(status: BasalStatus) {
        _basalStatus.value = status
    }

    internal fun updateHourlyBasalRates(rates: List<Double>) {
        _hourlyBasalRates.value = rates
    }

    /**
     * Updates the basal history with new points and prunes records older than 24 hours.
     */
    internal fun updateBasalHistory(newPoints: List<InsulinHistoryPoint>) {
        val cutoff = Timestamp.now().minusHours(24)
        val sorted = (currentBasalPoints + newPoints)
            .distinctBy { it.pumpId ?: it.timestamp }
            .sortedBy { it.timestamp }

        val inside = sorted.filter { it.timestamp >= cutoff }
        val lastOutside = sorted.lastOrNull { it.timestamp < cutoff }

        currentBasalPoints = if (lastOutside != null) listOf(lastOutside) + inside else inside
        rebuildHistory()
    }

    /**
     * Updates the bolus history with new points and prunes records older than 24 hours.
     */
    internal fun updateBolusHistory(newPoints: List<InsulinHistoryPoint>) {
        val cutoff = Timestamp.now().minusHours(24)
        val sorted = (currentBolusPoints + newPoints)
            .distinctBy { it.pumpId ?: it.timestamp }
            .sortedBy { it.timestamp }

        currentBolusPoints = sorted.filter { it.timestamp >= cutoff }
        rebuildHistory()
    }

    private fun rebuildHistory() {
        val allPoints = (currentBasalPoints + currentBolusPoints).sortedBy { it.timestamp }
        if (allPoints.isEmpty()) {
            _history.value = null
        } else {
            _history.value = InsulinHistory(
                from = allPoints.first().timestamp,
                to = allPoints.last().timestamp,
                points = allPoints
            )
        }
    }

    /**
     * Updates the current error states.
     */
    internal fun updateErrorStates(errors: Set<DanaPumpError>) {
        _errorStates.value = errors
    }

    /**
     * Updates the active or recent bolus status.
     */
    internal fun updateBolusStatus(status: BolusStatus) {
        _bolusStatus.value = status
    }

    /**
     * Emits a bolus lifecycle event.
     */
    internal fun emitBolusEvent(event: BolusEvent) {
        _bolusEvents.tryEmit(event)
    }
}