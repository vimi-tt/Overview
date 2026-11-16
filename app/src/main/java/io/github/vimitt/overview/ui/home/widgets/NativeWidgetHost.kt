package io.github.vimitt.overview.ui.home.widgets

import android.Manifest
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Rect
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.view.View
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.vimitt.overview.R
import io.github.vimitt.overview.data.MediaSessionSnapshot
import io.github.vimitt.overview.data.NativeWidgetSystemProviders
import io.github.vimitt.overview.data.OverviewNotificationListenerService
import io.github.vimitt.overview.domain.model.AppInfo
import io.github.vimitt.overview.domain.model.CalendarEventItem
import io.github.vimitt.overview.domain.model.DeviceStatusSnapshot
import io.github.vimitt.overview.domain.model.NativeWidgetType
import io.github.vimitt.overview.domain.model.QuickNoteItem
import io.github.vimitt.overview.ui.home.HomeWorkspaceItem
import io.github.vimitt.overview.ui.theme.OverviewTheme

/**
 * Estado em memória dos provedores do sistema mantido apenas enquanto há widgets visíveis na Home.
 */
class NativeWidgetLiveEnvironment(
    val currentTimeMillis: Long,
    val nextAlarmMillis: Long?,
    val hasCalendarPermission: Boolean,
    val upcomingEvents: List<CalendarEventItem>,
    val deviceStatus: DeviceStatusSnapshot,
    val mediaSession: MediaSessionSnapshot,
    val onRequestCalendarPermission: () -> Unit,
    val onRequestCoarseLocationOrUseGps: () -> Unit,
    val onOpenWeatherCityPicker: () -> Unit,
    val onOpenNoteDialog: (QuickNoteItem?) -> Unit,
    val onOpenTaskDialog: () -> Unit,
)

/**
 * Coordena receptores/listeners do sistema sob demanda (somente quando há widgets nativos visíveis
 * na tela inicial) e gerencia os diálogos leves de nota rápida, tarefa e seleção de cidade.
 */
