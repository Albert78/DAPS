package de.dh.pump.omnipod.dash.ui

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.dh.daps.common.model.BasalStatus
import de.dh.daps.common.model.BolusEvent
import de.dh.daps.common.model.BolusStatus
import de.dh.daps.common.model.Expiration
import de.dh.daps.common.model.InsulinAmount
import de.dh.daps.common.model.InsulinConcentration
import de.dh.daps.common.model.InsulinHistory
import de.dh.daps.common.model.InsulinPump
import de.dh.daps.common.model.InsulinPumpStatus
import de.dh.daps.common.model.PumpAlerts
import de.dh.daps.common.model.PumpCapabilities
import de.dh.daps.common.model.PumpHardwareInformation
import de.dh.daps.common.model.data.InsulinProfile
import de.dh.daps.common.model.data.Timestamp
import de.dh.daps.common.model.getDefaultInsulinTypes
import de.dh.daps.common.ui.UiText
import de.dh.daps.ui.common.composables.FramedCard
import de.dh.daps.ui.common.composables.screenTitle
import de.dh.daps.ui.common.theme.AppPreview
import de.dh.pump.omnipod.dash.OmnipodDashInsulinPumpDriver
import de.dh.pump.omnipod.dash.R
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import de.dh.daps.common.R as CommonR

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OmnipodDashConfigurationScreen(
    pump: InsulinPump,
    driver: OmnipodDashInsulinPumpDriver?,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val availableInsulinTypes = remember(context) { getDefaultInsulinTypes(context) }
    val currentInsulinTypeId by pump.insulinTypeId.collectAsState()
    val currentConcentration by pump.insulinConcentration.collectAsState()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = screenTitle(stringResource(R.string.omnipod_dash_config_screen_title)),
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // FramedCard: Insulin Type Setting
                FramedCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.omnipod_dash_config_insulin_type_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource(R.string.omnipod_dash_config_insulin_type_subtitle),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            availableInsulinTypes.forEach { insulinType ->
                                val isSelected = currentInsulinTypeId == insulinType.id
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        driver?.let { d ->
                                            scope.launch {
                                                val stateManager = d.getPodStateManager()
                                                stateManager.setInsulinTypeId(insulinType.id)
                                                stateManager.saveToPreferences()
                                            }
                                        }
                                    },
                                    label = { Text(text = insulinType.name) }
                                )
                            }
                        }
                    }
                }

                // FramedCard: Insulin Concentration Setting
                FramedCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.omnipod_dash_config_concentration_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource(R.string.omnipod_dash_config_concentration_subtitle),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val options = listOf(
                                InsulinConcentration.U100 to "U100",
                                InsulinConcentration.U40 to "U40",
                                InsulinConcentration.U200 to "U200",
                                InsulinConcentration.U20 to "U20",
                            )

                            options.forEach { (concentration, label) ->
                                val isSelected = currentConcentration == concentration
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        driver?.let { d ->
                                            scope.launch {
                                                val stateManager = d.getPodStateManager()
                                                stateManager.setInsulinConcentration(concentration)
                                                stateManager.saveToPreferences()
                                            }
                                        }
                                    },
                                    label = { Text(text = label) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private class PreviewOmnipodPump(
    override val insulinTypeId: StateFlow<String?> = MutableStateFlow(null),
    override val insulinConcentration: StateFlow<InsulinConcentration> = MutableStateFlow(InsulinConcentration.U100),
) : InsulinPump {
    override val insulinPumpId: String = "preview-omnipod"
    override val insulinPumpDisplayName: UiText = UiText.DynamicString("Omnipod Dash")
    override val startDate: StateFlow<Timestamp?> = MutableStateFlow(null)
    override val isExpired: StateFlow<Boolean> = MutableStateFlow(false)
    override val expirations: StateFlow<List<Expiration>> = MutableStateFlow(emptyList())
    override val hardwareInformation: StateFlow<PumpHardwareInformation?> = MutableStateFlow(null)
    override val pumpCapabilities: StateFlow<PumpCapabilities> = MutableStateFlow(
        PumpCapabilities(
            minBasalRate = InsulinAmount(0.05),
            supportsZeroBasal = true,
            minBasalIncrement = InsulinAmount(0.05),
            minBolusAmount = InsulinAmount(0.05),
            minBolusIncrement = InsulinAmount(0.05),
            maxBolusSize = InsulinAmount(30.0)
        )
    )
    override val isConnected: StateFlow<Boolean> = MutableStateFlow(true)
    override val isBlocked: StateFlow<Boolean> = MutableStateFlow(false)
    override val pumpStatus: StateFlow<InsulinPumpStatus> = MutableStateFlow(
        object : InsulinPumpStatus {
            override val pumpSuspended: Boolean = false
            override val batteryRemainingPercent: Int = 100
            override val reservoirRemainingUnits: InsulinAmount = InsulinAmount(200.0)
            override val lastSyncTimestamp: Timestamp = Timestamp.now()
        }
    )
    override val alerts: StateFlow<PumpAlerts> = MutableStateFlow(PumpAlerts())
    override val basalStatus: StateFlow<BasalStatus> = MutableStateFlow(BasalStatus())
    override val bolusStatus: StateFlow<BolusStatus> = MutableStateFlow(BolusStatus())
    override val bolusEvents: SharedFlow<BolusEvent> = MutableSharedFlow()
    override val history: StateFlow<InsulinHistory?> = MutableStateFlow(null)
    override suspend fun bolus(amount: InsulinAmount, bolusId: String?) {}
    override suspend fun stopBolus() {}
    override suspend fun tempBasal(percent: Int, durationHours: Int) {}
    override suspend fun cancelTempBasal() {}
    override suspend fun setSuspend(suspended: Boolean) {}
    override suspend fun setProfile(profile: InsulinProfile) {}
    override suspend fun syncHistory(start: Timestamp) {}
    override suspend fun refreshStatus() {}
    override fun stop() {}
}

@Preview(name = "Omnipod Dash Config - Light", showBackground = true)
@Preview(name = "Omnipod Dash Config - Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun OmnipodDashConfigurationScreenPreview() {
    AppPreview {
        OmnipodDashConfigurationScreen(
            pump = PreviewOmnipodPump(),
            driver = null,
            onClose = {}
        )
    }
}