package de.dh.daps.ui.screens.systemcontrol

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import de.dh.daps.common.BG_READING_BAD_THRESHOLD_MINUTES
import de.dh.daps.common.BG_READING_WARNING_THRESHOLD_MINUTES
import de.dh.daps.common.CANNULA_CHANGE_WARNING_THRESHOLD_HOURS
import de.dh.daps.common.CONNECTION_BAD_THRESHOLD_MINUTES
import de.dh.daps.common.CONNECTION_WARNING_THRESHOLD_MINUTES
import de.dh.daps.common.CORE_CALCULATION_BAD_THRESHOLD_MINUTES
import de.dh.daps.common.CORE_CALCULATION_WARNING_THRESHOLD_MINUTES
import de.dh.daps.common.PHONE_BATTERY_LOW_THRESHOLD
import de.dh.daps.common.PHONE_BATTERY_WARNING_THRESHOLD
import de.dh.daps.common.PUMP_BATTERY_LOW_THRESHOLD
import de.dh.daps.common.PUMP_BATTERY_WARNING_THRESHOLD
import de.dh.daps.common.PUMP_RESERVOIR_LOW_THRESHOLD
import de.dh.daps.common.PUMP_RESERVOIR_WARNING_THRESHOLD
import de.dh.daps.common.SENSOR_EXPIRATION_WARNING_THRESHOLD_HOURS
import de.dh.daps.common.model.ApsMode
import de.dh.daps.common.model.Expiration
import de.dh.daps.common.model.ExpirationDate
import de.dh.daps.common.model.GlucoseSourcePluginUiProvider
import de.dh.daps.common.model.GlucoseSourceStatus
import de.dh.daps.common.model.InsulinAmount
import de.dh.daps.common.model.InsulinPumpStatus
import de.dh.daps.common.model.InsulinStatus
import de.dh.daps.common.model.PumpHardwareInformation
import de.dh.daps.common.model.PumpPluginUiProvider
import de.dh.daps.common.model.SourceHardwareInformation
import de.dh.daps.common.model.ToDo
import java.time.LocalDate
import java.time.ZoneId
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

sealed interface OverviewAndroidSystemUiState {
    data object Loading : OverviewAndroidSystemUiState
    data class Content(
        val bluetoothStatus: StatusMetric<Boolean>,
        val phoneBattery: StatusMetric<Int>,
        val permissionsStatus: StatusMetric<Int>,
        val dapsServiceStatus: StatusMetric<Boolean>
    ) : OverviewAndroidSystemUiState
}

sealed interface OverviewApsSystemUiState {
    data object Loading : OverviewApsSystemUiState
    data class Content(
        val mode: StatusMetric<ApsMode>,
        val lastCalculation: StatusMetric<Timestamp>,
        val status: StatusMetric<Int>
    ) : OverviewApsSystemUiState
}

sealed interface OverviewGlucoseSourceUiState {
    data object Loading : OverviewGlucoseSourceUiState
    data object NoneConfigured : OverviewGlucoseSourceUiState
    data class Content(
        val sensorName: UiText,
        val status: StatusMetric<GlucoseSourceStatus>,
        val lastConnection: StatusMetric<Timestamp>,
        val lastReading: StatusMetric<Timestamp>,
        val sensorExpiration: StatusMetric<Timestamp>
    ) : OverviewGlucoseSourceUiState
}

sealed interface OverviewPumpUiState {
    data object Loading : OverviewPumpUiState
    data object NoneConfigured : OverviewPumpUiState
    data class Content(
        val pumpName: UiText,
        val state: StatusMetric<OverviewPumpState>,
        val lastBolus: StatusMetric<Timestamp>,
        val battery: StatusMetric<Int>,
        val reservoir: StatusMetric<InsulinAmount>,
        val lastConnection: StatusMetric<Timestamp>,
        val nextCannulaChange: StatusMetric<ExpirationDate>
    ) : OverviewPumpUiState
}

sealed interface OverviewTabUiState {
    data object Loading : OverviewTabUiState
    data class Content(
        val androidSystem: OverviewAndroidSystemUiState,
        val apsSystem: OverviewApsSystemUiState,
        val glucoseSource: OverviewGlucoseSourceUiState,
        val insulinPump: OverviewPumpUiState
    ) : OverviewTabUiState
}

