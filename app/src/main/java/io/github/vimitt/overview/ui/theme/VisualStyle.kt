package io.github.vimitt.overview.ui.theme

import androidx.compose.runtime.Immutable
import io.github.vimitt.overview.domain.model.VisualStyle

/**
 * Tokens de variantes de componentes para diferenciar os estilos GNOME e Expressive
 * mantendo layout e hierarquia consistentes.
 */
@Immutable
data class OverviewComponentTokens(
    val style: VisualStyle,
    val isExpressive: Boolean,
    val useIconSwitch: Boolean,
    val useExpressiveSegmented: Boolean,
    val useExpressiveLoading: Boolean,
    val useShapeMorphOnPress: Boolean,
    val useTonalCardBorder: Boolean,
)

fun overviewComponentTokens(style: VisualStyle): OverviewComponentTokens {
    val expressive = style == VisualStyle.EXPRESSIVE
    return OverviewComponentTokens(
        style = style,
        isExpressive = expressive,
        useIconSwitch = expressive,
        useExpressiveSegmented = expressive,
        useExpressiveLoading = expressive,
        useShapeMorphOnPress = expressive,
        useTonalCardBorder = expressive,
    )
}
