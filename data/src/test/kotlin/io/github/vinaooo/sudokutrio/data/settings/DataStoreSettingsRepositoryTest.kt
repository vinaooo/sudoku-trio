package io.github.vinaooo.sudokutrio.data.settings

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import io.github.vinaooo.sudokutrio.domain.model.BoardAlignment
import io.github.vinaooo.sudokutrio.domain.model.Difficulty
import io.github.vinaooo.sudokutrio.domain.model.GameMode
import io.github.vinaooo.sudokutrio.domain.model.Handedness
import io.github.vinaooo.sudokutrio.domain.model.PhoneViewSide
import io.github.vinaooo.sudokutrio.domain.model.Settings
import io.github.vinaooo.sudokutrio.domain.model.ThemeColor
import io.github.vinaooo.sudokutrio.domain.model.ThemeMode
import io.github.vinaooo.sudokutrio.domain.model.Variant
import io.kotest.matchers.shouldBe
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class DataStoreSettingsRepositoryTest {
    @TempDir
    lateinit var dir: File

    private val scope = TestScope(StandardTestDispatcher())

    private val store by lazy {
        PreferenceDataStoreFactory.create(scope = scope.backgroundScope) { File(dir, "settings.preferences_pb") }
    }

    private fun repository() = DataStoreSettingsRepository(store)

    @Test
    fun `first launch reads the defaults`() = scope.runTest {
        repository().settings.first() shouldBe Settings()
    }

    @Test
    fun `every setting is persisted`() = scope.runTest {
        val changed = Settings(
            mode = GameMode(Variant.KILLER, Difficulty.EXPERT),
            themeMode = ThemeMode.DARK,
            dynamicColor = false,
            themeColor = ThemeColor.PURPLE,
            soundEnabled = false,
            hapticsEnabled = false,
            handedness = Handedness.LEFT,
            boardAlignment = BoardAlignment.BOTTOM,
            phoneView = true,
            phoneViewSide = PhoneViewSide.LEFT,
        )
        repository().update { changed }

        repository().settings.first() shouldBe changed
    }

    @Test
    fun `updates transform the current value`() = scope.runTest {
        val repository = repository()
        repository.update { it.copy(themeMode = ThemeMode.LIGHT) }
        repository.update { it.copy(soundEnabled = false) }

        repository.settings.first() shouldBe Settings(themeMode = ThemeMode.LIGHT, soundEnabled = false)
    }

    @Test
    fun `a value from a newer version reads as the default`() = scope.runTest {
        store.edit {
            it[stringPreferencesKey("variant")] = "SAMURAI"
            it[stringPreferencesKey("theme_color")] = "GOLD"
        }

        repository().settings.first() shouldBe Settings()
    }
}
