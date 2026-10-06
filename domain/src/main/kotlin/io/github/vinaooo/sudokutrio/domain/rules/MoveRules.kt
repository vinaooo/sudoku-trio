package io.github.vinaooo.sudokutrio.domain.rules

import io.github.vinaooo.sudokutrio.domain.model.GameState
import io.github.vinaooo.sudokutrio.domain.model.Grid
import io.github.vinaooo.sudokutrio.domain.model.Move
import io.github.vinaooo.sudokutrio.domain.scoring.ScoreEvent

/** One rule per move type: each knows when its move is legal and how to perform it. */
internal interface MoveRule<M : Move> {
    fun isLegal(state: GameState, move: M): Boolean

    fun perform(state: GameState, move: M): Transition
}

/** A cell the player may write in: on the board and not a given. */
private fun GameState.isEditable(cell: Int) = cell in Grid.CELLS && !puzzle.isGiven(cell)

/**
 * Writing a digit clears the cell's notes and removes that digit from the notes of every cell it sees.
 * A digit that differs from the solution is a mistake.
 */
internal object PlaceRule : MoveRule<Move.Place> {
    override fun isLegal(state: GameState, move: Move.Place) =
        state.isEditable(move.cell) && move.digit in Grid.DIGITS && state.board.values[move.cell] != move.digit

    override fun perform(state: GameState, move: Move.Place): Transition {
        val peers = constraintsFor(state.mode.variant).units(state.puzzle).peersOf(move.cell)
        val board = state.board.withValue(move.cell, move.digit).withNotes(move.cell, emptySet())
            .withoutNote(peers, move.digit)
        val mistake = move.digit != state.puzzle.solution[move.cell]
        return Transition(
            state.copy(board = board, mistakes = state.mistakes + if (mistake) 1 else 0),
            listOfNotNull(ScoreEvent.Mistake.takeIf { mistake }),
        )
    }
}

internal object EraseRule : MoveRule<Move.Erase> {
    override fun isLegal(state: GameState, move: Move.Erase) = state.isEditable(move.cell) &&
        (state.board.values[move.cell] != 0 || state.board.notes[move.cell].isNotEmpty())

    override fun perform(state: GameState, move: Move.Erase) =
        Transition(state.copy(board = state.board.withValue(move.cell, 0).withNotes(move.cell, emptySet())))
}

/** Notes go in empty cells only. */
internal object ToggleNoteRule : MoveRule<Move.ToggleNote> {
    override fun isLegal(state: GameState, move: Move.ToggleNote) =
        state.isEditable(move.cell) && move.digit in Grid.DIGITS && state.board.values[move.cell] == 0

    override fun perform(state: GameState, move: Move.ToggleNote): Transition {
        val notes = state.board.notes[move.cell]
        val toggled = if (move.digit in notes) notes - move.digit else notes + move.digit
        return Transition(state.copy(board = state.board.withNotes(move.cell, toggled)))
    }
}
