package io.github.vimitt.overview.ui.settings.folders

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.vimitt.overview.R
import io.github.vimitt.overview.domain.model.AppFolder
import io.github.vimitt.overview.domain.model.FolderPreviewStyle
import io.github.vimitt.overview.domain.model.SettingsDefaults
import io.github.vimitt.overview.ui.components.FolderEditorDialog
import io.github.vimitt.overview.ui.settings.components.SegmentedRow
import io.github.vimitt.overview.ui.settings.components.SettingsDivider
import io.github.vimitt.overview.ui.settings.components.SettingsGroup
import io.github.vimitt.overview.ui.settings.components.SettingsRow
import io.github.vimitt.overview.ui.settings.components.SettingsScaffold
import io.github.vimitt.overview.ui.settings.components.SwitchRow
import io.github.vimitt.overview.ui.theme.OverviewTheme

@Composable
fun FoldersSettingsScreen(
    uiState: FoldersSettingsUiState,
    onPreviewStyleChange: (FolderPreviewStyle) -> Unit,
    onColumnsChange: (Int) -> Unit,
    onSortAppsInsideChange: (Boolean) -> Unit,
    onCloseOnAppLaunchChange: (Boolean) -> Unit,
    onSaveFolder: (folderId: String?, name: String, appKeys: List<String>) -> Unit,
    onDeleteFolder: (folderId: String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = OverviewTheme.colors
    val settings = uiState.settings
    var editorTarget by remember { mutableStateOf<FolderEditorState?>(null) }

    SettingsScaffold(
        title = stringResource(R.string.settings_hub_folders),
        onBack = onBack,
        modifier = modifier,
    ) {
        item(key = "group_appearance") {
            SettingsGroup(title = stringResource(R.string.settings_section_folders_appearance)) {
                SegmentedRow(
                    title = stringResource(R.string.settings_folders_preview_label),
                    options = listOf(
                        FolderPreviewStyle.GRID_2X2 to stringResource(R.string.settings_folders_preview_grid),
                        FolderPreviewStyle.STACKED to stringResource(R.string.settings_folders_preview_stacked),
                    ),
                    selected = settings.previewStyle,
                    onSelect = onPreviewStyleChange,
                )
                SettingsDivider()
                SegmentedRow(
                    title = stringResource(R.string.settings_folders_columns_label),
                    options = listOf(
                        SettingsDefaults.MIN_FOLDER_COLUMNS to "3",
                        SettingsDefaults.MAX_FOLDER_COLUMNS to "4",
                    ),
                    selected = settings.folderColumns,
                    onSelect = onColumnsChange,
                )
                SettingsDivider()
                SwitchRow(
                    title = stringResource(R.string.settings_folders_sort_inside),
                    subtitle = stringResource(R.string.settings_folders_sort_inside_subtitle),
                    checked = settings.sortAppsInside,
                    onCheckedChange = onSortAppsInsideChange,
                )
                SettingsDivider()
                SwitchRow(
                    title = stringResource(R.string.settings_folders_close_launch),
                    subtitle = stringResource(R.string.settings_folders_close_launch_subtitle),
                    checked = settings.closeOnAppLaunch,
                    onCheckedChange = onCloseOnAppLaunchChange,
                )
            }
        }

        item(key = "group_manage") {
            SettingsGroup(title = stringResource(R.string.settings_section_folders_manage)) {
                SettingsRow(
                    title = stringResource(R.string.settings_folders_create),
                    subtitle = stringResource(R.string.settings_folders_create_subtitle),
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = colors.accent,
                            modifier = Modifier.size(22.dp),
                        )
                    },
                    onClick = {
                        editorTarget = FolderEditorState(folder = null)
                    },
                )
                if (settings.folders.isEmpty()) {
                    SettingsDivider()
                    SettingsRow(
                        title = stringResource(R.string.settings_folders_empty),
                        enabled = false,
                    )
                } else {
                    for (folder in settings.folders) {
                        SettingsDivider()
                        val countLabel = pluralStringResource(
                            R.plurals.settings_folders_app_count,
                            folder.appKeys.size,
                            folder.appKeys.size,
                        )
                        SettingsRow(
                            title = folder.name,
                            subtitle = countLabel,
                            icon = {
                                Icon(
                                    painter = painterResource(R.drawable.ic_settings_folders),
                                    contentDescription = null,
                                    tint = colors.accent,
                                    modifier = Modifier.size(20.dp),
                                )
                            },
                            trailingContent = {
                                IconButton(onClick = { onDeleteFolder(folder.id) }) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = stringResource(R.string.settings_folder_delete),
                                        tint = colors.onSurfaceMuted,
                                    )
                                }
                            },
                            onClick = {
                                editorTarget = FolderEditorState(folder = folder)
                            },
                        )
                    }
                }
            }
        }

        item(key = "bottom_spacer") {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    val editing = editorTarget
    if (editing != null) {
        FolderEditorDialog(
            initialName = editing.folder?.name.orEmpty(),
            initialAppKeys = editing.folder?.appKeys.orEmpty(),
            isNewFolder = editing.folder == null,
            allApps = uiState.allApps,
            onSave = { name, keys ->
                onSaveFolder(editing.folder?.id, name, keys)
                editorTarget = null
            },
            onDismiss = { editorTarget = null },
        )
    }
}

private data class FolderEditorState(
    val folder: AppFolder?,
)
