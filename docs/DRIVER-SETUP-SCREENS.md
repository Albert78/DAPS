# Documentation: Driver Setup Screens Architecture & Navigation

This document describes the design, architecture, and navigation model for driver setup screens (such as insulin pump and glucose source driver setups) in DAPS.

---

## 💡 Overview & Core Motivation

Hardware drivers in DAPS (e.g., Omnipod Dash, Dana-i, CGM sensors) manage their own lifecycle states, setup flows, and state persistence via `PluginPreferences`. Drivers expose a Compose screen (`SetupScreen`) that adapts dynamically based on the current driver state (`DeviceLifecycleState`).

A major architectural requirement is handling **nested navigation** cleanly:
1. **Host Navigation (`PumpSetupScreen` / `GlucoseSourceSetupScreen`)**: Manages the top-level driver selection list and host-screen exit/pause functionality.
2. **Driver Workflow (`Driver.SetupScreen`)**: Manages driver-internal steps, BLE discovery, pod priming/activation, and explicit driver reset/unbinding.

---

## ⚙️ The 3 Navigation & Action Levels

To ensure predictable UX and prevent accidental data loss or state corruption, navigation actions are strictly categorized into three distinct levels:

```
+-----------------------------------------------------------------------------------+
|  [App BackStack: Dashboard / Setup Wizard / Calling Screen]                        |
+-----------------------------------------------------------------------------------+
                                         ^
                                         |  Level 1: Pause / Exit Host Screen
                                         v
+-----------------------------------------------------------------------------------+
|  PumpSetupScreen (Host Container)                                                 |
|                                                                                   |
|  TopBar Navigation ('X' / ArrowBack) -> EXITS Host Screen to App BackStack        |
|  (Preserves driver state in PluginPreferences; pauses setup transparently)        |
|                                                                                   |
|  +-----------------------------------------------------------------------------+  |
|  | State A: `selectedDriver == null`                                           |  |
|  | -> Displays Driver Selection List                                           |  |
|  +-----------------------------------------------------------------------------+  |
|  | State B: `selectedDriver != null` (Renders `Driver.SetupScreen`)            |  |
|  |                                                                             |  |
|  | * Level 3: Internal Step Back -> Driver manages wizard steps (e.g. 2 -> 1) |  |
|  |                                                                             |  |
|  | * Level 2: Driver Reset / Unbind / Cancel Setup                            |  |
|  |   -> Driver clears data & fires `onDriverReset()`                           |  |
|  |   -> Host sets `selectedDriver = null` -> Returns to Driver Selection List  |  |
|  +-----------------------------------------------------------------------------+  |
+-----------------------------------------------------------------------------------+
```

### Level 1: Temporary Exit / Pausing ("Outward Navigation")
* **Trigger**: TopBar **'X'** (Close) or **`ArrowBack`** on `PumpSetupScreen`, or system Back gesture.
* **Behavior**: Navigates back up the app's BackStack to the caller (e.g., Main Dashboard or Setup Wizard).
* **Driver State Impact**: **None**. The driver retains its current state (`NoDevice`, `SetupInProgress`, or `DeviceBound`) and persists it in `PluginPreferences`.
* **Resuming**: When the user re-opens `PumpSetupScreen` later, the ViewModel restores the active/persisted driver and resumes execution **at the exact same step**.

---

### Level 2: Driver Reset & Lifecycle Termination ("Return to Driver Selection List")
* **Trigger**: Explicit actions **within the driver's UI**:
  * In `NoDevice`: *"Change / Cancel Driver Selection"* button.
  * In `SetupInProgress`: *"Cancel Setup"* button (with confirmation dialog).
  * In `DeviceBound`: *"Unbind Device"* button (with confirmation dialog).
* **Behavior**: The driver clears its internal persisted data, cancels any active bonding, resets its state to `NoDevice`/`Unbound`, and invokes the `onDriverReset()` callback.
* **Host Response**: `PumpSetupScreen` receives `onDriverReset()`, sets `selectedDriver = null`, and displays the **Driver Selection List**.

---

### Level 3: Internal Step Backward (Driver Wizard)
* **Trigger**: A "Back" button within the driver's setup wizard UI, or the system Back gesture when handled locally by the driver.
* **Behavior**: Navigates to the previous step within the driver's internal workflow (e.g., Step 2 $\rightarrow$ Step 1).
* **System Back Button Interception (`BackHandler`) Rule**:
  * **When at Step 2 or higher (and step-back is valid/safe)**: The driver **intercepts** the system Back gesture by declaring a local `BackHandler { goToPreviousStep() }`. In Jetpack Compose, inner `BackHandler` instances take precedence over parent handlers, ensuring the back gesture steps backward within the driver wizard.
  * **When at Step 1 (or if step-back is unsafe/prohibited)**: The driver **does NOT** intercept step-back. The system Back gesture falls through to the host container's Level 1 `BackHandler` (pausing setup and exiting to the app caller). Alternatively, if leaving mid-setup requires confirmation, the driver may intercept Back to display an explicit confirmation dialog (*"Pause setup and exit?"* vs. *"Cancel setup"*).

