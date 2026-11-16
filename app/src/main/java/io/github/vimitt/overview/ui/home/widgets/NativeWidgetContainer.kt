package io.github.vimitt.overview.ui.home.widgets

import android.graphics.Matrix
import android.graphics.Path as AndroidPath
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.vimitt.overview.domain.model.IconShape
import io.github.vimitt.overview.ui.components.OverviewPolygonFactory
import io.github.vimitt.overview.ui.theme.OverviewTheme

private class NativeWidgetCoordinatesHolder {
    var coordinates: LayoutCoordinates? = null
}

/**
 * Contêiner comum para todos os widgets nativos do Overview.
 * No modo GNOME preserva superfície calma e plana; no Material 3 Expressive aplica
 * camadas geométricas tonais, morphing de cantos e física de mola apenas quando visível.
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
        targetValue = if (isVisible || !expressiveMotionEnabled) 1f else 0.91f,
        animationSpec = if (animateWhenVisible) motion.expressiveSpec() else snap(),
        label = "native_widget_scale",
    )
    val entranceOffsetY by animateDpAsState(
        targetValue = if (isVisible || !expressiveMotionEnabled) 0.dp else 10.dp,
        animationSpec = if (animateWhenVisible) motion.spatialSpec() else snap(),
        label = "native_widget_offset",
    )
    val entranceAlpha by animateFloatAsState(
        targetValue = if (isVisible || !expressiveMotionEnabled) 1f else 0f,
        animationSpec = if (animateWhenVisible) motion.effectSpec(durationMillis = 160) else snap(),
        label = "native_widget_alpha",
    )

    val cornerRadius by animateDpAsState(
        targetValue = when {
            isExpressive && isMoving -> 18.dp
            isExpressive -> shapes.dashRadius
            else -> shapes.largeRadius
        },
        animationSpec = if (animateWhenVisible) motion.expressiveSpec() else snap(),
        label = "native_widget_corner",
    )
    val containerShape = if (isExpressive) RoundedCornerShape(cornerRadius) else shapes.large

    val containerColor = if (isExpressive) {
        lerp(colors.surfaceContainerHigh, colors.accentContainer, 0.28f).copy(alpha = 0.94f)
    } else {
        colors.surface.copy(alpha = 0.88f)
    }
    val borderColor = when {
        isMoving -> colors.accent
        isExpressive -> colors.accent.copy(alpha = 0.36f)
        else -> colors.hairlineBorder
    }
    val borderWidth = if (isMoving) 2.dp else if (isExpressive) 1.2.dp else 1.dp

    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val coordsHolder = remember { NativeWidgetCoordinatesHolder() }
    val currentOnDragStart by rememberUpdatedState(onDragStart)
    val currentOnDragMove by rememberUpdatedState(onDragMove)
    val currentOnDragEnd by rememberUpdatedState(onDragEnd)
    val currentOnDragCancel by rememberUpdatedState(onDragCancel)

    // Formas geométricas reutilizáveis para a textura expressiva de fundo
    val scallopPolygon = remember(isExpressive) {
        if (isExpressive) OverviewPolygonFactory.clockScallopPolygon() else null
    }
    val cloverPolygon = remember(isExpressive) {
        if (isExpressive) OverviewPolygonFactory.iconPolygon(IconShape.CLOVER, 0.7f) else null
    }
    val decoAndroidPath1 = remember(isExpressive) { if (isExpressive) AndroidPath() else null }
    val decoComposePath1 = remember(decoAndroidPath1) { decoAndroidPath1?.asComposePath() }
    val decoAndroidPath2 = remember(isExpressive) { if (isExpressive) AndroidPath() else null }
    val decoComposePath2 = remember(decoAndroidPath2) { decoAndroidPath2?.asComposePath() }
    val decoMatrix = remember(isExpressive) { if (isExpressive) Matrix() else null }

    val horizontalPad = if (compactPadding) 10.dp else if (isExpressive) 13.dp else 12.dp
    val verticalPad = if (compactPadding) 8.dp else if (isExpressive) 11.dp else 10.dp

    Box(
        modifier = modifier
            .fillMaxSize()
            .then(
                if (expressiveMotionEnabled) {
                    val offsetPx = with(density) { entranceOffsetY.toPx() }
                    Modifier.graphicsLayer {
                        scaleX = entranceScale
                        scaleY = entranceScale
                        translationY = offsetPx
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
                        activePointerPos = if (coords != null && coords.isAttached) {
                            coords.localToWindow(localOffset)
                        } else {
                            localOffset
                        }
                        currentOnDragStart(activePointerPos)
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val coords = coordsHolder.coordinates
                        activePointerPos = if (coords != null && coords.isAttached) {
                            coords.localToWindow(change.position)
                        } else {
                            activePointerPos + dragAmount
                        }
                        currentOnDragMove(activePointerPos)
                    },
                    onDragEnd = { currentOnDragEnd() },
                    onDragCancel = { currentOnDragCancel() },
                )
            }
            .then(
                if (isExpressive) {
                    Modifier.shadow(elevation = 10.dp, shape = containerShape, clip = false)
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
            ),
    ) {
        if (isExpressive &&
            scallopPolygon != null &&
            cloverPolygon != null &&
            decoAndroidPath1 != null &&
            decoComposePath1 != null &&
            decoAndroidPath2 != null &&
            decoComposePath2 != null &&
            decoMatrix != null
        ) {
            val accentBlobColor = colors.accentContainer.copy(alpha = 0.42f)
            val secondaryBlobColor = colors.accent.copy(alpha = 0.14f)
            Canvas(modifier = Modifier.fillMaxSize()) {
                val primarySize = (size.minDimension * 0.72f).coerceAtLeast(56.dp.toPx())
                OverviewPolygonFactory.fillScaledAndroidPath(
                    polygon = scallopPolygon,
                    sizePx = primarySize,
                    outPath = decoAndroidPath1,
                    workMatrix = decoMatrix,
                )
                decoAndroidPath1.offset(size.width - primarySize * 0.62f, -primarySize * 0.28f)
                drawPath(path = decoComposePath1, color = accentBlobColor)

                val secondarySize = (size.minDimension * 0.56f).coerceAtLeast(42.dp.toPx())
                OverviewPolygonFactory.fillScaledAndroidPath(
                    polygon = cloverPolygon,
                    sizePx = secondarySize,
                    outPath = decoAndroidPath2,
                    workMatrix = decoMatrix,
                )
                decoAndroidPath2.offset(-secondarySize * 0.34f, size.height - secondarySize * 0.64f)
                drawPath(path = decoComposePath2, color = secondaryBlobColor)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = horizontalPad, vertical = verticalPad),
            verticalArrangement = Arrangement.SpaceBetween,
            content = content,
        )
    }
}

/**
 * Cabeçalho adaptativo dos widgets nativos:
 * discreto no estilo GNOME e em pílula tonal expressiva no Material Expressive.
 */
