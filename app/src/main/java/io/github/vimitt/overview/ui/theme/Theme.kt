package io.github.vimitt.overview.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import io.github.vimitt.overview.domain.model.ThemeMode

val LocalOverviewColors = staticCompositionLocalOf {
    overviewColors(isDark = true)
}

/**
 * Atalho idiomático para acessar os tokens visuais do tema atual.
 */
object OverviewTheme {
    val colors: OverviewColors
        @Composable
        @ReadOnlyComposable
        get() = LocalOverviewColors.current

    val reducedMotion: Boolean
        @Composable
        @ReadOnlyComposable
        get() = LocalReducedMotion.current
}

/**
 * Tema principal do launcher Overview, usando Material 3 apenas como base
 * estrutural e aplicando os tokens visuais inspirados no Adwaita.
 */
@Composable
fun OverviewTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    accentColor: Color = AdwaitaPalette.Blue,
    content: @Composable () -> Unit,
) {
    val systemDark = isSystemInDarkTheme()
    val isDark = when (themeMode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val reducedMotion = rememberReducedMotion()
    val colors = remember(isDark, accentColor) {
        overviewColors(isDark = isDark, accentColor = accentColor)
    }

    val materialScheme = remember(colors) {
        if (colors.isDark) {
            darkColorScheme(
                primary = colors.accent,
                onPrimary = colors.onAccent,
                background = Color.Transparent,
                surface = colors.surface,
                surfaceContainer = colors.surfaceContainer,
                surfaceVariant = colors.surfaceElevated,
                onBackground = colors.onSurface,
                onSurface = colors.onSurface,
                onSurfaceVariant = colors.onSurfaceMuted,
                outline = colors.hairlineBorder,
            )
        } else {
            lightColorScheme(
                primary = colors.accent,
                onPrimary = colors.onAccent,
                background = Color.Transparent,
                surface = colors.surface,
                surfaceContainer = colors.surfaceContainer,
                surfaceVariant = colors.surfaceElevated,
                onBackground = colors.onSurface,
                onSurface = colors.onSurface,
                onSurfaceVariant = colors.onSurfaceMuted,
                outline = colors.hairlineBorder,
            )
        }
    }

    CompositionLocalProvider(
        LocalOverviewColors provides colors,
        LocalReducedMotion provides reducedMotion,
    ) {
        MaterialTheme(
            colorScheme = materialScheme,
            shapes = OverviewShapes,
            typography = OverviewTypography,
            content = content,
        )
    }
}
