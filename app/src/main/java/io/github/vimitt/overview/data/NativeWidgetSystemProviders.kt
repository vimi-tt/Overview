package io.github.vimitt.overview.data

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.content.ComponentName
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.Bitmap
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.CancellationSignal
import android.os.Environment
import android.os.Looper
import android.os.StatFs
import android.provider.AlarmClock
import android.provider.CalendarContract
import android.provider.Settings
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.content.ContextCompat
import io.github.vimitt.overview.R
import io.github.vimitt.overview.domain.model.CalendarEventItem
import io.github.vimitt.overview.domain.model.DeviceStatusSnapshot
import io.github.vimitt.overview.domain.model.WeatherLocation
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale

@Immutable
data class MediaSessionSnapshot(
    val title: String = "",
    val artist: String = "",
    val packageName: String = "",
    val isPlaying: Boolean = false,
    val artwork: ImageBitmap? = null,
    val controller: MediaController? = null,
    val hasNotificationAccess: Boolean = false,
)

/**
 * Provedores sob demanda de dados do sistema Android (alarme, agenda, bateria,
 * armazenamento, rede, localização aproximada e controle de mídia) usados pelos widgets nativos.
 */
object NativeWidgetSystemProviders {

    fun readNextAlarmTimeMillis(context: Context): Long? {
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return null
        return alarmManager.nextAlarmClock?.triggerTime
    }

    fun openAlarmsApp(context: Context) {
        val showAlarms = Intent(AlarmClock.ACTION_SHOW_ALARMS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val fallback = Intent(Settings.ACTION_DATE_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(showAlarms) }
            .onFailure { runCatching { context.startActivity(fallback) } }
    }

    fun hasCalendarPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CALENDAR,
        ) == PackageManager.PERMISSION_GRANTED

