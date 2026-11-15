package io.github.vimitt.overview.ui.settings.appearance

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
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
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import io.github.vimitt.overview.R
import io.github.vimitt.overview.data.IconPackInfo
import io.github.vimitt.overview.data.NotificationBadgeRepository
import io.github.vimitt.overview.domain.model.IconShape
import io.github.vimitt.overview.domain.model.SettingsDefaults
import io.github.vimitt.overview.domain.model.ThemeMode
import io.github.vimitt.overview.ui.settings.components.ColorPickerRow
import io.github.vimitt.overview.ui.settings.components.NavigationRow
import io.github.vimitt.overview.ui.settings.components.SegmentedRow
import io.github.vimitt.overview.ui.settings.components.SettingsDivider
import io.github.vimitt.overview.ui.settings.components.SettingsGroup
import io.github.vimitt.overview.ui.settings.components.SettingsRow
import io.github.vimitt.overview.ui.settings.components.SettingsScaffold
import io.github.vimitt.overview.ui.settings.components.SliderRow
import io.github.vimitt.overview.ui.settings.components.SwitchRow
import io.github.vimitt.overview.ui.theme.AdwaitaPalette
import io.github.vimitt.overview.ui.theme.OverviewCornerRadius
import io.github.vimitt.overview.ui.theme.OverviewTheme
import kotlin.math.roundToInt

