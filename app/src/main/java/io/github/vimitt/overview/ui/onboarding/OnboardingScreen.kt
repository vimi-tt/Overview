package io.github.vimitt.overview.ui.onboarding

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.vimitt.overview.R
import io.github.vimitt.overview.data.NativeWidgetSystemProviders
import io.github.vimitt.overview.data.NotificationBadgeRepository
import io.github.vimitt.overview.domain.model.VisualStyle
import io.github.vimitt.overview.ui.components.OverviewAppIcon
import io.github.vimitt.overview.ui.components.OverviewPolygonFactory
import io.github.vimitt.overview.ui.components.RoundedPolygonShape
import io.github.vimitt.overview.ui.components.rememberIsLifecycleActive
import io.github.vimitt.overview.ui.theme.OverviewTheme
import kotlinx.coroutines.delay

private enum class OnboardingStep {
    INTRO,
    WELCOME,
    REQUESTS,
    THEME,
}

/**
 * Fluxo inicial de boas-vindas exibido na primeira abertura do Overview (ou em prévia Debug):
 * 1) Animação simples de entrada (`INTRO`)
 * 2) Boas-vindas (`WELCOME`)
 * 3) Requisições de permissões opcionais (`REQUESTS`)
 * 4) Escolha de tema GNOME vs Material Expressive com transição animada (`THEME`).
 */
