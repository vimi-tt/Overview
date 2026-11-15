package io.github.vimitt.overview.ui.settings.appgrid

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.vimitt.overview.R
import io.github.vimitt.overview.domain.model.AppInfo
import io.github.vimitt.overview.ui.settings.components.SettingsGroup
import io.github.vimitt.overview.ui.settings.components.SettingsScaffold
import io.github.vimitt.overview.ui.settings.components.SwitchRow
import io.github.vimitt.overview.ui.theme.OverviewCornerRadius
import io.github.vimitt.overview.ui.theme.OverviewTheme

@Composable
fun HiddenAppsScreen(
    uiState: AppGridSettingsUiState,
    onShowHiddenInSearchChange: (Boolean) -> Unit,
    onToggleAppHidden: (appKey: String, hidden: Boolean) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onClearSearchQuery: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = OverviewTheme.colors

    SettingsScaffold(
        title = stringResource(R.string.settings_hidden_apps_title),
        onBack = onBack,
        modifier = modifier,
    ) {
        item(key = "show_hidden_in_search") {
            SettingsGroup {
                SwitchRow(
                    title = stringResource(R.string.settings_hidden_show_in_search),
                    subtitle = stringResource(R.string.settings_hidden_show_in_search_subtitle),
                    checked = uiState.settings.showHiddenInSearch,
                    onCheckedChange = onShowHiddenInSearchChange,
                )
            }
        }

        item(key = "filter_bar") {
            HiddenAppsFilterBar(
                query = uiState.hiddenSearchQuery,
                onQueryChange = onSearchQueryChange,
                onClear = onClearSearchQuery,
            )
        }

        if (uiState.filteredAllApps.isEmpty()) {
            item(key = "empty_hidden_list") {
                Text(
                    text = stringResource(R.string.settings_hidden_apps_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceMuted,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 16.dp),
                )
            }
        } else {
            items(
                items = uiState.filteredAllApps,
                key = { it.key },
            ) { app ->
                val isHidden = app.key in uiState.settings.hiddenAppKeys
                HiddenAppToggleRow(
                    app = app,
                    isHidden = isHidden,
                    onToggle = { onToggleAppHidden(app.key, !isHidden) },
                )
            }
        }

        item(key = "bottom_spacer") {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun HiddenAppsFilterBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
) {
    val colors = OverviewTheme.colors
    val shape = RoundedCornerShape(OverviewCornerRadius.Pill)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp)
            .clip(shape)
            .background(colors.searchFieldBackground)
            .border(1.dp, colors.hairlineBorder, shape)
            .padding(horizontal = 14.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.Search,
            contentDescription = null,
            tint = colors.onSurfaceMuted,
            modifier = Modifier.size(20.dp),
        )
        Spacer(modifier = Modifier.width(10.dp))
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.CenterStart,
        ) {
            if (query.isEmpty()) {
                Text(
                    text = stringResource(R.string.settings_hidden_apps_search_hint),
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.onSurfaceMuted,
                    maxLines = 1,
                )
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.onSurface),
                cursorBrush = SolidColor(colors.accent),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (query.isNotEmpty()) {
            IconButton(
                onClick = onClear,
                modifier = Modifier.size(36.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Clear,
                    contentDescription = stringResource(R.string.clear_search_content_description),
                    tint = colors.onSurfaceMuted,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
private fun HiddenAppToggleRow(
    app: AppInfo,
    isHidden: Boolean,
    onToggle: () -> Unit,
) {
    val colors = OverviewTheme.colors
    val shape = RoundedCornerShape(OverviewCornerRadius.Small)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 56.dp)
            .clip(shape)
            .background(colors.surfaceContainer)
            .clickable(role = Role.Switch, onClick = onToggle)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val bitmap = app.icon
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = null,
                modifier = Modifier.size(36.dp),
            )
        } else {
            Spacer(modifier = Modifier.size(36.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = app.label,
            style = MaterialTheme.typography.bodyLarge,
            color = if (isHidden) colors.onSurfaceMuted else colors.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Switch(
            checked = isHidden,
            onCheckedChange = null,
            colors = SwitchDefaults.colors(
                checkedThumbColor = colors.onAccent,
                checkedTrackColor = colors.accent,
            ),
        )
    }
}
