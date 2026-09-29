package de.dh.daps.ui.screens.systemcontrol

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import de.dh.daps.common.CANNULA_CHANGE_WARNING_THRESHOLD_HOURS
import de.dh.daps.common.CONNECTION_WARNING_THRESHOLD_MINUTES
import de.dh.daps.common.PHONE_BATTERY_LOW_THRESHOLD
import de.dh.daps.common.PHONE_BATTERY_WARNING_THRESHOLD
import de.dh.daps.common.PUMP_BATTERY_LOW_THRESHOLD
import de.dh.daps.common.PUMP_BATTERY_WARNING_THRESHOLD
import de.dh.daps.common.PUMP_RESERVOIR_LOW_THRESHOLD
import de.dh.daps.common.PUMP_RESERVOIR_WARNING_THRESHOLD
import de.dh.daps.common.model.ApsMode
import de.dh.daps.common.model.GlucoseSourceStatus
import de.dh.daps.common.model.HardwareInformation
import de.dh.daps.common.model.InsulinAmount
import de.dh.daps.common.model.InsulinPumpStatus
import de.dh.daps.common.model.InsulinStatus
import de.dh.daps.common.model.ReplaceableComponent
import de.dh.daps.common.model.data.BgReading
import de.dh.daps.common.model.data.BgReadingsInterval
import de.dh.daps.common.model.data.Timestamp
import de.dh.daps.common.ui.UiText
import de.dh.daps.core.SystemRegistry
import de.dh.daps.core.pump.JobErrorCode
import de.dh.daps.core.pump.PumpCommand
import de.dh.daps.core.pump.PumpJob
import de.dh.daps.core.repository.BluetoothStatus
import de.dh.daps.ui.R
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

enum class ValueStatus {
    GOOD,
    WARNING,
    BAD
}

enum class OverviewPumpState {
    ACTIVE,
    SUSPENDED,
    ERROR
}

data class StatusMetric<T>(
    val value: T? = null,
    val status: ValueStatus? = null
)

data class OverviewAndroidSystemUiState(
    val bluetoothStatus: StatusMetric<Boolean> = StatusMetric(false, status = ValueStatus.BAD),
    val phoneBattery: StatusMetric<Int> = StatusMetric(0, status = ValueStatus.BAD),
    val permissionsStatus: StatusMetric<Int> = StatusMetric(0, status = ValueStatus.BAD),
    val dapsServiceStatus: StatusMetric<Boolean> = StatusMetric(false, status = ValueStatus.BAD)
)

data class OverviewApsSystemUiState(
    val mode: StatusMetric<ApsMode> = StatusMetric(ApsMode.AutoCorrection, status = ValueStatus.GOOD),
    val lastCalculation: StatusMetric<Timestamp> = StatusMetric(status = ValueStatus.GOOD),
    val status: StatusMetric<Int> = StatusMetric(0, status = ValueStatus.GOOD)
)

data class OverviewGlucoseSourceUiState(
    val sensorName: UiText,
    val status: StatusMetric<GlucoseSourceStatus> = StatusMetric(status = ValueStatus.GOOD),
    val lastConnection: StatusMetric<Timestamp> = StatusMetric(status = ValueStatus.GOOD),
    val lastReading: StatusMetric<Timestamp> = StatusMetric(status = ValueStatus.GOOD),
    val sensorExpiration: StatusMetric<Timestamp> = StatusMetric(status = ValueStatus.GOOD)
)

data class OverviewPumpUiState(
    val pumpName: UiText,
    val state: StatusMetric<OverviewPumpState> = StatusMetric(OverviewPumpState.ACTIVE, status = ValueStatus.GOOD),
    val lastBolus: StatusMetric<Timestamp> = StatusMetric(status = ValueStatus.GOOD),
    val battery: StatusMetric<Int> = StatusMetric(status = ValueStatus.GOOD),
    val reservoir: StatusMetric<InsulinAmount> = StatusMetric(status = ValueStatus.GOOD),
    val lastConnection: StatusMetric<Timestamp> = StatusMetric(status = ValueStatus.GOOD),
    val nextCannulaChange: StatusMetric<Timestamp> = StatusMetric(status = ValueStatus.GOOD)
)

