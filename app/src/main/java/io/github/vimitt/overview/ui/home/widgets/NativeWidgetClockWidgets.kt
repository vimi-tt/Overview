package io.github.vimitt.overview.ui.home.widgets

import android.os.SystemClock
import android.text.format.DateFormat
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.vimitt.overview.R
import io.github.vimitt.overview.domain.model.CalendarEventItem
import io.github.vimitt.overview.domain.model.ClockToolMode
import io.github.vimitt.overview.domain.model.TimerStopwatchState
import io.github.vimitt.overview.domain.model.WeatherSnapshot
import io.github.vimitt.overview.ui.components.ExpressiveWavyProgressBar
import io.github.vimitt.overview.ui.components.OverviewPolygonFactory
import io.github.vimitt.overview.ui.components.RoundedPolygonShape
import io.github.vimitt.overview.ui.theme.OverviewTheme
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Resumo do dia estilo At a Glance: próximo evento (ou data), clima e alarme integrados
 * numa linha ou cartão compacto adaptável ao tamanho escolhido.
 */
@Composable
fun DailySummaryNativeWidget(
    nextEvent: CalendarEventItem?,
    weather: WeatherSnapshot?,
    nextAlarmMillis: Long?,
    currentTimeMillis: Long,
    spanX: Int,
    spanY: Int,
    isVisible: Boolean,
    onOpenEvent: (CalendarEventItem?) -> Unit,
    onOpenWeather: () -> Unit,
    onOpenAlarms: () -> Unit,
) {
    val context = LocalContext.current
    val colors = OverviewTheme.colors
    val shapes = OverviewTheme.shapes
    val motion = OverviewTheme.motion
    val isExpressive = OverviewTheme.components.isExpressive
    val expressiveMotion = isExpressive && motion.usesSprings && !motion.reducedMotion
    val is24Hour = DateFormat.is24HourFormat(context)
    val locale = remember { Locale.getDefault() }

    val datePrefix = remember(currentTimeMillis, locale) {
        val pattern = DateFormat.getBestDateTimePattern(locale, if (spanX <= 2) "EEEdMMM" else "EEEEdMMM")
        SimpleDateFormat(pattern, locale).format(Date(currentTimeMillis))
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
    }
    val eventTimeLabel = remember(nextEvent, is24Hour, locale) {
        nextEvent?.let { ev ->
            if (ev.allDay) {
                ""
            } else {
                SimpleDateFormat(if (is24Hour) "HH:mm" else "h:mm", locale).format(Date(ev.beginMs))
            }
        }
    }
    val alarmShort = remember(nextAlarmMillis, is24Hour, locale) {
        nextAlarmMillis?.let { ts ->
            SimpleDateFormat(if (is24Hour) "HH:mm" else "h:mm", locale).format(Date(ts))
        }
    }

    val scallopShape = remember(isExpressive) {
        if (isExpressive) RoundedPolygonShape(OverviewPolygonFactory.clockScallopPolygon()) else null
    }

    val weatherPulse by animateFloatAsState(
        targetValue = if (expressiveMotion && isVisible && weather != null) 1f else 0.96f,
        animationSpec = if (expressiveMotion && isVisible) motion.expressiveSpec() else snap(),
        label = "summaryWeatherPulse",
    )

    val leadText = if (nextEvent != null) {
        if (!eventTimeLabel.isNullOrBlank()) "$eventTimeLabel · ${nextEvent.title}" else nextEvent.title
    } else {
        datePrefix
    }

    if (spanY >= 2) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            NativeWidgetHeader(
                title = stringResource(R.string.native_widget_daily_summary_title),
                icon = {
                    CalendarGlyph(
                        tint = if (isExpressive) colors.onAccentContainer else colors.accent,
                        size = 13.dp,
                    )
                },
                trailing = {
                    if (alarmShort != null) {
                        SummaryAlarmChip(
                            alarmShort = alarmShort,
                            isExpressive = isExpressive,
                            onClick = onOpenAlarms,
                        )
                    }
                },
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(if (isExpressive) shapes.large else shapes.small)
                    .then(
                        if (isExpressive) {
                            Modifier
                                .background(colors.surfaceContainerHigh.copy(alpha = 0.72f))
                                .padding(horizontal = 10.dp, vertical = 8.dp)
                        } else {
                            Modifier.padding(vertical = 4.dp)
                        },
                    )
                    .clickable(role = Role.Button) { onOpenEvent(nextEvent) },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = leadText,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = if (isExpressive) FontWeight.Bold else FontWeight.SemiBold,
                    ),
                    color = colors.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(end = 8.dp),
                )

                SummaryWeatherChip(
                    weather = weather,
                    isExpressive = isExpressive,
                    scallopShape = scallopShape,
                    scale = weatherPulse,
                    onClick = onOpenWeather,
                )
            }
        }
        return
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(if (isExpressive) 7.dp else 6.dp),
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .clip(if (isExpressive) shapes.pill else shapes.small)
                .then(
                    if (isExpressive) {
                        Modifier
                            .background(colors.surfaceContainerHigh.copy(alpha = 0.65f))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    } else {
                        Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                    },
                )
                .clickable(role = Role.Button) { onOpenEvent(nextEvent) },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (isExpressive && nextEvent != null) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(colors.accent),
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = leadText,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (isExpressive) FontWeight.Bold else FontWeight.Medium,
                ),
                color = colors.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        SummaryWeatherChip(
            weather = weather,
            isExpressive = isExpressive,
            scallopShape = scallopShape,
            scale = weatherPulse,
            onClick = onOpenWeather,
        )

        if (alarmShort != null && spanX >= 3) {
            SummaryAlarmChip(
                alarmShort = alarmShort,
                isExpressive = isExpressive,
                onClick = onOpenAlarms,
            )
        }
    }
}

