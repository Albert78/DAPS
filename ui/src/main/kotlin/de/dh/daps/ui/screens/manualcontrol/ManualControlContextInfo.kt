package de.dh.daps.ui.screens.manualcontrol

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.dh.daps.common.model.InsulinAmount
import de.dh.daps.common.model.data.BgReading
import de.dh.daps.common.model.data.BgSampleKind
import de.dh.daps.common.model.data.BgValue
import de.dh.daps.common.model.data.Timestamp
import de.dh.daps.ui.common.carbsValue
import de.dh.daps.ui.common.glucoseUnitLabel
import de.dh.daps.ui.common.glucoseValue
import de.dh.daps.ui.common.insulinValue
import de.dh.daps.ui.common.theme.AppPreview
import de.dh.daps.ui.common.theme.ExtendedTheme
import de.dh.daps.ui.common.time
import de.dh.daps.common.R as CommonR

@Composable
fun ManualControlContextInfo(
    contextInfo: ManualControlContextInfoUiModel
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RectangleShape,
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val displayBgValue = contextInfo.lastBgReading?.value ?: BgValue.INVALID
                val bgText = glucoseValue(displayBgValue, default = "??")
                val colors = ExtendedTheme.semanticColors
                val textColor = if (displayBgValue.isInvalid()) {
                    Color.Gray
                } else when {
                    displayBgValue.mgdl < 55 -> colors.glucoseVeryLow
                    displayBgValue.mgdl < 70 -> colors.glucoseLow
                    displayBgValue.mgdl < 180 -> colors.glucoseTarget
                    displayBgValue.mgdl < 220 -> colors.glucoseElevated
                    else -> colors.glucoseHigh
                }
                Text(
                    text = bgText,
                    style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold),
                    color = textColor
                )
                Text(
                    text = glucoseUnitLabel(),
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.Gray,
                    modifier = Modifier
                        .align(Alignment.Bottom)
                        .padding(bottom = 12.dp)
                )
            }

            val timestamp = contextInfo.lastBgReading?.timestamp
            val timeText = if (timestamp != null && timestamp.isValid()) {
                stringResource(CommonR.string.at_time_format, time(timestamp))
            } else {
                "--"
            }
            Text(
                text = timeText,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )

            Spacer(Modifier.height(12.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = stringResource(CommonR.string.label_cob),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = carbsValue(contextInfo.cob),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = stringResource(CommonR.string.label_iob),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = insulinValue(contextInfo.iob.iu),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "Light Mode")
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, name = "Dark Mode")
@Composable
private fun ManualControlContextInfoPreview() {
    AppPreview {
        ManualControlContextInfo(
            contextInfo = ManualControlContextInfoUiModel(
                lastBgReading = BgReading(
                    value = BgValue.fromMgDl(120),
                    sampleKind = BgSampleKind.Value,
                    timestamp = Timestamp.now()
                ),
                iob = InsulinAmount(1.5),
                cob = 25.0
            )
        )
    }
}