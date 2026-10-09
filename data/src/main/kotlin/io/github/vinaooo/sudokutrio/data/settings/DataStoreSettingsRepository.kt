package io.github.vinaooo.sudokutrio.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import io.github.vinaooo.sudokutrio.domain.model.GameMode
import io.github.vinaooo.sudokutrio.domain.model.Settings
import io.github.vinaooo.sudokutrio.domain.repository.SettingsRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Sudoku Trio's own settings in Preferences DataStore, the one vinkit's `DataStoreAppSettingsRepository` keeps the
 * common settings in: each writes only its own keys. A value this version doesn't know (from a newer one)
 * reads as the default.
 */
class DataStoreSettingsRepository @Inject constructor(private val dataStore: DataStore<Preferences>) :
    SettingsRepository {

    override val settings: Flow<Settings> = dataStore.data.map { it.toSettings() }

    override suspend fun update(transform: (Settings) -> Settings) {
        dataStore.edit { prefs -> prefs.write(transform(prefs.toSettings())) }
    }

    private fun Preferences.toSettings(): Settings {
        val defaults = Settings()
        return Settings(
            mode = GameMode(
                variant = enumOrDefault(this[Keys.VARIANT], defaults.mode.variant),
                difficulty = enumOrDefault(this[Keys.DIFFICULTY], defaults.mode.difficulty),
            ),
            winStreak = this[Keys.WIN_STREAK] ?: defaults.winStreak,
        )
    }

    private fun MutablePreferences.write(settings: Settings) {
        this[Keys.VARIANT] = settings.mode.variant.name
        this[Keys.DIFFICULTY] = settings.mode.difficulty.name
        this[Keys.WIN_STREAK] = settings.winStreak
    }

    private inline fun <reified T : Enum<T>> enumOrDefault(name: String?, default: T): T =
        enumValues<T>().firstOrNull { it.name == name } ?: default

    private object Keys {
        val VARIANT = stringPreferencesKey("variant")
        val DIFFICULTY = stringPreferencesKey("difficulty")
        val WIN_STREAK = intPreferencesKey("win_streak")
    }
}
