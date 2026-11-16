package io.github.vimitt.overview.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import io.github.vimitt.overview.R
import io.github.vimitt.overview.data.NativeWidgetSystemProviders
import io.github.vimitt.overview.data.QuickSettingsController
import io.github.vimitt.overview.data.QuickSettingsSnapshot
import io.github.vimitt.overview.ui.home.widgets.PlayPauseGlyph
import io.github.vimitt.overview.ui.home.widgets.SkipTrackGlyph
import io.github.vimitt.overview.ui.theme.OverviewTheme
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Pílula compacta no topo direito da tela inicial para abrir o painel rápido estilo GNOME.
 */
@Composable
fun HomeQuickSettingsPill(
    batteryLevel: Int,
    isCharging: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = OverviewTheme.colors
    val shape = OverviewTheme.shapes.pill
    val isExpressive = OverviewTheme.components.isExpressive
    val openCd = stringResource(R.string.quick_settings_open_cd)

    val pillFill = if (isExpressive) {
        lerp(colors.dashBackground, colors.accentContainer, 0.24f)
    } else {
        colors.dashBackground
    }
    val pillBorder = if (isExpressive) {
        colors.accent.copy(alpha = 0.28f)
    } else {
        colors.hairlineBorder
    }

    Row(
        modifier = modifier
            .defaultMinSize(minHeight = 36.dp)
            .clip(shape)
            .background(pillFill)
            .border(1.dp, pillBorder, shape)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = openCd }
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        QsSlidersGlyph(tint = colors.onSurface, size = 15.dp)
        QsBatteryGlyph(
            level = batteryLevel,
            isCharging = isCharging,
            tint = colors.onSurface,
            accent = colors.accent,
            size = 16.dp,
        )
        Text(
            text = stringResource(R.string.quick_settings_battery_percent, batteryLevel),
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.SemiBold,
                fontFeatureSettings = "tnum",
            ),
            color = colors.onSurface,
        )
    }
}

/**
 * Painel próprio de configurações rápidas inspirado no GNOME Shell (libadwaita)
 * com suporte automático ao estilo Expressive.
 */