@Composable
fun OnboardingScreen(
    currentVisualStyle: VisualStyle,
    permissionRevision: Int,
    notificationBadgeRepository: NotificationBadgeRepository,
    isDebugPreview: Boolean,
    onRequestCalendarPermission: () -> Unit,
    onRequestLocationPermission: () -> Unit,
    onSelectVisualStyle: (VisualStyle) -> Unit,
    onComplete: () -> Unit,
    onDismissPreview: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val colors = OverviewTheme.colors
    val shapes = OverviewTheme.shapes
    val motion = OverviewTheme.motion
    val reducedMotion = OverviewTheme.reducedMotion

    var currentStep by remember { mutableStateOf(OnboardingStep.INTRO) }

    BackHandler(enabled = currentStep != OnboardingStep.INTRO) {
        when (currentStep) {
            OnboardingStep.WELCOME -> if (isDebugPreview) onDismissPreview()
            OnboardingStep.REQUESTS -> currentStep = OnboardingStep.WELCOME
            OnboardingStep.THEME -> currentStep = OnboardingStep.REQUESTS
            OnboardingStep.INTRO -> Unit
        }
    }

    // Fundo suave com leve gradiente tonal que acompanha o tema animado (GNOME <-> Material)
    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(
            colors.windowBackground,
            lerp(colors.windowBackground, colors.accentContainer, if (colors.isDark) 0.20f else 0.16f),
            colors.windowBackground,
        ),
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundBrush)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (currentStep != OnboardingStep.INTRO) {
                OnboardingTopBar(
                    step = currentStep,
                    isDebugPreview = isDebugPreview,
                    onDismissPreview = onDismissPreview,
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .widthIn(max = 540.dp),
                contentAlignment = Alignment.Center,
            ) {
                AnimatedContent(
                    targetState = currentStep,
                    transitionSpec = {
                        if (reducedMotion) {
                            fadeIn(tween(150)) togetherWith fadeOut(tween(120))
                        } else if (initialState == OnboardingStep.INTRO) {
                            (fadeIn(tween(340)) + scaleIn(initialScale = 0.96f, animationSpec = tween(360))) togetherWith
                                fadeOut(tween(220))
                        } else {
                            val forward = targetState.ordinal >= initialState.ordinal
                            val enterOffset: (Int) -> Int = { fullWidth ->
                                if (forward) fullWidth / 5 else -fullWidth / 5
                            }
                            val exitOffset: (Int) -> Int = { fullWidth ->
                                if (forward) -fullWidth / 5 else fullWidth / 5
                            }
                            (fadeIn(tween(260)) + slideInHorizontally(tween(280), enterOffset)) togetherWith
                                (fadeOut(tween(180)) + slideOutHorizontally(tween(220), exitOffset))
                        }
                    },
                    label = "onboardingStepTransition",
                ) { step ->
                    when (step) {
                        OnboardingStep.INTRO -> OnboardingIntroPhase(
                            reducedMotion = reducedMotion,
                            onFinished = { currentStep = OnboardingStep.WELCOME },
                        )

                        OnboardingStep.WELCOME -> OnboardingWelcomeStep(
                            onStart = { currentStep = OnboardingStep.REQUESTS },
                        )

                        OnboardingStep.REQUESTS -> OnboardingRequestsStep(
                            context = context,
                            permissionRevision = permissionRevision,
                            notificationBadgeRepository = notificationBadgeRepository,
                            onRequestCalendarPermission = onRequestCalendarPermission,
                            onRequestLocationPermission = onRequestLocationPermission,
                            onBack = { currentStep = OnboardingStep.WELCOME },
                            onNext = { currentStep = OnboardingStep.THEME },
                        )

                        OnboardingStep.THEME -> OnboardingThemeStep(
                            selectedStyle = currentVisualStyle,
                            reducedMotion = reducedMotion,
                            onSelectStyle = onSelectVisualStyle,
                            onBack = { currentStep = OnboardingStep.REQUESTS },
                            onFinish = {
                                if (isDebugPreview) onDismissPreview() else onComplete()
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OnboardingTopBar(
    step: OnboardingStep,
    isDebugPreview: Boolean,
    onDismissPreview: () -> Unit,
) {
    val colors = OverviewTheme.colors
    val shapes = OverviewTheme.shapes
    val activeIndex = when (step) {
        OnboardingStep.WELCOME -> 0
        OnboardingStep.REQUESTS -> 1
        OnboardingStep.THEME -> 2
        OnboardingStep.INTRO -> 0
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 540.dp)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        if (isDebugPreview) {
            Box(
                modifier = Modifier
                    .clip(shapes.pill)
                    .background(colors.accentContainer)
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            ) {
                Text(
                    text = stringResource(R.string.onboarding_preview_badge),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = colors.onAccentContainer,
                )
            }
        } else {
            Spacer(modifier = Modifier.width(36.dp))
        }

        // Indicador de progresso em 3 etapas
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            for (i in 0..2) {
                val isActive = i == activeIndex
                val widthDp by animateDpAsState(
                    targetValue = if (isActive) 22.dp else 7.dp,
                    animationSpec = tween(240),
                    label = "onboardingStepDot_$i",
                )
                Box(
                    modifier = Modifier
                        .size(width = widthDp, height = 7.dp)
                        .clip(CircleShape)
                        .background(
                            if (isActive) colors.accent else colors.onSurfaceMuted.copy(alpha = 0.28f),
                        ),
                )
            }
        }

        if (isDebugPreview) {
            IconButton(
                onClick = onDismissPreview,
                modifier = Modifier.size(36.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.settings_close_dialog),
                    tint = colors.onSurfaceMuted,
                )
            }
        } else {
            Spacer(modifier = Modifier.width(36.dp))
        }
    }
}

@Composable
private fun OnboardingIntroPhase(
    reducedMotion: Boolean,
    onFinished: () -> Unit,
) {
    val colors = OverviewTheme.colors
    val progress = remember { Animatable(0f) }

    LaunchedEffect(reducedMotion) {
        if (reducedMotion) {
            progress.snapTo(1f)
            delay(220L)
        } else {
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 780, easing = FastOutSlowInEasing),
            )
            delay(420L)
        }
        onFinished()
    }

    val scale = 0.84f + 0.16f * progress.value
    val alpha = progress.value.coerceIn(0f, 1f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onFinished,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.graphicsLayer {
                scaleX = scale
                scaleY = scale
                this.alpha = alpha
            },
        ) {
            OverviewAppIcon(size = 84.dp)
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = colors.onSurface,
            )
        }
    }
}

@Composable
private fun OnboardingWelcomeStep(
    onStart: () -> Unit,
) {
    val colors = OverviewTheme.colors
    val shapes = OverviewTheme.shapes

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Spacer(modifier = Modifier.height(1.dp))

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.padding(horizontal = 16.dp),
        ) {
            OverviewAppIcon(size = 76.dp)
            Text(
                text = stringResource(R.string.onboarding_welcome_title),
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = colors.onSurface,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(R.string.onboarding_welcome_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceMuted,
                textAlign = TextAlign.Center,
            )
        }

        Button(
            onClick = onStart,
            shape = shapes.pill,
            colors = ButtonDefaults.buttonColors(
                containerColor = colors.accent,
                contentColor = colors.onAccent,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
        ) {
            Text(
                text = stringResource(R.string.onboarding_start_cta),
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            )
        }
    }
}

