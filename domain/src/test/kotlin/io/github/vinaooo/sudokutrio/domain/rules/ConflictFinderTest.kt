package io.github.vinaooo.sudokutrio.domain.rules

import io.github.vinaooo.sudokutrio.domain.CAGES
import io.github.vinaooo.sudokutrio.domain.SOLUTION
import io.github.vinaooo.sudokutrio.domain.model.Board
import io.github.vinaooo.sudokutrio.domain.model.GameState
import io.github.vinaooo.sudokutrio.domain.model.Grid
import io.github.vinaooo.sudokutrio.domain.model.Variant
import io.github.vinaooo.sudokutrio.domain.newState
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class ConflictFinderTest {
    private val finder = ConflictFinder()

    private fun GameState.with(vararg digits: Pair<Int, Int>): GameState {
        var board: Board = board
        digits.forEach { (cell, digit) -> board = board.withValue(cell, digit) }
        return copy(board = board)
    }

    @Test
    fun `a solved or untouched board has no conflicts`() {
        val state = newState(empty = Grid.CELLS.toList())
        finder.conflicts(state).shouldBeEmpty()
        finder.conflicts(state.copy(board = Board(SOLUTION))).shouldBeEmpty()
    }

    @Test
    fun `a digit repeated in a row marks both cells`() {
        val state = newState(empty = Grid.CELLS.toList()).with(0 to 5, 8 to 5)
        finder.conflicts(state) shouldBe setOf(0, 8)
    }

    @Test
    fun `columns and boxes count too`() {
        val empty = newState(empty = Grid.CELLS.toList())
        finder.conflicts(empty.with(0 to 5, 72 to 5)) shouldBe setOf(0, 72)
        finder.conflicts(empty.with(0 to 5, 20 to 5)) shouldBe setOf(0, 20)
    }

    @Test
    fun `a wrong digit with no visible clash is no conflict`() {
        val state = newState(empty = Grid.CELLS.toList()).with(0 to 1)
        finder.conflicts(state).shouldBeEmpty()
    }

    @Test
    fun `diagonals clash only in sudoku X`() {
        finder.conflicts(newState(empty = Grid.CELLS.toList()).with(0 to 5, 80 to 5)).shouldBeEmpty()
        finder.conflicts(newState(Variant.X, empty = Grid.CELLS.toList()).with(0 to 5, 80 to 5)) shouldBe setOf(0, 80)
    }

    @Test
    fun `a cage over its sum marks the whole cage`() {
        val state = newState(Variant.KILLER, empty = Grid.CELLS.toList())
        val cage = CAGES.first()
        finder.conflicts(state.with(0 to cage.sum - 1, 1 to 2)) shouldBe cage.cells.toSet()
    }

    @Test
    fun `a partial cage at or under its sum is fine`() {
        val state = newState(Variant.KILLER, empty = Grid.CELLS.toList())
        val cage = CAGES.first()
        finder.conflicts(state.with(0 to 1)).shouldBeEmpty()
        finder.conflicts(state.with(0 to SOLUTION[0], 1 to SOLUTION[1])).shouldBeEmpty()
    }

    @Test
    fun `a full cage with the wrong sum marks the whole cage`() {
        val state = newState(Variant.KILLER, empty = Grid.CELLS.toList())
        val cage = CAGES.first()
        val under = state.with(0 to 1, 1 to 2, 2 to 3)
        (6 < cage.sum) shouldBe true
        finder.conflicts(under) shouldBe cage.cells.toSet()
        finder.conflicts(state.with(*cage.cells.map { it to SOLUTION[it] }.toTypedArray())).shouldBeEmpty()
    }
}