@Composable
fun GnomeQuickSettingsPanel(
    hasNotificationAccess: Boolean,
    onToggleThemeDark: (currentlyDark: Boolean) -> Unit,
    onOpenLauncherSettings: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val colors = OverviewTheme.colors
    val shapes = OverviewTheme.shapes
    val motion = OverviewTheme.motion
    val isExpressive = OverviewTheme.components.isExpressive

    val torchCameraId = remember(context) { QuickSettingsController.findTorchCameraId(context) }
    var torchEnabled by remember { mutableStateOf(false) }
    var refreshRevision by remember { mutableIntStateOf(0) }

    DisposableEffect(context, torchCameraId) {
        val unregisterTorch = QuickSettingsController.registerTorchObserver(
            context = context,
            cameraId = torchCameraId,
            onTorchChanged = { enabled -> torchEnabled = enabled },
        )
        val unregisterSystem = QuickSettingsController.registerSystemStateReceiver(
            context = context,
            onStateChanged = { refreshRevision++ },
        )
        onDispose {
            unregisterTorch()
            unregisterSystem()
        }
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        refreshRevision++
    }

    val snapshot: QuickSettingsSnapshot = remember(
        refreshRevision,
        torchEnabled,
        torchCameraId,
        hasNotificationAccess,
    ) {
        QuickSettingsController.readSnapshot(
            context = context,
            torchEnabled = torchEnabled,
            torchAvailable = torchCameraId != null,
            hasNotificationAccess = hasNotificationAccess,
        )
    }

    var brightnessSlider by remember(snapshot.brightnessFraction) {
        mutableFloatStateOf(snapshot.brightnessFraction)
    }
    var volumeSlider by remember(snapshot.mediaVolume) {
        mutableFloatStateOf(snapshot.mediaVolume.toFloat())
    }
    var lastNonZeroVolume by remember { mutableIntStateOf(snapshot.mediaVolume.coerceAtLeast(1)) }
    if (snapshot.mediaVolume > 0) {
        lastNonZeroVolume = snapshot.mediaVolume
    }

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    val entryProgress by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = if (isExpressive) {
            motion.expressiveSpec(durationMillis = 230)
        } else {
            motion.spatialSpec(durationMillis = 180)
        },
        label = "gnomeQuickSettingsEntry",
    )
    val clampedProgress = entryProgress.coerceIn(0f, 1f)

    val panelShape = shapes.large
    val panelBackground = if (isExpressive) {
        lerp(colors.surfaceContainerHigh, colors.accentContainer, 0.14f)
    } else {
        colors.surface
    }
    val panelBorder = if (isExpressive) {
        colors.accent.copy(alpha = 0.28f)
    } else {
        colors.hairlineBorder
    }

    val openCd = stringResource(R.string.quick_settings_title)
    BackHandler(onBack = onDismiss)

    val backdropInteraction = remember { MutableInteractionSource() }
    val cardInteraction = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .semantics { contentDescription = openCd }
            .background(Color.Black.copy(alpha = 0.38f * clampedProgress))
            .clickable(
                interactionSource = backdropInteraction,
                indication = null,
                onClick = onDismiss,
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.TopEnd,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 410.dp)
                .fillMaxWidth()
                .graphicsLayer {
                    val scale = if (isExpressive) {
                        0.88f + 0.12f * entryProgress
                    } else {
                        0.94f + 0.06f * entryProgress
                    }
                    scaleX = scale
                    scaleY = scale
                    transformOrigin = TransformOrigin(0.88f, 0f)
                    translationY = (1f - entryProgress) * -18f
                    alpha = clampedProgress
                }
                .shadow(elevation = 18.dp, shape = panelShape, clip = false)
                .clip(panelShape)
                .background(panelBackground)
                .border(1.dp, panelBorder, panelShape)
                .clickable(
                    interactionSource = cardInteraction,
                    indication = null,
                    onClick = {},
                )
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            QuickSettingsHeaderRow(
                batteryLevel = snapshot.batteryLevel,
                isCharging = snapshot.isCharging,
                isDarkTheme = colors.isDark,
                onBatteryClick = {
                    NativeWidgetSystemProviders.openBatterySettings(context)
                    onDismiss()
                },
                onToggleTheme = { onToggleThemeDark(colors.isDark) },
                onOpenLauncherSettings = {
                    onDismiss()
                    onOpenLauncherSettings()
                },
                onOpenSystemSettings = {
                    QuickSettingsController.openSystemSettings(context)
                    onDismiss()
                },
                onClose = onDismiss,
            )

            QuickSettingsSlidersCard(
                brightnessFraction = brightnessSlider,
                canWriteBrightness = snapshot.canWriteSystemSettings,
                onBrightnessChange = { next ->
                    brightnessSlider = next
                    if (snapshot.canWriteSystemSettings) {
                        QuickSettingsController.setScreenBrightness(context, next)
                    }
                },
                onRequestBrightnessPermission = {
                    QuickSettingsController.openWriteSettingsPermission(context)
                },
                volumeValue = volumeSlider,
                maxVolume = snapshot.mediaMaxVolume,
                onVolumeChange = { next ->
                    volumeSlider = next
                    QuickSettingsController.setMediaVolume(context, next.roundToInt())
                },
                onToggleMute = {
                    val target = if (volumeSlider.roundToInt() > 0) 0 else lastNonZeroVolume
                    volumeSlider = target.toFloat()
                    QuickSettingsController.setMediaVolume(context, target)
                },
            )

            QuickSettingsTilesGrid(
                snapshot = snapshot,
                isDarkTheme = colors.isDark,
                onWifiClick = { QuickSettingsController.openWifiPanel(context) },
                onInternetClick = { QuickSettingsController.openInternetPanel(context) },
                onBluetoothClick = { QuickSettingsController.openBluetoothSettings(context) },
                onFlashlightClick = {
                    if (QuickSettingsController.toggleTorch(context, torchCameraId, torchEnabled)) {
                        torchEnabled = !torchEnabled
                    }
                },
                onDndClick = {
                    if (QuickSettingsController.toggleDoNotDisturb(context, snapshot.dndEnabled)) {
                        refreshRevision++
                    }
                },
                onRotateClick = {
                    if (QuickSettingsController.toggleAutoRotate(context, snapshot.autoRotateEnabled)) {
                        refreshRevision++
                    }
                },
                onDarkStyleClick = { onToggleThemeDark(colors.isDark) },
                onAirplaneClick = { QuickSettingsController.openAirplaneSettings(context) },
            )

            val media = snapshot.mediaSnapshot
            if (media.hasNotificationAccess && (media.controller != null || media.title.isNotBlank())) {
                QuickSettingsMediaCard(
                    title = media.title.ifBlank { stringResource(R.string.native_widget_media_untitled) },
                    artist = media.artist,
                    isPlaying = media.isPlaying,
                    artwork = media.artwork,
                    onCardClick = { NativeWidgetSystemProviders.openMediaSessionApp(context, media) },
                    onPrevClick = {
                        media.controller?.transportControls?.skipToPrevious()
                        refreshRevision++
                    },
                    onPlayPauseClick = {
                        val controls = media.controller?.transportControls
                        if (media.isPlaying) {
                            controls?.pause()
                        } else {
                            controls?.play()
                        }
                        refreshRevision++
                    },
                    onNextClick = {
                        media.controller?.transportControls?.skipToNext()
                        refreshRevision++
                    },
                )
            }
        }
    }
}

