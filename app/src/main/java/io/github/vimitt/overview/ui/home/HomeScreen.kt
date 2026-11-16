package io.github.vimitt.overview.ui.home

import android.graphics.Rect
import android.os.SystemClock
import android.view.View
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.vimitt.overview.R
import io.github.vimitt.overview.data.AppWidgetController
import io.github.vimitt.overview.domain.model.AppInfo
import io.github.vimitt.overview.domain.model.GestureSettings
import io.github.vimitt.overview.domain.model.HomeItem
import io.github.vimitt.overview.domain.model.LauncherGestureAction
import io.github.vimitt.overview.ui.ActiveDragState
import io.github.vimitt.overview.ui.DockDropTarget
import io.github.vimitt.overview.ui.DragPayload
import io.github.vimitt.overview.ui.HomeDropTarget
import io.github.vimitt.overview.ui.components.AppIconItem
import io.github.vimitt.overview.ui.components.DashBar
import io.github.vimitt.overview.ui.components.HomeClock
import io.github.vimitt.overview.ui.home.widgets.NativeWidgetActions
import io.github.vimitt.overview.ui.home.widgets.NativeWidgetHost
import io.github.vimitt.overview.ui.home.widgets.NativeWidgetLiveEnvironment
import io.github.vimitt.overview.ui.home.widgets.rememberNativeWidgetEnvironment
import io.github.vimitt.overview.ui.theme.OverviewTheme

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
    onOpenOverview: () -> Unit,
    onOpenOverviewSearch: () -> Unit,
    onLongPress: () -> Unit,
    canPlaceMovingItemAt: (cellX: Int, cellY: Int) -> Boolean,
    onPlaceMovingItemAt: (cellX: Int, cellY: Int) -> Unit,
    onCancelMovingItem: () -> Unit,
    canPlaceDragItemAt: (cellX: Int, cellY: Int, spanX: Int, spanY: Int, ignoreItemId: String?) -> Boolean = { _, _, _, _, _ -> false },
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
    isVisible: Boolean = true,
    dockDropTarget: DockDropTarget? = null,
    onDockBoundsChanged: (ComposeRect?) -> Unit = {},
    onStartDockDrag: (AppInfo, Int, Offset) -> Unit = { _, _, _ -> },
    onDockDragMove: (Offset) -> Unit = {},
    onDockDragEnd: () -> Unit = {},
    onDockDragCancel: () -> Unit = {},
    nativeWidgetActions: NativeWidgetActions = NativeWidgetActions(),
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

    Box(modifier = modifier.fillMaxSize()) {
        // Fundo vazio recebe toque longo/duplo apenas fora de widgets, atalhos e dock.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(onLongPress, doubleTapAction, isMoving, isResizing, isDragInProgress) {
                    if (isMoving || isDragInProgress) return@pointerInput
                    detectTapGestures(
                        onTap = if (isResizing) {
                            { onCancelResizingItem() }
                        } else {
                            null
                        },
                        onLongPress = if (!isResizing) {
                            {
                                val sinceDrag = SystemClock.uptimeMillis() - lastDragActiveEndUptimeMs
                                if (!currentDragInProgress && !currentIsMoving && !currentIsResizing && sinceDrag > 400L) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onLongPress()
                                }
                            }
                        } else {
                            null
                        },
                        onDoubleTap = if (!isResizing && doubleTapAction != LauncherGestureAction.NONE) {
                            {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onExecuteGesture(doubleTapAction)
                            }
                        } else {
                            null
                        },
                    )
                },
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
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
                }
                ResizeModeBanner(
                    title = label,
                    onMove = { onStartMovingResizedItem(resizingItem.item.id) },
                    onRemove = { onRemoveResizedItem(resizingItem.item.id) },
                    onDone = onCancelResizingItem,
                    modifier = Modifier.padding(top = 8.dp, bottom = 8.dp),
                )
            } else if (uiState.showClock) {
                HomeClock(
                    style = uiState.clockStyle,
                    compact = hasWorkspaceItems,
                    backgroundAlpha = uiState.clockBackgroundAlpha,
                    isVisible = isVisible,
                    modifier = Modifier
                        .graphicsLayer {
                            alpha = if (isDraggingRemovable) 0f else 1f
                        }
                        .padding(
                            top = if (hasWorkspaceItems) 8.dp else 44.dp,
                            bottom = if (hasWorkspaceItems) 8.dp else 0.dp,
                        ),
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .onPlaced { coordinates ->
                        onGridBoundsChanged(coordinates.boundsInWindow())
                    },
            ) {
                if (hasWorkspaceItems || isMoving || isDraggingOverGrid) {
                    HomeWorkspaceGrid(
                        items = uiState.workspaceItems,
                        frequentApps = uiState.frequentApps,
                        nativeWidgetsState = uiState.nativeWidgets,
                        nativeEnvironment = nativeEnvironment,
                        nativeWidgetActions = nativeWidgetActions,
                        isVisible = isVisible,
                        gridColumns = uiState.gridColumns,
                        gridRows = uiState.gridRows,
                        iconScale = uiState.iconScale,
                        showLabels = uiState.showLabels,
                        labelScale = uiState.labelScale,
                        showNotificationDots = uiState.showNotificationDots,
                        badgePackages = uiState.badgePackages,
                        movingItemId = uiState.movingItemId,
                        resizingItem = resizingItem,
                        activeDrag = activeDrag,
                        dropTarget = dropTarget,
                        appWidgetController = appWidgetController,
                        onAppClick = onAppClick,
                        canPlaceMovingItemAt = canPlaceMovingItemAt,
                        onPlaceMovingItemAt = onPlaceMovingItemAt,
                        canPlaceItem = canPlaceDragItemAt,
                        onResizeWidgetBounds = onResizeWidgetBounds,
                        onStartItemDrag = onStartItemDrag,
                        onItemDragMove = onItemDragMove,
                        onItemDragEnd = onItemDragEnd,
                        onItemDragCancel = onItemDragCancel,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
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
                    modifier = Modifier.onPlaced { coordinates ->
                        onDockBoundsChanged(coordinates.boundsInWindow())
                    },
                )
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
    showNotificationDots: Boolean,
    badgePackages: Set<String>,
    movingItemId: String?,
    resizingItem: HomeWorkspaceItem?,
    activeDrag: ActiveDragState?,
    dropTarget: HomeDropTarget?,
    appWidgetController: AppWidgetController,
    onAppClick: (app: AppInfo, sourceView: View, sourceBounds: Rect?) -> Unit,
    canPlaceMovingItemAt: (cellX: Int, cellY: Int) -> Boolean,
    onPlaceMovingItemAt: (cellX: Int, cellY: Int) -> Unit,
    canPlaceItem: (cellX: Int, cellY: Int, spanX: Int, spanY: Int, ignoreItemId: String?) -> Boolean,
    onResizeWidgetBounds: (itemId: String, cellX: Int, cellY: Int, spanX: Int, spanY: Int) -> Unit,
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
                val targetColor = if (dropTarget.isValid) {
                    colors.accent
                } else {
                    Color(0xFFE01B24)
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
                        .padding(4.dp)
                        .clip(targetShape)
                        .background(targetColor.copy(alpha = 0.24f))
                        .border(width = 2.dp, color = targetColor, shape = targetShape),
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
    val dangerColor = Color(0xFFE01B24)

    Row(
        modifier = modifier
            .clip(shape)
            .background(
                if (isActive) {
                    dangerColor.copy(alpha = 0.90f)
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
            tint = if (isActive) Color.White else dangerColor,
            modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = stringResource(R.string.home_drag_remove),
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            color = if (isActive) Color.White else colors.onSurface,
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
    val dangerColor = Color(0xFFE01B24)

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
            .padding(start = 14.dp, end = 6.dp, top = 2.dp, bottom = 2.dp),
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
        Spacer(modifier = Modifier.width(6.dp))
        TextButton(onClick = onMove) {
            Text(
                text = stringResource(R.string.home_resize_move),
                color = colors.onSurface,
            )
        }
        IconButton(
            onClick = onRemove,
            modifier = Modifier.size(36.dp),
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
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(colors.accent),
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
            .clip(pillShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            )
            .semantics { contentDescription = label }
            .padding(horizontal = 24.dp, vertical = 10.dp),
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
    isVisible: Boolean = true,
) {
    val colors = OverviewTheme.colors
    val motion = OverviewTheme.motion
    val isExpressive = OverviewTheme.components.isExpressive
    val expressiveMotionEnabled = isExpressive && motion.usesSprings && !motion.reducedMotion
    val animateWhenVisible = expressiveMotionEnabled && isVisible

    val entranceScale by animateFloatAsState(
        targetValue = if (isVisible || !expressiveMotionEnabled) 1f else 0.94f,
        animationSpec = if (animateWhenVisible) motion.expressiveSpec() else snap(),
        label = "dock_search_scale",
    )
    val entranceAlpha by animateFloatAsState(
        targetValue = if (isVisible || !expressiveMotionEnabled) 1f else 0f,
        animationSpec = if (animateWhenVisible) motion.effectSpec(durationMillis = 140) else snap(),
        label = "dock_search_alpha",
    )

    val shape = OverviewTheme.shapes.pill
    val clampedAlpha = backgroundAlpha.coerceIn(0f, 1f)
    val effectiveAlpha = clampedAlpha.coerceAtLeast(0.28f)
    val backgroundColor = if (isExpressive) {
        lerp(colors.surfaceContainerHigh, colors.accentContainer, 0.24f).copy(alpha = effectiveAlpha)
    } else {
        colors.dashBackground.copy(alpha = effectiveAlpha)
    }
    val borderColor = if (isExpressive) {
        colors.accent.copy(alpha = 0.26f * effectiveAlpha)
    } else {
        colors.hairlineBorder.copy(alpha = colors.hairlineBorder.alpha * effectiveAlpha)
    }

    Row(
        modifier = modifier
            .widthIn(max = 420.dp)
            .fillMaxWidth()
            .then(
                if (expressiveMotionEnabled) {
                    Modifier.graphicsLayer {
                        scaleX = entranceScale
                        scaleY = entranceScale
                        alpha = entranceAlpha
                    }
                } else {
                    Modifier
                },
            )
            .then(
                if (isExpressive) {
                    Modifier.shadow(
                        elevation = (8f * effectiveAlpha).dp,
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