@Composable
private fun SummaryWeatherChip(
    weather: WeatherSnapshot?,
    isExpressive: Boolean,
    scallopShape: RoundedPolygonShape?,
    scale: Float,
    onClick: () -> Unit,
) {
    val colors = OverviewTheme.colors
    val shapes = OverviewTheme.shapes
    val chipShape = if (isExpressive) shapes.pill else shapes.small

    Row(
        modifier = Modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(chipShape)
            .then(
                if (isExpressive) {
                    Modifier.background(colors.accentContainer)
                } else {
                    Modifier
                        .background(colors.surfaceElevated)
                        .border(1.dp, colors.hairlineBorder, chipShape)
                },
            )
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        if (weather != null) {
            if (isExpressive && scallopShape != null) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(scallopShape)
                        .background(colors.accent),
                    contentAlignment = Alignment.Center,
                ) {
                    WeatherGlyph(
                        weatherCode = weather.weatherCode,
                        isDay = weather.isDay,
                        tint = colors.onAccent,
                        accent = colors.onAccent,
                        size = 13.dp,
                    )
                }
            } else {
                WeatherGlyph(
                    weatherCode = weather.weatherCode,
                    isDay = weather.isDay,
                    tint = colors.onSurfaceMuted,
                    accent = colors.accent,
                    size = 15.dp,
                )
            }
            Text(
                text = "${weather.temperatureC.roundToInt()}°",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontFeatureSettings = "tnum",
                ),
                color = if (isExpressive) colors.onAccentContainer else colors.onSurface,
            )
        } else {
            Text(
                text = stringResource(R.string.native_widget_summary_tap_weather),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = if (isExpressive) FontWeight.Bold else FontWeight.Normal,
                ),
                color = if (isExpressive) colors.onAccentContainer else colors.onSurfaceMuted,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun SummaryAlarmChip(
    alarmShort: String,
    isExpressive: Boolean,
    onClick: () -> Unit,
) {
    val colors = OverviewTheme.colors
    val shapes = OverviewTheme.shapes
    val chipShape = if (isExpressive) shapes.pill else shapes.small

    Row(
        modifier = Modifier
            .clip(chipShape)
            .then(
                if (isExpressive) {
                    Modifier.background(colors.surfaceContainerHigh.copy(alpha = 0.85f))
                } else {
                    Modifier
                        .background(colors.surfaceElevated)
                        .border(1.dp, colors.hairlineBorder, chipShape)
                },
            )
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        AlarmBellGlyph(tint = colors.accent, size = 13.dp)
        Text(
            text = alarmShort,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = if (isExpressive) FontWeight.Bold else FontWeight.Medium,
                fontFeatureSettings = "tnum",
            ),
            color = colors.onSurface,
        )
    }
}