@Composable
fun rememberNativeWidgetEnvironment(
    workspaceItems: List<HomeWorkspaceItem>,
    hasSavedWeatherLocation: Boolean,
    hasWeatherCache: Boolean,
    actions: NativeWidgetActions,
    isVisible: Boolean,
): NativeWidgetLiveEnvironment {
    val context = LocalContext.current

    val nativeTypes = remember(workspaceItems) {
        workspaceItems
            .asSequence()
            .filterIsInstance<HomeWorkspaceItem.NativeWidget>()
            .map { it.item.widgetType }
            .toSet()
    }
    val hasNativeWidgets = nativeTypes.isNotEmpty()
    val needsTime = isVisible && (
        NativeWidgetType.CLOCK_ALARM in nativeTypes ||
            NativeWidgetType.DAILY_SUMMARY in nativeTypes ||
            NativeWidgetType.AGENDA in nativeTypes
        )
    val needsCalendar = isVisible && (
        NativeWidgetType.AGENDA in nativeTypes ||
            NativeWidgetType.DAILY_SUMMARY in nativeTypes
        )
    val needsWeather = isVisible && (
        NativeWidgetType.WEATHER in nativeTypes ||
            NativeWidgetType.DAILY_SUMMARY in nativeTypes
        )
    val needsDevice = isVisible && NativeWidgetType.DEVICE_STATUS in nativeTypes
    val needsMedia = isVisible && NativeWidgetType.MEDIA_PLAYER in nativeTypes

    var currentTimeMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var nextAlarmMillis by remember { mutableStateOf<Long?>(null) }
    var hasCalendarPerm by remember {
        mutableStateOf(NativeWidgetSystemProviders.hasCalendarPermission(context))
    }
    var calendarReloadTick by remember { mutableIntStateOf(0) }
    var upcomingEvents by remember { mutableStateOf<List<CalendarEventItem>>(emptyList()) }
    var deviceStatus by remember {
        mutableStateOf(NativeWidgetSystemProviders.readDeviceStatusSnapshot(context))
    }
    var mediaSnapshot by remember {
        mutableStateOf(MediaSessionSnapshot())
    }

    val calendarPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        hasCalendarPerm = granted
        calendarReloadTick++
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            actions.onUseDeviceWeatherLocation()
        }
    }

    // Relógio + Alarme somente quando visível e necessário
    DisposableEffect(context, needsTime) {
        if (!needsTime) return@DisposableEffect onDispose {}
        currentTimeMillis = System.currentTimeMillis()
        nextAlarmMillis = NativeWidgetSystemProviders.readNextAlarmTimeMillis(context)
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                currentTimeMillis = System.currentTimeMillis()
                nextAlarmMillis = NativeWidgetSystemProviders.readNextAlarmTimeMillis(context)
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_TIME_TICK)
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
            addAction("android.app.action.NEXT_ALARM_CLOCK_CHANGED")
        }
        context.registerReceiver(receiver, filter)
        onDispose { context.unregisterReceiver(receiver) }
    }

    // Bateria / Armazenamento / Rede somente quando o widget de dispositivo está visível
    DisposableEffect(context, needsDevice) {
        if (!needsDevice) return@DisposableEffect onDispose {}
        deviceStatus = NativeWidgetSystemProviders.readDeviceStatusSnapshot(context)
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                deviceStatus = NativeWidgetSystemProviders.readDeviceStatusSnapshot(
                    context = context,
                    batteryIntent = if (intent?.action == Intent.ACTION_BATTERY_CHANGED) intent else null,
                )
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction("android.net.conn.CONNECTIVITY_CHANGE")
        }
        context.registerReceiver(receiver, filter)
        onDispose { context.unregisterReceiver(receiver) }
    }

    // Agenda do calendário sob demanda ao ficar visível
    LaunchedEffect(needsCalendar, calendarReloadTick, currentTimeMillis / 60_000L) {
        if (!needsCalendar) return@LaunchedEffect
        val perm = NativeWidgetSystemProviders.hasCalendarPermission(context)
        hasCalendarPerm = perm
        upcomingEvents = if (perm) {
            NativeWidgetSystemProviders.readUpcomingCalendarEvents(context, limit = 6)
        } else {
            emptyList()
        }
    }

    // Clima Open-Meteo: atualiza apenas respeitando cache TTL (30 min) quando visível
    LaunchedEffect(needsWeather, hasSavedWeatherLocation, hasWeatherCache) {
        if (needsWeather && hasSavedWeatherLocation) {
            actions.onRefreshWeather(false)
        }
    }

    // Sessão de mídia ativa (listener ativo somente com widget na tela)
    DisposableEffect(context, needsMedia) {
        if (!needsMedia) return@DisposableEffect onDispose {}
        val listenerComponent = ComponentName(context, OverviewNotificationListenerService::class.java)
        val hasAccess = isNotificationListenerEnabled(context, listenerComponent)
        if (!hasAccess) {
            mediaSnapshot = MediaSessionSnapshot(hasNotificationAccess = false)
            return@DisposableEffect onDispose {}
        }

        val msm = context.getSystemService(MediaSessionManager::class.java)
        var attachedController: MediaController? = null
        val controllerCallback = object : MediaController.Callback() {
            override fun onPlaybackStateChanged(state: PlaybackState?) {
                mediaSnapshot = NativeWidgetSystemProviders.readMediaSnapshot(
                    context = context,
                    controllers = null,
                    hasAccess = true,
                )
            }

            override fun onMetadataChanged(metadata: MediaMetadata?) {
                mediaSnapshot = NativeWidgetSystemProviders.readMediaSnapshot(
                    context = context,
                    controllers = null,
                    hasAccess = true,
                )
            }
        }

        fun bindControllers(controllers: List<MediaController>?) {
            attachedController?.unregisterCallback(controllerCallback)
            val snap = NativeWidgetSystemProviders.readMediaSnapshot(
                context = context,
                controllers = controllers,
                hasAccess = true,
            )
            mediaSnapshot = snap
            attachedController = snap.controller?.also { it.registerCallback(controllerCallback) }
        }

        val sessionListener = MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
            bindControllers(controllers)
        }

        runCatching {
            val initial = msm?.getActiveSessions(listenerComponent)
            bindControllers(initial)
            msm?.addOnActiveSessionsChangedListener(sessionListener, listenerComponent)
        }.onFailure {
            mediaSnapshot = MediaSessionSnapshot(hasNotificationAccess = false)
        }

        onDispose {
            attachedController?.unregisterCallback(controllerCallback)
            runCatching { msm?.removeOnActiveSessionsChangedListener(sessionListener) }
        }
    }

    // Diálogos leves locais dos widgets nativos
    var editingNote by remember { mutableStateOf<QuickNoteItem?>(null) }
    var showNoteDialog by remember { mutableStateOf(false) }
    var showTaskDialog by remember { mutableStateOf(false) }
    var showWeatherCityDialog by remember { mutableStateOf(false) }

    if (hasNativeWidgets && showNoteDialog) {
        QuickNoteDialog(
            initialNote = editingNote,
            onSave = actions.onSaveQuickNote,
            onDelete = actions.onDeleteQuickNote,
            onDismiss = {
                showNoteDialog = false
                editingNote = null
            },
        )
    }

    if (hasNativeWidgets && showTaskDialog) {
        QuickTaskDialog(
            onAdd = actions.onAddQuickTask,
            onDismiss = { showTaskDialog = false },
        )
    }

    if (hasNativeWidgets && showWeatherCityDialog) {
        WeatherCityPickerDialog(
            onSearchCities = actions.onSearchWeatherCities,
            onSelectLocation = actions.onSelectWeatherLocation,
            onUseGpsLocation = {
                if (NativeWidgetSystemProviders.hasCoarseLocationPermission(context)) {
                    actions.onUseDeviceWeatherLocation()
                } else {
                    locationPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                }
            },
            onDismiss = { showWeatherCityDialog = false },
        )
    }

    return remember(
        currentTimeMillis,
        nextAlarmMillis,
        hasCalendarPerm,
        upcomingEvents,
        deviceStatus,
        mediaSnapshot,
    ) {
        NativeWidgetLiveEnvironment(
            currentTimeMillis = currentTimeMillis,
            nextAlarmMillis = nextAlarmMillis,
            hasCalendarPermission = hasCalendarPerm,
            upcomingEvents = upcomingEvents,
            deviceStatus = deviceStatus,
            mediaSession = mediaSnapshot,
            onRequestCalendarPermission = {
                calendarPermissionLauncher.launch(Manifest.permission.READ_CALENDAR)
            },
            onRequestCoarseLocationOrUseGps = {
                if (NativeWidgetSystemProviders.hasCoarseLocationPermission(context)) {
                    actions.onUseDeviceWeatherLocation()
                } else {
                    locationPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                }
            },
            onOpenWeatherCityPicker = { showWeatherCityDialog = true },
            onOpenNoteDialog = { note ->
                editingNote = note
                showNoteDialog = true
            },
            onOpenTaskDialog = { showTaskDialog = true },
        )
    }
}

