package de.dh.daps.ui.controls.history

import de.dh.daps.common.model.data.BgReading
import de.dh.daps.common.model.data.BgSampleKind
import de.dh.daps.common.model.data.BgValue
import de.dh.daps.common.model.data.Timestamp
import kotlin.math.sin
import kotlin.random.Random

fun generatedBg(
    minsInterval: Short,
    index: Int,
    startTs: Timestamp,
    base: Double = 120.0,
    amplitude: Double = 20.0,
    noiseFactor: Double = 2.0
): BgReading {
    val curve = amplitude * sin(index * minsInterval / 50.0) + (amplitude * 0.75) * sin(index * minsInterval / 60.0)
    val noise = if (noiseFactor <= 0.0) 0.0 else Random.nextDouble(-noiseFactor, noiseFactor)
    return BgReading(
        value = BgValue.fromMgDl((base + curve + noise).toInt().coerceIn(40, 400)),
        sampleKind = BgSampleKind.Value,
        timestamp = startTs.plusMinutes(index * minsInterval)
    )
}

fun createSampleReadings(
    size: Int,
    minsInterval: Short,
    base: Double = 120.0,
    amplitude: Double = 20.0,
    noiseFactor: Double = 2.0
): List<BgReading> {
    val startTs = Timestamp.now().minusMinutes(minsInterval * size + 10)
    return List(size) { index ->
        generatedBg(
            minsInterval = minsInterval,
            index = index,
            startTs = startTs,
            base = base,
            amplitude = amplitude,
            noiseFactor = noiseFactor
        )
    }
}