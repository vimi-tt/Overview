package io.github.vimitt.overview.di

import android.content.Context
import io.github.vimitt.overview.data.AppIconCache
import io.github.vimitt.overview.data.AppWidgetController
import io.github.vimitt.overview.data.AppsRepository
import io.github.vimitt.overview.data.DefaultDashPackageProvider
import io.github.vimitt.overview.data.HomeWorkspaceRepository
import io.github.vimitt.overview.data.LauncherAppsRepository
import io.github.vimitt.overview.data.PreferencesRepository
import io.github.vimitt.overview.domain.usecase.FilterAppsUseCase
import io.github.vimitt.overview.domain.usecase.HomeGridPlacementUseCase
import io.github.vimitt.overview.domain.usecase.ResolveDefaultDashAppsUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Contêiner manual de dependências do aplicativo.
 *
 * Mantém a inicialização explícita e leve, evitando sobrecarga de geração
 * de código durante a iteração inicial.
 */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val iconCache = AppIconCache()

    val appsRepository: AppsRepository = LauncherAppsRepository(
        context = appContext,
        appScope = appScope,
        iconCache = iconCache,
    )

    val preferencesRepository: PreferencesRepository = PreferencesRepository(appContext)
    val defaultDashPackageProvider: DefaultDashPackageProvider = DefaultDashPackageProvider(appContext)

    val filterAppsUseCase: FilterAppsUseCase = FilterAppsUseCase()
    val resolveDefaultDashAppsUseCase: ResolveDefaultDashAppsUseCase = ResolveDefaultDashAppsUseCase()
    val homeGridPlacementUseCase: HomeGridPlacementUseCase = HomeGridPlacementUseCase()

    val homeWorkspaceRepository: HomeWorkspaceRepository = HomeWorkspaceRepository(
        context = appContext,
        placementUseCase = homeGridPlacementUseCase,
    )

    val appWidgetController: AppWidgetController = AppWidgetController(appContext)
}
