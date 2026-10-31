package de.dh.daps.ui.screens.driversetup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner

/**
 * Hält den [ViewModelStore] des aktuell im Setup/Betrieb befindlichen Treibers.
 * Überlebt Activity-Konfigurationsänderungen (Rotationen), da diese Instanz
 * am übergeordneten [ViewModelStoreOwner] (z.B. NavEntry) verankert ist.
 */
class DriverViewModelStoreHolder : ViewModel() {
    private var driverViewModelStore: ViewModelStore? = null

    fun getOrCreateStore(): ViewModelStore {
        return driverViewModelStore ?: ViewModelStore().also { driverViewModelStore = it }
    }

    fun clearStore() {
        driverViewModelStore?.clear()
        driverViewModelStore = null
    }

    override fun onCleared() {
        clearStore()
    }
}