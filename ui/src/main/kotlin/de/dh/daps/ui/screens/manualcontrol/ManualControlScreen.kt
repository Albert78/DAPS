package de.dh.daps.ui.screens.manualcontrol

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.dh.daps.common.domain.model.data.BgReading
import de.dh.daps.common.domain.model.data.BgSampleKind
import de.dh.daps.common.domain.model.data.BgValue
import de.dh.daps.common.domain.model.data.Minutes
import de.dh.daps.common.domain.model.data.Timestamp
import de.dh.daps.common.domain.pump.BasalStatus
import de.dh.daps.common.domain.model.therapy.DeferredBolus
import de.dh.daps.common.domain.model.therapy.InsulinAmount
import de.dh.daps.common.domain.model.therapy.InsulinApplication
import de.dh.daps.common.domain.model.therapy.InsulinOrigin
import de.dh.daps.common.domain.model.therapy.InsulinType
import de.dh.daps.common.domain.model.therapy.MealEntry
import de.dh.daps.common.domain.model.therapy.getDefaultSlowMealType
import de.dh.daps.common.domain.model.therapy.getDefaultStandardMealType
import de.dh.daps.common.navigation.ManualControlInitialDialog
import de.dh.daps.core.orchestration.ActiveRecommendations
import de.dh.daps.core.orchestration.ApsRecommendation
import de.dh.daps.ui.R
import de.dh.daps.ui.common.composables.PrimaryButton
import de.dh.daps.ui.common.composables.contentScrollIndicator
import de.dh.daps.ui.common.composables.screenTitle
import de.dh.daps.ui.common.theme.AppPreview
import java.util.Calendar
import de.dh.daps.common.R as CommonR

