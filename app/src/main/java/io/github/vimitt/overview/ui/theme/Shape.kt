package io.github.vimitt.overview.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.RoundedPolygon
import io.github.vimitt.overview.domain.model.IconShape
import io.github.vimitt.overview.domain.model.ShapeFamily
import io.github.vimitt.overview.domain.model.VisualStyle
import io.github.vimitt.overview.ui.components.OverviewPolygonFactory
import io.github.vimitt.overview.ui.components.RoundedPolygonShape

/**
 * Raios padrão herdados do estilo Adwaita (mantidos para compatibilidade com o estilo GNOME).
 */
object OverviewCornerRadius {
    val Small = 10.dp
    val SettingsGroup = 14.dp
    val Medium = 16.dp
    val Large = 24.dp
    val Dash = 28.dp
    val Pill = 999.dp
}

val OverviewShapes = Shapes(
    extraSmall = RoundedCornerShape(OverviewCornerRadius.Small),
    small = RoundedCornerShape(OverviewCornerRadius.Small),
    medium = RoundedCornerShape(OverviewCornerRadius.Medium),
    large = RoundedCornerShape(OverviewCornerRadius.Large),
    extraLarge = RoundedCornerShape(OverviewCornerRadius.Dash),
)

/**
 * Tokens completos de forma expostos por [OverviewTheme.shapes].
 */
@Immutable
data class OverviewShapeTokens(
    val family: ShapeFamily,
    val iconShape: IconShape,
    val iconRoundness: Float,
    val smallRadius: Dp,
    val settingsGroupRadius: Dp,
    val mediumRadius: Dp,
    val largeRadius: Dp,
    val dashRadius: Dp,
    val pillRadius: Dp,
    val small: Shape,
    val settingsGroup: Shape,
    val medium: Shape,
    val large: Shape,
    val dash: Shape,
    val pill: Shape,
    val iconBasePolygon: RoundedPolygon,
    val iconPressedPolygon: RoundedPolygon,
    val iconPressMorph: Morph,
    val iconComposeShape: Shape,
    val iconPressedComposeShape: Shape,
    val materialShapes: Shapes,
)

/**
 * Gera os tokens de forma de acordo com [visualStyle], [shapeFamily], [iconShape] e [iconRoundness].
 */
fun overviewShapeTokens(
    visualStyle: VisualStyle = VisualStyle.GNOME,
    shapeFamily: ShapeFamily = ShapeFamily.ROUNDED,
    iconShape: IconShape = IconShape.ROUNDED_SQUARE,
    iconRoundness: Float = 0.5f,
): OverviewShapeTokens {
    val clampedRoundness = iconRoundness.coerceIn(0f, 1f)
    val basePolygon = OverviewPolygonFactory.iconPolygon(iconShape, clampedRoundness)
    val pressedPolygon = OverviewPolygonFactory.pressedTargetPolygon(iconShape, clampedRoundness)
    val pressMorph = Morph(basePolygon, pressedPolygon)
    val baseComposeShape = RoundedPolygonShape(basePolygon)
    val pressedComposeShape = RoundedPolygonShape(pressedPolygon)

    // No estilo GNOME clássico, preserva exatamente os raios originais do Adwaita.
    if (visualStyle == VisualStyle.GNOME && shapeFamily == ShapeFamily.ROUNDED) {
        return OverviewShapeTokens(
            family = shapeFamily,
            iconShape = iconShape,
            iconRoundness = clampedRoundness,
            smallRadius = OverviewCornerRadius.Small,
            settingsGroupRadius = OverviewCornerRadius.SettingsGroup,
            mediumRadius = OverviewCornerRadius.Medium,
            largeRadius = OverviewCornerRadius.Large,
            dashRadius = OverviewCornerRadius.Dash,
            pillRadius = OverviewCornerRadius.Pill,
            small = RoundedCornerShape(OverviewCornerRadius.Small),
            settingsGroup = RoundedCornerShape(OverviewCornerRadius.SettingsGroup),
            medium = RoundedCornerShape(OverviewCornerRadius.Medium),
            large = RoundedCornerShape(OverviewCornerRadius.Large),
            dash = RoundedCornerShape(OverviewCornerRadius.Dash),
            pill = RoundedCornerShape(OverviewCornerRadius.Pill),
            iconBasePolygon = basePolygon,
            iconPressedPolygon = pressedPolygon,
            iconPressMorph = pressMorph,
            iconComposeShape = baseComposeShape,
            iconPressedComposeShape = pressedComposeShape,
            materialShapes = OverviewShapes,
        )
    }

    val scale = when (shapeFamily) {
        ShapeFamily.SOFT -> 0.72f
        ShapeFamily.ROUNDED -> if (visualStyle == VisualStyle.EXPRESSIVE) 1.15f else 1.0f
        ShapeFamily.EXPRESSIVE -> if (visualStyle == VisualStyle.EXPRESSIVE) 1.38f else 1.20f
    }

    val smallR = (OverviewCornerRadius.Small.value * scale).dp
    val groupR = (OverviewCornerRadius.SettingsGroup.value * scale).dp
    val mediumR = (OverviewCornerRadius.Medium.value * scale).dp
    val largeR = (OverviewCornerRadius.Large.value * scale).dp
    val dashR = (OverviewCornerRadius.Dash.value * scale.coerceAtMost(1.25f)).dp

    val smallShape = RoundedCornerShape(smallR)
    val groupShape = RoundedCornerShape(groupR)
    val mediumShape = RoundedCornerShape(mediumR)
    val largeShape = RoundedCornerShape(largeR)
    val dashShape = RoundedCornerShape(dashR)
    val pillShape = RoundedCornerShape(OverviewCornerRadius.Pill)

    return OverviewShapeTokens(
        family = shapeFamily,
        iconShape = iconShape,
        iconRoundness = clampedRoundness,
        smallRadius = smallR,
        settingsGroupRadius = groupR,
        mediumRadius = mediumR,
        largeRadius = largeR,
        dashRadius = dashR,
        pillRadius = OverviewCornerRadius.Pill,
        small = smallShape,
        settingsGroup = groupShape,
        medium = mediumShape,
        large = largeShape,
        dash = dashShape,
        pill = pillShape,
        iconBasePolygon = basePolygon,
        iconPressedPolygon = pressedPolygon,
        iconPressMorph = pressMorph,
        iconComposeShape = baseComposeShape,
        iconPressedComposeShape = pressedComposeShape,
        materialShapes = Shapes(
            extraSmall = smallShape,
            small = smallShape,
            medium = mediumShape,
            large = largeShape,
            extraLarge = dashShape,
        ),
    )
}
