package io.github.vimitt.overview.ui.components

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.text.format.DateFormat
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import io.github.vimitt.overview.R
import io.github.vimitt.overview.data.NativeWidgetSystemProviders
import io.github.vimitt.overview.domain.model.CalendarEventItem
import io.github.vimitt.overview.domain.model.HomeClockStyle
import io.github.vimitt.overview.domain.model.HomeGridSpec
import io.github.vimitt.overview.domain.model.WeatherLocation
import io.github.vimitt.overview.domain.model.WeatherSnapshot
import io.github.vimitt.overview.ui.home.HomeWorkspaceItem
import io.github.vimitt.overview.ui.theme.OverviewTheme
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Pílula base compartilhada pelas três áreas da barra superior estilo GNOME.
 */
@Composable
fun GnomeTopBarPill(
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    val colors = OverviewTheme.colors
    val shape = OverviewTheme.shapes.pill
    val isExpressive = OverviewTheme.components.isExpressive

    val pillTop = if (isExpressive) {
        lerp(colors.dashBackground, colors.accentContainer, 0.28f)
    } else {
        lerp(colors.dashBackground, Color.White, if (colors.isDark) 0.06f else 0.20f)
    }
    val pillBottom = if (isExpressive) {
        lerp(colors.dashBackground, colors.accentContainer, 0.16f)
    } else {
        colors.dashBackground
    }
    val pillBrush = androidx.compose.ui.graphics.Brush.verticalGradient(colors = listOf(pillTop, pillBottom))
    val rimTop = if (isExpressive) {
        lerp(colors.accent.copy(alpha = 0.34f), Color.White, if (colors.isDark) 0.18f else 0.45f)
    } else {
        lerp(colors.hairlineBorder, Color.White, if (colors.isDark) 0.16f else 0.55f)
    }
    val rimBottom = if (isExpressive) {
        colors.accent.copy(alpha = 0.20f)
    } else {
        colors.hairlineBorder.copy(alpha = 0.70f)
    }
    val pillBorderBrush = androidx.compose.ui.graphics.Brush.verticalGradient(colors = listOf(rimTop, rimBottom))

    Box(
        modifier = modifier
            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
            .clip(shape)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier
                .defaultMinSize(minHeight = 36.dp)
                .shadow(
                    elevation = if (isExpressive) 5.dp else 3.dp,
                    shape = shape,
                    clip = false,
                )
                .clip(shape)
                .background(pillBrush)
                .border(1.dp, pillBorderBrush, shape)
                .padding(horizontal = 13.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            content = content,
        )
    }
}

/**
 * Hospeda um painel da barra superior com transição de entrada e saída do Material 3,
 * suavizando o fundo e preservando a continuidade espacial com a pílula de origem.
 */
@Composable
fun GnomeTopPopoverHost(
    visible: Boolean,
    content: @Composable AnimatedVisibilityScope.() -> Unit,
) {
    val motion = OverviewTheme.motion
    val reducedMotion = OverviewTheme.reducedMotion
    val enterFade = if (reducedMotion) {
        fadeIn(motion.effectSpec())
    } else {
        fadeIn(motion.effectSpec(durationMillis = 170))
    }
    val exitFade = if (reducedMotion) {
        fadeOut(motion.effectSpec())
    } else {
        fadeOut(motion.effectSpec(durationMillis = 160))
    }
    AnimatedVisibility(
        visible = visible,
        enter = enterFade,
        exit = exitFade,
        content = content,
    )
}

/**
 * Contêiner flutuante padrão dos painéis que se expandem a partir da barra superior GNOME.
 */
