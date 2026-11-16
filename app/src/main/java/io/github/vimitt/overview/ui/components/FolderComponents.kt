package io.github.vimitt.overview.ui.components

import android.graphics.Rect
import android.os.SystemClock
import android.view.View
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.vimitt.overview.R
import io.github.vimitt.overview.domain.model.AppInfo
import io.github.vimitt.overview.domain.model.FolderPreviewStyle
import io.github.vimitt.overview.domain.model.FolderSettings
import io.github.vimitt.overview.domain.model.SettingsDefaults
import io.github.vimitt.overview.ui.theme.OverviewTheme

private class FolderCoordinatesHolder {
    var coordinates: LayoutCoordinates? = null
}

private class FolderDragState {
    var isDragging: Boolean = false
    var lastDragEndUptimeMs: Long = 0L
}

/**
 * Ícone composto de pasta reutilizado tanto na grade do Overview quanto na tela inicial.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FolderIconTile(
    folderId: String,
    label: String,
    apps: List<AppInfo>,
    hasBadge: Boolean,
    previewStyle: FolderPreviewStyle,
    iconSize: Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showLabel: Boolean = true,
    labelFontScale: Float = 1f,
    labelColor: Color? = null,
    labelShadow: Shadow? = null,
    isDropHighlight: Boolean = false,
    onLongClick: (() -> Unit)? = null,
    onDragStart: ((Offset) -> Unit)? = null,
    onDragMove: ((Offset) -> Unit)? = null,
    onDragEnd: (() -> Unit)? = null,
    onDragCancel: (() -> Unit)? = null,
) {
    val colors = OverviewTheme.colors
    val shapes = OverviewTheme.shapes
    val tileShape = shapes.large
    val previewApps = remember(apps) { apps.take(4) }
    val haptic = LocalHapticFeedback.current
    val coordsHolder = remember { FolderCoordinatesHolder() }
    val dragState = remember { FolderDragState() }

    val currentOnClick by rememberUpdatedState(onClick)
    val currentOnLongClick by rememberUpdatedState(onLongClick)
    val currentOnDragStart by rememberUpdatedState(onDragStart)
    val currentOnDragMove by rememberUpdatedState(onDragMove)
    val currentOnDragEnd by rememberUpdatedState(onDragEnd)
    val currentOnDragCancel by rememberUpdatedState(onDragCancel)

    val triggerClick = remember {
        {
            val elapsed = SystemClock.uptimeMillis() - dragState.lastDragEndUptimeMs
            if (!dragState.isDragging && elapsed > 350L) {
                currentOnClick()
            }
        }
    }

    val interactionModifier = if (onDragStart != null) {
        Modifier
            .clickable(onClick = triggerClick)
            .pointerInput(folderId) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { localOffset ->
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        dragState.isDragging = true
                        val coords = coordsHolder.coordinates
                        val windowPos = if (coords != null && coords.isAttached) {
                            coords.localToWindow(localOffset)
                        } else {
                            localOffset
                        }
                        currentOnDragStart?.invoke(windowPos)
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        val coords = coordsHolder.coordinates
                        val windowPos = if (coords != null && coords.isAttached) {
                            coords.localToWindow(change.position)
                        } else {
                            change.position
                        }
                        currentOnDragMove?.invoke(windowPos)
                    },
                    onDragEnd = {
                        dragState.isDragging = false
                        dragState.lastDragEndUptimeMs = SystemClock.uptimeMillis()
                        currentOnDragEnd?.invoke()
                    },
                    onDragCancel = {
                        dragState.isDragging = false
                        dragState.lastDragEndUptimeMs = SystemClock.uptimeMillis()
                        currentOnDragCancel?.invoke()
                    },
                )
            }
    } else if (onLongClick != null) {
        Modifier.combinedClickable(
            onClick = triggerClick,
            onLongClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                currentOnLongClick?.invoke()
            },
        )
    } else {
        Modifier.clickable(onClick = triggerClick)
    }

    val containerBg = if (isDropHighlight) {
        colors.accentContainer.copy(alpha = 0.92f)
    } else {
        colors.surfaceElevated.copy(alpha = 0.85f)
    }
    val containerBorder = if (isDropHighlight) colors.accent else colors.hairlineBorder
    val borderWidth = if (isDropHighlight) 2.dp else 1.dp

    Column(
        modifier = modifier
            .onPlaced { coords -> coordsHolder.coordinates = coords }
            .clip(shapes.medium)
            .then(interactionModifier)
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
                    .background(containerBg)
                    .border(borderWidth, containerBorder, tileShape)
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

            if (hasBadge) {
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

        if (showLabel) {
            Spacer(modifier = Modifier.height(8.dp))
            val baseStyle = MaterialTheme.typography.labelMedium
            val resolvedStyle = if (labelFontScale == 1f && labelShadow == null) {
                baseStyle
            } else {
                remember(baseStyle, labelFontScale, labelShadow) {
                    baseStyle.copy(
                        fontSize = baseStyle.fontSize * labelFontScale,
                        lineHeight = baseStyle.lineHeight * labelFontScale,
                        shadow = labelShadow,
                    )
                }
            }
            Text(
                text = label,
                style = resolvedStyle,
                color = labelColor ?: colors.onSurface,
                minLines = 2,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/**
 * Pop-up de pasta compartilhado entre o Overview e a tela inicial.
 */
