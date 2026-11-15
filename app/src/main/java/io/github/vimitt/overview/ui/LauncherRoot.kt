package io.github.vimitt.overview.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.vimitt.overview.R
import io.github.vimitt.overview.domain.model.AppInfo
import io.github.vimitt.overview.domain.model.ThemeMode
import io.github.vimitt.overview.ui.home.HomeScreen
import io.github.vimitt.overview.ui.home.HomeUiState
import io.github.vimitt.overview.ui.overview.OverviewScreen
import io.github.vimitt.overview.ui.overview.OverviewUiState
import io.github.vimitt.overview.ui.settings.SettingsScreen
import io.github.vimitt.overview.ui.settings.SettingsUiState
import io.github.vimitt.overview.ui.theme.OverviewCornerRadius
import io.github.vimitt.overview.ui.theme.OverviewTheme
import io.github.vimitt.overview.ui.theme.overviewSpring
import io.github.vimitt.overview.ui.theme.overviewTween
import kotlin.math.roundToInt

/**
 * Raiz visual do launcher que coordena a transição entre Home, Overview,
 * menu contextual de toque longo e tela de personalização.
 */
@Composable
fun LauncherRoot(
    isOverviewOpen: Boolean,
    isHomeMenuOpen: Boolean,
    isSettingsOpen: Boolean,
    blurRadiusDp: Float,
    homeUiState: HomeUiState,
    searchQuery: String,
    overviewUiState: OverviewUiState,
    settingsUiState: SettingsUiState,
    onOpenOverview: () -> Unit,
    onCloseOverview: () -> Unit,
    onHomeLongPress: () -> Unit,
    onDismissHomeMenu: () -> Unit,
    onOpenSettings: () -> Unit,
    onCloseSettings: () -> Unit,
    onOpenWallpaperPicker: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onClearSearch: () -> Unit,
    onLaunchFromHome: (
        app: AppInfo,
        sourceView: android.view.View,
        sourceBounds: android.graphics.Rect?,
        reducedMotion: Boolean,
    ) -> Unit,
    onLaunchFromOverview: (
        app: AppInfo,
        sourceView: android.view.View,
        sourceBounds: android.graphics.Rect?,
        reducedMotion: Boolean,
    ) -> Unit,
    onThemeModeChange: (ThemeMode) -> Unit,
    onAccentColorChange: (Long) -> Unit,
    onBlurRadiusChange: (Float) -> Unit,
    onShowClockChange: (Boolean) -> Unit,
    onToggleDashApp: (AppInfo) -> Unit,
    onResetDashToAuto: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val reducedMotion = OverviewTheme.reducedMotion
    val density = LocalDensity.current
    val context = LocalContext.current

    val blurActive = isOverviewOpen && !isSettingsOpen
    val blurProgress by animateFloatAsState(
        targetValue = if (blurActive) 1f else 0f,
        animationSpec = overviewTween(reducedMotion, durationMillis = 220),
        label = "overviewBlurProgress",
    )

    val homeBlurPx = with(density) { (blurRadiusDp * blurProgress).dp.toPx() }
    val windowBlurPx = with(density) {
        if (blurActive) blurRadiusDp.dp.toPx().roundToInt().coerceAtMost(48) else 0
    }

    WindowBackgroundBlurEffect(
        context = context,
        blurRadiusPx = windowBlurPx,
    )

    Box(modifier = modifier.fillMaxSize()) {
        HomeScreen(
            uiState = homeUiState,
            onAppClick = { app, view, bounds ->
                onLaunchFromHome(app, view, bounds, reducedMotion)
            },
            onOpenOverview = onOpenOverview,
            onLongPress = onHomeLongPress,
            modifier = Modifier.graphicsLayer {
                renderEffect = if (homeBlurPx > 0.5f) {
                    BlurEffect(
                        radiusX = homeBlurPx,
                        radiusY = homeBlurPx,
                        edgeTreatment = TileMode.Clamp,
                    )
                } else {
                    null
                }
            },
        )

        val enterTransition = if (reducedMotion) {
            fadeIn(overviewTween(reducedMotion = true))
        } else {
            fadeIn(overviewTween(reducedMotion = false, durationMillis = 200)) +
                slideInVertically(
                    animationSpec = overviewSpring(reducedMotion = false),
                    initialOffsetY = { fullHeight -> fullHeight / 10 },
                )
        }

        val exitTransition = if (reducedMotion) {
            fadeOut(overviewTween(reducedMotion = true))
        } else {
            fadeOut(overviewTween(reducedMotion = false, durationMillis = 180)) +
                slideOutVertically(
                    animationSpec = overviewSpring(reducedMotion = false),
                    targetOffsetY = { fullHeight -> fullHeight / 12 },
                )
        }

        AnimatedVisibility(
            visible = isOverviewOpen && !isSettingsOpen,
            enter = enterTransition,
            exit = exitTransition,
        ) {
            OverviewScreen(
                searchQuery = searchQuery,
                uiState = overviewUiState,
                onSearchQueryChange = onSearchQueryChange,
                onClearSearch = onClearSearch,
                onAppClick = { app, view, bounds ->
                    onLaunchFromOverview(app, view, bounds, reducedMotion)
                },
                onClose = onCloseOverview,
                onOpenSettings = onOpenSettings,
            )
        }

        AnimatedVisibility(
            visible = isHomeMenuOpen && !isOverviewOpen && !isSettingsOpen,
            enter = fadeIn(overviewTween(reducedMotion, durationMillis = 160)),
            exit = fadeOut(overviewTween(reducedMotion, durationMillis = 140)),
        ) {
            HomeContextMenuOverlay(
                onCustomize = {
                    onDismissHomeMenu()
                    onOpenSettings()
                },
                onChangeWallpaper = {
                    onDismissHomeMenu()
                    onOpenWallpaperPicker()
                },
                onDismiss = onDismissHomeMenu,
            )
        }

        AnimatedVisibility(
            visible = isSettingsOpen,
            enter = enterTransition,
            exit = exitTransition,
        ) {
            SettingsScreen(
                uiState = settingsUiState,
                onThemeModeChange = onThemeModeChange,
                onAccentColorChange = onAccentColorChange,
                onBlurRadiusChange = onBlurRadiusChange,
                onShowClockChange = onShowClockChange,
                onToggleDashApp = onToggleDashApp,
                onResetDashToAuto = onResetDashToAuto,
                onOpenWallpaperPicker = onOpenWallpaperPicker,
                onClose = onCloseSettings,
            )
        }
    }
}

