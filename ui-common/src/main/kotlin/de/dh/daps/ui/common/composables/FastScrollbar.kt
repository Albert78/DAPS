package de.dh.daps.ui.common.composables

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Fast-scroll bar for [LazyListState] with auto-hiding thumb and optional floating text badge.
 *
 * @param listState State of the [LazyColumn] / list being scrolled.
 * @param modifier Modifier for the scrollbar container.
 * @param getBadgeText Optional provider function mapping an item index to a text string (e.g. formatted date/time).
 * @param autoHideDelayMs Delay in milliseconds before hiding the scrollbar when scrolling stops.
 * @param thumbHeight Height of the scrollbar thumb pill.
 * @param thumbWidth Width of the scrollbar thumb pill.
 */
@Composable
fun FastScrollbar(
    listState: LazyListState,
    modifier: Modifier = Modifier,
    getBadgeText: ((index: Int) -> String?)? = null,
    autoHideDelayMs: Long = 1500L,
    thumbHeight: Dp = 48.dp,
    thumbWidth: Dp = 8.dp
) {
    val coroutineScope = rememberCoroutineScope()
    var isDragging by remember { mutableStateOf(false) }
    var dragProgress by remember { mutableFloatStateOf(0f) }

    val isScrollInProgress = listState.isScrollInProgress
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(isScrollInProgress, isDragging) {
        if (isScrollInProgress || isDragging) {
            isVisible = true
        } else {
            delay(autoHideDelayMs)
            isVisible = false
        }
    }

    val totalItems by remember { derivedStateOf { listState.layoutInfo.totalItemsCount } }
    if (totalItems <= 1) return

    val firstVisibleIndex by remember { derivedStateOf { listState.firstVisibleItemIndex } }
    val visibleItemsCount by remember {
        derivedStateOf { listState.layoutInfo.visibleItemsInfo.size.coerceAtLeast(1) }
    }

    val maxScrollIndex = (totalItems - visibleItemsCount).coerceAtLeast(1)
    val currentProgress = if (isDragging) {
        dragProgress
    } else {
        (firstVisibleIndex.toFloat() / maxScrollIndex).coerceIn(0f, 1f)
    }

    val currentItemIndex = if (isDragging) {
        (dragProgress * (totalItems - 1)).roundToInt().coerceIn(0, totalItems - 1)
    } else {
        firstVisibleIndex.coerceIn(0, totalItems - 1)
    }

    val badgeText = getBadgeText?.invoke(currentItemIndex)

    val alpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "FastScrollbarAlpha"
    )

    if (alpha > 0f) {
        BoxWithConstraints(
            modifier = modifier
                .alpha(alpha)
                .width(56.dp)
        ) {
            val trackHeightPx = constraints.maxHeight.toFloat()
            val density = LocalDensity.current
            val thumbHeightPx = with(density) { thumbHeight.toPx() }
            val availableTrackHeightPx = (trackHeightPx - thumbHeightPx).coerceAtLeast(1f)

            val thumbOffsetYPx = currentProgress * availableTrackHeightPx
            val thumbOffsetYDp = with(density) { thumbOffsetYPx.toDp() }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(totalItems) {
                        detectVerticalDragGestures(
                            onDragStart = { offset ->
                                isDragging = true
                                val newProgress = ((offset.y - thumbHeightPx / 2) / availableTrackHeightPx).coerceIn(0f, 1f)
                                dragProgress = newProgress
                                val targetIndex = (newProgress * (totalItems - 1)).roundToInt().coerceIn(0, totalItems - 1)
                                coroutineScope.launch {
                                    listState.scrollToItem(targetIndex)
                                }
                            },
                            onDragEnd = { isDragging = false },
                            onDragCancel = { isDragging = false },
                            onVerticalDrag = { change, _ ->
                                change.consume()
                                isDragging = true
                                val newProgress = ((change.position.y - thumbHeightPx / 2) / availableTrackHeightPx).coerceIn(0f, 1f)
                                dragProgress = newProgress
                                val targetIndex = (newProgress * (totalItems - 1)).roundToInt().coerceIn(0, totalItems - 1)
                                coroutineScope.launch {
                                    listState.scrollToItem(targetIndex)
                                }
                            }
                        )
                    }
            ) {
                if (!badgeText.isNullOrEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        tonalElevation = 6.dp,
                        shadowElevation = 4.dp,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(y = thumbOffsetYDp, x = (-16).dp)
                    ) {
                        Text(
                            text = badgeText,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(y = thumbOffsetYDp)
                        .padding(end = 4.dp)
                        .width(thumbWidth)
                        .height(thumbHeight)
                        .clip(CircleShape)
                        .background(
                            if (isDragging) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
                        )
                )
            }
        }
    }
}