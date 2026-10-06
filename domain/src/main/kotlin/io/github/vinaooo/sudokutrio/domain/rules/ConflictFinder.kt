package io.github.vinaooo.sudokutrio.domain.rules

import io.github.vinaooo.sudokutrio.domain.model.GameState

/**
 * The cells to highlight as conflicting: a digit repeated in one of the variant's groups, and every cell of a cage
 * whose digits add up to more than its sum, or to a different sum once it is full.
 * Only what the player can see counts: a wrong digit with no visible clash is not a conflict.
 */
class ConflictFinder {
    fun conflicts(state: GameState): Set<Int> {
        val values = state.board.values
        val repeated = constraintsFor(state.mode.variant).units(state.puzzle).flatMap { unit ->
            unit.filter { cell -> values[cell] != 0 && unit.count { values[it] == values[cell] } > 1 }
        }
        val badCages = state.puzzle.cages.filter { cage ->
            val digits = cage.cells.map { values[it] }
            val total = digits.sum()
            total > cage.sum || (digits.none { it == 0 } && total != cage.sum)
        }
        return (repeated + badCages.flatMap { it.cells }).toSet()
    }
}
