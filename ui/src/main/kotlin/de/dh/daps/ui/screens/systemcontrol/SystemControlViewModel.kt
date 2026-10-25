package de.dh.daps.ui.screens.systemcontrol

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import de.dh.daps.common.BG_READING_BAD_THRESHOLD_MINUTES
import de.dh.daps.common.BG_READING_WARNING_THRESHOLD_MINUTES
import de.dh.daps.common.CATHETER_CHANGE_WARNING_THRESHOLD_HOURS
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
import de.dh.daps.common.model.ExpirationDate
import de.dh.daps.common.model.GlucoseSource
import de.dh.daps.common.model.GlucoseSourceDriver
import de.dh.daps.common.model.GlucoseSourceStatus
import de.dh.daps.common.model.InsulinAmount
import de.dh.daps.common.model.InsulinConcentration
import de.dh.daps.common.model.InsulinPump
import de.dh.daps.common.model.InsulinPumpDriver
import de.dh.daps.common.model.InsulinPumpStatus
import de.dh.daps.common.model.PumpLifecycleState
import de.dh.daps.common.model.PumpReplaceableComponent
import de.dh.daps.common.model.InsulinStatus
import de.dh.daps.common.model.PumpHardwareInformation
import de.dh.daps.common.model.SourceHardwareInformation
import de.dh.daps.common.model.data.BgReading
import de.dh.daps.common.model.data.BgReadingsInterval
import de.dh.daps.common.model.data.Timestamp
import de.dh.daps.common.ui.UiText
import de.dh.daps.core.SystemRegistry
import de.dh.daps.core.aps.ApsIssue
import de.dh.daps.core.pump.BolusJob
import de.dh.daps.core.pump.BolusJobType
import de.dh.daps.core.pump.JobErrorCode
import de.dh.daps.core.pump.PumpCoordinatorState
import de.dh.daps.core.pump.PumpIssue
import de.dh.daps.core.pump.PumpSyncStatus
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
import java.time.LocalDate

enum class ValueStatus {
    GOOD,
    WARNING,
    BAD
}

enum class OverviewPumpState {
    ACTIVE,
    SUSPENDED,
    ERROR,
    EXPIRED
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

sealed interface ApsSystemStatus {
    data object Active : ApsSystemStatus
    data object Suspended : ApsSystemStatus
    data class Issues(val count: Int) : ApsSystemStatus
}

sealed interface OverviewApsSystemUiState {
    data object Loading : OverviewApsSystemUiState
    data class Content(
        val mode: StatusMetric<ApsMode>,
        val lastCalculation: StatusMetric<Timestamp>,
        val issues: List<ApsIssue>
    ) : OverviewApsSystemUiState {
        val unintendedIssues: List<ApsIssue>
            get() = if (mode.value == ApsMode.Suspend) {
                issues.filterNot { it is ApsIssue.Pump && it.issue is PumpIssue.Inoperative }
            } else {
                issues
            }

        val status: StatusMetric<ApsSystemStatus>
            get() {
                val unintended = unintendedIssues
                return when {
                    unintended.isNotEmpty() -> StatusMetric(
                        value = ApsSystemStatus.Issues(unintended.size),
                        status = ValueStatus.BAD
                    )
                    mode.value == ApsMode.Suspend -> StatusMetric(
                        value = ApsSystemStatus.Suspended,
                        status = ValueStatus.WARNING
                    )
                    else -> StatusMetric(
                        value = ApsSystemStatus.Active,
                        status = ValueStatus.GOOD
                    )
                }
            }
    }
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
        val lastInsulin: StatusMetric<Timestamp>,
        val battery: StatusMetric<Int>,
        val reservoir: StatusMetric<InsulinAmount>,
        val lastConnection: StatusMetric<Timestamp>,
        val nextExpiration: StatusMetric<ExpirationDate>,
        val nextExpirationType: PumpReplaceableComponent? = null,
        val insulinConcentration: InsulinConcentration
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
        val hasNextPrediction: Boolean,
        val expiration: Timestamp? = null,
        val isExpired: Boolean,
        val glucoseSourcePluginSection: (@Composable () -> Unit)? = null,
        val glucoseSourceSettingsScreen: (@Composable (onClose: () -> Unit) -> Unit)? = null
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
        val insulinConcentration: InsulinConcentration,
        val lastConnectionTimestamp: Timestamp,
        val manufacturer: String? = null,
        val pumpModel: String? = null,
        val serialNumber: String? = null,
        val pumpConnected: Boolean,
        val isSuspended: Boolean,
        val lifecycleState: PumpLifecycleState,
        val pendingJobs: List<PumpJobItem>,
        val coordinatorState: PumpCoordinatorState,
        val syncStatus: PumpSyncStatus,
        val nextWakeupTime: Timestamp? = null,
        val pumpPluginSection: (@Composable () -> Unit)? = null,
        val pumpSettingsScreen: (@Composable (onClose: () -> Unit) -> Unit)? = null
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
        val hasNextPrediction: Boolean,
        val expiration: Timestamp? = null,
        val isExpired: Boolean,
        val source: GlucoseSource? = null,
        val driver: GlucoseSourceDriver? = null
    )

