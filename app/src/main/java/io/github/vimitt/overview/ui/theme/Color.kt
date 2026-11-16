package io.github.vimitt.overview.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import io.github.vimitt.overview.R
import io.github.vimitt.overview.domain.model.ColorSourceMode
import io.github.vimitt.overview.domain.model.VisualStyle
import kotlin.math.max
import kotlin.math.min

/**
 * Tokens de cor centralizados inspirados na paleta do libadwaita / GNOME 51,
 * com suporte a superfícies tonais e cor dinâmica (Material You) no estilo Expressive.
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

    // Presets extras mais vibrantes para cor personalizada
    val CustomAccentPresets: List<Pair<String, Long>> = listOf(
        "Índigo" to 0xFF5B5CE6L,
        "Lavanda" to 0xFF7D52B8L,
        "Ciano" to 0xFF0087A8L,
        "Esmeralda" to 0xFF1F9D55L,
        "Âmbar" to 0xFFD97706L,
        "Coral" to 0xFFE04F5FL,
        "Magenta" to 0xFFC63D85L,
        "Grafite" to 0xFF526375L,
    )

    val AccentPresetNameRes: Map<Long, Int> = mapOf(
        0xFF3584E4L to R.string.color_preset_blue,
        0xFF2190A4L to R.string.color_preset_teal,
        0xFF3A944AL to R.string.color_preset_green,
        0xFFC88800L to R.string.color_preset_yellow,
        0xFFED5B00L to R.string.color_preset_orange,
        0xFFE62D42L to R.string.color_preset_red,
        0xFFD56199L to R.string.color_preset_pink,
        0xFF9141ACL to R.string.color_preset_purple,
        0xFF6F8396L to R.string.color_preset_slate,
        0xFF5B5CE6L to R.string.color_preset_indigo,
        0xFF7D52B8L to R.string.color_preset_lavender,
        0xFF0087A8L to R.string.color_preset_cyan,
        0xFF1F9D55L to R.string.color_preset_emerald,
        0xFFD97706L to R.string.color_preset_amber,
        0xFFE04F5FL to R.string.color_preset_coral,
        0xFFC63D85L to R.string.color_preset_magenta,
        0xFF526375L to R.string.color_preset_graphite,
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
    val accentContainer: Color,
    val onAccentContainer: Color,
    val secondaryAccent: Color,
    val windowBackground: Color,
    val surface: Color,
    val surfaceContainer: Color,
    val surfaceElevated: Color,
    val surfaceContainerHigh: Color,
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
 * Calcula a razão de contraste WCAG 2.1 (de 1f a 21f) considerando alpha sobre o fundo.
 */
fun contrastRatio(foreground: Color, background: Color): Float {
    val bgOpaque = if (background.alpha < 1f) {
        lerp(Color.Black, background.copy(alpha = 1f), background.alpha)
    } else {
        background
    }
    val fgComposited = if (foreground.alpha < 1f) {
        lerp(bgOpaque, foreground.copy(alpha = 1f), foreground.alpha)
    } else {
        foreground
    }
    val l1 = fgComposited.luminance()
    val l2 = bgOpaque.luminance()
    val lighter = max(l1, l2)
    val darker = min(l1, l2)
    return (lighter + 0.05f) / (darker + 0.05f)
}

/**
 * Escolhe o texto de maior contraste sobre uma cor de fundo (claro vs escuro).
 */
fun bestOnColor(
    background: Color,
    lightOption: Color = Color.White,
    darkOption: Color = Color(0xFF101820),
): Color = if (contrastRatio(lightOption, background) >= contrastRatio(darkOption, background)) {
    lightOption
} else {
    darkOption
}

/**
 * Garante contraste mínimo WCAG AA para textos principais, ajustando em direção ao preto/branco se necessário.
 */
fun ensureWcagAa(
    foreground: Color,
    background: Color,
    minRatio: Float = 4.5f,
): Color {
    if (contrastRatio(foreground, background) >= minRatio) return foreground
    val target = bestOnColor(background, lightOption = Color.White, darkOption = Color(0xFF141414))
    var low = 0f
    var high = 1f
    var candidate = target
    repeat(8) {
        val mid = (low + high) / 2f
        val mixed = lerp(foreground, target, mid)
        if (contrastRatio(mixed, background) >= minRatio) {
            candidate = mixed
            high = mid
        } else {
            low = mid
        }
    }
    return candidate
}

