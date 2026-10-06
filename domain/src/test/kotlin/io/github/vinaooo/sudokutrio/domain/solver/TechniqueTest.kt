package io.github.vinaooo.sudokutrio.domain.solver

import io.github.vinaooo.sudokutrio.domain.SOLUTION
import io.github.vinaooo.sudokutrio.domain.model.Cage
import io.github.vinaooo.sudokutrio.domain.model.Grid
import io.github.vinaooo.sudokutrio.domain.model.Puzzle
import io.github.vinaooo.sudokutrio.domain.model.Variant
import io.github.vinaooo.sudokutrio.domain.puzzle
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.Test

/** Each technique on a hand-made candidate grid: every empty cell may hold anything unless set otherwise. */
class TechniqueTest {
    private val classic = SolverContext(puzzle(), Variant.CLASSIC)

    private fun mask(vararg digits: Int) = digits.fold(0) { m, d -> m or bit(d) }

    private fun grid(masks: Map<Int, Int> = emptyMap(), values: Map<Int, Int> = emptyMap()) = Candidates.raw(
        values = Grid.CELLS.map { values[it] ?: 0 },
        masks = Grid.CELLS.map { if (it in values) 0 else masks[it] ?: ALL_DIGITS },
    )

    /** Every cell of [cells] without [digit]. */
    private fun without(digit: Int, cells: Iterable<Int>) = cells.associateWith { ALL_DIGITS and bit(digit).inv() }

    private fun Deduction?.eliminations() = shouldBeInstanceOf<Deduction.Eliminations>().eliminations

    @Test
    fun `naked single - a cell with one candidate`() {
        NakedSingle.find(grid(), classic).shouldBeNull()
        NakedSingle.find(grid(mapOf(5 to mask(7))), classic) shouldBe
            Deduction.Placement(5, 7, TechniqueKind.NAKED_SINGLE, listOf(5))
    }

    @Test
    fun `hidden single - a digit with one spot in a house`() {
        HiddenSingle.find(grid(), classic).shouldBeNull()
        val grid = grid(without(4, (0..8) - 3))
        HiddenSingle.find(grid, classic) shouldBe
            Deduction.Placement(3, 4, TechniqueKind.HIDDEN_SINGLE, (0..8).toList())
    }

    @Test
    fun `hidden single ignores a digit already placed in the house`() {
        HiddenSingle.find(grid(without(4, 1..8), values = mapOf(0 to 4)), classic).shouldBeNull()
    }

    @Test
    fun `locked candidates - a box's digit confined to one row leaves the rest of the row`() {
        val box = listOf(0, 1, 2, 9, 10, 11, 18, 19, 20)
        val step = LockedCandidates.find(grid(without(5, box - setOf(0, 1))), classic)
        step.eliminations() shouldContainExactlyInAnyOrder (3..8).map { Elimination(it, 5) }
        step!!.cells shouldBe listOf(0, 1)
    }

    @Test
    fun `locked candidates - near miss when the spots share no other group`() {
        val box = listOf(0, 1, 2, 9, 10, 11, 18, 19, 20)
        LockedCandidates.find(grid(without(5, box - setOf(0, 10))), classic).shouldBeNull()
    }

    @Test
    fun `locked candidates on a diagonal in sudoku X`() {
        val x = SolverContext(puzzle(), Variant.X)
        // In box 0 digit 5 fits only cells 0 and 10, both on the main diagonal.
        val box = listOf(0, 1, 2, 9, 10, 11, 18, 19, 20)
        LockedCandidates.find(grid(without(5, box - setOf(0, 10))), x).eliminations() shouldContain Elimination(80, 5)
    }

    @Test
    fun `naked pair - two cells with the same two candidates`() {
        val step = NakedSubset(2, TechniqueKind.NAKED_PAIR).find(grid(mapOf(0 to mask(1, 2), 1 to mask(1, 2))), classic)
        step.eliminations() shouldContain Elimination(8, 1)
        step.eliminations() shouldContain Elimination(8, 2)
        step.eliminations() shouldNotContain Elimination(0, 1)
        step!!.cells shouldBe listOf(0, 1)
        NakedSubset(
            2,
            TechniqueKind.NAKED_PAIR,
        ).find(grid(mapOf(0 to mask(1, 2), 1 to mask(1, 3))), classic).shouldBeNull()
    }

    @Test
    fun `naked pair inside a killer cage`() {
        val cage = Cage(10, listOf(0, 13, 30))
        val killer = SolverContext(Puzzle(List(81) { 0 }, SOLUTION, listOf(cage)), Variant.KILLER)
        val step = NakedSubset(2, TechniqueKind.NAKED_PAIR).find(grid(mapOf(0 to mask(1, 2), 13 to mask(1, 2))), killer)
        step.eliminations() shouldContainExactlyInAnyOrder listOf(Elimination(30, 1), Elimination(30, 2))
    }

    @Test
    fun `naked triple - three cells sharing three candidates`() {
        val masks = mapOf(0 to mask(1, 2), 1 to mask(2, 3), 2 to mask(1, 3))
        NakedSubset(3, TechniqueKind.NAKED_TRIPLE).find(grid(masks), classic).eliminations() shouldContain
            Elimination(8, 3)
    }

    @Test
    fun `hidden pair - two digits confined to two cells of a house`() {
        val others = (2..8).associateWith { ALL_DIGITS and mask(1, 2).inv() }
        val step = HiddenSubset(2, TechniqueKind.HIDDEN_PAIR).find(grid(others), classic)
        step.eliminations() shouldContainExactlyInAnyOrder
            (3..9).flatMap { listOf(Elimination(0, it), Elimination(1, it)) }
        step!!.cells shouldBe listOf(0, 1)
    }

