package io.github.vimitt.overview.ui.settings.overview

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import io.github.vimitt.overview.R
import io.github.vimitt.overview.domain.model.OverviewAnimationSpeed
import io.github.vimitt.overview.domain.model.OverviewSettings
import io.github.vimitt.overview.domain.model.OverviewTransitionStyle
import io.github.vimitt.overview.domain.model.SettingsDefaults
import io.github.vimitt.overview.ui.settings.components.SegmentedRow
import io.github.vimitt.overview.ui.settings.components.SettingsDivider
import io.github.vimitt.overview.ui.settings.components.SettingsGroup
import io.github.vimitt.overview.ui.settings.components.SettingsScaffold
import io.github.vimitt.overview.ui.settings.components.SliderRow
import io.github.vimitt.overview.ui.settings.components.SwitchRow
import io.github.vimitt.overview.ui.theme.OverviewTheme
import kotlin.math.roundToInt

@Composable
fun OverviewSettingsScreen(
    uiState: OverviewSettingsUiState,
    onBlurRadiusChange: (Float) -> Unit,
    onScrimAlphaChange: (Float) -> Unit,
    onTransitionStyleChange: (OverviewTransitionStyle) -> Unit,
    onAnimationSpeedChange: (OverviewAnimationSpeed) -> Unit,
    onHomeDepthEffectChange: (Boolean) -> Unit,
    onAppLaunchAnimationChange: (Boolean) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val settings = uiState.settings
    val reducedMotion = OverviewTheme.reducedMotion

    SettingsScaffold(
        title = stringResource(R.string.settings_hub_overview),
        onBack = onBack,
        modifier = modifier,
    ) {
        item(key = "preview") {
            OverviewEffectsPreview(settings = settings)
        }

        if (reducedMotion) {
            item(key = "reduced_motion_notice") {
                ReducedMotionNoticeBanner()
            }
        }

        item(key = "group_blur") {
            SettingsGroup(title = stringResource(R.string.settings_section_overview_blur)) {
                val blurLabel = if (settings.blurRadiusDp < 1f) {
                    stringResource(R.string.settings_blur_off)
                } else {
                    "${settings.blurRadiusDp.roundToInt()} dp"
                }
                SliderRow(
                    title = stringResource(R.string.settings_blur_label),
                    subtitle = stringResource(R.string.settings_blur_subtitle),
                    value = settings.blurRadiusDp,
                    valueRange = SettingsDefaults.MIN_BLUR_RADIUS_DP..SettingsDefaults.MAX_BLUR_RADIUS_DP,
                    valueLabel = blurLabel,
                    onValueChange = onBlurRadiusChange,
                )
                SettingsDivider()
                val scrimPercent = "${(settings.scrimAlpha * 100f).roundToInt()}%"
                SliderRow(
                    title = stringResource(R.string.settings_overview_scrim_label),
                    subtitle = stringResource(R.string.settings_overview_scrim_subtitle),
                    value = settings.scrimAlpha,
                    valueRange = SettingsDefaults.MIN_OVERVIEW_SCRIM_ALPHA..SettingsDefaults.MAX_OVERVIEW_SCRIM_ALPHA,
                    valueLabel = scrimPercent,
                    onValueChange = onScrimAlphaChange,
                )
            }
        }

        item(key = "group_motion") {
            SettingsGroup(title = stringResource(R.string.settings_section_overview_motion)) {
                SegmentedRow(
                    title = stringResource(R.string.settings_overview_transition_label),
                    options = listOf(
                        OverviewTransitionStyle.GNOME_SHELL to stringResource(R.string.settings_overview_transition_gnome),
                        OverviewTransitionStyle.SLIDE to stringResource(R.string.settings_overview_transition_slide),
                        OverviewTransitionStyle.FADE to stringResource(R.string.settings_overview_transition_fade),
                    ),
                    selected = settings.transitionStyle,
                    onSelect = onTransitionStyleChange,
                )
                SettingsDivider()
                SegmentedRow(
                    title = stringResource(R.string.settings_overview_speed_label),
                    options = listOf(
                        OverviewAnimationSpeed.FAST to stringResource(R.string.settings_overview_speed_fast),
                        OverviewAnimationSpeed.NORMAL to stringResource(R.string.settings_overview_speed_normal),
                        OverviewAnimationSpeed.RELAXED to stringResource(R.string.settings_overview_speed_relaxed),
                    ),
                    selected = settings.animationSpeed,
                    onSelect = onAnimationSpeedChange,
                )
                SettingsDivider()
                SwitchRow(
                    title = stringResource(R.string.settings_overview_depth_label),
                    subtitle = stringResource(R.string.settings_overview_depth_subtitle),
                    checked = settings.homeDepthEffect,
                    onCheckedChange = onHomeDepthEffectChange,
                )
                SettingsDivider()
                SwitchRow(
                    title = stringResource(R.string.settings_overview_launch_anim_label),
                    subtitle = stringResource(R.string.settings_overview_launch_anim_subtitle),
                    checked = settings.appLaunchAnimation,
                    onCheckedChange = onAppLaunchAnimationChange,
                )
            }
        }

        item(key = "bottom_spacer") {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ReducedMotionNoticeBanner() {
    val colors = OverviewTheme.colors
    val shape = OverviewTheme.shapes.settingsGroup

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.accent.copy(alpha = 0.14f))
            .border(1.dp, colors.accent.copy(alpha = 0.45f), shape)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Default.Info,
            contentDescription = null,
            tint = colors.accent,
            modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = stringResource(R.string.settings_overview_reduced_motion_notice),
            style = MaterialTheme.typography.bodySmall,
            color = colors.onSurface,
        )
    }
}

