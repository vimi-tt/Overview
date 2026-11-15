package io.github.vimitt.overview.ui.home

import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.SizeF
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import io.github.vimitt.overview.R
import io.github.vimitt.overview.data.AppWidgetController
import io.github.vimitt.overview.ui.theme.OverviewCornerRadius
import io.github.vimitt.overview.ui.theme.OverviewTheme
import kotlin.math.abs

/**
 * Renderiza um `AppWidgetHostView` dentro da grade da tela inicial,
 * preservando interações internas do widget e permitindo toque longo
 * para redimensionar, mover ou remover.
 */
@Composable
fun HomeWidgetView(
    widgetItem: HomeWorkspaceItem.Widget,
    appWidgetController: AppWidgetController,
    widthDp: Dp,
    heightDp: Dp,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val providerInfo = widgetItem.providerInfo
    if (providerInfo == null) {
        UnavailableWidgetCard(
            onLongPress = onLongPress,
            modifier = modifier,
        )
        return
    }

    val context = LocalContext.current
    val hostView = remember(widgetItem.item.appWidgetId, providerInfo) {
        appWidgetController.createHostView(
            activityContext = context,
            appWidgetId = widgetItem.item.appWidgetId,
            providerInfo = providerInfo,
        )
    }

    AndroidView(
        factory = { viewContext ->
            WidgetHostContainer(viewContext).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
                setHostView(hostView)
                onLongPressListener = onLongPress
            }
        },
        update = { container ->
            container.onLongPressListener = onLongPress
            container.setHostView(hostView)
            updateWidgetDimensions(hostView, widthDp.value, heightDp.value)
        },
        modifier = modifier.clip(RoundedCornerShape(OverviewCornerRadius.Large)),
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun UnavailableWidgetCard(
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = OverviewTheme.colors
    val shape = RoundedCornerShape(OverviewCornerRadius.Medium)

    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(shape)
            .background(colors.dashBackground)
            .border(1.dp, colors.hairlineBorder, shape)
            .combinedClickable(
                onClick = onLongPress,
                onLongClick = onLongPress,
            )
            .padding(12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.home_widget_unavailable),
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurfaceMuted,
            textAlign = TextAlign.Center,
        )
    }
}

private fun updateWidgetDimensions(
    hostView: AppWidgetHostView,
    widthDp: Float,
    heightDp: Float,
) {
    val safeW = widthDp.coerceAtLeast(48f)
    val safeH = heightDp.coerceAtLeast(48f)
    hostView.updateAppWidgetSize(
        Bundle(),
        listOf(SizeF(safeW, safeH)),
    )
}

/**
 * Contêiner leve que detecta toque longo sem interceptar toques normais nem
 * rolagem interna do widget.
 */
private class WidgetHostContainer(context: Context) : FrameLayout(context) {

    var onLongPressListener: (() -> Unit)? = null

    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private val longPressTimeout = ViewConfiguration.getLongPressTimeout().toLong()
    private val handler = Handler(Looper.getMainLooper())

    private var downX = 0f
    private var downY = 0f
    private var longPressTriggered = false

    private val longPressRunnable = Runnable {
        longPressTriggered = true
        performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        cancelChildTouch()
        onLongPressListener?.invoke()
    }

    fun setHostView(view: AppWidgetHostView) {
        if (childCount == 1 && getChildAt(0) === view) return
        (view.parent as? ViewGroup)?.removeView(view)
        removeAllViews()
        addView(
            view,
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT),
        )
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = ev.x
                downY = ev.y
                longPressTriggered = false
                handler.removeCallbacks(longPressRunnable)
                handler.postDelayed(longPressRunnable, longPressTimeout)
            }

            MotionEvent.ACTION_MOVE -> {
                if (abs(ev.x - downX) > touchSlop || abs(ev.y - downY) > touchSlop) {
                    handler.removeCallbacks(longPressRunnable)
                }
            }

            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL,
            -> {
                handler.removeCallbacks(longPressRunnable)
            }
        }
        return longPressTriggered
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_UP || event.actionMasked == MotionEvent.ACTION_CANCEL) {
            handler.removeCallbacks(longPressRunnable)
            longPressTriggered = false
        }
        return longPressTriggered || super.onTouchEvent(event)
    }

    override fun onDetachedFromWindow() {
        handler.removeCallbacks(longPressRunnable)
        super.onDetachedFromWindow()
    }

    private fun cancelChildTouch() {
        val now = SystemClock.uptimeMillis()
        val cancelEvent = MotionEvent.obtain(now, now, MotionEvent.ACTION_CANCEL, 0f, 0f, 0)
        for (i in 0 until childCount) {
            getChildAt(i).dispatchTouchEvent(cancelEvent)
        }
        cancelEvent.recycle()
    }
}