@Composable
private fun OnboardingRequestsStep(
    context: Context,
    permissionRevision: Int,
    notificationBadgeRepository: NotificationBadgeRepository,
    onRequestCalendarPermission: () -> Unit,
    onRequestLocationPermission: () -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit,
) {
    val colors = OverviewTheme.colors
    val shapes = OverviewTheme.shapes
    val isLifecycleActive = rememberIsLifecycleActive()
    val scrollState = rememberScrollState()

    val hasNotificationAccess = remember(isLifecycleActive, permissionRevision) {
        notificationBadgeRepository.hasNotificationAccess()
    }
    val hasCalendarPerm = remember(isLifecycleActive, permissionRevision) {
        NativeWidgetSystemProviders.hasCalendarPermission(context)
    }
    val hasLocationPerm = remember(isLifecycleActive, permissionRevision) {
        NativeWidgetSystemProviders.hasCoarseLocationPermission(context)
    }
    val isDefaultHome = remember(isLifecycleActive, permissionRevision) {
        isDefaultHomeLauncher(context)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(vertical = 12.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = stringResource(R.string.onboarding_requests_title),
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = colors.onSurface,
                )
                Text(
                    text = stringResource(R.string.onboarding_requests_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceMuted,
                )
            }

            PermissionRequestCard(
                icon = Icons.Default.Notifications,
                title = stringResource(R.string.onboarding_req_notifications_title),
                description = stringResource(R.string.onboarding_req_notifications_desc),
                granted = hasNotificationAccess,
                onRequest = {
                    runCatching {
                        context.startActivity(notificationBadgeRepository.createAccessSettingsIntent())
                    }.onFailure {
                        runCatching {
                            context.startActivity(
                                Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                },
                            )
                        }
                    }
                },
            )

            PermissionRequestCard(
                icon = Icons.Default.DateRange,
                title = stringResource(R.string.onboarding_req_calendar_title),
                description = stringResource(R.string.onboarding_req_calendar_desc),
                granted = hasCalendarPerm,
                onRequest = onRequestCalendarPermission,
            )

            PermissionRequestCard(
                icon = Icons.Default.LocationOn,
                title = stringResource(R.string.onboarding_req_location_title),
                description = stringResource(R.string.onboarding_req_location_desc),
                granted = hasLocationPerm,
                onRequest = onRequestLocationPermission,
            )

            PermissionRequestCard(
                icon = Icons.Default.Home,
                title = stringResource(R.string.onboarding_req_default_home_title),
                description = stringResource(R.string.onboarding_req_default_home_desc),
                granted = isDefaultHome,
                onRequest = {
                    val homeSettings = Intent(Settings.ACTION_HOME_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    runCatching { context.startActivity(homeSettings) }.onFailure {
                        runCatching {
                            context.startActivity(
                                Intent(Settings.ACTION_SETTINGS).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                },
                            )
                        }
                    }
                },
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedButton(
                onClick = onBack,
                shape = shapes.pill,
                modifier = Modifier.height(46.dp),
            ) {
                Text(text = stringResource(R.string.onboarding_back))
            }
            Spacer(modifier = Modifier.weight(1f))
            TextButton(onClick = onNext) {
                Text(
                    text = stringResource(R.string.onboarding_skip),
                    color = colors.onSurfaceMuted,
                )
            }
            Button(
                onClick = onNext,
                shape = shapes.pill,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.accent,
                    contentColor = colors.onAccent,
                ),
                modifier = Modifier.height(46.dp),
            ) {
                Text(text = stringResource(R.string.onboarding_continue))
            }
        }
    }
}

