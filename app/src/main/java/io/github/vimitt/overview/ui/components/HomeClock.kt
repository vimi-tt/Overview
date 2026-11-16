package io.github.vimitt.overview.ui.components

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Matrix
import android.graphics.Path as AndroidPath
import android.text.format.DateFormat
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import io.github.vimitt.overview.R
import io.github.vimitt.overview.domain.model.HomeClockStyle
import io.github.vimitt.overview.domain.model.SettingsDefaults
import io.github.vimitt.overview.domain.model.WeatherLocation
import io.github.vimitt.overview.domain.model.WeatherSnapshot
import io.github.vimitt.overview.ui.home.widgets.WeatherGlyph
import io.github.vimitt.overview.ui.theme.OverviewTheme
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Informa se o ciclo de vida atual está ativo (`STARTED` ou superior).
 * Evita manter receptores de tempo e animações ativos fora da tela.
 */
@Composable
fun rememberIsLifecycleActive(): Boolean {
    val lifecycleOwner = LocalLifecycleOwner.current
    var isStarted by remember(lifecycleOwner) {
        mutableStateOf(
            lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED),
        )
    }
    DisposableEffect(lifecycleOwner) {
        isStarted = lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> isStarted = true
                Lifecycle.Event.ON_STOP -> isStarted = false
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    return isStarted
}

private data class ClockWeatherMeta(
    val savedLocation: WeatherLocation? = null,
    val isLoading: Boolean = false,
    val onRefreshWeatherClick: (() -> Unit)? = null,
)

private val LocalClockWeatherMeta = compositionLocalOf { ClockWeatherMeta() }

/**
 * Relógio configurável da tela inicial e da pré-visualização nas configurações.
 * Só registra `BroadcastReceiver` e executa animações Expressive quando `isVisible` é verdadeiro.
 */