@Composable
fun GnomeTopPopoverScaffold(
    panelTitle: String,
    alignment: Alignment,
    transformOrigin: TransformOrigin,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    visibilityScope: AnimatedVisibilityScope? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = OverviewTheme.colors
    val shapes = OverviewTheme.shapes
    val motion = OverviewTheme.motion
    val reducedMotion = OverviewTheme.reducedMotion
    val isExpressive = OverviewTheme.components.isExpressive

    val panelShape = shapes.large
    val panelBackground = if (isExpressive) {
        lerp(colors.surfaceContainerHigh, colors.accentContainer, 0.14f)
    } else {
        colors.surface
    }
    val panelBorder = if (isExpressive) {
        colors.accent.copy(alpha = 0.28f)
    } else {
        colors.hairlineBorder
    }

    BackHandler(onBack = onDismiss)

    val backdropInteraction = remember { MutableInteractionSource() }
    val cardInteraction = remember { MutableInteractionSource() }

    val cardEnter: EnterTransition
    val cardExit: ExitTransition
    if (reducedMotion) {
        cardEnter = EnterTransition.None
        cardExit = ExitTransition.None
    } else {
        val spatialEnterFloat = if (isExpressive) {
            motion.expressiveSpec<Float>(durationMillis = 230)
        } else {
            motion.spatialSpec(durationMillis = 180)
        }
        val spatialEnterOffset = if (isExpressive) {
            motion.expressiveSpec(durationMillis = 230, visibilityThreshold = IntOffset.VisibilityThreshold)
        } else {
            motion.spatialSpec(durationMillis = 180, visibilityThreshold = IntOffset.VisibilityThreshold)
        }
        val spatialExitFloat = motion.spatialSpec<Float>(durationMillis = 160)
        val spatialExitOffset = motion.spatialSpec(
            durationMillis = 160,
            visibilityThreshold = IntOffset.VisibilityThreshold,
        )
        cardEnter = fadeIn(motion.effectSpec(durationMillis = 150)) +
            scaleIn(
                initialScale = if (isExpressive) 0.88f else 0.94f,
                transformOrigin = transformOrigin,
                animationSpec = spatialEnterFloat,
            ) +
            slideInVertically(
                animationSpec = spatialEnterOffset,
                initialOffsetY = { -it / 16 },
            )
        cardExit = fadeOut(motion.effectSpec(durationMillis = 140)) +
            scaleOut(
                targetScale = if (isExpressive) 0.90f else 0.95f,
                transformOrigin = transformOrigin,
                animationSpec = spatialExitFloat,
            ) +
            slideOutVertically(
                animationSpec = spatialExitOffset,
                targetOffsetY = { -it / 18 },
            )
    }

    val animatedCardModifier = if (visibilityScope != null) {
        with(visibilityScope) {
            Modifier.animateEnterExit(
                enter = cardEnter,
                exit = cardExit,
            )
        }
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .semantics { contentDescription = panelTitle }
            .background(Color.Black.copy(alpha = 0.38f))
            .clickable(
                interactionSource = backdropInteraction,
                indication = null,
                onClick = onDismiss,
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = alignment,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 440.dp)
                .fillMaxWidth()
                .then(animatedCardModifier)
                .shadow(elevation = 18.dp, shape = panelShape, clip = false)
                .clip(panelShape)
                .background(panelBackground)
                .border(1.dp, panelBorder, panelShape)
                .clickable(
                    interactionSource = cardInteraction,
                    indication = null,
                    onClick = {},
                )
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            content = content,
        )
    }
}

/**
 * Pílula no canto superior esquerdo (estilo indicador de áreas de trabalho do GNOME)
 * para visualizar e criar páginas/abas da tela inicial.
 */
@Composable
fun HomePagesPill(
    pageCount: Int,
    currentPage: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = OverviewTheme.colors
    val safeCount = pageCount.coerceIn(1, HomeGridSpec.MAX_PAGES)
    val safeCurrent = currentPage.coerceIn(0, safeCount - 1)

    GnomeTopBarPill(
        contentDescription = stringResource(R.string.top_bar_pages_cd),
        onClick = onClick,
        modifier = modifier,
    ) {
        TopPagesGlyph(
            tint = colors.onSurface,
            accent = colors.accent,
            size = 14.dp,
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            for (index in 0 until safeCount) {
                val isSelected = index == safeCurrent
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .size(width = if (isSelected) 14.dp else 5.dp, height = 5.dp)
                        .background(
                            if (isSelected) {
                                colors.accent
                            } else {
                                colors.onSurface.copy(alpha = 0.35f)
                            },
                        ),
                )
            }
        }
    }
}

/**
 * Pílula compacta centralizada no topo com o horário atual, inspirada na barra superior do GNOME.
 */
@Composable
fun HomeTopClockPill(
    isVisible: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val colors = OverviewTheme.colors
    var currentTimeMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }

    DisposableEffect(context, isVisible) {
        if (!isVisible) {
            return@DisposableEffect onDispose {}
        }
        currentTimeMillis = System.currentTimeMillis()
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                currentTimeMillis = System.currentTimeMillis()
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_TIME_TICK)
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
        }
        runCatching {
            ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_EXPORTED)
        }
        onDispose {
            runCatching { context.unregisterReceiver(receiver) }
        }
    }

    val is24Hour = DateFormat.is24HourFormat(context)
    val locale = remember { Locale.getDefault() }
    val timeText = remember(currentTimeMillis, is24Hour, locale) {
        val pattern = if (is24Hour) "HH:mm" else "h:mm"
        SimpleDateFormat(pattern, locale).format(Date(currentTimeMillis))
    }

    GnomeTopBarPill(
        contentDescription = stringResource(R.string.top_bar_clock_cd),
        onClick = onClick,
        modifier = modifier,
    ) {
        Text(
            text = timeText,
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.SemiBold,
                fontFeatureSettings = "tnum",
            ),
            color = colors.onSurface,
        )
    }
}

