package de.dh.daps.ui.common.formatters

import android.content.res.Resources
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext

/**
 * Plural string treatment with fixed support of the string for quantity 0.
 * Works around Android's implementation which uses CLDR plural rules which make the {@code "zero"}
 * quantity string never used; instead, for quantity 0, the {@code "other"} string is used.
 * To work around this, the caller must provide the {@code "zero"} quantity string as separate string resource.
 *
 * Example XML declaration:
 * ```xml
 * <resources>
 *     <!-- The 'zero' item in this plurals resource is ignored by the system for quantity 0 on English and German locales -->
 *     <plurals name="n_items_available">
 *         <item quantity="zero">@string/no_items_available</item>
 *         <item quantity="one">1 item available</item>
 *         <item quantity="other">%d items available</item>
 *     </plurals>
 *
 *     <!-- This separate string is used as a workaround -->
 *     <string name="no_items_available">No items available</string>
 * </resources>
 * ```
 *
 * Code usage:
 * ```kotlin
 * getQuantityStringZero(
 *     resources,
 *     R.plurals.n_items_available,
 *     R.string.no_items_available,
 *     0
 * ) // returns "No items available"
 * ```
 *
 * @param resources The resources to get the strings from.
 * @param pluralsStringResId The resource id of the plurals string.
 * @param zeroStringResId The resource id of the string providing the {@code "zero"} string contents.
 * @param quantity The quantity to use as selector.
 * @return Correct quantity string, depending on quantity.
 */
fun getQuantityStringZero(
    resources: Resources,
    @PluralsRes pluralsStringResId: Int,
    @StringRes zeroStringResId: Int,
    quantity: Int,
    vararg formatArgs: Any
): String {
    return if (quantity == 0) {
        resources.getString(zeroStringResId, *formatArgs)
    } else {
        if (formatArgs.isEmpty()) {
            resources.getQuantityString(pluralsStringResId, quantity, quantity)
        } else {
            resources.getQuantityString(pluralsStringResId, quantity, *formatArgs)
        }
    }
}

/**
 * Composable UI function.
 * Plural string treatment with fixed support of the string for quantity 0.
 *
 * @param pluralsStringResId The resource id of the plurals string.
 * @param zeroStringResId The resource id of the string providing the {@code "zero"} string contents.
 * @param quantity The quantity to use as selector.
 * @return Correct quantity string, depending on quantity.
 */
@Composable
fun getQuantityStringZero(
    @PluralsRes pluralsStringResId: Int,
    @StringRes zeroStringResId: Int,
    quantity: Int,
    vararg formatArgs: Any
): String {
    LocalConfiguration.current
    val resources = LocalContext.current.resources
    return getQuantityStringZero(resources, pluralsStringResId, zeroStringResId, quantity, *formatArgs)
}

/**
 * Composable UI function.
 * Plural string resource wrapper with fixed support of the string for quantity 0.
 *
 * @param pluralsResId The resource id of the plurals string.
 * @param zeroResId The resource id of the string providing the {@code "zero"} string contents.
 * @param quantity The quantity to use as selector.
 * @return Correct quantity string, depending on quantity.
 */
@Composable
fun pluralStringResourceZero(
    @PluralsRes pluralsResId: Int,
    @StringRes zeroResId: Int,
    quantity: Int,
    vararg formatArgs: Any
): String {
    LocalConfiguration.current
    val resources = LocalContext.current.resources
    return getQuantityStringZero(resources, pluralsResId, zeroResId, quantity, *formatArgs)
}