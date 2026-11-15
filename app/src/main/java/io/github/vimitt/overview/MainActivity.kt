package io.github.vimitt.overview

import android.app.WallpaperColors
import android.app.WallpaperManager
import android.content.Intent
import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.vimitt.overview.domain.model.LauncherPreferences
import io.github.vimitt.overview.ui.LauncherRoot
import io.github.vimitt.overview.ui.home.HomeViewModel
import io.github.vimitt.overview.ui.overview.OverviewViewModel
import io.github.vimitt.overview.ui.settings.SettingsViewModel
import io.github.vimitt.overview.ui.theme.OverviewTheme

/**
 * Activity principal registrada como launcher (`HOME`) em modo `singleTask`.
 * Também responde a `ACTION_APPLICATION_PREFERENCES` para abrir a tela de
 * personalização a partir das configurações do sistema Android.
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

    private val settingsViewModel: SettingsViewModel by viewModels {
        SettingsViewModel.Factory(
            appsRepository = appContainer.appsRepository,
            preferencesRepository = appContainer.preferencesRepository,
        )
    }

    private var isOverviewOpen by mutableStateOf(false)
    private var isHomeMenuOpen by mutableStateOf(false)
    private var isSettingsOpen by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
        )
        handleIncomingIntent(intent)

        setContent {
            val preferences by appContainer.preferencesRepository.preferences
                .collectAsStateWithLifecycle(initialValue = LauncherPreferences())
            val homeUiState by homeViewModel.uiState.collectAsStateWithLifecycle()
            val searchQuery by overviewViewModel.searchQuery.collectAsStateWithLifecycle()
            val overviewUiState by overviewViewModel.uiState.collectAsStateWithLifecycle()
            val settingsUiState by settingsViewModel.uiState.collectAsStateWithLifecycle()

            var wallpaperSupportsDarkText by remember {
                mutableStateOf(readWallpaperSupportsDarkText())
            }

            DisposableEffect(Unit) {
                val wm = getSystemService(WallpaperManager::class.java)
                val listener = WallpaperManager.OnColorsChangedListener { colors, which ->
                    if ((which and WallpaperManager.FLAG_SYSTEM) != 0) {
                        val hints = colors?.colorHints ?: 0
                        wallpaperSupportsDarkText =
                            (hints and WallpaperColors.HINT_SUPPORTS_DARK_TEXT) != 0
                    }
                }
                wm?.addOnColorsChangedListener(listener, android.os.Handler(android.os.Looper.getMainLooper()))
                onDispose {
                    wm?.removeOnColorsChangedListener(listener)
                }
            }

            OverviewTheme(
                themeMode = preferences.themeMode,
                accentColor = Color(preferences.accentColorArgb),
            ) {
                val isDark = OverviewTheme.colors.isDark
                LaunchedEffect(isOverviewOpen, isSettingsOpen, isDark, wallpaperSupportsDarkText) {
                    val controller = WindowCompat.getInsetsController(window, window.decorView)
                    val useLightBars = when {
                        isSettingsOpen || isOverviewOpen -> !isDark
                        else -> wallpaperSupportsDarkText
                    }
                    controller.isAppearanceLightStatusBars = useLightBars
                    controller.isAppearanceLightNavigationBars = useLightBars
                }

                LauncherRoot(
                    isOverviewOpen = isOverviewOpen,
                    isHomeMenuOpen = isHomeMenuOpen,
                    isSettingsOpen = isSettingsOpen,
                    blurRadiusDp = preferences.overviewBlurRadiusDp,
                    homeUiState = homeUiState,
                    searchQuery = searchQuery,
                    overviewUiState = overviewUiState,
                    settingsUiState = settingsUiState,
                    onOpenOverview = {
                        isHomeMenuOpen = false
                        isOverviewOpen = true
                    },
                    onCloseOverview = {
                        isOverviewOpen = false
                        overviewViewModel.onClearSearch()
                    },
                    onHomeLongPress = { isHomeMenuOpen = true },
                    onDismissHomeMenu = { isHomeMenuOpen = false },
                    onOpenSettings = {
                        isHomeMenuOpen = false
                        isSettingsOpen = true
                    },
                    onCloseSettings = { isSettingsOpen = false },
                    onOpenWallpaperPicker = ::openWallpaperPicker,
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
                    onThemeModeChange = settingsViewModel::onThemeModeChange,
                    onAccentColorChange = settingsViewModel::onAccentColorChange,
                    onBlurRadiusChange = settingsViewModel::onBlurRadiusChange,
                    onShowClockChange = settingsViewModel::onShowClockChange,
                    onToggleDashApp = settingsViewModel::onToggleDashApp,
                    onResetDashToAuto = settingsViewModel::onResetDashToAuto,
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent == null) return
        when {
            intent.action == Intent.ACTION_APPLICATION_PREFERENCES -> {
                isHomeMenuOpen = false
                isOverviewOpen = false
                isSettingsOpen = true
            }

            intent.hasCategory(Intent.CATEGORY_HOME) || intent.action == Intent.ACTION_MAIN -> {
                isHomeMenuOpen = false
                isSettingsOpen = false
                isOverviewOpen = false
                overviewViewModel.onClearSearch()
            }
        }
    }

    private fun readWallpaperSupportsDarkText(): Boolean {
        val wm = getSystemService(WallpaperManager::class.java) ?: return false
        val hints = wm.getWallpaperColors(WallpaperManager.FLAG_SYSTEM)?.colorHints ?: 0
        return (hints and WallpaperColors.HINT_SUPPORTS_DARK_TEXT) != 0
    }

    private fun openWallpaperPicker() {
        val picker = Intent.createChooser(
            Intent(Intent.ACTION_SET_WALLPAPER),
            getString(R.string.wallpaper_chooser_title),
        )
        runCatching { startActivity(picker) }
    }
}
