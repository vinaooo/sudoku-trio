package io.github.vinaooo.sudokutrio.domain.model

import io.github.vinaooo.sudokutrio.domain.mode
import io.github.vinaooo.sudokutrio.domain.newState
import io.github.vinaooo.vinkit.core.AchievementProgress
import io.github.vinaooo.vinkit.core.GameStats
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import java.time.LocalDate
import java.time.LocalDateTime
import org.junit.jupiter.api.Test

class AchievementsTest {
    private val noon = LocalDateTime.of(2026, 10, 9, 12, 0)

    private fun stats(vararg won: Pair<GameMode, Int>) = won.toMap().mapValues {
        GameStats(played = it.value, won = it.value)
    }

    private fun win(
        difficulty: Difficulty = Difficulty.EASY,
        mistakes: Int = 1,
        hints: Int = 1,
        seconds: Long = 3600,
        at: LocalDateTime = noon,
    ) = Achievements.earned(
        AchievementFacts(
            stats = emptyMap(),
            won = newState().copy(
                mode = mode(difficulty = difficulty),
                mistakes = mistakes,
                hintsUsed = hints,
                elapsedSeconds = seconds,
            ),
            now = at,
        ),
    )

    @Test
    fun `ladders unlock at their counts, not before`() {
        val nine = Achievements.earned(AchievementFacts(stats(mode() to 9), winStreak = 2, dayStreak = 2))
        nine shouldBe setOf(Achievement.PLAYED_1, Achievement.WON_1, Achievement.WIN_CLASSIC)
        val ten = Achievements.earned(AchievementFacts(stats(mode() to 10), winStreak = 3, dayStreak = 3))
        ten shouldContainAll setOf(Achievement.PLAYED_10, Achievement.WON_10, Achievement.STREAK_3, Achievement.DAYS_3)
        ten shouldNotContain Achievement.STREAK_5
    }

    @Test
    fun `played counts losses too`() {
        val lost = mapOf(mode() to GameStats(played = 1))
        Achievements.earned(AchievementFacts(lost)) shouldBe setOf(Achievement.PLAYED_1)
    }

    @Test
    fun `variant and expert wins, the trio and every mode`() {
        val some = Achievements.earned(
            AchievementFacts(stats(mode(Variant.X, Difficulty.EXPERT) to 1, mode(Variant.KILLER) to 1)),
        )
        some shouldContainAll setOf(Achievement.WIN_X, Achievement.WIN_X_EXPERT, Achievement.WIN_KILLER)
        some shouldNotContain Achievement.WIN_TRIO
        some shouldNotContain Achievement.WIN_KILLER_EXPERT

        val trio = Achievements.earned(AchievementFacts(stats(*Variant.entries.map { mode(it) to 1 }.toTypedArray())))
        trio shouldContain Achievement.WIN_TRIO
        trio shouldNotContain Achievement.WIN_EVERY_MODE

        val all = Achievements.earned(AchievementFacts(stats(*allModes.map { it to 1 }.toTypedArray())))
        all shouldContainAll
            setOf(Achievement.WIN_EVERY_MODE, Achievement.WIN_CLASSIC_EXPERT, Achievement.WIN_KILLER_EXPERT)
    }

    @Test
    fun `killer master takes ten killer expert wins`() {
        val killerExpert = mode(Variant.KILLER, Difficulty.EXPERT)
        Achievements.earned(AchievementFacts(stats(killerExpert to 9))) shouldNotContain Achievement.KILLER_MASTER
        Achievements.earned(AchievementFacts(stats(killerExpert to 10))) shouldContain Achievement.KILLER_MASTER
    }

    @Test
    fun `marathon takes five wins in a day, weekend a flag`() {
        Achievements.earned(AchievementFacts(emptyMap(), winsToday = 4)) shouldBe emptySet()
        Achievements.earned(AchievementFacts(emptyMap(), winsToday = 5)) shouldBe setOf(Achievement.MARATHON)
        Achievements.earned(AchievementFacts(emptyMap(), weekend = true)) shouldBe setOf(Achievement.WEEKEND)
    }

