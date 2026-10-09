package de.dh.daps.ui.common.icons

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Renders a star icon with a custom character/number drawn inside its center.
 */
@Composable
fun BadgedStarIcon(
    text: String,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
) {
    // Boost text alpha relative to tint alpha so unselected symbols stay legible
    val textAlpha = (tint.alpha * 1.25f).coerceAtMost(1f)
    val starAlpha = (tint.alpha * 0.25f).coerceAtMost(0.25f)

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(24.dp)
    ) {
        Icon(
            imageVector = Icon_Meal_Custom,
            contentDescription = null,
            tint = tint.copy(alpha = starAlpha),
            modifier = Modifier.fillMaxSize()
        )
        Text(
            text = text.take(1).uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
            ),
            color = tint.copy(alpha = textAlpha),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 1.dp)
        )
    }
}