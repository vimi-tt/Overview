package io.github.vimitt.overview.ui.home.widgets

import android.os.SystemClock
import android.text.format.DateFormat
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
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
import io.github.vimitt.overview.ui.theme.OverviewTheme
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Widget 1: Relógio + data + próximo alarme, com layout distinto entre GNOME e Material Expressive.
 */
@Composable
fun ClockAlarmNativeWidget(
    currentTimeMillis: Long,
    nextAlarmMillis: Long?,
    spanY: Int,
    isVisible: Boolean,
    onOpenAlarms: () -> Unit,
) {
    val context = LocalContext.current
    val colors = OverviewTheme.colors
    val shapes = OverviewTheme.shapes
    val motion = OverviewTheme.motion
    val isExpressive = OverviewTheme.components.isExpressive
    val is24Hour = DateFormat.is24HourFormat(context)
    val locale = remember { Locale.getDefault() }

    val nowDate = remember(currentTimeMillis) { Date(currentTimeMillis) }
    val timeText = remember(nowDate, is24Hour, locale) {
        SimpleDateFormat(if (is24Hour) "HH:mm" else "h:mm", locale).format(nowDate)
    }
    val dateText = remember(nowDate, locale) {
        val pattern = DateFormat.getBestDateTimePattern(locale, "EEEEdMMM")
        SimpleDateFormat(pattern, locale).format(nowDate)
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
    }
    val alarmText = remember(nextAlarmMillis, is24Hour, locale) {
        nextAlarmMillis?.let { ts ->
            val pattern = DateFormat.getBestDateTimePattern(
                locale,
                if (is24Hour) "EEEHm" else "EEEhm",
            )
            SimpleDateFormat(pattern, locale).format(Date(ts))
        }
    }

    // Pulso discreto no modo Expressive somente na virada de minuto e enquanto visível.
    val expressiveTick = remember { Animatable(1f) }
    val minuteKey = currentTimeMillis / 60_000L
    var lastMinuteKey by remember { mutableLongStateOf(minuteKey) }
    LaunchedEffect(minuteKey, isVisible, isExpressive, motion.usesSprings, motion.reducedMotion) {
        val enabled = isVisible && isExpressive && motion.usesSprings && !motion.reducedMotion
        if (!enabled) {
            lastMinuteKey = minuteKey
            if (expressiveTick.value != 1f) expressiveTick.snapTo(1f)
            return@LaunchedEffect
        }
        if (minuteKey != lastMinuteKey) {
            lastMinuteKey = minuteKey
            expressiveTick.snapTo(1.035f)
            expressiveTick.animateTo(1f, animationSpec = motion.expressiveSpec())
        }
    }

    if (spanY <= 1) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(role = Role.Button, onClick = onOpenAlarms),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = timeText,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = if (isExpressive) FontWeight.ExtraBold else FontWeight.SemiBold,
                        fontFeatureSettings = "tnum",
                    ),
                    color = colors.onSurface,
                    modifier = Modifier.graphicsLayer {
                        val s = expressiveTick.value
                        scaleX = s
                        scaleY = s
                    },
                )
                Text(
                    text = dateText,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            AlarmBadge(
                alarmText = alarmText,
                isExpressive = isExpressive,
            )
        }
    } else if (isExpressive) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .clickable(role = Role.Button, onClick = onOpenAlarms),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Box(
                    modifier = Modifier
                        .clip(shapes.pill)
                        .background(colors.accentContainer)
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = dateText,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = colors.onAccentContainer,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                AlarmBadge(alarmText = alarmText, isExpressive = true)
            }
            Text(
                text = timeText,
                style = MaterialTheme.typography.displaySmall.copy(
                    fontSize = 44.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFeatureSettings = "tnum",
                ),
                color = colors.onSurface,
                modifier = Modifier.graphicsLayer {
                    val s = expressiveTick.value
                    scaleX = s
                    scaleY = s
                },
            )
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .clickable(role = Role.Button, onClick = onOpenAlarms),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = timeText,
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontSize = 38.sp,
                        fontWeight = FontWeight.Medium,
                        fontFeatureSettings = "tnum",
                    ),
                    color = colors.onSurface,
                )
                Text(
                    text = dateText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(bottom = 6.dp),
                )
            }
            HorizontalDivider(thickness = 1.dp, color = colors.hairlineBorder)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AlarmBellGlyph(
                    tint = if (alarmText != null) colors.accent else colors.onSurfaceMuted,
                    size = 15.dp,
                )
                Text(
                    text = alarmText?.let {
                        "${stringResource(R.string.native_widget_next_alarm_prefix)} · $it"
                    } ?: stringResource(R.string.native_widget_no_alarm),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (alarmText != null) colors.onSurface else colors.onSurfaceMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun AlarmBadge(
    alarmText: String?,
    isExpressive: Boolean,
) {
    val colors = OverviewTheme.colors
    val shapes = OverviewTheme.shapes
    val badgeShape = if (isExpressive) shapes.pill else shapes.small

    Row(
        modifier = Modifier
            .clip(badgeShape)
            .background(
                when {
                    alarmText != null && isExpressive -> colors.surfaceContainerHigh.copy(alpha = 0.85f)
                    alarmText != null -> colors.surfaceElevated
                    else -> colors.surfaceContainer.copy(alpha = 0.45f)
                },
            )
            .then(
                if (!isExpressive) Modifier.border(1.dp, colors.hairlineBorder, badgeShape) else Modifier,
            )
            .padding(horizontal = 9.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        AlarmBellGlyph(
            tint = if (alarmText != null) colors.accent else colors.onSurfaceMuted,
            size = 14.dp,
        )
        Text(
            text = alarmText ?: stringResource(R.string.native_widget_no_alarm),
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = if (isExpressive) FontWeight.SemiBold else FontWeight.Medium,
            ),
            color = if (alarmText != null) colors.onSurface else colors.onSurfaceMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * Widget 2: Resumo do dia estilo At a Glance numa única linha (evento, clima e alarme).
 */
@Composable
fun DailySummaryNativeWidget(
    nextEvent: CalendarEventItem?,
    weather: WeatherSnapshot?,
    nextAlarmMillis: Long?,
    currentTimeMillis: Long,
    onOpenEvent: (CalendarEventItem?) -> Unit,
    onOpenWeather: () -> Unit,
    onOpenAlarms: () -> Unit,
) {
    val context = LocalContext.current
    val colors = OverviewTheme.colors
    val shapes = OverviewTheme.shapes
    val isExpressive = OverviewTheme.components.isExpressive
    val is24Hour = DateFormat.is24HourFormat(context)
    val locale = remember { Locale.getDefault() }

    val datePrefix = remember(currentTimeMillis, locale) {
        val pattern = DateFormat.getBestDateTimePattern(locale, "EEEEdMMM")
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

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(if (isExpressive) 8.dp else 6.dp),
    ) {
        val leadText = if (nextEvent != null) {
            if (!eventTimeLabel.isNullOrBlank()) "$eventTimeLabel · ${nextEvent.title}" else nextEvent.title
        } else {
            datePrefix
        }
        Row(
            modifier = Modifier
                .weight(1f)
                .clip(if (isExpressive) shapes.pill else shapes.small)
                .clickable(role = Role.Button) { onOpenEvent(nextEvent) }
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = leadText,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (isExpressive) FontWeight.SemiBold else FontWeight.Medium,
                ),
                color = colors.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        // Clima na mesma linha
        Row(
            modifier = Modifier
                .clip(if (isExpressive) shapes.pill else shapes.small)
                .then(
                    if (isExpressive) {
                        Modifier.background(colors.accentContainer)
                    } else {
                        Modifier
                            .background(colors.surfaceElevated)
                            .border(1.dp, colors.hairlineBorder, shapes.small)
                    },
                )
                .clickable(role = Role.Button, onClick = onOpenWeather)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (weather != null) {
                WeatherGlyph(
                    weatherCode = weather.weatherCode,
                    isDay = weather.isDay,
                    tint = if (isExpressive) colors.onAccentContainer else colors.onSurfaceMuted,
                    accent = colors.accent,
                    size = 15.dp,
                )
                Text(
                    text = "${weather.temperatureC.roundToInt()}°",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = if (isExpressive) colors.onAccentContainer else colors.onSurface,
                )
            } else {
                Text(
                    text = stringResource(R.string.native_widget_summary_tap_weather),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isExpressive) colors.onAccentContainer else colors.onSurfaceMuted,
                )
            }
        }

        // Alarme na mesma linha
        if (alarmShort != null) {
            Row(
                modifier = Modifier
                    .clip(if (isExpressive) shapes.pill else shapes.small)
                    .then(
                        if (isExpressive) {
                            Modifier.background(colors.surfaceContainerHigh.copy(alpha = 0.8f))
                        } else {
                            Modifier
                                .background(colors.surfaceElevated)
                                .border(1.dp, colors.hairlineBorder, shapes.small)
                        },
                    )
                    .clickable(role = Role.Button, onClick = onOpenAlarms)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                AlarmBellGlyph(tint = colors.accent, size = 13.dp)
                Text(
                    text = alarmShort,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Medium,
                        fontFeatureSettings = "tnum",
                    ),
                    color = colors.onSurface,
                )
            }
        }
    }
}

/**
 * Widget 10: Timer e cronômetro diretamente na Home, com ticker ativo apenas enquanto visível e em execução.
 */
@Composable
fun TimerStopwatchNativeWidget(
    state: TimerStopwatchState,
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
    val isExpressive = OverviewTheme.components.isExpressive

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

    Column(
        modifier = Modifier.fillMaxWidth().fillMaxHeight(),
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
                    .background(colors.surfaceContainer.copy(alpha = 0.7f))
                    .padding(2.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                ModeChip(
                    label = stringResource(R.string.native_widget_mode_timer),
                    selected = state.mode == ClockToolMode.TIMER,
                    isExpressive = isExpressive,
                    onClick = { onSetMode(ClockToolMode.TIMER) },
                )
                ModeChip(
                    label = stringResource(R.string.native_widget_mode_stopwatch),
                    selected = state.mode == ClockToolMode.STOPWATCH,
                    isExpressive = isExpressive,
                    onClick = { onSetMode(ClockToolMode.STOPWATCH) },
                )
            }

            if (state.mode == ClockToolMode.TIMER && spanY >= 2) {
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
                        fontSize = if (spanY <= 1) 24.sp else 32.sp,
                        fontWeight = if (isExpressive) FontWeight.ExtraBold else FontWeight.SemiBold,
                        fontFeatureSettings = "tnum",
                    ),
                    color = if (timerFinished) colors.accent else colors.onSurface,
                )
                if (timerFinished) {
                    Text(
                        text = stringResource(R.string.native_widget_timer_done),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = colors.accent,
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                NativeWidgetActionPill(
                    label = stringResource(R.string.native_widget_timer_reset),
                    emphasized = false,
                    onClick = {
                        if (state.mode == ClockToolMode.TIMER) onResetTimer() else onResetStopwatch()
                    },
                )
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

        if (state.mode == ClockToolMode.TIMER && spanY >= 2) {
            val fraction = if (state.timerDurationMs > 0L) {
                (timerRemaining.toFloat() / state.timerDurationMs.toFloat()).coerceIn(0f, 1f)
            } else {
                0f
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (isExpressive) 8.dp else 4.dp)
                    .clip(if (isExpressive) CircleShape else shapes.small)
                    .background(colors.surfaceContainerHigh.copy(alpha = 0.65f)),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction)
                        .fillMaxHeight()
                        .clip(if (isExpressive) CircleShape else shapes.small)
                        .background(colors.accent),
                )
            }
        }
    }
}

@Composable
private fun ModeChip(
    label: String,
    selected: Boolean,
    isExpressive: Boolean,
    onClick: () -> Unit,
) {
    val colors = OverviewTheme.colors
    val shapes = OverviewTheme.shapes
    val chipShape = if (isExpressive) shapes.pill else shapes.small

    Box(
        modifier = Modifier
            .clip(chipShape)
            .background(
                if (selected) {
                    if (isExpressive) colors.accentContainer else colors.surfaceElevated
                } else {
                    colors.surface.copy(alpha = 0f)
                },
            )
            .clickable(role = Role.Tab, onClick = onClick)
            .padding(horizontal = 9.dp, vertical = 4.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            ),
            color = when {
                selected && isExpressive -> colors.onAccentContainer
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