@Composable
private fun HomeContextMenuOverlay(
    onCustomize: () -> Unit,
    onChangeWallpaper: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = OverviewTheme.colors
    val cardShape = RoundedCornerShape(OverviewCornerRadius.Large)
    val backdropInteraction = remember { MutableInteractionSource() }
    val cardInteraction = remember { MutableInteractionSource() }

    BackHandler(onBack = onDismiss)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.36f))
            .clickable(
                interactionSource = backdropInteraction,
                indication = null,
                onClick = onDismiss,
            )
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 24.dp),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 420.dp)
                .fillMaxWidth()
                .clip(cardShape)
                .background(colors.surface)
                .border(width = 1.dp, color = colors.hairlineBorder, shape = cardShape)
                .clickable(
                    interactionSource = cardInteraction,
                    indication = null,
                    onClick = {},
                ),
        ) {
            Text(
                text = stringResource(R.string.home_menu_title),
                style = MaterialTheme.typography.labelMedium,
                color = colors.onSurfaceMuted,
                modifier = Modifier.padding(start = 20.dp, top = 16.dp, bottom = 8.dp),
            )

            HomeMenuRow(
                icon = Icons.Filled.Settings,
                label = stringResource(R.string.home_menu_customize),
                onClick = onCustomize,
            )

            HorizontalDivider(thickness = 1.dp, color = colors.hairlineBorder)

            HomeMenuRow(
                icon = Icons.Filled.Edit,
                label = stringResource(R.string.home_menu_wallpaper),
                onClick = onChangeWallpaper,
            )
        }
    }
}

@Composable
private fun HomeMenuRow(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    val colors = OverviewTheme.colors

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = colors.accent,
            modifier = Modifier.size(22.dp),
        )
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            color = colors.onSurface,
        )
    }
}

@Composable
private fun WindowBackgroundBlurEffect(
    context: Context,
    blurRadiusPx: Int,
) {
    val activity = context.findActivity() ?: return

    LaunchedEffect(activity, blurRadiusPx) {
        val windowManager = activity.getSystemService(WindowManager::class.java)
        val blurSupported = windowManager?.isCrossWindowBlurEnabled == true
        activity.window.setBackgroundBlurRadius(if (blurSupported) blurRadiusPx else 0)
    }

    DisposableEffect(activity) {
        onDispose {
            activity.window.setBackgroundBlurRadius(0)
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
