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
 * Custom Material-style icon representing a Continuous Glucose Monitor (CGM) Sensor device.
 * Focus is placed on the wearable sensor pod / patch and transmitter wireless signal waves.
 */
val Icons.Outlined.CgmSensor: ImageVector
    get() {
        if (_cgmSensor != null) {
            return _cgmSensor!!
        }
        _cgmSensor = materialIcon(name = "Outlined.CgmSensor") {
            // 1. Adhesive Patch (Outer oval ring contour)
            materialPath {
                moveTo(8.0f, 12.0f)
                horizontalLineTo(16.0f)
                curveTo(18.21f, 12.0f, 20.0f, 13.79f, 20.0f, 16.0f)
                curveTo(20.0f, 18.21f, 18.21f, 20.0f, 16.0f, 20.0f)
                horizontalLineTo(8.0f)
                curveTo(5.79f, 20.0f, 4.0f, 18.21f, 4.0f, 16.0f)
                curveTo(4.0f, 13.79f, 5.79f, 12.0f, 8.0f, 12.0f)
                close()

                // Cutout inner edge of patch
                moveTo(8.0f, 13.5f)
                curveTo(6.62f, 13.5f, 5.5f, 14.62f, 5.5f, 16.0f)
                curveTo(5.5f, 17.38f, 6.62f, 18.5f, 8.0f, 18.5f)
                horizontalLineTo(16.0f)
                curveTo(17.38f, 18.5f, 18.5f, 17.38f, 18.5f, 16.0f)
                curveTo(18.5f, 14.62f, 17.38f, 13.5f, 16.0f, 13.5f)
                horizontalLineTo(8.0f)
                close()
            }

            // 2. Transmitter Unit (Raised central transmitter module)
            materialPath {
                moveTo(9.0f, 14.5f)
                horizontalLineTo(15.0f)
                curveTo(15.83f, 14.5f, 16.5f, 15.17f, 16.5f, 16.0f)
                curveTo(16.5f, 16.83f, 15.83f, 17.5f, 15.0f, 17.5f)
                horizontalLineTo(9.0f)
                curveTo(8.17f, 17.5f, 7.5f, 16.83f, 7.5f, 16.0f)
                curveTo(7.5f, 15.17f, 8.17f, 14.5f, 9.0f, 14.5f)
                close()
            }

            // 3. Sensor Signal Wave (Inner Arc)
            materialPath {
                moveTo(9.2f, 8.8f)
                curveTo(10.8f, 7.2f, 13.2f, 7.2f, 14.8f, 8.8f)
                lineTo(16.2f, 7.4f)
                curveTo(13.8f, 5.0f, 10.2f, 5.0f, 7.8f, 7.4f)
                lineTo(9.2f, 8.8f)
                close()
            }

            // 4. Sensor Signal Wave (Outer Arc)
            materialPath {
                moveTo(6.4f, 6.0f)
                curveTo(9.5f, 2.9f, 14.5f, 2.9f, 17.6f, 6.0f)
                lineTo(19.0f, 4.6f)
                curveTo(15.1f, 0.7f, 8.9f, 0.7f, 5.0f, 4.6f)
                lineTo(6.4f, 6.0f)
                close()
            }
        }
        return _cgmSensor!!
    }

private var _cgmSensor: ImageVector? = null

@Preview(showBackground = true)
@Composable
private fun CgmSensorIconPreview() {
    AppTheme {
        Surface {
            Icon(
                imageVector = Icons.Outlined.CgmSensor,
                contentDescription = "CGM Sensor Icon Preview",
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}