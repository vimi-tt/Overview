package io.github.vimitt.overview.ui.overview

import android.graphics.Rect
import android.view.View
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.github.vimitt.overview.data.AppsRepository
import io.github.vimitt.overview.data.NotificationBadgeRepository
import io.github.vimitt.overview.data.SettingsRepository
import io.github.vimitt.overview.domain.model.AppInfo
import io.github.vimitt.overview.domain.usecase.FilterAppsUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn

/**
 * ViewModel responsável pelo Overview: filtragem em tempo real, ordenação,
 * visibilidade de aplicativos e abertura com animação.
 */
class OverviewViewModel(
    private val appsRepository: AppsRepository,
    private val filterAppsUseCase: FilterAppsUseCase,
    settingsRepository: SettingsRepository,
    notificationBadgeRepository: NotificationBadgeRepository,
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")

    /** Exposto de forma síncrona para evitar atrasos no campo de busca. */
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val uiState: StateFlow<OverviewUiState> = combine(
        appsRepository.apps,
        _searchQuery,
        settingsRepository.appGridSettings,
        settingsRepository.appearanceSettings,
        notificationBadgeRepository.activeBadgePackages,
    ) { apps, query, gridSettings, appearance, badges ->
        OverviewUiState(
            searchQuery = query,
            apps = filterAppsUseCase(
                apps = apps,
                query = query,
                sortOrder = gridSettings.sortOrder,
                hiddenAppKeys = gridSettings.hiddenAppKeys,
                showHiddenInSearch = gridSettings.showHiddenInSearch,
            ),
            gridColumns = gridSettings.columns,
            iconScale = gridSettings.iconScale,
            showNotificationDots = appearance.notificationDotsEnabled,
            badgePackages = badges,
        )
    }
        .flowOn(Dispatchers.Default)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = OverviewUiState(),
        )

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onClearSearch() {
        _searchQuery.value = ""
    }

    fun onAppClick(
        app: AppInfo,
        sourceView: View?,
        sourceBounds: Rect?,
        reducedMotion: Boolean,
    ): Boolean {
        return appsRepository.launchApp(
            app = app,
            sourceView = sourceView,
            sourceBounds = sourceBounds,
            reducedMotion = reducedMotion,
        )
    }

    class Factory(
        private val appsRepository: AppsRepository,
        private val filterAppsUseCase: FilterAppsUseCase,
        private val settingsRepository: SettingsRepository,
        private val notificationBadgeRepository: NotificationBadgeRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return OverviewViewModel(
                appsRepository = appsRepository,
                filterAppsUseCase = filterAppsUseCase,
                settingsRepository = settingsRepository,
                notificationBadgeRepository = notificationBadgeRepository,
            ) as T
        }
    }
}
