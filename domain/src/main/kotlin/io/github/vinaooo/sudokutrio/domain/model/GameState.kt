package io.github.vinaooo.sudokutrio.domain.model

import kotlinx.serialization.Serializable

/** Immutable snapshot of a Sudoku game. */
@Serializable
data class GameState(
    val puzzle: Puzzle,
    val board: Board,
    val mode: GameMode,
    val score: Int = 0,
    /** Digits placed that differ from the solution. Counted silently for the score; undo never takes one back. */
    val mistakes: Int = 0,
    val hintsUsed: Int = 0,
    val moves: Int = 0,
    val elapsedSeconds: Long = 0,
    /** The hint on show, until it is carried out or another move or undo makes it stale. */
    val pendingHint: Hint? = null,
) {
    val isWon: Boolean
        get() = board.values == puzzle.solution
}