    fun hasCoarseLocationPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION,
            ) == PackageManager.PERMISSION_GRANTED

    private fun hasFineLocationPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED

    fun isLocationEnabled(context: Context): Boolean {
        val lm = context.getSystemService(LocationManager::class.java) ?: return false
        return runCatching { lm.isLocationEnabled }.getOrDefault(false)
    }

    fun openLocationSourceSettings(context: Context) {
        val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }
    }

    fun openAppPermissionSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }
    }

    suspend fun readUpcomingCalendarEvents(
        context: Context,
        limit: Int = 5,
    ): List<CalendarEventItem> = withContext(Dispatchers.IO) {
        if (!hasCalendarPermission(context)) return@withContext emptyList()

        runCatching {
            val now = System.currentTimeMillis()
            val windowStart = now - 10 * 60_000L
            val windowEnd = now + 14L * 24 * 60 * 60_000L
            val builder = CalendarContract.Instances.CONTENT_URI.buildUpon()
            ContentUris.appendId(builder, windowStart)
            ContentUris.appendId(builder, windowEnd)
            val uri = builder.build()

            val projection = arrayOf(
                CalendarContract.Instances.EVENT_ID,
                CalendarContract.Instances.TITLE,
                CalendarContract.Instances.BEGIN,
                CalendarContract.Instances.END,
                CalendarContract.Instances.ALL_DAY,
                CalendarContract.Instances.EVENT_LOCATION,
            )

            // Alguns provedores OEM não expõem Instances.VISIBLE na seleção; tenta filtrar visíveis e recua se preciso.
            val cursor = runCatching {
                context.contentResolver.query(
                    uri,
                    projection,
                    "${CalendarContract.Instances.VISIBLE} = 1",
                    null,
                    "${CalendarContract.Instances.BEGIN} ASC",
                )
            }.getOrElse {
                context.contentResolver.query(
                    uri,
                    projection,
                    null,
                    null,
                    "${CalendarContract.Instances.BEGIN} ASC",
                )
            }

            val events = mutableListOf<CalendarEventItem>()
            cursor?.use { c ->
                val idIdx = c.getColumnIndex(CalendarContract.Instances.EVENT_ID)
                val titleIdx = c.getColumnIndex(CalendarContract.Instances.TITLE)
                val beginIdx = c.getColumnIndex(CalendarContract.Instances.BEGIN)
                val endIdx = c.getColumnIndex(CalendarContract.Instances.END)
                val allDayIdx = c.getColumnIndex(CalendarContract.Instances.ALL_DAY)
                val locIdx = c.getColumnIndex(CalendarContract.Instances.EVENT_LOCATION)

                while (c.moveToNext() && events.size < limit) {
                    val eventId = c.safeLongOrNull(idIdx) ?: continue
                    val beginMs = c.safeLongOrNull(beginIdx) ?: continue
                    val endMs = c.safeLongOrNull(endIdx) ?: beginMs
                    if (endMs < now) continue
                    val title = c.safeStringOrEmpty(titleIdx).trim()
                    val allDay = c.safeIntOrZero(allDayIdx) == 1
                    val location = c.safeStringOrEmpty(locIdx).trim()
                    events += CalendarEventItem(
                        id = eventId,
                        title = title.ifBlank { context.getString(R.string.native_widget_agenda_untitled) },
                        beginMs = beginMs,
                        endMs = endMs,
                        allDay = allDay,
                        location = location,
                    )
                }
            }
            events
        }.getOrDefault(emptyList())
    }

    private fun Cursor.safeLongOrNull(index: Int): Long? =
        if (index >= 0 && !isNull(index)) runCatching { getLong(index) }.getOrNull() else null

    private fun Cursor.safeIntOrZero(index: Int): Int =
        if (index >= 0 && !isNull(index)) runCatching { getInt(index) }.getOrDefault(0) else 0

    private fun Cursor.safeStringOrEmpty(index: Int): String =
        if (index >= 0 && !isNull(index)) runCatching { getString(index).orEmpty() }.getOrDefault("") else ""

    fun openCalendarEvent(context: Context, eventId: Long) {
        val uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = uri
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }
            .onFailure { openCalendarApp(context) }
    }

    fun openCalendarApp(context: Context) {
        val calIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_APP_CALENDAR)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(calIntent) }.onFailure {
            val viewTime = Intent(Intent.ACTION_VIEW).apply {
                data = CalendarContract.CONTENT_URI.buildUpon().appendPath("time").build()
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            runCatching { context.startActivity(viewTime) }
        }
    }

    @SuppressLint("MissingPermission")
    suspend fun resolveCoarseWeatherLocation(
        context: Context,
        weatherClient: OpenMeteoWeatherClient = OpenMeteoWeatherClient(),
    ): WeatherLocation? = withContext(Dispatchers.IO) {
        if (!hasCoarseLocationPermission(context)) return@withContext null

        val lm = context.getSystemService(LocationManager::class.java)
        val cachedLocation = lm?.let { readMostRecentLastKnownLocation(context, it) }
        val nowMs = System.currentTimeMillis()
        val isCachedFresh = cachedLocation != null && (nowMs - cachedLocation.time) <= 20 * 60_000L

        val hardwareLocation = when {
            isCachedFresh -> cachedLocation
            lm != null && isLocationEnabled(context) ->
                requestCurrentLocationAcrossProviders(context, lm) ?: cachedLocation
            else -> cachedLocation
        }

        if (hardwareLocation != null) {
            val resolvedCity = resolveCityLabel(context, hardwareLocation.latitude, hardwareLocation.longitude)
                ?: weatherClient.reverseGeocodeCity(hardwareLocation.latitude, hardwareLocation.longitude)
            return@withContext WeatherLocation(
                name = resolvedCity ?: context.getString(R.string.native_widget_weather_current_location),
                latitude = hardwareLocation.latitude,
                longitude = hardwareLocation.longitude,
            )
        }

        // Último recurso se os provedores do dispositivo não responderem (ex.: ambiente interno / sem Play Services)
        weatherClient.fetchApproximateIpLocation()
    }

    @SuppressLint("MissingPermission")
    private fun readMostRecentLastKnownLocation(
        context: Context,
        lm: LocationManager,
    ): Location? {
        val candidateProviders = buildSet {
            add(LocationManager.FUSED_PROVIDER)
            add(LocationManager.NETWORK_PROVIDER)
            add(LocationManager.PASSIVE_PROVIDER)
            if (hasFineLocationPermission(context) || Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                add(LocationManager.GPS_PROVIDER)
            }
            runCatching { lm.getProviders(true) }.getOrNull()?.forEach { add(it) }
        }
        return candidateProviders
            .mapNotNull { provider -> runCatching { lm.getLastKnownLocation(provider) }.getOrNull() }
            .maxByOrNull { it.time }
    }

    @SuppressLint("MissingPermission")
    private suspend fun requestCurrentLocationAcrossProviders(
        context: Context,
        lm: LocationManager,
    ): Location? {
        val enabledProviders = buildList {
            val discovered = runCatching { lm.getProviders(true) }.getOrNull().orEmpty()
            listOf(
                LocationManager.FUSED_PROVIDER,
                LocationManager.NETWORK_PROVIDER,
                LocationManager.GPS_PROVIDER,
            ).forEach { p ->
                if (runCatching { lm.isProviderEnabled(p) }.getOrDefault(false)) add(p)
            }
            discovered.forEach { p ->
                if (p != LocationManager.PASSIVE_PROVIDER && p !in this) add(p)
            }
        }
        if (enabledProviders.isEmpty()) return null

        return withTimeoutOrNull(6_500L) {
            coroutineScope {
                val winner = CompletableDeferred<Location>()
                val executor = ContextCompat.getMainExecutor(context)
                val signals = mutableListOf<CancellationSignal>()
                val listeners = mutableListOf<LocationListener>()

                for (provider in enabledProviders) {
                    val signal = CancellationSignal()
                    signals += signal
                    launch {
                        runCatching {
                            lm.getCurrentLocation(provider, signal, executor) { loc ->
                                if (loc != null) {
                                    winner.complete(loc)
                                }
                            }
                        }
                    }

                    // Fallback paralelo via LocationListener caso getCurrentLocation retorne null neste provedor
                    val listener = object : LocationListener {
                        override fun onLocationChanged(location: Location) {
                            winner.complete(location)
                        }
                    }
                    listeners += listener
                    runCatching {
                        lm.requestLocationUpdates(provider, 500L, 0f, listener, Looper.getMainLooper())
                    }
                }

                try {
                    winner.await()
                } finally {
                    signals.forEach { runCatching { it.cancel() } }
                    listeners.forEach { runCatching { lm.removeUpdates(it) } }
                }
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun resolveCityLabel(context: Context, lat: Double, lon: Double): String? {
        if (!Geocoder.isPresent()) return null
        return runCatching {
            val geocoder = Geocoder(context, Locale.getDefault())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                var city: String? = null
                val latch = java.util.concurrent.CountDownLatch(1)
                geocoder.getFromLocation(lat, lon, 1) { list ->
                    city = list.firstOrNull()?.let { it.locality ?: it.subAdminArea ?: it.adminArea }
                    latch.countDown()
                }
                latch.await(2, java.util.concurrent.TimeUnit.SECONDS)
                city
            } else {
                geocoder.getFromLocation(lat, lon, 1)
                    ?.firstOrNull()
                    ?.let { it.locality ?: it.subAdminArea ?: it.adminArea }
            }
        }.getOrNull()
    }

    fun readDeviceStatusSnapshot(
        context: Context,
        batteryIntent: Intent? = context.registerReceiver(
            null,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED),
        ),
    ): DeviceStatusSnapshot {
        val batteryManager = context.getSystemService(BatteryManager::class.java)
        val level = batteryIntent?.let { intent ->
            val raw = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
            if (raw >= 0 && scale > 0) ((raw * 100f) / scale).toInt().coerceIn(0, 100) else null
        } ?: batteryManager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)?.coerceIn(0, 100) ?: 0

        val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL

        val statFs = runCatching { StatFs(Environment.getDataDirectory().path) }.getOrNull()
        val totalBytes = statFs?.totalBytes ?: 0L
        val availableBytes = statFs?.availableBytes ?: 0L
        val usedBytes = (totalBytes - availableBytes).coerceAtLeast(0L)

        val cm = context.getSystemService(ConnectivityManager::class.java)
        val activeNetwork = cm?.activeNetwork
        val caps = activeNetwork?.let { cm.getNetworkCapabilities(it) }
        val isOnline = caps != null && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        val isWifi = caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
        val networkLabel = when {
            caps == null || !isOnline -> context.getString(R.string.native_widget_network_offline)
            isWifi -> context.getString(R.string.native_widget_network_wifi)
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ->
                context.getString(R.string.native_widget_network_cellular)
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) ->
                context.getString(R.string.native_widget_network_ethernet)
            else -> context.getString(R.string.native_widget_network_connected)
        }

        return DeviceStatusSnapshot(
            batteryLevel = level,
            isCharging = isCharging,
            storageUsedBytes = usedBytes,
            storageTotalBytes = totalBytes,
            networkLabel = networkLabel,
            isOnline = isOnline,
            isWifi = isWifi,
        )
    }

    fun openBatterySettings(context: Context) {
        val power = Intent(Intent.ACTION_POWER_USAGE_SUMMARY).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val settings = Intent(Settings.ACTION_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(power) }
            .onFailure { runCatching { context.startActivity(settings) } }
    }

    fun readMediaSnapshot(
        context: Context,
        controllers: List<MediaController>?,
        hasAccess: Boolean,
    ): MediaSessionSnapshot {
        if (!hasAccess) {
            return MediaSessionSnapshot(hasNotificationAccess = false)
        }
        val activeList = controllers ?: runCatching {
            val msm = context.getSystemService(MediaSessionManager::class.java)
            val component = ComponentName(context, OverviewNotificationListenerService::class.java)
            msm?.getActiveSessions(component)
        }.getOrNull().orEmpty()

        val controller = activeList.firstOrNull {
            it.playbackState?.state == PlaybackState.STATE_PLAYING
        } ?: activeList.firstOrNull()

        if (controller == null) {
            return MediaSessionSnapshot(hasNotificationAccess = true)
        }

        val metadata = controller.metadata
        val title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE).orEmpty().trim()
        val artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST)
            ?: metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)
            ?: ""
        val isPlaying = controller.playbackState?.state == PlaybackState.STATE_PLAYING
        val rawBitmap = metadata?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: metadata?.getBitmap(MediaMetadata.METADATA_KEY_ART)
            ?: metadata?.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON)
        val artwork = rawBitmap?.let { scaleDownArtwork(it, maxSizePx = 160)?.asImageBitmap() }

        return MediaSessionSnapshot(
            title = title.ifBlank { context.getString(R.string.native_widget_media_untitled) },
            artist = artist.trim(),
            packageName = controller.packageName.orEmpty(),
            isPlaying = isPlaying,
            artwork = artwork,
            controller = controller,
            hasNotificationAccess = true,
        )
    }

    fun openMediaSessionApp(context: Context, snapshot: MediaSessionSnapshot) {
        val pending = snapshot.controller?.sessionActivity
        if (pending != null) {
            val sent = runCatching { pending.send() }.isSuccess
            if (sent) return
        }
        val pkg = snapshot.packageName
        if (pkg.isNotBlank()) {
            val launch = context.packageManager.getLaunchIntentForPackage(pkg)?.apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (launch != null) {
                runCatching { context.startActivity(launch) }
            }
        }
    }

    private fun scaleDownArtwork(bitmap: Bitmap, maxSizePx: Int): Bitmap? {
        if (bitmap.isRecycled) return null
        val maxDim = maxOf(bitmap.width, bitmap.height)
        if (maxDim <= maxSizePx) return bitmap
        val ratio = maxSizePx.toFloat() / maxDim.toFloat()
        val targetW = (bitmap.width * ratio).toInt().coerceAtLeast(1)
        val targetH = (bitmap.height * ratio).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, targetW, targetH, true)
    }
}
