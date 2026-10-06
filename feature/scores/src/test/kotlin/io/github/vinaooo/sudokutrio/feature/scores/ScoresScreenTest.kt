package io.github.vinaooo.sudokutrio.feature.scores

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.github.vinaooo.sudokutrio.core.designsystem.theme.SudokuTrioTheme
import io.github.vinaooo.sudokutrio.domain.model.Difficulty
import io.github.vinaooo.sudokutrio.domain.model.GameMode
import io.github.vinaooo.sudokutrio.domain.model.GameStats
import io.github.vinaooo.sudokutrio.domain.model.ScoreRecord
import io.github.vinaooo.sudokutrio.domain.model.Variant
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
    fun `tabs per mode, the stats read as items, and the back arrow`() {
        val selected = mutableListOf<GameMode>()
        var back = 0
        compose.setContent {
            SudokuTrioTheme {
                ScoresScreen(
                    ScoresUiState(
                        scores = listOf(ScoreRecord(classic, 900, 65, 2, 1, NOON)),
                        stats = GameStats(played = 4, won = 3),
                        isLoading = false,
                        modes = listOf(classic, killer),
                        mode = classic,
                    ),
                    onBack = { back++ },
                    onSelectMode = { selected += it },
                )
            }
        }
        compose.onNodeWithText("900").assertExists()
        compose.onNodeWithText("2 mistakes · 1 hint", substring = true).assertExists()
        compose.onNodeWithContentDescription("Won, 3").assertExists()
        compose.onNodeWithContentDescription("Win rate, 75%").assertExists()
        compose.onNodeWithText("Killer · Hard").performClick()
        selected shouldContainExactly listOf(killer)
        compose.onNodeWithContentDescription("Back").performClick()
        listOf(back) shouldContainExactly listOf(1)
    }

    @Test
    fun `a mode played but never won shows its stats and an invitation`() {
        compose.setContent {
            SudokuTrioTheme {
                ScoresScreen(
                    ScoresUiState(
                        stats = GameStats(played = 2),
                        isLoading = false,
                        modes = listOf(killer),
                        mode = killer,
                    ),
                    onBack = {},
                )
            }
        }
        compose.onNodeWithText("Killer · Hard").assertExists()
        compose.onNodeWithContentDescription("Played, 2").assertExists()
        compose.onNodeWithText("Win a game to see your scores here.").assertExists()
    }

    private companion object {
        /** Noon UTC, so the date is the same in every time zone that runs the tests. */
        const val NOON = 1_767_268_800_000L
    }
}
