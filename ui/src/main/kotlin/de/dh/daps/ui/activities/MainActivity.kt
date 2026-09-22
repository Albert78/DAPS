package de.dh.daps.ui.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import de.dh.daps.common.navigation.BolusHistoryRoute
import de.dh.daps.common.navigation.DashboardRoute
import de.dh.daps.common.navigation.FeatureNavGraph
import de.dh.daps.common.navigation.FoodDatabaseRoute
import de.dh.daps.common.navigation.HistoricalMealRoute
import de.dh.daps.common.navigation.ManualControlInitialDialog
import de.dh.daps.common.navigation.ManualControlRoute
import de.dh.daps.common.navigation.MasterDataRoute
import de.dh.daps.common.navigation.MealCorrectionBolusRoute
import de.dh.daps.common.navigation.MealsRoute
import de.dh.daps.common.navigation.NavigationViewModel
import de.dh.daps.common.navigation.SystemControlRoute
import de.dh.daps.common.navigation.combineEntryProviders
import de.dh.daps.core.SystemRegistry
import de.dh.daps.core.system.RegistryProvider
import de.dh.daps.ui.GlobalViewModel
import de.dh.daps.ui.R
import de.dh.daps.ui.common.composables.EdgeToEdgeHandler
import de.dh.daps.ui.common.icons.Icon_ManualControlMode
import de.dh.daps.ui.common.icons.Icon_Bolus
import de.dh.daps.ui.common.icons.Icon_Meal
import de.dh.daps.ui.common.icons.Icon_Food_Database
import de.dh.daps.ui.common.icons.Icon_Master_Data
import de.dh.daps.ui.common.icons.Icon_System_Control
import de.dh.daps.ui.common.theme.AppPreview
import de.dh.daps.ui.common.theme.AppTheme
import de.dh.daps.ui.common.theme.rememberUseDarkTheme
import de.dh.daps.ui.navigation.MainFeatureNavGraph
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

class MainActivity : ComponentActivity() {
    private lateinit var navViewModel: NavigationViewModel
    private lateinit var globalViewModel: GlobalViewModel
    private val intentHandler = IntentHandler()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val registry = (application as RegistryProvider).registry

        // Start the background service
        startForegroundService(Intent(this, registry.apsServiceClass))

        navViewModel = ViewModelProvider(
            this,
            NavigationViewModel.Companion.NavigationViewModelFactory(listOf(DashboardRoute))
        )[NavigationViewModel::class.java]

        globalViewModel = ViewModelProvider(
            this,
            GlobalViewModel.Companion.Factory(registry)
        )[GlobalViewModel::class.java]

        intentHandler.handleIntent(intent, navViewModel)

