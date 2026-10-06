package io.github.vinaooo.sudokutrio.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import io.github.vinaooo.sudokutrio.domain.model.Difficulty
import io.github.vinaooo.sudokutrio.domain.model.GameMode
import io.github.vinaooo.sudokutrio.domain.model.GameStats
import io.github.vinaooo.sudokutrio.domain.model.ScoreRecord
import io.github.vinaooo.sudokutrio.domain.model.Variant
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RoomRepositoriesTest {
    private val db = Room.inMemoryDatabaseBuilder(
        ApplicationProvider.getApplicationContext(),
        SudokuTrioDatabase::class.java,
    )
        .allowMainThreadQueries()
        .build()
    private val scores = RoomScoreRepository(db.scoreDao())
    private val stats = RoomStatsRepository(db, db.statsDao())
    private val classic = GameMode(Variant.CLASSIC, Difficulty.EASY)
    private val killer = GameMode(Variant.KILLER, Difficulty.EXPERT)

    private fun record(mode: GameMode, points: Int, seconds: Long = 100, at: Long = 0) =
        ScoreRecord(mode, points, seconds, mistakes = 1, hintsUsed = 2, playedAtMillis = at)

    @After
    fun tearDown() = db.close()

    @Test
    fun `top scores are per mode, in ranking order, limited`() = runTest {
        listOf(
            record(classic, 500, at = 3),
            record(classic, 800),
            record(classic, 500, seconds = 60, at = 5),
            record(classic, 500, seconds = 60, at = 1),
            record(killer, 9000),
        ).forEach { scores.add(it) }

        val top = scores.observeTopScores(classic, limit = 3).first()

        top shouldBe listOf(record(classic, 800), record(classic, 500, 60, 1), record(classic, 500, 60, 5))
        top shouldBe top.sortedWith(ScoreRecord.rankingFor(classic))
        scores.observeTopScores(killer).first() shouldBe listOf(record(killer, 9000))
    }

    @Test
    fun `scores of a mode this version doesn't know are skipped`() = runTest {
        db.scoreDao().insert(
            ScoreEntity(
                variant = "SAMURAI",
                difficulty = "EASY",
                points = 1,
                elapsedSeconds = 1,
                mistakes = 0,
                hintsUsed = 0,
                playedAtMillis = 0,
            ),
        )
        db.scoreDao().insert(
            ScoreEntity(
                variant = "CLASSIC",
                difficulty = "INSANE",
                points = 1,
                elapsedSeconds = 1,
                mistakes = 0,
                hintsUsed = 0,
                playedAtMillis = 0,
            ),
        )
        scores.observeTopScores(classic).first() shouldBe emptyList()
        db.statsDao().upsert(StatsEntity("SAMURAI", "EASY", 1, 0, 0, 0))
        stats.observePlayedModes().first() shouldBe emptySet()
    }

    @Test
    fun `no stats yet read as zeros`() = runTest {
        stats.observe(classic).first() shouldBe GameStats()
    }

    @Test
    fun `stats updates build on what is stored, per mode`() = runTest {
        stats.update(classic) { it.afterWin() }
        stats.update(classic) { it.afterWin() }
        stats.update(classic) { it.afterLoss() }
        stats.update(killer) { it.afterWin() }

        stats.observe(classic).first() shouldBe GameStats(played = 3, won = 2, currentStreak = 0, bestStreak = 2)
        stats.observe(killer).first() shouldBe GameStats(played = 1, won = 1, currentStreak = 1, bestStreak = 1)
    }

    @Test
    fun `played modes are the ones with a game played, won or not`() = runTest {
        stats.observePlayedModes().test {
            awaitItem() shouldBe emptySet()
            stats.update(killer) { it.afterLoss() }
            awaitItem() shouldBe setOf(killer)
            cancelAndIgnoreRemainingEvents()
        }
        stats.update(classic) { it }
        stats.observePlayedModes().first() shouldBe setOf(killer)
        stats.update(classic) { it.afterWin() }
        stats.observePlayedModes().first() shouldBe setOf(killer, classic)
    }
}