    @Test
    fun `hidden pair - near miss when one digit has a third spot`() {
        val others = (3..8).associateWith { ALL_DIGITS and mask(1, 2).inv() } + (2 to (ALL_DIGITS and bit(2).inv()))
        HiddenSubset(2, TechniqueKind.HIDDEN_PAIR).find(grid(others), classic).shouldBeNull()
    }

    @Test
    fun `hidden triple - three digits confined to three cells`() {
        val others = (3..8).associateWith { ALL_DIGITS and mask(1, 2, 3).inv() }
        HiddenSubset(3, TechniqueKind.HIDDEN_TRIPLE).find(grid(others), classic).eliminations() shouldContain
            Elimination(2, 9)
    }

    @Test
    fun `x-wing - a digit in two rows on the same two columns`() {
        val rows = (0..8) + (36..44)
        val masks = without(5, rows - setOf(1, 7, 37, 43))
        val step = Fish(2, TechniqueKind.X_WING).find(grid(masks), classic)
        step.eliminations() shouldContainExactlyInAnyOrder
            (0..8).filter { it != 0 && it != 4 }.flatMap { row ->
                listOf(
                    Elimination(row * 9 + 1, 5),
                    Elimination(
                        row * 9 + 7,
                        5,
                    ),
                )
            }
        step!!.cells shouldContainExactlyInAnyOrder listOf(1, 7, 37, 43)
    }

    @Test
    fun `x-wing - near miss on three columns`() {
        val rows = (0..8) + (36..44)
        Fish(2, TechniqueKind.X_WING).find(grid(without(5, rows - setOf(1, 7, 37, 42))), classic).shouldBeNull()
    }

    @Test
    fun `x-wing on columns`() {
        val columns = (0..8).flatMap { listOf(it * 9 + 2, it * 9 + 6) }
        val masks = without(5, columns - setOf(2, 6, 74, 78))
        Fish(2, TechniqueKind.X_WING).find(grid(masks), classic).eliminations() shouldContain Elimination(4, 5)
    }

    @Test
    fun `swordfish - three rows on three columns, which an x-wing misses`() {
        val rows = (0..8) + (27..35) + (54..62)
        val masks = without(5, rows - setOf(1, 4, 31, 34, 55, 61))
        Fish(2, TechniqueKind.X_WING).find(grid(masks), classic).shouldBeNull()
        Fish(3, TechniqueKind.SWORDFISH).find(grid(masks), classic).eliminations() shouldContain Elimination(10, 5)
    }

    @Test
    fun `xy-wing - a pivot and two pincers rule out their shared digit`() {
        val masks = mapOf(0 to mask(1, 2), 4 to mask(1, 3), 18 to mask(2, 3))
        val step = XyWing.find(grid(masks), classic)
        step.eliminations() shouldContain Elimination(22, 3)
        step.eliminations() shouldContain Elimination(1, 3)
        step!!.cells shouldBe listOf(0, 4, 18)
        XyWing.find(grid(mapOf(0 to mask(1, 2), 4 to mask(1, 3), 18 to mask(2, 4))), classic).shouldBeNull()
        XyWing.find(grid(mapOf(0 to mask(1, 2), 4 to mask(1, 3), 18 to mask(1, 3))), classic).shouldBeNull()
    }

    @Test
    fun `cage combination - only the digits that add up to the sum`() {
        val killer = SolverContext(Puzzle(List(81) { 0 }, SOLUTION, listOf(Cage(3, listOf(0, 1)))), Variant.KILLER)
        CageCombination.find(grid(), killer).eliminations() shouldContainExactlyInAnyOrder
            (3..9).flatMap { listOf(Elimination(0, it), Elimination(1, it)) }
        CageCombination.find(grid(mapOf(0 to mask(1, 2), 1 to mask(1, 2))), killer).shouldBeNull()
    }

    @Test
    fun `cage combination counts placed digits and never repeats one`() {
        val killer = SolverContext(Puzzle(List(81) { 0 }, SOLUTION, listOf(Cage(6, listOf(0, 1, 2)))), Variant.KILLER)
        val step = CageCombination.find(grid(values = mapOf(0 to 1)), killer).eliminations()
        step.filter { it.cell == 1 }.map { it.digit } shouldContainExactlyInAnyOrder listOf(1, 4, 5, 6, 7, 8, 9)
    }

    @Test
    fun `rule of 45 - a row's cell left over by its cages`() {
        val cages =
            listOf(Cage(6, listOf(0, 1, 2)), Cage(15, listOf(3, 4, 5)), Cage(17, listOf(6, 7)), Cage(10, listOf(8, 17)))
        val killer = SolverContext(Puzzle(List(81) { 0 }, SOLUTION, cages), Variant.KILLER)
        killer.innies shouldBe listOf(Cage(7, listOf(8)))
        RuleOf45.find(grid(), killer).eliminations() shouldContainExactlyInAnyOrder
            (Grid.DIGITS - 7).map { Elimination(8, it) }
        RuleOf45.find(grid(), classic).shouldBeNull()
    }

    @Test
    fun `combinations pick every subset of a size in order`() {
        listOf(1, 2, 3).combinations(2) shouldBe listOf(listOf(1, 2), listOf(1, 3), listOf(2, 3))
        listOf(1, 2).combinations(3) shouldBe emptyList()
        listOf(1).combinations(0) shouldBe listOf(emptyList())
    }
}
