package io.github.vimitt.overview.ui.home

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Rect
import android.os.BatteryManager
import android.os.SystemClock
import android.view.View
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect as ComposeRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.vimitt.overview.R
import io.github.vimitt.overview.data.AppWidgetController
import io.github.vimitt.overview.domain.model.AppInfo
import io.github.vimitt.overview.domain.model.FolderSettings
import io.github.vimitt.overview.domain.model.GestureSettings
import io.github.vimitt.overview.domain.model.HomeGridSpec
import io.github.vimitt.overview.domain.model.HomeItem
import io.github.vimitt.overview.domain.model.LauncherGestureAction
import io.github.vimitt.overview.ui.ActiveDragState
import io.github.vimitt.overview.ui.DockDropTarget
import io.github.vimitt.overview.ui.DragPayload
import io.github.vimitt.overview.ui.HomeDropTarget
import io.github.vimitt.overview.ui.components.AppIconItem
import io.github.vimitt.overview.ui.components.DashBar
import io.github.vimitt.overview.ui.components.FolderIconTile
import io.github.vimitt.overview.ui.components.HomePagesPill
import io.github.vimitt.overview.ui.components.HomeQuickSettingsPill
import io.github.vimitt.overview.ui.components.HomeTopClockPill
import io.github.vimitt.overview.ui.home.widgets.NativeWidgetActions
import io.github.vimitt.overview.ui.home.widgets.NativeWidgetDialogsOverlay
import io.github.vimitt.overview.ui.home.widgets.NativeWidgetHost
import io.github.vimitt.overview.ui.home.widgets.NativeWidgetLiveEnvironment
import io.github.vimitt.overview.ui.home.widgets.rememberNativeWidgetEnvironment
import io.github.vimitt.overview.ui.theme.OverviewTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private class HomeGestureGuard {
    var activeChildCount: Int = 0
    var lastChildInteractionUptimeMs: Long = 0L

    fun isChildActive(): Boolean {
        return activeChildCount > 0 || (SystemClock.uptimeMillis() - lastChildInteractionUptimeMs) < 160L
    }
}

private fun Modifier.suppressHomeBackgroundLongPress(guard: HomeGestureGuard): Modifier =
    pointerInput(guard) {
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            guard.activeChildCount++
            try {
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    if (event.changes.none { it.pressed }) break
                }
            } finally {
                guard.activeChildCount = (guard.activeChildCount - 1).coerceAtLeast(0)
                guard.lastChildInteractionUptimeMs = SystemClock.uptimeMillis()
            }
        }
    }

/**
 * Tela inicial transparente sobre o papel de parede do sistema,
 * com relógio opcional, grade de atalhos/widgets e o Dash fixo inferior.
 */
