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
import io.github.vimitt.overview.data.NativeWidgetSystemProviders
import io.github.vimitt.overview.data.NativeWidgetsRepository
import io.github.vimitt.overview.data.NotificationBadgeRepository
import io.github.vimitt.overview.data.SettingsRepository
import io.github.vimitt.overview.data.WidgetAppGroup
import io.github.vimitt.overview.domain.model.AppInfo
import io.github.vimitt.overview.domain.model.ClockToolMode
import io.github.vimitt.overview.domain.model.HomeItem
import io.github.vimitt.overview.domain.model.NativeWidgetType
import io.github.vimitt.overview.domain.model.WeatherLocation
import io.github.vimitt.overview.domain.usecase.HomeGridPlacementUseCase
import io.github.vimitt.overview.domain.usecase.ResolveDefaultDashAppsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel responsável pela tela inicial, Dash de favoritos, widgets nativos e grade de atalhos/widgets.
 */
class HomeViewModel(
    private val appContext: Context,
    private val appsRepository: AppsRepository,
    private val settingsRepository: SettingsRepository,
    notificationBadgeRepository: NotificationBadgeRepository,
    private val homeWorkspaceRepository: HomeWorkspaceRepository,
    private val appWidgetController: AppWidgetController,
    private val nativeWidgetsRepository: NativeWidgetsRepository,
    private val homeGridPlacementUseCase: HomeGridPlacementUseCase,
    private val defaultDashPackageProvider: DefaultDashPackageProvider,
    private val resolveDefaultDashAppsUseCase: ResolveDefaultDashAppsUseCase,
) : ViewModel() {

    private val preferredPackagesBySlot by lazy {
        defaultDashPackageProvider.resolvePreferredPackagesBySlot()
    }

    private val movingItemId = MutableStateFlow<String?>(null)
    private val resizingItemId = MutableStateFlow<String?>(null)

    val uiState: StateFlow<HomeUiState> = combine(
        appsRepository.apps,
        settingsRepository.preferences,
        homeWorkspaceRepository.items,
        nativeWidgetsRepository.state,
        combine(
            movingItemId,
            resizingItemId,
            notificationBadgeRepository.activeBadgePackages,
            ::Triple,
        ),
    ) { apps, preferences, rawItems, nativeWidgets, (movingId, resizingId, badgePackages) ->
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

                is HomeItem.NativeWidget -> {
                    HomeWorkspaceItem.NativeWidget(
                        item = item,
                        label = appContext.getString(item.widgetType.titleRes),
                    )
                }
            }
        }

        val rankedFrequent = apps
            .asSequence()
            .filter { (nativeWidgets.appLaunchCounts[it.key] ?: 0) > 0 }
            .sortedByDescending { nativeWidgets.appLaunchCounts[it.key] ?: 0 }
            .take(8)
            .toList()
        val frequentApps = if (rankedFrequent.size >= 4) {
            rankedFrequent
        } else {
            (rankedFrequent + dashApps)
                .distinctBy { it.key }
                .take(8)
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
        val activeResizingId = if (
            !preferences.home.lockLayout &&
            activeMovingId == null &&
            resizingId != null &&
            rawItems.any {
                it.id == resizingId && (it is HomeItem.Widget || it is HomeItem.NativeWidget)
            }
        ) {
            resizingId
        } else {
            null
        }

        HomeUiState(
            dashApps = dashApps,
            workspaceItems = resolvedWorkspace,
            frequentApps = frequentApps,
            nativeWidgets = nativeWidgets,
            showClock = preferences.showClock,
            movingItemId = activeMovingId,
            resizingItemId = activeResizingId,
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
        resizingItemId.value = null
        movingItemId.value = itemId
    }

    fun cancelMovingItem() {
        movingItemId.value = null
    }

    fun startResizingItem(itemId: String) {
        if (uiState.value.lockLayout) return
        movingItemId.value = null
        resizingItemId.value = itemId
    }

    fun cancelResizingItem() {
        resizingItemId.value = null
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
            val item = currentState.workspaceItems
                .firstOrNull { it.item.id == itemId }
                ?.item
                ?: return@launch
            if (item !is HomeItem.Widget && item !is HomeItem.NativeWidget) return@launch

            val minX = if (item is HomeItem.NativeWidget) {
                item.widgetType.minSpanX.coerceAtMost(currentState.gridColumns)
            } else {
                1
            }
            val minY = if (item is HomeItem.NativeWidget) {
                item.widgetType.minSpanY.coerceAtMost(currentState.gridRows)
            } else {
                1
            }

            val nextX = (item.spanX + deltaSpanX).coerceIn(minX, currentState.gridColumns)
            val nextY = (item.spanY + deltaSpanY).coerceIn(minY, currentState.gridRows)
            if (nextX == item.spanX && nextY == item.spanY) return@launch

            homeWorkspaceRepository.resizeWidget(
                id = itemId,
                newSpanX = nextX,
                newSpanY = nextY,
                columns = currentState.gridColumns,
                rows = currentState.gridRows,
            )
        }
    }

    /**
     * Aplica novo retângulo (`cellX`, `cellY`, `spanX`, `spanY`) após arraste dinâmico das bordas do widget.
     */
    fun resizeWidgetBounds(
        itemId: String,
        newCellX: Int,
        newCellY: Int,
        newSpanX: Int,
        newSpanY: Int,
    ) {
        val currentState = uiState.value
        if (currentState.lockLayout) return
        viewModelScope.launch {
            homeWorkspaceRepository.resizeWidget(
                id = itemId,
                newSpanX = newSpanX,
                newSpanY = newSpanY,
                newCellX = newCellX,
                newCellY = newCellY,
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
            if (resizingItemId.value == itemId) {
                resizingItemId.value = null
            }
            val removed = homeWorkspaceRepository.removeItem(itemId)
            if (removed is HomeItem.Widget) {
                appWidgetController.deleteAppWidgetId(removed.appWidgetId)
            }
        }
    }

    /**
     * Reorganiza os apps da dock ou solta um app em uma posição específica.
     */
    fun dropAppOnDock(app: AppInfo, targetIndex: Int) {
        val state = uiState.value
        if (state.lockLayout) return
        val maxIcons = state.dockSettings.iconCount.coerceAtLeast(1)
        val currentKeys = state.dashApps.map { it.key }.toMutableList()
        val existingIndex = currentKeys.indexOf(app.key)

        if (existingIndex >= 0) {
            val safeTarget = targetIndex.coerceIn(0, (currentKeys.size - 1).coerceAtLeast(0))
            if (existingIndex == safeTarget) return
            currentKeys.removeAt(existingIndex)
            currentKeys.add(safeTarget.coerceIn(0, currentKeys.size), app.key)
        } else if (currentKeys.size < maxIcons) {
            currentKeys.add(targetIndex.coerceIn(0, currentKeys.size), app.key)
        } else {
            val replaceIndex = targetIndex.coerceIn(0, currentKeys.lastIndex)
            currentKeys[replaceIndex] = app.key
        }

        viewModelScope.launch {
            settingsRepository.setPinnedDashKeys(currentKeys)
        }
    }

    /**
     * Remove um app específico da dock mantendo os demais fixados em ordem.
     */
    fun removeAppFromDock(appKey: String) {
        val state = uiState.value
        if (state.lockLayout) return
        val updatedKeys = state.dashApps
            .map { it.key }
            .filterNot { it == appKey }

        viewModelScope.launch {
            settingsRepository.setPinnedDashKeys(updatedKeys)
        }
    }

    suspend fun loadAvailableWidgets(): List<WidgetAppGroup> {
        return appWidgetController.loadInstalledWidgets()
    }

    fun addNativeWidgetToHome(
        widgetType: NativeWidgetType,
        preferredCellX: Int? = null,
        preferredCellY: Int? = null,
        onResult: (Boolean) -> Unit = {},
    ) {
        val state = uiState.value
        if (state.lockLayout) {
            onResult(false)
            return
        }
        viewModelScope.launch {
            val added = homeWorkspaceRepository.addNativeWidget(
                widgetType = widgetType,
                preferredCellX = preferredCellX,
                preferredCellY = preferredCellY,
                columns = state.gridColumns,
                rows = state.gridRows,
            )
            onResult(added)
        }
    }

    fun saveQuickNote(content: String, noteId: String? = null) {
        viewModelScope.launch {
            nativeWidgetsRepository.saveQuickNote(content = content, noteId = noteId)
        }
    }

    fun deleteQuickNote(noteId: String) {
        viewModelScope.launch {
            nativeWidgetsRepository.deleteQuickNote(noteId)
        }
    }

    fun addQuickTask(title: String) {
        viewModelScope.launch {
            nativeWidgetsRepository.addTask(title)
        }
    }

    fun toggleQuickTask(taskId: String) {
        viewModelScope.launch {
            nativeWidgetsRepository.toggleTask(taskId)
        }
    }

    fun clearCompletedTasks() {
        viewModelScope.launch {
            nativeWidgetsRepository.clearCompletedTasks()
        }
    }

    fun refreshWeather(
        force: Boolean = false,
        onError: ((String) -> Unit)? = null,
    ) {
        viewModelScope.launch {
            val ok = nativeWidgetsRepository.refreshWeather(force = force)
            if (!ok && onError != null) {
                val err = nativeWidgetsRepository.currentWeatherError
                if (!err.isNullOrBlank()) {
                    onError(err)
                }
            }
        }
    }

    suspend fun searchWeatherCities(query: String): Result<List<WeatherLocation>> {
        return nativeWidgetsRepository.searchWeatherCities(query)
    }

    fun selectWeatherLocation(location: WeatherLocation) {
        viewModelScope.launch {
            nativeWidgetsRepository.setWeatherLocationAndRefresh(location)
        }
    }

    fun useDeviceWeatherLocation(
        onResult: (location: WeatherLocation?, forecastUpdated: Boolean) -> Unit = { _, _ -> },
    ) {
        viewModelScope.launch {
            val location = NativeWidgetSystemProviders.resolveCoarseWeatherLocation(appContext)
            if (location != null) {
                val refreshed = nativeWidgetsRepository.setWeatherLocationAndRefresh(location)
                onResult(location, refreshed)
            } else {
                onResult(null, false)
            }
        }
    }

    fun setClockToolMode(mode: ClockToolMode) {
        viewModelScope.launch {
            nativeWidgetsRepository.setClockToolMode(mode)
        }
    }

    fun setTimerDuration(durationMs: Long) {
        viewModelScope.launch {
            nativeWidgetsRepository.setTimerDuration(durationMs)
        }
    }

    fun toggleTimer() {
        viewModelScope.launch {
            nativeWidgetsRepository.toggleTimer()
        }
    }

    fun resetTimer() {
        viewModelScope.launch {
            nativeWidgetsRepository.resetTimer()
        }
    }

    fun finishTimerIfElapsed() {
        viewModelScope.launch {
            nativeWidgetsRepository.finishTimerIfElapsed()
        }
    }

    fun toggleStopwatch() {
        viewModelScope.launch {
            nativeWidgetsRepository.toggleStopwatch()
        }
    }

    fun resetStopwatch() {
        viewModelScope.launch {
            nativeWidgetsRepository.resetStopwatch()
        }
    }

    class Factory(
        private val appContext: Context,
        private val appsRepository: AppsRepository,
        private val settingsRepository: SettingsRepository,
        private val notificationBadgeRepository: NotificationBadgeRepository,
        private val homeWorkspaceRepository: HomeWorkspaceRepository,
        private val appWidgetController: AppWidgetController,
        private val nativeWidgetsRepository: NativeWidgetsRepository,
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
                nativeWidgetsRepository = nativeWidgetsRepository,
                homeGridPlacementUseCase = homeGridPlacementUseCase,
                defaultDashPackageProvider = defaultDashPackageProvider,
                resolveDefaultDashAppsUseCase = resolveDefaultDashAppsUseCase,
            ) as T
        }
    }
}