@Composable
private fun QuickSettingsHeaderRow(
    batteryLevel: Int,
    isCharging: Boolean,
    isDarkTheme: Boolean,
    onBatteryClick: () -> Unit,
    onToggleTheme: () -> Unit,
    onOpenLauncherSettings: () -> Unit,
    onOpenSystemSettings: () -> Unit,
    onClose: () -> Unit,
) {
    val colors = OverviewTheme.colors
    val pillShape = OverviewTheme.shapes.pill
    val pillBg = colors.surfaceElevated

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            modifier = Modifier
                .defaultMinSize(minHeight = 36.dp)
                .clip(pillShape)
                .background(pillBg)
                .border(1.dp, colors.hairlineBorder, pillShape)
                .clickable(role = Role.Button, onClick = onBatteryClick)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            QsBatteryGlyph(
                level = batteryLevel,
                isCharging = isCharging,
                tint = colors.onSurface,
                accent = colors.accent,
                size = 18.dp,
            )
            Text(
                text = if (isCharging) {
                    stringResource(R.string.quick_settings_battery_charging, batteryLevel)
                } else {
                    stringResource(R.string.quick_settings_battery_percent, batteryLevel)
                },
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontFeatureSettings = "tnum",
                ),
                color = colors.onSurface,
                maxLines = 1,
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HeaderCircleButton(
                contentDescription = stringResource(R.string.quick_settings_toggle_theme_cd),
                onClick = onToggleTheme,
            ) {
                QsMoonGlyph(
                    filled = isDarkTheme,
                    tint = if (isDarkTheme) colors.accent else colors.onSurface,
                    size = 17.dp,
                )
            }
            HeaderCircleButton(
                contentDescription = stringResource(R.string.open_settings_content_description),
                onClick = onOpenLauncherSettings,
            ) {
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = null,
                    tint = colors.onSurface,
                    modifier = Modifier.size(18.dp),
                )
            }
            HeaderCircleButton(
                contentDescription = stringResource(R.string.quick_settings_system_settings_cd),
                onClick = onOpenSystemSettings,
            ) {
                QsSlidersGlyph(tint = colors.onSurface, size = 17.dp)
            }
            HeaderCircleButton(
                contentDescription = stringResource(R.string.settings_close_dialog),
                onClick = onClose,
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = null,
                    tint = colors.onSurfaceMuted,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
private fun HeaderCircleButton(
    contentDescription: String,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
) {
    val colors = OverviewTheme.colors
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(colors.surfaceElevated)
            .border(1.dp, colors.hairlineBorder, CircleShape)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        icon()
    }
}

