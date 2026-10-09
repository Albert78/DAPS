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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.dh.daps.ui.common.theme.AppTheme

val Icons.Outlined.Meal: ImageVector
    get() {
        if (_meal != null) {
            return _meal!!
        }
        _meal = materialIcon(name = "Outlined.Meal") {
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
        return _meal!!
    }

private var _meal: ImageVector? = null

@Preview(showBackground = true)
@Composable
private fun MealIconPreview() {
    AppTheme {
        Surface {
            Icon(
                imageVector = Icons.Outlined.Meal,
                contentDescription = "Meal Icon Preview",
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}