/**
 * Painel superior esquerdo para ver todas as páginas/abas da tela inicial,
 * alternar entre elas, remover páginas vazias e criar novas páginas.
 */
@Composable
fun GnomeHomePagesPanel(
    pageCount: Int,
    currentPage: Int,
    workspaceItems: List<HomeWorkspaceItem>,
    gridColumns: Int,
    gridRows: Int,
    lockLayout: Boolean,
    onSelectPage: (Int) -> Unit,
    onCreatePage: () -> Unit,
    onRemoveEmptyPage: (Int) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    visibilityScope: AnimatedVisibilityScope? = null,
) {
    val colors = OverviewTheme.colors
    val shapes = OverviewTheme.shapes
    val isExpressive = OverviewTheme.components.isExpressive
    val title = stringResource(R.string.home_pages_title)

    val safeCount = pageCount.coerceIn(1, HomeGridSpec.MAX_PAGES)
    val safeCurrent = currentPage.coerceIn(0, safeCount - 1)
    val itemsByPage = remember(workspaceItems) {
        workspaceItems.groupBy { it.item.page }
    }
    val canCreateMore = safeCount < HomeGridSpec.MAX_PAGES

    GnomeTopPopoverScaffold(
        panelTitle = title,
        alignment = Alignment.TopStart,
        transformOrigin = TransformOrigin(0.14f, 0f),
        onDismiss = onDismiss,
        modifier = modifier,
        visibilityScope = visibilityScope,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = colors.onSurface,
                )
                Box(
                    modifier = Modifier
                        .clip(shapes.pill)
                        .background(if (isExpressive) colors.accentContainer else colors.surfaceElevated)
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                ) {
                    Text(
                        text = stringResource(R.string.home_pages_count_badge, safeCount, HomeGridSpec.MAX_PAGES),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontFeatureSettings = "tnum",
                        ),
                        color = if (isExpressive) colors.onAccentContainer else colors.onSurfaceMuted,
                    )
                }
            }
            PanelCloseButton(onClick = onDismiss)
        }

        val totalSlots = safeCount + if (canCreateMore) 1 else 0
        val rowsCount = (totalSlots + 1) / 2
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            for (rowIndex in 0 until rowsCount) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    for (col in 0 until 2) {
                        val slotIndex = rowIndex * 2 + col
                        if (slotIndex < safeCount) {
                            val pageItems = itemsByPage[slotIndex].orEmpty()
                            HomePagePreviewCard(
                                pageIndex = slotIndex,
                                isSelected = slotIndex == safeCurrent,
                                items = pageItems,
                                gridColumns = gridColumns,
                                gridRows = gridRows,
                                canRemove = safeCount > 1 && pageItems.isEmpty() && !lockLayout,
                                onSelect = {
                                    onSelectPage(slotIndex)
                                    onDismiss()
                                },
                                onRemove = { onRemoveEmptyPage(slotIndex) },
                                modifier = Modifier.weight(1f),
                            )
                        } else if (slotIndex == safeCount && canCreateMore) {
                            CreateHomePageCard(
                                onClick = onCreatePage,
                                modifier = Modifier.weight(1f),
                            )
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        if (!canCreateMore) {
            Text(
                text = stringResource(R.string.home_pages_limit_reached, HomeGridSpec.MAX_PAGES),
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceMuted,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun HomePagePreviewCard(
    pageIndex: Int,
    isSelected: Boolean,
    items: List<HomeWorkspaceItem>,
    gridColumns: Int,
    gridRows: Int,
    canRemove: Boolean,
    onSelect: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = OverviewTheme.colors
    val shapes = OverviewTheme.shapes
    val isExpressive = OverviewTheme.components.isExpressive

    val cardFill = when {
        isSelected && isExpressive -> lerp(colors.surfaceElevated, colors.accentContainer, 0.36f)
        isSelected -> lerp(colors.surfaceElevated, colors.accentContainer, 0.24f)
        else -> colors.surfaceElevated
    }
    val cardBorder = if (isSelected) {
        colors.accent
    } else {
        colors.hairlineBorder
    }

    Column(
        modifier = modifier
            .clip(shapes.medium)
            .background(cardFill)
            .border(width = if (isSelected) 1.5.dp else 1.dp, color = cardBorder, shape = shapes.medium)
            .clickable(role = Role.Button, onClick = onSelect)
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(R.string.home_pages_page_title, pageIndex + 1),
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                ),
                color = colors.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            when {
                canRemove -> {
                    val removeCd = stringResource(R.string.home_pages_remove_cd, pageIndex + 1)
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(colors.surface.copy(alpha = 0.72f))
                            .clickable(role = Role.Button, onClick = onRemove)
                            .semantics { contentDescription = removeCd },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = removeCd,
                            tint = colors.onSurfaceMuted,
                            modifier = Modifier.size(14.dp),
                        )
                    }
                }
                isSelected -> {
                    Box(
                        modifier = Modifier
                            .clip(shapes.pill)
                            .background(colors.accent)
                            .padding(horizontal = 7.dp, vertical = 2.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.home_pages_current_badge),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = colors.onAccent,
                        )
                    }
                }
            }
        }

        HomePageMiniGridPreview(
            items = items,
            gridColumns = gridColumns,
            gridRows = gridRows,
            isSelected = isSelected,
            modifier = Modifier
                .fillMaxWidth()
                .height(82.dp),
        )

        Text(
            text = if (items.isEmpty()) {
                stringResource(R.string.home_pages_empty)
            } else {
                pluralStringResource(R.plurals.home_pages_items_count, items.size, items.size)
            },
            style = MaterialTheme.typography.labelSmall,
            color = if (isSelected) colors.accent else colors.onSurfaceMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun HomePageMiniGridPreview(
    items: List<HomeWorkspaceItem>,
    gridColumns: Int,
    gridRows: Int,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = OverviewTheme.colors
    val shapes = OverviewTheme.shapes
    val safeCols = gridColumns.coerceIn(3, 6)
    val safeRows = gridRows.coerceIn(3, 6)

    BoxWithConstraints(
        modifier = modifier
            .clip(shapes.small)
            .background(colors.surface.copy(alpha = if (isSelected) 0.72f else 0.56f))
            .border(1.dp, colors.hairlineBorder, shapes.small)
            .padding(5.dp),
    ) {
        if (items.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "·",
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.onSurfaceMuted.copy(alpha = 0.55f),
                )
            }
        } else {
            val cellWidth = maxWidth / safeCols
            val cellHeight = maxHeight / safeRows
            for (workspaceItem in items) {
                val item = workspaceItem.item
                val spanX = item.spanX.coerceIn(1, safeCols)
                val spanY = item.spanY.coerceIn(1, safeRows)
                val cellX = item.cellX.coerceIn(0, (safeCols - spanX).coerceAtLeast(0))
                val cellY = item.cellY.coerceIn(0, (safeRows - spanY).coerceAtLeast(0))

                val tileColor = when (workspaceItem) {
                    is HomeWorkspaceItem.Widget,
                    is HomeWorkspaceItem.NativeWidget,
                    -> colors.accent.copy(alpha = 0.42f)
                    is HomeWorkspaceItem.Folder -> colors.accentContainer
                    is HomeWorkspaceItem.Shortcut,
                    is HomeWorkspaceItem.PinnedShortcut,
                    -> colors.onSurface.copy(alpha = 0.24f)
                }

                Box(
                    modifier = Modifier
                        .offset(x = cellWidth * cellX, y = cellHeight * cellY)
                        .size(width = cellWidth * spanX, height = cellHeight * spanY)
                        .padding(1.5.dp)
                        .clip(shapes.small)
                        .background(tileColor),
                )
            }
        }
    }
}

