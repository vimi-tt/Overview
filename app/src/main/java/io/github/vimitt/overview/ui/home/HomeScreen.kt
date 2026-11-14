package io.github.vimitt.overview.ui.home

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Rect
import android.text.format.DateFormat
import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.vimitt.overview.R
import io.github.vimitt.overview.domain.model.AppInfo
import io.github.vimitt.overview.ui.components.DashBar
import io.github.vimitt.overview.ui.theme.OverviewCornerRadius
import io.github.vimitt.overview.ui.theme.OverviewTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Tela inicial transparente sobre o papel de parede do sistema,
 * com relógio opcional e o Dash de favoritos fixo na parte inferior.
 */
@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onAppClick: (app: AppInfo, sourceView: View, sourceBounds: Rect?) -> Unit,
    onOpenOverview: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val swipeThresholdPx = with(LocalDensity.current) { 56.dp.toPx() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(swipeThresholdPx) {
                var totalDragY = 0f
                var triggered = false
                detectVerticalDragGestures(
                    onDragStart = {
                        totalDragY = 0f
                        triggered = false
                    },
                    onVerticalDrag = { _, dragAmount ->
                        totalDragY += dragAmount
                        if (!triggered && totalDragY <= -swipeThresholdPx) {
                            triggered = true
                            onOpenOverview()
                        }
                    },
                )
            }
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        if (uiState.showClock) {
            HomeClock(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 48.dp),
            )
        }

        Column(
            modifier = Modifier.align(Alignment.BottomCenter),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Bottom,
        ) {
            OverviewHandlePill(onClick = onOpenOverview)
            Spacer(modifier = Modifier.height(12.dp))
            DashBar(
                apps = uiState.dashApps,
                onAppClick = onAppClick,
            )
        }
    }
}

@Composable
private fun HomeClock(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val colors = OverviewTheme.colors
    var currentTimeMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }

    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                currentTimeMillis = System.currentTimeMillis()
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_TIME_TICK)
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
        }
        context.registerReceiver(receiver, filter)
        onDispose {
            context.unregisterReceiver(receiver)
        }
    }

    val now = remember(currentTimeMillis) { Date(currentTimeMillis) }
    val is24Hour = DateFormat.is24HourFormat(context)
    val timeText = remember(now, is24Hour) {
        val pattern = if (is24Hour) "HH:mm" else "h:mm"
        SimpleDateFormat(pattern, Locale.getDefault()).format(now)
    }
    val dateText = remember(now) {
        val pattern = DateFormat.getBestDateTimePattern(Locale.getDefault(), "EEEEdMMMM")
        SimpleDateFormat(pattern, Locale.getDefault()).format(now)
    }

    val textShadow = Shadow(
        color = colors.wallpaperShadow,
        offset = Offset(0f, 2f),
        blurRadius = 10f,
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = timeText,
            style = MaterialTheme.typography.displayLarge.copy(shadow = textShadow),
            color = colors.wallpaperForeground,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = dateText,
            style = MaterialTheme.typography.titleMedium.copy(shadow = textShadow),
            color = colors.wallpaperForeground.copy(alpha = 0.92f),
        )
    }
}

@Composable
private fun OverviewHandlePill(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = OverviewTheme.colors
    val label = stringResource(R.string.open_overview_content_description)
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(OverviewCornerRadius.Pill))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            )
            .semantics { contentDescription = label }
            .padding(horizontal = 24.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(width = 40.dp, height = 4.dp)
                .clip(RoundedCornerShape(OverviewCornerRadius.Pill))
                .background(colors.wallpaperForeground.copy(alpha = 0.72f)),
        )
    }
}
