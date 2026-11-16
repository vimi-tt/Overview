package io.github.vimitt.overview.ui.home.widgets

import androidx.compose.runtime.Immutable
import io.github.vimitt.overview.domain.model.ClockToolMode
import io.github.vimitt.overview.domain.model.WeatherLocation
import io.github.vimitt.overview.domain.model.WeatherProvider

/**
 * Agrupador de ações interativas disparadas pelos widgets nativos na tela inicial.
 */
@Immutable
data class NativeWidgetActions(
    val onSaveQuickNote: (content: String, noteId: String?) -> Unit = { _, _ -> },
    val onDeleteQuickNote: (noteId: String) -> Unit = {},
    val onAddQuickTask: (title: String) -> Unit = {},
    val onToggleQuickTask: (taskId: String) -> Unit = {},
    val onClearCompletedTasks: () -> Unit = {},
    val onRefreshWeather: (force: Boolean) -> Unit = {},
    val onSearchWeatherCities: suspend (query: String) -> Result<List<WeatherLocation>> = { Result.success(emptyList()) },
    val onSelectWeatherLocation: (WeatherLocation) -> Unit = {},
    val onSelectWeatherProvider: (WeatherProvider) -> Unit = {},
    val onUseDeviceWeatherLocation: () -> Unit = {},
    val onRequestCalendarPermission: () -> Unit = {},
    val onRequestLocationPermission: () -> Unit = {},
    val onSetClockToolMode: (ClockToolMode) -> Unit = {},
    val onSetTimerDuration: (durationMs: Long) -> Unit = {},
    val onToggleTimer: () -> Unit = {},
    val onResetTimer: () -> Unit = {},
    val onFinishTimerIfElapsed: () -> Unit = {},
    val onToggleStopwatch: () -> Unit = {},
    val onResetStopwatch: () -> Unit = {},
    val onOpenNotificationAccessSettings: () -> Unit = {},
)
