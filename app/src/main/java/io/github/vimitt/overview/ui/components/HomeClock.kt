package io.github.vimitt.overview.ui.components

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.text.format.DateFormat
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.vimitt.overview.R
import io.github.vimitt.overview.domain.model.HomeClockStyle
import io.github.vimitt.overview.ui.theme.OverviewCornerRadius
import io.github.vimitt.overview.ui.theme.OverviewTheme
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

/**
 * Relógio configurável da tela inicial e da pré-visualização nas configurações.
 * Reage a `ACTION_TIME_TICK`, mudanças manuais de horário e fuso horário sem polling.
 */
@Composable
fun HomeClock(
    style: HomeClockStyle,
    compact: Boolean,
    modifier: Modifier = Modifier,
    useWallpaperColors: Boolean = true,
) {
    val context = LocalContext.current
    val colors = OverviewTheme.colors
    var currentTimeMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }

    DisposableEffect(context) {
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
        context.registerReceiver(receiver, filter)
        onDispose {
            context.unregisterReceiver(receiver)
        }
    }

    val now = remember(currentTimeMillis) { Date(currentTimeMillis) }
    val is24Hour = DateFormat.is24HourFormat(context)
    val locale = remember { Locale.getDefault() }

    val timeText = remember(now, is24Hour, locale) {
        val pattern = if (is24Hour) "HH:mm" else "h:mm"
        SimpleDateFormat(pattern, locale).format(now)
    }
    val dateFullText = remember(now, locale) {
        val pattern = DateFormat.getBestDateTimePattern(locale, "EEEEdMMMM")
        SimpleDateFormat(pattern, locale).format(now)
    }
    val weekdayText = remember(now, locale) {
        SimpleDateFormat("EEEE", locale).format(now).uppercase(locale)
    }
    val dayMonthText = remember(now, locale) {
        val pattern = DateFormat.getBestDateTimePattern(locale, "dMMMM")
        SimpleDateFormat(pattern, locale).format(now)
    }
    val shortDateText = remember(now, locale) {
        val pattern = DateFormat.getBestDateTimePattern(locale, "EEEdMMM")
        SimpleDateFormat(pattern, locale).format(now)
    }
    val calendar = remember(now) {
        Calendar.getInstance().apply { time = now }
    }

    val primaryForeground = if (useWallpaperColors) {
        colors.wallpaperForeground
    } else {
        colors.onSurface
    }
    val secondaryForeground = if (useWallpaperColors) {
        colors.wallpaperForeground.copy(alpha = 0.88f)
    } else {
        colors.onSurfaceMuted
    }
    val textShadow = if (useWallpaperColors) {
        Shadow(
            color = colors.wallpaperShadow,
            offset = Offset(0f, 2f),
            blurRadius = 10f,
        )
    } else {
        null
    }

    when (style) {
        HomeClockStyle.GNOME -> GnomeClassicClock(
            timeText = timeText,
            dateText = dateFullText,
            compact = compact,
            primaryColor = primaryForeground,
            secondaryColor = secondaryForeground,
            shadow = textShadow,
            modifier = modifier,
        )

        HomeClockStyle.STACKED -> StackedClock(
            calendar = calendar,
            is24Hour = is24Hour,
            shortDateText = shortDateText,
            compact = compact,
            primaryColor = primaryForeground,
            accentColor = colors.accent,
            secondaryColor = secondaryForeground,
            shadow = textShadow,
            modifier = modifier,
        )

        HomeClockStyle.ANALOG -> AnalogClock(
            calendar = calendar,
            dateText = shortDateText,
            compact = compact,
            primaryColor = primaryForeground,
            secondaryColor = secondaryForeground,
            accentColor = colors.accent,
            dialFill = if (useWallpaperColors) {
                colors.surface.copy(alpha = 0.28f)
            } else {
                colors.surfaceElevated
            },
            dialBorder = if (useWallpaperColors) {
                colors.wallpaperForeground.copy(alpha = 0.24f)
            } else {
                colors.hairlineBorder
            },
            shadow = textShadow,
            modifier = modifier,
        )

        HomeClockStyle.SIDE_BY_SIDE -> SideBySideClock(
            timeText = timeText,
            weekdayText = weekdayText,
            dayMonthText = dayMonthText,
            compact = compact,
            primaryColor = primaryForeground,
            secondaryColor = secondaryForeground,
            accentColor = colors.accent,
            shadow = textShadow,
            modifier = modifier,
        )

        HomeClockStyle.PILL -> PillClock(
            timeText = timeText,
            shortDateText = shortDateText,
            compact = compact,
            primaryColor = primaryForeground,
            secondaryColor = secondaryForeground,
            accentColor = colors.accent,
            containerColor = if (useWallpaperColors) {
                colors.surface.copy(alpha = 0.56f)
            } else {
                colors.surfaceElevated
            },
            borderColor = colors.hairlineBorder,
            modifier = modifier,
        )

        HomeClockStyle.FULL_PILL -> {
            val formattedDate = remember(dateFullText, locale) {
                dateFullText.replaceFirstChar { ch ->
                    if (ch.isLowerCase()) ch.titlecase(locale) else ch.toString()
                }
            }
            val hourOfDay = calendar.get(Calendar.HOUR_OF_DAY)
            val greeting = when (hourOfDay) {
                in 5..11 -> stringResource(R.string.clock_greeting_morning)
                in 12..17 -> stringResource(R.string.clock_greeting_afternoon)
                else -> stringResource(R.string.clock_greeting_evening)
            }
            FullPillSummaryClock(
                timeText = timeText,
                dateText = if (compact) shortDateText else formattedDate,
                greeting = greeting,
                compact = compact,
                primaryColor = primaryForeground,
                secondaryColor = secondaryForeground,
                accentColor = colors.accent,
                containerColor = if (useWallpaperColors) {
                    colors.surface.copy(alpha = 0.56f)
                } else {
                    colors.surfaceElevated
                },
                borderColor = colors.hairlineBorder,
                modifier = modifier,
            )
        }
    }
}

