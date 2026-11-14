package io.github.vimitt.overview.ui.overview

import android.graphics.Rect
import android.view.View
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.github.vimitt.overview.data.AppsRepository
import io.github.vimitt.overview.domain.model.AppInfo
import io.github.vimitt.overview.domain.usecase.FilterAppsUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn

/**
 * ViewModel responsável pelo Overview: filtragem em tempo real e abertura de apps.
 */
class OverviewViewModel(
    private val appsRepository: AppsRepository,
    private val filterAppsUseCase: FilterAppsUseCase,
) : ViewModel() {

    private val searchQuery = MutableStateFlow("")

    val uiState: StateFlow<OverviewUiState> = combine(
        appsRepository.apps,
        searchQuery,
    ) { apps, query ->
        OverviewUiState(
            searchQuery = query,
            apps = filterAppsUseCase(apps, query),
        )
    }
        .flowOn(Dispatchers.Default)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = OverviewUiState(),
        )

    fun onSearchQueryChange(query: String) {
        searchQuery.value = query
    }

    fun onClearSearch() {
        searchQuery.value = ""
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
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return OverviewViewModel(
                appsRepository = appsRepository,
                filterAppsUseCase = filterAppsUseCase,
            ) as T
        }
    }
}
