package io.github.vimitt.overview.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import io.github.vimitt.overview.domain.model.AppGridSettings
import io.github.vimitt.overview.domain.model.AppSortOrder
import io.github.vimitt.overview.domain.model.AppearanceSettings
import io.github.vimitt.overview.domain.model.DockSettings
import io.github.vimitt.overview.domain.model.HomeSettings
import io.github.vimitt.overview.domain.model.IconShape
import io.github.vimitt.overview.domain.model.LauncherPreferences
import io.github.vimitt.overview.domain.model.SettingsDefaults
import io.github.vimitt.overview.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.overviewSettingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "overview_preferences",
)

/**
 * Repositório central de configurações com DataStore (Preferences), expondo fluxos
 * tipados por categoria e aplicando validação centralizada dos limites.
 */
class SettingsRepository(
    private val context: Context,
) {
    private val rawData: Flow<Preferences> = context.overviewSettingsDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }

    val appearanceSettings: Flow<AppearanceSettings> = rawData
        .map { prefs -> mapAppearance(prefs) }
        .distinctUntilChanged()

    val homeSettings: Flow<HomeSettings> = rawData
        .map { prefs -> mapHome(prefs) }
        .distinctUntilChanged()

    val dockSettings: Flow<DockSettings> = rawData
        .map { prefs -> mapDock(prefs) }
        .distinctUntilChanged()

    val appGridSettings: Flow<AppGridSettings> = rawData
        .map { prefs -> mapAppGrid(prefs) }
        .distinctUntilChanged()

    val preferences: Flow<LauncherPreferences> = rawData
        .map { prefs ->
            LauncherPreferences(
                appearance = mapAppearance(prefs),
                home = mapHome(prefs),
                dock = mapDock(prefs),
                appGrid = mapAppGrid(prefs),
            )
        }
        .distinctUntilChanged()

    // region Aparência

    suspend fun setThemeMode(mode: ThemeMode) {
        context.overviewSettingsDataStore.edit { prefs ->
            prefs[KEY_THEME_MODE] = mode.name
        }
    }

    suspend fun setAccentColor(argb: Long) {
        context.overviewSettingsDataStore.edit { prefs ->
            prefs[KEY_ACCENT_COLOR] = argb
        }
    }

    suspend fun setIconPackPackage(packageName: String?) {
        context.overviewSettingsDataStore.edit { prefs ->
            if (packageName.isNullOrBlank()) {
                prefs.remove(KEY_ICON_PACK)
            } else {
                prefs[KEY_ICON_PACK] = packageName
            }
        }
    }

    suspend fun setIconShape(shape: IconShape) {
        context.overviewSettingsDataStore.edit { prefs ->
            prefs[KEY_ICON_SHAPE] = shape.name
        }
    }

    suspend fun setNotificationDotsEnabled(enabled: Boolean) {
        context.overviewSettingsDataStore.edit { prefs ->
            prefs[KEY_NOTIFICATION_DOTS] = enabled
        }
    }

    suspend fun setOverviewBlurRadius(radiusDp: Float) {
        val clamped = radiusDp.coerceIn(
            SettingsDefaults.MIN_BLUR_RADIUS_DP,
            SettingsDefaults.MAX_BLUR_RADIUS_DP,
        )
        context.overviewSettingsDataStore.edit { prefs ->
            prefs[KEY_BLUR_RADIUS] = clamped
        }
    }

    // endregion

    // region Tela inicial

    suspend fun setHomeGridColumns(columns: Int) {
        val clamped = columns.coerceIn(
            SettingsDefaults.MIN_GRID_COLUMNS,
            SettingsDefaults.MAX_GRID_COLUMNS,
        )
        context.overviewSettingsDataStore.edit { prefs ->
            prefs[KEY_HOME_GRID_COLUMNS] = clamped
        }
    }

    suspend fun setHomeGridRows(rows: Int) {
        val clamped = rows.coerceIn(
            SettingsDefaults.MIN_GRID_ROWS,
            SettingsDefaults.MAX_GRID_ROWS,
        )
        context.overviewSettingsDataStore.edit { prefs ->
            prefs[KEY_HOME_GRID_ROWS] = clamped
        }
    }

    suspend fun setHomeIconScale(scale: Float) {
        val clamped = scale.coerceIn(
            SettingsDefaults.MIN_ICON_SCALE,
            SettingsDefaults.MAX_ICON_SCALE,
        )
        context.overviewSettingsDataStore.edit { prefs ->
            prefs[KEY_HOME_ICON_SCALE] = clamped
        }
    }

    suspend fun setHomeShowLabels(show: Boolean) {
        context.overviewSettingsDataStore.edit { prefs ->
            prefs[KEY_HOME_SHOW_LABELS] = show
        }
    }

    suspend fun setHomeLabelScale(scale: Float) {
        val clamped = scale.coerceIn(
            SettingsDefaults.MIN_LABEL_SCALE,
            SettingsDefaults.MAX_LABEL_SCALE,
        )
        context.overviewSettingsDataStore.edit { prefs ->
            prefs[KEY_HOME_LABEL_SCALE] = clamped
        }
    }

    suspend fun setHomeLockLayout(locked: Boolean) {
        context.overviewSettingsDataStore.edit { prefs ->
            prefs[KEY_HOME_LOCK_LAYOUT] = locked
        }
    }

    suspend fun setShowClock(show: Boolean) {
        context.overviewSettingsDataStore.edit { prefs ->
            prefs[KEY_SHOW_CLOCK] = show
        }
    }

    // endregion

    // region Dock

    suspend fun setDockIconCount(count: Int) {
        val clamped = count.coerceIn(
            SettingsDefaults.MIN_DOCK_ICONS,
            SettingsDefaults.MAX_DOCK_ICONS,
        )
        context.overviewSettingsDataStore.edit { prefs ->
            prefs[KEY_DOCK_ICON_COUNT] = clamped
        }
    }

    suspend fun setDockBackgroundAlpha(alpha: Float) {
        val clamped = alpha.coerceIn(
            SettingsDefaults.MIN_DOCK_ALPHA,
            SettingsDefaults.MAX_DOCK_ALPHA,
        )
        context.overviewSettingsDataStore.edit { prefs ->
            prefs[KEY_DOCK_BG_ALPHA] = clamped
        }
    }

    suspend fun setDockShowSearchBar(show: Boolean) {
        context.overviewSettingsDataStore.edit { prefs ->
            prefs[KEY_DOCK_SHOW_SEARCH] = show
        }
    }

    suspend fun setPinnedDashKeys(keys: List<String>) {
        context.overviewSettingsDataStore.edit { prefs ->
            prefs[KEY_PINNED_DASH] = keys
                .distinct()
                .take(SettingsDefaults.MAX_DOCK_ICONS)
                .joinToString(DASH_SEPARATOR)
        }
    }

    // endregion

    // region Grade de apps

    suspend fun setAppGridColumns(columns: Int) {
        val clamped = columns.coerceIn(
            SettingsDefaults.MIN_GRID_COLUMNS,
            SettingsDefaults.MAX_GRID_COLUMNS,
        )
        context.overviewSettingsDataStore.edit { prefs ->
            prefs[KEY_APP_GRID_COLUMNS] = clamped
        }
    }

    suspend fun setAppGridIconScale(scale: Float) {
        val clamped = scale.coerceIn(
            SettingsDefaults.MIN_ICON_SCALE,
            SettingsDefaults.MAX_ICON_SCALE,
        )
        context.overviewSettingsDataStore.edit { prefs ->
            prefs[KEY_APP_GRID_ICON_SCALE] = clamped
        }
    }

    suspend fun setAppSortOrder(order: AppSortOrder) {
        context.overviewSettingsDataStore.edit { prefs ->
            prefs[KEY_APP_SORT_ORDER] = order.name
        }
    }

    suspend fun setAppHidden(appKey: String, hidden: Boolean) {
        context.overviewSettingsDataStore.edit { prefs ->
            val current = prefs[KEY_HIDDEN_APPS].orEmpty().toMutableSet()
            if (hidden) {
                current.add(appKey)
            } else {
                current.remove(appKey)
            }
            prefs[KEY_HIDDEN_APPS] = current
        }
    }

    suspend fun setShowHiddenInSearch(show: Boolean) {
        context.overviewSettingsDataStore.edit { prefs ->
            prefs[KEY_SHOW_HIDDEN_IN_SEARCH] = show
        }
    }

    // endregion

    private fun mapAppearance(prefs: Preferences): AppearanceSettings {
        val themeMode = prefs[KEY_THEME_MODE]
            ?.let { raw -> runCatching { ThemeMode.valueOf(raw) }.getOrNull() }
            ?: ThemeMode.SYSTEM

        val accentColorArgb = prefs[KEY_ACCENT_COLOR]
            ?: SettingsDefaults.DEFAULT_ACCENT_COLOR_ARGB

        val iconPack = prefs[KEY_ICON_PACK]?.takeIf { it.isNotBlank() }

        val iconShape = prefs[KEY_ICON_SHAPE]
            ?.let { raw -> runCatching { IconShape.valueOf(raw) }.getOrNull() }
            ?: IconShape.ROUNDED_SQUARE

        val notificationDots = prefs[KEY_NOTIFICATION_DOTS] ?: false

        val blurRadiusDp = (prefs[KEY_BLUR_RADIUS] ?: SettingsDefaults.DEFAULT_BLUR_RADIUS_DP)
            .coerceIn(SettingsDefaults.MIN_BLUR_RADIUS_DP, SettingsDefaults.MAX_BLUR_RADIUS_DP)

        return AppearanceSettings(
            themeMode = themeMode,
            accentColorArgb = accentColorArgb,
            iconPackPackage = iconPack,
            iconShape = iconShape,
            notificationDotsEnabled = notificationDots,
            overviewBlurRadiusDp = blurRadiusDp,
        )
    }

    private fun mapHome(prefs: Preferences): HomeSettings {
        val columns = (prefs[KEY_HOME_GRID_COLUMNS] ?: SettingsDefaults.DEFAULT_HOME_COLUMNS)
            .coerceIn(SettingsDefaults.MIN_GRID_COLUMNS, SettingsDefaults.MAX_GRID_COLUMNS)

        val rows = (prefs[KEY_HOME_GRID_ROWS] ?: SettingsDefaults.DEFAULT_HOME_ROWS)
            .coerceIn(SettingsDefaults.MIN_GRID_ROWS, SettingsDefaults.MAX_GRID_ROWS)

        val iconScale = (prefs[KEY_HOME_ICON_SCALE] ?: SettingsDefaults.DEFAULT_ICON_SCALE)
            .coerceIn(SettingsDefaults.MIN_ICON_SCALE, SettingsDefaults.MAX_ICON_SCALE)

        val showLabels = prefs[KEY_HOME_SHOW_LABELS] ?: true

        val labelScale = (prefs[KEY_HOME_LABEL_SCALE] ?: SettingsDefaults.DEFAULT_LABEL_SCALE)
            .coerceIn(SettingsDefaults.MIN_LABEL_SCALE, SettingsDefaults.MAX_LABEL_SCALE)

        val lockLayout = prefs[KEY_HOME_LOCK_LAYOUT] ?: false
        val showClock = prefs[KEY_SHOW_CLOCK] ?: true

        return HomeSettings(
            gridColumns = columns,
            gridRows = rows,
            iconScale = iconScale,
            showLabels = showLabels,
            labelScale = labelScale,
            lockLayout = lockLayout,
            showClock = showClock,
        )
    }

    private fun mapDock(prefs: Preferences): DockSettings {
        val iconCount = (prefs[KEY_DOCK_ICON_COUNT] ?: SettingsDefaults.DEFAULT_DOCK_ICONS)
            .coerceIn(SettingsDefaults.MIN_DOCK_ICONS, SettingsDefaults.MAX_DOCK_ICONS)

        val bgAlpha = (prefs[KEY_DOCK_BG_ALPHA] ?: SettingsDefaults.DEFAULT_DOCK_ALPHA)
            .coerceIn(SettingsDefaults.MIN_DOCK_ALPHA, SettingsDefaults.MAX_DOCK_ALPHA)

        val showSearch = prefs[KEY_DOCK_SHOW_SEARCH] ?: false

        val pinnedKeys = prefs[KEY_PINNED_DASH]
            ?.split(DASH_SEPARATOR)
            ?.filter { it.isNotBlank() }
            ?: emptyList()

        return DockSettings(
            iconCount = iconCount,
            backgroundAlpha = bgAlpha,
            showSearchBar = showSearch,
            pinnedKeys = pinnedKeys,
        )
    }

    private fun mapAppGrid(prefs: Preferences): AppGridSettings {
        val columns = (prefs[KEY_APP_GRID_COLUMNS] ?: SettingsDefaults.DEFAULT_APP_GRID_COLUMNS)
            .coerceIn(SettingsDefaults.MIN_GRID_COLUMNS, SettingsDefaults.MAX_GRID_COLUMNS)

        val iconScale = (prefs[KEY_APP_GRID_ICON_SCALE] ?: SettingsDefaults.DEFAULT_ICON_SCALE)
            .coerceIn(SettingsDefaults.MIN_ICON_SCALE, SettingsDefaults.MAX_ICON_SCALE)

        val sortOrder = prefs[KEY_APP_SORT_ORDER]
            ?.let { raw -> runCatching { AppSortOrder.valueOf(raw) }.getOrNull() }
            ?: AppSortOrder.ALPHABETICAL_ASC

        val hiddenKeys = prefs[KEY_HIDDEN_APPS].orEmpty()
        val showHiddenInSearch = prefs[KEY_SHOW_HIDDEN_IN_SEARCH] ?: false

        return AppGridSettings(
            columns = columns,
            iconScale = iconScale,
            sortOrder = sortOrder,
            hiddenAppKeys = hiddenKeys,
            showHiddenInSearch = showHiddenInSearch,
        )
    }

    private companion object {
        // Chaves já existentes (compatibilidade garantida com overview_preferences)
        val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        val KEY_ACCENT_COLOR = longPreferencesKey("accent_color_argb")
        val KEY_BLUR_RADIUS = floatPreferencesKey("overview_blur_radius_dp")
        val KEY_SHOW_CLOCK = booleanPreferencesKey("show_clock")
        val KEY_PINNED_DASH = stringPreferencesKey("pinned_dash_keys")
        const val DASH_SEPARATOR = "|"

        // Chaves de Aparência
        val KEY_ICON_PACK = stringPreferencesKey("icon_pack_package")
        val KEY_ICON_SHAPE = stringPreferencesKey("icon_shape")
        val KEY_NOTIFICATION_DOTS = booleanPreferencesKey("notification_dots_enabled")

        // Chaves de Tela inicial
        val KEY_HOME_GRID_COLUMNS = intPreferencesKey("home_grid_columns")
        val KEY_HOME_GRID_ROWS = intPreferencesKey("home_grid_rows")
        val KEY_HOME_ICON_SCALE = floatPreferencesKey("home_icon_scale")
        val KEY_HOME_SHOW_LABELS = booleanPreferencesKey("home_show_labels")
        val KEY_HOME_LABEL_SCALE = floatPreferencesKey("home_label_scale")
        val KEY_HOME_LOCK_LAYOUT = booleanPreferencesKey("home_lock_layout")

        // Chaves de Dock
        val KEY_DOCK_ICON_COUNT = intPreferencesKey("dock_icon_count")
        val KEY_DOCK_BG_ALPHA = floatPreferencesKey("dock_bg_alpha")
        val KEY_DOCK_SHOW_SEARCH = booleanPreferencesKey("dock_show_search")

        // Chaves de Grade de aplicativos
        val KEY_APP_GRID_COLUMNS = intPreferencesKey("app_grid_columns")
        val KEY_APP_GRID_ICON_SCALE = floatPreferencesKey("app_grid_icon_scale")
        val KEY_APP_SORT_ORDER = stringPreferencesKey("app_sort_order")
        val KEY_HIDDEN_APPS = stringSetPreferencesKey("hidden_app_keys")
        val KEY_SHOW_HIDDEN_IN_SEARCH = booleanPreferencesKey("show_hidden_in_search")
    }
}
