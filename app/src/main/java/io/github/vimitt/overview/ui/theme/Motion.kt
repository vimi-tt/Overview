package io.github.vimitt.overview.ui.theme

import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import io.github.vimitt.overview.domain.model.MotionIntensity
import io.github.vimitt.overview.domain.model.VisualStyle
import kotlin.math.roundToInt

/**
 * Indica se o usuário ativou animações reduzidas nas configurações do sistema.
 */
val LocalReducedMotion = staticCompositionLocalOf { false }

/**
 * Tokens centralizados de movimento do tema (tweens curtos no GNOME, molas no Expressive).
 */
@Immutable
data class OverviewMotionTokens(
    val visualStyle: VisualStyle,
    val intensity: MotionIntensity,
    val reducedMotion: Boolean,
    val usesSprings: Boolean,
    val spatialDampingRatio: Float,
    val emphasizedDampingRatio: Float,
    val spatialStiffness: Float,
    val fastStiffness: Float,
) {
    /**
     * Especificação espacial principal (abertura de overview, pastas, cards, dialogs).
     */
    fun <T> spatialSpec(
        durationMillis: Int = 200,
        durationScale: Float = 1f,
        visibilityThreshold: T? = null,
    ): FiniteAnimationSpec<T> = when {
        reducedMotion -> snap()
        usesSprings -> spring(
            dampingRatio = spatialDampingRatio,
            stiffness = spatialStiffness / durationScale.coerceIn(0.6f, 1.6f),
            visibilityThreshold = visibilityThreshold,
        )
        else -> tween(durationMillis = (durationMillis * durationScale).roundToInt().coerceAtLeast(1))
    }

    /**
     * Especificação espacial com mais destaque (feedback de toque, morphing de estado, drop).
     */
    fun <T> expressiveSpec(
        durationMillis: Int = 160,
        durationScale: Float = 1f,
        visibilityThreshold: T? = null,
    ): FiniteAnimationSpec<T> = when {
        reducedMotion -> snap()
        usesSprings -> spring(
            dampingRatio = emphasizedDampingRatio,
            stiffness = fastStiffness / durationScale.coerceIn(0.6f, 1.6f),
            visibilityThreshold = visibilityThreshold,
        )
        else -> tween(durationMillis = (durationMillis * durationScale).roundToInt().coerceAtLeast(1))
    }

    /**
     * Especificação rápida para efeitos não-espaciais (opacidade/cor, sempre sem overshoot).
     */
    fun <T> effectSpec(
        durationMillis: Int = 160,
        durationScale: Float = 1f,
    ): FiniteAnimationSpec<T> = if (reducedMotion) {
        snap()
    } else {
        tween(durationMillis = (durationMillis * durationScale).roundToInt().coerceAtLeast(1))
    }

    /**
     * Especificação para indicadores/pílulas de seleção sem bounce exagerado.
     */
    fun <T> indicatorSpec(
        visibilityThreshold: T? = null,
    ): FiniteAnimationSpec<T> = when {
        reducedMotion -> snap()
        usesSprings -> spring(
            dampingRatio = spatialDampingRatio,
            stiffness = fastStiffness,
            visibilityThreshold = visibilityThreshold,
        )
        else -> spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow,
            visibilityThreshold = visibilityThreshold,
        )
    }
}

fun overviewMotionTokens(
    visualStyle: VisualStyle = VisualStyle.GNOME,
    intensity: MotionIntensity = MotionIntensity.MEDIUM,
    reducedMotion: Boolean = false,
): OverviewMotionTokens {
    val usesSprings = visualStyle == VisualStyle.EXPRESSIVE && !reducedMotion
    return OverviewMotionTokens(
        visualStyle = visualStyle,
        intensity = intensity,
        reducedMotion = reducedMotion,
        usesSprings = usesSprings,
        spatialDampingRatio = intensity.spatialDampingRatio,
        emphasizedDampingRatio = intensity.emphasizedDampingRatio,
        spatialStiffness = intensity.spatialStiffness,
        fastStiffness = intensity.fastStiffness,
    )
}

/**
 * Observa a configuração global `ANIMATOR_DURATION_SCALE` do Android em tempo real.
 * Quando a escala é zero (ex.: "Remover animações" nas opções de acessibilidade),
 * retorna `true` para desativar transições espaciais e reduzir movimento.
 */
@Composable
fun rememberReducedMotion(): Boolean {
    val context = LocalContext.current
    var reducedMotion by remember(context) {
        mutableStateOf(isReducedMotionEnabled(context))
    }

    DisposableEffect(context) {
        val resolver = context.contentResolver
        val uri = Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE)
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                reducedMotion = isReducedMotionEnabled(context)
            }
        }
        resolver.registerContentObserver(uri, false, observer)
        onDispose {
            resolver.unregisterContentObserver(observer)
        }
    }

    return reducedMotion
}

fun isReducedMotionEnabled(context: Context): Boolean {
    val scale = Settings.Global.getFloat(
        context.contentResolver,
        Settings.Global.ANIMATOR_DURATION_SCALE,
        1f,
    )
    return scale <= 0f
}

/**
 * Especificação de animação que respeita o modo de movimento reduzido.
 */
fun <T> overviewSpring(
    reducedMotion: Boolean,
    dampingRatio: Float = Spring.DampingRatioNoBouncy,
    stiffness: Float = Spring.StiffnessMediumLow,
): FiniteAnimationSpec<T> = if (reducedMotion) {
    snap()
} else {
    spring(dampingRatio = dampingRatio, stiffness = stiffness)
}

fun <T> overviewTween(
    reducedMotion: Boolean,
    durationMillis: Int = 220,
    durationScale: Float = 1f,
): FiniteAnimationSpec<T> = if (reducedMotion) {
    snap()
} else {
    tween(durationMillis = (durationMillis * durationScale).roundToInt().coerceAtLeast(1))
}
