package io.github.vimitt.overview.ui.home

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Rect
import android.text.format.DateFormat
import android.view.View
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect as ComposeRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.vimitt.overview.R
import io.github.vimitt.overview.data.AppWidgetController
import io.github.vimitt.overview.domain.model.AppInfo
import io.github.vimitt.overview.domain.model.HomeGridSpec
import io.github.vimitt.overview.ui.ActiveDragState
import io.github.vimitt.overview.ui.DragPayload
import io.github.vimitt.overview.ui.HomeDropTarget
import io.github.vimitt.overview.ui.components.AppIconItem
import io.github.vimitt.overview.ui.components.DashBar
import io.github.vimitt.overview.ui.theme.OverviewCornerRadius
import io.github.vimitt.overview.ui.theme.OverviewTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
    onLongPress: () -> Unit,
    canPlaceMovingItemAt: (cellX: Int, cellY: Int) -> Boolean,
    onPlaceMovingItemAt: (cellX: Int, cellY: Int) -> Unit,
    onCancelMovingItem: () -> Unit,
    onGridBoundsChanged: (ComposeRect) -> Unit,
    onRemoveZoneBoundsChanged: (ComposeRect?) -> Unit,
    onStartItemDrag: (HomeWorkspaceItem, Offset) -> Unit,
    onItemDragMove: (Offset) -> Unit,
    onItemDragEnd: () -> Unit,
    onItemDragCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val swipeThresholdPx = with(LocalDensity.current) { 36.dp.toPx() }
    val haptic = LocalHapticFeedback.current
    val hasWorkspaceItems = uiState.workspaceItems.isNotEmpty()
    val isMoving = uiState.movingItemId != null
    val isDragInProgress = activeDrag != null
    val isDraggingOverGrid = activeDrag?.hasMovedPastSlop == true
    val isDraggingFromHome = isDraggingOverGrid && activeDrag.payload is DragPayload.FromHome

    LaunchedEffect(isDraggingFromHome) {
        if (!isDraggingFromHome) {
            onRemoveZoneBoundsChanged(null)
        }
    }

    if (isMoving) {
        BackHandler(onBack = onCancelMovingItem)
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(onLongPress, isMoving, isDragInProgress) {
                    if (isMoving || isDragInProgress) return@pointerInput
                    detectTapGestures(
                        onLongPress = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onLongPress()
                        },
                    )
                }
                .pointerInput(swipeThresholdPx, isMoving, isDragInProgress) {
                    if (isMoving || isDragInProgress) return@pointerInput
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
                            if (!triggered && totalDragY <= -swipeThresholdPx) {
                                triggered = true
                                onOpenOverview()
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
            } else if (uiState.showClock) {
                HomeClock(
                    compact = hasWorkspaceItems,
                    modifier = Modifier
                        .graphicsLayer {
                            alpha = if (isDraggingFromHome) 0f else 1f
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
                        movingItemId = uiState.movingItemId,
                        activeDrag = activeDrag,
                        dropTarget = dropTarget,
                        appWidgetController = appWidgetController,
                        onAppClick = onAppClick,
                        canPlaceMovingItemAt = canPlaceMovingItemAt,
                        onPlaceMovingItemAt = onPlaceMovingItemAt,
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
                Spacer(modifier = Modifier.height(10.dp))
                DashBar(
                    apps = uiState.dashApps,
                    onAppClick = onAppClick,
                )
            }
        }

        if (isDraggingFromHome) {
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
    movingItemId: String?,
    activeDrag: ActiveDragState?,
    dropTarget: HomeDropTarget?,
    appWidgetController: AppWidgetController,
    onAppClick: (app: AppInfo, sourceView: View, sourceBounds: Rect?) -> Unit,
    canPlaceMovingItemAt: (cellX: Int, cellY: Int) -> Boolean,
    onPlaceMovingItemAt: (cellX: Int, cellY: Int) -> Unit,
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

    BoxWithConstraints(modifier = modifier) {
        val cellWidth = maxWidth / HomeGridSpec.COLUMNS
        val cellHeight = maxHeight / HomeGridSpec.ROWS

        if (isDraggingOverGrid) {
            val cellShape = RoundedCornerShape(OverviewCornerRadius.Medium)
            for (y in 0 until HomeGridSpec.ROWS) {
                for (x in 0 until HomeGridSpec.COLUMNS) {
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
                val targetShape = RoundedCornerShape(OverviewCornerRadius.Large)
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
            for (y in 0 until HomeGridSpec.ROWS) {
                for (x in 0 until HomeGridSpec.COLUMNS) {
                    val canPlace = canPlaceMovingItemAt(x, y)
                    val cellShape = RoundedCornerShape(OverviewCornerRadius.Medium)
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
                val itemWidth = cellWidth * item.spanX
                val itemHeight = cellHeight * item.spanY

                Box(
                    modifier = Modifier
                        .offset(x = cellWidth * item.cellX, y = cellHeight * item.cellY)
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
                                iconSize = 52.dp,
                                showLabel = true,
                                labelColor = colors.wallpaperForeground,
                                labelShadow = labelShadow,
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
                                                shape = RoundedCornerShape(OverviewCornerRadius.Large),
                                            )
                                        } else {
                                            Modifier
                                        },
                                    ),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RemoveDropZonePill(
    isActive: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = OverviewTheme.colors
    val shape = RoundedCornerShape(OverviewCornerRadius.Pill)
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
private fun MoveModeBanner(
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = OverviewTheme.colors
    val shape = RoundedCornerShape(OverviewCornerRadius.Pill)

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
private fun HomeClock(
    compact: Boolean,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val colors = OverviewTheme.colors
    var currentTimeMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }

    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                currentTimeMillis = System.currentTimeMillis()
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_TIME_TICK)
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
        }
        context.registerReceiver(receiver, filter)
        onDispose {
            context.unregisterReceiver(receiver)
        }
    }

    val now = remember(currentTimeMillis) { Date(currentTimeMillis) }
    val is24Hour = DateFormat.is24HourFormat(context)
    val timeText = remember(now, is24Hour) {
        val pattern = if (is24Hour) "HH:mm" else "h:mm"
        SimpleDateFormat(pattern, Locale.getDefault()).format(now)
    }
    val dateText = remember(now) {
        val pattern = DateFormat.getBestDateTimePattern(Locale.getDefault(), "EEEEdMMMM")
        SimpleDateFormat(pattern, Locale.getDefault()).format(now)
    }

    val textShadow = Shadow(
        color = colors.wallpaperShadow,
        offset = Offset(0f, 2f),
        blurRadius = 10f,
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = timeText,
            style = if (compact) {
                MaterialTheme.typography.displayLarge.copy(
                    fontSize = 38.sp,
                    lineHeight = 42.sp,
                    shadow = textShadow,
                )
            } else {
                MaterialTheme.typography.displayLarge.copy(shadow = textShadow)
            },
            color = colors.wallpaperForeground,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = dateText,
            style = (if (compact) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.titleMedium)
                .copy(shadow = textShadow),
            color = colors.wallpaperForeground.copy(alpha = 0.92f),
        )
    }
}

@Composable
private fun OverviewHandlePill(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = OverviewTheme.colors
    val label = stringResource(R.string.open_overview_content_description)
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(OverviewCornerRadius.Pill))
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
                .clip(RoundedCornerShape(OverviewCornerRadius.Pill))
                .background(colors.wallpaperForeground.copy(alpha = 0.72f)),
        )
    }
}