/**
 * Aumenta suavemente a saturação/luminosidade do destaque no estilo Expressive sem perder legibilidade,
 * em Kotlin puro (sem dependência de framework Android para facilitar testes unitários).
 */
private fun boostExpressiveAccent(base: Color, isDark: Boolean): Color {
    val r = base.red
    val g = base.green
    val b = base.blue
    val maxC = max(r, max(g, b))
    val minC = min(r, min(g, b))
    val delta = maxC - minC

    var hue = when {
        delta <= 1e-5f -> 0f
        maxC == r -> 60f * (((g - b) / delta) % 6f)
        maxC == g -> 60f * (((b - r) / delta) + 2f)
        else -> 60f * (((r - g) / delta) + 4f)
    }
    if (hue < 0f) hue += 360f

    val sat = if (maxC <= 1e-5f) 0f else (delta / maxC)
    val boostedSat = (sat * 1.14f).coerceIn(0f, 1f)
    val boostedVal = if (isDark) {
        (maxC * 1.08f).coerceIn(0.42f, 0.96f)
    } else {
        (maxC * 0.96f).coerceIn(0.28f, 0.88f)
    }

    val c = boostedVal * boostedSat
    val x = c * (1f - kotlin.math.abs(((hue / 60f) % 2f) - 1f))
    val m = boostedVal - c
    val (rp, gp, bp) = when {
        hue < 60f -> Triple(c, x, 0f)
        hue < 120f -> Triple(x, c, 0f)
        hue < 180f -> Triple(0f, c, x)
        hue < 240f -> Triple(0f, x, c)
        hue < 300f -> Triple(x, 0f, c)
        else -> Triple(c, 0f, x)
    }
    return Color(
        red = (rp + m).coerceIn(0f, 1f),
        green = (gp + m).coerceIn(0f, 1f),
        blue = (bp + m).coerceIn(0f, 1f),
        alpha = base.alpha,
    )
}

/**
 * Gera os tokens de cor para tema claro ou escuro a partir do estilo visual,
 * modo de origem de cor e eventual [ColorScheme] dinâmico do sistema (Android 12+).
 */
