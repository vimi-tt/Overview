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
import io.github.vimitt.overview.domain.model.AppSortOrder
import io.github.vimitt.overview.domain.model.FolderSettings
import io.github.vimitt.overview.domain.usecase.FilterAppsUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.Collator

/**
 * ViewModel responsável pelo Overview: filtragem em tempo real, ordenação,
 * pastas de aplicativos, visibilidade e abertura com animação.
 */
class OverviewViewModel(
    private val appsRepository: AppsRepository,
    private val filterAppsUseCase: FilterAppsUseCase,
    private val settingsRepository: SettingsRepository,
    notificationBadgeRepository: NotificationBadgeRepository,
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")

    /** Exposto de forma síncrona para evitar atrasos no campo de busca. */
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val uiState: StateFlow<OverviewUiState> = combine(
        appsRepository.apps,
        _searchQuery,
        settingsRepository.preferences,
        notificationBadgeRepository.activeBadgePackages,
    ) { apps, query, preferences, badges ->
        val gridSettings = preferences.appGrid
        val appearance = preferences.appearance
        val searchSettings = preferences.search
        val folderSettings = preferences.folders

        val filteredApps = filterAppsUseCase(
            apps = apps,
            query = query,
            sortOrder = gridSettings.sortOrder,
            hiddenAppKeys = gridSettings.hiddenAppKeys,
            showHiddenInSearch = gridSettings.showHiddenInSearch,
            matchPackageName = searchSettings.matchPackageName,
        )

        val entries = if (query.isBlank()) {
            buildOverviewGridEntries(
                visibleApps = filteredApps,
                folderSettings = folderSettings,
                sortOrder = gridSettings.sortOrder,
                showDots = appearance.notificationDotsEnabled,
                badgePackages = badges,
            )
        } else {
            emptyList()
        }

        OverviewUiState(
            searchQuery = query,
            apps = filteredApps,
            gridEntries = entries,
            gridColumns = gridSettings.columns,
            gridLayoutMode = gridSettings.layoutMode,
            iconScale = gridSettings.iconScale,
            showNotificationDots = appearance.notificationDotsEnabled,
            badgePackages = badges,
            searchSettings = searchSettings,
            folderSettings = folderSettings,
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

    fun moveAppToFolder(appKey: String, folderId: String) {
        viewModelScope.launch {
            settingsRepository.moveAppToFolder(appKey = appKey, targetFolderId = folderId)
        }
    }

    fun createFolderWithApp(name: String, appKey: String) {
        viewModelScope.launch {
            settingsRepository.saveFolder(
                folderId = null,
                name = name,
                appKeys = listOf(appKey),
            )
        }
    }

    fun removeAppFromFolder(appKey: String, folderId: String) {
        viewModelScope.launch {
            settingsRepository.removeAppFromFolder(appKey = appKey, folderId = folderId)
        }
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

/**
 * Monta a lista da grade paginada garantindo que todas as pastas apareçam sempre no começo,
 * seguidas pelos aplicativos soltos na ordenação configurada.
 */
internal fun buildOverviewGridEntries(
    visibleApps: List<AppInfo>,
    folderSettings: FolderSettings,
    sortOrder: AppSortOrder,
    showDots: Boolean,
    badgePackages: Set<String>,
): List<OverviewGridEntry> {
    if (folderSettings.folders.isEmpty()) {
        return visibleApps.map { OverviewGridEntry.App(it) }
    }

    val appsByKey = visibleApps.associateBy { it.key }
    val collator = Collator.getInstance().apply { strength = Collator.PRIMARY }
    val groupedKeys = mutableSetOf<String>()

    val folderEntries = folderSettings.folders.mapNotNull { folder ->
        val resolved = folder.appKeys.mapNotNull { key -> appsByKey[key] }
        if (resolved.isEmpty()) return@mapNotNull null

        for (app in resolved) {
            groupedKeys += app.key
        }

        val orderedInside = if (folderSettings.sortAppsInside) {
            resolved.sortedWith { a, b -> collator.compare(a.label, b.label) }
        } else {
            resolved
        }
        val hasBadge = showDots && orderedInside.any { it.packageName in badgePackages }
        OverviewGridEntry.Folder(
            folder = folder,
            apps = orderedInside,
            hasBadge = hasBadge,
        )
    }

    val orderedFolders = when (sortOrder) {
        AppSortOrder.ALPHABETICAL_DESC -> folderEntries.sortedWith { a, b ->
            collator.compare(b.label, a.label)
        }

        AppSortOrder.ALPHABETICAL_ASC,
        AppSortOrder.RECENTLY_INSTALLED,
        -> folderEntries.sortedWith { a, b ->
            collator.compare(a.label, b.label)
        }
    }

    val looseApps = visibleApps
        .filterNot { it.key in groupedKeys }
        .map { OverviewGridEntry.App(it) }

    return orderedFolders + looseApps
}
