package io.github.vimitt.overview.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import io.github.vimitt.overview.R
import io.github.vimitt.overview.domain.model.TypographyWeightMode
import io.github.vimitt.overview.domain.model.VisualStyle

@OptIn(ExperimentalTextApi::class)
private fun adwaitaVariableFont(
    resId: Int,
    weight: FontWeight,
    style: FontStyle,
): Font = Font(
    resId = resId,
    weight = weight,
    style = style,
    variationSettings = FontVariation.Settings(
        FontVariation.weight(weight.weight),
    ),
)

/**
 * Família tipográfica centralizada baseada na fonte variável oficial **Adwaita Sans** (OFL-1.1).
 */
val OverviewFontFamily: FontFamily = FontFamily(
    listOf(
        FontWeight.Thin,
        FontWeight.ExtraLight,
        FontWeight.Light,
        FontWeight.Normal,
        FontWeight.Medium,
        FontWeight.SemiBold,
        FontWeight.Bold,
        FontWeight.ExtraBold,
        FontWeight.Black,
    ).flatMap { weight ->
        listOf(
            adwaitaVariableFont(R.font.adwaita_sans_regular, weight, FontStyle.Normal),
            adwaitaVariableFont(R.font.adwaita_sans_italic, weight, FontStyle.Italic),
        )
    },
)

val OverviewTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = OverviewFontFamily,
        fontWeight = FontWeight.Light,
        fontSize = 54.sp,
        lineHeight = 60.sp,
        letterSpacing = (-0.5).sp,
    ),
    displayMedium = TextStyle(
        fontFamily = OverviewFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 42.sp,
        lineHeight = 48.sp,
        letterSpacing = (-0.25).sp,
    ),
    displaySmall = TextStyle(
        fontFamily = OverviewFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 34.sp,
        lineHeight = 40.sp,
    ),
    headlineLarge = TextStyle(
        fontFamily = OverviewFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = OverviewFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = OverviewFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
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
    titleSmall = TextStyle(
        fontFamily = OverviewFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
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
    bodySmall = TextStyle(
        fontFamily = OverviewFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = OverviewFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = OverviewFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = OverviewFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp,
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

    val resolvedTypography = OverviewTypography.copy(
        displayLarge = OverviewTypography.displayLarge.copy(
            fontWeight = displayWeight,
            fontSize = if (isExpressive) 56.sp else 54.sp,
            lineHeight = if (isExpressive) 64.sp else 60.sp,
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
            lineHeight = if (isExpressive) 23.sp else 22.sp,
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
