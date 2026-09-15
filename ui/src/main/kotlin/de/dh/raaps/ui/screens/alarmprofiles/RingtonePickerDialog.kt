package de.dh.raaps.ui.screens.alarmprofiles

import android.content.Context
import android.media.RingtoneManager
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import de.dh.raaps.common.R as CommonR
import de.dh.raaps.ui.R
import de.dh.raaps.ui.common.composables.NormalTextButton
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
    onSoundSelected: (String?) -> Unit,
    onPlayPreview: (String?) -> Unit,
    onStopPreview: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedUri by remember { mutableStateOf(currentUri) }
    var ringtones by remember { mutableStateOf<List<RingtoneItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    val customAudioLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val uriString = uri.toString()
            selectedUri = uriString
            onPlayPreview(uriString)
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

    AlertDialog(
        onDismissRequest = {
            onStopPreview()
            onDismiss()
        },
        title = {
            Text(text = stringResource(id = R.string.alarm_profile_sound_picker_title))
        },
        text = {
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 350.dp)
                ) {
                    items(ringtones) { item ->
                        if (item.isCustomAction) {
                            HorizontalDivider()
                            ListItem(
                                headlineContent = {
                                    Text(
                                        text = item.title,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                },
                                leadingContent = {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                },
                                modifier = Modifier.clickable {
                                    customAudioLauncher.launch("audio/*")
                                }
                            )
                        } else {
                            val isSelected = (item.uri == selectedUri)
                            val displayTitle = if (isSelected && !item.uri.isNullOrEmpty() && item.uri == selectedUri) {
                                getRingtoneTitle(context, item.uri)
                            } else {
                                item.title
                            }
                            ListItem(
                                headlineContent = { Text(displayTitle) },
                                leadingContent = {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = null
                                    )
                                },
                                modifier = Modifier.clickable {
                                    selectedUri = item.uri
                                    onPlayPreview(item.uri)
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            NormalTextButton(
                onClick = {
                    onStopPreview()
                    onSoundSelected(selectedUri)
                    onDismiss()
                }
            ) {
                Text(stringResource(id = CommonR.string.action_save))
            }
        },
        dismissButton = {
            NormalTextButton(
                onClick = {
                    onStopPreview()
                    onDismiss()
                }
            ) {
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