@Composable
fun FolderPopupOverlay(
    folderName: String,
    apps: List<AppInfo>,
    folderSettings: FolderSettings,
    iconSize: Dp,
    showNotificationDots: Boolean,
    badgePackages: Set<String>,
    onAppClick: (app: AppInfo, sourceView: View, sourceBounds: Rect?) -> Unit,
    onAppLongClick: (AppInfo) -> Unit,
    onDismiss: () -> Unit,
    onEditFolder: (() -> Unit)? = null,
) {
    val colors = OverviewTheme.colors
    val isExpressive = OverviewTheme.components.isExpressive
    val columns = folderSettings.folderColumns.coerceIn(
        SettingsDefaults.MIN_FOLDER_COLUMNS,
        SettingsDefaults.MAX_FOLDER_COLUMNS,
    )
    val rows = remember(apps, columns) { apps.chunked(columns) }

    OverviewPopupSurface(
        onDismiss = onDismiss,
        bottomSheet = false,
        scrimMaxAlpha = 0.42f,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (isExpressive) {
                        Modifier.background(
                            lerp(colors.surfaceContainerHigh, colors.accentContainer, 0.24f),
                        )
                    } else {
                        Modifier
                    },
                )
                .padding(
                    start = 20.dp,
                    end = 10.dp,
                    top = if (isExpressive) 14.dp else 10.dp,
                    bottom = if (isExpressive) 14.dp else 10.dp,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = folderName,
                    style = if (isExpressive) {
                        OverviewTheme.typography.sectionHeader
                    } else {
                        MaterialTheme.typography.titleMedium
                    },
                    color = colors.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = pluralStringResource(
                        R.plurals.settings_folders_app_count,
                        apps.size,
                        apps.size,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isExpressive) colors.onAccentContainer.copy(alpha = 0.85f) else colors.onSurfaceMuted,
                )
            }
            if (onEditFolder != null) {
                IconButton(onClick = onEditFolder) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = stringResource(R.string.overview_folder_edit_apps),
                        tint = if (isExpressive) colors.accent else colors.onSurfaceMuted,
                        modifier = Modifier.size(19.dp),
                    )
                }
            }
            IconButton(onClick = onDismiss) {
                if (isExpressive) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(colors.accentContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = stringResource(R.string.settings_close_dialog),
                            tint = colors.onAccentContainer,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                } else {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(R.string.settings_close_dialog),
                        tint = colors.onSurfaceMuted,
                    )
                }
            }
        }

        OverviewPopupDivider()

        if (apps.isEmpty()) {
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
                            androidx.compose.runtime.key(app.key) {
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

/**
 * Diálogo compartilhado para criar ou editar o nome e os aplicativos de uma pasta.
 */
@Composable
fun FolderEditorDialog(
    initialName: String,
    initialAppKeys: List<String>,
    isNewFolder: Boolean,
    allApps: List<AppInfo>,
    onSave: (name: String, appKeys: List<String>) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = OverviewTheme.colors
    var folderName by remember(initialName, isNewFolder) {
        mutableStateOf(initialName)
    }
    var appFilter by remember { mutableStateOf("") }
    val selectedKeys = remember(initialName, initialAppKeys, isNewFolder) {
        mutableStateListOf<String>().apply {
            addAll(initialAppKeys)
        }
    }

    val filteredApps = remember(allApps, appFilter) {
        val q = appFilter.trim()
        if (q.isEmpty()) {
            allApps
        } else {
            allApps.filter { it.label.contains(q, ignoreCase = true) }
        }
    }

    val titleRes = if (isNewFolder) {
        R.string.settings_folder_dialog_title_new
    } else {
        R.string.settings_folder_dialog_title_edit
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surfaceContainer,
        titleContentColor = colors.onSurface,
        textContentColor = colors.onSurface,
        title = { Text(stringResource(titleRes)) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = folderName,
                    onValueChange = { folderName = it },
                    label = { Text(stringResource(R.string.settings_folder_name_label)) },
                    placeholder = { Text(stringResource(R.string.settings_folder_name_placeholder)) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.accent,
                        unfocusedBorderColor = colors.hairlineBorder,
                        focusedLabelColor = colors.accent,
                        cursorColor = colors.accent,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = appFilter,
                    onValueChange = { appFilter = it },
                    placeholder = { Text(stringResource(R.string.settings_hidden_apps_search_hint)) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.accent,
                        unfocusedBorderColor = colors.hairlineBorder,
                        cursorColor = colors.accent,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.settings_folder_select_apps, selectedKeys.size),
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.onSurfaceMuted,
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 240.dp),
                ) {
                    items(items = filteredApps, key = { it.key }) { app ->
                        val isSelected = app.key in selectedKeys
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(OverviewTheme.shapes.medium)
                                .clickable(role = Role.Checkbox) {
                                    if (isSelected) {
                                        selectedKeys.remove(app.key)
                                    } else {
                                        selectedKeys.add(app.key)
                                    }
                                }
                                .padding(horizontal = 4.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            val icon = app.icon
                            if (icon != null) {
                                Image(
                                    bitmap = icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(32.dp),
                                    contentScale = ContentScale.Fit,
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(colors.surfaceElevated),
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = app.label,
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = null,
                                colors = CheckboxDefaults.colors(checkedColor = colors.accent),
                            )
                        }
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.settings_cancel), color = colors.onSurfaceMuted)
            }
        },
        confirmButton = {
            val canSave = folderName.isNotBlank() && selectedKeys.isNotEmpty()
            TextButton(
                enabled = canSave,
                onClick = { onSave(folderName, selectedKeys.toList()) },
            ) {
                Text(
                    text = stringResource(R.string.settings_folder_save),
                    color = if (canSave) colors.accent else colors.onSurfaceMuted,
                )
            }
        },
    )
}
