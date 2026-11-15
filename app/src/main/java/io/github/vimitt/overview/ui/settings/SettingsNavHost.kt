package io.github.vimitt.overview.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.unit.IntOffset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import io.github.vimitt.overview.di.AppContainer
import io.github.vimitt.overview.ui.settings.about.AboutScreen
import io.github.vimitt.overview.ui.settings.appgrid.AppGridSettingsScreen
import io.github.vimitt.overview.ui.settings.appgrid.AppGridSettingsViewModel
import io.github.vimitt.overview.ui.settings.appgrid.HiddenAppsScreen
import io.github.vimitt.overview.ui.settings.appearance.AppearanceSettingsScreen
import io.github.vimitt.overview.ui.settings.appearance.AppearanceViewModel
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
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val navController = rememberNavController()
    val motion = OverviewTheme.motion
    val reducedMotion = OverviewTheme.reducedMotion

    val enterAnim = if (reducedMotion) {
        fadeIn(motion.effectSpec())
    } else if (motion.usesSprings) {
        fadeIn(motion.effectSpec(durationMillis = 190)) +
            scaleIn(
                initialScale = 0.96f,
                transformOrigin = TransformOrigin(0.8f, 0.5f),
                animationSpec = motion.expressiveSpec(durationMillis = 240),
            ) +
            slideInHorizontally(
                motion.expressiveSpec(
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
        modifier = modifier,
    ) {
        composable(SettingsRoute.HUB) {
            BackHandler(onBack = onClose)

            val hubViewModel: SettingsHubViewModel = viewModel(
                factory = SettingsHubViewModel.Factory(appContainer.settingsRepository),
            )
            val uiState by hubViewModel.uiState.collectAsStateWithLifecycle()

            SettingsHubScreen(
                uiState = uiState,
                onOpenAppearance = { navController.navigate(SettingsRoute.APPEARANCE) },
                onOpenHome = { navController.navigate(SettingsRoute.HOME) },
                onOpenDock = { navController.navigate(SettingsRoute.DOCK) },
                onOpenAppGrid = { navController.navigate(SettingsRoute.APP_GRID) },
                onOpenOverview = { navController.navigate(SettingsRoute.OVERVIEW) },
                onOpenSearch = { navController.navigate(SettingsRoute.SEARCH) },
                onOpenFolders = { navController.navigate(SettingsRoute.FOLDERS) },
                onOpenGestures = { navController.navigate(SettingsRoute.GESTURES) },
                onOpenAbout = { navController.navigate(SettingsRoute.ABOUT) },
                onClose = onClose,
            )
        }

        composable(SettingsRoute.APPEARANCE) {
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
                onBack = { navController.popBackStack() },
            )
        }

        composable(SettingsRoute.HOME) {
            val viewModel: HomeSettingsViewModel = viewModel(
                factory = HomeSettingsViewModel.Factory(appContainer.settingsRepository),
            )
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            HomeSettingsScreen(
                uiState = uiState,
                onGridColumnsChange = viewModel::onGridColumnsChange,
                onGridRowsChange = viewModel::onGridRowsChange,
                onIconScaleChange = viewModel::onIconScaleChange,
                onShowLabelsChange = viewModel::onShowLabelsChange,
                onLabelScaleChange = viewModel::onLabelScaleChange,
                onLockLayoutChange = viewModel::onLockLayoutChange,
                onShowClockChange = viewModel::onShowClockChange,
                onClockStyleChange = viewModel::onClockStyleChange,
                onClockBackgroundAlphaChange = viewModel::onClockBackgroundAlphaChange,
                onOpenWallpaperPicker = onOpenWallpaperPicker,
                onBack = { navController.popBackStack() },
            )
        }

        composable(SettingsRoute.DOCK) {
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
                onBack = { navController.popBackStack() },
            )
        }

        composable(SettingsRoute.APP_GRID) {
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
                onOpenHiddenApps = { navController.navigate(SettingsRoute.HIDDEN_APPS) },
                onBack = { navController.popBackStack() },
            )
        }

        composable(SettingsRoute.HIDDEN_APPS) {
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
                onBack = { navController.popBackStack() },
            )
        }

        composable(SettingsRoute.OVERVIEW) {
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
                onBack = { navController.popBackStack() },
            )
        }

        composable(SettingsRoute.SEARCH) {
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
                onBack = { navController.popBackStack() },
            )
        }

        composable(SettingsRoute.FOLDERS) {
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
                onBack = { navController.popBackStack() },
            )
        }

        composable(SettingsRoute.GESTURES) {
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
                onBack = { navController.popBackStack() },
            )
        }

        composable(SettingsRoute.ABOUT) {
            AboutScreen(
                onBack = { navController.popBackStack() },
            )
        }
    }
}
