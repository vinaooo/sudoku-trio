package io.github.vinaooo.sudokutrio.domain.rules

import io.github.vinaooo.sudokutrio.domain.model.Grid
import io.github.vinaooo.sudokutrio.domain.model.Move
import io.github.vinaooo.sudokutrio.domain.model.Variant
import io.github.vinaooo.sudokutrio.domain.newState
import io.github.vinaooo.sudokutrio.domain.session.GameSession
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.enum
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.long
import io.kotest.property.checkAll
import kotlin.random.Random
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class GameInvariantsPropertyTest {
    private val engine = GameEngine()

    /** Any move, legal or not: cells and digits a little outside the board's range too. */
    private fun randomMove(random: Random): Move {
        val cell = random.nextInt(-1, Grid.SIZE + 1)
        val digit = random.nextInt(0, Grid.SIDE + 2)
        return when (random.nextInt(MOVE_KINDS)) {
            0 -> Move.Place(cell, digit)
            1 -> Move.ToggleNote(cell, digit)
            2 -> Move.Erase(cell)
            3 -> Move.RevealHint
            else -> Move.ApplyHint
        }
    }

    @Test
    fun `any sequence of moves keeps the givens, the counters and the board consistent`() = runTest {
        checkAll(ITERATIONS, Arb.enum<Variant>(), Arb.long()) { variant, seed ->
            val random = Random(seed)
            var session = GameSession(seed, newState(variant))
            repeat(STEPS) {
                val move = randomMove(random)
                val legal = engine.isLegal(session.state, move)
                legal shouldBe (move in engine.legalMoves(session.state))
                val before = session
                val played = session.play(move, engine)
                (played != null) shouldBe legal
                if (played != null) {
                    played.state.moves shouldBe before.state.moves + if (move == Move.RevealHint) 0 else 1
                    if (played.state.board != before.state.board) {
                        played.undo().shouldNotBeNull().state.board shouldBe before.state.board
                    }
                    session = played
                }
                val state = session.state
                Grid.CELLS.filter { state.puzzle.isGiven(it) }.forEach {
                    state.board.values[it] shouldBe state.puzzle.givens[it]
                }
                Grid.CELLS.filter { state.board.values[it] != 0 }.forEach { state.board.notes[it] shouldBe emptySet() }
                (state.score >= 0) shouldBe true
            }
        }
    }

    @Test
    fun `undoing every move and redoing them all gives the same board`() = runTest {
        checkAll(ITERATIONS, Arb.enum<Variant>(), Arb.list(Arb.int(0, Int.MAX_VALUE), 1..STEPS)) { variant, picks ->
            var session = GameSession(0, newState(variant))
            picks.forEach { pick ->
                val legal = engine.legalMoves(session.state)
                if (legal.isNotEmpty()) session = session.play(legal[pick % legal.size], engine).shouldNotBeNull()
            }
            var rewound = session
            while (rewound.canUndo) rewound = rewound.undo().shouldNotBeNull()
            rewound.state.board shouldBe newState(variant).board
            var replayed = rewound
            while (replayed.canRedo) replayed = replayed.redo().shouldNotBeNull()
            replayed.state.board shouldBe session.state.board
        }
    }

    private companion object {
        const val MOVE_KINDS = 5
        const val ITERATIONS = 100
        const val STEPS = 30
    }
}
