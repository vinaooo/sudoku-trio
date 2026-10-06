package io.github.vinaooo.sudokutrio.feature.game

import io.github.vinaooo.sudokutrio.domain.fake.FakeSavedGameRepository
import io.github.vinaooo.sudokutrio.domain.fake.FakeScoreRepository
import io.github.vinaooo.sudokutrio.domain.fake.FakeSettingsRepository
import io.github.vinaooo.sudokutrio.domain.fake.FakeStatsRepository
import io.github.vinaooo.sudokutrio.domain.generator.PuzzleGenerator
import io.github.vinaooo.sudokutrio.domain.model.Difficulty
import io.github.vinaooo.sudokutrio.domain.model.GameMode
import io.github.vinaooo.sudokutrio.domain.model.GameStats
import io.github.vinaooo.sudokutrio.domain.model.Hint
import io.github.vinaooo.sudokutrio.domain.model.Move
import io.github.vinaooo.sudokutrio.domain.model.Puzzle
import io.github.vinaooo.sudokutrio.domain.model.Settings
import io.github.vinaooo.sudokutrio.domain.model.Variant
import io.github.vinaooo.sudokutrio.domain.rules.ConflictFinder
import io.github.vinaooo.sudokutrio.domain.rules.GameEngine
import io.github.vinaooo.sudokutrio.domain.session.GameSession
import io.github.vinaooo.sudokutrio.domain.usecase.AbandonGame
import io.github.vinaooo.sudokutrio.domain.usecase.FinishGame
import io.github.vinaooo.sudokutrio.domain.usecase.PreparePuzzle
import io.github.vinaooo.sudokutrio.domain.usecase.RestartGame
import io.github.vinaooo.sudokutrio.domain.usecase.ResumeGame
import io.github.vinaooo.sudokutrio.domain.usecase.SaveGame
import io.github.vinaooo.sudokutrio.domain.usecase.StartNewGame
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class GameViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val savedGames = FakeSavedGameRepository()
    private val stats = FakeStatsRepository()
    private val scores = FakeScoreRepository()
    private val settings = FakeSettingsRepository()
    private val feedback = FakeGameFeedback()
    private val engine = GameEngine()

    private val solution = (
        "249368715356971824781542639512783496467195283893426571928614357675839142134257968"
        ).map { it.digitToInt() }

    /** Every third cell empty: cell 0 is empty, cell 1 a given. */
    private val puzzle = Puzzle(solution.mapIndexed { cell, d -> if (cell % 3 == 0) 0 else d }, solution)
    private val lastCell = Puzzle(solution.mapIndexed { cell, d -> if (cell == 0) 0 else d }, solution)
    private val classic = GameMode(Variant.CLASSIC, Difficulty.EASY)
    private val generated = mutableListOf<GameMode>()

    /** Gates generation: a test can hold it to see the loading state, then let it go. */
    private var gate: CompletableDeferred<Unit>? = null
    private var nextPuzzle = puzzle
    private val generator = PuzzleGenerator { mode, _ ->
        gate?.await()
        generated += mode
        nextPuzzle
    }

    @BeforeEach
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterEach
    fun tearDown() = Dispatchers.resetMain()

    private val created = mutableListOf<GameViewModel>()

    /** Like runTest, but pauses every ViewModel at the end, even on failure, so the clock loop stops. */
    private fun gameTest(block: suspend TestScope.() -> Unit) = runTest(dispatcher) {
        try {
            block()
        } finally {
            created.forEach { it.onIntent(GameIntent.Pause) }
        }
    }

    private fun TestScope.viewModel(search: kotlinx.coroutines.CoroutineDispatcher = dispatcher): GameViewModel {
        val abandon = AbandonGame(savedGames, stats)
        return GameViewModel(
            startNewGame = StartNewGame(abandon, savedGames, generator, { 42L }, engine, dispatcher),
            preparePuzzle = PreparePuzzle(generator, { 43L }, dispatcher),
            restartGame = RestartGame(abandon, savedGames, engine),
            resumeGame = ResumeGame(savedGames),
            saveGame = SaveGame(savedGames),
            finishGame = FinishGame(scores, stats, savedGames) { 5_000L },
            settingsRepository = settings,
            engine = engine,
            conflicts = ConflictFinder(),
            feedback = feedback,
            searchDispatcher = search,
        ).also {
            created += it
            runCurrent()
        }
    }

    private val GameViewModel.state get() = uiState.value
    private val GameViewModel.session get() = state.session!!

    private fun TestScope.play(vm: GameViewModel, vararg intents: GameIntent) {
        intents.forEach { vm.onIntent(it) }
        runCurrent()
    }

    @Test
    fun `with nothing saved, a puzzle is generated in the chosen mode and saved`() = gameTest {
        settings.settings.value = Settings(mode = GameMode(Variant.KILLER, Difficulty.HARD))
        val vm = viewModel()
        // The game, then the next one prepared in the background.
        generated shouldContainExactly List(2) { GameMode(Variant.KILLER, Difficulty.HARD) }
        vm.state.loading shouldBe false
        vm.session.state.mode shouldBe GameMode(Variant.KILLER, Difficulty.HARD)
        savedGames.saved shouldBe vm.session
    }

    @Test
    fun `the saved game in progress is resumed, not regenerated`() = gameTest {
        val saved = GameSession(9, engine.newGame(puzzle, classic)).play(Move.ToggleNote(0, 3), engine)!!
        savedGames.saved = saved
        val vm = viewModel()
        vm.session shouldBe saved
        generated shouldBe listOf(classic)
    }

    @Test
    fun `while a puzzle is generated the screen shows it is loading`() = gameTest {
        gate = CompletableDeferred()
        val vm = viewModel()
        vm.state.loading shouldBe true
        vm.state.session.shouldBeNull()
        gate!!.complete(Unit)
        runCurrent()
        vm.state.loading shouldBe false
    }

    @Test
    fun `a digit goes in the selected cell, and nowhere without one`() = gameTest {
        val vm = viewModel()
        play(vm, GameIntent.Digit(2))
        vm.session.state.moves shouldBe 0
        play(vm, GameIntent.SelectCell(0), GameIntent.Digit(2))
        vm.session.state.board.values[0] shouldBe 2
        vm.state.selected shouldBe 0
        feedback.sounds shouldContainExactly listOf(FeedbackEvent.MOVE)
        savedGames.saved shouldBe vm.session
        vm.state.announcement?.announcement shouldBe Announcement.Placed(0, 2)
    }

    @Test
    fun `a wrong digit sounds and reads exactly like a right one`() = gameTest {
        val vm = viewModel()
        play(vm, GameIntent.SelectCell(0), GameIntent.Digit(5))
        vm.session.state.mistakes shouldBe 1
        feedback.sounds shouldContainExactly listOf(FeedbackEvent.MOVE)
        feedback.haptics shouldContainExactly listOf(FeedbackEvent.MOVE)
        vm.state.announcement?.announcement shouldBe Announcement.Placed(0, 5)
    }

    @Test
    fun `typing over a given is refused`() = gameTest {
        val vm = viewModel()
        play(vm, GameIntent.SelectCell(1), GameIntent.Digit(5))
        vm.session.state.moves shouldBe 0
        feedback.sounds shouldContainExactly listOf(FeedbackEvent.REJECTED)
    }

    @Test
    fun `sound and vibration follow the settings`() = gameTest {
        settings.settings.value = Settings(soundEnabled = false)
        val vm = viewModel()
        play(vm, GameIntent.SelectCell(0), GameIntent.Digit(2))
        feedback.sounds shouldBe emptyList()
        feedback.haptics shouldContainExactly listOf(FeedbackEvent.MOVE)
    }

    @Test
    fun `in notes mode digits toggle pencil marks`() = gameTest {
        val vm = viewModel()
        play(vm, GameIntent.ToggleNotes, GameIntent.SelectCell(0), GameIntent.Digit(3), GameIntent.Digit(7))
        vm.state.notesMode shouldBe true
        vm.session.state.board.notes[0] shouldBe setOf(3, 7)
        play(vm, GameIntent.Digit(3))
        vm.session.state.board.notes[0] shouldBe setOf(7)
        vm.state.announcement?.announcement shouldBe Announcement.Noted(0, 3, added = false)
        play(vm, GameIntent.ToggleNotes, GameIntent.Digit(2))
        vm.session.state.board.values[0] shouldBe 2
    }

    @Test
    fun `erase clears the selected cell`() = gameTest {
        val vm = viewModel()
        play(vm, GameIntent.SelectCell(0), GameIntent.Digit(5), GameIntent.Erase)
        vm.session.state.board.values[0] shouldBe 0
        vm.state.announcement?.announcement shouldBe Announcement.Erased(0)
    }

    @Test
    fun `conflicts follow the board`() = gameTest {
        val vm = viewModel()
        // Cell 1 holds 4: a 4 in cell 0 repeats it in row 0.
        play(vm, GameIntent.SelectCell(0), GameIntent.Digit(4))
        vm.state.conflicts.contains(0) shouldBe true
        vm.state.conflicts.contains(1) shouldBe true
        play(vm, GameIntent.Undo)
        vm.state.conflicts shouldBe emptySet()
    }

    @Test
    fun `undo and redo step the board and save`() = gameTest {
        val vm = viewModel()
        play(vm, GameIntent.SelectCell(0), GameIntent.Digit(2), GameIntent.Undo)
        vm.session.state.board.values[0] shouldBe 0
        savedGames.saved shouldBe vm.session
        vm.state.announcement?.announcement shouldBe Announcement.Undone
        play(vm, GameIntent.Redo)
        vm.session.state.board.values[0] shouldBe 2
        vm.state.announcement?.announcement shouldBe Announcement.Redone
    }

    @Test
    fun `the first hint tap shows the hint, selects its cell and saves, the second carries it out`() = gameTest {
        val vm = viewModel()
        play(vm, GameIntent.Hint)
        val hint = vm.session.state.pendingHint.shouldBeInstanceOf<Hint.Placement>()
        vm.session.state.hintsUsed shouldBe 1
        vm.state.selected shouldBe hint.cell
        savedGames.saved shouldBe vm.session
        vm.state.announcement?.announcement shouldBe Announcement.Hinted(hint)
        play(vm, GameIntent.Hint)
        vm.session.state.board.values[hint.cell] shouldBe hint.digit
        vm.session.state.pendingHint.shouldBeNull()
        vm.session.state.hintsUsed shouldBe 1
        vm.state.announcement?.announcement shouldBe Announcement.Placed(hint.cell, hint.digit)
    }

    @Test
    fun `a move made while the hint is being found makes it stale`() = gameTest {
        val vm = viewModel()
        vm.onIntent(GameIntent.Hint)
        vm.onIntent(GameIntent.SelectCell(3))
        vm.onIntent(GameIntent.Digit(3))
        runCurrent()
        vm.session.state.pendingHint.shouldBeNull()
        vm.session.state.hintsUsed shouldBe 0
        vm.session.state.board.values[3] shouldBe 3
    }

    @Test
    fun `filling the last cell wins, finishes the game once and clears the save`() = gameTest {
        nextPuzzle = lastCell
        val vm = viewModel()
        play(vm, GameIntent.SelectCell(0), GameIntent.Digit(2))
        vm.session.state.isWon shouldBe true
        vm.state.winRecord.shouldNotBeNull().points shouldBe vm.session.state.score
        scores.records.value.size shouldBe 1
        stats.observe(classic).first() shouldBe GameStats(played = 1, won = 1, currentStreak = 1, bestStreak = 1)
        savedGames.saved.shouldBeNull()
        feedback.sounds shouldContainExactly listOf(FeedbackEvent.WIN)
        play(vm, GameIntent.Undo, GameIntent.Pause)
        scores.records.value.size shouldBe 1
        savedGames.saved.shouldBeNull()
    }

    @Test
    fun `a new game counts the unfinished one as a loss and starts another`() = gameTest {
        val vm = viewModel()
        play(vm, GameIntent.SelectCell(0), GameIntent.Digit(2))
        val before = vm.session
        play(vm, GameIntent.NewGame)
        vm.session shouldNotBe before
        vm.session.state.moves shouldBe 0
        vm.state.selected.shouldBeNull()
        stats.observe(classic).first().played shouldBe 1
    }

    @Test
    fun `a new game starts at once with the puzzle prepared in the background`() = gameTest {
        val vm = viewModel()
        generated.size shouldBe 2
        play(vm, GameIntent.NewGame)
        vm.state.loading shouldBe false
        vm.session.seed shouldBe 43L
        // Only the one after it is generated now.
        generated.size shouldBe 3
    }

    @Test
    fun `a new game waits for the puzzle still being prepared, and a second one replaces it`() = gameTest {
        val vm = viewModel()
        gate = CompletableDeferred()
        play(vm, GameIntent.NewGame, GameIntent.NewGame)
        vm.state.loading shouldBe true
        play(vm, GameIntent.NewGame)
        gate!!.complete(Unit)
        advanceUntilIdle()
        vm.state.loading shouldBe false
    }

    @Test
    fun `restart plays the same puzzle from its givens`() = gameTest {
        val vm = viewModel()
        play(vm, GameIntent.SelectCell(0), GameIntent.Digit(2), GameIntent.Restart)
        vm.session.state.board.values shouldBe puzzle.givens
        vm.session.state.puzzle shouldBe puzzle
        generated.size shouldBe 2
    }

    @Test
    fun `changing the mode in settings starts a new game in it`() = gameTest {
        val vm = viewModel()
        settings.update { it.copy(mode = GameMode(Variant.X, Difficulty.MEDIUM)) }
        runCurrent()
        vm.session.state.mode shouldBe GameMode(Variant.X, Difficulty.MEDIUM)
        // The classic puzzle prepared before isn't used: one in the new mode is made, and the next one prepared.
        generated shouldBe
            listOf(classic, classic, GameMode(Variant.X, Difficulty.MEDIUM), GameMode(Variant.X, Difficulty.MEDIUM))
        settings.update { it.copy(soundEnabled = false) }
        runCurrent()
        generated.size shouldBe 4
    }

    @Test
    fun `the clock runs from the moment the board shows, only while the screen is visible`() = gameTest {
        val vm = viewModel()
        advanceTimeBy(5_500)
        vm.session.state.elapsedSeconds shouldBe 0
        play(vm, GameIntent.Resume)
        advanceTimeBy(3_500)
        vm.session.state.elapsedSeconds shouldBe 3
        play(vm, GameIntent.Pause)
        savedGames.saved shouldBe vm.session
        advanceTimeBy(5_000)
        vm.session.state.elapsedSeconds shouldBe 3
    }

    @Test
    fun `the clock stops while a puzzle is generated and once the game is won`() = gameTest {
        nextPuzzle = lastCell
        val vm = viewModel()
        play(vm, GameIntent.Resume, GameIntent.SelectCell(0), GameIntent.Digit(2))
        advanceTimeBy(3_500)
        vm.session.state.elapsedSeconds shouldBe 0
        gate = CompletableDeferred()
        settings.update { it.copy(mode = GameMode(Variant.KILLER, Difficulty.EXPERT)) }
        runCurrent()
        advanceTimeBy(3_500)
        vm.state.loading shouldBe true
        vm.session.state.elapsedSeconds shouldBe 0
    }

    @Test
    fun `after a win, the next game's clock runs`() = gameTest {
        nextPuzzle = lastCell
        val vm = viewModel()
        play(vm, GameIntent.Resume, GameIntent.SelectCell(0), GameIntent.Digit(2))
        vm.session.state.isWon shouldBe true
        nextPuzzle = puzzle
        settings.update { it.copy(mode = GameMode(Variant.X, Difficulty.EASY)) }
        runCurrent()
        advanceTimeBy(2_500)
        vm.session.state.isWon shouldBe false
        vm.session.state.elapsedSeconds shouldBe 2
    }

    @Test
    fun `a clock tick during the hint search keeps the hint and charges the second`() = gameTest {
        // Its own scheduler: without one it would share the main one, and advanceUntilIdle would run the clock forever.
        val search = StandardTestDispatcher(kotlinx.coroutines.test.TestCoroutineScheduler())
        val vm = viewModel(search)
        val start = vm.session.state.score
        play(vm, GameIntent.Resume, GameIntent.Hint)
        advanceTimeBy(1_500)
        search.scheduler.advanceUntilIdle()
        runCurrent()
        vm.session.state.pendingHint.shouldNotBeNull()
        vm.session.state.elapsedSeconds shouldBe 1
        vm.session.state.score shouldBe start - 1 - 200
    }
}
