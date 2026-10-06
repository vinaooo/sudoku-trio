package io.github.vinaooo.sudokutrio.domain.generator

import io.github.vinaooo.sudokutrio.domain.model.Difficulty
import io.github.vinaooo.sudokutrio.domain.model.GameMode
import io.github.vinaooo.sudokutrio.domain.model.Grid
import io.github.vinaooo.sudokutrio.domain.model.Variant
import io.github.vinaooo.sudokutrio.domain.rules.constraintsFor
import io.github.vinaooo.sudokutrio.domain.rules.units
import io.github.vinaooo.sudokutrio.domain.solver.LogicalSolver
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.ints.shouldBeGreaterThanOrEqual
import io.kotest.matchers.ints.shouldBeInRange
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlin.random.Random
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class GeneratorTest {
    private val generator = SeededPuzzleGenerator()
    private val solver = LogicalSolver()

    @ParameterizedTest
    @EnumSource(Variant::class)
    fun `a full grid obeys every group of its variant`(variant: Variant) {
        val grid = FullGridBuilder.build(variant, Random(1))
        constraintsFor(variant).units(emptyList()).forEach { unit ->
            unit.map { grid[it] }.sorted() shouldBe Grid.DIGITS.toList()
        }
    }

    @Test
    fun `cages cover the board once, with side-by-side cells, distinct digits and their sums`() {
        val grid = FullGridBuilder.build(Variant.KILLER, Random(2))
        val cages = CagePartitioner.partition(grid, Random(2))
        cages.flatMap { it.cells }.sorted() shouldBe Grid.CELLS.toList()
        cages.forEach { cage ->
            cage.sum shouldBe cage.cells.sumOf { grid[it] }
            cage.cells.map { grid[it] }.distinct().size shouldBe cage.cells.size
            cage.cells.size shouldBeInRange 1..5
        }
        cages.count { it.cells.size == 1 } shouldBe 0
    }

    @Test
    fun `difficulty bands split scores per variant`() {
        DifficultyBands.difficultyOf(Variant.CLASSIC, 45) shouldBe Difficulty.EASY
        DifficultyBands.difficultyOf(Variant.CLASSIC, 46) shouldBe Difficulty.MEDIUM
        DifficultyBands.difficultyOf(Variant.CLASSIC, 63) shouldBe Difficulty.EXPERT
        DifficultyBands.difficultyOf(Variant.KILLER, 63) shouldBe Difficulty.EASY
        DifficultyBands.lowest(Variant.X, Difficulty.EASY) shouldBe 0
        DifficultyBands.lowest(Variant.X, Difficulty.HARD) shouldBe 71
        DifficultyBands.ceiling(Variant.X, Difficulty.MEDIUM) shouldBe 70
        DifficultyBands.ceiling(Variant.X, Difficulty.EXPERT) shouldBe null
    }

    @ParameterizedTest
    @EnumSource(Variant::class)
    fun `an easy puzzle has one solution, is easy and comes back from its seed`(variant: Variant) = runTest {
        val mode = GameMode(variant, Difficulty.EASY)
        val puzzle = generator.generate(mode, 11)
        SolutionCounter.count(puzzle, variant) shouldBe 1
        val result = solver.solve(puzzle, variant, Difficulty.EASY)
        result.solved shouldBe true
        result.values shouldBe puzzle.solution
        DifficultyBands.difficultyOf(variant, result.score) shouldBe Difficulty.EASY
        generator.generate(mode, 11) shouldBe puzzle
        generator.generate(mode, 12) shouldNotBe puzzle
    }

    @Test
    fun `an expert classic puzzle reaches its band`() = runTest {
        val puzzle = generator.generate(GameMode(Variant.CLASSIC, Difficulty.EXPERT), 5)
        SolutionCounter.count(puzzle, Variant.CLASSIC) shouldBe 1
        solver.solve(puzzle, Variant.CLASSIC).score shouldBeGreaterThanOrEqual 63
    }

    @Test
    fun `a killer hard puzzle leans on its cages`() = runTest {
        val puzzle = generator.generate(GameMode(Variant.KILLER, Difficulty.HARD), 5)
        SolutionCounter.count(puzzle, Variant.KILLER) shouldBe 1
        puzzle.givens.count { it != 0 } shouldBeInRange 0..10
        DifficultyBands.difficultyOf(Variant.KILLER, solver.solve(puzzle, Variant.KILLER).score) shouldBe
            Difficulty.HARD
    }

    @Test
    fun `out of attempts, the closest puzzle is used`() = runTest {
        val puzzle = SeededPuzzleGenerator(maxAttempts = 1).generate(GameMode(Variant.CLASSIC, Difficulty.EXPERT), 1)
        SolutionCounter.count(puzzle, Variant.CLASSIC) shouldBe 1
    }

    @Test
    fun `generation stops when cancelled`() = runTest {
        shouldThrow<CancellationException> {
            withContext(Job().apply { cancel() }) {
                generator.generate(GameMode(Variant.CLASSIC, Difficulty.EASY), 1)
            }
        }
    }
}
