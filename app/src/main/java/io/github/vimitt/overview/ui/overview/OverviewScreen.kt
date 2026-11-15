package io.github.vimitt.overview.ui.overview

import android.graphics.Rect
import android.view.View
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.vimitt.overview.R
import io.github.vimitt.overview.domain.model.AppInfo
import io.github.vimitt.overview.ui.components.AppIconItem
import io.github.vimitt.overview.ui.components.OverviewSearchBar
import io.github.vimitt.overview.ui.theme.OverviewCornerRadius
import io.github.vimitt.overview.ui.theme.OverviewTheme
import io.github.vimitt.overview.ui.theme.overviewSpring
import io.github.vimitt.overview.ui.theme.overviewTween
import kotlin.math.ceil

/**
 * Tela do Overview inspirada no GNOME Shell: barra de busca no topo e grade
 * paginada horizontalmente com os aplicativos ordenados alfabeticamente.
 *
 * Não aplica `imePadding()` no contêiner da grade paginada para evitar recálculo
 * de linhas/páginas a cada frame da animação do teclado.
 */
@Composable
fun OverviewScreen(
    isActive: Boolean,
    isDraggingToHome: Boolean,
    searchQuery: String,
    uiState: OverviewUiState,
    onSearchQueryChange: (String) -> Unit,
    onClearSearch: () -> Unit,
    onAppClick: (app: AppInfo, sourceView: View, sourceBounds: Rect?) -> Unit,
    onStartAppDrag: (AppInfo, Offset) -> Unit,
    onAppDragMove: (Offset) -> Unit,
    onAppDragEnd: () -> Boolean,
    onAppDragCancel: () -> Unit,
    onAddAppToHome: (AppInfo) -> Unit,
    onOpenAppInfo: (AppInfo) -> Unit,
    onClose: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    focusSearchTrigger: Int = 0,
) {
    val colors = OverviewTheme.colors
    val reducedMotion = OverviewTheme.reducedMotion
    val keyboardController = LocalSoftwareKeyboardController.current
    val rootView = LocalView.current
    val swipeDownThresholdPx = with(LocalDensity.current) { 56.dp.toPx() }
    val isSearching = searchQuery.isNotBlank()
    val searchFocusRequester = remember { FocusRequester() }
    var handledFocusTrigger by remember { mutableIntStateOf(0) }
    var actionTargetApp by remember { mutableStateOf<AppInfo?>(null) }
    var dragCandidateApp by remember { mutableStateOf<AppInfo?>(null) }

    LaunchedEffect(isActive, focusSearchTrigger) {
        if (isActive && focusSearchTrigger > handledFocusTrigger) {
            handledFocusTrigger = focusSearchTrigger
            searchFocusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    val contentAlpha by animateFloatAsState(
        targetValue = if (isDraggingToHome) 0f else 1f,
        animationSpec = overviewTween(reducedMotion = reducedMotion, durationMillis = 140),
        label = "overviewDragFade",
    )

    val handleAppClick = remember(onAppClick, keyboardController) {
        { app: AppInfo, view: View, bounds: Rect? ->
            keyboardController?.hide()
            onAppClick(app, view, bounds)
        }
    }

    val handleAppDragStart = remember(onStartAppDrag, keyboardController) {
        { app: AppInfo, windowOffset: Offset ->
            keyboardController?.hide()
            dragCandidateApp = app
            onStartAppDrag(app, windowOffset)
        }
    }

    val handleAppDragEnd = remember(onAppDragEnd) {
        {
            val handledAsDrop = onAppDragEnd()
            val candidate = dragCandidateApp
            dragCandidateApp = null
            if (!handledAsDrop && candidate != null) {
                actionTargetApp = candidate
            }
        }
    }

    val handleAppDragCancel = remember(onAppDragCancel) {
        {
            dragCandidateApp = null
            onAppDragCancel()
        }
    }

    BackHandler(enabled = isActive && actionTargetApp == null) {
        keyboardController?.hide()
        onClose()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer { this.alpha = contentAlpha },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.overviewScrim)
                .pointerInput(isActive, isDraggingToHome, swipeDownThresholdPx) {
                    if (!isActive || isDraggingToHome) return@pointerInput
                    var totalDragY = 0f
                    var triggered = false
                    detectVerticalDragGestures(
                        onDragStart = {
                            totalDragY = 0f
                            triggered = false
                        },
                        onDragEnd = {
                            totalDragY = 0f
                            triggered = false
                        },
                        onDragCancel = {
                            totalDragY = 0f
                            triggered = false
                        },
                        onVerticalDrag = { _, dragAmount ->
                            totalDragY += dragAmount
                            if (!triggered && totalDragY >= swipeDownThresholdPx) {
                                triggered = true
                                keyboardController?.hide()
                                onClose()
                            }
                        },
                    )
                }
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            OverviewSearchBar(
                query = searchQuery,
                onQueryChange = onSearchQueryChange,
                onClear = onClearSearch,
                focusRequester = searchFocusRequester,
                onOpenSettings = {
                    keyboardController?.hide()
                    onOpenSettings()
                },
                onSubmit = {
                    if (uiState.apps.size == 1) {
                        keyboardController?.hide()
                        onAppClick(uiState.apps.first(), rootView, null)
                    } else {
                        keyboardController?.hide()
                    }
                },
                modifier = Modifier
                    .widthIn(max = 520.dp)
                    .padding(top = 8.dp),
            )

            Spacer(modifier = Modifier.height(20.dp))

            val iconSize = remember(uiState.iconScale) { (54f * uiState.iconScale).dp }

            when {
                uiState.apps.isEmpty() -> {
                    EmptyOverviewState(
                        isSearching = isSearching,
                        modifier = Modifier.weight(1f),
                    )
                }

                isSearching -> {
                    SearchResultsGrid(
                        apps = uiState.apps,
                        columns = uiState.gridColumns,
                        iconSize = iconSize,
                        showNotificationDots = uiState.showNotificationDots,
                        badgePackages = uiState.badgePackages,
                        scrollEnabled = isActive && !isDraggingToHome,
                        onAppClick = handleAppClick,
                        onAppDragStart = handleAppDragStart,
                        onAppDragMove = onAppDragMove,
                        onAppDragEnd = handleAppDragEnd,
                        onAppDragCancel = handleAppDragCancel,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                    )
                }

                else -> {
                    PaginatedAppGrid(
                        apps = uiState.apps,
                        columns = uiState.gridColumns,
                        iconScale = uiState.iconScale,
                        iconSize = iconSize,
                        showNotificationDots = uiState.showNotificationDots,
                        badgePackages = uiState.badgePackages,
                        scrollEnabled = isActive && !isDraggingToHome,
                        onAppClick = handleAppClick,
                        onAppDragStart = handleAppDragStart,
                        onAppDragMove = onAppDragMove,
                        onAppDragEnd = handleAppDragEnd,
                        onAppDragCancel = handleAppDragCancel,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                    )
                }
            }
        }

        val selectedApp = actionTargetApp
        if (selectedApp != null && isActive) {
            OverviewAppActionOverlay(
                app = selectedApp,
                onAddToHome = {
                    actionTargetApp = null
                    onAddAppToHome(selectedApp)
                },
                onOpenAppInfo = {
                    actionTargetApp = null
                    onOpenAppInfo(selectedApp)
                },
                onDismiss = { actionTargetApp = null },
            )
        }
    }
}