@Composable
private fun PermissionRequestCard(
    icon: ImageVector,
    title: String,
    description: String,
    granted: Boolean,
    onRequest: () -> Unit,
) {
    val colors = OverviewTheme.colors
    val shapes = OverviewTheme.shapes
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shapes.large)
            .background(colors.surface.copy(alpha = 0.92f))
            .border(
                width = 1.dp,
                color = if (granted) colors.accent.copy(alpha = 0.45f) else colors.hairlineBorder,
                shape = shapes.large,
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(shapes.medium)
                .background(if (granted) colors.accentContainer else colors.surfaceElevated),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (granted) Icons.Default.Check else icon,
                contentDescription = null,
                tint = if (granted) colors.accent else colors.onSurfaceMuted,
                modifier = Modifier.size(19.dp),
            )
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = colors.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceMuted,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Box(
            modifier = Modifier
                .clip(shapes.pill)
                .background(if (granted) colors.accentContainer else colors.accent)
                .clickable(enabled = !granted, role = Role.Button, onClick = onRequest)
                .padding(horizontal = 12.dp, vertical = 7.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(
                    if (granted) R.string.onboarding_req_granted else R.string.onboarding_req_grant,
                ),
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = if (granted) colors.onAccentContainer else colors.onAccent,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun OnboardingThemeStep(
    selectedStyle: VisualStyle,
    reducedMotion: Boolean,
    onSelectStyle: (VisualStyle) -> Unit,
    onBack: () -> Unit,
    onFinish: () -> Unit,
) {
    val colors = OverviewTheme.colors
    val shapes = OverviewTheme.shapes
    val motion = OverviewTheme.motion
    val isExpressive = selectedStyle == VisualStyle.EXPRESSIVE
    val scrollState = rememberScrollState()

    // Animações de transição explícitas ao trocar entre GNOME e Material Expressive
    val previewCorner by animateDpAsState(
        targetValue = if (isExpressive) 26.dp else 14.dp,
        animationSpec = if (reducedMotion) snap() else motion.expressiveSpec(durationMillis = 360),
        label = "themePreviewCorner",
    )
    val previewInnerCorner by animateDpAsState(
        targetValue = if (isExpressive) 18.dp else 10.dp,
        animationSpec = if (reducedMotion) snap() else motion.expressiveSpec(durationMillis = 340),
        label = "themePreviewInnerCorner",
    )
    val previewPulse = remember { Animatable(1f) }
    val sweepProgress = remember { Animatable(0f) }

    LaunchedEffect(selectedStyle, reducedMotion) {
        if (reducedMotion) {
            previewPulse.snapTo(1f)
            sweepProgress.snapTo(1f)
            return@LaunchedEffect
        }
        previewPulse.snapTo(0.95f)
        previewPulse.animateTo(1f, animationSpec = motion.expressiveSpec())
        sweepProgress.snapTo(0f)
        sweepProgress.animateTo(1f, animationSpec = tween(durationMillis = 480, easing = FastOutSlowInEasing))
    }

    val scallopShape = remember { RoundedPolygonShape(OverviewPolygonFactory.clockScallopPolygon()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(vertical = 12.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = stringResource(R.string.onboarding_theme_title),
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = colors.onSurface,
                )
                Text(
                    text = stringResource(R.string.onboarding_theme_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceMuted,
                )
            }

            // Prévia animada ao vivo que se transforma entre GNOME e Material Expressive
            val previewShape = RoundedCornerShape(previewCorner)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        scaleX = previewPulse.value
                        scaleY = previewPulse.value
                    }
                    .clip(previewShape)
                    .background(
                        if (isExpressive) {
                            lerp(colors.surfaceContainerHigh, colors.accentContainer, 0.34f)
                        } else {
                            colors.surface
                        },
                    )
                    .border(
                        width = if (isExpressive) 1.5.dp else 1.dp,
                        color = if (isExpressive) colors.accent.copy(alpha = 0.48f) else colors.hairlineBorder,
                        shape = previewShape,
                    ),
            ) {
                // Onda sutil de troca de tema durante a transição
                if (!reducedMotion && sweepProgress.value < 0.99f) {
                    Canvas(modifier = Modifier.matchParentSize()) {
                        val radius = size.maxDimension * (0.25f + 0.95f * sweepProgress.value)
                        val waveAlpha = (1f - sweepProgress.value) * 0.24f
                        drawCircle(
                            color = colors.accent.copy(alpha = waveAlpha),
                            radius = radius,
                            center = Offset(size.width * 0.5f, size.height * 0.5f),
                        )
                    }
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
                            text = stringResource(R.string.onboarding_theme_preview_caption),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = if (isExpressive) colors.accent else colors.onSurfaceMuted,
                        )
                        Box(
                            modifier = Modifier
                                .clip(shapes.pill)
                                .background(if (isExpressive) colors.accentContainer else colors.surfaceElevated)
                                .padding(horizontal = 9.dp, vertical = 3.dp),
                        ) {
                            Text(
                                text = stringResource(
                                    if (isExpressive) {
                                        R.string.settings_visual_style_expressive
                                    } else {
                                        R.string.settings_visual_style_gnome
                                    },
                                ),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (isExpressive) colors.onAccentContainer else colors.onSurface,
                            )
                        }
                    }

                    // Mini relógio + clima animado
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column {
                            Text(
                                text = "09:41",
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = if (isExpressive) FontWeight.ExtraBold else FontWeight.SemiBold,
                                    letterSpacing = if (isExpressive) (-0.8).sp else 0.sp,
                                ),
                                color = colors.onSurface,
                            )
                            Text(
                                text = stringResource(R.string.onboarding_theme_preview_sample_meta),
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.onSurfaceMuted,
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(if (isExpressive) scallopShape else CircleShape)
                                .background(if (isExpressive) colors.accent else colors.surfaceElevated),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = if (isExpressive) colors.onAccent else colors.accent,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }

                    // Mini dock/card morfado
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(previewInnerCorner))
                            .background(colors.dashBackground)
                            .border(1.dp, colors.hairlineBorder, RoundedCornerShape(previewInnerCorner))
                            .padding(horizontal = 12.dp, vertical = 9.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        for (i in 0..3) {
                            val itemShape = if (isExpressive && i % 2 == 0) {
                                scallopShape
                            } else {
                                RoundedCornerShape(if (isExpressive) 12.dp else 8.dp)
                            }
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(itemShape)
                                    .background(
                                        if (i == 0) colors.accent else colors.accentContainer,
                                    ),
                            )
                        }
                    }
                }
            }

            // Opções GNOME e Material Expressive
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectableGroup(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                ThemeChoiceCard(
                    title = stringResource(R.string.settings_visual_style_gnome),
                    description = stringResource(R.string.onboarding_theme_gnome_desc),
                    selected = selectedStyle == VisualStyle.GNOME,
                    expressivePreview = false,
                    onClick = { onSelectStyle(VisualStyle.GNOME) },
                    modifier = Modifier.weight(1f),
                )
                ThemeChoiceCard(
                    title = stringResource(R.string.settings_visual_style_expressive),
                    description = stringResource(R.string.onboarding_theme_expressive_desc),
                    selected = selectedStyle == VisualStyle.EXPRESSIVE,
                    expressivePreview = true,
                    onClick = { onSelectStyle(VisualStyle.EXPRESSIVE) },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedButton(
                onClick = onBack,
                shape = shapes.pill,
                modifier = Modifier.height(46.dp),
            ) {
                Text(text = stringResource(R.string.onboarding_back))
            }
            Button(
                onClick = onFinish,
                shape = shapes.pill,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.accent,
                    contentColor = colors.onAccent,
                ),
                modifier = Modifier.height(46.dp),
            ) {
                Text(
                    text = stringResource(R.string.onboarding_finish),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                )
            }
        }
    }
}

