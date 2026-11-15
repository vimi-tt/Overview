package io.github.vimitt.overview.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.WindowManager
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect as ComposeRect
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import io.github.vimitt.overview.R
import io.github.vimitt.overview.data.AppWidgetController
import io.github.vimitt.overview.data.WidgetAppGroup
import io.github.vimitt.overview.data.WidgetEntry
import io.github.vimitt.overview.di.AppContainer
import io.github.vimitt.overview.domain.model.AppInfo
import io.github.vimitt.overview.domain.model.GestureSettings
import io.github.vimitt.overview.domain.model.LauncherGestureAction
import io.github.vimitt.overview.domain.model.OverviewSettings
import io.github.vimitt.overview.domain.model.OverviewTransitionStyle
import io.github.vimitt.overview.ui.components.OverviewPopupActionRow
import io.github.vimitt.overview.ui.components.OverviewPopupDivider
import io.github.vimitt.overview.ui.components.OverviewPopupSurface
import io.github.vimitt.overview.ui.components.rememberIsLifecycleActive
import io.github.vimitt.overview.ui.home.HomeAppPickerScreen
import io.github.vimitt.overview.ui.home.HomeItemActionOverlay
import io.github.vimitt.overview.ui.home.HomeScreen
import io.github.vimitt.overview.ui.home.HomeUiState
import io.github.vimitt.overview.ui.home.HomeWidgetPickerScreen
import io.github.vimitt.overview.ui.home.HomeWorkspaceItem
import io.github.vimitt.overview.ui.overview.OverviewScreen
import io.github.vimitt.overview.ui.overview.OverviewUiState
import io.github.vimitt.overview.ui.settings.SettingsNavHost
import io.github.vimitt.overview.ui.theme.OverviewTheme
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Raiz visual do launcher que coordena Home, Overview, seletores de apps/widgets,
 * menu contextual e tela de personalização.
 */
