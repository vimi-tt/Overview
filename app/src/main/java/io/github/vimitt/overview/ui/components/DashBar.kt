package io.github.vimitt.overview.ui.components

import android.graphics.Rect
import android.view.View
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import io.github.vimitt.overview.domain.model.AppInfo
import io.github.vimitt.overview.ui.theme.OverviewTheme

/**
 * Dash inferior inspirado no GNOME Shell, exibindo até 5 aplicativos favoritos.
 * Suporta reorganização por drag & drop e animações de mola no modo Expressive
 * apenas quando está visível para economizar recursos.
 */
@Composable
fun DashBar(
    apps: List<AppInfo>,
    onAppClick: (app: AppInfo, sourceView: View, sourceBounds: Rect?) -> Unit,
    modifier: Modifier = Modifier,
    backgroundAlpha: Float = 0.85f,
    showNotificationDots: Boolean = false,
    badgePackages: Set<String> = emptySet(),
    isVisible: Boolean = true,
    onAppDragStart: ((app: AppInfo, index: Int, pointerInWindow: Offset) -> Unit)? = null,
    onAppDragMove: ((pointerInWindow: Offset) -> Unit)? = null,
    onAppDragEnd: (() -> Unit)? = null,
    onAppDragCancel: (() -> Unit)? = null,
    draggingFromDockIndex: Int? = null,
    dockDropHighlightIndex: Int? = null,
) {
    if (apps.isEmpty()) return

    val colors = OverviewTheme.colors
    val motion = OverviewTheme.motion
    val isExpressive = OverviewTheme.components.isExpressive
    val expressiveMotionEnabled = isExpressive && motion.usesSprings && !motion.reducedMotion
    val animateWhenVisible = expressiveMotionEnabled && isVisible

    val containerShape = if (isExpressive) OverviewTheme.shapes.pill else OverviewTheme.shapes.dash
    val clampedAlpha = backgroundAlpha.coerceIn(0f, 1f)
    val backgroundColor = if (isExpressive) {
        lerp(colors.surfaceContainerHigh, colors.accentContainer, 0.22f).copy(alpha = clampedAlpha)
    } else {
        colors.dashBackground.copy(alpha = clampedAlpha)
    }
    val borderColor = if (isExpressive) {
        colors.outlineVariant.copy(alpha = (colors.outlineVariant.alpha * clampedAlpha).coerceIn(0f, 1f))
    } else {
        colors.hairlineBorder.copy(alpha = (colors.hairlineBorder.alpha * clampedAlpha).coerceIn(0f, 1f))
    }

    Row(
        modifier = modifier
            .widthIn(max = 420.dp)
            .fillMaxWidth()
            .then(
                if (isExpressive && clampedAlpha > 0.15f) {
                    Modifier.shadow(
                        elevation = (6f * clampedAlpha).dp,
                        shape = containerShape,
                        clip = false,
                    )
                } else {
                    Modifier
                },
            )
            .clip(containerShape)
            .background(backgroundColor)
            .border(width = 1.dp, color = borderColor, shape = containerShape)
            .padding(
                horizontal = if (isExpressive) 14.dp else 12.dp,
                vertical = if (isExpressive) 8.dp else 6.dp,
            ),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        for ((index, app) in apps.withIndex()) {
            key(app.key) {
                val isDragSource = draggingFromDockIndex == index
                val isDropTarget = dockDropHighlightIndex == index
                val slotScale by animateFloatAsState(
                    targetValue = if (isDropTarget) 1.12f else 1f,
                    animationSpec = if (animateWhenVisible) motion.expressiveSpec() else snap(),
                    label = "dash_slot_scale",
                )
                val highlightAlpha by animateFloatAsState(
                    targetValue = if (isDropTarget) 0.24f else 0f,
                    animationSpec = if (animateWhenVisible) motion.effectSpec(durationMillis = 120) else snap(),
                    label = "dash_slot_highlight",
                )
                val startAtIndex = remember(index, onAppDragStart) {
                    onAppDragStart?.let { callback ->
                        { draggedApp: AppInfo, pointerInWindow: Offset ->
                            callback(draggedApp, index, pointerInWindow)
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .graphicsLayer {
                            scaleX = slotScale
                            scaleY = slotScale
                            alpha = if (isDragSource) 0.28f else 1f
                        }
                        .clip(OverviewTheme.shapes.medium)
                        .then(
                            if (highlightAlpha > 0f) {
                                Modifier.background(colors.accent.copy(alpha = highlightAlpha))
                            } else {
                                Modifier
                            },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    AppIconItem(
                        app = app,
                        onClick = onAppClick,
                        iconSize = 48.dp,
                        showLabel = false,
                        showNotificationDot = showNotificationDots && app.packageName in badgePackages,
                        onDragStart = startAtIndex,
                        onDragMove = onAppDragMove,
                        onDragEnd = onAppDragEnd,
                        onDragCancel = onAppDragCancel,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}