        setContent {
            val useDarkTheme = rememberUseDarkTheme(registry.appPreferencesRepository)
            val glucoseUnit by globalViewModel.glucoseUnit.collectAsState()

            EdgeToEdgeHandler(useDarkTheme)
            AppTheme(
                darkTheme = useDarkTheme,
                glucoseUnit = glucoseUnit
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainApp(registry)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intentHandler.handleIntent(intent, navViewModel)
    }

    @Composable
    fun MainApp(registry: SystemRegistry) {
        val backStack by navViewModel.backstack.collectAsState()
        val currentRoute = backStack.lastOrNull()

        val extraGraphs = getExtraNavGraphs?.let { it(navViewModel) } ?: emptyList()

        val extraDashboardContent: @Composable () -> Unit = @Composable {
            extraGraphs.forEach { it.DashboardExtension() }
        }

        val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
        val scope = rememberCoroutineScope()
        val density = LocalDensity.current

        val mainGraph = MainFeatureNavGraph(
            activity = this,
            navViewModel = navViewModel,
            registry = registry,
            extraDashboardContent = extraDashboardContent
        )
        val allGraphs = listOf(mainGraph) + extraGraphs

        val combinedProvider = combineEntryProviders(*allGraphs.toTypedArray())

        val isTopLevel = currentRoute in listOf(
            DashboardRoute
        )

        var showHamburger by remember { mutableStateOf(isTopLevel) }
        LaunchedEffect(isTopLevel) {
            if (isTopLevel) {
                delay(400.milliseconds) // Delay to wait for screen transition
                showHamburger = true
            } else {
                showHamburger = false
            }
        }

        val drawerWidth = 320.dp
        val safeInsets = WindowInsets.safeDrawing.asPaddingValues(density)
        val verticalPadding = safeInsets.calculateBottomPadding()
        val statusBarHeight = safeInsets.calculateTopPadding()

        Box(modifier = Modifier.fillMaxSize()) {
            ModalNavigationDrawer(
                drawerState = drawerState,
                gesturesEnabled = isTopLevel,
                drawerContent = {
                    DrawerContent(
                        currentRoute = currentRoute,
                        onRouteSelected = { route ->
                            scope.launch { drawerState.close() }
                            navViewModel.push(route)
                        },
                        drawerWidth = drawerWidth,
                        statusBarHeight = statusBarHeight,
                        verticalPadding = verticalPadding
                    )
                }
            ) {
                NavDisplay(
                    backStack = backStack,
                    onBack = { navViewModel.pop() },
                    entryDecorators = listOf(
                        rememberSaveableStateHolderNavEntryDecorator(),
                        rememberViewModelStoreNavEntryDecorator()
                    ),
                    entryProvider = combinedProvider
                )
            }

            if (showHamburger) {
                val rotation by animateFloatAsState(
                    targetValue = if (drawerState.targetValue == DrawerValue.Open) 90f else 0f,
                    label = "HamburgerRotation"
                )

                IconButton(
                    onClick = {
                        scope.launch {
                            if (drawerState.isOpen) drawerState.close() else drawerState.open()
                        }
                    },
                    modifier = Modifier
                        .safeDrawingPadding()
                        .padding(start = 8.dp, top = 8.dp)
                        .graphicsLayer {
                            rotationZ = rotation
                        }
                ) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = stringResource(id = R.string.cd_open_navigation_drawer)
                    )
                }
            }
        }
    }

    class IntentHandler {
        fun parseIntent(intent: Intent?): List<NavKey>? {
            if (intent == null) return null

            val data = intent.data
            if (intent.action == Intent.ACTION_VIEW && data?.scheme == "app" && data.host == "daps.dh.de") {
                val path = data.path
                if (path == "/dashboard") {
                    return listOf(DashboardRoute)
                } else if (path?.startsWith("/meal/") == true) {
                    val mealId = data.lastPathSegment?.toLongOrNull()
                    if (mealId != null) {
                        return listOf(DashboardRoute, HistoricalMealRoute(mealId))
                    }
            } else if (path == "/mealcorrectionbolus") {
                val carbsInG = data.getQueryParameter("carbsInG")?.toDoubleOrNull()
                return listOf(DashboardRoute, MealCorrectionBolusRoute(prefilledCarbsInG = carbsInG))
            } else if (path == "/manualcontrol") {
                val dialogStr = data.getQueryParameter("dialog")
                val initialDialog = when (dialogStr) {
                    "bolus" -> ManualControlInitialDialog.BOLUS
                    "temp_basal" -> ManualControlInitialDialog.TEMP_BASAL
                    else -> ManualControlInitialDialog.NONE
                }
                return listOf(DashboardRoute, ManualControlRoute(initialDialog = initialDialog))
            }
        }

        return null
    }

    fun handleIntent(intent: Intent?, navViewModel: NavigationViewModel) {
        val routes = parseIntent(intent)
        if (routes != null) {
            navViewModel.reset(routes)
        }
    }

    companion object {
        fun createStartDashboardIntent(context: Context): Intent {
            return Intent(context, MainActivity::class.java).apply {
                action = Intent.ACTION_VIEW
                data = "app://daps.dh.de/dashboard".toUri()
            }
        }

        fun createEditMealIntent(context: Context, mealId: Long): Intent {
            return Intent(context, MainActivity::class.java).apply {
                action = Intent.ACTION_VIEW
                data = "app://daps.dh.de/meal/$mealId".toUri()
            }
        }

        fun createMealCorrectionBolusIntent(
            context: Context,
            carbsInG: Double? = null
        ): Intent {
            return Intent(context, MainActivity::class.java).apply {
                action = Intent.ACTION_VIEW
                val uriBuilder = "app://daps.dh.de/mealcorrectionbolus".toUri().buildUpon()
                if (carbsInG != null) {
                    uriBuilder.appendQueryParameter("carbsInG", carbsInG.toString())
                }
                data = uriBuilder.build()
            }
        }

        fun createManualControlIntent(
            context: Context,
            initialDialog: ManualControlInitialDialog = ManualControlInitialDialog.NONE
        ): Intent {
            return Intent(context, MainActivity::class.java).apply {
                action = Intent.ACTION_VIEW
                val uriBuilder = "app://daps.dh.de/manualcontrol".toUri().buildUpon()
                val dialogStr = when (initialDialog) {
                    ManualControlInitialDialog.BOLUS -> "bolus"
                    ManualControlInitialDialog.TEMP_BASAL -> "temp_basal"
                    ManualControlInitialDialog.NONE -> null
                }
                if (dialogStr != null) {
                    uriBuilder.appendQueryParameter("dialog", dialogStr)
                }
                data = uriBuilder.build()
            }
        }
    }
}

    companion object {
        // Hack to transport extra nav graphs from MainApplication into MainActivity. Any better solution is welcome...
        var getExtraNavGraphs: ((navViewModel: NavigationViewModel) -> List<FeatureNavGraph>)? = null

        fun createStartDashboardIntent(context: Context): Intent =
            IntentHandler.createStartDashboardIntent(context)

        fun createEditMealIntent(context: Context, mealId: Long): Intent =
            IntentHandler.createEditMealIntent(context, mealId)

        fun createMealCorrectionBolusIntent(
            context: Context,
            carbsInG: Double? = null
        ): Intent = IntentHandler.createMealCorrectionBolusIntent(context, carbsInG)

        fun createManualControlIntent(
            context: Context,
            initialDialog: ManualControlInitialDialog = ManualControlInitialDialog.NONE
        ): Intent = IntentHandler.createManualControlIntent(context, initialDialog)
    }
}

