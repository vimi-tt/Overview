package io.github.vimitt.overview.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.vimitt.overview.R
import io.github.vimitt.overview.data.WidgetAppGroup
import io.github.vimitt.overview.data.WidgetEntry
import io.github.vimitt.overview.domain.model.AppInfo
import io.github.vimitt.overview.domain.model.HomeGridSpec
import io.github.vimitt.overview.ui.components.AppIconItem
import io.github.vimitt.overview.ui.components.ExpressiveLoadingIndicator
import io.github.vimitt.overview.ui.components.OverviewSearchBar
import io.github.vimitt.overview.ui.theme.OverviewTheme

/**
 * Seletor de aplicativos no estilo Adwaita para adicionar atalhos à tela inicial.
 */
@Composable
fun HomeAppPickerScreen(
    apps: List<AppInfo>,
    onSelectApp: (AppInfo) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = OverviewTheme.colors
    var query by remember { mutableStateOf("") }
    val filteredApps = remember(apps, query) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) {
            apps
        } else {
            apps.filter {
                it.label.contains(trimmed, ignoreCase = true) ||
                    it.packageName.contains(trimmed, ignoreCase = true)
            }
        }
    }

    BackHandler(onBack = onClose)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.windowBackground)
            .statusBarsPadding()
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        PickerHeader(
            title = stringResource(R.string.picker_apps_title),
            onClose = onClose,
        )

        OverviewSearchBar(
            query = query,
            onQueryChange = { query = it },
            onClear = { query = "" },
            onSubmit = {},
            modifier = Modifier
                .widthIn(max = 560.dp)
                .padding(horizontal = 16.dp, vertical = 4.dp),
        )

        Spacer(modifier = Modifier.height(12.dp))

        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .widthIn(max = 600.dp)
                .fillMaxWidth(),
        ) {
            val columns = (maxWidth / 88.dp).toInt().coerceIn(4, 6)
            LazyVerticalGrid(
                columns = GridCells.Fixed(columns),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items(
                    items = filteredApps,
                    key = { it.key },
                ) { app ->
                    AppIconItem(
                        app = app,
                        onClick = { selected, _, _ -> onSelectApp(selected) },
                        iconSize = 52.dp,
                        showLabel = true,
                    )
                }
            }
        }
    }
}

/**
 * Seletor de widgets instalados agrupados por aplicativo, com pré-visualização e tamanho.
 */
@Composable
fun HomeWidgetPickerScreen(
    loadWidgets: suspend () -> List<WidgetAppGroup>,
    onSelectWidget: (WidgetEntry) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = OverviewTheme.colors
    var query by remember { mutableStateOf("") }
    var expandedPackage by remember { mutableStateOf<String?>(null) }

    val groups by produceState<List<WidgetAppGroup>?>(initialValue = null) {
        value = loadWidgets()
    }

    val filteredGroups = remember(groups, query) {
        val loaded = groups ?: return@remember emptyList()
        val trimmed = query.trim()
        if (trimmed.isEmpty()) {
            loaded
        } else {
            loaded.mapNotNull { group ->
                if (group.appLabel.contains(trimmed, ignoreCase = true)) {
                    group
                } else {
                    val matchingWidgets = group.widgets.filter {
                        it.label.contains(trimmed, ignoreCase = true)
                    }
                    if (matchingWidgets.isEmpty()) null else group.copy(widgets = matchingWidgets)
                }
            }
        }
    }

    BackHandler(onBack = onClose)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.windowBackground)
            .statusBarsPadding()
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        PickerHeader(
            title = stringResource(R.string.picker_widgets_title),
            onClose = onClose,
        )

        OverviewSearchBar(
            query = query,
            onQueryChange = { query = it },
            onClear = { query = "" },
            onSubmit = {},
            modifier = Modifier
                .widthIn(max = 560.dp)
                .padding(horizontal = 16.dp, vertical = 4.dp),
        )

        Spacer(modifier = Modifier.height(12.dp))

        when {
            groups == null -> {
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        if (OverviewTheme.components.useExpressiveLoading) {
                            ExpressiveLoadingIndicator(size = 46.dp)
                        }
                        Text(
                            text = stringResource(R.string.picker_widgets_loading),
                            style = MaterialTheme.typography.bodyLarge,
                            color = colors.onSurfaceMuted,
                        )
                    }
                }
            }

            filteredGroups.isEmpty() -> {
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.picker_widgets_empty),
                        style = MaterialTheme.typography.bodyLarge,
                        color = colors.onSurfaceMuted,
                    )
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .widthIn(max = 600.dp)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(
                        items = filteredGroups,
                        key = { it.packageName },
                    ) { group ->
                        val isExpanded = query.isNotBlank() || expandedPackage == group.packageName
                        WidgetAppGroupCard(
                            group = group,
                            isExpanded = isExpanded,
                            onToggleExpand = {
                                expandedPackage = if (expandedPackage == group.packageName) {
                                    null
                                } else {
                                    group.packageName
                                }
                            },
                            onSelectWidget = onSelectWidget,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WidgetAppGroupCard(
    group: WidgetAppGroup,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onSelectWidget: (WidgetEntry) -> Unit,
) {
    val colors = OverviewTheme.colors
    val shape = OverviewTheme.shapes.medium

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.surface)
            .border(1.dp, colors.hairlineBorder, shape),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggleExpand)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (group.appIcon != null) {
                Image(
                    bitmap = group.appIcon,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                )
            } else {
                Spacer(modifier = Modifier.size(32.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = group.appLabel,
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${group.widgets.size} widget(s)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceMuted,
                )
            }
            Icon(
                imageVector = if (isExpanded) {
                    Icons.Filled.KeyboardArrowUp
                } else {
                    Icons.Filled.KeyboardArrowDown
                },
                contentDescription = null,
                tint = colors.onSurfaceMuted,
            )
        }

        if (isExpanded) {
            for (widget in group.widgets) {
                HorizontalDivider(thickness = 1.dp, color = colors.hairlineBorder)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectWidget(widget) }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = widget.label,
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                            color = colors.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${widget.spanX}×${widget.spanY}",
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.accent,
                        )
                    }
                    if (widget.preview != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Image(
                            bitmap = widget.preview,
                            contentDescription = widget.label,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 56.dp, max = 140.dp),
                        )
                    }
                }
            }
        }
    }
}