@Composable
fun HomeClock(
    style: HomeClockStyle,
    compact: Boolean,
    modifier: Modifier = Modifier,
    useWallpaperColors: Boolean = true,
    backgroundAlpha: Float = SettingsDefaults.DEFAULT_CLOCK_ALPHA,
    isVisible: Boolean = true,
    weather: WeatherSnapshot? = null,
    weatherLocation: WeatherLocation? = null,
    isWeatherLoading: Boolean = false,
    showWeather: Boolean = true,
    onWeatherClick: (() -> Unit)? = null,
    onRefreshWeatherClick: (() -> Unit)? = null,
    onEnableLocationClick: (() -> Unit)? = null,
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

    val motion = OverviewTheme.motion
    val isExpressive = OverviewTheme.components.isExpressive
    val expressiveMotionEnabled = isExpressive && motion.usesSprings && !motion.reducedMotion
    val animateWhileVisible = expressiveMotionEnabled && isVisible

    val entranceScale by animateFloatAsState(
        targetValue = if (isVisible || !expressiveMotionEnabled) 1f else 0.94f,
        animationSpec = if (animateWhileVisible) motion.spatialSpec() else snap(),
        label = "clock_entrance_scale",
    )
    val entranceAlpha by animateFloatAsState(
        targetValue = if (isVisible || !expressiveMotionEnabled) 1f else 0f,
        animationSpec = if (animateWhileVisible) motion.effectSpec(durationMillis = 140) else snap(),
        label = "clock_entrance_alpha",
    )

    val minuteTickPulse = remember { Animatable(1f) }
    val minuteKey = remember(now) { currentTimeMillis / 60_000L }
    var lastAnimatedMinute by remember { mutableLongStateOf(minuteKey) }

    LaunchedEffect(minuteKey, animateWhileVisible, style) {
        if (!animateWhileVisible || style == HomeClockStyle.ANALOG) {
            lastAnimatedMinute = minuteKey
            if (minuteTickPulse.value != 1f) {
                minuteTickPulse.snapTo(1f)
            }
            return@LaunchedEffect
        }
        if (minuteKey != lastAnimatedMinute) {
            lastAnimatedMinute = minuteKey
            minuteTickPulse.snapTo(1.045f)
            minuteTickPulse.animateTo(
                targetValue = 1f,
                animationSpec = motion.expressiveSpec(),
            )
        }
    }

    val clockModifier = if (expressiveMotionEnabled) {
        modifier.graphicsLayer {
            val pulse = minuteTickPulse.value
            scaleX = entranceScale * pulse
            scaleY = entranceScale * pulse
            alpha = entranceAlpha
        }
    } else {
        modifier
    }

    val clampedAlpha = backgroundAlpha.coerceIn(
        SettingsDefaults.MIN_CLOCK_ALPHA,
        SettingsDefaults.MAX_CLOCK_ALPHA,
    )
    // Garante contraste quando o fundo translúcido passa a dominar sobre o papel de parede.
    val useSurfaceContrast = !useWallpaperColors || clampedAlpha >= 0.35f
    val cardPrimaryForeground = if (useSurfaceContrast) colors.onSurface else primaryForeground
    val cardSecondaryForeground = if (useSurfaceContrast) colors.onSurfaceMuted else secondaryForeground
    val cardFill = if (isExpressive) {
        val tonalBase = if (useWallpaperColors) {
            lerp(colors.surfaceContainerHigh, colors.accentContainer, 0.34f)
        } else {
            lerp(colors.surfaceElevated, colors.accentContainer, 0.28f)
        }
        tonalBase.copy(alpha = clampedAlpha)
    } else if (useWallpaperColors) {
        colors.surface.copy(alpha = clampedAlpha)
    } else {
        colors.surfaceElevated.copy(alpha = clampedAlpha)
    }
    val cardBorder = if (isExpressive) {
        colors.accent.copy(alpha = (0.36f * clampedAlpha).coerceIn(0f, 1f))
    } else {
        colors.hairlineBorder.copy(alpha = (clampedAlpha * 1.15f).coerceIn(0f, 1f))
    }

    val dateChipFill = if (useWallpaperColors) {
        lerp(colors.surfaceContainerHigh, colors.accentContainer, 0.38f).copy(alpha = 0.86f)
    } else {
        colors.accentContainer.copy(alpha = 0.92f)
    }
    val dateChipText = if (useWallpaperColors) colors.onSurface else colors.onAccentContainer
    val dateChipBorder = colors.accent.copy(alpha = 0.34f)

    val effectiveWeather = if (showWeather) weather else null
    val effectiveSavedLocation = if (showWeather) weatherLocation else null
    val canShowLocationAction = showWeather &&
        effectiveWeather == null &&
        effectiveSavedLocation == null &&
        onEnableLocationClick != null

    CompositionLocalProvider(
        LocalClockWeatherMeta provides ClockWeatherMeta(
            savedLocation = effectiveSavedLocation,
            isLoading = isWeatherLoading,
            onRefreshWeatherClick = onRefreshWeatherClick,
        ),
    ) {
    when (style) {
        HomeClockStyle.GNOME -> GnomeClassicClock(
            timeText = timeText,
            dateText = dateFullText,
            compact = compact,
            isExpressive = isExpressive,
            primaryColor = primaryForeground,
            secondaryColor = secondaryForeground,
            accentColor = colors.accent,
            chipFill = dateChipFill,
            chipTextColor = dateChipText,
            chipBorder = dateChipBorder,
            shadow = textShadow,
            weather = effectiveWeather,
            showLocationCta = canShowLocationAction,
            onWeatherClick = onWeatherClick,
            onEnableLocationClick = onEnableLocationClick,
            modifier = clockModifier,
        )

        HomeClockStyle.STACKED -> StackedClock(
            calendar = calendar,
            is24Hour = is24Hour,
            shortDateText = shortDateText,
            compact = compact,
            isExpressive = isExpressive,
            primaryColor = primaryForeground,
            accentColor = colors.accent,
            secondaryColor = secondaryForeground,
            chipFill = dateChipFill,
            chipTextColor = dateChipText,
            chipBorder = dateChipBorder,
            shadow = textShadow,
            weather = effectiveWeather,
            showLocationCta = canShowLocationAction,
            onWeatherClick = onWeatherClick,
            onEnableLocationClick = onEnableLocationClick,
            modifier = clockModifier,
        )

        HomeClockStyle.ANALOG -> AnalogClock(
            calendar = calendar,
            dateText = shortDateText,
            compact = compact,
            isExpressive = isExpressive,
            animateMotion = animateWhileVisible,
            handColor = cardPrimaryForeground,
            dateColor = secondaryForeground,
            accentColor = colors.accent,
            dialFill = cardFill,
            dialBorder = if (isExpressive) {
                colors.accent.copy(alpha = (0.38f + clampedAlpha * 0.4f).coerceIn(0f, 1f))
            } else if (useSurfaceContrast) {
                cardBorder
            } else {
                colors.wallpaperForeground.copy(alpha = (0.18f + clampedAlpha * 0.4f).coerceIn(0f, 1f))
            },
            chipFill = dateChipFill,
            chipTextColor = dateChipText,
            chipBorder = dateChipBorder,
            shadow = textShadow,
            weather = effectiveWeather,
            showLocationCta = canShowLocationAction,
            onWeatherClick = onWeatherClick,
            onEnableLocationClick = onEnableLocationClick,
            modifier = clockModifier,
        )

        HomeClockStyle.SIDE_BY_SIDE -> SideBySideClock(
            timeText = timeText,
            weekdayText = weekdayText,
            dayMonthText = dayMonthText,
            compact = compact,
            isExpressive = isExpressive,
            primaryColor = primaryForeground,
            secondaryColor = secondaryForeground,
            accentColor = colors.accent,
            accentContainer = colors.accentContainer,
            onAccentContainer = colors.onAccentContainer,
            chipFill = dateChipFill,
            chipTextColor = dateChipText,
            chipBorder = dateChipBorder,
            shadow = textShadow,
            weather = effectiveWeather,
            showLocationCta = canShowLocationAction,
            onWeatherClick = onWeatherClick,
            onEnableLocationClick = onEnableLocationClick,
            modifier = clockModifier,
        )

        HomeClockStyle.PILL -> PillClock(
            timeText = timeText,
            shortDateText = shortDateText,
            compact = compact,
            isExpressive = isExpressive,
            primaryColor = cardPrimaryForeground,
            secondaryColor = cardSecondaryForeground,
            accentColor = colors.accent,
            accentContainer = colors.accentContainer,
            onAccentContainer = colors.onAccentContainer,
            containerColor = cardFill,
            borderColor = cardBorder,
            shadowAlpha = clampedAlpha,
            weather = effectiveWeather,
            showLocationCta = canShowLocationAction,
            onWeatherClick = onWeatherClick,
            onEnableLocationClick = onEnableLocationClick,
            modifier = clockModifier,
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
                isExpressive = isExpressive,
                primaryColor = cardPrimaryForeground,
                secondaryColor = cardSecondaryForeground,
                accentColor = colors.accent,
                accentContainer = colors.accentContainer,
                onAccentContainer = colors.onAccentContainer,
                containerColor = cardFill,
                borderColor = cardBorder,
                shadowAlpha = clampedAlpha,
                weather = effectiveWeather,
                showLocationCta = canShowLocationAction,
                onWeatherClick = onWeatherClick,
                onEnableLocationClick = onEnableLocationClick,
                modifier = clockModifier,
            )
        }
    }
    }
}

