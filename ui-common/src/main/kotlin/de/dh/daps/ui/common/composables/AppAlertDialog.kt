package de.dh.daps.ui.common.composables

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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

/**
 * High-level convenience variant of [AppAlertDialog] integrating [DialogSurface], [DialogTitle],
 * accessibility semantics, and [DialogOkDismissButtons].
 */
@Composable
fun AppAlertDialog(
    onDismissRequest: () -> Unit,
    titleText: String,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    okButtonText: String = stringResource(id = android.R.string.ok),
    cancelButtonText: String = stringResource(id = android.R.string.cancel),
    isOkEnabled: Boolean = true,
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

                DialogOkDismissButtons(
                    onConfirm = onConfirm,
                    onDismiss = onDismissRequest,
                    okButtonText = okButtonText,
                    cancelButtonText = cancelButtonText,
                    isOkEnabled = isOkEnabled,
                )
            }
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