fun overviewColors(
    isDark: Boolean,
    accentColor: Color = AdwaitaPalette.Blue,
    visualStyle: VisualStyle = VisualStyle.GNOME,
    colorSourceMode: ColorSourceMode = ColorSourceMode.GNOME,
    dynamicScheme: ColorScheme? = null,
): OverviewColors {
    // No modo GNOME padrão sem cor dinâmica, preserva exatamente a paleta atual.
    if (visualStyle == VisualStyle.GNOME && colorSourceMode != ColorSourceMode.DYNAMIC) {
        val resolvedAccent = if (isDark && accentColor == AdwaitaPalette.Blue) {
            AdwaitaPalette.BlueDarkVariant
        } else {
            accentColor
        }
        val onAccent = if (isDark) {
            bestOnColor(resolvedAccent, lightOption = Color.White, darkOption = Color(0xFF101820))
        } else {
            bestOnColor(resolvedAccent, lightOption = Color.White, darkOption = Color(0xFF101820))
        }
        val accentContainer = if (isDark) {
            lerp(AdwaitaPalette.DarkSurfaceElevated, resolvedAccent, 0.24f)
        } else {
            lerp(AdwaitaPalette.LightSurface, resolvedAccent, 0.14f)
        }
        val onAccentContainer = if (isDark) {
            ensureWcagAa(AdwaitaPalette.DarkOnSurface, accentContainer)
        } else {
            ensureWcagAa(resolvedAccent, accentContainer)
        }

        return if (isDark) {
            OverviewColors(
                isDark = true,
                accent = resolvedAccent,
                onAccent = if (accentColor == AdwaitaPalette.Blue) Color(0xFF101820) else onAccent,
                accentContainer = accentContainer,
                onAccentContainer = onAccentContainer,
                secondaryAccent = resolvedAccent.copy(alpha = 0.82f),
                windowBackground = AdwaitaPalette.DarkWindowBackground,
                surface = AdwaitaPalette.DarkSurface,
                surfaceContainer = AdwaitaPalette.DarkSurfaceContainer,
                surfaceElevated = AdwaitaPalette.DarkSurfaceElevated,
                surfaceContainerHigh = Color(0xFF424242),
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
                onAccent = if (accentColor == AdwaitaPalette.Blue) Color.White else onAccent,
                accentContainer = accentContainer,
                onAccentContainer = onAccentContainer,
                secondaryAccent = resolvedAccent.copy(alpha = 0.85f),
                windowBackground = AdwaitaPalette.LightWindowBackground,
                surface = AdwaitaPalette.LightSurface,
                surfaceContainer = AdwaitaPalette.LightSurfaceContainer,
                surfaceElevated = AdwaitaPalette.LightSurfaceElevated,
                surfaceContainerHigh = Color(0xFFE0E0E0),
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

    // Dinâmico (Material You) no estilo GNOME ou Expressive
    if (colorSourceMode == ColorSourceMode.DYNAMIC && dynamicScheme != null) {
        val rawAccent = if (visualStyle == VisualStyle.EXPRESSIVE) {
            boostExpressiveAccent(dynamicScheme.primary, isDark)
        } else {
            dynamicScheme.primary
        }
        val windowBg = if (visualStyle == VisualStyle.EXPRESSIVE) {
            dynamicScheme.background
        } else if (isDark) {
            lerp(AdwaitaPalette.DarkWindowBackground, dynamicScheme.background, 0.35f)
        } else {
            lerp(AdwaitaPalette.LightWindowBackground, dynamicScheme.background, 0.35f)
        }
        val surface = if (visualStyle == VisualStyle.EXPRESSIVE) {
            dynamicScheme.surfaceContainerLow
        } else if (isDark) {
            lerp(AdwaitaPalette.DarkSurface, dynamicScheme.surfaceContainer, 0.35f)
        } else {
            lerp(AdwaitaPalette.LightSurface, dynamicScheme.surfaceContainerLow, 0.35f)
        }
        val surfaceContainer = if (visualStyle == VisualStyle.EXPRESSIVE) {
            dynamicScheme.surfaceContainer
        } else {
            surface
        }
        val surfaceElevated = if (visualStyle == VisualStyle.EXPRESSIVE) {
            dynamicScheme.surfaceContainerHigh
        } else if (isDark) {
            lerp(AdwaitaPalette.DarkSurfaceElevated, dynamicScheme.surfaceContainerHigh, 0.40f)
        } else {
            lerp(AdwaitaPalette.LightSurfaceElevated, dynamicScheme.surfaceContainerHigh, 0.40f)
        }
        val surfaceHigh = dynamicScheme.surfaceContainerHighest
        val onSurface = ensureWcagAa(dynamicScheme.onSurface, surfaceContainer, minRatio = 4.5f)
        val onSurfaceMuted = ensureWcagAa(dynamicScheme.onSurfaceVariant, surfaceContainer, minRatio = 4.5f)
        val onAccent = bestOnColor(rawAccent)
        val accentContainer = dynamicScheme.primaryContainer
        val onAccentContainer = ensureWcagAa(dynamicScheme.onPrimaryContainer, accentContainer, minRatio = 4.5f)

        return OverviewColors(
            isDark = isDark,
            accent = rawAccent,
            onAccent = onAccent,
            accentContainer = accentContainer,
            onAccentContainer = onAccentContainer,
            secondaryAccent = dynamicScheme.secondary,
            windowBackground = windowBg,
            surface = surface,
            surfaceContainer = surfaceContainer,
            surfaceElevated = surfaceElevated,
            surfaceContainerHigh = surfaceHigh,
            searchFieldBackground = surfaceElevated,
            onSurface = onSurface,
            onSurfaceMuted = onSurfaceMuted,
            hairlineBorder = dynamicScheme.outlineVariant.copy(alpha = if (isDark) 0.55f else 0.65f),
            dashBackground = windowBg.copy(alpha = if (isDark) 0.84f else 0.88f),
            overviewScrim = windowBg.copy(alpha = if (isDark) 0.76f else 0.72f),
            wallpaperForeground = AdwaitaPalette.WallpaperForeground,
            wallpaperShadow = AdwaitaPalette.WallpaperShadow,
        )
    }

    // Estilo Expressive com cor GNOME ou personalizada -> superfícies tonais calmas + destaque mais vivo
    val baseAccent = if (isDark && accentColor == AdwaitaPalette.Blue) {
        AdwaitaPalette.BlueDarkVariant
    } else {
        accentColor
    }
    val resolvedAccent = boostExpressiveAccent(baseAccent, isDark)
    val onAccent = bestOnColor(resolvedAccent)

    return if (isDark) {
        val windowBg = lerp(AdwaitaPalette.DarkWindowBackground, resolvedAccent, 0.06f)
        val surface = lerp(AdwaitaPalette.DarkSurface, resolvedAccent, 0.08f)
        val surfaceContainer = lerp(Color(0xFF2D2E33), resolvedAccent, 0.10f)
        val surfaceElevated = lerp(AdwaitaPalette.DarkSurfaceElevated, resolvedAccent, 0.13f)
        val surfaceHigh = lerp(Color(0xFF42444B), resolvedAccent, 0.16f)
        val accentContainer = lerp(AdwaitaPalette.DarkSurfaceElevated, resolvedAccent, 0.34f)
        val onAccentContainer = ensureWcagAa(Color.White, accentContainer, minRatio = 4.5f)
        val onSurface = ensureWcagAa(AdwaitaPalette.DarkOnSurface, surfaceContainer, minRatio = 4.5f)
        val onSurfaceMuted = ensureWcagAa(AdwaitaPalette.DarkOnSurfaceMuted, surfaceContainer, minRatio = 4.5f)

        OverviewColors(
            isDark = true,
            accent = resolvedAccent,
            onAccent = onAccent,
            accentContainer = accentContainer,
            onAccentContainer = onAccentContainer,
            secondaryAccent = lerp(resolvedAccent, Color.White, 0.25f),
            windowBackground = windowBg,
            surface = surface,
            surfaceContainer = surfaceContainer,
            surfaceElevated = surfaceElevated,
            surfaceContainerHigh = surfaceHigh,
            searchFieldBackground = surfaceElevated,
            onSurface = onSurface,
            onSurfaceMuted = onSurfaceMuted,
            hairlineBorder = lerp(AdwaitaPalette.DarkBorder.copy(alpha = 1f), resolvedAccent, 0.30f)
                .copy(alpha = 0.22f),
            dashBackground = windowBg.copy(alpha = 0.85f),
            overviewScrim = lerp(AdwaitaPalette.DarkOverviewScrim.copy(alpha = 1f), resolvedAccent, 0.08f)
                .copy(alpha = 0.76f),
            wallpaperForeground = AdwaitaPalette.WallpaperForeground,
            wallpaperShadow = AdwaitaPalette.WallpaperShadow,
        )
    } else {
        val windowBg = lerp(AdwaitaPalette.LightWindowBackground, resolvedAccent, 0.045f)
        val surface = lerp(AdwaitaPalette.LightSurface, resolvedAccent, 0.03f)
        val surfaceContainer = lerp(Color(0xFFF6F7FA), resolvedAccent, 0.055f)
        val surfaceElevated = lerp(AdwaitaPalette.LightSurfaceElevated, resolvedAccent, 0.085f)
        val surfaceHigh = lerp(Color(0xFFE1E4EA), resolvedAccent, 0.11f)
        val accentContainer = lerp(Color.White, resolvedAccent, 0.18f)
        val onAccentContainer = ensureWcagAa(resolvedAccent, accentContainer, minRatio = 4.5f)
        val onSurface = ensureWcagAa(AdwaitaPalette.LightOnSurface, surfaceContainer, minRatio = 4.5f)
        val onSurfaceMuted = ensureWcagAa(AdwaitaPalette.LightOnSurfaceMuted, surfaceContainer, minRatio = 4.5f)

        OverviewColors(
            isDark = false,
            accent = resolvedAccent,
            onAccent = onAccent,
            accentContainer = accentContainer,
            onAccentContainer = onAccentContainer,
            secondaryAccent = lerp(resolvedAccent, Color(0xFF1F2430), 0.15f),
            windowBackground = windowBg,
            surface = surface,
            surfaceContainer = surfaceContainer,
            surfaceElevated = surfaceElevated,
            surfaceContainerHigh = surfaceHigh,
            searchFieldBackground = surfaceElevated,
            onSurface = onSurface,
            onSurfaceMuted = onSurfaceMuted,
            hairlineBorder = lerp(Color(0xFF1F2430), resolvedAccent, 0.35f).copy(alpha = 0.14f),
            dashBackground = windowBg.copy(alpha = 0.89f),
            overviewScrim = lerp(AdwaitaPalette.LightOverviewScrim.copy(alpha = 1f), resolvedAccent, 0.05f)
                .copy(alpha = 0.72f),
            wallpaperForeground = AdwaitaPalette.WallpaperForeground,
            wallpaperShadow = AdwaitaPalette.WallpaperShadow,
        )
    }
}
