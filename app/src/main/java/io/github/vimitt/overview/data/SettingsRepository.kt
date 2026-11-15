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
import io.github.vimitt.overview.domain.model.AppFolder
import io.github.vimitt.overview.domain.model.AppGridLayoutMode
import io.github.vimitt.overview.domain.model.AppGridSettings
import io.github.vimitt.overview.domain.model.AppSortOrder
import io.github.vimitt.overview.domain.model.AppearanceSettings
import io.github.vimitt.overview.domain.model.ColorSourceMode
import io.github.vimitt.overview.domain.model.DockSettings
import io.github.vimitt.overview.domain.model.FolderPreviewStyle
import io.github.vimitt.overview.domain.model.FolderSettings
import io.github.vimitt.overview.domain.model.GestureSensitivity
import io.github.vimitt.overview.domain.model.GestureSettings
import io.github.vimitt.overview.domain.model.HomeClockStyle
import io.github.vimitt.overview.domain.model.HomeSettings
import io.github.vimitt.overview.domain.model.IconShape
import io.github.vimitt.overview.domain.model.LauncherGestureAction
import io.github.vimitt.overview.domain.model.LauncherPreferences
import io.github.vimitt.overview.domain.model.MotionIntensity
import io.github.vimitt.overview.domain.model.OverviewAnimationSpeed
import io.github.vimitt.overview.domain.model.OverviewSettings
import io.github.vimitt.overview.domain.model.OverviewTransitionStyle
import io.github.vimitt.overview.domain.model.SearchSettings
import io.github.vimitt.overview.domain.model.SettingsDefaults
import io.github.vimitt.overview.domain.model.ShapeFamily
import io.github.vimitt.overview.domain.model.ThemeMode
import io.github.vimitt.overview.domain.model.TypographyWeightMode
import io.github.vimitt.overview.domain.model.VisualStyle
import io.github.vimitt.overview.domain.model.WebSearchEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.UUID

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

    val overviewSettings: Flow<OverviewSettings> = rawData
        .map { prefs -> mapOverview(prefs) }
        .distinctUntilChanged()

    val searchSettings: Flow<SearchSettings> = rawData
        .map { prefs -> mapSearch(prefs) }
        .distinctUntilChanged()

    val folderSettings: Flow<FolderSettings> = rawData
        .map { prefs -> mapFolders(prefs) }
        .distinctUntilChanged()

    val gestureSettings: Flow<GestureSettings> = rawData
        .map { prefs -> mapGestures(prefs) }
        .distinctUntilChanged()

    val preferences: Flow<LauncherPreferences> = rawData
        .map { prefs ->
            LauncherPreferences(
                appearance = mapAppearance(prefs),
                home = mapHome(prefs),
                dock = mapDock(prefs),
                appGrid = mapAppGrid(prefs),
                overview = mapOverview(prefs),
                search = mapSearch(prefs),
                folders = mapFolders(prefs),
                gestures = mapGestures(prefs),
            )
        }
        .distinctUntilChanged()

    // region Aparência

    suspend fun setVisualStyle(style: VisualStyle) {
        context.overviewSettingsDataStore.edit { prefs ->
            prefs[KEY_VISUAL_STYLE] = style.name
        }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.overviewSettingsDataStore.edit { prefs ->
            prefs[KEY_THEME_MODE] = mode.name
        }
    }

    suspend fun setColorSourceMode(mode: ColorSourceMode) {
        context.overviewSettingsDataStore.edit { prefs ->
            prefs[KEY_COLOR_SOURCE_MODE] = mode.name
        }
    }

    suspend fun setAccentColor(argb: Long) {
        context.overviewSettingsDataStore.edit { prefs ->
            prefs[KEY_ACCENT_COLOR] = argb
        }
    }

    suspend fun setCustomAccentColor(argb: Long) {
        context.overviewSettingsDataStore.edit { prefs ->
            prefs[KEY_CUSTOM_ACCENT_COLOR] = argb
        }
    }

    suspend fun setMotionIntensity(intensity: MotionIntensity) {
        context.overviewSettingsDataStore.edit { prefs ->
            prefs[KEY_MOTION_INTENSITY] = intensity.name
        }
    }

    suspend fun setShapeFamily(family: ShapeFamily) {
        context.overviewSettingsDataStore.edit { prefs ->
            prefs[KEY_SHAPE_FAMILY] = family.name
        }
    }

    suspend fun setIconRoundness(roundness: Float) {
        val clamped = roundness.coerceIn(
            SettingsDefaults.MIN_ICON_ROUNDNESS,
            SettingsDefaults.MAX_ICON_ROUNDNESS,
        )
        context.overviewSettingsDataStore.edit { prefs ->
            prefs[KEY_ICON_ROUNDNESS] = clamped
        }
    }

    suspend fun setTypographyWeightMode(mode: TypographyWeightMode) {
        context.overviewSettingsDataStore.edit { prefs ->
            prefs[KEY_TYPOGRAPHY_WEIGHT_MODE] = mode.name
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

    // endregion

    // region Overview

    suspend fun setOverviewBlurEnabled(enabled: Boolean) {
        context.overviewSettingsDataStore.edit { prefs ->
            if (enabled) {
                val restored = (prefs[KEY_BLUR_LAST_NONZERO] ?: SettingsDefaults.DEFAULT_BLUR_RADIUS_DP)
                    .coerceIn(4f, SettingsDefaults.MAX_BLUR_RADIUS_DP)
                prefs[KEY_BLUR_RADIUS] = restored
            } else {
                val current = prefs[KEY_BLUR_RADIUS] ?: SettingsDefaults.DEFAULT_BLUR_RADIUS_DP
                if (current >= 1f) {
                    prefs[KEY_BLUR_LAST_NONZERO] = current
                }
                prefs[KEY_BLUR_RADIUS] = 0f
            }
        }
    }

    suspend fun setOverviewBlurRadius(radiusDp: Float) {
        val clamped = radiusDp.coerceIn(
            SettingsDefaults.MIN_BLUR_RADIUS_DP,
            SettingsDefaults.MAX_BLUR_RADIUS_DP,
        )
        context.overviewSettingsDataStore.edit { prefs ->
            prefs[KEY_BLUR_RADIUS] = clamped
            if (clamped >= 1f) {
                prefs[KEY_BLUR_LAST_NONZERO] = clamped
            }
        }
    }

    suspend fun setOverviewScrimAlpha(alpha: Float) {
        val clamped = alpha.coerceIn(
            SettingsDefaults.MIN_OVERVIEW_SCRIM_ALPHA,
            SettingsDefaults.MAX_OVERVIEW_SCRIM_ALPHA,
        )
        context.overviewSettingsDataStore.edit { prefs ->
            prefs[KEY_OVERVIEW_SCRIM_ALPHA] = clamped
        }
    }

    suspend fun setOverviewTransitionStyle(style: OverviewTransitionStyle) {
        context.overviewSettingsDataStore.edit { prefs ->
            prefs[KEY_OVERVIEW_TRANSITION_STYLE] = style.name
        }
    }

    suspend fun setOverviewAnimationSpeed(speed: OverviewAnimationSpeed) {
        context.overviewSettingsDataStore.edit { prefs ->
            prefs[KEY_OVERVIEW_ANIMATION_SPEED] = speed.name
        }
    }

    suspend fun setOverviewHomeDepthEffect(enabled: Boolean) {
        context.overviewSettingsDataStore.edit { prefs ->
            prefs[KEY_OVERVIEW_HOME_DEPTH] = enabled
        }
    }

    suspend fun setOverviewAppLaunchAnimation(enabled: Boolean) {
        context.overviewSettingsDataStore.edit { prefs ->
            prefs[KEY_OVERVIEW_APP_LAUNCH_ANIM] = enabled
        }
    }

    // endregion

    // region Busca

    suspend fun setSearchAutoFocusOnOpen(enabled: Boolean) {
        context.overviewSettingsDataStore.edit { prefs ->
            prefs[KEY_SEARCH_AUTO_FOCUS] = enabled
        }
    }

    suspend fun setSearchMatchPackageName(enabled: Boolean) {
        context.overviewSettingsDataStore.edit { prefs ->
            prefs[KEY_SEARCH_MATCH_PACKAGE] = enabled
        }
    }

    suspend fun setSearchClearOnClose(enabled: Boolean) {
        context.overviewSettingsDataStore.edit { prefs ->
            prefs[KEY_SEARCH_CLEAR_ON_CLOSE] = enabled
        }
    }

    suspend fun setSearchShowWebSuggestion(enabled: Boolean) {
        context.overviewSettingsDataStore.edit { prefs ->
            prefs[KEY_SEARCH_SHOW_WEB_SUGGESTION] = enabled
        }
    }

    suspend fun setSearchWebEngine(engine: WebSearchEngine) {
        context.overviewSettingsDataStore.edit { prefs ->
            prefs[KEY_SEARCH_WEB_ENGINE] = engine.name
        }
    }

    // endregion

    // region Pastas

    suspend fun setFolderPreviewStyle(style: FolderPreviewStyle) {
        context.overviewSettingsDataStore.edit { prefs ->
            prefs[KEY_FOLDER_PREVIEW_STYLE] = style.name
        }
    }

    suspend fun setFolderColumns(columns: Int) {
        val clamped = columns.coerceIn(
            SettingsDefaults.MIN_FOLDER_COLUMNS,
            SettingsDefaults.MAX_FOLDER_COLUMNS,
        )
        context.overviewSettingsDataStore.edit { prefs ->
            prefs[KEY_FOLDER_COLUMNS] = clamped
        }
    }

    suspend fun setFolderSortAppsInside(enabled: Boolean) {
        context.overviewSettingsDataStore.edit { prefs ->
            prefs[KEY_FOLDER_SORT_INSIDE] = enabled
        }
    }

    suspend fun setFolderCloseOnAppLaunch(enabled: Boolean) {
        context.overviewSettingsDataStore.edit { prefs ->
            prefs[KEY_FOLDER_CLOSE_ON_LAUNCH] = enabled
        }
    }

    suspend fun saveFolder(
        folderId: String?,
        name: String,
        appKeys: List<String>,
    ) {
        val cleanName = name.trim().ifBlank { return }
        val distinctKeys = appKeys.filter { it.isNotBlank() }.distinct()
        context.overviewSettingsDataStore.edit { prefs ->
            val current = decodeFolders(prefs[KEY_FOLDERS_JSON].orEmpty()).toMutableList()
            val targetId = folderId ?: UUID.randomUUID().toString()
            val keySet = distinctKeys.toSet()

            // Remove dos demais grupos para evitar duplicidade na grade.
            for (i in current.indices) {
                if (current[i].id != targetId) {
                    current[i] = current[i].copy(
                        appKeys = current[i].appKeys.filterNot { it in keySet },
                    )
                }
            }

            val existingIndex = current.indexOfFirst { it.id == targetId }
            val updated = AppFolder(id = targetId, name = cleanName, appKeys = distinctKeys)
            if (existingIndex >= 0) {
                current[existingIndex] = updated
            } else {
                current += updated
            }
            prefs[KEY_FOLDERS_JSON] = encodeFolders(current)
        }
    }

    suspend fun deleteFolder(folderId: String) {
        context.overviewSettingsDataStore.edit { prefs ->
            val current = decodeFolders(prefs[KEY_FOLDERS_JSON].orEmpty())
                .filterNot { it.id == folderId }
            prefs[KEY_FOLDERS_JSON] = encodeFolders(current)
        }
    }

    suspend fun moveAppToFolder(appKey: String, targetFolderId: String) {
        context.overviewSettingsDataStore.edit { prefs ->
            val current = decodeFolders(prefs[KEY_FOLDERS_JSON].orEmpty()).map { folder ->
                val cleaned = folder.appKeys.filterNot { it == appKey }
                if (folder.id == targetFolderId) {
                    folder.copy(appKeys = cleaned + appKey)
                } else {
                    folder.copy(appKeys = cleaned)
                }
            }
            prefs[KEY_FOLDERS_JSON] = encodeFolders(current)
        }
    }

    suspend fun removeAppFromFolder(appKey: String, folderId: String) {
        context.overviewSettingsDataStore.edit { prefs ->
            val current = decodeFolders(prefs[KEY_FOLDERS_JSON].orEmpty()).mapNotNull { folder ->
                if (folder.id == folderId) {
                    val remaining = folder.appKeys.filterNot { it == appKey }
                    if (remaining.isEmpty()) null else folder.copy(appKeys = remaining)
                } else {
                    folder
                }
            }
            prefs[KEY_FOLDERS_JSON] = encodeFolders(current)
        }
    }

    // endregion

    // region Gestos

    suspend fun setSwipeUpGesture(action: LauncherGestureAction) {
        context.overviewSettingsDataStore.edit { prefs ->
            prefs[KEY_GESTURE_SWIPE_UP] = action.name
        }
    }

    suspend fun setSwipeDownGesture(action: LauncherGestureAction) {
        context.overviewSettingsDataStore.edit { prefs ->
            prefs[KEY_GESTURE_SWIPE_DOWN] = action.name
        }
    }

    suspend fun setDoubleTapGesture(action: LauncherGestureAction) {
        context.overviewSettingsDataStore.edit { prefs ->
            prefs[KEY_GESTURE_DOUBLE_TAP] = action.name
        }
    }

    suspend fun setHomeButtonGesture(action: LauncherGestureAction) {
        context.overviewSettingsDataStore.edit { prefs ->
            prefs[KEY_GESTURE_HOME_BUTTON] = action.name
        }
    }

    suspend fun setGestureSensitivity(sensitivity: GestureSensitivity) {
        context.overviewSettingsDataStore.edit { prefs ->
            prefs[KEY_GESTURE_SENSITIVITY] = sensitivity.name
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

    suspend fun setHomeClockStyle(style: HomeClockStyle) {
        context.overviewSettingsDataStore.edit { prefs ->
            prefs[KEY_HOME_CLOCK_STYLE] = style.name
        }
    }

    suspend fun setHomeClockBackgroundAlpha(alpha: Float) {
        val clamped = alpha.coerceIn(
            SettingsDefaults.MIN_CLOCK_ALPHA,
            SettingsDefaults.MAX_CLOCK_ALPHA,
        )
        context.overviewSettingsDataStore.edit { prefs ->
            prefs[KEY_HOME_CLOCK_BG_ALPHA] = clamped
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

    suspend fun setAppGridLayoutMode(mode: AppGridLayoutMode) {
        context.overviewSettingsDataStore.edit { prefs ->
            prefs[KEY_APP_GRID_LAYOUT_MODE] = mode.name
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
        val visualStyle = prefs[KEY_VISUAL_STYLE]
            ?.let { raw -> runCatching { VisualStyle.valueOf(raw) }.getOrNull() }
            ?: VisualStyle.GNOME

        val themeMode = prefs[KEY_THEME_MODE]
            ?.let { raw -> runCatching { ThemeMode.valueOf(raw) }.getOrNull() }
            ?: ThemeMode.SYSTEM

        val colorSourceMode = prefs[KEY_COLOR_SOURCE_MODE]
            ?.let { raw -> runCatching { ColorSourceMode.valueOf(raw) }.getOrNull() }
            ?: ColorSourceMode.GNOME

        val accentColorArgb = prefs[KEY_ACCENT_COLOR]
            ?: SettingsDefaults.DEFAULT_ACCENT_COLOR_ARGB

        val customAccentColorArgb = prefs[KEY_CUSTOM_ACCENT_COLOR]
            ?: SettingsDefaults.DEFAULT_CUSTOM_ACCENT_COLOR_ARGB

        val motionIntensity = prefs[KEY_MOTION_INTENSITY]
            ?.let { raw -> runCatching { MotionIntensity.valueOf(raw) }.getOrNull() }
            ?: MotionIntensity.MEDIUM

        val shapeFamily = prefs[KEY_SHAPE_FAMILY]
            ?.let { raw -> runCatching { ShapeFamily.valueOf(raw) }.getOrNull() }
            ?: ShapeFamily.ROUNDED

        val iconRoundness = (prefs[KEY_ICON_ROUNDNESS] ?: SettingsDefaults.DEFAULT_ICON_ROUNDNESS)
            .coerceIn(SettingsDefaults.MIN_ICON_ROUNDNESS, SettingsDefaults.MAX_ICON_ROUNDNESS)

        val typographyWeightMode = prefs[KEY_TYPOGRAPHY_WEIGHT_MODE]
            ?.let { raw -> runCatching { TypographyWeightMode.valueOf(raw) }.getOrNull() }
            ?: TypographyWeightMode.NORMAL

        val iconPack = prefs[KEY_ICON_PACK]?.takeIf { it.isNotBlank() }

        val iconShape = prefs[KEY_ICON_SHAPE]
            ?.let { raw -> runCatching { IconShape.valueOf(raw) }.getOrNull() }
            ?: IconShape.ROUNDED_SQUARE

        val notificationDots = prefs[KEY_NOTIFICATION_DOTS] ?: false

        return AppearanceSettings(
            visualStyle = visualStyle,
            themeMode = themeMode,
            colorSourceMode = colorSourceMode,
            accentColorArgb = accentColorArgb,
            customAccentColorArgb = customAccentColorArgb,
            motionIntensity = motionIntensity,
            shapeFamily = shapeFamily,
            iconRoundness = iconRoundness,
            typographyWeightMode = typographyWeightMode,
            iconPackPackage = iconPack,
            iconShape = iconShape,
            notificationDotsEnabled = notificationDots,
        )
    }

    private fun mapOverview(prefs: Preferences): OverviewSettings {
        val blurRadiusDp = (prefs[KEY_BLUR_RADIUS] ?: SettingsDefaults.DEFAULT_BLUR_RADIUS_DP)
            .coerceIn(SettingsDefaults.MIN_BLUR_RADIUS_DP, SettingsDefaults.MAX_BLUR_RADIUS_DP)

        val scrimAlpha = (prefs[KEY_OVERVIEW_SCRIM_ALPHA] ?: SettingsDefaults.DEFAULT_OVERVIEW_SCRIM_ALPHA)
            .coerceIn(SettingsDefaults.MIN_OVERVIEW_SCRIM_ALPHA, SettingsDefaults.MAX_OVERVIEW_SCRIM_ALPHA)

        val transitionStyle = prefs[KEY_OVERVIEW_TRANSITION_STYLE]
            ?.let { raw -> runCatching { OverviewTransitionStyle.valueOf(raw) }.getOrNull() }
            ?: OverviewTransitionStyle.GNOME_SHELL

        val animationSpeed = prefs[KEY_OVERVIEW_ANIMATION_SPEED]
            ?.let { raw -> runCatching { OverviewAnimationSpeed.valueOf(raw) }.getOrNull() }
            ?: OverviewAnimationSpeed.NORMAL

        val homeDepthEffect = prefs[KEY_OVERVIEW_HOME_DEPTH] ?: true
        val appLaunchAnimation = prefs[KEY_OVERVIEW_APP_LAUNCH_ANIM] ?: true

        return OverviewSettings(
            blurRadiusDp = blurRadiusDp,
            scrimAlpha = scrimAlpha,
            transitionStyle = transitionStyle,
            animationSpeed = animationSpeed,
            homeDepthEffect = homeDepthEffect,
            appLaunchAnimation = appLaunchAnimation,
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
        val clockStyle = prefs[KEY_HOME_CLOCK_STYLE]?.let { saved ->
            runCatching { HomeClockStyle.valueOf(saved) }.getOrNull()
        } ?: HomeClockStyle.GNOME
        val clockBackgroundAlpha = (prefs[KEY_HOME_CLOCK_BG_ALPHA] ?: SettingsDefaults.DEFAULT_CLOCK_ALPHA)
            .coerceIn(SettingsDefaults.MIN_CLOCK_ALPHA, SettingsDefaults.MAX_CLOCK_ALPHA)

        return HomeSettings(
            gridColumns = columns,
            gridRows = rows,
            iconScale = iconScale,
            showLabels = showLabels,
            labelScale = labelScale,
            lockLayout = lockLayout,
            showClock = showClock,
            clockStyle = clockStyle,
            clockBackgroundAlpha = clockBackgroundAlpha,
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

        val layoutMode = prefs[KEY_APP_GRID_LAYOUT_MODE]
            ?.let { raw -> runCatching { AppGridLayoutMode.valueOf(raw) }.getOrNull() }
            ?: AppGridLayoutMode.PAGED

        val sortOrder = prefs[KEY_APP_SORT_ORDER]
            ?.let { raw -> runCatching { AppSortOrder.valueOf(raw) }.getOrNull() }
            ?: AppSortOrder.ALPHABETICAL_ASC

        val hiddenKeys = prefs[KEY_HIDDEN_APPS].orEmpty()
        val showHiddenInSearch = prefs[KEY_SHOW_HIDDEN_IN_SEARCH] ?: false

        return AppGridSettings(
            columns = columns,
            iconScale = iconScale,
            layoutMode = layoutMode,
            sortOrder = sortOrder,
            hiddenAppKeys = hiddenKeys,
            showHiddenInSearch = showHiddenInSearch,
        )
    }

    private fun mapSearch(prefs: Preferences): SearchSettings {
        val autoFocus = prefs[KEY_SEARCH_AUTO_FOCUS] ?: false
        val matchPkg = prefs[KEY_SEARCH_MATCH_PACKAGE] ?: false
        val clearOnClose = prefs[KEY_SEARCH_CLEAR_ON_CLOSE] ?: true
        val showWeb = prefs[KEY_SEARCH_SHOW_WEB_SUGGESTION] ?: true
        val webEngine = prefs[KEY_SEARCH_WEB_ENGINE]
            ?.let { raw -> runCatching { WebSearchEngine.valueOf(raw) }.getOrNull() }
            ?: WebSearchEngine.DUCKDUCKGO

        return SearchSettings(
            autoFocusOnOpen = autoFocus,
            matchPackageName = matchPkg,
            clearOnClose = clearOnClose,
            showWebSearchSuggestion = showWeb,
            webSearchEngine = webEngine,
        )
    }

    private fun mapFolders(prefs: Preferences): FolderSettings {
        val style = prefs[KEY_FOLDER_PREVIEW_STYLE]
            ?.let { raw -> runCatching { FolderPreviewStyle.valueOf(raw) }.getOrNull() }
            ?: FolderPreviewStyle.GRID_2X2
        val columns = (prefs[KEY_FOLDER_COLUMNS] ?: SettingsDefaults.DEFAULT_FOLDER_COLUMNS)
            .coerceIn(SettingsDefaults.MIN_FOLDER_COLUMNS, SettingsDefaults.MAX_FOLDER_COLUMNS)
        val sortInside = prefs[KEY_FOLDER_SORT_INSIDE] ?: true
        val closeOnLaunch = prefs[KEY_FOLDER_CLOSE_ON_LAUNCH] ?: true
        val folders = decodeFolders(prefs[KEY_FOLDERS_JSON].orEmpty())

        return FolderSettings(
            previewStyle = style,
            folderColumns = columns,
            sortAppsInside = sortInside,
            closeOnAppLaunch = closeOnLaunch,
            folders = folders,
        )
    }

    private fun mapGestures(prefs: Preferences): GestureSettings {
        val swipeUp = prefs[KEY_GESTURE_SWIPE_UP]
            ?.let { raw -> runCatching { LauncherGestureAction.valueOf(raw) }.getOrNull() }
            ?: LauncherGestureAction.OPEN_OVERVIEW
        val swipeDown = prefs[KEY_GESTURE_SWIPE_DOWN]
            ?.let { raw -> runCatching { LauncherGestureAction.valueOf(raw) }.getOrNull() }
            ?: LauncherGestureAction.EXPAND_NOTIFICATIONS
        val doubleTap = prefs[KEY_GESTURE_DOUBLE_TAP]
            ?.let { raw -> runCatching { LauncherGestureAction.valueOf(raw) }.getOrNull() }
            ?: LauncherGestureAction.NONE
        val homeButton = prefs[KEY_GESTURE_HOME_BUTTON]
            ?.let { raw -> runCatching { LauncherGestureAction.valueOf(raw) }.getOrNull() }
            ?: LauncherGestureAction.NONE
        val sensitivity = prefs[KEY_GESTURE_SENSITIVITY]
            ?.let { raw -> runCatching { GestureSensitivity.valueOf(raw) }.getOrNull() }
            ?: GestureSensitivity.MEDIUM

        return GestureSettings(
            swipeUpAction = swipeUp,
            swipeDownAction = swipeDown,
            doubleTapAction = doubleTap,
            homeButtonAction = homeButton,
            sensitivity = sensitivity,
        )
    }

    private fun encodeFolders(folders: List<AppFolder>): String {
        val array = JSONArray()
        for (folder in folders) {
            val keysArray = JSONArray()
            for (key in folder.appKeys) {
                keysArray.put(key)
            }
            val obj = JSONObject().apply {
                put("id", folder.id)
                put("name", folder.name)
                put("appKeys", keysArray)
            }
            array.put(obj)
        }
        return array.toString()
    }

    private fun decodeFolders(raw: String): List<AppFolder> {
        if (raw.isBlank()) return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList(array.length()) {
                for (i in 0 until array.length()) {
                    val obj = array.optJSONObject(i) ?: continue
                    val id = obj.optString("id").takeIf { it.isNotBlank() } ?: continue
                    val name = obj.optString("name").takeIf { it.isNotBlank() } ?: continue
                    val keysArray = obj.optJSONArray("appKeys")
                    val appKeys = buildList {
                        if (keysArray != null) {
                            for (j in 0 until keysArray.length()) {
                                val key = keysArray.optString(j)
                                if (key.isNotBlank()) add(key)
                            }
                        }
                    }
                    add(AppFolder(id = id, name = name, appKeys = appKeys))
                }
            }
        }.getOrDefault(emptyList())
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
        val KEY_VISUAL_STYLE = stringPreferencesKey("visual_style")
        val KEY_COLOR_SOURCE_MODE = stringPreferencesKey("color_source_mode")
        val KEY_CUSTOM_ACCENT_COLOR = longPreferencesKey("custom_accent_color_argb")
        val KEY_MOTION_INTENSITY = stringPreferencesKey("motion_intensity")
        val KEY_SHAPE_FAMILY = stringPreferencesKey("shape_family")
        val KEY_ICON_ROUNDNESS = floatPreferencesKey("icon_roundness")
        val KEY_TYPOGRAPHY_WEIGHT_MODE = stringPreferencesKey("typography_weight_mode")
        val KEY_ICON_PACK = stringPreferencesKey("icon_pack_package")
        val KEY_ICON_SHAPE = stringPreferencesKey("icon_shape")
        val KEY_NOTIFICATION_DOTS = booleanPreferencesKey("notification_dots_enabled")
        val KEY_BLUR_LAST_NONZERO = floatPreferencesKey("overview_blur_last_nonzero_dp")

        // Chaves de Tela inicial
        val KEY_HOME_GRID_COLUMNS = intPreferencesKey("home_grid_columns")
        val KEY_HOME_GRID_ROWS = intPreferencesKey("home_grid_rows")
        val KEY_HOME_ICON_SCALE = floatPreferencesKey("home_icon_scale")
        val KEY_HOME_SHOW_LABELS = booleanPreferencesKey("home_show_labels")
        val KEY_HOME_LABEL_SCALE = floatPreferencesKey("home_label_scale")
        val KEY_HOME_LOCK_LAYOUT = booleanPreferencesKey("home_lock_layout")
        val KEY_HOME_CLOCK_STYLE = stringPreferencesKey("home_clock_style")
        val KEY_HOME_CLOCK_BG_ALPHA = floatPreferencesKey("home_clock_bg_alpha")

        // Chaves de Dock
        val KEY_DOCK_ICON_COUNT = intPreferencesKey("dock_icon_count")
        val KEY_DOCK_BG_ALPHA = floatPreferencesKey("dock_bg_alpha")
        val KEY_DOCK_SHOW_SEARCH = booleanPreferencesKey("dock_show_search")

        // Chaves de Grade de aplicativos
        val KEY_APP_GRID_COLUMNS = intPreferencesKey("app_grid_columns")
        val KEY_APP_GRID_ICON_SCALE = floatPreferencesKey("app_grid_icon_scale")
        val KEY_APP_GRID_LAYOUT_MODE = stringPreferencesKey("app_grid_layout_mode")
        val KEY_APP_SORT_ORDER = stringPreferencesKey("app_sort_order")
        val KEY_HIDDEN_APPS = stringSetPreferencesKey("hidden_app_keys")
        val KEY_SHOW_HIDDEN_IN_SEARCH = booleanPreferencesKey("show_hidden_in_search")

        // Chaves de Overview
        val KEY_OVERVIEW_SCRIM_ALPHA = floatPreferencesKey("overview_scrim_alpha")
        val KEY_OVERVIEW_TRANSITION_STYLE = stringPreferencesKey("overview_transition_style")
        val KEY_OVERVIEW_ANIMATION_SPEED = stringPreferencesKey("overview_animation_speed")
        val KEY_OVERVIEW_HOME_DEPTH = booleanPreferencesKey("overview_home_depth_effect")
        val KEY_OVERVIEW_APP_LAUNCH_ANIM = booleanPreferencesKey("overview_app_launch_anim")

        // Chaves de Busca
        val KEY_SEARCH_AUTO_FOCUS = booleanPreferencesKey("search_auto_focus")
        val KEY_SEARCH_MATCH_PACKAGE = booleanPreferencesKey("search_match_package")
        val KEY_SEARCH_CLEAR_ON_CLOSE = booleanPreferencesKey("search_clear_on_close")
        val KEY_SEARCH_SHOW_WEB_SUGGESTION = booleanPreferencesKey("search_show_web_suggestion")
        val KEY_SEARCH_WEB_ENGINE = stringPreferencesKey("search_web_engine")

        // Chaves de Pastas
        val KEY_FOLDER_PREVIEW_STYLE = stringPreferencesKey("folder_preview_style")
        val KEY_FOLDER_COLUMNS = intPreferencesKey("folder_columns")
        val KEY_FOLDER_SORT_INSIDE = booleanPreferencesKey("folder_sort_inside")
        val KEY_FOLDER_CLOSE_ON_LAUNCH = booleanPreferencesKey("folder_close_on_launch")
        val KEY_FOLDERS_JSON = stringPreferencesKey("app_folders_json")

        // Chaves de Gestos
        val KEY_GESTURE_SWIPE_UP = stringPreferencesKey("gesture_swipe_up")
        val KEY_GESTURE_SWIPE_DOWN = stringPreferencesKey("gesture_swipe_down")
        val KEY_GESTURE_DOUBLE_TAP = stringPreferencesKey("gesture_double_tap")
        val KEY_GESTURE_HOME_BUTTON = stringPreferencesKey("gesture_home_button")
        val KEY_GESTURE_SENSITIVITY = stringPreferencesKey("gesture_sensitivity")
    }
}
