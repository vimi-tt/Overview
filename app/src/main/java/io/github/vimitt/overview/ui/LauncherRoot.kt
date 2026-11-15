package io.github.vimitt.overview.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.vimitt.overview.R
import io.github.vimitt.overview.data.AppWidgetController
import io.github.vimitt.overview.data.WidgetAppGroup
import io.github.vimitt.overview.data.WidgetEntry
import io.github.vimitt.overview.di.AppContainer
import io.github.vimitt.overview.domain.model.AppInfo
import io.github.vimitt.overview.ui.home.HomeAppPickerScreen
import io.github.vimitt.overview.ui.home.HomeItemActionOverlay
import io.github.vimitt.overview.ui.home.HomeScreen
import io.github.vimitt.overview.ui.home.HomeUiState
import io.github.vimitt.overview.ui.home.HomeWidgetPickerScreen
import io.github.vimitt.overview.ui.home.HomeWorkspaceItem
import io.github.vimitt.overview.ui.overview.OverviewScreen
import io.github.vimitt.overview.ui.overview.OverviewUiState
import io.github.vimitt.overview.ui.settings.SettingsNavHost
import io.github.vimitt.overview.ui.theme.OverviewCornerRadius
import io.github.vimitt.overview.ui.theme.OverviewTheme
import io.github.vimitt.overview.ui.theme.overviewTween
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
    blurRadiusDp: Float,
    appWidgetController: AppWidgetController,
    homeUiState: HomeUiState,
    searchQuery: String,
    overviewUiState: OverviewUiState,
    allApps: List<AppInfo>,
    onOpenOverview: () -> Unit,
    onOpenOverviewSearch: () -> Unit,
    onCloseOverview: () -> Unit,
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
    onOpenSettings: () -> Unit,
    onCloseSettings: () -> Unit,
    onOpenWallpaperPicker: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onClearSearch: () -> Unit,
    onAddAppFromOverview: (AppInfo) -> Unit,
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

    val anyModalFullScreen = isSettingsOpen || isAppPickerOpen || isWidgetPickerOpen
    val overviewActive = isOverviewOpen && !anyModalFullScreen
    val isDraggingFromOverviewActive =
        activeDrag?.payload is DragPayload.FromOverview && activeDrag?.hasMovedPastSlop == true
    val overviewContentVisible = overviewActive || (activeDrag?.payload is DragPayload.FromOverview)
    val blurActive = overviewActive && !isDraggingFromOverviewActive

    val blurProgress by animateFloatAsState(
        targetValue = if (blurActive) 1f else 0f,
        animationSpec = overviewTween(reducedMotion, durationMillis = 160),
        label = "overviewBlurProgress",
    )

    val homeBlurPx = with(density) { (blurRadiusDp * blurProgress).dp.toPx() }
    val windowBlurPx = with(density) {
        if (blurActive) blurRadiusDp.dp.toPx().roundToInt().coerceAtMost(48) else 0
    }

    WindowBackgroundBlurEffect(
        context = context,
        blurRadiusPx = windowBlurPx,
    )

    Box(modifier = modifier.fillMaxSize()) {
        HomeScreen(
            uiState = homeUiState,
            appWidgetController = appWidgetController,
            activeDrag = activeDrag,
            dropTarget = currentDropTarget,
            onAppClick = { app, view, bounds ->
                onLaunchFromHome(app, view, bounds, reducedMotion)
            },
            onOpenOverview = onOpenOverview,
            onOpenOverviewSearch = onOpenOverviewSearch,
            onLongPress = onHomeLongPress,
            canPlaceMovingItemAt = canPlaceMovingItemAt,
            onPlaceMovingItemAt = onPlaceMovingItemAt,
            onCancelMovingItem = onCancelMovingItem,
            onGridBoundsChanged = { bounds -> homeGridBounds = bounds },
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
                val target = currentDropTarget
                activeDrag = null
                if (finished != null && finished.payload is DragPayload.FromHome) {
                    if (!finished.hasMovedPastSlop) {
                        onWorkspaceItemLongPress(finished.payload.item)
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
            modifier = Modifier.graphicsLayer {
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

        val enterTransition = if (reducedMotion) {
            fadeIn(overviewTween(reducedMotion = true))
        } else {
            fadeIn(overviewTween(reducedMotion = false, durationMillis = 180)) +
                slideInVertically(
                    animationSpec = overviewTween(reducedMotion = false, durationMillis = 200),
                    initialOffsetY = { fullHeight -> fullHeight / 10 },
                )
        }

        val exitTransition = if (reducedMotion) {
            fadeOut(overviewTween(reducedMotion = true))
        } else {
            fadeOut(overviewTween(reducedMotion = false, durationMillis = 140)) +
                slideOutVertically(
                    animationSpec = overviewTween(reducedMotion = false, durationMillis = 140),
                    targetOffsetY = { fullHeight -> fullHeight / 12 },
                )
        }

        AnimatedVisibility(
            visible = overviewContentVisible,
            enter = enterTransition,
            exit = if (lastClosedByDrag) ExitTransition.None else exitTransition,
        ) {
            OverviewScreen(
                isActive = overviewActive && !isDraggingFromOverviewActive,
                isDraggingToHome = isDraggingFromOverviewActive,
                searchQuery = searchQuery,
                uiState = overviewUiState,
                focusSearchTrigger = overviewFocusSearchTrigger,
                onSearchQueryChange = onSearchQueryChange,
                onClearSearch = onClearSearch,
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
                    val target = currentDropTarget
                    activeDrag = null
                    val wasDrag = finished?.hasMovedPastSlop == true
                    if (wasDrag && finished.payload is DragPayload.FromOverview && target != null) {
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
                onOpenSettings = onOpenSettings,
            )
        }

        AnimatedVisibility(
            visible = isHomeMenuOpen && !isOverviewOpen && !anyModalFullScreen,
            enter = fadeIn(overviewTween(reducedMotion, durationMillis = 160)),
            exit = fadeOut(overviewTween(reducedMotion, durationMillis = 140)),
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
            enter = enterTransition,
            exit = exitTransition,
        ) {
            SettingsNavHost(
                appContainer = appContainer,
                onOpenWallpaperPicker = onOpenWallpaperPicker,
                onClose = onCloseSettings,
            )
        }

        DragGhostOverlay(
            activeDrag = activeDrag,
            isOverRemoveZone = currentDropTarget?.isOverRemoveZone == true,
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
    val cardShape = RoundedCornerShape(OverviewCornerRadius.Large)
    val backdropInteraction = remember { MutableInteractionSource() }
    val cardInteraction = remember { MutableInteractionSource() }

    BackHandler(onBack = onDismiss)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.36f))
            .clickable(
                interactionSource = backdropInteraction,
                indication = null,
                onClick = onDismiss,
            )
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 24.dp),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 420.dp)
                .fillMaxWidth()
                .clip(cardShape)
                .background(colors.surface)
                .border(width = 1.dp, color = colors.hairlineBorder, shape = cardShape)
                .clickable(
                    interactionSource = cardInteraction,
                    indication = null,
                    onClick = {},
                ),
        ) {
            Text(
                text = stringResource(R.string.home_menu_title),
                style = MaterialTheme.typography.labelMedium,
                color = colors.onSurfaceMuted,
                modifier = Modifier.padding(start = 20.dp, top = 16.dp, bottom = 8.dp),
            )

            HomeMenuRow(
                icon = Icons.Filled.Add,
                label = stringResource(R.string.home_menu_add_app),
                onClick = onAddApp,
            )

            HorizontalDivider(thickness = 1.dp, color = colors.hairlineBorder)

            HomeMenuRow(
                icon = Icons.Filled.Star,
                label = stringResource(R.string.home_menu_add_widget),
                onClick = onAddWidget,
            )

            HorizontalDivider(thickness = 1.dp, color = colors.hairlineBorder)

            HomeMenuRow(
                icon = Icons.Filled.Settings,
                label = stringResource(R.string.home_menu_customize),
                onClick = onCustomize,
            )

            HorizontalDivider(thickness = 1.dp, color = colors.hairlineBorder)

            HomeMenuRow(
                icon = Icons.Filled.Edit,
                label = stringResource(R.string.home_menu_wallpaper),
                onClick = onChangeWallpaper,
            )
        }
    }
}

@Composable
private fun HomeMenuRow(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    val colors = OverviewTheme.colors

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = colors.accent,
            modifier = Modifier.size(22.dp),
        )
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            color = colors.onSurface,
        )
    }
}

@Composable
private fun WindowBackgroundBlurEffect(
    context: Context,
    blurRadiusPx: Int,
) {
    val activity = context.findActivity() ?: return

    LaunchedEffect(activity, blurRadiusPx) {
        val windowManager = activity.getSystemService(WindowManager::class.java)
        val blurSupported = windowManager?.isCrossWindowBlurEnabled == true
        activity.window.setBackgroundBlurRadius(if (blurSupported) blurRadiusPx else 0)
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
