package io.github.vimitt.overview.ui.settings

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import io.github.vimitt.overview.R
import io.github.vimitt.overview.data.LauncherBackupHelper
import io.github.vimitt.overview.di.AppContainer
import io.github.vimitt.overview.domain.model.NativeWidgetsState
import io.github.vimitt.overview.domain.model.WeatherLocation
import io.github.vimitt.overview.domain.model.WeatherProvider
import io.github.vimitt.overview.ui.settings.about.AboutScreen
import io.github.vimitt.overview.ui.settings.appgrid.AppGridSettingsScreen
import io.github.vimitt.overview.ui.settings.appgrid.AppGridSettingsViewModel
import io.github.vimitt.overview.ui.settings.appgrid.HiddenAppsScreen
import io.github.vimitt.overview.ui.settings.appearance.AppearanceSettingsScreen
import io.github.vimitt.overview.ui.settings.appearance.AppearanceViewModel
import io.github.vimitt.overview.ui.settings.components.LocalSettingsDualPane
import io.github.vimitt.overview.ui.settings.dock.DockSettingsScreen
import io.github.vimitt.overview.ui.settings.dock.DockSettingsViewModel
import io.github.vimitt.overview.ui.settings.folders.FoldersSettingsScreen
import io.github.vimitt.overview.ui.settings.folders.FoldersSettingsViewModel
import io.github.vimitt.overview.ui.settings.gestures.GesturesSettingsScreen
import io.github.vimitt.overview.ui.settings.gestures.GesturesSettingsViewModel
import io.github.vimitt.overview.ui.settings.home.HomeSettingsScreen
import io.github.vimitt.overview.ui.settings.home.HomeSettingsViewModel
import io.github.vimitt.overview.ui.settings.overview.OverviewSettingsScreen
import io.github.vimitt.overview.ui.settings.overview.OverviewSettingsViewModel
import io.github.vimitt.overview.ui.settings.search.SearchSettingsScreen
import io.github.vimitt.overview.ui.settings.search.SearchSettingsViewModel
import io.github.vimitt.overview.ui.theme.OverviewTheme
import kotlinx.coroutines.launch

private object SettingsRoute {
    const val HUB = "settings_hub"
    const val APPEARANCE = "settings_appearance"
    const val HOME = "settings_home"
    const val DOCK = "settings_dock"
    const val APP_GRID = "settings_app_grid"
    const val HIDDEN_APPS = "settings_hidden_apps"
    const val OVERVIEW = "settings_overview"
    const val SEARCH = "settings_search"
    const val FOLDERS = "settings_folders"
    const val GESTURES = "settings_gestures"
    const val ABOUT = "settings_about"
}

/**
 * Grafo de navegação das configurações do Overview com transições adaptadas
 * ao ajuste de animações reduzidas do sistema.
 */
