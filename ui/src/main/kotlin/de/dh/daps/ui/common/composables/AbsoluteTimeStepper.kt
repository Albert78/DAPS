package de.dh.daps.ui.common.composables

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.material3.Surface
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import de.dh.daps.common.model.data.Minutes
import de.dh.daps.common.model.data.Timestamp
import de.dh.daps.ui.common.icons.Icon_Minus
import de.dh.daps.ui.common.icons.Icon_Plus
import de.dh.daps.ui.common.theme.AppTheme
import de.dh.daps.ui.common.time

@Composable
fun AbsoluteTimeStepper(
    currentTime: Timestamp,
    onTimeChange: (Timestamp) -> Unit,
    modifier: Modifier = Modifier,
    stepMinutes: Int = 5,
    minTime: Timestamp? = null,
    maxTime: Timestamp? = null,
    style: StepperStyle = StepperDefaults.defaultStyle()
) {
    var showPickerDialog by remember { mutableStateOf(false) }

    val now = Timestamp.now()
    val effectiveMinTime = minTime ?: (now - Minutes(120))
    val effectiveMaxTime = maxTime ?: (now + Minutes(30))

    val allowedTimestamps = remember(now, effectiveMinTime, effectiveMaxTime) {
        val stepMs = stepMinutes * 60000L
        val baseStart = (effectiveMinTime.ms / stepMs) * stepMs
        val baseEnd = (effectiveMaxTime.ms / stepMs) * stepMs
        (baseStart..baseEnd step stepMs).map { Timestamp(it) }
    }

    if (showPickerDialog) {
        // Find nearest allowed timestamp for initial selection
        val initialSelection = allowedTimestamps.minByOrNull { kotlin.math.abs(it.ms - currentTime.ms) } ?: currentTime

        WheelPickerDialog(
            initialValue = initialSelection,
            options = allowedTimestamps,
            onValueSelected = { onTimeChange(it) },
            onDismiss = { showPickerDialog = false },
            labelProvider = { time(it) },
            width = 150.dp
        )
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        IconButton(
            onClick = {
                // Snap to previous multiple of stepMinutes
                val currentMs = currentTime.ms
                val stepMs = stepMinutes * 60000L
                val remainder = currentMs % stepMs
                val nextTime = if (remainder == 0L) currentTime - Minutes(stepMinutes.toShort())
                               else Timestamp(currentMs - remainder)
                if (nextTime >= effectiveMinTime) {
                    onTimeChange(nextTime)
                }
            },
            modifier = Modifier.size(style.buttonSize),
            enabled = currentTime > effectiveMinTime
        ) {
            Icon(Icon_Minus, contentDescription = null, modifier = Modifier.size(style.buttonSize * 0.5f))
        }

        Spacer(Modifier.width(style.spacing))

        Text(
            text = time(currentTime),
            style = style.textStyle,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .width(style.valueWidth)
                .clip(RoundedCornerShape(8.dp))
                .clickable { showPickerDialog = true }
                .padding(vertical = 4.dp)
        )

        Spacer(Modifier.width(style.spacing))

        IconButton(
            onClick = {
                // Snap to next multiple of stepMinutes
                val currentMs = currentTime.ms
                val stepMs = stepMinutes * 60000L
                val remainder = currentMs % stepMs
                val nextTime = if (remainder == 0L) currentTime + Minutes(stepMinutes.toShort())
                               else Timestamp(currentMs + (stepMs - remainder))
                if (nextTime <= effectiveMaxTime) {
                    onTimeChange(nextTime)
                }
            },
            modifier = Modifier.size(style.buttonSize),
            enabled = currentTime < effectiveMaxTime
        ) {
            Icon(Icon_Plus, contentDescription = null, modifier = Modifier.size(style.buttonSize * 0.5f))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AbsoluteTimeStepperPreview() {
    var time by remember { mutableStateOf(Timestamp.now()) }
    AppTheme {
        Surface {
            AbsoluteTimeStepper(
                currentTime = time,
                onTimeChange = { time = it },
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}