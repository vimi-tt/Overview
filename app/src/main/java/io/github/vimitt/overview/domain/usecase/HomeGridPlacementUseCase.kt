package io.github.vimitt.overview.domain.usecase

import io.github.vimitt.overview.domain.model.HomeGridSpec
import io.github.vimitt.overview.domain.model.HomeItem

/**
 * Posição validada na grade da tela inicial.
 */
data class GridPlacement(
    val cellX: Int,
    val cellY: Int,
    val spanX: Int,
    val spanY: Int,
)

/**
 * Regras puras de posicionamento, colisão e redimensionamento na grade da tela inicial.
 */
class HomeGridPlacementUseCase(
    private val columns: Int = HomeGridSpec.COLUMNS,
    private val rows: Int = HomeGridSpec.ROWS,
) {
    /**
     * Encontra a primeira posição livre capaz de acomodar um item de tamanho [desiredSpanX] x [desiredSpanY].
     * Se não houver espaço exato para um widget grande, reduz o tamanho progressivamente até `1x1`
     * se necessário.
     */
    fun findAvailableSlot(
        existingItems: List<HomeItem>,
        desiredSpanX: Int,
        desiredSpanY: Int,
        preferredCellX: Int? = null,
        preferredCellY: Int? = null,
        columns: Int = this.columns,
        rows: Int = this.rows,
    ): GridPlacement? {
        val clampedX = desiredSpanX.coerceIn(1, columns)
        val clampedY = desiredSpanY.coerceIn(1, rows)

        if (preferredCellX != null && preferredCellY != null) {
            if (canPlace(existingItems, preferredCellX, preferredCellY, clampedX, clampedY, columns = columns, rows = rows)) {
                return GridPlacement(preferredCellX, preferredCellY, clampedX, clampedY)
            }
        }

        // Tenta primeiro no tamanho desejado e depois reduz caso a grade esteja quase cheia.
        val candidateSpans = buildList {
            add(clampedX to clampedY)
            if (clampedX > 1 || clampedY > 1) {
                for (sy in clampedY downTo 1) {
                    for (sx in clampedX downTo 1) {
                        if (sx != clampedX || sy != clampedY) {
                            add(sx to sy)
                        }
                    }
                }
            }
        }

        for ((sx, sy) in candidateSpans) {
            for (y in 0..(rows - sy)) {
                for (x in 0..(columns - sx)) {
                    if (canPlace(existingItems, x, y, sx, sy, columns = columns, rows = rows)) {
                        return GridPlacement(x, y, sx, sy)
                    }
                }
            }
        }
        return null
    }

    /**
     * Verifica se um retângulo [cellX], [cellY], [spanX] x [spanY] cabe na grade sem colisões,
     * ignorando opcionalmente [ignoreItemId].
     */
    fun canPlace(
        existingItems: List<HomeItem>,
        cellX: Int,
        cellY: Int,
        spanX: Int,
        spanY: Int,
        ignoreItemId: String? = null,
        columns: Int = this.columns,
        rows: Int = this.rows,
    ): Boolean {
        if (cellX < 0 || cellY < 0 || spanX < 1 || spanY < 1) return false
        if (cellX + spanX > columns || cellY + spanY > rows) return false

        val right = cellX + spanX
        val bottom = cellY + spanY

        for (item in existingItems) {
            if (item.id == ignoreItemId) continue
            val itemRight = item.cellX + item.spanX
            val itemBottom = item.cellY + item.spanY
            val overlaps = cellX < itemRight &&
                right > item.cellX &&
                cellY < itemBottom &&
                bottom > item.cellY
            if (overlaps) return false
        }
        return true
    }
}
