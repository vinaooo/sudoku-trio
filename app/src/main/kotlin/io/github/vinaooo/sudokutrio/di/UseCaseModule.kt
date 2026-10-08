package io.github.vinaooo.sudokutrio.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.vinaooo.sudokutrio.domain.generator.PuzzleGenerator
import io.github.vinaooo.sudokutrio.domain.repository.Clock
import io.github.vinaooo.sudokutrio.domain.repository.SavedGameRepository
import io.github.vinaooo.sudokutrio.domain.repository.SeedSource
import io.github.vinaooo.sudokutrio.domain.rules.GameEngine
import io.github.vinaooo.sudokutrio.domain.usecase.AbandonGame
import io.github.vinaooo.sudokutrio.domain.usecase.FinishGame
import io.github.vinaooo.sudokutrio.domain.usecase.PreparePuzzle
import io.github.vinaooo.sudokutrio.domain.usecase.RestartGame
import io.github.vinaooo.sudokutrio.domain.usecase.ResumeGame
import io.github.vinaooo.sudokutrio.domain.usecase.SaveGame
import io.github.vinaooo.sudokutrio.domain.usecase.StartNewGame
import io.github.vinaooo.vinkit.core.ScoreRepository
import io.github.vinaooo.vinkit.core.StatsRepository
import kotlinx.coroutines.Dispatchers

/** Domain use cases, wired to the data layer's repositories. */
@Module
@InstallIn(SingletonComponent::class)
object UseCaseModule {
    @Provides fun abandonGame(savedGames: SavedGameRepository, stats: StatsRepository) = AbandonGame(savedGames, stats)

    /** Puzzles are generated on [Dispatchers.Default]: it takes a moment and works the CPU. */
    @Provides
    fun startNewGame(
        abandon: AbandonGame,
        savedGames: SavedGameRepository,
        generator: PuzzleGenerator,
        seeds: SeedSource,
        engine: GameEngine,
    ) = StartNewGame(abandon, savedGames, generator, seeds, engine, Dispatchers.Default)

    @Provides
    fun preparePuzzle(generator: PuzzleGenerator, seeds: SeedSource) =
        PreparePuzzle(generator, seeds, Dispatchers.Default)

    @Provides
    fun restartGame(abandon: AbandonGame, savedGames: SavedGameRepository, engine: GameEngine) =
        RestartGame(abandon, savedGames, engine)

    @Provides fun resumeGame(savedGames: SavedGameRepository) = ResumeGame(savedGames)

    @Provides fun saveGame(savedGames: SavedGameRepository) = SaveGame(savedGames)

    @Provides
    fun finishGame(scores: ScoreRepository, stats: StatsRepository, savedGames: SavedGameRepository, clock: Clock) =
        FinishGame(scores, stats, savedGames, clock)
}
