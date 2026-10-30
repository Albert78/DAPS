package de.dh.daps.ui.common.composables

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
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
                paneTitle = titleText
            },
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
            ) {
                DialogTitle(text = titleText, icon = icon)

                Spacer(modifier = Modifier.height(16.dp))

                content()

                Spacer(modifier = Modifier.height(24.dp))

                if (dismissButtonText == null) {
                    DialogConfirmButton(
                        onConfirm = onConfirm,
                        confirmButtonText = confirmButtonText,
                        isConfirmEnabled = isConfirmEnabled,
                    )
                } else {
                    DialogConfirmDismissButtons(
                        onConfirm = onConfirm,
                        onDismiss = onDismissRequest,
                        confirmButtonText = confirmButtonText,
                        dismissButtonText = dismissButtonText,
                        isConfirmEnabled = isConfirmEnabled,
                    )
                }
            }
        }
    }
}

/**
 * Alert dialog with custom buttons.
 */
@Composable
fun AppAlertDialog(
    title: @Composable (() -> Unit),
    modifier: Modifier = Modifier,
    icon: @Composable (() -> Unit)? = null,
    onDismissRequest: () -> Unit = {},
    confirmButton: @Composable () -> Unit,
    dismissButton: @Composable (() -> Unit)? = null,
    content: @Composable (() -> Unit)? = null,
) {
    AlertDialog(
        modifier = modifier,
        onDismissRequest = onDismissRequest,
        confirmButton = confirmButton,
        dismissButton = dismissButton,
        icon = icon,
        title = title,
        text = content,
    )
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