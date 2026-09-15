package de.dh.daps.ui.screens.alarmprofiles

import android.content.Context
import android.content.res.Configuration
import android.media.RingtoneManager
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import kotlin.math.roundToInt
import de.dh.daps.common.R as CommonR
import de.dh.daps.ui.R
import de.dh.daps.ui.common.composables.NormalTextButton
import de.dh.daps.ui.common.composables.contentScrollIndicator
import de.dh.daps.ui.common.theme.AppTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class RingtoneItem(
    val title: String,
    val uri: String?, // null = Standard System-Alarmton
    val isCustomAction: Boolean = false
)

@Composable
fun RingtonePickerDialog(
    currentUri: String?,
    currentVolume: Int,
    onSoundSelected: (String?, Int) -> Unit,
    onPlayPreview: (String?, Int) -> Unit,
    onUpdateVolume: (Int) -> Unit,
    onStopPreview: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedUri by remember { mutableStateOf(currentUri) }
    var selectedVolume by remember { mutableIntStateOf(currentVolume) }
    var ringtones by remember { mutableStateOf<List<RingtoneItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    val customAudioLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val uriString = uri.toString()
            selectedUri = uriString
            onPlayPreview(uriString, selectedVolume)
        }
    }

    val defaultTitle = stringResource(id = R.string.alarm_profile_sound_default)
    val customFileTitle = stringResource(id = R.string.alarm_profile_sound_select_custom_file)

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val list = mutableListOf<RingtoneItem>()
            list.add(
                RingtoneItem(
                    title = defaultTitle,
                    uri = null
                )
            )

            if (!currentUri.isNullOrEmpty()) {
                val customTitle = getRingtoneTitle(context, currentUri)
                list.add(RingtoneItem(title = customTitle, uri = currentUri))
            }

            try {
                val manager = RingtoneManager(context).apply {
                    setType(RingtoneManager.TYPE_ALARM)
                }
                val cursor = manager.cursor
                while (cursor != null && cursor.moveToNext()) {
                    val title = cursor.getString(RingtoneManager.TITLE_COLUMN_INDEX)
                    val uri = manager.getRingtoneUri(cursor.position)?.toString()
                    if (!title.isNullOrEmpty() && uri != null && uri != currentUri) {
                        list.add(RingtoneItem(title = title, uri = uri))
                    }
                }
            } catch (e: Exception) {
                Log.e("RingtonePickerDialog", "Error loading system ringtones", e)
            }

            list.add(
                RingtoneItem(
                    title = customFileTitle,
                    uri = "ACTION_CUSTOM_FILE",
                    isCustomAction = true
                )
            )

            ringtones = list
            isLoading = false
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            onStopPreview()
        }
    }

    RingtonePickerDialogContent(
        selectedUri = selectedUri,
        selectedVolume = selectedVolume,
        ringtones = ringtones,
        isLoading = isLoading,
        onSelectUri = { uri ->
            selectedUri = uri
            onPlayPreview(uri, selectedVolume)
        },
        onVolumeChanged = { newVolume ->
            selectedVolume = newVolume
            onUpdateVolume(newVolume)
        },
        onPickCustomFile = {
            customAudioLauncher.launch("audio/*")
        },
        onConfirm = {
            onStopPreview()
            onSoundSelected(selectedUri, selectedVolume)
            onDismiss()
        },
        onDismiss = {
            onStopPreview()
            onDismiss()
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RingtonePickerDialogContent(
    selectedUri: String?,
    selectedVolume: Int,
    ringtones: List<RingtoneItem>,
    isLoading: Boolean,
    onSelectUri: (String?) -> Unit,
    onVolumeChanged: (Int) -> Unit,
    onPickCustomFile: () -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                text = stringResource(id = R.string.alarm_profile_sound_picker_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val listState = rememberLazyListState()
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 280.dp)
                            .contentScrollIndicator(listState),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(
                            items = ringtones,
                            key = { it.uri ?: "default_sound" }
                        ) { item ->
                            if (item.isCustomAction) {
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 4.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { onPickCustomFile() },
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                                    border = BorderStroke(
                                        1.dp,
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = item.title,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            } else {
                                val isSelected = (item.uri == selectedUri)
                                val containerColor = if (isSelected) {
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                                }

                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { onSelectUri(item.uri) },
                                    color = containerColor,
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            modifier = Modifier.weight(1f),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            RadioButton(
                                                selected = isSelected,
                                                onClick = null
                                            )
                                            Text(
                                                text = item.title,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.padding(end = 4.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    // Live Volume Slider inside Dialog
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(id = R.string.alarm_profile_volume_label, selectedVolume),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Slider(
                            value = selectedVolume.toFloat(),
                            onValueChange = { floatValue ->
                                val roundedVol = ((floatValue / 5f).roundToInt() * 5).coerceIn(0, 100)
                                onVolumeChanged(roundedVol)
                            },
                            valueRange = 0f..100f,
                            steps = 19
                        )
                    }
                }
            }
        },
        confirmButton = {
            NormalTextButton(onClick = onConfirm) {
                Text(
                    text = stringResource(id = CommonR.string.action_save),
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            NormalTextButton(onClick = onDismiss) {
                Text(stringResource(id = CommonR.string.action_cancel))
            }
        }
    )
}

private fun getRingtoneTitle(context: Context, soundUriString: String?): String {
    if (soundUriString.isNullOrEmpty()) {
        return context.getString(R.string.alarm_profile_sound_default)
    }
    return try {
        val uri = soundUriString.toUri()
        val ringtone = RingtoneManager.getRingtone(context, uri)
        val title = ringtone?.getTitle(context)
        if (!title.isNullOrEmpty()) title else context.getString(R.string.alarm_profile_sound_custom)
    } catch (_: Exception) {
        context.getString(R.string.alarm_profile_sound_custom)
    }
}

@Preview(showBackground = true, name = "Light Mode")
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, name = "Dark Mode")
@Composable
fun RingtonePickerDialogPreview() {
    AppTheme {
        RingtonePickerDialogContent(
            selectedUri = "content://media/internal/audio/media/1",
            selectedVolume = 80,
            ringtones = listOf(
                RingtoneItem("Standard System-Alarmton", null),
                RingtoneItem("Sanftes Erwachen", "content://media/internal/audio/media/1"),
                RingtoneItem("Digitaler Alarm", "content://media/internal/audio/media/2"),
                RingtoneItem("Eigene Audio-Datei auswählen…", "ACTION_CUSTOM_FILE", isCustomAction = true)
            ),
            isLoading = false,
            onSelectUri = {},
            onVolumeChanged = {},
            onPickCustomFile = {},
            onConfirm = {},
            onDismiss = {}
        )
    }
}