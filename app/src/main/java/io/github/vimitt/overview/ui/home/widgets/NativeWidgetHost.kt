package io.github.vimitt.overview.ui.home.widgets

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
import androidx.core.content.ContextCompat
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
 * Modal leve ativo dos widgets nativos e clima do relógio, exibido na camada superior da Home.
 */
sealed interface NativeWidgetActiveDialog {
    data class QuickNote(val note: QuickNoteItem?) : NativeWidgetActiveDialog
    data object QuickTask : NativeWidgetActiveDialog
    data object WeatherCityPicker : NativeWidgetActiveDialog
}

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
    val activeDialog: NativeWidgetActiveDialog?,
    val onDismissDialog: () -> Unit,
    val onRequestCalendarPermission: () -> Unit,
    val onRequestCoarseLocationOrUseGps: () -> Unit,
    val onOpenWeatherCityPicker: () -> Unit,
    val onOpenNoteDialog: (QuickNoteItem?) -> Unit,
    val onOpenTaskDialog: () -> Unit,
)

/**
 * Coordena receptores/listeners do sistema sob demanda (somente quando há widgets nativos
 * ou clima do relógio visíveis na tela inicial) sem emitir UI bloqueando a ordem Z da Home.
 */
@Composable
fun rememberNativeWidgetEnvironment(
    workspaceItems: List<HomeWorkspaceItem>,
    hasSavedWeatherLocation: Boolean,
    hasWeatherCache: Boolean,
    actions: NativeWidgetActions,
    isVisible: Boolean,
    includeClockWeather: Boolean = false,
    permissionRevision: Int = 0,
): NativeWidgetLiveEnvironment {
    val context = LocalContext.current

    val nativeTypes = remember(workspaceItems) {
        workspaceItems
            .asSequence()
            .filterIsInstance<HomeWorkspaceItem.NativeWidget>()
            .map { it.item.widgetType }
            .toSet()
    }
    val needsTime = isVisible && (
        NativeWidgetType.DAILY_SUMMARY in nativeTypes ||
            NativeWidgetType.AGENDA in nativeTypes
        )
    val needsCalendar = isVisible && (
        NativeWidgetType.AGENDA in nativeTypes ||
            NativeWidgetType.DAILY_SUMMARY in nativeTypes
        )
    val needsWeather = isVisible && (
        includeClockWeather ||
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
    var upcomingEvents by remember { mutableStateOf<List<CalendarEventItem>>(emptyList()) }
    var deviceStatus by remember {
        mutableStateOf(NativeWidgetSystemProviders.readDeviceStatusSnapshot(context))
    }
    var mediaSnapshot by remember {
        mutableStateOf(MediaSessionSnapshot())
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
        runCatching {
            ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_EXPORTED)
        }
        onDispose { runCatching { context.unregisterReceiver(receiver) } }
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
        runCatching {
            ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_EXPORTED)
        }
        onDispose { runCatching { context.unregisterReceiver(receiver) } }
    }

    // Agenda do calendário sob demanda ao ficar visível ou atualizar permissões
    LaunchedEffect(needsCalendar, permissionRevision, currentTimeMillis / 60_000L) {
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
    LaunchedEffect(needsWeather, hasSavedWeatherLocation, hasWeatherCache, permissionRevision) {
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

    var activeDialog by remember { mutableStateOf<NativeWidgetActiveDialog?>(null) }

    return remember(
        currentTimeMillis,
        nextAlarmMillis,
        hasCalendarPerm,
        upcomingEvents,
        deviceStatus,
        mediaSnapshot,
        activeDialog,
        actions,
    ) {
        NativeWidgetLiveEnvironment(
            currentTimeMillis = currentTimeMillis,
            nextAlarmMillis = nextAlarmMillis,
            hasCalendarPermission = hasCalendarPerm,
            upcomingEvents = upcomingEvents,
            deviceStatus = deviceStatus,
            mediaSession = mediaSnapshot,
            activeDialog = activeDialog,
            onDismissDialog = { activeDialog = null },
            onRequestCalendarPermission = actions.onRequestCalendarPermission,
            onRequestCoarseLocationOrUseGps = {
                if (NativeWidgetSystemProviders.hasCoarseLocationPermission(context) &&
                    NativeWidgetSystemProviders.isLocationEnabled(context)
                ) {
                    actions.onUseDeviceWeatherLocation()
                } else {
                    actions.onRequestLocationPermission()
                }
            },
            onOpenWeatherCityPicker = { activeDialog = NativeWidgetActiveDialog.WeatherCityPicker },
            onOpenNoteDialog = { note -> activeDialog = NativeWidgetActiveDialog.QuickNote(note) },
            onOpenTaskDialog = { activeDialog = NativeWidgetActiveDialog.QuickTask },
        )
    }
}

/**
 * Renderiza os diálogos modais de notas, tarefas e escolha de cidade acima de toda a Home,
 * evitando interceptação de toque pela grade ou coluna principal.
 */
@Composable
fun NativeWidgetDialogsOverlay(
    environment: NativeWidgetLiveEnvironment,
    actions: NativeWidgetActions,
) {
    when (val dialog = environment.activeDialog) {
        is NativeWidgetActiveDialog.QuickNote -> QuickNoteDialog(
            initialNote = dialog.note,
            onSave = actions.onSaveQuickNote,
            onDelete = actions.onDeleteQuickNote,
            onDismiss = environment.onDismissDialog,
        )

        NativeWidgetActiveDialog.QuickTask -> QuickTaskDialog(
            onAdd = actions.onAddQuickTask,
            onDismiss = environment.onDismissDialog,
        )

        NativeWidgetActiveDialog.WeatherCityPicker -> WeatherCityPickerDialog(
            onSearchCities = actions.onSearchWeatherCities,
            onSelectLocation = actions.onSelectWeatherLocation,
            onUseGpsLocation = environment.onRequestCoarseLocationOrUseGps,
            onDismiss = environment.onDismissDialog,
        )

        null -> Unit
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
            NativeWidgetType.DAILY_SUMMARY -> DailySummaryNativeWidget(
                nextEvent = environment.upcomingEvents.firstOrNull(),
                weather = nativeWidgetsState.weather,
                nextAlarmMillis = environment.nextAlarmMillis,
                currentTimeMillis = environment.currentTimeMillis,
                spanX = spanX,
                spanY = spanY,
                isVisible = isVisible,
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
                spanX = spanX,
                spanY = spanY,
                onRequestPermission = environment.onRequestCalendarPermission,
                onOpenEvent = { ev -> NativeWidgetSystemProviders.openCalendarEvent(context, ev.id) },
                onOpenCalendarApp = { NativeWidgetSystemProviders.openCalendarApp(context) },
            )

            NativeWidgetType.WEATHER -> WeatherNativeWidget(
                weather = nativeWidgetsState.weather,
                isLoading = nativeWidgetsState.isWeatherLoading,
                errorText = nativeWidgetsState.weatherError,
                spanX = spanX,
                spanY = spanY,
                isVisible = isVisible,
                onOpenCityPicker = environment.onOpenWeatherCityPicker,
                onUseLocation = environment.onRequestCoarseLocationOrUseGps,
                onRefresh = { actions.onRefreshWeather(true) },
            )

            NativeWidgetType.MEDIA_PLAYER -> MediaPlayerNativeWidget(
                media = environment.mediaSession,
                spanX = spanX,
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
                spanX = spanX,
                spanY = spanY,
                isVisible = isVisible,
                onOpenSettings = { NativeWidgetSystemProviders.openBatterySettings(context) },
            )

            NativeWidgetType.QUICK_NOTES -> QuickNotesNativeWidget(
                notes = nativeWidgetsState.notes,
                spanX = spanX,
                spanY = spanY,
                onCreateNote = { environment.onOpenNoteDialog(null) },
                onEditNote = { note -> environment.onOpenNoteDialog(note) },
            )

            NativeWidgetType.TASKS -> TasksNativeWidget(
                tasks = nativeWidgetsState.tasks,
                spanX = spanX,
                spanY = spanY,
                isVisible = isVisible,
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
                spanX = spanX,
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
        lerp(colors.surfaceContainerHigh, colors.accentContainer, 0.30f)
    } else {
        colors.surfaceElevated
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(bg)
            .border(
                width = if (isExpressive) 1.2.dp else 1.dp,
                color = if (isExpressive) colors.accent.copy(alpha = 0.36f) else colors.hairlineBorder,
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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                modifier = Modifier.weight(1f),
            ) {
                if (isExpressive) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(colors.accent),
                    )
                }
                Text(
                    text = stringResource(type.titleRes),
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = if (isExpressive) FontWeight.Bold else FontWeight.SemiBold,
                    ),
                    color = colors.onSurface,
                )
            }
            Box(
                modifier = Modifier
                    .clip(if (isExpressive) shapes.pill else shapes.small)
                    .background(if (isExpressive) colors.accentContainer else colors.surfaceContainer)
                    .padding(horizontal = 8.dp, vertical = 2.dp),
            ) {
                Text(
                    text = "${type.defaultSpanX}×${type.defaultSpanY}",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = if (isExpressive) colors.onAccentContainer else colors.onSurfaceMuted,
                )
            }
        }
        Text(
            text = stringResource(type.descriptionRes),
            style = MaterialTheme.typography.bodySmall,
            color = colors.onSurfaceMuted,
        )
        Row(
            modifier = Modifier.padding(top = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(if (isExpressive) 14.dp else 6.dp)
                    .clip(if (isExpressive) CircleShape else shapes.small)
                    .background(colors.accent),
            )
            Box(
                modifier = Modifier
                    .width(if (isExpressive) 62.dp else 44.dp)
                    .height(if (isExpressive) 7.dp else 3.dp)
                    .clip(CircleShape)
                    .background(colors.accent.copy(alpha = if (isExpressive) 0.65f else 0.45f)),
            )
            Box(
                modifier = Modifier
                    .width(30.dp)
                    .height(if (isExpressive) 7.dp else 3.dp)
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