@Composable
fun DrawerContent(
    currentRoute: NavKey?,
    onRouteSelected: (NavKey) -> Unit,
    drawerWidth: Dp = 320.dp,
    statusBarHeight: Dp = 0.dp,
    verticalPadding: Dp = 0.dp
) {
    ModalDrawerSheet(
        modifier = Modifier.width(drawerWidth),
        drawerContainerColor = Color.Transparent,
        drawerTonalElevation = 0.dp,
        windowInsets = WindowInsets(0.dp)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = statusBarHeight + 2.dp, bottom = verticalPadding),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            shape = MaterialTheme.shapes.large
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .padding(top = 50.dp)
                        .height(100.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_app_logo),
                        contentDescription = null,
                        modifier = Modifier.size(56.dp),
                        tint = Color.Unspecified
                    )
                    Spacer(Modifier.width(16.dp))
                    Text(
                        stringResource(id = R.string.drawer_header_title),
                        style = MaterialTheme.typography.headlineMedium
                    )
                }
                HorizontalDivider()

                DrawerItem(
                    label = stringResource(id = R.string.menu_meals_label),
                    icon = Icon_Meal,
                    selected = currentRoute == MealsRoute,
                    onClick = { onRouteSelected(MealsRoute) }
                )
                DrawerItem(
                    label = stringResource(id = R.string.menu_bolus_history_label),
                    icon = Icon_Bolus,
                    selected = currentRoute == BolusHistoryRoute,
                    onClick = { onRouteSelected(BolusHistoryRoute) }
                )
                DrawerItem(
                    label = stringResource(id = R.string.menu_food_database_label),
                    icon = Icon_Food_Database,
                    selected = currentRoute == FoodDatabaseRoute,
                    onClick = { onRouteSelected(FoodDatabaseRoute) }
                )
                DrawerItem(
                    label = stringResource(id = R.string.manual_control_screen_title),
                    icon = Icon_ManualControlMode,
                    selected = currentRoute is ManualControlRoute,
                    onClick = { onRouteSelected(ManualControlRoute()) }
                )
                DrawerItem(
                    label = stringResource(id = R.string.menu_system_control_label),
                    icon = Icon_System_Control,
                    selected = currentRoute is SystemControlRoute,
                    onClick = { onRouteSelected(SystemControlRoute()) }
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                DrawerItem(
                    label = stringResource(id = R.string.menu_master_data_label),
                    icon = Icon_Master_Data,
                    selected = currentRoute == MasterDataRoute,
                    onClick = { onRouteSelected(MasterDataRoute) }
                )
            }
        }
    }
}

@Composable
private fun DrawerItem(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    NavigationDrawerItem(
        label = { Text(label) },
        icon = { Icon(imageVector = icon, contentDescription = null) },
        selected = selected,
        onClick = onClick,
        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
    )
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, widthDp = 320)
@Composable
fun DrawerPreview() {
    AppPreview {
        DrawerContent(
            currentRoute = DashboardRoute,
            onRouteSelected = {},
            statusBarHeight = 24.dp,
            verticalPadding = 16.dp
        )
    }
}