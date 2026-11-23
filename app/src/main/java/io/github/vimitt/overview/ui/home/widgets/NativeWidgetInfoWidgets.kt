package io.github.vimitt.overview.ui.home.widgets

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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
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
import io.github.vimitt.overview.domain.model.DeviceStatusSnapshot
import io.github.vimitt.overview.domain.model.WeatherSnapshot
import io.github.vimitt.overview.ui.components.ExpressiveLoadingIndicator
import io.github.vimitt.overview.ui.components.ExpressiveWavyProgressBar
import io.github.vimitt.overview.ui.components.OverviewPolygonFactory
import io.github.vimitt.overview.ui.components.RoundedPolygonShape
import io.github.vimitt.overview.ui.theme.OverviewTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Agenda com os próximos compromissos e toque direto para abrir o evento no calendário.
 */
@Composable
fun AgendaNativeWidget(
    hasPermission: Boolean,
    events: List<CalendarEventItem>,
    spanX: Int,
    spanY: Int,
    onRequestPermission: () -> Unit,
    onOpenEvent: (CalendarEventItem) -> Unit,
    onOpenCalendarApp: () -> Unit,
) {
    val context = LocalContext.current
    val colors = OverviewTheme.colors
    val shapes = OverviewTheme.shapes
    val isExpressive = OverviewTheme.components.isExpressive
    val is24Hour = DateFormat.is24HourFormat(context)
    val locale = remember { Locale.getDefault() }
    val todayDayNum = remember(locale) {
        SimpleDateFormat("d", locale).format(Date())
    }
    val todayWeekday = remember(locale) {
        SimpleDateFormat("EEE", locale).format(Date()).uppercase(locale)
    }

    Column(
        modifier = Modifier.fillMaxWidth().fillMaxHeight(),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        NativeWidgetHeader(
            title = stringResource(R.string.native_widget_agenda_title),
            icon = {
                CalendarGlyph(
                    tint = if (isExpressive) colors.onAccentContainer else colors.accent,
                    size = 13.dp,
                )
            },
            trailing = {
                if (hasPermission && events.isNotEmpty() && spanX >= 2) {
                    NativeWidgetActionPill(
                        label = "${events.size}",
                        emphasized = isExpressive,
                        onClick = onOpenCalendarApp,
                    )
                }
            },
        )

        when {
            !hasPermission -> {
                if (spanX <= 1 && spanY <= 1) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .clickable(role = Role.Button, onClick = onRequestPermission),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        NativeWidgetActionPill(
                            label = stringResource(R.string.native_widget_agenda_permission_cta),
                            onClick = onRequestPermission,
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = stringResource(R.string.native_widget_agenda_permission_title),
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceMuted,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f).padding(end = 8.dp),
                        )
                        NativeWidgetActionPill(
                            label = stringResource(R.string.native_widget_agenda_permission_cta),
                            onClick = onRequestPermission,
                        )
                    }
                }
            }

            events.isEmpty() -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clickable(role = Role.Button, onClick = onOpenCalendarApp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (spanX >= 2) {
                        Column(
                            modifier = Modifier
                                .clip(shapes.medium)
                                .background(
                                    if (isExpressive) colors.accentContainer else colors.surfaceElevated,
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (isExpressive) colors.accent.copy(alpha = 0.22f) else colors.hairlineBorder,
                                    shape = shapes.medium,
                                )
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                text = todayWeekday,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                ),
                                color = if (isExpressive) colors.onAccentContainer else colors.accent,
                            )
                            Text(
                                text = todayDayNum,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    fontFeatureSettings = "tnum",
                                ),
                                color = if (isExpressive) colors.onAccentContainer else colors.onSurface,
                            )
                        }
                    }
                    Text(
                        text = stringResource(R.string.native_widget_agenda_empty),
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        color = colors.onSurfaceMuted,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            else -> {
                val visibleLimit = when {
                    spanY <= 1 -> 1
                    spanY == 2 -> 2
                    else -> 4
                }
                val visibleEvents = events.take(visibleLimit)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(if (isExpressive) 5.dp else 4.dp),
                ) {
                    for ((index, event) in visibleEvents.withIndex()) {
                        val timeLabel = remember(event.id, event.beginMs, is24Hour, locale, spanX) {
                            if (event.allDay) {
                                context.getString(R.string.native_widget_agenda_all_day)
                            } else {
                                val fmt = if (spanX <= 1) {
                                    if (is24Hour) "HH:mm" else "h:mm"
                                } else {
                                    if (is24Hour) "EEE HH:mm" else "EEE h:mm"
                                }
                                SimpleDateFormat(fmt, locale).format(Date(event.beginMs))
                            }
                        }
                        val isPrimaryEvent = index == 0
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(if (isExpressive) shapes.medium else shapes.small)
                                .then(
                                    if (isPrimaryEvent) {
                                        Modifier
                                            .background(
                                                if (isExpressive) {
                                                    lerp(colors.surfaceContainerHigh, colors.accentContainer, 0.38f)
                                                } else {
                                                    lerp(colors.surfaceElevated, colors.accentContainer, 0.14f)
                                                },
                                            )
                                            .border(
                                                width = 1.dp,
                                                color = if (isExpressive) colors.accent.copy(alpha = 0.24f) else colors.hairlineBorder,
                                                shape = if (isExpressive) shapes.medium else shapes.small,
                                            )
                                            .padding(horizontal = 8.dp, vertical = 6.dp)
                                    } else if (isExpressive) {
                                        Modifier
                                            .background(colors.surfaceContainerHigh.copy(alpha = 0.65f))
                                            .padding(horizontal = 8.dp, vertical = 5.dp)
                                    } else {
                                        Modifier.padding(horizontal = 4.dp, vertical = 3.dp)
                                    },
                                )
                                .clickable(role = Role.Button) { onOpenEvent(event) },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(7.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(if (isPrimaryEvent) 3.5.dp else 2.5.dp)
                                    .height(if (isPrimaryEvent) 18.dp else 14.dp)
                                    .clip(CircleShape)
                                    .background(if (isPrimaryEvent) colors.accent else colors.accent.copy(alpha = 0.55f)),
                            )
                            if (spanX >= 2) {
                                Box(
                                    modifier = Modifier
                                        .clip(shapes.pill)
                                        .background(
                                            if (isPrimaryEvent) {
                                                colors.accentContainer
                                            } else {
                                                colors.surfaceContainerHigh.copy(alpha = 0.85f)
                                            },
                                        )
                                        .padding(horizontal = 6.dp, vertical = 2.dp),
                                ) {
                                    Text(
                                        text = timeLabel,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = if (isPrimaryEvent) FontWeight.Bold else FontWeight.Medium,
                                            fontFeatureSettings = "tnum",
                                        ),
                                        color = if (isPrimaryEvent) colors.onAccentContainer else colors.onSurfaceMuted,
                                        maxLines = 1,
                                    )
                                }
                            }
                            Text(
                                text = event.title,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = if (isPrimaryEvent) FontWeight.Bold else FontWeight.Medium,
                                ),
                                color = colors.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )
                        }
                        if (!isExpressive && index < visibleEvents.lastIndex) {
                            HorizontalDivider(thickness = 1.dp, color = colors.hairlineBorder)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Clima via Open-Meteo (temperatura atual, min/máx e previsão horária).
 */
@Composable
fun WeatherNativeWidget(
    weather: WeatherSnapshot?,
    isLoading: Boolean,
    errorText: String?,
    spanX: Int,
    spanY: Int,
    isVisible: Boolean,
    onOpenCityPicker: () -> Unit,
    onUseLocation: () -> Unit,
    onRefresh: () -> Unit,
) {
    val colors = OverviewTheme.colors
    val shapes = OverviewTheme.shapes
    val motion = OverviewTheme.motion
    val isExpressive = OverviewTheme.components.isExpressive
    val expressiveMotion = isExpressive && motion.usesSprings && !motion.reducedMotion

    val scallopShape = remember(isExpressive) {
        if (isExpressive) RoundedPolygonShape(OverviewPolygonFactory.clockScallopPolygon()) else null
    }
    val heroScale = remember { Animatable(1f) }
    LaunchedEffect(weather?.temperatureC, weather?.updatedAtMs, expressiveMotion, isVisible) {
        if (!expressiveMotion || !isVisible || weather == null) {
            heroScale.snapTo(1f)
            return@LaunchedEffect
        }
        heroScale.snapTo(0.88f)
        heroScale.animateTo(1f, animationSpec = motion.expressiveSpec())
    }

    Column(
        modifier = Modifier.fillMaxWidth().fillMaxHeight(),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        NativeWidgetHeader(
            title = weather?.location?.displayLabel ?: stringResource(R.string.native_widget_weather_title),
            icon = {
                WeatherGlyph(
                    weatherCode = weather?.weatherCode ?: 0,
                    isDay = weather?.isDay ?: true,
                    tint = if (isExpressive) colors.onAccentContainer else colors.onSurfaceMuted,
                    accent = if (isExpressive) colors.onAccentContainer else colors.accent,
                    size = 13.dp,
                )
            },
            trailing = {
                if (isLoading && isExpressive) {
                    ExpressiveLoadingIndicator(size = 18.dp)
                } else if (spanX >= 2) {
                    NativeWidgetActionPill(
                        label = stringResource(R.string.native_widget_weather_choose_city),
                        emphasized = false,
                        onClick = onOpenCityPicker,
                    )
                }
            },
        )

        if (weather == null) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (spanX >= 2 || spanY >= 2) {
                    Text(
                        text = errorText ?: stringResource(R.string.native_widget_weather_setup_title),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceMuted,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    NativeWidgetActionPill(
                        label = stringResource(R.string.native_widget_weather_choose_city),
                        onClick = onOpenCityPicker,
                    )
                    if (spanX >= 2) {
                        NativeWidgetActionPill(
                            label = stringResource(R.string.native_widget_weather_use_gps),
                            emphasized = false,
                            onClick = onUseLocation,
                        )
                    }
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(role = Role.Button, onClick = onRefresh),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.graphicsLayer {
                        scaleX = heroScale.value
                        scaleY = heroScale.value
                    },
                ) {
                    if (isExpressive && scallopShape != null) {
                        Box(
                            modifier = Modifier
                                .size(if (spanY <= 1) 34.dp else 44.dp)
                                .clip(scallopShape)
                                .background(colors.accentContainer),
                            contentAlignment = Alignment.Center,
                        ) {
                            WeatherGlyph(
                                weatherCode = weather.weatherCode,
                                isDay = weather.isDay,
                                tint = colors.onAccentContainer,
                                accent = colors.accent,
                                size = if (spanY <= 1) 20.dp else 26.dp,
                            )
                        }
                    } else {
                        WeatherGlyph(
                            weatherCode = weather.weatherCode,
                            isDay = weather.isDay,
                            tint = colors.onSurfaceMuted,
                            accent = colors.accent,
                            size = if (spanY <= 1) 24.dp else 34.dp,
                        )
                    }
                    Text(
                        text = "${weather.temperatureC.roundToInt()}°",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontSize = if (spanX <= 1 || spanY <= 1) 28.sp else 40.sp,
                            letterSpacing = (-1.2).sp,
                            fontWeight = if (isExpressive) FontWeight.ExtraBold else FontWeight.Bold,
                            fontFeatureSettings = "tnum",
                        ),
                        color = colors.onSurface,
                    )
                }

                if (spanX >= 2) {
                    val tempRange = (weather.maxTempC - weather.minTempC).coerceAtLeast(1.0)
                    val tempPos = ((weather.temperatureC - weather.minTempC) / tempRange).toFloat().coerceIn(0.08f, 0.92f)
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(shapes.pill)
                                .background(
                                    if (isExpressive) {
                                        colors.accentContainer.copy(alpha = 0.82f)
                                    } else {
                                        colors.surfaceElevated
                                    },
                                )
                                .padding(horizontal = 8.dp, vertical = 2.dp),
                        ) {
                            Text(
                                text = stringResource(wmoWeatherDescriptionRes(weather.weatherCode)),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (isExpressive) FontWeight.Bold else FontWeight.SemiBold,
                                ),
                                color = if (isExpressive) colors.onAccentContainer else colors.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                        ) {
                            Text(
                                text = "${weather.minTempC.roundToInt()}°",
                                style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                                color = colors.onSurfaceMuted,
                            )
                            Box(
                                modifier = Modifier
                                    .width(34.dp)
                                    .height(4.dp)
                                    .clip(CircleShape)
                                    .background(colors.surfaceContainerHigh),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(tempPos)
                                        .fillMaxHeight()
                                        .clip(CircleShape)
                                        .background(
                                            Brush.horizontalGradient(
                                                colors = listOf(colors.accent.copy(alpha = 0.55f), colors.accent),
                                            ),
                                        ),
                                )
                            }
                            Text(
                                text = "${weather.maxTempC.roundToInt()}°",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontFeatureSettings = "tnum",
                                ),
                                color = colors.onSurface,
                            )
                        }
                    }
                }
            }

            if (spanY >= 2 && weather.hourlyForecast.isNotEmpty()) {
                if (!isExpressive) {
                    HorizontalDivider(thickness = 1.dp, color = colors.hairlineBorder)
                }
                val slotCount = spanX.coerceIn(2, 4)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    for ((index, point) in weather.hourlyForecast.take(slotCount).withIndex()) {
                        val slotModifier = if (isExpressive) {
                            val bg = if (index == 0) {
                                colors.accentContainer.copy(alpha = 0.85f)
                            } else {
                                lerp(colors.surfaceContainerHigh, colors.accentContainer, 0.22f)
                            }
                            Modifier
                                .weight(1f)
                                .clip(shapes.medium)
                                .background(bg)
                                .padding(vertical = 5.dp)
                        } else {
                            Modifier
                                .weight(1f)
                                .padding(vertical = 2.dp)
                        }
                        Column(
                            modifier = slotModifier,
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            Text(
                                text = point.hourLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isExpressive && index == 0) colors.onAccentContainer else colors.onSurfaceMuted,
                            )
                            WeatherGlyph(
                                weatherCode = point.weatherCode,
                                isDay = true,
                                tint = if (isExpressive && index == 0) colors.onAccentContainer else colors.onSurfaceMuted,
                                accent = colors.accent,
                                size = 15.dp,
                            )
                            Text(
                                text = "${point.temperatureC.roundToInt()}°",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (isExpressive && index == 0) colors.onAccentContainer else colors.onSurface,
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Bateria, armazenamento e rede num cartão compacto adaptativo ao tema.
 */
@Composable
fun DeviceStatusNativeWidget(
    status: DeviceStatusSnapshot,
    spanX: Int,
    spanY: Int,
    isVisible: Boolean,
    onOpenSettings: () -> Unit,
) {
    val colors = OverviewTheme.colors
    val shapes = OverviewTheme.shapes
    val motion = OverviewTheme.motion
    val isExpressive = OverviewTheme.components.isExpressive
    val expressiveMotion = isExpressive && motion.usesSprings && !motion.reducedMotion

    val usedGb = status.storageUsedBytes / (1024f * 1024f * 1024f)
    val totalGb = status.storageTotalBytes / (1024f * 1024f * 1024f)
    val storageFraction = if (status.storageTotalBytes > 0L) {
        (status.storageUsedBytes.toFloat() / status.storageTotalBytes.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }
    val storageLabel = if (totalGb > 0f) {
        String.format(Locale.getDefault(), "%.0f / %.0f GB", usedGb, totalGb)
    } else {
        "—"
    }
    val batteryLabel = if (status.isCharging) {
        stringResource(R.string.native_widget_battery_charging, status.batteryLevel)
    } else {
        "${status.batteryLevel}%"
    }
    val batteryFraction = (status.batteryLevel / 100f).coerceIn(0.02f, 1f)

    val animatedBatteryFraction by animateFloatAsState(
        targetValue = batteryFraction,
        animationSpec = if (expressiveMotion && isVisible) motion.expressiveSpec() else snap(),
        label = "deviceBatteryAnim",
    )
    val animatedStorageFraction by animateFloatAsState(
        targetValue = storageFraction.coerceIn(0.02f, 1f),
        animationSpec = if (expressiveMotion && isVisible) motion.expressiveSpec() else snap(),
        label = "deviceStorageAnim",
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .clickable(role = Role.Button, onClick = onOpenSettings),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        NativeWidgetHeader(
            title = stringResource(R.string.native_widget_device_title),
            icon = {
                BatteryBoltGlyph(
                    tint = if (isExpressive) colors.onAccentContainer else colors.accent,
                    size = 13.dp,
                )
            },
            trailing = {
                if (spanX >= 2) {
                    Row(
                        modifier = Modifier
                            .clip(shapes.pill)
                            .background(
                                if (status.isOnline && isExpressive) {
                                    colors.accentContainer.copy(alpha = 0.85f)
                                } else {
                                    colors.surfaceElevated
                                },
                            )
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(
                                    if (status.isOnline) colors.accent else colors.onSurfaceMuted.copy(alpha = 0.5f),
                                ),
                        )
                        Text(
                            text = status.networkLabel,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (isExpressive) FontWeight.Bold else FontWeight.Medium,
                            ),
                            color = if (status.isOnline && isExpressive) colors.onAccentContainer else colors.onSurface,
                            maxLines = 1,
                        )
                    }
                }
            },
        )

        if (spanY <= 1) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                DeviceBatteryRing(
                    level = status.batteryLevel,
                    fraction = animatedBatteryFraction,
                    isCharging = status.isCharging,
                    ringSize = if (spanX <= 1) 34.dp else 40.dp,
                )
                if (spanX >= 2) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = stringResource(R.string.native_widget_storage_label),
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.onSurfaceMuted,
                                maxLines = 1,
                            )
                            Text(
                                text = storageLabel,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontFeatureSettings = "tnum",
                                ),
                                color = colors.onSurface,
                                maxLines = 1,
                            )
                        }
                        if (isExpressive) {
                            ExpressiveWavyProgressBar(
                                progress = animatedStorageFraction,
                                color = colors.accent,
                                trackColor = colors.surfaceContainerHigh.copy(alpha = 0.65f),
                                animateWave = status.isCharging && isVisible && expressiveMotion,
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(CircleShape)
                                    .background(colors.surfaceContainerHigh.copy(alpha = 0.75f)),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(animatedStorageFraction)
                                        .fillMaxHeight()
                                        .clip(CircleShape)
                                        .background(colors.accent),
                                )
                            }
                        }
                    }
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                DeviceBatteryRing(
                    level = status.batteryLevel,
                    fraction = animatedBatteryFraction,
                    isCharging = status.isCharging,
                    ringSize = 54.dp,
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    DeviceMetricRow(
                        label = stringResource(R.string.native_widget_battery_label),
                        valueText = batteryLabel,
                        fraction = animatedBatteryFraction,
                        isExpressive = isExpressive,
                        animateWavy = status.isCharging && isVisible && expressiveMotion,
                    )
                    DeviceMetricRow(
                        label = stringResource(R.string.native_widget_storage_label),
                        valueText = storageLabel,
                        fraction = animatedStorageFraction,
                        isExpressive = isExpressive,
                        animateWavy = false,
                    )
                }
            }
        }
    }
}

