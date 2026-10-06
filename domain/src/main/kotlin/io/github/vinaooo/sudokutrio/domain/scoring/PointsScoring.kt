package io.github.vinaooo.sudokutrio.domain.scoring

import io.github.vinaooo.sudokutrio.domain.model.Difficulty
import io.github.vinaooo.sudokutrio.domain.model.GameMode
import io.github.vinaooo.sudokutrio.domain.model.Variant

/**
 * Points, as chosen with the user: a game starts at a base that grows with the difficulty and the variant, then
 * loses a point per second, 100 per mistake and 200 per hint, never going below zero. The highest score ranks first.
 */
object PointsScoring : ScoringStrategy {
    override val rankingOrder = RankingOrder.HIGHEST_SCORE

    override fun startingScore(mode: GameMode): Int = base(mode.difficulty) * factorPercent(mode.variant) / PERCENT

    override fun pointsFor(event: ScoreEvent): Int = when (event) {
        ScoreEvent.Mistake -> MISTAKE
        ScoreEvent.HintUsed -> HINT
        is ScoreEvent.TimeElapsed -> -event.seconds.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
    }

    private fun base(difficulty: Difficulty): Int = when (difficulty) {
        Difficulty.EASY -> EASY_BASE
        Difficulty.MEDIUM -> MEDIUM_BASE
        Difficulty.HARD -> HARD_BASE
        Difficulty.EXPERT -> EXPERT_BASE
    }

    private fun factorPercent(variant: Variant): Int = when (variant) {
        Variant.CLASSIC -> CLASSIC_FACTOR
        Variant.X -> X_FACTOR
        Variant.KILLER -> KILLER_FACTOR
    }

    private const val EASY_BASE = 1000
    private const val MEDIUM_BASE = 2000
    private const val HARD_BASE = 4000
    private const val EXPERT_BASE = 8000
    private const val CLASSIC_FACTOR = 100
    private const val X_FACTOR = 120
    private const val KILLER_FACTOR = 150
    private const val PERCENT = 100
    private const val MISTAKE = -100
    private const val HINT = -200
}
