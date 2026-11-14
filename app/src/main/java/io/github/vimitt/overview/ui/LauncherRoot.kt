package io.github.vimitt.overview.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import io.github.vimitt.overview.ui.home.HomeScreen
import io.github.vimitt.overview.ui.home.HomeUiState
import io.github.vimitt.overview.ui.overview.OverviewScreen
import io.github.vimitt.overview.ui.overview.OverviewUiState
import io.github.vimitt.overview.ui.theme.OverviewTheme
import io.github.vimitt.overview.ui.theme.overviewSpring
import io.github.vimitt.overview.ui.theme.overviewTween
import kotlin.math.roundToInt

/**
 * Raiz visual do launcher que coordena a transição entre a Home e o Overview,
 * aplicando blur com `RenderEffect` (camada Home) e blur de janela sobre o wallpaper.
 */
@Composable
fun LauncherRoot(
    isOverviewOpen: Boolean,
    blurRadiusDp: Float,
    homeUiState: HomeUiState,
    overviewUiState: OverviewUiState,
    onOpenOverview: () -> Unit,
    onCloseOverview: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onClearSearch: () -> Unit,
    onLaunchFromHome: (
        app: io.github.vimitt.overview.domain.model.AppInfo,
        sourceView: android.view.View,
        sourceBounds: android.graphics.Rect?,
        reducedMotion: Boolean,
    ) -> Unit,
    onLaunchFromOverview: (
        app: io.github.vimitt.overview.domain.model.AppInfo,
        sourceView: android.view.View,
        sourceBounds: android.graphics.Rect?,
        reducedMotion: Boolean,
    ) -> Unit,
    modifier: Modifier = Modifier,
) {
    val reducedMotion = OverviewTheme.reducedMotion
    val density = LocalDensity.current
    val context = LocalContext.current

    val blurProgress by animateFloatAsState(
        targetValue = if (isOverviewOpen) 1f else 0f,
        animationSpec = overviewTween(reducedMotion, durationMillis = 220),
        label = "overviewBlurProgress",
    )

    val homeBlurPx = with(density) { (blurRadiusDp * blurProgress).dp.toPx() }
    val windowBlurPx = with(density) {
        if (isOverviewOpen) blurRadiusDp.dp.toPx().roundToInt() else 0
    }

    // Aplica blur real atrás da janela transparente (Android 12+) quando suportado pelo dispositivo.
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
            visible = isOverviewOpen,
            enter = enterTransition,
            exit = exitTransition,
        ) {
            OverviewScreen(
                uiState = overviewUiState,
                onSearchQueryChange = onSearchQueryChange,
                onClearSearch = onClearSearch,
                onAppClick = { app, view, bounds ->
                    onLaunchFromOverview(app, view, bounds, reducedMotion)
                },
                onClose = onCloseOverview,
            )
        }
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
