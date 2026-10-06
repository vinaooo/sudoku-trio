package io.github.vinaooo.sudokutrio.domain.rules

import io.github.vinaooo.sudokutrio.domain.SOLUTION
import io.github.vinaooo.sudokutrio.domain.model.Difficulty
import io.github.vinaooo.sudokutrio.domain.model.GameMode
import io.github.vinaooo.sudokutrio.domain.model.GameState
import io.github.vinaooo.sudokutrio.domain.model.Grid
import io.github.vinaooo.sudokutrio.domain.model.Move
import io.github.vinaooo.sudokutrio.domain.model.Variant
import io.github.vinaooo.sudokutrio.domain.newState
import io.github.vinaooo.sudokutrio.domain.puzzle
import io.github.vinaooo.sudokutrio.domain.scoring.ScoreEvent
import io.github.vinaooo.sudokutrio.domain.wrongDigit
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.Test

class GameEngineTest {
    private val engine = GameEngine()

    /** Cell 0 is empty, cell 1 a given. */
    private val state = newState()

    private fun GameState.play(move: Move): GameState =
        engine.apply(this, move).shouldBeInstanceOf<MoveOutcome.Applied>().state

    private fun rejected(state: GameState, move: Move) = engine.apply(state, move) shouldBe MoveOutcome.Rejected

    @Test
    fun `a new game starts on the givens with the mode's starting score`() {
        val expert = engine.newGame(puzzle(), GameMode(Variant.KILLER, Difficulty.EXPERT))
        expert.board.values shouldBe puzzle().givens
        expert.score shouldBe 12_000
        expert.moves shouldBe 0
        expert.mistakes shouldBe 0
    }

    @Test
    fun `placing the right digit writes it, counts a move and costs nothing`() {
        val outcome = engine.apply(state, Move.Place(0, SOLUTION[0])).shouldBeInstanceOf<MoveOutcome.Applied>()
        outcome.state.board.values[0] shouldBe SOLUTION[0]
        outcome.state.moves shouldBe 1
        outcome.state.mistakes shouldBe 0
        outcome.state.score shouldBe state.score
        outcome.events.shouldBeEmpty()
    }

    @Test
    fun `a wrong digit is a silent mistake that costs 100`() {
        val outcome = engine.apply(state, Move.Place(0, wrongDigit(0))).shouldBeInstanceOf<MoveOutcome.Applied>()
        outcome.state.board.values[0] shouldBe wrongDigit(0)
        outcome.state.mistakes shouldBe 1
        outcome.state.score shouldBe state.score - 100
        outcome.events shouldBe listOf(ScoreEvent.Mistake)
    }

    @Test
    fun `a digit can replace another but not itself`() {
        val wrong = state.play(Move.Place(0, wrongDigit(0)))
        rejected(wrong, Move.Place(0, wrongDigit(0)))
        wrong.play(Move.Place(0, SOLUTION[0])).board.values[0] shouldBe SOLUTION[0]
    }

    @Test
    fun `givens, cells off the board and digits outside 1 to 9 are rejected`() {
        rejected(state, Move.Place(1, wrongDigit(1)))
        rejected(state, Move.Place(-1, 1))
        rejected(state, Move.Place(81, 1))
        rejected(state, Move.Place(0, 0))
        rejected(state, Move.Place(0, 10))
        rejected(state, Move.ToggleNote(1, 3))
        rejected(state, Move.ToggleNote(0, 0))
        rejected(state, Move.ToggleNote(0, 10))
        rejected(state, Move.ToggleNote(81, 1))
        rejected(state, Move.Erase(1))
        rejected(state, Move.Erase(-1))
    }

    @Test
    fun `notes toggle in empty cells only`() {
        val noted = state.play(Move.ToggleNote(0, 3)).play(Move.ToggleNote(0, 5))
        noted.board.notes[0] shouldBe setOf(3, 5)
        noted.play(Move.ToggleNote(0, 3)).board.notes[0] shouldBe setOf(5)
        noted.moves shouldBe 2
        rejected(state.play(Move.Place(0, SOLUTION[0])), Move.ToggleNote(0, 3))
    }

