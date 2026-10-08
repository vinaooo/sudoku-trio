package io.github.vinaooo.sudokutrio.feature.game.ui

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import io.github.vinaooo.sudokutrio.core.designsystem.theme.SudokuTrioTheme
import io.github.vinaooo.sudokutrio.domain.model.Difficulty
import io.github.vinaooo.sudokutrio.domain.model.GameMode
import io.github.vinaooo.sudokutrio.domain.model.Move
import io.github.vinaooo.sudokutrio.domain.model.Puzzle
import io.github.vinaooo.sudokutrio.domain.model.Variant
import io.github.vinaooo.sudokutrio.domain.model.key
import io.github.vinaooo.sudokutrio.domain.rules.GameEngine
import io.github.vinaooo.sudokutrio.domain.session.GameSession
import io.github.vinaooo.sudokutrio.feature.game.GameIntent
import io.github.vinaooo.sudokutrio.feature.game.GameUiState
import io.github.vinaooo.vinkit.core.ScoreRecord
import io.kotest.matchers.collections.shouldContainExactly
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The game screen's controls send their intents; the screen shows what the state says. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp-port")
class GameScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val engine = GameEngine()
    private val solution = (
        "249368715356971824781542639512783496467195283893426571928614357675839142134257968"
        ).map { it.digitToInt() }
    private val puzzle = Puzzle(solution.mapIndexed { cell, d -> if (cell % 3 == 0) 0 else d }, solution)
    private val session = GameSession(1, engine.newGame(puzzle, GameMode(Variant.CLASSIC, Difficulty.EASY)))
    private val intents = mutableListOf<GameIntent>()

    private fun show(state: GameUiState) = compose.setContent {
        SudokuTrioTheme { GameScreen(state, { intents += it }) }
    }

    private fun ready(session: GameSession = this.session) = GameUiState(session = session, loading = false)

    @Test
    fun `tapping a cell selects it`() {
        show(ready())
        compose.onNodeWithTag(BOARD_TAG).performTouchInput {
            // Row 2, column 4 of a square board as wide as the node.
            val cell = width / 9f
            click(androidx.compose.ui.geometry.Offset(cell * 4.5f, cell * 2.5f))
        }
        intents shouldContainExactly listOf(GameIntent.SelectCell(22))
    }

    @Test
    fun `the number pad sends digits, erase and notes`() {
        show(ready())
        compose.onNodeWithText("7").performClick()
        compose.onNodeWithContentDescription("Erase").performClick()
        compose.onNodeWithContentDescription("Notes").performClick()
        intents shouldContainExactly listOf(GameIntent.Digit(7), GameIntent.Erase, GameIntent.ToggleNotes)
    }

    @Test
    fun `undo and redo follow the session, and hint is always there`() {
        show(ready())
        compose.onNodeWithContentDescription("Undo").assertIsNotEnabled()
        compose.onNodeWithContentDescription("Redo").assertIsNotEnabled()
        compose.onNodeWithContentDescription("Hint").performClick()
        intents shouldContainExactly listOf(GameIntent.Hint)
    }

    @Test
    fun `undo is offered after a move`() {
        show(ready(session.play(Move.ToggleNote(0, 1), engine)!!))
        compose.onNodeWithContentDescription("Undo").assertIsEnabled().performClick()
        intents shouldContainExactly listOf(GameIntent.Undo)
    }

    @Test
    fun `a hint on show names its technique and turns the hint button into apply`() {
        show(ready(session.play(Move.RevealHint, engine)!!))
        compose.onNodeWithText("Look at the highlighted cells. Tap the hint again to fill it in.").assertExists()
        compose.onNodeWithContentDescription("Apply the hint").performClick()
        intents shouldContainExactly listOf(GameIntent.Hint)
    }

    @Test
    fun `the top bar reads the mode and the time as one item`() {
        show(ready(session.tick(65, engine)))
        compose.onNodeWithContentDescription("Classic · Easy, Time, 1 minute 5 seconds").assertExists()
    }

    @Test
    fun `while a puzzle is made the screen says so and the pad waits`() {
        show(GameUiState(loading = true))
        compose.onNodeWithContentDescription("Making a new puzzle").assertExists()
        compose.onNodeWithText("7").assertIsNotEnabled()
    }

    @Test
    fun `the new game menu starts a new game or restarts this one`() {
        show(ready())
        compose.onNodeWithContentDescription("New game").performClick()
        compose.onNodeWithText("Restart this board").performClick()
        compose.onNodeWithContentDescription("New game").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("New game").performClick()
        intents shouldContainExactly listOf(GameIntent.Restart, GameIntent.NewGame)
    }

    @Test
    fun `the win shows the score, time, mistakes and hints`() {
        show(
            ready().copy(
                winRecord = ScoreRecord(
                    session.state.mode.key,
                    795,
                    65,
                    0,
                    mapOf(
                        "mistakes" to "2",
                        "hints" to "1",
                    ),
                ),
            ),
        )
        compose.onNodeWithText("You won!").assertExists()
        compose.onNodeWithText("Score: 795").assertExists()
        compose.onNodeWithText("Time: 1:05").assertExists()
        compose.onNodeWithText("Mistakes: 2").assertExists()
        compose.onNodeWithText("Hints: 1").assertExists()
    }

    @Test
    @Config(qualifiers = "w891dp-h411dp-land")
    fun `landscape shows the board and every control`() {
        show(ready())
        compose.onNodeWithTag(BOARD_TAG).assertExists()
        compose.onNodeWithContentDescription("Notes").assertExists()
        compose.onNodeWithContentDescription("Hint").assertExists()
    }

    @Test
    fun `scores and settings stay reachable while a hint shows`() {
        var opened = 0
        compose.setContent {
            SudokuTrioTheme {
                GameScreen(ready(session.play(Move.RevealHint, engine)!!), {
                }, onOpenScores = {}, onOpenSettings = { opened++ })
            }
        }
        compose.onNodeWithContentDescription("Scores").assertExists()
        compose.onNodeWithContentDescription("Settings").performClick()
        listOf(opened) shouldContainExactly listOf(1)
    }

    @Test
    fun `the notes toggle in the toolbar shows when notes mode is on`() {
        show(ready().copy(notesMode = true))
        compose.onNodeWithContentDescription("Notes").assertIsOn().performClick()
        intents shouldContainExactly listOf(GameIntent.ToggleNotes)
    }
}
