package io.github.vimitt.overview

import android.Manifest
import android.app.WallpaperColors
import android.app.WallpaperManager
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.content.pm.LauncherApps
import android.graphics.Color as AndroidColor
import android.net.Uri
import android.os.Bundle
import android.os.Process
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import io.github.vimitt.overview.data.LauncherShortcutHelper
import io.github.vimitt.overview.data.NativeWidgetSystemProviders
import io.github.vimitt.overview.data.SystemPanelHelper
import io.github.vimitt.overview.data.WidgetEntry
import io.github.vimitt.overview.domain.model.AppInfo
import io.github.vimitt.overview.domain.model.ColorSourceMode
import io.github.vimitt.overview.domain.model.LauncherGestureAction
import io.github.vimitt.overview.domain.model.LauncherPreferences
import io.github.vimitt.overview.domain.model.NativeWidgetType
import io.github.vimitt.overview.domain.model.ThemeMode
import io.github.vimitt.overview.ui.LauncherRoot
import io.github.vimitt.overview.ui.components.OverviewPopupSurface
import io.github.vimitt.overview.ui.home.HomeViewModel
import io.github.vimitt.overview.ui.home.HomeWorkspaceItem
import io.github.vimitt.overview.ui.home.widgets.NativeWidgetActions
import io.github.vimitt.overview.ui.onboarding.OnboardingScreen
import io.github.vimitt.overview.ui.overview.OverviewViewModel
import io.github.vimitt.overview.ui.theme.OverviewTheme
import kotlinx.coroutines.launch

/**
 * Activity principal registrada como launcher (`HOME`) em modo `singleTask`.
 * Também gerencia o ciclo de vida de `AppWidgetHost` e `ACTION_APPLICATION_PREFERENCES`.
 */
class MainActivity : ComponentActivity() {

    private data class PendingWidgetRequest(
        val appWidgetId: Int,
        val entry: WidgetEntry,
    )

    private val appContainer by lazy {
        (application as OverviewApplication).container
    }

    private val homeViewModel: HomeViewModel by viewModels {
        HomeViewModel.Factory(
            appContext = applicationContext,
            appsRepository = appContainer.appsRepository,
            settingsRepository = appContainer.settingsRepository,
            notificationBadgeRepository = appContainer.notificationBadgeRepository,
            homeWorkspaceRepository = appContainer.homeWorkspaceRepository,
            appWidgetController = appContainer.appWidgetController,
            nativeWidgetsRepository = appContainer.nativeWidgetsRepository,
            homeGridPlacementUseCase = appContainer.homeGridPlacementUseCase,
            defaultDashPackageProvider = appContainer.defaultDashPackageProvider,
            resolveDefaultDashAppsUseCase = appContainer.resolveDefaultDashAppsUseCase,
        )
    }

    private val overviewViewModel: OverviewViewModel by viewModels {
        OverviewViewModel.Factory(
            appsRepository = appContainer.appsRepository,
            filterAppsUseCase = appContainer.filterAppsUseCase,
            settingsRepository = appContainer.settingsRepository,
            notificationBadgeRepository = appContainer.notificationBadgeRepository,
        )
    }

    private var isOverviewOpen by mutableStateOf(false)
    private var overviewFocusSearchTrigger by mutableIntStateOf(0)
    private var isHomeMenuOpen by mutableStateOf(false)
    private var isSettingsOpen by mutableStateOf(false)
    private var isAppPickerOpen by mutableStateOf(false)
    private var isWidgetPickerOpen by mutableStateOf(false)
    private var isQuickSettingsOpen by mutableStateOf(false)
    private var openHomeFolderId by mutableStateOf<String?>(null)
    private var selectedHomeItemId by mutableStateOf<String?>(null)
    private var permissionRevision by mutableIntStateOf(0)
    private var isOnboardingPreviewOpen by mutableStateOf(false)
    private var onboardingRunKey by mutableIntStateOf(0)
    private var requestedCalendarOnce = false
    private var requestedLocationOnce = false
    private var pendingLocationRetryAfterSettings = false
    private var pendingWidgetRequest: PendingWidgetRequest? = null
    private var pendingPinItemRequest by mutableStateOf<LauncherApps.PinItemRequest?>(null)
    private var currentHomePage by mutableIntStateOf(0)
    private var currentPreferences: LauncherPreferences = LauncherPreferences()

