package io.github.vinaooo.sudokutrio.domain.model

import io.github.vinaooo.sudokutrio.domain.scoring.RankingOrder
import io.github.vinaooo.sudokutrio.domain.scoring.scoringFor
import io.github.vinaooo.vinkit.core.Ranking
import io.github.vinaooo.vinkit.core.ScoreRecord

/** Sudoku Trio's scores are vinkit's: a mode's key is "KILLER_HARD", and the mistakes and hints go in the extras. */
val GameMode.key: String get() = "${variant.name}_${difficulty.name}"

/** The mode of a [key]; null for one this version doesn't know. */
fun gameModeOf(key: String): GameMode? {
    val (variant, difficulty) = key.split('_').takeIf { it.size == 2 } ?: return null
    return GameMode(enumOrNull<Variant>(variant) ?: return null, enumOrNull<Difficulty>(difficulty) ?: return null)
}

/** How the mode's scores rank: highest points first, then the fastest, then the earliest. */
fun GameMode.ranking(): Ranking = when (scoringFor(this).rankingOrder) {
    RankingOrder.HIGHEST_SCORE -> Ranking.HIGHEST_POINTS
}

/** This game as a score played at [nowMillis]. */
fun GameState.toRecord(nowMillis: Long) = ScoreRecord(
    mode = mode.key,
    points = score,
    elapsedSeconds = elapsedSeconds,
    playedAtMillis = nowMillis,
    extras = mapOf(MISTAKES to mistakes.toString(), HINTS to hintsUsed.toString()),
)

val ScoreRecord.mistakes: Int get() = extras[MISTAKES]?.toIntOrNull() ?: 0
val ScoreRecord.hintsUsed: Int get() = extras[HINTS]?.toIntOrNull() ?: 0

private inline fun <reified T : Enum<T>> enumOrNull(name: String): T? = enumValues<T>().firstOrNull { it.name == name }

private const val MISTAKES = "mistakes"
private const val HINTS = "hints"
