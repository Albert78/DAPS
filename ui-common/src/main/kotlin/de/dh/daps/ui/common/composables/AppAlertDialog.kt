package de.dh.daps.ui.common.composables

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.dialog
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import de.dh.daps.ui.common.theme.AppPreview

/**
 * High-level convenience variant of [AppAlertDialog] integrating [DialogSurface], [DialogTitle],
 * accessibility semantics, and [DialogConfirmDismissButtons].
 */
@Composable
fun AppAlertDialog(
    titleText: String,
    modifier: Modifier = Modifier,
    icon: @Composable (() -> Unit)? = null,
    confirmButtonText: String = stringResource(id = android.R.string.ok),
    onConfirm: () -> Unit,
    isConfirmEnabled: Boolean = true,
    dismissButtonText: String? = stringResource(id = android.R.string.cancel),
    onDismissRequest: () -> Unit,
    content: @Composable () -> Unit,
) {
    AppAlertDialog(
        titleText = titleText,
        modifier = modifier,
        icon = icon,
        onDismissRequest = onDismissRequest,
        confirmButton = {
            NormalTextButton(
                onClick = onConfirm,
                enabled = isConfirmEnabled,
            ) {
                Text(confirmButtonText)
            }
        },
        dismissButton = if (dismissButtonText == null) {
            null
        } else {
            {
                NormalTextButton(
                    onClick = onDismissRequest,
                ) {
                    Text(dismissButtonText)
                }
            }
        },
        content = content,
    )
}

/**
 * Alert dialog with custom buttons.
 */
@Composable
fun AppAlertDialog(
    titleText: String,
    modifier: Modifier = Modifier,
    icon: @Composable (() -> Unit)? = null,
    onDismissRequest: () -> Unit = {},
    confirmButton: @Composable () -> Unit,
    dismissButton: @Composable (() -> Unit)? = null,
    content: @Composable (() -> Unit)? = null,
) {
    Dialog(
        onDismissRequest = onDismissRequest,
    ) {
        DialogSurface(
            modifier = modifier.semantics {
                dialog()
                paneTitle = titleText
            },
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
            ) {
                DialogTitle(text = titleText, icon = icon)

                if (content != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    content()
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Spacer(
                        modifier = Modifier.weight(1f)
                    )
                    if (dismissButton != null) {
                        dismissButton()
                        Spacer(modifier = Modifier.padding(start = 8.dp))
                    }
                    confirmButton()
                }
            }
        }
    }
}

/**
 * Custom content variant of [AppAlertDialog] using [Dialog], [DialogSurface], and proper accessibility semantics.
 */
@Composable
fun AppAlertDialog(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    titleText: String? = null,
    properties: DialogProperties = DialogProperties(),
    content: @Composable () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = properties,
    ) {
        DialogSurface(
            modifier = modifier.semantics {
                dialog()
                titleText?.let { paneTitle = it }
            },
        ) {
            content()
        }
    }
}

@Preview(heightDp = 400)
@Composable
fun AppAlertDialogPreview() {
    AppPreview {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            AppAlertDialog(
                onDismissRequest = {},
                titleText = "Simple AppAlertDialog",
                onConfirm = {},
            ) {
                Text("This dialog shows the simples form of AppAlertDialog.")
            }
        }
    }
}

@Preview(heightDp = 400)
@Composable
fun AppAlertDialogWithIconPreview() {
    AppPreview {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            AppAlertDialog(
                onDismissRequest = {},
                titleText = "Dialog with Icon",
                icon = {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                    )
                },
                onConfirm = {},
            ) {
                Text("This dialog includes an optional icon parameter.")
            }
        }
    }
}

@Preview(heightDp = 400)
@Composable
fun AppAlertDialogCustomPreview() {
    AppPreview {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            AppAlertDialog(
                onDismissRequest = {},
                titleText = "Convenience Dialog",
                onConfirm = {},
            ) {
                Text("This is a preview of the high-level custom AppAlertDialog with accessibility semantics.")
            }
        }
    }
}