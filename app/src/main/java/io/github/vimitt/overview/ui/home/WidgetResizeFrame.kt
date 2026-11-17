package io.github.vimitt.overview.ui.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import io.github.vimitt.overview.domain.model.HomeItem
import io.github.vimitt.overview.ui.theme.OverviewTheme
import kotlin.math.roundToInt

private enum class ResizeEdge {
    LEFT,
    TOP,
    RIGHT,
    BOTTOM,
}

private data class ResizeCellCandidate(
    val cellX: Int,
    val cellY: Int,
    val spanX: Int,
    val spanY: Int,
)

/**
 * Moldura interativa sobre a grade que permite arrastar as 4 extremidades de um widget
 * selecionado para redimensioná-lo dinamicamente célula a célula.
 */
@Composable
fun WidgetResizeOverlayFrame(
    workspaceItem: HomeWorkspaceItem,
    gridColumns: Int,
    gridRows: Int,
    cellWidthPx: Float,
    cellHeightPx: Float,
    canPlaceItem: (cellX: Int, cellY: Int, spanX: Int, spanY: Int, ignoreItemId: String?) -> Boolean,
    onCommitBounds: (cellX: Int, cellY: Int, spanX: Int, spanY: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (cellWidthPx <= 1f || cellHeightPx <= 1f) return
    val item = workspaceItem.item
    if (item !is HomeItem.Widget && item !is HomeItem.NativeWidget) return

    val colors = OverviewTheme.colors
    val shapes = OverviewTheme.shapes
    val motion = OverviewTheme.motion
    val isExpressive = OverviewTheme.components.isExpressive
    val expressiveMotion = isExpressive && motion.usesSprings && !motion.reducedMotion
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current

    val minSpanX = when (item) {
        is HomeItem.NativeWidget -> item.widgetType.minSpanX.coerceIn(1, gridColumns)
        is HomeItem.Widget -> 1
    }
    val minSpanY = when (item) {
        is HomeItem.NativeWidget -> item.widgetType.minSpanY.coerceIn(1, gridRows)
        is HomeItem.Widget -> 1
    }

    val safeSpanX = item.spanX.coerceIn(minSpanX, gridColumns)
    val safeSpanY = item.spanY.coerceIn(minSpanY, gridRows)
    val safeCellX = item.cellX.coerceIn(0, (gridColumns - safeSpanX).coerceAtLeast(0))
    val safeCellY = item.cellY.coerceIn(0, (gridRows - safeSpanY).coerceAtLeast(0))

    val origLeft = safeCellX * cellWidthPx
    val origTop = safeCellY * cellHeightPx
    val origRight = (safeCellX + safeSpanX) * cellWidthPx
    val origBottom = (safeCellY + safeSpanY) * cellHeightPx
    val minWidthPx = minSpanX * cellWidthPx
    val minHeightPx = minSpanY * cellHeightPx
    val gridWidthPx = gridColumns * cellWidthPx
    val gridHeightPx = gridRows * cellHeightPx

    var activeEdge by remember(item.id, safeCellX, safeCellY, safeSpanX, safeSpanY) {
        mutableStateOf<ResizeEdge?>(null)
    }
    var dragDeltaX by remember(item.id, safeCellX, safeCellY, safeSpanX, safeSpanY) {
        mutableFloatStateOf(0f)
    }
    var dragDeltaY by remember(item.id, safeCellX, safeCellY, safeSpanX, safeSpanY) {
        mutableFloatStateOf(0f)
    }

    val liveLeft = if (activeEdge == ResizeEdge.LEFT) {
        (origLeft + dragDeltaX).coerceIn(0f, origRight - minWidthPx)
    } else {
        origLeft
    }
    val liveRight = if (activeEdge == ResizeEdge.RIGHT) {
        (origRight + dragDeltaX).coerceIn(origLeft + minWidthPx, gridWidthPx)
    } else {
        origRight
    }
    val liveTop = if (activeEdge == ResizeEdge.TOP) {
        (origTop + dragDeltaY).coerceIn(0f, origBottom - minHeightPx)
    } else {
        origTop
    }
    val liveBottom = if (activeEdge == ResizeEdge.BOTTOM) {
        (origBottom + dragDeltaY).coerceIn(origTop + minHeightPx, gridHeightPx)
    } else {
        origBottom
    }

    val candidate = remember(
        activeEdge,
        liveLeft,
        liveTop,
        liveRight,
        liveBottom,
        safeCellX,
        safeCellY,
        safeSpanX,
        safeSpanY,
        cellWidthPx,
        cellHeightPx,
    ) {
        val rightCol = safeCellX + safeSpanX
        val bottomRow = safeCellY + safeSpanY
        when (activeEdge) {
            ResizeEdge.LEFT -> {
                val nextCellX = (liveLeft / cellWidthPx).roundToInt()
                    .coerceIn(0, (rightCol - minSpanX).coerceAtLeast(0))
                ResizeCellCandidate(
                    cellX = nextCellX,
                    cellY = safeCellY,
                    spanX = (rightCol - nextCellX).coerceAtLeast(minSpanX),
                    spanY = safeSpanY,
                )
            }

            ResizeEdge.RIGHT -> {
                val nextRightCol = (liveRight / cellWidthPx).roundToInt()
                    .coerceIn(safeCellX + minSpanX, gridColumns)
                ResizeCellCandidate(
                    cellX = safeCellX,
                    cellY = safeCellY,
                    spanX = (nextRightCol - safeCellX).coerceAtLeast(minSpanX),
                    spanY = safeSpanY,
                )
            }

            ResizeEdge.TOP -> {
                val nextCellY = (liveTop / cellHeightPx).roundToInt()
                    .coerceIn(0, (bottomRow - minSpanY).coerceAtLeast(0))
                ResizeCellCandidate(
                    cellX = safeCellX,
                    cellY = nextCellY,
                    spanX = safeSpanX,
                    spanY = (bottomRow - nextCellY).coerceAtLeast(minSpanY),
                )
            }

            ResizeEdge.BOTTOM -> {
                val nextBottomRow = (liveBottom / cellHeightPx).roundToInt()
                    .coerceIn(safeCellY + minSpanY, gridRows)
                ResizeCellCandidate(
                    cellX = safeCellX,
                    cellY = safeCellY,
                    spanX = safeSpanX,
                    spanY = (nextBottomRow - safeCellY).coerceAtLeast(minSpanY),
                )
            }

            null -> ResizeCellCandidate(
                cellX = safeCellX,
                cellY = safeCellY,
                spanX = safeSpanX,
                spanY = safeSpanY,
            )
        }
    }

    val isCandidateValid = remember(candidate, item.id, canPlaceItem) {
        canPlaceItem(
            candidate.cellX,
            candidate.cellY,
            candidate.spanX,
            candidate.spanY,
            item.id,
        )
    }

    LaunchedEffect(candidate, activeEdge) {
        if (activeEdge != null) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    val frameEntranceScale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = if (expressiveMotion) motion.expressiveSpec() else snap(),
        label = "resizeFrameEntrance",
    )

    val frameColor by animateColorAsState(
        targetValue = if (isCandidateValid) colors.accent else colors.error,
        animationSpec = if (expressiveMotion) motion.effectSpec(120) else snap(),
        label = "resizeFrameColor",
    )

    val frameShape = if (isExpressive) shapes.dash else shapes.large
    val currentCandidate by rememberUpdatedState(candidate)
    val currentValid by rememberUpdatedState(isCandidateValid)
    val currentOnCommit by rememberUpdatedState(onCommitBounds)

    val handleTouchPx = with(density) { 48.dp.roundToPx() }
    val halfHandleTouchPx = handleTouchPx / 2

    val insetPx = with(density) { 4.dp.toPx() }
    val snapWidthDp = with(density) {
        (candidate.spanX * cellWidthPx - insetPx * 2f).coerceAtLeast(24f).toDp()
    }
    val snapHeightDp = with(density) {
        (candidate.spanY * cellHeightPx - insetPx * 2f).coerceAtLeast(24f).toDp()
    }
    val liveWidthDp = with(density) {
        ((liveRight - liveLeft) - insetPx * 2f).coerceAtLeast(24f).toDp()
    }
    val liveHeightDp = with(density) {
        ((liveBottom - liveTop) - insetPx * 2f).coerceAtLeast(24f).toDp()
    }

    Box(modifier = modifier.fillMaxSize()) {
        // Sombra do retângulo snapped de destino na grade
        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        x = (candidate.cellX * cellWidthPx + insetPx).roundToInt(),
                        y = (candidate.cellY * cellHeightPx + insetPx).roundToInt(),
                    )
                }
                .size(width = snapWidthDp, height = snapHeightDp)
                .clip(frameShape)
                .background(frameColor.copy(alpha = if (activeEdge != null) 0.20f else 0.08f))
                .border(
                    width = if (activeEdge != null) 2.dp else 1.5.dp,
                    color = frameColor.copy(alpha = if (activeEdge != null) 0.85f else 0.5f),
                    shape = frameShape,
                ),
        )

        // Contorno elástico que acompanha o dedo em tempo real durante o arraste das bordas
        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        x = (liveLeft + insetPx).roundToInt(),
                        y = (liveTop + insetPx).roundToInt(),
                    )
                }
                .size(width = liveWidthDp, height = liveHeightDp)
                .graphicsLayer {
                    scaleX = frameEntranceScale
                    scaleY = frameEntranceScale
                }
                .border(
                    width = 2.5.dp,
                    color = frameColor,
                    shape = frameShape,
                ),
            contentAlignment = Alignment.TopCenter,
        ) {
            // Crachá com a dimensão atual em células (ex.: 2 × 1)
            Box(
                modifier = Modifier
                    .padding(top = 6.dp)
                    .clip(shapes.pill)
                    .background(if (isCandidateValid) colors.accent else colors.error)
                    .padding(horizontal = 10.dp, vertical = 3.dp),
            ) {
                Text(
                    text = "${candidate.spanX} × ${candidate.spanY}",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = if (isCandidateValid) colors.onAccent else colors.onError,
                )
            }
        }

        val finishEdgeDrag: () -> Unit = {
            val snap = currentCandidate
            if (currentValid &&
                (snap.cellX != safeCellX || snap.cellY != safeCellY ||
                    snap.spanX != safeSpanX || snap.spanY != safeSpanY)
            ) {
                currentOnCommit(snap.cellX, snap.cellY, snap.spanX, snap.spanY)
            }
            activeEdge = null
            dragDeltaX = 0f
            dragDeltaY = 0f
        }

        // Alça esquerda
        ResizeEdgeHandle(
            edge = ResizeEdge.LEFT,
            active = activeEdge == ResizeEdge.LEFT,
            accentColor = frameColor,
            isExpressive = isExpressive,
            offsetPx = IntOffset(
                x = (liveLeft + insetPx - halfHandleTouchPx).roundToInt(),
                y = (((liveTop + liveBottom) / 2f) - halfHandleTouchPx).roundToInt(),
            ),
            onDragStart = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                activeEdge = ResizeEdge.LEFT
                dragDeltaX = 0f
            },
            onDragDelta = { dragAmount -> dragDeltaX += dragAmount.x },
            onDragFinish = finishEdgeDrag,
        )

        // Alça direita
        ResizeEdgeHandle(
            edge = ResizeEdge.RIGHT,
            active = activeEdge == ResizeEdge.RIGHT,
            accentColor = frameColor,
            isExpressive = isExpressive,
            offsetPx = IntOffset(
                x = (liveRight - insetPx - halfHandleTouchPx).roundToInt(),
                y = (((liveTop + liveBottom) / 2f) - halfHandleTouchPx).roundToInt(),
            ),
            onDragStart = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                activeEdge = ResizeEdge.RIGHT
                dragDeltaX = 0f
            },
            onDragDelta = { dragAmount -> dragDeltaX += dragAmount.x },
            onDragFinish = finishEdgeDrag,
        )

        // Alça superior
        ResizeEdgeHandle(
            edge = ResizeEdge.TOP,
            active = activeEdge == ResizeEdge.TOP,
            accentColor = frameColor,
            isExpressive = isExpressive,
            offsetPx = IntOffset(
                x = (((liveLeft + liveRight) / 2f) - halfHandleTouchPx).roundToInt(),
                y = (liveTop + insetPx - halfHandleTouchPx).roundToInt(),
            ),
            onDragStart = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                activeEdge = ResizeEdge.TOP
                dragDeltaY = 0f
            },
            onDragDelta = { dragAmount -> dragDeltaY += dragAmount.y },
            onDragFinish = finishEdgeDrag,
        )

        // Alça inferior
        ResizeEdgeHandle(
            edge = ResizeEdge.BOTTOM,
            active = activeEdge == ResizeEdge.BOTTOM,
            accentColor = frameColor,
            isExpressive = isExpressive,
            offsetPx = IntOffset(
                x = (((liveLeft + liveRight) / 2f) - halfHandleTouchPx).roundToInt(),
                y = (liveBottom - insetPx - halfHandleTouchPx).roundToInt(),
            ),
            onDragStart = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                activeEdge = ResizeEdge.BOTTOM
                dragDeltaY = 0f
            },
            onDragDelta = { dragAmount -> dragDeltaY += dragAmount.y },
            onDragFinish = finishEdgeDrag,
        )
    }
}

