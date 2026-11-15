package io.github.vimitt.overview.ui.settings.appgrid

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.vimitt.overview.R
import io.github.vimitt.overview.domain.model.AppSortOrder
import io.github.vimitt.overview.domain.model.SettingsDefaults
import io.github.vimitt.overview.ui.settings.components.NavigationRow
import io.github.vimitt.overview.ui.settings.components.SettingsDivider
import io.github.vimitt.overview.ui.settings.components.SettingsGroup
import io.github.vimitt.overview.ui.settings.components.SettingsScaffold
import io.github.vimitt.overview.ui.settings.components.SingleChoiceRow
import io.github.vimitt.overview.ui.settings.components.SliderRow
import io.github.vimitt.overview.ui.theme.OverviewTheme
import kotlin.math.roundToInt

@Composable
fun AppGridSettingsScreen(
    uiState: AppGridSettingsUiState,
    onColumnsChange: (Int) -> Unit,
    onIconScaleChange: (Float) -> Unit,
    onSortOrderChange: (AppSortOrder) -> Unit,
    onOpenHiddenApps: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = OverviewTheme.colors
    val settings = uiState.settings

    SettingsScaffold(
        title = stringResource(R.string.settings_hub_app_grid),
        onBack = onBack,
        modifier = modifier,
    ) {
        item(key = "app_grid_layout_group") {
            SettingsGroup(title = stringResource(R.string.settings_section_drawer_layout)) {
                SliderRow(
                    title = stringResource(R.string.settings_app_grid_columns),
                    value = settings.columns.toFloat(),
                    valueRange = SettingsDefaults.MIN_GRID_COLUMNS.toFloat()..SettingsDefaults.MAX_GRID_COLUMNS.toFloat(),
                    steps = SettingsDefaults.MAX_GRID_COLUMNS - SettingsDefaults.MIN_GRID_COLUMNS - 1,
                    valueLabel = stringResource(R.string.settings_app_grid_columns_format, settings.columns),
                    onValueChange = { value ->
                        onColumnsChange(value.roundToInt())
                    },
                )
                SettingsDivider()
                SliderRow(
                    title = stringResource(R.string.settings_app_grid_icon_scale),
                    value = settings.iconScale,
                    valueRange = SettingsDefaults.MIN_ICON_SCALE..SettingsDefaults.MAX_ICON_SCALE,
                    valueLabel = "${(settings.iconScale * 100f).roundToInt()}%",
                    onValueChange = onIconScaleChange,
                )
            }
        }

        item(key = "app_grid_apps_group") {
            SettingsGroup(title = stringResource(R.string.settings_section_drawer_apps)) {
                SingleChoiceRow(
                    title = stringResource(R.string.settings_app_sort_order),
                    options = listOf(
                        AppSortOrder.ALPHABETICAL_ASC to stringResource(R.string.settings_sort_alpha_asc),
                        AppSortOrder.ALPHABETICAL_DESC to stringResource(R.string.settings_sort_alpha_desc),
                        AppSortOrder.RECENTLY_INSTALLED to stringResource(R.string.settings_sort_recent),
                    ),
                    selected = settings.sortOrder,
                    onSelect = onSortOrderChange,
                )
                SettingsDivider()
                val hiddenCount = settings.hiddenAppKeys.size
                val hiddenSubtitle = when (hiddenCount) {
                    0 -> stringResource(R.string.settings_hidden_apps_none)
                    1 -> stringResource(R.string.settings_hidden_apps_one)
                    else -> stringResource(R.string.settings_hidden_apps_count, hiddenCount)
                }
                NavigationRow(
                    title = stringResource(R.string.settings_hidden_apps_title),
                    subtitle = hiddenSubtitle,
                    icon = {
                        Icon(
                            painter = painterResource(R.drawable.ic_settings_hidden),
                            contentDescription = null,
                            tint = colors.accent,
                            modifier = Modifier.size(22.dp),
                        )
                    },
                    onClick = onOpenHiddenApps,
                )
            }
        }

        item(key = "bottom_spacer") {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
