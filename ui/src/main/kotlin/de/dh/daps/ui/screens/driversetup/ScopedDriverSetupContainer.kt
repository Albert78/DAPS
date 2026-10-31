package de.dh.daps.ui.screens.driversetup

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * Wraps a driver's setup UI in an isolated [ViewModelStoreOwner].
 * Binds the driver's ViewModelStore to the parent [ViewModelStoreOwner] (e.g. the NavEntry of the PumpSetupRoute)
 * via [DriverViewModelStoreHolder], so that driver ViewModels survive activity configuration changes (rotations).
 *
 * When [driverId] changes or leaves composition, the driver ViewModelStore is cleared immediately.
 */
@Composable
fun ScopedDriverSetupContainer(
    driverId: String,
    content: @Composable () -> Unit
) {
    val parentOwner = LocalViewModelStoreOwner.current
        ?: error("No LocalViewModelStoreOwner found for ScopedDriverSetupContainer")

    val holderViewModel: DriverViewModelStoreHolder = viewModel(viewModelStoreOwner = parentOwner)

    DisposableEffect(driverId) {
        onDispose {
            holderViewModel.clearStore()
        }
    }

    val driverStore = holderViewModel.getOrCreateStore()
    val localOwner = remember(driverStore) {
        object : ViewModelStoreOwner {
            override val viewModelStore = driverStore
        }
    }

    CompositionLocalProvider(LocalViewModelStoreOwner provides localOwner) {
        content()
    }
}