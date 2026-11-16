package io.github.vimitt.overview.ui.home.widgets

import android.graphics.Rect
import android.view.View
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.vimitt.overview.R
import io.github.vimitt.overview.data.MediaSessionSnapshot
import io.github.vimitt.overview.domain.model.AppInfo
import io.github.vimitt.overview.domain.model.QuickNoteItem
import io.github.vimitt.overview.domain.model.QuickTaskItem
import io.github.vimitt.overview.ui.components.AppIconItem
import io.github.vimitt.overview.ui.components.OverviewPolygonFactory
import io.github.vimitt.overview.ui.components.RoundedPolygonShape
import io.github.vimitt.overview.ui.theme.OverviewTheme
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

/**
 * Controle de música com capa do álbum, play/pause e troca de faixa na tela inicial.
 */
@Composable
fun MediaPlayerNativeWidget(
    media: MediaSessionSnapshot,
    spanX: Int,
    spanY: Int,
    isVisible: Boolean,
    onRequestNotificationAccess: () -> Unit,
    onOpenPlayerApp: () -> Unit,
    onPlayPause: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSkipNext: () -> Unit,
) {
    val colors = OverviewTheme.colors
    val shapes = OverviewTheme.shapes
    val motion = OverviewTheme.motion
    val isExpressive = OverviewTheme.components.isExpressive
    val expressiveMotion = isExpressive && motion.usesSprings && !motion.reducedMotion
    val animateWhenVisible = expressiveMotion && isVisible

    val playButtonCorner by animateDpAsState(
        targetValue = if (isExpressive) {
            if (media.isPlaying) 12.dp else 22.dp
        } else {
            8.dp
        },
        animationSpec = if (animateWhenVisible) motion.expressiveSpec() else snap(),
        label = "media_play_morph",
    )
    val artworkScale by animateFloatAsState(
        targetValue = if (animateWhenVisible && media.isPlaying) 1.05f else 1f,
        animationSpec = if (animateWhenVisible) motion.expressiveSpec() else snap(),
        label = "media_art_scale",
    )

    val animateSpin = media.isPlaying && isVisible && !motion.reducedMotion
    val playButtonSpinAngle = if (animateSpin) {
        val spinTransition = rememberInfiniteTransition(label = "mediaPlaySpinTransition")
        val angle by spinTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 5200, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
            label = "mediaPlaySpinAngle",
        )
        angle
    } else {
        0f
    }
    val playScallopShape = androidx.compose.runtime.remember(isExpressive) {
        if (isExpressive) RoundedPolygonShape(OverviewPolygonFactory.clockScallopPolygon()) else null
    }

    if (!media.hasNotificationAccess) {
        Column(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            NativeWidgetHeader(title = stringResource(R.string.native_widget_media_title))
            Box(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = stringResource(R.string.native_widget_media_access_title),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceMuted,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f).padding(end = 8.dp),
                    )
                    NativeWidgetActionPill(
                        label = stringResource(R.string.native_widget_media_access_cta),
                        onClick = onRequestNotificationAccess,
                    )
                }
            }
        }
        return
    }

    val hasSession = media.controller != null
    val artSize = if (spanY <= 1) 40.dp else 56.dp
    val artShape = if (isExpressive) shapes.large else shapes.medium

    Column(
        modifier = Modifier.fillMaxWidth().fillMaxHeight(),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        if (spanY >= 2) {
            NativeWidgetHeader(
                title = stringResource(R.string.native_widget_media_title),
                trailing = {
                    if (isExpressive && media.isPlaying && isVisible) {
                        ExpressiveEqualizerBars(
                            color = colors.accent,
                            animate = animateWhenVisible,
                        )
                    }
                },
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
            Box(
                modifier = Modifier
                    .size(artSize)
                    .graphicsLayer {
                        scaleX = artworkScale
                        scaleY = artworkScale
                    }
                    .clip(artShape)
                    .background(
                        if (isExpressive) colors.accentContainer else colors.surfaceElevated,
                    )
                    .then(
                        if (!isExpressive) Modifier.border(1.dp, colors.hairlineBorder, artShape) else Modifier,
                    )
                    .clickable(enabled = hasSession, role = Role.Button, onClick = onOpenPlayerApp),
                contentAlignment = Alignment.Center,
            ) {
                val art = media.artwork
                if (art != null) {
                    Image(
                        bitmap = art,
                        contentDescription = media.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    PlayPauseGlyph(
                        isPlaying = media.isPlaying,
                        tint = if (isExpressive) colors.onAccentContainer else colors.onSurfaceMuted,
                        size = 18.dp,
                    )
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable(enabled = hasSession, role = Role.Button, onClick = onOpenPlayerApp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = if (hasSession) media.title else stringResource(R.string.native_widget_media_idle),
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = if (isExpressive) FontWeight.Bold else FontWeight.SemiBold,
                        ),
                        color = colors.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (isExpressive && spanY <= 1 && media.isPlaying && isVisible) {
                        ExpressiveEqualizerBars(
                            color = colors.accent,
                            animate = animateWhenVisible,
                        )
                    }
                }
                if (media.artist.isNotBlank()) {
                    Text(
                        text = media.artist,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                if (spanX >= 3) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .clickable(enabled = hasSession, role = Role.Button, onClick = onSkipPrevious),
                        contentAlignment = Alignment.Center,
                    ) {
                        SkipTrackGlyph(
                            forward = false,
                            tint = if (hasSession) colors.onSurface else colors.onSurfaceMuted.copy(alpha = 0.45f),
                            size = 15.dp,
                        )
                    }
                }

                val playButtonSize = if (spanY <= 1) 38.dp else 44.dp
                val playContainerShape = if (isExpressive && media.isPlaying && playScallopShape != null) {
                    playScallopShape
                } else {
                    RoundedCornerShape(playButtonCorner)
                }
                val playContainerBg = when {
                    !hasSession -> colors.surfaceContainerHigh.copy(alpha = 0.55f)
                    isExpressive && media.isPlaying -> colors.accent
                    isExpressive -> colors.accentContainer
                    else -> colors.surfaceElevated
                }
                val playGlyphTint = when {
                    !hasSession -> colors.onSurfaceMuted
                    isExpressive && media.isPlaying -> colors.onAccent
                    isExpressive -> colors.onAccentContainer
                    else -> colors.accent
                }

                Box(
                    modifier = Modifier
                        .size(playButtonSize)
                        .clickable(enabled = hasSession, role = Role.Button, onClick = onPlayPause),
                    contentAlignment = Alignment.Center,
                ) {
                    // Fundo ou anel giratório durante reprodução sem rotacionar o ícone central
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                rotationZ = playButtonSpinAngle
                            }
                            .clip(playContainerShape)
                            .background(playContainerBg)
                            .then(
                                if (!isExpressive) {
                                    Modifier.border(
                                        width = 1.dp,
                                        color = if (media.isPlaying && hasSession) {
                                            colors.accent.copy(alpha = 0.65f)
                                        } else {
                                            colors.hairlineBorder
                                        },
                                        shape = playContainerShape,
                                    )
                                } else {
                                    Modifier
                                },
                            ),
                    )
                    if (animateSpin && !isExpressive) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawArc(
                                color = colors.accent,
                                startAngle = playButtonSpinAngle - 90f,
                                sweepAngle = 150f,
                                useCenter = false,
                                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round),
                            )
                        }
                    }
                    PlayPauseGlyph(
                        isPlaying = media.isPlaying,
                        tint = playGlyphTint,
                        size = 17.dp,
                    )
                }

                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .clickable(enabled = hasSession, role = Role.Button, onClick = onSkipNext),
                    contentAlignment = Alignment.Center,
                ) {
                    SkipTrackGlyph(
                        forward = true,
                        tint = if (hasSession) colors.onSurface else colors.onSurfaceMuted.copy(alpha = 0.45f),
                        size = 15.dp,
                    )
                }
            }
        }
        }
    }
}

