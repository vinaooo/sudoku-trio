package io.github.vinaooo.sudokutrio.feature.game.board

import io.github.vinaooo.sudokutrio.domain.model.Grid

/**
 * Where the board sits in a [width] × [height] area: the largest square that fits, centered across and at the top,
 * split into 9 × 9 cells. Pure, so layout and hit-testing are tested without Compose.
 */
class BoardGeometry(width: Float, height: Float) {
    val side: Float = minOf(width, height)
    val left: Float = (width - side) / 2
    val top: Float = 0f
    val cellSize: Float = side / Grid.SIDE

    /** The cell under a touch at ([x], [y]), or null off the board. */
    fun cellAt(x: Float, y: Float): Int? {
        val column = ((x - left) / cellSize).toInt()
        val row = ((y - top) / cellSize).toInt()
        val inside = x >= left && y >= top && column in 0 until Grid.SIDE && row in 0 until Grid.SIDE
        return if (inside) Grid.cell(row, column) else null
    }

    fun cellLeft(cell: Int): Float = left + Grid.column(cell) * cellSize

    fun cellTop(cell: Int): Float = top + Grid.row(cell) * cellSize
}
