package io.github.vimitt.overview.ui.settings

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.vimitt.overview.BuildConfig
import io.github.vimitt.overview.R
import io.github.vimitt.overview.domain.model.AppSortOrder
import io.github.vimitt.overview.domain.model.ThemeMode
import io.github.vimitt.overview.ui.settings.components.NavigationRow
import io.github.vimitt.overview.ui.settings.components.SettingsDivider
import io.github.vimitt.overview.ui.settings.components.SettingsGroup
import io.github.vimitt.overview.ui.settings.components.SettingsRow
import io.github.vimitt.overview.ui.settings.components.SettingsScaffold
import io.github.vimitt.overview.ui.theme.AdwaitaPalette
import io.github.vimitt.overview.ui.theme.OverviewTheme

@Composable
fun SettingsHubScreen(
    uiState: SettingsHubUiState,
    onOpenAppearance: () -> Unit,
    onOpenHome: () -> Unit,
    onOpenDock: () -> Unit,
    onOpenAppGrid: () -> Unit,
    onOpenAbout: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = OverviewTheme.colors
    val prefs = uiState.preferences

    val accentName = AdwaitaPalette.AccentPresets
        .firstOrNull { it.second == prefs.appearance.accentColorArgb }
        ?.first
        ?: AdwaitaPalette.AccentPresets.first().first

    val themeLabel = when (prefs.appearance.themeMode) {
        ThemeMode.SYSTEM -> stringResource(R.string.settings_theme_system)
        ThemeMode.LIGHT -> stringResource(R.string.settings_theme_light)
        ThemeMode.DARK -> stringResource(R.string.settings_theme_dark)
    }
    val appearanceSubtitle = "$accentName · $themeLabel"

    val homeSubtitle = if (prefs.home.lockLayout) {
        stringResource(
            R.string.settings_home_summary_locked,
            prefs.home.gridColumns,
            prefs.home.gridRows,
        )
    } else {
        stringResource(
            R.string.settings_home_grid_format,
            prefs.home.gridColumns,
            prefs.home.gridRows,
        )
    }

    val dockSubtitle = if (prefs.dock.pinnedKeys.isEmpty()) {
        stringResource(R.string.settings_dock_summary_auto, prefs.dock.iconCount)
    } else {
        stringResource(
            R.string.settings_dock_summary_pinned,
            prefs.dock.iconCount,
            prefs.dock.pinnedKeys.size,
        )
    }

    val sortLabel = when (prefs.appGrid.sortOrder) {
        AppSortOrder.ALPHABETICAL_ASC -> stringResource(R.string.settings_sort_alpha_asc)
        AppSortOrder.ALPHABETICAL_DESC -> stringResource(R.string.settings_sort_alpha_desc)
        AppSortOrder.RECENTLY_INSTALLED -> stringResource(R.string.settings_sort_recent)
    }
    val columnsText = stringResource(R.string.settings_app_grid_columns_format, prefs.appGrid.columns)
    val appGridSubtitle = "$columnsText · $sortLabel"

    val aboutSubtitle = "${BuildConfig.VERSION_NAME} · ${stringResource(R.string.settings_about_license_value)}"

    SettingsScaffold(
        title = stringResource(R.string.settings_title),
        onBack = onClose,
        modifier = modifier,
    ) {
        item(key = "categories_main") {
            SettingsGroup {
                NavigationRow(
                    title = stringResource(R.string.settings_hub_appearance),
                    subtitle = appearanceSubtitle,
                    icon = {
                        Icon(
                            painter = painterResource(R.drawable.ic_settings_appearance),
                            contentDescription = null,
                            tint = colors.accent,
                            modifier = Modifier.size(22.dp),
                        )
                    },
                    onClick = onOpenAppearance,
                )
                SettingsDivider()
                NavigationRow(
                    title = stringResource(R.string.settings_hub_home),
                    subtitle = homeSubtitle,
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Home,
                            contentDescription = null,
                            tint = colors.accent,
                            modifier = Modifier.size(22.dp),
                        )
                    },
                    onClick = onOpenHome,
                )
                SettingsDivider()
                NavigationRow(
                    title = stringResource(R.string.settings_hub_dock),
                    subtitle = dockSubtitle,
                    icon = {
                        Icon(
                            painter = painterResource(R.drawable.ic_settings_dock),
                            contentDescription = null,
                            tint = colors.accent,
                            modifier = Modifier.size(22.dp),
                        )
                    },
                    onClick = onOpenDock,
                )
                SettingsDivider()
                NavigationRow(
                    title = stringResource(R.string.settings_hub_app_grid),
                    subtitle = appGridSubtitle,
                    icon = {
                        Icon(
                            painter = painterResource(R.drawable.ic_settings_grid),
                            contentDescription = null,
                            tint = colors.accent,
                            modifier = Modifier.size(22.dp),
                        )
                    },
                    onClick = onOpenAppGrid,
                )
            }
        }

        item(key = "categories_about") {
            SettingsGroup {
                NavigationRow(
                    title = stringResource(R.string.settings_hub_about),
                    subtitle = aboutSubtitle,
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = colors.onSurfaceMuted,
                            modifier = Modifier.size(22.dp),
                        )
                    },
                    onClick = onOpenAbout,
                )
            }
        }

        item(key = "categories_planned") {
            SettingsGroup(title = stringResource(R.string.settings_hub_future_header)) {
                SettingsRow(
                    title = stringResource(R.string.settings_hub_future_overview),
                    subtitle = stringResource(R.string.settings_hub_future_subtitle),
                    enabled = false,
                )
                SettingsDivider()
                SettingsRow(
                    title = stringResource(R.string.settings_hub_future_search),
                    subtitle = stringResource(R.string.settings_hub_future_subtitle),
                    enabled = false,
                )
            }
        }

        item(key = "bottom_spacer") {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
