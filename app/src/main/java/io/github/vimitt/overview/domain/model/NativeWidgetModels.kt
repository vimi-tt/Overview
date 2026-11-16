package io.github.vimitt.overview.domain.model

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import io.github.vimitt.overview.R

/**
 * Catálogo dos widgets nativos do Overview que se adaptam aos estilos GNOME e Material Expressive.
 */
enum class NativeWidgetType(
    @StringRes val titleRes: Int,
    @StringRes val descriptionRes: Int,
    val defaultSpanX: Int,
    val defaultSpanY: Int,
    val minSpanX: Int = 1,
    val minSpanY: Int = 1,
) {
    DAILY_SUMMARY(
        titleRes = R.string.native_widget_daily_summary_title,
        descriptionRes = R.string.native_widget_daily_summary_desc,
        defaultSpanX = 3,
        defaultSpanY = 1,
        minSpanX = 2,
        minSpanY = 1,
    ),
    AGENDA(
        titleRes = R.string.native_widget_agenda_title,
        descriptionRes = R.string.native_widget_agenda_desc,
        defaultSpanX = 2,
        defaultSpanY = 2,
        minSpanX = 1,
        minSpanY = 1,
    ),
    WEATHER(
        titleRes = R.string.native_widget_weather_title,
        descriptionRes = R.string.native_widget_weather_desc,
        defaultSpanX = 2,
        defaultSpanY = 1,
        minSpanX = 1,
        minSpanY = 1,
    ),
    MEDIA_PLAYER(
        titleRes = R.string.native_widget_media_title,
        descriptionRes = R.string.native_widget_media_desc,
        defaultSpanX = 3,
        defaultSpanY = 1,
        minSpanX = 2,
        minSpanY = 1,
    ),
    DEVICE_STATUS(
        titleRes = R.string.native_widget_device_title,
        descriptionRes = R.string.native_widget_device_desc,
        defaultSpanX = 2,
        defaultSpanY = 1,
        minSpanX = 1,
        minSpanY = 1,
    ),
    QUICK_NOTES(
        titleRes = R.string.native_widget_notes_title,
        descriptionRes = R.string.native_widget_notes_desc,
        defaultSpanX = 2,
        defaultSpanY = 1,
        minSpanX = 1,
        minSpanY = 1,
    ),
    TASKS(
        titleRes = R.string.native_widget_tasks_title,
        descriptionRes = R.string.native_widget_tasks_desc,
        defaultSpanX = 2,
        defaultSpanY = 2,
        minSpanX = 1,
        minSpanY = 1,
    ),
    FREQUENT_APPS(
        titleRes = R.string.native_widget_frequent_apps_title,
        descriptionRes = R.string.native_widget_frequent_apps_desc,
        defaultSpanX = 3,
        defaultSpanY = 1,
        minSpanX = 1,
        minSpanY = 1,
    ),
    TIMER_STOPWATCH(
        titleRes = R.string.native_widget_timer_title,
        descriptionRes = R.string.native_widget_timer_desc,
        defaultSpanX = 2,
        defaultSpanY = 1,
        minSpanX = 1,
        minSpanY = 1,
    ),
}

@Immutable
data class QuickNoteItem(
    val id: String,
    val content: String,
    val updatedAtMs: Long,
)

@Immutable
data class QuickTaskItem(
    val id: String,
    val title: String,
    val completed: Boolean = false,
)

@Immutable
data class WeatherLocation(
    val name: String,
    val region: String = "",
    val country: String = "",
    val latitude: Double,
    val longitude: Double,
) {
    val displayLabel: String
        get() = listOf(name, region)
            .filter { it.isNotBlank() }
            .distinct()
            .joinToString(", ")
            .ifBlank { name }
}

@Immutable
data class HourlyWeatherPoint(
    val timeIso: String,
    val hourLabel: String,
    val temperatureC: Double,
    val weatherCode: Int,
)

@Immutable
data class WeatherSnapshot(
    val location: WeatherLocation,
    val temperatureC: Double,
    val apparentTempC: Double,
    val minTempC: Double,
    val maxTempC: Double,
    val weatherCode: Int,
    val isDay: Boolean,
    val windSpeedKmh: Double,
    val hourlyForecast: List<HourlyWeatherPoint> = emptyList(),
    val updatedAtMs: Long,
)

enum class ClockToolMode {
    TIMER,
    STOPWATCH,
}

@Immutable
data class TimerStopwatchState(
    val mode: ClockToolMode = ClockToolMode.TIMER,
    val timerDurationMs: Long = 5 * 60_000L,
    val timerRemainingMs: Long = 5 * 60_000L,
    val timerRunning: Boolean = false,
    val timerTargetElapsedMs: Long = 0L,
    val stopwatchAccumulatedMs: Long = 0L,
    val stopwatchRunning: Boolean = false,
    val stopwatchStartElapsedMs: Long = 0L,
)

@Immutable
data class CalendarEventItem(
    val id: Long,
    val title: String,
    val beginMs: Long,
    val endMs: Long,
    val allDay: Boolean,
    val location: String = "",
)

@Immutable
data class DeviceStatusSnapshot(
    val batteryLevel: Int = 0,
    val isCharging: Boolean = false,
    val storageUsedBytes: Long = 0L,
    val storageTotalBytes: Long = 0L,
    val networkLabel: String = "",
    val isOnline: Boolean = false,
    val isWifi: Boolean = false,
)

@Immutable
data class NativeWidgetsState(
    val notes: List<QuickNoteItem> = emptyList(),
    val tasks: List<QuickTaskItem> = emptyList(),
    val weatherLocation: WeatherLocation? = null,
    val weather: WeatherSnapshot? = null,
    val isWeatherLoading: Boolean = false,
    val weatherError: String? = null,
    val timerStopwatch: TimerStopwatchState = TimerStopwatchState(),
    val appLaunchCounts: Map<String, Int> = emptyMap(),
)
