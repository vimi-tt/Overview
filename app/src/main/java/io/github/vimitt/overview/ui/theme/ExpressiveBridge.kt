package io.github.vimitt.overview.ui.theme

import android.content.Context
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import io.github.vimitt.overview.domain.model.VisualStyle

/**
 * Camada de isolamento para APIs de tema do Material 3 e `dynamicColorScheme` (Android 12+, minSdk 31).
 *
 * Na versão estável `1.4.0` do `androidx.compose.material3`, `MaterialExpressiveTheme` e
 * `MotionScheme` permanecem internos (a API pública expressiva ainda está na trilha `1.5.0-rc01`).
 * Por isso, este bridge aplica o `MaterialTheme` estável combinado aos tokens próprios do
 * `OverviewTheme` (`OverviewMotionTokens`, `OverviewShapeTokens`, `OverviewTypographyTokens`),
 * permitindo trocar para `MaterialExpressiveTheme` em um único arquivo quando `1.5.0` estabilizar.
 */
internal fun resolveDynamicColorScheme(
    context: Context,
    isDark: Boolean,
): ColorScheme = if (isDark) {
    dynamicDarkColorScheme(context)
} else {
    dynamicLightColorScheme(context)
}

@Composable
internal fun OverviewMaterialThemeBridge(
    @Suppress("UNUSED_PARAMETER") visualStyle: VisualStyle,
    colorScheme: ColorScheme,
    shapes: Shapes,
    typography: Typography,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = colorScheme,
        shapes = shapes,
        typography = typography,
        content = content,
    )
}