@Composable
private fun CreateHomePageCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = OverviewTheme.colors
    val shapes = OverviewTheme.shapes
    val isExpressive = OverviewTheme.components.isExpressive

    Column(
        modifier = modifier
            .clip(shapes.medium)
            .background(
                if (isExpressive) {
                    lerp(colors.surfaceElevated, colors.accentContainer, 0.20f)
                } else {
                    colors.surfaceElevated
                },
            )
            .border(1.dp, colors.accent.copy(alpha = 0.36f), shapes.medium)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(10.dp)
            .height(132.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(colors.accentContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = null,
                tint = colors.onAccentContainer,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.home_pages_create),
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
            color = colors.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = stringResource(R.string.home_pages_create_subtitle),
            style = MaterialTheme.typography.labelSmall,
            color = colors.onSurfaceMuted,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * Painel central estilo GNOME que expande o relógio configurado pelo usuário
 * junto com um calendário mensal interativo e os compromissos do dia selecionado.
 */
@Composable
fun GnomeClockCalendarPanel(
    clockStyle: HomeClockStyle,
    clockBackgroundAlpha: Float,
    weather: WeatherSnapshot?,
    weatherLocation: WeatherLocation?,
    isWeatherLoading: Boolean,
    showWeather: Boolean,
    hasCalendarPermission: Boolean,
    permissionRevision: Int,
    onWeatherClick: () -> Unit,
    onRefreshWeatherClick: () -> Unit,
    onEnableLocationClick: () -> Unit,
    onRequestCalendarPermission: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    visibilityScope: AnimatedVisibilityScope? = null,
) {
    val context = LocalContext.current
    val colors = OverviewTheme.colors
    val shapes = OverviewTheme.shapes
    val isExpressive = OverviewTheme.components.isExpressive
    val locale = remember { Locale.getDefault() }
    val is24Hour = DateFormat.is24HourFormat(context)
    val title = stringResource(R.string.clock_calendar_title)

    val today = remember {
        val cal = Calendar.getInstance()
        CalendarDay(
            year = cal.get(Calendar.YEAR),
            month = cal.get(Calendar.MONTH),
            dayOfMonth = cal.get(Calendar.DAY_OF_MONTH),
        )
    }
    var displayedYear by remember { mutableIntStateOf(today.year) }
    var displayedMonth by remember { mutableIntStateOf(today.month) }
    var selectedDay by remember { mutableStateOf(today) }
    var monthEvents by remember { mutableStateOf<List<CalendarEventItem>>(emptyList()) }

    LaunchedEffect(displayedYear, displayedMonth, hasCalendarPermission, permissionRevision) {
        if (!hasCalendarPermission) {
            monthEvents = emptyList()
            return@LaunchedEffect
        }
        val startCal = Calendar.getInstance().apply {
            clear()
            set(displayedYear, displayedMonth, 1, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val endCal = (startCal.clone() as Calendar).apply {
            add(Calendar.MONTH, 1)
        }
        // Inclui margem de 1 dia para capturar eventos de dia inteiro em UTC.
        monthEvents = NativeWidgetSystemProviders.readCalendarEventsForRange(
            context = context,
            startMs = startCal.timeInMillis - DAY_MS,
            endMs = endCal.timeInMillis + DAY_MS,
        )
    }

    val eventsByDayInMonth = remember(monthEvents, displayedYear, displayedMonth) {
        buildEventsByDayMap(
            events = monthEvents,
            targetYear = displayedYear,
            targetMonth = displayedMonth,
        )
    }

    val selectedDayEvents = remember(eventsByDayInMonth, selectedDay, displayedYear, displayedMonth, monthEvents) {
        if (selectedDay.year == displayedYear && selectedDay.month == displayedMonth) {
            eventsByDayInMonth[selectedDay.dayOfMonth].orEmpty()
        } else {
            emptyList()
        }
    }

    GnomeTopPopoverScaffold(
        panelTitle = title,
        alignment = Alignment.TopCenter,
        transformOrigin = TransformOrigin(0.5f, 0f),
        onDismiss = onDismiss,
        modifier = modifier,
        visibilityScope = visibilityScope,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = colors.onSurface,
            )
            PanelCloseButton(onClick = onDismiss)
        }

        // Relógio atual expandido com todas as informações (hora, data e clima).
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shapes.medium)
                .background(
                    if (isExpressive) {
                        lerp(colors.surfaceElevated, colors.accentContainer, 0.20f)
                    } else {
                        colors.surfaceElevated
                    },
                )
                .border(1.dp, colors.hairlineBorder, shapes.medium)
                .padding(horizontal = 14.dp, vertical = 16.dp),
            contentAlignment = Alignment.Center,
        ) {
            HomeClock(
                style = clockStyle,
                compact = false,
                useWallpaperColors = false,
                backgroundAlpha = clockBackgroundAlpha.coerceAtLeast(0.85f),
                isVisible = true,
                weather = weather,
                weatherLocation = weatherLocation,
                isWeatherLoading = isWeatherLoading,
                showWeather = showWeather,
                onWeatherClick = {
                    onDismiss()
                    onWeatherClick()
                },
                onRefreshWeatherClick = onRefreshWeatherClick,
                onEnableLocationClick = {
                    onDismiss()
                    onEnableLocationClick()
                },
            )
        }

        // Calendário mensal interativo estilo GNOME.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shapes.medium)
                .background(colors.surfaceElevated)
                .border(1.dp, colors.hairlineBorder, shapes.medium)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            CalendarMonthHeader(
                year = displayedYear,
                month = displayedMonth,
                locale = locale,
                showTodayButton = displayedYear != today.year || displayedMonth != today.month || selectedDay != today,
                onPrevMonth = {
                    val prev = shiftMonth(displayedYear, displayedMonth, -1)
                    displayedYear = prev.first
                    displayedMonth = prev.second
                    selectedDay = CalendarDay(prev.first, prev.second, 1)
                },
                onNextMonth = {
                    val next = shiftMonth(displayedYear, displayedMonth, 1)
                    displayedYear = next.first
                    displayedMonth = next.second
                    selectedDay = CalendarDay(next.first, next.second, 1)
                },
                onTodayClick = {
                    displayedYear = today.year
                    displayedMonth = today.month
                    selectedDay = today
                },
            )

            CalendarMonthGrid(
                year = displayedYear,
                month = displayedMonth,
                today = today,
                selectedDay = selectedDay,
                eventDays = eventsByDayInMonth.keys,
                locale = locale,
                onSelectDay = { dayNumber ->
                    selectedDay = CalendarDay(displayedYear, displayedMonth, dayNumber)
                },
            )

            HorizontalDivider(thickness = 1.dp, color = colors.hairlineBorder)

            SelectedDayScheduleSection(
                selectedDay = selectedDay,
                locale = locale,
                is24Hour = is24Hour,
                hasCalendarPermission = hasCalendarPermission,
                events = selectedDayEvents,
                onRequestCalendarPermission = onRequestCalendarPermission,
                onOpenEvent = { event ->
                    onDismiss()
                    NativeWidgetSystemProviders.openCalendarEvent(context, event.id)
                },
                onOpenCalendarApp = {
                    onDismiss()
                    NativeWidgetSystemProviders.openCalendarApp(context)
                },
            )
        }
    }
}

