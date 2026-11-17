package io.github.vimitt.overview.ui.settings.appearance

import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import io.github.vimitt.overview.R
import io.github.vimitt.overview.data.IconPackInfo
import io.github.vimitt.overview.data.NotificationBadgeRepository
import io.github.vimitt.overview.domain.model.ColorSourceMode
import io.github.vimitt.overview.domain.model.IconShape
import io.github.vimitt.overview.domain.model.MotionIntensity
import io.github.vimitt.overview.domain.model.SettingsDefaults
import io.github.vimitt.overview.domain.model.ShapeFamily
import io.github.vimitt.overview.domain.model.ThemeMode
import io.github.vimitt.overview.domain.model.TypographyWeightMode
import io.github.vimitt.overview.domain.model.VisualStyle
import io.github.vimitt.overview.ui.components.ExpressiveLoadingIndicator
import io.github.vimitt.overview.ui.components.ExpressiveSplitButton
import io.github.vimitt.overview.ui.components.ExpressiveWavyProgressBar
import io.github.vimitt.overview.ui.components.MorphPolygonShape
import io.github.vimitt.overview.ui.components.OverviewPolygonFactory
import io.github.vimitt.overview.ui.components.RoundedPolygonShape
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
import io.github.vimitt.overview.ui.theme.OverviewTheme
import kotlin.math.roundToInt