/**
 * Despacha um item [HomeWorkspaceItem.NativeWidget] para o componente correspondente.
 */
@Composable
fun NativeWidgetHost(
    widgetItem: HomeWorkspaceItem.NativeWidget,
    environment: NativeWidgetLiveEnvironment,
    nativeWidgetsState: io.github.vimitt.overview.domain.model.NativeWidgetsState,
    frequentApps: List<AppInfo>,
    showNotificationDots: Boolean,
    badgePackages: Set<String>,
    isVisible: Boolean,
    isMoving: Boolean,
    actions: NativeWidgetActions,
    onAppClick: (app: AppInfo, sourceView: View, sourceBounds: Rect?) -> Unit,
    onDragStart: (Offset) -> Unit,
    onDragMove: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val type = widgetItem.item.widgetType
    val spanX = widgetItem.item.spanX
    val spanY = widgetItem.item.spanY

    NativeWidgetCard(
        itemId = widgetItem.item.id,
        isVisible = isVisible,
        isMoving = isMoving,
        compactPadding = spanY <= 1,
        onDragStart = onDragStart,
        onDragMove = onDragMove,
        onDragEnd = onDragEnd,
        onDragCancel = onDragCancel,
        modifier = modifier,
    ) {
        when (type) {
            NativeWidgetType.CLOCK_ALARM -> ClockAlarmNativeWidget(
                currentTimeMillis = environment.currentTimeMillis,
                nextAlarmMillis = environment.nextAlarmMillis,
                spanY = spanY,
                isVisible = isVisible,
                onOpenAlarms = { NativeWidgetSystemProviders.openAlarmsApp(context) },
            )

            NativeWidgetType.DAILY_SUMMARY -> DailySummaryNativeWidget(
                nextEvent = environment.upcomingEvents.firstOrNull(),
                weather = nativeWidgetsState.weather,
                nextAlarmMillis = environment.nextAlarmMillis,
                currentTimeMillis = environment.currentTimeMillis,
                onOpenEvent = { ev ->
                    if (ev != null) {
                        NativeWidgetSystemProviders.openCalendarEvent(context, ev.id)
                    } else if (!environment.hasCalendarPermission) {
                        environment.onRequestCalendarPermission()
                    } else {
                        NativeWidgetSystemProviders.openCalendarApp(context)
                    }
                },
                onOpenWeather = { environment.onOpenWeatherCityPicker() },
                onOpenAlarms = { NativeWidgetSystemProviders.openAlarmsApp(context) },
            )

            NativeWidgetType.AGENDA -> AgendaNativeWidget(
                hasPermission = environment.hasCalendarPermission,
                events = environment.upcomingEvents,
                spanY = spanY,
                onRequestPermission = environment.onRequestCalendarPermission,
                onOpenEvent = { ev -> NativeWidgetSystemProviders.openCalendarEvent(context, ev.id) },
                onOpenCalendarApp = { NativeWidgetSystemProviders.openCalendarApp(context) },
            )

            NativeWidgetType.WEATHER -> WeatherNativeWidget(
                weather = nativeWidgetsState.weather,
                isLoading = nativeWidgetsState.isWeatherLoading,
                errorText = nativeWidgetsState.weatherError,
                spanY = spanY,
                onOpenCityPicker = environment.onOpenWeatherCityPicker,
                onUseLocation = environment.onRequestCoarseLocationOrUseGps,
                onRefresh = { actions.onRefreshWeather(true) },
            )

            NativeWidgetType.MEDIA_PLAYER -> MediaPlayerNativeWidget(
                media = environment.mediaSession,
                spanY = spanY,
                isVisible = isVisible,
                onRequestNotificationAccess = actions.onOpenNotificationAccessSettings,
                onOpenPlayerApp = {
                    NativeWidgetSystemProviders.openMediaSessionApp(context, environment.mediaSession)
                },
                onPlayPause = {
                    val controller = environment.mediaSession.controller ?: return@MediaPlayerNativeWidget
                    if (environment.mediaSession.isPlaying) {
                        controller.transportControls.pause()
                    } else {
                        controller.transportControls.play()
                    }
                },
                onSkipPrevious = {
                    environment.mediaSession.controller?.transportControls?.skipToPrevious()
                },
                onSkipNext = {
                    environment.mediaSession.controller?.transportControls?.skipToNext()
                },
            )

            NativeWidgetType.DEVICE_STATUS -> DeviceStatusNativeWidget(
                status = environment.deviceStatus,
                spanY = spanY,
                onOpenSettings = { NativeWidgetSystemProviders.openBatterySettings(context) },
            )

            NativeWidgetType.QUICK_NOTES -> QuickNotesNativeWidget(
                notes = nativeWidgetsState.notes,
                spanY = spanY,
                onCreateNote = { environment.onOpenNoteDialog(null) },
                onEditNote = { note -> environment.onOpenNoteDialog(note) },
            )

            NativeWidgetType.TASKS -> TasksNativeWidget(
                tasks = nativeWidgetsState.tasks,
                spanY = spanY,
                onToggleTask = actions.onToggleQuickTask,
                onAddTask = environment.onOpenTaskDialog,
                onClearCompleted = actions.onClearCompletedTasks,
            )

            NativeWidgetType.FREQUENT_APPS -> FrequentAppsNativeWidget(
                apps = frequentApps,
                spanX = spanX,
                spanY = spanY,
                showNotificationDots = showNotificationDots,
                badgePackages = badgePackages,
                onAppClick = onAppClick,
            )

            NativeWidgetType.TIMER_STOPWATCH -> TimerStopwatchNativeWidget(
                state = nativeWidgetsState.timerStopwatch,
                spanY = spanY,
                isVisible = isVisible,
                onSetMode = actions.onSetClockToolMode,
                onSetTimerDuration = actions.onSetTimerDuration,
                onToggleTimer = actions.onToggleTimer,
                onResetTimer = actions.onResetTimer,
                onFinishTimerIfElapsed = actions.onFinishTimerIfElapsed,
                onToggleStopwatch = actions.onToggleStopwatch,
                onResetStopwatch = actions.onResetStopwatch,
            )
        }
    }
}

