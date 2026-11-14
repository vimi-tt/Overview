package io.github.vimitt.overview.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.MediaStore
import android.provider.Settings

/**
 * Consulta ao [PackageManager] os pacotes padrão do dispositivo para as categorias
 * iniciais do Dash (telefone, mensagens, navegador, câmera e configurações).
 */
class DefaultDashPackageProvider(
    private val context: Context,
) {
    fun resolvePreferredPackagesBySlot(): List<Set<String>> {
        val pm = context.packageManager
        return listOf(
            queryPackages(pm, Intent(Intent.ACTION_DIAL)),
            queryPackages(
                pm,
                Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_MESSAGING),
            ),
            queryPackages(
                pm,
                Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_BROWSER),
            ),
            queryPackages(pm, Intent(MediaStore.ACTION_IMAGE_CAPTURE)),
            queryPackages(pm, Intent(Settings.ACTION_SETTINGS)),
        )
    }

    private fun queryPackages(pm: PackageManager, intent: Intent): Set<String> {
        return runCatching {
            val resolved = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val flags = PackageManager.ResolveInfoFlags.of(
                    PackageManager.MATCH_DEFAULT_ONLY.toLong(),
                )
                pm.queryIntentActivities(intent, flags)
            } else {
                @Suppress("DEPRECATION")
                pm.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
            }
            resolved.mapNotNull { it.activityInfo?.packageName }.toSet()
        }.getOrDefault(emptySet())
    }
}