/**
 * Timer e cronômetro diretamente na Home, com ticker ativo apenas enquanto visível e em execução.
 */
@Composable
fun TimerStopwatchNativeWidget(
    state: TimerStopwatchState,
    spanX: Int,
    spanY: Int,
    isVisible: Boolean,
    onSetMode: (ClockToolMode) -> Unit,
    onSetTimerDuration: (Long) -> Unit,
    onToggleTimer: () -> Unit,
    onResetTimer: () -> Unit,
    onFinishTimerIfElapsed: () -> Unit,
    onToggleStopwatch: () -> Unit,
    onResetStopwatch: () -> Unit,
) {
    val colors = OverviewTheme.colors
    val shapes = OverviewTheme.shapes
    val motion = OverviewTheme.motion
    val isExpressive = OverviewTheme.components.isExpressive
    val expressiveMotion = isExpressive && motion.usesSprings && !motion.reducedMotion

    var elapsedSampleMs by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    val isActiveTicker = isVisible && (
        (state.mode == ClockToolMode.TIMER && state.timerRunning) ||
            (state.mode == ClockToolMode.STOPWATCH && state.stopwatchRunning)
        )

    LaunchedEffect(
        isActiveTicker,
        state.mode,
        state.timerRunning,
        state.timerTargetElapsedMs,
        state.stopwatchRunning,
    ) {
        elapsedSampleMs = SystemClock.elapsedRealtime()
        if (!isActiveTicker) return@LaunchedEffect
        val stepMs = if (state.mode == ClockToolMode.STOPWATCH) 200L else 500L
        while (true) {
            val now = SystemClock.elapsedRealtime()
            elapsedSampleMs = now
            if (state.mode == ClockToolMode.TIMER && state.timerRunning && now >= state.timerTargetElapsedMs) {
                onFinishTimerIfElapsed()
                break
            }
            delay(stepMs)
        }
    }

    val timerRemaining = if (state.timerRunning) {
        (state.timerTargetElapsedMs - elapsedSampleMs).coerceAtLeast(0L)
    } else {
        state.timerRemainingMs
    }
    val stopwatchElapsed = if (state.stopwatchRunning) {
        state.stopwatchAccumulatedMs + (elapsedSampleMs - state.stopwatchStartElapsedMs).coerceAtLeast(0L)
    } else {
        state.stopwatchAccumulatedMs
    }

    val formattedClock = if (state.mode == ClockToolMode.TIMER) {
        formatDurationClock(timerRemaining, showTenths = false)
    } else {
        formatDurationClock(stopwatchElapsed, showTenths = true)
    }
    val isRunning = if (state.mode == ClockToolMode.TIMER) state.timerRunning else state.stopwatchRunning
    val timerFinished = state.mode == ClockToolMode.TIMER && !state.timerRunning && timerRemaining == 0L

    // Pulso com física de mola na troca de estado (iniciar/pausar/concluir) no modo Expressive
    val toggleScale = remember { Animatable(1f) }
    LaunchedEffect(isRunning, timerFinished, state.mode, expressiveMotion, isVisible) {
        if (!expressiveMotion || !isVisible) {
            toggleScale.snapTo(1f)
            return@LaunchedEffect
        }
        toggleScale.snapTo(1.08f)
        toggleScale.animateTo(1f, animationSpec = motion.expressiveSpec())
    }

    val actionCorner by animateDpAsState(
        targetValue = if (isExpressive) {
            if (isRunning) 10.dp else 20.dp
        } else {
            shapes.smallRadius
        },
        animationSpec = if (expressiveMotion && isVisible) motion.expressiveSpec() else snap(),
        label = "timerMorphCorner",
    )

    val timerFraction = if (state.timerDurationMs > 0L) {
        (timerRemaining.toFloat() / state.timerDurationMs.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }

    // Modo compacto para 1×1
    if (spanX <= 1 && spanY <= 1) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .clickable(role = Role.Button) {
                    if (state.mode == ClockToolMode.TIMER) onToggleTimer() else onToggleStopwatch()
                },
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(
                    if (state.mode == ClockToolMode.TIMER) {
                        R.string.native_widget_mode_timer
                    } else {
                        R.string.native_widget_mode_stopwatch
                    },
                ),
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = if (isExpressive) colors.accent else colors.onSurfaceMuted,
                maxLines = 1,
            )
            Text(
                text = formattedClock,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontFeatureSettings = "tnum",
                ),
                color = if (timerFinished) colors.accent else colors.onSurface,
                modifier = Modifier.graphicsLayer {
                    scaleX = toggleScale.value
                    scaleY = toggleScale.value
                },
            )
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(RoundedCornerShape(actionCorner))
                    .background(if (isRunning) colors.accent else colors.accentContainer),
                contentAlignment = Alignment.Center,
            ) {
                PlayPauseGlyph(
                    isPlaying = isRunning,
                    tint = if (isRunning) colors.onAccent else colors.onAccentContainer,
                    size = 13.dp,
                )
            }
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                modifier = Modifier
                    .clip(if (isExpressive) shapes.pill else shapes.small)
                    .background(
                        if (isExpressive) {
                            colors.surfaceContainerHigh.copy(alpha = 0.78f)
                        } else {
                            colors.surfaceContainer.copy(alpha = 0.7f)
                        },
                    )
                    .padding(2.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                ModeChip(
                    label = stringResource(R.string.native_widget_mode_timer),
                    selected = state.mode == ClockToolMode.TIMER,
                    isExpressive = isExpressive,
                    expressiveMotion = expressiveMotion && isVisible,
                    onClick = { onSetMode(ClockToolMode.TIMER) },
                )
                ModeChip(
                    label = stringResource(R.string.native_widget_mode_stopwatch),
                    selected = state.mode == ClockToolMode.STOPWATCH,
                    isExpressive = isExpressive,
                    expressiveMotion = expressiveMotion && isVisible,
                    onClick = { onSetMode(ClockToolMode.STOPWATCH) },
                )
            }

            if (state.mode == ClockToolMode.TIMER && spanY >= 2 && spanX >= 2) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    for (minutes in listOf(1, 5, 15)) {
                        val ms = minutes * 60_000L
                        val active = state.timerDurationMs == ms
                        Box(
                            modifier = Modifier
                                .clip(if (isExpressive) shapes.pill else shapes.small)
                                .background(
                                    if (active) colors.accentContainer else colors.surfaceContainer.copy(alpha = 0.6f),
                                )
                                .clickable { onSetTimerDuration(ms) }
                                .padding(horizontal = 7.dp, vertical = 3.dp),
                        ) {
                            Text(
                                text = "${minutes}m",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = if (active) colors.onAccentContainer else colors.onSurfaceMuted,
                            )
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(
                    text = formattedClock,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontSize = if (spanY <= 1) 24.sp else 36.sp,
                        letterSpacing = (-0.8).sp,
                        fontWeight = if (isExpressive) FontWeight.ExtraBold else FontWeight.Bold,
                        fontFeatureSettings = "tnum",
                    ),
                    color = if (timerFinished || (isExpressive && isRunning)) colors.accent else colors.onSurface,
                    modifier = Modifier.graphicsLayer {
                        scaleX = toggleScale.value
                        scaleY = toggleScale.value
                    },
                )
                if (timerFinished) {
                    Text(
                        text = stringResource(R.string.native_widget_timer_done),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = colors.accent,
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (spanX >= 2) {
                    NativeWidgetActionPill(
                        label = stringResource(R.string.native_widget_timer_reset),
                        emphasized = false,
                        onClick = {
                            if (state.mode == ClockToolMode.TIMER) onResetTimer() else onResetStopwatch()
                        },
                    )
                }
                if (isExpressive) {
                    Box(
                        modifier = Modifier
                            .size(if (spanY <= 1) 34.dp else 40.dp)
                            .clip(RoundedCornerShape(actionCorner))
                            .background(if (isRunning) colors.accent else colors.accentContainer)
                            .clickable(role = Role.Button) {
                                if (state.mode == ClockToolMode.TIMER) onToggleTimer() else onToggleStopwatch()
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        PlayPauseGlyph(
                            isPlaying = isRunning,
                            tint = if (isRunning) colors.onAccent else colors.onAccentContainer,
                            size = 16.dp,
                        )
                    }
                } else {
                    NativeWidgetActionPill(
                        label = stringResource(
                            if (isRunning) R.string.native_widget_timer_pause else R.string.native_widget_timer_start,
                        ),
                        emphasized = true,
                        onClick = {
                            if (state.mode == ClockToolMode.TIMER) onToggleTimer() else onToggleStopwatch()
                        },
                    )
                }
            }
        }

        if (state.mode == ClockToolMode.TIMER && (spanY >= 2 || isRunning)) {
            if (isExpressive) {
                ExpressiveWavyProgressBar(
                    progress = timerFraction,
                    color = colors.accent,
                    trackColor = colors.surfaceContainerHigh.copy(alpha = 0.65f),
                    animateWave = isRunning && isVisible && expressiveMotion,
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(shapes.small)
                        .background(colors.surfaceContainerHigh.copy(alpha = 0.65f)),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(timerFraction)
                            .fillMaxHeight()
                            .clip(shapes.small)
                            .background(colors.accent),
                    )
                }
            }
        }
    }
}