@Composable
private fun QuickSettingsSlidersCard(
    brightnessFraction: Float,
    canWriteBrightness: Boolean,
    onBrightnessChange: (Float) -> Unit,
    onRequestBrightnessPermission: () -> Unit,
    volumeValue: Float,
    maxVolume: Int,
    onVolumeChange: (Float) -> Unit,
    onToggleMute: () -> Unit,
) {
    val colors = OverviewTheme.colors
    val cardShape = OverviewTheme.shapes.medium

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(colors.surfaceContainer)
            .border(1.dp, colors.hairlineBorder, cardShape)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .clickable(
                        role = Role.Button,
                        onClick = if (canWriteBrightness) ({}) else onRequestBrightnessPermission,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                QsSunGlyph(tint = colors.onSurface, size = 18.dp)
            }
            if (canWriteBrightness) {
                Slider(
                    value = brightnessFraction.coerceIn(0.04f, 1f),
                    onValueChange = onBrightnessChange,
                    valueRange = 0.04f..1f,
                    colors = SliderDefaults.colors(
                        thumbColor = colors.accent,
                        activeTrackColor = colors.accent,
                        inactiveTrackColor = colors.surfaceElevated,
                    ),
                    modifier = Modifier.weight(1f),
                )
            } else {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(OverviewTheme.shapes.pill)
                        .background(colors.surfaceElevated)
                        .clickable(role = Role.Button, onClick = onRequestBrightnessPermission)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.quick_settings_brightness),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceMuted,
                    )
                    Text(
                        text = stringResource(R.string.quick_settings_brightness_grant),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = colors.accent,
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            val volumeCd = stringResource(R.string.quick_settings_volume)
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .clickable(role = Role.Button, onClick = onToggleMute)
                    .semantics { contentDescription = volumeCd },
                contentAlignment = Alignment.Center,
            ) {
                QsVolumeGlyph(
                    muted = volumeValue.roundToInt() == 0,
                    tint = colors.onSurface,
                    size = 18.dp,
                )
            }
            Slider(
                value = volumeValue.coerceIn(0f, maxVolume.toFloat()),
                onValueChange = onVolumeChange,
                valueRange = 0f..maxVolume.toFloat(),
                colors = SliderDefaults.colors(
                    thumbColor = colors.accent,
                    activeTrackColor = colors.accent,
                    inactiveTrackColor = colors.surfaceElevated,
                ),
                modifier = Modifier
                    .weight(1f)
                    .semantics { contentDescription = volumeCd },
            )
        }
    }
}

