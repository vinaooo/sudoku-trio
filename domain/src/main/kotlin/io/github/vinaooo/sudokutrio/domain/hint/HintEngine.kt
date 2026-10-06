package io.github.vinaooo.sudokutrio.domain.hint

import io.github.vinaooo.sudokutrio.domain.model.GameState
import io.github.vinaooo.sudokutrio.domain.model.Hint
import io.github.vinaooo.sudokutrio.domain.solver.LogicalSolver

/**
 * The next logical step for the player: a wrong digit first, since nothing sound follows from it; otherwise the
 * next digit the techniques find from the digits on the board (never the player's notes). A board the techniques
 * can't crack, such as a replayed bug report, gets a cell revealed from the solution instead.
 */
class HintEngine(private val solver: LogicalSolver = LogicalSolver()) {
    /** Null when the game is won. */
    fun hintFor(state: GameState): Hint? {
        if (state.isWon) return null
        val values = state.board.values
        val solution = state.puzzle.solution
        values.indices.firstOrNull { values[it] != 0 && values[it] != solution[it] }?.let { return Hint.WrongDigit(it) }
        solver.nextPlacement(state.puzzle, state.mode.variant, values)?.let { step ->
            return Hint.Placement(step.cell, step.digit, step.technique, step.cells)
        }
        val cell = values.indexOfFirst { it == 0 }
        return Hint.Placement(cell, solution[cell], technique = null, cells = listOf(cell))
    }
}
