package io.github.vinaooo.sudokutrio.domain.usecase

import io.github.vinaooo.sudokutrio.domain.model.AchievementFacts
import io.github.vinaooo.sudokutrio.domain.model.Achievements
import io.github.vinaooo.sudokutrio.domain.model.GameState
import io.github.vinaooo.sudokutrio.domain.model.allModes
import io.github.vinaooo.sudokutrio.domain.model.key
import io.github.vinaooo.sudokutrio.domain.repository.Clock
import io.github.vinaooo.sudokutrio.domain.repository.SettingsRepository
import io.github.vinaooo.vinkit.core.AchievementRepository
import io.github.vinaooo.vinkit.core.StatsRepository
import java.time.LocalDate
import kotlinx.coroutines.flow.first

/**
 * Marks today as played and unlocks the badges earned so far. Ladders read the stats, so badges a player already
 * deserved unlock the first time this runs. The game screen learns of new badges by watching the repository.
 */
class RecordAchievements(
    private val achievements: AchievementRepository,
    private val stats: StatsRepository,
    private val settings: SettingsRepository,
    private val clock: Clock,
) {
    private var lastPlayed: LocalDate? = null

    /** A counted game ended ([state] won, or abandoned in progress), after its stats were updated. */
    suspend fun gameEnded(state: GameState) {
        val won = state.isWon
        settings.update { it.copy(winStreak = if (won) it.winStreak + 1 else 0) }
        record(state.takeIf { won })
    }

    /** A move was played: the first one each day marks the day. */
    suspend fun played() {
        val today = clock.now().toLocalDate()
        if (today == lastPlayed) return
        lastPlayed = today
        record(won = null)
    }

    private suspend fun record(won: GameState?) {
        val all = allModes.associateWith { stats.observe(it.key).first() }
        val winStreak = settings.settings.first().winStreak
        val now = clock.now()
        val today = now.toLocalDate()
        achievements.update { progress ->
            val collected = Achievements.collect(progress.collected, now, won != null)
            val facts = AchievementFacts(
                stats = all,
                winStreak = winStreak,
                dayStreak = Achievements.dayStreak(collected.getValue(Achievements.DAYS_PLAYED), today),
                winsToday = collected.getValue(Achievements.WINS_TODAY).size,
                weekend = won != null && Achievements.weekend(collected.getValue(Achievements.SATURDAYS_WON), today),
                won = won,
                now = now,
            )
            Achievements.after(progress.copy(collected = collected), facts)
        }
    }
}
