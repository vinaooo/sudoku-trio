package io.github.vinaooo.sudokutrio.domain.usecase

import io.github.vinaooo.sudokutrio.domain.generator.PuzzleGenerator
import io.github.vinaooo.sudokutrio.domain.model.GameMode
import io.github.vinaooo.sudokutrio.domain.model.GameStats
import io.github.vinaooo.sudokutrio.domain.model.Puzzle
import io.github.vinaooo.sudokutrio.domain.model.ScoreRecord
import io.github.vinaooo.sudokutrio.domain.repository.Clock
import io.github.vinaooo.sudokutrio.domain.repository.SavedGameRepository
import io.github.vinaooo.sudokutrio.domain.repository.ScoreRepository
import io.github.vinaooo.sudokutrio.domain.repository.SeedSource
import io.github.vinaooo.sudokutrio.domain.repository.StatsRepository
import io.github.vinaooo.sudokutrio.domain.rules.GameEngine
import io.github.vinaooo.sudokutrio.domain.session.GameSession
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/** Counts a saved game that was being played as a loss in its own mode, before another game replaces it. */
class AbandonGame(private val savedGames: SavedGameRepository, private val stats: StatsRepository) {
    suspend operator fun invoke() {
        val saved = savedGames.load() ?: return
        if (saved.isInProgress) stats.update(saved.state.mode, GameStats::afterLoss)
    }
}

/** A puzzle made ahead for [mode], from [seed], so a new game can start at once. */
data class PreparedPuzzle(val mode: GameMode, val seed: Long, val puzzle: Puzzle)

/** Makes the next puzzle of [GameMode] from a fresh seed, on [dispatcher], while the current game is played. */
class PreparePuzzle(
    private val generator: PuzzleGenerator,
    private val seeds: SeedSource,
    private val dispatcher: CoroutineDispatcher,
) {
    suspend operator fun invoke(mode: GameMode): PreparedPuzzle {
        val seed = seeds.nextSeed()
        return PreparedPuzzle(mode, seed, withContext(dispatcher) { generator.generate(mode, seed) })
    }
}

/**
 * Starts a new puzzle in [GameMode]: the [prepared] one when it is of this mode, else one generated now from a fresh
 * seed on [dispatcher] (it takes a moment and can be cancelled). An unfinished game it replaces counts as a loss.
 */
class StartNewGame(
    private val abandon: AbandonGame,
    private val savedGames: SavedGameRepository,
    private val generator: PuzzleGenerator,
    private val seeds: SeedSource,
    private val engine: GameEngine,
    private val dispatcher: CoroutineDispatcher,
) {
    suspend operator fun invoke(mode: GameMode, prepared: PreparedPuzzle? = null): GameSession {
        val (seed, puzzle) = prepared?.takeIf { it.mode == mode }?.let { it.seed to it.puzzle } ?: run {
            val seed = seeds.nextSeed()
            seed to withContext(dispatcher) { generator.generate(mode, seed) }
        }
        abandon()
        return GameSession(seed, engine.newGame(puzzle, mode)).also { savedGames.save(it) }
    }
}

/**
 * Plays the same board again from its givens, in its own mode, keeping the puzzle rather than generating it again.
 * Like any new game, an unfinished one counts as a loss.
 */
class RestartGame(
    private val abandon: AbandonGame,
    private val savedGames: SavedGameRepository,
    private val engine: GameEngine,
) {
    suspend operator fun invoke(session: GameSession): GameSession {
        abandon()
        val state = session.state
        return GameSession(session.seed, engine.newGame(state.puzzle, state.mode)).also { savedGames.save(it) }
    }
}

class ResumeGame(private val savedGames: SavedGameRepository) {
    suspend operator fun invoke(): GameSession? = savedGames.load()?.takeUnless { it.state.isWon }
}

class SaveGame(private val savedGames: SavedGameRepository) {
    suspend operator fun invoke(session: GameSession) = savedGames.save(session)
}

/** Records a won game: its score in its mode's ranking and the win in its stats; the saved game goes. */
class FinishGame(
    private val scores: ScoreRepository,
    private val stats: StatsRepository,
    private val savedGames: SavedGameRepository,
    private val clock: Clock,
) {
    suspend operator fun invoke(session: GameSession): ScoreRecord {
        val state = session.state
        check(state.isWon) { "Only a won game is finished." }
        val record = ScoreRecord(
            mode = state.mode,
            points = state.score,
            elapsedSeconds = state.elapsedSeconds,
            mistakes = state.mistakes,
            hintsUsed = state.hintsUsed,
            playedAtMillis = clock.nowMillis(),
        )
        scores.add(record)
        stats.update(state.mode, GameStats::afterWin)
        savedGames.clear()
        return record
    }
}

class ObserveTopScores(private val scores: ScoreRepository) {
    operator fun invoke(mode: GameMode): Flow<List<ScoreRecord>> = scores.observeTopScores(mode)
}

/** The modes played at least once, for the Scores screen's tabs: a mode never won still shows its stats. */
class ObservePlayedModes(private val stats: StatsRepository) {
    operator fun invoke(): Flow<Set<GameMode>> = stats.observePlayedModes()
}

class ObserveStats(private val stats: StatsRepository) {
    operator fun invoke(mode: GameMode): Flow<GameStats> = stats.observe(mode)
}
