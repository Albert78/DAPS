package de.dh.daps.plugin.simbody.ui.screens

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import de.dh.daps.common.model.BasalStatus
import de.dh.daps.common.model.BolusEvent
import de.dh.daps.common.model.BolusStatus
import de.dh.daps.common.model.InsulinAmount
import de.dh.daps.common.model.InsulinConcentration
import de.dh.daps.common.model.InsulinHistory
import de.dh.daps.common.model.InsulinPump
import de.dh.daps.common.model.InsulinPumpStatus
import de.dh.daps.common.model.PluginPreferences
import de.dh.daps.common.model.PumpAlerts
import de.dh.daps.common.model.PumpCapabilities
import de.dh.daps.common.model.PumpHardwareInformation
import de.dh.daps.common.model.PumpLifecycleState
import de.dh.daps.common.model.data.InsulinProfile
import de.dh.daps.common.model.data.Timestamp
import de.dh.daps.common.model.getDefaultInsulinTypes
import de.dh.daps.common.ui.UiText
import de.dh.daps.plugin.simbody.R
import de.dh.daps.plugin.simbody.SimBodyInsulinPump
import de.dh.daps.plugin.simbody.SimBodyInsulinPumpDriver
import de.dh.daps.ui.common.composables.PumpConfigurationScreen
import de.dh.daps.ui.common.theme.AppPreview
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@Composable
fun SimBodyConfigurationScreen(
    pump: InsulinPump,
    preferences: PluginPreferences?,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val availableInsulinTypes = remember(context) { getDefaultInsulinTypes(context) }
    val currentInsulinTypeId by pump.insulinTypeId.collectAsState()
    val currentConcentration by pump.insulinConcentration.collectAsState()

    PumpConfigurationScreen(
        title = stringResource(R.string.sim_body_config_screen_title),
        selectedTypeId = currentInsulinTypeId,
        availableTypes = availableInsulinTypes,
        onSelectType = { insulinType ->
            (pump as? SimBodyInsulinPump)?.setInsulinTypeId(insulinType.id)
            preferences?.let { prefs ->
                scope.launch {
                    prefs.putString(SimBodyInsulinPumpDriver.KEY_INSULIN_TYPE_ID, insulinType.id)
                }
            }
        },
        selectedConcentration = currentConcentration,
        onSelectConcentration = { concentration ->
            (pump as? SimBodyInsulinPump)?.setInsulinConcentration(concentration)
            preferences?.let { prefs ->
                scope.launch {
                    prefs.putInt(SimBodyInsulinPumpDriver.KEY_INSULIN_CONCENTRATION_UVALUE, concentration.uValue)
                }
            }
        },
        onClose = onClose,
        modifier = modifier,
        typeCardSubtitle = stringResource(R.string.sim_body_config_insulin_type_subtitle),
        concentrationCardSubtitle = stringResource(R.string.sim_body_config_concentration_subtitle),
    )
}

private class PreviewSimBodyPump(
    override val insulinTypeId: StateFlow<String?> = MutableStateFlow(null),
    override val insulinConcentration: StateFlow<InsulinConcentration> = MutableStateFlow(InsulinConcentration.U100),
) : InsulinPump {
    override val insulinPumpId: String = "preview-simbody"
    override val insulinPumpDisplayName: UiText = UiText.DynamicString("SimBody Virtuelle Insulinpumpe")
    override val lifecycleState: StateFlow<PumpLifecycleState> = MutableStateFlow(PumpLifecycleState.Durable())
    override val hardwareInformation: StateFlow<PumpHardwareInformation?> = MutableStateFlow(null)
    override val pumpCapabilities: StateFlow<PumpCapabilities> = MutableStateFlow(
        PumpCapabilities(
            minBasalRate = InsulinAmount(0.0),
            supportsZeroBasal = true,
            minBasalIncrement = InsulinAmount(0.01),
            minBolusAmount = InsulinAmount(0.01),
            minBolusIncrement = InsulinAmount(0.01),
            maxBolusSize = InsulinAmount(40.0)
        )
    )
    override val isConnected: StateFlow<Boolean> = MutableStateFlow(true)
    override val isBlocked: StateFlow<Boolean> = MutableStateFlow(false)
    override val pumpStatus: StateFlow<InsulinPumpStatus> = MutableStateFlow(
        object : InsulinPumpStatus {
            override val pumpSuspended: Boolean = false
            override val batteryRemainingPercent: Int = 100
            override val reservoirRemainingUnits: InsulinAmount = InsulinAmount(300.0)
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

@Preview(name = "SimBody Config - Light", showBackground = true)
@Preview(name = "SimBody Config - Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SimBodyConfigurationScreenPreview() {
    AppPreview {
        SimBodyConfigurationScreen(
            pump = PreviewSimBodyPump(),
            preferences = null,
            onClose = {}
        )
    }
}