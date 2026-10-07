package de.dh.daps.plugin.glucose

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.dh.daps.common.model.GlucoseSource
import de.dh.daps.common.model.GlucoseSourceStatus
import de.dh.daps.common.model.SourceHardwareInformation
import de.dh.daps.common.model.data.BgReading
import de.dh.daps.common.model.data.BgReadingsInterval
import de.dh.daps.common.model.data.BgSampleKind
import de.dh.daps.common.model.data.BgValue
import de.dh.daps.common.model.data.Minutes
import de.dh.daps.common.model.data.RawBg
import de.dh.daps.common.model.data.Timestamp
import de.dh.daps.common.ui.UiText
import de.dh.daps.common.model.GlucoseSourcePluginUiProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlin.random.Random
import kotlin.time.Duration.Companion.milliseconds

class SampleGlucoseSource : GlucoseSource, GlucoseSourcePluginUiProvider {
    override val glucoseSourceId: String = SOURCE_ID
    override val sourceDisplayName: UiText = UiText.StringResource(R.string.sample_cgm_driver_display_name)
    override val readingsInterval: BgReadingsInterval
        get() = BgReadingsInterval.OneMinute
    override val readingsTimeDelay = Minutes(5)
    override val status: StateFlow<GlucoseSourceStatus> = MutableStateFlow(GlucoseSourceStatus.Ok)
    override val expirationDate: StateFlow<Timestamp?> = MutableStateFlow(null)
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

    @Composable
    override fun GlucoseSourceControlSection() {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "This content is provided by the Sample Glucose Source.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Version: 1.0.0-sample",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
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