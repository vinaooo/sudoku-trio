package io.github.vinaooo.sudokutrio.domain.model

import kotlinx.serialization.Serializable

/** A group of cells whose digits add up to [sum] and never repeat (Killer Sudoku). */
@Serializable
data class Cage(val sum: Int, val cells: List<Int>) {
    init {
        require(cells.isNotEmpty() && cells.all { it in Grid.CELLS }) { "A cage needs cells on the board." }
    }
}

/**
 * A board to solve: [givens] (0 for an empty cell) and the unique [solution], both row by row, plus the
 * [cages] of a Killer Sudoku.
 */
@Serializable
data class Puzzle(val givens: List<Int>, val solution: List<Int>, val cages: List<Cage> = emptyList()) {
    init {
        require(givens.size == Grid.SIZE && solution.size == Grid.SIZE) { "A puzzle has ${Grid.SIZE} cells." }
        require(solution.all { it in Grid.DIGITS }) { "The solution fills every cell with 1–9." }
        require(givens.indices.all { givens[it] == 0 || givens[it] == solution[it] }) {
            "Every given matches the solution."
        }
    }

    fun isGiven(cell: Int): Boolean = givens[cell] != 0
}
