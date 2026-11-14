package io.github.vimitt.overview.data

import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap

/**
 * Cache em memória para ícones de aplicativos já convertidos em [ImageBitmap].
 *
 * Evita decodificação repetida de Drawables na thread principal durante
 * recomposições e rolagem da grade do Overview.
 */
class AppIconCache(maxEntries: Int = DEFAULT_MAX_ENTRIES) {

    private val cache = LruCache<String, ImageBitmap>(maxEntries)

    fun get(key: String): ImageBitmap? = cache.get(key)

    fun put(key: String, bitmap: ImageBitmap) {
        cache.put(key, bitmap)
    }

    /**
     * Remove todos os ícones em cache pertencentes ao pacote informado.
     */
    fun evictPackage(packageName: String) {
        val prefix = "$packageName/"
        val snapshotKeys = cache.snapshot().keys
        for (key in snapshotKeys) {
            if (key.startsWith(prefix)) {
                cache.remove(key)
            }
        }
    }

    fun clear() {
        cache.evictAll()
    }

    private companion object {
        const val DEFAULT_MAX_ENTRIES = 256
    }
}
