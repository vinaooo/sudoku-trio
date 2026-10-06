package io.github.vinaooo.sudokutrio.domain.generator

import io.github.vinaooo.sudokutrio.domain.model.GameMode
import io.github.vinaooo.sudokutrio.domain.model.Grid
import io.github.vinaooo.sudokutrio.domain.model.Puzzle
import io.github.vinaooo.sudokutrio.domain.model.Variant
import io.github.vinaooo.sudokutrio.domain.solver.LogicalSolver
import kotlin.math.abs
import kotlin.random.Random
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

/** Makes the puzzle for a mode from a seed: the same seed always gives the same puzzle. */
fun interface PuzzleGenerator {
    suspend fun generate(mode: GameMode, seed: Long): Puzzle
}

/**
 * Digs clues out of a random full grid. A clue stays out only while the logical solver, with the techniques the
 * target difficulty allows, still solves the whole puzzle within that difficulty's score ceiling
 * ([DifficultyBands]). A full solve by sound techniques also proves the solution is unique. Killer may keep a few
 * givens when its cages alone aren't enough.
 *
 * An attempt whose score ends below the target band tries a new grid; after [maxAttempts] the closest puzzle is
 * used. The budget counts attempts, never time, so a seed gives the same puzzle on every device.
 */
class SeededPuzzleGenerator(
    private val solver: LogicalSolver = LogicalSolver(),
    private val maxAttempts: Int = DEFAULT_ATTEMPTS,
) : PuzzleGenerator {

    private data class Scored(val puzzle: Puzzle, val score: Int)

    override suspend fun generate(mode: GameMode, seed: Long): Puzzle {
        val random = Random(seed)
        val target = DifficultyBands.lowest(mode.variant, mode.difficulty)
        var best: Scored? = null
        repeat(maxAttempts) {
            val attempt = dig(mode, random)
            if (attempt.score >= target) return attempt.puzzle
            if (best == null || abs(target - attempt.score) < abs(target - best.score)) best = attempt
        }
        return checkNotNull(best).puzzle
    }

    /** Checks for cancellation after every cell it digs, so a cancelled game stops within one solve. */
    private suspend fun dig(mode: GameMode, random: Random): Scored {
        val solution = FullGridBuilder.build(mode.variant, random)
        val cages = if (mode.variant == Variant.KILLER) CagePartitioner.partition(solution, random) else emptyList()
        val ceiling = DifficultyBands.ceiling(mode.variant, mode.difficulty) ?: Int.MAX_VALUE
        val givens = solution.toMutableList()
        var score = 0
        val groups = if (cages.isEmpty()) symmetricPairs() else Grid.CELLS.map { listOf(it) }
        groups.shuffled(random).forEach { group ->
            currentCoroutineContext().ensureActive()
            group.forEach { givens[it] = 0 }
            val result = solver.solve(Puzzle(givens, solution, cages), mode.variant, mode.difficulty)
            if (result.solved && result.score <= ceiling) {
                score = result.score
            } else {
                group.forEach { givens[it] = solution[it] }
            }
        }
        return Scored(Puzzle(givens.toList(), solution, cages), score)
    }

    private companion object {
        const val DEFAULT_ATTEMPTS = 20

        /** Cells dug in pairs mirrored through the center, the classic look; the center cell alone. */
        fun symmetricPairs(): List<List<Int>> = Grid.CELLS.filter { it <= Grid.SIZE - 1 - it }
            .map { listOf(it, Grid.SIZE - 1 - it).distinct() }
    }
}