@Composable
fun LauncherRoot(
    appContainer: AppContainer,
    isOverviewOpen: Boolean,
    overviewFocusSearchTrigger: Int,
    isHomeMenuOpen: Boolean,
    isSettingsOpen: Boolean,
    isAppPickerOpen: Boolean,
    isWidgetPickerOpen: Boolean,
    selectedHomeItem: HomeWorkspaceItem?,
    overviewSettings: OverviewSettings,
    gestureSettings: GestureSettings,
    appWidgetController: AppWidgetController,
    homeUiState: HomeUiState,
    searchQuery: String,
    overviewUiState: OverviewUiState,
    allApps: List<AppInfo>,
    onOpenOverview: () -> Unit,
    onOpenOverviewSearch: () -> Unit,
    onCloseOverview: () -> Unit,
    onExecuteGesture: (LauncherGestureAction) -> Unit,
    onHomeLongPress: () -> Unit,
    onDismissHomeMenu: () -> Unit,
    onOpenAppPicker: () -> Unit,
    onCloseAppPicker: () -> Unit,
    onOpenWidgetPicker: () -> Unit,
    onCloseWidgetPicker: () -> Unit,
    onSelectHomeAppToAdd: (AppInfo) -> Unit,
    onSelectWidgetToAdd: (WidgetEntry) -> Unit,
    loadAvailableWidgets: suspend () -> List<WidgetAppGroup>,
    onWorkspaceItemLongPress: (HomeWorkspaceItem) -> Unit,
    onDismissHomeItemAction: () -> Unit,
    onStartMovingHomeItem: (String) -> Unit,
    onResizeWidget: (itemId: String, deltaX: Int, deltaY: Int) -> Unit,
    onRemoveHomeItem: (String) -> Unit,
    canPlaceMovingItemAt: (cellX: Int, cellY: Int) -> Boolean,
    onPlaceMovingItemAt: (cellX: Int, cellY: Int) -> Unit,
    onCancelMovingItem: () -> Unit,
    canPlaceDragItemAt: (cellX: Int, cellY: Int, spanX: Int, spanY: Int, ignoreItemId: String?) -> Boolean,
    onMoveHomeItemTo: (itemId: String, cellX: Int, cellY: Int) -> Unit,
    onDropAppFromOverviewAt: (app: AppInfo, cellX: Int, cellY: Int) -> Unit,
    onDropAppOnDock: (app: AppInfo, targetIndex: Int) -> Unit,
    onRemoveAppFromDock: (appKey: String) -> Unit,
    onOpenSettings: () -> Unit,
    onCloseSettings: () -> Unit,
    onOpenWallpaperPicker: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onClearSearch: () -> Unit,
    onAddAppFromOverview: (AppInfo) -> Unit,
    onMoveAppToFolder: (appKey: String, folderId: String) -> Unit,
    onCreateFolderWithApp: (name: String, appKey: String) -> Unit,
    onRemoveAppFromFolder: (appKey: String, folderId: String) -> Unit,
    onOpenAppInfo: (AppInfo) -> Unit,
    onLaunchFromHome: (
        app: AppInfo,
        sourceView: android.view.View,
        sourceBounds: android.graphics.Rect?,
        reducedMotion: Boolean,
    ) -> Unit,
    onLaunchFromOverview: (
        app: AppInfo,
        sourceView: android.view.View,
        sourceBounds: android.graphics.Rect?,
        reducedMotion: Boolean,
    ) -> Unit,
    modifier: Modifier = Modifier,
) {
    val reducedMotion = OverviewTheme.reducedMotion
    val motion = OverviewTheme.motion
    val shapes = OverviewTheme.shapes
    val density = LocalDensity.current
    val context = LocalContext.current
    val viewConfig = LocalViewConfiguration.current
    val showLockedToast = remember(context) {
        {
            Toast.makeText(
                context,
                R.string.home_layout_locked_notice,
                Toast.LENGTH_SHORT,
            ).show()
        }
    }

    var activeDrag by remember { mutableStateOf<ActiveDragState?>(null) }
    var homeGridBounds by remember { mutableStateOf<ComposeRect?>(null) }
    var dockBounds by remember { mutableStateOf<ComposeRect?>(null) }
    var removeZoneBounds by remember { mutableStateOf<ComposeRect?>(null) }
    var lastClosedByDrag by remember { mutableStateOf(false) }

    LaunchedEffect(isOverviewOpen) {
        if (isOverviewOpen) {
            lastClosedByDrag = false
        }
    }

    val dragSlopPx = maxOf(
        viewConfig.touchSlop * 1.5f,
        with(density) { 14.dp.toPx() },
    )
    val dragSlopSqPx = dragSlopPx * dragSlopPx

    val updateDragPosition: (Offset) -> Unit = { pointerPos ->
        val current = activeDrag
        if (current != null) {
            val movedPast = current.hasMovedPastSlop ||
                (pointerPos - current.startInWindow).getDistanceSquared() >= dragSlopSqPx
            activeDrag = current.copy(
                pointerInWindow = pointerPos,
                hasMovedPastSlop = movedPast,
            )
        }
    }

    val currentDockTarget: DockDropTarget? = activeDrag?.takeIf { it.hasMovedPastSlop }?.let { drag ->
        computeDockDropTarget(
            pointerInWindow = drag.pointerInWindow,
            dockBounds = dockBounds,
            itemCount = homeUiState.dashApps.size,
            payload = drag.payload,
        )
    }

    val currentDropTarget: HomeDropTarget? = activeDrag?.takeIf { it.hasMovedPastSlop }?.let { drag ->
        computeHomeDropTarget(
            pointerInWindow = drag.pointerInWindow,
            gridBounds = homeGridBounds,
            removeZoneBounds = removeZoneBounds,
            payload = drag.payload,
            canPlace = canPlaceDragItemAt,
            gridColumns = homeUiState.gridColumns,
            gridRows = homeUiState.gridRows,
        )
    }
    val effectiveDropTarget: HomeDropTarget? = if (currentDockTarget != null) null else currentDropTarget

    val isLifecycleActive = rememberIsLifecycleActive()
    val anyModalFullScreen = isSettingsOpen || isAppPickerOpen || isWidgetPickerOpen
    val overviewActive = isOverviewOpen && !anyModalFullScreen
    val isDraggingFromOverviewActive =
        activeDrag?.payload is DragPayload.FromOverview && activeDrag?.hasMovedPastSlop == true

    val overviewProgress = remember { Animatable(if (isOverviewOpen) 1f else 0f) }
    var interactiveDragFraction by remember { mutableStateOf<Float?>(null) }

    LaunchedEffect(anyModalFullScreen) {
        if (anyModalFullScreen) {
            interactiveDragFraction = null
        }
    }

    LaunchedEffect(
        overviewActive,
        interactiveDragFraction,
        isDraggingFromOverviewActive,
        lastClosedByDrag,
        overviewSettings.animationSpeed,
        motion,
    ) {
        if (isDraggingFromOverviewActive) {
            overviewProgress.snapTo(0f)
            return@LaunchedEffect
        }
        val dragFraction = interactiveDragFraction
        if (dragFraction != null) {
            overviewProgress.snapTo(dragFraction)
        } else if (overviewActive) {
            overviewProgress.animateTo(
                targetValue = 1f,
                animationSpec = motion.spatialSpec(
                    durationMillis = 200,
                    durationScale = overviewSettings.animationSpeed.durationScale,
                ),
            )
        } else if (lastClosedByDrag) {
            overviewProgress.snapTo(0f)
        } else {
            overviewProgress.animateTo(
                targetValue = 0f,
                animationSpec = motion.spatialSpec(
                    durationMillis = 160,
                    durationScale = overviewSettings.animationSpeed.durationScale,
                ),
            )
        }
    }

    val currentOverviewProgress = overviewProgress.value
    val shouldComposeOverview = overviewActive ||
        currentOverviewProgress > 0.005f ||
        interactiveDragFraction != null ||
        (activeDrag?.payload is DragPayload.FromOverview)

    val blurFraction = if (isDraggingFromOverviewActive) 0f else currentOverviewProgress.coerceIn(0f, 1.05f)
    val homeBlurPx = with(density) { (overviewSettings.blurRadiusDp * blurFraction).dp.toPx() }
    val windowBlurPx = with(density) {
        (overviewSettings.blurRadiusDp * blurFraction).dp.toPx().roundToInt().coerceIn(0, 48)
    }
    val homeScale = if (overviewSettings.homeDepthEffect && !reducedMotion && !isDraggingFromOverviewActive) {
        1f - (0.055f * currentOverviewProgress)
    } else {
        1f
    }

    WindowBackgroundBlurEffect(
        context = context,
        blurRadiusPx = windowBlurPx,
    )

    val isHomeVisible = isLifecycleActive &&
        !anyModalFullScreen &&
        (!overviewActive || currentOverviewProgress < 0.95f)

    Box(modifier = modifier.fillMaxSize()) {
        HomeScreen(
            uiState = homeUiState,
            appWidgetController = appWidgetController,
            activeDrag = activeDrag,
            dropTarget = effectiveDropTarget,
            isVisible = isHomeVisible,
            dockDropTarget = currentDockTarget,
            gestureSettings = gestureSettings,
            onExecuteGesture = onExecuteGesture,
            onAppClick = { app, view, bounds ->
                onLaunchFromHome(app, view, bounds, reducedMotion)
            },
            onOpenOverview = onOpenOverview,
            onOpenOverviewSearch = onOpenOverviewSearch,
            onOverviewDragProgress = { fraction ->
                interactiveDragFraction = if (fraction > 0f) fraction else null
            },
            onOverviewDragEnd = {
                interactiveDragFraction = null
            },
            onLongPress = onHomeLongPress,
            canPlaceMovingItemAt = canPlaceMovingItemAt,
            onPlaceMovingItemAt = onPlaceMovingItemAt,
            onCancelMovingItem = onCancelMovingItem,
            onGridBoundsChanged = { bounds -> homeGridBounds = bounds },
            onDockBoundsChanged = { bounds -> dockBounds = bounds },
            onRemoveZoneBoundsChanged = { bounds -> removeZoneBounds = bounds },
            onStartItemDrag = { item, startPos ->
                if (homeUiState.lockLayout) {
                    showLockedToast()
                } else {
                    activeDrag = ActiveDragState(
                        payload = DragPayload.FromHome(item),
                        startInWindow = startPos,
                        pointerInWindow = startPos,
                        hasMovedPastSlop = false,
                    )
                }
            },
            onItemDragMove = updateDragPosition,
            onItemDragEnd = {
                val finished = activeDrag
                val target = effectiveDropTarget
                val dockTarget = currentDockTarget
                activeDrag = null
                if (finished != null && finished.payload is DragPayload.FromHome) {
                    val draggedShortcutApp = finished.payload.draggedApp
                    if (!finished.hasMovedPastSlop) {
                        onWorkspaceItemLongPress(finished.payload.item)
                    } else if (dockTarget != null && draggedShortcutApp != null) {
                        onDropAppOnDock(draggedShortcutApp, dockTarget.targetIndex)
                    } else if (target != null && target.isOverRemoveZone) {
                        onRemoveHomeItem(finished.payload.item.item.id)
                    } else if (target != null && target.isValid) {
                        onMoveHomeItemTo(
                            finished.payload.item.item.id,
                            target.cellX,
                            target.cellY,
                        )
                    }
                }
            },
            onItemDragCancel = {
                activeDrag = null
            },
            onStartDockDrag = { app, index, startPos ->
                if (homeUiState.lockLayout) {
                    showLockedToast()
                } else {
                    activeDrag = ActiveDragState(
                        payload = DragPayload.FromDock(app = app, sourceIndex = index),
                        startInWindow = startPos,
                        pointerInWindow = startPos,
                        hasMovedPastSlop = false,
                    )
                }
            },
            onDockDragMove = updateDragPosition,
            onDockDragEnd = {
                val finished = activeDrag
                val target = effectiveDropTarget
                val dockTarget = currentDockTarget
                activeDrag = null
                if (finished != null && finished.payload is DragPayload.FromDock && finished.hasMovedPastSlop) {
                    if (dockTarget != null) {
                        onDropAppOnDock(finished.payload.app, dockTarget.targetIndex)
                    } else if (target != null && target.isOverRemoveZone) {
                        onRemoveAppFromDock(finished.payload.app.key)
                    } else if (target != null && target.isValid) {
                        onDropAppFromOverviewAt(
                            finished.payload.app,
                            target.cellX,
                            target.cellY,
                        )
                    }
                }
            },
            onDockDragCancel = {
                activeDrag = null
            },
            modifier = Modifier.graphicsLayer {
                scaleX = homeScale
                scaleY = homeScale
                renderEffect = if (homeBlurPx > 0.5f) {
                    BlurEffect(
                        radiusX = homeBlurPx,
                        radiusY = homeBlurPx,
                        edgeTreatment = TileMode.Clamp,
                    )
                } else {
                    null
                }
            },
        )

        val modalDurationScale = overviewSettings.animationSpeed.durationScale
        val enterTransition = if (reducedMotion) {
            fadeIn(motion.effectSpec())
        } else {
            fadeIn(motion.effectSpec(durationMillis = 180, durationScale = modalDurationScale)) +
                slideInVertically(
                    animationSpec = motion.spatialSpec(
                        durationMillis = 200,
                        durationScale = modalDurationScale,
                        visibilityThreshold = IntOffset.VisibilityThreshold,
                    ),
                    initialOffsetY = { fullHeight -> fullHeight / 10 },
                )
        }

        val exitTransition = if (reducedMotion) {
            fadeOut(motion.effectSpec())
        } else {
            fadeOut(motion.effectSpec(durationMillis = 140, durationScale = modalDurationScale)) +
                slideOutVertically(
                    animationSpec = motion.spatialSpec(
                        durationMillis = 140,
                        durationScale = modalDurationScale,
                        visibilityThreshold = IntOffset.VisibilityThreshold,
                    ),
                    targetOffsetY = { fullHeight -> fullHeight / 12 },
                )
        }

        val settingsEnterTransition = if (reducedMotion || !motion.usesSprings) {
            enterTransition
        } else {
            // Abertura expressiva com expansão central e mola mais presente para Configurações
            fadeIn(motion.effectSpec(durationMillis = 200, durationScale = modalDurationScale)) +
                scaleIn(
                    initialScale = 0.88f,
                    transformOrigin = TransformOrigin.Center,
                    animationSpec = motion.expressiveSpec(
                        durationMillis = 280,
                        durationScale = modalDurationScale,
                    ),
                ) +
                slideInVertically(
                    animationSpec = motion.expressiveSpec(
                        durationMillis = 280,
                        durationScale = modalDurationScale,
                        visibilityThreshold = IntOffset.VisibilityThreshold,
                    ),
                    initialOffsetY = { fullHeight -> fullHeight / 8 },
                )
        }

        val settingsExitTransition = if (reducedMotion || !motion.usesSprings) {
            exitTransition
        } else {
            fadeOut(motion.effectSpec(durationMillis = 150, durationScale = modalDurationScale)) +
                scaleOut(
                    targetScale = 0.92f,
                    transformOrigin = TransformOrigin.Center,
                    animationSpec = motion.spatialSpec(
                        durationMillis = 170,
                        durationScale = modalDurationScale,
                    ),
                ) +
                slideOutVertically(
                    animationSpec = motion.spatialSpec(
                        durationMillis = 170,
                        durationScale = modalDurationScale,
                        visibilityThreshold = IntOffset.VisibilityThreshold,
                    ),
                    targetOffsetY = { fullHeight -> fullHeight / 10 },
                )
        }

        if (shouldComposeOverview) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        val p = if (isDraggingFromOverviewActive) 1f else currentOverviewProgress
                        alpha = p.coerceIn(0f, 1f)
                        if (!reducedMotion && !isDraggingFromOverviewActive) {
                            when (overviewSettings.transitionStyle) {
                                OverviewTransitionStyle.GNOME_SHELL -> {
                                    val s = 0.94f + (0.06f * p)
                                    scaleX = s
                                    scaleY = s
                                    translationY = (1f - p) * size.height * 0.06f
                                }

                                OverviewTransitionStyle.SLIDE -> {
                                    translationY = (1f - p) * size.height * 0.14f
                                }

                                OverviewTransitionStyle.FADE -> Unit
                            }
                        }
                    },
            ) {
                OverviewScreen(
                    isActive = overviewActive && !isDraggingFromOverviewActive &&
                        (interactiveDragFraction == null || isOverviewOpen),
                    isDraggingToHome = isDraggingFromOverviewActive,
                    searchQuery = searchQuery,
                    uiState = overviewUiState,
                    focusSearchTrigger = overviewFocusSearchTrigger,
                    scrimAlpha = overviewSettings.scrimAlpha,
                    animationDurationScale = overviewSettings.animationSpeed.durationScale,
                    closeSensitivity = gestureSettings.sensitivity,
                    onSearchQueryChange = onSearchQueryChange,
                    onClearSearch = onClearSearch,
                    onMoveAppToFolder = onMoveAppToFolder,
                    onCreateFolderWithApp = onCreateFolderWithApp,
                    onRemoveAppFromFolder = onRemoveAppFromFolder,
                    onAppClick = { app, view, bounds ->
                        onLaunchFromOverview(app, view, bounds, reducedMotion)
                    },
                    onStartAppDrag = { app, startPos ->
                        activeDrag = ActiveDragState(
                            payload = DragPayload.FromOverview(app),
                            startInWindow = startPos,
                            pointerInWindow = startPos,
                            hasMovedPastSlop = false,
                        )
                    },
                    onAppDragMove = updateDragPosition,
                    onAppDragEnd = {
                        val finished = activeDrag
                        val target = effectiveDropTarget
                        val dockTarget = currentDockTarget
                        activeDrag = null
                        val wasDrag = finished?.hasMovedPastSlop == true
                        if (wasDrag && finished.payload is DragPayload.FromOverview) {
                            if (dockTarget != null) {
                                if (homeUiState.lockLayout) {
                                    showLockedToast()
                                } else {
                                    onDropAppOnDock(finished.payload.app, dockTarget.targetIndex)
                                    lastClosedByDrag = true
                                    onCloseOverview()
                                }
                            } else if (target != null) {
                                if (homeUiState.lockLayout) {
                                    showLockedToast()
                                } else if (target.isValid) {
                                    onDropAppFromOverviewAt(
                                        finished.payload.app,
                                        target.cellX,
                                        target.cellY,
                                    )
                                    lastClosedByDrag = true
                                    onCloseOverview()
                                }
                            }
                        }
                        wasDrag
                    },
                    onAppDragCancel = {
                        activeDrag = null
                    },
                    onAddAppToHome = { app ->
                        if (homeUiState.lockLayout) {
                            showLockedToast()
                        } else {
                            onAddAppFromOverview(app)
                        }
                    },
                    onOpenAppInfo = onOpenAppInfo,
                    onClose = onCloseOverview,
                    onCloseDragProgress = { closeFraction ->
                        interactiveDragFraction = (1f - closeFraction).coerceIn(0f, 1f)
                    },
                    onCloseDragEnd = {
                        interactiveDragFraction = null
                    },
                    onOpenSettings = onOpenSettings,
                )
            }
        }

        AnimatedVisibility(
            visible = isHomeMenuOpen && !isOverviewOpen && !anyModalFullScreen,
            enter = fadeIn(motion.effectSpec(durationMillis = 160)),
            exit = fadeOut(motion.effectSpec(durationMillis = 140)),
        ) {
            HomeContextMenuOverlay(
                onAddApp = {
                    onDismissHomeMenu()
                    if (homeUiState.lockLayout) showLockedToast() else onOpenAppPicker()
                },
                onAddWidget = {
                    onDismissHomeMenu()
                    if (homeUiState.lockLayout) showLockedToast() else onOpenWidgetPicker()
                },
                onCustomize = {
                    onDismissHomeMenu()
                    onOpenSettings()
                },
                onChangeWallpaper = {
                    onDismissHomeMenu()
                    onOpenWallpaperPicker()
                },
                onDismiss = onDismissHomeMenu,
            )
        }

        if (selectedHomeItem != null && !isOverviewOpen && !anyModalFullScreen) {
            HomeItemActionOverlay(
                workspaceItem = selectedHomeItem,
                onMove = {
                    onDismissHomeItemAction()
                    onStartMovingHomeItem(selectedHomeItem.item.id)
                },
                onResizeWidget = { deltaX, deltaY ->
                    onResizeWidget(selectedHomeItem.item.id, deltaX, deltaY)
                },
                onRemove = {
                    onDismissHomeItemAction()
                    onRemoveHomeItem(selectedHomeItem.item.id)
                },
                onDismiss = onDismissHomeItemAction,
            )
        }

        AnimatedVisibility(
            visible = isAppPickerOpen,
            enter = enterTransition,
            exit = exitTransition,
        ) {
            HomeAppPickerScreen(
                apps = allApps,
                onSelectApp = onSelectHomeAppToAdd,
                onClose = onCloseAppPicker,
            )
        }

        AnimatedVisibility(
            visible = isWidgetPickerOpen,
            enter = enterTransition,
            exit = exitTransition,
        ) {
            HomeWidgetPickerScreen(
                loadWidgets = loadAvailableWidgets,
                onSelectWidget = onSelectWidgetToAdd,
                onClose = onCloseWidgetPicker,
            )
        }

        AnimatedVisibility(
            visible = isSettingsOpen,
            enter = settingsEnterTransition,
            exit = settingsExitTransition,
        ) {
            SettingsNavHost(
                appContainer = appContainer,
                onOpenWallpaperPicker = onOpenWallpaperPicker,
                onClose = onCloseSettings,
            )
        }

        DragGhostOverlay(
            activeDrag = activeDrag,
            isOverRemoveZone = effectiveDropTarget?.isOverRemoveZone == true,
        )
    }
}

