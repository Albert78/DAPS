package de.dh.pump.omnipod.dash.ui

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
import de.dh.daps.common.domain.model.data.Timestamp
import de.dh.daps.common.domain.pump.BasalStatus
import de.dh.daps.common.domain.pump.BolusEvent
import de.dh.daps.common.domain.pump.BolusStatus
import de.dh.daps.common.domain.pump.InsulinPump
import de.dh.daps.common.domain.pump.InsulinPumpStatus
import de.dh.daps.common.domain.pump.PatchPhase
import de.dh.daps.common.domain.pump.PumpAlerts
import de.dh.daps.common.domain.pump.PumpCapabilities
import de.dh.daps.common.domain.pump.PumpHardwareInformation
import de.dh.daps.common.domain.pump.PumpLifecycleState
import de.dh.daps.common.domain.model.therapy.InsulinAmount
import de.dh.daps.common.domain.model.therapy.InsulinConcentration
import de.dh.daps.common.domain.model.therapy.InsulinHistory
import de.dh.daps.common.domain.model.therapy.InsulinProfile
import de.dh.daps.common.domain.model.therapy.getDefaultInsulinTypes
import de.dh.daps.common.ui.UiText
import de.dh.daps.ui.common.composables.PumpConfigurationScreen
import de.dh.daps.ui.common.theme.AppPreview
import de.dh.pump.omnipod.dash.OmnipodDashInsulinPumpDriver
import de.dh.pump.omnipod.dash.R
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

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

    PumpConfigurationScreen(
        title = stringResource(R.string.omnipod_dash_config_screen_title),
        selectedTypeId = currentInsulinTypeId,
        availableTypes = availableInsulinTypes,
        onSelectType = { insulinType ->
            driver?.let { d ->
                scope.launch {
                    val stateManager = d.getPodStateManager()
                    stateManager.setInsulinTypeId(insulinType.id)
                    stateManager.saveToPreferences()
                }
            }
        },
        selectedConcentration = currentConcentration,
        onSelectConcentration = { concentration ->
            driver?.let { d ->
                scope.launch {
                    val stateManager = d.getPodStateManager()
                    stateManager.setInsulinConcentration(concentration)
                    stateManager.saveToPreferences()
                }
            }
        },
        onClose = onClose,
        modifier = modifier,
        typeCardSubtitle = stringResource(R.string.omnipod_dash_config_insulin_type_subtitle),
        concentrationCardSubtitle = stringResource(R.string.omnipod_dash_config_concentration_subtitle),
    )
}

private class PreviewOmnipodPump(
    override val insulinTypeId: StateFlow<String?> = MutableStateFlow(null),
    override val insulinConcentration: StateFlow<InsulinConcentration> = MutableStateFlow(InsulinConcentration.U100),
) : InsulinPump {
    override val insulinPumpId: String = "preview-omnipod"
    override val insulinPumpDisplayName: UiText = UiText.DynamicString("Omnipod Dash")
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
            override val lifecycleState: PumpLifecycleState = PumpLifecycleState.Patch(
                phase = PatchPhase.ACTIVE,
                activationTime = Timestamp.now(),
                activeUntil = Timestamp.now().plusHours(72),
                graceUntil = Timestamp.now().plusHours(80)
            )
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