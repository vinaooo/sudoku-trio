package io.github.vinaooo.sudokutrio.domain.model

import io.github.vinaooo.vinkit.core.AchievementProgress
import io.github.vinaooo.vinkit.core.GameStats
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * A badge. Its name is its key in storage (vinkit's `AchievementProgress`): never rename one after release.
 * A badge on a [ladder] is earned once that count reaches [count].
 */
enum class Achievement(val ladder: Ladder? = null, val count: Int = 0) {
    PLAYED_1(Ladder.PLAYED, 1),
    PLAYED_10(Ladder.PLAYED, 10),
    PLAYED_50(Ladder.PLAYED, 50),
    PLAYED_100(Ladder.PLAYED, 100),
    PLAYED_500(Ladder.PLAYED, 500),
    DAYS_3(Ladder.DAYS, 3),
    DAYS_7(Ladder.DAYS, 7),
    DAYS_14(Ladder.DAYS, 14),
    DAYS_30(Ladder.DAYS, 30),
    DAYS_100(Ladder.DAYS, 100),
    DAYS_365(Ladder.DAYS, 365),
    WON_1(Ladder.WON, 1),
    WON_10(Ladder.WON, 10),
    WON_50(Ladder.WON, 50),
    WON_100(Ladder.WON, 100),
    WON_500(Ladder.WON, 500),
    WON_1000(Ladder.WON, 1000),
    STREAK_3(Ladder.STREAK, 3),
    STREAK_5(Ladder.STREAK, 5),
    STREAK_10(Ladder.STREAK, 10),
    STREAK_20(Ladder.STREAK, 20),
    WIN_CLASSIC,
    WIN_X,
    WIN_KILLER,
    WIN_TRIO,
    WIN_CLASSIC_EXPERT,
    WIN_X_EXPERT,
    WIN_KILLER_EXPERT,
    WIN_EVERY_MODE,
    KILLER_MASTER,
    FLAWLESS,
    FLAWLESS_EXPERT,
    NO_HINTS,
    PERFECT,
    FAST_EASY,
    FAST_MEDIUM,
    FAST_HARD,
    FAST_EXPERT,
    NIGHT_OWL,
    EARLY_BIRD,
    MARATHON,
    WEEKEND,
}

/** What a ladder of badges counts. */
enum class Ladder {
    /** Games played (won or counted as lost), in every mode. */
    PLAYED,

    /** Days played in a row. */
    DAYS,

    /** Games won, in every mode. */
    WON,

    /** Games won in a row, in any mode. */
    STREAK,
}

/** The badges earned that this version knows; keys a newer version wrote are skipped. */
val AchievementProgress.badges: Set<Achievement>
    get() = Achievement.entries.filter { it.name in unlocked }.toSet()

/**
 * What the badges are judged on: every mode's [stats], the wins in a row in any mode, the days played in a row, the
 * games won today, whether a Saturday's win was followed by a Sunday's ([weekend]), and the game just [won] at [now]
 * (the phone's local time; null when no game was just won).
 */
data class AchievementFacts(
    val stats: Map<GameMode, GameStats>,
    val winStreak: Int = 0,
    val dayStreak: Int = 0,
    val winsToday: Int = 0,
    val weekend: Boolean = false,
    val won: GameState? = null,
    val now: LocalDateTime = LocalDateTime.MIN,
)

/** When each badge is earned. */
object Achievements {
    /** The collected sets' names: never rename. Days played (ISO dates), today's wins and Saturdays won (ISO). */
    const val DAYS_PLAYED = "days_played"
    const val WINS_TODAY = "wins_today"
    const val SATURDAYS_WON = "saturdays_won"

    /** Days played are kept for a year, enough for the longest ladder; older ones go. */
    private const val KEPT_DAYS = 366L
    private const val MARATHON_WINS = 5
    private const val KILLER_MASTER_WINS = 10
    private const val NIGHT_OWL_UNTIL = 5
    private const val EARLY_BIRD_UNTIL = 7
    private const val MINUTE = 60L

    private val VARIANT_WINS = mapOf(
        Variant.CLASSIC to Achievement.WIN_CLASSIC,
        Variant.X to Achievement.WIN_X,
        Variant.KILLER to Achievement.WIN_KILLER,
    )
    private val EXPERT_WINS = mapOf(
        Variant.CLASSIC to Achievement.WIN_CLASSIC_EXPERT,
        Variant.X to Achievement.WIN_X_EXPERT,
        Variant.KILLER to Achievement.WIN_KILLER_EXPERT,
    )

    /** A win faster than this (seconds), in any variant, earns the difficulty's speed badge. */
    private val FAST = mapOf(
        Difficulty.EASY to (Achievement.FAST_EASY to 4 * MINUTE),
        Difficulty.MEDIUM to (Achievement.FAST_MEDIUM to 8 * MINUTE),
        Difficulty.HARD to (Achievement.FAST_HARD to 15 * MINUTE),
        Difficulty.EXPERT to (Achievement.FAST_EXPERT to 25 * MINUTE),
    )

    /** [progress] with the badges [facts] earn added; what this version doesn't know is kept. */
    fun after(progress: AchievementProgress, facts: AchievementFacts): AchievementProgress =
        progress.copy(unlocked = progress.unlocked + earned(facts).map { it.name })

    fun earned(facts: AchievementFacts): Set<Achievement> = buildSet {
        val counts = mapOf(
            Ladder.PLAYED to facts.stats.values.sumOf { it.played },
            Ladder.DAYS to facts.dayStreak,
            Ladder.WON to facts.stats.values.sumOf { it.won },
            Ladder.STREAK to facts.winStreak,
        )
        addAll(Achievement.entries.filter { badge -> badge.ladder?.let { counts.getValue(it) >= badge.count } == true })
        val modesWon = facts.stats.filterValues { it.won > 0 }.keys
        modesWon.forEach { mode ->
            add(VARIANT_WINS.getValue(mode.variant))
            if (mode.difficulty == Difficulty.EXPERT) add(EXPERT_WINS.getValue(mode.variant))
        }
        if (modesWon.map { it.variant }.containsAll(Variant.entries)) add(Achievement.WIN_TRIO)
        if (modesWon.containsAll(allModes)) add(Achievement.WIN_EVERY_MODE)
        val killerExpert = facts.stats[GameMode(Variant.KILLER, Difficulty.EXPERT)]?.won ?: 0
        if (killerExpert >= KILLER_MASTER_WINS) add(Achievement.KILLER_MASTER)
        if (facts.winsToday >= MARATHON_WINS) add(Achievement.MARATHON)
        if (facts.weekend) add(Achievement.WEEKEND)
        facts.won?.let { addAll(winBadges(it, facts.now)) }
    }

    /**
     * The collected sets after a record at [now] ([won]: a game was just won): today is a day played, a win joins
     * today's wins (a Saturday's also joins the Saturdays won), and what no badge can use any more goes.
     */
    fun collect(collected: Map<String, Set<String>>, now: LocalDateTime, won: Boolean): Map<String, Set<String>> {
        val today = now.toLocalDate()
        val oldestDay = today.minusDays(KEPT_DAYS).toString()
        val lastWeek = today.minusDays(DayOfWeek.entries.size.toLong()).toString()
        val win = if (won) setOf(now.toString()) else emptySet()
        val saturday = if (won && today.dayOfWeek == DayOfWeek.SATURDAY) setOf(today.toString()) else emptySet()
        // ISO dates sort as text, so a plain comparison drops the old ones.
        return collected + mapOf(
            DAYS_PLAYED to collected[DAYS_PLAYED].orEmpty().filter { it > oldestDay }.toSet() + today.toString(),
            WINS_TODAY to collected[WINS_TODAY].orEmpty().filter { it.startsWith("${today}T") }.toSet() + win,
            SATURDAYS_WON to collected[SATURDAYS_WON].orEmpty().filter { it > lastWeek }.toSet() + saturday,
        )
    }

    /** The days in a row played up to [today], from the ISO dates in [days]. */
    fun dayStreak(days: Set<String>, today: LocalDate): Int =
        generateSequence(today) { it.minusDays(1) }.takeWhile { it.toString() in days }.count()

    /** A win on a Sunday ([today]) the day after a Saturday's win. */
    fun weekend(saturdays: Set<String>, today: LocalDate): Boolean =
        today.dayOfWeek == DayOfWeek.SUNDAY && today.minusDays(1).toString() in saturdays

    private fun winBadges(state: GameState, now: LocalDateTime): Set<Achievement> = buildSet {
        val expert = state.mode.difficulty == Difficulty.EXPERT
        if (state.mistakes == 0) add(Achievement.FLAWLESS)
        if (state.mistakes == 0 && expert) add(Achievement.FLAWLESS_EXPERT)
        if (state.hintsUsed == 0 && state.mode.difficulty >= Difficulty.HARD) add(Achievement.NO_HINTS)
        if (state.mistakes == 0 && state.hintsUsed == 0 && expert) add(Achievement.PERFECT)
        val (fast, seconds) = FAST.getValue(state.mode.difficulty)
        if (state.elapsedSeconds < seconds) add(fast)
        when {
            now.hour < NIGHT_OWL_UNTIL -> add(Achievement.NIGHT_OWL)
            now.hour < EARLY_BIRD_UNTIL -> add(Achievement.EARLY_BIRD)
        }
    }
}
