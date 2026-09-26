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
    override val glucoseSourceName: UiText = UiText.StringResource(R.string.sim_body_glucose_source_name)

    override val dataProviderType: String = "CGM"

    override val readingsInterval: BgReadingsInterval
        get() = BgReadingsInterval.FiveMinutes

    override val readingsTimeDelay = DEFAULT_READINGS_DELAY

    override fun getSensorTypeName() = "Sim Body Dexcom G6 Plugin"

    override fun start() {
    }

    override fun stop() {
    }

    override fun getValues(): Flow<BgReading> {
        return glucoseReadings
    }

    companion object {
        val DEFAULT_READINGS_DELAY = Minutes(5)
    }
}