sealed interface SourceTabUiState {
    data object Loading : SourceTabUiState
    data object NoneConfigured : SourceTabUiState
    data class Content(
        val glucoseSourceName: UiText,
        val readingsInterval: BgReadingsInterval,
        val manufacturer: String? = null,
        val model: String? = null,
        val serialNumber: String? = null,
        val startDate: Timestamp? = null,
        val lastBgReading: BgReading? = null,
        val nextPredictedTimestamp: Timestamp? = null,
        val hasNextPrediction: Boolean = false,
        val estimatedExpirationTimestamp: Timestamp? = null,
        val glucoseSourcePluginSection: (@Composable () -> Unit)? = null
    ) : SourceTabUiState
}

data class PumpJobItem(
    val id: String,
    val title: UiText,
    val errorMessage: UiText? = null,
    val hasError: Boolean = errorMessage != null
)

sealed interface PumpTabUiState {
    data object Loading : PumpTabUiState
    data object NoneConfigured : PumpTabUiState
    data class Content(
        val pumpName: UiText,
        val batteryPercent: Int,
        val reservoirRemaining: InsulinAmount,
        val lastConnectionTimestamp: Timestamp,
        val manufacturer: String? = null,
        val pumpModel: String? = null,
        val serialNumber: String? = null,
        val pumpConnected: Boolean = false,
        val isSuspended: Boolean = false,
        val startDate: Timestamp? = null,
        val expirations: List<Expiration> = emptyList(),
        val pendingJobs: List<PumpJobItem> = emptyList(),
        val pumpPluginSection: (@Composable () -> Unit)? = null
    ) : PumpTabUiState
}

sealed interface SystemControlUiState {
    data object Loading : SystemControlUiState
    data class Content(
        val overviewUiState: OverviewTabUiState,
        val sourceTabUiState: SourceTabUiState,
        val pumpTabUiState: PumpTabUiState
    ) : SystemControlUiState
}

