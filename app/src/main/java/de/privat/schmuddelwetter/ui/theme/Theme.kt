package de.privat.schmuddelwetter.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Zusätzliche, im Material3-ColorScheme nicht vorgesehene Nothing-Farbrollen
 * (z. B. feine Trennlinien im Grid-Look).
 */
data class ExtendedColors(
    val divider: Color,
    val dotGrid: Color,
    val onSurfaceMuted: Color,
)

val LocalExtendedColors = staticCompositionLocalOf {
    ExtendedColors(divider = LightDivider, dotGrid = Graphite200, onSurfaceMuted = Graphite400)
}

private val DarkScheme = darkColorScheme(
    primary = PureWhite,
    onPrimary = PureBlack,
    secondary = AccentRed,
    onSecondary = PureWhite,
    background = PureBlack,
    onBackground = PureWhite,
    surface = Graphite800,
    onSurface = PureWhite,
    surfaceVariant = Graphite700,
    onSurfaceVariant = Graphite200,
    outline = Graphite600,
    error = AccentRed,
)

private val LightScheme = lightColorScheme(
    primary = PureBlack,
    onPrimary = PureWhite,
    secondary = AccentRed,
    onSecondary = PureWhite,
    background = OffWhite,
    onBackground = PureBlack,
    surface = PureWhite,
    onSurface = PureBlack,
    surfaceVariant = LightSurface,
    onSurfaceVariant = Graphite400,
    outline = LightDivider,
    error = AccentRed,
)

@Composable
fun SchmuddelwetterTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkScheme else LightScheme
    val extended = if (darkTheme) {
        ExtendedColors(divider = Graphite600, dotGrid = Graphite400, onSurfaceMuted = Graphite200)
    } else {
        ExtendedColors(divider = LightDivider, dotGrid = Graphite400, onSurfaceMuted = Graphite400)
    }

    CompositionLocalProvider(LocalExtendedColors provides extended) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content,
        )
    }
}

object AppTheme {
    val extendedColors: ExtendedColors
        @Composable
        get() = LocalExtendedColors.current
}