/**
 * Menu contextual ao segurar um atalho ou widget já posicionado na tela inicial.
 */
@Composable
fun HomeItemActionOverlay(
    workspaceItem: HomeWorkspaceItem,
    onMove: () -> Unit,
    onResizeWidget: (deltaX: Int, deltaY: Int) -> Unit,
    onRemove: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = OverviewTheme.colors
    val cardShape = OverviewTheme.shapes.large
    val backdropInteraction = remember { MutableInteractionSource() }
    val cardInteraction = remember { MutableInteractionSource() }

    val title = when (workspaceItem) {
        is HomeWorkspaceItem.Shortcut -> workspaceItem.app.label
        is HomeWorkspaceItem.Widget -> workspaceItem.label
    }

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
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = colors.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            )

            if (workspaceItem is HomeWorkspaceItem.Widget) {
                HorizontalDivider(thickness = 1.dp, color = colors.hairlineBorder)
                WidgetResizeRow(
                    label = stringResource(R.string.home_widget_width),
                    value = workspaceItem.item.spanX,
                    max = HomeGridSpec.COLUMNS,
                    onDecrement = { onResizeWidget(-1, 0) },
                    onIncrement = { onResizeWidget(1, 0) },
                )
                WidgetResizeRow(
                    label = stringResource(R.string.home_widget_height),
                    value = workspaceItem.item.spanY,
                    max = HomeGridSpec.ROWS,
                    onDecrement = { onResizeWidget(0, -1) },
                    onIncrement = { onResizeWidget(0, 1) },
                )
            }

            HorizontalDivider(thickness = 1.dp, color = colors.hairlineBorder)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onMove)
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.home_action_move),
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.onSurface,
                )
            }

            HorizontalDivider(thickness = 1.dp, color = colors.hairlineBorder)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onRemove)
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = null,
                    tint = Color(0xFFE62D42),
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = stringResource(R.string.home_action_remove),
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFFE62D42),
                )
            }
        }
    }
}

@Composable
private fun WidgetResizeRow(
    label: String,
    value: Int,
    max: Int,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
) {
    val colors = OverviewTheme.colors

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = colors.onSurface,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onDecrement, enabled = value > 1) {
            Text(
                text = "−",
                style = MaterialTheme.typography.titleLarge,
                color = if (value > 1) colors.accent else colors.onSurfaceMuted,
            )
        }
        Text(
            text = "$value",
            style = MaterialTheme.typography.titleMedium,
            color = colors.onSurface,
            modifier = Modifier.padding(horizontal = 6.dp),
        )
        IconButton(onClick = onIncrement, enabled = value < max) {
            Text(
                text = "+",
                style = MaterialTheme.typography.titleLarge,
                color = if (value < max) colors.accent else colors.onSurfaceMuted,
            )
        }
    }
}

@Composable
private fun PickerHeader(
    title: String,
    onClose: () -> Unit,
) {
    val colors = OverviewTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 600.dp)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onClose) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.settings_back_content_description),
                tint = colors.onSurface,
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = colors.onSurface,
        )
    }
}