@Composable
private fun SearchResultsGrid(
    apps: List<AppInfo>,
    columns: Int,
    iconSize: Dp,
    showNotificationDots: Boolean,
    badgePackages: Set<String>,
    scrollEnabled: Boolean,
    onAppClick: (app: AppInfo, sourceView: View, sourceBounds: Rect?) -> Unit,
    onAppDragStart: (AppInfo, Offset) -> Unit,
    onAppDragMove: (Offset) -> Unit,
    onAppDragEnd: () -> Unit,
    onAppDragCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val imeBottom = WindowInsets.ime.asPaddingValues().calculateBottomPadding()
    val safeColumns = columns.coerceIn(3, 6)

    LazyVerticalGrid(
        columns = GridCells.Fixed(safeColumns),
        userScrollEnabled = scrollEnabled,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = imeBottom + 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        items(
            items = apps,
            key = { it.key },
        ) { app ->
            AppIconItem(
                app = app,
                onClick = onAppClick,
                onDragStart = onAppDragStart,
                onDragMove = onAppDragMove,
                onDragEnd = onAppDragEnd,
                onDragCancel = onAppDragCancel,
                iconSize = iconSize,
                showLabel = true,
                showNotificationDot = showNotificationDots && app.packageName in badgePackages,
            )
        }
    }
}

