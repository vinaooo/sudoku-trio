package io.github.vinaooo.sudokutrio.domain.rules

import io.github.vinaooo.sudokutrio.domain.model.GameState
import io.github.vinaooo.sudokutrio.domain.model.Grid
import io.github.vinaooo.sudokutrio.domain.model.Move

/** The rules of every variant: the variant only changes which cells see each other ([constraintsFor]). */
class SudokuRules : RuleSet {
    override fun isLegal(state: GameState, move: Move): Boolean = !state.isWon &&
        when (move) {
            is Move.Place -> PlaceRule.isLegal(state, move)
            is Move.Erase -> EraseRule.isLegal(state, move)
            is Move.ToggleNote -> ToggleNoteRule.isLegal(state, move)
        }

    override fun perform(state: GameState, move: Move): Transition = when (move) {
        is Move.Place -> PlaceRule.perform(state, move)
        is Move.Erase -> EraseRule.perform(state, move)
        is Move.ToggleNote -> ToggleNoteRule.perform(state, move)
    }

    override fun legalMoves(state: GameState): List<Move> = Grid.CELLS.flatMap { cell ->
        Grid.DIGITS.flatMap { digit -> listOf(Move.Place(cell, digit), Move.ToggleNote(cell, digit)) } +
            Move.Erase(cell)
    }.filter { isLegal(state, it) }
}
