package io.github.vinaooo.sudokutrio.domain.scoring

import io.github.vinaooo.sudokutrio.domain.model.GameMode

/** How a ranking sorts its scores. */
enum class RankingOrder { HIGHEST_SCORE, }

interface ScoringStrategy {
    val rankingOrder: RankingOrder

    /** The score a new game starts with. */
    fun startingScore(mode: GameMode): Int

    fun pointsFor(event: ScoreEvent): Int

    /** The score as the strategy allows it: never below zero unless overridden. */
    fun bounded(score: Int): Int = score.coerceAtLeast(0)
}

/** The scoring each [GameMode] plays with. Every mode plays for points so far. */
@Suppress("UnusedParameter") // The seam where a new scoring mode is picked.
fun scoringFor(mode: GameMode): ScoringStrategy = PointsScoring