data class OverviewTabUiState(
    val androidSystem: OverviewAndroidSystemUiState = OverviewAndroidSystemUiState(),
    val apsSystem: OverviewApsSystemUiState = OverviewApsSystemUiState(),
    val glucoseSource: OverviewGlucoseSourceUiState? = null,
    val insulinPump: OverviewPumpUiState? = null
)

data class SourceTabUiState(
    val glucoseSourceName: UiText,
    val sensorTypeName: String,
    val readingsInterval: BgReadingsInterval,
    val manufacturer: String? = null,
    val serialNumber: String? = null,
    val lastBgReading: BgReading? = null,
    val nextPredictedTimestamp: Timestamp? = null,
    val hasNextPrediction: Boolean = false,
    val sensorCode: String? = null,
    val transmitterSerialNumber: String? = null,
    val estimatedExpirationTimestamp: Timestamp? = null,
    val glucoseSourcePluginSection: (@Composable () -> Unit)? = null
)

data class PumpJobItem(
    val id: String,
    val title: UiText,
    val errorMessage: UiText? = null,
    val hasError: Boolean = errorMessage != null
)

data class PumpTabUiState(
    val pumpName: UiText,
    val batteryPercent: Int,
    val reservoirRemaining: InsulinAmount,
    val lastConnectionTimestamp: Timestamp,
    val pumpModel: String? = null,
    val manufacturer: String? = null,
    val serialNumber: String? = null,
    val pumpConnected: Boolean = false,
    val isSuspended: Boolean = false,
    val pendingJobs: List<PumpJobItem> = emptyList(),
    val pumpPluginSection: (@Composable () -> Unit)? = null
)

