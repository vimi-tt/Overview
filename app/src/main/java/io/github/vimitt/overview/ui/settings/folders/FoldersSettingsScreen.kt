package io.github.vimitt.overview.ui.settings.folders

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.vimitt.overview.R
import io.github.vimitt.overview.domain.model.AppFolder
import io.github.vimitt.overview.domain.model.AppInfo
import io.github.vimitt.overview.domain.model.FolderPreviewStyle
import io.github.vimitt.overview.domain.model.SettingsDefaults
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
                        val countLabel = if (folder.appKeys.size == 1) {
                            stringResource(R.string.settings_folders_app_count_one)
                        } else {
                            stringResource(R.string.settings_folders_app_count_many, folder.appKeys.size)
                        }
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
            initialFolder = editing.folder,
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

@Composable
private fun FolderEditorDialog(
    initialFolder: AppFolder?,
    allApps: List<AppInfo>,
    onSave: (name: String, appKeys: List<String>) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = OverviewTheme.colors
    var folderName by remember(initialFolder?.id) {
        mutableStateOf(initialFolder?.name.orEmpty())
    }
    val selectedKeys = remember(initialFolder?.id) {
        mutableStateListOf<String>().apply {
            addAll(initialFolder?.appKeys.orEmpty())
        }
    }

    val titleRes = if (initialFolder == null) {
        R.string.settings_folder_dialog_title_new
    } else {
        R.string.settings_folder_dialog_title_edit
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surfaceContainer,
        titleContentColor = colors.onSurface,
        textContentColor = colors.onSurface,
        title = { Text(stringResource(titleRes)) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = folderName,
                    onValueChange = { folderName = it },
                    label = { Text(stringResource(R.string.settings_folder_name_label)) },
                    placeholder = { Text(stringResource(R.string.settings_folder_name_placeholder)) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.accent,
                        unfocusedBorderColor = colors.hairlineBorder,
                        focusedLabelColor = colors.accent,
                        cursorColor = colors.accent,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = stringResource(R.string.settings_folder_select_apps, selectedKeys.size),
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.onSurfaceMuted,
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 260.dp),
                ) {
                    items(items = allApps, key = { it.key }) { app ->
                        val isSelected = app.key in selectedKeys
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(OverviewTheme.shapes.medium)
                                .clickable(role = Role.Checkbox) {
                                    if (isSelected) {
                                        selectedKeys.remove(app.key)
                                    } else {
                                        selectedKeys.add(app.key)
                                    }
                                }
                                .padding(horizontal = 4.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            val icon = app.icon
                            if (icon != null) {
                                Image(
                                    bitmap = icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(32.dp),
                                    contentScale = ContentScale.Fit,
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(colors.surfaceElevated),
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = app.label,
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = null,
                                colors = CheckboxDefaults.colors(checkedColor = colors.accent),
                            )
                        }
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.settings_cancel), color = colors.onSurfaceMuted)
            }
        },
        confirmButton = {
            val canSave = folderName.isNotBlank() && selectedKeys.isNotEmpty()
            TextButton(
                enabled = canSave,
                onClick = { onSave(folderName, selectedKeys.toList()) },
            ) {
                Text(
                    text = stringResource(R.string.settings_folder_save),
                    color = if (canSave) colors.accent else colors.onSurfaceMuted,
                )
            }
        },
    )
}
