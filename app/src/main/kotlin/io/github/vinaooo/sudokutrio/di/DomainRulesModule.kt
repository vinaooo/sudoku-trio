package io.github.vinaooo.sudokutrio.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.vinaooo.sudokutrio.domain.generator.PuzzleGenerator
import io.github.vinaooo.sudokutrio.domain.generator.SeededPuzzleGenerator
import io.github.vinaooo.sudokutrio.domain.rules.ConflictFinder
import io.github.vinaooo.sudokutrio.domain.rules.GameEngine

/** The domain layer is plain Kotlin with no DI annotations, so its rules are assembled here. */
@Module
@InstallIn(SingletonComponent::class)
object DomainRulesModule {
    @Provides fun gameEngine() = GameEngine()

    @Provides fun conflictFinder() = ConflictFinder()

    @Provides fun puzzleGenerator(): PuzzleGenerator = SeededPuzzleGenerator()
}