@Composable
fun HomeScreen(
    uiState: HomeUiState,
    appWidgetController: AppWidgetController,
    activeDrag: ActiveDragState?,
    dropTarget: HomeDropTarget?,
    onAppClick: (app: AppInfo, sourceView: View, sourceBounds: Rect?) -> Unit,
    onPinnedShortcutClick: (HomeWorkspaceItem.PinnedShortcut, Rect?) -> Unit = { _, _ -> },
    onFolderClick: (HomeWorkspaceItem.Folder) -> Unit = {},
    onOpenHomePages: (() -> Unit)? = null,
    onOpenClockCalendar: (() -> Unit)? = null,
    onOpenQuickSettings: (() -> Unit)? = null,
    onOpenOverview: () -> Unit,
    onOpenOverviewSearch: () -> Unit,
    onLongPress: () -> Unit,
    canPlaceMovingItemAt: (cellX: Int, cellY: Int, page: Int) -> Boolean,
    onPlaceMovingItemAt: (cellX: Int, cellY: Int, page: Int) -> Unit,
    onCancelMovingItem: () -> Unit,
    canPlaceDragItemAt: (cellX: Int, cellY: Int, spanX: Int, spanY: Int, page: Int, ignoreItemId: String?) -> Boolean = { _, _, _, _, _, _ -> false },
    onResizeWidgetBounds: (itemId: String, cellX: Int, cellY: Int, spanX: Int, spanY: Int) -> Unit = { _, _, _, _, _ -> },
    onStartMovingResizedItem: (String) -> Unit = {},
    onRemoveResizedItem: (String) -> Unit = {},
    onCancelResizingItem: () -> Unit = {},
    onGridBoundsChanged: (ComposeRect) -> Unit,
    onRemoveZoneBoundsChanged: (ComposeRect?) -> Unit,
    onStartItemDrag: (HomeWorkspaceItem, Offset) -> Unit,
    onItemDragMove: (Offset) -> Unit,
    onItemDragEnd: () -> Unit,
    onItemDragCancel: () -> Unit,
    modifier: Modifier = Modifier,
    currentPage: Int = 0,
    onCurrentPageChanged: (Int) -> Unit = {},
    isVisible: Boolean = true,
    dockDropTarget: DockDropTarget? = null,
    onDockBoundsChanged: (ComposeRect?) -> Unit = {},
    onStartDockDrag: (AppInfo, Int, Offset) -> Unit = { _, _, _ -> },
    onDockDragMove: (Offset) -> Unit = {},
    onDockDragEnd: () -> Unit = {},
    onDockDragCancel: () -> Unit = {},
    nativeWidgetActions: NativeWidgetActions = NativeWidgetActions(),
    permissionRevision: Int = 0,
    gestureSettings: GestureSettings = GestureSettings(),
    onExecuteGesture: (LauncherGestureAction) -> Unit = {},
    onOverviewDragProgress: (Float) -> Unit = {},
    onOverviewDragEnd: (shouldOpen: Boolean) -> Unit = {},
) {
    val nativeEnvironment = rememberNativeWidgetEnvironment(
        workspaceItems = uiState.workspaceItems,
        hasSavedWeatherLocation = uiState.nativeWidgets.weatherLocation != null,
        hasWeatherCache = uiState.nativeWidgets.weather != null,
        actions = nativeWidgetActions,
        isVisible = isVisible,
        includeClockWeather = uiState.homeSettings.showTopClockButton && uiState.homeSettings.showClockWeather,
        permissionRevision = permissionRevision,
    )
    val density = LocalDensity.current
    val dragRangePx = with(density) { 180.dp.toPx() }
    val dragTriggerPx = with(density) { gestureSettings.sensitivity.triggerDp.dp.toPx() }
    val minFlingDistancePx = with(density) { 12.dp.toPx() }
    val velocityThreshold = gestureSettings.sensitivity.velocityThresholdPx
    val swipeUpAction = gestureSettings.swipeUpAction
    val swipeDownAction = gestureSettings.swipeDownAction
    val doubleTapAction = gestureSettings.doubleTapAction
    val haptic = LocalHapticFeedback.current
    val velocityTracker = remember { VelocityTracker() }
    val hasWorkspaceItems = uiState.workspaceItems.isNotEmpty()
    val isMoving = uiState.movingItemId != null
    val resizingItem = remember(uiState.workspaceItems, uiState.resizingItemId) {
        val targetId = uiState.resizingItemId ?: return@remember null
        uiState.workspaceItems.firstOrNull {
            it.item.id == targetId && (it.item is HomeItem.Widget || it.item is HomeItem.NativeWidget)
        }
    }
    val isResizing = resizingItem != null
    val isDragInProgress = activeDrag != null
    val isDraggingOverGrid = activeDrag?.hasMovedPastSlop == true
    val isDraggingRemovable = isDraggingOverGrid &&
        (activeDrag.payload is DragPayload.FromHome || activeDrag.payload is DragPayload.FromDock)

    var lastDragActiveEndUptimeMs by remember { mutableLongStateOf(0L) }
    var wasDragging by remember { mutableLongStateOf(0L) }
    LaunchedEffect(isDragInProgress) {
        if (isDragInProgress) {
            wasDragging = 1L
        } else if (wasDragging == 1L) {
            wasDragging = 0L
            lastDragActiveEndUptimeMs = SystemClock.uptimeMillis()
        }
    }
    val currentDragInProgress by rememberUpdatedState(isDragInProgress)
    val currentIsMoving by rememberUpdatedState(isMoving)
    val currentIsResizing by rememberUpdatedState(isResizing)
    val gestureGuard = remember { HomeGestureGuard() }

    LaunchedEffect(isDraggingRemovable) {
        if (!isDraggingRemovable) {
            onRemoveZoneBoundsChanged(null)
        }
    }

    LaunchedEffect(isMoving, isResizing, isDragInProgress) {
        if (isMoving || isResizing || isDragInProgress) {
            onOverviewDragEnd(false)
        }
    }

    if (isMoving) {
        BackHandler(onBack = onCancelMovingItem)
    } else if (isResizing) {
        BackHandler(onBack = onCancelResizingItem)
    }

    val showLeftPagesPill = uiState.homeSettings.showTopPagesButton && onOpenHomePages != null
    val showCenterClockPill = uiState.homeSettings.showTopClockButton && onOpenClockCalendar != null
    val showRightQuickSettingsPill = uiState.homeSettings.showQuickSettingsButton && onOpenQuickSettings != null
    val showAnyTopBarPill = showLeftPagesPill || showCenterClockPill || showRightQuickSettingsPill

    val displayedPageCount = if (isDraggingOverGrid || isMoving) {
        (uiState.pageCount + 1).coerceIn(1, HomeGridSpec.MAX_PAGES)
    } else {
        uiState.pageCount.coerceIn(1, HomeGridSpec.MAX_PAGES)
    }
    val pagerState = rememberPagerState(
        initialPage = currentPage.coerceIn(0, displayedPageCount - 1),
        pageCount = { displayedPageCount },
    )
    val pagerScope = rememberCoroutineScope()

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(onLongPress, doubleTapAction, isMoving, isResizing, isDragInProgress) {
                    if (isMoving || isDragInProgress) return@pointerInput
                    detectTapGestures(
                        onTap = if (isResizing) {
                            {
                                if (!gestureGuard.isChildActive()) {
                                    onCancelResizingItem()
                                }
                            }
                        } else {
                            null
                        },
                        onLongPress = if (!isResizing) {
                            {
                                val sinceDrag = SystemClock.uptimeMillis() - lastDragActiveEndUptimeMs
                                if (!gestureGuard.isChildActive() &&
                                    !currentDragInProgress &&
                                    !currentIsMoving &&
                                    !currentIsResizing &&
                                    sinceDrag > 400L
                                ) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onLongPress()
                                }
                            }
                        } else {
                            null
                        },
                        onDoubleTap = if (!isResizing && doubleTapAction != LauncherGestureAction.NONE) {
                            {
                                if (!gestureGuard.isChildActive()) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onExecuteGesture(doubleTapAction)
                                }
                            }
                        } else {
                            null
                        },
                    )
                }
                .pointerInput(
                    dragRangePx,
                    dragTriggerPx,
                    minFlingDistancePx,
                    velocityThreshold,
                    swipeUpAction,
                    swipeDownAction,
                    isMoving,
                    isResizing,
                    isDragInProgress,
                ) {
                    if (isMoving || isResizing || isDragInProgress) return@pointerInput
                    var totalDragY = 0f
                    detectVerticalDragGestures(
                        onDragStart = {
                            totalDragY = 0f
                            velocityTracker.resetTracking()
                        },
                        onDragEnd = {
                            val sinceDrag = SystemClock.uptimeMillis() - lastDragActiveEndUptimeMs
                            if (currentDragInProgress || currentIsMoving || currentIsResizing || sinceDrag < 400L) {
                                totalDragY = 0f
                                onOverviewDragEnd(false)
                                return@detectVerticalDragGestures
                            }
                            val velocityY = velocityTracker.calculateVelocity().y
                            val swipeUpTriggered = totalDragY <= -dragTriggerPx ||
                                (velocityY <= -velocityThreshold && totalDragY <= -minFlingDistancePx)
                            val swipeDownTriggered = totalDragY >= dragTriggerPx ||
                                (velocityY >= velocityThreshold && totalDragY >= minFlingDistancePx)
                            totalDragY = 0f

                            if (swipeUpAction == LauncherGestureAction.OPEN_OVERVIEW) {
                                onOverviewDragEnd(swipeUpTriggered)
                                if (swipeUpTriggered) {
                                    onOpenOverview()
                                } else if (swipeDownTriggered && swipeDownAction != LauncherGestureAction.NONE) {
                                    onExecuteGesture(swipeDownAction)
                                }
                            } else {
                                onOverviewDragEnd(false)
                                if (swipeUpTriggered && swipeUpAction != LauncherGestureAction.NONE) {
                                    onExecuteGesture(swipeUpAction)
                                } else if (swipeDownTriggered && swipeDownAction != LauncherGestureAction.NONE) {
                                    onExecuteGesture(swipeDownAction)
                                }
                            }
                        },
                        onDragCancel = {
                            totalDragY = 0f
                            onOverviewDragEnd(false)
                        },
                        onVerticalDrag = { change, dragAmount ->
                            val sinceDrag = SystemClock.uptimeMillis() - lastDragActiveEndUptimeMs
                            if (currentDragInProgress || currentIsMoving || currentIsResizing || sinceDrag < 400L) {
                                return@detectVerticalDragGestures
                            }
                            velocityTracker.addPosition(change.uptimeMillis, change.position)
                            totalDragY += dragAmount
                            if (swipeUpAction == LauncherGestureAction.OPEN_OVERVIEW && totalDragY < 0f) {
                                onOverviewDragProgress((-totalDragY / dragRangePx).coerceIn(0f, 1f))
                            } else {
                                onOverviewDragProgress(0f)
                            }
                        },
                    )
                }
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (isMoving) {
                MoveModeBanner(
                    onCancel = onCancelMovingItem,
                    modifier = Modifier.padding(top = 8.dp, bottom = 8.dp),
                )
            } else if (resizingItem != null) {
                val label = when (resizingItem) {
                    is HomeWorkspaceItem.Widget -> resizingItem.label
                    is HomeWorkspaceItem.NativeWidget -> resizingItem.label
                    is HomeWorkspaceItem.Shortcut -> resizingItem.app.label
                    is HomeWorkspaceItem.PinnedShortcut -> resizingItem.item.label
                    is HomeWorkspaceItem.Folder -> resizingItem.item.name
                }
                ResizeModeBanner(
                    title = label,
                    onMove = { onStartMovingResizedItem(resizingItem.item.id) },
                    onRemove = { onRemoveResizedItem(resizingItem.item.id) },
                    onDone = onCancelResizingItem,
                    modifier = Modifier.padding(top = 8.dp, bottom = 8.dp),
                )
            } else if (showAnyTopBarPill) {
                Spacer(modifier = Modifier.height(44.dp))
            }

            var localGridBounds by remember { mutableStateOf<ComposeRect?>(null) }

            LaunchedEffect(pagerState.currentPage, displayedPageCount) {
                onCurrentPageChanged(pagerState.currentPage.coerceIn(0, displayedPageCount - 1))
            }

            LaunchedEffect(currentPage, displayedPageCount) {
                val safeTarget = currentPage.coerceIn(0, displayedPageCount - 1)
                if (safeTarget != pagerState.currentPage && !pagerState.isScrollInProgress) {
                    pagerState.animateScrollToPage(safeTarget)
                }
            }

            val edgeZonePx = with(density) { 40.dp.toPx() }
            val dragEdgeDelta = remember(isDraggingOverGrid, activeDrag?.pointerInWindow, localGridBounds, pagerState.currentPage, displayedPageCount) {
                val bounds = localGridBounds
                val pointer = activeDrag?.pointerInWindow
                if (!isDraggingOverGrid || bounds == null || pointer == null) {
                    0
                } else if (pointer.x <= bounds.left + edgeZonePx && pagerState.currentPage > 0) {
                    -1
                } else if (pointer.x >= bounds.right - edgeZonePx && pagerState.currentPage < displayedPageCount - 1) {
                    1
                } else {
                    0
                }
            }
            LaunchedEffect(isDraggingOverGrid, dragEdgeDelta, pagerState.currentPage) {
                if (isDraggingOverGrid && dragEdgeDelta != 0) {
                    delay(550L)
                    val targetPage = (pagerState.currentPage + dragEdgeDelta)
                        .coerceIn(0, displayedPageCount - 1)
                    if (targetPage != pagerState.currentPage) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        pagerState.animateScrollToPage(targetPage)
                    }
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .onPlaced { coordinates ->
                        val bounds = coordinates.boundsInWindow()
                        localGridBounds = bounds
                        onGridBoundsChanged(bounds)
                    },
            ) {
                if (hasWorkspaceItems || displayedPageCount > 1 || isMoving || isDraggingOverGrid) {
                    HorizontalPager(
                        state = pagerState,
                        userScrollEnabled = !isDraggingOverGrid && !isResizing,
                        beyondViewportPageCount = 1,
                        modifier = Modifier.fillMaxSize(),
                    ) { page ->
                        val pageItems = remember(uiState.workspaceItems, page) {
                            uiState.workspaceItems.filter { it.item.page == page }
                        }
                        HomeWorkspaceGrid(
                            items = pageItems,
                            frequentApps = uiState.frequentApps,
                            nativeWidgetsState = uiState.nativeWidgets,
                            nativeEnvironment = nativeEnvironment,
                            nativeWidgetActions = nativeWidgetActions,
                            isVisible = isVisible && pagerState.currentPage == page,
                            gridColumns = uiState.gridColumns,
                            gridRows = uiState.gridRows,
                            iconScale = uiState.iconScale,
                            showLabels = uiState.showLabels,
                            labelScale = uiState.labelScale,
                            folderSettings = uiState.folderSettings,
                            showNotificationDots = uiState.showNotificationDots,
                            badgePackages = uiState.badgePackages,
                            movingItemId = uiState.movingItemId,
                            resizingItem = resizingItem?.takeIf { it.item.page == page },
                            activeDrag = activeDrag,
                            dropTarget = dropTarget?.takeIf { it.page == page },
                            appWidgetController = appWidgetController,
                            onAppClick = onAppClick,
                            onPinnedShortcutClick = onPinnedShortcutClick,
                            onFolderClick = onFolderClick,
                            canPlaceMovingItemAt = { cellX, cellY ->
                                canPlaceMovingItemAt(cellX, cellY, page)
                            },
                            onPlaceMovingItemAt = { cellX, cellY ->
                                onPlaceMovingItemAt(cellX, cellY, page)
                            },
                            canPlaceItem = { cellX, cellY, spanX, spanY, ignoreId ->
                                canPlaceDragItemAt(cellX, cellY, spanX, spanY, page, ignoreId)
                            },
                            onResizeWidgetBounds = onResizeWidgetBounds,
                            gestureGuard = gestureGuard,
                            onStartItemDrag = onStartItemDrag,
                            onItemDragMove = onItemDragMove,
                            onItemDragEnd = onItemDragEnd,
                            onItemDragCancel = onItemDragCancel,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }

            if (displayedPageCount > 1) {
                HomePageIndicator(
                    pageCount = displayedPageCount,
                    currentPage = pagerState.currentPage,
                    onSelectPage = { page ->
                        pagerScope.launch { pagerState.animateScrollToPage(page) }
                    },
                    modifier = Modifier.padding(top = 4.dp, bottom = 2.dp),
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
            ) {
                OverviewHandlePill(onClick = onOpenOverview)
                if (uiState.showDockSearchBar) {
                    Spacer(modifier = Modifier.height(6.dp))
                    DockSearchBarPill(
                        backgroundAlpha = uiState.dockBackgroundAlpha,
                        onClick = onOpenOverviewSearch,
                        isVisible = isVisible,
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                val draggingFromDockIndex =
                    (activeDrag?.payload as? DragPayload.FromDock)
                        ?.takeIf { activeDrag.hasMovedPastSlop }
                        ?.sourceIndex
                DashBar(
                    apps = uiState.dashApps,
                    onAppClick = onAppClick,
                    backgroundAlpha = uiState.dockBackgroundAlpha,
                    showNotificationDots = uiState.showNotificationDots,
                    badgePackages = uiState.badgePackages,
                    isVisible = isVisible,
                    onAppDragStart = onStartDockDrag,
                    onAppDragMove = onDockDragMove,
                    onAppDragEnd = onDockDragEnd,
                    onAppDragCancel = onDockDragCancel,
                    draggingFromDockIndex = draggingFromDockIndex,
                    dockDropHighlightIndex = dockDropTarget?.targetIndex,
                    modifier = Modifier
                        .suppressHomeBackgroundLongPress(gestureGuard)
                        .onPlaced { coordinates ->
                            onDockBoundsChanged(coordinates.boundsInWindow())
                        },
                )
            }
        }

        if (showAnyTopBarPill &&
            !isDraggingRemovable &&
            !isMoving &&
            !isResizing
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(start = 16.dp, end = 16.dp, top = 8.dp),
            ) {
                if (uiState.homeSettings.showTopPagesButton && onOpenHomePages != null) {
                    HomePagesPill(
                        pageCount = uiState.pageCount,
                        currentPage = pagerState.currentPage,
                        onClick = onOpenHomePages,
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .suppressHomeBackgroundLongPress(gestureGuard),
                    )
                }

                if (uiState.homeSettings.showTopClockButton && onOpenClockCalendar != null) {
                    HomeTopClockPill(
                        isVisible = isVisible,
                        onClick = onOpenClockCalendar,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .suppressHomeBackgroundLongPress(gestureGuard),
                    )
                }

                if (uiState.homeSettings.showQuickSettingsButton && onOpenQuickSettings != null) {
                    val context = LocalContext.current
                    var batteryLevel by remember { mutableIntStateOf(0) }
                    var batteryCharging by remember { mutableStateOf(false) }

                    DisposableEffect(context, isVisible) {
                        if (!isVisible) {
                            return@DisposableEffect onDispose {}
                        }
                        fun updateFromBatteryIntent(intent: Intent?) {
                            val raw = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
                            val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
                            if (raw >= 0 && scale > 0) {
                                batteryLevel = ((raw * 100f) / scale).toInt().coerceIn(0, 100)
                            }
                            val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
                            batteryCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                                status == BatteryManager.BATTERY_STATUS_FULL
                        }
                        updateFromBatteryIntent(
                            context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED)),
                        )
                        val receiver = object : BroadcastReceiver() {
                            override fun onReceive(ctx: Context?, intent: Intent?) {
                                updateFromBatteryIntent(intent)
                            }
                        }
                        runCatching {
                            ContextCompat.registerReceiver(
                                context,
                                receiver,
                                IntentFilter(Intent.ACTION_BATTERY_CHANGED),
                                ContextCompat.RECEIVER_NOT_EXPORTED,
                            )
                        }
                        onDispose {
                            runCatching { context.unregisterReceiver(receiver) }
                        }
                    }

                    HomeQuickSettingsPill(
                        batteryLevel = batteryLevel,
                        isCharging = batteryCharging,
                        onClick = onOpenQuickSettings,
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .suppressHomeBackgroundLongPress(gestureGuard),
                    )
                }
            }
        }

        if (isDraggingRemovable) {
            RemoveDropZonePill(
                isActive = dropTarget?.isOverRemoveZone == true,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 12.dp)
                    .onPlaced { coordinates ->
                        onRemoveZoneBoundsChanged(coordinates.boundsInWindow())
                    },
            )
        }

        // Diálogos de notas, tarefas e cidade renderizados no topo da pilha da Home para receber todos os toques.
        NativeWidgetDialogsOverlay(
            environment = nativeEnvironment,
            actions = nativeWidgetActions,
            selectedWeatherProvider = uiState.homeSettings.weatherProvider,
            showClockWeather = uiState.homeSettings.showClockWeather,
        )
    }
}