@Composable
private fun ClockDateChip(
    text: String,
    compact: Boolean,
    fillColor: Color,
    textColor: Color,
    borderColor: Color,
) {
    val shape = OverviewTheme.shapes.pill
    Text(
        text = text,
        style = (if (compact) MaterialTheme.typography.labelMedium else MaterialTheme.typography.titleSmall)
            .copy(fontWeight = FontWeight.SemiBold),
        color = textColor,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .clip(shape)
            .background(fillColor)
            .border(width = 1.dp, color = borderColor, shape = shape)
            .padding(
                horizontal = if (compact) 10.dp else 14.dp,
                vertical = if (compact) 3.dp else 5.dp,
            ),
    )
}

@Composable
private fun ClockWeatherBadge(
    weather: WeatherSnapshot?,
    showLocationCta: Boolean,
    compact: Boolean,
    isExpressive: Boolean,
    textColor: Color,
    accentColor: Color,
    fillColor: Color,
    borderColor: Color,
    shadow: Shadow?,
    onWeatherClick: (() -> Unit)?,
    onEnableLocationClick: (() -> Unit)?,
) {
    val weatherMeta = LocalClockWeatherMeta.current
    val savedLocation = weatherMeta.savedLocation
    if (weather == null && savedLocation == null && !showLocationCta) return

    val shape = OverviewTheme.shapes.pill
    val clickAction = when {
        weather != null -> onWeatherClick
        savedLocation != null -> weatherMeta.onRefreshWeatherClick ?: onWeatherClick
        else -> onEnableLocationClick
    }
    val clickModifier = if (clickAction != null) {
        Modifier.clickable(role = Role.Button, onClick = clickAction)
    } else {
        Modifier
    }

    val badgeText = when {
        weather != null -> "${weather.temperatureC.roundToInt()}°"
        savedLocation != null -> if (weatherMeta.isLoading) {
            "${savedLocation.name}…"
        } else {
            savedLocation.name
        }
        else -> stringResource(R.string.clock_enable_location_cta)
    }

    if (isExpressive) {
        Row(
            modifier = Modifier
                .clip(shape)
                .background(fillColor)
                .border(width = 1.dp, color = borderColor, shape = shape)
                .then(clickModifier)
                .padding(
                    horizontal = if (compact) 8.dp else 11.dp,
                    vertical = if (compact) 3.dp else 4.dp,
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            if (weather != null) {
                WeatherGlyph(
                    weatherCode = weather.weatherCode,
                    isDay = weather.isDay,
                    tint = textColor,
                    accent = accentColor,
                    size = if (compact) 14.dp else 16.dp,
                )
            } else if (savedLocation != null) {
                WeatherGlyph(
                    weatherCode = 1,
                    isDay = true,
                    tint = textColor,
                    accent = accentColor,
                    size = if (compact) 14.dp else 16.dp,
                )
            } else {
                ClockLocationGlyph(tint = accentColor, size = if (compact) 12.dp else 14.dp)
            }
            Text(
                text = badgeText,
                style = (if (compact) MaterialTheme.typography.labelMedium else MaterialTheme.typography.labelLarge)
                    .copy(
                        fontWeight = FontWeight.Bold,
                        fontFeatureSettings = "tnum",
                    ),
                color = textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    } else {
        Row(
            modifier = Modifier
                .clip(shape)
                .then(clickModifier)
                .padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            if (weather != null) {
                WeatherGlyph(
                    weatherCode = weather.weatherCode,
                    isDay = weather.isDay,
                    tint = textColor,
                    accent = accentColor,
                    size = if (compact) 15.dp else 17.dp,
                )
            } else if (savedLocation != null) {
                WeatherGlyph(
                    weatherCode = 1,
                    isDay = true,
                    tint = textColor,
                    accent = accentColor,
                    size = if (compact) 15.dp else 17.dp,
                )
            } else {
                ClockLocationGlyph(tint = accentColor, size = if (compact) 13.dp else 15.dp)
            }
            Text(
                text = badgeText,
                style = (if (compact) MaterialTheme.typography.bodySmall else MaterialTheme.typography.titleSmall)
                    .copy(
                        fontWeight = FontWeight.Medium,
                        fontFeatureSettings = "tnum",
                        shadow = shadow,
                    ),
                color = textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun ClockLocationGlyph(
    tint: Color,
    size: Dp = 14.dp,
) {
    Canvas(modifier = Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = (w * 0.12f).coerceAtLeast(1.4f)
        val center = Offset(w * 0.5f, h * 0.42f)
        drawCircle(
            color = tint,
            radius = w * 0.28f,
            center = center,
            style = Stroke(width = stroke),
        )
        drawCircle(
            color = tint,
            radius = w * 0.10f,
            center = center,
        )
        drawLine(
            color = tint,
            start = Offset(w * 0.5f, h * 0.70f),
            end = Offset(w * 0.5f, h * 0.90f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
    }
}

@Composable
private fun GnomeClassicClock(
    timeText: String,
    dateText: String,
    compact: Boolean,
    isExpressive: Boolean,
    primaryColor: Color,
    secondaryColor: Color,
    accentColor: Color,
    chipFill: Color,
    chipTextColor: Color,
    chipBorder: Color,
    shadow: Shadow?,
    weather: WeatherSnapshot?,
    showLocationCta: Boolean,
    onWeatherClick: (() -> Unit)?,
    onEnableLocationClick: (() -> Unit)?,
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
                    fontWeight = if (isExpressive) FontWeight.Bold else MaterialTheme.typography.displayLarge.fontWeight,
                    letterSpacing = if (isExpressive) (-0.8).sp else 0.sp,
                    fontFeatureSettings = "tnum",
                    shadow = shadow,
                )
            } else {
                MaterialTheme.typography.displayLarge.copy(
                    fontWeight = if (isExpressive) FontWeight.Bold else MaterialTheme.typography.displayLarge.fontWeight,
                    letterSpacing = if (isExpressive) (-1.2).sp else 0.sp,
                    fontFeatureSettings = "tnum",
                    shadow = shadow,
                )
            },
            color = primaryColor,
        )
        Spacer(modifier = Modifier.height(if (isExpressive) 6.dp else 2.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (isExpressive) {
                ClockDateChip(
                    text = dateText,
                    compact = compact,
                    fillColor = chipFill,
                    textColor = chipTextColor,
                    borderColor = chipBorder,
                )
            } else {
                Text(
                    text = dateText,
                    style = (if (compact) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.titleMedium)
                        .copy(shadow = shadow),
                    color = secondaryColor,
                )
            }
            val weatherMeta = LocalClockWeatherMeta.current
            if (weather != null || weatherMeta.savedLocation != null || showLocationCta) {
                if (!isExpressive) {
                    Text(
                        text = "·",
                        style = (if (compact) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.titleMedium)
                            .copy(shadow = shadow),
                        color = secondaryColor,
                    )
                }
                ClockWeatherBadge(
                    weather = weather,
                    showLocationCta = showLocationCta,
                    compact = compact,
                    isExpressive = isExpressive,
                    textColor = if (isExpressive) chipTextColor else secondaryColor,
                    accentColor = accentColor,
                    fillColor = chipFill,
                    borderColor = chipBorder,
                    shadow = shadow,
                    onWeatherClick = onWeatherClick,
                    onEnableLocationClick = onEnableLocationClick,
                )
            }
        }
    }
}

@Composable
private fun StackedClock(
    calendar: Calendar,
    is24Hour: Boolean,
    shortDateText: String,
    compact: Boolean,
    isExpressive: Boolean,
    primaryColor: Color,
    accentColor: Color,
    secondaryColor: Color,
    chipFill: Color,
    chipTextColor: Color,
    chipBorder: Color,
    shadow: Shadow?,
    weather: WeatherSnapshot?,
    showLocationCta: Boolean,
    onWeatherClick: (() -> Unit)?,
    onEnableLocationClick: (() -> Unit)?,
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
    val digitWeight = if (isExpressive) FontWeight.ExtraBold else FontWeight.Bold
    val digitSpacing = if (isExpressive && !compact) (-1.5).sp else 0.sp

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
                        fontWeight = digitWeight,
                        letterSpacing = digitSpacing,
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
                        fontWeight = digitWeight,
                        shadow = shadow,
                    ),
                    color = accentColor,
                )
                Text(
                    text = minuteText,
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontSize = digitSize,
                        lineHeight = digitLineHeight,
                        fontWeight = digitWeight,
                        letterSpacing = digitSpacing,
                        fontFeatureSettings = "tnum",
                        shadow = shadow,
                    ),
                    color = accentColor,
                )
            }
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalAlignment = Alignment.Start,
            ) {
                if (isExpressive) {
                    ClockDateChip(
                        text = shortDateText,
                        compact = true,
                        fillColor = chipFill,
                        textColor = chipTextColor,
                        borderColor = chipBorder,
                    )
                } else {
                    Text(
                        text = shortDateText,
                        style = MaterialTheme.typography.bodyMedium.copy(shadow = shadow),
                        color = secondaryColor,
                    )
                }
                ClockWeatherBadge(
                    weather = weather,
                    showLocationCta = showLocationCta,
                    compact = true,
                    isExpressive = isExpressive,
                    textColor = if (isExpressive) chipTextColor else secondaryColor,
                    accentColor = accentColor,
                    fillColor = chipFill,
                    borderColor = chipBorder,
                    shadow = shadow,
                    onWeatherClick = onWeatherClick,
                    onEnableLocationClick = onEnableLocationClick,
                )
            }
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
                    fontWeight = digitWeight,
                    letterSpacing = digitSpacing,
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
                    fontWeight = digitWeight,
                    letterSpacing = digitSpacing,
                    fontFeatureSettings = "tnum",
                    shadow = shadow,
                ),
                color = accentColor,
            )
            Spacer(modifier = Modifier.height(if (isExpressive) 8.dp else 6.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (isExpressive) {
                    ClockDateChip(
                        text = shortDateText,
                        compact = false,
                        fillColor = chipFill,
                        textColor = chipTextColor,
                        borderColor = chipBorder,
                    )
                } else {
                    Text(
                        text = shortDateText,
                        style = MaterialTheme.typography.titleMedium.copy(shadow = shadow),
                        color = secondaryColor,
                    )
                }
                ClockWeatherBadge(
                    weather = weather,
                    showLocationCta = showLocationCta,
                    compact = false,
                    isExpressive = isExpressive,
                    textColor = if (isExpressive) chipTextColor else secondaryColor,
                    accentColor = accentColor,
                    fillColor = chipFill,
                    borderColor = chipBorder,
                    shadow = shadow,
                    onWeatherClick = onWeatherClick,
                    onEnableLocationClick = onEnableLocationClick,
                )
            }
        }
    }
}

