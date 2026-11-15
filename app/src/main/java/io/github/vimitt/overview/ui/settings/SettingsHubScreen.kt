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
import io.github.vimitt.overview.domain.model.AppGridLayoutMode
import io.github.vimitt.overview.domain.model.AppSortOrder
import io.github.vimitt.overview.domain.model.ColorSourceMode
import io.github.vimitt.overview.domain.model.FolderPreviewStyle
import io.github.vimitt.overview.domain.model.OverviewTransitionStyle
import io.github.vimitt.overview.domain.model.ThemeMode
import io.github.vimitt.overview.domain.model.VisualStyle
import io.github.vimitt.overview.domain.model.WebSearchEngine
import io.github.vimitt.overview.ui.settings.components.NavigationRow
import io.github.vimitt.overview.ui.settings.components.SettingsDivider
import io.github.vimitt.overview.ui.settings.components.SettingsGroup
import io.github.vimitt.overview.ui.settings.components.SettingsScaffold
import io.github.vimitt.overview.ui.settings.gestures.gestureActionLabel
import io.github.vimitt.overview.ui.theme.AdwaitaPalette
import io.github.vimitt.overview.ui.theme.OverviewTheme

@Composable
fun SettingsHubScreen(
    uiState: SettingsHubUiState,
    onOpenAppearance: () -> Unit,
    onOpenHome: () -> Unit,
    onOpenDock: () -> Unit,
    onOpenAppGrid: () -> Unit,
    onOpenOverview: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenFolders: () -> Unit,
    onOpenGestures: () -> Unit,
    onOpenAbout: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = OverviewTheme.colors
    val prefs = uiState.preferences

    val styleLabel = when (prefs.appearance.visualStyle) {
        VisualStyle.GNOME -> stringResource(R.string.settings_visual_style_gnome)
        VisualStyle.EXPRESSIVE -> stringResource(R.string.settings_visual_style_expressive)
    }

    val colorSourceLabel = when (prefs.appearance.colorSourceMode) {
        ColorSourceMode.GNOME -> AdwaitaPalette.AccentPresets
            .firstOrNull { it.second == prefs.appearance.accentColorArgb }
            ?.first
            ?: AdwaitaPalette.AccentPresets.first().first

        ColorSourceMode.DYNAMIC -> stringResource(R.string.settings_color_source_dynamic)
        ColorSourceMode.CUSTOM -> stringResource(R.string.settings_color_source_custom)
    }

    val themeLabel = when (prefs.appearance.themeMode) {
        ThemeMode.SYSTEM -> stringResource(R.string.settings_theme_system)
        ThemeMode.LIGHT -> stringResource(R.string.settings_theme_light)
        ThemeMode.DARK -> stringResource(R.string.settings_theme_dark)
    }
    val appearanceSubtitle = "$styleLabel · $colorSourceLabel · $themeLabel"

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
    val layoutLabel = when (prefs.appGrid.layoutMode) {
        AppGridLayoutMode.PAGED -> stringResource(R.string.settings_app_grid_layout_paged)
        AppGridLayoutMode.CONTINUOUS -> stringResource(R.string.settings_app_grid_layout_continuous)
    }
    val appGridSubtitle = "$columnsText · $layoutLabel · $sortLabel"

    val blurText = if (prefs.overview.blurRadiusDp < 1f) {
        stringResource(R.string.settings_overview_blur_summary_off)
    } else {
        stringResource(R.string.settings_overview_blur_summary_on, prefs.overview.blurRadiusDp.toInt())
    }
    val transitionText = when (prefs.overview.transitionStyle) {
        OverviewTransitionStyle.GNOME_SHELL -> stringResource(R.string.settings_overview_transition_gnome)
        OverviewTransitionStyle.SLIDE -> stringResource(R.string.settings_overview_transition_slide)
        OverviewTransitionStyle.FADE -> stringResource(R.string.settings_overview_transition_fade)
    }
    val overviewSubtitle = "$blurText · $transitionText"

    val engineName = when (prefs.search.webSearchEngine) {
        WebSearchEngine.DUCKDUCKGO -> stringResource(R.string.settings_search_engine_duckduckgo)
        WebSearchEngine.GOOGLE -> stringResource(R.string.settings_search_engine_google)
        WebSearchEngine.STARTPAGE -> stringResource(R.string.settings_search_engine_startpage)
        WebSearchEngine.ECOSIA -> stringResource(R.string.settings_search_engine_ecosia)
    }
    val searchSubtitle = if (prefs.search.autoFocusOnOpen) {
        stringResource(R.string.settings_search_summary_autofocus, engineName)
    } else {
        stringResource(R.string.settings_search_summary_manual, engineName)
    }

    val folderStyleText = when (prefs.folders.previewStyle) {
        FolderPreviewStyle.GRID_2X2 -> stringResource(R.string.settings_folders_preview_grid)
        FolderPreviewStyle.STACKED -> stringResource(R.string.settings_folders_preview_stacked)
    }
    val foldersSubtitle = when (prefs.folders.folders.size) {
        0 -> stringResource(R.string.settings_folders_summary_none, folderStyleText)
        1 -> stringResource(R.string.settings_folders_summary_one, folderStyleText)
        else -> stringResource(
            R.string.settings_folders_summary_many,
            prefs.folders.folders.size,
            folderStyleText,
        )
    }

    val gesturesSubtitle = stringResource(
        R.string.settings_gestures_summary_format,
        gestureActionLabel(prefs.gestures.swipeUpAction),
        gestureActionLabel(prefs.gestures.swipeDownAction),
    )

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
                SettingsDivider()
                NavigationRow(
                    title = stringResource(R.string.settings_hub_overview),
                    subtitle = overviewSubtitle,
                    icon = {
                        Icon(
                            painter = painterResource(R.drawable.ic_settings_overview),
                            contentDescription = null,
                            tint = colors.accent,
                            modifier = Modifier.size(22.dp),
                        )
                    },
                    onClick = onOpenOverview,
                )
                SettingsDivider()
                NavigationRow(
                    title = stringResource(R.string.settings_hub_search),
                    subtitle = searchSubtitle,
                    icon = {
                        Icon(
                            painter = painterResource(R.drawable.ic_settings_search),
                            contentDescription = null,
                            tint = colors.accent,
                            modifier = Modifier.size(22.dp),
                        )
                    },
                    onClick = onOpenSearch,
                )
                SettingsDivider()
                NavigationRow(
                    title = stringResource(R.string.settings_hub_folders),
                    subtitle = foldersSubtitle,
                    icon = {
                        Icon(
                            painter = painterResource(R.drawable.ic_settings_folders),
                            contentDescription = null,
                            tint = colors.accent,
                            modifier = Modifier.size(22.dp),
                        )
                    },
                    onClick = onOpenFolders,
                )
                SettingsDivider()
                NavigationRow(
                    title = stringResource(R.string.settings_hub_gestures),
                    subtitle = gesturesSubtitle,
                    icon = {
                        Icon(
                            painter = painterResource(R.drawable.ic_settings_gestures),
                            contentDescription = null,
                            tint = colors.accent,
                            modifier = Modifier.size(22.dp),
                        )
                    },
                    onClick = onOpenGestures,
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

        item(key = "bottom_spacer") {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
