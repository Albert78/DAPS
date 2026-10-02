package de.dh.daps

import android.app.Activity
import android.app.Application
import android.app.ForegroundServiceStartNotAllowedException
import android.content.Intent
import android.os.Bundle
import androidx.core.content.ContextCompat
import de.dh.daps.core.InitializationState
import de.dh.daps.core.SystemRegistry
import de.dh.daps.core.SystemRegistryImpl
import de.dh.daps.core.system.RegistryProvider
import de.dh.daps.notifications.AndroidNotificationsImpl
import de.dh.daps.pluginmanager.PluginManagerImpl
import de.dh.daps.services.ApsService
import de.dh.daps.services.BootReceiver
import de.dh.daps.ui.activities.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Main application class for DAPS.
 * Responsibility is limited to system entry points and lifecycle management.
 */
class MainApplication : Application(), RegistryProvider {
    lateinit var androidNotifications: AndroidNotificationsImpl
        private set

    override lateinit var registry: SystemRegistry
        private set

    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()
        instance = this

        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityResumed(activity: Activity) {
                if (::registry.isInitialized && registry.initializationState.value == InitializationState.READY) {
                    startApsService()
                    registry.permissionRepository.refreshPermissions()
                }
            }
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
            override fun onActivityStarted(activity: Activity) {}
            override fun onActivityPaused(activity: Activity) {}
            override fun onActivityStopped(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {}
        })

        androidNotifications = AndroidNotificationsImpl(this)

        val pluginManager = PluginManagerImpl(this)

        // Dynamic plugin registration - function depends on chosen flavor
        registerPlugins(pluginManager, this)

        registry = SystemRegistryImpl.create(
            application = this,
            scope = applicationScope,
            pluginManager = pluginManager,
            androidNotifications = androidNotifications,
            onPermissionsChanged = { startApsService() }
        )

        applicationScope.launch {
            registry.initializationState.collect { state ->
                if (state == InitializationState.READY) {
                    startApsService()
                }
            }
        }

        MainActivity.getExtraNavGraphs = ::getExtraNavGraphs

        BootReceiver.enableBootReceiver(this)
    }

    override fun onTerminate() {
        if (::registry.isInitialized) {
            registry.glucoseSourceManager.stop()
            registry.systemOrchestrator.stop()
        }
        applicationScope.cancel()
        super.onTerminate()
    }

    fun startApsService() {
        val intent = Intent(this, ApsService::class.java)
        try {
            ContextCompat.startForegroundService(this, intent)
        } catch (e: ForegroundServiceStartNotAllowedException) {
            // Android 12+ may throw ForegroundServiceStartNotAllowedException
            // if called from background without proper exemptions (like ignoring battery optimizations).
            // TODO: Handle
        } catch (e: IllegalStateException) {
            // TODO: Handle
        }
    }

    companion object {
        lateinit var instance: MainApplication
            private set
    }
}