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
import de.dh.daps.ui.common.theme.AppPreview
import de.dh.daps.ui.common.theme.AppTheme

/**
 * Datenbank-Icon (3 gestapelte Scheiben/Zylinder mit klaren Trennfugen).
 */
val Icons.Outlined.Database: ImageVector
    get() {
        if (_database != null) return _database!!
        _database = materialIcon(name = "Outlined.Database") {
            materialPath {
                // Disk 1 (top)
                moveTo(12.0f, 2.5f)
                curveTo(17.0f, 2.5f, 21.0f, 3.8f, 21.0f, 5.5f)
                verticalLineTo(7.5f)
                curveTo(21.0f, 9.2f, 17.0f, 10.5f, 12.0f, 10.5f)
                curveTo(7.0f, 10.5f, 3.0f, 9.2f, 3.0f, 7.5f)
                verticalLineTo(5.5f)
                curveTo(3.0f, 3.8f, 7.0f, 2.5f, 12.0f, 2.5f)
                close()

                // Disk 2 (middle)
                moveTo(3.0f, 9.2f)
                verticalLineTo(12.5f)
                curveTo(3.0f, 14.2f, 7.0f, 15.5f, 12.0f, 15.5f)
                curveTo(17.0f, 15.5f, 21.0f, 14.2f, 21.0f, 12.5f)
                verticalLineTo(9.2f)
                curveTo(21.0f, 10.2f, 17.0f, 11.2f, 12.0f, 11.2f)
                curveTo(7.0f, 11.2f, 3.0f, 10.2f, 3.0f, 9.2f)
                close()

                // Disk 3 (bottom)
                moveTo(3.0f, 14.2f)
                verticalLineTo(17.5f)
                curveTo(3.0f, 19.2f, 7.0f, 20.5f, 12.0f, 20.5f)
                curveTo(17.0f, 20.5f, 21.0f, 19.2f, 21.0f, 17.5f)
                verticalLineTo(14.2f)
                curveTo(21.0f, 15.2f, 17.0f, 16.2f, 12.0f, 16.2f)
                curveTo(7.0f, 16.2f, 3.0f, 15.2f, 3.0f, 14.2f)
                close()
            }
        }
        return _database!!
    }

private var _database: ImageVector? = null

@Preview(showBackground = true)
@Composable
private fun DatabaseIconPreview() {
    AppPreview  {
        Icon(
            imageVector = Icons.Outlined.Database,
            contentDescription = "Database Icon Preview",
            modifier = Modifier.padding(16.dp)
        )
    }
}