@Composable
fun AppearanceSettingsScreen(
    uiState: AppearanceUiState,
    notificationBadgeRepository: NotificationBadgeRepository,
    onVisualStyleChange: (VisualStyle) -> Unit,
    onThemeModeChange: (ThemeMode) -> Unit,
    onColorSourceModeChange: (ColorSourceMode) -> Unit,
    onAccentColorChange: (Long) -> Unit,
    onCustomAccentColorChange: (Long) -> Unit,
    onMotionIntensityChange: (MotionIntensity) -> Unit,
    onShapeFamilyChange: (ShapeFamily) -> Unit,
    onIconShapeChange: (IconShape) -> Unit,
    onIconRoundnessChange: (Float) -> Unit,
    onTypographyWeightModeChange: (TypographyWeightMode) -> Unit,
    onIconPackSelect: (String?) -> Unit,
    onNotificationDotsToggle: (Boolean) -> Boolean,
    onRefreshNotificationAccess: () -> Unit,
    onOverviewBlurEnabledChange: (Boolean) -> Unit,
    onOverviewBlurRadiusChange: (Float) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val settings = uiState.settings
    var showIconPackDialog by remember { mutableStateOf(false) }
    var showNotificationAccessDialog by remember { mutableStateOf(false) }
    var showCustomHexDialog by remember { mutableStateOf(false) }

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
        item(key = "group_visual_style") {
            SettingsGroup(title = stringResource(R.string.settings_section_visual_style)) {
                VisualStylePreviewCard(
                    selectedStyle = settings.visualStyle,
                    onSelectStyle = onVisualStyleChange,
                )
                SettingsDivider()
                val styleSubtitle = when (settings.visualStyle) {
                    VisualStyle.GNOME -> stringResource(R.string.settings_visual_style_gnome_sub)
                    VisualStyle.EXPRESSIVE -> stringResource(R.string.settings_visual_style_expressive_sub)
                }
                SegmentedRow(
                    title = stringResource(R.string.settings_visual_style_label),
                    subtitle = styleSubtitle,
                    options = listOf(
                        VisualStyle.GNOME to stringResource(R.string.settings_visual_style_gnome),
                        VisualStyle.EXPRESSIVE to stringResource(R.string.settings_visual_style_expressive),
                    ),
                    selected = settings.visualStyle,
                    onSelect = onVisualStyleChange,
                    useButtonGroup = true,
                )
            }
        }

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
                val colorSourceSubtitle = when (settings.colorSourceMode) {
                    ColorSourceMode.GNOME -> stringResource(R.string.settings_color_source_subtitle_gnome)
                    ColorSourceMode.DYNAMIC -> stringResource(R.string.settings_color_source_subtitle_dynamic)
                    ColorSourceMode.CUSTOM -> stringResource(R.string.settings_color_source_subtitle_custom)
                }
                SegmentedRow(
                    title = stringResource(R.string.settings_color_source_label),
                    subtitle = colorSourceSubtitle,
                    options = listOf(
                        ColorSourceMode.GNOME to stringResource(R.string.settings_color_source_gnome),
                        ColorSourceMode.DYNAMIC to stringResource(R.string.settings_color_source_dynamic),
                        ColorSourceMode.CUSTOM to stringResource(R.string.settings_color_source_custom),
                    ),
                    selected = settings.colorSourceMode,
                    onSelect = onColorSourceModeChange,
                    useButtonGroup = true,
                )

                when (settings.colorSourceMode) {
                    ColorSourceMode.GNOME -> {
                        SettingsDivider()
                        val localizedPresets = AdwaitaPalette.AccentPresets.map { (fallback, argb) ->
                            val label = AdwaitaPalette.AccentPresetNameRes[argb]?.let { stringResource(it) } ?: fallback
                            label to argb
                        }
                        ColorPickerRow(
                            title = stringResource(R.string.settings_accent_label),
                            subtitle = stringResource(R.string.settings_accent_subtitle),
                            presets = localizedPresets,
                            selectedArgb = settings.accentColorArgb,
                            onSelect = onAccentColorChange,
                        )
                    }

                    ColorSourceMode.CUSTOM -> {
                        SettingsDivider()
                        val localizedCustomPresets = AdwaitaPalette.CustomAccentPresets.map { (fallback, argb) ->
                            val label = AdwaitaPalette.AccentPresetNameRes[argb]?.let { stringResource(it) } ?: fallback
                            label to argb
                        }
                        ColorPickerRow(
                            title = stringResource(R.string.settings_custom_accent_label),
                            subtitle = formatHexColor(settings.customAccentColorArgb),
                            presets = localizedCustomPresets,
                            selectedArgb = settings.customAccentColorArgb,
                            onSelect = onCustomAccentColorChange,
                        )
                        SettingsDivider()
                        SettingsRow(
                            title = stringResource(R.string.settings_custom_accent_hex),
                            subtitle = formatHexColor(settings.customAccentColorArgb),
                            icon = {
                                Icon(
                                    imageVector = Icons.Filled.Edit,
                                    contentDescription = null,
                                    tint = OverviewTheme.colors.accent,
                                )
                            },
                            trailingContent = {
                                TextButton(
                                    onClick = {
                                        onCustomAccentColorChange(SettingsDefaults.DEFAULT_CUSTOM_ACCENT_COLOR_ARGB)
                                    },
                                ) {
                                    Text(
                                        text = stringResource(R.string.settings_custom_accent_reset),
                                        color = OverviewTheme.colors.accent,
                                    )
                                }
                            },
                            role = Role.Button,
                            onClick = { showCustomHexDialog = true },
                        )
                    }

                    ColorSourceMode.DYNAMIC -> Unit
                }
            }
        }

        item(key = "group_motion_typography") {
            SettingsGroup(title = stringResource(R.string.settings_section_motion_type)) {
                val motionSubtitle = if (settings.visualStyle == VisualStyle.GNOME) {
                    stringResource(R.string.settings_motion_intensity_subtitle_gnome)
                } else {
                    when (settings.motionIntensity) {
                        MotionIntensity.LOW -> stringResource(R.string.settings_motion_intensity_subtitle_low)
                        MotionIntensity.MEDIUM -> stringResource(R.string.settings_motion_intensity_subtitle_medium)
                        MotionIntensity.HIGH -> stringResource(R.string.settings_motion_intensity_subtitle_high)
                    }
                }
                SegmentedRow(
                    title = stringResource(R.string.settings_motion_intensity_label),
                    subtitle = motionSubtitle,
                    options = listOf(
                        MotionIntensity.LOW to stringResource(R.string.settings_motion_intensity_low),
                        MotionIntensity.MEDIUM to stringResource(R.string.settings_motion_intensity_medium),
                        MotionIntensity.HIGH to stringResource(R.string.settings_motion_intensity_high),
                    ),
                    selected = settings.motionIntensity,
                    onSelect = onMotionIntensityChange,
                )
                SettingsDivider()
                val typographySubtitle = when (settings.typographyWeightMode) {
                    TypographyWeightMode.NORMAL ->
                        stringResource(R.string.settings_typography_weight_subtitle_normal)

                    TypographyWeightMode.EMPHATIC ->
                        stringResource(R.string.settings_typography_weight_subtitle_emphatic)
                }
                SegmentedRow(
                    title = stringResource(R.string.settings_typography_weight_label),
                    subtitle = typographySubtitle,
                    options = listOf(
                        TypographyWeightMode.NORMAL to stringResource(R.string.settings_typography_weight_normal),
                        TypographyWeightMode.EMPHATIC to stringResource(R.string.settings_typography_weight_emphatic),
                    ),
                    selected = settings.typographyWeightMode,
                    onSelect = onTypographyWeightModeChange,
                )
            }
        }

        item(key = "group_icons") {
            SettingsGroup(title = stringResource(R.string.settings_section_icons)) {
                val shapeFamilySubtitle = when (settings.shapeFamily) {
                    ShapeFamily.SOFT -> stringResource(R.string.settings_shape_family_subtitle_soft)
                    ShapeFamily.ROUNDED -> stringResource(R.string.settings_shape_family_subtitle_rounded)
                    ShapeFamily.EXPRESSIVE -> stringResource(R.string.settings_shape_family_subtitle_expressive)
                }
                SegmentedRow(
                    title = stringResource(R.string.settings_shape_family_label),
                    subtitle = shapeFamilySubtitle,
                    options = listOf(
                        ShapeFamily.SOFT to stringResource(R.string.settings_shape_family_soft),
                        ShapeFamily.ROUNDED to stringResource(R.string.settings_shape_family_rounded),
                        ShapeFamily.EXPRESSIVE to stringResource(R.string.settings_shape_family_expressive),
                    ),
                    selected = settings.shapeFamily,
                    onSelect = onShapeFamilyChange,
                )
                SettingsDivider()
                IconShapePickerRow(
                    selectedShape = settings.iconShape,
                    roundness = settings.iconRoundness,
                    onSelect = onIconShapeChange,
                )
                SettingsDivider()
                SliderRow(
                    title = stringResource(R.string.settings_icon_roundness_label),
                    value = settings.iconRoundness,
                    valueRange = SettingsDefaults.MIN_ICON_ROUNDNESS..SettingsDefaults.MAX_ICON_ROUNDNESS,
                    valueLabel = stringResource(
                        R.string.settings_icon_roundness_value,
                        (settings.iconRoundness * 100f).roundToInt(),
                    ),
                    onValueChange = onIconRoundnessChange,
                )
                SettingsDivider()
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

        item(key = "group_effects") {
            val blurEnabled = uiState.overviewBlurRadiusDp > 0.5f
            val blurToggleSubtitle = if (blurEnabled) {
                stringResource(
                    R.string.settings_appearance_blur_toggle_on,
                    uiState.overviewBlurRadiusDp.roundToInt(),
                )
            } else {
                stringResource(R.string.settings_appearance_blur_toggle_off)
            }
            val blurValueLabel = if (!blurEnabled) {
                stringResource(R.string.settings_blur_off)
            } else {
                "${uiState.overviewBlurRadiusDp.roundToInt()} dp"
            }

            SettingsGroup(title = stringResource(R.string.settings_section_appearance_effects)) {
                SwitchRow(
                    title = stringResource(R.string.settings_appearance_blur_toggle),
                    subtitle = blurToggleSubtitle,
                    checked = blurEnabled,
                    onCheckedChange = onOverviewBlurEnabledChange,
                )
                SettingsDivider()
                SliderRow(
                    title = stringResource(R.string.settings_appearance_blur_intensity),
                    value = uiState.overviewBlurRadiusDp,
                    valueRange = SettingsDefaults.MIN_BLUR_RADIUS_DP..SettingsDefaults.MAX_BLUR_RADIUS_DP,
                    valueLabel = blurValueLabel,
                    onValueChange = onOverviewBlurRadiusChange,
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

    if (showCustomHexDialog) {
        CustomAccentHexDialog(
            initialArgb = settings.customAccentColorArgb,
            onApply = { argb ->
                onCustomAccentColorChange(argb)
                showCustomHexDialog = false
            },
            onDismiss = { showCustomHexDialog = false },
        )
    }

    if (showNotificationAccessDialog) {
        val colors = OverviewTheme.colors
        val isExpressive = OverviewTheme.components.isExpressive
        AlertDialog(
            onDismissRequest = { showNotificationAccessDialog = false },
            shape = OverviewTheme.shapes.large,
            containerColor = if (isExpressive) colors.surfaceContainerHigh else colors.surfaceContainer,
            titleContentColor = colors.onSurface,
            textContentColor = colors.onSurface,
            title = { Text(stringResource(R.string.settings_notification_permission_title)) },
            text = { Text(stringResource(R.string.settings_notification_permission_body)) },
            confirmButton = {
                Button(
                    onClick = {
                        showNotificationAccessDialog = false
                        val detailIntent = notificationBadgeRepository.createAccessSettingsIntent()
                        runCatching { context.startActivity(detailIntent) }.onFailure {
                            val fallback = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            runCatching { context.startActivity(fallback) }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.accent,
                        contentColor = colors.onAccent,
                    ),
                ) {
                    Text(text = stringResource(R.string.settings_notification_permission_grant))
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
private fun VisualStylePreviewCard(
    selectedStyle: VisualStyle,
    onSelectStyle: (VisualStyle) -> Unit,
) {
    val colors = OverviewTheme.colors
    val shapes = OverviewTheme.shapes
    val motion = OverviewTheme.motion
    var previewPulse by remember { mutableStateOf(false) }

    val pulseProgress by animateFloatAsState(
        targetValue = if (previewPulse) 1f else 0f,
        animationSpec = motion.expressiveSpec(durationMillis = 220),
        label = "visualStylePreviewPulse",
    )
    val morphShape = remember(shapes.iconPressMorph) {
        MorphPolygonShape(shapes.iconPressMorph) { pulseProgress.coerceIn(0f, 1f) }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.settings_visual_style_preview_title),
                style = OverviewTheme.typography.rowTitle,
                color = colors.onSurface,
            )
            ExpressiveSplitButton(
                primaryLabel = stringResource(R.string.settings_visual_style_preview_action),
                onPrimaryClick = { previewPulse = !previewPulse },
                onSecondaryClick = {
                    val next = if (selectedStyle == VisualStyle.GNOME) {
                        VisualStyle.EXPRESSIVE
                    } else {
                        VisualStyle.GNOME
                    }
                    onSelectStyle(next)
                },
                secondaryContentDescription = stringResource(R.string.settings_visual_style_preview_toggle_cd),
                primaryIcon = Icons.Filled.PlayArrow,
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            StyleMiniPreviewTile(
                title = stringResource(R.string.settings_visual_style_gnome),
                caption = stringResource(R.string.settings_visual_style_preview_gnome_pill),
                selected = selectedStyle == VisualStyle.GNOME,
                isExpressivePreview = false,
                pulseProgress = pulseProgress,
                morphShape = CircleShape,
                onClick = { onSelectStyle(VisualStyle.GNOME) },
                modifier = Modifier.weight(1f),
            )
            StyleMiniPreviewTile(
                title = stringResource(R.string.settings_visual_style_expressive),
                caption = stringResource(R.string.settings_visual_style_preview_expressive_pill),
                selected = selectedStyle == VisualStyle.EXPRESSIVE,
                isExpressivePreview = true,
                pulseProgress = pulseProgress,
                morphShape = morphShape,
                onClick = { onSelectStyle(VisualStyle.EXPRESSIVE) },
                modifier = Modifier.weight(1f),
            )
        }

        if (selectedStyle == VisualStyle.EXPRESSIVE) {
            ExpressiveWavyProgressBar(
                progress = if (previewPulse) 0.84f else 0.52f,
            )
        }
    }
}

@Composable
private fun StyleMiniPreviewTile(
    title: String,
    caption: String,
    selected: Boolean,
    isExpressivePreview: Boolean,
    pulseProgress: Float,
    morphShape: Shape,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = OverviewTheme.colors
    val shapes = OverviewTheme.shapes
    val tileShape = if (isExpressivePreview) shapes.large else shapes.settingsGroup
    val bgColor = when {
        selected && isExpressivePreview -> colors.accentContainer.copy(alpha = 0.62f)
        selected -> colors.surfaceElevated
        else -> colors.windowBackground
    }
    val borderColor = if (selected) colors.accent else colors.hairlineBorder
    val borderWidth = if (selected) 2.dp else 1.dp

    Column(
        modifier = modifier
            .defaultMinSize(minHeight = 98.dp)
            .clip(tileShape)
            .background(bgColor)
            .border(borderWidth, borderColor, tileShape)
            .selectable(
                selected = selected,
                role = Role.RadioButton,
                onClick = onClick,
            )
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = if (isExpressivePreview) FontWeight.Bold else FontWeight.Medium,
                ),
                color = colors.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .graphicsLayer {
                        if (isExpressivePreview) {
                            val s = 1f + 0.10f * pulseProgress
                            scaleX = s
                            scaleY = s
                        }
                    }
                    .clip(morphShape)
                    .background(colors.accent.copy(alpha = if (selected) 0.92f else 0.45f)),
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(3) { idx ->
                val chipShape = if (isExpressivePreview && idx == 1) {
                    morphShape
                } else if (isExpressivePreview) {
                    RoundedCornerShape(10.dp)
                } else {
                    RoundedCornerShape(6.dp)
                }
                Box(
                    modifier = Modifier
                        .size(width = 24.dp, height = 14.dp)
                        .clip(chipShape)
                        .background(
                            if (idx == 0) {
                                colors.accent.copy(alpha = 0.30f)
                            } else {
                                colors.onSurfaceMuted.copy(alpha = 0.22f)
                            },
                        ),
                )
            }
        }

        Text(
            text = caption,
            style = MaterialTheme.typography.labelSmall,
            color = colors.onSurfaceMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun IconShapePickerRow(
    selectedShape: IconShape,
    roundness: Float,
    onSelect: (IconShape) -> Unit,
) {
    val colors = OverviewTheme.colors
    var dialogOpen by remember { mutableStateOf(false) }

    val options = listOf(
        IconShape.CIRCLE to stringResource(R.string.settings_icon_shape_circle),
        IconShape.ROUNDED_SQUARE to stringResource(R.string.settings_icon_shape_rounded),
        IconShape.SQUIRCLE to stringResource(R.string.settings_icon_shape_squircle),
        IconShape.SQUARE to stringResource(R.string.settings_icon_shape_square),
        IconShape.CLOVER to stringResource(R.string.settings_icon_shape_clover),
        IconShape.FLOWER to stringResource(R.string.settings_icon_shape_flower),
    )

    val currentLabel = options.firstOrNull { it.first == selectedShape }?.second.orEmpty()
    val previewShape = remember(selectedShape, roundness) {
        shapeForPreview(selectedShape, roundness)
    }

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
                        .clip(previewShape)
                        .background(colors.accent.copy(alpha = 0.22f))
                        .border(1.5.dp, colors.accent, previewShape),
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
                Column(modifier = Modifier.fillMaxWidth().selectableGroup()) {
                    for ((shapeOption, label) in options) {
                        val isSelected = shapeOption == selectedShape
                        val itemShape = remember(shapeOption, roundness) {
                            shapeForPreview(shapeOption, roundness)
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 48.dp)
                                .clip(OverviewTheme.shapes.small)
                                .selectable(
                                    selected = isSelected,
                                    role = Role.RadioButton,
                                    onClick = {
                                        onSelect(shapeOption)
                                        dialogOpen = false
                                    },
                                )
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
                                    .clip(itemShape)
                                    .background(colors.accent.copy(alpha = 0.22f))
                                    .border(1.5.dp, colors.accent, itemShape),
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

@Composable
private fun CustomAccentHexDialog(
    initialArgb: Long,
    onApply: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = OverviewTheme.colors
    var hexInput by remember(initialArgb) {
        mutableStateOf(formatHexColor(initialArgb).removePrefix("#"))
    }
    var hasError by remember { mutableStateOf(false) }

    val parsedColor = remember(hexInput) { parseHexColorArgb(hexInput) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = OverviewTheme.shapes.large,
        containerColor = if (OverviewTheme.components.isExpressive) {
            colors.surfaceContainerHigh
        } else {
            colors.surfaceContainer
        },
        titleContentColor = colors.onSurface,
        textContentColor = colors.onSurface,
        title = {
            Text(
                text = stringResource(R.string.settings_custom_accent_hex),
                style = MaterialTheme.typography.titleLarge,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(parsedColor ?: initialArgb))
                            .border(1.dp, colors.hairlineBorder, CircleShape),
                    )
                    OutlinedTextField(
                        value = hexInput,
                        onValueChange = { raw ->
                            hexInput = raw.uppercase().take(7)
                            hasError = false
                        },
                        singleLine = true,
                        isError = hasError,
                        prefix = { Text("#") },
                        modifier = Modifier.weight(1f),
                    )
                }
                if (hasError) {
                    Text(
                        text = stringResource(R.string.settings_custom_accent_invalid_hex),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.error,
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val value = parseHexColorArgb(hexInput)
                    if (value != null) {
                        onApply(value)
                    } else {
                        hasError = true
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.accent,
                    contentColor = colors.onAccent,
                ),
            ) {
                Text(text = stringResource(R.string.settings_custom_accent_apply))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(R.string.settings_cancel),
                    color = colors.onSurfaceMuted,
                )
            }
        },
    )
}

private fun formatHexColor(argb: Long): String {
    val rgb = (argb and 0xFFFFFFL).toInt()
    return "#%06X".format(rgb)
}

private fun parseHexColorArgb(input: String): Long? {
    val cleaned = input.trim().removePrefix("#")
    if (cleaned.length != 6) return null
    val rgb = cleaned.toLongOrNull(16) ?: return null
    return 0xFF000000L or rgb
}

private fun shapeForPreview(
    shape: IconShape,
    roundness: Float = 0.5f,
): Shape = when (shape) {
    IconShape.CIRCLE -> CircleShape
    IconShape.ROUNDED_SQUARE,
    IconShape.SQUARE,
    IconShape.SQUIRCLE,
    IconShape.CLOVER,
    IconShape.FLOWER -> RoundedPolygonShape(OverviewPolygonFactory.iconPolygon(shape, roundness))
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
    val shapes = OverviewTheme.shapes
    val components = OverviewTheme.components

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = OverviewTheme.shapes.large,
        containerColor = if (OverviewTheme.components.isExpressive) {
            colors.surfaceContainerHigh
        } else {
            colors.surfaceContainer
        },
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
                    .height(260.dp)
                    .selectableGroup(),
            ) {
                item(key = "system_default") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 48.dp)
                            .clip(shapes.small)
                            .selectable(
                                selected = selectedPackage == null,
                                role = Role.RadioButton,
                                onClick = { onSelect(null) },
                            )
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = selectedPackage == null,
                            onClick = null,
                            colors = RadioButtonDefaults.colors(selectedColor = colors.accent),
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Icon(
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
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            if (components.useExpressiveLoading) {
                                ExpressiveLoadingIndicator(size = 32.dp)
                            }
                            Text(
                                text = stringResource(R.string.settings_icon_pack_loading),
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.onSurfaceMuted,
                            )
                        }
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
                                .clip(shapes.small)
                                .selectable(
                                    selected = selectedPackage == pack.packageName,
                                    role = Role.RadioButton,
                                    onClick = { onSelect(pack.packageName) },
                                )
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
