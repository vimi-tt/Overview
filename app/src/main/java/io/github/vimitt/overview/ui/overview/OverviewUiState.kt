package io.github.vimitt.overview.ui.overview

import androidx.compose.runtime.Immutable
import io.github.vimitt.overview.domain.model.AppInfo

/**
 * Estado imutável da tela de Overview (busca e grade de aplicativos).
 */
@Immutable
data class OverviewUiState(
    val searchQuery: String = "",
    val apps: List<AppInfo> = emptyList(),
) {
    val isSearching: Boolean
        get() = searchQuery.isNotBlank()
}