@Composable
private fun GnomeClassicClock(
    timeText: String,
    dateText: String,
    compact: Boolean,
    primaryColor: Color,
    secondaryColor: Color,
    shadow: Shadow?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = timeText,
            style = if (compact) {
                MaterialTheme.typography.displayLarge.copy(
                    fontSize = 38.sp,
                    lineHeight = 42.sp,
                    fontFeatureSettings = "tnum",
                    shadow = shadow,
                )
            } else {
                MaterialTheme.typography.displayLarge.copy(
                    fontFeatureSettings = "tnum",
                    shadow = shadow,
                )
            },
            color = primaryColor,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = dateText,
            style = (if (compact) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.titleMedium)
                .copy(shadow = shadow),
            color = secondaryColor,
        )
    }
}

@Composable
private fun StackedClock(
    calendar: Calendar,
    is24Hour: Boolean,
    shortDateText: String,
    compact: Boolean,
    primaryColor: Color,
    accentColor: Color,
    secondaryColor: Color,
    shadow: Shadow?,
    modifier: Modifier = Modifier,
) {
    val rawHour = if (is24Hour) {
        calendar.get(Calendar.HOUR_OF_DAY)
    } else {
        calendar.get(Calendar.HOUR).let { if (it == 0) 12 else it }
    }
    val hourText = rawHour.toString().padStart(2, '0')
    val minuteText = calendar.get(Calendar.MINUTE).toString().padStart(2, '0')
    val digitSize = if (compact) 36.sp else 58.sp
    val digitLineHeight = if (compact) 34.sp else 54.sp

    if (compact) {
        Row(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = hourText,
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontSize = digitSize,
                        lineHeight = digitLineHeight,
                        fontWeight = FontWeight.Bold,
                        fontFeatureSettings = "tnum",
                        shadow = shadow,
                    ),
                    color = primaryColor,
                )
                Text(
                    text = ":",
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontSize = digitSize,
                        lineHeight = digitLineHeight,
                        fontWeight = FontWeight.Bold,
                        shadow = shadow,
                    ),
                    color = accentColor,
                )
                Text(
                    text = minuteText,
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontSize = digitSize,
                        lineHeight = digitLineHeight,
                        fontWeight = FontWeight.Bold,
                        fontFeatureSettings = "tnum",
                        shadow = shadow,
                    ),
                    color = accentColor,
                )
            }
            Text(
                text = shortDateText,
                style = MaterialTheme.typography.bodyMedium.copy(shadow = shadow),
                color = secondaryColor,
            )
        }
    } else {
        Column(
            modifier = modifier,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = hourText,
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = digitSize,
                    lineHeight = digitLineHeight,
                    fontWeight = FontWeight.Bold,
                    fontFeatureSettings = "tnum",
                    shadow = shadow,
                ),
                color = primaryColor,
            )
            Text(
                text = minuteText,
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = digitSize,
                    lineHeight = digitLineHeight,
                    fontWeight = FontWeight.Bold,
                    fontFeatureSettings = "tnum",
                    shadow = shadow,
                ),
                color = accentColor,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = shortDateText,
                style = MaterialTheme.typography.titleMedium.copy(shadow = shadow),
                color = secondaryColor,
            )
        }
    }
}

