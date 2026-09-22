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
 * Standard Basal icon (Drop on wavy water surface).
 */
val Icons.Outlined.Basal: ImageVector
    get() {
        if (_basal != null) {
            return _basal!!
        }
        _basal = createBasalWaveBaseIcon()
        return _basal!!
    }

private var _basal: ImageVector? = null

/**
 * Temp Basal icon (Basal drop shifted left with '+' and '-' in upper right corner).
 */
val Icons.Outlined.Temp_Basal: ImageVector
    get() {
        if (_tempBasal != null) {
            return _tempBasal!!
        }
        _tempBasal = createTempBasalIcon()
        return _tempBasal!!
    }

private var _tempBasal: ImageVector? = null

/**
 * Drop on a gentle wavy water surface at the bottom.
 */
private fun createBasalWaveBaseIcon(): ImageVector = materialIcon(name = "Outlined.Basal") {
    // Drop (slightly shifted up for the wave line underneath)
    materialPath {
        moveTo(12.0f, 1.0f)
        curveToRelative(-4.8f, 4.1f, -7.2f, 7.6f, -7.2f, 10.6f)
        curveToRelative(0.0f, 4.5f, 3.4f, 7.4f, 7.2f, 7.4f)
        reflectiveCurveToRelative(7.2f, -2.9f, 7.2f, -7.4f)
        curveTo(19.2f, 8.6f, 16.8f, 5.1f, 12.0f, 1.0f)
        close()
        moveTo(12.0f, 17.2f)
        curveToRelative(-3.0f, 0.0f, -5.4f, -2.3f, -5.4f, -5.6f)
        curveToRelative(0.0f, -2.1f, 1.8f, -4.9f, 5.4f, -8.2f)
        curveToRelative(3.6f, 3.3f, 5.4f, 6.1f, 5.4f, 8.2f)
        curveTo(17.4f, 14.9f, 15.0f, 17.2f, 12.0f, 17.2f)
        close()
        // Reflection arc
        moveTo(8.2f, 12.0f)
        curveToRelative(0.3f, 0.0f, 0.6f, 0.2f, 0.7f, 0.5f)
        curveToRelative(0.4f, 2.0f, 2.0f, 2.7f, 3.3f, 2.6f)
        curveToRelative(0.4f, 0.0f, 0.7f, 0.3f, 0.7f, 0.7f)
        curveToRelative(0.0f, 0.4f, -0.3f, 0.7f, -0.7f, 0.7f)
        curveToRelative(-1.9f, 0.1f, -4.1f, -1.0f, -4.6f, -3.7f)
        curveTo(7.5f, 12.4f, 7.8f, 12.0f, 8.2f, 12.0f)
        close()
    }
    // Wavy water surface at bottom
    materialPath {
        moveTo(2.0f, 20.5f)
        curveToRelative(2.5f, -1.5f, 5.5f, 1.5f, 8.0f, 0.0f)
        curveToRelative(2.5f, -1.5f, 5.5f, 1.5f, 8.0f, 0.0f)
        curveToRelative(1.5f, -0.9f, 3.0f, -0.2f, 4.0f, 0.5f)
        lineToRelative(-0.6f, 1.3f)
        curveToRelative(-0.8f, -0.6f, -2.0f, -1.1f, -3.4f, -0.3f)
        curveToRelative(-2.5f, 1.5f, -5.5f, -1.5f, -8.0f, 0.0f)
        curveToRelative(-2.5f, 1.5f, -5.5f, -1.5f, -8.0f, 0.0f)
        close()
    }
}

/**
 * Temp Basal icon (Basal drop shifted left, with '+' in top right and '-' underneath standing free).
 */
private fun createTempBasalIcon(): ImageVector = materialIcon(name = "Outlined.Temp_Basal") {
    // Group for drop and wave line, shifted left by 2.5 units
    group(translationX = -2.5f) {
        // Basal drop with reflection arc
        materialPath {
            moveTo(12.0f, 1.0f)
            curveToRelative(-4.8f, 4.1f, -7.2f, 7.6f, -7.2f, 10.6f)
            curveToRelative(0.0f, 4.5f, 3.4f, 7.4f, 7.2f, 7.4f)
            reflectiveCurveToRelative(7.2f, -2.9f, 7.2f, -7.4f)
            curveTo(19.2f, 8.6f, 16.8f, 5.1f, 12.0f, 1.0f)
            close()
            moveTo(12.0f, 17.2f)
            curveToRelative(-3.0f, 0.0f, -5.4f, -2.3f, -5.4f, -5.6f)
            curveToRelative(0.0f, -2.1f, 1.8f, -4.9f, 5.4f, -8.2f)
            curveToRelative(3.6f, 3.3f, 5.4f, 6.1f, 5.4f, 8.2f)
            curveTo(17.4f, 14.9f, 15.0f, 17.2f, 12.0f, 17.2f)
            close()
            // Reflection arc
            moveTo(8.2f, 12.0f)
            curveToRelative(0.3f, 0.0f, 0.6f, 0.2f, 0.7f, 0.5f)
            curveToRelative(0.4f, 2.0f, 2.0f, 2.7f, 3.3f, 2.6f)
            curveToRelative(0.4f, 0.0f, 0.7f, 0.3f, 0.7f, 0.7f)
            curveToRelative(0.0f, 0.4f, -0.3f, 0.7f, -0.7f, 0.7f)
            curveToRelative(-1.9f, 0.1f, -4.1f, -1.0f, -4.6f, -3.7f)
            curveTo(7.5f, 12.4f, 7.8f, 12.0f, 8.2f, 12.0f)
            close()
        }
        // Wavy water surface at bottom
        materialPath {
            moveTo(2.0f, 20.5f)
            curveToRelative(2.5f, -1.5f, 5.5f, 1.5f, 8.0f, 0.0f)
            curveToRelative(2.5f, -1.5f, 5.5f, 1.5f, 8.0f, 0.0f)
            curveToRelative(1.5f, -0.9f, 3.0f, -0.2f, 4.0f, 0.5f)
            lineToRelative(-0.6f, 1.3f)
            curveToRelative(-0.8f, -0.6f, -2.0f, -1.1f, -3.4f, -0.3f)
            curveToRelative(-2.5f, 1.5f, -5.5f, -1.5f, -8.0f, 0.0f)
            curveToRelative(-2.5f, 1.5f, -5.5f, -1.5f, -8.0f, 0.0f)
            close()
        }
    }
    // '+' in top right standing free (centered at (19.5, 3.5))
    materialPath {
        // Horizontal bar Plus
        moveTo(17.0f, 2.9f)
        horizontalLineTo(22.0f)
        verticalLineTo(4.1f)
        horizontalLineTo(17.0f)
        close()
        // Vertical bar Plus
        moveTo(18.9f, 1.0f)
        horizontalLineTo(20.1f)
        verticalLineTo(6.0f)
        horizontalLineTo(18.9f)
        close()
    }
    // '-' underneath in top right standing free (centered at (19.5, 8.5))
    materialPath {
        moveTo(17.0f, 7.9f)
        horizontalLineTo(22.0f)
        verticalLineTo(9.1f)
        horizontalLineTo(17.0f)
        close()
    }
}

@Preview(showBackground = true)
@Composable
private fun BasalIconPreview() {
    AppTheme {
        Surface {
            Icon(
                imageVector = Icons.Outlined.Basal,
                contentDescription = "Basal Icon Preview",
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TempBasalIconPreview() {
    AppTheme {
        Surface {
            Icon(
                imageVector = Icons.Outlined.Temp_Basal,
                contentDescription = "Temp Basal Icon Preview",
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}