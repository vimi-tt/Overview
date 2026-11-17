package io.github.vimitt.overview.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import io.github.vimitt.overview.domain.model.ColorSourceMode
import io.github.vimitt.overview.domain.model.IconShape
import io.github.vimitt.overview.domain.model.MotionIntensity
import io.github.vimitt.overview.domain.model.SettingsDefaults
import io.github.vimitt.overview.domain.model.ShapeFamily
import io.github.vimitt.overview.domain.model.ThemeMode
import io.github.vimitt.overview.domain.model.TypographyWeightMode
import io.github.vimitt.overview.domain.model.VisualStyle

val LocalVisualStyle = staticCompositionLocalOf { VisualStyle.GNOME }

val LocalOverviewColors = staticCompositionLocalOf {
    overviewColors(isDark = true)
}

val LocalOverviewShapes = staticCompositionLocalOf {
    overviewShapeTokens()
}

val LocalOverviewMotion = staticCompositionLocalOf {
    overviewMotionTokens()
}

val LocalOverviewTypography = staticCompositionLocalOf {
    overviewTypographyTokens()
}

val LocalOverviewComponents = staticCompositionLocalOf {
    overviewComponentTokens(VisualStyle.GNOME)
}

/**
 * Atalho idiomático para acessar todos os tokens visuais do tema atual via `CompositionLocal`.
 */
object OverviewTheme {
    val style: VisualStyle
        @Composable
        @ReadOnlyComposable
        get() = LocalVisualStyle.current

    val colors: OverviewColors
        @Composable
        @ReadOnlyComposable
        get() = LocalOverviewColors.current

    val shapes: OverviewShapeTokens
        @Composable
        @ReadOnlyComposable
        get() = LocalOverviewShapes.current

    val motion: OverviewMotionTokens
        @Composable
        @ReadOnlyComposable
        get() = LocalOverviewMotion.current

    val typography: OverviewTypographyTokens
        @Composable
        @ReadOnlyComposable
        get() = LocalOverviewTypography.current

    val components: OverviewComponentTokens
        @Composable
        @ReadOnlyComposable
        get() = LocalOverviewComponents.current

    val reducedMotion: Boolean
        @Composable
        @ReadOnlyComposable
        get() = LocalReducedMotion.current
}

/**
 * Tema principal do launcher Overview.
 *
 * Alterna dinamicamente entre o estilo [VisualStyle.GNOME] (Adwaita clássico) e
 * [VisualStyle.EXPRESSIVE] (GNOME × Material 3 Expressive) via `CompositionLocal`.
 */
