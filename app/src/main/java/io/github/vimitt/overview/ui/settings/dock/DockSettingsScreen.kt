package io.github.vimitt.overview.ui.settings.dock

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.vimitt.overview.R
import io.github.vimitt.overview.domain.model.AppInfo
import io.github.vimitt.overview.domain.model.SettingsDefaults
import io.github.vimitt.overview.ui.settings.components.SettingsDivider
import io.github.vimitt.overview.ui.settings.components.SettingsGroup
import io.github.vimitt.overview.ui.settings.components.SettingsRow
import io.github.vimitt.overview.ui.settings.components.SettingsScaffold
import io.github.vimitt.overview.ui.settings.components.SliderRow
import io.github.vimitt.overview.ui.settings.components.SwitchRow
import io.github.vimitt.overview.ui.theme.OverviewTheme
import kotlin.math.roundToInt

@Composable
fun DockSettingsScreen(
    uiState: DockSettingsUiState,
    onIconCountChange: (Int) -> Unit,
    onBackgroundAlphaChange: (Float) -> Unit,
    onShowSearchBarChange: (Boolean) -> Unit,
    onTogglePinnedApp: (AppInfo) -> Unit,
    onResetToAuto: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = OverviewTheme.colors
    val settings = uiState.settings

    SettingsScaffold(
        title = stringResource(R.string.settings_hub_dock),
        onBack = onBack,
        modifier = modifier,
    ) {
        item(key = "dock_layout_group") {
            SettingsGroup(title = stringResource(R.string.settings_section_dock_layout)) {
                SliderRow(
                    title = stringResource(R.string.settings_dock_icon_count),
                    value = settings.iconCount.toFloat(),
                    valueRange = SettingsDefaults.MIN_DOCK_ICONS.toFloat()..SettingsDefaults.MAX_DOCK_ICONS.toFloat(),
                    steps = SettingsDefaults.MAX_DOCK_ICONS - SettingsDefaults.MIN_DOCK_ICONS - 1,
                    valueLabel = pluralStringResource(
                        R.plurals.settings_dock_icons_count,
                        settings.iconCount,
                        settings.iconCount,
                    ),
                    onValueChange = { value ->
                        onIconCountChange(value.roundToInt())
                    },
                )
                SettingsDivider()
                SliderRow(
                    title = stringResource(R.string.settings_dock_bg_alpha),
                    value = settings.backgroundAlpha,
                    valueRange = SettingsDefaults.MIN_DOCK_ALPHA..SettingsDefaults.MAX_DOCK_ALPHA,
                    valueLabel = "${(settings.backgroundAlpha * 100f).roundToInt()}%",
                    onValueChange = onBackgroundAlphaChange,
                )
                SettingsDivider()
                SwitchRow(
                    title = stringResource(R.string.settings_dock_show_search),
                    subtitle = stringResource(R.string.settings_dock_show_search_subtitle),
                    checked = settings.showSearchBar,
                    onCheckedChange = onShowSearchBarChange,
                )
            }
        }

        item(key = "dock_favorites_header") {
            SettingsGroup(title = stringResource(R.string.settings_section_dock_favorites)) {
                val badgeText = if (uiState.isUsingAutoDock) {
                    stringResource(R.string.settings_dash_auto_badge)
                } else {
                    "${settings.pinnedKeys.size}/${settings.iconCount}"
                }
                SettingsRow(
                    title = badgeText,
                    subtitle = pluralStringResource(
                        R.plurals.settings_dash_subtitle,
                        settings.iconCount,
                        settings.iconCount,
                    ),
                    trailingContent = if (!uiState.isUsingAutoDock) {
                        {
                            TextButton(onClick = onResetToAuto) {
                                Text(
                                    text = stringResource(R.string.settings_dash_reset),
                                    color = colors.accent,
                                )
                            }
                        }
                    } else {
                        null
                    },
                )
            }
        }

        items(
            items = uiState.allApps,
            key = { it.key },
        ) { app ->
            val isPinned = app.key in settings.pinnedKeys
            val canSelectMore = settings.pinnedKeys.size < settings.iconCount
            DockPinnedAppRow(
                app = app,
                isSelected = isPinned,
                enabled = isPinned || canSelectMore,
                onToggle = { onTogglePinnedApp(app) },
            )
        }

        item(key = "bottom_spacer") {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun DockPinnedAppRow(
    app: AppInfo,
    isSelected: Boolean,
    enabled: Boolean,
    onToggle: () -> Unit,
) {
    val colors = OverviewTheme.colors
    val shape = OverviewTheme.shapes.small

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 52.dp)
            .clip(shape)
            .background(colors.surfaceContainer)
            .clickable(enabled = enabled, onClick = onToggle)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val bitmap = app.icon
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = null,
                modifier = Modifier.size(34.dp),
            )
        } else {
            Spacer(modifier = Modifier.size(34.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = app.label,
                style = MaterialTheme.typography.bodyLarge,
                color = if (enabled) colors.onSurface else colors.onSurfaceMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Checkbox(
            checked = isSelected,
            onCheckedChange = if (enabled) {
                { onToggle() }
            } else {
                null
            },
            enabled = enabled,
            colors = CheckboxDefaults.colors(
                checkedColor = colors.accent,
                checkmarkColor = colors.onAccent,
            ),
        )
    }
}