@Composable
private fun ThemeChoiceCard(
    title: String,
    description: String,
    selected: Boolean,
    expressivePreview: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = OverviewTheme.colors
    val corner by animateDpAsState(
        targetValue = if (expressivePreview) 22.dp else 14.dp,
        animationSpec = tween(260),
        label = "choiceCorner_$expressivePreview",
    )
    val scale by animateFloatAsState(
        targetValue = if (selected) 1f else 0.97f,
        animationSpec = tween(220),
        label = "choiceScale_$expressivePreview",
    )
    val cardShape = RoundedCornerShape(corner)

    Column(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(cardShape)
            .background(
                if (selected) {
                    lerp(colors.surfaceContainerHigh, colors.accentContainer, 0.45f)
                } else {
                    colors.surface.copy(alpha = 0.85f)
                },
            )
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) colors.accent else colors.hairlineBorder,
                shape = cardShape,
            )
            .selectable(
                selected = selected,
                role = Role.RadioButton,
                onClick = onClick,
            )
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                ),
                color = colors.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(if (selected) colors.accent else Color.Transparent)
                    .border(
                        width = 1.5.dp,
                        color = if (selected) colors.accent else colors.onSurfaceMuted.copy(alpha = 0.45f),
                        shape = CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (selected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = colors.onAccent,
                        modifier = Modifier.size(12.dp),
                    )
                }
            }
        }
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = colors.onSurfaceMuted,
            minLines = 3,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun isDefaultHomeLauncher(context: Context): Boolean {
    val homeIntent = Intent(Intent.ACTION_MAIN).apply {
        addCategory(Intent.CATEGORY_HOME)
    }
    val resolved = runCatching {
        context.packageManager.resolveActivity(homeIntent, PackageManager.MATCH_DEFAULT_ONLY)
    }.getOrNull()
    return resolved?.activityInfo?.packageName == context.packageName
}