---

## 🎨 UI & TopBar Behavior Matrix

| Driver Lifecycle State | TopBar Icon (Host) | Back Gesture & TopBar Action | In-Screen Content Controls | In-Screen Action Effect |
| :--- | :--- | :--- | :--- | :--- |
| **Driver Selection List** (`selectedDriver == null`) | `ArrowBack` | Exits `PumpSetupScreen` back to App Caller | Driver Cards | Selects driver (`selectedDriver = driver`) |
| **`NoDevice`** (Device Scan / Selection) | `ArrowBack` | Deselects driver $\rightarrow$ Returns to Driver Selection List | *"Change Driver"* / *"Cancel"* | Resets driver $\rightarrow$ Invokes `onDriverReset()` $\rightarrow$ Returns to Driver Selection List |
| **`SetupInProgress`** (e.g., Pod Activation) | `Close` ('X') | Exits host screen back to App Caller (Pauses setup) | *"Cancel Setup"* (Red Outlined) | Shows confirmation dialog $\rightarrow$ Resets driver $\rightarrow$ Invokes `onDriverReset()` $\rightarrow$ Returns to Driver Selection List |
| **`DeviceBound`** (Active Device) | `Close` ('X') | Exits host screen back to App Caller | *"Unbind Device"* (Red Outlined) | Shows confirmation dialog $\rightarrow$ Unbinds device $\rightarrow$ Invokes `onDriverReset()` $\rightarrow$ Returns to Driver Selection List |

---

## 📐 Architecture & Interface Contracts

### 1. Driver SetupScreen Contract

Drivers expose `SetupScreen` accepting an explicit `onDriverReset` callback parameter:

```kotlin
interface InsulinPumpDriver {
    val driverId: String
    val displayName: UiText
    val lifecycleState: StateFlow<DeviceLifecycleState>

    /**
     * Renders the driver's setup UI for its current [DeviceLifecycleState].
     *
     * @param onDriverReset Callback invoked when the driver is explicitly reset,
     *                      cancelled, or unbound by the user. The host container
     *                      will restore the driver selection list upon receiving this.
     */
    @Composable
    fun SetupScreen(
        onDriverReset: () -> Unit
    )
}
```

---

### 2. Host Container (`PumpSetupScreen`) Implementation Pattern

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PumpSetupContent(
    uiState: PumpSetupUiState,
    onNavigateUp: () -> Unit,                     // Exits host screen to App Caller
    onSelectDriver: (InsulinPumpDriver?) -> Unit, // Updates driver selection (null = show list)
    onClearError: () -> Unit
) {
    val selectedDriver = uiState.selectedDriver
    val titleText = selectedDriver?.displayName?.asString()
        ?: stringResource(id = R.string.pump_setup_screen_title)

    // System back gesture always pauses/exits host screen to caller
    BackHandler(enabled = true) {
        onNavigateUp()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = screenTitle(titleText),
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        if (selectedDriver == null) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(id = CommonR.string.cd_navigate_up)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = stringResource(id = CommonR.string.cd_close)
                            )
                        }
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
            if (selectedDriver != null) {
                ScopedDriverSetupContainer(driverId = selectedDriver.driverId) {
                    selectedDriver.SetupScreen(
                        onDriverReset = {
                            // Driver has been reset/unbound -> Return to selection list
                            onSelectDriver(null)
                        }
                    )
                }
            } else {
                PumpDriverSelectionContent(
                    uiState = uiState,
                    onSelectDriver = onSelectDriver
                )
            }
        }
    }
}
```

---

### 3. Driver Implementation Example (`OmnipodDashSetupScreen`)

```kotlin
@Composable
fun OmnipodDashSetupScreen(
    driver: OmnipodDashInsulinPumpDriver,
    onDriverReset: () -> Unit
) {
    val lifecycleState by driver.lifecycleState.collectAsStateWithLifecycle()

    when (lifecycleState) {
        is DeviceLifecycleState.DeviceBound -> {
            OmnipodDashBoundContent(
                onUnbind = {
                    viewModel.unbind(driver)
                    onDriverReset() // Return host to Driver Selection List
                }
            )
        }
        else -> {
            OmnipodDashSetupContent(
                onCancelSetup = {
                    driver.cancelSetup() // Wipe pod activation state & preferences
                    onDriverReset()      // Return host to Driver Selection List
                }
            )
        }
    }
}
```

---

## 🔑 Key Benefits Summary

1. **Clear Mental Model**: Closing the screen via TopBar ('X') always pauses and exits to the app caller. Resetting or unbinding the driver happens explicitly in the driver UI and returns to driver selection.
2. **Safe & Resilient Setup**: Mid-activation states (e.g. Omnipod filling/priming) can be paused anytime without losing progress or corrupting hardware state.
3. **Decoupled Architecture**: Host screens (`PumpSetupScreen`, `GlucoseSourceSetupScreen`) do not need internal knowledge of driver-specific setup steps.