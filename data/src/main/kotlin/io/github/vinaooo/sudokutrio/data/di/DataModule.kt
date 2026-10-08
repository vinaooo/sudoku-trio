package io.github.vinaooo.sudokutrio.data.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.vinaooo.sudokutrio.data.game.FileSavedGameRepository
import io.github.vinaooo.sudokutrio.data.settings.DataStoreSettingsRepository
import io.github.vinaooo.sudokutrio.data.system.RandomSeedSource
import io.github.vinaooo.sudokutrio.data.system.SystemClock
import io.github.vinaooo.sudokutrio.domain.repository.Clock
import io.github.vinaooo.sudokutrio.domain.repository.SavedGameRepository
import io.github.vinaooo.sudokutrio.domain.repository.SeedSource
import io.github.vinaooo.sudokutrio.domain.repository.SettingsRepository
import io.github.vinaooo.vinkit.core.AppSettings
import io.github.vinaooo.vinkit.core.AppSettingsRepository
import io.github.vinaooo.vinkit.core.ScoreRepository
import io.github.vinaooo.vinkit.core.StatsRepository
import io.github.vinaooo.vinkit.core.ThemeColor
import io.github.vinaooo.vinkit.scores.data.RoomScoreRepository
import io.github.vinaooo.vinkit.scores.data.RoomStatsRepository
import io.github.vinaooo.vinkit.scores.data.ScoresDatabase
import io.github.vinaooo.vinkit.settings.DataStoreAppSettingsRepository
import java.io.File
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers

@Module
@InstallIn(SingletonComponent::class)
internal object DataProvidersModule {

    /** vinkit's scores and per-mode stats (`vinkit_scores.db`). */
    @Provides
    @Singleton
    fun scoresDatabase(@ApplicationContext context: Context): ScoresDatabase {
        // Sudoku Trio's own database before vinkit's; nothing reads it any more (a no-op once it's gone).
        context.deleteDatabase("sudokutrio.db")
        return ScoresDatabase.create(context)
    }

    @Provides
    @Singleton
    fun scores(db: ScoresDatabase): ScoreRepository = RoomScoreRepository(db)

    @Provides
    @Singleton
    fun stats(db: ScoresDatabase): StatsRepository = RoomStatsRepository(db)

    @Provides
    @Singleton
    fun settingsDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create { context.preferencesDataStoreFile("settings") }

    /** The theme, feedback and layout settings every vinkit game has, in the same DataStore; blue is the brand. */
    @Provides
    @Singleton
    fun appSettings(dataStore: DataStore<Preferences>): AppSettingsRepository =
        DataStoreAppSettingsRepository(dataStore, AppSettings(themeColor = ThemeColor.BLUE))

    @Provides
    @Singleton
    fun savedGameRepository(@ApplicationContext context: Context): SavedGameRepository =
        FileSavedGameRepository(File(context.filesDir, "saved_game.json"), Dispatchers.IO)
}

@Module
@InstallIn(SingletonComponent::class)
internal interface DataBindingsModule {
    @Binds
    @Singleton
    fun settings(impl: DataStoreSettingsRepository): SettingsRepository

    @Binds
    fun seedSource(impl: RandomSeedSource): SeedSource

    @Binds
    fun clock(impl: SystemClock): Clock
}
