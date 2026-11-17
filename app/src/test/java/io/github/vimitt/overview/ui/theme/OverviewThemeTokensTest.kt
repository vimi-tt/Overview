package io.github.vimitt.overview.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.graphics.shapes.Morph
import io.github.vimitt.overview.domain.model.ColorSourceMode
import io.github.vimitt.overview.domain.model.IconShape
import io.github.vimitt.overview.domain.model.MotionIntensity
import io.github.vimitt.overview.domain.model.ShapeFamily
import io.github.vimitt.overview.domain.model.TypographyWeightMode
import io.github.vimitt.overview.domain.model.VisualStyle
import io.github.vimitt.overview.ui.components.OverviewPolygonFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OverviewThemeTokensTest {

    @Test
    fun `gnome style preserves exact baseline adwaita colors and radii`() {
        val light = overviewColors(isDark = false, visualStyle = VisualStyle.GNOME)
        val dark = overviewColors(isDark = true, visualStyle = VisualStyle.GNOME)

        assertEquals(AdwaitaPalette.LightWindowBackground, light.windowBackground)
        assertEquals(AdwaitaPalette.DarkWindowBackground, dark.windowBackground)
        assertEquals(AdwaitaPalette.Blue, light.accent)
        assertEquals(AdwaitaPalette.BlueDarkVariant, dark.accent)

        val shapes = overviewShapeTokens(visualStyle = VisualStyle.GNOME)
        assertEquals(OverviewCornerRadius.SettingsGroup, shapes.settingsGroupRadius)
        assertEquals(OverviewCornerRadius.Dash, shapes.dashRadius)
    }

    @Test
    fun `expressive colors maintain WCAG AA contrast on surfaces across all presets`() {
        for (isDark in listOf(false, true)) {
            for ((_, argb) in AdwaitaPalette.AccentPresets + AdwaitaPalette.CustomAccentPresets) {
                val colors = overviewColors(
                    isDark = isDark,
                    accentColor = Color(argb),
                    visualStyle = VisualStyle.EXPRESSIVE,
                    colorSourceMode = ColorSourceMode.CUSTOM,
                )
                assertTrue(
                    "onSurface WCAG AA failed for $argb (dark=$isDark)",
                    contrastRatio(colors.onSurface, colors.surfaceContainer) >= 4.5f,
                )
                assertTrue(
                    "onSurfaceMuted WCAG AA failed for $argb (dark=$isDark)",
                    contrastRatio(colors.onSurfaceMuted, colors.surfaceContainer) >= 4.5f,
                )
                assertTrue(
                    "onAccent WCAG AA large/UI contrast failed for $argb (dark=$isDark)",
                    contrastRatio(colors.onAccent, colors.accent) >= 3.0f,
                )
                assertTrue(
                    "onAccentContainer WCAG AA contrast failed for $argb (dark=$isDark)",
                    contrastRatio(colors.onAccentContainer, colors.accentContainer) >= 4.5f,
                )
                assertTrue(
                    "error WCAG AA contrast on surfaceContainer failed for $argb (dark=$isDark)",
                    contrastRatio(colors.error, colors.surfaceContainer) >= 4.5f,
                )
                assertTrue(
                    "onErrorContainer WCAG AA contrast failed for $argb (dark=$isDark)",
                    contrastRatio(colors.onErrorContainer, colors.errorContainer) >= 4.5f,
                )
                assertTrue(
                    "onTertiaryContainer WCAG AA contrast failed for $argb (dark=$isDark)",
                    contrastRatio(colors.onTertiaryContainer, colors.tertiaryContainer) >= 4.5f,
                )
                assertTrue(
                    "swatch icon bestOnColor WCAG AA non-text contrast failed for $argb",
                    contrastRatio(bestOnColor(Color(argb)), Color(argb)) >= 3.0f,
                )
            }
        }
    }

    @Test
    fun `motion tokens disable springs when reduced motion is active`() {
        val gnome = overviewMotionTokens(
            visualStyle = VisualStyle.GNOME,
            intensity = MotionIntensity.HIGH,
            reducedMotion = false,
        )
        assertFalse(gnome.usesSprings)

        val expressive = overviewMotionTokens(
            visualStyle = VisualStyle.EXPRESSIVE,
            intensity = MotionIntensity.HIGH,
            reducedMotion = false,
        )
        assertTrue(expressive.usesSprings)

        val expressiveReduced = overviewMotionTokens(
            visualStyle = VisualStyle.EXPRESSIVE,
            intensity = MotionIntensity.HIGH,
            reducedMotion = true,
        )
        assertFalse(expressiveReduced.usesSprings)
    }

    @Test
    fun `shape tokens and polygons cover all icon shapes and roundness range`() {
        for (shape in IconShape.entries) {
            for (roundness in listOf(0f, 0.5f, 1f)) {
                val base = OverviewPolygonFactory.iconPolygon(shape, roundness)
                val target = OverviewPolygonFactory.pressedTargetPolygon(shape, roundness)
                val morph = Morph(base, target)
                assertTrue(morph.asCubics(0.5f).isNotEmpty())
            }
        }

        val expressiveShapes = overviewShapeTokens(
            visualStyle = VisualStyle.EXPRESSIVE,
            shapeFamily = ShapeFamily.EXPRESSIVE,
        )
        assertTrue(expressiveShapes.settingsGroupRadius > OverviewCornerRadius.SettingsGroup)
    }

    @Test
    fun `typography tokens strengthen titles in expressive or emphatic mode`() {
        val normal = overviewTypographyTokens(VisualStyle.GNOME, TypographyWeightMode.NORMAL)
        val expressive = overviewTypographyTokens(VisualStyle.EXPRESSIVE, TypographyWeightMode.EMPHATIC)

        assertTrue(expressive.topBarTitle.fontSize > normal.topBarTitle.fontSize)
        assertTrue((expressive.topBarTitle.fontWeight?.weight ?: 0) > (normal.topBarTitle.fontWeight?.weight ?: 0))
    }

    @Test
    fun `embedded adwaita sans font family is applied across all typography tokens`() {
        for (style in VisualStyle.entries) {
            for (weightMode in TypographyWeightMode.entries) {
                val tokens = overviewTypographyTokens(style, weightMode)
                val t = tokens.typography
                assertEquals(OverviewFontFamily, t.displayLarge.fontFamily)
                assertEquals(OverviewFontFamily, t.displayMedium.fontFamily)
                assertEquals(OverviewFontFamily, t.displaySmall.fontFamily)
                assertEquals(OverviewFontFamily, t.headlineLarge.fontFamily)
                assertEquals(OverviewFontFamily, t.headlineMedium.fontFamily)
                assertEquals(OverviewFontFamily, t.headlineSmall.fontFamily)
                assertEquals(OverviewFontFamily, t.titleLarge.fontFamily)
                assertEquals(OverviewFontFamily, t.titleMedium.fontFamily)
                assertEquals(OverviewFontFamily, t.titleSmall.fontFamily)
                assertEquals(OverviewFontFamily, t.bodyLarge.fontFamily)
                assertEquals(OverviewFontFamily, t.bodyMedium.fontFamily)
                assertEquals(OverviewFontFamily, t.bodySmall.fontFamily)
                assertEquals(OverviewFontFamily, t.labelLarge.fontFamily)
                assertEquals(OverviewFontFamily, t.labelMedium.fontFamily)
                assertEquals(OverviewFontFamily, t.labelSmall.fontFamily)
                assertEquals(OverviewFontFamily, tokens.sectionHeader.fontFamily)
                assertEquals(OverviewFontFamily, tokens.rowTitle.fontFamily)
                assertEquals(OverviewFontFamily, tokens.topBarTitle.fontFamily)
            }
        }
    }

    @Test
    fun `every accent color preset has a localized string resource`() {
        val allPresets = AdwaitaPalette.AccentPresets + AdwaitaPalette.CustomAccentPresets
        for ((name, argb) in allPresets) {
            assertTrue(
                "Missing AccentPresetNameRes entry for $name ($argb)",
                AdwaitaPalette.AccentPresetNameRes.containsKey(argb),
            )
        }
    }
}
