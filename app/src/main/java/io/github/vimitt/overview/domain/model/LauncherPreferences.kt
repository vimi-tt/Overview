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
 * Formato visual aplicado aos ícones de aplicativos.
 */
enum class IconShape {
    CIRCLE,
    ROUNDED_SQUARE,
    SQUARE,
}

/**
 * Critério de ordenação dos aplicativos na grade do Overview.
 */
enum class AppSortOrder {
    ALPHABETICAL_ASC,
    ALPHABETICAL_DESC,
    RECENTLY_INSTALLED,
}

/**
 * Estilo visual da transição entre a tela inicial e o Overview.
 */
enum class OverviewTransitionStyle {
    GNOME_SHELL,
    SLIDE,
    FADE,
}

/**
 * Multiplicador de duração aplicado às animações próprias do launcher.
 */
enum class OverviewAnimationSpeed(val durationScale: Float) {
    FAST(0.65f),
    NORMAL(1.0f),
    RELAXED(1.45f),
}

/**
 * Valores padrão e limites validados centralizados para todas as configurações do Overview.
 */
object SettingsDefaults {
    const val DEFAULT_ACCENT_COLOR_ARGB: Long = 0xFF3584E4L
    const val MIN_BLUR_RADIUS_DP: Float = 0f
    const val MAX_BLUR_RADIUS_DP: Float = 48f
    const val DEFAULT_BLUR_RADIUS_DP: Float = 14f

    const val MIN_GRID_COLUMNS: Int = 3
    const val MAX_GRID_COLUMNS: Int = 6
    const val MIN_GRID_ROWS: Int = 3
    const val MAX_GRID_ROWS: Int = 6
    const val DEFAULT_HOME_COLUMNS: Int = 4
    const val DEFAULT_HOME_ROWS: Int = 5

    const val MIN_ICON_SCALE: Float = 0.75f
    const val MAX_ICON_SCALE: Float = 1.35f
    const val DEFAULT_ICON_SCALE: Float = 1.0f

    const val MIN_LABEL_SCALE: Float = 0.75f
    const val MAX_LABEL_SCALE: Float = 1.30f
    const val DEFAULT_LABEL_SCALE: Float = 1.0f

    const val MIN_DOCK_ICONS: Int = 3
    const val MAX_DOCK_ICONS: Int = 6
    const val DEFAULT_DOCK_ICONS: Int = 5

    const val MIN_DOCK_ALPHA: Float = 0f
    const val MAX_DOCK_ALPHA: Float = 1f
    const val DEFAULT_DOCK_ALPHA: Float = 0.85f

    const val DEFAULT_APP_GRID_COLUMNS: Int = 4

    const val MIN_OVERVIEW_SCRIM_ALPHA: Float = 0.25f
    const val MAX_OVERVIEW_SCRIM_ALPHA: Float = 0.95f
    const val DEFAULT_OVERVIEW_SCRIM_ALPHA: Float = 0.70f
}

/**
 * Preferências da categoria Aparência (tema, destaque e ícones).
 */
@Immutable
data class AppearanceSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val accentColorArgb: Long = SettingsDefaults.DEFAULT_ACCENT_COLOR_ARGB,
    val iconPackPackage: String? = null,
    val iconShape: IconShape = IconShape.ROUNDED_SQUARE,
    val notificationDotsEnabled: Boolean = false,
)

/**
 * Preferências da categoria Overview (desfoque, fundo e animações).
 */
@Immutable
data class OverviewSettings(
    val blurRadiusDp: Float = SettingsDefaults.DEFAULT_BLUR_RADIUS_DP,
    val scrimAlpha: Float = SettingsDefaults.DEFAULT_OVERVIEW_SCRIM_ALPHA,
    val transitionStyle: OverviewTransitionStyle = OverviewTransitionStyle.GNOME_SHELL,
    val animationSpeed: OverviewAnimationSpeed = OverviewAnimationSpeed.NORMAL,
    val homeDepthEffect: Boolean = true,
    val appLaunchAnimation: Boolean = true,
)

/**
 * Preferências da categoria Tela inicial.
 */
@Immutable
data class HomeSettings(
    val gridColumns: Int = SettingsDefaults.DEFAULT_HOME_COLUMNS,
    val gridRows: Int = SettingsDefaults.DEFAULT_HOME_ROWS,
    val iconScale: Float = SettingsDefaults.DEFAULT_ICON_SCALE,
    val showLabels: Boolean = true,
    val labelScale: Float = SettingsDefaults.DEFAULT_LABEL_SCALE,
    val lockLayout: Boolean = false,
    val showClock: Boolean = true,
)

/**
 * Preferências da categoria Dock (Dash).
 */
@Immutable
data class DockSettings(
    val iconCount: Int = SettingsDefaults.DEFAULT_DOCK_ICONS,
    val backgroundAlpha: Float = SettingsDefaults.DEFAULT_DOCK_ALPHA,
    val showSearchBar: Boolean = false,
    val pinnedKeys: List<String> = emptyList(),
)

/**
 * Preferências da categoria Grade de aplicativos.
 */
@Immutable
data class AppGridSettings(
    val columns: Int = SettingsDefaults.DEFAULT_APP_GRID_COLUMNS,
    val iconScale: Float = SettingsDefaults.DEFAULT_ICON_SCALE,
    val sortOrder: AppSortOrder = AppSortOrder.ALPHABETICAL_ASC,
    val hiddenAppKeys: Set<String> = emptySet(),
    val showHiddenInSearch: Boolean = false,
)

/**
 * Agregado completo das preferências persistidas do Overview.
 */
@Immutable
data class LauncherPreferences(
    val appearance: AppearanceSettings = AppearanceSettings(),
    val home: HomeSettings = HomeSettings(),
    val dock: DockSettings = DockSettings(),
    val appGrid: AppGridSettings = AppGridSettings(),
    val overview: OverviewSettings = OverviewSettings(),
) {
    val themeMode: ThemeMode get() = appearance.themeMode
    val accentColorArgb: Long get() = appearance.accentColorArgb
    val overviewBlurRadiusDp: Float get() = overview.blurRadiusDp
    val showClock: Boolean get() = home.showClock
    val pinnedDashKeys: List<String> get() = dock.pinnedKeys

    companion object {
        const val DEFAULT_ACCENT_COLOR_ARGB: Long = SettingsDefaults.DEFAULT_ACCENT_COLOR_ARGB
        const val DEFAULT_BLUR_RADIUS_DP: Float = SettingsDefaults.DEFAULT_BLUR_RADIUS_DP
        const val MAX_DASH_ITEMS: Int = SettingsDefaults.MAX_DOCK_ICONS
    }
}