@Composable
private fun ModeChip(
    label: String,
    selected: Boolean,
    isExpressive: Boolean,
    expressiveMotion: Boolean,
    onClick: () -> Unit,
) {
    val colors = OverviewTheme.colors
    val shapes = OverviewTheme.shapes
    val motion = OverviewTheme.motion
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()

    val corner by animateDpAsState(
        targetValue = when {
            isExpressive && pressed -> 8.dp
            isExpressive -> 999.dp
            else -> shapes.smallRadius
        },
        animationSpec = if (expressiveMotion) motion.expressiveSpec() else snap(),
        label = "modeChipCorner",
    )
    val bg by animateColorAsState(
        targetValue = if (selected) {
            if (isExpressive) colors.accent else colors.surfaceElevated
        } else {
            colors.surface.copy(alpha = 0f)
        },
        animationSpec = if (expressiveMotion) motion.effectSpec(140) else snap(),
        label = "modeChipBg",
    )
    val chipShape = RoundedCornerShape(corner)

    Box(
        modifier = Modifier
            .clip(chipShape)
            .background(bg)
            .clickable(
                interactionSource = interaction,
                indication = null,
                role = Role.Tab,
                onClick = onClick,
            )
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            ),
            color = when {
                selected && isExpressive -> colors.onAccent
                selected -> colors.onSurface
                else -> colors.onSurfaceMuted
            },
        )
    }
}

private fun formatDurationClock(millis: Long, showTenths: Boolean): String {
    val totalSeconds = millis / 1000L
    val hours = totalSeconds / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L
    return when {
        hours > 0 -> String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, seconds)
        showTenths -> {
            val tenths = (millis % 1000L) / 100L
            String.format(Locale.getDefault(), "%02d:%02d.%d", minutes, seconds, tenths)
        }
        else -> String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
    }
}
