package de.dh.daps.common.domain.model.therapy

import de.dh.daps.common.DEFAULT_BG_LOW_THRESHOLD_MGDL
import de.dh.daps.common.DEFAULT_BG_TARGET_MGDL
import de.dh.daps.common.MINUTES_PER_DAY
import de.dh.daps.common.domain.model.data.BgBlock
import de.dh.daps.common.domain.model.data.BgDelta
import de.dh.daps.common.domain.model.data.BgValue
import de.dh.daps.common.domain.model.data.Minutes
import de.dh.daps.common.domain.model.data.getAmountForMinute
import de.dh.daps.common.domain.model.data.getBgForMinute

/**
 * A segment of a 24-hour day profile representing constant therapy factors.
 */
data class DayProfileSegment(
    val startMinute: Minutes,
    val endMinute: Minutes,
    val targetBg: BgValue,
    val lowThreshold: BgValue,
    val basalRate: InsulinAmount,
    val isf: BgDelta,
    val cr: Double
) {
    fun containsMinute(minute: Minutes): Boolean =
        minute.value >= startMinute.value && minute.value < endMinute.value
}

/**
 * Materialized 24-hour day profile timeline covering 0 to 1440 minutes.
 */
data class DayProfileTimeline(
    val segments: List<DayProfileSegment>
) {
    /**
     * Gets the segment for the given minute since midnight.
     */
    fun getAt(minuteSinceMidnight: Minutes): DayProfileSegment {
        if (segments.isEmpty()) {
            return DEFAULT_SEGMENT
        }
        val min = minuteSinceMidnight.value.toInt().coerceIn(0, MINUTES_PER_DAY - 1)
        for (segment in segments) {
            if (min >= segment.startMinute.value.toInt() && min < segment.endMinute.value.toInt()) {
                return segment
            }
        }
        return segments.last()
    }

    companion object {
        private val DEFAULT_SEGMENT = DayProfileSegment(
            startMinute = Minutes(0),
            endMinute = Minutes(MINUTES_PER_DAY.toShort()),
            targetBg = BgValue.fromMgDl(DEFAULT_BG_TARGET_MGDL),
            lowThreshold = BgValue.fromMgDl(DEFAULT_BG_LOW_THRESHOLD_MGDL),
            basalRate = InsulinAmount(0.0),
            isf = BgDelta.fromMgDl(0),
            cr = 0.0
        )

        /**
         * Creates a [DayProfileTimeline] by building non-overlapping segments from the provided blocks.
         */
        fun create(
            insulinProfile: InsulinProfile,
            defaultBgBlocks: List<BgBlock>
        ): DayProfileTimeline {
            val boundaries = mutableSetOf(0, MINUTES_PER_DAY)

            fun collectBoundariesFromBlocks(durations: List<Int>) {
                var acc = 0
                for (d in durations) {
                    acc += d
                    if (acc in 1 until MINUTES_PER_DAY) {
                        boundaries.add(acc)
                    }
                }
            }

            collectBoundariesFromBlocks(defaultBgBlocks.map { it.duration.value.toInt() })
            collectBoundariesFromBlocks(insulinProfile.basalBlocks.map { it.duration.value.toInt() })
            collectBoundariesFromBlocks(insulinProfile.isfBlocks.map { it.duration.value.toInt() })
            collectBoundariesFromBlocks(insulinProfile.crBlocks.map { it.duration.value.toInt() })

            val sortedBoundaries = boundaries.sorted()
            val segments = mutableListOf<DayProfileSegment>()

            for (i in 0 until sortedBoundaries.size - 1) {
                val start = sortedBoundaries[i]
                val end = sortedBoundaries[i + 1]
                val sampleMin = Minutes(start.toShort())

                val (targetBg, lowThreshold) = defaultBgBlocks.getBgForMinute(sampleMin)
                val basal = insulinProfile.basalBlocks.getAmountForMinute(sampleMin)
                val isfAmount = insulinProfile.isfBlocks.getAmountForMinute(sampleMin)
                val cr = insulinProfile.crBlocks.getAmountForMinute(sampleMin)

                segments.add(
                    DayProfileSegment(
                        startMinute = Minutes(start.toShort()),
                        endMinute = Minutes(end.toShort()),
                        targetBg = targetBg,
                        lowThreshold = lowThreshold,
                        basalRate = InsulinAmount(basal),
                        isf = BgDelta.fromMgDl(isfAmount.toInt()),
                        cr = cr
                    )
                )
            }

            return DayProfileTimeline(segments)
        }
    }
}