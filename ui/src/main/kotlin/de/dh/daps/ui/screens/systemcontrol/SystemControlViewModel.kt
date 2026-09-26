package de.dh.daps.ui.screens.systemcontrol

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import de.dh.daps.common.model.GlucoseSource
import de.dh.daps.common.model.HardwareInformation
import de.dh.daps.common.model.InsulinPumpStatus
import de.dh.daps.common.model.ReplaceableComponent
import de.dh.daps.common.model.data.BgReading
import de.dh.daps.common.model.data.BgReadingsInterval
import de.dh.daps.common.model.data.GlucoseUnit
import de.dh.daps.common.model.data.Timestamp
import de.dh.daps.common.ui.UiText
import de.dh.daps.core.SystemRegistry
import de.dh.daps.core.aps.CoreInsight
import de.dh.daps.core.pump.JobErrorCode
import de.dh.daps.core.pump.PumpCommand
import de.dh.daps.core.pump.PumpJob
import de.dh.daps.glucoseUnit
import de.dh.daps.ui.R
import de.dh.daps.ui.common.time
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn

enum class ValueStatus {
    GOOD,
    WARNING,
    BAD
}

data class StatusValueItem(
    val label: String,
    val value: String,
    val relativeTime: String? = null,
    val timestamp: Timestamp? = null,
    val status: ValueStatus? = null
)

data class AndroidSystemUiState(
    val title: String = "Android",
    val bluetoothStatus: StatusValueItem = StatusValueItem("Bluetooth-Status", "Aktiviert", status = ValueStatus.GOOD),
    val phoneBatteryStatus: StatusValueItem = StatusValueItem("Batteriestatus Telefon", "82%", status = ValueStatus.GOOD),
    val permissionsStatus: StatusValueItem = StatusValueItem("Berechtigungen", "Alle erteilt", status = ValueStatus.GOOD),
    val dapsServiceStatus: StatusValueItem = StatusValueItem("DAPS-System-Service", "Aktiv", status = ValueStatus.GOOD)
)

data class ApsSystemUiState(
    val title: String = "APS-System",
    val mode: StatusValueItem = StatusValueItem("APS-Modus", "Auto-Korrektur", status = ValueStatus.GOOD),
    val lastCalculation: StatusValueItem = StatusValueItem("Letzte Berechnung", "--", status = ValueStatus.GOOD),
    val status: StatusValueItem = StatusValueItem("Status", "Aktiv", status = ValueStatus.GOOD)
)

data class OverviewGlucoseSourceUiState(
    val sensorName: UiText = UiText.DynamicString("--"),
    val lastConnection: StatusValueItem = StatusValueItem("Letzte Verbindung", "--", status = ValueStatus.GOOD),
    val lastReading: StatusValueItem = StatusValueItem("Letzter Messwert", "--", status = ValueStatus.GOOD),
    val sensorExpiration: StatusValueItem = StatusValueItem("Ablaufdatum Sensor", "--", status = ValueStatus.GOOD)
)

data class OverviewPumpUiState(
    val pumpName: String = "--",
    val status: StatusValueItem = StatusValueItem("Status", "Inaktiv", status = ValueStatus.GOOD),
    val lastBolus: StatusValueItem = StatusValueItem("Letzter Bolus", "--", status = ValueStatus.GOOD),
    val batteryStatus: StatusValueItem = StatusValueItem("Batteriestatus", "--", status = ValueStatus.GOOD),
    val reservoirStatus: StatusValueItem = StatusValueItem("Reservoir-Füllstand", "--", status = ValueStatus.GOOD),
    val lastConnection: StatusValueItem = StatusValueItem("Letzte Verbindung", "--", status = ValueStatus.GOOD),
    val nextPodChange: StatusValueItem = StatusValueItem("Nächster Pod-Wechsel", "--", status = ValueStatus.GOOD)
)

