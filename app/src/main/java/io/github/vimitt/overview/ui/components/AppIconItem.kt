package io.github.vimitt.overview.ui.components

import android.graphics.Rect
import android.os.SystemClock
import android.view.View
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.vimitt.overview.domain.model.AppInfo
import io.github.vimitt.overview.ui.theme.OverviewCornerRadius
import io.github.vimitt.overview.ui.theme.OverviewTheme
import kotlin.math.roundToInt

private class CoordinatesHolder {
    var coordinates: LayoutCoordinates? = null
}

private class DragGestureState {
    var isDragging: Boolean = false
    var lastDragEndUptimeMs: Long = 0L
}

/**
 * Item reutilizável de aplicativo (ícone + rótulo opcional) otimizado para
 * rolagem a 60/120 Hz: evita corrotinas por item e usa `onPlaced` em vez de
 * `onGloballyPositioned` para não disparar callbacks a cada frame de scroll.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppIconItem(
    app: AppInfo,
    onClick: (app: AppInfo, sourceView: View, sourceBounds: Rect?) -> Unit,
    modifier: Modifier = Modifier,
    iconSize: Dp = 54.dp,
    showLabel: Boolean = true,
    labelFontScale: Float = 1f,
    labelColor: Color? = null,
    labelShadow: Shadow? = null,
    showNotificationDot: Boolean = false,
    onLongClick: ((AppInfo) -> Unit)? = null,
    onDragStart: ((AppInfo, Offset) -> Unit)? = null,
    onDragMove: ((Offset) -> Unit)? = null,
    onDragEnd: (() -> Unit)? = null,
    onDragCancel: (() -> Unit)? = null,
) {
    val colors = OverviewTheme.colors
    val view = LocalView.current
    val haptic = LocalHapticFeedback.current
    val iconCoordinatesHolder = remember { CoordinatesHolder() }
    val rootCoordinatesHolder = remember { CoordinatesHolder() }
    val dragState = remember { DragGestureState() }

    val currentApp by rememberUpdatedState(app)
    val currentOnClick by rememberUpdatedState(onClick)
    val currentOnDragStart by rememberUpdatedState(onDragStart)
    val currentOnDragMove by rememberUpdatedState(onDragMove)
    val currentOnDragEnd by rememberUpdatedState(onDragEnd)
    val currentOnDragCancel by rememberUpdatedState(onDragCancel)

    val triggerClick: () -> Unit = {
        val elapsedSinceDrag = SystemClock.uptimeMillis() - dragState.lastDragEndUptimeMs
        if (!dragState.isDragging && elapsedSinceDrag > 350L) {
            val coords = iconCoordinatesHolder.coordinates
            val bounds = if (coords != null && coords.isAttached) {
                val rect = coords.boundsInWindow()
                Rect(
                    rect.left.roundToInt(),
                    rect.top.roundToInt(),
                    rect.right.roundToInt(),
                    rect.bottom.roundToInt(),
                )
            } else {
                null
            }
            currentOnClick(currentApp, view, bounds)
        }
    }

    val interactionModifier = if (onDragStart != null) {
        Modifier
            .clickable(onClick = triggerClick)
            .pointerInput(app.key) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { localOffset ->
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        dragState.isDragging = true
                        val coords = rootCoordinatesHolder.coordinates
                        val windowPos = if (coords != null && coords.isAttached) {
                            coords.localToWindow(localOffset)
                        } else {
                            localOffset
                        }
                        currentOnDragStart?.invoke(currentApp, windowPos)
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        val coords = rootCoordinatesHolder.coordinates
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
    } else {
        Modifier.combinedClickable(
            onClick = triggerClick,
            onLongClick = if (onLongClick != null) {
                {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongClick(app)
                }
            } else {
                null
            },
        )
    }

    Column(
        modifier = modifier
            .onPlaced { coordinates ->
                rootCoordinatesHolder.coordinates = coordinates
            }
            .clip(RoundedCornerShape(OverviewCornerRadius.Medium))
            .then(interactionModifier)
            .padding(horizontal = 6.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top,
    ) {
        Box(
            modifier = Modifier
                .size(iconSize)
                .onPlaced { coordinates ->
                    iconCoordinatesHolder.coordinates = coordinates
                },
            contentAlignment = Alignment.Center,
        ) {
            val bitmap = app.icon
            if (bitmap != null) {
                Image(
                    bitmap = bitmap,
                    contentDescription = app.label,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit,
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
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

            if (showNotificationDot) {
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
            val labelStyle = MaterialTheme.typography.labelMedium
            Text(
                text = app.label,
                style = labelStyle.copy(
                    fontSize = labelStyle.fontSize * labelFontScale,
                    lineHeight = labelStyle.lineHeight * labelFontScale,
                    shadow = labelShadow,
                ),
                color = labelColor ?: colors.onSurface,
                minLines = 2,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
        }
    }
}
