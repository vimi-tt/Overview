package io.github.vimitt.overview.data

import io.github.vimitt.overview.domain.model.HourlyWeatherPoint
import io.github.vimitt.overview.domain.model.WeatherLocation
import io.github.vimitt.overview.domain.model.WeatherProvider
import io.github.vimitt.overview.domain.model.WeatherSnapshot
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
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
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return@withContext Result.success(emptyList())
        val lang = Locale.getDefault().language.ifBlank { "en" }
        val encoded = URLEncoder.encode(trimmed, Charsets.UTF_8.name())
        val primary = runCatching {
            val endpoint =
                "https://geocoding-api.open-meteo.com/v1/search?name=$encoded&count=6&language=$lang&format=json"
            parseGeocodingJson(httpGet(endpoint, "Open-Meteo Geocoding"))
        }
        if (primary.isSuccess && primary.getOrNull().orEmpty().isNotEmpty()) {
            return@withContext primary
        }
        runCatching<List<WeatherLocation>> {
            val nominatim =
                "https://nominatim.openstreetmap.org/search?q=$encoded&format=jsonv2&limit=6&accept-language=$lang&addressdetails=1"
            parseNominatimSearchJson(httpGet(nominatim, "Nominatim"))
        }.recoverCatching { err ->
            primary.getOrNull() ?: throw (primary.exceptionOrNull() ?: err)
        }
    }

    suspend fun fetchForecast(
        location: WeatherLocation,
        provider: WeatherProvider = WeatherProvider.AUTO,
        nowMs: Long = System.currentTimeMillis(),
    ): Result<WeatherSnapshot> = withContext(ioDispatcher) {
        val lat = String.format(Locale.US, "%.4f", location.latitude)
        val lon = String.format(Locale.US, "%.4f", location.longitude)

        when (provider) {
            WeatherProvider.OPEN_METEO -> fetchOpenMeteo(location, lat, lon, nowMs)
            WeatherProvider.MET_NORWAY -> fetchMetNorway(location, lat, lon, nowMs)
            WeatherProvider.WTTR_IN -> fetchWttrIn(location, lat, lon, nowMs)
            WeatherProvider.AUTO -> {
                val metResult = fetchMetNorway(location, lat, lon, nowMs)
                if (metResult.isSuccess) return@withContext metResult
                val openMeteoResult = fetchOpenMeteo(location, lat, lon, nowMs)
                if (openMeteoResult.isSuccess) return@withContext openMeteoResult
                fetchWttrIn(location, lat, lon, nowMs).recoverCatching { wttrErr ->
                    throw (metResult.exceptionOrNull() ?: openMeteoResult.exceptionOrNull() ?: wttrErr)
                }
            }
        }
    }

    private fun fetchOpenMeteo(
        location: WeatherLocation,
        lat: String,
        lon: String,
        nowMs: Long,
    ): Result<WeatherSnapshot> {
        val primaryEndpoint =
            "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon" +
                "&current=temperature_2m,apparent_temperature,weather_code,is_day,wind_speed_10m" +
                "&hourly=temperature_2m,weather_code" +
                "&daily=temperature_2m_max,temperature_2m_min" +
                "&timezone=auto&forecast_days=2"
        val primaryResult = runCatching {
            parseOpenMeteoForecastJson(json = httpGet(primaryEndpoint, "Open-Meteo"), location = location, nowMs = nowMs)
        }
        if (primaryResult.isSuccess) return primaryResult

        val minimalEndpoint =
            "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon" +
                "&current=temperature_2m,weather_code,is_day&timezone=UTC"
        return runCatching {
            parseOpenMeteoForecastJson(json = httpGet(minimalEndpoint, "Open-Meteo"), location = location, nowMs = nowMs)
        }.recoverCatching {
            throw (primaryResult.exceptionOrNull() ?: it)
        }
    }

    private fun fetchMetNorway(
        location: WeatherLocation,
        lat: String,
        lon: String,
        nowMs: Long,
    ): Result<WeatherSnapshot> = runCatching {
        val endpoint = "https://api.met.no/weatherapi/locationforecast/2.0/compact?lat=$lat&lon=$lon"
        parseMetNorwayCompactJson(json = httpGet(endpoint, "MET Norway"), location = location, nowMs = nowMs)
    }

    private fun fetchWttrIn(
        location: WeatherLocation,
        lat: String,
        lon: String,
        nowMs: Long,
    ): Result<WeatherSnapshot> = runCatching {
        val endpoint = "https://wttr.in/$lat,$lon?format=j1"
        parseWttrInJson(json = httpGet(endpoint, "wttr.in"), location = location, nowMs = nowMs)
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
        val lang = Locale.getDefault().language.ifBlank { "en" }
        runCatching {
            val endpoint =
                "https://api.bigdatacloud.net/data/reverse-geocode-client?latitude=$lat&longitude=$lon&localityLanguage=$lang"
            parseReverseGeocodeCityJson(httpGet(endpoint))
        }.getOrNull()
    }

    private fun httpGet(urlString: String, serviceName: String = "Weather"): String {
        val connection = (URL(urlString).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            instanceFollowRedirects = true
            connectTimeout = 10_000
            readTimeout = 10_000
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "OverviewLauncher/1.0 (https://github.com/vimitt/Overview)")
        }
        return try {
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.bufferedReader()?.use(BufferedReader::readText).orEmpty()
            check(code in 200..299) { "$serviceName HTTP $code" }
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
            val temperatureC = current.getDouble("temperature_2m").takeIf { it.isFinite() }
                ?: error("Temperatura atual ausente")
            val apparentTempC = current.optDouble("apparent_temperature", temperatureC)
                .takeIf { it.isFinite() } ?: temperatureC
            val weatherCode = current.optInt("weather_code", 0)
            val isDay = current.optInt("is_day", 1) == 1
            val windSpeed = current.optDouble("wind_speed_10m", 0.0)
                .takeIf { it.isFinite() } ?: 0.0

            val daily = root.optJSONObject("daily")
            val minArr = daily?.optJSONArray("temperature_2m_min")
            val maxArr = daily?.optJSONArray("temperature_2m_max")
            val minTemp = (if (minArr != null && minArr.length() > 0) minArr.optDouble(0, temperatureC) else temperatureC)
                .takeIf { it.isFinite() } ?: temperatureC
            val maxTemp = (if (maxArr != null && maxArr.length() > 0) maxArr.optDouble(0, temperatureC) else temperatureC)
                .takeIf { it.isFinite() } ?: temperatureC

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
                        val hourTemp = temps.optDouble(i, temperatureC).takeIf { it.isFinite() } ?: temperatureC
                        add(
                            HourlyWeatherPoint(
                                timeIso = timeIso,
                                hourLabel = hourLabel,
                                temperatureC = hourTemp,
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
                sourceLabel = "Open-Meteo",
            )
        }

        fun parseNominatimSearchJson(json: String): List<WeatherLocation> {
            if (json.isBlank()) return emptyList()
            val arr = JSONArray(json)
            return buildList(arr.length()) {
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    val lat = obj.optString("lat").toDoubleOrNull() ?: continue
                    val lon = obj.optString("lon").toDoubleOrNull() ?: continue
                    val addr = obj.optJSONObject("address")
                    val city = listOfNotNull(
                        addr?.optString("city"),
                        addr?.optString("town"),
                        addr?.optString("village"),
                        addr?.optString("municipality"),
                        obj.optString("name"),
                    ).firstOrNull { it.isNotBlank() } ?: continue
                    add(
                        WeatherLocation(
                            name = city,
                            region = addr?.optString("state").orEmpty(),
                            country = addr?.optString("country").orEmpty(),
                            latitude = lat,
                            longitude = lon,
                        ),
                    )
                }
            }
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

        fun parseMetNorwayCompactJson(
            json: String,
            location: WeatherLocation,
            nowMs: Long,
        ): WeatherSnapshot {
            val root = JSONObject(json)
            val timeseries = root.getJSONObject("properties").getJSONArray("timeseries")
            check(timeseries.length() > 0) { "Previsão vazia" }

            val first = timeseries.getJSONObject(0)
            val firstData = first.getJSONObject("data")
            val instantDetails = firstData.getJSONObject("instant").getJSONObject("details")
            val temperatureC = instantDetails.getDouble("air_temperature").takeIf { it.isFinite() }
                ?: error("Temperatura ausente")
            val windSpeedKmh = (instantDetails.optDouble("wind_speed", 0.0).takeIf { it.isFinite() } ?: 0.0) * 3.6
            val firstSymbol = firstData.optJSONObject("next_1_hours")
                ?.optJSONObject("summary")
                ?.optString("symbol_code")
                .orEmpty()
            val (weatherCode, isDay) = mapMetSymbolToWmo(firstSymbol)

            var minTemp = temperatureC
            var maxTemp = temperatureC
            val hourlyPoints = mutableListOf<HourlyWeatherPoint>()
            val scanLimit = minOf(timeseries.length(), 24)
            for (i in 0 until scanLimit) {
                val entry = timeseries.optJSONObject(i) ?: continue
                val data = entry.optJSONObject("data") ?: continue
                val temp = data.optJSONObject("instant")
                    ?.optJSONObject("details")
                    ?.optDouble("air_temperature", temperatureC)
                    ?.takeIf { it.isFinite() }
                    ?: continue
                if (temp < minTemp) minTemp = temp
                if (temp > maxTemp) maxTemp = temp

                if (i in 1..4) {
                    val timeIso = entry.optString("time")
                    val hourLabel = if (timeIso.length >= 16) timeIso.substring(11, 16) else "${i}h"
                    val sym = data.optJSONObject("next_1_hours")
                        ?.optJSONObject("summary")
                        ?.optString("symbol_code")
                        .orEmpty()
                    val (wmo, _) = mapMetSymbolToWmo(sym.ifBlank { firstSymbol })
                    hourlyPoints += HourlyWeatherPoint(
                        timeIso = timeIso,
                        hourLabel = hourLabel,
                        temperatureC = temp,
                        weatherCode = wmo,
                    )
                }
            }

            return WeatherSnapshot(
                location = location,
                temperatureC = temperatureC,
                apparentTempC = temperatureC,
                minTempC = minTemp,
                maxTempC = maxTemp,
                weatherCode = weatherCode,
                isDay = isDay,
                windSpeedKmh = windSpeedKmh,
                hourlyForecast = hourlyPoints,
                updatedAtMs = nowMs,
                sourceLabel = "MET Norway",
            )
        }

        private fun mapMetSymbolToWmo(symbol: String): Pair<Int, Boolean> {
            val lower = symbol.lowercase(Locale.US)
            val isDay = !lower.endsWith("_night")
            val wmo = when {
                "thunder" in lower -> 95
                "snow" in lower || "sleet" in lower -> 71
                "heavyrain" in lower -> 65
                "rain" in lower -> 61
                "fog" in lower -> 45
                "cloudy" in lower && "partly" !in lower -> 3
                "partlycloudy" in lower || "fair" in lower -> 2
                "clearsky" in lower -> 0
                else -> 2
            }
            return wmo to isDay
        }

        fun parseWttrInJson(
            json: String,
            location: WeatherLocation,
            nowMs: Long,
        ): WeatherSnapshot {
            val root = JSONObject(json)
            val currentArr = root.getJSONArray("current_condition")
            check(currentArr.length() > 0) { "Sem condição atual em wttr.in" }
            val current = currentArr.getJSONObject(0)
            val tempC = current.getString("temp_C").toDoubleOrNull()?.takeIf { it.isFinite() }
                ?: error("Temperatura ausente em wttr.in")
            val feelsLikeC = current.optString("FeelsLikeC").toDoubleOrNull()?.takeIf { it.isFinite() } ?: tempC
            val windKmh = current.optString("windspeedKmph").toDoubleOrNull()?.takeIf { it.isFinite() } ?: 0.0
            val weatherCode = mapWwoCodeToWmo(current.optString("weatherCode").toIntOrNull() ?: 113)

            val daysArr = root.optJSONArray("weather")
            val today = daysArr?.optJSONObject(0)
            val minTemp = today?.optString("mintempC")?.toDoubleOrNull()?.takeIf { it.isFinite() } ?: tempC
            val maxTemp = today?.optString("maxtempC")?.toDoubleOrNull()?.takeIf { it.isFinite() } ?: tempC

            val hourlyPoints = mutableListOf<HourlyWeatherPoint>()
            val hourlyArr = today?.optJSONArray("hourly")
            if (hourlyArr != null) {
                for (i in 0 until minOf(hourlyArr.length(), 4)) {
                    val h = hourlyArr.optJSONObject(i) ?: continue
                    val rawTime = h.optString("time").padStart(4, '0')
                    val hourLabel = if (rawTime.length >= 4) "${rawTime.substring(0, 2)}:${rawTime.substring(2, 4)}" else "${i * 3}h"
                    val hTemp = h.optString("tempC").toDoubleOrNull()?.takeIf { it.isFinite() } ?: tempC
                    val hCode = mapWwoCodeToWmo(h.optString("weatherCode").toIntOrNull() ?: 113)
                    hourlyPoints += HourlyWeatherPoint(
                        timeIso = rawTime,
                        hourLabel = hourLabel,
                        temperatureC = hTemp,
                        weatherCode = hCode,
                    )
                }
            }

            return WeatherSnapshot(
                location = location,
                temperatureC = tempC,
                apparentTempC = feelsLikeC,
                minTempC = minTemp,
                maxTempC = maxTemp,
                weatherCode = weatherCode,
                isDay = true,
                windSpeedKmh = windKmh,
                hourlyForecast = hourlyPoints,
                updatedAtMs = nowMs,
                sourceLabel = "wttr.in",
            )
        }

        private fun mapWwoCodeToWmo(code: Int): Int = when (code) {
            113 -> 0
            116 -> 2
            119, 122 -> 3
            143, 248, 260 -> 45
            176, 263, 266, 281, 293, 296, 353 -> 51
            299, 302, 305, 308, 356, 359 -> 61
            179, 227, 230, 323, 326, 329, 332, 335, 338, 368, 371 -> 71
            200, 386, 389, 392, 395 -> 95
            else -> 2
        }
    }
}
