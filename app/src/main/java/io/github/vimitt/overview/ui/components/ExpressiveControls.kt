package io.github.vimitt.overview.ui.components

import android.graphics.Matrix
import android.graphics.Path as AndroidPath
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.toPath
import io.github.vimitt.overview.ui.theme.OverviewTheme
import kotlin.math.PI
import kotlin.math.sin

/**
 * Indicador de carregamento expressivo com *shape morphing* contínuo usando `graphics-shapes`.
 *
 * Reutiliza instâncias de [Morph], [AndroidPath] e [Matrix] entre frames para evitar alocações em 60/120 Hz.
 * Quando animações reduzidas estão ativas no sistema, exibe um [CircularProgressIndicator] simples.
 */
@Composable
fun ExpressiveLoadingIndicator(
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    indicatorColor: Color = OverviewTheme.colors.accent,
    containerColor: Color = OverviewTheme.colors.accentContainer,
) {
    val reducedMotion = OverviewTheme.reducedMotion
    if (reducedMotion) {
        Box(
            modifier = modifier.size(size),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(size * 0.72f),
                color = indicatorColor,
                strokeWidth = 3.dp,
            )
        }
        return
    }

    val morphs = remember {
        val polygons = OverviewPolygonFactory.loadingPolygons()
        List(polygons.size) { index ->
            val nextIndex = (index + 1) % polygons.size
            Morph(polygons[index], polygons[nextIndex])
        }
    }
    val workAndroidPath = remember { AndroidPath() }
    val workComposePath = remember(workAndroidPath) { workAndroidPath.asComposePath() }
    val workMatrix = remember { Matrix() }

    val transition = rememberInfiniteTransition(label = "expressiveLoadingTransition")
    val morphCycle by transition.animateFloat(
        initialValue = 0f,
        targetValue = morphs.size.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = morphs.size * 700, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "expressiveLoadingMorphCycle",
    )
    val rotationDeg by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "expressiveLoadingRotation",
    )

    Box(
        modifier = modifier
            .size(size)
            .clip(OverviewTheme.shapes.pill)
            .background(containerColor),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(
            modifier = Modifier
                .size(size * 0.64f)
                .graphicsLayer { rotationZ = rotationDeg },
        ) {
            val idx = morphCycle.toInt().coerceIn(0, morphs.lastIndex)
            val localProgress = (morphCycle - idx).coerceIn(0f, 1f)

            workAndroidPath.rewind()
            morphs[idx].toPath(localProgress, workAndroidPath)
            workMatrix.reset()
            workMatrix.setScale(this.size.width, this.size.height)
            workAndroidPath.transform(workMatrix)

            drawPath(
                path = workComposePath,
                color = indicatorColor,
            )
        }
    }
}

/**
 * Barra de progresso ondulada inspirada no Material 3 Expressive (`WavyProgressIndicator`).
 *
 * Útil para feedback visual em pré-visualizações e controles sem depender de APIs instáveis.
 */
@Composable
fun ExpressiveWavyProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = OverviewTheme.colors.accent,
    trackColor: Color = OverviewTheme.colors.surfaceElevated,
    animateWave: Boolean = true,
) {
    val reducedMotion = OverviewTheme.reducedMotion
    val clampedProgress = progress.coerceIn(0f, 1f)
    val wavePath = remember { Path() }

    val phase = if (reducedMotion || !animateWave) {
        0f
    } else {
        val transition = rememberInfiniteTransition(label = "wavyProgressTransition")
        val animatedPhase by transition.animateFloat(
            initialValue = 0f,
            targetValue = (2f * PI).toFloat(),
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1200, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
            label = "wavyProgressPhase",
        )
        animatedPhase
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(10.dp),
    ) {
        val centerY = size.height / 2f
        val strokePx = 3.dp.toPx()
        val amplitudePx = if (reducedMotion || !animateWave) 0f else 2.4.dp.toPx()
        val activeEndX = size.width * clampedProgress

        // Trilha inativa
        drawLine(
            color = trackColor,
            start = androidx.compose.ui.geometry.Offset(activeEndX, centerY),
            end = androidx.compose.ui.geometry.Offset(size.width, centerY),
            strokeWidth = strokePx,
            cap = StrokeCap.Round,
        )

        if (activeEndX <= 1f) return@Canvas

        if (amplitudePx <= 0.1f) {
            drawLine(
                color = color,
                start = androidx.compose.ui.geometry.Offset(0f, centerY),
                end = androidx.compose.ui.geometry.Offset(activeEndX, centerY),
                strokeWidth = strokePx,
                cap = StrokeCap.Round,
            )
        } else {
            wavePath.reset()
            val wavelengthPx = 22.dp.toPx()
            val stepPx = 3f
            var x = 0f
            wavePath.moveTo(0f, centerY + sin(phase) * amplitudePx)
            while (x < activeEndX) {
                x = (x + stepPx).coerceAtMost(activeEndX)
                val angle = (x / wavelengthPx) * (2f * PI.toFloat()) + phase
                wavePath.lineTo(x, centerY + sin(angle) * amplitudePx)
            }
            drawPath(
                path = wavePath,
                color = color,
                style = Stroke(width = strokePx, cap = StrokeCap.Round),
            )
        }
    }
}

