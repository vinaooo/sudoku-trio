package io.github.vinaooo.sudokutrio.domain.model

import io.github.vinaooo.sudokutrio.domain.scoring.RankingOrder
import io.github.vinaooo.sudokutrio.domain.scoring.scoringFor

/** A won game in a mode's ranking. */
data class ScoreRecord(
    val mode: GameMode,
    val points: Int,
    val elapsedSeconds: Long,
    val mistakes: Int,
    val hintsUsed: Int,
    val playedAtMillis: Long,
) {
    companion object {
        const val TOP_LIMIT = 10

        /** Highest points first; ties go to the fastest game, then the earliest. */
        private val HIGHEST_SCORE: Comparator<ScoreRecord> = compareByDescending<ScoreRecord> { it.points }
            .thenBy { it.elapsedSeconds }.thenBy { it.playedAtMillis }

        fun rankingFor(mode: GameMode): Comparator<ScoreRecord> = when (scoringFor(mode).rankingOrder) {
            RankingOrder.HIGHEST_SCORE -> HIGHEST_SCORE
        }
    }
}
