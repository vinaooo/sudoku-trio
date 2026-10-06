package io.github.vinaooo.sudokutrio.domain.rules

import io.github.vinaooo.sudokutrio.domain.hint.HintEngine
import io.github.vinaooo.sudokutrio.domain.model.GameState
import io.github.vinaooo.sudokutrio.domain.model.Grid
import io.github.vinaooo.sudokutrio.domain.model.Move

/**
 * The rules of every variant: the variant only changes which cells see each other ([constraintsFor]). Any move but
 * revealing a hint clears the hint on show.
 */
class SudokuRules(hints: HintEngine = HintEngine()) : RuleSet {
    private val revealHint = RevealHintRule(hints)

    override fun isLegal(state: GameState, move: Move): Boolean = !state.isWon &&
        when (move) {
            is Move.Place -> PlaceRule.isLegal(state, move)
            is Move.Erase -> EraseRule.isLegal(state, move)
            is Move.ToggleNote -> ToggleNoteRule.isLegal(state, move)
            is Move.RevealHint -> revealHint.isLegal(state, move)
            is Move.ApplyHint -> ApplyHintRule.isLegal(state, move)
        }

    override fun perform(state: GameState, move: Move): Transition {
        val transition = when (move) {
            is Move.Place -> PlaceRule.perform(state, move)
            is Move.Erase -> EraseRule.perform(state, move)
            is Move.ToggleNote -> ToggleNoteRule.perform(state, move)
            is Move.RevealHint -> return revealHint.perform(state, move)
            is Move.ApplyHint -> ApplyHintRule.perform(state, move)
        }
        return transition.copy(state = transition.state.copy(pendingHint = null))
    }

    override fun legalMoves(state: GameState): List<Move> = (
        Grid.CELLS.flatMap { cell ->
            Grid.DIGITS.flatMap { digit -> listOf(Move.Place(cell, digit), Move.ToggleNote(cell, digit)) } +
                Move.Erase(cell)
        } + Move.RevealHint + Move.ApplyHint
        ).filter { isLegal(state, it) }
}
