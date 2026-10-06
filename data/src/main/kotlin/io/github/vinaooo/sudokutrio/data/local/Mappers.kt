package io.github.vinaooo.sudokutrio.data.local

import io.github.vinaooo.sudokutrio.domain.model.Difficulty
import io.github.vinaooo.sudokutrio.domain.model.GameMode
import io.github.vinaooo.sudokutrio.domain.model.GameStats
import io.github.vinaooo.sudokutrio.domain.model.ScoreRecord
import io.github.vinaooo.sudokutrio.domain.model.Variant

/** The mode stored as [variant] and [difficulty], or null for one this version doesn't know (from a newer one). */
internal fun gameModeOf(variant: String, difficulty: String): GameMode? {
    val v = Variant.entries.firstOrNull { it.name == variant } ?: return null
    val d = Difficulty.entries.firstOrNull { it.name == difficulty } ?: return null
    return GameMode(v, d)
}

internal fun ScoreRecord.toEntity() = ScoreEntity(
    variant = mode.variant.name,
    difficulty = mode.difficulty.name,
    points = points,
    elapsedSeconds = elapsedSeconds,
    mistakes = mistakes,
    hintsUsed = hintsUsed,
    playedAtMillis = playedAtMillis,
)

internal fun ScoreEntity.toDomain(): ScoreRecord? = gameModeOf(variant, difficulty)?.let { mode ->
    ScoreRecord(mode, points, elapsedSeconds, mistakes, hintsUsed, playedAtMillis)
}

internal fun GameStats.toEntity(mode: GameMode) = StatsEntity(
    variant = mode.variant.name,
    difficulty = mode.difficulty.name,
    played = played,
    won = won,
    currentStreak = currentStreak,
    bestStreak = bestStreak,
)

internal fun StatsEntity?.toDomain() = this?.let { GameStats(played, won, currentStreak, bestStreak) } ?: GameStats()
