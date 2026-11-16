package io.github.vimitt.overview.ui.home.widgets

import android.graphics.Rect
import android.view.View
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.snap
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import io.github.vimitt.overview.ui.theme.OverviewTheme

/**
 * Widget 5: Controle de música com capa do álbum, play/pause e troca de faixa na tela inicial.
 */
@Composable
fun MediaPlayerNativeWidget(
    media: MediaSessionSnapshot,
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
    val animateWhenVisible = isExpressive && isVisible && motion.usesSprings && !motion.reducedMotion

    val playButtonCorner by animateDpAsState(
        targetValue = if (isExpressive) {
            if (media.isPlaying) 12.dp else 22.dp
        } else {
            8.dp
        },
        animationSpec = if (animateWhenVisible) motion.expressiveSpec() else snap(),
        label = "media_play_morph",
    )

    if (!media.hasNotificationAccess) {
        Column(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            NativeWidgetHeader(title = stringResource(R.string.native_widget_media_title))
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
        return
    }

    val hasSession = media.controller != null
    val artSize = if (spanY <= 1) 42.dp else 64.dp
    val artShape = if (isExpressive) shapes.large else shapes.medium

    Column(
        modifier = Modifier.fillMaxWidth().fillMaxHeight(),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        if (spanY >= 2) {
            NativeWidgetHeader(
                title = stringResource(R.string.native_widget_media_title),
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(artSize)
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
                        size = 20.dp,
                    )
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable(enabled = hasSession, role = Role.Button, onClick = onOpenPlayerApp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = if (hasSession) media.title else stringResource(R.string.native_widget_media_idle),
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = if (isExpressive) FontWeight.Bold else FontWeight.SemiBold,
                    ),
                    color = colors.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
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
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .clickable(enabled = hasSession, role = Role.Button, onClick = onSkipPrevious),
                    contentAlignment = Alignment.Center,
                ) {
                    SkipTrackGlyph(
                        forward = false,
                        tint = if (hasSession) colors.onSurface else colors.onSurfaceMuted.copy(alpha = 0.45f),
                        size = 16.dp,
                    )
                }

                Box(
                    modifier = Modifier
                        .size(if (spanY <= 1) 38.dp else 44.dp)
                        .clip(RoundedCornerShape(playButtonCorner))
                        .background(
                            when {
                                !hasSession -> colors.surfaceContainerHigh.copy(alpha = 0.55f)
                                isExpressive && media.isPlaying -> colors.accent
                                isExpressive -> colors.accentContainer
                                else -> colors.surfaceElevated
                            },
                        )
                        .then(
                            if (!isExpressive) {
                                Modifier.border(1.dp, colors.hairlineBorder, RoundedCornerShape(playButtonCorner))
                            } else {
                                Modifier
                            },
                        )
                        .clickable(enabled = hasSession, role = Role.Button, onClick = onPlayPause),
                    contentAlignment = Alignment.Center,
                ) {
                    PlayPauseGlyph(
                        isPlaying = media.isPlaying,
                        tint = when {
                            !hasSession -> colors.onSurfaceMuted
                            isExpressive && media.isPlaying -> colors.onAccent
                            isExpressive -> colors.onAccentContainer
                            else -> colors.accent
                        },
                        size = 18.dp,
                    )
                }

                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .clickable(enabled = hasSession, role = Role.Button, onClick = onSkipNext),
                    contentAlignment = Alignment.Center,
                ) {
                    SkipTrackGlyph(
                        forward = true,
                        tint = if (hasSession) colors.onSurface else colors.onSurfaceMuted.copy(alpha = 0.45f),
                        size = 16.dp,
                    )
                }
            }
        }
    }
}

/**
 * Widget 7: Notas rápidas para visualizar e editar a última nota direto na tela inicial.
 */
