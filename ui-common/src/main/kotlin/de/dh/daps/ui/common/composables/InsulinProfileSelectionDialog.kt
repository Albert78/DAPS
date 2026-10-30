package de.dh.daps.ui.common.composables

import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ListItem
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import de.dh.daps.common.domain.model.therapy.InsulinProfile
import de.dh.daps.common.R as CommonR

@Composable
fun InsulinProfileSelectionDialog(
    profiles: List<InsulinProfile>,
    activeProfileId: Long?,
    onProfileSelected: (InsulinProfile) -> Unit,
    onDismiss: () -> Unit
) {
    AppAlertDialog(
        titleText = stringResource(id = CommonR.string.therapy_insulin_profile_selection_title),
        onDismissRequest = onDismiss,
        confirmButtonText = stringResource(id = android.R.string.cancel),
        onConfirm = onDismiss,
        dismissButtonText = null,
    ) {
        LazyColumn {
            items(profiles) { profile ->
                ListItem(
                    headlineContent = { Text(profile.name) },
                    leadingContent = {
                        RadioButton(
                            selected = profile.id == activeProfileId,
                            onClick = null // Handled by ListItem click
                        )
                    },
                    modifier = Modifier.clickable { onProfileSelected(profile) }
                )
            }
        }
    }
}