package io.github.vinaooo.sudokutrio.feature.game.board

import io.github.vinaooo.sudokutrio.domain.model.Grid
import io.github.vinaooo.sudokutrio.domain.model.Puzzle
import io.github.vinaooo.sudokutrio.domain.model.Variant
import io.github.vinaooo.sudokutrio.domain.rules.constraintsFor
import io.github.vinaooo.sudokutrio.domain.rules.peersOf
import io.github.vinaooo.sudokutrio.domain.rules.units

/** How a cell is lit, strongest last: a conflict shows over everything. */
enum class CellHighlight { NONE, PEER, SAME_DIGIT, HINT, SELECTED, CONFLICT }

/** Lights a board around its selected cell. Which cells see each other is worked out once per puzzle. */
class CellHighlighter(puzzle: Puzzle, variant: Variant) {
    private val peers: List<Set<Int>> = constraintsFor(variant).units(puzzle).let { units ->
        Grid.CELLS.map { units.peersOf(it) }
    }

    /**
     * The highlight of each cell of a board with [values]: the [selected] cell, the cells it sees (its row, column,
     * box, and diagonals in X or cage in Killer), the cells holding its digit, the [hintCells] and the [conflicts].
     */
    fun highlights(
        values: List<Int>,
        selected: Int?,
        hintCells: Collection<Int> = emptyList(),
        conflicts: Set<Int> = emptySet(),
    ): List<CellHighlight> {
        val digit = selected?.let { values[it] } ?: 0
        val seen = selected?.let { peers[it] }.orEmpty()
        return Grid.CELLS.map { cell ->
            when {
                cell in conflicts -> CellHighlight.CONFLICT
                cell == selected -> CellHighlight.SELECTED
                cell in hintCells -> CellHighlight.HINT
                digit != 0 && values[cell] == digit -> CellHighlight.SAME_DIGIT
                cell in seen -> CellHighlight.PEER
                else -> CellHighlight.NONE
            }
        }
    }
}

/** The digits on the board nine times, wrong ones included: the number pad dims them. */
fun completedDigits(values: List<Int>): Set<Int> =
    values.filter { it != 0 }.groupingBy { it }.eachCount().filterValues { it >= Grid.SIDE }.keys