    private val calendarPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        permissionRevision++
        if (!granted && !ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_CALENDAR)) {
            Toast.makeText(this, R.string.permission_calendar_settings_notice, Toast.LENGTH_SHORT).show()
            NativeWidgetSystemProviders.openAppPermissionSettings(this)
        }
    }

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { results ->
        permissionRevision++
        val granted = results[Manifest.permission.ACCESS_COARSE_LOCATION] == true ||
            results[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            NativeWidgetSystemProviders.hasCoarseLocationPermission(this)
        if (granted) {
            fetchDeviceWeatherLocationOrOpenGps()
        } else if (!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_COARSE_LOCATION)) {
            Toast.makeText(this, R.string.permission_location_settings_notice, Toast.LENGTH_SHORT).show()
            pendingLocationRetryAfterSettings = true
            NativeWidgetSystemProviders.openAppPermissionSettings(this)
        }
    }

    private val bindWidgetLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val pending = pendingWidgetRequest
        if (pending == null) return@registerForActivityResult

        if (result.resultCode == RESULT_OK) {
            continueAfterWidgetBound(pending)
        } else {
            appContainer.appWidgetController.deleteAppWidgetId(pending.appWidgetId)
            pendingWidgetRequest = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
        )
        handleIncomingIntent(intent, isNewIntent = false)

        setContent {
            val loadedPreferences by appContainer.settingsRepository.preferences
                .collectAsStateWithLifecycle(initialValue = null)
            val preferences = loadedPreferences ?: LauncherPreferences()
            SideEffect {
                currentPreferences = preferences
            }
            val allApps by appContainer.appsRepository.apps
                .collectAsStateWithLifecycle(initialValue = emptyList())
            val homeUiState by homeViewModel.uiState.collectAsStateWithLifecycle()
            val searchQuery by overviewViewModel.searchQuery.collectAsStateWithLifecycle()
            val overviewUiState by overviewViewModel.uiState.collectAsStateWithLifecycle()

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
                wm?.addOnColorsChangedListener(
                    listener,
                    android.os.Handler(android.os.Looper.getMainLooper()),
                )
                onDispose {
                    wm?.removeOnColorsChangedListener(listener)
                }
            }

            val appearance = preferences.appearance
            val resolvedAccent = if (appearance.colorSourceMode == ColorSourceMode.CUSTOM) {
                Color(appearance.customAccentColorArgb)
            } else {
                Color(appearance.accentColorArgb)
            }
            OverviewTheme(
                visualStyle = appearance.visualStyle,
                themeMode = appearance.themeMode,
                colorSourceMode = appearance.colorSourceMode,
                accentColor = resolvedAccent,
                motionIntensity = appearance.motionIntensity,
                shapeFamily = appearance.shapeFamily,
                iconShape = appearance.iconShape,
                iconRoundness = appearance.iconRoundness,
                typographyWeightMode = appearance.typographyWeightMode,
            ) {
                val isDark = OverviewTheme.colors.isDark
                val isOnboardingActive = loadedPreferences != null &&
                    (!preferences.onboardingCompleted || isOnboardingPreviewOpen)
                val anyModalOpen = isOnboardingActive || isSettingsOpen || isAppPickerOpen ||
                    isWidgetPickerOpen || isOverviewOpen || isQuickSettingsOpen
                val hasNotificationAccess = remember(permissionRevision) {
                    appContainer.notificationBadgeRepository.hasNotificationAccess()
                }
                LaunchedEffect(anyModalOpen, isDark, wallpaperSupportsDarkText) {
                    val controller = WindowCompat.getInsetsController(window, window.decorView)
                    val useLightBars = if (anyModalOpen) !isDark else wallpaperSupportsDarkText
                    controller.isAppearanceLightStatusBars = useLightBars
                    controller.isAppearanceLightNavigationBars = useLightBars
                }

                val selectedHomeItem: HomeWorkspaceItem? = remember(
                    selectedHomeItemId,
                    homeUiState.workspaceItems,
                ) {
                    val targetId = selectedHomeItemId ?: return@remember null
                    homeUiState.workspaceItems.firstOrNull { it.item.id == targetId }
                }

                val nativeWidgetActions = remember(homeViewModel, appContainer) {
                    NativeWidgetActions(
                        onSaveQuickNote = homeViewModel::saveQuickNote,
                        onDeleteQuickNote = homeViewModel::deleteQuickNote,
                        onAddQuickTask = homeViewModel::addQuickTask,
                        onToggleQuickTask = homeViewModel::toggleQuickTask,
                        onClearCompletedTasks = homeViewModel::clearCompletedTasks,
                        onRefreshWeather = { force ->
                            homeViewModel.refreshWeather(force = force)
                        },
                        onManualRefreshWeather = {
                            homeViewModel.refreshWeather(
                                force = true,
                                onError = { err ->
                                    Toast.makeText(this@MainActivity, err, Toast.LENGTH_SHORT).show()
                                },
                            )
                        },
                        onSearchWeatherCities = homeViewModel::searchWeatherCities,
                        onSelectWeatherLocation = homeViewModel::selectWeatherLocation,
                        onSelectWeatherProvider = { provider ->
                            homeViewModel.selectWeatherProvider(provider) { err ->
                                Toast.makeText(this@MainActivity, err, Toast.LENGTH_SHORT).show()
                            }
                        },
                        onUseDeviceWeatherLocation = ::fetchDeviceWeatherLocationOrOpenGps,
                        onHideClockWeather = homeViewModel::hideClockWeather,
                        onRequestCalendarPermission = ::requestCalendarPermission,
                        onRequestLocationPermission = ::requestWeatherLocation,
                        onSetClockToolMode = homeViewModel::setClockToolMode,
                        onSetTimerDuration = homeViewModel::setTimerDuration,
                        onToggleTimer = homeViewModel::toggleTimer,
                        onResetTimer = homeViewModel::resetTimer,
                        onFinishTimerIfElapsed = homeViewModel::finishTimerIfElapsed,
                        onToggleStopwatch = homeViewModel::toggleStopwatch,
                        onResetStopwatch = homeViewModel::resetStopwatch,
                        onOpenNotificationAccessSettings = {
                            runCatching {
                                startActivity(appContainer.notificationBadgeRepository.createAccessSettingsIntent())
                            }
                        },
                    )
                }

                if (loadedPreferences == null) {
                    Box(modifier = Modifier.fillMaxSize())
                } else {
                    AnimatedContent(
                        targetState = isOnboardingActive,
                        transitionSpec = {
                            fadeIn(tween(320)) togetherWith fadeOut(tween(240))
                        },
                        label = "rootOnboardingTransition",
                    ) { showOnboarding ->
                        if (showOnboarding) {
                            key(onboardingRunKey) {
                                OnboardingScreen(
                                    currentVisualStyle = appearance.visualStyle,
                                    permissionRevision = permissionRevision,
                                    notificationBadgeRepository = appContainer.notificationBadgeRepository,
                                    isDebugPreview = isOnboardingPreviewOpen,
                                    onRequestCalendarPermission = ::requestCalendarPermission,
                                    onRequestLocationPermission = ::requestWeatherLocation,
                                    onSelectVisualStyle = { style ->
                                        lifecycleScope.launch {
                                            appContainer.settingsRepository.setVisualStyle(style)
                                        }
                                    },
                                    onComplete = {
                                        lifecycleScope.launch {
                                            appContainer.settingsRepository.setOnboardingCompleted(true)
                                        }
                                    },
                                    onDismissPreview = {
                                        isOnboardingPreviewOpen = false
                                    },
                                )
                            }
                        } else {
                            LauncherRoot(
                                appContainer = appContainer,
                                isOverviewOpen = isOverviewOpen,
                                overviewFocusSearchTrigger = overviewFocusSearchTrigger,
                                isHomeMenuOpen = isHomeMenuOpen,
                                isSettingsOpen = isSettingsOpen,
                                isAppPickerOpen = isAppPickerOpen,
                                isWidgetPickerOpen = isWidgetPickerOpen,
                                isQuickSettingsOpen = isQuickSettingsOpen,
                                openHomeFolderId = openHomeFolderId,
                                selectedHomeItem = selectedHomeItem,
                                hasNotificationAccess = hasNotificationAccess,
                                overviewSettings = preferences.overview,
                                gestureSettings = preferences.gestures,
                                appWidgetController = appContainer.appWidgetController,
                                homeUiState = homeUiState,
                                searchQuery = searchQuery,
                                overviewUiState = overviewUiState,
                                allApps = allApps,
                                onOpenOverview = ::openOverview,
                                onOpenOverviewSearch = ::openOverviewSearch,
                                onCloseOverview = ::closeOverview,
                                onOpenQuickSettings = {
                                    isHomeMenuOpen = false
                                    selectedHomeItemId = null
                                    isQuickSettingsOpen = true
                                },
                                onCloseQuickSettings = { isQuickSettingsOpen = false },
                                onToggleThemeDark = { currentlyDark ->
                                    val nextMode = if (currentlyDark) ThemeMode.LIGHT else ThemeMode.DARK
                                    lifecycleScope.launch {
                                        appContainer.settingsRepository.setThemeMode(nextMode)
                                    }
                                },
                                onExecuteGesture = ::executeGestureAction,
                                onHomeLongPress = {
                                    selectedHomeItemId = null
                                    isHomeMenuOpen = true
                                },
                                onDismissHomeMenu = { isHomeMenuOpen = false },
                                onOpenAppPicker = { isAppPickerOpen = true },
                                onCloseAppPicker = { isAppPickerOpen = false },
                                onOpenWidgetPicker = { isWidgetPickerOpen = true },
                                onCloseWidgetPicker = { isWidgetPickerOpen = false },
                                onSelectHomeAppToAdd = { app ->
                                    isAppPickerOpen = false
                                    addAppShortcutToHome(app)
                                },
                                onSelectNativeWidgetToAdd = { widgetType ->
                                    isWidgetPickerOpen = false
                                    addNativeWidgetToHome(widgetType)
                                },
                                onSelectWidgetToAdd = { entry ->
                                    isWidgetPickerOpen = false
                                    beginAddWidgetFlow(entry)
                                },
                                loadAvailableWidgets = homeViewModel::loadAvailableWidgets,
                                nativeWidgetActions = nativeWidgetActions,
                                permissionRevision = permissionRevision,
                                onWorkspaceItemLongPress = { workspaceItem ->
                                    isHomeMenuOpen = false
                                    selectedHomeItemId = workspaceItem.item.id
                                },
                                onDismissHomeItemAction = { selectedHomeItemId = null },
                                onOpenHomeFolder = { folderId ->
                                    isHomeMenuOpen = false
                                    selectedHomeItemId = null
                                    openHomeFolderId = folderId
                                },
                                onDismissHomeFolder = { openHomeFolderId = null },
                                onAddHomeFolder = { name, appKeys ->
                                    homeViewModel.addFolderToHome(
                                        name = name,
                                        appKeys = appKeys,
                                        preferredPage = currentHomePage,
                                    ) { added ->
                                        val msg = if (added) R.string.home_item_added else R.string.home_grid_full
                                        Toast.makeText(this@MainActivity, msg, Toast.LENGTH_SHORT).show()
                                    }
                                },
                                onMergeShortcutsIntoFolder = { sourceId, targetId, appKey ->
                                    homeViewModel.mergeShortcutsIntoFolder(sourceId, targetId, appKey)
                                },
                                onAddAppToHomeFolder = { folderId, appKey, sourceId ->
                                    homeViewModel.addAppToHomeFolder(folderId, appKey, sourceId)
                                },
                                onRemoveAppFromHomeFolder = homeViewModel::removeAppFromHomeFolder,
                                onMoveAppFromHomeFolderToHome = { folderId, app ->
                                    homeViewModel.moveAppFromFolderToHome(folderId, app) { added ->
                                        if (!added) {
                                            Toast.makeText(this@MainActivity, R.string.home_grid_full, Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                onUpdateHomeFolder = homeViewModel::updateHomeFolder,
                                findFolderMergeTargetAt = homeViewModel::findFolderMergeTargetAt,
                                onStartMovingHomeItem = homeViewModel::startMovingItem,
                                onStartResizingHomeItem = homeViewModel::startResizingItem,
                                onCancelResizingHomeItem = homeViewModel::cancelResizingItem,
                                onResizeWidgetBounds = homeViewModel::resizeWidgetBounds,
                                onResizeWidget = homeViewModel::resizeWidget,
                                onRemoveHomeItem = homeViewModel::removeHomeItem,
                                canPlaceMovingItemAt = homeViewModel::canPlaceMovingItemAt,
                                onPlaceMovingItemAt = homeViewModel::placeMovingItemAt,
                                onCancelMovingItem = homeViewModel::cancelMovingItem,
                                canPlaceDragItemAt = homeViewModel::canPlaceItemAt,
                                onMoveHomeItemTo = homeViewModel::moveItemTo,
                                onDropAppFromOverviewAt = { app, cellX, cellY, page ->
                                    addAppShortcutToHome(
                                        app = app,
                                        preferredCellX = cellX,
                                        preferredCellY = cellY,
                                        preferredPage = page,
                                    )
                                },
                                onDropAppOnDock = homeViewModel::dropAppOnDock,
                                onRemoveAppFromDock = homeViewModel::removeAppFromDock,
                                currentHomePage = currentHomePage,
                                onCurrentHomePageChanged = { currentHomePage = it },
                                onLaunchPinnedShortcut = { pinned, sourceBounds ->
                                    val launched = LauncherShortcutHelper.launchPinnedShortcut(
                                        context = this@MainActivity,
                                        packageName = pinned.item.packageName,
                                        shortcutId = pinned.item.shortcutId,
                                        userHandle = pinned.userHandle ?: Process.myUserHandle(),
                                        sourceBounds = sourceBounds,
                                    )
                                    if (!launched) {
                                        Toast.makeText(
                                            this@MainActivity,
                                            R.string.shortcut_unavailable,
                                            Toast.LENGTH_SHORT,
                                        ).show()
                                    }
                                },
                                onOpenSettings = {
                                    isHomeMenuOpen = false
                                    isSettingsOpen = true
                                },
                                onCloseSettings = { isSettingsOpen = false },
                                onPreviewOnboarding = {
                                    onboardingRunKey++
                                    isOnboardingPreviewOpen = true
                                },
                                onOpenWallpaperPicker = ::openWallpaperPicker,
                                onSearchQueryChange = overviewViewModel::onSearchQueryChange,
                                onClearSearch = overviewViewModel::onClearSearch,
                                onAddAppFromOverview = { app ->
                                    addAppShortcutToHome(app)
                                },
                                onMoveAppToFolder = overviewViewModel::moveAppToFolder,
                                onCreateFolderWithApp = overviewViewModel::createFolderWithApp,
                                onRemoveAppFromFolder = overviewViewModel::removeAppFromFolder,
                                onOpenAppInfo = ::openAppInfo,
                                onLaunchFromHome = { app, view, bounds, reducedMotion ->
                                    val effectiveReducedMotion = reducedMotion || !preferences.overview.appLaunchAnimation
                                    homeViewModel.onAppClick(app, view, bounds, effectiveReducedMotion)
                                },
                                onLaunchFromOverview = { app, view, bounds, reducedMotion ->
                                    val effectiveReducedMotion = reducedMotion || !preferences.overview.appLaunchAnimation
                                    val launched = overviewViewModel.onAppClick(app, view, bounds, effectiveReducedMotion)
                                    if (launched) {
                                        closeOverview()
                                    }
                                },
                            )
                        }
                    }

                    val activePinRequest = pendingPinItemRequest
                    if (activePinRequest != null && !isOnboardingActive) {
                        PinItemConfirmationOverlay(
                            request = activePinRequest,
                            onAccept = {
                                pendingPinItemRequest = null
                                acceptPinItemRequest(activePinRequest)
                            },
                            onDismiss = {
                                pendingPinItemRequest = null
                            },
                        )
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        appContainer.appWidgetController.startListening()
    }

    override fun onResume() {
        super.onResume()
        permissionRevision++
        if (pendingLocationRetryAfterSettings) {
            pendingLocationRetryAfterSettings = false
            if (NativeWidgetSystemProviders.hasCoarseLocationPermission(this) &&
                NativeWidgetSystemProviders.isLocationEnabled(this)
            ) {
                fetchDeviceWeatherLocationOrOpenGps()
            }
        }
    }

    override fun onStop() {
        super.onStop()
        appContainer.appWidgetController.stopListening()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent, isNewIntent = true)
    }

    @Deprecated("Necessário para o fluxo de configuração do AppWidgetHost")
    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CONFIGURE_WIDGET) {
            val pending = pendingWidgetRequest ?: return
            pendingWidgetRequest = null
            if (resultCode == RESULT_OK) {
                finalizeWidgetPlacement(pending)
            } else {
                appContainer.appWidgetController.deleteAppWidgetId(pending.appWidgetId)
            }
        }
    }

    private fun openOverview() {
        isHomeMenuOpen = false
        selectedHomeItemId = null
        homeViewModel.cancelResizingItem()
        isOverviewOpen = true
        if (currentPreferences.search.autoFocusOnOpen) {
            overviewFocusSearchTrigger++
        }
    }

    private fun openOverviewSearch() {
        isHomeMenuOpen = false
        selectedHomeItemId = null
        homeViewModel.cancelResizingItem()
        isOverviewOpen = true
        overviewFocusSearchTrigger++
    }

    private fun closeOverview() {
        isOverviewOpen = false
        if (currentPreferences.search.clearOnClose) {
            overviewViewModel.onClearSearch()
        }
    }

    private fun executeGestureAction(action: LauncherGestureAction) {
        when (action) {
            LauncherGestureAction.NONE -> Unit
            LauncherGestureAction.OPEN_OVERVIEW -> openOverview()
            LauncherGestureAction.OPEN_OVERVIEW_SEARCH -> openOverviewSearch()
            LauncherGestureAction.OPEN_QUICK_SETTINGS -> {
                isHomeMenuOpen = false
                selectedHomeItemId = null
                openHomeFolderId = null
                isQuickSettingsOpen = true
            }
            LauncherGestureAction.EXPAND_NOTIFICATIONS -> SystemPanelHelper.expandNotificationsPanel(this)
            LauncherGestureAction.EXPAND_QUICK_SETTINGS -> {
                if (!SystemPanelHelper.expandQuickSettingsPanel(this)) {
                    isHomeMenuOpen = false
                    selectedHomeItemId = null
                    openHomeFolderId = null
                    isQuickSettingsOpen = true
                }
            }
            LauncherGestureAction.OPEN_SETTINGS -> {
                isHomeMenuOpen = false
                isOverviewOpen = false
                isQuickSettingsOpen = false
                isSettingsOpen = true
            }
            LauncherGestureAction.OPEN_WALLPAPER -> openWallpaperPicker()
            LauncherGestureAction.OPEN_ASSISTANT -> SystemPanelHelper.openAssistant(this)
            LauncherGestureAction.OPEN_CAMERA -> SystemPanelHelper.openCamera(this)
        }
    }

    private fun handleIncomingIntent(intent: Intent?, isNewIntent: Boolean) {
        if (intent == null) return
        when {
            intent.action == LauncherApps.ACTION_CONFIRM_PIN_SHORTCUT ||
                intent.action == LauncherApps.ACTION_CONFIRM_PIN_APPWIDGET -> {
                val launcherApps = getSystemService(LauncherApps::class.java)
                val request = runCatching { launcherApps?.getPinItemRequest(intent) }.getOrNull()
                if (request != null && request.isValid) {
                    isHomeMenuOpen = false
                    isSettingsOpen = false
                    isAppPickerOpen = false
                    isWidgetPickerOpen = false
                    isQuickSettingsOpen = false
                    openHomeFolderId = null
                    selectedHomeItemId = null
                    isOverviewOpen = false
                    pendingPinItemRequest = request
                }
            }

            intent.action == Intent.ACTION_APPLICATION_PREFERENCES -> {
                isHomeMenuOpen = false
                isOverviewOpen = false
                isAppPickerOpen = false
                isWidgetPickerOpen = false
                isQuickSettingsOpen = false
                openHomeFolderId = null
                selectedHomeItemId = null
                isSettingsOpen = true
            }

            intent.hasCategory(Intent.CATEGORY_HOME) -> {
                val wasAlreadyOnCleanHome = isNewIntent &&
                    hasWindowFocus() &&
                    !isOverviewOpen &&
                    !isHomeMenuOpen &&
                    !isSettingsOpen &&
                    !isAppPickerOpen &&
                    !isWidgetPickerOpen &&
                    !isQuickSettingsOpen &&
                    openHomeFolderId == null &&
                    selectedHomeItemId == null

                isHomeMenuOpen = false
                isSettingsOpen = false
                isAppPickerOpen = false
                isWidgetPickerOpen = false
                isQuickSettingsOpen = false
                openHomeFolderId = null
                selectedHomeItemId = null
                isOverviewOpen = false
                homeViewModel.cancelMovingItem()
                homeViewModel.cancelResizingItem()
                if (currentPreferences.search.clearOnClose) {
                    overviewViewModel.onClearSearch()
                }

                if (wasAlreadyOnCleanHome &&
                    currentPreferences.gestures.homeButtonAction != LauncherGestureAction.NONE
                ) {
                    executeGestureAction(currentPreferences.gestures.homeButtonAction)
                }
            }

            intent.action == Intent.ACTION_MAIN -> {
                val launchedFromInsideLauncher = isNewIntent && hasWindowFocus()
                isHomeMenuOpen = false
                isAppPickerOpen = false
                isWidgetPickerOpen = false
                isQuickSettingsOpen = false
                openHomeFolderId = null
                selectedHomeItemId = null
                isOverviewOpen = false
                homeViewModel.cancelMovingItem()
                homeViewModel.cancelResizingItem()
                if (currentPreferences.search.clearOnClose) {
                    overviewViewModel.onClearSearch()
                }
                isSettingsOpen = launchedFromInsideLauncher
            }
        }
    }

    private fun addAppShortcutToHome(
        app: AppInfo,
        preferredCellX: Int? = null,
        preferredCellY: Int? = null,
        preferredPage: Int = currentHomePage,
    ) {
        homeViewModel.addAppToHome(
            app = app,
            preferredCellX = preferredCellX,
            preferredCellY = preferredCellY,
            preferredPage = preferredPage,
        ) { added ->
            if (!added || preferredCellX == null) {
                val msg = if (added) R.string.home_item_added else R.string.home_grid_full
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun addNativeWidgetToHome(widgetType: NativeWidgetType) {
        homeViewModel.addNativeWidgetToHome(
            widgetType = widgetType,
            preferredPage = currentHomePage,
        ) { added ->
            val msg = if (added) R.string.home_item_added else R.string.home_grid_full
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        }
    }

    private fun beginAddWidgetFlow(entry: WidgetEntry) {
        val controller = appContainer.appWidgetController
        val appWidgetId = controller.allocateAppWidgetId()
        val request = PendingWidgetRequest(appWidgetId = appWidgetId, entry = entry)
        pendingWidgetRequest = request

        val bound = controller.bindAppWidgetIdIfAllowed(appWidgetId, entry.providerInfo)
        if (bound) {
            continueAfterWidgetBound(request)
        } else {
            bindWidgetLauncher.launch(controller.createBindIntent(appWidgetId, entry.providerInfo))
        }
    }

    private fun continueAfterWidgetBound(request: PendingWidgetRequest) {
        val controller = appContainer.appWidgetController
        if (controller.requiresConfiguration(request.entry.providerInfo)) {
            runCatching {
                controller.startConfigureActivity(
                    activity = this,
                    appWidgetId = request.appWidgetId,
                    requestCode = REQUEST_CONFIGURE_WIDGET,
                )
            }.onFailure {
                pendingWidgetRequest = null
                controller.deleteAppWidgetId(request.appWidgetId)
            }
        } else {
            pendingWidgetRequest = null
            finalizeWidgetPlacement(request)
        }
    }

    private fun finalizeWidgetPlacement(request: PendingWidgetRequest) {
        homeViewModel.addWidgetToHome(
            appWidgetId = request.appWidgetId,
            provider = request.entry.providerInfo.provider,
            spanX = request.entry.spanX,
            spanY = request.entry.spanY,
            preferredPage = currentHomePage,
        ) { added ->
            val msg = if (added) R.string.home_item_added else R.string.home_grid_full
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        }
    }

    private fun acceptPinItemRequest(request: LauncherApps.PinItemRequest) {
        if (!request.isValid) return
        if (homeViewModel.uiState.value.lockLayout) {
            Toast.makeText(this, R.string.home_layout_locked_notice, Toast.LENGTH_SHORT).show()
            return
        }

        when (request.requestType) {
            LauncherApps.PinItemRequest.REQUEST_TYPE_SHORTCUT -> {
                val info = request.shortcutInfo ?: return
                val accepted = runCatching { request.accept() }.getOrDefault(false)
                if (!accepted) return
                lifecycleScope.launch {
                    val icon = LauncherShortcutHelper.loadShortcutIcon(this@MainActivity, info)
                    val label = (info.shortLabel ?: info.longLabel ?: info.id).toString().trim()
                    homeViewModel.addPinnedShortcutToHome(
                        packageName = info.`package`,
                        shortcutId = info.id,
                        label = label.ifBlank { info.id },
                        userHandle = info.userHandle,
                        icon = icon,
                        preferredPage = currentHomePage,
                    ) { added ->
                        val msg = if (added) R.string.home_item_added else R.string.home_grid_full
                        Toast.makeText(this@MainActivity, msg, Toast.LENGTH_SHORT).show()
                    }
                }
            }

            LauncherApps.PinItemRequest.REQUEST_TYPE_APPWIDGET -> {
                val providerInfo = request.getAppWidgetProviderInfo(this) ?: return
                val controller = appContainer.appWidgetController
                val appWidgetId = controller.allocateAppWidgetId()
                val extras = Bundle().apply {
                    putInt(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                }
                val accepted = runCatching { request.accept(extras) }.getOrDefault(false)
                if (!accepted) {
                    controller.deleteAppWidgetId(appWidgetId)
                    return
                }
                val (spanX, spanY) = controller.computeDefaultSpan(providerInfo)
                val entry = WidgetEntry(
                    providerInfo = providerInfo,
                    label = providerInfo.loadLabel(packageManager).ifBlank { providerInfo.provider.shortClassName },
                    spanX = spanX,
                    spanY = spanY,
                    preview = null,
                )
                continueAfterWidgetBound(PendingWidgetRequest(appWidgetId = appWidgetId, entry = entry))
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

    private fun openAppInfo(app: AppInfo) {
        val detailsIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", app.packageName, null)
        }
        runCatching { startActivity(detailsIntent) }
    }

    private fun requestCalendarPermission() {
        if (NativeWidgetSystemProviders.hasCalendarPermission(this)) {
            permissionRevision++
            return
        }
        if (requestedCalendarOnce &&
            !ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_CALENDAR)
        ) {
            Toast.makeText(this, R.string.permission_calendar_settings_notice, Toast.LENGTH_SHORT).show()
            NativeWidgetSystemProviders.openAppPermissionSettings(this)
            return
        }
        requestedCalendarOnce = true
        runCatching {
            calendarPermissionLauncher.launch(Manifest.permission.READ_CALENDAR)
        }.onFailure {
            Toast.makeText(this, R.string.permission_calendar_settings_notice, Toast.LENGTH_SHORT).show()
            NativeWidgetSystemProviders.openAppPermissionSettings(this)
        }
    }

    private fun requestWeatherLocation() {
        if (NativeWidgetSystemProviders.hasCoarseLocationPermission(this)) {
            permissionRevision++
            fetchDeviceWeatherLocationOrOpenGps()
            return
        }
        if (requestedLocationOnce &&
            !ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_COARSE_LOCATION)
        ) {
            Toast.makeText(this, R.string.permission_location_settings_notice, Toast.LENGTH_SHORT).show()
            pendingLocationRetryAfterSettings = true
            NativeWidgetSystemProviders.openAppPermissionSettings(this)
            return
        }
        requestedLocationOnce = true
        runCatching {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                ),
            )
        }.onFailure {
            Toast.makeText(this, R.string.permission_location_settings_notice, Toast.LENGTH_SHORT).show()
            pendingLocationRetryAfterSettings = true
            NativeWidgetSystemProviders.openAppPermissionSettings(this)
        }
    }

    private fun fetchDeviceWeatherLocationOrOpenGps() {
        if (!NativeWidgetSystemProviders.isLocationEnabled(this)) {
            Toast.makeText(this, R.string.weather_location_gps_disabled_notice, Toast.LENGTH_SHORT).show()
            pendingLocationRetryAfterSettings = true
            NativeWidgetSystemProviders.openLocationSourceSettings(this)
            return
        }
        Toast.makeText(this, R.string.weather_location_resolving_notice, Toast.LENGTH_SHORT).show()
        homeViewModel.useDeviceWeatherLocation { location, forecastUpdated ->
            when {
                location == null -> {
                    Toast.makeText(this, R.string.weather_location_unavailable_notice, Toast.LENGTH_SHORT).show()
                }
                !forecastUpdated -> {
                    Toast.makeText(
                        this,
                        getString(R.string.weather_location_found_forecast_pending, location.name),
                        Toast.LENGTH_LONG,
                    ).show()
                }
            }
        }
    }

    private companion object {
        const val REQUEST_CONFIGURE_WIDGET = 4001
    }
}

@Composable
private fun PinItemConfirmationOverlay(
    request: LauncherApps.PinItemRequest,
    onAccept: () -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val colors = OverviewTheme.colors
    val isShortcut = request.requestType == LauncherApps.PinItemRequest.REQUEST_TYPE_SHORTCUT
    val titleRes = if (isShortcut) {
        R.string.pin_shortcut_dialog_title
    } else {
        R.string.pin_widget_dialog_title
    }
    val itemLabel = remember(request) {
        if (isShortcut) {
            val info = request.shortcutInfo
            (info?.shortLabel ?: info?.longLabel ?: info?.id ?: "").toString().trim()
        } else {
            val provider = request.getAppWidgetProviderInfo(context)
            provider?.loadLabel(context.packageManager).orEmpty()
        }
    }
    val shortcutIcon by produceState<ImageBitmap?>(initialValue = null, key1 = request) {
        val info = request.shortcutInfo
        value = if (isShortcut && info != null) {
            LauncherShortcutHelper.loadShortcutIcon(context, info)
        } else {
            null
        }
    }

    OverviewPopupSurface(
        onDismiss = onDismiss,
        bottomSheet = false,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(titleRes),
                style = OverviewTheme.typography.sectionHeader,
                color = colors.onSurface,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (shortcutIcon != null) {
                    Image(
                        bitmap = shortcutIcon!!,
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.size(44.dp),
                    )
                }
                Text(
                    text = itemLabel,
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                OutlinedButton(onClick = onDismiss) {
                    Text(text = stringResource(R.string.settings_cancel))
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = onAccept) {
                    Text(text = stringResource(R.string.pin_item_accept))
                }
            }
        }
    }
}
