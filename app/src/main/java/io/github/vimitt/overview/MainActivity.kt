package io.github.vimitt.overview

import android.content.Intent
import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.vimitt.overview.domain.model.LauncherPreferences
import io.github.vimitt.overview.ui.LauncherRoot
import io.github.vimitt.overview.ui.home.HomeViewModel
import io.github.vimitt.overview.ui.overview.OverviewViewModel
import io.github.vimitt.overview.ui.theme.OverviewTheme

/**
 * Activity principal registrada como launcher (`HOME`) em modo `singleTask`.
 */
class MainActivity : ComponentActivity() {

    private val appContainer by lazy {
        (application as OverviewApplication).container
    }

    private val homeViewModel: HomeViewModel by viewModels {
        HomeViewModel.Factory(
            appsRepository = appContainer.appsRepository,
            preferencesRepository = appContainer.preferencesRepository,
            defaultDashPackageProvider = appContainer.defaultDashPackageProvider,
            resolveDefaultDashAppsUseCase = appContainer.resolveDefaultDashAppsUseCase,
        )
    }

    private val overviewViewModel: OverviewViewModel by viewModels {
        OverviewViewModel.Factory(
            appsRepository = appContainer.appsRepository,
            filterAppsUseCase = appContainer.filterAppsUseCase,
        )
    }

    private var isOverviewOpen by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
        )

        setContent {
            val preferences by appContainer.preferencesRepository.preferences
                .collectAsStateWithLifecycle(initialValue = LauncherPreferences())
            val homeUiState by homeViewModel.uiState.collectAsStateWithLifecycle()
            val overviewUiState by overviewViewModel.uiState.collectAsStateWithLifecycle()

            OverviewTheme(
                themeMode = preferences.themeMode,
                accentColor = Color(preferences.accentColorArgb),
            ) {
                val isDark = OverviewTheme.colors.isDark
                LaunchedEffect(isOverviewOpen, isDark) {
                    val controller = WindowCompat.getInsetsController(window, window.decorView)
                    val useLightBars = isOverviewOpen && !isDark
                    controller.isAppearanceLightStatusBars = useLightBars
                    controller.isAppearanceLightNavigationBars = useLightBars
                }

                LauncherRoot(
                    isOverviewOpen = isOverviewOpen,
                    blurRadiusDp = preferences.overviewBlurRadiusDp,
                    homeUiState = homeUiState,
                    overviewUiState = overviewUiState,
                    onOpenOverview = { isOverviewOpen = true },
                    onCloseOverview = {
                        isOverviewOpen = false
                        overviewViewModel.onClearSearch()
                    },
                    onSearchQueryChange = overviewViewModel::onSearchQueryChange,
                    onClearSearch = overviewViewModel::onClearSearch,
                    onLaunchFromHome = { app, view, bounds, reducedMotion ->
                        homeViewModel.onAppClick(app, view, bounds, reducedMotion)
                    },
                    onLaunchFromOverview = { app, view, bounds, reducedMotion ->
                        val launched = overviewViewModel.onAppClick(app, view, bounds, reducedMotion)
                        if (launched) {
                            isOverviewOpen = false
                            overviewViewModel.onClearSearch()
                        }
                    },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        // Ao pressionar o botão Home, retorna à tela inicial limpando a busca.
        if (intent.hasCategory(Intent.CATEGORY_HOME) || intent.action == Intent.ACTION_MAIN) {
            isOverviewOpen = false
            overviewViewModel.onClearSearch()
        }
    }
}
