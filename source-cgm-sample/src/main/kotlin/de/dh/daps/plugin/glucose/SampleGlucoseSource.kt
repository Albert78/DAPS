package de.dh.daps.plugin.glucose

import de.dh.daps.common.domain.model.data.BgReading
import de.dh.daps.common.domain.model.data.BgSampleKind
import de.dh.daps.common.domain.model.data.BgValue
import de.dh.daps.common.domain.model.data.Minutes
import de.dh.daps.common.domain.model.data.RawBg
import de.dh.daps.common.domain.model.data.Timestamp
import de.dh.daps.common.domain.model.cgm.GlucoseSource
import de.dh.daps.common.domain.model.cgm.GlucoseSourceStatus
import de.dh.daps.common.domain.model.cgm.SourceHardwareInformation
import de.dh.daps.common.domain.model.data.BgReadingsInterval
import de.dh.daps.common.ui.UiText
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlin.random.Random
import kotlin.time.Duration.Companion.milliseconds

class SampleGlucoseSource : GlucoseSource {
    override val glucoseSourceId: String = SOURCE_ID
    override val sourceDisplayName: UiText = UiText.StringResource(R.string.sample_cgm_driver_display_name)
    override val readingsInterval: BgReadingsInterval
        get() = BgReadingsInterval.OneMinute
    override val readingsTimeDelay = Minutes(5)
    override val status: StateFlow<GlucoseSourceStatus> = MutableStateFlow(GlucoseSourceStatus.Ok)
    override val startDate: StateFlow<Timestamp?> = MutableStateFlow(null)
    override val endDate: StateFlow<Timestamp?> = MutableStateFlow(null)
    override val isExpired: StateFlow<Boolean> = MutableStateFlow(false)
    override val lastConnection: StateFlow<Timestamp?> = MutableStateFlow(null)
    override val sensorType: StateFlow<String> = MutableStateFlow("Dexcom-G6")
    override val hardwareInformation: StateFlow<SourceHardwareInformation?> = MutableStateFlow(
        SourceHardwareInformation(
            manufacturer = "Sample Manufacturer",
            model = "Dexcom G6",
            serialNumber = "12345678"
        )
    )

    override fun start() {
        // Nothing to do
    }

    override fun stop() {
        // Nothing to do
    }

    fun getRawGlucoseReadings(): Flow<RawBg> = flow {
        while (true) {
            val reading = RawBg(
                value = BgValue.fromMgDl(100 + Random.nextInt(-10, 10)),
                timestamp = Timestamp(System.currentTimeMillis()),
            )
            emit(reading)
            delay((1000 * 60).milliseconds) // Emit minute for demo purposes
        }
    }

    override fun getValues(): Flow<BgReading> {
        return getRawGlucoseReadings().map { raw ->
            sampleMapRawValues(raw)
        }
    }

    private fun sampleMapRawValues(raw: RawBg): BgReading {
        // Sample decoding for raw values
        val kind = when (raw.value.mgdl.toInt()) {
            39 -> BgSampleKind.Low
            401 -> BgSampleKind.High
            0 -> BgSampleKind.Invalid
            else -> BgSampleKind.Value
        }
        return BgReading(
            value = raw.value,
            sampleKind = kind,
            timestamp = raw.timestamp
        )
    }

    companion object {
        const val SOURCE_ID = "sample-glucose-source"
    }
}