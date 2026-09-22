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
 * Alternative Basal icon (Drop with wavy water level inside).
 */
val Icons.Outlined.Basal_Alternative: ImageVector
    get() {
        if (_basalAlternative != null) {
            return _basalAlternative!!
        }
        _basalAlternative = createBasalWaveInsideIcon()
        return _basalAlternative!!
    }

private var _basalAlternative: ImageVector? = null

private fun createBasalWaveInsideIcon(): ImageVector = materialIcon(name = "Outlined.Basal_Alternative") {
    materialPath {
        // Drop outline
        moveTo(12.0f, 2.0f)
        curveToRelative(-5.33f, 4.55f, -8.0f, 8.48f, -8.0f, 11.8f)
        curveToRelative(0.0f, 4.98f, 3.8f, 8.2f, 8.0f, 8.2f)
        reflectiveCurveToRelative(8.0f, -3.22f, 8.0f, -8.2f)
        curveTo(20.0f, 10.48f, 17.33f, 6.55f, 12.0f, 2.0f)
        close()
        moveTo(12.0f, 20.0f)
        curveToRelative(-3.35f, 0.0f, -6.0f, -2.57f, -6.0f, -6.2f)
        curveToRelative(0.0f, -2.34f, 1.95f, -5.44f, 6.0f, -9.14f)
        curveToRelative(4.05f, 3.7f, 6.0f, 6.79f, 6.0f, 9.14f)
        curveTo(18.0f, 17.43f, 15.35f, 20.0f, 12.0f, 20.0f)
        close()
    }
    // Wavy water surface in lower third
    materialPath {
        moveTo(6.8f, 14.0f)
        curveToRelative(1.5f, -1.2f, 3.5f, 1.2f, 5.2f, 0.0f)
        curveToRelative(1.5f, -1.2f, 3.5f, 1.2f, 5.2f, 0.0f)
        lineToRelative(0.2f, 1.2f)
        curveToRelative(-1.8f, 1.3f, -3.8f, -1.1f, -5.4f, 0.1f)
        curveToRelative(-1.8f, 1.3f, -3.8f, -1.1f, -5.4f, 0.1f)
        close()
    }
}

@Preview(showBackground = true)
@Composable
private fun BasalAlternativeIconPreview() {
    AppTheme {
        Surface {
            Icon(
                imageVector = Icons.Outlined.Basal_Alternative,
                contentDescription = "Basal Alternative Icon Preview",
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}