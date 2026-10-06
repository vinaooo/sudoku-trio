package io.github.vinaooo.sudokutrio.feature.scores

import app.cash.turbine.test
import io.github.vinaooo.sudokutrio.domain.fake.FakeScoreRepository
import io.github.vinaooo.sudokutrio.domain.fake.FakeStatsRepository
import io.github.vinaooo.sudokutrio.domain.model.Difficulty
import io.github.vinaooo.sudokutrio.domain.model.GameMode
import io.github.vinaooo.sudokutrio.domain.model.GameStats
import io.github.vinaooo.sudokutrio.domain.model.ScoreRecord
import io.github.vinaooo.sudokutrio.domain.model.Variant
import io.github.vinaooo.sudokutrio.domain.usecase.ObservePlayedModes
import io.github.vinaooo.sudokutrio.domain.usecase.ObserveStats
import io.github.vinaooo.sudokutrio.domain.usecase.ObserveTopScores
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ScoresViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val killerHard = GameMode(Variant.KILLER, Difficulty.HARD)
    private val classicEasy = GameMode(Variant.CLASSIC, Difficulty.EASY)
    private val xExpert = GameMode(Variant.X, Difficulty.EXPERT)
    private val scores = FakeScoreRepository(
        listOf(
            ScoreRecord(classicEasy, 700, 120, 1, 0, 1),
            ScoreRecord(classicEasy, 900, 100, 0, 0, 2),
            ScoreRecord(killerHard, 5000, 600, 0, 1, 3),
        ),
    )
    private val stats = FakeStatsRepository(
        mapOf(
            killerHard to GameStats(played = 2, won = 1),
            classicEasy to GameStats(played = 2, won = 2, currentStreak = 2, bestStreak = 2),
            xExpert to GameStats(played = 1),
        ),
    )

    @BeforeEach
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterEach
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel() = ScoresViewModel(ObserveTopScores(scores), ObserveStats(stats), ObservePlayedModes(stats))

    @Test
    fun `a tab per mode played, won or not, in variant then difficulty order, the first one shown`() =
        runTest(dispatcher) {
            viewModel().uiState.test {
                awaitItem().isLoading shouldBe true
                val state = awaitItem()
                state.modes shouldBe listOf(classicEasy, xExpert, killerHard)
                state.mode shouldBe classicEasy
                state.scores.map { it.points } shouldBe listOf(900, 700)
                state.stats.won shouldBe 2
            }
        }

    @Test
    fun `choosing a tab shows that mode's ranking and stats`() = runTest(dispatcher) {
        val vm = viewModel()
        vm.uiState.test {
            skipItems(2)
            vm.selectMode(xExpert)
            val state = awaitItem()
            state.mode shouldBe xExpert
            state.scores shouldBe emptyList()
            state.stats shouldBe GameStats(played = 1)
        }
    }

    @Test
    fun `with nothing played there are no tabs and nothing to rank`() = runTest(dispatcher) {
        val empty = FakeStatsRepository()
        ScoresViewModel(ObserveTopScores(FakeScoreRepository()), ObserveStats(empty), ObservePlayedModes(empty))
            .uiState.test {
                skipItems(1)
                val state = awaitItem()
                state.modes shouldBe emptyList()
                state.scores shouldBe emptyList()
                state.isLoading shouldBe false
            }
    }
}
