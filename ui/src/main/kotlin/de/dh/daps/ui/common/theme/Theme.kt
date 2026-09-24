package de.dh.daps.ui.common.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import de.dh.daps.common.model.data.CarbsUnit
import de.dh.daps.common.model.data.GlucoseUnit
import de.dh.daps.ui.common.LocalAppFormatters
import de.dh.daps.ui.common.LocalCarbsUnit
import de.dh.daps.ui.common.LocalGlucoseUnit
import de.dh.daps.ui.common.composables.AppColorBlue
import de.dh.daps.ui.common.rememberAppFormatters

@Immutable
data class StatusColors(
    val safe: Color,
    val warning: Color,
    val overdue: Color
)

val LocalStatusColors = staticCompositionLocalOf {
    StatusColors(
        safe = Color.Unspecified,
        warning = Color.Unspecified,
        overdue = Color.Unspecified
    )
}

@Immutable
data class SemanticColors(
    val good: Color,
    val warning: Color,
    val bad: Color,
    val highContrast: Color,
    val border: Color,
)

val LocalSemanticColors = staticCompositionLocalOf {
    SemanticColors(
        good = Color.Unspecified,
        warning = Color.Unspecified,
        bad = Color.Unspecified,
        highContrast = Color.Unspecified,
        border = Color.Unspecified
    )
}

object ExtendedTheme {
    val statusColors: StatusColors
        @Composable
        get() = LocalStatusColors.current

    val semanticColors: SemanticColors
        @Composable
        get() = LocalSemanticColors.current
}

private val LightStatusColors = StatusColors(
    safe = status_light_safe,
    warning = status_light_warning,
    overdue = status_light_overdue
)

private val DarkStatusColors = StatusColors(
    safe = status_dark_safe,
    warning = status_dark_warning,
    overdue = status_dark_overdue
)

private val LightSemanticColors = SemanticColors(
    good = semantic_light_good,
    warning = semantic_light_warning,
    bad = semantic_light_bad,
    highContrast = Color.Black,
    border = AppColorBlue.copy(alpha = 0.3f)
)

private val DarkSemanticColors = SemanticColors(
    good = semantic_dark_good,
    warning = semantic_dark_warning,
    bad = semantic_dark_bad,
    highContrast = Color.White,
    border = AppColorBlue.copy(alpha = 0.3f)
)

private val LightColors = lightColorScheme(
    primary = md_theme_light_primary,
    onPrimary = md_theme_light_onPrimary,
    primaryContainer = md_theme_light_primaryContainer,
    onPrimaryContainer = md_theme_light_onPrimaryContainer,
    secondary = md_theme_light_secondary,
    onSecondary = md_theme_light_onSecondary,
    secondaryContainer = md_theme_light_secondaryContainer,
    onSecondaryContainer = md_theme_light_onSecondaryContainer,
    tertiary = md_theme_light_tertiary,
    onTertiary = md_theme_light_onTertiary,
    tertiaryContainer = md_theme_light_tertiaryContainer,
    onTertiaryContainer = md_theme_light_onTertiaryContainer,
    error = md_theme_light_error,
    onError = md_theme_light_onError,
    errorContainer = md_theme_light_errorContainer,
    onErrorContainer = md_theme_light_onErrorContainer,
    background = md_theme_light_background,
    onBackground = md_theme_light_onBackground,
    surface = md_theme_light_surface,
    onSurface = md_theme_light_onSurface,
    surfaceVariant = md_theme_light_surfaceVariant,
    onSurfaceVariant = md_theme_light_onSurfaceVariant,
    outline = md_theme_light_outline,
    surfaceContainer = md_theme_light_surfaceContainer,
    surfaceContainerHigh = md_theme_light_surfaceContainerHigh,
    surfaceContainerHighest = md_theme_light_surfaceContainerHighest
)

private val DarkColors = darkColorScheme(
    primary = md_theme_dark_primary,
    onPrimary = md_theme_dark_onPrimary,
    primaryContainer = md_theme_dark_primaryContainer,
    onPrimaryContainer = md_theme_dark_onPrimaryContainer,
    secondary = md_theme_dark_secondary,
    onSecondary = md_theme_dark_onSecondary,
    secondaryContainer = md_theme_dark_secondaryContainer,
    onSecondaryContainer = md_theme_dark_onSecondaryContainer,
    tertiary = md_theme_dark_tertiary,
    onTertiary = md_theme_dark_onTertiary,
    tertiaryContainer = md_theme_dark_tertiaryContainer,
    onTertiaryContainer = md_theme_dark_onTertiaryContainer,
    error = md_theme_dark_error,
    onError = md_theme_dark_onError,
    errorContainer = md_theme_dark_errorContainer,
    onErrorContainer = md_theme_dark_onErrorContainer,
    background = md_theme_dark_background,
    onBackground = md_theme_dark_onBackground,
    surface = md_theme_dark_surface,
    onSurface = md_theme_dark_onSurface,
    surfaceVariant = md_theme_dark_surfaceVariant,
    onSurfaceVariant = md_theme_dark_onSurfaceVariant,
    outline = md_theme_dark_outline,
    surfaceContainer = md_theme_dark_surfaceContainer,
    surfaceContainerHigh = md_theme_dark_surfaceContainerHigh,
    surfaceContainerHighest = md_theme_dark_surfaceContainerHighest
)

@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    glucoseUnit: GlucoseUnit = GlucoseUnit.MG_DL,
    carbsUnit: CarbsUnit = CarbsUnit.GRAMS,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        DarkColors
    } else {
        LightColors
    }

    val statusColors = if (darkTheme) {
        DarkStatusColors
    } else {
        LightStatusColors
    }

    val semanticColors = if (darkTheme) {
        DarkSemanticColors
    } else {
        LightSemanticColors
    }

    CompositionLocalProvider(
        LocalStatusColors provides statusColors,
        LocalSemanticColors provides semanticColors,
        LocalAppFormatters provides rememberAppFormatters(),
        LocalGlucoseUnit provides glucoseUnit,
        LocalCarbsUnit provides carbsUnit
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            content = content
        )
    }
}

/**
 * Preview wrapper that combines [AppTheme] and [Surface] to provide a consistent
 * background, content color, and app context (like [LocalGlucoseUnit]) for Compose Previews.
 */
@Composable
fun AppPreview(
    modifier: Modifier = Modifier,
    darkTheme: Boolean = isSystemInDarkTheme(),
    glucoseUnit: GlucoseUnit = GlucoseUnit.MG_DL,
    carbsUnit: CarbsUnit = CarbsUnit.GRAMS,
    content: @Composable () -> Unit
) {
    AppTheme(
        darkTheme = darkTheme,
        glucoseUnit = glucoseUnit,
        carbsUnit = carbsUnit
    ) {
        Surface(
            modifier = modifier,
            color = MaterialTheme.colorScheme.background,
            content = content
        )
    }
}