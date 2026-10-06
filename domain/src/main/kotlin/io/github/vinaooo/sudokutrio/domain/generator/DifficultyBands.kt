package io.github.vinaooo.sudokutrio.domain.generator

import io.github.vinaooo.sudokutrio.domain.model.Difficulty
import io.github.vinaooo.sudokutrio.domain.model.Variant

/**
 * The solver scores ([io.github.vinaooo.sudokutrio.domain.solver.SolveResult.score]) each difficulty spans, per
 * variant. Tuned with `./gradlew :domain:benchmarkGenerator`: Killer scores run higher because its cages take many
 * steps, Sudoku X a little higher than Classic.
 */
object DifficultyBands {
    private val lowestScores: Map<Variant, List<Int>> = mapOf(
        // Medium, hard and expert start at these scores; easy is everything below medium.
        Variant.CLASSIC to listOf(46, 55, 63),
        Variant.X to listOf(51, 71, 93),
        Variant.KILLER to listOf(161, 221, 271),
    )

    fun difficultyOf(variant: Variant, score: Int): Difficulty =
        Difficulty.entries[lowestScores.getValue(variant).count { score >= it }]

    /** The lowest score [difficulty] needs: 0 for easy. */
    fun lowest(variant: Variant, difficulty: Difficulty): Int =
        if (difficulty == Difficulty.EASY) 0 else lowestScores.getValue(variant)[difficulty.ordinal - 1]

    /** The highest score [difficulty] allows, or null for the hardest level, which has no ceiling. */
    fun ceiling(variant: Variant, difficulty: Difficulty): Int? =
        lowestScores.getValue(variant).getOrNull(difficulty.ordinal)?.minus(1)
}
