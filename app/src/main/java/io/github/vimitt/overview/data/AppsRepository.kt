package io.github.vimitt.overview.data

import android.graphics.Rect
import android.view.View
import io.github.vimitt.overview.domain.model.AppInfo
import kotlinx.coroutines.flow.Flow

/**
 * Contrato do repositório responsável por listar e abrir aplicativos instalados.
 */
interface AppsRepository {
    /** Fluxo reativo com os aplicativos ordenados alfabeticamente. */
    val apps: Flow<List<AppInfo>>

    /**
     * Solicita a abertura do aplicativo via [android.content.pm.LauncherApps].
     *
     * @param app Aplicativo alvo.
     * @param sourceView View raiz usada como âncora da animação de abertura.
     * @param sourceBounds Limites na tela do ícone tocado, quando disponíveis.
     * @param reducedMotion Indica se animações reduzidas estão ativas no sistema.
     * @return `true` se a inicialização foi disparada sem falhas.
     */
    fun launchApp(
        app: AppInfo,
        sourceView: View?,
        sourceBounds: Rect?,
        reducedMotion: Boolean,
    ): Boolean

    /** Força uma recarga manual da lista de aplicativos. */
    fun refresh()
}