/**
 * Miniatura ilustrativa para a lista de widgets nativos no seletor de widgets,
 * adaptada visualmente ao estilo GNOME ou Material Expressive.
 */
@Composable
fun NativeWidgetPickerThumbnail(
    type: NativeWidgetType,
    modifier: Modifier = Modifier,
) {
    val colors = OverviewTheme.colors
    val shapes = OverviewTheme.shapes
    val isExpressive = OverviewTheme.components.isExpressive
    val cardShape = if (isExpressive) shapes.dash else shapes.medium
    val bg = if (isExpressive) {
        lerp(colors.surfaceContainerHigh, colors.accentContainer, 0.24f)
    } else {
        colors.surfaceElevated
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(bg)
            .border(
                width = 1.dp,
                color = if (isExpressive) colors.accent.copy(alpha = 0.28f) else colors.hairlineBorder,
                shape = cardShape,
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(type.titleRes),
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = if (isExpressive) FontWeight.Bold else FontWeight.SemiBold,
                ),
                color = colors.onSurface,
            )
            Box(
                modifier = Modifier
                    .clip(if (isExpressive) shapes.pill else shapes.small)
                    .background(if (isExpressive) colors.accentContainer else colors.surfaceContainer)
                    .padding(horizontal = 8.dp, vertical = 2.dp),
            ) {
                Text(
                    text = "${type.defaultSpanX}×${type.defaultSpanY}",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = if (isExpressive) colors.onAccentContainer else colors.onSurfaceMuted,
                )
            }
        }
        Text(
            text = stringResource(type.descriptionRes),
            style = MaterialTheme.typography.bodySmall,
            color = colors.onSurfaceMuted,
        )
        // Pequeno detalhe gráfico temático diferenciando os estilos
        Row(
            modifier = Modifier.padding(top = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(if (isExpressive) 10.dp else 6.dp)
                    .clip(if (isExpressive) CircleShape else shapes.small)
                    .background(colors.accent),
            )
            Box(
                modifier = Modifier
                    .width(if (isExpressive) 54.dp else 44.dp)
                    .height(if (isExpressive) 6.dp else 3.dp)
                    .clip(CircleShape)
                    .background(colors.accent.copy(alpha = 0.45f)),
            )
            Box(
                modifier = Modifier
                    .width(26.dp)
                    .height(if (isExpressive) 6.dp else 3.dp)
                    .clip(CircleShape)
                    .background(colors.onSurfaceMuted.copy(alpha = 0.25f)),
            )
        }
    }
}

private fun isNotificationListenerEnabled(
    context: Context,
    listenerComponent: ComponentName,
): Boolean {
    val enabled = android.provider.Settings.Secure.getString(
        context.contentResolver,
        "enabled_notification_listeners",
    ).orEmpty()
    val full = listenerComponent.flattenToString()
    val short = listenerComponent.flattenToShortString()
    return enabled.split(':').any { it == full || it == short }
}
