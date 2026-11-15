package io.github.vimitt.overview.ui.settings.dock

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.github.vimitt.overview.data.AppsRepository
import io.github.vimitt.overview.data.SettingsRepository
import io.github.vimitt.overview.domain.model.AppInfo
import io.github.vimitt.overview.domain.model.DockSettings
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@Immutable
data class DockSettingsUiState(
    val settings: DockSettings = DockSettings(),
    val allApps: List<AppInfo> = emptyList(),
) {
    val isUsingAutoDock: Boolean
        get() = settings.pinnedKeys.isEmpty()
}

/**
 * ViewModel da subtela Dock.
 */
class DockSettingsViewModel(
    appsRepository: AppsRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    val uiState: StateFlow<DockSettingsUiState> = combine(
        settingsRepository.dockSettings,
        appsRepository.apps,
    ) { dock, apps ->
        DockSettingsUiState(
            settings = dock,
            allApps = apps,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DockSettingsUiState(),
    )

    fun onIconCountChange(count: Int) {
        viewModelScope.launch {
            settingsRepository.setDockIconCount(count)
            val currentPinned = uiState.value.settings.pinnedKeys
            if (currentPinned.size > count) {
                settingsRepository.setPinnedDashKeys(currentPinned.take(count))
            }
        }
    }

    fun onBackgroundAlphaChange(alpha: Float) {
        viewModelScope.launch {
            settingsRepository.setDockBackgroundAlpha(alpha)
        }
    }

    fun onShowSearchBarChange(show: Boolean) {
        viewModelScope.launch {
            settingsRepository.setDockShowSearchBar(show)
        }
    }

    fun onTogglePinnedApp(app: AppInfo) {
        viewModelScope.launch {
            val state = uiState.value.settings
            val current = state.pinnedKeys.toMutableList()
            if (app.key in current) {
                current.remove(app.key)
            } else if (current.size < state.iconCount) {
                current.add(app.key)
            } else {
                return@launch
            }
            settingsRepository.setPinnedDashKeys(current)
        }
    }

    fun onResetToAuto() {
        viewModelScope.launch {
            settingsRepository.setPinnedDashKeys(emptyList())
        }
    }

    class Factory(
        private val appsRepository: AppsRepository,
        private val settingsRepository: SettingsRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DockSettingsViewModel(
                appsRepository = appsRepository,
                settingsRepository = settingsRepository,
            ) as T
        }
    }
}
