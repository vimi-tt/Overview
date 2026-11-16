package io.github.vimitt.overview.data

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.content.pm.ShortcutInfo
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.os.Process
import android.os.UserHandle
import android.util.DisplayMetrics
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import io.github.vimitt.overview.domain.model.AppInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Representa um atalho dinâmico, estático ou fixado retornado pelo sistema para um aplicativo.
 */
data class AppShortcutItem(
    val id: String,
    val packageName: String,
    val label: String,
    val icon: ImageBitmap?,
    val shortcutInfo: ShortcutInfo,
)

/**
 * Utilitário para consultar/executar App Shortcuts (`LauncherApps`) e desinstalar aplicativos.
 */
object LauncherShortcutHelper {
    private const val MAX_MENU_SHORTCUTS = 5

    suspend fun queryAppShortcuts(
        context: Context,
        app: AppInfo,
    ): List<AppShortcutItem> = withContext(Dispatchers.IO) {
        val launcherApps = context.getSystemService(LauncherApps::class.java) ?: return@withContext emptyList()
        if (!launcherApps.hasShortcutHostPermission()) return@withContext emptyList()

        val user = app.userHandle ?: Process.myUserHandle()
        val query = LauncherApps.ShortcutQuery()
            .setPackage(app.packageName)
            .setQueryFlags(
                LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC or
                    LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST or
                    LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED,
            )

        val rawShortcuts = runCatching {
            launcherApps.getShortcuts(query, user).orEmpty()
        }.getOrDefault(emptyList())

        rawShortcuts
            .asSequence()
            .filter { it.isEnabled }
            .sortedWith(
                compareBy<ShortcutInfo> { !it.isDeclaredInManifest }
                    .thenBy { it.rank },
            )
            .take(MAX_MENU_SHORTCUTS)
            .map { info ->
                val label = (info.shortLabel ?: info.longLabel ?: info.id).toString().trim()
                val icon = loadShortcutIconSync(launcherApps, info)
                AppShortcutItem(
                    id = info.id,
                    packageName = info.`package`,
                    label = label.ifBlank { info.id },
                    icon = icon,
                    shortcutInfo = info,
                )
            }
            .toList()
    }

    suspend fun loadPinnedShortcutIcon(
        context: Context,
        packageName: String,
        shortcutId: String,
        userHandle: UserHandle = Process.myUserHandle(),
    ): ImageBitmap? = withContext(Dispatchers.IO) {
        val launcherApps = context.getSystemService(LauncherApps::class.java) ?: return@withContext null
        if (!launcherApps.hasShortcutHostPermission()) return@withContext null

        val query = LauncherApps.ShortcutQuery()
            .setPackage(packageName)
            .setShortcutIds(listOf(shortcutId))
            .setQueryFlags(
                LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED or
                    LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC or
                    LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST,
            )

        val info = runCatching {
            launcherApps.getShortcuts(query, userHandle)?.firstOrNull()
        }.getOrNull() ?: return@withContext null

        loadShortcutIconSync(launcherApps, info)
    }

    suspend fun loadShortcutIcon(
        context: Context,
        shortcutInfo: ShortcutInfo,
    ): ImageBitmap? = withContext(Dispatchers.IO) {
        val launcherApps = context.getSystemService(LauncherApps::class.java) ?: return@withContext null
        loadShortcutIconSync(launcherApps, shortcutInfo)
    }

    fun launchShortcut(
        context: Context,
        shortcutInfo: ShortcutInfo,
        sourceBounds: Rect? = null,
    ): Boolean {
        val launcherApps = context.getSystemService(LauncherApps::class.java) ?: return false
        return runCatching {
            launcherApps.startShortcut(shortcutInfo, sourceBounds, null)
            true
        }.getOrDefault(false)
    }

    fun launchPinnedShortcut(
        context: Context,
        packageName: String,
        shortcutId: String,
        userHandle: UserHandle = Process.myUserHandle(),
        sourceBounds: Rect? = null,
    ): Boolean {
        val launcherApps = context.getSystemService(LauncherApps::class.java) ?: return false
        return runCatching {
            launcherApps.startShortcut(packageName, shortcutId, sourceBounds, null, userHandle)
            true
        }.getOrDefault(false)
    }

    fun canUninstallApp(
        context: Context,
        packageName: String,
    ): Boolean {
        if (packageName == context.packageName) return false
        return runCatching {
            val appInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getApplicationInfo(
                    packageName,
                    PackageManager.ApplicationInfoFlags.of(0L),
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getApplicationInfo(packageName, 0)
            }
            (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) == 0
        }.getOrDefault(false)
    }

    fun uninstallApp(
        context: Context,
        packageName: String,
        userHandle: UserHandle? = null,
    ) {
        if (!canUninstallApp(context, packageName)) return
        val intent = Intent(Intent.ACTION_DELETE, Uri.parse("package:$packageName")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (userHandle != null) {
                putExtra(Intent.EXTRA_USER, userHandle)
            }
        }
        runCatching {
            context.startActivity(intent)
        }
    }

    private fun loadShortcutIconSync(
        launcherApps: LauncherApps,
        shortcutInfo: ShortcutInfo,
    ): ImageBitmap? = runCatching {
        val drawable = launcherApps.getShortcutBadgedIconDrawable(
            shortcutInfo,
            DisplayMetrics.DENSITY_XHIGH,
        ) ?: return@runCatching null
        drawable.toBitmap(width = 128, height = 128).asImageBitmap()
    }.getOrNull()
}
