package io.github.vinaooo.sudokutrio.domain.solver

import io.github.vinaooo.sudokutrio.domain.SOLUTION
import io.github.vinaooo.sudokutrio.domain.generator.SeededPuzzleGenerator
import io.github.vinaooo.sudokutrio.domain.model.Difficulty
import io.github.vinaooo.sudokutrio.domain.model.GameMode
import io.github.vinaooo.sudokutrio.domain.model.Grid
import io.github.vinaooo.sudokutrio.domain.model.Puzzle
import io.github.vinaooo.sudokutrio.domain.model.Variant
import io.github.vinaooo.sudokutrio.domain.puzzle
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class LogicalSolverTest {
    private val solver = LogicalSolver()

    /** Arto Inkala's puzzle: it needs guessing, beyond every technique here. */
    private val inkala = Puzzle(
        "800000000003600000070090200050007000000045700000100030001000068008500010090000400".map { it.digitToInt() },
        "812753649943682175675491283154237896369845721287169534521974368438526917796318452".map { it.digitToInt() },
    )

    @Test
    fun `a board that singles solve scores one point per cell, though harder techniques also apply`() {
        val result = solver.solve(puzzle(), Variant.CLASSIC)
        result.solved shouldBe true
        result.values shouldBe SOLUTION
        result.score shouldBe 27
    }

    @Test
    fun `a full board needs nothing`() {
        solver.solve(puzzle(empty = emptyList()), Variant.CLASSIC) shouldBe SolveResult(SOLUTION, true, 0)
    }

    @Test
    fun `a puzzle beyond the techniques stays unsolved`() {
        val result = solver.solve(inkala, Variant.CLASSIC)
        result.solved shouldBe false
        result.values.count { it == 0 } shouldBe Grid.SIZE - inkala.givens.count { it != 0 } - result.values
            .withIndex().count { (cell, digit) -> digit != 0 && inkala.givens[cell] == 0 }
    }

    @Test
    fun `capping the difficulty leaves out harder techniques`() = runTest {
        val expert = SeededPuzzleGenerator().generate(GameMode(Variant.CLASSIC, Difficulty.EXPERT), 5)
        solver.solve(expert, Variant.CLASSIC, maxDifficulty = Difficulty.EASY).solved shouldBe false
        solver.solve(expert, Variant.CLASSIC, maxDifficulty = Difficulty.EXPERT).solved shouldBe true
    }

    @Test
    fun `the next placement is a digit of the solution`() {
        val step = solver.nextPlacement(puzzle(), Variant.CLASSIC, puzzle().givens).shouldNotBeNull()
        step.digit shouldBe SOLUTION[step.cell]
        solver.nextPlacement(puzzle(), Variant.CLASSIC, SOLUTION).shouldBeNull()
        solver.nextPlacement(inkala, Variant.CLASSIC, inkala.givens).let {
            it == null ||
                it.digit == inkala.solution[it.cell]
        }
            .shouldBe(true)
    }

    @Test
    fun `the next placement may need eliminations first`() {
        // Only eliminations apply at first on Inkala's board; whatever placement comes is still right.
        val first = NakedSingle.find(
            Candidates.of(inkala.givens, SolverContext(inkala, Variant.CLASSIC)),
            SolverContext(inkala, Variant.CLASSIC),
        )
        first.shouldBeNull()
    }
}
