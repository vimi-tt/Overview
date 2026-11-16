package io.github.vimitt.overview.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import io.github.vimitt.overview.domain.model.AppInfo
import io.github.vimitt.overview.domain.model.HomeGridSpec
import io.github.vimitt.overview.ui.home.HomeWorkspaceItem
import io.github.vimitt.overview.ui.theme.OverviewTheme
import kotlin.math.floor
import kotlin.math.roundToInt

/**
 * Origem e dados do item sendo arrastado diretamente na tela inicial ou a partir do overview.
 */
sealed interface DragPayload {
    val spanX: Int
    val spanY: Int
    val ignoreItemId: String?
    val draggedApp: AppInfo?

    data class FromOverview(val app: AppInfo) : DragPayload {
        override val spanX: Int = 1
        override val spanY: Int = 1
        override val ignoreItemId: String? = null
        override val draggedApp: AppInfo = app
    }

    data class FromHome(val item: HomeWorkspaceItem) : DragPayload {
        override val spanX: Int = item.item.spanX
        override val spanY: Int = item.item.spanY
        override val ignoreItemId: String = item.item.id
        override val draggedApp: AppInfo? = (item as? HomeWorkspaceItem.Shortcut)?.app
    }

    data class FromDock(
        val app: AppInfo,
        val sourceIndex: Int,
    ) : DragPayload {
        override val spanX: Int = 1
        override val spanY: Int = 1
        override val ignoreItemId: String? = null
        override val draggedApp: AppInfo = app
    }
}

/**
 * Estado em tempo real do gesto de arrastar ativo.
 */
data class ActiveDragState(
    val payload: DragPayload,
    val startInWindow: Offset,
    val pointerInWindow: Offset,
    val hasMovedPastSlop: Boolean,
)

/**
 * Célula de destino calculada na grade da tela inicial durante um arraste.
 */
data class HomeDropTarget(
    val cellX: Int,
    val cellY: Int,
    val spanX: Int,
    val spanY: Int,
    val isValid: Boolean,
    val isOverRemoveZone: Boolean,
)

/**
 * Calcula a célula alvo ou zona de remoção correspondente à posição do dedo na janela.
 */
/**
 * Destino calculado sobre a barra da dock durante um drag & drop.
 */
data class DockDropTarget(
    val targetIndex: Int,
)

/**
 * Calcula em qual posição da dock um app arrastado cairia com base nas coordenadas na janela.
 * Aceita apps vindos do overview, de atalhos da tela inicial ou da própria dock.
 */
fun computeDockDropTarget(
    pointerInWindow: Offset,
    dockBounds: Rect?,
    itemCount: Int,
    payload: DragPayload,
): DockDropTarget? {
    if (payload.draggedApp == null || dockBounds == null || dockBounds.width <= 0f) return null
    if (!dockBounds.contains(pointerInWindow)) return null

    val slots = itemCount.coerceAtLeast(1)
    val relativeX = ((pointerInWindow.x - dockBounds.left) / dockBounds.width).coerceIn(0f, 0.9999f)
    val targetIndex = (relativeX * slots).toInt().coerceIn(0, slots - 1)
    return DockDropTarget(targetIndex = targetIndex)
}

fun computeHomeDropTarget(
    pointerInWindow: Offset,
    gridBounds: Rect?,
    removeZoneBounds: Rect?,
    payload: DragPayload,
    canPlace: (cellX: Int, cellY: Int, spanX: Int, spanY: Int, ignoreItemId: String?) -> Boolean,
    gridColumns: Int = HomeGridSpec.COLUMNS,
    gridRows: Int = HomeGridSpec.ROWS,
): HomeDropTarget? {
    if ((payload is DragPayload.FromHome || payload is DragPayload.FromDock) &&
        removeZoneBounds != null &&
        removeZoneBounds.contains(pointerInWindow)
    ) {
        val homeItem = (payload as? DragPayload.FromHome)?.item?.item
        return HomeDropTarget(
            cellX = homeItem?.cellX ?: 0,
            cellY = homeItem?.cellY ?: 0,
            spanX = payload.spanX,
            spanY = payload.spanY,
            isValid = true,
            isOverRemoveZone = true,
        )
    }

    if (gridBounds == null || gridBounds.width <= 0f || gridBounds.height <= 0f) {
        return null
    }

    // Cancela o drop se o dedo estiver muito abaixo da área da grade (ex.: sobre o dash).
    val verticalMarginBottomPx = 48f
    val verticalMarginTopPx = 72f
    if (pointerInWindow.y > gridBounds.bottom + verticalMarginBottomPx ||
        pointerInWindow.y < gridBounds.top - verticalMarginTopPx
    ) {
        return null
    }

    val clampedX = pointerInWindow.x.coerceIn(gridBounds.left, gridBounds.right - 1f)
    val clampedY = pointerInWindow.y.coerceIn(gridBounds.top, gridBounds.bottom - 1f)

    val safeColumns = gridColumns.coerceAtLeast(1)
    val safeRows = gridRows.coerceAtLeast(1)
    val cellWidth = gridBounds.width / safeColumns
    val cellHeight = gridBounds.height / safeRows

    val rawCol = (clampedX - gridBounds.left) / cellWidth
    val rawRow = (clampedY - gridBounds.top) / cellHeight

    val targetCellX = floor(rawCol - (payload.spanX - 1) / 2f)
        .toInt()
        .coerceIn(0, (safeColumns - payload.spanX).coerceAtLeast(0))
    val targetCellY = floor(rawRow - (payload.spanY - 1) / 2f)
        .toInt()
        .coerceIn(0, (safeRows - payload.spanY).coerceAtLeast(0))

    val isValid = canPlace(
        targetCellX,
        targetCellY,
        payload.spanX,
        payload.spanY,
        payload.ignoreItemId,
    )

    return HomeDropTarget(
        cellX = targetCellX,
        cellY = targetCellY,
        spanX = payload.spanX,
        spanY = payload.spanY,
        isValid = isValid,
        isOverRemoveZone = false,
    )
}

