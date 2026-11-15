package io.github.vimitt.overview.ui.overview

import android.content.Intent
import android.graphics.Rect
import android.net.Uri
import android.view.View
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.vimitt.overview.R
import io.github.vimitt.overview.domain.model.AppFolder
import io.github.vimitt.overview.domain.model.AppInfo
import io.github.vimitt.overview.domain.model.FolderPreviewStyle
import io.github.vimitt.overview.domain.model.FolderSettings
import io.github.vimitt.overview.domain.model.GestureSensitivity
import io.github.vimitt.overview.domain.model.SearchSettings
import io.github.vimitt.overview.domain.model.SettingsDefaults
import io.github.vimitt.overview.domain.model.WebSearchEngine
import io.github.vimitt.overview.ui.components.AppIconItem
import io.github.vimitt.overview.ui.components.OverviewSearchBar
import io.github.vimitt.overview.ui.theme.OverviewTheme
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
    scrimAlpha: Float = SettingsDefaults.DEFAULT_OVERVIEW_SCRIM_ALPHA,
    animationDurationScale: Float = 1f,
    closeSensitivity: GestureSensitivity = GestureSensitivity.MEDIUM,
    onMoveAppToFolder: (appKey: String, folderId: String) -> Unit = { _, _ -> },
    onCreateFolderWithApp: (name: String, appKey: String) -> Unit = { _, _ -> },
    onRemoveAppFromFolder: (appKey: String, folderId: String) -> Unit = { _, _ -> },
    onCloseDragProgress: (Float) -> Unit = {},
    onCloseDragEnd: (shouldClose: Boolean) -> Unit = {},
) {
    val colors = OverviewTheme.colors
    val motion = OverviewTheme.motion
    val reducedMotion = OverviewTheme.reducedMotion
    val keyboardController = LocalSoftwareKeyboardController.current
    val context = LocalContext.current
    val rootView = LocalView.current
    val density = LocalDensity.current
    val dragRangePx = with(density) { 180.dp.toPx() }
    val dragTriggerPx = with(density) { closeSensitivity.triggerDp.dp.toPx() }
    val minFlingDistancePx = with(density) { 14.dp.toPx() }
    val velocityThreshold = closeSensitivity.velocityThresholdPx
    val velocityTracker = remember { VelocityTracker() }
    val isSearching = searchQuery.isNotBlank()
    val searchFocusRequester = remember { FocusRequester() }
    var handledFocusTrigger by remember { mutableIntStateOf(0) }
    var actionTargetApp by remember { mutableStateOf<AppInfo?>(null) }
    var folderPickerTargetApp by remember { mutableStateOf<AppInfo?>(null) }
    var openFolderId by remember { mutableStateOf<String?>(null) }
    var dragCandidateApp by remember { mutableStateOf<AppInfo?>(null) }

    val openFolderEntry = remember(openFolderId, uiState.gridEntries) {
        openFolderId?.let { id ->
            uiState.gridEntries
                .filterIsInstance<OverviewGridEntry.Folder>()
                .firstOrNull { it.folder.id == id }
        }
    }

    LaunchedEffect(isActive, openFolderId, openFolderEntry) {
        if (!isActive || (openFolderId != null && openFolderEntry == null)) {
            openFolderId = null
        }
    }

    LaunchedEffect(isActive, focusSearchTrigger) {
        if (isActive && focusSearchTrigger > handledFocusTrigger) {
            handledFocusTrigger = focusSearchTrigger
            searchFocusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    val openWebSearch: (String) -> Unit = remember(context, uiState.searchSettings.webSearchEngine) {
        { query ->
            val trimmed = query.trim()
            if (trimmed.isNotEmpty()) {
                val url = uiState.searchSettings.webSearchEngine.buildSearchUrl(trimmed)
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                runCatching { context.startActivity(intent) }
            }
        }
    }

    val contentAlpha by animateFloatAsState(
        targetValue = if (isDraggingToHome) 0f else 1f,
        animationSpec = motion.effectSpec(
            durationMillis = 140,
            durationScale = animationDurationScale,
        ),
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

    BackHandler(
        enabled = isActive &&
            actionTargetApp == null &&
            folderPickerTargetApp == null &&
            openFolderEntry == null,
    ) {
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
                .background(colors.overviewScrim.copy(alpha = scrimAlpha.coerceIn(0f, 1f)))
                .pointerInput(
                    isActive,
                    isDraggingToHome,
                    dragRangePx,
                    dragTriggerPx,
                    minFlingDistancePx,
                    velocityThreshold,
                ) {
                    if (!isActive || isDraggingToHome) return@pointerInput
                    var totalDragY = 0f
                    detectVerticalDragGestures(
                        onDragStart = {
                            totalDragY = 0f
                            velocityTracker.resetTracking()
                        },
                        onDragEnd = {
                            val velocityY = velocityTracker.calculateVelocity().y
                            val shouldClose = totalDragY >= dragTriggerPx ||
                                (velocityY >= velocityThreshold && totalDragY >= minFlingDistancePx)
                            totalDragY = 0f
                            onCloseDragEnd(shouldClose)
                            if (shouldClose) {
                                keyboardController?.hide()
                                onClose()
                            }
                        },
                        onDragCancel = {
                            totalDragY = 0f
                            onCloseDragEnd(false)
                        },
                        onVerticalDrag = { change, dragAmount ->
                            velocityTracker.addPosition(change.uptimeMillis, change.position)
                            totalDragY += dragAmount
                            if (totalDragY > 0f) {
                                onCloseDragProgress((totalDragY / dragRangePx).coerceIn(0f, 1f))
                            } else {
                                onCloseDragProgress(0f)
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
                    } else if (
                        uiState.apps.isEmpty() &&
                        isSearching &&
                        uiState.searchSettings.showWebSearchSuggestion
                    ) {
                        keyboardController?.hide()
                        openWebSearch(searchQuery)
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
                isSearching && uiState.apps.isEmpty() -> {
                    EmptyOverviewState(
                        isSearching = true,
                        searchQuery = searchQuery,
                        searchSettings = uiState.searchSettings,
                        onWebSearch = openWebSearch,
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
                        searchQuery = searchQuery,
                        searchSettings = uiState.searchSettings,
                        onWebSearch = openWebSearch,
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

                uiState.gridEntries.isEmpty() -> {
                    EmptyOverviewState(
                        isSearching = false,
                        searchQuery = "",
                        searchSettings = uiState.searchSettings,
                        onWebSearch = openWebSearch,
                        modifier = Modifier.weight(1f),
                    )
                }

                else -> {
                    PaginatedAppGrid(
                        entries = uiState.gridEntries,
                        columns = uiState.gridColumns,
                        iconScale = uiState.iconScale,
                        iconSize = iconSize,
                        showNotificationDots = uiState.showNotificationDots,
                        badgePackages = uiState.badgePackages,
                        folderPreviewStyle = uiState.folderSettings.previewStyle,
                        animationDurationScale = animationDurationScale,
                        scrollEnabled = isActive && !isDraggingToHome && openFolderEntry == null,
                        onAppClick = handleAppClick,
                        onFolderClick = { folderId ->
                            keyboardController?.hide()
                            openFolderId = folderId
                        },
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

        val currentFolder = openFolderEntry
        if (currentFolder != null && isActive) {
            FolderPopupOverlay(
                entry = currentFolder,
                folderSettings = uiState.folderSettings,
                iconSize = remember(uiState.iconScale) { (54f * uiState.iconScale).dp },
                showNotificationDots = uiState.showNotificationDots,
                badgePackages = uiState.badgePackages,
                onAppClick = { app, view, bounds ->
                    if (uiState.folderSettings.closeOnAppLaunch) {
                        openFolderId = null
                    }
                    handleAppClick(app, view, bounds)
                },
                onAppLongClick = { app ->
                    actionTargetApp = app
                },
                onDismiss = { openFolderId = null },
            )
        }

        val selectedApp = actionTargetApp
        if (selectedApp != null && isActive) {
            val containingFolder = remember(selectedApp.key, uiState.folderSettings.folders) {
                uiState.folderSettings.folders.firstOrNull { selectedApp.key in it.appKeys }
            }
            OverviewAppActionOverlay(
                app = selectedApp,
                currentFolder = containingFolder,
                onAddToHome = {
                    actionTargetApp = null
                    openFolderId = null
                    onAddAppToHome(selectedApp)
                },
                onMoveToFolder = {
                    actionTargetApp = null
                    folderPickerTargetApp = selectedApp
                },
                onRemoveFromFolder = if (containingFolder != null) {
                    {
                        actionTargetApp = null
                        onRemoveAppFromFolder(selectedApp.key, containingFolder.id)
                    }
                } else {
                    null
                },
                onOpenAppInfo = {
                    actionTargetApp = null
                    onOpenAppInfo(selectedApp)
                },
                onDismiss = { actionTargetApp = null },
            )
        }

        val pickerApp = folderPickerTargetApp
        if (pickerApp != null && isActive) {
            FolderPickerOverlay(
                app = pickerApp,
                folders = uiState.folderSettings.folders,
                onSelectFolder = { folder ->
                    folderPickerTargetApp = null
                    onMoveAppToFolder(pickerApp.key, folder.id)
                },
                onCreateFolder = { folderName ->
                    folderPickerTargetApp = null
                    onCreateFolderWithApp(folderName, pickerApp.key)
                },
                onDismiss = { folderPickerTargetApp = null },
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
    searchQuery: String,
    searchSettings: SearchSettings,
    onWebSearch: (String) -> Unit,
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
    val showWebPill = searchSettings.showWebSearchSuggestion && searchQuery.isNotBlank()

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

        if (showWebPill) {
            item(
                key = "web_search_suggestion",
                span = { GridItemSpan(safeColumns) },
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    WebSearchSuggestionPill(
                        query = searchQuery.trim(),
                        engine = searchSettings.webSearchEngine,
                        onClick = { onWebSearch(searchQuery) },
                    )
                }
            }
        }
    }
}

@Composable
private fun PaginatedAppGrid(
    entries: List<OverviewGridEntry>,
    columns: Int,
    iconScale: Float,
    iconSize: Dp,
    showNotificationDots: Boolean,
    badgePackages: Set<String>,
    folderPreviewStyle: FolderPreviewStyle,
    animationDurationScale: Float,
    scrollEnabled: Boolean,
    onAppClick: (app: AppInfo, sourceView: View, sourceBounds: Rect?) -> Unit,
    onFolderClick: (folderId: String) -> Unit,
    onAppDragStart: (AppInfo, Offset) -> Unit,
    onAppDragMove: (Offset) -> Unit,
    onAppDragEnd: () -> Unit,
    onAppDragCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val motion = OverviewTheme.motion
    val safeColumns = columns.coerceIn(3, 6)

    BoxWithConstraints(modifier = modifier) {
        // Reserva espaço inferior para os indicadores de página.
        val gridAvailableHeight = (maxHeight - 28.dp).coerceAtLeast(120.dp)
        val rowUnit = (104f * iconScale.coerceAtLeast(0.85f)).dp
        val rows = (gridAvailableHeight / rowUnit).toInt().coerceIn(3, 6)
        val pageSize = safeColumns * rows
        val pageCount = remember(entries.size, pageSize) {
            ceil(entries.size / pageSize.toFloat()).toInt().coerceAtLeast(1)
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

        val flingBehavior = PagerDefaults.flingBehavior(
            state = pagerState,
            snapAnimationSpec = motion.spatialSpec(
                durationMillis = 240,
                durationScale = animationDurationScale,
            ),
        )

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            HorizontalPager(
                state = pagerState,
                userScrollEnabled = scrollEnabled,
                flingBehavior = flingBehavior,
                beyondViewportPageCount = 1,
                key = { page -> page },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) { pageIndex ->
                val fromIndex = pageIndex * pageSize
                val toIndex = (fromIndex + pageSize).coerceAtMost(entries.size)
                val pageEntries = remember(entries, fromIndex, toIndex) {
                    if (fromIndex < toIndex) entries.subList(fromIndex, toIndex) else emptyList()
                }

                AppGridPage(
                    entries = pageEntries,
                    columns = safeColumns,
                    rows = rows,
                    iconSize = iconSize,
                    showNotificationDots = showNotificationDots,
                    badgePackages = badgePackages,
                    folderPreviewStyle = folderPreviewStyle,
                    onAppClick = onAppClick,
                    onFolderClick = onFolderClick,
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
    entries: List<OverviewGridEntry>,
    columns: Int,
    rows: Int,
    iconSize: Dp,
    showNotificationDots: Boolean,
    badgePackages: Set<String>,
    folderPreviewStyle: FolderPreviewStyle,
    onAppClick: (app: AppInfo, sourceView: View, sourceBounds: Rect?) -> Unit,
    onFolderClick: (folderId: String) -> Unit,
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
                    val entry = entries.getOrNull(index)
                    when (entry) {
                        is OverviewGridEntry.App -> {
                            key(entry.key) {
                                AppIconItem(
                                    app = entry.app,
                                    onClick = onAppClick,
                                    onDragStart = onAppDragStart,
                                    onDragMove = onAppDragMove,
                                    onDragEnd = onAppDragEnd,
                                    onDragCancel = onAppDragCancel,
                                    iconSize = iconSize,
                                    showLabel = true,
                                    showNotificationDot = showNotificationDots &&
                                        entry.app.packageName in badgePackages,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }

                        is OverviewGridEntry.Folder -> {
                            key(entry.key) {
                                FolderIconItem(
                                    entry = entry,
                                    previewStyle = folderPreviewStyle,
                                    iconSize = iconSize,
                                    onClick = { onFolderClick(entry.folder.id) },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }

                        null -> {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FolderIconItem(
    entry: OverviewGridEntry.Folder,
    previewStyle: FolderPreviewStyle,
    iconSize: Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = OverviewTheme.colors
    val shapes = OverviewTheme.shapes
    val tileShape = shapes.large
    val previewApps = remember(entry.apps) { entry.apps.take(4) }

    Column(
        modifier = modifier
            .clip(shapes.medium)
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top,
    ) {
        Box(
            modifier = Modifier.size(iconSize),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(tileShape)
                    .background(colors.surfaceElevated.copy(alpha = 0.85f))
                    .border(1.dp, colors.hairlineBorder, tileShape)
                    .padding(6.dp),
                contentAlignment = Alignment.Center,
            ) {
                when (previewStyle) {
                    FolderPreviewStyle.GRID_2X2 -> {
                        val miniSize = (iconSize - 16.dp) / 2f
                        Column(
                            verticalArrangement = Arrangement.spacedBy(3.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            for (r in 0..1) {
                                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                    for (c in 0..1) {
                                        val miniApp = previewApps.getOrNull(r * 2 + c)
                                        if (miniApp?.icon != null) {
                                            Image(
                                                bitmap = miniApp.icon,
                                                contentDescription = null,
                                                modifier = Modifier
                                                    .size(miniSize)
                                                    .clip(RoundedCornerShape(4.dp)),
                                                contentScale = ContentScale.Fit,
                                            )
                                        } else {
                                            Spacer(modifier = Modifier.size(miniSize))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    FolderPreviewStyle.STACKED -> {
                        val stackApps = previewApps.take(3).reversed()
                        val miniSize = iconSize * 0.56f
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            stackApps.forEachIndexed { idx, miniApp ->
                                val shift = (idx - (stackApps.size - 1) / 2f) * 5f
                                val bitmap = miniApp.icon
                                if (bitmap != null) {
                                    Image(
                                        bitmap = bitmap,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .offset(x = shift.dp, y = (-shift * 0.6f).dp)
                                            .size(miniSize)
                                            .clip(RoundedCornerShape(6.dp)),
                                        contentScale = ContentScale.Fit,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (entry.hasBadge) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(colors.surface)
                        .padding(1.5.dp)
                        .clip(CircleShape)
                        .background(colors.accent),
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = entry.label,
            style = MaterialTheme.typography.labelMedium,
            color = colors.onSurface,
            minLines = 2,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun FolderPopupOverlay(
    entry: OverviewGridEntry.Folder,
    folderSettings: FolderSettings,
    iconSize: Dp,
    showNotificationDots: Boolean,
    badgePackages: Set<String>,
    onAppClick: (app: AppInfo, sourceView: View, sourceBounds: Rect?) -> Unit,
    onAppLongClick: (AppInfo) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = OverviewTheme.colors
    val shapes = OverviewTheme.shapes
    val motion = OverviewTheme.motion
    val cardShape = shapes.large
    val backdropInteraction = remember { MutableInteractionSource() }
    val cardInteraction = remember { MutableInteractionSource() }
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    val entryProgress by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = motion.spatialSpec(durationMillis = 180),
        label = "folderPopupEntry",
    )
    val columns = folderSettings.folderColumns.coerceIn(
        SettingsDefaults.MIN_FOLDER_COLUMNS,
        SettingsDefaults.MAX_FOLDER_COLUMNS,
    )
    val rows = remember(entry.apps, columns) { entry.apps.chunked(columns) }

    BackHandler(onBack = onDismiss)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.42f * entryProgress.coerceIn(0f, 1f)))
            .clickable(
                interactionSource = backdropInteraction,
                indication = null,
                onClick = onDismiss,
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 28.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 420.dp)
                .fillMaxWidth()
                .graphicsLayer {
                    val scale = 0.90f + 0.10f * entryProgress
                    scaleX = scale
                    scaleY = scale
                    alpha = entryProgress.coerceIn(0f, 1f)
                }
                .clip(cardShape)
                .background(colors.surface)
                .border(1.dp, colors.hairlineBorder, cardShape)
                .clickable(
                    interactionSource = cardInteraction,
                    indication = null,
                    onClick = {},
                ),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = entry.folder.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = if (entry.apps.size == 1) {
                            stringResource(R.string.settings_folders_app_count_one)
                        } else {
                            stringResource(R.string.settings_folders_app_count_many, entry.apps.size)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceMuted,
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(R.string.settings_close_dialog),
                        tint = colors.onSurfaceMuted,
                    )
                }
            }

            HorizontalDivider(thickness = 1.dp, color = colors.hairlineBorder)

            if (entry.apps.isEmpty()) {
                Text(
                    text = stringResource(R.string.overview_folder_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceMuted,
                    modifier = Modifier.padding(24.dp),
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 380.dp)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    rows.forEach { rowApps ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            rowApps.forEach { app ->
                                key(app.key) {
                                    AppIconItem(
                                        app = app,
                                        onClick = onAppClick,
                                        onLongClick = onAppLongClick,
                                        iconSize = iconSize,
                                        showLabel = true,
                                        showNotificationDot = showNotificationDots &&
                                            app.packageName in badgePackages,
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                            }
                            repeat(columns - rowApps.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OverviewAppActionOverlay(
    app: AppInfo,
    currentFolder: AppFolder?,
    onAddToHome: () -> Unit,
    onMoveToFolder: () -> Unit,
    onRemoveFromFolder: (() -> Unit)?,
    onOpenAppInfo: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = OverviewTheme.colors
    val shapes = OverviewTheme.shapes
    val motion = OverviewTheme.motion
    val cardShape = shapes.large
    val backdropInteraction = remember { MutableInteractionSource() }
    val cardInteraction = remember { MutableInteractionSource() }
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    val entryProgress by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = motion.spatialSpec(durationMillis = 160),
        label = "overviewAppActionEntry",
    )

    BackHandler(onBack = onDismiss)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.36f * entryProgress.coerceIn(0f, 1f)))
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
                .graphicsLayer {
                    val scale = 0.94f + 0.06f * entryProgress
                    scaleX = scale
                    scaleY = scale
                    alpha = entryProgress.coerceIn(0f, 1f)
                    translationY = (1f - entryProgress) * 28f
                }
                .clip(cardShape)
                .background(colors.surface)
                .border(1.dp, colors.hairlineBorder, cardShape)
                .clickable(
                    interactionSource = cardInteraction,
                    indication = null,
                    onClick = {},
                ),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            ) {
                Text(
                    text = app.label,
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (currentFolder != null) {
                    Text(
                        text = currentFolder.name,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
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
                    .clickable(onClick = onMoveToFolder)
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_settings_folders),
                    contentDescription = null,
                    tint = colors.onSurface,
                    modifier = Modifier.size(22.dp),
                )
                Spacer(modifier = Modifier.width(14.dp))
                Text(
                    text = stringResource(R.string.overview_action_move_to_folder),
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.onSurface,
                )
            }
            if (onRemoveFromFolder != null) {
                HorizontalDivider(thickness = 1.dp, color = colors.hairlineBorder)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onRemoveFromFolder)
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = null,
                        tint = colors.onSurfaceMuted,
                        modifier = Modifier.size(22.dp),
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Text(
                        text = stringResource(R.string.overview_folder_remove_app),
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.onSurface,
                    )
                }
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
private fun FolderPickerOverlay(
    app: AppInfo,
    folders: List<AppFolder>,
    onSelectFolder: (AppFolder) -> Unit,
    onCreateFolder: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = OverviewTheme.colors
    val shapes = OverviewTheme.shapes
    val motion = OverviewTheme.motion
    val cardShape = shapes.large
    val backdropInteraction = remember { MutableInteractionSource() }
    val cardInteraction = remember { MutableInteractionSource() }
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    val entryProgress by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = motion.spatialSpec(durationMillis = 170),
        label = "folderPickerEntry",
    )
    var isCreating by remember { mutableStateOf(folders.isEmpty()) }
    var newFolderName by remember { mutableStateOf("") }

    BackHandler(onBack = onDismiss)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.42f * entryProgress.coerceIn(0f, 1f)))
            .clickable(
                interactionSource = backdropInteraction,
                indication = null,
                onClick = onDismiss,
            )
            .imePadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 420.dp)
                .fillMaxWidth()
                .graphicsLayer {
                    val scale = 0.92f + 0.08f * entryProgress
                    scaleX = scale
                    scaleY = scale
                    alpha = entryProgress.coerceIn(0f, 1f)
                }
                .clip(cardShape)
                .background(colors.surface)
                .border(1.dp, colors.hairlineBorder, cardShape)
                .clickable(
                    interactionSource = cardInteraction,
                    indication = null,
                    onClick = {},
                ),
        ) {
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                Text(
                    text = stringResource(R.string.overview_folder_picker_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.onSurface,
                )
                Text(
                    text = app.label,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            HorizontalDivider(thickness = 1.dp, color = colors.hairlineBorder)

            if (folders.isNotEmpty() && !isCreating) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 260.dp)
                        .verticalScroll(rememberScrollState()),
                ) {
                    folders.forEach { folder ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectFolder(folder) }
                                .padding(horizontal = 20.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_settings_folders),
                                contentDescription = null,
                                tint = colors.accent,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = folder.name,
                                style = MaterialTheme.typography.bodyLarge,
                                color = colors.onSurface,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        HorizontalDivider(thickness = 1.dp, color = colors.hairlineBorder)
                    }
                }
            }

            if (isCreating) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedTextField(
                        value = newFolderName,
                        onValueChange = { newFolderName = it },
                        singleLine = true,
                        label = { Text(stringResource(R.string.settings_folder_name_label)) },
                        placeholder = { Text(stringResource(R.string.settings_folder_name_placeholder)) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text(stringResource(R.string.settings_cancel))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        TextButton(
                            enabled = newFolderName.isNotBlank(),
                            onClick = { onCreateFolder(newFolderName.trim()) },
                        ) {
                            Text(stringResource(R.string.settings_folder_save))
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isCreating = true }
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
                        text = stringResource(R.string.settings_folders_create),
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.accent,
                    )
                }
            }
        }
    }
}

@Composable
private fun WebSearchSuggestionPill(
    query: String,
    engine: WebSearchEngine,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = OverviewTheme.colors
    val engineName = when (engine) {
        WebSearchEngine.DUCKDUCKGO -> stringResource(R.string.settings_search_engine_duckduckgo)
        WebSearchEngine.GOOGLE -> stringResource(R.string.settings_search_engine_google)
        WebSearchEngine.STARTPAGE -> stringResource(R.string.settings_search_engine_startpage)
        WebSearchEngine.ECOSIA -> stringResource(R.string.settings_search_engine_ecosia)
    }
    val pillShape = OverviewTheme.shapes.pill

    Row(
        modifier = modifier
            .clip(pillShape)
            .background(colors.surfaceElevated)
            .border(1.dp, colors.hairlineBorder, pillShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.Search,
            contentDescription = null,
            tint = colors.accent,
            modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = stringResource(R.string.overview_web_search_pill, query, engineName),
            style = MaterialTheme.typography.labelLarge,
            color = colors.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun PageIndicator(
    pageCount: Int,
    currentPageProvider: () -> Int,
    modifier: Modifier = Modifier,
) {
    val colors = OverviewTheme.colors
    val motion = OverviewTheme.motion
    val shapes = OverviewTheme.shapes
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
                animationSpec = motion.indicatorSpec(visibilityThreshold = Dp.VisibilityThreshold),
                label = "pageDotWidth",
            )
            Box(
                modifier = Modifier
                    .size(width = width, height = 7.dp)
                    .clip(shapes.pill)
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
    searchQuery: String,
    searchSettings: SearchSettings,
    onWebSearch: (String) -> Unit,
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
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = colors.onSurfaceMuted,
                textAlign = TextAlign.Center,
            )
            if (isSearching && searchSettings.showWebSearchSuggestion && searchQuery.isNotBlank()) {
                WebSearchSuggestionPill(
                    query = searchQuery.trim(),
                    engine = searchSettings.webSearchEngine,
                    onClick = { onWebSearch(searchQuery) },
                )
            }
        }
    }
}
