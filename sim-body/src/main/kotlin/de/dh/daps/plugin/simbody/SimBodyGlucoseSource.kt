package de.dh.daps.plugin.simbody

import de.dh.daps.common.model.GlucoseSource
import de.dh.daps.common.model.data.BgReading
import de.dh.daps.common.model.data.BgReadingsInterval
import de.dh.daps.common.model.data.Minutes
import de.dh.daps.common.ui.UiText
import kotlinx.coroutines.flow.Flow

class SimBodyGlucoseSource(
    private val glucoseReadings: Flow<BgReading>
): GlucoseSource {
    override val glucoseSourceId: String = SOURCE_ID
    override val sourceDisplayName: UiText = UiText.StringResource(R.string.sim_body_cgm_sensor_display_name)
    override val readingsInterval: BgReadingsInterval
        get() = BgReadingsInterval.FiveMinutes
    override val readingsTimeDelay = DEFAULT_READINGS_DELAY
    override fun getSensorTypeName() = "Sim Body Dexcom G6"

    override fun start() {
    }

    override fun stop() {
    }

    override fun getValues(): Flow<BgReading> {
        return glucoseReadings
    }

    companion object {
        const val SOURCE_ID = "simbody-glucose-source"
        val DEFAULT_READINGS_DELAY = Minutes(5)
    }
}