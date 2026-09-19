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

/**
 * Ein maßgeschneidertes OpenLoop-Icon im Material-Stil:
 * Ein geöffneter Kreis/Loop mit Unterbrechung und Öffnungspfeil an der Oberseite.
 */
val Icons.Outlined.OpenLoop: ImageVector
    get() {
        if (_openLoop != null) {
            return _openLoop!!
        }
        _openLoop = materialIcon(name = "Outlined.OpenLoop") {
            // Geöffneter Kreisbogen (Loop mit Lücke oben)
            materialPath {
                moveTo(12.0f, 3.0f)
                curveTo(16.97f, 3.0f, 21.0f, 7.03f, 21.0f, 12.0f)
                curveTo(21.0f, 16.97f, 16.97f, 21.0f, 12.0f, 21.0f)
                curveTo(7.03f, 21.0f, 3.0f, 16.97f, 3.0f, 12.0f)
                curveTo(3.0f, 8.5f, 5.05f, 5.48f, 8.0f, 4.15f)
                lineTo(8.8f, 6.0f)
                curveTo(6.5f, 7.1f, 5.0f, 9.4f, 5.0f, 12.0f)
                curveTo(5.0f, 15.87f, 8.13f, 19.0f, 12.0f, 19.0f)
                curveTo(15.87f, 19.0f, 19.0f, 15.87f, 19.0f, 12.0f)
                curveTo(19.0f, 8.13f, 15.87f, 5.0f, 12.0f, 5.0f)
                verticalLineTo(3.0f)
                close()

                // Pfeil an der Öffnung
                moveTo(6.5f, 2.0f)
                lineTo(10.0f, 5.5f)
                lineTo(8.58f, 6.92f)
                lineTo(6.5f, 4.84f)
                lineTo(4.42f, 6.92f)
                lineTo(3.0f, 5.5f)
                close()
            }
        }
        return _openLoop!!
    }

private var _openLoop: ImageVector? = null

@Preview(showBackground = true)
@Composable
private fun OpenLoopIconPreview() {
    AppTheme {
        Surface {
            Icon(
                imageVector = Icons.Outlined.OpenLoop,
                contentDescription = "Open Loop Icon Preview",
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}