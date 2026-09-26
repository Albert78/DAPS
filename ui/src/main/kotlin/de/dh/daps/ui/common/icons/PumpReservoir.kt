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
 * A custom pump reservoir icon (vial/cartridge adapted from the Vaccines symbol,
 * scaled to fill the full vertical height of 24dp).
 */
val Icons.Outlined.PumpReservoir: ImageVector
    get() {
        if (_pumpReservoir != null) {
            return _pumpReservoir!!
        }
        _pumpReservoir = materialIcon(name = "Outlined.PumpReservoir") {
            // 1. Cap (top closure)
            materialPath {
                moveTo(8f, 2f)
                horizontalLineTo(16f)
                verticalLineTo(4f)
                horizontalLineTo(8f)
                close()
            }

            // 2. Neck (narrow section below cap)
            materialPath {
                moveTo(10f, 4f)
                horizontalLineTo(14f)
                verticalLineTo(6f)
                horizontalLineTo(10f)
                close()
            }

            // 3. Vial body (outer wall contour)
            materialPath {
                moveTo(10f, 6f)
                lineTo(7f, 8f)
                verticalLineTo(20f)
                curveTo(7f, 21.1f, 7.9f, 22f, 9f, 22f)
                horizontalLineTo(15f)
                curveTo(16.1f, 22f, 17f, 21.1f, 17f, 20f)
                verticalLineTo(8f)
                lineTo(14f, 6f)
                close()

                // Inner cutout for empty upper portion
                moveTo(9f, 8.5f)
                horizontalLineTo(15f)
                verticalLineTo(12f)
                horizontalLineTo(9f)
                close()
            }

            // 4. Liquid level in reservoir (filled lower portion)
            materialPath {
                moveTo(9f, 12f)
                horizontalLineTo(15f)
                verticalLineTo(20f)
                curveTo(15f, 20.3f, 14.7f, 20.5f, 14.3f, 20.5f)
                horizontalLineTo(9.7f)
                curveTo(9.3f, 20.5f, 9f, 20.3f, 9f, 20f)
                close()
            }
        }
        return _pumpReservoir!!
    }

private var _pumpReservoir: ImageVector? = null

@Preview(showBackground = true)
@Composable
private fun PumpReservoirIconPreview() {
    AppTheme {
        Surface {
            Icon(
                imageVector = Icons.Outlined.PumpReservoir,
                contentDescription = "Pump Reservoir Icon Preview",
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}