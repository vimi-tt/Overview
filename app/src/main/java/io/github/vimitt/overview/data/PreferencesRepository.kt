package io.github.vimitt.overview.data

import io.github.vimitt.overview.domain.model.LauncherPreferences
import io.github.vimitt.overview.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow

/**
 * Fachada de compatibilidade temporária sobre [SettingsRepository].
 */
class PreferencesRepository(
    private val settingsRepository: SettingsRepository,
) {
    val preferences: Flow<LauncherPreferences> = settingsRepository.preferences

    suspend fun setThemeMode(mode: ThemeMode) = settingsRepository.setThemeMode(mode)

    suspend fun setAccentColor(argb: Long) = settingsRepository.setAccentColor(argb)

    suspend fun setOverviewBlurRadius(radiusDp: Float) =
        settingsRepository.setOverviewBlurRadius(radiusDp)

    suspend fun setShowClock(show: Boolean) = settingsRepository.setShowClock(show)

    suspend fun setPinnedDashKeys(keys: List<String>) = settingsRepository.setPinnedDashKeys(keys)
}
