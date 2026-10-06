package io.github.vinaooo.sudokutrio.domain.model

import io.github.vinaooo.sudokutrio.domain.SOLUTION
import io.github.vinaooo.sudokutrio.domain.puzzle
import io.github.vinaooo.sudokutrio.domain.wrongDigit
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class ModelTest {

    @Test
    fun `cells are numbered row by row`() {
        Grid.cell(row = 2, column = 7) shouldBe 25
        Grid.row(25) shouldBe 2
        Grid.column(25) shouldBe 7
        Grid.CELLS.last shouldBe 80
    }

    @Test
    fun `a puzzle needs 81 cells`() {
        shouldThrow<IllegalArgumentException> { Puzzle(SOLUTION.drop(1), SOLUTION) }
        shouldThrow<IllegalArgumentException> { Puzzle(SOLUTION, SOLUTION.drop(1)) }
    }

    @Test
    fun `a solution fills every cell with 1 to 9`() {
        shouldThrow<IllegalArgumentException> {
            Puzzle(
                List(81) { 0 },
                SOLUTION.mapIndexed { i, d ->
                    if (i ==
                        5
                    ) {
                        0
                    } else {
                        d
                    }
                },
            )
        }
        shouldThrow<IllegalArgumentException> {
            Puzzle(
                List(81) { 0 },
                SOLUTION.mapIndexed { i, d ->
                    if (i ==
                        5
                    ) {
                        10
                    } else {
                        d
                    }
                },
            )
        }
    }

    @Test
    fun `every given matches the solution`() {
        shouldThrow<IllegalArgumentException> {
            Puzzle(SOLUTION.mapIndexed { i, d -> if (i == 3) wrongDigit(3) else d }, SOLUTION)
        }
    }

    @Test
    fun `a cage needs cells on the board`() {
        shouldThrow<IllegalArgumentException> { Cage(5, emptyList()) }
        shouldThrow<IllegalArgumentException> { Cage(5, listOf(81)) }
        shouldThrow<IllegalArgumentException> { Cage(5, listOf(-1)) }
        Cage(5, listOf(0, 80)).cells shouldBe listOf(0, 80)
    }

    @Test
    fun `givens are the non-empty cells`() {
        val puzzle = puzzle(empty = listOf(0))
        puzzle.isGiven(0) shouldBe false
        puzzle.isGiven(1) shouldBe true
        Board.from(puzzle).values shouldBe puzzle.givens
    }

    @Test
    fun `a board needs 81 values and 81 note sets`() {
        shouldThrow<IllegalArgumentException> { Board(List(80) { 0 }) }
        shouldThrow<IllegalArgumentException> { Board(List(81) { 0 }, List(80) { emptySet() }) }
    }

    @Test
    fun `board edits touch only their cells`() {
        val board = Board(List(81) { 0 }).withNotes(1, setOf(4, 5)).withNotes(2, setOf(4)).withNotes(3, setOf(4))
        board.withValue(7, 3).values.withIndex().filter { it.value != 0 }.map { it.index } shouldBe listOf(7)
        val cleared = board.withoutNote(listOf(1, 2), 4)
        cleared.notes[1] shouldBe setOf(5)
        cleared.notes[2] shouldBe emptySet()
        cleared.notes[3] shouldBe setOf(4)
    }
}