@Composable
fun QuickNotesNativeWidget(
    notes: List<QuickNoteItem>,
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
                if (notes.size > 1) {
                    Text(
                        text = stringResource(R.string.native_widget_notes_count, notes.size),
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.onSurfaceMuted,
                    )
                }
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(if (isExpressive) CircleShape else shapes.small)
                        .background(if (isExpressive) colors.accentContainer else colors.surfaceElevated)
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
                        tint = if (isExpressive) colors.onAccentContainer else colors.accent,
                        modifier = Modifier.size(16.dp),
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
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        } else {
            val noteBoxModifier = if (isExpressive) {
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(top = 6.dp)
                    .clip(shapes.medium)
                    .background(colors.surfaceContainerHigh.copy(alpha = 0.62f))
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            } else {
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(top = 6.dp)
            }
            Box(modifier = noteBoxModifier, contentAlignment = Alignment.TopStart) {
                Text(
                    text = latest.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurface,
                    maxLines = if (spanY <= 1) 1 else if (spanY == 2) 4 else 8,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/**
 * Widget 8: Lista curta de tarefas interativa com marcação de concluída direto na Home.
 */
@Composable
fun TasksNativeWidget(
    tasks: List<QuickTaskItem>,
    spanY: Int,
    onToggleTask: (String) -> Unit,
    onAddTask: () -> Unit,
    onClearCompleted: () -> Unit,
) {
    val colors = OverviewTheme.colors
    val shapes = OverviewTheme.shapes
    val isExpressive = OverviewTheme.components.isExpressive
    val doneCount = tasks.count { it.completed }

    Column(
        modifier = Modifier.fillMaxWidth().fillMaxHeight(),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        NativeWidgetHeader(
            title = stringResource(R.string.native_widget_tasks_title),
            trailing = {
                if (doneCount > 0) {
                    NativeWidgetActionPill(
                        label = stringResource(R.string.native_widget_tasks_clear_done),
                        emphasized = false,
                        onClick = onClearCompleted,
                    )
                }
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(if (isExpressive) CircleShape else shapes.small)
                        .background(if (isExpressive) colors.accentContainer else colors.surfaceElevated)
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
                        tint = if (isExpressive) colors.onAccentContainer else colors.accent,
                        modifier = Modifier.size(16.dp),
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
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(if (isExpressive) shapes.medium else shapes.small)
                            .then(
                                if (isExpressive) {
                                    Modifier
                                        .background(
                                            if (task.completed) {
                                                colors.surfaceContainer.copy(alpha = 0.45f)
                                            } else {
                                                colors.surfaceContainerHigh.copy(alpha = 0.65f)
                                            },
                                        )
                                        .padding(horizontal = 8.dp, vertical = 5.dp)
                                } else {
                                    Modifier.padding(vertical = 3.dp)
                                },
                            )
                            .clickable(role = Role.Checkbox) { onToggleTask(task.id) },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        val checkShape = if (isExpressive) CircleShape else RoundedCornerShape(4.dp)
                        Box(
                            modifier = Modifier
                                .size(18.dp)
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
                                    modifier = Modifier.size(12.dp),
                                )
                            }
                        }

                        Text(
                            text = task.title,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = if (isExpressive && !task.completed) FontWeight.Medium else FontWeight.Normal,
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
 * Widget 9: Apps frequentes com base nos aplicativos mais abertos pelo usuário.
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
    val columns = spanX.coerceIn(3, 5)
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
                contentAlignment = Alignment.CenterStart,
            ) {
                Text(
                    text = stringResource(R.string.native_widget_frequent_apps_empty),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceMuted,
                )
            }
        } else {
            val rows = visibleApps.chunked(columns)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                for (rowApps in rows) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(
                                if (isExpressive && spanY >= 2) {
                                    Modifier
                                        .clip(shapes.large)
                                        .background(colors.surfaceContainerHigh.copy(alpha = 0.50f))
                                        .padding(vertical = 4.dp)
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
                                iconSize = if (spanY <= 1) 42.dp else 44.dp,
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
