package io.github.vimitt.overview

import android.app.Application
import io.github.vimitt.overview.di.AppContainer

/**
 * Classe [Application] que mantém o contêiner de dependências durante o ciclo de vida do processo.
 */
class OverviewApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
