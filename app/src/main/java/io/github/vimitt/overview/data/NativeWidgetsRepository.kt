package io.github.vimitt.overview.data

import android.content.Context
import android.os.SystemClock
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import io.github.vimitt.overview.domain.model.ClockToolMode
import io.github.vimitt.overview.domain.model.HourlyWeatherPoint
import io.github.vimitt.overview.domain.model.NativeWidgetsState
import io.github.vimitt.overview.domain.model.QuickNoteItem
import io.github.vimitt.overview.domain.model.QuickTaskItem
import io.github.vimitt.overview.domain.model.TimerStopwatchState
import io.github.vimitt.overview.domain.model.WeatherLocation
import io.github.vimitt.overview.domain.model.WeatherSnapshot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.UUID

private val Context.nativeWidgetsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "overview_native_widgets",
)

/**
 * Repositório DataStore para dados persistidos dos widgets nativos:
 * notas rápidas, tarefas, clima/localidade em cache, timer/cronômetro e apps frequentes.
 */
class NativeWidgetsRepository(
    private val context: Context,
    private val weatherClient: OpenMeteoWeatherClient = OpenMeteoWeatherClient(),
) {
    private val weatherLoading = MutableStateFlow(false)
    private val weatherError = MutableStateFlow<String?>(null)

    private val storedState: Flow<NativeWidgetsState> = context.nativeWidgetsDataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { prefs ->
            NativeWidgetsState(
                notes = decodeNotes(prefs[KEY_NOTES].orEmpty()),
                tasks = decodeTasks(prefs[KEY_TASKS].orEmpty()),
                weatherLocation = decodeLocation(prefs[KEY_WEATHER_LOCATION].orEmpty()),
                weather = decodeWeather(prefs[KEY_WEATHER_CACHE].orEmpty()),
                timerStopwatch = decodeTimerStopwatch(prefs[KEY_TIMER_STOPWATCH].orEmpty()),
                appLaunchCounts = decodeLaunchCounts(prefs[KEY_APP_LAUNCH_COUNTS].orEmpty()),
            )
        }

    val state: Flow<NativeWidgetsState> = combine(
        storedState,
        weatherLoading,
        weatherError,
    ) { persisted, isLoading, err ->
        persisted.copy(
            isWeatherLoading = isLoading,
            weatherError = err,
        )
    }

    // --- Notas rápidas ---

    suspend fun saveQuickNote(content: String, noteId: String? = null) {
        val trimmed = content.trim()
        if (trimmed.isEmpty()) return
        val now = System.currentTimeMillis()
        context.nativeWidgetsDataStore.edit { prefs ->
            val current = decodeNotes(prefs[KEY_NOTES].orEmpty()).toMutableList()
            if (noteId != null) {
                val idx = current.indexOfFirst { it.id == noteId }
                if (idx >= 0) {
                    current[idx] = current[idx].copy(content = trimmed, updatedAtMs = now)
                } else {
                    current.add(0, QuickNoteItem(id = noteId, content = trimmed, updatedAtMs = now))
                }
            } else {
                current.add(0, QuickNoteItem(id = UUID.randomUUID().toString(), content = trimmed, updatedAtMs = now))
            }
            prefs[KEY_NOTES] = encodeNotes(current.take(MAX_NOTES))
        }
    }

    suspend fun deleteQuickNote(noteId: String) {
        context.nativeWidgetsDataStore.edit { prefs ->
            val updated = decodeNotes(prefs[KEY_NOTES].orEmpty()).filterNot { it.id == noteId }
            prefs[KEY_NOTES] = encodeNotes(updated)
        }
    }

    // --- Tarefas ---

    suspend fun addTask(title: String) {
        val trimmed = title.trim()
        if (trimmed.isEmpty()) return
        context.nativeWidgetsDataStore.edit { prefs ->
            val current = decodeTasks(prefs[KEY_TASKS].orEmpty()).toMutableList()
            current.add(0, QuickTaskItem(id = UUID.randomUUID().toString(), title = trimmed, completed = false))
            prefs[KEY_TASKS] = encodeTasks(current.take(MAX_TASKS))
        }
    }

    suspend fun toggleTask(taskId: String) {
        context.nativeWidgetsDataStore.edit { prefs ->
            val current = decodeTasks(prefs[KEY_TASKS].orEmpty()).map { task ->
                if (task.id == taskId) task.copy(completed = !task.completed) else task
            }
            prefs[KEY_TASKS] = encodeTasks(current)
        }
    }

    suspend fun removeTask(taskId: String) {
        context.nativeWidgetsDataStore.edit { prefs ->
            val updated = decodeTasks(prefs[KEY_TASKS].orEmpty()).filterNot { it.id == taskId }
            prefs[KEY_TASKS] = encodeTasks(updated)
        }
    }

    suspend fun clearCompletedTasks() {
        context.nativeWidgetsDataStore.edit { prefs ->
            val updated = decodeTasks(prefs[KEY_TASKS].orEmpty()).filterNot { it.completed }
            prefs[KEY_TASKS] = encodeTasks(updated)
        }
    }

    // --- Apps frequentes ---

    suspend fun recordAppLaunch(appKey: String) {
        context.nativeWidgetsDataStore.edit { prefs ->
            val counts = decodeLaunchCounts(prefs[KEY_APP_LAUNCH_COUNTS].orEmpty()).toMutableMap()
            counts[appKey] = (counts[appKey] ?: 0) + 1
            prefs[KEY_APP_LAUNCH_COUNTS] = encodeLaunchCounts(counts)
        }
    }

    // --- Clima (Open-Meteo) ---

    suspend fun searchWeatherCities(query: String): Result<List<WeatherLocation>> =
        weatherClient.searchCities(query)

    suspend fun setWeatherLocationAndRefresh(location: WeatherLocation) {
        context.nativeWidgetsDataStore.edit { prefs ->
            prefs[KEY_WEATHER_LOCATION] = encodeLocation(location)
        }
        refreshWeather(force = true, explicitLocation = location)
    }

    suspend fun refreshWeather(
        force: Boolean = false,
        explicitLocation: WeatherLocation? = null,
    ) {
        if (weatherLoading.value) return
        val current = storedState.first()
        val targetLocation = explicitLocation ?: current.weatherLocation ?: return
        val cached = current.weather
        val now = System.currentTimeMillis()
        if (!force &&
            cached != null &&
            cached.location.latitude == targetLocation.latitude &&
            cached.location.longitude == targetLocation.longitude &&
            (now - cached.updatedAtMs) < WEATHER_TTL_MS
        ) {
            return
        }

        weatherLoading.value = true
        weatherError.value = null
        val result = weatherClient.fetchForecast(location = targetLocation, nowMs = now)
        result.fold(
            onSuccess = { snapshot ->
                context.nativeWidgetsDataStore.edit { prefs ->
                    prefs[KEY_WEATHER_CACHE] = encodeWeather(snapshot)
                }
                weatherError.value = null
            },
            onFailure = { error ->
                weatherError.value = error.localizedMessage ?: "Falha ao atualizar clima"
            },
        )
        weatherLoading.value = false
    }

    // --- Timer e Cronômetro ---

    suspend fun setClockToolMode(mode: ClockToolMode) {
        context.nativeWidgetsDataStore.edit { prefs ->
            val current = decodeTimerStopwatch(prefs[KEY_TIMER_STOPWATCH].orEmpty())
            prefs[KEY_TIMER_STOPWATCH] = encodeTimerStopwatch(current.copy(mode = mode))
        }
    }

    suspend fun setTimerDuration(durationMs: Long) {
        val safeDuration = durationMs.coerceIn(10_000L, 12 * 60 * 60_000L)
        context.nativeWidgetsDataStore.edit { prefs ->
            val current = decodeTimerStopwatch(prefs[KEY_TIMER_STOPWATCH].orEmpty())
            prefs[KEY_TIMER_STOPWATCH] = encodeTimerStopwatch(
                current.copy(
                    mode = ClockToolMode.TIMER,
                    timerDurationMs = safeDuration,
                    timerRemainingMs = safeDuration,
                    timerRunning = false,
                    timerTargetElapsedMs = 0L,
                ),
            )
        }
    }

    suspend fun toggleTimer(nowElapsedMs: Long = SystemClock.elapsedRealtime()) {
        context.nativeWidgetsDataStore.edit { prefs ->
            val current = decodeTimerStopwatch(prefs[KEY_TIMER_STOPWATCH].orEmpty())
            val next = if (current.timerRunning) {
                val remaining = (current.timerTargetElapsedMs - nowElapsedMs).coerceAtLeast(0L)
                current.copy(
                    timerRunning = false,
                    timerRemainingMs = remaining,
                    timerTargetElapsedMs = 0L,
                )
            } else {
                val baseRemaining = if (current.timerRemainingMs > 0L) {
                    current.timerRemainingMs
                } else {
                    current.timerDurationMs
                }
                current.copy(
                    timerRunning = true,
                    timerRemainingMs = baseRemaining,
                    timerTargetElapsedMs = nowElapsedMs + baseRemaining,
                )
            }
            prefs[KEY_TIMER_STOPWATCH] = encodeTimerStopwatch(next)
        }
    }

    suspend fun resetTimer() {
        context.nativeWidgetsDataStore.edit { prefs ->
            val current = decodeTimerStopwatch(prefs[KEY_TIMER_STOPWATCH].orEmpty())
            prefs[KEY_TIMER_STOPWATCH] = encodeTimerStopwatch(
                current.copy(
                    timerRunning = false,
                    timerRemainingMs = current.timerDurationMs,
                    timerTargetElapsedMs = 0L,
                ),
            )
        }
    }

    suspend fun finishTimerIfElapsed(nowElapsedMs: Long = SystemClock.elapsedRealtime()) {
        context.nativeWidgetsDataStore.edit { prefs ->
            val current = decodeTimerStopwatch(prefs[KEY_TIMER_STOPWATCH].orEmpty())
            if (current.timerRunning && nowElapsedMs >= current.timerTargetElapsedMs) {
                prefs[KEY_TIMER_STOPWATCH] = encodeTimerStopwatch(
                    current.copy(
                        timerRunning = false,
                        timerRemainingMs = 0L,
                        timerTargetElapsedMs = 0L,
                    ),
                )
            }
        }
    }

    suspend fun toggleStopwatch(nowElapsedMs: Long = SystemClock.elapsedRealtime()) {
        context.nativeWidgetsDataStore.edit { prefs ->
            val current = decodeTimerStopwatch(prefs[KEY_TIMER_STOPWATCH].orEmpty())
            val next = if (current.stopwatchRunning) {
                val total = current.stopwatchAccumulatedMs + (nowElapsedMs - current.stopwatchStartElapsedMs).coerceAtLeast(0L)
                current.copy(
                    stopwatchRunning = false,
                    stopwatchAccumulatedMs = total,
                    stopwatchStartElapsedMs = 0L,
                )
            } else {
                current.copy(
                    stopwatchRunning = true,
                    stopwatchStartElapsedMs = nowElapsedMs,
                )
            }
            prefs[KEY_TIMER_STOPWATCH] = encodeTimerStopwatch(next)
        }
    }

    suspend fun resetStopwatch() {
        context.nativeWidgetsDataStore.edit { prefs ->
            val current = decodeTimerStopwatch(prefs[KEY_TIMER_STOPWATCH].orEmpty())
            prefs[KEY_TIMER_STOPWATCH] = encodeTimerStopwatch(
                current.copy(
                    stopwatchRunning = false,
                    stopwatchAccumulatedMs = 0L,
                    stopwatchStartElapsedMs = 0L,
                ),
            )
        }
    }

    private fun encodeNotes(notes: List<QuickNoteItem>): String {
        val arr = JSONArray()
        for (note in notes) {
            arr.put(
                JSONObject().apply {
                    put("id", note.id)
                    put("content", note.content)
                    put("updatedAtMs", note.updatedAtMs)
                },
            )
        }
        return arr.toString()
    }

    private fun decodeNotes(raw: String): List<QuickNoteItem> {
        if (raw.isBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            buildList(arr.length()) {
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    val content = obj.optString("content")
                    if (content.isNotBlank()) {
                        add(
                            QuickNoteItem(
                                id = obj.optString("id", UUID.randomUUID().toString()),
                                content = content,
                                updatedAtMs = obj.optLong("updatedAtMs", 0L),
                            ),
                        )
                    }
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun encodeTasks(tasks: List<QuickTaskItem>): String {
        val arr = JSONArray()
        for (task in tasks) {
            arr.put(
                JSONObject().apply {
                    put("id", task.id)
                    put("title", task.title)
                    put("completed", task.completed)
                },
            )
        }
        return arr.toString()
    }

    private fun decodeTasks(raw: String): List<QuickTaskItem> {
        if (raw.isBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            buildList(arr.length()) {
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    val title = obj.optString("title")
                    if (title.isNotBlank()) {
                        add(
                            QuickTaskItem(
                                id = obj.optString("id", UUID.randomUUID().toString()),
                                title = title,
                                completed = obj.optBoolean("completed", false),
                            ),
                        )
                    }
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun encodeLocation(location: WeatherLocation): String = JSONObject().apply {
        put("name", location.name)
        put("region", location.region)
        put("country", location.country)
        put("latitude", location.latitude)
        put("longitude", location.longitude)
    }.toString()

    private fun decodeLocation(raw: String): WeatherLocation? {
        if (raw.isBlank()) return null
        return runCatching {
            val obj = JSONObject(raw)
            WeatherLocation(
                name = obj.getString("name"),
                region = obj.optString("region"),
                country = obj.optString("country"),
                latitude = obj.getDouble("latitude"),
                longitude = obj.getDouble("longitude"),
            )
        }.getOrNull()
    }

    private fun encodeWeather(weather: WeatherSnapshot): String {
        val hourlyArr = JSONArray()
        for (point in weather.hourlyForecast) {
            hourlyArr.put(
                JSONObject().apply {
                    put("timeIso", point.timeIso)
                    put("hourLabel", point.hourLabel)
                    put("temperatureC", point.temperatureC)
                    put("weatherCode", point.weatherCode)
                },
            )
        }
        return JSONObject().apply {
            put("location", JSONObject(encodeLocation(weather.location)))
            put("temperatureC", weather.temperatureC)
            put("apparentTempC", weather.apparentTempC)
            put("minTempC", weather.minTempC)
            put("maxTempC", weather.maxTempC)
            put("weatherCode", weather.weatherCode)
            put("isDay", weather.isDay)
            put("windSpeedKmh", weather.windSpeedKmh)
            put("hourly", hourlyArr)
            put("updatedAtMs", weather.updatedAtMs)
        }.toString()
    }

    private fun decodeWeather(raw: String): WeatherSnapshot? {
        if (raw.isBlank()) return null
        return runCatching {
            val obj = JSONObject(raw)
            val loc = decodeLocation(obj.getJSONObject("location").toString()) ?: return@runCatching null
            val hourlyArr = obj.optJSONArray("hourly")
            val hourly = buildList {
                if (hourlyArr != null) {
                    for (i in 0 until hourlyArr.length()) {
                        val h = hourlyArr.optJSONObject(i) ?: continue
                        add(
                            HourlyWeatherPoint(
                                timeIso = h.optString("timeIso"),
                                hourLabel = h.optString("hourLabel"),
                                temperatureC = h.optDouble("temperatureC"),
                                weatherCode = h.optInt("weatherCode"),
                            ),
                        )
                    }
                }
            }
            WeatherSnapshot(
                location = loc,
                temperatureC = obj.getDouble("temperatureC"),
                apparentTempC = obj.optDouble("apparentTempC", obj.getDouble("temperatureC")),
                minTempC = obj.optDouble("minTempC", obj.getDouble("temperatureC")),
                maxTempC = obj.optDouble("maxTempC", obj.getDouble("temperatureC")),
                weatherCode = obj.optInt("weatherCode", 0),
                isDay = obj.optBoolean("isDay", true),
                windSpeedKmh = obj.optDouble("windSpeedKmh", 0.0),
                hourlyForecast = hourly,
                updatedAtMs = obj.optLong("updatedAtMs", 0L),
            )
        }.getOrNull()
    }

    private fun encodeLaunchCounts(counts: Map<String, Int>): String {
        val obj = JSONObject()
        for ((key, value) in counts) {
            obj.put(key, value)
        }
        return obj.toString()
    }

    private fun decodeLaunchCounts(raw: String): Map<String, Int> {
        if (raw.isBlank()) return emptyMap()
        return runCatching {
            val obj = JSONObject(raw)
            buildMap {
                val keys = obj.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    val count = obj.optInt(key, 0)
                    if (count > 0) put(key, count)
                }
            }
        }.getOrDefault(emptyMap())
    }

    private fun encodeTimerStopwatch(state: TimerStopwatchState): String = JSONObject().apply {
        put("mode", state.mode.name)
        put("timerDurationMs", state.timerDurationMs)
        put("timerRemainingMs", state.timerRemainingMs)
        put("timerRunning", state.timerRunning)
        put("timerTargetElapsedMs", state.timerTargetElapsedMs)
        put("stopwatchAccumulatedMs", state.stopwatchAccumulatedMs)
        put("stopwatchRunning", state.stopwatchRunning)
        put("stopwatchStartElapsedMs", state.stopwatchStartElapsedMs)
    }.toString()

    private fun decodeTimerStopwatch(raw: String): TimerStopwatchState {
        if (raw.isBlank()) return TimerStopwatchState()
        return runCatching {
            val obj = JSONObject(raw)
            val mode = ClockToolMode.entries.firstOrNull { it.name == obj.optString("mode") }
                ?: ClockToolMode.TIMER
            TimerStopwatchState(
                mode = mode,
                timerDurationMs = obj.optLong("timerDurationMs", 5 * 60_000L),
                timerRemainingMs = obj.optLong("timerRemainingMs", 5 * 60_000L),
                timerRunning = obj.optBoolean("timerRunning", false),
                timerTargetElapsedMs = obj.optLong("timerTargetElapsedMs", 0L),
                stopwatchAccumulatedMs = obj.optLong("stopwatchAccumulatedMs", 0L),
                stopwatchRunning = obj.optBoolean("stopwatchRunning", false),
                stopwatchStartElapsedMs = obj.optLong("stopwatchStartElapsedMs", 0L),
            )
        }.getOrDefault(TimerStopwatchState())
    }

    private companion object {
        val KEY_NOTES = stringPreferencesKey("quick_notes_json")
        val KEY_TASKS = stringPreferencesKey("quick_tasks_json")
        val KEY_WEATHER_LOCATION = stringPreferencesKey("weather_location_json")
        val KEY_WEATHER_CACHE = stringPreferencesKey("weather_cache_json")
        val KEY_TIMER_STOPWATCH = stringPreferencesKey("timer_stopwatch_json")
        val KEY_APP_LAUNCH_COUNTS = stringPreferencesKey("app_launch_counts_json")
        const val MAX_NOTES = 12
        const val MAX_TASKS = 30
        const val WEATHER_TTL_MS = 30 * 60_000L
    }
}
