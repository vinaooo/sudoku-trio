package io.github.vinaooo.sudokutrio.feature.scores

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.github.vinaooo.sudokutrio.core.designsystem.theme.SudokuTrioTheme
import io.github.vinaooo.sudokutrio.domain.model.Difficulty
import io.github.vinaooo.sudokutrio.domain.model.GameMode
import io.github.vinaooo.sudokutrio.domain.model.Variant
import io.github.vinaooo.sudokutrio.domain.model.key
import io.github.vinaooo.vinkit.core.GameStats
import io.github.vinaooo.vinkit.core.ScoreRecord
import io.github.vinaooo.vinkit.scores.ModeSection
import io.github.vinaooo.vinkit.scores.ScoresUiState
import io.kotest.matchers.collections.shouldContainExactly
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ScoresScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val classic = GameMode(Variant.CLASSIC, Difficulty.EASY)
    private val killer = GameMode(Variant.KILLER, Difficulty.HARD)

    @Test
    fun `tabs per variant, the stats read as items, and the back arrow`() {
        val selected = mutableListOf<String>()
        var back = 0
        compose.setContent {
            SudokuTrioTheme {
                ScoresScreen(
                    ScoresUiState(
                        isLoading = false,
                        groups = listOf("CLASSIC", "KILLER"),
                        group = "CLASSIC",
                        sections = listOf(
                            ModeSection(
                                classic.key,
                                GameStats(played = 4, won = 3),
                                listOf(
                                    ScoreRecord(classic.key, 900, 65, NOON, mapOf("mistakes" to "2", "hints" to "1")),
                                ),
                            ),
                        ),
                    ),
                    onBack = { back++ },
                    onSelectVariant = { selected += it },
                )
            }
        }
        compose.onNodeWithText("900").assertExists()
        compose.onNodeWithText("2 mistakes · 1 hint", substring = true).assertExists()
        compose.onNodeWithContentDescription("Won, 3").assertExists()
        compose.onNodeWithContentDescription("Win rate, 75%").assertExists()
        compose.onNodeWithText("Killer").performClick()
        selected shouldContainExactly listOf("KILLER")
        compose.onNodeWithContentDescription("Back").performClick()
        listOf(back) shouldContainExactly listOf(1)
    }

    @Test
    fun `a mode played but never won shows its stats and an invitation`() {
        compose.setContent {
            SudokuTrioTheme {
                ScoresScreen(
                    ScoresUiState(
                        isLoading = false,
                        groups = listOf("KILLER"),
                        group = "KILLER",
                        sections = listOf(ModeSection(killer.key, GameStats(played = 2))),
                    ),
                    onBack = {},
                )
            }
        }
        compose.onNodeWithText("Killer").assertExists()
        compose.onNodeWithContentDescription("Played, 2").assertExists()
        compose.onNodeWithText("Win a game to see your scores here.").assertExists()
    }

    private companion object {
        /** Noon UTC, so the date is the same in every time zone that runs the tests. */
        const val NOON = 1_767_268_800_000L
    }
}
