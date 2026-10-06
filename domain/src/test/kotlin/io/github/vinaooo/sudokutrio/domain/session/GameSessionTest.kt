package io.github.vinaooo.sudokutrio.domain.session

import io.github.vinaooo.sudokutrio.domain.SOLUTION
import io.github.vinaooo.sudokutrio.domain.model.Move
import io.github.vinaooo.sudokutrio.domain.newState
import io.github.vinaooo.sudokutrio.domain.rules.GameEngine
import io.github.vinaooo.sudokutrio.domain.wrongDigit
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class GameSessionTest {
    private val engine = GameEngine()
    private val fresh = GameSession(seed = 42, state = newState())

    private fun GameSession.play(move: Move) = play(move, engine).shouldNotBeNull()

    @Test
    fun `a fresh session has nothing to undo or redo and is not in progress`() {
        fresh.canUndo shouldBe false
        fresh.canRedo shouldBe false
        fresh.isInProgress shouldBe false
        fresh.undo().shouldBeNull()
        fresh.redo().shouldBeNull()
    }

    @Test
    fun `an illegal move changes nothing`() {
        fresh.play(Move.Place(1, 1), engine).shouldBeNull()
    }

    @Test
    fun `a move puts the game in progress and can be undone`() {
        val played = fresh.play(Move.ToggleNote(0, 4))
        played.isInProgress shouldBe true
        played.canUndo shouldBe true
        played.canRedo shouldBe false
    }

    @Test
    fun `undo restores the board but keeps the clock, moves, mistakes and score`() {
        val played = fresh.play(Move.ToggleNote(2, 7)).play(Move.Place(0, wrongDigit(0))).tick(12, engine)
        val undone = played.undo().shouldNotBeNull()
        undone.state.board shouldBe fresh.play(Move.ToggleNote(2, 7)).state.board
        undone.state.moves shouldBe 2
        undone.state.mistakes shouldBe 1
        undone.state.elapsedSeconds shouldBe 12
        undone.state.score shouldBe played.state.score
        undone.canRedo shouldBe true
    }

    @Test
    fun `undo brings back the notes a digit cleared`() {
        val digit = SOLUTION[0]
        val noted = fresh.play(Move.ToggleNote(2, digit))
        val placed = noted.play(Move.Place(0, digit))
        placed.state.board.notes[2] shouldBe emptySet()
        placed.undo().shouldNotBeNull().state.board shouldBe noted.state.board
    }

    @Test
    fun `redo writes the undone board again, counts a move and charges nothing`() {
        val played = fresh.play(Move.Place(0, wrongDigit(0)))
        val redone = played.undo().shouldNotBeNull().redo().shouldNotBeNull()
        redone.state.board shouldBe played.state.board
        redone.state.moves shouldBe 2
        redone.state.mistakes shouldBe 1
        redone.state.score shouldBe played.state.score
        redone.canRedo shouldBe false
        redone.canUndo shouldBe true
        redone.undo().shouldNotBeNull().state.board shouldBe fresh.state.board
    }

    @Test
    fun `a new move clears the redo stack`() {
        val undone = fresh.play(Move.ToggleNote(0, 4)).undo().shouldNotBeNull()
        undone.play(Move.ToggleNote(0, 5)).canRedo shouldBe false
    }

    @Test
    fun `a won game is over - no undo, no redo, not in progress`() {
        val almost = GameSession(1, newState(empty = listOf(0, 2)))
        val undone = almost.play(Move.Place(2, SOLUTION[2])).play(Move.ToggleNote(0, 1)).undo().shouldNotBeNull()
        val won = undone.play(Move.Place(0, SOLUTION[0]))
        won.state.isWon shouldBe true
        won.isInProgress shouldBe false
        won.canUndo shouldBe false
        won.canRedo shouldBe false
        won.undo().shouldBeNull()
        won.redo().shouldBeNull()
        undone.canRedo shouldBe true
    }

    @Test
    fun `ticking advances the clock`() {
        fresh.tick(5, engine).state.elapsedSeconds shouldBe 5
    }
}
