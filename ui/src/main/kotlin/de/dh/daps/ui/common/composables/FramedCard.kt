package de.dh.daps.ui.common.composables

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import de.dh.daps.ui.common.theme.ExtendedTheme

@Composable
fun FramedCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(12.dp),
    containerColor: Color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
    borderColor: Color = ExtendedTheme.semanticColors.border,
    borderWidth: Dp = 2.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    if (onClick != null) {
        OutlinedCard(
            onClick = onClick,
            modifier = modifier,
            shape = shape,
            colors = CardDefaults.outlinedCardColors(
                containerColor = containerColor
            ),
            border = BorderStroke(borderWidth, borderColor),
            content = content
        )
    } else {
        OutlinedCard(
            modifier = modifier,
            shape = shape,
            colors = CardDefaults.outlinedCardColors(
                containerColor = containerColor
            ),
            border = BorderStroke(borderWidth, borderColor),
            content = content
        )
    }
}