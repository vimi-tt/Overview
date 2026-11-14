package io.github.vimitt.overview.ui.settings

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.github.vimitt.overview.data.AppsRepository
import io.github.vimitt.overview.data.PreferencesRepository
import io.github.vimitt.overview.domain.model.AppInfo
import io.github.vimitt.overview.domain.model.LauncherPreferences
import io.github.vimitt.overview.domain.model.ThemeMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@Immutable
data class SettingsUiState(
    val preferences: LauncherPreferences = LauncherPreferences(),
    val allApps: List<AppInfo> = emptyList(),
) {
    val isUsingAutoDash: Boolean
        get() = preferences.pinnedDashKeys.isEmpty()
}

/**
 * ViewModel da tela de personalização do Overview.
 */
class SettingsViewModel(
    appsRepository: AppsRepository,
    private val preferencesRepository: PreferencesRepository,
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = combine(
        preferencesRepository.preferences,
        appsRepository.apps,
    ) { prefs, apps ->
        SettingsUiState(
            preferences = prefs,
            allApps = apps,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SettingsUiState(),
    )

    fun onThemeModeChange(mode: ThemeMode) {
        viewModelScope.launch {
            preferencesRepository.setThemeMode(mode)
        }
    }

    fun onAccentColorChange(argb: Long) {
        viewModelScope.launch {
            preferencesRepository.setAccentColor(argb)
        }
    }

    fun onBlurRadiusChange(radiusDp: Float) {
        viewModelScope.launch {
            preferencesRepository.setOverviewBlurRadius(radiusDp)
        }
    }

    fun onShowClockChange(show: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setShowClock(show)
        }
    }

    fun onToggleDashApp(app: AppInfo) {
        viewModelScope.launch {
            val current = uiState.value.preferences.pinnedDashKeys.toMutableList()
            if (app.key in current) {
                current.remove(app.key)
            } else if (current.size < LauncherPreferences.MAX_DASH_ITEMS) {
                current.add(app.key)
            } else {
                return@launch
            }
            preferencesRepository.setPinnedDashKeys(current)
        }
    }

    fun onResetDashToAuto() {
        viewModelScope.launch {
            preferencesRepository.setPinnedDashKeys(emptyList())
        }
    }

    class Factory(
        private val appsRepository: AppsRepository,
        private val preferencesRepository: PreferencesRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SettingsViewModel(
                appsRepository = appsRepository,
                preferencesRepository = preferencesRepository,
            ) as T
        }
    }
}
