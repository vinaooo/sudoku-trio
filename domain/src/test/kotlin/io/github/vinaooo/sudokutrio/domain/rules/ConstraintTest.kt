package io.github.vinaooo.sudokutrio.domain.rules

import io.github.vinaooo.sudokutrio.domain.CAGES
import io.github.vinaooo.sudokutrio.domain.SOLUTION
import io.github.vinaooo.sudokutrio.domain.model.Grid
import io.github.vinaooo.sudokutrio.domain.model.Variant
import io.github.vinaooo.sudokutrio.domain.puzzle
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class ConstraintTest {
    private val classic = puzzle()
    private val killer = puzzle(cages = CAGES)

    @Test
    fun `classic has 9 rows, 9 columns and 9 boxes, each holding 1 to 9 in the solution`() {
        val units = constraintsFor(Variant.CLASSIC).units(classic)
        units.size shouldBe 27
        units.forEach { unit -> unit.map { SOLUTION[it] }.sorted() shouldBe Grid.DIGITS.toList() }
        units.flatten().groupingBy { it }.eachCount().values.toSet() shouldBe setOf(3)
    }

    @Test
    fun `rows, columns and boxes hold the expected cells`() {
        RowConstraint.units(classic)[1] shouldBe (9..17).toList()
        ColumnConstraint.units(classic)[2] shouldBe listOf(2, 11, 20, 29, 38, 47, 56, 65, 74)
        BoxConstraint.units(classic)[4] shouldContainExactlyInAnyOrder listOf(30, 31, 32, 39, 40, 41, 48, 49, 50)
    }

    @Test
    fun `sudoku X adds both diagonals`() {
        val units = constraintsFor(Variant.X).units(classic)
        units.size shouldBe 29
        DiagonalConstraint.units(classic) shouldBe listOf(
            listOf(0, 10, 20, 30, 40, 50, 60, 70, 80),
            listOf(8, 16, 24, 32, 40, 48, 56, 64, 72),
        )
    }

    @Test
    fun `killer adds its cages`() {
        val units = constraintsFor(Variant.KILLER).units(killer)
        units.size shouldBe 27 + CAGES.size
        CageConstraint.units(killer) shouldBe CAGES.map { it.cells }
    }

    @Test
    fun `peers are every other cell sharing a group`() {
        val classicUnits = constraintsFor(Variant.CLASSIC).units(classic)
        classicUnits.peersOf(0).size shouldBe 20
        (0 in classicUnits.peersOf(0)) shouldBe false
        constraintsFor(Variant.X).units(classic).peersOf(0).size shouldBe 26
        constraintsFor(Variant.X).units(classic).peersOf(40).size shouldBe 32
        constraintsFor(Variant.X).units(classic).peersOf(1).size shouldBe 20
    }
}
