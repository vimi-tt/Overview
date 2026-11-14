package io.github.vimitt.overview.domain.model

import androidx.compose.runtime.Immutable

/**
 * Modo de tema visual do launcher.
 */
enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
}

/**
 * Preferências persistidas do Overview.
 *
 * Mantém valores configuráveis para o MVP e já prepara campos
 * para a futura tela de personalização.
 */
@Immutable
data class LauncherPreferences(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val accentColorArgb: Long = DEFAULT_ACCENT_COLOR_ARGB,
    val overviewBlurRadiusDp: Float = DEFAULT_BLUR_RADIUS_DP,
    val showClock: Boolean = true,
    val pinnedDashKeys: List<String> = emptyList(),
) {
    companion object {
        const val DEFAULT_ACCENT_COLOR_ARGB: Long = 0xFF3584E4L
        const val DEFAULT_BLUR_RADIUS_DP: Float = 24f
        const val MAX_DASH_ITEMS: Int = 5
    }
}
