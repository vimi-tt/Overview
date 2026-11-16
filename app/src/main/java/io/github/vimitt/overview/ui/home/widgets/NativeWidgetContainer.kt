package io.github.vimitt.overview.ui.home.widgets

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.vimitt.overview.ui.theme.OverviewTheme

private class NativeWidgetCoordinatesHolder {
    var coordinates: LayoutCoordinates? = null
}

/**
 * Contêiner comum para todos os widgets nativos do Overview.
 * Adapta superfície, bordas, forma e entrada animada automaticamente ao estilo visual
 * ativo (GNOME Adwaita calmo vs. Material 3 Expressive tonal) e suporta drag após toque longo.
 */
@Composable
fun NativeWidgetCard(
    itemId: String,
    isVisible: Boolean,
    isMoving: Boolean,
    onDragStart: (Offset) -> Unit,
    onDragMove: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit,
    modifier: Modifier = Modifier,
    compactPadding: Boolean = false,
    onCardTap: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = OverviewTheme.colors
    val shapes = OverviewTheme.shapes
    val motion = OverviewTheme.motion
    val isExpressive = OverviewTheme.components.isExpressive
    val expressiveMotionEnabled = isExpressive && motion.usesSprings && !motion.reducedMotion
    val animateWhenVisible = expressiveMotionEnabled && isVisible

    val entranceScale by animateFloatAsState(
        targetValue = if (isVisible || !expressiveMotionEnabled) 1f else 0.96f,
        animationSpec = if (animateWhenVisible) motion.spatialSpec() else snap(),
        label = "native_widget_scale",
    )
    val entranceAlpha by animateFloatAsState(
        targetValue = if (isVisible || !expressiveMotionEnabled) 1f else 0f,
        animationSpec = if (animateWhenVisible) motion.effectSpec(durationMillis = 150) else snap(),
        label = "native_widget_alpha",
    )

    val containerShape = if (isExpressive) shapes.dash else shapes.large
    val containerColor = if (isExpressive) {
        lerp(colors.surfaceContainerHigh, colors.accentContainer, 0.22f).copy(alpha = 0.92f)
    } else {
        colors.surface.copy(alpha = 0.88f)
    }
    val borderColor = when {
        isMoving -> colors.accent
        isExpressive -> colors.accent.copy(alpha = 0.28f)
        else -> colors.hairlineBorder
    }
    val borderWidth = if (isMoving) 2.dp else 1.dp

    val haptic = LocalHapticFeedback.current
    val coordsHolder = remember { NativeWidgetCoordinatesHolder() }
    val currentOnDragStart by rememberUpdatedState(onDragStart)
    val currentOnDragMove by rememberUpdatedState(onDragMove)
    val currentOnDragEnd by rememberUpdatedState(onDragEnd)
    val currentOnDragCancel by rememberUpdatedState(onDragCancel)

    Box(
        modifier = modifier
            .fillMaxSize()
            .then(
                if (expressiveMotionEnabled) {
                    Modifier.graphicsLayer {
                        scaleX = entranceScale
                        scaleY = entranceScale
                        alpha = entranceAlpha
                    }
                } else {
                    Modifier
                },
            )
            .onPlaced { coordsHolder.coordinates = it }
            .pointerInput(itemId) {
                var activePointerPos = Offset.Zero
                detectDragGesturesAfterLongPress(
                    onDragStart = { localOffset ->
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        val coords = coordsHolder.coordinates
                        activePointerPos = coords?.localToWindow(localOffset) ?: localOffset
                        currentOnDragStart(activePointerPos)
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        activePointerPos += dragAmount
                        currentOnDragMove(activePointerPos)
                    },
                    onDragEnd = { currentOnDragEnd() },
                    onDragCancel = { currentOnDragCancel() },
                )
            }
            .then(
                if (isExpressive) {
                    Modifier.shadow(elevation = 8.dp, shape = containerShape, clip = false)
                } else {
                    Modifier
                },
            )
            .clip(containerShape)
            .background(containerColor)
            .border(width = borderWidth, color = borderColor, shape = containerShape)
            .then(
                if (onCardTap != null) {
                    Modifier.clickable(role = Role.Button, onClick = onCardTap)
                } else {
                    Modifier
                },
            )
            .padding(
                horizontal = if (compactPadding) 12.dp else if (isExpressive) 14.dp else 13.dp,
                vertical = if (compactPadding) 8.dp else if (isExpressive) 12.dp else 10.dp,
            ),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
            content = content,
        )
    }
}

/**
 * Cabeçalho adaptativo dos widgets nativos:
 * discreto no estilo GNOME e com badge/tipografia tonal no Material Expressive.
 */
@Composable
fun NativeWidgetHeader(
    title: String,
    trailing: (@Composable RowScope.() -> Unit)? = null,
) {
    val colors = OverviewTheme.colors
    val typography = OverviewTheme.typography
    val isExpressive = OverviewTheme.components.isExpressive

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        if (isExpressive) {
            Text(
                text = title,
                style = typography.sectionHeader.copy(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                ),
                color = colors.accent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
        } else {
            Text(
                text = title.uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(
                    letterSpacing = 0.7.sp,
                    fontWeight = FontWeight.Medium,
                ),
                color = colors.onSurfaceMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
        }
        if (trailing != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                content = trailing,
            )
        }
    }
}

/**
 * Botão/chip compacto usado dentro de widgets para ações rápidas ou solicitação de permissão.
 */
@Composable
fun NativeWidgetActionPill(
    label: String,
    onClick: () -> Unit,
    emphasized: Boolean = true,
) {
    val colors = OverviewTheme.colors
    val shapes = OverviewTheme.shapes
    val isExpressive = OverviewTheme.components.isExpressive
    val pillShape = if (isExpressive) shapes.pill else shapes.small

    val bg = when {
        emphasized && isExpressive -> colors.accentContainer
        emphasized -> colors.accent.copy(alpha = 0.16f)
        isExpressive -> colors.surfaceContainerHigh.copy(alpha = 0.75f)
        else -> colors.surfaceElevated
    }
    val fg = when {
        emphasized && isExpressive -> colors.onAccentContainer
        emphasized -> colors.accent
        else -> colors.onSurface
    }

    Box(
        modifier = Modifier
            .clip(pillShape)
            .background(bg)
            .then(
                if (!isExpressive) {
                    Modifier.border(1.dp, colors.hairlineBorder, pillShape)
                } else {
                    Modifier
                },
            )
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = if (isExpressive) FontWeight.SemiBold else FontWeight.Medium,
            ),
            color = fg,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