@Composable
private fun ExpressiveEqualizerBars(
    color: Color,
    animate: Boolean,
) {
    val phase = if (animate) {
        val transition = rememberInfiniteTransition(label = "mediaEqTransition")
        val value by transition.animateFloat(
            initialValue = 0f,
            targetValue = (2f * PI).toFloat(),
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1100, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
            label = "mediaEqPhase",
        )
        value
    } else {
        0.8f
    }

    Canvas(modifier = Modifier.size(width = 14.dp, height = 12.dp)) {
        val barWidth = 3.dp.toPx()
        val gap = (size.width - barWidth * 3f) / 2f
        val radius = CornerRadius(barWidth / 2f, barWidth / 2f)
        for (i in 0..2) {
            val factor = 0.35f + 0.65f * abs(sin(phase + i * 1.15f))
            val barH = size.height * factor
            drawRoundRect(
                color = color,
                topLeft = Offset(x = i * (barWidth + gap), y = size.height - barH),
                size = Size(width = barWidth, height = barH),
                cornerRadius = radius,
            )
        }
    }
}

/**
 * Notas rápidas para visualizar e editar a última nota direto na tela inicial.
 */
@Composable
fun QuickNotesNativeWidget(
    notes: List<QuickNoteItem>,
    spanX: Int,
    spanY: Int,
    onCreateNote: () -> Unit,
    onEditNote: (QuickNoteItem) -> Unit,
) {
    val colors = OverviewTheme.colors
    val shapes = OverviewTheme.shapes
    val isExpressive = OverviewTheme.components.isExpressive
    val latest = notes.firstOrNull()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .clickable(role = Role.Button) {
                if (latest != null) onEditNote(latest) else onCreateNote()
            },
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        NativeWidgetHeader(
            title = stringResource(R.string.native_widget_notes_title),
            trailing = {
                if (notes.size > 1 && spanX >= 2) {
                    Text(
                        text = stringResource(R.string.native_widget_notes_count, notes.size),
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.onSurfaceMuted,
                    )
                }
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(if (isExpressive) CircleShape else shapes.small)
                        .background(if (isExpressive) colors.accent else colors.surfaceElevated)
                        .then(
                            if (!isExpressive) {
                                Modifier.border(1.dp, colors.hairlineBorder, shapes.small)
                            } else {
                                Modifier
                            },
                        )
                        .clickable(role = Role.Button, onClick = onCreateNote),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = stringResource(R.string.native_widget_notes_save),
                        tint = if (isExpressive) colors.onAccent else colors.accent,
                        modifier = Modifier.size(15.dp),
                    )
                }
            },
        )

        if (latest == null) {
            Box(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.CenterStart,
            ) {
                Text(
                    text = stringResource(R.string.native_widget_notes_empty),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceMuted,
                    maxLines = if (spanY <= 1) 1 else 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        } else {
            val noteBoxModifier = if (isExpressive) {
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(top = 5.dp)
                    .clip(shapes.medium)
                    .background(lerp(colors.surfaceContainerHigh, colors.accentContainer, 0.30f))
                    .padding(horizontal = 9.dp, vertical = 6.dp)
            } else {
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(top = 5.dp)
            }
            Row(
                modifier = noteBoxModifier,
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                verticalAlignment = Alignment.Top,
            ) {
                if (isExpressive) {
                    Box(
                        modifier = Modifier
                            .width(3.dp)
                            .fillMaxHeight()
                            .clip(CircleShape)
                            .background(colors.accent),
                    )
                }
                Text(
                    text = latest.content,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = if (isExpressive) FontWeight.Medium else FontWeight.Normal,
                    ),
                    color = colors.onSurface,
                    maxLines = if (spanY <= 1) 1 else if (spanY == 2) 4 else 8,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/**
 * Lista curta de tarefas interativa com marcação de concluída direto na Home.
 */
@Composable
fun TasksNativeWidget(
    tasks: List<QuickTaskItem>,
    spanX: Int,
    spanY: Int,
    isVisible: Boolean,
    onToggleTask: (String) -> Unit,
    onAddTask: () -> Unit,
    onClearCompleted: () -> Unit,
) {
    val colors = OverviewTheme.colors
    val shapes = OverviewTheme.shapes
    val motion = OverviewTheme.motion
    val isExpressive = OverviewTheme.components.isExpressive
    val expressiveMotion = isExpressive && motion.usesSprings && !motion.reducedMotion
    val doneCount = tasks.count { it.completed }

    Column(
        modifier = Modifier.fillMaxWidth().fillMaxHeight(),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        NativeWidgetHeader(
            title = stringResource(R.string.native_widget_tasks_title),
            trailing = {
                if (doneCount > 0 && spanX >= 2) {
                    NativeWidgetActionPill(
                        label = stringResource(R.string.native_widget_tasks_clear_done),
                        emphasized = false,
                        onClick = onClearCompleted,
                    )
                } else if (isExpressive && tasks.isNotEmpty() && spanX >= 2) {
                    Box(
                        modifier = Modifier
                            .clip(shapes.pill)
                            .background(colors.surfaceContainerHigh.copy(alpha = 0.8f))
                            .padding(horizontal = 7.dp, vertical = 2.dp),
                    ) {
                        Text(
                            text = "$doneCount/${tasks.size}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontFeatureSettings = "tnum",
                            ),
                            color = colors.accent,
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(if (isExpressive) CircleShape else shapes.small)
                        .background(if (isExpressive) colors.accent else colors.surfaceElevated)
                        .then(
                            if (!isExpressive) {
                                Modifier.border(1.dp, colors.hairlineBorder, shapes.small)
                            } else {
                                Modifier
                            },
                        )
                        .clickable(role = Role.Button, onClick = onAddTask),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = stringResource(R.string.native_widget_tasks_add),
                        tint = if (isExpressive) colors.onAccent else colors.accent,
                        modifier = Modifier.size(15.dp),
                    )
                }
            },
        )

        if (tasks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clickable(role = Role.Button, onClick = onAddTask),
                contentAlignment = Alignment.CenterStart,
            ) {
                Text(
                    text = stringResource(R.string.native_widget_tasks_empty),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceMuted,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        } else {
            val visibleLimit = when {
                spanY <= 1 -> 1
                spanY == 2 -> 3
                else -> 5
            }
            val visibleTasks = tasks.take(visibleLimit)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(if (isExpressive) 5.dp else 3.dp),
            ) {
                for ((index, task) in visibleTasks.withIndex()) {
                    val checkCorner by animateDpAsState(
                        targetValue = if (isExpressive) {
                            if (task.completed) 10.dp else 5.dp
                        } else {
                            4.dp
                        },
                        animationSpec = if (expressiveMotion && isVisible) motion.expressiveSpec() else snap(),
                        label = "taskCheckCorner",
                    )
                    val checkScale by animateFloatAsState(
                        targetValue = if (expressiveMotion && isVisible && task.completed) 1.08f else 1f,
                        animationSpec = if (expressiveMotion && isVisible) motion.expressiveSpec() else snap(),
                        label = "taskCheckScale",
                    )
                    val rowBg by animateColorAsState(
                        targetValue = when {
                            !isExpressive -> Color.Transparent
                            task.completed -> colors.surfaceContainer.copy(alpha = 0.45f)
                            else -> lerp(colors.surfaceContainerHigh, colors.accentContainer, 0.24f)
                        },
                        animationSpec = if (expressiveMotion && isVisible) motion.effectSpec(140) else snap(),
                        label = "taskRowBg",
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(if (isExpressive) shapes.medium else shapes.small)
                            .then(
                                if (isExpressive) {
                                    Modifier
                                        .background(rowBg)
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                } else {
                                    Modifier.padding(vertical = 2.dp)
                                },
                            )
                            .clickable(role = Role.Checkbox) { onToggleTask(task.id) },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(7.dp),
                    ) {
                        val checkShape = RoundedCornerShape(checkCorner)
                        Box(
                            modifier = Modifier
                                .size(17.dp)
                                .graphicsLayer {
                                    scaleX = checkScale
                                    scaleY = checkScale
                                }
                                .clip(checkShape)
                                .background(
                                    if (task.completed) colors.accent else colors.surface.copy(alpha = 0f),
                                )
                                .border(
                                    width = 1.5.dp,
                                    color = if (task.completed) colors.accent else colors.onSurfaceMuted,
                                    shape = checkShape,
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (task.completed) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = colors.onAccent,
                                    modifier = Modifier.size(11.dp),
                                )
                            }
                        }

                        Text(
                            text = task.title,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = if (isExpressive && !task.completed) FontWeight.SemiBold else FontWeight.Normal,
                                textDecoration = if (task.completed) TextDecoration.LineThrough else null,
                            ),
                            color = if (task.completed) colors.onSurfaceMuted else colors.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (!isExpressive && index < visibleTasks.lastIndex) {
                        HorizontalDivider(thickness = 1.dp, color = colors.hairlineBorder)
                    }
                }
            }
        }
    }
}

/**
 * Apps frequentes com base nos aplicativos mais abertos pelo usuário.
 */
@Composable
fun FrequentAppsNativeWidget(
    apps: List<AppInfo>,
    spanX: Int,
    spanY: Int,
    showNotificationDots: Boolean,
    badgePackages: Set<String>,
    onAppClick: (app: AppInfo, sourceView: View, sourceBounds: Rect?) -> Unit,
) {
    val colors = OverviewTheme.colors
    val shapes = OverviewTheme.shapes
    val isExpressive = OverviewTheme.components.isExpressive
    val columns = spanX.coerceIn(1, 5)
    val maxApps = if (spanY <= 1) columns else columns * 2
    val visibleApps = apps.take(maxApps)

    Column(
        modifier = Modifier.fillMaxWidth().fillMaxHeight(),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        if (spanY >= 2) {
            NativeWidgetHeader(title = stringResource(R.string.native_widget_frequent_apps_title))
        }

        if (visibleApps.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.native_widget_frequent_apps_empty),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceMuted,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        } else {
            val rows = visibleApps.chunked(columns)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    for (rowApps in rows) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(
                                    if (isExpressive) {
                                        Modifier
                                            .clip(shapes.large)
                                            .background(lerp(colors.surfaceContainer, colors.accentContainer, 0.25f))
                                            .border(1.dp, colors.accent.copy(alpha = 0.20f), shapes.large)
                                            .padding(horizontal = 4.dp, vertical = 4.dp)
                                    } else {
                                        Modifier
                                    },
                                ),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            for (app in rowApps) {
                                AppIconItem(
                                    app = app,
                                    onClick = onAppClick,
                                    iconSize = if (spanY <= 1) 40.dp else 44.dp,
                                    showLabel = spanY >= 2,
                                    showNotificationDot = showNotificationDots && app.packageName in badgePackages,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
