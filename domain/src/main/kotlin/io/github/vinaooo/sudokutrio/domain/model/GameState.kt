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
) {
    val isWon: Boolean
        get() = board.values == puzzle.solution
}