@Composable
private fun OverviewEffectsPreview(
    settings: OverviewSettings,
) {
    val colors = OverviewTheme.colors
    val motion = OverviewTheme.motion
    val shapes = OverviewTheme.shapes
    val reducedMotion = OverviewTheme.reducedMotion
    val density = LocalDensity.current
    var isPreviewOpen by remember { mutableStateOf(true) }

    val progress by animateFloatAsState(
        targetValue = if (isPreviewOpen) 1f else 0f,
        animationSpec = motion.spatialSpec(
            durationMillis = 240,
            durationScale = settings.animationSpeed.durationScale,
        ),
        label = "overviewSettingsPreviewProgress",
    )

    val clampedBlurFraction = progress.coerceIn(0f, 1.05f)
    val blurPx = with(density) { (settings.blurRadiusDp * 0.45f * clampedBlurFraction).dp.toPx() }
    val homeScale = if (settings.homeDepthEffect && !reducedMotion) {
        1f - (0.06f * progress)
    } else {
        1f
    }

    val cardShape = shapes.settingsGroup
    val baseScrim = colors.overviewScrim.copy(alpha = (settings.scrimAlpha * progress).coerceIn(0f, 1f))

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(148.dp)
                .clip(cardShape)
                .background(
                    Brush.linearGradient(
                        listOf(
                            colors.accent.copy(alpha = 0.65f),
                            colors.surfaceElevated,
                            colors.accent.copy(alpha = 0.35f),
                        ),
                    ),
                )
                .border(1.dp, colors.hairlineBorder, cardShape)
                .clickable(role = Role.Button) { isPreviewOpen = !isPreviewOpen },
        ) {
            // Camada da mini-Home com zoom de profundidade e RenderEffect de blur
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = homeScale
                        scaleY = homeScale
                        renderEffect = if (blurPx > 0.5f) {
                            BlurEffect(
                                radiusX = blurPx,
                                radiusY = blurPx,
                                edgeTreatment = TileMode.Clamp,
                            )
                        } else {
                            null
                        }
                    }
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 64.dp, height = 16.dp)
                        .clip(shapes.pill)
                        .background(Color.White.copy(alpha = 0.85f)),
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .clip(shapes.pill)
                        .background(colors.dashBackground)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                ) {
                    repeat(4) {
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(colors.accent.copy(alpha = 0.8f)),
                        )
                    }
                }
            }

            // Camada de scrim + mini-Overview segundo o estilo de transição escolhido
            if (progress > 0.01f) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(baseScrim)
                        .graphicsLayer {
                            alpha = progress.coerceIn(0f, 1f)
                            when (settings.transitionStyle) {
                                OverviewTransitionStyle.GNOME_SHELL -> {
                                    val s = 0.92f + 0.08f * progress
                                    scaleX = s
                                    scaleY = s
                                    translationY = (1f - progress) * 28f
                                }

                                OverviewTransitionStyle.SLIDE -> {
                                    translationY = (1f - progress) * 44f
                                }

                                OverviewTransitionStyle.FADE -> Unit
                            }
                        }
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.72f)
                            .height(16.dp)
                            .clip(shapes.pill)
                            .background(colors.searchFieldBackground),
                    )
                    repeat(2) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            repeat(4) {
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(colors.accent.copy(alpha = 0.85f)),
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = stringResource(R.string.settings_overview_preview_hint),
            style = MaterialTheme.typography.bodySmall,
            color = colors.onSurfaceMuted,
        )
    }
}
