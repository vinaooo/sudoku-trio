package io.github.vinaooo.sudokutrio.domain.generator

import io.github.vinaooo.sudokutrio.domain.model.Difficulty
import io.github.vinaooo.sudokutrio.domain.model.GameMode
import io.github.vinaooo.sudokutrio.domain.model.Variant
import io.github.vinaooo.sudokutrio.domain.solver.LogicalSolver
import kotlin.system.measureTimeMillis
import kotlinx.coroutines.runBlocking

/**
 * `./gradlew :domain:benchmarkGenerator [-Pseeds=30]`: per mode, how long generation takes, how many givens are
 * left, how often the target difficulty is reached, and whether every puzzle has exactly one solution.
 */
fun main(args: Array<String>) = runBlocking {
    val seeds = args.firstOrNull()?.toInt() ?: 30
    val generator = SeededPuzzleGenerator()
    val solver = LogicalSolver()
    println("mode                 median ms  max ms  givens  on-target  unique")
    Variant.entries.forEach { variant ->
        Difficulty.entries.forEach { difficulty ->
            val mode = GameMode(variant, difficulty)
            val times = mutableListOf<Long>()
            var onTarget = 0
            var unique = 0
            var givens = 0
            repeat(seeds) { seed ->
                lateinit var puzzle: io.github.vinaooo.sudokutrio.domain.model.Puzzle
                times += measureTimeMillis { puzzle = generator.generate(mode, seed.toLong()) }
                if (DifficultyBands.difficultyOf(variant, solver.solve(puzzle, variant).score) == difficulty) onTarget++
                if (SolutionCounter.count(puzzle, variant) == 1) unique++
                givens += puzzle.givens.count { it != 0 }
            }
            times.sort()
            println(
                "%-20s %9d %7d %7d %7d/%d %5d/%d".format(
                    "$variant $difficulty",
                    times[times.size / 2],
                    times.last(),
                    givens / seeds,
                    onTarget,
                    seeds,
                    unique,
                    seeds,
                ),
            )
        }
    }
}
