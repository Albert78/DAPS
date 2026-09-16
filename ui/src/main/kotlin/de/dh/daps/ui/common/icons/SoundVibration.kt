package de.dh.daps.ui.common.icons

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.dh.daps.ui.common.theme.AppTheme

@Suppress("UnusedReceiverParameter")
val Icons.Outlined.SoundVibration: ImageVector
    get() {
        if (_soundVibration != null) {
            return _soundVibration!!
        }
        _soundVibration = ImageVector.Builder(
            name = "Outlined.SoundVibration",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // Speaker body
            path(fill = SolidColor(Color.Black)) {
                moveTo(3.0f, 9.0f)
                verticalLineToRelative(6.0f)
                horizontalLineToRelative(4.0f)
                lineToRelative(5.0f, 5.0f)
                verticalLineTo(4.0f)
                lineToRelative(-5.0f, 5.0f)
                horizontalLineTo(3.0f)
                close()
            }
            // Vertical vibration sawtooth wave
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2.0f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(19.5f, 4.5f)
                lineTo(16.5f, 7.0f)
                lineTo(19.5f, 9.5f)
                lineTo(16.5f, 12.0f)
                lineTo(19.5f, 14.5f)
                lineTo(16.5f, 17.0f)
                lineTo(19.5f, 19.5f)
            }
        }.build()
        return _soundVibration!!
    }

private var _soundVibration: ImageVector? = null

@Suppress("UnusedReceiverParameter")
val Icons.Outlined.SoundOnly: ImageVector
    get() {
        if (_soundOnly != null) {
            return _soundOnly!!
        }
        _soundOnly = ImageVector.Builder(
            name = "Outlined.SoundOnly",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // Speaker body
            path(fill = SolidColor(Color.Black)) {
                moveTo(3.0f, 9.0f)
                verticalLineToRelative(6.0f)
                horizontalLineToRelative(4.0f)
                lineToRelative(5.0f, 5.0f)
                verticalLineTo(4.0f)
                lineToRelative(-5.0f, 5.0f)
                horizontalLineTo(3.0f)
                close()
            }
            // Inner sound wave
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2.0f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(15.0f, 8.5f)
                curveTo(16.5f, 10.0f, 16.5f, 14.0f, 15.0f, 15.5f)
            }
            // Outer sound wave
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2.0f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(18.5f, 6.0f)
                curveTo(21.5f, 9.0f, 21.5f, 15.0f, 18.5f, 18.0f)
            }
        }.build()
        return _soundOnly!!
    }

private var _soundOnly: ImageVector? = null

@Suppress("UnusedReceiverParameter")
val Icons.Outlined.VibrationOnly: ImageVector
    get() {
        if (_vibrationOnly != null) {
            return _vibrationOnly!!
        }
        _vibrationOnly = ImageVector.Builder(
            name = "Outlined.VibrationOnly",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // Upper speaker body piece (above slash line gap)
            path(fill = SolidColor(Color.Black)) {
                moveTo(12.0f, 4.0f)
                lineTo(12.0f, 10.6f)
                lineTo(8.0f, 7.7f)
                close()
            }
            // Lower speaker body piece (below slash line gap)
            path(fill = SolidColor(Color.Black)) {
                moveTo(3.0f, 9.0f)
                verticalLineToRelative(6.0f)
                horizontalLineToRelative(4.0f)
                lineToRelative(5.0f, 5.0f)
                verticalLineTo(14.2f)
                lineTo(4.8f, 9.0f)
                horizontalLineTo(3.0f)
                close()
            }
            // Vertical vibration sawtooth wave
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2.0f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(19.5f, 4.5f)
                lineTo(16.5f, 7.0f)
                lineTo(19.5f, 9.5f)
                lineTo(16.5f, 12.0f)
                lineTo(19.5f, 14.5f)
                lineTo(16.5f, 17.0f)
                lineTo(19.5f, 19.5f)
            }
            // Slash line crossing speaker only (flatter angle, cut off right after speaker)
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2.0f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(2.5f, 5.5f)
                lineTo(13.0f, 13.1f)
            }
        }.build()
        return _vibrationOnly!!
    }

private var _vibrationOnly: ImageVector? = null

@Suppress("UnusedReceiverParameter")
val Icons.Outlined.SoundOff: ImageVector
    get() {
        if (_soundOff != null) {
            return _soundOff!!
        }
        _soundOff = ImageVector.Builder(
            name = "Outlined.SoundOff",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // Upper speaker body piece
            path(fill = SolidColor(Color.Black)) {
                moveTo(12.0f, 4.0f)
                lineTo(12.0f, 10.6f)
                lineTo(8.0f, 7.7f)
                close()
            }
            // Lower speaker body piece
            path(fill = SolidColor(Color.Black)) {
                moveTo(3.0f, 9.0f)
                verticalLineToRelative(6.0f)
                horizontalLineToRelative(4.0f)
                lineToRelative(5.0f, 5.0f)
                verticalLineTo(14.2f)
                lineTo(4.8f, 9.0f)
                horizontalLineTo(3.0f)
                close()
            }
            // Inner sound wave (upper segment)
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2.0f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(15.0f, 8.5f)
                curveTo(16.5f, 10.0f, 16.3f, 12.0f, 15.7f, 13.0f)
            }
            // Outer sound wave (upper segment)
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2.0f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(18.5f, 6.0f)
                curveTo(21.5f, 9.0f, 21.0f, 13.5f, 19.5f, 15.5f)
            }
            // Slash line crossing speaker and waves (flatter angle)
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2.0f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(2.5f, 5.5f)
                lineTo(20.5f, 18.5f)
            }
        }.build()
        return _soundOff!!
    }

private var _soundOff: ImageVector? = null

@Preview(showBackground = true)
@Composable
private fun SoundVibrationIconsPreview() {
    AppTheme {
        Surface {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.SoundVibration,
                    contentDescription = "Sound + Vibration"
                )
                Icon(
                    imageVector = Icons.Outlined.SoundOnly,
                    contentDescription = "Nur Sound"
                )
                Icon(
                    imageVector = Icons.Outlined.VibrationOnly,
                    contentDescription = "Nur Vibration"
                )
                Icon(
                    imageVector = Icons.Outlined.SoundOff,
                    contentDescription = "Gar nichts"
                )
            }
        }
    }
}