data class OverviewTabUiState(
    val androidSystem: AndroidSystemUiState = AndroidSystemUiState(),
    val apsSystem: ApsSystemUiState = ApsSystemUiState(),
    val glucoseSource: OverviewGlucoseSourceUiState = OverviewGlucoseSourceUiState(),
    val pump: OverviewPumpUiState = OverviewPumpUiState()
)

data class GlucoseSourceTabUiState(
    val glucoseSourceName: UiText? = null,
    val manufacturer: String? = null,
    val serialNumber: String? = null,
    val sensorTypeName: String? = null,
    val readingsIntervalText: String? = null,
    val lastBgReading: BgReading? = null,
    val lastBgValueText: String? = null,
    val lastReadingTimeText: String? = null,
    val lastReadingRelativeTimeText: String? = null,
    val nextPredictedTimestamp: Timestamp? = null,
    val nextReadingTimeText: String? = null,
    val nextReadingRelativeTimeText: String? = null,
    val hasNextPrediction: Boolean = false,
    val sensorCode: String? = null,
    val transmitterSerialNumber: String? = null,
    val estimatedExpirationTimestamp: Timestamp? = null,
    val estimatedExpirationDateText: String? = null,
    val showPluginSection: Boolean = false,
    val glucoseSourcePluginSection: (@Composable () -> Unit)? = null
)

data class PumpJobItem(
    val id: String,
    val title: UiText,
    val errorMessage: UiText? = null,
    val hasError: Boolean = errorMessage != null
)

data class PumpTabUiState(
    val pumpModel: String? = null,
    val manufacturer: String? = null,
    val serialNumber: String? = null,
    val pumpConnected: Boolean = false,
    val isSuspended: Boolean = false,
    val batteryPercentText: String = "--",
    val reservoirUnits: Double? = null,
    val reservoirText: String = "--",
    val lastConnectionTimestamp: Timestamp? = null,
    val lastConnectionTimeText: String = "--",
    val lastConnectionRelativeTimeText: String? = null,
    val pendingJobs: List<PumpJobItem> = emptyList(),
    val pumpPluginSection: (@Composable () -> Unit)? = null
)

data class SystemControlUiState(
    val overviewUiState: OverviewTabUiState = OverviewTabUiState(),
    val glucoseSourceTabUiState: GlucoseSourceTabUiState = GlucoseSourceTabUiState(),
    val pumpTabUiState: PumpTabUiState = PumpTabUiState(),

    val coreInsights: List<CoreInsight> = emptyList(),
    val glucoseSourceName: UiText? = glucoseSourceTabUiState.glucoseSourceName,
    val sensorTypeName: String? = glucoseSourceTabUiState.sensorTypeName,
    val readingsInterval: BgReadingsInterval? = null,
    val lastBgReading: BgReading? = null,
    val nextPredictedTimestamp: Timestamp = Timestamp.INVALID,
    val glucoseUnit: GlucoseUnit = GlucoseUnit.MG_DL,
    val glucoseSourcePluginUiProvider: GlucoseSourcePluginUiProvider? = null,
    val pumpPluginUiProvider: PumpPluginUiProvider? = null,
    val pumpConnected: Boolean = pumpTabUiState.pumpConnected,
    val pumpModel: String? = pumpTabUiState.pumpModel,
    val pumpStatus: InsulinPumpStatus? = null,
    val lastPumpConnection: Timestamp = Timestamp.INVALID,
    val pendingPumpJobs: List<PumpJob> = emptyList()
)

