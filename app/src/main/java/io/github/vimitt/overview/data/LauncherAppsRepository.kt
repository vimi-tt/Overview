package io.github.vimitt.overview.data

import android.app.ActivityOptions
import android.content.Context
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.os.UserHandle
import android.os.UserManager
import android.util.Log
import android.view.View
import androidx.compose.ui.graphics.ImageBitmap
import io.github.vimitt.overview.domain.model.AppInfo
import io.github.vimitt.overview.domain.model.AppearanceSettings
import io.github.vimitt.overview.domain.model.IconShape
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.Collator
import kotlin.math.roundToInt

/**
 * Implementação de [AppsRepository] apoiada em [LauncherApps] e cache de ícones em memória.
 *
 * Escuta alterações de pacotes em tempo real (instalação, remoção, atualização e suspensão),
 * aplica pacote de ícones / forma configurada e processa bitmaps fora da thread principal.
 */
class LauncherAppsRepository(
    private val context: Context,
    private val appScope: CoroutineScope,
    private val iconCache: AppIconCache = AppIconCache(),
    private val iconPackRepository: IconPackRepository = IconPackRepository(context),
    private val appearanceFlow: Flow<AppearanceSettings> = emptyFlow(),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : AppsRepository {

    private val launcherApps: LauncherApps? = context.getSystemService(LauncherApps::class.java)
    private val userManager: UserManager? = context.getSystemService(UserManager::class.java)

    private val _apps = MutableStateFlow<List<AppInfo>>(emptyList())
    override val apps: Flow<List<AppInfo>> = _apps.asStateFlow()

    @Volatile
    private var currentIconPackPackage: String? = null

    @Volatile
    private var currentIconShape: IconShape = IconShape.ROUNDED_SQUARE

    // Canal conflated evita múltiplas cargas simultâneas quando vários pacotes mudam em sequência.
    private val reloadTriggers = Channel<Unit>(Channel.CONFLATED)

    private val iconSizePx: Int =
        (ICON_SIZE_DP * context.resources.displayMetrics.density).roundToInt().coerceIn(96, 192)
    private val iconDensityDpi: Int = context.resources.displayMetrics.densityDpi

    private val packageCallback = object : LauncherApps.Callback() {
        override fun onPackageRemoved(packageName: String, user: UserHandle) {
            iconCache.evictPackage(packageName)
            refresh()
        }

        override fun onPackageAdded(packageName: String, user: UserHandle) {
            iconCache.evictPackage(packageName)
            refresh()
        }

        override fun onPackageChanged(packageName: String, user: UserHandle) {
            iconCache.evictPackage(packageName)
            refresh()
        }

        override fun onPackagesAvailable(
            packageNames: Array<out String>,
            user: UserHandle,
            replacing: Boolean,
        ) {
            packageNames.forEach(iconCache::evictPackage)
            refresh()
        }

        override fun onPackagesUnavailable(
            packageNames: Array<out String>,
            user: UserHandle,
            replacing: Boolean,
        ) {
            packageNames.forEach(iconCache::evictPackage)
            refresh()
        }

        override fun onPackagesSuspended(packageNames: Array<out String>, user: UserHandle) {
            packageNames.forEach(iconCache::evictPackage)
            refresh()
        }

        override fun onPackagesUnsuspended(packageNames: Array<out String>, user: UserHandle) {
            packageNames.forEach(iconCache::evictPackage)
            refresh()
        }
    }

    init {
        launcherApps?.registerCallback(packageCallback, Handler(Looper.getMainLooper()))
        appScope.launch {
            appearanceFlow
                .map { it.iconPackPackage to it.iconShape }
                .distinctUntilChanged()
                .collectLatest { (packPkg, shape) ->
                    val changed = currentIconPackPackage != packPkg || currentIconShape != shape
                    currentIconPackPackage = packPkg
                    currentIconShape = shape
                    if (changed) {
                        iconCache.clear()
                        refresh()
                    }
                }
        }
        appScope.launch {
            // Carga inicial imediata antes de aguardar novos gatilhos.
            _apps.value = loadAllApps()
            for (trigger in reloadTriggers) {
                _apps.value = loadAllApps()
            }
        }
    }

    override fun refresh() {
        reloadTriggers.trySend(Unit)
    }

    override fun launchApp(
        app: AppInfo,
        sourceView: View?,
        sourceBounds: Rect?,
        reducedMotion: Boolean,
    ): Boolean {
        val service = launcherApps ?: return false
        return try {
            val options = buildLaunchOptions(sourceView, sourceBounds, reducedMotion)
            service.startMainActivity(
                app.toComponentName(),
                app.userHandle ?: android.os.Process.myUserHandle(),
                sourceBounds,
                options?.toBundle(),
            )
            true
        } catch (exception: SecurityException) {
            Log.w(TAG, "Permissão negada ao abrir ${app.packageName}", exception)
            false
        } catch (exception: IllegalArgumentException) {
            Log.w(TAG, "Atividade inválida ao abrir ${app.packageName}", exception)
            false
        }
    }

    private fun buildLaunchOptions(
        sourceView: View?,
        sourceBounds: Rect?,
        reducedMotion: Boolean,
    ): ActivityOptions? {
        if (reducedMotion) {
            return ActivityOptions.makeCustomAnimation(context, 0, 0)
        }
        if (sourceView != null && sourceBounds != null && !sourceBounds.isEmpty) {
            return ActivityOptions.makeScaleUpAnimation(
                sourceView,
                sourceBounds.left,
                sourceBounds.top,
                sourceBounds.width(),
                sourceBounds.height(),
            )
        }
        return ActivityOptions.makeBasic()
    }

    private suspend fun loadAllApps(): List<AppInfo> = withContext(ioDispatcher) {
        val service = launcherApps ?: return@withContext emptyList()
        val profiles = userManager?.userProfiles ?: listOf(android.os.Process.myUserHandle())
        val collator = Collator.getInstance().apply { strength = Collator.PRIMARY }
        val shape = currentIconShape
        val iconPack = iconPackRepository.loadIconPack(currentIconPackPackage)
        val packTag = iconPack?.packageName.orEmpty()

        val loaded = mutableListOf<AppInfo>()
        for (profile in profiles) {
            val activities = try {
                service.getActivityList(null, profile)
            } catch (exception: SecurityException) {
                Log.w(TAG, "Não foi possível consultar atividades do perfil $profile", exception)
                emptyList()
            }

            for (activityInfo in activities) {
                val component = activityInfo.componentName

                val cacheKey =
                    "${component.packageName}/${component.className}/${profile.hashCode()}/${packTag}_${shape.name}"
                val icon = iconCache.get(cacheKey)
                    ?: decodeIcon(activityInfo, iconPack, shape)?.also { decoded ->
                        iconCache.put(cacheKey, decoded)
                    }

                loaded += AppInfo(
                    packageName = component.packageName,
                    activityName = component.className,
                    label = activityInfo.label?.toString()?.trim().orEmpty()
                        .ifEmpty { component.packageName },
                    icon = icon,
                    userHandle = profile,
                    firstInstallTimeMillis = activityInfo.firstInstallTime,
                )
            }
        }

        loaded.sortWith { first, second ->
            collator.compare(first.label, second.label)
        }
        loaded
    }

    private fun decodeIcon(
        activityInfo: LauncherActivityInfo,
        iconPack: LoadedIconPack?,
        shape: IconShape,
    ): ImageBitmap? {
        return try {
            val component = activityInfo.componentName
            val packDrawable = iconPack?.loadDrawable(component.packageName, component.className)
            val drawable = packDrawable ?: activityInfo.getBadgedIcon(iconDensityDpi)
            iconPackRepository.renderIconBitmap(
                drawable = drawable,
                shape = shape,
                sizePx = iconSizePx,
                fromIconPack = packDrawable != null,
            )
        } catch (exception: Exception) {
            Log.w(TAG, "Falha ao renderizar ícone de ${activityInfo.componentName}", exception)
            null
        }
    }

    private companion object {
        const val TAG = "LauncherAppsRepo"
        const val ICON_SIZE_DP = 56
    }
}
