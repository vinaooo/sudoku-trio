package io.github.vinaooo.sudokutrio.domain.history

import io.github.vinaooo.sudokutrio.domain.model.Board
import io.github.vinaooo.sudokutrio.domain.model.GameState
import kotlinx.serialization.Serializable

/**
 * Unlimited, immutable undo and redo stacks of board snapshots. Undo restores the previous digits and notes only:
 * the clock, the move count, the mistakes, the hints and the score keep their values, so undoing a mistake never
 * takes its penalty back, and undo itself costs nothing. Redo writes the undone board again and counts as a move,
 * but earns nothing and charges nothing. A new move clears the redo stack.
 */
@Serializable
data class UndoHistory(val undos: List<Board> = emptyList(), val redos: List<Board> = emptyList()) {
    val canUndo: Boolean get() = undos.isNotEmpty()

    val canRedo: Boolean get() = redos.isNotEmpty()

    fun push(before: GameState): UndoHistory = UndoHistory(undos + before.board)

    fun undo(current: GameState): Pair<GameState, UndoHistory>? {
        val previous = undos.lastOrNull() ?: return null
        return current.copy(board = previous) to UndoHistory(undos.dropLast(1), redos + current.board)
    }

    fun redo(current: GameState): Pair<GameState, UndoHistory>? {
        val next = redos.lastOrNull() ?: return null
        return current.copy(board = next, moves = current.moves + 1) to
            UndoHistory(undos + current.board, redos.dropLast(1))
    }
}
