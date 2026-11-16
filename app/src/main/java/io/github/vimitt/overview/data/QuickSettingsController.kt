package io.github.vimitt.overview.data

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.KeyEvent
import androidx.compose.runtime.Immutable
import androidx.core.content.ContextCompat

@Immutable
data class QuickSettingsSnapshot(
    val batteryLevel: Int = 0,
    val isCharging: Boolean = false,
    val wifiEnabled: Boolean = false,
    val wifiConnected: Boolean = false,
    val networkLabel: String = "",
    val isOnline: Boolean = false,
    val cellularConnected: Boolean = false,
    val bluetoothEnabled: Boolean = false,
    val airplaneModeEnabled: Boolean = false,
    val dndEnabled: Boolean = false,
    val hasDndPolicyAccess: Boolean = false,
    val autoRotateEnabled: Boolean = false,
    val canWriteSystemSettings: Boolean = false,
    val torchAvailable: Boolean = false,
    val torchEnabled: Boolean = false,
    val brightnessFraction: Float = 0.5f,
    val mediaVolume: Int = 0,
    val mediaMaxVolume: Int = 15,
    val mediaSnapshot: MediaSessionSnapshot = MediaSessionSnapshot(),
)

/**
 * Controlador de leitura e acionamento dos ajustes rápidos do dispositivo Android
 * para o painel próprio estilo GNOME.
 */
object QuickSettingsController {

    fun findTorchCameraId(context: Context): String? {
        val cm = context.getSystemService(CameraManager::class.java) ?: return null
        return runCatching {
            cm.cameraIdList.firstOrNull { id ->
                val chars = cm.getCameraCharacteristics(id)
                chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
        }.getOrNull()
    }

    fun readSnapshot(
        context: Context,
        torchEnabled: Boolean,
        torchAvailable: Boolean,
        hasNotificationAccess: Boolean,
    ): QuickSettingsSnapshot {
        val deviceStatus = NativeWidgetSystemProviders.readDeviceStatusSnapshot(context)

        val wifiManager = context.applicationContext.getSystemService(WifiManager::class.java)
        val wifiEnabled = runCatching { wifiManager?.isWifiEnabled == true }.getOrDefault(false)

        val cm = context.getSystemService(ConnectivityManager::class.java)
        val caps = cm?.activeNetwork?.let { cm.getNetworkCapabilities(it) }
        val wifiConnected = caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
        val cellularConnected = caps?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true

        val cr = context.contentResolver
        val bluetoothEnabled = runCatching {
            Settings.Global.getInt(cr, Settings.Global.BLUETOOTH_ON, 0) == 1
        }.getOrDefault(false)
        val airplaneModeEnabled = runCatching {
            Settings.Global.getInt(cr, Settings.Global.AIRPLANE_MODE_ON, 0) == 1
        }.getOrDefault(false)

        val nm = context.getSystemService(NotificationManager::class.java)
        val dndFilter = nm?.currentInterruptionFilter ?: NotificationManager.INTERRUPTION_FILTER_ALL
        val dndEnabled = dndFilter != NotificationManager.INTERRUPTION_FILTER_ALL &&
            dndFilter != NotificationManager.INTERRUPTION_FILTER_UNKNOWN
        val hasDndPolicyAccess = nm?.isNotificationPolicyAccessGranted == true

        val canWrite = Settings.System.canWrite(context)
        val autoRotateEnabled = runCatching {
            Settings.System.getInt(cr, Settings.System.ACCELEROMETER_ROTATION, 0) == 1
        }.getOrDefault(false)
        val brightnessRaw = runCatching {
            Settings.System.getInt(cr, Settings.System.SCREEN_BRIGHTNESS, 128)
        }.getOrDefault(128).coerceIn(0, 255)

        val audioManager = context.getSystemService(AudioManager::class.java)
        val maxVol = audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC)?.coerceAtLeast(1) ?: 15
        val currentVol = audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC)?.coerceIn(0, maxVol) ?: 0

        val mediaSnapshot = NativeWidgetSystemProviders.readMediaSnapshot(
            context = context,
            controllers = null,
            hasAccess = hasNotificationAccess,
        )

