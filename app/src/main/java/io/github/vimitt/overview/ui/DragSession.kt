package io.github.vimitt.overview.ui

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
import io.github.vimitt.overview.ui.theme.OverviewCornerRadius
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

    data class FromOverview(val app: AppInfo) : DragPayload {
        override val spanX: Int = 1
        override val spanY: Int = 1
        override val ignoreItemId: String? = null
    }

    data class FromHome(val item: HomeWorkspaceItem) : DragPayload {
        override val spanX: Int = item.item.spanX
        override val spanY: Int = item.item.spanY
        override val ignoreItemId: String = item.item.id
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
fun computeHomeDropTarget(
    pointerInWindow: Offset,
    gridBounds: Rect?,
    removeZoneBounds: Rect?,
    payload: DragPayload,
    canPlace: (cellX: Int, cellY: Int, spanX: Int, spanY: Int, ignoreItemId: String?) -> Boolean,
    gridColumns: Int = HomeGridSpec.COLUMNS,
    gridRows: Int = HomeGridSpec.ROWS,
): HomeDropTarget? {
    if (payload is DragPayload.FromHome &&
        removeZoneBounds != null &&
        removeZoneBounds.contains(pointerInWindow)
    ) {
        return HomeDropTarget(
            cellX = payload.item.item.cellX,
            cellY = payload.item.item.cellY,
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
    val density = LocalDensity.current
    var overlayOriginInWindow by remember { mutableStateOf(Offset.Zero) }
    var ghostWidthPx by remember { mutableStateOf(0) }
    var ghostHeightPx by remember { mutableStateOf(0) }

    val localPointer = activeDrag.pointerInWindow - overlayOriginInWindow
    val verticalLiftPx = with(density) { 44.dp.roundToPx() }
    val offsetX = (localPointer.x - ghostWidthPx / 2f).roundToInt()
    val offsetY = (localPointer.y - ghostHeightPx / 2f - verticalLiftPx).roundToInt()

    val borderColor = if (isOverRemoveZone) {
        Color(0xFFE01B24)
    } else {
        colors.accent.copy(alpha = 0.85f)
    }

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
                .onPlaced { coords ->
                    ghostWidthPx = coords.size.width
                    ghostHeightPx = coords.size.height
                },
        ) {
            when (val payload = activeDrag.payload) {
                is DragPayload.FromOverview -> {
                    ShortcutDragGhost(app = payload.app, borderColor = borderColor)
                }

                is DragPayload.FromHome -> {
                    when (val item = payload.item) {
                        is HomeWorkspaceItem.Shortcut -> {
                            ShortcutDragGhost(app = item.app, borderColor = borderColor)
                        }

                        is HomeWorkspaceItem.Widget -> {
                            WidgetDragGhost(
                                label = item.label,
                                spanX = item.item.spanX,
                                spanY = item.item.spanY,
                                borderColor = borderColor,
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
) {
    val colors = OverviewTheme.colors

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .shadow(12.dp, RoundedCornerShape(OverviewCornerRadius.Large))
            .clip(RoundedCornerShape(OverviewCornerRadius.Large))
            .background(colors.surfaceElevated.copy(alpha = 0.94f))
            .border(1.5.dp, borderColor, RoundedCornerShape(OverviewCornerRadius.Large))
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
                    .clip(RoundedCornerShape(OverviewCornerRadius.Medium))
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
) {
    val colors = OverviewTheme.colors
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .shadow(14.dp, RoundedCornerShape(OverviewCornerRadius.Large))
            .clip(RoundedCornerShape(OverviewCornerRadius.Large))
            .background(colors.surfaceElevated.copy(alpha = 0.94f))
            .border(1.5.dp, borderColor, RoundedCornerShape(OverviewCornerRadius.Large))
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