@Composable
private fun AnalogClock(
    calendar: Calendar,
    dateText: String,
    compact: Boolean,
    primaryColor: Color,
    secondaryColor: Color,
    accentColor: Color,
    dialFill: Color,
    dialBorder: Color,
    shadow: Shadow?,
    modifier: Modifier = Modifier,
) {
    val dialSize: Dp = if (compact) 74.dp else 132.dp
    val hour12 = calendar.get(Calendar.HOUR)
    val minute = calendar.get(Calendar.MINUTE)
    val hourAngleDeg = ((hour12 + minute / 60f) / 12f) * 360f
    val minuteAngleDeg = (minute / 60f) * 360f

    if (compact) {
        Row(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AnalogDial(
                dialSize = dialSize,
                hourAngleDeg = hourAngleDeg,
                minuteAngleDeg = minuteAngleDeg,
                primaryColor = primaryColor,
                accentColor = accentColor,
                dialFill = dialFill,
                dialBorder = dialBorder,
            )
            Text(
                text = dateText,
                style = MaterialTheme.typography.titleSmall.copy(shadow = shadow),
                color = secondaryColor,
            )
        }
    } else {
        Column(
            modifier = modifier,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AnalogDial(
                dialSize = dialSize,
                hourAngleDeg = hourAngleDeg,
                minuteAngleDeg = minuteAngleDeg,
                primaryColor = primaryColor,
                accentColor = accentColor,
                dialFill = dialFill,
                dialBorder = dialBorder,
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = dateText,
                style = MaterialTheme.typography.titleMedium.copy(shadow = shadow),
                color = secondaryColor,
            )
        }
    }
}

@Composable
private fun AnalogDial(
    dialSize: Dp,
    hourAngleDeg: Float,
    minuteAngleDeg: Float,
    primaryColor: Color,
    accentColor: Color,
    dialFill: Color,
    dialBorder: Color,
) {
    Canvas(modifier = Modifier.size(dialSize)) {
        val radius = size.minDimension / 2f
        val center = Offset(size.width / 2f, size.height / 2f)

        drawCircle(
            color = dialFill,
            radius = radius,
            center = center,
        )
        drawCircle(
            color = dialBorder,
            radius = radius - 1.dp.toPx(),
            center = center,
            style = Stroke(width = 1.5.dp.toPx()),
        )

        // Marcações das 12 horas com destaque discreto às 12h.
        for (tick in 0 until 12) {
            val angleRad = Math.toRadians((tick * 30.0) - 90.0)
            val isQuarter = tick % 3 == 0
            val outer = radius * 0.88f
            val inner = radius * if (isQuarter) 0.74f else 0.80f
            val start = Offset(
                x = center.x + (cos(angleRad) * inner).toFloat(),
                y = center.y + (sin(angleRad) * inner).toFloat(),
            )
            val end = Offset(
                x = center.x + (cos(angleRad) * outer).toFloat(),
                y = center.y + (sin(angleRad) * outer).toFloat(),
            )
            drawLine(
                color = if (tick == 0) accentColor else primaryColor.copy(alpha = if (isQuarter) 0.78f else 0.42f),
                start = start,
                end = end,
                strokeWidth = if (isQuarter) 2.2.dp.toPx() else 1.2.dp.toPx(),
                cap = StrokeCap.Round,
            )
        }

        val hourRad = Math.toRadians(hourAngleDeg.toDouble() - 90.0)
        val hourEnd = Offset(
            x = center.x + (cos(hourRad) * radius * 0.48f).toFloat(),
            y = center.y + (sin(hourRad) * radius * 0.48f).toFloat(),
        )
        drawLine(
            color = primaryColor,
            start = center,
            end = hourEnd,
            strokeWidth = (dialSize * 0.048f).toPx().coerceAtLeast(2.5f),
            cap = StrokeCap.Round,
        )

        val minuteRad = Math.toRadians(minuteAngleDeg.toDouble() - 90.0)
        val minuteEnd = Offset(
            x = center.x + (cos(minuteRad) * radius * 0.70f).toFloat(),
            y = center.y + (sin(minuteRad) * radius * 0.70f).toFloat(),
        )
        drawLine(
            color = accentColor,
            start = center,
            end = minuteEnd,
            strokeWidth = (dialSize * 0.032f).toPx().coerceAtLeast(1.8f),
            cap = StrokeCap.Round,
        )

        drawCircle(
            color = accentColor,
            radius = (dialSize * 0.046f).toPx().coerceAtLeast(3f),
            center = center,
        )
    }
}

