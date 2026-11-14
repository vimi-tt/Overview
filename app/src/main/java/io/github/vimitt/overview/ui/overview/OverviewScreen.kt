package io.github.vimitt.overview.ui.overview

import android.graphics.Rect
import android.view.View
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.vimitt.overview.R
import io.github.vimitt.overview.domain.model.AppInfo
import io.github.vimitt.overview.ui.components.AppIconItem
import io.github.vimitt.overview.ui.components.OverviewSearchBar
import io.github.vimitt.overview.ui.theme.OverviewCornerRadius
import io.github.vimitt.overview.ui.theme.OverviewTheme
import io.github.vimitt.overview.ui.theme.overviewSpring
import kotlin.math.ceil

/**
 * Tela do Overview inspirada no GNOME Shell: barra de busca no topo e grade
 * paginada horizontalmente com os aplicativos ordenados alfabeticamente.
 */
@Composable
fun OverviewScreen(
    uiState: OverviewUiState,
    onSearchQueryChange: (String) -> Unit,
    onClearSearch: () -> Unit,
    onAppClick: (app: AppInfo, sourceView: View, sourceBounds: Rect?) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = OverviewTheme.colors
    val keyboardController = LocalSoftwareKeyboardController.current
    val rootView = LocalView.current
    val swipeDownThresholdPx = with(LocalDensity.current) { 72.dp.toPx() }

    BackHandler {
        keyboardController?.hide()
        onClose()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.overviewScrim)
            .pointerInput(swipeDownThresholdPx) {
                var totalDragY = 0f
                var triggered = false
                detectVerticalDragGestures(
                    onDragStart = {
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
            .imePadding()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        OverviewSearchBar(
            query = uiState.searchQuery,
            onQueryChange = onSearchQueryChange,
            onClear = onClearSearch,
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

        if (uiState.apps.isEmpty()) {
            EmptyOverviewState(
                isSearching = uiState.isSearching,
                modifier = Modifier.weight(1f),
            )
        } else {
            PaginatedAppGrid(
                apps = uiState.apps,
                searchQuery = uiState.searchQuery,
                onAppClick = { app, view, bounds ->
                    keyboardController?.hide()
                    onAppClick(app, view, bounds)
                },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun PaginatedAppGrid(
    apps: List<AppInfo>,
    searchQuery: String,
    onAppClick: (app: AppInfo, sourceView: View, sourceBounds: Rect?) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier) {
        val columns = (maxWidth / 88.dp).toInt().coerceIn(4, 6)
        // Reserva espaço inferior para os indicadores de página.
        val gridAvailableHeight = (maxHeight - 28.dp).coerceAtLeast(120.dp)
        val rows = (gridAvailableHeight / 106.dp).toInt().coerceIn(3, 6)
        val pageSize = columns * rows
        val pageCount = remember(apps.size, pageSize) {
            ceil(apps.size / pageSize.toFloat()).toInt().coerceAtLeast(1)
        }

        val pagerState = rememberPagerState(
            initialPage = 0,
            pageCount = { pageCount },
        )

        // Ao digitar uma nova busca, retorna suavemente à primeira página.
        LaunchedEffect(searchQuery, pageCount) {
            if (pagerState.currentPage >= pageCount || searchQuery.isNotEmpty()) {
                pagerState.scrollToPage(0)
            }
        }

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            HorizontalPager(
                state = pagerState,
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
                    columns = columns,
                    rows = rows,
                    onAppClick = onAppClick,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            if (pageCount > 1) {
                PageIndicator(
                    pageCount = pageCount,
                    currentPage = pagerState.currentPage,
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
    onAppClick: (app: AppInfo, sourceView: View, sourceBounds: Rect?) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.SpaceEvenly,
    ) {
        for (row in 0 until rows) {
            Row(
                modifier = Modifier.fillMaxWidth(),
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
                                iconSize = 54.dp,
                                showLabel = true,
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
private fun PageIndicator(
    pageCount: Int,
    currentPage: Int,
    modifier: Modifier = Modifier,
) {
    val colors = OverviewTheme.colors
    val reducedMotion = OverviewTheme.reducedMotion

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
