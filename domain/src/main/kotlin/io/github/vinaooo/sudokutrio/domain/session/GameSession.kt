package io.github.vinaooo.sudokutrio.domain.session

import io.github.vinaooo.sudokutrio.domain.history.UndoHistory
import io.github.vinaooo.sudokutrio.domain.model.GameState
import io.github.vinaooo.sudokutrio.domain.model.Move
import io.github.vinaooo.sudokutrio.domain.rules.GameEngine
import io.github.vinaooo.sudokutrio.domain.rules.MoveOutcome
import kotlinx.serialization.Serializable

/** A game being played: the puzzle's seed, the current board and its undo history. This is what gets saved. */
@Serializable
data class GameSession(val seed: Long, val state: GameState, val history: UndoHistory = UndoHistory()) {
    val canUndo: Boolean get() = !state.isWon && history.canUndo

    val canRedo: Boolean get() = !state.isWon && history.canRedo

    val isInProgress: Boolean get() = state.moves > 0 && !state.isWon

    fun play(move: Move, engine: GameEngine): GameSession? = when (val outcome = engine.apply(state, move)) {
        // Only a move that changes the board can be undone: revealing a hint can't.
        is MoveOutcome.Applied -> copy(
            state = outcome.state,
            history = if (outcome.state.board != state.board) history.push(state) else history,
        )
        MoveOutcome.Rejected -> null
    }

    fun undo(): GameSession? = if (state.isWon) {
        null
    } else {
        history.undo(state)?.let { (restored, remaining) -> copy(state = restored, history = remaining) }
    }

    fun redo(): GameSession? = if (state.isWon) {
        null
    } else {
        history.redo(state)?.let { (replayed, remaining) -> copy(state = replayed, history = remaining) }
    }

    fun tick(elapsedSeconds: Long, engine: GameEngine): GameSession = copy(state = engine.tick(state, elapsedSeconds))
}