@Composable
private fun AnalogClock(
    calendar: Calendar,
    dateText: String,
    compact: Boolean,
    isExpressive: Boolean,
    animateMotion: Boolean,
    handColor: Color,
    dateColor: Color,
    accentColor: Color,
    dialFill: Color,
    dialBorder: Color,
    chipFill: Color,
    chipTextColor: Color,
    chipBorder: Color,
    shadow: Shadow?,
    weather: WeatherSnapshot?,
    showLocationCta: Boolean,
    onWeatherClick: (() -> Unit)?,
    onEnableLocationClick: (() -> Unit)?,
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
                isExpressive = isExpressive,
                animateMotion = animateMotion,
                hourAngleDeg = hourAngleDeg,
                minuteAngleDeg = minuteAngleDeg,
                primaryColor = handColor,
                accentColor = accentColor,
                dialFill = dialFill,
                dialBorder = dialBorder,
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalAlignment = Alignment.Start,
            ) {
                if (isExpressive) {
                    ClockDateChip(
                        text = dateText,
                        compact = true,
                        fillColor = chipFill,
                        textColor = chipTextColor,
                        borderColor = chipBorder,
                    )
                } else {
                    Text(
                        text = dateText,
                        style = MaterialTheme.typography.titleSmall.copy(shadow = shadow),
                        color = dateColor,
                    )
                }
                ClockWeatherBadge(
                    weather = weather,
                    showLocationCta = showLocationCta,
                    compact = true,
                    isExpressive = isExpressive,
                    textColor = if (isExpressive) chipTextColor else dateColor,
                    accentColor = accentColor,
                    fillColor = chipFill,
                    borderColor = chipBorder,
                    shadow = shadow,
                    onWeatherClick = onWeatherClick,
                    onEnableLocationClick = onEnableLocationClick,
                )
            }
        }
    } else {
        Column(
            modifier = modifier,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AnalogDial(
                dialSize = dialSize,
                isExpressive = isExpressive,
                animateMotion = animateMotion,
                hourAngleDeg = hourAngleDeg,
                minuteAngleDeg = minuteAngleDeg,
                primaryColor = handColor,
                accentColor = accentColor,
                dialFill = dialFill,
                dialBorder = dialBorder,
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (isExpressive) {
                    ClockDateChip(
                        text = dateText,
                        compact = false,
                        fillColor = chipFill,
                        textColor = chipTextColor,
                        borderColor = chipBorder,
                    )
                } else {
                    Text(
                        text = dateText,
                        style = MaterialTheme.typography.titleMedium.copy(shadow = shadow),
                        color = dateColor,
                    )
                }
                ClockWeatherBadge(
                    weather = weather,
                    showLocationCta = showLocationCta,
                    compact = false,
                    isExpressive = isExpressive,
                    textColor = if (isExpressive) chipTextColor else dateColor,
                    accentColor = accentColor,
                    fillColor = chipFill,
                    borderColor = chipBorder,
                    shadow = shadow,
                    onWeatherClick = onWeatherClick,
                    onEnableLocationClick = onEnableLocationClick,
                )
            }
        }
    }
}

