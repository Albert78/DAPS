package de.dh.raaps.ui.screens.meals

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
import de.dh.raaps.common.model.ID_MEAL_FAST
import de.dh.raaps.common.model.ID_MEAL_HIGH_FAT
import de.dh.raaps.common.model.ID_MEAL_SLOW
import de.dh.raaps.common.model.ID_MEAL_STANDARD
import de.dh.raaps.common.model.MealType
import de.dh.raaps.ui.common.icons.Icon_Meal_Custom
import de.dh.raaps.ui.common.icons.Icon_Meal_Fast
import de.dh.raaps.ui.common.icons.Icon_Meal_High_Fat
import de.dh.raaps.ui.common.icons.Icon_Meal_Slow
import de.dh.raaps.ui.common.icons.Icon_Meal_Standard

/**
 * Renders the icon for a [MealType].
 * Standard meal types use their default icon.
 * Custom meal types with a symbol render a [BadgedStarIcon] using the symbol.
 * Fallbacks to [Icon_Meal_Custom] if no symbol is set.
 */
@Composable
fun MealTypeIcon(
    mealType: MealType,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
) {
    when (mealType.id) {
        ID_MEAL_FAST -> Icon(
            imageVector = Icon_Meal_Fast,
            contentDescription = mealType.name,
            modifier = modifier,
            tint = tint
        )
        ID_MEAL_STANDARD -> Icon(
            imageVector = Icon_Meal_Standard,
            contentDescription = mealType.name,
            modifier = modifier,
            tint = tint
        )
        ID_MEAL_HIGH_FAT -> Icon(
            imageVector = Icon_Meal_High_Fat,
            contentDescription = mealType.name,
            modifier = modifier,
            tint = tint
        )
        ID_MEAL_SLOW -> Icon(
            imageVector = Icon_Meal_Slow,
            contentDescription = mealType.name,
            modifier = modifier,
            tint = tint
        )
        else -> {
            val symbol = mealType.symbol
            if (symbol.isNullOrBlank()) {
                Icon(
                    imageVector = Icon_Meal_Custom,
                    contentDescription = mealType.name,
                    modifier = modifier,
                    tint = tint
                )
            } else {
                BadgedStarIcon(
                    text = symbol,
                    modifier = modifier,
                    tint = tint
                )
            }
        }
    }
}

/**
 * Renders a star icon with a custom character/number drawn inside its center.
 */
@Composable
fun BadgedStarIcon(
    text: String,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(24.dp)
    ) {
        Icon(
            imageVector = Icon_Meal_Custom,
            contentDescription = null,
            tint = tint.copy(alpha = 0.45f),
            modifier = Modifier.fillMaxSize()
        )
        Text(
            text = text.take(1).uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
            ),
            color = tint,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 1.dp)
        )
    }
}