@Composable
fun SettingsNavHost(
    appContainer: AppContainer,
    onOpenWallpaperPicker: () -> Unit,
    onActivateWeatherLocation: () -> Unit = {},
    onSearchWeatherCities: suspend (String) -> Result<List<WeatherLocation>> = { Result.success(emptyList()) },
    onSelectWeatherLocation: (WeatherLocation) -> Unit = {},
    onSelectWeatherProvider: (WeatherProvider) -> Unit = {},
    onPreviewOnboarding: () -> Unit = {},
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = OverviewTheme.colors
    val motion = OverviewTheme.motion
    val reducedMotion = OverviewTheme.reducedMotion

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val useDualPane = maxWidth >= 840.dp
        if (useDualPane) {
            var selectedCategory by rememberSaveable { mutableStateOf(SettingsCategory.APPEARANCE) }
            var isHiddenAppsOpen by rememberSaveable { mutableStateOf(false) }

            BackHandler {
                if (isHiddenAppsOpen) {
                    isHiddenAppsOpen = false
                } else {
                    onClose()
                }
            }

            val selectCategory: (SettingsCategory) -> Unit = { category ->
                selectedCategory = category
                isHiddenAppsOpen = false
            }

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .background(colors.windowBackground),
            ) {
                Box(
                    modifier = Modifier
                        .widthIn(min = 320.dp, max = 380.dp)
                        .weight(0.36f)
                        .fillMaxHeight(),
                ) {
                    SettingsHubDestination(
                        appContainer = appContainer,
                        selectedCategory = selectedCategory,
                        onOpenCategory = selectCategory,
                        onPreviewOnboarding = onPreviewOnboarding,
                        onClose = onClose,
                    )
                }
                VerticalDivider(thickness = 1.dp, color = colors.outlineVariant)
                Box(
                    modifier = Modifier
                        .weight(0.64f)
                        .fillMaxHeight(),
                ) {
                    val detailRoute = if (isHiddenAppsOpen) {
                        SettingsRoute.HIDDEN_APPS
                    } else {
                        selectedCategory.toRoute()
                    }
                    CompositionLocalProvider(LocalSettingsDualPane provides !isHiddenAppsOpen) {
                        AnimatedContent(
                            targetState = detailRoute,
                            transitionSpec = {
                                fadeIn(motion.effectSpec(durationMillis = 150)) togetherWith
                                    fadeOut(motion.effectSpec(durationMillis = 120))
                            },
                            label = "settingsDualPaneDetail",
                        ) { route ->
                            SettingsDetailDestination(
                                route = route,
                                appContainer = appContainer,
                                onOpenWallpaperPicker = onOpenWallpaperPicker,
                                onActivateWeatherLocation = onActivateWeatherLocation,
                                onSearchWeatherCities = onSearchWeatherCities,
                                onSelectWeatherLocation = onSelectWeatherLocation,
                                onSelectWeatherProvider = onSelectWeatherProvider,
                                onOpenHiddenApps = { isHiddenAppsOpen = true },
                                onBack = {
                                    if (isHiddenAppsOpen) {
                                        isHiddenAppsOpen = false
                                    } else {
                                        onClose()
                                    }
                                },
                            )
                        }
                    }
                }
            }
            return@BoxWithConstraints
        }

        val navController = rememberNavController()
        val enterAnim = if (reducedMotion) {
            fadeIn(motion.effectSpec())
        } else if (motion.usesSprings) {
            fadeIn(motion.effectSpec(durationMillis = 190)) +
                scaleIn(
                    initialScale = 0.96f,
                    transformOrigin = TransformOrigin(0.8f, 0.5f),
                    animationSpec = motion.spatialSpec(durationMillis = 240),
                ) +
                slideInHorizontally(
                    motion.spatialSpec(
                        durationMillis = 240,
                        visibilityThreshold = IntOffset.VisibilityThreshold,
                    ),
                ) { it / 5 }
        } else {
            fadeIn(motion.effectSpec(durationMillis = 180)) +
                slideInHorizontally(
                    motion.spatialSpec(
                        durationMillis = 200,
                        visibilityThreshold = IntOffset.VisibilityThreshold,
                    ),
                ) { it / 8 }
        }

        val exitAnim = if (reducedMotion) {
            fadeOut(motion.effectSpec())
        } else if (motion.usesSprings) {
            fadeOut(motion.effectSpec(durationMillis = 140)) +
                scaleOut(
                    targetScale = 0.97f,
                    transformOrigin = TransformOrigin(0.2f, 0.5f),
                    animationSpec = motion.spatialSpec(durationMillis = 170),
                )
        } else {
            fadeOut(motion.effectSpec(durationMillis = 140))
        }

        val popEnterAnim = if (reducedMotion) {
            fadeIn(motion.effectSpec())
        } else if (motion.usesSprings) {
            fadeIn(motion.effectSpec(durationMillis = 170)) +
                scaleIn(
                    initialScale = 0.97f,
                    transformOrigin = TransformOrigin(0.2f, 0.5f),
                    animationSpec = motion.spatialSpec(durationMillis = 200),
                )
        } else {
            fadeIn(motion.effectSpec(durationMillis = 160))
        }

        val popExitAnim = if (reducedMotion) {
            fadeOut(motion.effectSpec())
        } else if (motion.usesSprings) {
            fadeOut(motion.effectSpec(durationMillis = 150)) +
                scaleOut(
                    targetScale = 0.95f,
                    transformOrigin = TransformOrigin(0.8f, 0.5f),
                    animationSpec = motion.spatialSpec(durationMillis = 190),
                ) +
                slideOutHorizontally(
                    motion.spatialSpec(
                        durationMillis = 190,
                        visibilityThreshold = IntOffset.VisibilityThreshold,
                    ),
                ) { it / 5 }
        } else {
            fadeOut(motion.effectSpec(durationMillis = 140)) +
                slideOutHorizontally(
                    motion.spatialSpec(
                        durationMillis = 160,
                        visibilityThreshold = IntOffset.VisibilityThreshold,
                    ),
                ) { it / 8 }
        }

        NavHost(
            navController = navController,
            startDestination = SettingsRoute.HUB,
            enterTransition = { enterAnim },
            exitTransition = { exitAnim },
            popEnterTransition = { popEnterAnim },
            popExitTransition = { popExitAnim },
            modifier = Modifier.fillMaxSize(),
        ) {
            composable(SettingsRoute.HUB) {
                BackHandler(onBack = onClose)
                SettingsHubDestination(
                    appContainer = appContainer,
                    selectedCategory = null,
                    onOpenCategory = { category -> navController.navigate(category.toRoute()) },
                    onPreviewOnboarding = onPreviewOnboarding,
                    onClose = onClose,
                )
            }

            listOf(
                SettingsRoute.APPEARANCE,
                SettingsRoute.HOME,
                SettingsRoute.DOCK,
                SettingsRoute.APP_GRID,
                SettingsRoute.HIDDEN_APPS,
                SettingsRoute.OVERVIEW,
                SettingsRoute.SEARCH,
                SettingsRoute.FOLDERS,
                SettingsRoute.GESTURES,
                SettingsRoute.ABOUT,
            ).forEach { route ->
                composable(route) {
                    SettingsDetailDestination(
                        route = route,
                        appContainer = appContainer,
                        onOpenWallpaperPicker = onOpenWallpaperPicker,
                        onActivateWeatherLocation = onActivateWeatherLocation,
                        onSearchWeatherCities = onSearchWeatherCities,
                        onSelectWeatherLocation = onSelectWeatherLocation,
                        onSelectWeatherProvider = onSelectWeatherProvider,
                        onOpenHiddenApps = { navController.navigate(SettingsRoute.HIDDEN_APPS) },
                        onBack = { navController.popBackStack() },
                    )
                }
            }
        }
    }
}