    @Test
    fun `flawless, no hints and perfect`() {
        win() shouldBe emptySet()
        win(mistakes = 0) shouldBe setOf(Achievement.FLAWLESS)
        win(Difficulty.MEDIUM, hints = 0) shouldBe emptySet()
        win(Difficulty.HARD, hints = 0) shouldBe setOf(Achievement.NO_HINTS)
        win(Difficulty.EXPERT, mistakes = 0) shouldBe setOf(Achievement.FLAWLESS, Achievement.FLAWLESS_EXPERT)
        win(Difficulty.EXPERT, hints = 0) shouldBe setOf(Achievement.NO_HINTS)
        win(Difficulty.EXPERT, mistakes = 0, hints = 0) shouldBe
            setOf(Achievement.FLAWLESS, Achievement.FLAWLESS_EXPERT, Achievement.NO_HINTS, Achievement.PERFECT)
    }

    @Test
    fun `speed badges per difficulty, under the limit only`() {
        val limits = mapOf(
            Difficulty.EASY to (Achievement.FAST_EASY to 240L),
            Difficulty.MEDIUM to (Achievement.FAST_MEDIUM to 480L),
            Difficulty.HARD to (Achievement.FAST_HARD to 900L),
            Difficulty.EXPERT to (Achievement.FAST_EXPERT to 1500L),
        )
        limits.forEach { (difficulty, limit) ->
            val (badge, seconds) = limit
            win(difficulty, seconds = seconds - 1) shouldContain badge
            win(difficulty, seconds = seconds) shouldNotContain badge
        }
    }

    @Test
    fun `night owl before five, early bird before seven, on the phone's time`() {
        fun at(hour: Int, minute: Int) = win(at = noon.withHour(hour).withMinute(minute))
        at(0, 0) shouldBe setOf(Achievement.NIGHT_OWL)
        at(4, 59) shouldBe setOf(Achievement.NIGHT_OWL)
        at(5, 0) shouldBe setOf(Achievement.EARLY_BIRD)
        at(6, 59) shouldBe setOf(Achievement.EARLY_BIRD)
        at(7, 0) shouldBe emptySet()
    }

    @Test
    fun `badges add to what was unlocked, keeping unknown keys`() {
        val before = AchievementProgress(unlocked = setOf("FROM_A_NEWER_VERSION"))
        val after = Achievements.after(before, AchievementFacts(stats(mode() to 1)))
        after.unlocked shouldBe setOf("FROM_A_NEWER_VERSION", "PLAYED_1", "WON_1", "WIN_CLASSIC")
        after.badges shouldBe setOf(Achievement.PLAYED_1, Achievement.WON_1, Achievement.WIN_CLASSIC)
    }

    @Test
    fun `collect marks the day and the win, and drops what no badge needs`() {
        val saturday = LocalDateTime.of(2026, 10, 10, 9, 30)
        val before = mapOf(
            Achievements.DAYS_PLAYED to setOf("2025-10-09", "2025-10-10", "2026-10-09"),
            Achievements.WINS_TODAY to setOf("2026-10-09T23:00", "2026-10-10T08:00"),
            Achievements.SATURDAYS_WON to setOf("2026-10-03", "2026-09-26"),
            "other" to setOf("kept"),
        )
        Achievements.collect(before, saturday, won = true) shouldBe mapOf(
            Achievements.DAYS_PLAYED to setOf("2025-10-10", "2026-10-09", "2026-10-10"),
            Achievements.WINS_TODAY to setOf("2026-10-10T08:00", "2026-10-10T09:30"),
            Achievements.SATURDAYS_WON to setOf("2026-10-10"),
            "other" to setOf("kept"),
        )
        Achievements.collect(emptyMap(), saturday, won = false) shouldBe mapOf(
            Achievements.DAYS_PLAYED to setOf("2026-10-10"),
            Achievements.WINS_TODAY to emptySet(),
            Achievements.SATURDAYS_WON to emptySet(),
        )
        Achievements.collect(emptyMap(), noon, won = true)[Achievements.SATURDAYS_WON] shouldBe emptySet()
    }

    @Test
    fun `day streak and weekend`() {
        val today = LocalDate.of(2026, 10, 11) // a Sunday
        Achievements.dayStreak(setOf("2026-10-11", "2026-10-10", "2026-10-08"), today) shouldBe 2
        Achievements.dayStreak(setOf("2026-10-10"), today) shouldBe 0
        Achievements.weekend(setOf("2026-10-10"), today) shouldBe true
        Achievements.weekend(setOf("2026-10-03"), today) shouldBe false
        Achievements.weekend(setOf("2026-10-10"), today.plusDays(1)) shouldBe false
    }
}
