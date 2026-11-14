package io.github.vimitt.overview.ui.components

import android.graphics.Rect
import android.view.View
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.vimitt.overview.domain.model.AppInfo
import io.github.vimitt.overview.ui.theme.OverviewCornerRadius
import io.github.vimitt.overview.ui.theme.OverviewTheme
import io.github.vimitt.overview.ui.theme.overviewSpring
import kotlin.math.roundToInt

private class CoordinatesHolder {
    var coordinates: LayoutCoordinates? = null
}

/**
 * Item reutilizável de aplicativo (ícone + rótulo opcional) com captura sob demanda
 * da posição na tela para animar a abertura via `LauncherApps.startMainActivity`.
 */
@Composable
fun AppIconItem(
    app: AppInfo,
    onClick: (app: AppInfo, sourceView: View, sourceBounds: Rect?) -> Unit,
    modifier: Modifier = Modifier,
    iconSize: Dp = 54.dp,
    showLabel: Boolean = true,
) {
    val colors = OverviewTheme.colors
    val reducedMotion = OverviewTheme.reducedMotion
    val view = LocalView.current

    // Armazena as coordenadas sem acionar escrita de estado Compose durante o layout.
    val coordinatesHolder = remember { CoordinatesHolder() }
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val pressScale by animateFloatAsState(
        targetValue = if (isPressed && !reducedMotion) 0.92f else 1f,
        animationSpec = overviewSpring(reducedMotion),
        label = "appIconPressScale",
    )

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(OverviewCornerRadius.Medium))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    val coords = coordinatesHolder.coordinates
                    val bounds = if (coords != null && coords.isAttached) {
                        val rect = coords.boundsInWindow()
                        Rect(
                            rect.left.roundToInt(),
                            rect.top.roundToInt(),
                            rect.right.roundToInt(),
                            rect.bottom.roundToInt(),
                        )
                    } else {
                        null
                    }
                    onClick(app, view, bounds)
                },
            )
            .padding(horizontal = 6.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top,
    ) {
        Box(
            modifier = Modifier
                .size(iconSize)
                .graphicsLayer {
                    scaleX = pressScale
                    scaleY = pressScale
                }
                .onGloballyPositioned { coordinates ->
                    coordinatesHolder.coordinates = coordinates
                },
            contentAlignment = Alignment.Center,
        ) {
            val bitmap = app.icon
            if (bitmap != null) {
                Image(
                    bitmap = bitmap,
                    contentDescription = app.label,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit,
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(OverviewCornerRadius.Medium))
                        .background(colors.surfaceElevated),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Home,
                        contentDescription = app.label,
                        tint = colors.onSurfaceMuted,
                    )
                }
            }
        }

        if (showLabel) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = app.label,
                style = MaterialTheme.typography.labelMedium,
                color = colors.onSurface,
                minLines = 2,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
        }
    }
}
