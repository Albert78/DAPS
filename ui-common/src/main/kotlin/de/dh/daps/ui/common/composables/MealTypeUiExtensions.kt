package de.dh.daps.ui.common.composables

import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import de.dh.daps.common.ID_MEAL_FAST
import de.dh.daps.common.ID_MEAL_HIGH_FAT
import de.dh.daps.common.ID_MEAL_SLOW
import de.dh.daps.common.ID_MEAL_STANDARD
import de.dh.daps.common.model.MealType
import de.dh.daps.ui.common.R
import de.dh.daps.ui.common.icons.BadgedStarIcon
import de.dh.daps.ui.common.icons.Icon_Meal_Custom
import de.dh.daps.ui.common.icons.Icon_Meal_Fast
import de.dh.daps.ui.common.icons.Icon_Meal_High_Fat
import de.dh.daps.ui.common.icons.Icon_Meal_Slow
import de.dh.daps.ui.common.icons.Icon_Meal_Standard
import de.dh.daps.common.R as CommonR

/**
 * Returns true if this [MealType] is one of the built-in standard meal types.
 */
fun MealType.isStandardMealType(): Boolean {
    return id in setOf(ID_MEAL_FAST, ID_MEAL_STANDARD, ID_MEAL_HIGH_FAT, ID_MEAL_SLOW)
}

/**
 * Returns a display label for built-in standard meal types (e.g. "Standardmahlzeittyp Schnelle Kohlenhydrate"),
 * or null if this [MealType] is a custom meal type.
 */
@Composable
fun MealType.getStandardMealTypeLabel(): String? {
    if (!isStandardMealType()) return null
    val standardName = when (id) {
        ID_MEAL_FAST -> stringResource(CommonR.string.meal_type_fast_carbs_name)
        ID_MEAL_STANDARD -> stringResource(CommonR.string.meal_type_standard_meal_name)
        ID_MEAL_HIGH_FAT -> stringResource(CommonR.string.meal_type_high_fat_meal_name)
        ID_MEAL_SLOW -> stringResource(CommonR.string.meal_type_slow_meal_name)
        else -> name
    }
    val displayName = name.ifBlank { standardName }
    return stringResource(R.string.meal_type_standard_label_format, displayName)
}

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
            tint = tint,
        )
        ID_MEAL_STANDARD -> Icon(
            imageVector = Icon_Meal_Standard,
            contentDescription = mealType.name,
            modifier = modifier,
            tint = tint,
        )
        ID_MEAL_HIGH_FAT -> Icon(
            imageVector = Icon_Meal_High_Fat,
            contentDescription = modifier.toString(), // or mealType.name
            modifier = modifier,
            tint = tint,
        )
        ID_MEAL_SLOW -> Icon(
            imageVector = Icon_Meal_Slow,
            contentDescription = mealType.name,
            modifier = modifier,
            tint = tint,
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