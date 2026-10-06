package io.github.vinaooo.sudokutrio.domain.scoring

/** Things that happen during play that a [ScoringStrategy] may reward or penalize. */
sealed interface ScoreEvent {
    /** A digit placed that differs from the solution. */
    data object Mistake : ScoreEvent

    data object HintUsed : ScoreEvent

    data class TimeElapsed(val seconds: Long) : ScoreEvent
}
