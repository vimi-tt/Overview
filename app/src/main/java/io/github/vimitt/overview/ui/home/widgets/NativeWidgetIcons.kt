package io.github.vimitt.overview.ui.home.widgets

import androidx.annotation.StringRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.vimitt.overview.R
import kotlin.math.cos
import kotlin.math.sin

/**
 * Mapeia o código WMO da API Open-Meteo para descrição legível em português.
 */
@StringRes
fun wmoWeatherDescriptionRes(code: Int): Int = when (code) {
    0 -> R.string.native_widget_weather_cond_clear
    1, 2 -> R.string.native_widget_weather_cond_partly_cloudy
    3 -> R.string.native_widget_weather_cond_cloudy
    45, 48 -> R.string.native_widget_weather_cond_fog
    51, 53, 55, 56, 57 -> R.string.native_widget_weather_cond_drizzle
    61, 63, 65, 66, 67, 80, 81, 82 -> R.string.native_widget_weather_cond_rain
    71, 73, 75, 77, 85, 86 -> R.string.native_widget_weather_cond_snow
    95, 96, 99 -> R.string.native_widget_weather_cond_storm
    else -> R.string.native_widget_weather_cond_cloudy
}

/**
 * Ícone vetorial leve de clima desenhado via Canvas sem alocar recursos externos.
 */
@Composable
fun WeatherGlyph(
    weatherCode: Int,
    isDay: Boolean,
    tint: Color,
    accent: Color,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = (w * 0.09f).coerceAtLeast(1.8f)

        when (weatherCode) {
            0 -> {
                if (isDay) {
                    val center = Offset(w * 0.5f, h * 0.5f)
                    drawCircle(color = accent, radius = w * 0.24f, center = center)
                    for (i in 0 until 8) {
                        val rad = Math.toRadians(i * 45.0)
                        val r1 = w * 0.34f
                        val r2 = w * 0.44f
                        drawLine(
                            color = accent,
                            start = Offset(
                                center.x + (cos(rad) * r1).toFloat(),
                                center.y + (sin(rad) * r1).toFloat(),
                            ),
                            end = Offset(
                                center.x + (cos(rad) * r2).toFloat(),
                                center.y + (sin(rad) * r2).toFloat(),
                            ),
                            strokeWidth = stroke,
                            cap = StrokeCap.Round,
                        )
                    }
                } else {
                    drawCircle(
                        color = accent,
                        radius = w * 0.30f,
                        center = Offset(w * 0.48f, h * 0.50f),
                        style = Stroke(width = stroke * 1.4f),
                    )
                }
            }

            1, 2 -> {
                drawCircle(color = accent, radius = w * 0.20f, center = Offset(w * 0.66f, h * 0.34f))
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(w * 0.16f, h * 0.42f),
                    size = Size(w * 0.62f, h * 0.34f),
                    cornerRadius = CornerRadius(w * 0.17f, w * 0.17f),
                )
                drawCircle(color = tint, radius = w * 0.17f, center = Offset(w * 0.40f, h * 0.42f))
            }

            51, 53, 55, 56, 57, 61, 63, 65, 66, 67, 80, 81, 82, 95, 96, 99 -> {
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(w * 0.16f, h * 0.28f),
                    size = Size(w * 0.68f, h * 0.32f),
                    cornerRadius = CornerRadius(w * 0.16f, w * 0.16f),
                )
                drawCircle(color = tint, radius = w * 0.18f, center = Offset(w * 0.44f, h * 0.28f))
                val dropColor = if (weatherCode >= 95) accent else accent.copy(alpha = 0.92f)
                for (i in 0..2) {
                    val x = w * (0.30f + i * 0.20f)
                    drawLine(
                        color = dropColor,
                        start = Offset(x, h * 0.68f),
                        end = Offset(x - w * 0.05f, h * 0.85f),
                        strokeWidth = stroke,
                        cap = StrokeCap.Round,
                    )
                }
            }

            else -> {
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(w * 0.14f, h * 0.38f),
                    size = Size(w * 0.72f, h * 0.36f),
                    cornerRadius = CornerRadius(w * 0.18f, w * 0.18f),
                )
                drawCircle(color = tint, radius = w * 0.20f, center = Offset(w * 0.42f, h * 0.38f))
                drawCircle(color = tint, radius = w * 0.15f, center = Offset(w * 0.62f, h * 0.40f))
            }
        }
    }
}

@Composable
fun PlayPauseGlyph(
    isPlaying: Boolean,
    tint: Color,
    modifier: Modifier = Modifier,
    size: Dp = 20.dp,
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        if (isPlaying) {
            val barW = w * 0.22f
            drawRoundRect(
                color = tint,
                topLeft = Offset(w * 0.22f, h * 0.18f),
                size = Size(barW, h * 0.64f),
                cornerRadius = CornerRadius(w * 0.08f, w * 0.08f),
            )
            drawRoundRect(
                color = tint,
                topLeft = Offset(w * 0.56f, h * 0.18f),
                size = Size(barW, h * 0.64f),
                cornerRadius = CornerRadius(w * 0.08f, w * 0.08f),
            )
        } else {
            val path = Path().apply {
                moveTo(w * 0.28f, h * 0.18f)
                lineTo(w * 0.82f, h * 0.50f)
                lineTo(w * 0.28f, h * 0.82f)
                close()
            }
            drawPath(path = path, color = tint)
        }
    }
}

@Composable
fun SkipTrackGlyph(
    forward: Boolean,
    tint: Color,
    modifier: Modifier = Modifier,
    size: Dp = 18.dp,
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val path = Path().apply {
            if (forward) {
                moveTo(w * 0.22f, h * 0.22f)
                lineTo(w * 0.66f, h * 0.50f)
                lineTo(w * 0.22f, h * 0.78f)
                close()
            } else {
                moveTo(w * 0.78f, h * 0.22f)
                lineTo(w * 0.34f, h * 0.50f)
                lineTo(w * 0.78f, h * 0.78f)
                close()
            }
        }
        drawPath(path = path, color = tint)
        drawRoundRect(
            color = tint,
            topLeft = Offset(if (forward) w * 0.72f else w * 0.18f, h * 0.22f),
            size = Size(w * 0.10f, h * 0.56f),
            cornerRadius = CornerRadius(w * 0.05f, w * 0.05f),
        )
    }
}

@Composable
fun AlarmBellGlyph(
    tint: Color,
    modifier: Modifier = Modifier,
    size: Dp = 16.dp,
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = (w * 0.11f).coerceAtLeast(1.5f)
        drawCircle(
            color = tint,
            radius = w * 0.34f,
            center = Offset(w * 0.5f, h * 0.54f),
            style = Stroke(width = stroke),
        )
        drawLine(
            color = tint,
            start = Offset(w * 0.5f, h * 0.54f),
            end = Offset(w * 0.5f, h * 0.36f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = tint,
            start = Offset(w * 0.5f, h * 0.54f),
            end = Offset(w * 0.66f, h * 0.60f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = tint,
            start = Offset(w * 0.18f, h * 0.24f),
            end = Offset(w * 0.30f, h * 0.14f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = tint,
            start = Offset(w * 0.82f, h * 0.24f),
            end = Offset(w * 0.70f, h * 0.14f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
    }
}
