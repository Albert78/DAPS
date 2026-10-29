package de.dh.daps.ui.screens.alertprofiles

import android.content.Context
import android.content.res.Configuration
import android.media.RingtoneManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import de.dh.daps.common.domain.model.alarm.AlertDisplayMode
import de.dh.daps.common.domain.model.alarm.AlertSignalConfig
import de.dh.daps.common.domain.model.alarm.SoundConfig
import de.dh.daps.common.domain.model.alarm.VibrationMode
import de.dh.daps.ui.R
import de.dh.daps.ui.common.composables.FramedCard
import de.dh.daps.ui.common.composables.NormalTextButton
import de.dh.daps.ui.common.icons.Icon_Sound_Off
import de.dh.daps.ui.common.icons.Icon_Sound_Only
import de.dh.daps.ui.common.icons.Icon_Sound_Vibration
import de.dh.daps.ui.common.icons.Icon_Vibration_Only
import de.dh.daps.ui.common.theme.AppPreview
import kotlin.math.roundToInt

@Composable
fun SignalConfigCard(
    title: String,
    config: AlertSignalConfig,
    onConfigChanged: (AlertSignalConfig) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    showEnabledToggle: Boolean = false,
    onDelete: (() -> Unit)? = null,
    onPlayPreview: (() -> Unit)? = null,
    onSelectSound: (() -> Unit)? = null,
    onUpdateVolume: ((Int) -> Unit)? = null
) {
    val context = LocalContext.current
    val sound = config.soundConfig
    val soundTitle = remember(sound?.soundUri) {
        getRingtoneTitle(context, sound?.soundUri)
    }

    val hasSound = config.enabled && sound != null && sound.volume > 0
    val hasVibration = config.enabled && config.vibrationMode != VibrationMode.OFF

    val (soundVibrationIcon, contentDescRes) = when {
        hasSound && hasVibration -> Icon_Sound_Vibration to R.string.cd_test_alarm_sound_and_vibration
        hasSound -> Icon_Sound_Only to R.string.cd_test_alarm_sound
        hasVibration -> Icon_Vibration_Only to R.string.cd_test_alarm_vibration
        else -> Icon_Sound_Off to R.string.cd_alarm_sound_and_vibration_off
    }

    FramedCard(
        modifier = modifier.fillMaxWidth(),
        containerColor = if (config.enabled) {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
        }
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (!subtitle.isNullOrBlank()) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (onPlayPreview != null) {
                        IconButton(
                            onClick = onPlayPreview,
                            enabled = config.enabled && (hasSound || hasVibration)
                        ) {
                            Icon(
                                imageVector = soundVibrationIcon,
                                contentDescription = stringResource(id = contentDescRes)
                            )
                        }
                    }

                    if (onDelete != null) {
                        IconButton(onClick = onDelete) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = stringResource(id = R.string.alert_profile_delete_override),
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }

            // Signal Enabled Toggle Switch (if applicable)
            if (showEnabledToggle) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(id = R.string.alert_profile_signal_enabled),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Switch(
                        checked = config.enabled,
                        onCheckedChange = { enabled ->
                            onConfigChanged(config.copy(enabled = enabled))
                        }
                    )
                }
            }

            // Configuration Body (Only rendered if enabled)
            if (config.enabled) {
                // Full Screen Alarm Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(id = R.string.alert_profile_show_full_screen),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Switch(
                        checked = config.isFullScreen,
                        onCheckedChange = { showFS ->
                            val newDisplayMode = if (showFS) {
                                AlertDisplayMode.FullScreen(sound = SoundConfig(volume = 80))
                            } else {
                                AlertDisplayMode.NotificationOnly
                            }
                            onConfigChanged(config.copy(displayMode = newDisplayMode))
                        }
                    )
                }

                // Audio Sound Section (only active if FullScreen)
                if (config.isFullScreen) {
                    // Sound Enable Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(id = R.string.alert_profile_play_sound),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Switch(
                            checked = sound != null,
                            onCheckedChange = { playSound ->
                                val updatedSound = if (playSound) {
                                    SoundConfig(volume = 80)
                                } else {
                                    null
                                }
                                onConfigChanged(config.copy(displayMode = AlertDisplayMode.FullScreen(sound = updatedSound)))
                            }
                        )
                    }

                    if (sound != null) {
                        // Sound Selection
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(id = R.string.alert_profile_sound_label),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = soundTitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (!sound.soundUri.isNullOrEmpty()) {
                                    IconButton(
                                        onClick = {
                                            val updatedSound = sound.copy(soundUri = null)
                                            onConfigChanged(config.copy(displayMode = AlertDisplayMode.FullScreen(updatedSound)))
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = stringResource(id = R.string.alert_profile_sound_reset)
                                        )
                                    }
                                }
                                if (onSelectSound != null) {
                                    NormalTextButton(onClick = onSelectSound) {
                                        Text(text = stringResource(id = R.string.alert_profile_sound_select))
                                    }
                                }
                            }
                        }

                        // Volume Slider
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(id = R.string.alert_profile_volume_label, sound.volume),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        Slider(
                            value = sound.volume.toFloat(),
                            onValueChange = { floatValue ->
                                val roundedVolume = ((floatValue / 5f).roundToInt() * 5).coerceIn(0, 100)
                                val updatedSound = sound.copy(volume = roundedVolume)
                                onConfigChanged(config.copy(displayMode = AlertDisplayMode.FullScreen(updatedSound)))
                                onUpdateVolume?.invoke(roundedVolume)
                            },
                            valueRange = 0f..100f,
                            steps = 19
                        )
                    }
                }

                // Vibration Mode
                Text(
                    text = stringResource(id = R.string.alert_profile_vibration_label),
                    style = MaterialTheme.typography.bodyMedium
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    VibrationMode.entries.forEach { mode ->
                        FilterChip(
                            selected = config.vibrationMode == mode,
                            onClick = { onConfigChanged(config.copy(vibrationMode = mode)) },
                            label = {
                                Text(
                                    text = vibrationModeLabel(mode),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        )
                    }
                }

                // DND Override Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(id = R.string.alert_profile_override_dnd),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Switch(
                        checked = config.overrideDnd,
                        onCheckedChange = { onConfigChanged(config.copy(overrideDnd = it)) }
                    )
                }
            }
        }
    }
}

