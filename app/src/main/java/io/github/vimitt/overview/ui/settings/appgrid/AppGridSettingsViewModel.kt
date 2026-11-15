package io.github.vimitt.overview.ui.settings.appgrid

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.github.vimitt.overview.data.AppsRepository
import io.github.vimitt.overview.data.SettingsRepository
import io.github.vimitt.overview.domain.model.AppGridLayoutMode
import io.github.vimitt.overview.domain.model.AppGridSettings
import io.github.vimitt.overview.domain.model.AppInfo
import io.github.vimitt.overview.domain.model.AppSortOrder
import io.github.vimitt.overview.domain.usecase.FilterAppsUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@Immutable
data class AppGridSettingsUiState(
    val settings: AppGridSettings = AppGridSettings(),
    val filteredAllApps: List<AppInfo> = emptyList(),
    val hiddenSearchQuery: String = "",
)

/**
 * ViewModel das subtelas Grade de apps e Aplicativos ocultos.
 */
class AppGridSettingsViewModel(
    appsRepository: AppsRepository,
    private val filterAppsUseCase: FilterAppsUseCase,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _hiddenSearchQuery = MutableStateFlow("")

    val uiState: StateFlow<AppGridSettingsUiState> = combine(
        settingsRepository.appGridSettings,
        appsRepository.apps,
        _hiddenSearchQuery,
    ) { gridSettings, apps, query ->
        AppGridSettingsUiState(
            settings = gridSettings,
            filteredAllApps = filterAppsUseCase(
                apps = apps,
                query = query,
                sortOrder = AppSortOrder.ALPHABETICAL_ASC,
                hiddenAppKeys = emptySet(),
                showHiddenInSearch = true,
            ),
            hiddenSearchQuery = query,
        )
    }
        .flowOn(Dispatchers.Default)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AppGridSettingsUiState(),
        )

    fun onColumnsChange(columns: Int) {
        viewModelScope.launch {
            settingsRepository.setAppGridColumns(columns)
        }
    }

    fun onIconScaleChange(scale: Float) {
        viewModelScope.launch {
            settingsRepository.setAppGridIconScale(scale)
        }
    }

    fun onLayoutModeChange(mode: AppGridLayoutMode) {
        viewModelScope.launch {
            settingsRepository.setAppGridLayoutMode(mode)
        }
    }

    fun onSortOrderChange(sortOrder: AppSortOrder) {
        viewModelScope.launch {
            settingsRepository.setAppSortOrder(sortOrder)
        }
    }

    fun onToggleAppHidden(appKey: String, hidden: Boolean) {
        viewModelScope.launch {
            settingsRepository.setAppHidden(appKey, hidden)
        }
    }

    fun onShowHiddenInSearchChange(show: Boolean) {
        viewModelScope.launch {
            settingsRepository.setShowHiddenInSearch(show)
        }
    }

    fun onHiddenSearchQueryChange(query: String) {
        _hiddenSearchQuery.value = query
    }

    fun onClearHiddenSearchQuery() {
        _hiddenSearchQuery.value = ""
    }

    class Factory(
        private val appsRepository: AppsRepository,
        private val filterAppsUseCase: FilterAppsUseCase,
        private val settingsRepository: SettingsRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AppGridSettingsViewModel(
                appsRepository = appsRepository,
                filterAppsUseCase = filterAppsUseCase,
                settingsRepository = settingsRepository,
            ) as T
        }
    }
}