private fun SettingsCategory.toRoute(): String = when (this) {
    SettingsCategory.APPEARANCE -> SettingsRoute.APPEARANCE
    SettingsCategory.HOME -> SettingsRoute.HOME
    SettingsCategory.DOCK -> SettingsRoute.DOCK
    SettingsCategory.APP_GRID -> SettingsRoute.APP_GRID
    SettingsCategory.OVERVIEW -> SettingsRoute.OVERVIEW
    SettingsCategory.SEARCH -> SettingsRoute.SEARCH
    SettingsCategory.FOLDERS -> SettingsRoute.FOLDERS
    SettingsCategory.GESTURES -> SettingsRoute.GESTURES
    SettingsCategory.ABOUT -> SettingsRoute.ABOUT
}

@Composable
private fun SettingsHubDestination(
    appContainer: AppContainer,
    selectedCategory: SettingsCategory?,
    onOpenCategory: (SettingsCategory) -> Unit,
    onPreviewOnboarding: () -> Unit,
    onClose: () -> Unit,
) {
    val hubViewModel: SettingsHubViewModel = viewModel(
        factory = SettingsHubViewModel.Factory(appContainer.settingsRepository),
    )
    val uiState by hubViewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            LauncherBackupHelper.exportToUri(
                context = context,
                uri = uri,
                settingsRepository = appContainer.settingsRepository,
                homeWorkspaceRepository = appContainer.homeWorkspaceRepository,
            ).onSuccess {
                Toast.makeText(context, R.string.backup_export_success, Toast.LENGTH_SHORT).show()
            }.onFailure {
                Toast.makeText(context, R.string.backup_export_error, Toast.LENGTH_SHORT).show()
            }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            LauncherBackupHelper.importFromUri(
                context = context,
                uri = uri,
                settingsRepository = appContainer.settingsRepository,
                homeWorkspaceRepository = appContainer.homeWorkspaceRepository,
                appWidgetController = appContainer.appWidgetController,
            ).onSuccess {
                Toast.makeText(context, R.string.backup_import_success, Toast.LENGTH_SHORT).show()
            }.onFailure {
                Toast.makeText(context, R.string.backup_import_error, Toast.LENGTH_SHORT).show()
            }
        }
    }

    SettingsHubScreen(
        uiState = uiState,
        selectedCategory = selectedCategory,
        onOpenAppearance = { onOpenCategory(SettingsCategory.APPEARANCE) },
        onOpenHome = { onOpenCategory(SettingsCategory.HOME) },
        onOpenDock = { onOpenCategory(SettingsCategory.DOCK) },
        onOpenAppGrid = { onOpenCategory(SettingsCategory.APP_GRID) },
        onOpenOverview = { onOpenCategory(SettingsCategory.OVERVIEW) },
        onOpenSearch = { onOpenCategory(SettingsCategory.SEARCH) },
        onOpenFolders = { onOpenCategory(SettingsCategory.FOLDERS) },
        onOpenGestures = { onOpenCategory(SettingsCategory.GESTURES) },
        onOpenAbout = { onOpenCategory(SettingsCategory.ABOUT) },
        onExportBackup = {
            exportLauncher.launch(context.getString(R.string.backup_default_filename))
        },
        onImportBackup = {
            importLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
        },
        onPreviewOnboarding = onPreviewOnboarding,
        onClose = onClose,
    )
}