data class SystemControlUiState(
    val overviewUiState: OverviewTabUiState = OverviewTabUiState(),
    val sourceTabUiState: SourceTabUiState? = null,
    val pumpTabUiState: PumpTabUiState? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
class SystemControlViewModel(
    systemRegistry: SystemRegistry
) : ViewModel() {
    private data class GlucoseUiData(
        val sourceName: UiText,
        val sensorTypeName: String,
        val readingsInterval: BgReadingsInterval,
        val status: GlucoseSourceStatus,
        val lastConnection: Timestamp? = null,
        val lastBgReading: BgReading? = null,
        val nextPredictedTimestamp: Timestamp? = null,
        val hasNextPrediction: Boolean = false,
        val estimatedExpirationTimestamp: Timestamp? = null,
        val pluginUiProvider: GlucoseSourcePluginUiProvider? = null
    )

    private data class PumpUiData(
        val pumpName: UiText,
        val connected: Boolean = false,
        val model: String? = null,
        val manufacturer: String? = null,
        val serialNumber: String? = null,
        val status: InsulinPumpStatus,
        val lastConnection: Timestamp = Timestamp.INVALID,
        val nextCannulaChange: Timestamp? = null,
        val jobs: List<PumpJob> = emptyList(),
        val isSuspended: Boolean = false,
        val hasError: Boolean = false,
        val pluginUiProvider: PumpPluginUiProvider? = null
    )

    private val glucoseRepository = systemRegistry.glucoseRepository
    private val treatmentRepository = systemRegistry.treatmentRepository
    private val glucoseSourceManager = systemRegistry.glucoseSourceManager
    private val pumpManager = systemRegistry.pumpManager
    private val deviceStatusRepository = systemRegistry.deviceStatusRepository
    private val permissionRepository = systemRegistry.permissionRepository
    private val systemOrchestrator = systemRegistry.systemOrchestrator

    // Collect base infos in flows

    private val androidSystemInfo = combine(
        deviceStatusRepository.observeBluetoothStatus(),
        deviceStatusRepository.observeBatteryPercentage(),
        deviceStatusRepository.isServiceRunning,
        permissionRepository.permissionSummary
    ) { btStatus, batteryPct, isRunning, permSummary ->
        OverviewAndroidSystemUiState(
            bluetoothStatus = StatusMetric(
                value = btStatus == BluetoothStatus.ENABLED,
                status = when (btStatus) {
                    BluetoothStatus.ENABLED -> ValueStatus.GOOD
                    BluetoothStatus.TURNING_ON, BluetoothStatus.TURNING_OFF -> ValueStatus.WARNING
                    BluetoothStatus.DISABLED, BluetoothStatus.UNAVAILABLE -> ValueStatus.BAD
                }
            ),
            phoneBattery = StatusMetric(
                value = batteryPct,
                status = when {
                    batteryPct < PHONE_BATTERY_LOW_THRESHOLD -> ValueStatus.BAD
                    batteryPct < PHONE_BATTERY_WARNING_THRESHOLD -> ValueStatus.WARNING
                    else -> ValueStatus.GOOD
                }
            ),
            permissionsStatus = StatusMetric(
                value = permSummary.numMissing,
                status = if (permSummary.isAllGranted) ValueStatus.GOOD else ValueStatus.WARNING
            ),
            dapsServiceStatus = StatusMetric(
                value = isRunning,
                status = if (isRunning) ValueStatus.GOOD else ValueStatus.BAD
            )
        )
    }

    private val lastBolusTimestamp = treatmentRepository.observeInsulinApplications()
        .map { applications ->
            applications
                .filter { !it.basal && it.status == InsulinStatus.Confirmed }
                .maxByOrNull { it.timestamp }
                ?.timestamp
        }

    private val sourceInfo = glucoseSourceManager.activeGlucoseSource.flatMapLatest { source ->
        if (source == null) {
            flowOf(null)
        } else {
            val sourceName = source.sourceDisplayName
            val sensorType = source.getSensorTypeName()
            val interval = source.readingsInterval

            val nextPredicted = glucoseSourceManager.predictNextValueTimestamp()
            val hasPrediction = nextPredicted.isValid()

            val replaceable = source as? ReplaceableComponent
            val expTimestamp = replaceable?.endDate

            val provider = source as? GlucoseSourcePluginUiProvider

            combine(
                source.status,
                glucoseRepository.currentBg,
                source.lastConnection
            ) { status, currentBg, lastConn ->
                GlucoseUiData(
                    sourceName = sourceName,
                    sensorTypeName = sensorType,
                    readingsInterval = interval,
                    status = status,
                    lastConnection = lastConn,
                    lastBgReading = currentBg,
                    nextPredictedTimestamp = if (hasPrediction) nextPredicted else null,
                    hasNextPrediction = hasPrediction,
                    estimatedExpirationTimestamp = expTimestamp,
                    pluginUiProvider = provider
                )
            }
        }
    }

    private val pumpInfo = pumpManager.activeInsulinPump.flatMapLatest { pump ->
        if (pump == null) {
            flowOf(null)
        } else {
            val pumpName = pump.insulinPumpDisplayName
            val nextCannulaChange = (pump as? ReplaceableComponent)?.endDate

            val coordinator = pumpManager.pumpCoordinator
            val jobsFlow = coordinator?.pendingJobs ?: flowOf(emptyList())
            val lastConnFlow = coordinator?.lastConnectionTime ?: flowOf(Timestamp.INVALID)

            combine(
                pump.isConnected,
                pump.hardwareInformation,
                pump.pumpStatus,
                jobsFlow,
                lastConnFlow
            ) { connected: Boolean, hardware: HardwareInformation?, status: InsulinPumpStatus, jobs: List<PumpJob>, lastConn: Timestamp ->
                PumpUiData(
                    connected = connected,
                    pumpName = pumpName,
                    model = hardware?.model,
                    manufacturer = hardware?.manufacturer,
                    serialNumber = hardware?.serialNumber,
                    status = status,
                    lastConnection = lastConn,
                    nextCannulaChange = nextCannulaChange,
                    jobs = jobs,
                    isSuspended = status.pumpSuspended,
                    hasError = jobs.any { it.lastError != null },
                    pluginUiProvider = pump as? PumpPluginUiProvider
                )
            }
        }
    }

    private val apsInfo = combine(
        systemOrchestrator.apsMode,
        systemOrchestrator.apsIssues,
        systemOrchestrator.lastSuccessfulCoreCalculation
    ) { mode, issues, lastCalcTs ->
        val statusValue = issues.size
        val statusValueStatus = if (issues.isEmpty()) ValueStatus.GOOD else ValueStatus.BAD
        val modeValueStatus = when (mode) {
            ApsMode.AutoCorrection -> ValueStatus.GOOD
            ApsMode.BasalOnly -> ValueStatus.WARNING
            ApsMode.Suspend -> ValueStatus.BAD
        }
        OverviewApsSystemUiState(
            mode = StatusMetric(mode, status = modeValueStatus),
            lastCalculation = StatusMetric(
                value = lastCalcTs,
                status = if (lastCalcTs.isValid()) ValueStatus.GOOD else ValueStatus.WARNING
            ),
            status = StatusMetric(statusValue, status = statusValueStatus)
        )
    }

    // UI state is a combination of base flows

    val uiState: StateFlow<SystemControlUiState> = combine(
        apsInfo,
        sourceInfo,
        pumpInfo,
        lastBolusTimestamp,
        androidSystemInfo
    ) { apsSystem, gInfo, pInfo, lastBolusTs, androidSystem ->
        // Overview Tab State
        val overviewGlucoseSource = gInfo?.let {
            val (glucoseSourceStatus, glucoseValueStatus) = when (it.status) {
                GlucoseSourceStatus.Ok -> it.status to ValueStatus.GOOD
                GlucoseSourceStatus.Expired -> it.status to ValueStatus.WARNING
                GlucoseSourceStatus.Error -> it.status to ValueStatus.BAD
            }
            val lastConnectionStatus = when {
                it.lastConnection == null || it.lastConnection.isInvalid() -> ValueStatus.BAD
                it.lastConnection < Timestamp.now().minusMinutes(CONNECTION_WARNING_THRESHOLD_MINUTES) -> ValueStatus.WARNING
                else -> ValueStatus.GOOD
            }
            OverviewGlucoseSourceUiState(
                sensorName = it.sourceName,
                status = StatusMetric(
                    value = glucoseSourceStatus,
                    status = glucoseValueStatus
                ),
                lastConnection = StatusMetric(
                    value = it.lastConnection,
                    status = lastConnectionStatus
                ),
                lastReading = StatusMetric(
                    value = it.lastBgReading?.timestamp,
                    status = ValueStatus.GOOD
                ),
                sensorExpiration = StatusMetric(
                    value = it.estimatedExpirationTimestamp,
                    status = ValueStatus.GOOD
                )
            )
        }

        val overviewInsulinPump = pInfo?.let {
            val (pumpOverviewState, pumpValueStatus) = when {
                it.hasError -> OverviewPumpState.ERROR to ValueStatus.BAD
                it.isSuspended -> OverviewPumpState.SUSPENDED to ValueStatus.WARNING
                else -> OverviewPumpState.ACTIVE to ValueStatus.GOOD
            }
            val lastConnectionStatus = when {
                it.lastConnection.isInvalid() -> ValueStatus.BAD
                it.lastConnection < Timestamp.now().minusMinutes(CONNECTION_WARNING_THRESHOLD_MINUTES) -> ValueStatus.WARNING
                else -> ValueStatus.GOOD
            }
            val nextCannulaChangeStatus = when {
                it.nextCannulaChange == null || it.nextCannulaChange.isInvalid() -> ValueStatus.GOOD
                it.nextCannulaChange <= Timestamp.now() -> ValueStatus.BAD
                it.nextCannulaChange < Timestamp.now().plusHours(CANNULA_CHANGE_WARNING_THRESHOLD_HOURS) -> ValueStatus.WARNING
                else -> ValueStatus.GOOD
            }
            OverviewPumpUiState(
                pumpName = it.pumpName,
                state = StatusMetric(
                    value = pumpOverviewState,
                    status = pumpValueStatus
                ),
                lastBolus = StatusMetric(value = lastBolusTs, status = if (lastBolusTs == null) ValueStatus.WARNING else ValueStatus.GOOD),
                battery = StatusMetric(
                    value = it.status.batteryRemainingPercent,
                    status = when {
                        it.status.batteryRemainingPercent <= PUMP_BATTERY_LOW_THRESHOLD -> ValueStatus.BAD
                        it.status.batteryRemainingPercent < PUMP_BATTERY_WARNING_THRESHOLD -> ValueStatus.WARNING
                        else -> ValueStatus.GOOD
                    }
                ),
                reservoir = StatusMetric(
                    value = it.status.reservoirRemainingUnits,
                    status = when {
                        it.status.reservoirRemainingUnits <= PUMP_RESERVOIR_LOW_THRESHOLD -> ValueStatus.BAD
                        it.status.reservoirRemainingUnits < PUMP_RESERVOIR_WARNING_THRESHOLD -> ValueStatus.WARNING
                        else -> ValueStatus.GOOD
                    }
                ),
                lastConnection = StatusMetric(
                    value = it.lastConnection,
                    status = lastConnectionStatus
                ),
                nextCannulaChange = StatusMetric(
                    value = it.nextCannulaChange,
                    status = nextCannulaChangeStatus
                )
            )
        }

        val overviewState = OverviewTabUiState(
            androidSystem = androidSystem,
            apsSystem = apsSystem,
            glucoseSource = overviewGlucoseSource,
            insulinPump = overviewInsulinPump
        )

        // Source Tab State
        val sourceTabState = gInfo?.let {
            SourceTabUiState(
                glucoseSourceName = it.sourceName,
                manufacturer = null,
                sensorTypeName = it.sensorTypeName,
                readingsInterval = it.readingsInterval,
                lastBgReading = it.lastBgReading,
                nextPredictedTimestamp = it.nextPredictedTimestamp,
                hasNextPrediction = it.hasNextPrediction,
                estimatedExpirationTimestamp = it.estimatedExpirationTimestamp,
                glucoseSourcePluginSection = it.pluginUiProvider?.let { provider -> { provider.GlucoseSourceControlSection() } }
            )
        }

        // Pump Tab State
        val pumpTabState = pInfo?.let {
            val pumpJobsList = it.jobs.map { job ->
                val titleText = when (val cmd = job.command) {
                    is PumpCommand.RefreshStatus -> UiText.StringResource(R.string.system_control_pump_job_type_refresh_status)
                    is PumpCommand.SyncHistory -> UiText.StringResource(R.string.system_control_pump_job_type_history_sync)
                    is PumpCommand.DeliverBolus -> UiText.StringResource(R.string.system_control_pump_job_type_bolus, cmd.amount.iu)
                    is PumpCommand.SetTempBasal -> UiText.StringResource(R.string.system_control_pump_job_type_temp_basal, cmd.percent)
                    is PumpCommand.SetProfile -> UiText.StringResource(R.string.system_control_pump_job_type_profile)
                    is PumpCommand.CancelTempBasal -> UiText.StringResource(R.string.system_control_pump_job_type_cancel_temp_basal)
                    is PumpCommand.CancelBolus -> UiText.StringResource(R.string.system_control_pump_job_type_cancel_bolus)
                }
                val errText = job.lastError?.let { err ->
                    when (err) {
                        JobErrorCode.Expired -> UiText.StringResource(R.string.system_control_pump_job_error_expired)
                        is JobErrorCode.ConnectionFailed -> {
                            val suffix = err.message?.let { ": $it" } ?: ""
                            UiText.StringResource(R.string.system_control_pump_job_error_connection_failed, suffix)
                        }
                        is JobErrorCode.CommandFailed -> {
                            val suffix = err.message?.let { ": $it" } ?: ""
                            UiText.StringResource(R.string.system_control_pump_job_error_command_failed, err.status.name, suffix)
                        }
                        is JobErrorCode.TechnicalError -> {
                            val suffix = err.message?.let { ": $it" } ?: ""
                            UiText.StringResource(R.string.system_control_pump_job_error_technical, suffix)
                        }
                    }
                }
                PumpJobItem(
                    id = job.id,
                    title = titleText,
                    errorMessage = errText
                )
            }

            PumpTabUiState(
                pumpName = it.pumpName,
                pumpModel = it.model,
                manufacturer = it.manufacturer,
                serialNumber = it.serialNumber,
                pumpConnected = it.connected,
                isSuspended = it.isSuspended,
                batteryPercent = it.status.batteryRemainingPercent,
                reservoirRemaining = it.status.reservoirRemainingUnits,
                lastConnectionTimestamp = it.lastConnection,
                pendingJobs = pumpJobsList,
                pumpPluginSection = it.pluginUiProvider?.let { provider -> { provider.PumpControlSection() } }
            )
        }

        SystemControlUiState(
            overviewUiState = overviewState,
            sourceTabUiState = sourceTabState,
            pumpTabUiState = pumpTabState
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SystemControlUiState()
    )

    fun stopActiveGlucoseSource() {
        glucoseSourceManager.glucoseSource?.stop()
        glucoseSourceManager.glucoseSource = null
    }

    fun disconnectPumpForMaintenance() {
        // TODO
    }

    fun cancelPumpJob(jobId: String) {
        pumpManager.cancelJobs { it.id == jobId }
    }

    fun refreshPumpStatus() {
        pumpManager.issueCommand(PumpCommand.RefreshStatus)
    }

    companion object {
        class Factory(private val registry: SystemRegistry) : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                return SystemControlViewModel(registry) as T
            }
        }
    }
}