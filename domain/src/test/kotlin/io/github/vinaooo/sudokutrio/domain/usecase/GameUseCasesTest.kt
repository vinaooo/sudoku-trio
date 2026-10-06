package io.github.vinaooo.sudokutrio.domain.usecase

import io.github.vinaooo.sudokutrio.domain.SOLUTION
import io.github.vinaooo.sudokutrio.domain.fake.FakeSavedGameRepository
import io.github.vinaooo.sudokutrio.domain.fake.FakeScoreRepository
import io.github.vinaooo.sudokutrio.domain.fake.FakeStatsRepository
import io.github.vinaooo.sudokutrio.domain.generator.PuzzleGenerator
import io.github.vinaooo.sudokutrio.domain.mode
import io.github.vinaooo.sudokutrio.domain.model.Difficulty
import io.github.vinaooo.sudokutrio.domain.model.GameStats
import io.github.vinaooo.sudokutrio.domain.model.Move
import io.github.vinaooo.sudokutrio.domain.model.ScoreRecord
import io.github.vinaooo.sudokutrio.domain.model.Variant
import io.github.vinaooo.sudokutrio.domain.newState
import io.github.vinaooo.sudokutrio.domain.puzzle
import io.github.vinaooo.sudokutrio.domain.rules.GameEngine
import io.github.vinaooo.sudokutrio.domain.session.GameSession
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class GameUseCasesTest {
    private val engine = GameEngine()
    private val saved = FakeSavedGameRepository()
    private val stats = FakeStatsRepository()
    private val scores = FakeScoreRepository()
    private val generated = mutableListOf<Pair<Any, Long>>()
    private val generator = PuzzleGenerator { mode, seed ->
        generated += mode to seed
        puzzle()
    }
    private val abandon = AbandonGame(saved, stats)

    private fun inProgress(variant: Variant = Variant.X) =
        GameSession(9, newState(variant)).play(Move.ToggleNote(0, 1), engine).shouldNotBeNull()

    @Test
    fun `a new game is generated from a fresh seed, in the chosen mode, and saved`() = runTest {
        val start = StartNewGame(abandon, saved, generator, { 77L }, engine, StandardTestDispatcher(testScheduler))
        val session = start(mode(Variant.KILLER, Difficulty.HARD))
        session.seed shouldBe 77
        session.state.mode shouldBe mode(Variant.KILLER, Difficulty.HARD)
        session.state.board.values shouldBe puzzle().givens
        session.state.score shouldBe 6000
        generated shouldBe listOf(mode(Variant.KILLER, Difficulty.HARD) to 77L)
        saved.saved shouldBe session
    }

    @Test
    fun `a new game counts an unfinished one as a loss in its own mode`() = runTest {
        saved.saved = inProgress()
        StartNewGame(abandon, saved, generator, { 1L }, engine, StandardTestDispatcher(testScheduler))(mode())
        stats.observe(mode(Variant.X)).first() shouldBe GameStats(played = 1)
        stats.observe(mode()).first() shouldBe GameStats()
        ObservePlayedModes(stats)().first() shouldBe setOf(mode(Variant.X))
    }

    @Test
    fun `an untouched or won game isn't a loss`() = runTest {
        saved.saved = GameSession(1, newState())
        abandon()
        val won = GameSession(2, newState(empty = listOf(0))).play(Move.Place(0, SOLUTION[0]), engine)
        saved.saved = won
        abandon()
        stats.stats.value shouldBe emptyMap()
    }

    @Test
    fun `restart plays the same puzzle again from its givens, as a loss of the unfinished one`() = runTest {
        val session = inProgress()
        saved.saved = session
        val restarted = RestartGame(abandon, saved, engine)(session)
        restarted.seed shouldBe session.seed
        restarted.state shouldBe engine.newGame(session.state.puzzle, session.state.mode)
        saved.saved shouldBe restarted
        stats.observe(session.state.mode).first().played shouldBe 1
    }

    @Test
    fun `resume returns the saved game unless it was won`() = runTest {
        ResumeGame(saved)().shouldBeNull()
        val session = inProgress()
        SaveGame(saved)(session)
        ResumeGame(saved)() shouldBe session
        saved.saved = GameSession(2, newState(empty = emptyList()))
        ResumeGame(saved)().shouldBeNull()
    }

    @Test
    fun `finishing records the score and the win, and clears the save`() = runTest {
        val won = GameSession(2, newState(empty = listOf(0)).copy(mistakes = 2, hintsUsed = 1))
            .tick(30, engine).play(Move.Place(0, SOLUTION[0]), engine).shouldNotBeNull()
        saved.saved = won
        val record = FinishGame(scores, stats, saved) { 1234L }(won)
        record shouldBe ScoreRecord(mode(), won.state.score, 30, 2, 1, 1234)
        scores.records.value shouldBe listOf(record)
        stats.observe(mode()).first() shouldBe GameStats(played = 1, won = 1, currentStreak = 1, bestStreak = 1)
        saved.saved.shouldBeNull()
        ObserveTopScores(scores)(mode()).first() shouldBe listOf(record)
        ObservePlayedModes(stats)().first() shouldBe setOf(mode())
        ObserveStats(stats)(mode()).first().won shouldBe 1
    }

    @Test
    fun `only a won game can be finished`() = runTest {
        shouldThrow<IllegalStateException> { FinishGame(scores, stats, saved) { 0L }(inProgress()) }
    }

    @Test
    fun `a puzzle can be prepared ahead, with the seed it was made from`() = runTest {
        val prepare = PreparePuzzle(generator, { 99L }, StandardTestDispatcher(testScheduler))
        val prepared = prepare(mode(Variant.X))
        prepared shouldBe PreparedPuzzle(mode(Variant.X), 99L, puzzle())
        generated shouldBe listOf(mode(Variant.X) to 99L)
    }

    @Test
    fun `a new game uses the prepared puzzle of its mode without generating again`() = runTest {
        saved.saved = inProgress()
        val start = StartNewGame(abandon, saved, generator, { 1L }, engine, StandardTestDispatcher(testScheduler))
        val prepared = PreparedPuzzle(mode(Variant.KILLER), 55L, puzzle(empty = listOf(0)))
        val session = start(mode(Variant.KILLER), prepared)
        session.seed shouldBe 55L
        session.state.puzzle shouldBe prepared.puzzle
        generated shouldBe emptyList()
        stats.observe(mode(Variant.X)).first().played shouldBe 1
        saved.saved shouldBe session
    }

    @Test
    fun `a prepared puzzle of another mode is ignored`() = runTest {
        val start = StartNewGame(abandon, saved, generator, { 1L }, engine, StandardTestDispatcher(testScheduler))
        val session = start(mode(Variant.CLASSIC), PreparedPuzzle(mode(Variant.KILLER), 55L, puzzle(empty = listOf(0))))
        session.seed shouldBe 1L
        generated shouldBe listOf(mode(Variant.CLASSIC) to 1L)
    }
}