@Composable
private fun SettingsDetailDestination(
    route: String,
    appContainer: AppContainer,
    onOpenWallpaperPicker: () -> Unit,
    onActivateWeatherLocation: () -> Unit,
    onSearchWeatherCities: suspend (String) -> Result<List<WeatherLocation>>,
    onSelectWeatherLocation: (WeatherLocation) -> Unit,
    onSelectWeatherProvider: (WeatherProvider) -> Unit,
    onOpenHiddenApps: () -> Unit,
    onBack: () -> Unit,
) {
    when (route) {
        SettingsRoute.APPEARANCE -> {
            val viewModel: AppearanceViewModel = viewModel(
                factory = AppearanceViewModel.Factory(
                    settingsRepository = appContainer.settingsRepository,
                    iconPackRepository = appContainer.iconPackRepository,
                    notificationBadgeRepository = appContainer.notificationBadgeRepository,
                ),
            )
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            AppearanceSettingsScreen(
                uiState = uiState,
                notificationBadgeRepository = appContainer.notificationBadgeRepository,
                onVisualStyleChange = viewModel::onVisualStyleChange,
                onThemeModeChange = viewModel::onThemeModeChange,
                onColorSourceModeChange = viewModel::onColorSourceModeChange,
                onAccentColorChange = viewModel::onAccentColorChange,
                onCustomAccentColorChange = viewModel::onCustomAccentColorChange,
                onMotionIntensityChange = viewModel::onMotionIntensityChange,
                onShapeFamilyChange = viewModel::onShapeFamilyChange,
                onIconShapeChange = viewModel::onIconShapeChange,
                onIconRoundnessChange = viewModel::onIconRoundnessChange,
                onTypographyWeightModeChange = viewModel::onTypographyWeightModeChange,
                onIconPackSelect = viewModel::onIconPackSelect,
                onNotificationDotsToggle = viewModel::onNotificationDotsToggle,
                onRefreshNotificationAccess = viewModel::refreshNotificationAccess,
                onOverviewBlurEnabledChange = viewModel::onOverviewBlurEnabledChange,
                onOverviewBlurRadiusChange = viewModel::onOverviewBlurRadiusChange,
                onBack = onBack,
            )
        }

        SettingsRoute.HOME -> {
            val viewModel: HomeSettingsViewModel = viewModel(
                factory = HomeSettingsViewModel.Factory(appContainer.settingsRepository),
            )
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val nativeWidgetsState by appContainer.nativeWidgetsRepository.state
                .collectAsStateWithLifecycle(initialValue = NativeWidgetsState())

            HomeSettingsScreen(
                uiState = uiState,
                weather = nativeWidgetsState.weather,
                weatherLocation = nativeWidgetsState.weatherLocation,
                onGridColumnsChange = viewModel::onGridColumnsChange,
                onGridRowsChange = viewModel::onGridRowsChange,
                onIconScaleChange = viewModel::onIconScaleChange,
                onShowLabelsChange = viewModel::onShowLabelsChange,
                onLabelScaleChange = viewModel::onLabelScaleChange,
                onLockLayoutChange = viewModel::onLockLayoutChange,
                onShowClockChange = viewModel::onShowClockChange,
                onShowClockWeatherChange = viewModel::onShowClockWeatherChange,
                onWeatherProviderChange = { provider ->
                    viewModel.onWeatherProviderChange(provider)
                    onSelectWeatherProvider(provider)
                },
                onClockStyleChange = viewModel::onClockStyleChange,
                onClockBackgroundAlphaChange = viewModel::onClockBackgroundAlphaChange,
                onShowQuickSettingsButtonChange = viewModel::onShowQuickSettingsButtonChange,
                onActivateWeatherLocation = onActivateWeatherLocation,
                onSearchWeatherCities = onSearchWeatherCities,
                onSelectWeatherLocation = onSelectWeatherLocation,
                onOpenWallpaperPicker = onOpenWallpaperPicker,
                onBack = onBack,
            )
        }

        SettingsRoute.DOCK -> {
            val viewModel: DockSettingsViewModel = viewModel(
                factory = DockSettingsViewModel.Factory(
                    appsRepository = appContainer.appsRepository,
                    settingsRepository = appContainer.settingsRepository,
                ),
            )
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            DockSettingsScreen(
                uiState = uiState,
                onIconCountChange = viewModel::onIconCountChange,
                onBackgroundAlphaChange = viewModel::onBackgroundAlphaChange,
                onShowSearchBarChange = viewModel::onShowSearchBarChange,
                onTogglePinnedApp = viewModel::onTogglePinnedApp,
                onResetToAuto = viewModel::onResetToAuto,
                onBack = onBack,
            )
        }

        SettingsRoute.APP_GRID -> {
            val viewModel: AppGridSettingsViewModel = viewModel(
                factory = AppGridSettingsViewModel.Factory(
                    appsRepository = appContainer.appsRepository,
                    filterAppsUseCase = appContainer.filterAppsUseCase,
                    settingsRepository = appContainer.settingsRepository,
                ),
            )
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            AppGridSettingsScreen(
                uiState = uiState,
                onColumnsChange = viewModel::onColumnsChange,
                onIconScaleChange = viewModel::onIconScaleChange,
                onLayoutModeChange = viewModel::onLayoutModeChange,
                onSortOrderChange = viewModel::onSortOrderChange,
                onOpenHiddenApps = onOpenHiddenApps,
                onBack = onBack,
            )
        }

        SettingsRoute.HIDDEN_APPS -> {
            val viewModel: AppGridSettingsViewModel = viewModel(
                factory = AppGridSettingsViewModel.Factory(
                    appsRepository = appContainer.appsRepository,
                    filterAppsUseCase = appContainer.filterAppsUseCase,
                    settingsRepository = appContainer.settingsRepository,
                ),
            )
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            HiddenAppsScreen(
                uiState = uiState,
                onShowHiddenInSearchChange = viewModel::onShowHiddenInSearchChange,
                onToggleAppHidden = viewModel::onToggleAppHidden,
                onSearchQueryChange = viewModel::onHiddenSearchQueryChange,
                onClearSearchQuery = viewModel::onClearHiddenSearchQuery,
                onBack = onBack,
            )
        }

        SettingsRoute.OVERVIEW -> {
            val viewModel: OverviewSettingsViewModel = viewModel(
                factory = OverviewSettingsViewModel.Factory(appContainer.settingsRepository),
            )
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            OverviewSettingsScreen(
                uiState = uiState,
                onBlurRadiusChange = viewModel::onBlurRadiusChange,
                onScrimAlphaChange = viewModel::onScrimAlphaChange,
                onTransitionStyleChange = viewModel::onTransitionStyleChange,
                onAnimationSpeedChange = viewModel::onAnimationSpeedChange,
                onHomeDepthEffectChange = viewModel::onHomeDepthEffectChange,
                onAppLaunchAnimationChange = viewModel::onAppLaunchAnimationChange,
                onBack = onBack,
            )
        }

        SettingsRoute.SEARCH -> {
            val viewModel: SearchSettingsViewModel = viewModel(
                factory = SearchSettingsViewModel.Factory(appContainer.settingsRepository),
            )
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            SearchSettingsScreen(
                uiState = uiState,
                onAutoFocusChange = viewModel::onAutoFocusChange,
                onMatchPackageNameChange = viewModel::onMatchPackageNameChange,
                onClearOnCloseChange = viewModel::onClearOnCloseChange,
                onShowWebSuggestionChange = viewModel::onShowWebSuggestionChange,
                onWebEngineChange = viewModel::onWebEngineChange,
                onBack = onBack,
            )
        }

        SettingsRoute.FOLDERS -> {
            val viewModel: FoldersSettingsViewModel = viewModel(
                factory = FoldersSettingsViewModel.Factory(
                    appsRepository = appContainer.appsRepository,
                    settingsRepository = appContainer.settingsRepository,
                ),
            )
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            FoldersSettingsScreen(
                uiState = uiState,
                onPreviewStyleChange = viewModel::onPreviewStyleChange,
                onColumnsChange = viewModel::onColumnsChange,
                onSortAppsInsideChange = viewModel::onSortAppsInsideChange,
                onCloseOnAppLaunchChange = viewModel::onCloseOnAppLaunchChange,
                onSaveFolder = viewModel::saveFolder,
                onDeleteFolder = viewModel::deleteFolder,
                onBack = onBack,
            )
        }

        SettingsRoute.GESTURES -> {
            val viewModel: GesturesSettingsViewModel = viewModel(
                factory = GesturesSettingsViewModel.Factory(appContainer.settingsRepository),
            )
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            GesturesSettingsScreen(
                uiState = uiState,
                onSwipeUpChange = viewModel::onSwipeUpChange,
                onSwipeDownChange = viewModel::onSwipeDownChange,
                onDoubleTapChange = viewModel::onDoubleTapChange,
                onHomeButtonChange = viewModel::onHomeButtonChange,
                onSensitivityChange = viewModel::onSensitivityChange,
                onBack = onBack,
            )
        }

        SettingsRoute.ABOUT -> {
            AboutScreen(onBack = onBack)
        }
    }
}
