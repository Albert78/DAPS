package de.dh.pump.danai.ui

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
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
import de.dh.daps.ui.common.DefaultSteppingStrategy
import de.dh.daps.ui.common.composables.EditableValueStepper
import de.dh.daps.ui.common.composables.FramedCard
import de.dh.daps.ui.common.composables.PumpConfigurationScreen
import de.dh.daps.ui.common.theme.AppPreview
import de.dh.pump.danai.R
import de.dh.pump.danai.core.DanaIPumpAdapter
import de.dh.pump.danai.core.DanaPumpPreferences
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

@Composable
fun DanaIConfigurationScreen(
    pump: InsulinPump,
    preferences: PluginPreferences?,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val danaPrefs = remember(preferences) { preferences?.let { DanaPumpPreferences(it) } }

    val availableInsulinTypes = remember(context) { getDefaultInsulinTypes(context) }
    val currentInsulinTypeId by pump.insulinTypeId.collectAsState()
    val currentConcentration by pump.insulinConcentration.collectAsState()

    val currentCatheterWearDurationDays by remember(danaPrefs) {
        danaPrefs?.getCatheterWearDurationDaysFlow() ?: flowOf(DanaPumpPreferences.DEFAULT_CATHETER_WEAR_DURATION_DAYS)
    }.collectAsState(initial = DanaPumpPreferences.DEFAULT_CATHETER_WEAR_DURATION_DAYS)

    PumpConfigurationScreen(
        title = stringResource(R.string.danai_config_screen_title),
        selectedTypeId = currentInsulinTypeId,
        availableTypes = availableInsulinTypes,
        onSelectType = { insulinType ->
            danaPrefs?.let { prefs ->
                scope.launch {
                    prefs.saveInsulinTypeId(insulinType.id)
                }
            }
            if (pump is DanaIPumpAdapter) {
                pump.pump.setInsulinTypeId(insulinType.id)
            }
        },
        selectedConcentration = currentConcentration,
        onSelectConcentration = { concentration ->
            danaPrefs?.let { prefs ->
                scope.launch {
                    prefs.saveInsulinConcentration(concentration)
                }
            }
            if (pump is DanaIPumpAdapter) {
                pump.pump.setInsulinConcentration(concentration)
            }
        },
        onClose = onClose,
        modifier = modifier,
        typeCardSubtitle = stringResource(R.string.danai_config_insulin_type_subtitle),
        concentrationCardSubtitle = stringResource(R.string.danai_config_concentration_subtitle),
        additionalContent = {
            CatheterWearDurationCard(
                selectedDays = currentCatheterWearDurationDays,
                onSelectDays = { days ->
                    danaPrefs?.let { prefs ->
                        scope.launch {
                            prefs.saveCatheterWearDurationDays(days)
                        }
                    }
                }
            )
        }
    )
}

/**
 * Card displaying catheter wear duration selection stepper (2-14 days).
 */
@Composable
fun CatheterWearDurationCard(
    selectedDays: Int,
    onSelectDays: (Int) -> Unit,
    modifier: Modifier = Modifier,
    minDays: Int = 1,
    maxDays: Int = 14,
    title: String = stringResource(R.string.danai_config_catheter_title),
    subtitle: String = stringResource(R.string.danai_config_catheter_subtitle),
) {
    FramedCard(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(4.dp))

            EditableValueStepper(
                currentValue = selectedDays.toDouble(),
                onValueChange = { onSelectDays(it.toInt().coerceIn(minDays, maxDays)) },
                minValue = minDays.toDouble(),
                maxValue = maxDays.toDouble(),
                steppingStrategy = DefaultSteppingStrategy(step = 1.0),
                suffix = pluralStringResource(R.plurals.danai_config_catheter_days_suffix, selectedDays),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

private class PreviewDanaIPump(
    override val insulinTypeId: StateFlow<String?> = MutableStateFlow(null),
    override val insulinConcentration: StateFlow<InsulinConcentration> = MutableStateFlow(InsulinConcentration.U100),
) : InsulinPump {
    override val insulinPumpId: String = "preview-dana-i"
    override val insulinPumpDisplayName: UiText = UiText.DynamicString("Sooil Dana-i")
    override val hardwareInformation: StateFlow<PumpHardwareInformation?> = MutableStateFlow(null)
    override val pumpCapabilities: StateFlow<PumpCapabilities> = MutableStateFlow(
        PumpCapabilities(
            minBasalRate = InsulinAmount(0.04),
            supportsZeroBasal = true,
            minBasalIncrement = InsulinAmount(0.01),
            minBolusAmount = InsulinAmount(0.05),
            minBolusIncrement = InsulinAmount(0.05),
            maxBolusSize = InsulinAmount(25.0)
        )
    )
    override val isConnected: StateFlow<Boolean> = MutableStateFlow(true)
    override val isBlocked: StateFlow<Boolean> = MutableStateFlow(false)
    override val pumpStatus: StateFlow<InsulinPumpStatus> = MutableStateFlow(
        object : InsulinPumpStatus {
            override val pumpSuspended: Boolean = false
            override val batteryRemainingPercent: Int = 90
            override val reservoirRemainingUnits: InsulinAmount = InsulinAmount(150.0)
            override val lastSyncTimestamp: Timestamp = Timestamp.now()
            override val lifecycleState: PumpLifecycleState = PumpLifecycleState.Durable()
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

@Preview(name = "Dana-i Config - Light", showBackground = true)
@Preview(name = "Dana-i Config - Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun DanaIConfigurationScreenPreview() {
    AppPreview {
        DanaIConfigurationScreen(
            pump = PreviewDanaIPump(),
            preferences = null,
            onClose = {}
        )
    }
}