@Composable
private fun SideBySideClock(
    timeText: String,
    weekdayText: String,
    dayMonthText: String,
    compact: Boolean,
    primaryColor: Color,
    secondaryColor: Color,
    accentColor: Color,
    shadow: Shadow?,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(if (compact) 12.dp else 16.dp),
    ) {
        Text(
            text = timeText,
            style = MaterialTheme.typography.displayLarge.copy(
                fontSize = if (compact) 34.sp else 48.sp,
                lineHeight = if (compact) 38.sp else 52.sp,
                fontWeight = FontWeight.SemiBold,
                fontFeatureSettings = "tnum",
                shadow = shadow,
            ),
            color = primaryColor,
        )

        Box(
            modifier = Modifier
                .width(3.dp)
                .height(if (compact) 30.dp else 44.dp)
                .clip(RoundedCornerShape(OverviewCornerRadius.Pill))
                .background(accentColor),
        )

        Column(verticalArrangement = Arrangement.Center) {
            Text(
                text = weekdayText,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.1.sp,
                    shadow = shadow,
                ),
                color = accentColor,
            )
            Text(
                text = dayMonthText,
                style = (if (compact) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.titleSmall)
                    .copy(shadow = shadow),
                color = secondaryColor,
            )
        }
    }
}

@Composable
private fun PillClock(
    timeText: String,
    shortDateText: String,
    compact: Boolean,
    primaryColor: Color,
    secondaryColor: Color,
    accentColor: Color,
    containerColor: Color,
    borderColor: Color,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(OverviewCornerRadius.Pill)

    Row(
        modifier = modifier
            .clip(shape)
            .background(containerColor)
            .border(1.dp, borderColor, shape)
            .padding(
                horizontal = if (compact) 14.dp else 18.dp,
                vertical = if (compact) 7.dp else 10.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(accentColor),
        )
        Text(
            text = timeText,
            style = (if (compact) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleMedium)
                .copy(
                    fontWeight = FontWeight.SemiBold,
                    fontFeatureSettings = "tnum",
                ),
            color = primaryColor,
        )
        Text(
            text = "·",
            style = MaterialTheme.typography.titleSmall,
            color = secondaryColor,
        )
        Text(
            text = shortDateText,
            style = if (compact) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium,
            color = secondaryColor,
        )
    }
}

@Composable
private fun FullPillSummaryClock(
    timeText: String,
    dateText: String,
    greeting: String,
    compact: Boolean,
    primaryColor: Color,
    secondaryColor: Color,
    accentColor: Color,
    containerColor: Color,
    borderColor: Color,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(OverviewCornerRadius.Pill)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(containerColor)
            .border(1.dp, borderColor, shape)
            .padding(
                horizontal = if (compact) 18.dp else 22.dp,
                vertical = if (compact) 11.dp else 15.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = dateText,
                style = (if (compact) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleMedium)
                    .copy(fontWeight = FontWeight.SemiBold),
                color = primaryColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(accentColor),
                )
                Text(
                    text = greeting,
                    style = if (compact) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium,
                    color = secondaryColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Text(
            text = timeText,
            style = MaterialTheme.typography.headlineMedium.copy(
                fontSize = if (compact) 22.sp else 28.sp,
                lineHeight = if (compact) 26.sp else 32.sp,
                fontWeight = FontWeight.SemiBold,
                fontFeatureSettings = "tnum",
            ),
            color = primaryColor,
        )
    }
}
