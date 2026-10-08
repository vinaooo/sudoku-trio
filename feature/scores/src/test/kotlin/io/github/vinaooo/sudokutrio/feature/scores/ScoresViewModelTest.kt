package io.github.vinaooo.sudokutrio.feature.scores

import app.cash.turbine.test
import io.github.vinaooo.sudokutrio.domain.fake.FakeScoreRepository
import io.github.vinaooo.sudokutrio.domain.fake.FakeStatsRepository
import io.github.vinaooo.sudokutrio.domain.model.Difficulty
import io.github.vinaooo.sudokutrio.domain.model.GameMode
import io.github.vinaooo.sudokutrio.domain.model.Variant
import io.github.vinaooo.sudokutrio.domain.model.key
import io.github.vinaooo.vinkit.core.GameStats
import io.github.vinaooo.vinkit.core.ScoreRecord
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
    private val killerHard = GameMode(Variant.KILLER, Difficulty.HARD).key
    private val classicEasy = GameMode(Variant.CLASSIC, Difficulty.EASY).key
    private val xExpert = GameMode(Variant.X, Difficulty.EXPERT).key
    private val scores = FakeScoreRepository(
        listOf(
            ScoreRecord(classicEasy, 700, 120, 1),
            ScoreRecord(classicEasy, 900, 100, 2),
            ScoreRecord(killerHard, 5000, 600, 3),
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

    @Test
    fun `a tab per variant played, a section per difficulty, the first variant shown`() = runTest(dispatcher) {
        SudokuScoresViewModel(scores, stats).uiState.test {
            awaitItem().isLoading shouldBe true
            val state = awaitItem()
            state.groups shouldBe listOf("CLASSIC", "X", "KILLER")
            state.group shouldBe "CLASSIC"
            state.sections.map { it.mode } shouldBe Difficulty.entries.map { "CLASSIC_$it" }
            state.sections.first().scores.map { it.points } shouldBe listOf(900, 700)
            state.sections.first().stats.won shouldBe 2
        }
    }

    @Test
    fun `choosing a tab shows that variant's rankings and stats`() = runTest(dispatcher) {
        val vm = SudokuScoresViewModel(scores, stats)
        vm.uiState.test {
            skipItems(2)
            vm.selectGroup("X")
            val expert = awaitItem().sections.single { it.mode == xExpert }
            expert.scores shouldBe emptyList()
            expert.stats shouldBe GameStats(played = 1)
        }
    }

    @Test
    fun `with nothing played there are no tabs and nothing to rank`() = runTest(dispatcher) {
        SudokuScoresViewModel(FakeScoreRepository(), FakeStatsRepository()).uiState.test {
            skipItems(1)
            val state = awaitItem()
            state.groups shouldBe emptyList()
            state.sections shouldBe emptyList()
            state.isLoading shouldBe false
        }
    }
}
