package de.dh.daps.ui.common.icons

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.materialIcon
import androidx.compose.material.icons.materialPath
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.dh.daps.ui.common.theme.AppTheme

/**
 * Custom Icon for Meal Types (Mahlzeiten-Typen):
 * Fork and knife (somewhat narrower and vertically shortened), with a gear symbol underneath.
 */
val Icons.Outlined.Meal_Types: ImageVector
    get() {
        if (_mealTypes != null) {
            return _mealTypes!!
        }
        _mealTypes = materialIcon(name = "Outlined.Meal_Types") {
            // Group 1: Fork and Knife (narrower and vertically shortened)
            group(
                scaleX = 0.75f,
                scaleY = 0.48f,
                pivotX = 11.5f,
                pivotY = 2f,
                translationY = -0.5f
            ) {
                materialPath {
                    // Fork (Gabel)
                    moveTo(11.0f, 9.0f)
                    lineTo(9.0f, 9.0f)
                    lineTo(9.0f, 2.0f)
                    lineTo(7.0f, 2.0f)
                    verticalLineToRelative(7.0f)
                    lineTo(5.0f, 9.0f)
                    lineTo(5.0f, 2.0f)
                    lineTo(3.0f, 2.0f)
                    verticalLineToRelative(7.0f)
                    curveToRelative(0.0f, 2.12f, 1.66f, 3.84f, 3.75f, 3.97f)
                    lineTo(6.75f, 22.0f)
                    horizontalLineToRelative(2.5f)
                    verticalLineToRelative(-9.03f)
                    curveTo(11.34f, 12.84f, 13.0f, 11.12f, 13.0f, 9.0f)
                    lineTo(13.0f, 2.0f)
                    horizontalLineToRelative(-2.0f)
                    verticalLineToRelative(7.0f)
                    close()

                    // Knife (Messer)
                    moveTo(16.0f, 6.0f)
                    verticalLineToRelative(8.0f)
                    horizontalLineToRelative(1.5f)
                    verticalLineToRelative(8.0f)
                    horizontalLineToRelative(2.5f)
                    verticalLineTo(2.0f)
                    curveToRelative(-2.21f, 0.0f, -4.0f, 1.79f, -4.0f, 4.0f)
                    close()
                }
            }

            // Group 2: Gear icon centered underneath
            group(
                scaleX = 0.50f,
                scaleY = 0.50f,
                pivotX = 12f,
                pivotY = 12f,
                translationY = 5.0f
            ) {
                materialPath {
                    moveTo(19.43f, 12.98f)
                    curveToRelative(0.04f, -0.32f, 0.07f, -0.64f, 0.07f, -0.98f)
                    reflectiveCurveToRelative(-0.03f, -0.66f, -0.07f, -0.98f)
                    lineToRelative(2.11f, -1.65f)
                    curveToRelative(0.19f, -0.15f, 0.24f, -0.42f, 0.12f, -0.64f)
                    lineToRelative(-2.0f, -3.46f)
                    curveToRelative(-0.12f, -0.22f, -0.39f, -0.3f, -0.61f, -0.22f)
                    lineToRelative(-2.49f, 1.0f)
                    curveToRelative(-0.52f, -0.4f, -1.08f, -0.73f, -1.69f, -0.98f)
                    lineToRelative(-0.38f, -2.65f)
                    curveTo(14.46f, 2.18f, 14.25f, 2.0f, 14.0f, 2.0f)
                    horizontalLineToRelative(-4.0f)
                    curveToRelative(-0.25f, 0.0f, -0.46f, 0.18f, -0.49f, 0.42f)
                    lineToRelative(-0.38f, 2.65f)
                    curveToRelative(-0.61f, 0.25f, -1.17f, 0.59f, -1.69f, 0.98f)
                    lineToRelative(-2.49f, -1.0f)
                    curveToRelative(-0.23f, -0.09f, -0.49f, 0.0f, -0.61f, 0.22f)
                    lineToRelative(-2.0f, 3.46f)
                    curveToRelative(-0.13f, 0.22f, -0.07f, 0.49f, 0.12f, 0.64f)
                    lineToRelative(2.11f, 1.65f)
                    curveToRelative(-0.04f, 0.32f, -0.07f, 0.65f, -0.07f, 0.98f)
                    reflectiveCurveToRelative(0.03f, 0.66f, 0.07f, 0.98f)
                    lineToRelative(-2.11f, 1.65f)
                    curveToRelative(-0.19f, 0.15f, -0.24f, 0.42f, -0.12f, 0.64f)
                    lineToRelative(2.0f, 3.46f)
                    curveToRelative(0.12f, 0.22f, 0.39f, 0.3f, 0.61f, 0.22f)
                    lineToRelative(2.49f, -1.0f)
                    curveToRelative(0.52f, 0.4f, 1.08f, 0.73f, 1.69f, 0.98f)
                    lineToRelative(0.38f, 2.65f)
                    curveToRelative(0.03f, 0.24f, 0.24f, 0.42f, 0.49f, 0.42f)
                    horizontalLineToRelative(4.0f)
                    curveToRelative(0.25f, 0.0f, 0.46f, -0.18f, 0.49f, -0.42f)
                    lineToRelative(0.38f, -2.65f)
                    curveToRelative(0.61f, -0.25f, 1.17f, -0.59f, 1.69f, -0.98f)
                    lineToRelative(2.49f, 1.0f)
                    curveToRelative(0.23f, 0.09f, 0.49f, 0.0f, 0.61f, -0.22f)
                    lineToRelative(2.0f, -3.46f)
                    curveToRelative(0.12f, -0.22f, 0.07f, -0.49f, -0.12f, -0.64f)
                    lineToRelative(-2.11f, -1.65f)
                    close()
                    moveTo(12.0f, 15.5f)
                    curveToRelative(-1.93f, 0.0f, -3.5f, -1.57f, -3.5f, -3.5f)
                    reflectiveCurveToRelative(1.57f, -3.5f, 3.5f, -3.5f)
                    reflectiveCurveToRelative(3.5f, 1.57f, 3.5f, 3.5f)
                    reflectiveCurveToRelative(-1.57f, 3.5f, -3.5f, 3.5f)
                    close()
                }
            }
        }
        return _mealTypes!!
    }

private var _mealTypes: ImageVector? = null

@Preview(showBackground = true)
@Composable
private fun MealTypesIconPreview() {
    AppTheme {
        Surface {
            Icon(
                imageVector = Icons.Outlined.Meal_Types,
                contentDescription = "Meal Types Icon Preview",
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}