package io.github.vimitt.overview.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import io.github.vimitt.overview.domain.model.TypographyWeightMode
import io.github.vimitt.overview.domain.model.VisualStyle

/**
 * Família tipográfica centralizada.
 *
 * Para usar a fonte Adwaita Sans (ou outra fonte variável customizada com licença compatível),
 * basta adicionar os arquivos `.ttf` em `res/font/` e substituir esta referência por `FontFamily(...)`.
 */
val OverviewFontFamily: FontFamily = FontFamily.SansSerif

val OverviewTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = OverviewFontFamily,
        fontWeight = FontWeight.Light,
        fontSize = 54.sp,
        lineHeight = 60.sp,
        letterSpacing = (-0.5).sp,
    ),
    titleLarge = TextStyle(
        fontFamily = OverviewFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = OverviewFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 22.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = OverviewFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 21.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = OverviewFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = OverviewFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    ),
)

/**
 * Tokens tipográficos centralizados que variam conforme [VisualStyle] e [TypographyWeightMode].
 */
@Immutable
data class OverviewTypographyTokens(
    val weightMode: TypographyWeightMode,
    val typography: Typography,
    val sectionHeader: TextStyle,
    val rowTitle: TextStyle,
    val topBarTitle: TextStyle,
)

fun overviewTypographyTokens(
    visualStyle: VisualStyle = VisualStyle.GNOME,
    weightMode: TypographyWeightMode = TypographyWeightMode.NORMAL,
): OverviewTypographyTokens {
    val isExpressive = visualStyle == VisualStyle.EXPRESSIVE
    val isEmphatic = weightMode == TypographyWeightMode.EMPHATIC

    if (!isExpressive && !isEmphatic) {
        return OverviewTypographyTokens(
            weightMode = weightMode,
            typography = OverviewTypography,
            sectionHeader = OverviewTypography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            rowTitle = OverviewTypography.titleMedium,
            topBarTitle = OverviewTypography.titleLarge,
        )
    }

    val titleLargeWeight = when {
        isExpressive && isEmphatic -> FontWeight.ExtraBold
        isEmphatic || isExpressive -> FontWeight.Bold
        else -> FontWeight.SemiBold
    }
    val titleMediumWeight = when {
        isEmphatic -> FontWeight.SemiBold
        isExpressive -> FontWeight.SemiBold
        else -> FontWeight.Medium
    }
    val displayWeight = when {
        isEmphatic -> FontWeight.SemiBold
        isExpressive -> FontWeight.Normal
        else -> FontWeight.Light
    }
    val labelWeight = if (isEmphatic || isExpressive) FontWeight.SemiBold else FontWeight.Medium

    val titleLargeSize = if (isExpressive) 23.sp else 21.sp
    val titleLargeLine = if (isExpressive) 29.sp else 27.sp
    val titleMediumSize = if (isExpressive) 16.5.sp else 16.sp
    val sectionHeaderSize = if (isExpressive) 13.sp else 12.sp

    val resolvedTypography = Typography(
        displayLarge = OverviewTypography.displayLarge.copy(
            fontWeight = displayWeight,
            fontSize = if (isExpressive) 56.sp else 54.sp,
            letterSpacing = if (isExpressive) (-0.8).sp else (-0.5).sp,
        ),
        headlineMedium = TextStyle(
            fontFamily = OverviewFontFamily,
            fontWeight = titleLargeWeight,
            fontSize = if (isExpressive) 26.sp else 24.sp,
            lineHeight = if (isExpressive) 32.sp else 30.sp,
        ),
        titleLarge = TextStyle(
            fontFamily = OverviewFontFamily,
            fontWeight = titleLargeWeight,
            fontSize = titleLargeSize,
            lineHeight = titleLargeLine,
            letterSpacing = if (isExpressive) (-0.25).sp else 0.sp,
        ),
        titleMedium = TextStyle(
            fontFamily = OverviewFontFamily,
            fontWeight = titleMediumWeight,
            fontSize = titleMediumSize,
            lineHeight = 22.sp,
        ),
        titleSmall = TextStyle(
            fontFamily = OverviewFontFamily,
            fontWeight = titleMediumWeight,
            fontSize = 14.sp,
            lineHeight = 20.sp,
        ),
        bodyLarge = OverviewTypography.bodyLarge,
        bodyMedium = OverviewTypography.bodyMedium,
        labelLarge = TextStyle(
            fontFamily = OverviewFontFamily,
            fontWeight = labelWeight,
            fontSize = 14.sp,
            lineHeight = 20.sp,
        ),
        labelMedium = OverviewTypography.labelMedium.copy(fontWeight = labelWeight),
    )

    return OverviewTypographyTokens(
        weightMode = weightMode,
        typography = resolvedTypography,
        sectionHeader = TextStyle(
            fontFamily = OverviewFontFamily,
            fontWeight = if (isEmphatic) FontWeight.Bold else FontWeight.SemiBold,
            fontSize = sectionHeaderSize,
            lineHeight = 18.sp,
            letterSpacing = if (isExpressive) 0.25.sp else 0.sp,
        ),
        rowTitle = resolvedTypography.titleMedium,
        topBarTitle = resolvedTypography.titleLarge,
    )
}
