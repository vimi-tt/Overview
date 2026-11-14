package io.github.vimitt.overview.ui.home

import android.graphics.Rect
import android.view.View
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.github.vimitt.overview.data.AppsRepository
import io.github.vimitt.overview.data.DefaultDashPackageProvider
import io.github.vimitt.overview.data.PreferencesRepository
import io.github.vimitt.overview.domain.model.AppInfo
import io.github.vimitt.overview.domain.usecase.ResolveDefaultDashAppsUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/**
 * ViewModel responsável pela tela inicial e pelo Dash de favoritos.
 */
class HomeViewModel(
    private val appsRepository: AppsRepository,
    preferencesRepository: PreferencesRepository,
    private val defaultDashPackageProvider: DefaultDashPackageProvider,
    private val resolveDefaultDashAppsUseCase: ResolveDefaultDashAppsUseCase,
) : ViewModel() {

    private val preferredPackagesBySlot by lazy {
        defaultDashPackageProvider.resolvePreferredPackagesBySlot()
    }

    val uiState: StateFlow<HomeUiState> = combine(
        appsRepository.apps,
        preferencesRepository.preferences,
    ) { apps, preferences ->
        val dashApps = resolveDefaultDashAppsUseCase(
            apps = apps,
            pinnedKeys = preferences.pinnedDashKeys,
            preferredPackagesBySlot = preferredPackagesBySlot,
        )
        HomeUiState(
            dashApps = dashApps,
            showClock = preferences.showClock,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState(),
    )

    fun onAppClick(
        app: AppInfo,
        sourceView: View?,
        sourceBounds: Rect?,
        reducedMotion: Boolean,
    ) {
        appsRepository.launchApp(
            app = app,
            sourceView = sourceView,
            sourceBounds = sourceBounds,
            reducedMotion = reducedMotion,
        )
    }

    class Factory(
        private val appsRepository: AppsRepository,
        private val preferencesRepository: PreferencesRepository,
        private val defaultDashPackageProvider: DefaultDashPackageProvider,
        private val resolveDefaultDashAppsUseCase: ResolveDefaultDashAppsUseCase,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return HomeViewModel(
                appsRepository = appsRepository,
                preferencesRepository = preferencesRepository,
                defaultDashPackageProvider = defaultDashPackageProvider,
                resolveDefaultDashAppsUseCase = resolveDefaultDashAppsUseCase,
            ) as T
        }
    }
}
