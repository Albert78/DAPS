package de.dh.daps.ui.common.composables

import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import de.dh.daps.ui.common.icons.Icon_Scrollview_Arrow_Down
import de.dh.daps.ui.common.icons.Icon_Scrollview_Arrow_Left
import de.dh.daps.ui.common.icons.Icon_Scrollview_Arrow_Right
import de.dh.daps.ui.common.icons.Icon_Scrollview_Arrow_Up

/**
 * Modifier showing arrow up and down overlays over the scrollable content
 * if the child is scrollable in the corresponding direction.
 */
@Composable
fun Modifier.contentScrollIndicator(
    scrollableState: ScrollableState,
    indicatorHeight: Dp = 32.dp,
    iconSize: Dp = 64.dp,
    topIconPadding: Dp = 16.dp,
    bottomIconPadding: Dp = 16.dp,
    surfaceAlpha: Float = 0.8f,
    showTopGradient: Boolean = true,
    showBottomGradient: Boolean = true
): Modifier {
    val surfaceColor =
        MaterialTheme.colorScheme.surface.copy(alpha = surfaceAlpha)
    val iconTint =
        MaterialTheme.colorScheme.onSurfaceVariant

    val upPainter = rememberVectorPainter(image = Icon_Scrollview_Arrow_Up)
    val downPainter = rememberVectorPainter(image = Icon_Scrollview_Arrow_Down)

    return this.then(
        Modifier.scrollIndicatorDrawModifier(
            scrollableState = scrollableState,
            surfaceColor = surfaceColor,
            iconTint = iconTint,
            upPainter = upPainter,
            downPainter = downPainter,
            indicatorHeight = indicatorHeight,
            iconSize = iconSize,
            topIconPadding = topIconPadding,
            bottomIconPadding = bottomIconPadding,
            showTopGradient = showTopGradient,
            showBottomGradient = showBottomGradient
        )
    )
}

private fun Modifier.scrollIndicatorDrawModifier(
    scrollableState: ScrollableState,
    surfaceColor: Color,
    iconTint: Color,
    upPainter: Painter,
    downPainter: Painter,
    indicatorHeight: Dp,
    iconSize: Dp,
    topIconPadding: Dp,
    bottomIconPadding: Dp,
    showTopGradient: Boolean,
    showBottomGradient: Boolean
): Modifier = this.drawWithContent {
    drawContent()

    val canScrollUp = scrollableState.canScrollBackward
    val canScrollDown = scrollableState.canScrollForward

    val indicatorHeightPx = indicatorHeight.toPx()
    val iconSizePx = iconSize.toPx()
    val topIconPaddingPx = topIconPadding.toPx()
    val bottomIconPaddingPx = bottomIconPadding.toPx()

    if (canScrollUp) {
        if (showTopGradient) {
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(surfaceColor, Color.Transparent),
                    startY = 0f,
                    endY = indicatorHeightPx
                ),
                size = Size(size.width, indicatorHeightPx)
            )
        }

        with(upPainter) {
            translate(
                left = (this@drawWithContent.size.width - iconSizePx) / 2f,
                top = topIconPaddingPx
            ) {
                draw(
                    size = Size(iconSizePx, iconSizePx),
                    colorFilter = ColorFilter.tint(iconTint)
                )
            }
        }
    }

    if (canScrollDown) {
        if (showBottomGradient) {
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Transparent, surfaceColor),
                    startY = size.height - indicatorHeightPx,
                    endY = size.height
                ),
                topLeft = Offset(0f, size.height - indicatorHeightPx),
                size = Size(size.width, indicatorHeightPx)
            )
        }

        with(downPainter) {
            translate(
                left = (this@drawWithContent.size.width - iconSizePx) / 2f,
                top = size.height - iconSizePx - bottomIconPaddingPx
            ) {
                draw(
                    size = Size(iconSizePx, iconSizePx),
                    colorFilter = ColorFilter.tint(iconTint)
                )
            }
        }
    }
}

/**
 * Modifier showing arrow left and right overlays over the scrollable content
 * if the child is scrollable horizontally in the corresponding direction.
 */
@Composable
fun Modifier.horizontalContentScrollIndicator(
    scrollableState: ScrollableState,
    indicatorWidth: Dp = 24.dp,
    iconSize: Dp = 20.dp,
    leftIconPadding: Dp = 2.dp,
    rightIconPadding: Dp = 2.dp,
    surfaceAlpha: Float = 0.8f,
    showLeftGradient: Boolean = true,
    showRightGradient: Boolean = true
): Modifier {
    val surfaceColor =
        MaterialTheme.colorScheme.surface.copy(alpha = surfaceAlpha)
    val iconTint =
        MaterialTheme.colorScheme.onSurfaceVariant

    val leftPainter = rememberVectorPainter(image = Icon_Scrollview_Arrow_Left)
    val rightPainter = rememberVectorPainter(image = Icon_Scrollview_Arrow_Right)

    return this.then(
        Modifier.horizontalScrollIndicatorDrawModifier(
            scrollableState = scrollableState,
            surfaceColor = surfaceColor,
            iconTint = iconTint,
            leftPainter = leftPainter,
            rightPainter = rightPainter,
            indicatorWidth = indicatorWidth,
            iconSize = iconSize,
            leftIconPadding = leftIconPadding,
            rightIconPadding = rightIconPadding,
            showLeftGradient = showLeftGradient,
            showRightGradient = showRightGradient
        )
    )
}

private fun Modifier.horizontalScrollIndicatorDrawModifier(
    scrollableState: ScrollableState,
    surfaceColor: Color,
    iconTint: Color,
    leftPainter: Painter,
    rightPainter: Painter,
    indicatorWidth: Dp,
    iconSize: Dp,
    leftIconPadding: Dp,
    rightIconPadding: Dp,
    showLeftGradient: Boolean,
    showRightGradient: Boolean
): Modifier = this.drawWithContent {
    drawContent()

    val canScrollLeft = scrollableState.canScrollBackward
    val canScrollRight = scrollableState.canScrollForward

    val indicatorWidthPx = indicatorWidth.toPx()
    val iconSizePx = iconSize.toPx()
    val leftIconPaddingPx = leftIconPadding.toPx()
    val rightIconPaddingPx = rightIconPadding.toPx()

    if (canScrollLeft) {
        if (showLeftGradient) {
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(surfaceColor, Color.Transparent),
                    startX = 0f,
                    endX = indicatorWidthPx
                ),
                size = Size(indicatorWidthPx, size.height)
            )
        }

        with(leftPainter) {
            translate(
                left = leftIconPaddingPx,
                top = (size.height - iconSizePx) / 2f
            ) {
                draw(
                    size = Size(iconSizePx, iconSizePx),
                    colorFilter = ColorFilter.tint(iconTint)
                )
            }
        }
    }

    if (canScrollRight) {
        if (showRightGradient) {
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(Color.Transparent, surfaceColor),
                    startX = size.width - indicatorWidthPx,
                    endX = size.width
                ),
                topLeft = Offset(size.width - indicatorWidthPx, 0f),
                size = Size(indicatorWidthPx, size.height)
            )
        }

        with(rightPainter) {
            translate(
                left = size.width - iconSizePx - rightIconPaddingPx,
                top = (size.height - iconSizePx) / 2f
            ) {
                draw(
                    size = Size(iconSizePx, iconSizePx),
                    colorFilter = ColorFilter.tint(iconTint)
                )
            }
        }
    }
}