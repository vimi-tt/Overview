package io.github.vimitt.overview.ui.home.widgets

import android.text.format.DateFormat
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
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

    Column(
        modifier = Modifier.fillMaxWidth().fillMaxHeight(),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        NativeWidgetHeader(
            title = stringResource(R.string.native_widget_agenda_title),
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
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clickable(role = Role.Button, onClick = onOpenCalendarApp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Text(
                        text = stringResource(R.string.native_widget_agenda_empty),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceMuted,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
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
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(if (isExpressive) shapes.medium else shapes.small)
                                .then(
                                    if (isExpressive) {
                                        Modifier
                                            .background(lerp(colors.surfaceContainerHigh, colors.accentContainer, 0.30f))
                                            .padding(horizontal = 8.dp, vertical = 5.dp)
                                    } else {
                                        Modifier.padding(vertical = 2.dp)
                                    },
                                )
                                .clickable(role = Role.Button) { onOpenEvent(event) },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(7.dp),
                        ) {
                            if (isExpressive) {
                                Box(
                                    modifier = Modifier
                                        .width(3.dp)
                                        .height(18.dp)
                                        .clip(CircleShape)
                                        .background(colors.accent),
                                )
                                if (spanX >= 2) {
                                    Box(
                                        modifier = Modifier
                                            .clip(shapes.pill)
                                            .background(colors.accentContainer)
                                            .padding(horizontal = 6.dp, vertical = 2.dp),
                                    ) {
                                        Text(
                                            text = timeLabel,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontFeatureSettings = "tnum",
                                            ),
                                            color = colors.onAccentContainer,
                                            maxLines = 1,
                                        )
                                    }
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(colors.accent),
                                )
                                if (spanX >= 2) {
                                    Text(
                                        text = timeLabel,
                                        style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                                        color = colors.onSurfaceMuted,
                                        maxLines = 1,
                                    )
                                }
                            }
                            Text(
                                text = event.title,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = if (isExpressive) FontWeight.Bold else FontWeight.Medium,
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
                            fontSize = if (spanX <= 1 || spanY <= 1) 26.sp else 36.sp,
                            fontWeight = if (isExpressive) FontWeight.ExtraBold else FontWeight.SemiBold,
                            fontFeatureSettings = "tnum",
                        ),
                        color = colors.onSurface,
                    )
                }

                if (spanX >= 2) {
                    Column(horizontalAlignment = Alignment.End) {
                        if (isExpressive) {
                            Box(
                                modifier = Modifier
                                    .clip(shapes.pill)
                                    .background(colors.accentContainer.copy(alpha = 0.7f))
                                    .padding(horizontal = 8.dp, vertical = 2.dp),
                            ) {
                                Text(
                                    text = stringResource(wmoWeatherDescriptionRes(weather.weatherCode)),
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = colors.onAccentContainer,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        } else {
                            Text(
                                text = stringResource(wmoWeatherDescriptionRes(weather.weatherCode)),
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                color = colors.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Text(
                            text = stringResource(
                                R.string.native_widget_weather_min_max,
                                weather.minTempC.roundToInt(),
                                weather.maxTempC.roundToInt(),
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.onSurfaceMuted,
                        )
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
            trailing = {
                if (spanX >= 2) {
                    NativeWidgetActionPill(
                        label = status.networkLabel,
                        emphasized = status.isOnline && isExpressive,
                        onClick = onOpenSettings,
                    )
                }
            },
        )

        if (spanY <= 1) {
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = if (spanX <= 1) "${status.batteryLevel}%" else batteryLabel,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = if (isExpressive) FontWeight.ExtraBold else FontWeight.SemiBold,
                            fontFeatureSettings = "tnum",
                        ),
                        color = if (status.isCharging && isExpressive) colors.accent else colors.onSurface,
                        maxLines = 1,
                    )
                    if (spanX >= 2) {
                        Text(
                            text = storageLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.onSurfaceMuted,
                            maxLines = 1,
                        )
                    }
                }
                if (isExpressive) {
                    ExpressiveWavyProgressBar(
                        progress = animatedBatteryFraction,
                        color = colors.accent,
                        trackColor = colors.surfaceContainerHigh.copy(alpha = 0.65f),
                        animateWave = status.isCharging && isVisible && expressiveMotion,
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .clip(shapes.small)
                            .background(colors.surfaceContainerHigh.copy(alpha = 0.7f)),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(animatedBatteryFraction)
                                .fillMaxHeight()
                                .clip(shapes.small)
                                .background(colors.accent),
                        )
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(7.dp),
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