private data class CalendarDay(
    val year: Int,
    val month: Int,
    val dayOfMonth: Int,
)

@Composable
private fun CalendarMonthHeader(
    year: Int,
    month: Int,
    locale: Locale,
    showTodayButton: Boolean,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onTodayClick: () -> Unit,
) {
    val colors = OverviewTheme.colors
    val shapes = OverviewTheme.shapes
    val monthTitle = remember(year, month, locale) {
        val cal = Calendar.getInstance(locale).apply {
            clear()
            set(year, month, 1)
        }
        val raw = SimpleDateFormat("LLLL yyyy", locale).format(cal.time)
        raw.replaceFirstChar { ch -> if (ch.isLowerCase()) ch.titlecase(locale) else ch.toString() }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        IconButton(
            onClick = onPrevMonth,
            modifier = Modifier.size(36.dp),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = stringResource(R.string.clock_calendar_prev_month_cd),
                tint = colors.onSurface,
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f),
        ) {
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = monthTitle,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = colors.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (showTodayButton) {
                Box(
                    modifier = Modifier
                        .clip(shapes.pill)
                        .background(colors.accentContainer)
                        .clickable(role = Role.Button, onClick = onTodayClick)
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                ) {
                    Text(
                        text = stringResource(R.string.clock_calendar_today),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = colors.onAccentContainer,
                    )
                }
            }
            Spacer(modifier = Modifier.weight(1f))
        }

        IconButton(
            onClick = onNextMonth,
            modifier = Modifier.size(36.dp),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = stringResource(R.string.clock_calendar_next_month_cd),
                tint = colors.onSurface,
            )
        }
    }
}

