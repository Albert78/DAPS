package de.dh.daps.ui.common.composables

import android.R
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.dh.daps.ui.common.theme.AppPreview

@Composable
fun DialogConfirmDismissButtons(
    modifier: Modifier = Modifier,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    confirmButtonText: String = stringResource(id = R.string.ok),
    dismissButtonText: String = stringResource(id = R.string.cancel),
    isConfirmEnabled: Boolean = true
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
    ) {
        Spacer(
            modifier = Modifier.weight(1f)
        )
        NormalTextButton(
            onClick = onDismiss
        ) {
            Text(dismissButtonText)
        }
        Spacer(modifier = Modifier.padding(start = 8.dp))
        NormalTextButton(
            onClick = onConfirm,
            enabled = isConfirmEnabled
        ) {
            Text(confirmButtonText)
        }
    }
}

@Composable
fun DialogConfirmButton(
    modifier: Modifier = Modifier,
    onConfirm: () -> Unit,
    okButtonText: String = stringResource(id = R.string.ok),
    isOkEnabled: Boolean = true
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
    ) {
        Spacer(
            modifier = Modifier.weight(1f)
        )
        NormalTextButton(
            onClick = onConfirm,
            enabled = isOkEnabled
        ) {
            Text(okButtonText)
        }
    }
}

@Preview
@Composable
fun DialogConfirmDismissButtonsPreview() {
    AppPreview {
        DialogConfirmDismissButtons(
            onConfirm = {},
            onDismiss = {}
        )
    }
}

@Preview
@Composable
fun DialogConfirmButtonPreview() {
    AppPreview {
        DialogConfirmButton(
            onConfirm = {}
        )
    }
}