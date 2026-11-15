package io.github.vimitt.overview.ui.settings.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.vimitt.overview.R
import io.github.vimitt.overview.domain.model.HomeClockStyle
import io.github.vimitt.overview.domain.model.SettingsDefaults
import io.github.vimitt.overview.ui.components.HomeClock
import io.github.vimitt.overview.ui.settings.components.NavigationRow
import io.github.vimitt.overview.ui.settings.components.SettingsDivider
import io.github.vimitt.overview.ui.settings.components.SettingsGroup
import io.github.vimitt.overview.ui.settings.components.SettingsScaffold
import io.github.vimitt.overview.ui.settings.components.SingleChoiceRow
import io.github.vimitt.overview.ui.settings.components.SliderRow
import io.github.vimitt.overview.ui.settings.components.SwitchRow
import io.github.vimitt.overview.ui.theme.OverviewCornerRadius
import io.github.vimitt.overview.ui.theme.OverviewTheme
import kotlin.math.roundToInt

@Composable
fun HomeSettingsScreen(
    uiState: HomeSettingsUiState,
    onGridColumnsChange: (Int) -> Unit,
    onGridRowsChange: (Int) -> Unit,
    onIconScaleChange: (Float) -> Unit,
    onShowLabelsChange: (Boolean) -> Unit,
    onLabelScaleChange: (Float) -> Unit,
    onLockLayoutChange: (Boolean) -> Unit,
    onShowClockChange: (Boolean) -> Unit,
    onClockStyleChange: (HomeClockStyle) -> Unit,
    onOpenWallpaperPicker: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = OverviewTheme.colors
    val settings = uiState.settings

    SettingsScaffold(
        title = stringResource(R.string.settings_hub_home),
        onBack = onBack,
        modifier = modifier,
    ) {
        item(key = "home_grid_group") {
            SettingsGroup(title = stringResource(R.string.settings_section_home_grid)) {
                HomeGridPreview(
                    columns = settings.gridColumns,
                    rows = settings.gridRows,
                )
                SettingsDivider()
                SliderRow(
                    title = stringResource(R.string.settings_home_columns),
                    value = settings.gridColumns.toFloat(),
                    valueRange = SettingsDefaults.MIN_GRID_COLUMNS.toFloat()..SettingsDefaults.MAX_GRID_COLUMNS.toFloat(),
                    steps = SettingsDefaults.MAX_GRID_COLUMNS - SettingsDefaults.MIN_GRID_COLUMNS - 1,
                    valueLabel = "${settings.gridColumns}",
                    onValueChange = { value ->
                        onGridColumnsChange(value.roundToInt())
                    },
                )
                SettingsDivider()
                SliderRow(
                    title = stringResource(R.string.settings_home_rows),
                    value = settings.gridRows.toFloat(),
                    valueRange = SettingsDefaults.MIN_GRID_ROWS.toFloat()..SettingsDefaults.MAX_GRID_ROWS.toFloat(),
                    steps = SettingsDefaults.MAX_GRID_ROWS - SettingsDefaults.MIN_GRID_ROWS - 1,
                    valueLabel = "${settings.gridRows}",
                    onValueChange = { value ->
                        onGridRowsChange(value.roundToInt())
                    },
                )
            }
        }

        item(key = "home_items_group") {
            SettingsGroup(title = stringResource(R.string.settings_section_home_items)) {
                SliderRow(
                    title = stringResource(R.string.settings_home_icon_scale),
                    value = settings.iconScale,
                    valueRange = SettingsDefaults.MIN_ICON_SCALE..SettingsDefaults.MAX_ICON_SCALE,
                    valueLabel = "${(settings.iconScale * 100f).roundToInt()}%",
                    onValueChange = onIconScaleChange,
                )
                SettingsDivider()
                SwitchRow(
                    title = stringResource(R.string.settings_home_show_labels),
                    subtitle = stringResource(R.string.settings_home_show_labels_subtitle),
                    checked = settings.showLabels,
                    onCheckedChange = onShowLabelsChange,
                )
                SettingsDivider()
                SliderRow(
                    title = stringResource(R.string.settings_home_label_scale),
                    value = settings.labelScale,
                    valueRange = SettingsDefaults.MIN_LABEL_SCALE..SettingsDefaults.MAX_LABEL_SCALE,
                    valueLabel = "${(settings.labelScale * 100f).roundToInt()}%",
                    enabled = settings.showLabels,
                    onValueChange = onLabelScaleChange,
                )
            }
        }

        item(key = "home_clock_group") {
            val clockOptions = listOf(
                HomeClockStyle.GNOME to stringResource(R.string.settings_home_clock_style_gnome),
                HomeClockStyle.STACKED to stringResource(R.string.settings_home_clock_style_stacked),
                HomeClockStyle.ANALOG to stringResource(R.string.settings_home_clock_style_analog),
                HomeClockStyle.SIDE_BY_SIDE to stringResource(R.string.settings_home_clock_style_side),
                HomeClockStyle.PILL to stringResource(R.string.settings_home_clock_style_pill),
            )

            SettingsGroup(title = stringResource(R.string.settings_section_home_clock)) {
                HomeClockPreview(
                    showClock = settings.showClock,
                    style = settings.clockStyle,
                    onCycleStyle = {
                        val values = HomeClockStyle.entries
                        val next = values[(values.indexOf(settings.clockStyle) + 1) % values.size]
                        onClockStyleChange(next)
                    },
                )
                SettingsDivider()
                SwitchRow(
                    title = stringResource(R.string.settings_show_clock),
                    subtitle = stringResource(R.string.settings_show_clock_subtitle),
                    checked = settings.showClock,
                    onCheckedChange = onShowClockChange,
                )
                SettingsDivider()
                SingleChoiceRow(
                    title = stringResource(R.string.settings_home_clock_style),
                    options = clockOptions,
                    selected = settings.clockStyle,
                    enabled = settings.showClock,
                    onSelect = onClockStyleChange,
                )
            }
        }

        item(key = "home_general_group") {
            SettingsGroup(title = stringResource(R.string.settings_section_home_general)) {
                SwitchRow(
                    title = stringResource(R.string.settings_home_lock_layout),
                    subtitle = stringResource(R.string.settings_home_lock_layout_subtitle),
                    checked = settings.lockLayout,
                    onCheckedChange = onLockLayoutChange,
                )
                SettingsDivider()
                NavigationRow(
                    title = stringResource(R.string.settings_wallpaper_row),
                    icon = {
                        Icon(
                            painter = painterResource(R.drawable.ic_settings_wallpaper),
                            contentDescription = null,
                            tint = colors.accent,
                            modifier = Modifier.size(22.dp),
                        )
                    },
                    onClick = onOpenWallpaperPicker,
                )
            }
        }

        item(key = "bottom_spacer") {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun HomeClockPreview(
    showClock: Boolean,
    style: HomeClockStyle,
    onCycleStyle: () -> Unit,
) {
    val colors = OverviewTheme.colors
    val shape = RoundedCornerShape(OverviewCornerRadius.Medium)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 176.dp)
                .clip(shape)
                .background(colors.windowBackground)
                .border(1.dp, colors.hairlineBorder, shape)
                .clickable(enabled = showClock, onClick = onCycleStyle)
                .padding(horizontal = 16.dp, vertical = 18.dp),
            contentAlignment = Alignment.Center,
        ) {
            HomeClock(
                style = style,
                compact = false,
                useWallpaperColors = false,
                modifier = Modifier.alpha(if (showClock) 1f else 0.32f),
            )
        }
        if (showClock) {
            Text(
                text = stringResource(R.string.settings_home_clock_preview_hint),
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceMuted,
            )
        }
    }
}

@Composable
private fun HomeGridPreview(
    columns: Int,
    rows: Int,
) {
    val colors = OverviewTheme.colors
    val shape = RoundedCornerShape(OverviewCornerRadius.Small)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = stringResource(R.string.settings_home_grid_format, columns, rows),
            style = MaterialTheme.typography.titleMedium,
            color = colors.onSurface,
        )

        Column(
            modifier = Modifier
                .clip(shape)
                .background(colors.surfaceElevated)
                .border(1.dp, colors.hairlineBorder, shape)
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            repeat(rows) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    repeat(columns) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(colors.accent.copy(alpha = 0.45f)),
                        )
                    }
                }
            }
        }
    }
}
