package de.dh.daps.common.model

import androidx.compose.runtime.Composable

/**
 * Interface for glucose source plugins to provide additional UI content in the System Control screen.
 * Plugins that implement this alongside GlucoseSource will have their content displayed.
 */
interface GlucoseSourcePluginUiProvider {
    /**
     * Composable content to be displayed in the Glucose Source tab of the System Control screen.
     */
    @Composable
    fun GlucoseSourceControlSection()
}