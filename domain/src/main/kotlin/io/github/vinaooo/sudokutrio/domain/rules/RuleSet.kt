package io.github.vinaooo.sudokutrio.domain.rules

import io.github.vinaooo.sudokutrio.domain.model.GameState
import io.github.vinaooo.sudokutrio.domain.model.Move

/** The rules of the game. A new kind of move adds a [MoveRule] instead of changing the engine. */
interface RuleSet {
    fun isLegal(state: GameState, move: Move): Boolean

    /** Performs a move. Callers must check [isLegal] first. */
    fun perform(state: GameState, move: Move): Transition

    fun legalMoves(state: GameState): List<Move>
}
