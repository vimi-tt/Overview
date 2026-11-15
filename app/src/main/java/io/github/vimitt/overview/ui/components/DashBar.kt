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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import io.github.vimitt.overview.domain.model.AppInfo
import io.github.vimitt.overview.ui.theme.OverviewCornerRadius
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
    val dashShape = RoundedCornerShape(OverviewCornerRadius.Dash)
    val clampedAlpha = backgroundAlpha.coerceIn(0f, 1f)
    val borderColor = colors.hairlineBorder.copy(alpha = colors.hairlineBorder.alpha * clampedAlpha)

    Row(
        modifier = modifier
            .widthIn(max = 420.dp)
            .fillMaxWidth()
            .clip(dashShape)
            .background(colors.dashBackground.copy(alpha = clampedAlpha))
            .border(width = 1.dp, color = borderColor, shape = dashShape)
            .padding(horizontal = 12.dp, vertical = 6.dp),
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
