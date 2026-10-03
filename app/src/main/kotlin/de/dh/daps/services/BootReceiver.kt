package de.dh.daps.services

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import de.dh.daps.MainApplication
import de.dh.daps.core.InitializationState
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * This receiver is called when the device is rebooted.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val pendingResult = goAsync()
            val app = context.applicationContext as? MainApplication ?: run {
                pendingResult.finish()
                return
            }

            app.applicationScope.launch {
                try {
                    withTimeoutOrNull(BOOT_INITIALIZATION_TIMEOUT) {
                        app.registry.initializationState.first { it == InitializationState.READY }
                    }
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }

    companion object {
        private val BOOT_INITIALIZATION_TIMEOUT = 10.seconds

        fun enableBootReceiver(context: Context) {
            val receiver = ComponentName(context, BootReceiver::class.java)
            val pm = context.packageManager

            pm.setComponentEnabledSetting(
                receiver,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP
            )
        }

        fun disableBootReceiver(context: Context) {
            val receiver = ComponentName(context, BootReceiver::class.java)
            val pm = context.packageManager

            pm.setComponentEnabledSetting(
                receiver,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP
            )
        }
    }
}