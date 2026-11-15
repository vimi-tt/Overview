package io.github.vimitt.overview.data

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Path
import android.graphics.RectF
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.util.Log
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.content.res.ResourcesCompat
import io.github.vimitt.overview.domain.model.IconShape
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import java.text.Collator

/**
 * Metadados de um pacote de ícones instalado no dispositivo.
 */
@Immutable
data class IconPackInfo(
    val packageName: String,
    val label: String,
    val icon: ImageBitmap?,
)

/**
 * Tabela carregada de mapeamentos `ComponentInfo{pkg/cls}` -> recurso drawable de um icon pack.
 */
class LoadedIconPack(
    val packageName: String,
    private val resources: Resources,
    private val componentDrawables: Map<String, String>,
) {
    fun loadDrawable(packageName: String, activityName: String): Drawable? {
        val key = "$packageName/$activityName"
        val drawableName = componentDrawables[key]
            ?: componentDrawables["$packageName/"]
            ?: return null

        val resId = resolveDrawableId(drawableName)
        if (resId == 0) return null
        return runCatching {
            ResourcesCompat.getDrawable(resources, resId, null)
        }.getOrNull()
    }

    @SuppressLint("DiscouragedApi")
    private fun resolveDrawableId(drawableName: String): Int {
        return resources.getIdentifier(drawableName, "drawable", packageName)
    }
}

/**
 * Descobre pacotes de ícones compatíveis e renderiza ícones com a forma configurada fora da main thread.
 */
class IconPackRepository(
    private val context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    private val packageManager: PackageManager = context.packageManager

    suspend fun loadInstalledIconPacks(): List<IconPackInfo> = withContext(ioDispatcher) {
        val selfPackage = context.packageName
        val collator = Collator.getInstance().apply { strength = Collator.PRIMARY }
        val discovered = linkedMapOf<String, IconPackInfo>()

        for (action in ICON_PACK_ACTIONS) {
            val intent = Intent(action)
            val resolved = packageManager.queryIntentActivities(intent, PackageManager.MATCH_ALL)
            for (info in resolved) {
                val pkg = info.activityInfo?.packageName ?: continue
                if (pkg == selfPackage || pkg in discovered) continue

                val label = info.loadLabel(packageManager).toString().trim().ifEmpty { pkg }
                val iconBitmap = runCatching {
                    val raw = info.loadIcon(packageManager)
                    renderIconBitmap(raw, IconShape.ROUNDED_SQUARE, 96, fromIconPack = false)
                }.getOrNull()

                discovered[pkg] = IconPackInfo(
                    packageName = pkg,
                    label = label,
                    icon = iconBitmap,
                )
            }
        }

        discovered.values.sortedWith { a, b -> collator.compare(a.label, b.label) }
    }

    suspend fun loadIconPack(packageName: String?): LoadedIconPack? = withContext(ioDispatcher) {
        if (packageName.isNullOrBlank()) return@withContext null

        try {
            val res = packageManager.getResourcesForApplication(packageName)
            val mappings = parseAppFilter(res, packageName)
            LoadedIconPack(
                packageName = packageName,
                resources = res,
                componentDrawables = mappings,
            )
        } catch (exception: Exception) {
            Log.w(TAG, "Falha ao carregar pacote de ícones $packageName", exception)
            null
        }
    }

    fun renderIconBitmap(
        drawable: Drawable,
        shape: IconShape,
        sizePx: Int,
        fromIconPack: Boolean,
    ): ImageBitmap {
        val safeSize = sizePx.coerceAtLeast(48)
        val bitmap = Bitmap.createBitmap(safeSize, safeSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val fSize = safeSize.toFloat()

        if (drawable is AdaptiveIconDrawable) {
            val clipPath = buildShapePath(shape, fSize)
            val saveCount = canvas.save()
            canvas.clipPath(clipPath)

            // Ícones adaptativos usam 108dp com área útil central de 72dp (margem externa de 25%).
            val inset = (fSize * ADAPTIVE_INSET_RATIO).toInt()
            val left = -inset
            val top = -inset
            val right = safeSize + inset
            val bottom = safeSize + inset

            drawable.background?.let { bg ->
                bg.setBounds(left, top, right, bottom)
                bg.draw(canvas)
            }
            drawable.foreground?.let { fg ->
                fg.setBounds(left, top, right, bottom)
                fg.draw(canvas)
            }
            canvas.restoreToCount(saveCount)
        } else {
            val saveCount = canvas.save()
            if (!fromIconPack) {
                canvas.clipPath(buildShapePath(shape, fSize))
            }
            drawable.setBounds(0, 0, safeSize, safeSize)
            drawable.draw(canvas)
            canvas.restoreToCount(saveCount)
        }

        return bitmap.asImageBitmap()
    }

    private fun buildShapePath(shape: IconShape, sizePx: Float): Path {
        val rect = RectF(0f, 0f, sizePx, sizePx)
        return Path().apply {
            when (shape) {
                IconShape.CIRCLE -> addOval(rect, Path.Direction.CW)
                IconShape.ROUNDED_SQUARE -> {
                    val radius = sizePx * 0.24f
                    addRoundRect(rect, radius, radius, Path.Direction.CW)
                }

                IconShape.SQUARE -> {
                    val radius = sizePx * 0.06f
                    addRoundRect(rect, radius, radius, Path.Direction.CW)
                }
            }
        }
    }

    @SuppressLint("DiscouragedApi")
    private fun parseAppFilter(resources: Resources, packageName: String): Map<String, String> {
        val mappings = HashMap<String, String>(256)
        val xmlResId = resources.getIdentifier("appfilter", "xml", packageName)
        if (xmlResId != 0) {
            resources.getXml(xmlResId).use { parser ->
                readAppFilterFromParser(parser, mappings)
            }
            return mappings
        }

        // Alguns icon packs disponibilizam appfilter.xml dentro de assets/.
        runCatching {
            resources.assets.open("appfilter.xml").use { input ->
                val factory = org.xmlpull.v1.XmlPullParserFactory.newInstance()
                val parser = factory.newPullParser()
                parser.setInput(input, "UTF-8")
                readAppFilterFromParser(parser, mappings)
            }
        }
        return mappings
    }

    private fun readAppFilterFromParser(
        parser: XmlPullParser,
        target: MutableMap<String, String>,
    ) {
        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG && parser.name == "item") {
                val componentRaw = parser.getAttributeValue(null, "component")
                val drawableName = parser.getAttributeValue(null, "drawable")
                if (!componentRaw.isNullOrBlank() && !drawableName.isNullOrBlank()) {
                    val parsed = extractComponentKey(componentRaw)
                    if (parsed != null) {
                        target[parsed] = drawableName
                    }
                }
            }
            eventType = parser.next()
        }
    }

    private fun extractComponentKey(raw: String): String? {
        val start = raw.indexOf('{')
        val end = raw.indexOf('}')
        if (start == -1 || end <= start + 1) return null
        return raw.substring(start + 1, end)
    }

    private companion object {
        const val TAG = "IconPackRepo"
        const val ADAPTIVE_INSET_RATIO = 0.25f
        val ICON_PACK_ACTIONS = listOf(
            "org.adw.launcher.THEMES",
            "com.novalauncher.THEME",
            "com.anddoes.launcher.THEME",
            "com.gau.go.launcherex.theme",
        )
    }
}
