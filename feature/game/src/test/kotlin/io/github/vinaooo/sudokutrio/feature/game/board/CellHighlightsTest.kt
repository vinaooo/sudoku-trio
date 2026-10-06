package io.github.vinaooo.sudokutrio.feature.game.board

import io.github.vinaooo.sudokutrio.domain.model.Cage
import io.github.vinaooo.sudokutrio.domain.model.Puzzle
import io.github.vinaooo.sudokutrio.domain.model.Variant
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class CellHighlightsTest {
    private val solution = (
        "249368715356971824781542639512783496467195283893426571928614357675839142134257968"
        ).map { it.digitToInt() }
    private val puzzle = Puzzle(solution.mapIndexed { cell, d -> if (cell % 3 == 0) 0 else d }, solution)
    private val classic = CellHighlighter(puzzle, Variant.CLASSIC)

    @Test
    fun `nothing selected, nothing highlighted`() {
        classic.highlights(puzzle.givens, selected = null).toSet() shouldBe setOf(CellHighlight.NONE)
    }

    @Test
    fun `the selected cell, the cells it sees and the cells with its digit`() {
        // Cell 1 holds 4; cell 22 (row 2, column 4) also holds 4 and isn't a peer of cell 1.
        val highlights = classic.highlights(puzzle.givens, selected = 1)
        highlights[1] shouldBe CellHighlight.SELECTED
        highlights[0] shouldBe CellHighlight.PEER
        highlights[73] shouldBe CellHighlight.PEER
        highlights[19] shouldBe CellHighlight.PEER
        highlights[22] shouldBe CellHighlight.SAME_DIGIT
        highlights[40] shouldBe CellHighlight.NONE
        // A cell never sees its own digit elsewhere, so all 20 cells it sees are peers.
        highlights.count { it == CellHighlight.PEER } shouldBe 20
        highlights.count { it == CellHighlight.SAME_DIGIT } shouldBe puzzle.givens.count { it == 4 } - 1
    }

    @Test
    fun `an empty selected cell highlights no digit`() {
        val highlights = classic.highlights(puzzle.givens, selected = 0)
        highlights.count { it == CellHighlight.SAME_DIGIT } shouldBe 0
    }

    @Test
    fun `sudoku X adds the diagonals and killer the cage to the cells a cell sees`() {
        CellHighlighter(puzzle, Variant.X).highlights(puzzle.givens, selected = 0)[80] shouldBe CellHighlight.PEER
        classic.highlights(puzzle.givens, selected = 0)[80] shouldBe CellHighlight.NONE
        val killer = Puzzle(puzzle.givens, solution, listOf(Cage(10, listOf(0, 40))))
        CellHighlighter(killer, Variant.KILLER).highlights(puzzle.givens, selected = 0)[40] shouldBe CellHighlight.PEER
    }

    @Test
    fun `conflicts win over everything, then the selection, the hint, same digits and peers`() {
        val highlights = classic.highlights(
            puzzle.givens,
            selected = 1,
            hintCells = listOf(0, 1, 40),
            conflicts = setOf(1, 2),
        )
        highlights[1] shouldBe CellHighlight.CONFLICT
        highlights[2] shouldBe CellHighlight.CONFLICT
        highlights[0] shouldBe CellHighlight.HINT
        highlights[40] shouldBe CellHighlight.HINT
        highlights[3] shouldBe CellHighlight.PEER
    }

    @Test
    fun `a digit is complete once it is on the board nine times`() {
        completedDigits(solution) shouldBe (1..9).toSet()
        completedDigits(puzzle.givens) shouldBe emptySet()
        completedDigits(solution.mapIndexed { cell, d -> if (cell == 0) 0 else d }) shouldBe
            (1..9).toSet() - solution[0]
    }
}
