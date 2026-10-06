package io.github.vinaooo.sudokutrio.domain.model

import io.github.vinaooo.sudokutrio.domain.mode
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class StatsAndScoresTest {

    @Test
    fun `a win grows the streak and a loss ends it`() {
        val stats = GameStats().afterWin().afterWin().afterLoss().afterWin()
        stats shouldBe GameStats(played = 4, won = 3, currentStreak = 1, bestStreak = 2)
        stats.winRatePercent shouldBe 75
        GameStats().winRatePercent shouldBe 0
        GameStats(played = 3, won = 2).winRatePercent shouldBe 67
    }

    @Test
    fun `the ranking puts the highest score first, then the fastest, then the earliest`() {
        fun record(points: Int, seconds: Long, at: Long) = ScoreRecord(mode(), points, seconds, 0, 0, at)
        val records = listOf(record(500, 90, 1), record(800, 200, 2), record(500, 60, 3), record(500, 60, 0))
        records.sortedWith(ScoreRecord.rankingFor(mode())) shouldBe
            listOf(record(800, 200, 2), record(500, 60, 0), record(500, 60, 3), record(500, 90, 1))
    }

    @Test
    fun `settings default to classic easy, the system theme and blue`() {
        val settings = Settings()
        settings.mode shouldBe GameMode(Variant.CLASSIC, Difficulty.EASY)
        settings.themeColor shouldBe ThemeColor.BLUE
        settings.themeMode shouldBe ThemeMode.SYSTEM
        settings.dynamicColor shouldBe true
    }
}
