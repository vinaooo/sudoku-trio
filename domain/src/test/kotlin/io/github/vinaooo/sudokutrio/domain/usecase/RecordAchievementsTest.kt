package io.github.vinaooo.sudokutrio.domain.usecase

import io.github.vinaooo.sudokutrio.domain.fake.FakeAchievementRepository
import io.github.vinaooo.sudokutrio.domain.fake.FakeSettingsRepository
import io.github.vinaooo.sudokutrio.domain.fake.FakeStatsRepository
import io.github.vinaooo.sudokutrio.domain.mode
import io.github.vinaooo.sudokutrio.domain.model.Achievement
import io.github.vinaooo.sudokutrio.domain.model.Achievements
import io.github.vinaooo.sudokutrio.domain.model.Variant
import io.github.vinaooo.sudokutrio.domain.model.badges
import io.github.vinaooo.sudokutrio.domain.model.key
import io.github.vinaooo.sudokutrio.domain.newState
import io.github.vinaooo.sudokutrio.domain.repository.Clock
import io.github.vinaooo.vinkit.core.AchievementProgress
import io.github.vinaooo.vinkit.core.GameStats
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import java.time.LocalDateTime
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class RecordAchievementsTest {
    private val badges = FakeAchievementRepository()
    private val stats = FakeStatsRepository()
    private val settings = FakeSettingsRepository()
    private var now = LocalDateTime.of(2026, 10, 9, 12, 0) // a Friday
    private val record = RecordAchievements(
        badges,
        stats,
        settings,
        clock = object : Clock {
            override fun nowMillis() = 0L

            override fun now() = this@RecordAchievementsTest.now
        },
    )

    private val won = newState(empty = emptyList()).copy(mistakes = 1, hintsUsed = 1, elapsedSeconds = 3600)

    @Test
    fun `the first move of a day marks it, and only that one`() = runTest {
        record.played()
        badges.progress.value.collected[Achievements.DAYS_PLAYED] shouldBe setOf("2026-10-09")

        badges.progress.value = AchievementProgress()
        record.played()
        badges.progress.value shouldBe AchievementProgress()

        now = now.plusDays(1)
        record.played()
        badges.progress.value.collected[Achievements.DAYS_PLAYED] shouldBe setOf("2026-10-10")
    }

    @Test
    fun `three days in a row earn the three-day badge`() = runTest {
        badges.progress.value =
            AchievementProgress(collected = mapOf(Achievements.DAYS_PLAYED to setOf("2026-10-07", "2026-10-08")))

        record.played()

        badges.progress.value.badges shouldBe setOf(Achievement.DAYS_3)
    }

    @Test
    fun `ladders read every mode's stats, so earlier games count`() = runTest {
        stats.stats.value = mapOf(
            mode().key to GameStats(played = 6, won = 6),
            mode(Variant.KILLER).key to GameStats(played = 4, won = 4),
        )

        record.played()

        badges.progress.value.badges shouldContainAll
            setOf(Achievement.PLAYED_10, Achievement.WON_10, Achievement.WIN_CLASSIC, Achievement.WIN_KILLER)
    }

    @Test
    fun `wins build the streak, a loss ends it and earns no game badge`() = runTest {
        repeat(3) { record.gameEnded(won) }
        settings.settings.value.winStreak shouldBe 3
        badges.progress.value.badges shouldContain Achievement.STREAK_3

        record.gameEnded(won.copy(board = newState().board, mistakes = 0))
        settings.settings.value.winStreak shouldBe 0
        badges.progress.value.badges shouldNotContain Achievement.FLAWLESS
    }

    @Test
    fun `five wins in one day make a marathon, wins of the day before don't`() = runTest {
        now = now.withHour(23)
        repeat(2) { record.gameEnded(won) }
        now = now.plusHours(2)
        repeat(4) {
            now = now.plusMinutes(1)
            record.gameEnded(won)
        }
        badges.progress.value.badges shouldNotContain Achievement.MARATHON

        now = now.plusMinutes(1)
        record.gameEnded(won)
        badges.progress.value.badges shouldContain Achievement.MARATHON
    }

    @Test
    fun `a Saturday's win then a Sunday's make a weekend`() = runTest {
        now = now.plusDays(2) // Sunday, no Saturday win yet
        record.gameEnded(won)
        badges.progress.value.badges shouldNotContain Achievement.WEEKEND

        now = now.plusDays(6) // Saturday
        record.gameEnded(won)
        badges.progress.value.badges shouldNotContain Achievement.WEEKEND

        now = now.plusDays(1)
        record.played()
        badges.progress.value.badges shouldNotContain Achievement.WEEKEND
        record.gameEnded(won)
        badges.progress.value.badges shouldContain Achievement.WEEKEND
    }

    @Test
    fun `a win at night is judged on the phone's time`() = runTest {
        now = now.withHour(3)
        record.gameEnded(won)
        badges.progress.value.badges shouldContain Achievement.NIGHT_OWL
    }
}
