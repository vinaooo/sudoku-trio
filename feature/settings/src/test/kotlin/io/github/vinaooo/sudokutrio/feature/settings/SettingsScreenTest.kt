package io.github.vinaooo.sudokutrio.feature.settings

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import io.github.vinaooo.sudokutrio.core.designsystem.theme.SudokuTrioTheme
import io.github.vinaooo.sudokutrio.domain.model.Difficulty
import io.github.vinaooo.sudokutrio.domain.model.Settings
import io.github.vinaooo.sudokutrio.domain.model.Variant
import io.github.vinaooo.vinkit.core.BoardAlignment
import io.github.vinaooo.vinkit.core.Handedness
import io.github.vinaooo.vinkit.core.ThemeColor
import io.github.vinaooo.vinkit.core.ThemeMode
import io.kotest.matchers.collections.shouldContainExactly
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp-port")
class SettingsScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val changes = mutableListOf<SettingsChange>()

    private fun show(settings: Settings = Settings(), privacyOptions: Boolean = false, onPolicy: () -> Unit = {}) =
        compose.setContent {
            SudokuTrioTheme {
                SettingsScreen(
                    settings,
                    { changes += it },
                    onBack = {},
                    privacyOptionsRequired = privacyOptions,
                    onOpenPrivacyPolicy = onPolicy,
                )
            }
        }

    @Test
    fun `the game section picks the variant and the difficulty`() {
        show()
        compose.onNodeWithText("Killer").performClick()
        compose.onNodeWithContentDescription("Expert").performClick()
        changes shouldContainExactly listOf(
            SettingsChange.VariantChanged(Variant.KILLER),
            SettingsChange.DifficultyChanged(Difficulty.EXPERT),
        )
    }

    @Test
    fun `the chosen difficulty is named and explained under its buttons`() {
        show(Settings(mode = Settings().mode.copy(difficulty = Difficulty.HARD)))
        compose.onNodeWithText("Long solves with triples and X-wings").assertExists()
    }

    @Test
    fun `appearance and feedback rows send their changes`() {
        show(Settings(dynamicColor = false))
        compose.onNodeWithText("Dark").performScrollTo().performClick()
        compose.onNodeWithContentDescription("Orange").performScrollTo().performClick()
        compose.onNodeWithText("Left").performScrollTo().performClick()
        compose.onNodeWithText("Bottom").performScrollTo().performClick()
        compose.onNodeWithText("Vibration").performScrollTo().performClick()
        changes shouldContainExactly listOf(
            SettingsChange.ThemeModeChanged(ThemeMode.DARK),
            SettingsChange.ThemeColorChanged(ThemeColor.ORANGE),
            SettingsChange.HandednessChanged(Handedness.LEFT),
            SettingsChange.BoardAlignmentChanged(BoardAlignment.BOTTOM),
            SettingsChange.HapticsChanged(false),
        )
    }

    @Test
    fun `the color row hides while dynamic color is on`() {
        show(Settings(dynamicColor = true))
        compose.onNodeWithContentDescription("Orange").assertDoesNotExist()
    }

    @Test
    fun `the privacy policy is always there, privacy options only when required`() {
        var opened = 0
        show(onPolicy = { opened++ })
        compose.onNodeWithText("Privacy options").assertDoesNotExist()
        compose.onNodeWithText("Privacy policy").performScrollTo().performClick()
        listOf(opened) shouldContainExactly listOf(1)
    }

    @Test
    @Config(qualifiers = "sw600dp-w800dp-h1280dp-port")
    fun `a tablet offers phone view and, once on, its side`() {
        show(Settings(phoneView = true))
        compose.onNodeWithText("Phone view").assertExists()
        compose.onNodeWithText("Board side").assertExists()
    }
}
