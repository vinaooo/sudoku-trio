package io.github.vinaooo.sudokutrio.domain.rules

import io.github.vinaooo.sudokutrio.domain.model.GameState
import io.github.vinaooo.sudokutrio.domain.scoring.ScoreEvent

/** The board after a move plus what happened, before scoring is applied. */
data class Transition(val state: GameState, val events: List<ScoreEvent> = emptyList())
