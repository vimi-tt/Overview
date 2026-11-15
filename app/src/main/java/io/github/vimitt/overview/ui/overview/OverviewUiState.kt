package io.github.vimitt.overview.ui.overview

import androidx.compose.runtime.Immutable
import io.github.vimitt.overview.domain.model.AppFolder
import io.github.vimitt.overview.domain.model.AppGridLayoutMode
import io.github.vimitt.overview.domain.model.AppInfo
import io.github.vimitt.overview.domain.model.FolderSettings
import io.github.vimitt.overview.domain.model.SearchSettings
import io.github.vimitt.overview.domain.model.SettingsDefaults

/**
 * Item exibido na grade paginada do Overview (aplicativo individual ou pasta).
 */
@Immutable
sealed interface OverviewGridEntry {
    val key: String
    val label: String

    @Immutable
    data class App(val app: AppInfo) : OverviewGridEntry {
        override val key: String get() = app.key
        override val label: String get() = app.label
    }

    @Immutable
    data class Folder(
        val folder: AppFolder,
        val apps: List<AppInfo>,
        val hasBadge: Boolean,
    ) : OverviewGridEntry {
        override val key: String get() = "folder_${folder.id}"
        override val label: String get() = folder.name
    }
}

/**
 * Estado imutável da tela de Overview (busca, pastas e grade de aplicativos).
 */
@Immutable
data class OverviewUiState(
    val searchQuery: String = "",
    val apps: List<AppInfo> = emptyList(),
    val gridEntries: List<OverviewGridEntry> = emptyList(),
    val gridColumns: Int = SettingsDefaults.DEFAULT_APP_GRID_COLUMNS,
    val gridLayoutMode: AppGridLayoutMode = AppGridLayoutMode.PAGED,
    val iconScale: Float = SettingsDefaults.DEFAULT_ICON_SCALE,
    val showNotificationDots: Boolean = false,
    val badgePackages: Set<String> = emptySet(),
    val searchSettings: SearchSettings = SearchSettings(),
    val folderSettings: FolderSettings = FolderSettings(),
) {
    val isSearching: Boolean
        get() = searchQuery.isNotBlank()
}
