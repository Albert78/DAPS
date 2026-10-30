package de.dh.daps.plugin.simbody.ui.screens

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.dh.daps.common.domain.model.cgm.GlucoseSource
import de.dh.daps.common.domain.model.cgm.GlucoseSourceStatus
import de.dh.daps.common.domain.model.cgm.SourceHardwareInformation
import de.dh.daps.common.domain.model.data.BgReading
import de.dh.daps.common.domain.model.data.Timestamp
import de.dh.daps.common.ui.UiText
import de.dh.daps.plugin.simbody.R
import de.dh.daps.ui.common.theme.AppPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow
import de.dh.daps.common.R as CommonR

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimBodyGlucoseConfigurationScreen(
    glucoseSource: GlucoseSource,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var fastSimulation by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(stringResource(R.string.sim_body_cgm_config_screen_title))
                },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(CommonR.string.cd_close)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(text = "TODO")
        }
    }
}

private class PreviewSimBodyGlucoseSource : GlucoseSource {
    override val glucoseSourceId: String = "preview-glucose-source"
    override val sourceDisplayName: UiText = UiText.DynamicString("SimBody Virtueller Glukosesensor")
    override val readingsInterval = de.dh.daps.common.domain.model.data.BgReadingsInterval.FiveMinutes
    override val readingsTimeDelay = de.dh.daps.common.domain.model.data.Minutes(5)
    override val status: StateFlow<GlucoseSourceStatus> = MutableStateFlow(GlucoseSourceStatus.Ok)
    override val startDate: StateFlow<Timestamp?> = MutableStateFlow(null)
    override val endDate: StateFlow<Timestamp?> = MutableStateFlow(null)
    override val isExpired: StateFlow<Boolean> = MutableStateFlow(false)
    override val lastConnection: StateFlow<Timestamp?> = MutableStateFlow(null)
    override val sensorType: StateFlow<String> = MutableStateFlow("Sim-Body-Sensor")
    override val hardwareInformation: StateFlow<SourceHardwareInformation?> = MutableStateFlow(null)

    override fun start() {}
    override fun stop() {}
    override fun getValues(): Flow<BgReading> = emptyFlow()
}

@Preview(name = "SimBody Glucose Config - Light", showBackground = true)
@Preview(name = "SimBody Glucose Config - Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SimBodyGlucoseConfigurationScreenPreview() {
    AppPreview {
        SimBodyGlucoseConfigurationScreen(
            glucoseSource = PreviewSimBodyGlucoseSource(),
            onClose = {}
        )
    }
}