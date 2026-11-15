package io.github.vimitt.overview.ui.components

import android.graphics.Matrix
import android.graphics.Path as AndroidPath
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.circle
import androidx.graphics.shapes.star
import androidx.graphics.shapes.toPath
import io.github.vimitt.overview.domain.model.IconShape

/**
 * Fábrica de polígonos normalizados (coordenadas em `[0, 1]`) e utilitários de
 * *shape morphing* usando `androidx.graphics:graphics-shapes`.
 */
object OverviewPolygonFactory {

    private val unitSquareVertices = floatArrayOf(
        -1f, -1f,
        1f, -1f,
        1f, 1f,
        -1f, 1f,
    )

    /**
     * Constrói o [RoundedPolygon] normalizado para a forma de ícone e o fator de arredondamento (`0f..1f`).
     */
    fun iconPolygon(
        shape: IconShape,
        roundness: Float = 0.5f,
    ): RoundedPolygon {
        val t = roundness.coerceIn(0f, 1f)
        return when (shape) {
            IconShape.CIRCLE -> RoundedPolygon.circle(numVertices = 12).normalized()

            IconShape.ROUNDED_SQUARE -> RoundedPolygon(
                vertices = unitSquareVertices,
                rounding = CornerRounding(radius = 0.22f + 0.44f * t, smoothing = 0f),
            ).normalized()

            IconShape.SQUIRCLE -> RoundedPolygon(
                vertices = unitSquareVertices,
                rounding = CornerRounding(radius = 0.34f + 0.42f * t, smoothing = 0.85f),
            ).normalized()

            IconShape.SQUARE -> RoundedPolygon(
                vertices = unitSquareVertices,
                rounding = CornerRounding(radius = 0.06f + 0.14f * t, smoothing = 0f),
            ).normalized()

            IconShape.CLOVER -> RoundedPolygon.star(
                numVerticesPerRadius = 4,
                radius = 1f,
                innerRadius = 0.74f - 0.10f * t,
                rounding = CornerRounding(radius = 0.34f + 0.28f * t, smoothing = 0.65f),
                innerRounding = CornerRounding(radius = 0.26f, smoothing = 0.65f),
            ).normalized()

            IconShape.FLOWER -> RoundedPolygon.star(
                numVerticesPerRadius = 8,
                radius = 1f,
                innerRadius = 0.84f - 0.08f * t,
                rounding = CornerRounding(radius = 0.26f + 0.20f * t, smoothing = 0.55f),
                innerRounding = CornerRounding(radius = 0.18f, smoothing = 0.55f),
            ).normalized()
        }
    }

    /**
     * Forma de destino para o estado pressionado/selecionado no *morphing* expressivo.
     */
    fun pressedTargetPolygon(
        shape: IconShape,
        roundness: Float = 0.5f,
    ): RoundedPolygon = when (shape) {
        IconShape.CIRCLE -> iconPolygon(IconShape.SQUIRCLE, roundness = 0.85f)
        IconShape.ROUNDED_SQUARE -> iconPolygon(IconShape.CIRCLE, roundness)
        IconShape.SQUIRCLE -> iconPolygon(IconShape.CIRCLE, roundness)
        IconShape.SQUARE -> iconPolygon(IconShape.ROUNDED_SQUARE, roundness = 0.85f)
        IconShape.CLOVER -> iconPolygon(IconShape.CIRCLE, roundness)
        IconShape.FLOWER -> iconPolygon(IconShape.CIRCLE, roundness)
    }

    /**
     * Polígonos expressivos para o indicador de carregamento com *morphing* contínuo.
     */
    fun loadingPolygons(): List<RoundedPolygon> = listOf(
        RoundedPolygon.circle(numVertices = 8).normalized(),
        RoundedPolygon.star(
            numVerticesPerRadius = 4,
            radius = 1f,
            innerRadius = 0.66f,
            rounding = CornerRounding(radius = 0.36f, smoothing = 0.7f),
            innerRounding = CornerRounding(radius = 0.28f, smoothing = 0.7f),
        ).normalized(),
        RoundedPolygon(
            vertices = unitSquareVertices,
            rounding = CornerRounding(radius = 0.48f, smoothing = 0.85f),
        ).normalized(),
        RoundedPolygon.star(
            numVerticesPerRadius = 8,
            radius = 1f,
            innerRadius = 0.78f,
            rounding = CornerRounding(radius = 0.28f, smoothing = 0.6f),
            innerRounding = CornerRounding(radius = 0.22f, smoothing = 0.6f),
        ).normalized(),
    )

    /**
     * Preenche um [android.graphics.Path] já alocado com a forma escalada para `sizePx × sizePx`,
     * evitando alocações durante renderização de bitmaps fora da thread principal.
     */
    fun fillScaledAndroidPath(
        polygon: RoundedPolygon,
        sizePx: Float,
        outPath: AndroidPath,
        workMatrix: Matrix,
    ) {
        outPath.rewind()
        polygon.toPath(outPath)
        workMatrix.reset()
        workMatrix.setScale(sizePx, sizePx)
        outPath.transform(workMatrix)
    }

    /**
     * Mostrador recortado de 12 lóbulos inspirado no relógio analógico Material 3 Expressive.
     */
    fun clockScallopPolygon(): RoundedPolygon = RoundedPolygon.star(
        numVerticesPerRadius = 12,
        radius = 1f,
        innerRadius = 0.89f,
        rounding = CornerRounding(radius = 0.45f, smoothing = 0.7f),
        innerRounding = CornerRounding(radius = 0.34f, smoothing = 0.7f),
    ).normalized()
}

/**
 * [Shape] estática baseada em [RoundedPolygon] normalizado, reutilizando buffers de `Path` e `Matrix`.
 */
class RoundedPolygonShape(
    private val polygon: RoundedPolygon,
) : Shape {
    private val androidPath = AndroidPath()
    private val matrix = Matrix()
    private val composePath = Path()

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        androidPath.rewind()
        polygon.toPath(androidPath)
        matrix.reset()
        matrix.setScale(size.width, size.height)
        androidPath.transform(matrix)

        val backingPath = composePath.asAndroidPath()
        backingPath.set(androidPath)
        return Outline.Generic(composePath)
    }
}

/**
 * [Shape] animável que interpola entre duas formas via [Morph] sem alocar objetos a cada frame.
 */
class MorphPolygonShape(
    private val morph: Morph,
    private val progressProvider: () -> Float,
) : Shape {
    private val androidPath = AndroidPath()
    private val matrix = Matrix()
    private val composePath = Path()

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val progress = progressProvider().coerceIn(0f, 1f)
        androidPath.rewind()
        morph.toPath(progress, androidPath)
        matrix.reset()
        matrix.setScale(size.width, size.height)
        androidPath.transform(matrix)

        val backingPath = composePath.asAndroidPath()
        backingPath.set(androidPath)
        return Outline.Generic(composePath)
    }
}
