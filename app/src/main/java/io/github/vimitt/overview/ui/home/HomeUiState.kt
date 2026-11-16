package io.github.vimitt.overview.ui.home

import android.appwidget.AppWidgetProviderInfo
import androidx.compose.runtime.Immutable
import io.github.vimitt.overview.domain.model.AppInfo
import io.github.vimitt.overview.domain.model.DockSettings
import io.github.vimitt.overview.domain.model.HomeClockStyle
import io.github.vimitt.overview.domain.model.HomeItem
import io.github.vimitt.overview.domain.model.HomeSettings
import io.github.vimitt.overview.domain.model.NativeWidgetsState

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

    @Immutable
    data class NativeWidget(
        override val item: HomeItem.NativeWidget,
        val label: String,
    ) : HomeWorkspaceItem
}

/**
 * Estado imutável da tela inicial (Home), refletindo em tempo real as configurações
 * de grade, dock, widgets nativos e emblemas de notificação.
 */
@Immutable
data class HomeUiState(
    val dashApps: List<AppInfo> = emptyList(),
    val workspaceItems: List<HomeWorkspaceItem> = emptyList(),
    val frequentApps: List<AppInfo> = emptyList(),
    val nativeWidgets: NativeWidgetsState = NativeWidgetsState(),
    val showClock: Boolean = true,
    val movingItemId: String? = null,
    val resizingItemId: String? = null,
    val homeSettings: HomeSettings = HomeSettings(),
    val dockSettings: DockSettings = DockSettings(),
    val showNotificationDots: Boolean = false,
    val badgePackages: Set<String> = emptySet(),
) {
    val gridColumns: Int get() = homeSettings.gridColumns
    val gridRows: Int get() = homeSettings.gridRows
    val iconScale: Float get() = homeSettings.iconScale
    val showLabels: Boolean get() = homeSettings.showLabels
    val labelScale: Float get() = homeSettings.labelScale
    val lockLayout: Boolean get() = homeSettings.lockLayout
    val clockStyle: HomeClockStyle get() = homeSettings.clockStyle
    val clockBackgroundAlpha: Float get() = homeSettings.clockBackgroundAlpha
    val dockBackgroundAlpha: Float get() = dockSettings.backgroundAlpha
    val showDockSearchBar: Boolean get() = dockSettings.showSearchBar
}