@Composable
fun ManualControlScreen(
    viewModel: ManualControlViewModel,
    initialDialog: ManualControlInitialDialog = ManualControlInitialDialog.NONE,
    onNavigateUp: () -> Unit,
    onNavigateToMealCorrectionBolus: (Double?) -> Unit = {},
    onEditMeal: (Long) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    ManualControlContent(
        uiState = uiState,
        initialDialog = initialDialog,
        onNavigateUp = onNavigateUp,
        onNavigateToMealCorrectionBolus = onNavigateToMealCorrectionBolus,
        onEditMeal = onEditMeal,
        onDeliverBolus = { amount, handledDeferredBoluses, correctionPart, basalPart, recommendationToDismiss ->
            viewModel.deliverBolus(
                amount = amount,
                handledDeferredBoluses = handledDeferredBoluses,
                correctionPart = correctionPart,
                basalPart = basalPart,
                recommendationToDismiss = recommendationToDismiss
            )
        },
        onSetTempBasal = { durationHours, percent, recommendationToDismiss ->
            viewModel.setTempBasal(
                durationHours = durationHours,
                percent = percent,
                recommendationToDismiss = recommendationToDismiss
            )
        },
        onCancelBolus = { viewModel.cancelBolus() },
        onCancelTempBasal = { viewModel.cancelTempBasal() },
        onDismissLockError = { viewModel.dismissLockError() },
        onDismissRecommendation = { viewModel.dismissRecommendation(it) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualControlContent(
    uiState: ManualControlUiState,
    initialDialog: ManualControlInitialDialog = ManualControlInitialDialog.NONE,
    onNavigateUp: () -> Unit = {},
    onNavigateToMealCorrectionBolus: (Double?) -> Unit = {},
    onEditMeal: (Long) -> Unit = {},
    onDeliverBolus: (
        amount: InsulinAmount,
        handledDeferredBoluses: List<DeferredBolus>?,
        correctionPart: InsulinAmount,
        basalPart: InsulinAmount,
        recommendationToDismiss: ApsRecommendation.Bolus?
    ) -> Unit = { _, _, _, _, _ -> },
    onSetTempBasal: (
        durationHours: Int,
        percent: Int,
        recommendationToDismiss: ApsRecommendation.TempBasal?
    ) -> Unit = { _, _, _ -> },
    onCancelBolus: () -> Unit = {},
    onCancelTempBasal: () -> Unit = {},
    onDismissLockError: () -> Unit = {},
    onDismissRecommendation: (ApsRecommendation) -> Unit = {}
) {
    var activeDialog by remember { mutableStateOf<ManualControlDialog?>(null) }
    var handledInitialDialog by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(initialDialog, uiState.pump.isConfigured) {
        if (!handledInitialDialog && uiState.pump.isConfigured) {
            when (initialDialog) {
                ManualControlInitialDialog.BOLUS -> {
                    val bolusRec = uiState.activeRecommendations.bolus
                    activeDialog = ManualControlDialog.Bolus(
                        initialAmount = bolusRec?.amount ?: InsulinAmount.ZERO,
                        includedDeferredBoluses = bolusRec?.includedDeferredBoluses,
                        correctionPart = bolusRec?.correctionPart ?: InsulinAmount.ZERO,
                        basalPart = bolusRec?.basalPart ?: InsulinAmount.ZERO,
                        recommendationToDismiss = bolusRec
                    )
                    handledInitialDialog = true
                }
                ManualControlInitialDialog.TEMP_BASAL -> {
                    val tempBasalRec = uiState.activeRecommendations.tempBasal
                    val currentPercent = uiState.pump.basalStatus?.tempBasalPercent ?: 100
                    activeDialog = ManualControlDialog.TempBasal(
                        initialPercent = tempBasalRec?.percent ?: currentPercent,
                        initialDurationHours = tempBasalRec?.durationInHours ?: 1,
                        recommendationToDismiss = tempBasalRec
                    )
                    handledInitialDialog = true
                }
                ManualControlInitialDialog.NONE -> {}
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = screenTitle(stringResource(id = R.string.manual_control_screen_title)),
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(id = CommonR.string.cd_navigate_up)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        val scrollState = rememberScrollState()
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .contentScrollIndicator(scrollState)
                    .verticalScroll(scrollState),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Header: Context Info Card
                ManualControlContextInfo(contextInfo = uiState.contextInfo)

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Meals Overview
                    ManualControlMealsSection(
                        lastPastMeal = uiState.contextInfo.lastPastMeal,
                        nextPlannedMeal = uiState.contextInfo.nextPlannedMeal,
                        hasNextPlannedMealReminder = uiState.contextInfo.hasNextPlannedMealReminder,
                        onEditMeal = onEditMeal
                    )

                    // Recommendations
                    ManualControlRecommendationsSection(
                        recommendations = uiState.activeRecommendations,
                        onOpenBolusDialog = { activeDialog = it },
                        onOpenTempBasalDialog = { activeDialog = it },
                        onNavigateToMealCorrectionBolus = onNavigateToMealCorrectionBolus,
                        onDismissRecommendation = onDismissRecommendation
                    )

                    // Manual Pump Control
                    ManualControlPumpControlsSection(
                        pump = uiState.pump,
                        lastBolus = uiState.contextInfo.lastBolus,
                        onOpenBolusDialog = { activeDialog = it },
                        onCancelBolus = onCancelBolus,
                        onOpenTempBasalDialog = { activeDialog = it },
                        onCancelTempBasal = onCancelTempBasal
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }

    when (val dialog = activeDialog) {
        is ManualControlDialog.Bolus -> {
            DeliverBolusDialog(
                dialogData = dialog,
                minBolusAmount = uiState.pump.minBolusAmount,
                maxBolusSize = uiState.pump.maxBolusSize,
                onDismiss = { activeDialog = null },
                onConfirm = { amount, handledDeferredBoluses, correctionPart, basalPart, recommendationToDismiss ->
                    onDeliverBolus(
                        amount,
                        handledDeferredBoluses,
                        correctionPart,
                        basalPart,
                        recommendationToDismiss
                    )
                    activeDialog = null
                }
            )
        }
        is ManualControlDialog.TempBasal -> {
            SetTempBasalDialog(
                dialogData = dialog,
                onDismiss = { activeDialog = null },
                onConfirm = { durationHours, percent, recommendationToDismiss ->
                    onSetTempBasal(
                        durationHours,
                        percent,
                        recommendationToDismiss
                    )
                    activeDialog = null
                }
            )
        }
        null -> {}
    }

    if (uiState.showLockError) {
        AlertDialog(
            onDismissRequest = onDismissLockError,
            title = { Text(stringResource(id = R.string.core_issue_title)) },
            text = {
                Text(
                    text = stringResource(
                        id = R.string.treatment_lock_error_message,
                        uiState.lockErrorOwner ?: stringResource(id = R.string.manual_control_screen_title)
                    )
                )
            },
            confirmButton = {
                PrimaryButton(onClick = onDismissLockError) {
                    Text(stringResource(id = android.R.string.ok))
                }
            }
        )
    }
}

@Preview(showBackground = true, name = "Light Mode", heightDp = 1380)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, name = "Dark Mode", heightDp = 1380)
@Composable
fun ManualControlScreenPreview() {
    AppPreview {
        ManualControlContent(
            uiState = ManualControlUiState(
                contextInfo = ManualControlContextInfoUiModel(
                    lastBgReading = BgReading(
                        value = BgValue.fromMgDl(80),
                        sampleKind = BgSampleKind.Value,
                        timestamp = Timestamp.now()
                    ),
                    iob = InsulinAmount(1.2),
                    cob = 25.0,
                    lastPastMeal = MealEntry(
                        id = 1L,
                        timestamp = Timestamp.now().minusHours(2),
                        carbGrams = 45.0,
                        mealType = getDefaultStandardMealType(LocalContext.current),
                        administeredInsulinAmount = InsulinAmount(3.5)
                    ),
                    nextPlannedMeal = MealEntry(
                        id = 2L,
                        timestamp = Timestamp.now().plusHours(3),
                        carbGrams = 60.0,
                        mealType = getDefaultSlowMealType(LocalContext.current),
                        description = "Pizza"
                    ),
                    hasNextPlannedMealReminder = true,
                    lastBolus = InsulinApplication(
                        timestamp = Timestamp.now().minusHours(1),
                        amount = InsulinAmount(2.5),
                        insulinType = InsulinType(name = "NovoRapid", activeSubstance = "Insulin aspart", peak = Minutes(75), dia = Minutes(300)),
                        origin = InsulinOrigin.Pump
                    )
                ),
                activeRecommendations = ActiveRecommendations(
                    carbs = ApsRecommendation.Carbs(amountInGram = 20),
                    bolus = ApsRecommendation.Bolus(
                        amount = InsulinAmount(1.5),
                        correctionPart = InsulinAmount(1.0),
                        basalPart = InsulinAmount(0.0)
                    ),
                    tempBasal = ApsRecommendation.TempBasal(durationInHours = 2, percent = 80)
                ),
                pump = ManualControlPumpUiModel(
                    isConfigured = true,
                    basalStatus = BasalStatus(
                        isSuspended = false,
                        activeRate = InsulinAmount(0.5),
                        isTempBasal = false,
                    )
                )
            )
        )
    }
}

@Preview(showBackground = true, name = "Carb Recommendation", heightDp = 1000)
@Preview(showBackground = true, name = "Carb Recommendation Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, heightDp = 1000)
@Composable
fun ManualControlCarbRecommendationPreview() {
    val context = LocalContext.current
    val time1320 = remember {
        Timestamp(
            Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 13)
                set(Calendar.MINUTE, 20)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
        )
    }
    val time0845 = remember {
        Timestamp(
            Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 8)
                set(Calendar.MINUTE, 45)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
        )
    }

    AppPreview {
        ManualControlContent(
            uiState = ManualControlUiState(
                contextInfo = ManualControlContextInfoUiModel(
                    lastBgReading = BgReading(
                        value = BgValue.fromMgDl(75),
                        sampleKind = BgSampleKind.Value,
                        timestamp = time1320
                    ),
                    iob = InsulinAmount(0.2),
                    cob = 0.0,
                    lastPastMeal = MealEntry(
                        id = 1L,
                        timestamp = time0845,
                        carbGrams = 45.0,
                        mealType = getDefaultStandardMealType(context),
                        administeredInsulinAmount = InsulinAmount(3.5)
                    ),
                    nextPlannedMeal = null,
                    hasNextPlannedMealReminder = false,
                    lastBolus = InsulinApplication(
                        timestamp = time0845,
                        amount = InsulinAmount(3.5),
                        insulinType = InsulinType(name = "NovoRapid", activeSubstance = "Insulin aspart", peak = Minutes(75), dia = Minutes(300)),
                        origin = InsulinOrigin.Pump
                    )
                ),
                activeRecommendations = ActiveRecommendations(
                    carbs = ApsRecommendation.Carbs(amountInGram = 10)
                ),
                pump = ManualControlPumpUiModel(
                    isConfigured = true,
                    basalStatus = BasalStatus(
                        isSuspended = false,
                        activeRate = InsulinAmount(0.5),
                        isTempBasal = false,
                    )
                )
            )
        )
    }
}