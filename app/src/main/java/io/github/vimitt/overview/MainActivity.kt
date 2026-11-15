package io.github.vimitt.overview

import android.app.WallpaperColors
import android.app.WallpaperManager
import android.content.Intent
import android.graphics.Color as AndroidColor
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.vimitt.overview.data.WidgetEntry
import io.github.vimitt.overview.domain.model.AppInfo
import io.github.vimitt.overview.domain.model.LauncherPreferences
import io.github.vimitt.overview.ui.LauncherRoot
import io.github.vimitt.overview.ui.home.HomeViewModel
import io.github.vimitt.overview.ui.home.HomeWorkspaceItem
import io.github.vimitt.overview.ui.overview.OverviewViewModel
import io.github.vimitt.overview.ui.theme.OverviewTheme

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
    private var selectedHomeItemId by mutableStateOf<String?>(null)
    private var pendingWidgetRequest: PendingWidgetRequest? = null

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
        handleIncomingIntent(intent)

        setContent {
            val preferences by appContainer.settingsRepository.preferences
                .collectAsStateWithLifecycle(initialValue = LauncherPreferences())
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

            OverviewTheme(
                themeMode = preferences.themeMode,
                accentColor = Color(preferences.accentColorArgb),
            ) {
                val isDark = OverviewTheme.colors.isDark
                val anyModalOpen = isSettingsOpen || isAppPickerOpen || isWidgetPickerOpen || isOverviewOpen
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

                LauncherRoot(
                    appContainer = appContainer,
                    isOverviewOpen = isOverviewOpen,
                    overviewFocusSearchTrigger = overviewFocusSearchTrigger,
                    isHomeMenuOpen = isHomeMenuOpen,
                    isSettingsOpen = isSettingsOpen,
                    isAppPickerOpen = isAppPickerOpen,
                    isWidgetPickerOpen = isWidgetPickerOpen,
                    selectedHomeItem = selectedHomeItem,
                    overviewSettings = preferences.overview,
                    appWidgetController = appContainer.appWidgetController,
                    homeUiState = homeUiState,
                    searchQuery = searchQuery,
                    overviewUiState = overviewUiState,
                    allApps = allApps,
                    onOpenOverview = {
                        isHomeMenuOpen = false
                        selectedHomeItemId = null
                        isOverviewOpen = true
                    },
                    onOpenOverviewSearch = {
                        isHomeMenuOpen = false
                        selectedHomeItemId = null
                        isOverviewOpen = true
                        overviewFocusSearchTrigger++
                    },
                    onCloseOverview = {
                        isOverviewOpen = false
                        overviewViewModel.onClearSearch()
                    },
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
                    onSelectWidgetToAdd = { entry ->
                        isWidgetPickerOpen = false
                        beginAddWidgetFlow(entry)
                    },
                    loadAvailableWidgets = homeViewModel::loadAvailableWidgets,
                    onWorkspaceItemLongPress = { workspaceItem ->
                        isHomeMenuOpen = false
                        selectedHomeItemId = workspaceItem.item.id
                    },
                    onDismissHomeItemAction = { selectedHomeItemId = null },
                    onStartMovingHomeItem = homeViewModel::startMovingItem,
                    onResizeWidget = homeViewModel::resizeWidget,
                    onRemoveHomeItem = homeViewModel::removeHomeItem,
                    canPlaceMovingItemAt = homeViewModel::canPlaceMovingItemAt,
                    onPlaceMovingItemAt = homeViewModel::placeMovingItemAt,
                    onCancelMovingItem = homeViewModel::cancelMovingItem,
                    canPlaceDragItemAt = homeViewModel::canPlaceItemAt,
                    onMoveHomeItemTo = homeViewModel::moveItemTo,
                    onDropAppFromOverviewAt = { app, cellX, cellY ->
                        addAppShortcutToHome(
                            app = app,
                            preferredCellX = cellX,
                            preferredCellY = cellY,
                        )
                    },
                    onOpenSettings = {
                        isHomeMenuOpen = false
                        isSettingsOpen = true
                    },
                    onCloseSettings = { isSettingsOpen = false },
                    onOpenWallpaperPicker = ::openWallpaperPicker,
                    onSearchQueryChange = overviewViewModel::onSearchQueryChange,
                    onClearSearch = overviewViewModel::onClearSearch,
                    onAddAppFromOverview = { app ->
                        addAppShortcutToHome(app)
                    },
                    onOpenAppInfo = ::openAppInfo,
                    onLaunchFromHome = { app, view, bounds, reducedMotion ->
                        val effectiveReducedMotion = reducedMotion || !preferences.overview.appLaunchAnimation
                        homeViewModel.onAppClick(app, view, bounds, effectiveReducedMotion)
                    },
                    onLaunchFromOverview = { app, view, bounds, reducedMotion ->
                        val effectiveReducedMotion = reducedMotion || !preferences.overview.appLaunchAnimation
                        val launched = overviewViewModel.onAppClick(app, view, bounds, effectiveReducedMotion)
                        if (launched) {
                            isOverviewOpen = false
                            overviewViewModel.onClearSearch()
                        }
                    },
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        appContainer.appWidgetController.startListening()
    }

    override fun onStop() {
        super.onStop()
        appContainer.appWidgetController.stopListening()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
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

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent == null) return
        when {
            intent.action == Intent.ACTION_APPLICATION_PREFERENCES -> {
                isHomeMenuOpen = false
                isOverviewOpen = false
                isAppPickerOpen = false
                isWidgetPickerOpen = false
                selectedHomeItemId = null
                isSettingsOpen = true
            }

            intent.hasCategory(Intent.CATEGORY_HOME) || intent.action == Intent.ACTION_MAIN -> {
                isHomeMenuOpen = false
                isSettingsOpen = false
                isAppPickerOpen = false
                isWidgetPickerOpen = false
                selectedHomeItemId = null
                isOverviewOpen = false
                homeViewModel.cancelMovingItem()
                overviewViewModel.onClearSearch()
            }
        }
    }

    private fun addAppShortcutToHome(
        app: AppInfo,
        preferredCellX: Int? = null,
        preferredCellY: Int? = null,
    ) {
        homeViewModel.addAppToHome(
            app = app,
            preferredCellX = preferredCellX,
            preferredCellY = preferredCellY,
        ) { added ->
            if (!added || preferredCellX == null) {
                val msg = if (added) R.string.home_item_added else R.string.home_grid_full
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
            }
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
        ) { added ->
            val msg = if (added) R.string.home_item_added else R.string.home_grid_full
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
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

    private companion object {
        const val REQUEST_CONFIGURE_WIDGET = 4001
    }
}
