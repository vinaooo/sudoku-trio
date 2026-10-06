package io.github.vinaooo.sudokutrio.feature.settings

import io.github.vinaooo.sudokutrio.domain.fake.FakeSavedGameRepository
import io.github.vinaooo.sudokutrio.domain.fake.FakeSettingsRepository
import io.github.vinaooo.sudokutrio.domain.model.Difficulty
import io.github.vinaooo.sudokutrio.domain.model.GameMode
import io.github.vinaooo.sudokutrio.domain.model.Move
import io.github.vinaooo.sudokutrio.domain.model.Puzzle
import io.github.vinaooo.sudokutrio.domain.model.Settings
import io.github.vinaooo.sudokutrio.domain.model.ThemeColor
import io.github.vinaooo.sudokutrio.domain.model.ThemeMode
import io.github.vinaooo.sudokutrio.domain.model.Variant
import io.github.vinaooo.sudokutrio.domain.rules.GameEngine
import io.github.vinaooo.sudokutrio.domain.session.GameSession
import io.github.vinaooo.sudokutrio.domain.usecase.ResumeGame
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SettingsViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val repository = FakeSettingsRepository()
    private val saved = FakeSavedGameRepository()
    private val engine = GameEngine()
    private val solution = (
        "249368715356971824781542639512783496467195283893426571928614357675839142134257968"
        ).map { it.digitToInt() }
    private val fresh = GameSession(
        1,
        engine.newGame(Puzzle(solution.mapIndexed { i, d -> if (i % 3 == 0) 0 else d }, solution), Settings().mode),
    )

    @BeforeEach
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterEach
    fun tearDown() = Dispatchers.resetMain()

    private fun TestScope.viewModel() = SettingsViewModel(repository, ResumeGame(saved)).also {
        backgroundScope.launch { it.settings.collect {} }
        runCurrent()
    }

    @Test
    fun `an appearance change is saved at once`() = runTest(dispatcher) {
        val vm = viewModel()
        vm.onChange(SettingsChange.ThemeModeChanged(ThemeMode.DARK))
        vm.onChange(SettingsChange.ThemeColorChanged(ThemeColor.ORANGE))
        runCurrent()
        repository.settings.value.themeMode shouldBe ThemeMode.DARK
        repository.settings.value.themeColor shouldBe ThemeColor.ORANGE
        vm.pendingChange.value.shouldBeNull()
    }

    @Test
    fun `a new variant or difficulty with no game in progress is saved at once`() = runTest(dispatcher) {
        saved.saved = fresh
        val vm = viewModel()
        vm.onChange(SettingsChange.VariantChanged(Variant.KILLER))
        runCurrent()
        vm.onChange(SettingsChange.DifficultyChanged(Difficulty.EXPERT))
        runCurrent()
        repository.settings.value.mode shouldBe GameMode(Variant.KILLER, Difficulty.EXPERT)
    }

    @Test
    fun `with a game in progress, a mode change waits for confirmation`() = runTest(dispatcher) {
        saved.saved = fresh.play(Move.ToggleNote(0, 1), engine)
        val vm = viewModel()
        vm.onChange(SettingsChange.VariantChanged(Variant.X))
        runCurrent()
        vm.pendingChange.value shouldBe SettingsChange.VariantChanged(Variant.X)
        repository.settings.value.mode.variant shouldBe Variant.CLASSIC
        vm.confirmChange()
        runCurrent()
        repository.settings.value.mode.variant shouldBe Variant.X
        vm.pendingChange.value.shouldBeNull()
    }

    @Test
    fun `a mode change can be called off, and choosing the current mode asks nothing`() = runTest(dispatcher) {
        saved.saved = fresh.play(Move.ToggleNote(0, 1), engine)
        val vm = viewModel()
        vm.onChange(SettingsChange.DifficultyChanged(Difficulty.HARD))
        runCurrent()
        vm.dismissChange()
        vm.pendingChange.value.shouldBeNull()
        repository.settings.value.mode.difficulty shouldBe Difficulty.EASY
        vm.onChange(SettingsChange.DifficultyChanged(Difficulty.EASY))
        runCurrent()
        vm.pendingChange.value.shouldBeNull()
    }
}
