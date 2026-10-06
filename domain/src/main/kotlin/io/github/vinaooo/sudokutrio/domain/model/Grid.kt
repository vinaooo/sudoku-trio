package io.github.vinaooo.sudokutrio.domain.model

/** The 9×9 board's dimensions. Cells are numbered row by row, 0 to 80. */
object Grid {
    const val SIDE = 9
    const val BOX = 3
    const val SIZE = SIDE * SIDE
    val CELLS = 0 until SIZE
    val DIGITS = 1..SIDE

    fun row(cell: Int): Int = cell / SIDE

    fun column(cell: Int): Int = cell % SIDE

    fun cell(row: Int, column: Int): Int = row * SIDE + column
}