@Composable
private fun QuickSettingsTilesGrid(
    snapshot: QuickSettingsSnapshot,
    isDarkTheme: Boolean,
    onWifiClick: () -> Unit,
    onInternetClick: () -> Unit,
    onBluetoothClick: () -> Unit,
    onFlashlightClick: () -> Unit,
    onDndClick: () -> Unit,
    onRotateClick: () -> Unit,
    onDarkStyleClick: () -> Unit,
    onAirplaneClick: () -> Unit,
) {
    val onLabel = stringResource(R.string.quick_settings_state_on)
    val offLabel = stringResource(R.string.quick_settings_state_off)
    val connectedLabel = stringResource(R.string.quick_settings_state_connected)
    val unavailableLabel = stringResource(R.string.quick_settings_state_unavailable)

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            GnomeToggleTile(
                title = stringResource(R.string.quick_settings_wifi),
                subtitle = when {
                    snapshot.wifiConnected -> connectedLabel
                    snapshot.wifiEnabled -> onLabel
                    else -> offLabel
                },
                active = snapshot.wifiEnabled,
                onClick = onWifiClick,
                icon = { tint -> QsWifiGlyph(tint = tint, size = 18.dp) },
                modifier = Modifier.weight(1f),
            )
            GnomeToggleTile(
                title = stringResource(R.string.quick_settings_internet),
                subtitle = snapshot.networkLabel,
                active = snapshot.isOnline,
                onClick = onInternetClick,
                icon = { tint -> QsGlobeGlyph(tint = tint, size = 18.dp) },
                modifier = Modifier.weight(1f),
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            GnomeToggleTile(
                title = stringResource(R.string.quick_settings_bluetooth),
                subtitle = if (snapshot.bluetoothEnabled) onLabel else offLabel,
                active = snapshot.bluetoothEnabled,
                onClick = onBluetoothClick,
                icon = { tint -> QsBluetoothGlyph(tint = tint, size = 18.dp) },
                modifier = Modifier.weight(1f),
            )
            GnomeToggleTile(
                title = stringResource(R.string.quick_settings_flashlight),
                subtitle = when {
                    !snapshot.torchAvailable -> unavailableLabel
                    snapshot.torchEnabled -> onLabel
                    else -> offLabel
                },
                active = snapshot.torchEnabled,
                enabled = snapshot.torchAvailable,
                onClick = onFlashlightClick,
                icon = { tint -> QsFlashlightGlyph(tint = tint, size = 18.dp) },
                modifier = Modifier.weight(1f),
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            GnomeToggleTile(
                title = stringResource(R.string.quick_settings_dnd),
                subtitle = if (snapshot.dndEnabled) onLabel else offLabel,
                active = snapshot.dndEnabled,
                onClick = onDndClick,
                icon = { tint -> QsDndGlyph(tint = tint, size = 18.dp) },
                modifier = Modifier.weight(1f),
            )
            GnomeToggleTile(
                title = stringResource(R.string.quick_settings_rotate),
                subtitle = if (snapshot.autoRotateEnabled) onLabel else offLabel,
                active = snapshot.autoRotateEnabled,
                onClick = onRotateClick,
                icon = { tint -> QsRotateGlyph(tint = tint, size = 18.dp) },
                modifier = Modifier.weight(1f),
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            GnomeToggleTile(
                title = stringResource(R.string.quick_settings_dark_style),
                subtitle = if (isDarkTheme) onLabel else offLabel,
                active = isDarkTheme,
                onClick = onDarkStyleClick,
                icon = { tint -> QsMoonGlyph(filled = isDarkTheme, tint = tint, size = 18.dp) },
                modifier = Modifier.weight(1f),
            )
            GnomeToggleTile(
                title = stringResource(R.string.quick_settings_airplane),
                subtitle = if (snapshot.airplaneModeEnabled) onLabel else offLabel,
                active = snapshot.airplaneModeEnabled,
                onClick = onAirplaneClick,
                icon = { tint -> QsAirplaneGlyph(tint = tint, size = 18.dp) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun GnomeToggleTile(
    title: String,
    subtitle: String,
    active: Boolean,
    onClick: () -> Unit,
    icon: @Composable (tint: Color) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = OverviewTheme.colors
    val motion = OverviewTheme.motion
    val isExpressive = OverviewTheme.components.isExpressive
    val tileShape = OverviewTheme.shapes.pill

    val targetFill = when {
        !enabled -> colors.surfaceContainer.copy(alpha = 0.55f)
        active && isExpressive -> colors.accentContainer
        active -> colors.accent
        else -> colors.surfaceElevated
    }
    val targetContent = when {
        !enabled -> colors.onSurfaceMuted.copy(alpha = 0.55f)
        active && isExpressive -> colors.onAccentContainer
        active -> colors.onAccent
        else -> colors.onSurface
    }
    val targetSubtitle = when {
        !enabled -> colors.onSurfaceMuted.copy(alpha = 0.55f)
        active && isExpressive -> colors.onAccentContainer.copy(alpha = 0.78f)
        active -> colors.onAccent.copy(alpha = 0.85f)
        else -> colors.onSurfaceMuted
    }
    val borderColor = if (active && isExpressive) {
        colors.accent.copy(alpha = 0.45f)
    } else if (active) {
        colors.accent
    } else {
        colors.hairlineBorder
    }

    val fillColor by animateColorAsState(
        targetValue = targetFill,
        animationSpec = motion.effectSpec(durationMillis = 160),
        label = "qsTileFill",
    )

    Row(
        modifier = modifier
            .defaultMinSize(minHeight = 56.dp)
            .clip(tileShape)
            .background(fillColor)
            .border(1.dp, borderColor, tileShape)
            .clickable(enabled = enabled, role = Role.Switch, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        icon(targetContent)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                color = targetContent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = targetSubtitle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun QuickSettingsMediaCard(
    title: String,
    artist: String,
    isPlaying: Boolean,
    artwork: androidx.compose.ui.graphics.ImageBitmap?,
    onCardClick: () -> Unit,
    onPrevClick: () -> Unit,
    onPlayPauseClick: () -> Unit,
    onNextClick: () -> Unit,
) {
    val colors = OverviewTheme.colors
    val cardShape = OverviewTheme.shapes.medium

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(colors.surfaceContainer)
            .border(1.dp, colors.hairlineBorder, cardShape)
            .clickable(role = Role.Button, onClick = onCardClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (artwork != null) {
            Image(
                bitmap = artwork,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(40.dp)
                    .clip(OverviewTheme.shapes.small),
            )
        } else {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(OverviewTheme.shapes.small)
                    .background(colors.accentContainer),
                contentAlignment = Alignment.Center,
            ) {
                QsMusicNoteGlyph(tint = colors.onAccentContainer, size = 18.dp)
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                color = colors.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (artist.isNotBlank()) {
                Text(
                    text = artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onPrevClick, modifier = Modifier.size(34.dp)) {
                SkipTrackGlyph(forward = false, tint = colors.onSurface, size = 16.dp)
            }
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(colors.accentContainer)
                    .clickable(role = Role.Button, onClick = onPlayPauseClick),
                contentAlignment = Alignment.Center,
            ) {
                PlayPauseGlyph(isPlaying = isPlaying, tint = colors.onAccentContainer, size = 17.dp)
            }
            IconButton(onClick = onNextClick, modifier = Modifier.size(34.dp)) {
                SkipTrackGlyph(forward = true, tint = colors.onSurface, size = 16.dp)
            }
        }
    }
}

// region Ícones simbólicos estilo GNOME desenhados via Canvas

@Composable
private fun QsBatteryGlyph(
    level: Int,
    isCharging: Boolean,
    tint: Color,
    accent: Color,
    size: Dp,
) {
    Canvas(modifier = Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = (h * 0.09f).coerceAtLeast(1.5f)
        drawRoundRect(
            color = tint,
            topLeft = Offset(w * 0.08f, h * 0.24f),
            size = Size(w * 0.74f, h * 0.52f),
            cornerRadius = CornerRadius(w * 0.10f, w * 0.10f),
            style = Stroke(width = stroke),
        )
        drawRoundRect(
            color = tint,
            topLeft = Offset(w * 0.85f, h * 0.39f),
            size = Size(w * 0.08f, h * 0.22f),
            cornerRadius = CornerRadius(w * 0.04f, w * 0.04f),
        )
        val fillFrac = (level / 100f).coerceIn(0.06f, 1f)
        val fillColor = when {
            isCharging -> accent
            level <= 15 -> Color(0xFFE01B24)
            else -> tint
        }
        drawRoundRect(
            color = fillColor,
            topLeft = Offset(w * 0.15f, h * 0.32f),
            size = Size(w * 0.60f * fillFrac, h * 0.36f),
            cornerRadius = CornerRadius(w * 0.06f, w * 0.06f),
        )
    }
}

@Composable
private fun QsSlidersGlyph(tint: Color, size: Dp) {
    Canvas(modifier = Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = (h * 0.10f).coerceAtLeast(1.6f)
        drawLine(tint, Offset(w * 0.14f, h * 0.32f), Offset(w * 0.86f, h * 0.32f), stroke, StrokeCap.Round)
        drawCircle(tint, radius = h * 0.13f, center = Offset(w * 0.35f, h * 0.32f))
        drawLine(tint, Offset(w * 0.14f, h * 0.68f), Offset(w * 0.86f, h * 0.68f), stroke, StrokeCap.Round)
        drawCircle(tint, radius = h * 0.13f, center = Offset(w * 0.65f, h * 0.68f))
    }
}

@Composable
private fun QsSunGlyph(tint: Color, size: Dp) {
    Canvas(modifier = Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = (w * 0.09f).coerceAtLeast(1.6f)
        val c = Offset(w * 0.5f, h * 0.5f)
        drawCircle(tint, radius = w * 0.21f, center = c, style = Stroke(width = stroke))
        for (i in 0 until 8) {
            val rad = Math.toRadians(i * 45.0)
            drawLine(
                color = tint,
                start = Offset(c.x + (cos(rad) * w * 0.31f).toFloat(), c.y + (sin(rad) * h * 0.31f).toFloat()),
                end = Offset(c.x + (cos(rad) * w * 0.43f).toFloat(), c.y + (sin(rad) * h * 0.43f).toFloat()),
                strokeWidth = stroke,
                cap = StrokeCap.Round,
            )
        }
    }
}

@Composable
private fun QsVolumeGlyph(muted: Boolean, tint: Color, size: Dp) {
    Canvas(modifier = Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = (w * 0.09f).coerceAtLeast(1.6f)
        val speaker = Path().apply {
            moveTo(w * 0.16f, h * 0.38f)
            lineTo(w * 0.34f, h * 0.38f)
            lineTo(w * 0.54f, h * 0.20f)
            lineTo(w * 0.54f, h * 0.80f)
            lineTo(w * 0.34f, h * 0.62f)
            lineTo(w * 0.16f, h * 0.62f)
            close()
        }
        drawPath(speaker, color = tint)
        if (muted) {
            drawLine(tint, Offset(w * 0.66f, h * 0.36f), Offset(w * 0.86f, h * 0.64f), stroke, StrokeCap.Round)
            drawLine(tint, Offset(w * 0.86f, h * 0.36f), Offset(w * 0.66f, h * 0.64f), stroke, StrokeCap.Round)
        } else {
            drawArc(
                color = tint,
                startAngle = -45f,
                sweepAngle = 90f,
                useCenter = false,
                topLeft = Offset(w * 0.46f, h * 0.30f),
                size = Size(w * 0.26f, h * 0.40f),
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
            drawArc(
                color = tint,
                startAngle = -50f,
                sweepAngle = 100f,
                useCenter = false,
                topLeft = Offset(w * 0.54f, h * 0.18f),
                size = Size(w * 0.34f, h * 0.64f),
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
        }
    }
}

@Composable
private fun QsWifiGlyph(tint: Color, size: Dp) {
    Canvas(modifier = Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = (w * 0.10f).coerceAtLeast(1.7f)
        drawArc(
            color = tint,
            startAngle = 225f,
            sweepAngle = 90f,
            useCenter = false,
            topLeft = Offset(w * 0.12f, h * 0.22f),
            size = Size(w * 0.76f, h * 0.76f),
            style = Stroke(width = stroke, cap = StrokeCap.Round),
        )
        drawArc(
            color = tint,
            startAngle = 225f,
            sweepAngle = 90f,
            useCenter = false,
            topLeft = Offset(w * 0.27f, h * 0.39f),
            size = Size(w * 0.46f, h * 0.46f),
            style = Stroke(width = stroke, cap = StrokeCap.Round),
        )
        drawCircle(tint, radius = w * 0.08f, center = Offset(w * 0.50f, h * 0.74f))
    }
}

@Composable
private fun QsGlobeGlyph(tint: Color, size: Dp) {
    Canvas(modifier = Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = (w * 0.09f).coerceAtLeast(1.6f)
        drawCircle(tint, radius = w * 0.36f, center = Offset(w * 0.5f, h * 0.5f), style = Stroke(width = stroke))
        drawOval(
            color = tint,
            topLeft = Offset(w * 0.33f, h * 0.14f),
            size = Size(w * 0.34f, h * 0.72f),
            style = Stroke(width = stroke),
        )
        drawLine(tint, Offset(w * 0.16f, h * 0.5f), Offset(w * 0.84f, h * 0.5f), stroke)
    }
}

@Composable
private fun QsBluetoothGlyph(tint: Color, size: Dp) {
    Canvas(modifier = Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = (w * 0.10f).coerceAtLeast(1.6f)
        val path = Path().apply {
            moveTo(w * 0.32f, h * 0.32f)
            lineTo(w * 0.70f, h * 0.68f)
            lineTo(w * 0.50f, h * 0.86f)
            lineTo(w * 0.50f, h * 0.14f)
            lineTo(w * 0.70f, h * 0.32f)
            lineTo(w * 0.32f, h * 0.68f)
        }
        drawPath(
            path = path,
            color = tint,
            style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
    }
}

@Composable
private fun QsFlashlightGlyph(tint: Color, size: Dp) {
    Canvas(modifier = Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = (w * 0.09f).coerceAtLeast(1.6f)
        val path = Path().apply {
            moveTo(w * 0.28f, h * 0.22f)
            lineTo(w * 0.72f, h * 0.22f)
            lineTo(w * 0.60f, h * 0.42f)
            lineTo(w * 0.60f, h * 0.84f)
            lineTo(w * 0.40f, h * 0.84f)
            lineTo(w * 0.40f, h * 0.42f)
            close()
        }
        drawPath(path, color = tint, style = Stroke(width = stroke, join = StrokeJoin.Round))
        drawCircle(tint, radius = w * 0.06f, center = Offset(w * 0.50f, h * 0.58f))
    }
}

@Composable
private fun QsDndGlyph(tint: Color, size: Dp) {
    Canvas(modifier = Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = (w * 0.10f).coerceAtLeast(1.7f)
        drawCircle(tint, radius = w * 0.35f, center = Offset(w * 0.5f, h * 0.5f), style = Stroke(width = stroke))
        drawLine(tint, Offset(w * 0.30f, h * 0.5f), Offset(w * 0.70f, h * 0.5f), stroke, StrokeCap.Round)
    }
}

@Composable
private fun QsRotateGlyph(tint: Color, size: Dp) {
    Canvas(modifier = Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = (w * 0.10f).coerceAtLeast(1.6f)
        drawArc(
            color = tint,
            startAngle = 35f,
            sweepAngle = 270f,
            useCenter = false,
            topLeft = Offset(w * 0.18f, h * 0.18f),
            size = Size(w * 0.64f, h * 0.64f),
            style = Stroke(width = stroke, cap = StrokeCap.Round),
        )
        drawLine(tint, Offset(w * 0.72f, h * 0.18f), Offset(w * 0.80f, h * 0.38f), stroke, StrokeCap.Round)
        drawLine(tint, Offset(w * 0.80f, h * 0.38f), Offset(w * 0.60f, h * 0.36f), stroke, StrokeCap.Round)
    }
}

@Composable
private fun QsMoonGlyph(filled: Boolean, tint: Color, size: Dp) {
    Canvas(modifier = Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = (w * 0.09f).coerceAtLeast(1.6f)
        val c = Offset(w * 0.48f, h * 0.50f)
        if (filled) {
            drawCircle(tint, radius = w * 0.32f, center = c)
            drawCircle(
                color = Color.Black.copy(alpha = 0.35f),
                radius = w * 0.22f,
                center = Offset(w * 0.62f, h * 0.38f),
            )
        } else {
            drawArc(
                color = tint,
                startAngle = 60f,
                sweepAngle = 240f,
                useCenter = false,
                topLeft = Offset(w * 0.18f, h * 0.18f),
                size = Size(w * 0.64f, h * 0.64f),
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
        }
    }
}

@Composable
private fun QsAirplaneGlyph(tint: Color, size: Dp) {
    Canvas(modifier = Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = (w * 0.10f).coerceAtLeast(1.7f)
        drawLine(tint, Offset(w * 0.50f, h * 0.16f), Offset(w * 0.50f, h * 0.84f), stroke, StrokeCap.Round)
        drawLine(tint, Offset(w * 0.18f, h * 0.48f), Offset(w * 0.82f, h * 0.48f), stroke, StrokeCap.Round)
        drawLine(tint, Offset(w * 0.36f, h * 0.78f), Offset(w * 0.64f, h * 0.78f), stroke, StrokeCap.Round)
    }
}

@Composable
private fun QsMusicNoteGlyph(tint: Color, size: Dp) {
    Canvas(modifier = Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = (w * 0.10f).coerceAtLeast(1.6f)
        drawCircle(tint, radius = w * 0.14f, center = Offset(w * 0.36f, h * 0.70f))
        drawLine(tint, Offset(w * 0.48f, h * 0.70f), Offset(w * 0.48f, h * 0.22f), stroke, StrokeCap.Round)
        drawLine(tint, Offset(w * 0.48f, h * 0.22f), Offset(w * 0.76f, h * 0.30f), stroke, StrokeCap.Round)
    }
}

// endregion
