package de.dh.daps.ui.common.composables

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner

/**
 * Wraps a driver's setup UI in an isolated [ViewModelStoreOwner].
 * When [driverId] changes or this container leaves the composition,
 * the isolated [ViewModelStore] is cleared, guaranteeing that driver-specific
 * ViewModels do not leak state or linger in memory across driver selections.
 */
@Composable
fun ScopedDriverSetupContainer(
    driverId: String,
    content: @Composable () -> Unit
) {
    val viewModelStore = remember(driverId) { ViewModelStore() }
    val localOwner = remember(viewModelStore) {
        object : ViewModelStoreOwner {
            override val viewModelStore: ViewModelStore = viewModelStore
        }
    }

    CompositionLocalProvider(LocalViewModelStoreOwner provides localOwner) {
        content()
    }

    DisposableEffect(driverId) {
        onDispose {
            viewModelStore.clear()
        }
    }
}