    private data class PumpUiData(
        val pumpName: UiText,
        val connected: Boolean,
        val model: String? = null,
        val manufacturer: String? = null,
        val serialNumber: String? = null,
        val status: InsulinPumpStatus,
        val lastConnection: Timestamp,
        val lifecycleState: PumpLifecycleState,
        val jobs: List<BolusJob>,
        val coordinatorState: PumpCoordinatorState,
        val syncStatus: PumpSyncStatus,
        val nextWakeupTime: Timestamp? = null,
        val isSuspended: Boolean,
        val hasError: Boolean,
        val pump: InsulinPump? = null,
        val driver: InsulinPumpDriver? = null
    )

    private val glucoseRepository = systemRegistry.glucoseRepository
    private val treatmentRepository = systemRegistry.treatmentRepository
    private val glucoseSourceManager = systemRegistry.glucoseSourceManager
    private val glucoseSourceDriverManager = systemRegistry.glucoseSourceDriverManager
    private val pumpManager = systemRegistry.pumpManager
    private val pumpDriverManager = systemRegistry.pumpDriverManager
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

    private val lastInsulinTimestamp = treatmentRepository.observeInsulinApplications()
        .map { applications ->
            applications
                .filter { it.status == InsulinStatus.Confirmed }
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

            combine(
                source.status,
                source.sensorType,
                source.hardwareInformation,
                glucoseRepository.currentBg,
                source.lastConnection,
                source.startDate,
                source.endDate,
                source.isExpired,
                glucoseSourceDriverManager.activeDriver
            ) { array ->
                val status = array[0] as GlucoseSourceStatus
                val hardware = array[2] as SourceHardwareInformation?
                val currentBg = array[3] as BgReading?
                val lastConn = array[4] as Timestamp?
                val startDate = array[5] as Timestamp?
                val endDate = array[6] as Timestamp?
                val isExpired = array[7] as Boolean
                val activeDriver = array[8] as GlucoseSourceDriver?

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
                    expiration = endDate,
                    isExpired = isExpired,
                    source = source,
                    driver = activeDriver
                )
            }
        }
    }

    private val pumpInfo = pumpManager.activeInsulinPump.flatMapLatest { pump ->
        if (pump == null) {
            flowOf(null)
        } else {
            val pumpName = pump.insulinPumpDisplayName

            val coordinator = pumpManager.pumpCoordinator
            val jobsFlow = coordinator?.pendingBoluses ?: flowOf(emptyList())
            val lastConnFlow = coordinator?.lastConnectionTime ?: flowOf(Timestamp.INVALID)
            val coordinatorStateFlow = coordinator?.pumpCoordinatorState ?: flowOf(PumpCoordinatorState.Idle)
            val syncStatusFlow = coordinator?.syncStatus ?: flowOf(PumpSyncStatus.InSync(null))
            val nextWakeupFlow = coordinator?.nextWakeupTime ?: flowOf(null)

            combine(
                pump.isConnected,
                pump.hardwareInformation,
                pump.pumpStatus,
                jobsFlow,
                lastConnFlow,
                pumpDriverManager.activeDriver,
                coordinatorStateFlow,
                syncStatusFlow,
                nextWakeupFlow,
                pump.insulinConcentration,
                pump.insulinTypeId
            ) { array ->
                val connected = array[0] as Boolean
                val hardware = array[1] as PumpHardwareInformation?
                val status = array[2] as InsulinPumpStatus
                @Suppress("UNCHECKED_CAST")
                val jobs = array[3] as List<BolusJob>
                val lastConn = array[4] as Timestamp
                val activeDriver = array[5] as InsulinPumpDriver?
                val coordinatorState = array[6] as PumpCoordinatorState
                val syncStatus = array[7] as PumpSyncStatus
                val nextWakeup = array[8] as Timestamp?

                PumpUiData(
                    connected = connected,
                    pumpName = pumpName,
                    model = hardware?.model,
                    manufacturer = hardware?.manufacturer,
                    serialNumber = hardware?.serialNumber,
                    status = status,
                    lastConnection = lastConn,
                    lifecycleState = status.lifecycleState,
                    jobs = jobs,
                    coordinatorState = coordinatorState,
                    syncStatus = syncStatus,
                    nextWakeupTime = nextWakeup,
                    isSuspended = status.pumpSuspended,
                    hasError = jobs.any { it.lastError != null },
                    pump = pump,
                    driver = activeDriver
                )
            }
        }
    }

    private val apsInfo = combine(
        systemOrchestrator.apsMode,
        systemOrchestrator.apsIssues,
        systemOrchestrator.lastSuccessfulCoreCalculation
    ) { mode, issues, lastCalcTs ->
        val modeValueStatus = when (mode) {
            ApsMode.AutoCorrection -> ValueStatus.GOOD
            ApsMode.Manual -> ValueStatus.GOOD
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
            issues = issues.toList()
        )
    }

    // UI state is a combination of base flows

    val uiState: StateFlow<SystemControlUiState> = combine(
        apsInfo,
        sourceInfo,
        pumpInfo,
        lastInsulinTimestamp,
        androidSystemInfo
    ) { apsSystem, gInfo, pInfo, lastInsulinTs, androidSystem ->
        // Overview Tab State
        val overviewGlucoseSource: OverviewGlucoseSourceUiState = if (gInfo == null) {
            OverviewGlucoseSourceUiState.NoneConfigured
        } else {
            val (glucoseSourceStatus, glucoseValueStatus) = when {
                gInfo.isExpired -> GlucoseSourceStatus.Expired to ValueStatus.BAD
                gInfo.status == GlucoseSourceStatus.Error -> GlucoseSourceStatus.Error to ValueStatus.BAD
                gInfo.status == GlucoseSourceStatus.Expired -> GlucoseSourceStatus.Expired to ValueStatus.WARNING
                else -> GlucoseSourceStatus.Ok to ValueStatus.GOOD
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
            val sensorExpiration = gInfo.expiration
            val sensorExpirationStatus = when {
                sensorExpiration == null || sensorExpiration.isInvalid() -> ValueStatus.GOOD
                sensorExpiration <= Timestamp.now() -> ValueStatus.BAD
                sensorExpiration < Timestamp.now().plusHours(SENSOR_EXPIRATION_WARNING_THRESHOLD_HOURS) -> ValueStatus.WARNING
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
                    value = sensorExpiration,
                    status = sensorExpirationStatus
                )
            )
        }

        val overviewInsulinPump: OverviewPumpUiState = if (pInfo == null) {
            OverviewPumpUiState.NoneConfigured
        } else {
            val isExpired = when (val lifecycle = pInfo.lifecycleState) {
                is PumpLifecycleState.Patch -> lifecycle.isExpired
                is PumpLifecycleState.Durable -> false
            }
            val (pumpOverviewState, pumpValueStatus) = when {
                isExpired -> OverviewPumpState.EXPIRED to ValueStatus.BAD
                pInfo.hasError -> OverviewPumpState.ERROR to ValueStatus.BAD
                pInfo.isSuspended -> OverviewPumpState.SUSPENDED to ValueStatus.WARNING
                else -> OverviewPumpState.ACTIVE to ValueStatus.GOOD
            }
            val lastConnectionStatus = when {
                pInfo.lastConnection.isInvalid() || pInfo.lastConnection < Timestamp.now().minusMinutes(CONNECTION_BAD_THRESHOLD_MINUTES) -> ValueStatus.BAD
                pInfo.lastConnection < Timestamp.now().minusMinutes(CONNECTION_WARNING_THRESHOLD_MINUTES) -> ValueStatus.WARNING
                else -> ValueStatus.GOOD
            }
            val (nextExpirationItem, nextExpirationType) = when (val lifecycle = pInfo.lifecycleState) {
                is PumpLifecycleState.Patch -> {
                    val exp = if (lifecycle.activeUntil.isValid()) ExpirationDate.Hard(lifecycle.activeUntil) else null
                    exp to PumpReplaceableComponent.PATCH
                }
                is PumpLifecycleState.Durable -> {
                    val exp = lifecycle.approximateCatheterExpiration?.let { ExpirationDate.Approximate(it) }
                    exp to PumpReplaceableComponent.CATHETER
                }
            }
            val nextExpirationStatus = when (nextExpirationItem) {
                null -> ValueStatus.GOOD
                is ExpirationDate.Hard -> {
                    val ts = nextExpirationItem.dateTime
                    when {
                        ts.isInvalid() -> ValueStatus.GOOD
                        ts <= Timestamp.now() -> ValueStatus.BAD
                        ts < Timestamp.now().plusHours(CATHETER_CHANGE_WARNING_THRESHOLD_HOURS) -> ValueStatus.WARNING
                        else -> ValueStatus.GOOD
                    }
                }
                is ExpirationDate.Approximate -> {
                    val date = nextExpirationItem.date
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
                lastInsulin = StatusMetric(value = lastInsulinTs, status = if (lastInsulinTs == null) ValueStatus.WARNING else ValueStatus.GOOD),
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
                nextExpiration = StatusMetric(
                    value = nextExpirationItem,
                    status = nextExpirationStatus
                ),
                nextExpirationType = nextExpirationType,
                insulinConcentration = pInfo.pump?.insulinConcentration?.value ?: InsulinConcentration.U100
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
            val driver = gInfo.driver
            val source = gInfo.source
            val section = if (driver != null && source != null) driver.getGlucoseSourceControlSection(source) else null
            val sourceSettings: (@Composable (onClose: () -> Unit) -> Unit)? = if (driver != null && source != null && driver.getSettingsScreen(source) {} != null) {
                { onClose ->
                    driver.getSettingsScreen(source, onClose)?.invoke()
                }
            } else null

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
                expiration = gInfo.expiration,
                isExpired = gInfo.isExpired,
                glucoseSourcePluginSection = section,
                glucoseSourceSettingsScreen = sourceSettings
            )
        }

        // Pump Tab State
        val pumpTabState: PumpTabUiState = if (pInfo == null) {
            PumpTabUiState.NoneConfigured
        } else {
            val pumpJobsList = pInfo.jobs.map { job ->
                val titleText = when (val type = job.type) {
                    is BolusJobType.Deliver -> UiText.StringResource(R.string.system_control_pump_job_type_bolus, type.amount.iu)
                    is BolusJobType.Stop -> UiText.StringResource(R.string.system_control_pump_job_type_cancel_bolus)
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

            val driver = pInfo.driver
            val pump = pInfo.pump
            val section = if (driver != null && pump != null) driver.getPumpTabSection(pump) else null
            val pumpSettings: (@Composable (onClose: () -> Unit) -> Unit)? = if (driver != null && pump != null && driver.getSettingsScreen(pump) {} != null) {
                { onClose ->
                    driver.getSettingsScreen(pump, onClose)?.invoke()
                }
            } else null

            PumpTabUiState.Content(
                pumpName = pInfo.pumpName,
                batteryPercent = pInfo.status.batteryRemainingPercent,
                reservoirRemaining = pInfo.status.reservoirRemainingUnits,
                insulinConcentration = pInfo.pump?.insulinConcentration?.value ?: InsulinConcentration.U100,
                lastConnectionTimestamp = pInfo.lastConnection,
                manufacturer = pInfo.manufacturer,
                pumpModel = pInfo.model,
                serialNumber = pInfo.serialNumber,
                pumpConnected = pInfo.connected,
                isSuspended = pInfo.isSuspended,
                lifecycleState = pInfo.lifecycleState,
                pendingJobs = pumpJobsList,
                coordinatorState = pInfo.coordinatorState,
                syncStatus = pInfo.syncStatus,
                nextWakeupTime = pInfo.nextWakeupTime,
                pumpPluginSection = section,
                pumpSettingsScreen = pumpSettings
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

    fun cancelPumpJob(jobId: String) {
        pumpManager.cancelJobs { it.id == jobId }
    }

    fun refreshPumpStatus() {
        pumpManager.requestSync()
    }

    fun resetPumpCommunication() {
        pumpManager.reset()
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