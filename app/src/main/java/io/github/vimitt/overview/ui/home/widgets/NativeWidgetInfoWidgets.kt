package io.github.vimitt.overview.ui.home.widgets

import android.text.format.DateFormat
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import io.github.vimitt.overview.ui.theme.OverviewTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Widget 3: Agenda com os próximos compromissos e toque direto para abrir o evento no calendário.
 */
@Composable
fun AgendaNativeWidget(
    hasPermission: Boolean,
    events: List<CalendarEventItem>,
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
                if (hasPermission && events.isNotEmpty()) {
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
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceMuted,
                    )
                }
            }

            else -> {
                val visibleLimit = when {
                    spanY <= 1 -> 1
                    spanY == 2 -> 3
                    else -> 5
                }
                val visibleEvents = events.take(visibleLimit)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(if (isExpressive) 6.dp else 5.dp),
                ) {
                    for ((index, event) in visibleEvents.withIndex()) {
                        val timeLabel = remember(event.id, event.beginMs, is24Hour, locale) {
                            if (event.allDay) {
                                context.getString(R.string.native_widget_agenda_all_day)
                            } else {
                                SimpleDateFormat(if (is24Hour) "EEE HH:mm" else "EEE h:mm", locale)
                                    .format(Date(event.beginMs))
                            }
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(if (isExpressive) shapes.medium else shapes.small)
                                .then(
                                    if (isExpressive) {
                                        Modifier
                                            .background(colors.surfaceContainerHigh.copy(alpha = 0.6f))
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    } else {
                                        Modifier.padding(vertical = 3.dp)
                                    },
                                )
                                .clickable(role = Role.Button) { onOpenEvent(event) },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            if (isExpressive) {
                                Box(
                                    modifier = Modifier
                                        .clip(shapes.pill)
                                        .background(colors.accentContainer)
                                        .padding(horizontal = 7.dp, vertical = 2.dp),
                                ) {
                                    Text(
                                        text = timeLabel,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                        color = colors.onAccentContainer,
                                        maxLines = 1,
                                    )
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(colors.accent),
                                )
                                Text(
                                    text = timeLabel,
                                    style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                                    color = colors.onSurfaceMuted,
                                    maxLines = 1,
                                )
                            }
                            Text(
                                text = event.title,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = if (isExpressive) FontWeight.SemiBold else FontWeight.Medium,
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
 * Widget 4: Clima via Open-Meteo (temperatura atual, sensação, min/máx e previsão curta).
 */
@Composable
fun WeatherNativeWidget(
    weather: WeatherSnapshot?,
    isLoading: Boolean,
    errorText: String?,
    spanY: Int,
    onOpenCityPicker: () -> Unit,
    onUseLocation: () -> Unit,
    onRefresh: () -> Unit,
) {
    val colors = OverviewTheme.colors
    val shapes = OverviewTheme.shapes
    val isExpressive = OverviewTheme.components.isExpressive

    Column(
        modifier = Modifier.fillMaxWidth().fillMaxHeight(),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        NativeWidgetHeader(
            title = weather?.location?.displayLabel ?: stringResource(R.string.native_widget_weather_title),
            trailing = {
                if (isLoading && isExpressive) {
                    ExpressiveLoadingIndicator(size = 18.dp)
                } else {
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
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = errorText ?: stringResource(R.string.native_widget_weather_setup_title),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceMuted,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NativeWidgetActionPill(
                        label = stringResource(R.string.native_widget_weather_choose_city),
                        onClick = onOpenCityPicker,
                    )
                    NativeWidgetActionPill(
                        label = stringResource(R.string.native_widget_weather_use_gps),
                        emphasized = false,
                        onClick = onUseLocation,
                    )
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
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    WeatherGlyph(
                        weatherCode = weather.weatherCode,
                        isDay = weather.isDay,
                        tint = colors.onSurfaceMuted,
                        accent = colors.accent,
                        size = if (spanY <= 1) 26.dp else 36.dp,
                    )
                    Text(
                        text = "${weather.temperatureC.roundToInt()}°",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontSize = if (spanY <= 1) 28.sp else 36.sp,
                            fontWeight = if (isExpressive) FontWeight.ExtraBold else FontWeight.SemiBold,
                            fontFeatureSettings = "tnum",
                        ),
                        color = colors.onSurface,
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = stringResource(wmoWeatherDescriptionRes(weather.weatherCode)),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        color = colors.onSurface,
                        maxLines = 1,
                    )
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

            if (spanY >= 2 && weather.hourlyForecast.isNotEmpty()) {
                if (!isExpressive) {
                    HorizontalDivider(thickness = 1.dp, color = colors.hairlineBorder)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    for (point in weather.hourlyForecast.take(4)) {
                        val slotModifier = if (isExpressive) {
                            Modifier
                                .weight(1f)
                                .clip(shapes.medium)
                                .background(colors.surfaceContainerHigh.copy(alpha = 0.65f))
                                .padding(vertical = 6.dp)
                        } else {
                            Modifier
                                .weight(1f)
                                .padding(vertical = 2.dp)
                        }
                        Column(
                            modifier = slotModifier,
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(3.dp),
                        ) {
                            Text(
                                text = point.hourLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.onSurfaceMuted,
                            )
                            WeatherGlyph(
                                weatherCode = point.weatherCode,
                                isDay = true,
                                tint = colors.onSurfaceMuted,
                                accent = colors.accent,
                                size = 16.dp,
                            )
                            Text(
                                text = "${point.temperatureC.roundToInt()}°",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = colors.onSurface,
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Widget 6: Bateria, armazenamento e rede num cartão compacto adaptativo ao tema.
 */
@Composable
fun DeviceStatusNativeWidget(
    status: DeviceStatusSnapshot,
    spanY: Int,
    onOpenSettings: () -> Unit,
) {
    val colors = OverviewTheme.colors
    val shapes = OverviewTheme.shapes
    val isExpressive = OverviewTheme.components.isExpressive

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
                NativeWidgetActionPill(
                    label = status.networkLabel,
                    emphasized = status.isOnline && isExpressive,
                    onClick = onOpenSettings,
                )
            },
        )

        if (spanY <= 1) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "${stringResource(R.string.native_widget_battery_label)} · $batteryLabel",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                    color = colors.onSurface,
                )
                Text(
                    text = storageLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.onSurfaceMuted,
                )
            }
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                DeviceMetricRow(
                    label = stringResource(R.string.native_widget_battery_label),
                    valueText = batteryLabel,
                    fraction = (status.batteryLevel / 100f).coerceIn(0f, 1f),
                    isExpressive = isExpressive,
                )
                DeviceMetricRow(
                    label = stringResource(R.string.native_widget_storage_label),
                    valueText = storageLabel,
                    fraction = storageFraction,
                    isExpressive = isExpressive,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = stringResource(R.string.native_widget_network_label),
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.onSurfaceMuted,
                    )
                    Text(
                        text = status.networkLabel,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = if (isExpressive) FontWeight.SemiBold else FontWeight.Medium,
                        ),
                        color = if (status.isOnline) colors.accent else colors.onSurfaceMuted,
                    )
                }
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
                    fontWeight = FontWeight.SemiBold,
                    fontFeatureSettings = "tnum",
                ),
                color = colors.onSurface,
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (isExpressive) 8.dp else 5.dp)
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
