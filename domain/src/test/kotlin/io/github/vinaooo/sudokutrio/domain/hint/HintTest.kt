package io.github.vinaooo.sudokutrio.domain.hint

import io.github.vinaooo.sudokutrio.domain.SOLUTION
import io.github.vinaooo.sudokutrio.domain.generator.SeededPuzzleGenerator
import io.github.vinaooo.sudokutrio.domain.model.Board
import io.github.vinaooo.sudokutrio.domain.model.Difficulty
import io.github.vinaooo.sudokutrio.domain.model.GameMode
import io.github.vinaooo.sudokutrio.domain.model.GameState
import io.github.vinaooo.sudokutrio.domain.model.Hint
import io.github.vinaooo.sudokutrio.domain.model.Move
import io.github.vinaooo.sudokutrio.domain.model.Puzzle
import io.github.vinaooo.sudokutrio.domain.model.Variant
import io.github.vinaooo.sudokutrio.domain.newState
import io.github.vinaooo.sudokutrio.domain.rules.GameEngine
import io.github.vinaooo.sudokutrio.domain.rules.MoveOutcome
import io.github.vinaooo.sudokutrio.domain.scoring.ScoreEvent
import io.github.vinaooo.sudokutrio.domain.session.GameSession
import io.github.vinaooo.sudokutrio.domain.solver.Candidates
import io.github.vinaooo.sudokutrio.domain.solver.Deduction
import io.github.vinaooo.sudokutrio.domain.solver.HiddenSingle
import io.github.vinaooo.sudokutrio.domain.solver.NakedSingle
import io.github.vinaooo.sudokutrio.domain.solver.SolverContext
import io.github.vinaooo.sudokutrio.domain.solver.TechniqueKind
import io.github.vinaooo.sudokutrio.domain.wrongDigit
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class HintTest {
    private val hints = HintEngine()
    private val engine = GameEngine()
    private val state = newState()

    private fun GameState.play(move: Move) = engine.apply(this, move).shouldBeInstanceOf<MoveOutcome.Applied>().state

    @Test
    fun `a hint names the next logical digit, which is right`() {
        val hint = hints.hintFor(state).shouldBeInstanceOf<Hint.Placement>()
        hint.digit shouldBe SOLUTION[hint.cell]
        hint.technique.shouldNotBeNull()
        (hint.cell in hint.cells) shouldBe true
    }

    @Test
    fun `a wrong digit on the board comes first`() {
        val wrong = state.play(Move.Place(3, wrongDigit(3)))
        hints.hintFor(wrong) shouldBe Hint.WrongDigit(3)
        Hint.WrongDigit(3).cells shouldBe listOf(3)
    }

    @Test
    fun `a board the techniques can't crack gets a cell revealed`() {
        val bare = GameEngine().newGame(
            Puzzle(List(81) { 0 }, SOLUTION),
            state.mode,
        )
        hints.hintFor(bare) shouldBe Hint.Placement(0, SOLUTION[0], technique = null, cells = listOf(0))
    }

    @Test
    fun `a won game has no hint`() {
        hints.hintFor(state.copy(board = Board(SOLUTION))).shouldBeNull()
    }

    @Test
    fun `revealing a hint counts it and costs 200, but is not a move`() {
        val outcome = engine.apply(state, Move.RevealHint).shouldBeInstanceOf<MoveOutcome.Applied>()
        outcome.state.pendingHint shouldBe hints.hintFor(state)
        outcome.state.hintsUsed shouldBe 1
        outcome.state.moves shouldBe 0
        outcome.state.score shouldBe state.score - 200
        outcome.events shouldBe listOf(ScoreEvent.HintUsed)
        outcome.state.board shouldBe state.board
    }

    @Test
    fun `one hint at a time, and applying needs one on show`() {
        engine.apply(state, Move.ApplyHint) shouldBe MoveOutcome.Rejected
        engine.apply(state.play(Move.RevealHint), Move.RevealHint) shouldBe MoveOutcome.Rejected
    }

    @Test
    fun `applying a placement hint writes its digit without a mistake`() {
        val shown = state.play(Move.RevealHint)
        val hint = shown.pendingHint.shouldBeInstanceOf<Hint.Placement>()
        val applied = shown.play(Move.ApplyHint)
        applied.board.values[hint.cell] shouldBe hint.digit
        applied.pendingHint.shouldBeNull()
        applied.mistakes shouldBe 0
        applied.moves shouldBe 1
        applied.hintsUsed shouldBe 1
    }

    @Test
    fun `applying a wrong-digit hint erases it`() {
        val shown = state.play(Move.Place(3, wrongDigit(3))).play(Move.RevealHint)
        shown.play(Move.ApplyHint).board.values[3] shouldBe 0
    }

    @Test
    fun `any other move makes the hint stale`() {
        val shown = state.play(Move.RevealHint)
        shown.play(Move.ToggleNote(3, 1)).pendingHint.shouldBeNull()
    }

    @Test
    fun `revealing can't be undone, and undo clears the hint`() {
        val session = GameSession(1, state).play(Move.ToggleNote(3, 1), engine).shouldNotBeNull()
        val shown = session.play(Move.RevealHint, engine).shouldNotBeNull()
        shown.history shouldBe session.history
        val undone = shown.undo().shouldNotBeNull()
        undone.state.pendingHint.shouldBeNull()
        undone.state.hintsUsed shouldBe 1
        undone.redo().shouldNotBeNull().state.pendingHint.shouldBeNull()
    }

    @Test
    fun `hints follow the techniques in sudoku X and Killer too`() {
        listOf(
            newState(Variant.X),
            newState(Variant.KILLER),
        ).forEach { s ->
            val hint = hints.hintFor(s).shouldBeInstanceOf<Hint.Placement>()
            hint.digit shouldBe SOLUTION[hint.cell]
            (hint.technique in TechniqueKind.entries) shouldBe true
        }
    }

    @Test
    fun `a hint that needs eliminations first names the hardest of them, not the final single`() = runTest {
        val mode = GameMode(Variant.CLASSIC, Difficulty.EXPERT)
        val puzzle = SeededPuzzleGenerator().generate(mode, 5)
        // Play singles until only eliminations help.
        var state = engine.newGame(puzzle, mode)
        val context = SolverContext(puzzle, Variant.CLASSIC)
        while (true) {
            val candidates = Candidates.of(state.board.values, context)
            val single = NakedSingle.find(candidates, context) ?: HiddenSingle.find(candidates, context) ?: break
            single as Deduction.Placement
            state = state.play(Move.Place(single.cell, single.digit))
        }
        val hint = hints.hintFor(state).shouldBeInstanceOf<Hint.Placement>()
        hint.digit shouldBe puzzle.solution[hint.cell]
        (hint.technique!!.weight > TechniqueKind.HIDDEN_SINGLE.weight) shouldBe true
        (hint.cell in hint.cells) shouldBe true
    }
}
