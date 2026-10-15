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
 * Custom Material-style icon representing an Insulin Pump device.
 * Focus is placed on the pump device chassis, screen, infusion line connection, and control interface.
 */
val Icons.Outlined.InsulinPump: ImageVector
    get() {
        if (_insulinPump != null) {
            return _insulinPump!!
        }
        _insulinPump = materialIcon(name = "Outlined.InsulinPump") {
            // 1. Main Pump Body Contour (Outline)
            materialPath {
                moveTo(8.0f, 4.0f)
                horizontalLineTo(16.0f)
                curveTo(17.66f, 4.0f, 19.0f, 5.34f, 19.0f, 7.0f)
                verticalLineTo(17.0f)
                curveTo(19.0f, 18.66f, 17.66f, 20.0f, 16.0f, 20.0f)
                horizontalLineTo(8.0f)
                curveTo(6.34f, 20.0f, 5.0f, 18.66f, 5.0f, 17.0f)
                verticalLineTo(7.0f)
                curveTo(5.0f, 5.34f, 6.34f, 4.0f, 8.0f, 4.0f)
                close()

                // Hollow inside cavity
                moveTo(8.0f, 6.0f)
                curveTo(7.45f, 6.0f, 7.0f, 6.45f, 7.0f, 7.0f)
                verticalLineTo(17.0f)
                curveTo(7.0f, 17.55f, 7.45f, 18.0f, 8.0f, 18.0f)
                horizontalLineTo(16.0f)
                curveTo(16.55f, 18.0f, 17.0f, 17.55f, 17.0f, 17.0f)
                verticalLineTo(7.0f)
                curveTo(17.0f, 6.45f, 16.55f, 6.0f, 16.0f, 6.0f)
                horizontalLineTo(8.0f)
                close()
            }

            // 2. Pump Display Screen (Filled rectangle inset)
            materialPath {
                moveTo(8.5f, 7.5f)
                horizontalLineTo(15.5f)
                curveTo(15.78f, 7.5f, 16.0f, 7.72f, 16.0f, 8.0f)
                verticalLineTo(11.5f)
                curveTo(16.0f, 11.78f, 15.78f, 12.0f, 15.5f, 12.0f)
                horizontalLineTo(8.5f)
                curveTo(8.22f, 12.0f, 8.0f, 11.78f, 8.0f, 11.5f)
                verticalLineTo(8.0f)
                curveTo(8.0f, 7.72f, 8.22f, 7.5f, 8.5f, 7.5f)
                close()
            }

            // 3. Control Button Panel
            materialPath {
                moveTo(9.0f, 14.0f)
                horizontalLineTo(15.0f)
                curveTo(15.55f, 14.0f, 16.0f, 14.45f, 16.0f, 15.0f)
                verticalLineTo(16.5f)
                curveTo(16.0f, 17.05f, 15.55f, 17.5f, 15.0f, 17.5f)
                horizontalLineTo(9.0f)
                curveTo(8.45f, 17.5f, 8.0f, 17.05f, 8.0f, 16.5f)
                verticalLineTo(15.0f)
                curveTo(8.0f, 14.45f, 8.45f, 14.0f, 9.0f, 14.0f)
                close()
            }

            // 4. Infusion Line Connector (Top Cap & Curved Tubing)
            materialPath {
                moveTo(14.5f, 1.5f)
                horizontalLineTo(16.5f)
                verticalLineTo(4.0f)
                horizontalLineTo(14.5f)
                close()

                moveTo(16.0f, 1.5f)
                curveTo(16.0f, 0.7f, 17.5f, 0.7f, 18.5f, 1.5f)
                curveTo(19.2f, 2.1f, 19.5f, 3.0f, 19.5f, 4.0f)
                horizontalLineTo(18.0f)
                curveTo(18.0f, 3.4f, 17.8f, 2.8f, 17.3f, 2.4f)
                curveTo(16.9f, 2.0f, 16.0f, 2.0f, 16.0f, 1.5f)
                close()
            }
        }
        return _insulinPump!!
    }

private var _insulinPump: ImageVector? = null

@Preview(showBackground = true)
@Composable
private fun InsulinPumpIconPreview() {
    AppTheme {
        Surface {
            Icon(
                imageVector = Icons.Outlined.InsulinPump,
                contentDescription = "Insulin Pump Icon Preview",
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}