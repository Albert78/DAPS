package de.dh.daps.plugin.simbody

import de.dh.daps.common.model.Expiration
import de.dh.daps.common.model.GlucoseSource
import de.dh.daps.common.model.GlucoseSourceStatus
import de.dh.daps.common.model.SourceHardwareInformation
import de.dh.daps.common.model.data.BgReading
import de.dh.daps.common.model.data.BgReadingsInterval
import de.dh.daps.common.model.data.Minutes
import de.dh.daps.common.model.data.Timestamp
import de.dh.daps.common.ui.UiText
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.onEach

class SimBodyGlucoseSource(
    private val glucoseReadings: Flow<BgReading>,
    private val onStart: (() -> Unit)? = null
): GlucoseSource {
    override val glucoseSourceId: String = SOURCE_ID
    override val sourceDisplayName: UiText = UiText.StringResource(R.string.sim_body_cgm_sensor_display_name)
    override val readingsInterval: BgReadingsInterval
        get() = BgReadingsInterval.FiveMinutes
    override val readingsTimeDelay = DEFAULT_READINGS_DELAY
    override val status: StateFlow<GlucoseSourceStatus> = MutableStateFlow(GlucoseSourceStatus.Ok)
    override val startDate: StateFlow<Timestamp?> = MutableStateFlow(null)
    override val endDate: StateFlow<Expiration?> = MutableStateFlow(null)
    override val isExpired: StateFlow<Boolean> = MutableStateFlow(false)

    private val _lastConnection = MutableStateFlow<Timestamp?>(null)
    override val lastConnection: StateFlow<Timestamp?> = _lastConnection.asStateFlow()

    override val sensorType: StateFlow<String> = MutableStateFlow("Sim-Body-Sensor")
    override val hardwareInformation: StateFlow<SourceHardwareInformation?> = MutableStateFlow(
        SourceHardwareInformation(
            manufacturer = "DAPS",
            model = "Sim Body Sensor",
            serialNumber = "SIM-123456"
        )
    )

    override fun start() {
        onStart?.invoke()
    }

    override fun stop() {
    }

    override fun getValues(): Flow<BgReading> {
        return glucoseReadings.onEach {
            _lastConnection.value = Timestamp.now()
        }
    }

    companion object {
        const val SOURCE_ID = "simbody-glucose-source"
        val DEFAULT_READINGS_DELAY = Minutes(5)
    }
}