package io.github.vimitt.overview.ui.home

import android.content.ComponentName
import android.content.Context
import android.graphics.Rect
import android.view.View
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.github.vimitt.overview.data.AppWidgetController
import io.github.vimitt.overview.data.AppsRepository
import io.github.vimitt.overview.data.DefaultDashPackageProvider
import io.github.vimitt.overview.data.HomeWorkspaceRepository
import io.github.vimitt.overview.data.NotificationBadgeRepository
import io.github.vimitt.overview.data.SettingsRepository
import io.github.vimitt.overview.data.WidgetAppGroup
import io.github.vimitt.overview.domain.model.AppInfo
import io.github.vimitt.overview.domain.model.HomeItem
import io.github.vimitt.overview.domain.usecase.HomeGridPlacementUseCase
import io.github.vimitt.overview.domain.usecase.ResolveDefaultDashAppsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel responsável pela tela inicial, Dash de favoritos e grade de atalhos/widgets.
 */
class HomeViewModel(
    private val appContext: Context,
    private val appsRepository: AppsRepository,
    settingsRepository: SettingsRepository,
    notificationBadgeRepository: NotificationBadgeRepository,
    private val homeWorkspaceRepository: HomeWorkspaceRepository,
    private val appWidgetController: AppWidgetController,
    private val homeGridPlacementUseCase: HomeGridPlacementUseCase,
    private val defaultDashPackageProvider: DefaultDashPackageProvider,
    private val resolveDefaultDashAppsUseCase: ResolveDefaultDashAppsUseCase,
) : ViewModel() {

    private val preferredPackagesBySlot by lazy {
        defaultDashPackageProvider.resolvePreferredPackagesBySlot()
    }

    private val movingItemId = MutableStateFlow<String?>(null)

    val uiState: StateFlow<HomeUiState> = combine(
        appsRepository.apps,
        settingsRepository.preferences,
        homeWorkspaceRepository.items,
        movingItemId,
        notificationBadgeRepository.activeBadgePackages,
    ) { apps, preferences, rawItems, movingId, badgePackages ->
        val dashApps = resolveDefaultDashAppsUseCase(
            apps = apps,
            pinnedKeys = preferences.pinnedDashKeys,
            preferredPackagesBySlot = preferredPackagesBySlot,
            maxItems = preferences.dock.iconCount,
        )

        val appsByKey = apps.associateBy { it.key }
        val appsByPkgAndClass = apps.associateBy { "${it.packageName}/${it.activityName}" }
        val pm = appContext.packageManager

        val resolvedWorkspace = rawItems.mapNotNull { item ->
            when (item) {
                is HomeItem.AppShortcut -> {
                    val app = appsByKey[item.appKey]
                        ?: appsByPkgAndClass["${item.packageName}/${item.activityName}"]
                        ?: return@mapNotNull null
                    HomeWorkspaceItem.Shortcut(item = item, app = app)
                }

                is HomeItem.Widget -> {
                    val info = appWidgetController.findProviderInfo(
                        appWidgetId = item.appWidgetId,
                        providerPackage = item.providerPackage,
                        providerClass = item.providerClass,
                    )
                    val label = info?.loadLabel(pm).orEmpty()
                        .ifBlank { item.providerPackage }
                    HomeWorkspaceItem.Widget(
                        item = item,
                        providerInfo = info,
                        label = label,
                    )
                }
            }
        }

        val activeMovingId = if (
            !preferences.home.lockLayout &&
            movingId != null &&
            rawItems.any { it.id == movingId }
        ) {
            movingId
        } else {
            null
        }

        HomeUiState(
            dashApps = dashApps,
            workspaceItems = resolvedWorkspace,
            showClock = preferences.showClock,
            movingItemId = activeMovingId,
            homeSettings = preferences.home,
            dockSettings = preferences.dock,
            showNotificationDots = preferences.appearance.notificationDotsEnabled,
            badgePackages = badgePackages,
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

    fun addAppToHome(
        app: AppInfo,
        preferredCellX: Int? = null,
        preferredCellY: Int? = null,
        onResult: (Boolean) -> Unit = {},
    ) {
        val currentState = uiState.value
        if (currentState.lockLayout) {
            onResult(false)
            return
        }
        viewModelScope.launch {
            val added = homeWorkspaceRepository.addAppShortcut(
                app = app,
                preferredCellX = preferredCellX,
                preferredCellY = preferredCellY,
                columns = currentState.gridColumns,
                rows = currentState.gridRows,
            )
            onResult(added)
        }
    }

    fun canPlaceItemAt(
        cellX: Int,
        cellY: Int,
        spanX: Int,
        spanY: Int,
        ignoreItemId: String? = null,
    ): Boolean {
        val currentState = uiState.value
        if (currentState.lockLayout) return false
        val items = currentState.workspaceItems.map { it.item }
        return homeGridPlacementUseCase.canPlace(
            existingItems = items,
            cellX = cellX,
            cellY = cellY,
            spanX = spanX,
            spanY = spanY,
            ignoreItemId = ignoreItemId,
            columns = currentState.gridColumns,
            rows = currentState.gridRows,
        )
    }

    fun moveItemTo(itemId: String, cellX: Int, cellY: Int) {
        val currentState = uiState.value
        if (currentState.lockLayout) return
        viewModelScope.launch {
            val moved = homeWorkspaceRepository.moveItem(
                id = itemId,
                newCellX = cellX,
                newCellY = cellY,
                columns = currentState.gridColumns,
                rows = currentState.gridRows,
            )
            if (moved && movingItemId.value == itemId) {
                movingItemId.value = null
            }
        }
    }

    fun addWidgetToHome(
        appWidgetId: Int,
        provider: ComponentName,
        spanX: Int,
        spanY: Int,
        onResult: (Boolean) -> Unit = {},
    ) {
        val currentState = uiState.value
        if (currentState.lockLayout) {
            appWidgetController.deleteAppWidgetId(appWidgetId)
            onResult(false)
            return
        }
        viewModelScope.launch {
            val added = homeWorkspaceRepository.addWidget(
                appWidgetId = appWidgetId,
                provider = provider,
                desiredSpanX = spanX,
                desiredSpanY = spanY,
                columns = currentState.gridColumns,
                rows = currentState.gridRows,
            )
            if (!added) {
                appWidgetController.deleteAppWidgetId(appWidgetId)
            }
            onResult(added)
        }
    }

    fun startMovingItem(itemId: String) {
        if (uiState.value.lockLayout) return
        movingItemId.value = itemId
    }

    fun cancelMovingItem() {
        movingItemId.value = null
    }

    fun placeMovingItemAt(cellX: Int, cellY: Int) {
        val currentState = uiState.value
        if (currentState.lockLayout) return
        val targetId = movingItemId.value ?: return
        viewModelScope.launch {
            val moved = homeWorkspaceRepository.moveItem(
                id = targetId,
                newCellX = cellX,
                newCellY = cellY,
                columns = currentState.gridColumns,
                rows = currentState.gridRows,
            )
            if (moved) {
                movingItemId.value = null
            }
        }
    }

    fun canPlaceMovingItemAt(cellX: Int, cellY: Int): Boolean {
        val currentState = uiState.value
        if (currentState.lockLayout) return false
        val targetId = movingItemId.value ?: return false
        val items = currentState.workspaceItems.map { it.item }
        val moving = items.firstOrNull { it.id == targetId } ?: return false
        return homeGridPlacementUseCase.canPlace(
            existingItems = items,
            cellX = cellX,
            cellY = cellY,
            spanX = moving.spanX,
            spanY = moving.spanY,
            ignoreItemId = targetId,
            columns = currentState.gridColumns,
            rows = currentState.gridRows,
        )
    }

    fun resizeWidget(itemId: String, deltaSpanX: Int, deltaSpanY: Int) {
        val currentState = uiState.value
        if (currentState.lockLayout) return
        viewModelScope.launch {
            val widget = currentState.workspaceItems
                .firstOrNull { it.item.id == itemId }
                ?.item as? HomeItem.Widget
                ?: return@launch

            val nextX = (widget.spanX + deltaSpanX).coerceIn(1, currentState.gridColumns)
            val nextY = (widget.spanY + deltaSpanY).coerceIn(1, currentState.gridRows)
            if (nextX == widget.spanX && nextY == widget.spanY) return@launch

            homeWorkspaceRepository.resizeWidget(
                id = itemId,
                newSpanX = nextX,
                newSpanY = nextY,
                columns = currentState.gridColumns,
                rows = currentState.gridRows,
            )
        }
    }

    fun removeHomeItem(itemId: String) {
        if (uiState.value.lockLayout) return
        viewModelScope.launch {
            if (movingItemId.value == itemId) {
                movingItemId.value = null
            }
            val removed = homeWorkspaceRepository.removeItem(itemId)
            if (removed is HomeItem.Widget) {
                appWidgetController.deleteAppWidgetId(removed.appWidgetId)
            }
        }
    }

    suspend fun loadAvailableWidgets(): List<WidgetAppGroup> {
        return appWidgetController.loadInstalledWidgets()
    }

    class Factory(
        private val appContext: Context,
        private val appsRepository: AppsRepository,
        private val settingsRepository: SettingsRepository,
        private val notificationBadgeRepository: NotificationBadgeRepository,
        private val homeWorkspaceRepository: HomeWorkspaceRepository,
        private val appWidgetController: AppWidgetController,
        private val homeGridPlacementUseCase: HomeGridPlacementUseCase,
        private val defaultDashPackageProvider: DefaultDashPackageProvider,
        private val resolveDefaultDashAppsUseCase: ResolveDefaultDashAppsUseCase,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return HomeViewModel(
                appContext = appContext,
                appsRepository = appsRepository,
                settingsRepository = settingsRepository,
                notificationBadgeRepository = notificationBadgeRepository,
                homeWorkspaceRepository = homeWorkspaceRepository,
                appWidgetController = appWidgetController,
                homeGridPlacementUseCase = homeGridPlacementUseCase,
                defaultDashPackageProvider = defaultDashPackageProvider,
                resolveDefaultDashAppsUseCase = resolveDefaultDashAppsUseCase,
            ) as T
        }
    }
}