@Composable
private fun DeviceBatteryRing(
    level: Int,
    fraction: Float,
    isCharging: Boolean,
    ringSize: androidx.compose.ui.unit.Dp,
) {
    val colors = OverviewTheme.colors
    val trackColor = colors.surfaceContainerHigh.copy(alpha = 0.85f)
    val arcColor = if (level <= 15 && !isCharging) colors.error else colors.accent
    val strokeDp = if (ringSize >= 50.dp) 5.dp else 4.dp

    Box(
        modifier = Modifier.size(ringSize),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(ringSize)) {
            val strokePx = strokeDp.toPx()
            drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = strokePx, cap = StrokeCap.Round),
            )
            drawArc(
                color = arcColor,
                startAngle = -90f,
                sweepAngle = (fraction.coerceIn(0.03f, 1f) * 360f),
                useCenter = false,
                style = Stroke(width = strokePx, cap = StrokeCap.Round),
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (isCharging && ringSize >= 48.dp) {
                BatteryBoltGlyph(tint = arcColor, size = 10.dp)
            }
            Text(
                text = "$level%",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = if (ringSize >= 50.dp) 12.sp else 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFeatureSettings = "tnum",
                ),
                color = colors.onSurface,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun DeviceMetricRow(
    label: String,
    valueText: String,
    fraction: Float,
    isExpressive: Boolean,
    animateWavy: Boolean,
) {
    val colors = OverviewTheme.colors
    val shapes = OverviewTheme.shapes
    val trackShape = if (isExpressive) CircleShape else shapes.small

    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = colors.onSurfaceMuted,
            )
            Text(
                text = valueText,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = if (isExpressive) FontWeight.Bold else FontWeight.SemiBold,
                    fontFeatureSettings = "tnum",
                ),
                color = colors.onSurface,
            )
        }
        if (isExpressive) {
            ExpressiveWavyProgressBar(
                progress = fraction,
                color = colors.accent,
                trackColor = colors.surfaceContainerHigh.copy(alpha = 0.7f),
                animateWave = animateWavy,
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(trackShape)
                    .background(colors.surfaceContainerHigh.copy(alpha = 0.7f)),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction.coerceIn(0.02f, 1f))
                        .fillMaxHeight()
                        .clip(trackShape)
                        .background(colors.accent),
                )
            }
        }
    }
}