@OptIn(ExperimentalCoroutinesApi::class)
class SystemControlViewModel(
    systemRegistry: SystemRegistry
) : ViewModel() {
    private val systemMetricsRepository = systemRegistry.systemMetricsRepository
    private val glucoseRepository = systemRegistry.glucoseRepository
    private val glucoseSourceManager = systemRegistry.glucoseSourceManager
    private val appPreferencesRepository = systemRegistry.appPreferencesRepository
    private val pumpManager = systemRegistry.pumpManager

    private val glucoseInfo = combine(
        glucoseSourceManager.activeGlucoseSource,
        glucoseRepository.currentBg,
        appPreferencesRepository.cachedPreferences,
        glucoseSourceManager.lastInputTimestamp
    ) { source, currentBg, preferences, lastInput ->
        val sourceName = source?.let { UiText.DynamicString(it.glucoseSourceId) }
        val sensorType = source?.getSensorTypeName()
        val interval = source?.readingsInterval
        val intervalText = when (interval) {
            BgReadingsInterval.OneMinute -> "1 Minute"
            BgReadingsInterval.FiveMinutes -> "5 Minuten"
            BgReadingsInterval.AdHoc -> "Ad-hoc"
            null -> null
        }

        val lastBgText = currentBg?.value?.let { bg ->
            "${bg.toString(preferences.glucoseUnit)} ${if (preferences.glucoseUnit == GlucoseUnit.MG_DL) "mg/dl" else "mmol/l"}"
        }

        val lastReadingTimeText = currentBg?.timestamp?.let { time(it) }

        val nextPredicted = glucoseSourceManager.predictNextValueTimestamp()
        val hasPrediction = nextPredicted.isValid()
        val nextReadingTimeText = if (hasPrediction) time(nextPredicted) else null

        val replaceable = source as? ReplaceableComponent
        val expTimestamp = replaceable?.endDate

        val provider = source as? GlucoseSourcePluginUiProvider

        GlucoseUiData(
            source = source,
            sourceName = sourceName,
            sensorTypeName = sensorType,
            readingsInterval = interval,
            readingsIntervalText = intervalText,
            lastBgReading = currentBg,
            lastBgValueText = lastBgText,
            lastReadingTimeText = lastReadingTimeText,
            nextPredictedTimestamp = if (hasPrediction) nextPredicted else null,
            nextReadingTimeText = nextReadingTimeText,
            hasNextPrediction = hasPrediction,
            estimatedExpirationTimestamp = expTimestamp,
            glucoseUnit = preferences.glucoseUnit,
            pluginUiProvider = provider,
            lastInputTimestamp = lastInput
        )
    }

    private val pumpInfo = pumpManager.activeInsulinPump.flatMapLatest { pump ->
        if (pump == null) {
            flowOf(PumpUiData())
        } else {
            val coordinator = pumpManager.pumpCoordinator
            val jobsFlow = coordinator?.pendingJobs ?: flowOf(emptyList())
            val lastConnFlow = coordinator?.lastConnectionTime ?: flowOf(Timestamp.INVALID)

            combine(
                pump.isConnected,
                pump.hardwareInformation,
                pump.pumpStatus,
                jobsFlow,
                lastConnFlow
            ) { connected: Boolean, hardware: HardwareInformation?, status: InsulinPumpStatus?, jobs: List<PumpJob>, lastConn: Timestamp ->
                PumpUiData(
                    connected = connected,
                    model = hardware?.model,
                    manufacturer = hardware?.manufacturer,
                    serialNumber = hardware?.serialNumber,
                    status = status,
                    lastConnection = lastConn,
                    jobs = jobs,
                    isSuspended = status?.pumpSuspended == true,
                    pluginUiProvider = pump as? PumpPluginUiProvider
                )
            }
        }
    }

    val uiState: StateFlow<SystemControlUiState> = combine(
        systemMetricsRepository.observeInsights(),
        glucoseInfo,
        pumpInfo
    ) { insights, gInfo, pInfo ->
        // Overview Tab State
        val lastCalcInsight = insights.firstOrNull()
        val lastCalcTimeText = lastCalcInsight?.timestamp?.let { time(it) } ?: "--"

        val cgmLastConnText = if (gInfo.lastInputTimestamp.isValid()) time(gInfo.lastInputTimestamp) else "--"
        val pumpLastConnText = if (pInfo.lastConnection.isValid()) time(pInfo.lastConnection) else "--"

        val overviewState = OverviewTabUiState(
            androidSystem = AndroidSystemUiState(
                bluetoothStatus = StatusValueItem("Bluetooth-Status", "Aktiviert", status = ValueStatus.GOOD),
                phoneBatteryStatus = StatusValueItem("Batteriestatus Telefon", "82%", status = ValueStatus.GOOD),
                permissionsStatus = StatusValueItem("Berechtigungen", "Alle erteilt", status = ValueStatus.GOOD),
                dapsServiceStatus = StatusValueItem("DAPS-System-Service", "Aktiv", status = ValueStatus.GOOD)
            ),
            apsSystem = ApsSystemUiState(
                mode = StatusValueItem("APS-Modus", "Auto-Korrektur", status = ValueStatus.GOOD),
                lastCalculation = StatusValueItem("Letzte Berechnung", lastCalcTimeText, timestamp = lastCalcInsight?.timestamp, status = ValueStatus.GOOD),
                status = StatusValueItem("Status", if (insights.isNotEmpty()) "Aktiv" else "Inaktiv", status = ValueStatus.GOOD)
            ),
            glucoseSource = OverviewGlucoseSourceUiState(
                sensorName = gInfo.sourceName ?: UiText.DynamicString("Nicht verbunden"),
                lastConnection = StatusValueItem("Letzte Verbindung", cgmLastConnText, timestamp = gInfo.lastInputTimestamp, status = if (gInfo.source != null) ValueStatus.GOOD else ValueStatus.BAD),
                lastReading = StatusValueItem("Letzter Messwert", gInfo.lastBgValueText ?: "--", timestamp = gInfo.lastBgReading?.timestamp, status = ValueStatus.GOOD),
                sensorExpiration = StatusValueItem("Ablaufdatum Sensor", "--", timestamp = gInfo.estimatedExpirationTimestamp, status = ValueStatus.GOOD)
            ),
            pump = OverviewPumpUiState(
                pumpName = pInfo.model ?: "Nicht verbunden",
                status = StatusValueItem("Status", if (!pInfo.connected) "Nicht verbunden" else if (pInfo.isSuspended) "Unterbrochen" else "Aktiv", status = if (pInfo.connected) ValueStatus.GOOD else ValueStatus.BAD),
                lastBolus = StatusValueItem("Letzter Bolus", "--", status = ValueStatus.GOOD),
                batteryStatus = StatusValueItem("Batteriestatus", pInfo.status?.let { "${it.batteryRemainingPercent}%" } ?: "--", status = ValueStatus.GOOD),
                reservoirStatus = StatusValueItem("Reservoir-Füllstand", pInfo.status?.let { "${it.reservoirRemainingUnits.iu} I.E." } ?: "--", status = ValueStatus.GOOD),
                lastConnection = StatusValueItem("Letzte Verbindung", pumpLastConnText, timestamp = pInfo.lastConnection, status = ValueStatus.GOOD),
                nextPodChange = StatusValueItem("Nächster Pod-Wechsel", "--", status = ValueStatus.GOOD)
            )
        )

        // Glucose Source Tab State
        val glucoseSourceTabState = GlucoseSourceTabUiState(
            glucoseSourceName = gInfo.sourceName,
            manufacturer = null,
            sensorTypeName = gInfo.sensorTypeName,
            readingsIntervalText = gInfo.readingsIntervalText,
            lastBgReading = gInfo.lastBgReading,
            lastBgValueText = gInfo.lastBgValueText ?: "--",
            lastReadingTimeText = gInfo.lastReadingTimeText ?: "--",
            nextPredictedTimestamp = gInfo.nextPredictedTimestamp,
            nextReadingTimeText = gInfo.nextReadingTimeText,
            hasNextPrediction = gInfo.hasNextPrediction,
            estimatedExpirationTimestamp = gInfo.estimatedExpirationTimestamp,
            showPluginSection = gInfo.source != null,
            glucoseSourcePluginSection = gInfo.pluginUiProvider?.let { provider -> { provider.GlucoseSourceControlSection() } }
        )

        // Pump Tab State
        val pumpJobsList = pInfo.jobs.map { job ->
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

        val pumpTabState = PumpTabUiState(
            pumpModel = pInfo.model,
            manufacturer = pInfo.manufacturer,
            serialNumber = pInfo.serialNumber,
            pumpConnected = pInfo.connected,
            isSuspended = pInfo.isSuspended,
            batteryPercentText = pInfo.status?.let { "${it.batteryRemainingPercent}%" } ?: "--",
            reservoirUnits = pInfo.status?.reservoirRemainingUnits?.iu,
            reservoirText = pInfo.status?.let { "${it.reservoirRemainingUnits.iu} I.E." } ?: "--",
            lastConnectionTimestamp = pInfo.lastConnection,
            lastConnectionTimeText = pumpLastConnText,
            pendingJobs = pumpJobsList,
            pumpPluginSection = pInfo.pluginUiProvider?.let { provider -> { provider.PumpControlSection() } }
        )

        SystemControlUiState(
            overviewUiState = overviewState,
            glucoseSourceTabUiState = glucoseSourceTabState,
            pumpTabUiState = pumpTabState,
            coreInsights = insights,
            glucoseSourceName = gInfo.sourceName,
            sensorTypeName = gInfo.sensorTypeName,
            readingsInterval = gInfo.readingsInterval,
            lastBgReading = gInfo.lastBgReading,
            nextPredictedTimestamp = gInfo.nextPredictedTimestamp ?: Timestamp.INVALID,
            glucoseUnit = gInfo.glucoseUnit,
            glucoseSourcePluginUiProvider = gInfo.pluginUiProvider,
            pumpPluginUiProvider = pInfo.pluginUiProvider,
            pumpConnected = pInfo.connected,
            pumpModel = pInfo.model,
            pumpStatus = pInfo.status,
            lastPumpConnection = pInfo.lastConnection,
            pendingPumpJobs = pInfo.jobs
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
        pumpManager.issueCommand(PumpCommand.RefreshStatus)
    }

    fun cancelPumpJob(jobId: String) {
        pumpManager.cancelJobs { it.id == jobId }
    }

    fun refreshPumpStatus() {
        pumpManager.issueCommand(PumpCommand.RefreshStatus)
    }

    private data class GlucoseUiData(
        val source: GlucoseSource?,
        val sourceName: UiText?,
        val sensorTypeName: String?,
        val readingsInterval: BgReadingsInterval?,
        val readingsIntervalText: String?,
        val lastBgReading: BgReading?,
        val lastBgValueText: String?,
        val lastReadingTimeText: String?,
        val nextPredictedTimestamp: Timestamp?,
        val nextReadingTimeText: String?,
        val hasNextPrediction: Boolean,
        val estimatedExpirationTimestamp: Timestamp?,
        val glucoseUnit: GlucoseUnit,
        val pluginUiProvider: GlucoseSourcePluginUiProvider?,
        val lastInputTimestamp: Timestamp
    )

    private data class PumpUiData(
        val connected: Boolean = false,
        val model: String? = null,
        val manufacturer: String? = null,
        val serialNumber: String? = null,
        val status: InsulinPumpStatus? = null,
        val lastConnection: Timestamp = Timestamp.INVALID,
        val jobs: List<PumpJob> = emptyList(),
        val isSuspended: Boolean = false,
        val pluginUiProvider: PumpPluginUiProvider? = null
    )

    companion object {
        class Factory(private val registry: SystemRegistry) : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                return SystemControlViewModel(registry) as T
            }
        }
    }
}