@Composable
fun AppearanceSettingsScreen(
    uiState: AppearanceUiState,
    notificationBadgeRepository: NotificationBadgeRepository,
    onThemeModeChange: (ThemeMode) -> Unit,
    onAccentColorChange: (Long) -> Unit,
    onIconPackSelect: (String?) -> Unit,
    onIconShapeChange: (IconShape) -> Unit,
    onNotificationDotsToggle: (Boolean) -> Boolean,
    onRefreshNotificationAccess: () -> Unit,
    onBlurRadiusChange: (Float) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val settings = uiState.settings
    var showIconPackDialog by remember { mutableStateOf(false) }
    var showNotificationAccessDialog by remember { mutableStateOf(false) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        onRefreshNotificationAccess()
    }

    val selectedPackLabel = remember(settings.iconPackPackage, uiState.installedIconPacks) {
        val pkg = settings.iconPackPackage
        if (pkg == null) {
            null
        } else {
            uiState.installedIconPacks.firstOrNull { it.packageName == pkg }?.label ?: pkg
        }
    }

    SettingsScaffold(
        title = stringResource(R.string.settings_hub_appearance),
        onBack = onBack,
        modifier = modifier,
    ) {
        item(key = "group_theme") {
            SettingsGroup(title = stringResource(R.string.settings_section_theme)) {
                SegmentedRow(
                    title = stringResource(R.string.settings_theme_label),
                    options = listOf(
                        ThemeMode.SYSTEM to stringResource(R.string.settings_theme_system),
                        ThemeMode.LIGHT to stringResource(R.string.settings_theme_light),
                        ThemeMode.DARK to stringResource(R.string.settings_theme_dark),
                    ),
                    selected = settings.themeMode,
                    onSelect = onThemeModeChange,
                )
                SettingsDivider()
                ColorPickerRow(
                    title = stringResource(R.string.settings_accent_label),
                    subtitle = stringResource(R.string.settings_accent_subtitle),
                    presets = AdwaitaPalette.AccentPresets,
                    selectedArgb = settings.accentColorArgb,
                    onSelect = onAccentColorChange,
                )
            }
        }

        item(key = "group_icons") {
            SettingsGroup(title = stringResource(R.string.settings_section_icons)) {
                val iconPackSubtitle = when {
                    selectedPackLabel != null -> selectedPackLabel
                    uiState.isLoadingIconPacks -> stringResource(R.string.settings_icon_pack_loading)
                    else -> stringResource(R.string.settings_icon_pack_system)
                }
                NavigationRow(
                    title = stringResource(R.string.settings_icon_pack_label),
                    subtitle = iconPackSubtitle,
                    onClick = { showIconPackDialog = true },
                )
                SettingsDivider()
                IconShapePickerRow(
                    selectedShape = settings.iconShape,
                    onSelect = onIconShapeChange,
                )
                SettingsDivider()
                val dotsSubtitle = if (settings.notificationDotsEnabled && !uiState.hasNotificationAccess) {
                    stringResource(R.string.settings_notification_permission_missing)
                } else {
                    stringResource(R.string.settings_notification_dots_subtitle)
                }
                SwitchRow(
                    title = stringResource(R.string.settings_notification_dots),
                    subtitle = dotsSubtitle,
                    checked = settings.notificationDotsEnabled && uiState.hasNotificationAccess,
                    onCheckedChange = { wantEnable ->
                        val needsPermission = onNotificationDotsToggle(wantEnable)
                        if (needsPermission) {
                            showNotificationAccessDialog = true
                        }
                    },
                )
            }
        }

        item(key = "group_effects_temp") {
            SettingsGroup(title = stringResource(R.string.settings_section_effects)) {
                val blurLabel = if (settings.overviewBlurRadiusDp < 1f) {
                    stringResource(R.string.settings_blur_off)
                } else {
                    "${settings.overviewBlurRadiusDp.roundToInt()} dp"
                }
                SliderRow(
                    title = stringResource(R.string.settings_blur_label),
                    subtitle = stringResource(R.string.settings_blur_subtitle),
                    value = settings.overviewBlurRadiusDp,
                    valueRange = SettingsDefaults.MIN_BLUR_RADIUS_DP..SettingsDefaults.MAX_BLUR_RADIUS_DP,
                    valueLabel = blurLabel,
                    onValueChange = onBlurRadiusChange,
                )
            }
        }

        item(key = "bottom_spacer") {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showIconPackDialog) {
        IconPackPickerDialog(
            selectedPackage = settings.iconPackPackage,
            packs = uiState.installedIconPacks,
            isLoading = uiState.isLoadingIconPacks,
            onSelect = { pkg ->
                onIconPackSelect(pkg)
                showIconPackDialog = false
            },
            onDismiss = { showIconPackDialog = false },
        )
    }

    if (showNotificationAccessDialog) {
        val colors = OverviewTheme.colors
        AlertDialog(
            onDismissRequest = { showNotificationAccessDialog = false },
            containerColor = colors.surfaceContainer,
            titleContentColor = colors.onSurface,
            textContentColor = colors.onSurface,
            title = { Text(stringResource(R.string.settings_notification_permission_title)) },
            text = { Text(stringResource(R.string.settings_notification_permission_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showNotificationAccessDialog = false
                        val detailIntent = notificationBadgeRepository.createAccessSettingsIntent()
                        runCatching { context.startActivity(detailIntent) }.onFailure {
                            val fallback = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            runCatching { context.startActivity(fallback) }
                        }
                    },
                ) {
                    Text(
                        text = stringResource(R.string.settings_notification_permission_grant),
                        color = colors.accent,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showNotificationAccessDialog = false }) {
                    Text(
                        text = stringResource(R.string.home_move_cancel),
                        color = colors.onSurfaceMuted,
                    )
                }
            },
        )
    }
}

@Composable
private fun IconShapePickerRow(
    selectedShape: IconShape,
    onSelect: (IconShape) -> Unit,
) {
    val colors = OverviewTheme.colors
    var dialogOpen by remember { mutableStateOf(false) }

    val options = listOf(
        IconShape.CIRCLE to stringResource(R.string.settings_icon_shape_circle),
        IconShape.ROUNDED_SQUARE to stringResource(R.string.settings_icon_shape_rounded),
        IconShape.SQUARE to stringResource(R.string.settings_icon_shape_square),
    )

    val currentLabel = options.firstOrNull { it.first == selectedShape }?.second.orEmpty()

    SettingsRow(
        title = stringResource(R.string.settings_icon_shape_label),
        subtitle = currentLabel,
        onClick = { dialogOpen = true },
        role = Role.Button,
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(shapeForPreview(selectedShape))
                        .background(colors.accent.copy(alpha = 0.22f))
                        .border(1.5.dp, colors.accent, shapeForPreview(selectedShape)),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = colors.onSurfaceMuted,
                )
            }
        },
    )

    if (dialogOpen) {
        AlertDialog(
            onDismissRequest = { dialogOpen = false },
            containerColor = colors.surfaceContainer,
            titleContentColor = colors.onSurface,
            textContentColor = colors.onSurface,
            title = {
                Text(
                    text = stringResource(R.string.settings_icon_shape_label),
                    style = MaterialTheme.typography.titleLarge,
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    for ((shapeOption, label) in options) {
                        val isSelected = shapeOption == selectedShape
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 48.dp)
                                .clip(RoundedCornerShape(OverviewCornerRadius.Small))
                                .clickable(role = Role.RadioButton) {
                                    onSelect(shapeOption)
                                    dialogOpen = false
                                }
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = null,
                                colors = RadioButtonDefaults.colors(selectedColor = colors.accent),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(shapeForPreview(shapeOption))
                                    .background(colors.accent.copy(alpha = 0.22f))
                                    .border(1.5.dp, colors.accent, shapeForPreview(shapeOption)),
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyLarge,
                                color = colors.onSurface,
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { dialogOpen = false }) {
                    Text(
                        text = stringResource(R.string.settings_close_dialog),
                        color = colors.accent,
                    )
                }
            },
        )
    }
}