@Composable
fun OverviewTheme(
    visualStyle: VisualStyle = VisualStyle.GNOME,
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    colorSourceMode: ColorSourceMode = ColorSourceMode.GNOME,
    accentColor: Color = AdwaitaPalette.Blue,
    motionIntensity: MotionIntensity = MotionIntensity.MEDIUM,
    shapeFamily: ShapeFamily = ShapeFamily.ROUNDED,
    iconShape: IconShape = IconShape.ROUNDED_SQUARE,
    iconRoundness: Float = SettingsDefaults.DEFAULT_ICON_ROUNDNESS,
    typographyWeightMode: TypographyWeightMode = TypographyWeightMode.NORMAL,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val systemDark = isSystemInDarkTheme()
    val isDark = when (themeMode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val reducedMotion = rememberReducedMotion()
    val dynamicScheme = remember(context, isDark, colorSourceMode) {
        if (colorSourceMode == ColorSourceMode.DYNAMIC) {
            resolveDynamicColorScheme(context = context, isDark = isDark)
        } else {
            null
        }
    }

    val rawColors = remember(isDark, accentColor, visualStyle, colorSourceMode, dynamicScheme) {
        overviewColors(
            isDark = isDark,
            accentColor = accentColor,
            visualStyle = visualStyle,
            colorSourceMode = colorSourceMode,
            dynamicScheme = dynamicScheme,
        )
    }
    val colors = animateOverviewColors(target = rawColors, reducedMotion = reducedMotion)

    val shapeTokens = remember(visualStyle, shapeFamily, iconShape, iconRoundness) {
        overviewShapeTokens(
            visualStyle = visualStyle,
            shapeFamily = shapeFamily,
            iconShape = iconShape,
            iconRoundness = iconRoundness,
        )
    }

    val motionTokens = remember(visualStyle, motionIntensity, reducedMotion) {
        overviewMotionTokens(
            visualStyle = visualStyle,
            intensity = motionIntensity,
            reducedMotion = reducedMotion,
        )
    }

    val typographyTokens = remember(visualStyle, typographyWeightMode) {
        overviewTypographyTokens(
            visualStyle = visualStyle,
            weightMode = typographyWeightMode,
        )
    }

    val componentTokens = remember(visualStyle) {
        overviewComponentTokens(visualStyle)
    }

    val materialScheme = remember(colors) {
        if (colors.isDark) {
            darkColorScheme(
                primary = colors.accent,
                onPrimary = colors.onAccent,
                primaryContainer = colors.accentContainer,
                onPrimaryContainer = colors.onAccentContainer,
                secondary = colors.secondaryAccent,
                tertiary = colors.tertiary,
                onTertiary = colors.onTertiary,
                tertiaryContainer = colors.tertiaryContainer,
                onTertiaryContainer = colors.onTertiaryContainer,
                error = colors.error,
                onError = colors.onError,
                errorContainer = colors.errorContainer,
                onErrorContainer = colors.onErrorContainer,
                background = Color.Transparent,
                surface = colors.surface,
                surfaceContainer = colors.surfaceContainer,
                surfaceContainerHigh = colors.surfaceContainerHigh,
                surfaceVariant = colors.surfaceElevated,
                onBackground = colors.onSurface,
                onSurface = colors.onSurface,
                onSurfaceVariant = colors.onSurfaceMuted,
                outline = colors.hairlineBorder,
                outlineVariant = colors.outlineVariant,
            )
        } else {
            lightColorScheme(
                primary = colors.accent,
                onPrimary = colors.onAccent,
                primaryContainer = colors.accentContainer,
                onPrimaryContainer = colors.onAccentContainer,
                secondary = colors.secondaryAccent,
                tertiary = colors.tertiary,
                onTertiary = colors.onTertiary,
                tertiaryContainer = colors.tertiaryContainer,
                onTertiaryContainer = colors.onTertiaryContainer,
                error = colors.error,
                onError = colors.onError,
                errorContainer = colors.errorContainer,
                onErrorContainer = colors.onErrorContainer,
                background = Color.Transparent,
                surface = colors.surface,
                surfaceContainer = colors.surfaceContainer,
                surfaceContainerHigh = colors.surfaceContainerHigh,
                surfaceVariant = colors.surfaceElevated,
                onBackground = colors.onSurface,
                onSurface = colors.onSurface,
                onSurfaceVariant = colors.onSurfaceMuted,
                outline = colors.hairlineBorder,
                outlineVariant = colors.outlineVariant,
            )
        }
    }

    CompositionLocalProvider(
        LocalVisualStyle provides visualStyle,
        LocalOverviewColors provides colors,
        LocalOverviewShapes provides shapeTokens,
        LocalOverviewMotion provides motionTokens,
        LocalOverviewTypography provides typographyTokens,
        LocalOverviewComponents provides componentTokens,
        LocalReducedMotion provides reducedMotion,
    ) {
        OverviewMaterialThemeBridge(
            visualStyle = visualStyle,
            colorScheme = materialScheme,
            shapes = shapeTokens.materialShapes,
            typography = typographyTokens.typography,
            content = content,
        )
    }
}

@Composable
private fun animateOverviewColors(
    target: OverviewColors,
    reducedMotion: Boolean,
): OverviewColors {
    val spec = if (reducedMotion) {
        snap<Color>()
    } else {
        tween(durationMillis = 380, easing = FastOutSlowInEasing)
    }
    val accent by animateColorAsState(target.accent, spec, label = "theme_accent")
    val onAccent by animateColorAsState(target.onAccent, spec, label = "theme_on_accent")
    val accentContainer by animateColorAsState(target.accentContainer, spec, label = "theme_accent_container")
    val onAccentContainer by animateColorAsState(target.onAccentContainer, spec, label = "theme_on_accent_container")
    val secondaryAccent by animateColorAsState(target.secondaryAccent, spec, label = "theme_secondary_accent")
    val tertiary by animateColorAsState(target.tertiary, spec, label = "theme_tertiary")
    val onTertiary by animateColorAsState(target.onTertiary, spec, label = "theme_on_tertiary")
    val tertiaryContainer by animateColorAsState(target.tertiaryContainer, spec, label = "theme_tertiary_container")
    val onTertiaryContainer by animateColorAsState(target.onTertiaryContainer, spec, label = "theme_on_tertiary_container")
    val error by animateColorAsState(target.error, spec, label = "theme_error")
    val onError by animateColorAsState(target.onError, spec, label = "theme_on_error")
    val errorContainer by animateColorAsState(target.errorContainer, spec, label = "theme_error_container")
    val onErrorContainer by animateColorAsState(target.onErrorContainer, spec, label = "theme_on_error_container")
    val windowBackground by animateColorAsState(target.windowBackground, spec, label = "theme_window_bg")
    val surface by animateColorAsState(target.surface, spec, label = "theme_surface")
    val surfaceContainer by animateColorAsState(target.surfaceContainer, spec, label = "theme_surface_container")
    val surfaceElevated by animateColorAsState(target.surfaceElevated, spec, label = "theme_surface_elevated")
    val surfaceContainerHigh by animateColorAsState(target.surfaceContainerHigh, spec, label = "theme_surface_high")
    val searchFieldBackground by animateColorAsState(target.searchFieldBackground, spec, label = "theme_search_fill")
    val onSurface by animateColorAsState(target.onSurface, spec, label = "theme_on_surface")
    val onSurfaceMuted by animateColorAsState(target.onSurfaceMuted, spec, label = "theme_on_surface_muted")
    val hairlineBorder by animateColorAsState(target.hairlineBorder, spec, label = "theme_border")
    val outlineVariant by animateColorAsState(target.outlineVariant, spec, label = "theme_outline_variant")
    val dashBackground by animateColorAsState(target.dashBackground, spec, label = "theme_dash_bg")
    val overviewScrim by animateColorAsState(target.overviewScrim, spec, label = "theme_scrim")

    return target.copy(
        accent = accent,
        onAccent = onAccent,
        accentContainer = accentContainer,
        onAccentContainer = onAccentContainer,
        secondaryAccent = secondaryAccent,
        tertiary = tertiary,
        onTertiary = onTertiary,
        tertiaryContainer = tertiaryContainer,
        onTertiaryContainer = onTertiaryContainer,
        error = error,
        onError = onError,
        errorContainer = errorContainer,
        onErrorContainer = onErrorContainer,
        windowBackground = windowBackground,
        surface = surface,
        surfaceContainer = surfaceContainer,
        surfaceElevated = surfaceElevated,
        surfaceContainerHigh = surfaceContainerHigh,
        searchFieldBackground = searchFieldBackground,
        onSurface = onSurface,
        onSurfaceMuted = onSurfaceMuted,
        hairlineBorder = hairlineBorder,
        outlineVariant = outlineVariant,
        dashBackground = dashBackground,
        overviewScrim = overviewScrim,
    )
}