@Composable
private fun CalendarMonthGrid(
    year: Int,
    month: Int,
    today: CalendarDay,
    selectedDay: CalendarDay,
    eventDays: Set<Int>,
    locale: Locale,
    onSelectDay: (Int) -> Unit,
) {
    val colors = OverviewTheme.colors
    val firstDayOfWeek = remember(locale) {
        Calendar.getInstance(locale).firstDayOfWeek
    }
    val weekdayLabels = remember(locale, firstDayOfWeek) {
        val cal = Calendar.getInstance(locale)
        val fmt = SimpleDateFormat("EEEEE", locale)
        (0 until 7).map { offset ->
            val dow = ((firstDayOfWeek - Calendar.SUNDAY + offset) % 7) + Calendar.SUNDAY
            cal.set(Calendar.DAY_OF_WEEK, dow)
            fmt.format(cal.time).uppercase(locale)
        }
    }
    val monthMatrix = remember(year, month, firstDayOfWeek) {
        buildMonthMatrix(year = year, month = month, firstDayOfWeek = firstDayOfWeek)
    }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            for (label in weekdayLabels) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = colors.onSurfaceMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        for (week in monthMatrix) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                for (dayCell in week) {
                    if (dayCell == null) {
                        Spacer(modifier = Modifier.weight(1f).height(38.dp))
                    } else {
                        val isToday = year == today.year && month == today.month && dayCell == today.dayOfMonth
                        val isSelected = year == selectedDay.year && month == selectedDay.month && dayCell == selectedDay.dayOfMonth
                        val hasEvents = dayCell in eventDays

                        val cellBackground = when {
                            isToday && isSelected -> colors.accent
                            isSelected -> colors.accentContainer
                            isToday -> colors.accent.copy(alpha = 0.18f)
                            else -> Color.Transparent
                        }
                        val cellTextColor = when {
                            isToday && isSelected -> colors.onAccent
                            isSelected -> colors.onAccentContainer
                            isToday -> colors.accent
                            else -> colors.onSurface
                        }
                        val dotColor = when {
                            isToday && isSelected -> colors.onAccent
                            else -> colors.accent
                        }

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .clip(CircleShape)
                                .background(cellBackground)
                                .then(
                                    if (isToday && !isSelected) {
                                        Modifier.border(1.dp, colors.accent.copy(alpha = 0.7f), CircleShape)
                                    } else {
                                        Modifier
                                    },
                                )
                                .clickable(role = Role.Button) { onSelectDay(dayCell) },
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Text(
                                text = dayCell.toString(),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontFeatureSettings = "tnum",
                                ),
                                color = cellTextColor,
                            )
                            if (hasEvents) {
                                Box(
                                    modifier = Modifier
                                        .padding(top = 1.dp)
                                        .size(4.dp)
                                        .clip(CircleShape)
                                        .background(dotColor),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectedDayScheduleSection(
    selectedDay: CalendarDay,
    locale: Locale,
    is24Hour: Boolean,
    hasCalendarPermission: Boolean,
    events: List<CalendarEventItem>,
    onRequestCalendarPermission: () -> Unit,
    onOpenEvent: (CalendarEventItem) -> Unit,
    onOpenCalendarApp: () -> Unit,
) {
    val colors = OverviewTheme.colors
    val shapes = OverviewTheme.shapes
    val dayLabel = remember(selectedDay, locale) {
        val cal = Calendar.getInstance(locale).apply {
            clear()
            set(selectedDay.year, selectedDay.month, selectedDay.dayOfMonth)
        }
        val pattern = DateFormat.getBestDateTimePattern(locale, "dMMM")
        SimpleDateFormat(pattern, locale).format(cal.time)
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(R.string.clock_calendar_events_for_day, dayLabel),
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                color = colors.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).padding(end = 8.dp),
            )
            Box(
                modifier = Modifier
                    .clip(shapes.pill)
                    .background(colors.accentContainer)
                    .clickable(role = Role.Button, onClick = onOpenCalendarApp)
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            ) {
                Text(
                    text = stringResource(R.string.clock_calendar_open_app),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = colors.onAccentContainer,
                )
            }
        }

        when {
            !hasCalendarPermission -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(shapes.small)
                        .background(colors.surface.copy(alpha = 0.65f))
                        .border(1.dp, colors.hairlineBorder, shapes.small)
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = stringResource(R.string.native_widget_agenda_permission_title),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceMuted,
                        modifier = Modifier.weight(1f).padding(end = 8.dp),
                    )
                    Box(
                        modifier = Modifier
                            .clip(shapes.pill)
                            .background(colors.accent)
                            .clickable(role = Role.Button, onClick = onRequestCalendarPermission)
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.native_widget_agenda_permission_cta),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = colors.onAccent,
                        )
                    }
                }
            }

            events.isEmpty() -> {
                Text(
                    text = stringResource(R.string.clock_calendar_no_events),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceMuted,
                )
            }

            else -> {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (event in events.take(5)) {
                        val timeText = remember(event.id, event.beginMs, event.allDay, is24Hour, locale) {
                            if (event.allDay) {
                                null
                            } else {
                                val fmt = if (is24Hour) "HH:mm" else "h:mm a"
                                SimpleDateFormat(fmt, locale).format(Date(event.beginMs))
                            }
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(shapes.small)
                                .background(colors.surface.copy(alpha = 0.65f))
                                .border(1.dp, colors.hairlineBorder, shapes.small)
                                .clickable(role = Role.Button) { onOpenEvent(event) }
                                .padding(horizontal = 10.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(3.dp)
                                    .height(18.dp)
                                    .clip(CircleShape)
                                    .background(colors.accent),
                            )
                            Text(
                                text = timeText ?: stringResource(R.string.native_widget_agenda_all_day),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFeatureSettings = "tnum",
                                ),
                                color = colors.accent,
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = event.title,
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                    color = colors.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                if (event.location.isNotBlank()) {
                                    Text(
                                        text = event.location,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = colors.onSurfaceMuted,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PanelCloseButton(onClick: () -> Unit) {
    val colors = OverviewTheme.colors
    val isExpressive = OverviewTheme.components.isExpressive
    val closeCd = stringResource(R.string.settings_close_dialog)
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(if (isExpressive) colors.surfaceContainerHigh else colors.surfaceElevated)
            .border(1.dp, colors.hairlineBorder, CircleShape)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = closeCd },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.Close,
            contentDescription = closeCd,
            tint = colors.onSurface,
            modifier = Modifier.size(16.dp),
        )
    }
}

@Composable
private fun TopPagesGlyph(
    tint: Color,
    accent: Color,
    size: Dp,
) {
    Canvas(modifier = Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = Stroke(width = w * 0.11f)
        drawRoundRect(
            color = tint.copy(alpha = 0.55f),
            topLeft = Offset(w * 0.24f, h * 0.08f),
            size = Size(w * 0.68f, h * 0.68f),
            cornerRadius = CornerRadius(w * 0.18f, w * 0.18f),
            style = stroke,
        )
        drawRoundRect(
            color = accent.copy(alpha = 0.22f),
            topLeft = Offset(w * 0.08f, h * 0.24f),
            size = Size(w * 0.68f, h * 0.68f),
            cornerRadius = CornerRadius(w * 0.18f, w * 0.18f),
        )
        drawRoundRect(
            color = tint,
            topLeft = Offset(w * 0.08f, h * 0.24f),
            size = Size(w * 0.68f, h * 0.68f),
            cornerRadius = CornerRadius(w * 0.18f, w * 0.18f),
            style = stroke,
        )
    }
}

private const val DAY_MS = 24L * 60L * 60L * 1000L

internal fun shiftMonth(year: Int, month: Int, delta: Int): Pair<Int, Int> {
    val cal = Calendar.getInstance().apply {
        clear()
        set(year, month, 1)
        add(Calendar.MONTH, delta)
    }
    return cal.get(Calendar.YEAR) to cal.get(Calendar.MONTH)
}

internal fun buildMonthMatrix(
    year: Int,
    month: Int,
    firstDayOfWeek: Int,
): List<List<Int?>> {
    val cal = Calendar.getInstance().apply {
        clear()
        set(year, month, 1)
    }
    val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    val firstDow = cal.get(Calendar.DAY_OF_WEEK)
    val leadingEmpty = ((firstDow - firstDayOfWeek) + 7) % 7
    val totalCells = leadingEmpty + daysInMonth
    val weeksCount = (totalCells + 6) / 7

    return List(weeksCount) { week ->
        List(7) { dayOffset ->
            val index = week * 7 + dayOffset
            val dayNumber = index - leadingEmpty + 1
            if (dayNumber in 1..daysInMonth) dayNumber else null
        }
    }
}

internal fun buildEventsByDayMap(
    events: List<CalendarEventItem>,
    targetYear: Int,
    targetMonth: Int,
): Map<Int, List<CalendarEventItem>> {
    if (events.isEmpty()) return emptyMap()
    val localCal = Calendar.getInstance()
    val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
    val monthStartCal = Calendar.getInstance().apply {
        clear()
        set(targetYear, targetMonth, 1, 0, 0, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val daysInMonth = monthStartCal.getActualMaximum(Calendar.DAY_OF_MONTH)
    val result = mutableMapOf<Int, MutableList<CalendarEventItem>>()

    for (event in events) {
        if (event.allDay) {
            // Eventos de dia inteiro são armazenados pelo provedor do Android em meia-noite UTC.
            utcCal.timeInMillis = event.beginMs
            val startDayKey = utcCal.get(Calendar.YEAR) * 10_000 +
                utcCal.get(Calendar.MONTH) * 100 +
                utcCal.get(Calendar.DAY_OF_MONTH)
            utcCal.timeInMillis = (event.endMs - 1L).coerceAtLeast(event.beginMs)
            val endDayKey = utcCal.get(Calendar.YEAR) * 10_000 +
                utcCal.get(Calendar.MONTH) * 100 +
                utcCal.get(Calendar.DAY_OF_MONTH)

            for (day in 1..daysInMonth) {
                val key = targetYear * 10_000 + targetMonth * 100 + day
                if (key in startDayKey..endDayKey) {
                    result.getOrPut(day) { mutableListOf() }.add(event)
                }
            }
        } else {
            for (day in 1..daysInMonth) {
                localCal.clear()
                localCal.set(targetYear, targetMonth, day, 0, 0, 0)
                localCal.set(Calendar.MILLISECOND, 0)
                val dayStart = localCal.timeInMillis
                val dayEnd = dayStart + DAY_MS - 1L
                if (event.beginMs <= dayEnd && event.endMs >= dayStart) {
                    result.getOrPut(day) { mutableListOf() }.add(event)
                }
            }
        }
    }
    return result
}
