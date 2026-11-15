package io.github.vimitt.overview.ui.overview

import androidx.compose.runtime.Immutable
import io.github.vimitt.overview.domain.model.AppInfo
import io.github.vimitt.overview.domain.model.SettingsDefaults

/**
 * Estado imutável da tela de Overview (busca e grade de aplicativos).
 */
@Immutable
data class OverviewUiState(
    val searchQuery: String = "",
    val apps: List<AppInfo> = emptyList(),
    val gridColumns: Int = SettingsDefaults.DEFAULT_APP_GRID_COLUMNS,
    val iconScale: Float = SettingsDefaults.DEFAULT_ICON_SCALE,
    val showNotificationDots: Boolean = false,
    val badgePackages: Set<String> = emptySet(),
) {
    val isSearching: Boolean
        get() = searchQuery.isNotBlank()
}
