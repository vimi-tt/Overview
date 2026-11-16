package io.github.vimitt.overview.data

import android.app.Activity
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.UserManager
import android.util.Log
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import io.github.vimitt.overview.domain.model.HomeGridSpec
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.Collator
import kotlin.math.ceil
import kotlin.math.roundToInt

@Immutable
data class WidgetEntry(
    val providerInfo: AppWidgetProviderInfo,
    val label: String,
    val spanX: Int,
    val spanY: Int,
    val preview: ImageBitmap?,
)

@Immutable
data class WidgetAppGroup(
    val packageName: String,
    val appLabel: String,
    val appIcon: ImageBitmap?,
    val widgets: List<WidgetEntry>,
)

/**
 * Encapsula o ciclo de vida do [AppWidgetHost] e consultas ao [AppWidgetManager].
 */
class AppWidgetController(
    private val context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    private val appWidgetManager: AppWidgetManager? = AppWidgetManager.getInstance(context)
    private val userManager: UserManager? = context.getSystemService(UserManager::class.java)
    private val appWidgetHost = AppWidgetHost(context.applicationContext, APPWIDGET_HOST_ID)

    fun startListening() {
        runCatching { appWidgetHost.startListening() }
            .onFailure { Log.w(TAG, "Falha ao iniciar AppWidgetHost", it) }
    }

    fun stopListening() {
        runCatching { appWidgetHost.stopListening() }
            .onFailure { Log.w(TAG, "Falha ao parar AppWidgetHost", it) }
    }

    fun allocateAppWidgetId(): Int = appWidgetHost.allocateAppWidgetId()

    fun deleteAppWidgetId(appWidgetId: Int) {
        runCatching { appWidgetHost.deleteAppWidgetId(appWidgetId) }
    }

    fun bindAppWidgetIdIfAllowed(appWidgetId: Int, info: AppWidgetProviderInfo): Boolean {
        val manager = appWidgetManager ?: return false
        return runCatching {
            manager.bindAppWidgetIdIfAllowed(appWidgetId, info.profile, info.provider, null)
        }.getOrDefault(false)
    }

    fun createBindIntent(appWidgetId: Int, info: AppWidgetProviderInfo): Intent {
        return Intent(AppWidgetManager.ACTION_APPWIDGET_BIND).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, info.provider)
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER_PROFILE, info.profile)
        }
    }

    fun requiresConfiguration(info: AppWidgetProviderInfo): Boolean {
        if (info.configure == null) return false
        val optional = (info.widgetFeatures and AppWidgetProviderInfo.WIDGET_FEATURE_CONFIGURATION_OPTIONAL) != 0
        return !optional
    }

    fun startConfigureActivity(
        activity: Activity,
        appWidgetId: Int,
        requestCode: Int,
    ) {
        appWidgetHost.startAppWidgetConfigureActivityForResult(
            activity,
            appWidgetId,
            0,
            requestCode,
            null,
        )
    }

    fun findProviderInfo(
        appWidgetId: Int,
        providerPackage: String,
        providerClass: String,
    ): AppWidgetProviderInfo? {
        val manager = appWidgetManager ?: return null
        manager.getAppWidgetInfo(appWidgetId)?.let { return it }

        val target = ComponentName(providerPackage, providerClass)
        val profiles = userManager?.userProfiles ?: listOf(android.os.Process.myUserHandle())
        for (profile in profiles) {
            val match = manager.getInstalledProvidersForProfile(profile)
                .firstOrNull { it.provider == target }
            if (match != null) return match
        }
        return null
    }

    fun createHostView(
        activityContext: Context,
        appWidgetId: Int,
        providerInfo: AppWidgetProviderInfo,
    ): AppWidgetHostView {
        return appWidgetHost.createView(activityContext, appWidgetId, providerInfo).apply {
            setAppWidget(appWidgetId, providerInfo)
        }
    }

    /**
     * Carrega os widgets instalados agrupados por aplicativo, com pré-visualizações
     * decodificadas fora da thread principal.
     */
    suspend fun loadInstalledWidgets(): List<WidgetAppGroup> = withContext(ioDispatcher) {
        val manager = appWidgetManager ?: return@withContext emptyList()
        val pm = context.packageManager
        val density = context.resources.displayMetrics.density
        val densityDpi = context.resources.displayMetrics.densityDpi
        val iconPx = (36 * density).roundToInt().coerceIn(64, 128)
        val previewMaxPx = (160 * density).roundToInt().coerceIn(220, 420)
        val profiles = userManager?.userProfiles ?: listOf(android.os.Process.myUserHandle())

        val allProviders = profiles.flatMap { profile ->
            runCatching { manager.getInstalledProvidersForProfile(profile) }
                .getOrDefault(emptyList())
        }

        val grouped = allProviders.groupBy { it.provider.packageName }
        val collator = Collator.getInstance().apply { strength = Collator.PRIMARY }

        grouped.map { (pkg, providers) ->
            val appInfo = runCatching { pm.getApplicationInfo(pkg, 0) }.getOrNull()
            val appLabel = appInfo?.let { pm.getApplicationLabel(it).toString() } ?: pkg
            val appIcon = appInfo?.let { info ->
                runCatching {
                    pm.getApplicationIcon(info).toBitmap(iconPx, iconPx).asImageBitmap()
                }.getOrNull()
            }

            val widgetEntries = providers.map { info ->
                val (spanX, spanY) = computeDefaultSpan(info)
                val preview = decodePreview(info, densityDpi, previewMaxPx)
                WidgetEntry(
                    providerInfo = info,
                    label = info.loadLabel(pm).ifBlank { info.provider.shortClassName },
                    spanX = spanX,
                    spanY = spanY,
                    preview = preview,
                )
            }.sortedWith { a, b -> collator.compare(a.label, b.label) }

            WidgetAppGroup(
                packageName = pkg,
                appLabel = appLabel,
                appIcon = appIcon,
                widgets = widgetEntries,
            )
        }.sortedWith { a, b -> collator.compare(a.appLabel, b.appLabel) }
    }

    private fun decodePreview(
        info: AppWidgetProviderInfo,
        densityDpi: Int,
        maxSizePx: Int,
    ): ImageBitmap? {
        val drawable = runCatching { info.loadPreviewImage(context, densityDpi) }.getOrNull()
            ?: runCatching { info.loadIcon(context, densityDpi) }.getOrNull()
            ?: return null

        val intrinsicW = drawable.intrinsicWidth.takeIf { it > 0 } ?: maxSizePx
        val intrinsicH = drawable.intrinsicHeight.takeIf { it > 0 } ?: maxSizePx
        val scale = (maxSizePx.toFloat() / maxOf(intrinsicW, intrinsicH)).coerceAtMost(1f)
        val width = (intrinsicW * scale).roundToInt().coerceAtLeast(48)
        val height = (intrinsicH * scale).roundToInt().coerceAtLeast(48)

        return runCatching {
            drawable.toBitmap(width = width, height = height).asImageBitmap()
        }.getOrNull()
    }

    fun computeDefaultSpan(info: AppWidgetProviderInfo): Pair<Int, Int> {
        val rawX = if (info.targetCellWidth > 0) {
            info.targetCellWidth
        } else {
            ceil((info.minWidth + 30) / 70f).toInt()
        }
        val rawY = if (info.targetCellHeight > 0) {
            info.targetCellHeight
        } else {
            ceil((info.minHeight + 30) / 70f).toInt()
        }
        return rawX.coerceIn(1, HomeGridSpec.COLUMNS) to rawY.coerceIn(1, HomeGridSpec.ROWS)
    }

    private companion object {
        const val TAG = "AppWidgetController"
        const val APPWIDGET_HOST_ID = 1024
    }
}
