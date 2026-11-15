package io.github.vimitt.overview.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * Tokens de cor centralizados inspirados na paleta do libadwaita / GNOME 51.
 *
 * Todas as cores da interface devem derivar desta definição para manter
 * consistência entre tema claro, escuro e cor de destaque configurável.
 */
object AdwaitaPalette {
    // Cores de destaque oficiais inspiradas na paleta Adwaita
    val Blue = Color(0xFF3584E4)
    val BlueDarkVariant = Color(0xFF62A0EA)
    val Teal = Color(0xFF2190A4)
    val Green = Color(0xFF3A944A)
    val Yellow = Color(0xFFC88800)
    val Orange = Color(0xFFED5B00)
    val Red = Color(0xFFE62D42)
    val Pink = Color(0xFFD56199)
    val Purple = Color(0xFF9141AC)
    val Slate = Color(0xFF6F8396)

    val AccentPresets: List<Pair<String, Long>> = listOf(
        "Azul" to 0xFF3584E4L,
        "Turquesa" to 0xFF2190A4L,
        "Verde" to 0xFF3A944AL,
        "Amarelo" to 0xFFC88800L,
        "Laranja" to 0xFFED5B00L,
        "Vermelho" to 0xFFE62D42L,
        "Rosa" to 0xFFD56199L,
        "Roxo" to 0xFF9141ACL,
        "Ardósia" to 0xFF6F8396L,
    )

    // Superfícies claras
    val LightWindowBackground = Color(0xFFFAFAFA)
    val LightSurface = Color(0xFFFFFFFF)
    val LightSurfaceContainer = Color(0xFFFFFFFF)
    val LightSurfaceElevated = Color(0xFFEBEBEB)
    val LightSearchFill = Color(0xFFEAEAEA)
    val LightOnSurface = Color(0xFF242424)
    val LightOnSurfaceMuted = Color(0xFF5E5C64)
    val LightBorder = Color(0x1F000000)

    // Superfícies escuras
    val DarkWindowBackground = Color(0xFF242424)
    val DarkSurface = Color(0xFF303030)
    val DarkSurfaceContainer = Color(0xFF303030)
    val DarkSurfaceElevated = Color(0xFF383838)
    val DarkSearchFill = Color(0xFF3A3A3A)
    val DarkOnSurface = Color(0xFFFFFFFF)
    val DarkOnSurfaceMuted = Color(0xFFC0BFBC)
    val DarkBorder = Color(0x24FFFFFF)

    // Sobreposições sobre o papel de parede
    val LightDashBackground = Color(0xDCFAFAFA)
    val DarkDashBackground = Color(0xD2242424)
    val LightOverviewScrim = Color(0xADFAFAFA)
    val DarkOverviewScrim = Color(0xBA1C1C1C)

    // Texto sobre papel de parede (tela inicial)
    val WallpaperForeground = Color(0xFFFFFFFF)
    val WallpaperShadow = Color(0x66000000)
}

/**
 * Conjunto completo de tokens semânticos expostos pelo tema do Overview.
 */
@Immutable
data class OverviewColors(
    val isDark: Boolean,
    val accent: Color,
    val onAccent: Color,
    val windowBackground: Color,
    val surface: Color,
    val surfaceContainer: Color,
    val surfaceElevated: Color,
    val searchFieldBackground: Color,
    val onSurface: Color,
    val onSurfaceMuted: Color,
    val hairlineBorder: Color,
    val dashBackground: Color,
    val overviewScrim: Color,
    val wallpaperForeground: Color,
    val wallpaperShadow: Color,
)

/**
 * Gera os tokens de cor para tema claro ou escuro a partir da cor de destaque.
 */
fun overviewColors(
    isDark: Boolean,
    accentColor: Color = AdwaitaPalette.Blue,
): OverviewColors {
    // No tema escuro, suaviza levemente o azul padrão para manter conforto visual.
    val resolvedAccent = if (isDark && accentColor == AdwaitaPalette.Blue) {
        AdwaitaPalette.BlueDarkVariant
    } else {
        accentColor
    }

    return if (isDark) {
        OverviewColors(
            isDark = true,
            accent = resolvedAccent,
            onAccent = Color(0xFF101820),
            windowBackground = AdwaitaPalette.DarkWindowBackground,
            surface = AdwaitaPalette.DarkSurface,
            surfaceContainer = AdwaitaPalette.DarkSurfaceContainer,
            surfaceElevated = AdwaitaPalette.DarkSurfaceElevated,
            searchFieldBackground = AdwaitaPalette.DarkSearchFill,
            onSurface = AdwaitaPalette.DarkOnSurface,
            onSurfaceMuted = AdwaitaPalette.DarkOnSurfaceMuted,
            hairlineBorder = AdwaitaPalette.DarkBorder,
            dashBackground = AdwaitaPalette.DarkDashBackground,
            overviewScrim = AdwaitaPalette.DarkOverviewScrim,
            wallpaperForeground = AdwaitaPalette.WallpaperForeground,
            wallpaperShadow = AdwaitaPalette.WallpaperShadow,
        )
    } else {
        OverviewColors(
            isDark = false,
            accent = resolvedAccent,
            onAccent = Color.White,
            windowBackground = AdwaitaPalette.LightWindowBackground,
            surface = AdwaitaPalette.LightSurface,
            surfaceContainer = AdwaitaPalette.LightSurfaceContainer,
            surfaceElevated = AdwaitaPalette.LightSurfaceElevated,
            searchFieldBackground = AdwaitaPalette.LightSearchFill,
            onSurface = AdwaitaPalette.LightOnSurface,
            onSurfaceMuted = AdwaitaPalette.LightOnSurfaceMuted,
            hairlineBorder = AdwaitaPalette.LightBorder,
            dashBackground = AdwaitaPalette.LightDashBackground,
            overviewScrim = AdwaitaPalette.LightOverviewScrim,
            wallpaperForeground = AdwaitaPalette.WallpaperForeground,
            wallpaperShadow = AdwaitaPalette.WallpaperShadow,
        )
    }
}