@Composable
private fun vibrationModeLabel(mode: VibrationMode): String {
    return when (mode) {
        VibrationMode.OFF -> stringResource(id = R.string.alert_profile_vibration_off)
        VibrationMode.SHORT -> stringResource(id = R.string.alert_profile_vibration_short)
        VibrationMode.LONG -> stringResource(id = R.string.alert_profile_vibration_long)
        VibrationMode.CONTINUOUS -> stringResource(id = R.string.alert_profile_vibration_continuous)
    }
}

private fun getRingtoneTitle(context: Context, soundUriString: String?): String {
    if (soundUriString.isNullOrEmpty()) {
        return context.getString(R.string.alert_profile_sound_default)
    }
    return try {
        val uri = soundUriString.toUri()
        val ringtone = RingtoneManager.getRingtone(context, uri)
        val title = ringtone?.getTitle(context)
        if (!title.isNullOrEmpty()) title else context.getString(R.string.alert_profile_sound_custom)
    } catch (_: Exception) {
        context.getString(R.string.alert_profile_sound_custom)
    }
}

@Preview(showBackground = true, name = "Light Mode")
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, name = "Dark Mode")
@Composable
fun SignalConfigCardPreview() {
    AppPreview {
        SignalConfigCard(
            title = "Kritische Alarme",
            config = AlertSignalConfig(
                enabled = true,
                displayMode = AlertDisplayMode.FullScreen(SoundConfig(volume = 100)),
                vibrationMode = VibrationMode.CONTINUOUS,
                overrideDnd = true
            ),
            onConfigChanged = {},
            showEnabledToggle = false,
            onPlayPreview = {},
            onSelectSound = {}
        )
    }
}