/**
 * Grupo de botões conectados com transição orgânica de cantos e cores para o estilo Expressive.
 */
@Composable
fun <T> ExpressiveButtonGroup(
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    showCheckOnSelected: Boolean = true,
) {
    val colors = OverviewTheme.colors
    val motion = OverviewTheme.motion

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        options.forEachIndexed { index, (optionValue, label) ->
            val isSelected = optionValue == selected
            val interactionSource = remember { MutableInteractionSource() }
            val isPressed by interactionSource.collectIsPressedAsState()

            val outerCorner by animateDpAsState(
                targetValue = when {
                    isSelected -> 24.dp
                    isPressed -> 10.dp
                    index == 0 || index == options.lastIndex -> 16.dp
                    else -> 12.dp
                },
                animationSpec = motion.expressiveSpec(durationMillis = 180),
                label = "buttonGroupCorner",
            )
            val containerColor by animateColorAsState(
                targetValue = if (isSelected) colors.accent else colors.surfaceElevated,
                animationSpec = motion.effectSpec(durationMillis = 150),
                label = "buttonGroupBg",
            )
            val contentColor by animateColorAsState(
                targetValue = if (isSelected) colors.onAccent else colors.onSurface,
                animationSpec = motion.effectSpec(durationMillis = 150),
                label = "buttonGroupFg",
            )
            val pressScale by animateFloatAsState(
                targetValue = if (isPressed) 0.96f else 1f,
                animationSpec = motion.expressiveSpec(durationMillis = 140),
                label = "buttonGroupScale",
            )

            val shape = RoundedCornerShape(outerCorner)

            Row(
                modifier = Modifier
                    .weight(if (isSelected) 1.15f else 1f)
                    .defaultMinSize(minHeight = 48.dp)
                    .graphicsLayer {
                        scaleX = pressScale
                        scaleY = pressScale
                    }
                    .clip(shape)
                    .background(containerColor)
                    .then(
                        if (!isSelected) {
                            Modifier.border(1.dp, colors.hairlineBorder, shape)
                        } else {
                            Modifier
                        },
                    )
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        role = Role.RadioButton,
                        onClick = { onSelect(optionValue) },
                    )
                    .padding(horizontal = 10.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (isSelected && showCheckOnSelected) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                    ),
                    color = contentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/**
 * Botão dividido expressivo (ação principal + ação secundária/menu conectada).
 */
@Composable
fun ExpressiveSplitButton(
    primaryLabel: String,
    onPrimaryClick: () -> Unit,
    onSecondaryClick: () -> Unit,
    secondaryContentDescription: String,
    modifier: Modifier = Modifier,
    primaryIcon: ImageVector? = null,
    secondaryIcon: ImageVector = Icons.Filled.KeyboardArrowDown,
) {
    val colors = OverviewTheme.colors
    val motion = OverviewTheme.motion

    val leadingInteraction = remember { MutableInteractionSource() }
    val trailingInteraction = remember { MutableInteractionSource() }
    val leadingPressed by leadingInteraction.collectIsPressedAsState()
    val trailingPressed by trailingInteraction.collectIsPressedAsState()

    val leadingInnerCorner by animateDpAsState(
        targetValue = if (leadingPressed) 14.dp else 6.dp,
        animationSpec = motion.expressiveSpec(durationMillis = 140),
        label = "splitLeadingCorner",
    )
    val trailingInnerCorner by animateDpAsState(
        targetValue = if (trailingPressed) 14.dp else 6.dp,
        animationSpec = motion.expressiveSpec(durationMillis = 140),
        label = "splitTrailingCorner",
    )

    val leadingShape = RoundedCornerShape(
        topStart = 24.dp,
        bottomStart = 24.dp,
        topEnd = leadingInnerCorner,
        bottomEnd = leadingInnerCorner,
    )
    val trailingShape = RoundedCornerShape(
        topStart = trailingInnerCorner,
        bottomStart = trailingInnerCorner,
        topEnd = 24.dp,
        bottomEnd = 24.dp,
    )

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .defaultMinSize(minHeight = 48.dp)
                .clip(leadingShape)
                .background(colors.accentContainer)
                .clickable(
                    interactionSource = leadingInteraction,
                    indication = null,
                    role = Role.Button,
                    onClick = onPrimaryClick,
                )
                .padding(PaddingValues(horizontal = 16.dp, vertical = 10.dp)),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            if (primaryIcon != null) {
                Icon(
                    imageVector = primaryIcon,
                    contentDescription = null,
                    tint = colors.onAccentContainer,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = primaryLabel,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                color = colors.onAccentContainer,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Box(
            modifier = Modifier
                .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                .clip(trailingShape)
                .background(colors.accentContainer)
                .clickable(
                    interactionSource = trailingInteraction,
                    indication = null,
                    role = Role.Button,
                    onClick = onSecondaryClick,
                )
                .semantics { contentDescription = secondaryContentDescription }
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = secondaryIcon,
                contentDescription = null,
                tint = colors.onAccentContainer,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

private val POPUP_DESTRUCTIVE_COLOR = Color(0xFFE62D42)

/**
 * Contêiner compartilhado para pop-ups e planilhas contextuais do launcher.
 * No modo GNOME preserva superfície plana e entrada discreta; no modo Expressive
 * usa superfície tonal elevada, borda tonal, alça superior em planilhas inferiores
 * e entrada com física de mola.
 */
@Composable
fun OverviewPopupSurface(
    onDismiss: () -> Unit,
    bottomSheet: Boolean = true,
    enableImePadding: Boolean = false,
    scrimMaxAlpha: Float = if (bottomSheet) 0.36f else 0.42f,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = OverviewTheme.colors
    val shapes = OverviewTheme.shapes
    val motion = OverviewTheme.motion
    val isExpressive = OverviewTheme.components.isExpressive
    val cardShape = shapes.large
    val backdropInteraction = remember { MutableInteractionSource() }
    val cardInteraction = remember { MutableInteractionSource() }

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    val entryProgress by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = if (isExpressive) {
            motion.expressiveSpec(durationMillis = if (bottomSheet) 220 else 200)
        } else {
            motion.spatialSpec(durationMillis = if (bottomSheet) 160 else 180)
        },
        label = "overviewPopupEntry",
    )
    val clampedAlpha = entryProgress.coerceIn(0f, 1f)

    val containerColor = if (isExpressive) {
        lerp(colors.surfaceContainerHigh, colors.accentContainer, 0.14f)
    } else {
        colors.surface
    }
    val borderColor = if (isExpressive) {
        colors.accent.copy(alpha = 0.28f)
    } else {
        colors.hairlineBorder
    }

    BackHandler(onBack = onDismiss)

    val outerModifier = Modifier
        .fillMaxSize()
        .background(Color.Black.copy(alpha = scrimMaxAlpha * clampedAlpha))
        .clickable(
            interactionSource = backdropInteraction,
            indication = null,
            onClick = onDismiss,
        )
        .let { base -> if (!bottomSheet) base.statusBarsPadding() else base }
        .let { base -> if (enableImePadding) base.imePadding() else base }
        .navigationBarsPadding()
        .padding(
            horizontal = 20.dp,
            vertical = if (bottomSheet) 24.dp else 28.dp,
        )

    Box(
        modifier = outerModifier,
        contentAlignment = if (bottomSheet) Alignment.BottomCenter else Alignment.Center,
    ) {
        Column(
            modifier = modifier
                .widthIn(max = 420.dp)
                .fillMaxWidth()
                .graphicsLayer {
                    if (isExpressive) {
                        if (bottomSheet) {
                            val scale = 0.88f + 0.12f * entryProgress
                            scaleX = scale
                            scaleY = scale
                            transformOrigin = TransformOrigin(0.5f, 1f)
                            translationY = (1f - entryProgress) * 56f
                        } else {
                            val scale = 0.82f + 0.18f * entryProgress
                            scaleX = scale
                            scaleY = scale
                            transformOrigin = TransformOrigin.Center
                            translationY = (1f - entryProgress) * 24f
                        }
                    } else {
                        if (bottomSheet) {
                            val scale = 0.94f + 0.06f * entryProgress
                            scaleX = scale
                            scaleY = scale
                            translationY = (1f - entryProgress) * 28f
                        } else {
                            val scale = 0.90f + 0.10f * entryProgress
                            scaleX = scale
                            scaleY = scale
                        }
                    }
                    alpha = clampedAlpha
                }
                .then(
                    if (isExpressive) {
                        Modifier.shadow(elevation = 16.dp, shape = cardShape, clip = false)
                    } else {
                        Modifier
                    },
                )
                .clip(cardShape)
                .background(containerColor)
                .border(width = 1.dp, color = borderColor, shape = cardShape)
                .clickable(
                    interactionSource = cardInteraction,
                    indication = null,
                    onClick = {},
                ),
        ) {
            if (isExpressive && bottomSheet) {
                // Alça discreta para reforçar a hierarquia de planilha Material 3 Expressive
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp, bottom = 2.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 36.dp, height = 4.dp)
                            .clip(CircleShape)
                            .background(colors.onSurfaceMuted.copy(alpha = 0.35f)),
                    )
                }
            }
            content()
        }
    }
}

/**
 * Divisor adaptativo entre linhas de pop-up nos dois estilos visuais.
 */
@Composable
fun OverviewPopupDivider() {
    val colors = OverviewTheme.colors
    val isExpressive = OverviewTheme.components.isExpressive
    HorizontalDivider(
        thickness = 1.dp,
        color = if (isExpressive) {
            colors.hairlineBorder.copy(alpha = 0.55f)
        } else {
            colors.hairlineBorder
        },
        modifier = if (isExpressive) Modifier.padding(horizontal = 16.dp) else Modifier,
    )
}

/**
 * Linha de ação para pop-ups e menus contextuais, com emblema tonal no estilo Expressive.
 */
@Composable
fun OverviewPopupActionRow(
    label: String,
    onClick: () -> Unit,
    icon: ImageVector? = null,
    painter: Painter? = null,
    tint: Color = OverviewTheme.colors.accent,
    destructive: Boolean = false,
) {
    val colors = OverviewTheme.colors
    val isExpressive = OverviewTheme.components.isExpressive
    val effectiveTint = if (destructive) POPUP_DESTRUCTIVE_COLOR else tint
    val textColor = if (destructive) POPUP_DESTRUCTIVE_COLOR else colors.onSurface
    val labelStyle = if (isExpressive) {
        OverviewTheme.typography.rowTitle
    } else {
        MaterialTheme.typography.titleMedium
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(
                horizontal = 20.dp,
                vertical = if (isExpressive) 12.dp else 16.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null || painter != null) {
            if (isExpressive) {
                val badgeBg = if (destructive) {
                    POPUP_DESTRUCTIVE_COLOR.copy(alpha = 0.16f)
                } else if (effectiveTint == colors.accent) {
                    colors.accentContainer
                } else {
                    lerp(colors.surfaceContainerHigh, effectiveTint, 0.18f)
                }
                val badgeTint = if (effectiveTint == colors.accent && !destructive) {
                    colors.onAccentContainer
                } else {
                    effectiveTint
                }
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(OverviewTheme.shapes.small)
                        .background(badgeBg),
                    contentAlignment = Alignment.Center,
                ) {
                    if (icon != null) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = badgeTint,
                            modifier = Modifier.size(20.dp),
                        )
                    } else if (painter != null) {
                        Icon(
                            painter = painter,
                            contentDescription = null,
                            tint = badgeTint,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            } else {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = effectiveTint,
                        modifier = Modifier.size(22.dp),
                    )
                } else if (painter != null) {
                    Icon(
                        painter = painter,
                        contentDescription = null,
                        tint = effectiveTint,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
            Spacer(modifier = Modifier.width(14.dp))
        }
        Text(
            text = label,
            style = labelStyle,
            color = textColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
