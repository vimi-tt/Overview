package io.github.vimitt.overview.ui.components

import android.graphics.Rect
import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import io.github.vimitt.overview.domain.model.AppInfo
import io.github.vimitt.overview.ui.theme.OverviewTheme

/**
 * Dash inferior inspirado no GNOME Shell, exibindo até 5 aplicativos favoritos.
 */
@Composable
fun DashBar(
    apps: List<AppInfo>,
    onAppClick: (app: AppInfo, sourceView: View, sourceBounds: Rect?) -> Unit,
    modifier: Modifier = Modifier,
    backgroundAlpha: Float = 0.85f,
    showNotificationDots: Boolean = false,
    badgePackages: Set<String> = emptySet(),
) {
    if (apps.isEmpty()) return

    val colors = OverviewTheme.colors
    val isExpressive = OverviewTheme.components.isExpressive
    val containerShape = if (isExpressive) OverviewTheme.shapes.pill else OverviewTheme.shapes.dash
    val clampedAlpha = backgroundAlpha.coerceIn(0f, 1f)
    val backgroundColor = if (isExpressive) {
        lerp(colors.surfaceContainerHigh, colors.accentContainer, 0.26f).copy(alpha = clampedAlpha)
    } else {
        colors.dashBackground.copy(alpha = clampedAlpha)
    }
    val borderColor = if (isExpressive) {
        colors.accent.copy(alpha = 0.28f * clampedAlpha)
    } else {
        colors.hairlineBorder.copy(alpha = colors.hairlineBorder.alpha * clampedAlpha)
    }

    Row(
        modifier = modifier
            .widthIn(max = 420.dp)
            .fillMaxWidth()
            .then(
                if (isExpressive && clampedAlpha > 0.15f) {
                    Modifier.shadow(
                        elevation = (10f * clampedAlpha).dp,
                        shape = containerShape,
                        clip = false,
                    )
                } else {
                    Modifier
                },
            )
            .clip(containerShape)
            .background(backgroundColor)
            .border(width = 1.dp, color = borderColor, shape = containerShape)
            .padding(
                horizontal = if (isExpressive) 14.dp else 12.dp,
                vertical = if (isExpressive) 8.dp else 6.dp,
            ),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        for (app in apps) {
            key(app.key) {
                AppIconItem(
                    app = app,
                    onClick = onAppClick,
                    iconSize = 48.dp,
                    showLabel = false,
                    showNotificationDot = showNotificationDots && app.packageName in badgePackages,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}
