package io.github.vimitt.overview.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.vimitt.overview.R
import io.github.vimitt.overview.domain.model.AppInfo
import io.github.vimitt.overview.domain.model.LauncherPreferences
import io.github.vimitt.overview.domain.model.ThemeMode
import io.github.vimitt.overview.ui.theme.AdwaitaPalette
import io.github.vimitt.overview.ui.theme.OverviewCornerRadius
import io.github.vimitt.overview.ui.theme.OverviewTheme
import kotlin.math.roundToInt

/**
 * Tela de personalização inspirada nas páginas de preferências do libadwaita.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    onThemeModeChange: (ThemeMode) -> Unit,
    onAccentColorChange: (Long) -> Unit,
    onBlurRadiusChange: (Float) -> Unit,
    onShowClockChange: (Boolean) -> Unit,
    onToggleDashApp: (AppInfo) -> Unit,
    onResetDashToAuto: () -> Unit,
    onOpenWallpaperPicker: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = OverviewTheme.colors
    val prefs = uiState.preferences
    var isDashPickerExpanded by remember { mutableStateOf(false) }

    BackHandler(onBack = onClose)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.windowBackground)
            .statusBarsPadding()
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Barra superior estilo Adwaita HeaderBar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 600.dp)
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.settings_back_content_description),
                    tint = colors.onSurface,
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.settings_title),
                style = MaterialTheme.typography.titleLarge,
                color = colors.onSurface,
            )
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .widthIn(max = 600.dp)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            // Seção Aparência
            item(key = "section_appearance") {
                PreferenceSection(title = stringResource(R.string.settings_section_appearance)) {
                    // Seletor de tema
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = stringResource(R.string.settings_theme_label),
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.onSurface,
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        ThemeModeSegmentedControl(
                            selected = prefs.themeMode,
                            onSelect = onThemeModeChange,
                        )
                    }

                    PreferenceDivider()

                    // Seletor de cor de destaque Adwaita
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = stringResource(R.string.settings_accent_label),
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.onSurface,
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            for ((label, argb) in AdwaitaPalette.AccentPresets) {
                                AccentSwatch(
                                    label = label,
                                    color = Color(argb),
                                    isSelected = prefs.accentColorArgb == argb,
                                    onClick = { onAccentColorChange(argb) },
                                )
                            }
                        }
                    }

                    PreferenceDivider()

                    // Slider de intensidade do blur
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = stringResource(R.string.settings_blur_label),
                                style = MaterialTheme.typography.titleMedium,
                                color = colors.onSurface,
                            )
                            val blurLabel = if (prefs.overviewBlurRadiusDp < 1f) {
                                stringResource(R.string.settings_blur_off)
                            } else {
                                "${prefs.overviewBlurRadiusDp.roundToInt()} dp"
                            }
                            Text(
                                text = blurLabel,
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.onSurfaceMuted,
                            )
                        }
                        Slider(
                            value = prefs.overviewBlurRadiusDp,
                            onValueChange = onBlurRadiusChange,
                            valueRange = 0f..48f,
                            colors = SliderDefaults.colors(
                                thumbColor = colors.accent,
                                activeTrackColor = colors.accent,
                                inactiveTrackColor = colors.surfaceElevated,
                            ),
                        )
                    }
                }
            }

            // Seção Tela Inicial
            item(key = "section_home") {
                PreferenceSection(title = stringResource(R.string.settings_section_home)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onShowClockChange(!prefs.showClock) }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.settings_show_clock),
                                style = MaterialTheme.typography.titleMedium,
                                color = colors.onSurface,
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = stringResource(R.string.settings_show_clock_subtitle),
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.onSurfaceMuted,
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Switch(
                            checked = prefs.showClock,
                            onCheckedChange = onShowClockChange,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = colors.onAccent,
                                checkedTrackColor = colors.accent,
                            ),
                        )
                    }

                    PreferenceDivider()

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onOpenWallpaperPicker)
                            .padding(horizontal = 16.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(R.string.settings_wallpaper_row),
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.onSurface,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            // Seção Dash (até 5 favoritos)
            item(key = "section_dash_header") {
                PreferenceSection(title = stringResource(R.string.settings_section_dash)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isDashPickerExpanded = !isDashPickerExpanded }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.settings_dash_subtitle),
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.onSurfaceMuted,
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            val statusText = if (uiState.isUsingAutoDash) {
                                stringResource(R.string.settings_dash_auto_badge)
                            } else {
                                "${prefs.pinnedDashKeys.size}/${LauncherPreferences.MAX_DASH_ITEMS}"
                            }
                            Text(
                                text = statusText,
                                style = MaterialTheme.typography.labelMedium,
                                color = colors.accent,
                            )
                        }
                        if (!uiState.isUsingAutoDash) {
                            TextButton(onClick = onResetDashToAuto) {
                                Text(
                                    text = stringResource(R.string.settings_dash_reset),
                                    color = colors.accent,
                                )
                            }
                        }
                    }
                }
            }

            if (isDashPickerExpanded) {
                items(
                    items = uiState.allApps,
                    key = { it.key },
                ) { app ->
                    val isPinned = app.key in prefs.pinnedDashKeys
                    val canSelectMore = prefs.pinnedDashKeys.size < LauncherPreferences.MAX_DASH_ITEMS
                    DashAppSelectorRow(
                        app = app,
                        isSelected = isPinned,
                        enabled = isPinned || canSelectMore,
                        onToggle = { onToggleDashApp(app) },
                    )
                }
            }

            item(key = "bottom_spacer") {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun PreferenceSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = OverviewTheme.colors
    val shape = RoundedCornerShape(OverviewCornerRadius.Medium)

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = colors.onSurfaceMuted,
            modifier = Modifier.padding(start = 6.dp, bottom = 8.dp),
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(colors.surface)
                .border(width = 1.dp, color = colors.hairlineBorder, shape = shape),
            content = content,
        )
    }
}

@Composable
private fun PreferenceDivider() {
    HorizontalDivider(
        thickness = 1.dp,
        color = OverviewTheme.colors.hairlineBorder,
    )
}

@Composable
private fun ThemeModeSegmentedControl(
    selected: ThemeMode,
    onSelect: (ThemeMode) -> Unit,
) {
    val colors = OverviewTheme.colors
    val containerShape = RoundedCornerShape(OverviewCornerRadius.Small)
    val options = listOf(
        ThemeMode.SYSTEM to stringResource(R.string.settings_theme_system),
        ThemeMode.LIGHT to stringResource(R.string.settings_theme_light),
        ThemeMode.DARK to stringResource(R.string.settings_theme_dark),
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(containerShape)
            .background(colors.surfaceElevated)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        for ((mode, label) in options) {
            val isSelected = mode == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSelected) colors.accent else Color.Transparent)
                    .clickable { onSelect(mode) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = if (isSelected) colors.onAccent else colors.onSurface,
                )
            }
        }
    }
}

@Composable
private fun AccentSwatch(
    label: String,
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val colors = OverviewTheme.colors

    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(color)
            .then(
                if (isSelected) {
                    Modifier.border(width = 2.5.dp, color = colors.onSurface, shape = CircleShape)
                } else {
                    Modifier
                },
            )
            .clickable(onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        if (isSelected) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun DashAppSelectorRow(
    app: AppInfo,
    isSelected: Boolean,
    enabled: Boolean,
    onToggle: () -> Unit,
) {
    val colors = OverviewTheme.colors
    val shape = RoundedCornerShape(OverviewCornerRadius.Small)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.surface)
            .clickable(enabled = enabled, onClick = onToggle)
            .padding(horizontal = 14.dp, vertical = 10.dp),
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
        Text(
            text = app.label,
            style = MaterialTheme.typography.bodyLarge,
            color = if (enabled) colors.onSurface else colors.onSurfaceMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
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
