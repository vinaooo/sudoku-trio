package io.github.vinaooo.sudokutrio.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import io.github.vinaooo.sudokutrio.domain.model.GameMode
import io.github.vinaooo.sudokutrio.domain.model.Settings
import io.github.vinaooo.sudokutrio.domain.repository.SettingsRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Settings in Preferences DataStore. A value this version doesn't know (from a newer one) reads as the default. */
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
            themeMode = enumOrDefault(this[Keys.THEME_MODE], defaults.themeMode),
            dynamicColor = this[Keys.DYNAMIC_COLOR] ?: defaults.dynamicColor,
            themeColor = enumOrDefault(this[Keys.THEME_COLOR], defaults.themeColor),
            soundEnabled = this[Keys.SOUND] ?: defaults.soundEnabled,
            hapticsEnabled = this[Keys.HAPTICS] ?: defaults.hapticsEnabled,
            handedness = enumOrDefault(this[Keys.HANDEDNESS], defaults.handedness),
            boardAlignment = enumOrDefault(this[Keys.BOARD_ALIGNMENT], defaults.boardAlignment),
            phoneView = this[Keys.PHONE_VIEW] ?: defaults.phoneView,
            phoneViewSide = enumOrDefault(this[Keys.PHONE_VIEW_SIDE], defaults.phoneViewSide),
        )
    }

    private fun MutablePreferences.write(settings: Settings) {
        this[Keys.VARIANT] = settings.mode.variant.name
        this[Keys.DIFFICULTY] = settings.mode.difficulty.name
        this[Keys.THEME_MODE] = settings.themeMode.name
        this[Keys.DYNAMIC_COLOR] = settings.dynamicColor
        this[Keys.THEME_COLOR] = settings.themeColor.name
        this[Keys.SOUND] = settings.soundEnabled
        this[Keys.HAPTICS] = settings.hapticsEnabled
        this[Keys.HANDEDNESS] = settings.handedness.name
        this[Keys.BOARD_ALIGNMENT] = settings.boardAlignment.name
        this[Keys.PHONE_VIEW] = settings.phoneView
        this[Keys.PHONE_VIEW_SIDE] = settings.phoneViewSide.name
    }

    private inline fun <reified T : Enum<T>> enumOrDefault(name: String?, default: T): T =
        enumValues<T>().firstOrNull { it.name == name } ?: default

    private object Keys {
        val VARIANT = stringPreferencesKey("variant")
        val DIFFICULTY = stringPreferencesKey("difficulty")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val THEME_COLOR = stringPreferencesKey("theme_color")
        val SOUND = booleanPreferencesKey("sound")
        val HAPTICS = booleanPreferencesKey("haptics")
        val HANDEDNESS = stringPreferencesKey("handedness")
        val BOARD_ALIGNMENT = stringPreferencesKey("board_alignment")
        val PHONE_VIEW = booleanPreferencesKey("phone_view")
        val PHONE_VIEW_SIDE = stringPreferencesKey("phone_view_side")
    }
}