@Composable
private fun PaginatedAppGrid(
    apps: List<AppInfo>,
    columns: Int,
    iconScale: Float,
    iconSize: Dp,
    showNotificationDots: Boolean,
    badgePackages: Set<String>,
    scrollEnabled: Boolean,
    onAppClick: (app: AppInfo, sourceView: View, sourceBounds: Rect?) -> Unit,
    onAppDragStart: (AppInfo, Offset) -> Unit,
    onAppDragMove: (Offset) -> Unit,
    onAppDragEnd: () -> Unit,
    onAppDragCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val safeColumns = columns.coerceIn(3, 6)

    BoxWithConstraints(modifier = modifier) {
        // Reserva espaço inferior para os indicadores de página.
        val gridAvailableHeight = (maxHeight - 28.dp).coerceAtLeast(120.dp)
        val rowUnit = (104f * iconScale.coerceAtLeast(0.85f)).dp
        val rows = (gridAvailableHeight / rowUnit).toInt().coerceIn(3, 6)
        val pageSize = safeColumns * rows
        val pageCount = remember(apps.size, pageSize) {
            ceil(apps.size / pageSize.toFloat()).toInt().coerceAtLeast(1)
        }

        val pagerState = rememberPagerState(
            initialPage = 0,
            pageCount = { pageCount },
        )

        LaunchedEffect(pageCount) {
            if (pagerState.currentPage >= pageCount) {
                pagerState.scrollToPage((pageCount - 1).coerceAtLeast(0))
            }
        }

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            HorizontalPager(
                state = pagerState,
                userScrollEnabled = scrollEnabled,
                beyondViewportPageCount = 1,
                key = { page -> page },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) { pageIndex ->
                val fromIndex = pageIndex * pageSize
                val toIndex = (fromIndex + pageSize).coerceAtMost(apps.size)
                val pageApps = remember(apps, fromIndex, toIndex) {
                    if (fromIndex < toIndex) apps.subList(fromIndex, toIndex) else emptyList()
                }

                AppGridPage(
                    apps = pageApps,
                    columns = safeColumns,
                    rows = rows,
                    iconSize = iconSize,
                    showNotificationDots = showNotificationDots,
                    badgePackages = badgePackages,
                    onAppClick = onAppClick,
                    onAppDragStart = onAppDragStart,
                    onAppDragMove = onAppDragMove,
                    onAppDragEnd = onAppDragEnd,
                    onAppDragCancel = onAppDragCancel,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(),
                )
            }

            if (pageCount > 1) {
                PageIndicator(
                    pageCount = pageCount,
                    currentPageProvider = { pagerState.currentPage },
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun AppGridPage(
    apps: List<AppInfo>,
    columns: Int,
    rows: Int,
    iconSize: Dp,
    showNotificationDots: Boolean,
    badgePackages: Set<String>,
    onAppClick: (app: AppInfo, sourceView: View, sourceBounds: Rect?) -> Unit,
    onAppDragStart: (AppInfo, Offset) -> Unit,
    onAppDragMove: (Offset) -> Unit,
    onAppDragEnd: () -> Unit,
    onAppDragCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        for (row in 0 until rows) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Top,
            ) {
                for (column in 0 until columns) {
                    val index = row * columns + column
                    val app = apps.getOrNull(index)
                    if (app != null) {
                        key(app.key) {
                            AppIconItem(
                                app = app,
                                onClick = onAppClick,
                                onDragStart = onAppDragStart,
                                onDragMove = onAppDragMove,
                                onDragEnd = onAppDragEnd,
                                onDragCancel = onAppDragCancel,
                                iconSize = iconSize,
                                showLabel = true,
                                showNotificationDot = showNotificationDots && app.packageName in badgePackages,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun OverviewAppActionOverlay(
    app: AppInfo,
    onAddToHome: () -> Unit,
    onOpenAppInfo: () -> Unit,
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
                .border(1.dp, colors.hairlineBorder, cardShape)
                .clickable(
                    interactionSource = cardInteraction,
                    indication = null,
                    onClick = {},
                ),
        ) {
            Text(
                text = app.label,
                style = MaterialTheme.typography.titleMedium,
                color = colors.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            )
            HorizontalDivider(thickness = 1.dp, color = colors.hairlineBorder)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onAddToHome)
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = null,
                    tint = colors.accent,
                    modifier = Modifier.size(22.dp),
                )
                Spacer(modifier = Modifier.width(14.dp))
                Text(
                    text = stringResource(R.string.overview_action_add_home),
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.onSurface,
                )
            }
            HorizontalDivider(thickness = 1.dp, color = colors.hairlineBorder)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpenAppInfo)
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Filled.Info,
                    contentDescription = null,
                    tint = colors.onSurfaceMuted,
                    modifier = Modifier.size(22.dp),
                )
                Spacer(modifier = Modifier.width(14.dp))
                Text(
                    text = stringResource(R.string.overview_action_app_info),
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.onSurface,
                )
            }
        }
    }
}

@Composable
private fun PageIndicator(
    pageCount: Int,
    currentPageProvider: () -> Int,
    modifier: Modifier = Modifier,
) {
    val colors = OverviewTheme.colors
    val reducedMotion = OverviewTheme.reducedMotion
    val currentPage = currentPageProvider()

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        for (page in 0 until pageCount) {
            val isSelected = page == currentPage
            val width by animateDpAsState(
                targetValue = if (isSelected) 20.dp else 7.dp,
                animationSpec = overviewSpring(reducedMotion),
                label = "pageDotWidth",
            )
            Box(
                modifier = Modifier
                    .size(width = width, height = 7.dp)
                    .clip(RoundedCornerShape(OverviewCornerRadius.Pill))
                    .background(
                        if (isSelected) colors.accent else colors.onSurfaceMuted.copy(alpha = 0.45f),
                    ),
            )
        }
    }
}

@Composable
private fun EmptyOverviewState(
    isSearching: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = OverviewTheme.colors
    val message = if (isSearching) {
        stringResource(R.string.no_apps_found)
    } else {
        stringResource(R.string.loading_apps)
    }

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = colors.onSurfaceMuted,
            textAlign = TextAlign.Center,
        )
    }
}