private fun shapeForPreview(shape: IconShape): Shape = when (shape) {
    IconShape.CIRCLE -> CircleShape
    IconShape.ROUNDED_SQUARE -> RoundedCornerShape(8.dp)
    IconShape.SQUARE -> RoundedCornerShape(2.dp)
}

@Composable
private fun IconPackPickerDialog(
    selectedPackage: String?,
    packs: List<IconPackInfo>,
    isLoading: Boolean,
    onSelect: (String?) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = OverviewTheme.colors

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surfaceContainer,
        titleContentColor = colors.onSurface,
        textContentColor = colors.onSurface,
        title = {
            Text(
                text = stringResource(R.string.settings_icon_pack_label),
                style = MaterialTheme.typography.titleLarge,
            )
        },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp),
            ) {
                item(key = "system_default") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 48.dp)
                            .clip(RoundedCornerShape(OverviewCornerRadius.Small))
                            .clickable(role = Role.RadioButton) { onSelect(null) }
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = selectedPackage == null,
                            onClick = null,
                            colors = RadioButtonDefaults.colors(selectedColor = colors.accent),
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        androidx.compose.material3.Icon(
                            painter = painterResource(R.drawable.ic_settings_appearance),
                            contentDescription = null,
                            tint = colors.accent,
                            modifier = Modifier.size(28.dp),
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = stringResource(R.string.settings_icon_pack_system),
                            style = MaterialTheme.typography.bodyLarge,
                            color = colors.onSurface,
                        )
                    }
                }

                if (isLoading) {
                    item(key = "loading") {
                        Text(
                            text = stringResource(R.string.settings_icon_pack_loading),
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.onSurfaceMuted,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp),
                        )
                    }
                } else if (packs.isEmpty()) {
                    item(key = "empty") {
                        Text(
                            text = stringResource(R.string.settings_icon_pack_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.onSurfaceMuted,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp),
                        )
                    }
                } else {
                    items(
                        items = packs,
                        key = { it.packageName },
                    ) { pack ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 48.dp)
                                .clip(RoundedCornerShape(OverviewCornerRadius.Small))
                                .clickable(role = Role.RadioButton) { onSelect(pack.packageName) }
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = selectedPackage == pack.packageName,
                                onClick = null,
                                colors = RadioButtonDefaults.colors(selectedColor = colors.accent),
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            if (pack.icon != null) {
                                Image(
                                    bitmap = pack.icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(28.dp),
                                )
                            } else {
                                Spacer(modifier = Modifier.size(28.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = pack.label,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = colors.onSurface,
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(R.string.settings_close_dialog),
                    color = colors.accent,
                )
            }
        },
    )
}
