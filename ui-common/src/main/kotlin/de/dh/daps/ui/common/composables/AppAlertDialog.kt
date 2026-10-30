package de.dh.daps.ui.common.composables

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.dialog
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
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
    confirmButtonText: String = stringResource(id = android.R.string.ok),
    onConfirm: () -> Unit,
    isConfirmEnabled: Boolean = true,
    dismissButtonText: String = stringResource(id = android.R.string.cancel),
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
                DialogTitle(text = titleText)

                Spacer(modifier = Modifier.height(16.dp))

                content()

                Spacer(modifier = Modifier.height(24.dp))

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

/**
 * A drop-in replacement for Material 3 [AlertDialog] that enforces consistent DAPS application styling
 * across all dialogs.
 */
@Composable
fun AppAlertDialog(
    modifier: Modifier = Modifier,
    icon: @Composable (() -> Unit)? = null,
    title: @Composable (() -> Unit)? = null,
    text: @Composable (() -> Unit)? = null,
    onDismissRequest: () -> Unit = {},
    confirmButton: @Composable () -> Unit,
    dismissButton: @Composable (() -> Unit)? = null,
    shape: Shape = MaterialTheme.shapes.large,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    iconContentColor: Color = AlertDialogDefaults.iconContentColor,
    titleContentColor: Color = MaterialTheme.colorScheme.onSurface,
    textContentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    tonalElevation: Dp = 8.dp,
    properties: DialogProperties = DialogProperties(),
) {
    AlertDialog(
        modifier = modifier,
        onDismissRequest = onDismissRequest,
        confirmButton = confirmButton,
        dismissButton = dismissButton,
        icon = icon,
        title = title,
        text = text,
        shape = shape,
        containerColor = containerColor,
        iconContentColor = iconContentColor,
        titleContentColor = titleContentColor,
        textContentColor = textContentColor,
        tonalElevation = tonalElevation,
        properties = properties,
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

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun AppAlertDialogPreview() {
    AppPreview {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            AppAlertDialog(
                onDismissRequest = {},
                title = { Text("AppAlertDialog Title") },
                text = { Text("This is a preview of the AppAlertDialog drop-in replacement.") },
                confirmButton = {
                    NormalTextButton(onClick = {}) {
                        Text("OK")
                    }
                },
                dismissButton = {
                    NormalTextButton(onClick = {}) {
                        Text("Cancel")
                    }
                },
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
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