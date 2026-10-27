package de.dh.daps.notifications

import de.dh.daps.common.domain.model.data.BgReading
import de.dh.daps.common.domain.model.data.BgSampleKind
import de.dh.daps.common.domain.model.data.BgValue
import de.dh.daps.core.repository.GlucoseRepository

data class MainAppNotificationData(
    val lastBgSample: BgReading?,
    val secondToLastBgSample: BgReading?
) {
    fun getBgDelta(): BgValue? {
        if (lastBgSample == null || lastBgSample.sampleKind != BgSampleKind.Value
            || secondToLastBgSample == null || secondToLastBgSample.sampleKind != BgSampleKind.Value
        ) {
            return null
        }
        return BgValue.fromMgDl(lastBgSample.value.mgdl - secondToLastBgSample.value.mgdl)
    }

    companion object {
        fun create(glucoseRepository: GlucoseRepository): MainAppNotificationData {
            val lastBg = glucoseRepository.currentBg.value
            val secondToLastBg = glucoseRepository.lastBg
            return MainAppNotificationData(lastBg, secondToLastBg)
        }
    }
}