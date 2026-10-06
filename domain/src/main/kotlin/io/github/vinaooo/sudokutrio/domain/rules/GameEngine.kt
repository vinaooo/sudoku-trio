package io.github.vinaooo.sudokutrio.domain.rules

import io.github.vinaooo.sudokutrio.domain.model.Board
import io.github.vinaooo.sudokutrio.domain.model.GameMode
import io.github.vinaooo.sudokutrio.domain.model.GameState
import io.github.vinaooo.sudokutrio.domain.model.Move
import io.github.vinaooo.sudokutrio.domain.model.Puzzle
import io.github.vinaooo.sudokutrio.domain.scoring.ScoreEvent
import io.github.vinaooo.sudokutrio.domain.scoring.ScoringStrategy
import io.github.vinaooo.sudokutrio.domain.scoring.scoringFor

sealed interface MoveOutcome {
    data class Applied(val state: GameState, val events: List<ScoreEvent>) : MoveOutcome

    data object Rejected : MoveOutcome
}

/** Pure game loop: starts games, validates and applies moves, runs the clock and keeps the score. */
class GameEngine(
    private val rules: RuleSet = SudokuRules(),
    private val scoring: (GameMode) -> ScoringStrategy = ::scoringFor,
) {
    fun newGame(puzzle: Puzzle, mode: GameMode): GameState =
        GameState(puzzle = puzzle, board = Board.from(puzzle), mode = mode, score = scoring(mode).startingScore(mode))

    fun apply(state: GameState, move: Move): MoveOutcome {
        if (!rules.isLegal(state, move)) return MoveOutcome.Rejected
        val transition = rules.perform(state, move)
        // Revealing a hint only shows it: not a move.
        val moves = if (move == Move.RevealHint) state.moves else state.moves + 1
        val next = transition.state.copy(moves = moves).withPoints(transition.events)
        return MoveOutcome.Applied(next, transition.events)
    }

    /** Advances the clock to [elapsedSeconds], charging each new second. A won game's clock has stopped. */
    fun tick(state: GameState, elapsedSeconds: Long): GameState {
        if (state.isWon || elapsedSeconds <= state.elapsedSeconds) return state
        return state.copy(elapsedSeconds = elapsedSeconds)
            .withPoints(listOf(ScoreEvent.TimeElapsed(elapsedSeconds - state.elapsedSeconds)))
    }

    fun legalMoves(state: GameState): List<Move> = rules.legalMoves(state)

    fun isLegal(state: GameState, move: Move): Boolean = rules.isLegal(state, move)

    private fun GameState.withPoints(events: List<ScoreEvent>): GameState {
        val strategy = scoring(mode)
        return copy(score = strategy.bounded(score + events.sumOf(strategy::pointsFor)))
    }
}