@Composable
fun NativeWidgetHeader(
    title: String,
    trailing: (@Composable RowScope.() -> Unit)? = null,
) {
    val colors = OverviewTheme.colors
    val shapes = OverviewTheme.shapes
    val typography = OverviewTheme.typography
    val isExpressive = OverviewTheme.components.isExpressive

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        if (isExpressive) {
            Row(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .clip(shapes.pill)
                    .background(colors.accentContainer.copy(alpha = 0.72f))
                    .padding(horizontal = 8.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(colors.accent),
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = title,
                    style = typography.sectionHeader.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                    color = colors.onAccentContainer,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
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
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                content = trailing,
            )
        }
    }
}

/**
 * Botão/chip compacto usado dentro de widgets para ações rápidas,
 * com morphing de forma e mola de toque no estilo Expressive.
 */
@Composable
fun NativeWidgetActionPill(
    label: String,
    onClick: () -> Unit,
    emphasized: Boolean = true,
) {
    val colors = OverviewTheme.colors
    val shapes = OverviewTheme.shapes
    val motion = OverviewTheme.motion
    val isExpressive = OverviewTheme.components.isExpressive
    val expressiveMotion = isExpressive && motion.usesSprings && !motion.reducedMotion

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val corner by animateDpAsState(
        targetValue = if (isExpressive) {
            if (isPressed) 10.dp else 999.dp
        } else {
            shapes.smallRadius
        },
        animationSpec = if (expressiveMotion) motion.expressiveSpec() else snap(),
        label = "actionPillCorner",
    )
    val pressScale by animateFloatAsState(
        targetValue = if (expressiveMotion && isPressed) 0.92f else 1f,
        animationSpec = if (expressiveMotion) motion.expressiveSpec() else snap(),
        label = "actionPillScale",
    )
    val pillShape = if (isExpressive) RoundedCornerShape(corner) else shapes.small

    val targetBg = when {
        emphasized && isExpressive -> colors.accentContainer
        emphasized -> colors.accent.copy(alpha = 0.16f)
        isExpressive -> lerp(colors.surfaceContainerHigh, colors.accentContainer, 0.25f)
        else -> colors.surfaceElevated
    }
    val bg by animateColorAsState(
        targetValue = targetBg,
        animationSpec = if (expressiveMotion) motion.effectSpec(120) else snap(),
        label = "actionPillBg",
    )
    val fg = when {
        emphasized && isExpressive -> colors.onAccentContainer
        emphasized -> colors.accent
        else -> colors.onSurface
    }

    Box(
        modifier = Modifier
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .clip(pillShape)
            .background(bg)
            .then(
                if (!isExpressive) {
                    Modifier.border(1.dp, colors.hairlineBorder, pillShape)
                } else {
                    Modifier
                },
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(horizontal = 9.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = if (isExpressive) FontWeight.Bold else FontWeight.Medium,
            ),
            color = fg,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
