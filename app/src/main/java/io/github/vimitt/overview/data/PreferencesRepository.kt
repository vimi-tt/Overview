package io.github.vimitt.overview.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import io.github.vimitt.overview.domain.model.LauncherPreferences
import io.github.vimitt.overview.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.overviewDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "overview_preferences",
)

/**
 * Repositório de preferências persistidas com Jetpack DataStore.
 */
class PreferencesRepository(
    private val context: Context,
) {
    val preferences: Flow<LauncherPreferences> = context.overviewDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { prefs ->
            val themeMode = prefs[KEY_THEME_MODE]
                ?.let { raw -> runCatching { ThemeMode.valueOf(raw) }.getOrNull() }
                ?: ThemeMode.SYSTEM

            val accentColorArgb = prefs[KEY_ACCENT_COLOR]
                ?: LauncherPreferences.DEFAULT_ACCENT_COLOR_ARGB

            val blurRadiusDp = prefs[KEY_BLUR_RADIUS]
                ?: LauncherPreferences.DEFAULT_BLUR_RADIUS_DP

            val showClock = prefs[KEY_SHOW_CLOCK] ?: true

            val pinnedKeys = prefs[KEY_PINNED_DASH]
                ?.split(DASH_SEPARATOR)
                ?.filter { it.isNotBlank() }
                ?: emptyList()

            LauncherPreferences(
                themeMode = themeMode,
                accentColorArgb = accentColorArgb,
                overviewBlurRadiusDp = blurRadiusDp,
                showClock = showClock,
                pinnedDashKeys = pinnedKeys,
            )
        }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.overviewDataStore.edit { prefs ->
            prefs[KEY_THEME_MODE] = mode.name
        }
    }

    suspend fun setAccentColor(argb: Long) {
        context.overviewDataStore.edit { prefs ->
            prefs[KEY_ACCENT_COLOR] = argb
        }
    }

    suspend fun setOverviewBlurRadius(radiusDp: Float) {
        context.overviewDataStore.edit { prefs ->
            prefs[KEY_BLUR_RADIUS] = radiusDp.coerceIn(0f, 48f)
        }
    }

    suspend fun setShowClock(show: Boolean) {
        context.overviewDataStore.edit { prefs ->
            prefs[KEY_SHOW_CLOCK] = show
        }
    }

    suspend fun setPinnedDashKeys(keys: List<String>) {
        context.overviewDataStore.edit { prefs ->
            prefs[KEY_PINNED_DASH] = keys.take(LauncherPreferences.MAX_DASH_ITEMS)
                .joinToString(DASH_SEPARATOR)
        }
    }

    private companion object {
        val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        val KEY_ACCENT_COLOR = longPreferencesKey("accent_color_argb")
        val KEY_BLUR_RADIUS = floatPreferencesKey("overview_blur_radius_dp")
        val KEY_SHOW_CLOCK = booleanPreferencesKey("show_clock")
        val KEY_PINNED_DASH = stringPreferencesKey("pinned_dash_keys")
        const val DASH_SEPARATOR = "|"
    }
}