    @Test
    fun `placing a digit clears the cell's notes and that digit from the notes of every cell it sees`() {
        // Cells 0, 3 (row), 9 (column + box), 18 (column + box) and 6 (row) are empty; 30 is not a peer of 0.
        val digit = SOLUTION[0]
        val other = wrongDigit(0)
        val noted = listOf(0, 3, 9, 18, 6, 30).fold(state) { s, cell ->
            s.play(Move.ToggleNote(cell, digit)).play(Move.ToggleNote(cell, other))
        }
        val placed = noted.play(Move.Place(0, digit))
        placed.board.notes[0] shouldBe emptySet()
        listOf(3, 9, 18, 6).forEach { placed.board.notes[it] shouldBe setOf(other) }
        placed.board.notes[30] shouldBe setOf(digit, other)
    }

    @Test
    fun `in sudoku X a digit also clears the notes along its diagonal`() {
        val x = newState(Variant.X)
        val digit = SOLUTION[0]
        val placed = x.play(Move.ToggleNote(30, digit)).play(Move.Place(0, digit))
        placed.board.notes[30] shouldBe emptySet()
        val classic = state.play(Move.ToggleNote(30, digit)).play(Move.Place(0, digit))
        classic.board.notes[30] shouldBe setOf(digit)
    }

    @Test
    fun `erase clears a digit or notes, and needs one of them`() {
        rejected(state, Move.Erase(0))
        val placed = state.play(Move.Place(0, wrongDigit(0)))
        placed.play(Move.Erase(0)).board.values[0] shouldBe 0
        val noted = state.play(Move.ToggleNote(0, 4))
        noted.play(Move.Erase(0)).board.notes[0] shouldBe emptySet()
    }

    @Test
    fun `filling the last cell wins and nothing more is legal`() {
        val almost = newState(empty = listOf(0))
        val won = almost.play(Move.Place(0, SOLUTION[0]))
        won.isWon shouldBe true
        engine.legalMoves(won).shouldBeEmpty()
        rejected(won, Move.Place(0, wrongDigit(0)))
    }

    @Test
    fun `a full board with a wrong digit is not won`() {
        val almost = newState(empty = listOf(0))
        almost.play(Move.Place(0, wrongDigit(0))).isWon shouldBe false
    }

    @Test
    fun `legal moves are exactly the moves isLegal accepts`() {
        val legal = engine.legalMoves(state).toSet()
        // 27 empty cells: 9 digits and 9 notes each, nothing to erase yet, and a hint to reveal.
        legal.size shouldBe 27 * 18 + 1
        legal.all { engine.isLegal(state, it) } shouldBe true
        val givens = Grid.CELLS.filter { state.puzzle.isGiven(it) }.toSet()
        legal.none { it is Move.Place && it.cell in givens || it is Move.ToggleNote && it.cell in givens } shouldBe true
    }

    @Test
    fun `the clock charges a point per second and stops at a win`() {
        engine.tick(state, 30).let {
            it.elapsedSeconds shouldBe 30
            it.score shouldBe state.score - 30
        }
        engine.tick(engine.tick(state, 30), 30).score shouldBe state.score - 30
        engine.tick(engine.tick(state, 30), 45).score shouldBe state.score - 45
        engine.tick(engine.tick(state, 30), 10).elapsedSeconds shouldBe 30
        val won = newState(empty = listOf(0)).play(Move.Place(0, SOLUTION[0]))
        engine.tick(won, 99) shouldBe won
    }

    @Test
    fun `the score never goes below zero`() {
        engine.tick(state, 1_000_000).score shouldBe 0
        engine.tick(state, 1_000_000).play(Move.Place(0, wrongDigit(0))).score shouldBe 0
    }
}
