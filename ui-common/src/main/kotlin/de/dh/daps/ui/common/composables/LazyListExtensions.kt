package de.dh.daps.ui.common.composables

import androidx.compose.foundation.lazy.LazyListState
import kotlin.math.abs

/**
 * Animates scrolling to the [targetIndex]. If the distance from current position is larger than
 * [threshold], it first instantly jumps to an intermediate position near the target before
 * animating the remaining distance.
 */
suspend fun LazyListState.animateScrollToItemFast(
    targetIndex: Int,
    threshold: Int = 10
) {
    val currentIndex = firstVisibleItemIndex
    val distance = targetIndex - currentIndex
    if (abs(distance) > threshold) {
        val intermediateIndex = if (distance > 0) {
            (targetIndex - threshold).coerceAtLeast(currentIndex)
        } else {
            (targetIndex + threshold).coerceAtMost(currentIndex)
        }
        scrollToItem(intermediateIndex)
    }
    animateScrollToItem(targetIndex)
}