@Composable
private fun HomeContextMenuOverlay(
    onAddApp: () -> Unit,
    onAddWidget: () -> Unit,
    onCustomize: () -> Unit,
    onChangeWallpaper: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = OverviewTheme.colors
    val isExpressive = OverviewTheme.components.isExpressive

    OverviewPopupSurface(
        onDismiss = onDismiss,
        bottomSheet = true,
    ) {
        Text(
            text = stringResource(R.string.home_menu_title),
            style = if (isExpressive) {
                OverviewTheme.typography.sectionHeader
            } else {
                MaterialTheme.typography.labelMedium
            },
            color = if (isExpressive) colors.accent else colors.onSurfaceMuted,
            modifier = Modifier.padding(
                start = 20.dp,
                top = if (isExpressive) 10.dp else 16.dp,
                bottom = 8.dp,
            ),
        )

        OverviewPopupActionRow(
            icon = Icons.Filled.Add,
            label = stringResource(R.string.home_menu_add_app),
            onClick = onAddApp,
        )

        OverviewPopupDivider()

        OverviewPopupActionRow(
            icon = Icons.Filled.Star,
            label = stringResource(R.string.home_menu_add_widget),
            onClick = onAddWidget,
        )

        OverviewPopupDivider()

        OverviewPopupActionRow(
            icon = Icons.Filled.Settings,
            label = stringResource(R.string.home_menu_customize),
            onClick = onCustomize,
        )

        OverviewPopupDivider()

        OverviewPopupActionRow(
            icon = Icons.Filled.Edit,
            label = stringResource(R.string.home_menu_wallpaper),
            onClick = onChangeWallpaper,
        )
    }
}

@Composable
private fun WindowBackgroundBlurEffect(
    context: Context,
    blurRadiusPx: Int,
) {
    val activity = context.findActivity() ?: return
    var lastAppliedBlur by remember(activity) { mutableIntStateOf(-1) }

    LaunchedEffect(activity, blurRadiusPx) {
        val windowManager = activity.getSystemService(WindowManager::class.java)
        val blurSupported = windowManager?.isCrossWindowBlurEnabled == true
        val targetBlur = if (blurSupported) blurRadiusPx else 0
        val changedEnough = targetBlur == 0 && lastAppliedBlur != 0 ||
            abs(targetBlur - lastAppliedBlur) >= 2 ||
            (lastAppliedBlur == -1)
        if (changedEnough) {
            lastAppliedBlur = targetBlur
            activity.window.setBackgroundBlurRadius(targetBlur)
        }
    }

    DisposableEffect(activity) {
        onDispose {
            activity.window.setBackgroundBlurRadius(0)
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