        return QuickSettingsSnapshot(
            batteryLevel = deviceStatus.batteryLevel,
            isCharging = deviceStatus.isCharging,
            wifiEnabled = wifiEnabled,
            wifiConnected = wifiConnected,
            networkLabel = deviceStatus.networkLabel,
            isOnline = deviceStatus.isOnline,
            cellularConnected = cellularConnected,
            bluetoothEnabled = bluetoothEnabled,
            airplaneModeEnabled = airplaneModeEnabled,
            dndEnabled = dndEnabled,
            hasDndPolicyAccess = hasDndPolicyAccess,
            autoRotateEnabled = autoRotateEnabled,
            canWriteSystemSettings = canWrite,
            torchAvailable = torchAvailable,
            torchEnabled = torchEnabled,
            brightnessFraction = (brightnessRaw / 255f).coerceIn(0.04f, 1f),
            mediaVolume = currentVol,
            mediaMaxVolume = maxVol,
            mediaSnapshot = mediaSnapshot,
        )
    }

    fun registerTorchObserver(
        context: Context,
        cameraId: String?,
        onTorchChanged: (Boolean) -> Unit,
    ): () -> Unit {
        val cm = context.getSystemService(CameraManager::class.java)
        if (cm == null || cameraId == null) return {}
        val callback = object : CameraManager.TorchCallback() {
            override fun onTorchModeChanged(id: String, enabled: Boolean) {
                if (id == cameraId) {
                    onTorchChanged(enabled)
                }
            }

            override fun onTorchModeUnavailable(id: String) {
                if (id == cameraId) {
                    onTorchChanged(false)
                }
            }
        }
        val handler = Handler(Looper.getMainLooper())
        runCatching { cm.registerTorchCallback(callback, handler) }
        return { runCatching { cm.unregisterTorchCallback(callback) } }
    }

    fun registerSystemStateReceiver(
        context: Context,
        onStateChanged: () -> Unit,
    ): () -> Unit {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                onStateChanged()
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED)
            addAction(WifiManager.WIFI_STATE_CHANGED_ACTION)
            addAction(NotificationManager.ACTION_INTERRUPTION_FILTER_CHANGED)
            addAction("android.media.VOLUME_CHANGED_ACTION")
        }
        runCatching {
            ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        }
        return { runCatching { context.unregisterReceiver(receiver) } }
    }

    fun toggleTorch(context: Context, cameraId: String?, currentlyEnabled: Boolean): Boolean {
        if (cameraId == null) return false
        val cm = context.getSystemService(CameraManager::class.java) ?: return false
        return runCatching {
            cm.setTorchMode(cameraId, !currentlyEnabled)
            true
        }.getOrDefault(false)
    }

    fun setMediaVolume(context: Context, volume: Int) {
        val am = context.getSystemService(AudioManager::class.java) ?: return
        val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        runCatching {
            am.setStreamVolume(AudioManager.STREAM_MUSIC, volume.coerceIn(0, max), 0)
        }
    }

    fun setScreenBrightness(context: Context, fraction: Float): Boolean {
        if (!Settings.System.canWrite(context)) return false
        val value = (fraction.coerceIn(0.02f, 1f) * 255f).toInt().coerceIn(5, 255)
        return runCatching {
            Settings.System.putInt(
                context.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS_MODE,
                Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL,
            )
            Settings.System.putInt(
                context.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS,
                value,
            )
            true
        }.getOrDefault(false)
    }

    fun toggleAutoRotate(context: Context, currentlyEnabled: Boolean): Boolean {
        if (!Settings.System.canWrite(context)) {
            openWriteSettingsPermission(context)
            return false
        }
        return runCatching {
            Settings.System.putInt(
                context.contentResolver,
                Settings.System.ACCELEROMETER_ROTATION,
                if (currentlyEnabled) 0 else 1,
            )
            true
        }.getOrDefault(false)
    }

    fun toggleDoNotDisturb(context: Context, currentlyEnabled: Boolean): Boolean {
        val nm = context.getSystemService(NotificationManager::class.java) ?: return false
        if (!nm.isNotificationPolicyAccessGranted) {
            openDndPolicyAccessSettings(context)
            return false
        }
        val targetFilter = if (currentlyEnabled) {
            NotificationManager.INTERRUPTION_FILTER_ALL
        } else {
            NotificationManager.INTERRUPTION_FILTER_PRIORITY
        }
        return runCatching {
            nm.setInterruptionFilter(targetFilter)
            true
        }.getOrDefault(false)
    }

    fun openWifiPanel(context: Context) {
        launchWithFallback(
            context = context,
            primary = Intent(Settings.Panel.ACTION_WIFI),
            fallback = Intent(Settings.ACTION_WIFI_SETTINGS),
        )
    }

    fun openInternetPanel(context: Context) {
        launchWithFallback(
            context = context,
            primary = Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY),
            fallback = Intent(Settings.ACTION_WIRELESS_SETTINGS),
        )
    }

    fun openBluetoothSettings(context: Context) {
        launchSafe(context, Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
    }

    fun openAirplaneSettings(context: Context) {
        launchSafe(context, Intent(Settings.ACTION_AIRPLANE_MODE_SETTINGS))
    }

    fun openDisplaySettings(context: Context) {
        launchSafe(context, Intent(Settings.ACTION_DISPLAY_SETTINGS))
    }

    fun openSystemSettings(context: Context) {
        launchSafe(context, Intent(Settings.ACTION_SETTINGS))
    }

    fun openWriteSettingsPermission(context: Context) {
        val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
        }
        launchWithFallback(
            context = context,
            primary = intent,
            fallback = Intent(Settings.ACTION_DISPLAY_SETTINGS),
        )
    }

    fun openDndPolicyAccessSettings(context: Context) {
        launchWithFallback(
            context = context,
            primary = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS),
            fallback = Intent(Settings.ACTION_SOUND_SETTINGS),
        )
    }

    fun dispatchMediaKey(context: Context, keyCode: Int) {
        val audioManager = context.getSystemService(AudioManager::class.java) ?: return
        runCatching {
            audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
            audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))
        }
    }

    private fun launchSafe(context: Context, intent: Intent) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
    }

    private fun launchWithFallback(context: Context, primary: Intent, fallback: Intent) {
        primary.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        fallback.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(primary) }
            .onFailure { runCatching { context.startActivity(fallback) } }
    }
}
