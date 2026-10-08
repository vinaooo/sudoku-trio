package io.github.vinaooo.sudokutrio.domain.session

import io.github.vinaooo.sudokutrio.domain.generator.SeededPuzzleGenerator
import io.github.vinaooo.sudokutrio.domain.model.Difficulty
import io.github.vinaooo.sudokutrio.domain.model.GameMode
import io.github.vinaooo.sudokutrio.domain.model.Grid
import io.github.vinaooo.sudokutrio.domain.model.Move
import io.github.vinaooo.sudokutrio.domain.model.Variant
import io.github.vinaooo.sudokutrio.domain.rules.GameEngine
import io.kotest.matchers.ints.shouldBeLessThan
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test

class BoardCodecTest {
    private val engine = GameEngine()

    /** A Killer game half played, with notes in every empty cell and a hint on show. */
    private suspend fun midGame(): GameSession {
        val mode = GameMode(Variant.KILLER, Difficulty.MEDIUM)
        var session = GameSession(3, engine.newGame(SeededPuzzleGenerator().generate(mode, 3), mode))
        Grid.CELLS.filter { session.state.board.values[it] == 0 }.forEachIndexed { i, cell ->
            val move = if (i % 2 ==
                0
            ) {
                Move.Place(cell, session.state.puzzle.solution[cell])
            } else {
                Move.ToggleNote(cell, 1 + i % 9)
            }
            session = session.play(move, engine) ?: session
        }
        return session.play(Move.RevealHint, engine).shouldNotBeNull()
    }

    @Test
    fun `a state survives the codec`() = runTest {
        val state = midGame().state
        BoardCodec.decode(BoardCodec.encode(state)) shouldBe state
    }

    @Test
    fun `the code is short enough for an issue and tolerates line breaks`() = runTest {
        val code = BoardCodec.encode(midGame().state)
        code.length shouldBeLessThan 1_500
        BoardCodec.decode(code.chunked(60).joinToString("\n", postfix = "\n")) shouldBe BoardCodec.decode(code)
    }

    @Test
    fun `a whole session comes back from its JSON`() = runTest {
        val session = midGame()
        decodeSession(Json.encodeToString(GameSession.serializer(), session)) shouldBe session
    }
}
