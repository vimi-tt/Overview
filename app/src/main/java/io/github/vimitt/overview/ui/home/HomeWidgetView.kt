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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import io.github.vimitt.overview.R
import io.github.vimitt.overview.data.AppWidgetController
import io.github.vimitt.overview.ui.theme.OverviewTheme
import kotlin.math.abs

private class WidgetCoordinatesHolder {
    var coordinates: LayoutCoordinates? = null
    var composeDragActive: Boolean = false
    var viewDragActive: Boolean = false
}

/**
 * Renderiza um `AppWidgetHostView` dentro da grade da tela inicial,
 * preservando interações internas do widget e permitindo toque longo
 * com arraste direto ou abertura do menu contextual.
 */
@Composable
fun HomeWidgetView(
    widgetItem: HomeWorkspaceItem.Widget,
    appWidgetController: AppWidgetController,
    widthDp: Dp,
    heightDp: Dp,
    onDragStart: (Offset) -> Unit,
    onDragMove: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val providerInfo = widgetItem.providerInfo
    if (providerInfo == null) {
        UnavailableWidgetCard(
            itemId = widgetItem.item.id,
            onDragStart = onDragStart,
            onDragMove = onDragMove,
            onDragEnd = onDragEnd,
            onDragCancel = onDragCancel,
            modifier = modifier,
        )
        return
    }

    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val coordinatesHolder = remember { WidgetCoordinatesHolder() }
    val currentOnDragStart by rememberUpdatedState(onDragStart)
    val currentOnDragMove by rememberUpdatedState(onDragMove)
    val currentOnDragEnd by rememberUpdatedState(onDragEnd)
    val currentOnDragCancel by rememberUpdatedState(onDragCancel)

    val hostView = remember(widgetItem.item.appWidgetId, providerInfo) {
        appWidgetController.createHostView(
            activityContext = context,
            appWidgetId = widgetItem.item.appWidgetId,
            providerInfo = providerInfo,
        )
    }

    Box(
        modifier = modifier
            .onPlaced { coordinates ->
                coordinatesHolder.coordinates = coordinates
            }
            .pointerInput(widgetItem.item.id) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { localOffset ->
                        if (coordinatesHolder.viewDragActive) return@detectDragGesturesAfterLongPress
                        coordinatesHolder.composeDragActive = true
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        val coords = coordinatesHolder.coordinates
                        val windowPos = if (coords != null && coords.isAttached) {
                            coords.localToWindow(localOffset)
                        } else {
                            localOffset
                        }
                        currentOnDragStart(windowPos)
                    },
                    onDrag = { change, _ ->
                        if (!coordinatesHolder.composeDragActive) return@detectDragGesturesAfterLongPress
                        change.consume()
                        val coords = coordinatesHolder.coordinates
                        val windowPos = if (coords != null && coords.isAttached) {
                            coords.localToWindow(change.position)
                        } else {
                            change.position
                        }
                        currentOnDragMove(windowPos)
                    },
                    onDragEnd = {
                        if (coordinatesHolder.composeDragActive) {
                            coordinatesHolder.composeDragActive = false
                            currentOnDragEnd()
                        }
                    },
                    onDragCancel = {
                        if (coordinatesHolder.composeDragActive) {
                            coordinatesHolder.composeDragActive = false
                            currentOnDragCancel()
                        }
                    },
                )
            },
    ) {
        AndroidView(
            factory = { viewContext ->
                WidgetHostContainer(viewContext).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )
                    setHostView(hostView)
                    onDragStartListener = {
                        if (!coordinatesHolder.composeDragActive) {
                            coordinatesHolder.viewDragActive = true
                            currentOnDragStart(it)
                        }
                    }
                    onDragMoveListener = {
                        if (coordinatesHolder.viewDragActive) currentOnDragMove(it)
                    }
                    onDragEndListener = {
                        if (coordinatesHolder.viewDragActive) {
                            coordinatesHolder.viewDragActive = false
                            currentOnDragEnd()
                        }
                    }
                    onDragCancelListener = {
                        if (coordinatesHolder.viewDragActive) {
                            coordinatesHolder.viewDragActive = false
                            currentOnDragCancel()
                        }
                    }
                }
            },
            update = { container ->
                container.onDragStartListener = {
                    if (!coordinatesHolder.composeDragActive) {
                        coordinatesHolder.viewDragActive = true
                        currentOnDragStart(it)
                    }
                }
                container.onDragMoveListener = {
                    if (coordinatesHolder.viewDragActive) currentOnDragMove(it)
                }
                container.onDragEndListener = {
                    if (coordinatesHolder.viewDragActive) {
                        coordinatesHolder.viewDragActive = false
                        currentOnDragEnd()
                    }
                }
                container.onDragCancelListener = {
                    if (coordinatesHolder.viewDragActive) {
                        coordinatesHolder.viewDragActive = false
                        currentOnDragCancel()
                    }
                }
                container.setHostView(hostView)
                updateWidgetDimensions(hostView, widthDp.value, heightDp.value)
            },
            modifier = Modifier
                .fillMaxSize()
                .clip(OverviewTheme.shapes.large),
        )
    }
}