@Composable
private fun ResizeEdgeHandle(
    edge: ResizeEdge,
    active: Boolean,
    accentColor: Color,
    isExpressive: Boolean,
    offsetPx: IntOffset,
    onDragStart: () -> Unit,
    onDragDelta: (androidx.compose.ui.geometry.Offset) -> Unit,
    onDragFinish: () -> Unit,
) {
    val colors = OverviewTheme.colors
    val shapes = OverviewTheme.shapes
    val motion = OverviewTheme.motion
    val isVerticalEdge = edge == ResizeEdge.LEFT || edge == ResizeEdge.RIGHT
    val scale by animateFloatAsState(
        targetValue = if (active) 1.22f else 1f,
        animationSpec = if (isExpressive && motion.usesSprings && !motion.reducedMotion) {
            motion.expressiveSpec()
        } else {
            snap()
        },
        label = "resizeHandleScale",
    )
    val pillW = if (isVerticalEdge) 10.dp else 32.dp
    val pillH = if (isVerticalEdge) 32.dp else 10.dp

    Box(
        modifier = Modifier
            .offset { offsetPx }
            .size(48.dp)
            .semantics { contentDescription = edge.name }
            .pointerInput(edge) {
                detectDragGestures(
                    onDragStart = { onDragStart() },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        onDragDelta(dragAmount)
                    },
                    onDragEnd = { onDragFinish() },
                    onDragCancel = { onDragFinish() },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .width(pillW)
                .height(pillH)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .clip(shapes.pill)
                .background(if (active) accentColor else colors.surfaceElevated)
                .border(
                    width = 2.dp,
                    color = accentColor,
                    shape = shapes.pill,
                ),
        )
    }
}
