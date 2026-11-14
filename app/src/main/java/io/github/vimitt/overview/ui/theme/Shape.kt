package io.github.vimitt.overview.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Formas e raios de canto característicos do estilo Adwaita.
 */
object OverviewCornerRadius {
    val Small = 10.dp
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