@Composable
private fun UnavailableWidgetCard(
    itemId: String,
    onDragStart: (Offset) -> Unit,
    onDragMove: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = OverviewTheme.colors
    val shape = OverviewTheme.shapes.medium
    val haptic = LocalHapticFeedback.current
    val coordinatesHolder = remember { WidgetCoordinatesHolder() }
    val currentOnDragStart by rememberUpdatedState(onDragStart)
    val currentOnDragMove by rememberUpdatedState(onDragMove)
    val currentOnDragEnd by rememberUpdatedState(onDragEnd)
    val currentOnDragCancel by rememberUpdatedState(onDragCancel)

    Box(
        modifier = modifier
            .fillMaxSize()
            .onPlaced { coordinates ->
                coordinatesHolder.coordinates = coordinates
            }
            .clip(shape)
            .background(colors.dashBackground)
            .border(1.dp, colors.hairlineBorder, shape)
            .clickable {
                // Em widgets indisponíveis, um toque simples também permite abrir as opções de remoção.
                val coords = coordinatesHolder.coordinates
                val center = if (coords != null && coords.isAttached) {
                    coords.localToWindow(Offset(coords.size.width / 2f, coords.size.height / 2f))
                } else {
                    Offset.Zero
                }
                currentOnDragStart(center)
                currentOnDragEnd()
            }
            .pointerInput(itemId) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { localOffset ->
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        val coords = coordinatesHolder.coordinates
                        val windowPos = if (coords != null && coords.isAttached) {
                            coords.localToWindow(localOffset)
                        } else {
                            localOffset
                        }
                        currentOnDragStart(windowPos)
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        val coords = coordinatesHolder.coordinates
                        val windowPos = if (coords != null && coords.isAttached) {
                            coords.localToWindow(change.position)
                        } else {
                            change.position
                        }
                        currentOnDragMove(windowPos)
                    },
                    onDragEnd = { currentOnDragEnd() },
                    onDragCancel = { currentOnDragCancel() },
                )
            }
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
 * Contêiner leve que detecta toque longo com arraste em widgets interativos sem
 * interceptar toques normais nem rolagem interna.
 */
private class WidgetHostContainer(context: Context) : FrameLayout(context) {

    var onDragStartListener: ((Offset) -> Unit)? = null
    var onDragMoveListener: ((Offset) -> Unit)? = null
    var onDragEndListener: (() -> Unit)? = null
    var onDragCancelListener: (() -> Unit)? = null

    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private val longPressTimeout = ViewConfiguration.getLongPressTimeout().toLong()
    private val handler = Handler(Looper.getMainLooper())
    private val windowLocation = IntArray(2)

    private var downX = 0f
    private var downY = 0f
    private var longPressTriggered = false

    private val longPressRunnable = Runnable {
        longPressTriggered = true
        parent?.requestDisallowInterceptTouchEvent(true)
        performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        cancelChildTouch()
        onDragStartListener?.invoke(toWindowOffset(downX, downY))
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

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        val handled = super.dispatchTouchEvent(ev)
        if (ev.actionMasked == MotionEvent.ACTION_DOWN && !handled) {
            // Nenhum filho consumiu o toque; o pointerInput do Compose assumirá o gesto.
            handler.removeCallbacks(longPressRunnable)
        }
        return handled
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
                if (!longPressTriggered &&
                    (abs(ev.x - downX) > touchSlop || abs(ev.y - downY) > touchSlop)
                ) {
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
        when (event.actionMasked) {
            MotionEvent.ACTION_MOVE -> {
                if (longPressTriggered) {
                    onDragMoveListener?.invoke(toWindowOffset(event.x, event.y))
                    return true
                }
            }

            MotionEvent.ACTION_UP -> {
                handler.removeCallbacks(longPressRunnable)
                if (longPressTriggered) {
                    longPressTriggered = false
                    parent?.requestDisallowInterceptTouchEvent(false)
                    onDragEndListener?.invoke()
                    return true
                }
            }

            MotionEvent.ACTION_CANCEL -> {
                handler.removeCallbacks(longPressRunnable)
                if (longPressTriggered) {
                    longPressTriggered = false
                    parent?.requestDisallowInterceptTouchEvent(false)
                    onDragCancelListener?.invoke()
                    return true
                }
            }
        }
        return longPressTriggered || super.onTouchEvent(event)
    }

    override fun onDetachedFromWindow() {
        handler.removeCallbacks(longPressRunnable)
        super.onDetachedFromWindow()
    }

    private fun toWindowOffset(localX: Float, localY: Float): Offset {
        getLocationInWindow(windowLocation)
        return Offset(windowLocation[0] + localX, windowLocation[1] + localY)
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
