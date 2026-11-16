package io.github.vimitt.overview.data

import io.github.vimitt.overview.domain.model.HourlyWeatherPoint
import io.github.vimitt.overview.domain.model.WeatherLocation
import io.github.vimitt.overview.domain.model.WeatherSnapshot
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Cliente leve para a API aberta do Open-Meteo (previsão e geocodificação sem chave de API).
 */
class OpenMeteoWeatherClient(
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    suspend fun searchCities(query: String): Result<List<WeatherLocation>> = withContext(ioDispatcher) {
        runCatching {
            val trimmed = query.trim()
            if (trimmed.isEmpty()) return@runCatching emptyList()
            val encoded = URLEncoder.encode(trimmed, Charsets.UTF_8.name())
            val endpoint =
                "https://geocoding-api.open-meteo.com/v1/search?name=$encoded&count=6&language=pt&format=json"
            val body = httpGet(endpoint)
            parseGeocodingJson(body)
        }
    }

    suspend fun fetchForecast(
        location: WeatherLocation,
        nowMs: Long = System.currentTimeMillis(),
    ): Result<WeatherSnapshot> = withContext(ioDispatcher) {
        runCatching {
            val lat = String.format(Locale.US, "%.4f", location.latitude)
            val lon = String.format(Locale.US, "%.4f", location.longitude)
            val endpoint =
                "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon" +
                    "&current=temperature_2m,apparent_temperature,weather_code,is_day,wind_speed_10m" +
                    "&hourly=temperature_2m,weather_code" +
                    "&daily=temperature_2m_max,temperature_2m_min" +
                    "&timezone=auto&forecast_days=2"
            val body = httpGet(endpoint)
            parseOpenMeteoForecastJson(json = body, location = location, nowMs = nowMs)
        }
    }

    /**
     * Fallback leve por IP aproximado (sem chave de API) usado apenas quando os provedores
     * de localização do Android não conseguem obter fix em ambientes internos ou sem Play Services.
     */
    suspend fun fetchApproximateIpLocation(): WeatherLocation? = withContext(ioDispatcher) {
        runCatching { parseIpWhoIsJson(httpGet("https://ipwho.is/")) }.getOrNull()
            ?: runCatching { parseGeoJsJson(httpGet("https://get.geojs.io/v1/ip/geo.json")) }.getOrNull()
    }

    /**
     * Resolve o nome legível da cidade via geocodificação reversa sem chave quando `Geocoder` está indisponível.
     */
    suspend fun reverseGeocodeCity(latitude: Double, longitude: Double): String? = withContext(ioDispatcher) {
        val lat = String.format(Locale.US, "%.4f", latitude)
        val lon = String.format(Locale.US, "%.4f", longitude)
        runCatching {
            val endpoint =
                "https://api.bigdatacloud.net/data/reverse-geocode-client?latitude=$lat&longitude=$lon&localityLanguage=pt"
            parseReverseGeocodeCityJson(httpGet(endpoint))
        }.getOrNull()
    }

    private fun httpGet(urlString: String): String {
        val connection = (URL(urlString).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 9_000
            readTimeout = 9_000
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "OverviewLauncher/1.0")
        }
        return try {
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.bufferedReader()?.use(BufferedReader::readText).orEmpty()
            check(code in 200..299) { "HTTP $code ao consultar Open-Meteo" }
            text
        } finally {
            connection.disconnect()
        }
    }

    companion object {
        fun parseGeocodingJson(json: String): List<WeatherLocation> {
            if (json.isBlank()) return emptyList()
            val root = JSONObject(json)
            val results = root.optJSONArray("results") ?: return emptyList()
            return buildList(results.length()) {
                for (i in 0 until results.length()) {
                    val obj = results.optJSONObject(i) ?: continue
                    val name = obj.optString("name")
                    if (name.isBlank()) continue
                    add(
                        WeatherLocation(
                            name = name,
                            region = obj.optString("admin1"),
                            country = obj.optString("country"),
                            latitude = obj.getDouble("latitude"),
                            longitude = obj.getDouble("longitude"),
                        ),
                    )
                }
            }
        }

        fun parseOpenMeteoForecastJson(
            json: String,
            location: WeatherLocation,
            nowMs: Long,
        ): WeatherSnapshot {
            val root = JSONObject(json)
            val current = root.getJSONObject("current")
            val temperatureC = current.getDouble("temperature_2m")
            val apparentTempC = current.optDouble("apparent_temperature", temperatureC)
            val weatherCode = current.optInt("weather_code", 0)
            val isDay = current.optInt("is_day", 1) == 1
            val windSpeed = current.optDouble("wind_speed_10m", 0.0)

            val daily = root.optJSONObject("daily")
            val minArr = daily?.optJSONArray("temperature_2m_min")
            val maxArr = daily?.optJSONArray("temperature_2m_max")
            val minTemp = if (minArr != null && minArr.length() > 0) minArr.optDouble(0, temperatureC) else temperatureC
            val maxTemp = if (maxArr != null && maxArr.length() > 0) maxArr.optDouble(0, temperatureC) else temperatureC

            val hourlyObj = root.optJSONObject("hourly")
            val times = hourlyObj?.optJSONArray("time")
            val temps = hourlyObj?.optJSONArray("temperature_2m")
            val codes = hourlyObj?.optJSONArray("weather_code")
            val currentHourIso = SimpleDateFormat("yyyy-MM-dd'T'HH:00", Locale.US).format(Date(nowMs))

            val hourlyPoints = buildList {
                if (times != null && temps != null && codes != null) {
                    val count = minOf(times.length(), temps.length(), codes.length())
                    for (i in 0 until count) {
                        val timeIso = times.optString(i)
                        if (timeIso.isBlank() || timeIso <= currentHourIso) continue
                        val hourLabel = if (timeIso.length >= 16) timeIso.substring(11, 16) else timeIso
                        add(
                            HourlyWeatherPoint(
                                timeIso = timeIso,
                                hourLabel = hourLabel,
                                temperatureC = temps.optDouble(i, temperatureC),
                                weatherCode = codes.optInt(i, weatherCode),
                            ),
                        )
                        if (size >= 4) break
                    }
                }
            }

            return WeatherSnapshot(
                location = location,
                temperatureC = temperatureC,
                apparentTempC = apparentTempC,
                minTempC = minTemp,
                maxTempC = maxTemp,
                weatherCode = weatherCode,
                isDay = isDay,
                windSpeedKmh = windSpeed,
                hourlyForecast = hourlyPoints,
                updatedAtMs = nowMs,
            )
        }

        fun parseIpWhoIsJson(json: String): WeatherLocation? {
            if (json.isBlank()) return null
            val root = JSONObject(json)
            if (!root.optBoolean("success", true)) return null
            val lat = root.optDouble("latitude", Double.NaN)
            val lon = root.optDouble("longitude", Double.NaN)
            if (lat.isNaN() || lon.isNaN()) return null
            val city = root.optString("city").trim()
                .ifBlank { root.optString("region").trim() }
            if (city.isBlank()) return null
            return WeatherLocation(
                name = city,
                region = root.optString("region").trim(),
                country = root.optString("country").trim(),
                latitude = lat,
                longitude = lon,
            )
        }

        fun parseGeoJsJson(json: String): WeatherLocation? {
            if (json.isBlank()) return null
            val root = JSONObject(json)
            val lat = root.optString("latitude").toDoubleOrNull()
                ?: root.optDouble("latitude", Double.NaN)
            val lon = root.optString("longitude").toDoubleOrNull()
                ?: root.optDouble("longitude", Double.NaN)
            if (lat.isNaN() || lon.isNaN()) return null
            val city = root.optString("city").trim()
                .ifBlank { root.optString("region").trim() }
            if (city.isBlank()) return null
            return WeatherLocation(
                name = city,
                region = root.optString("region").trim(),
                country = root.optString("country").trim(),
                latitude = lat,
                longitude = lon,
            )
        }

        fun parseReverseGeocodeCityJson(json: String): String? {
            if (json.isBlank()) return null
            val root = JSONObject(json)
            return root.optString("city").trim()
                .ifBlank { root.optString("locality").trim() }
                .ifBlank { root.optString("principalSubdivision").trim() }
                .ifBlank { null }
        }
    }
}