@Composable
private fun AnalogDial(
    dialSize: Dp,
    isExpressive: Boolean,
    animateMotion: Boolean,
    hourAngleDeg: Float,
    minuteAngleDeg: Float,
    primaryColor: Color,
    accentColor: Color,
    dialFill: Color,
    dialBorder: Color,
) {
    // Mantém ângulo contínuo para o ponteiro avançar suavemente de 59 -> 00 sem girar ao contrário.
    var continuousMinuteAngle by remember { mutableFloatStateOf(minuteAngleDeg) }
    var lastRawMinuteAngle by remember { mutableFloatStateOf(minuteAngleDeg) }
    var continuousHourAngle by remember { mutableFloatStateOf(hourAngleDeg) }
    var lastRawHourAngle by remember { mutableFloatStateOf(hourAngleDeg) }

    if (minuteAngleDeg != lastRawMinuteAngle) {
        var delta = minuteAngleDeg - lastRawMinuteAngle
        if (delta < -180f) delta += 360f
        if (delta > 180f) delta -= 360f
        continuousMinuteAngle += delta
        lastRawMinuteAngle = minuteAngleDeg
    }
    if (hourAngleDeg != lastRawHourAngle) {
        var delta = hourAngleDeg - lastRawHourAngle
        if (delta < -180f) delta += 360f
        if (delta > 180f) delta -= 360f
        continuousHourAngle += delta
        lastRawHourAngle = hourAngleDeg
    }

    val motion = OverviewTheme.motion
    val handSpec = if (animateMotion) motion.expressiveSpec<Float>() else snap()
    val animatedMinuteAngle by animateFloatAsState(
        targetValue = continuousMinuteAngle,
        animationSpec = handSpec,
        label = "analog_minute_angle",
    )
    val animatedHourAngle by animateFloatAsState(
        targetValue = continuousHourAngle,
        animationSpec = handSpec,
        label = "analog_hour_angle",
    )

    // Pré-aloca o mostrador recortado de 12 lóbulos fora do bloco Canvas para evitar alocações por frame
    val scallopPolygon = remember { OverviewPolygonFactory.clockScallopPolygon() }
    val scallopAndroidPath = remember { AndroidPath() }
    val scallopComposePath = remember(scallopAndroidPath) { scallopAndroidPath.asComposePath() }
    val scallopMatrix = remember { Matrix() }

    Canvas(modifier = Modifier.size(dialSize)) {
        val radius = size.minDimension / 2f
        val center = Offset(size.width / 2f, size.height / 2f)

        if (isExpressive) {
            OverviewPolygonFactory.fillScaledAndroidPath(
                polygon = scallopPolygon,
                sizePx = size.minDimension,
                outPath = scallopAndroidPath,
                workMatrix = scallopMatrix,
            )
            drawPath(
                path = scallopComposePath,
                color = dialFill,
            )
            drawPath(
                path = scallopComposePath,
                color = dialBorder,
                style = Stroke(width = 1.8.dp.toPx()),
            )
            drawCircle(
                color = accentColor.copy(alpha = 0.14f),
                radius = radius * 0.58f,
                center = center,
                style = Stroke(width = 1.2.dp.toPx()),
            )
        } else {
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
        }

        // Marcações das 12 horas com destaque às 12h.
        val outerFactor = if (isExpressive) 0.80f else 0.88f
        val quarterInnerFactor = if (isExpressive) 0.67f else 0.74f
        val regularInnerFactor = if (isExpressive) 0.73f else 0.80f
        for (tick in 0 until 12) {
            val angleRad = Math.toRadians((tick * 30.0) - 90.0)
            val isQuarter = tick % 3 == 0
            val outer = radius * outerFactor
            val inner = radius * if (isQuarter) quarterInnerFactor else regularInnerFactor
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

        val hourRad = Math.toRadians(animatedHourAngle.toDouble() - 90.0)
        val hourEnd = Offset(
            x = center.x + (cos(hourRad) * radius * 0.46f).toFloat(),
            y = center.y + (sin(hourRad) * radius * 0.46f).toFloat(),
        )
        drawLine(
            color = primaryColor,
            start = center,
            end = hourEnd,
            strokeWidth = (dialSize * (if (isExpressive) 0.058f else 0.048f)).toPx().coerceAtLeast(2.5f),
            cap = StrokeCap.Round,
        )

        val minuteRad = Math.toRadians(animatedMinuteAngle.toDouble() - 90.0)
        val minuteEnd = Offset(
            x = center.x + (cos(minuteRad) * radius * 0.68f).toFloat(),
            y = center.y + (sin(minuteRad) * radius * 0.68f).toFloat(),
        )
        drawLine(
            color = accentColor,
            start = center,
            end = minuteEnd,
            strokeWidth = (dialSize * (if (isExpressive) 0.036f else 0.032f)).toPx().coerceAtLeast(1.8f),
            cap = StrokeCap.Round,
        )

        val pivotRadius = (dialSize * (if (isExpressive) 0.056f else 0.046f)).toPx().coerceAtLeast(3f)
        drawCircle(
            color = accentColor,
            radius = pivotRadius,
            center = center,
        )
        if (isExpressive) {
            drawCircle(
                color = primaryColor,
                radius = pivotRadius * 0.42f,
                center = center,
            )
        }
    }
}

@Composable
private fun SideBySideClock(
    timeText: String,
    weekdayText: String,
    dayMonthText: String,
    compact: Boolean,
    isExpressive: Boolean,
    primaryColor: Color,
    secondaryColor: Color,
    accentColor: Color,
    accentContainer: Color,
    onAccentContainer: Color,
    chipFill: Color,
    chipTextColor: Color,
    chipBorder: Color,
    shadow: Shadow?,
    weather: WeatherSnapshot?,
    showLocationCta: Boolean,
    onWeatherClick: (() -> Unit)?,
    onEnableLocationClick: (() -> Unit)?,
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
                fontWeight = if (isExpressive) FontWeight.ExtraBold else FontWeight.SemiBold,
                letterSpacing = if (isExpressive) (-0.8).sp else 0.sp,
                fontFeatureSettings = "tnum",
                shadow = shadow,
            ),
            color = primaryColor,
        )

        Box(
            modifier = Modifier
                .width(if (isExpressive) 5.dp else 3.dp)
                .height(if (compact) 34.dp else 52.dp)
                .clip(OverviewTheme.shapes.pill)
                .background(accentColor),
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(if (isExpressive) 3.dp else 1.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (isExpressive) {
                    Text(
                        text = weekdayText,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.1.sp,
                        ),
                        color = onAccentContainer,
                        modifier = Modifier
                            .clip(OverviewTheme.shapes.pill)
                            .background(accentContainer)
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                    )
                } else {
                    Text(
                        text = weekdayText,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.1.sp,
                            shadow = shadow,
                        ),
                        color = accentColor,
                    )
                }
                ClockWeatherBadge(
                    weather = weather,
                    showLocationCta = showLocationCta,
                    compact = true,
                    isExpressive = isExpressive,
                    textColor = if (isExpressive) chipTextColor else secondaryColor,
                    accentColor = accentColor,
                    fillColor = chipFill,
                    borderColor = chipBorder,
                    shadow = shadow,
                    onWeatherClick = onWeatherClick,
                    onEnableLocationClick = onEnableLocationClick,
                )
            }
            Text(
                text = dayMonthText,
                style = (if (compact) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.titleSmall)
                    .copy(
                        fontWeight = if (isExpressive) FontWeight.Medium else null,
                        shadow = shadow,
                    ),
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
    isExpressive: Boolean,
    primaryColor: Color,
    secondaryColor: Color,
    accentColor: Color,
    accentContainer: Color,
    onAccentContainer: Color,
    containerColor: Color,
    borderColor: Color,
    shadowAlpha: Float,
    weather: WeatherSnapshot?,
    showLocationCta: Boolean,
    onWeatherClick: (() -> Unit)?,
    onEnableLocationClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val shape = OverviewTheme.shapes.pill

    Row(
        modifier = modifier
            .then(
                if (isExpressive && shadowAlpha > 0.15f) {
                    Modifier.shadow(elevation = (8f * shadowAlpha).dp, shape = shape, clip = false)
                } else {
                    Modifier
                },
            )
            .clip(shape)
            .background(containerColor)
            .border(1.dp, borderColor, shape)
            .padding(
                horizontal = if (isExpressive) {
                    if (compact) 10.dp else 12.dp
                } else {
                    if (compact) 14.dp else 18.dp
                },
                vertical = if (compact) 6.dp else 8.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (isExpressive) {
            Text(
                text = timeText,
                style = (if (compact) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleMedium)
                    .copy(
                        fontWeight = FontWeight.Bold,
                        fontFeatureSettings = "tnum",
                    ),
                color = onAccentContainer,
                modifier = Modifier
                    .clip(shape)
                    .background(accentContainer)
                    .padding(
                        horizontal = if (compact) 10.dp else 12.dp,
                        vertical = if (compact) 3.dp else 4.dp,
                    ),
            )
            Text(
                text = shortDateText,
                style = (if (compact) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium)
                    .copy(fontWeight = FontWeight.Medium),
                color = primaryColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        } else {
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
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        ClockWeatherBadge(
            weather = weather,
            showLocationCta = showLocationCta,
            compact = true,
            isExpressive = isExpressive,
            textColor = if (isExpressive) primaryColor else secondaryColor,
            accentColor = accentColor,
            fillColor = accentContainer.copy(alpha = 0.72f),
            borderColor = borderColor,
            shadow = null,
            onWeatherClick = onWeatherClick,
            onEnableLocationClick = onEnableLocationClick,
        )
    }
}

@Composable
private fun FullPillSummaryClock(
    timeText: String,
    dateText: String,
    greeting: String,
    compact: Boolean,
    isExpressive: Boolean,
    primaryColor: Color,
    secondaryColor: Color,
    accentColor: Color,
    accentContainer: Color,
    onAccentContainer: Color,
    containerColor: Color,
    borderColor: Color,
    shadowAlpha: Float,
    weather: WeatherSnapshot?,
    showLocationCta: Boolean,
    onWeatherClick: (() -> Unit)?,
    onEnableLocationClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val shape = OverviewTheme.shapes.pill

    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (isExpressive && shadowAlpha > 0.15f) {
                    Modifier.shadow(elevation = (10f * shadowAlpha).dp, shape = shape, clip = false)
                } else {
                    Modifier
                },
            )
            .clip(shape)
            .background(containerColor)
            .border(1.dp, borderColor, shape)
            .padding(
                horizontal = if (compact) 16.dp else 20.dp,
                vertical = if (isExpressive) {
                    if (compact) 10.dp else 13.dp
                } else {
                    if (compact) 11.dp else 15.dp
                },
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = dateText,
                style = (if (compact) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleMedium)
                    .copy(fontWeight = if (isExpressive) FontWeight.Bold else FontWeight.SemiBold),
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
                val summarySubtitle = if (weather != null && !compact) {
                    "$greeting · ${weather.location.displayLabel}"
                } else {
                    greeting
                }
                Text(
                    text = summarySubtitle,
                    style = if (compact) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium,
                    color = secondaryColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ClockWeatherBadge(
                weather = weather,
                showLocationCta = showLocationCta,
                compact = compact,
                isExpressive = isExpressive,
                textColor = if (isExpressive) onAccentContainer else primaryColor,
                accentColor = accentColor,
                fillColor = accentContainer.copy(alpha = 0.72f),
                borderColor = borderColor,
                shadow = null,
                onWeatherClick = onWeatherClick,
                onEnableLocationClick = onEnableLocationClick,
            )

            if (isExpressive) {
                Text(
                    text = timeText,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontSize = if (compact) 20.sp else 26.sp,
                        lineHeight = if (compact) 24.sp else 30.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFeatureSettings = "tnum",
                    ),
                    color = onAccentContainer,
                    modifier = Modifier
                        .clip(shape)
                        .background(accentContainer)
                        .padding(
                            horizontal = if (compact) 12.dp else 16.dp,
                            vertical = if (compact) 5.dp else 8.dp,
                        ),
                )
            } else {
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
    }
}
