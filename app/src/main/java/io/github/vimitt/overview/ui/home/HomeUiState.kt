package io.github.vimitt.overview.ui.home

import android.appwidget.AppWidgetProviderInfo
import androidx.compose.runtime.Immutable
import io.github.vimitt.overview.domain.model.AppInfo
import io.github.vimitt.overview.domain.model.HomeItem

/**
 * Representação pronta para exibição na grade da tela inicial.
 */
@Immutable
sealed interface HomeWorkspaceItem {
    val item: HomeItem

    @Immutable
    data class Shortcut(
        override val item: HomeItem.AppShortcut,
        val app: AppInfo,
    ) : HomeWorkspaceItem

    @Immutable
    data class Widget(
        override val item: HomeItem.Widget,
        val providerInfo: AppWidgetProviderInfo?,
        val label: String,
    ) : HomeWorkspaceItem
}

/**
 * Estado imutável da tela inicial (Home).
 */
@Immutable
data class HomeUiState(
    val dashApps: List<AppInfo> = emptyList(),
    val workspaceItems: List<HomeWorkspaceItem> = emptyList(),
    val showClock: Boolean = true,
    val movingItemId: String? = null,
)
