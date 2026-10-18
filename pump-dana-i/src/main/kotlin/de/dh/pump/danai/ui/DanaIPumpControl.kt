package de.dh.pump.danai.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.dh.daps.common.model.InsulinPump
import de.dh.daps.ui.common.composables.FramedCard
import de.dh.daps.ui.common.composables.NormalButton
import de.dh.daps.ui.common.composables.SecondaryButton
import de.dh.daps.ui.common.theme.ExtendedTheme
import de.dh.pump.danai.R
import de.dh.pump.danai.core.DanaIPumpAdapter

/**
 * Composable providing Dana-i specific pump controls embedded in the System Control screen.
 * Highlights the card in warning/error signal color when maintenance mode is active.
 */
@Composable
fun DanaIPumpControl(
    pump: InsulinPump,
    modifier: Modifier = Modifier,
) {
    val adapter = pump as? DanaIPumpAdapter

    adapter?.let { activeAdapter ->
        val isMaintenanceActive = activeAdapter.isMaintenanceMode.collectAsState().value

        val containerColor = if (isMaintenanceActive) {
            MaterialTheme.colorScheme.errorContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        }

        val borderColor = if (isMaintenanceActive) {
            MaterialTheme.colorScheme.error
        } else {
            ExtendedTheme.semanticColors.border
        }

        val subtitleRes = if (isMaintenanceActive) {
            R.string.danai_pump_control_maintenance_subtitle_on
        } else {
            R.string.danai_pump_control_maintenance_subtitle_off
        }

        FramedCard(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            containerColor = containerColor,
            borderColor = borderColor,
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
            ) {
                Text(
                    text = stringResource(R.string.danai_pump_control_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isMaintenanceActive) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurface,
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stringResource(subtitleRes),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isMaintenanceActive) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (isMaintenanceActive) {
                    SecondaryButton(
                        onClick = { activeAdapter.setMaintenanceMode(false) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Build,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 8.dp),
                        )
                        Text(stringResource(R.string.danai_pump_control_btn_reconnect_maintenance))
                    }
                } else {
                    NormalButton(
                        onClick = { activeAdapter.setMaintenanceMode(true) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Build,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 8.dp),
                        )
                        Text(stringResource(R.string.danai_pump_control_btn_disconnect_maintenance))
                    }
                }
            }
        }
    }
}