/**
 * Representação visual flutuante do item sendo arrastado acima do dedo do usuário.
 */
@Composable
fun DragGhostOverlay(
    activeDrag: ActiveDragState?,
    isOverRemoveZone: Boolean,
    modifier: Modifier = Modifier,
) {
    if (activeDrag == null || !activeDrag.hasMovedPastSlop) return

    val colors = OverviewTheme.colors
    val motion = OverviewTheme.motion
    val shapes = OverviewTheme.shapes
    val density = LocalDensity.current
    var overlayOriginInWindow by remember { mutableStateOf(Offset.Zero) }
    var ghostWidthPx by remember { mutableStateOf(0) }
    var ghostHeightPx by remember { mutableStateOf(0) }
    var isPickedUp by remember(activeDrag) { mutableStateOf(false) }
    LaunchedEffect(activeDrag) {
        isPickedUp = true
    }
    val pickupScale by animateFloatAsState(
        targetValue = if (isPickedUp) 1.04f else 0.94f,
        animationSpec = motion.expressiveSpec(durationMillis = 160),
        label = "dragGhostPickupScale",
    )

    val localPointer = activeDrag.pointerInWindow - overlayOriginInWindow
    val verticalLiftPx = with(density) { 44.dp.roundToPx() }
    val offsetX = (localPointer.x - ghostWidthPx / 2f).roundToInt()
    val offsetY = (localPointer.y - ghostHeightPx / 2f - verticalLiftPx).roundToInt()

    val borderColor by animateColorAsState(
        targetValue = if (isOverRemoveZone) {
            Color(0xFFE01B24)
        } else {
            colors.accent.copy(alpha = 0.85f)
        },
        animationSpec = motion.effectSpec(durationMillis = 120),
        label = "dragGhostBorder",
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .onPlaced { coords ->
                overlayOriginInWindow = coords.positionInWindow()
            },
    ) {
        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX, offsetY) }
                .graphicsLayer {
                    scaleX = pickupScale
                    scaleY = pickupScale
                }
                .onPlaced { coords ->
                    ghostWidthPx = coords.size.width
                    ghostHeightPx = coords.size.height
                },
        ) {
            when (val payload = activeDrag.payload) {
                is DragPayload.FromOverview -> {
                    ShortcutDragGhost(
                        app = payload.app,
                        borderColor = borderColor,
                        cardShape = shapes.large,
                        fallbackShape = shapes.medium,
                    )
                }

                is DragPayload.FromDock -> {
                    ShortcutDragGhost(
                        app = payload.app,
                        borderColor = borderColor,
                        cardShape = shapes.large,
                        fallbackShape = shapes.medium,
                    )
                }

                is DragPayload.FromHome -> {
                    when (val item = payload.item) {
                        is HomeWorkspaceItem.Shortcut -> {
                            ShortcutDragGhost(
                                app = item.app,
                                borderColor = borderColor,
                                cardShape = shapes.large,
                                fallbackShape = shapes.medium,
                            )
                        }

                        is HomeWorkspaceItem.Widget -> {
                            WidgetDragGhost(
                                label = item.label,
                                spanX = item.item.spanX,
                                spanY = item.item.spanY,
                                borderColor = borderColor,
                                cardShape = shapes.large,
                            )
                        }

                        is HomeWorkspaceItem.NativeWidget -> {
                            WidgetDragGhost(
                                label = item.label,
                                spanX = item.item.spanX,
                                spanY = item.item.spanY,
                                borderColor = borderColor,
                                cardShape = shapes.large,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ShortcutDragGhost(
    app: AppInfo,
    borderColor: Color,
    cardShape: androidx.compose.ui.graphics.Shape,
    fallbackShape: androidx.compose.ui.graphics.Shape,
) {
    val colors = OverviewTheme.colors

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .shadow(12.dp, cardShape)
            .clip(cardShape)
            .background(colors.surfaceElevated.copy(alpha = 0.94f))
            .border(1.5.dp, borderColor, cardShape)
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        val bitmap = app.icon
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = app.label,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(58.dp),
            )
        } else {
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .clip(fallbackShape)
                    .background(colors.surfaceElevated),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Home,
                    contentDescription = app.label,
                    tint = colors.onSurfaceMuted,
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = app.label,
            style = MaterialTheme.typography.labelSmall,
            color = colors.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = 88.dp),
        )
    }
}

@Composable
private fun WidgetDragGhost(
    label: String,
    spanX: Int,
    spanY: Int,
    borderColor: Color,
    cardShape: androidx.compose.ui.graphics.Shape,
) {
    val colors = OverviewTheme.colors
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .shadow(14.dp, cardShape)
            .clip(cardShape)
            .background(colors.surfaceElevated.copy(alpha = 0.94f))
            .border(1.5.dp, borderColor, cardShape)
            .padding(horizontal = 18.dp, vertical = 14.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            repeat(2) {
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    repeat(2) {
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(colors.accent.copy(alpha = 0.85f)),
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium),
            color = colors.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = 160.dp),
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = "${spanX}×${spanY}",
            style = MaterialTheme.typography.labelSmall,
            color = colors.onSurfaceMuted,
        )
    }
}