@Composable
private fun HomeWorkspaceGrid(
    items: List<HomeWorkspaceItem>,
    frequentApps: List<AppInfo>,
    nativeWidgetsState: io.github.vimitt.overview.domain.model.NativeWidgetsState,
    nativeEnvironment: NativeWidgetLiveEnvironment,
    nativeWidgetActions: NativeWidgetActions,
    isVisible: Boolean,
    gridColumns: Int,
    gridRows: Int,
    iconScale: Float,
    showLabels: Boolean,
    labelScale: Float,
    folderSettings: FolderSettings,
    showNotificationDots: Boolean,
    badgePackages: Set<String>,
    movingItemId: String?,
    resizingItem: HomeWorkspaceItem?,
    activeDrag: ActiveDragState?,
    dropTarget: HomeDropTarget?,
    appWidgetController: AppWidgetController,
    onAppClick: (app: AppInfo, sourceView: View, sourceBounds: Rect?) -> Unit,
    onPinnedShortcutClick: (HomeWorkspaceItem.PinnedShortcut, Rect?) -> Unit,
    onFolderClick: (HomeWorkspaceItem.Folder) -> Unit,
    canPlaceMovingItemAt: (cellX: Int, cellY: Int) -> Boolean,
    onPlaceMovingItemAt: (cellX: Int, cellY: Int) -> Unit,
    canPlaceItem: (cellX: Int, cellY: Int, spanX: Int, spanY: Int, ignoreItemId: String?) -> Boolean,
    onResizeWidgetBounds: (itemId: String, cellX: Int, cellY: Int, spanX: Int, spanY: Int) -> Unit,
    gestureGuard: HomeGestureGuard,
    onStartItemDrag: (HomeWorkspaceItem, Offset) -> Unit,
    onItemDragMove: (Offset) -> Unit,
    onItemDragEnd: () -> Unit,
    onItemDragCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = OverviewTheme.colors
    val labelShadow = remember(colors.wallpaperShadow) {
        Shadow(
            color = colors.wallpaperShadow,
            offset = Offset(0f, 1.5f),
            blurRadius = 6f,
        )
    }
    val isDraggingOverGrid = activeDrag?.hasMovedPastSlop == true
    val safeColumns = gridColumns.coerceIn(3, 6)
    val safeRows = gridRows.coerceIn(3, 6)
    val shortcutIconSize = remember(iconScale) { (52f * iconScale).dp }

    BoxWithConstraints(modifier = modifier) {
        val cellWidth = maxWidth / safeColumns
        val cellHeight = maxHeight / safeRows
        val cellWidthPx = constraints.maxWidth.toFloat() / safeColumns
        val cellHeightPx = constraints.maxHeight.toFloat() / safeRows

        if (isDraggingOverGrid) {
            val cellShape = OverviewTheme.shapes.medium
            for (y in 0 until safeRows) {
                for (x in 0 until safeColumns) {
                    Box(
                        modifier = Modifier
                            .offset(x = cellWidth * x, y = cellHeight * y)
                            .size(width = cellWidth, height = cellHeight)
                            .padding(4.dp)
                            .clip(cellShape)
                            .border(
                                width = 1.dp,
                                color = colors.hairlineBorder.copy(alpha = 0.55f),
                                shape = cellShape,
                            ),
                    )
                }
            }

            if (dropTarget != null && !dropTarget.isOverRemoveZone) {
                val isMergeTarget = dropTarget.mergeFolderTargetId != null
                val targetColor = if (dropTarget.isValid) {
                    colors.accent
                } else {
                    colors.error
                }
                val targetShape = OverviewTheme.shapes.large
                Box(
                    modifier = Modifier
                        .offset(
                            x = cellWidth * dropTarget.cellX,
                            y = cellHeight * dropTarget.cellY,
                        )
                        .size(
                            width = cellWidth * dropTarget.spanX,
                            height = cellHeight * dropTarget.spanY,
                        )
                        .padding(if (isMergeTarget) 2.dp else 4.dp)
                        .clip(targetShape)
                        .background(
                            if (isMergeTarget) {
                                colors.accentContainer.copy(alpha = 0.42f)
                            } else {
                                targetColor.copy(alpha = 0.24f)
                            },
                        )
                        .border(
                            width = if (isMergeTarget) 2.5.dp else 2.dp,
                            color = targetColor,
                            shape = targetShape,
                        ),
                )
            }
        } else if (movingItemId != null) {
            for (y in 0 until safeRows) {
                for (x in 0 until safeColumns) {
                    val canPlace = canPlaceMovingItemAt(x, y)
                    val cellShape = OverviewTheme.shapes.medium
                    Box(
                        modifier = Modifier
                            .offset(x = cellWidth * x, y = cellHeight * y)
                            .size(width = cellWidth, height = cellHeight)
                            .padding(4.dp)
                            .clip(cellShape)
                            .background(
                                if (canPlace) {
                                    colors.accent.copy(alpha = 0.16f)
                                } else {
                                    colors.surface.copy(alpha = 0.08f)
                                },
                            )
                            .border(
                                width = 1.dp,
                                color = if (canPlace) {
                                    colors.accent.copy(alpha = 0.75f)
                                } else {
                                    colors.hairlineBorder
                                },
                                shape = cellShape,
                            )
                            .clickable(enabled = canPlace) {
                                onPlaceMovingItemAt(x, y)
                            },
                    )
                }
            }
        }

        for (workspaceItem in items) {
            key(workspaceItem.item.id) {
                val item = workspaceItem.item
                val isMovingThis = item.id == movingItemId
                val isDraggingThis = isDraggingOverGrid && activeDrag.payload.ignoreItemId == item.id
                val displaySpanX = item.spanX.coerceIn(1, safeColumns)
                val displaySpanY = item.spanY.coerceIn(1, safeRows)
                val displayCellX = item.cellX.coerceIn(0, (safeColumns - displaySpanX).coerceAtLeast(0))
                val displayCellY = item.cellY.coerceIn(0, (safeRows - displaySpanY).coerceAtLeast(0))
                val itemWidth = cellWidth * displaySpanX
                val itemHeight = cellHeight * displaySpanY

                Box(
                    modifier = Modifier
                        .offset(x = cellWidth * displayCellX, y = cellHeight * displayCellY)
                        .size(width = itemWidth, height = itemHeight)
                        .padding(4.dp)
                        .suppressHomeBackgroundLongPress(gestureGuard)
                        .graphicsLayer {
                            alpha = if (isDraggingThis) 0.25f else 1f
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    when (workspaceItem) {
                        is HomeWorkspaceItem.Shortcut -> {
                            AppIconItem(
                                app = workspaceItem.app,
                                onClick = onAppClick,
                                onDragStart = { _, windowPos ->
                                    onStartItemDrag(workspaceItem, windowPos)
                                },
                                onDragMove = onItemDragMove,
                                onDragEnd = onItemDragEnd,
                                onDragCancel = onItemDragCancel,
                                iconSize = shortcutIconSize,
                                showLabel = showLabels,
                                labelFontScale = labelScale,
                                labelColor = colors.wallpaperForeground,
                                labelShadow = labelShadow,
                                showNotificationDot = showNotificationDots &&
                                    workspaceItem.app.packageName in badgePackages,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }

                        is HomeWorkspaceItem.PinnedShortcut -> {
                            val displayApp = remember(workspaceItem) {
                                AppInfo(
                                    packageName = workspaceItem.item.packageName,
                                    activityName = workspaceItem.item.shortcutId,
                                    label = workspaceItem.item.label,
                                    icon = workspaceItem.icon,
                                    userHandle = workspaceItem.userHandle,
                                )
                            }
                            AppIconItem(
                                app = displayApp,
                                onClick = { _, _, bounds ->
                                    onPinnedShortcutClick(workspaceItem, bounds)
                                },
                                onDragStart = { _, windowPos ->
                                    onStartItemDrag(workspaceItem, windowPos)
                                },
                                onDragMove = onItemDragMove,
                                onDragEnd = onItemDragEnd,
                                onDragCancel = onItemDragCancel,
                                iconSize = shortcutIconSize,
                                showLabel = showLabels,
                                labelFontScale = labelScale,
                                labelColor = colors.wallpaperForeground,
                                labelShadow = labelShadow,
                                showNotificationDot = false,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }

                        is HomeWorkspaceItem.Folder -> {
                            val isDropTarget = isDraggingOverGrid &&
                                dropTarget?.mergeFolderTargetId == workspaceItem.item.id
                            FolderIconTile(
                                folderId = workspaceItem.item.id,
                                label = workspaceItem.item.name,
                                apps = workspaceItem.apps,
                                hasBadge = workspaceItem.hasBadge,
                                previewStyle = folderSettings.previewStyle,
                                iconSize = shortcutIconSize,
                                showLabel = showLabels,
                                labelFontScale = labelScale,
                                labelColor = colors.wallpaperForeground,
                                labelShadow = labelShadow,
                                isDropHighlight = isDropTarget,
                                onClick = { onFolderClick(workspaceItem) },
                                onDragStart = { windowPos ->
                                    onStartItemDrag(workspaceItem, windowPos)
                                },
                                onDragMove = onItemDragMove,
                                onDragEnd = onItemDragEnd,
                                onDragCancel = onItemDragCancel,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }

                        is HomeWorkspaceItem.Widget -> {
                            HomeWidgetView(
                                widgetItem = workspaceItem,
                                appWidgetController = appWidgetController,
                                widthDp = (itemWidth - 8.dp).coerceAtLeast(48.dp),
                                heightDp = (itemHeight - 8.dp).coerceAtLeast(48.dp),
                                onDragStart = { windowPos ->
                                    onStartItemDrag(workspaceItem, windowPos)
                                },
                                onDragMove = onItemDragMove,
                                onDragEnd = onItemDragEnd,
                                onDragCancel = onItemDragCancel,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .then(
                                        if (isMovingThis) {
                                            Modifier.border(
                                                width = 2.dp,
                                                color = colors.accent,
                                                shape = OverviewTheme.shapes.large,
                                            )
                                        } else {
                                            Modifier
                                        },
                                    ),
                            )
                        }

                        is HomeWorkspaceItem.NativeWidget -> {
                            NativeWidgetHost(
                                widgetItem = workspaceItem,
                                environment = nativeEnvironment,
                                nativeWidgetsState = nativeWidgetsState,
                                frequentApps = frequentApps,
                                showNotificationDots = showNotificationDots,
                                badgePackages = badgePackages,
                                isVisible = isVisible,
                                isMoving = isMovingThis,
                                actions = nativeWidgetActions,
                                onAppClick = onAppClick,
                                onDragStart = { windowPos ->
                                    onStartItemDrag(workspaceItem, windowPos)
                                },
                                onDragMove = onItemDragMove,
                                onDragEnd = onItemDragEnd,
                                onDragCancel = onItemDragCancel,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    }
                }
            }
        }

        if (resizingItem != null && !isDraggingOverGrid) {
            WidgetResizeOverlayFrame(
                workspaceItem = resizingItem,
                gridColumns = safeColumns,
                gridRows = safeRows,
                cellWidthPx = cellWidthPx,
                cellHeightPx = cellHeightPx,
                canPlaceItem = canPlaceItem,
                onCommitBounds = { cellX, cellY, spanX, spanY ->
                    onResizeWidgetBounds(resizingItem.item.id, cellX, cellY, spanX, spanY)
                },
                modifier = Modifier.suppressHomeBackgroundLongPress(gestureGuard),
            )
        }
    }
}

@Composable
private fun RemoveDropZonePill(
    isActive: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = OverviewTheme.colors
    val shape = OverviewTheme.shapes.pill
    val dangerColor = colors.error
    val activeContentColor = colors.onError

    Row(
        modifier = modifier
            .clip(shape)
            .background(
                if (isActive) {
                    dangerColor.copy(alpha = 0.94f)
                } else {
                    colors.dashBackground
                },
            )
            .border(
                width = if (isActive) 1.5.dp else 1.dp,
                color = if (isActive) dangerColor else colors.hairlineBorder,
                shape = shape,
            )
            .padding(horizontal = 18.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Default.Delete,
            contentDescription = null,
            tint = if (isActive) activeContentColor else dangerColor,
            modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = stringResource(R.string.home_drag_remove),
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            color = if (isActive) activeContentColor else colors.onSurface,
        )
    }
}

@Composable
private fun ResizeModeBanner(
    title: String,
    onMove: () -> Unit,
    onRemove: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = OverviewTheme.colors
    val shape = OverviewTheme.shapes.pill
    val isExpressive = OverviewTheme.components.isExpressive
    val dangerColor = colors.error

    Row(
        modifier = modifier
            .widthIn(max = 460.dp)
            .clip(shape)
            .background(
                if (isExpressive) {
                    lerp(colors.surfaceContainerHigh, colors.accentContainer, 0.25f)
                } else {
                    colors.dashBackground
                },
            )
            .border(1.dp, if (isExpressive) colors.accent.copy(alpha = 0.35f) else colors.hairlineBorder, shape)
            .padding(start = 14.dp, end = 4.dp, top = 2.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f, fill = false)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = colors.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = stringResource(R.string.home_resize_hint),
                style = MaterialTheme.typography.labelSmall,
                color = colors.onSurfaceMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(modifier = Modifier.width(4.dp))
        TextButton(
            onClick = onMove,
            modifier = Modifier.defaultMinSize(minHeight = 48.dp),
        ) {
            Text(
                text = stringResource(R.string.home_resize_move),
                color = colors.onSurface,
            )
        }
        IconButton(
            onClick = onRemove,
            modifier = Modifier.size(48.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = stringResource(R.string.home_resize_remove),
                tint = dangerColor,
                modifier = Modifier.size(18.dp),
            )
        }
        IconButton(
            onClick = onDone,
            modifier = Modifier.size(48.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(colors.accent),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = stringResource(R.string.home_resize_done),
                    tint = colors.onAccent,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
private fun MoveModeBanner(
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = OverviewTheme.colors
    val shape = OverviewTheme.shapes.pill

    Row(
        modifier = modifier
            .clip(shape)
            .background(colors.dashBackground)
            .border(1.dp, colors.hairlineBorder, shape)
            .padding(start = 16.dp, end = 6.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.home_move_hint),
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            color = colors.onSurface,
        )
        Spacer(modifier = Modifier.width(8.dp))
        TextButton(onClick = onCancel) {
            Text(
                text = stringResource(R.string.home_move_cancel),
                color = colors.accent,
            )
        }
    }
}

@Composable
private fun OverviewHandlePill(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = OverviewTheme.colors
    val pillShape = OverviewTheme.shapes.pill
    val label = stringResource(R.string.open_overview_content_description)
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .defaultMinSize(minWidth = 64.dp, minHeight = 48.dp)
            .clip(pillShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            )
            .semantics { contentDescription = label }
            .padding(horizontal = 24.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(width = 40.dp, height = 4.dp)
                .clip(pillShape)
                .background(colors.wallpaperForeground.copy(alpha = 0.72f)),
        )
    }
}

@Composable
private fun DockSearchBarPill(
    backgroundAlpha: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    @Suppress("UNUSED_PARAMETER") isVisible: Boolean = true,
) {
    val colors = OverviewTheme.colors
    val isExpressive = OverviewTheme.components.isExpressive

    val shape = OverviewTheme.shapes.pill
    val clampedAlpha = backgroundAlpha.coerceIn(0f, 1f)
    val effectiveAlpha = clampedAlpha.coerceAtLeast(0.28f)
    val backgroundColor = if (isExpressive) {
        lerp(colors.surfaceContainerHigh, colors.accentContainer, 0.18f).copy(alpha = effectiveAlpha)
    } else {
        colors.dashBackground.copy(alpha = effectiveAlpha)
    }
    val borderColor = if (isExpressive) {
        colors.outlineVariant.copy(alpha = (colors.outlineVariant.alpha * effectiveAlpha).coerceIn(0f, 1f))
    } else {
        colors.hairlineBorder.copy(alpha = (colors.hairlineBorder.alpha * effectiveAlpha).coerceIn(0f, 1f))
    }

    Row(
        modifier = modifier
            .widthIn(max = 420.dp)
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp)
            .then(
                if (isExpressive) {
                    Modifier.shadow(
                        elevation = (4f * effectiveAlpha).dp,
                        shape = shape,
                        clip = false,
                    )
                } else {
                    Modifier
                },
            )
            .clip(shape)
            .background(backgroundColor)
            .border(
                width = 1.dp,
                color = borderColor,
                shape = shape,
            )
            .clickable(role = Role.Button, onClick = onClick)
            .padding(
                horizontal = if (isExpressive) 12.dp else 16.dp,
                vertical = if (isExpressive) 8.dp else 10.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (isExpressive) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(colors.accentContainer.copy(alpha = (0.85f + 0.15f * effectiveAlpha))),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = stringResource(R.string.search_apps_content_description),
                    tint = colors.onAccentContainer,
                    modifier = Modifier.size(16.dp),
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = stringResource(R.string.search_apps_hint),
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = colors.onSurface.copy(alpha = 0.85f),
            )
        } else {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = stringResource(R.string.search_apps_content_description),
                tint = colors.onSurfaceMuted,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = stringResource(R.string.search_apps_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceMuted,
            )
        }
    }
}

@Composable
private fun HomePageIndicator(
    pageCount: Int,
    currentPage: Int,
    onSelectPage: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = OverviewTheme.colors
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier
                .clip(CircleShape)
                .background(colors.surfaceElevated.copy(alpha = 0.45f)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            for (index in 0 until pageCount) {
                val isSelected = index == currentPage
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .clickable(role = Role.Tab) { onSelectPage(index) },
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .size(width = if (isSelected) 16.dp else 6.dp, height = 6.dp)
                            .background(
                                if (isSelected) {
                                    colors.accent
                                } else {
                                    colors.wallpaperForeground.copy(alpha = 0.45f)
                                },
                            ),
                    )
                }
            }
        }
    }
}
