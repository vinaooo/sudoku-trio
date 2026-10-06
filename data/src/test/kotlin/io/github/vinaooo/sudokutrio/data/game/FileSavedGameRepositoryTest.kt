package io.github.vinaooo.sudokutrio.data.game

import io.github.vinaooo.sudokutrio.domain.model.Cage
import io.github.vinaooo.sudokutrio.domain.model.Difficulty
import io.github.vinaooo.sudokutrio.domain.model.GameMode
import io.github.vinaooo.sudokutrio.domain.model.Move
import io.github.vinaooo.sudokutrio.domain.model.Puzzle
import io.github.vinaooo.sudokutrio.domain.model.Variant
import io.github.vinaooo.sudokutrio.domain.rules.GameEngine
import io.github.vinaooo.sudokutrio.domain.session.GameSession
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import java.io.File
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class FileSavedGameRepositoryTest {
    @TempDir
    lateinit var dir: File

    private val dispatcher = StandardTestDispatcher()
    private val file get() = File(dir, "saved_game.json")
    private val repository get() = FileSavedGameRepository(file, dispatcher)
    private val engine = GameEngine()

    private val solution = (
        "249368715356971824781542639512783496467195283893426571928614357675839142134257968"
        ).map { it.digitToInt() }
    private val puzzle = Puzzle(
        givens = solution.mapIndexed { cell, digit -> if (cell % 3 == 0) 0 else digit },
        solution = solution,
        cages = listOf(Cage(15, listOf(0, 1, 2))),
    )
    private val session = GameSession(11, engine.newGame(puzzle, GameMode(Variant.KILLER, Difficulty.MEDIUM)))
        .play(Move.ToggleNote(0, 4), engine).shouldNotBeNull()
        .play(Move.Place(3, 1), engine).shouldNotBeNull()
        .play(Move.RevealHint, engine).shouldNotBeNull()

    @Test
    fun `nothing saved loads as null`() = runTest(dispatcher) {
        repository.load().shouldBeNull()
    }

    @Test
    fun `a saved session loads back with its undo history and hint`() = runTest(dispatcher) {
        repository.save(session)

        val loaded = FileSavedGameRepository(file, dispatcher).load()

        loaded shouldBe session
        loaded!!.undo()!!.state.board shouldBe session.undo()!!.state.board
    }

    @Test
    fun `saving again replaces the previous game`() = runTest(dispatcher) {
        repository.save(session)
        val newer = session.play(Move.ApplyHint, engine).shouldNotBeNull()
        repository.save(newer)

        repository.load() shouldBe newer
    }

    @Test
    fun `clear removes the saved game`() = runTest(dispatcher) {
        repository.save(session)
        repository.clear()

        repository.load().shouldBeNull()
        file.exists().shouldBeFalse()
    }

    @Test
    fun `a corrupted file is discarded instead of crashing`() = runTest(dispatcher) {
        file.writeText("{ not json")

        repository.load().shouldBeNull()
        file.exists().shouldBeFalse()
    }

    @Test
    fun `a file that breaks the puzzle's rules is discarded`() = runTest(dispatcher) {
        repository.save(session)
        file.writeText(file.readText().replaceFirst("\"givens\":[0,4", "\"givens\":[0,5"))

        repository.load().shouldBeNull()
        file.exists().shouldBeFalse()
    }

    @Test
    fun `a file from an unknown future version is ignored`() = runTest(dispatcher) {
        repository.save(session)
        file.writeText(file.readText().replace("\"version\":1", "\"version\":99"))

        repository.load().shouldBeNull()
    }

    @Test
    fun `no temporary files are left behind`() = runTest(dispatcher) {
        repository.save(session)

        dir.listFiles()!!.map { it.name } shouldBe listOf("saved_game.json")
    }
}