@OptIn(ExperimentalCoroutinesApi::class)
class SystemControlViewModel(
    systemRegistry: SystemRegistry
) : ViewModel() {
    private data class GlucoseUiData(
        val sourceName: UiText,
        val readingsInterval: BgReadingsInterval,
        val status: GlucoseSourceStatus,
        val manufacturer: String? = null,
        val model: String? = null,
        val serialNumber: String? = null,
        val startDate: Timestamp? = null,
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
        val startDate: Timestamp? = null,
        val expirations: List<Expiration> = emptyList(),
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
        OverviewAndroidSystemUiState.Content(
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
            val interval = source.readingsInterval

            val nextPredicted = glucoseSourceManager.predictNextValueTimestamp()
            val hasPrediction = nextPredicted.isValid()

            val provider = source as? GlucoseSourcePluginUiProvider

            combine(
                source.status,
                source.sensorType,
                source.hardwareInformation,
                glucoseRepository.currentBg,
                source.lastConnection,
                source.startDate,
                source.endDate,
                source.isExpired
            ) { array ->
                val status = array[0] as GlucoseSourceStatus
                val hardware = array[2] as SourceHardwareInformation?
                val currentBg = array[3] as BgReading?
                val lastConn = array[4] as Timestamp?
                val startDate = array[5] as Timestamp?
                val endDate = array[6] as Timestamp?
                val isExpired = array[7] as Boolean

                if (isExpired) {
                    null
                } else {
                    GlucoseUiData(
                        sourceName = sourceName,
                        readingsInterval = interval,
                        status = status,
                        manufacturer = hardware?.manufacturer,
                        model = hardware?.model,
                        serialNumber = hardware?.serialNumber,
                        startDate = startDate,
                        lastConnection = lastConn,
                        lastBgReading = currentBg,
                        nextPredictedTimestamp = if (hasPrediction) nextPredicted else null,
                        hasNextPrediction = hasPrediction,
                        estimatedExpirationTimestamp = endDate,
                        pluginUiProvider = provider
                    )
                }
            }
        }
    }

    private val pumpInfo = pumpManager.activeInsulinPump.flatMapLatest { pump ->
        if (pump == null) {
            flowOf(null)
        } else {
            val pumpName = pump.insulinPumpDisplayName

            val coordinator = pumpManager.pumpCoordinator
            val jobsFlow = coordinator?.pendingJobs ?: flowOf(emptyList())
            val lastConnFlow = coordinator?.lastConnectionTime ?: flowOf(Timestamp.INVALID)

            combine(
                pump.isConnected,
                pump.hardwareInformation,
                pump.pumpStatus,
                jobsFlow,
                lastConnFlow,
                pump.startDate,
                pump.isExpired,
                pump.expirations
            ) { array ->
                val connected = array[0] as Boolean
                val hardware = array[1] as PumpHardwareInformation?
                val status = array[2] as InsulinPumpStatus
                @Suppress("UNCHECKED_CAST")
                val jobs = array[3] as List<PumpJob>
                val lastConn = array[4] as Timestamp
                val startDate = array[5] as Timestamp?
                val isExpired = array[6] as Boolean
                @Suppress("UNCHECKED_CAST")
                val expirations = array[7] as List<Expiration>

                if (isExpired) {
                    null
                } else {
                    PumpUiData(
                        connected = connected,
                        pumpName = pumpName,
                        model = hardware?.model,
                        manufacturer = hardware?.manufacturer,
                        serialNumber = hardware?.serialNumber,
                        status = status,
                        lastConnection = lastConn,
                        startDate = startDate,
                        expirations = expirations,
                        jobs = jobs,
                        isSuspended = status.pumpSuspended,
                        hasError = jobs.any { it.lastError != null },
                        pluginUiProvider = pump as? PumpPluginUiProvider
                    )
                }
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
            ApsMode.OnlySuggestions -> ValueStatus.WARNING
            ApsMode.Suspend -> ValueStatus.BAD
        }
        val lastCalcStatus = when {
            lastCalcTs.isInvalid() || lastCalcTs < Timestamp.now().minusMinutes(CORE_CALCULATION_BAD_THRESHOLD_MINUTES) -> ValueStatus.BAD
            lastCalcTs < Timestamp.now().minusMinutes(CORE_CALCULATION_WARNING_THRESHOLD_MINUTES) -> ValueStatus.WARNING
            else -> ValueStatus.GOOD
        }
        OverviewApsSystemUiState.Content(
            mode = StatusMetric(mode, status = modeValueStatus),
            lastCalculation = StatusMetric(
                value = lastCalcTs,
                status = lastCalcStatus
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
        val overviewGlucoseSource: OverviewGlucoseSourceUiState = if (gInfo == null) {
            OverviewGlucoseSourceUiState.NoneConfigured
        } else {
            val (glucoseSourceStatus, glucoseValueStatus) = when (gInfo.status) {
                GlucoseSourceStatus.Ok -> gInfo.status to ValueStatus.GOOD
                GlucoseSourceStatus.Expired -> gInfo.status to ValueStatus.WARNING
                GlucoseSourceStatus.Error -> gInfo.status to ValueStatus.BAD
            }
            val lastConnectionStatus = when {
                gInfo.lastConnection == null || gInfo.lastConnection.isInvalid() || gInfo.lastConnection < Timestamp.now().minusMinutes(CONNECTION_BAD_THRESHOLD_MINUTES) -> ValueStatus.BAD
                gInfo.lastConnection < Timestamp.now().minusMinutes(CONNECTION_WARNING_THRESHOLD_MINUTES) -> ValueStatus.WARNING
                else -> ValueStatus.GOOD
            }
            val lastReadingTs = gInfo.lastBgReading?.timestamp
            val lastReadingStatus = when {
                lastReadingTs == null || lastReadingTs.isInvalid() || lastReadingTs < Timestamp.now().minusMinutes(BG_READING_BAD_THRESHOLD_MINUTES) -> ValueStatus.BAD
                lastReadingTs < Timestamp.now().minusMinutes(BG_READING_WARNING_THRESHOLD_MINUTES) -> ValueStatus.WARNING
                else -> ValueStatus.GOOD
            }
            val sensorExpirationTs = gInfo.estimatedExpirationTimestamp
            val sensorExpirationStatus = when {
                sensorExpirationTs == null || sensorExpirationTs.isInvalid() -> ValueStatus.GOOD
                sensorExpirationTs <= Timestamp.now() -> ValueStatus.BAD
                sensorExpirationTs < Timestamp.now().plusHours(SENSOR_EXPIRATION_WARNING_THRESHOLD_HOURS) -> ValueStatus.WARNING
                else -> ValueStatus.GOOD
            }
            OverviewGlucoseSourceUiState.Content(
                sensorName = gInfo.sourceName,
                status = StatusMetric(
                    value = glucoseSourceStatus,
                    status = glucoseValueStatus
                ),
                lastConnection = StatusMetric(
                    value = gInfo.lastConnection,
                    status = lastConnectionStatus
                ),
                lastReading = StatusMetric(
                    value = lastReadingTs,
                    status = lastReadingStatus
                ),
                sensorExpiration = StatusMetric(
                    value = sensorExpirationTs,
                    status = sensorExpirationStatus
                )
            )
        }

        val overviewInsulinPump: OverviewPumpUiState = if (pInfo == null) {
            OverviewPumpUiState.NoneConfigured
        } else {
            val (pumpOverviewState, pumpValueStatus) = when {
                pInfo.hasError -> OverviewPumpState.ERROR to ValueStatus.BAD
                pInfo.isSuspended -> OverviewPumpState.SUSPENDED to ValueStatus.WARNING
                else -> OverviewPumpState.ACTIVE to ValueStatus.GOOD
            }
            val lastConnectionStatus = when {
                pInfo.lastConnection.isInvalid() || pInfo.lastConnection < Timestamp.now().minusMinutes(CONNECTION_BAD_THRESHOLD_MINUTES) -> ValueStatus.BAD
                pInfo.lastConnection < Timestamp.now().minusMinutes(CONNECTION_WARNING_THRESHOLD_MINUTES) -> ValueStatus.WARNING
                else -> ValueStatus.GOOD
            }
            val nextCannulaChangeExpiration = pInfo.expirations
                .map { it.date }
                .minByOrNull { expDate ->
                    when (expDate) {
                        is ExpirationDate.Hard -> expDate.dateTime.ms
                        is ExpirationDate.Approximate -> expDate.date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                    }
                }
            val nextCannulaChangeStatus = when (nextCannulaChangeExpiration) {
                null -> ValueStatus.GOOD
                is ExpirationDate.Hard -> {
                    val ts = nextCannulaChangeExpiration.dateTime
                    when {
                        ts.isInvalid() -> ValueStatus.GOOD
                        ts <= Timestamp.now() -> ValueStatus.BAD
                        ts < Timestamp.now().plusHours(CANNULA_CHANGE_WARNING_THRESHOLD_HOURS) -> ValueStatus.WARNING
                        else -> ValueStatus.GOOD
                    }
                }
                is ExpirationDate.Approximate -> {
                    val date = nextCannulaChangeExpiration.date
                    val today = LocalDate.now()
                    when {
                        date <= today -> ValueStatus.BAD
                        date <= today.plusDays(1) -> ValueStatus.WARNING
                        else -> ValueStatus.GOOD
                    }
                }
            }
            OverviewPumpUiState.Content(
                pumpName = pInfo.pumpName,
                state = StatusMetric(
                    value = pumpOverviewState,
                    status = pumpValueStatus
                ),
                lastBolus = StatusMetric(value = lastBolusTs, status = if (lastBolusTs == null) ValueStatus.WARNING else ValueStatus.GOOD),
                battery = StatusMetric(
                    value = pInfo.status.batteryRemainingPercent,
                    status = when {
                        pInfo.status.batteryRemainingPercent <= PUMP_BATTERY_LOW_THRESHOLD -> ValueStatus.BAD
                        pInfo.status.batteryRemainingPercent < PUMP_BATTERY_WARNING_THRESHOLD -> ValueStatus.WARNING
                        else -> ValueStatus.GOOD
                    }
                ),
                reservoir = StatusMetric(
                    value = pInfo.status.reservoirRemainingUnits,
                    status = when {
                        pInfo.status.reservoirRemainingUnits <= PUMP_RESERVOIR_LOW_THRESHOLD -> ValueStatus.BAD
                        pInfo.status.reservoirRemainingUnits < PUMP_RESERVOIR_WARNING_THRESHOLD -> ValueStatus.WARNING
                        else -> ValueStatus.GOOD
                    }
                ),
                lastConnection = StatusMetric(
                    value = pInfo.lastConnection,
                    status = lastConnectionStatus
                ),
                nextCannulaChange = StatusMetric(
                    value = nextCannulaChangeExpiration,
                    status = nextCannulaChangeStatus
                )
            )
        }

        val overviewState = OverviewTabUiState.Content(
            androidSystem = androidSystem,
            apsSystem = apsSystem,
            glucoseSource = overviewGlucoseSource,
            insulinPump = overviewInsulinPump
        )

        // Source Tab State
        val sourceTabState: SourceTabUiState = if (gInfo == null) {
            SourceTabUiState.NoneConfigured
        } else {
            SourceTabUiState.Content(
                glucoseSourceName = gInfo.sourceName,
                manufacturer = gInfo.manufacturer,
                model = gInfo.model,
                serialNumber = gInfo.serialNumber,
                readingsInterval = gInfo.readingsInterval,
                startDate = gInfo.startDate,
                lastBgReading = gInfo.lastBgReading,
                nextPredictedTimestamp = gInfo.nextPredictedTimestamp,
                hasNextPrediction = gInfo.hasNextPrediction,
                estimatedExpirationTimestamp = gInfo.estimatedExpirationTimestamp,
                glucoseSourcePluginSection = gInfo.pluginUiProvider?.let { provider -> { provider.GlucoseSourceControlSection() } }
            )
        }

        // Pump Tab State
        val pumpTabState: PumpTabUiState = if (pInfo == null) {
            PumpTabUiState.NoneConfigured
        } else {
            val pumpJobsList = pInfo.jobs.map { job ->
                val titleText = when (val cmd = job.command) {
                    is PumpCommand.RefreshStatus -> UiText.StringResource(R.string.system_control_pump_job_type_refresh_status)
                    is PumpCommand.SyncHistory -> UiText.StringResource(R.string.system_control_pump_job_type_history_sync)
                    is PumpCommand.DeliverBolus -> UiText.StringResource(R.string.system_control_pump_job_type_bolus, cmd.amount.iu)
                    is PumpCommand.SetTempBasal -> UiText.StringResource(R.string.system_control_pump_job_type_temp_basal, cmd.percent)
                    is PumpCommand.SetProfile -> UiText.StringResource(R.string.system_control_pump_job_type_profile)
                    is PumpCommand.CancelTempBasal -> UiText.StringResource(R.string.system_control_pump_job_type_cancel_temp_basal)
                    is PumpCommand.CancelBolus -> UiText.StringResource(R.string.system_control_pump_job_type_cancel_bolus)
                    is PumpCommand.SetSuspend -> if (cmd.suspended) UiText.StringResource(R.string.system_control_pump_job_type_suspend) else UiText.StringResource(R.string.system_control_pump_job_type_resume)
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

            PumpTabUiState.Content(
                pumpName = pInfo.pumpName,
                batteryPercent = pInfo.status.batteryRemainingPercent,
                reservoirRemaining = pInfo.status.reservoirRemainingUnits,
                lastConnectionTimestamp = pInfo.lastConnection,
                manufacturer = pInfo.manufacturer,
                pumpModel = pInfo.model,
                serialNumber = pInfo.serialNumber,
                pumpConnected = pInfo.connected,
                isSuspended = pInfo.isSuspended,
                startDate = pInfo.startDate,
                expirations = pInfo.expirations,
                pendingJobs = pumpJobsList,
                pumpPluginSection = pInfo.pluginUiProvider?.let { provider -> { provider.PumpControlSection() } }
            )
        }

        SystemControlUiState.Content(
            overviewUiState = overviewState,
            sourceTabUiState = sourceTabState,
            pumpTabUiState = pumpTabState
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SystemControlUiState.Loading
    )

    fun stopActiveGlucoseSource() {
        glucoseSourceManager.glucoseSource?.stop()
        glucoseSourceManager.glucoseSource = null
    }

    fun disconnectPumpForMaintenance() {
        ToDo.toBeImplemented("disconnectPumpForMaintainance")
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