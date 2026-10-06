package io.github.vinaooo.sudokutrio.feature.game.ui

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.unit.dp
import io.github.vinaooo.sudokutrio.core.designsystem.theme.SudokuTrioTheme
import io.github.vinaooo.sudokutrio.domain.model.Cage
import io.github.vinaooo.sudokutrio.domain.model.Difficulty
import io.github.vinaooo.sudokutrio.domain.model.GameMode
import io.github.vinaooo.sudokutrio.domain.model.Move
import io.github.vinaooo.sudokutrio.domain.model.Puzzle
import io.github.vinaooo.sudokutrio.domain.model.Variant
import io.github.vinaooo.sudokutrio.domain.rules.ConflictFinder
import io.github.vinaooo.sudokutrio.domain.rules.GameEngine
import io.github.vinaooo.sudokutrio.domain.session.GameSession
import io.github.vinaooo.sudokutrio.feature.game.Announced
import io.github.vinaooo.sudokutrio.feature.game.Announcement
import io.github.vinaooo.sudokutrio.feature.game.GameIntent
import io.github.vinaooo.sudokutrio.feature.game.GameUiState
import io.kotest.matchers.collections.shouldContainExactly
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp-port")
class GameAccessibilityTest {
    @get:Rule
    val compose = createComposeRule()

    private val engine = GameEngine()
    private val solution = (
        "249368715356971824781542639512783496467195283893426571928614357675839142134257968"
        ).map { it.digitToInt() }
    private val puzzle = Puzzle(
        solution.mapIndexed { cell, d -> if (cell % 3 == 0) 0 else d },
        solution,
        listOf(Cage(15, listOf(0, 1, 2))),
    )
    private val intents = mutableListOf<GameIntent>()

    private fun show(state: GameUiState) = compose.setContent {
        SudokuTrioTheme { GameScreen(state, { intents += it }) }
    }

    private fun killer(vararg moves: Move): GameUiState {
        val session = moves.fold(GameSession(1, engine.newGame(puzzle, GameMode(Variant.KILLER, Difficulty.EASY)))) {
                s,
                m,
            ->
            s.play(m, engine)!!
        }
        return GameUiState(session = session, loading = false, conflicts = ConflictFinder().conflicts(session.state))
    }

    @Test
    fun `each cell reads its place, its digit or notes, conflicts and its cage`() {
        show(killer(Move.ToggleNote(0, 1), Move.ToggleNote(0, 7), Move.Place(3, 4)))
        compose.onNodeWithContentDescription("row 1, column 1, empty, notes 1 7, cage of 15").assertExists()
        compose.onNodeWithContentDescription("row 1, column 2, 4, given, repeated, cage of 15").assertExists()
        compose.onNodeWithContentDescription("row 1, column 4, 4, repeated").assertExists()
        compose.onNodeWithContentDescription("row 9, column 9, 8, given").assertExists()
    }

    @Test
    fun `a double tap on a cell selects it, which TalkBack reads as selected`() {
        show(killer().copy(selected = 40))
        compose.onNodeWithContentDescription("row 5, column 5, 9, given").assert(
            SemanticsMatcher.expectValue(SemanticsProperties.Selected, true),
        )
        compose.onNodeWithContentDescription("row 3, column 7, empty").performSemanticsAction(SemanticsActions.OnClick)
        intents shouldContainExactly listOf(GameIntent.SelectCell(24))
    }

    @Test
    fun `the announcer stays a 1dp live region, never stretched over the screen`() {
        show(killer().copy(announcement = Announced(Announcement.Undone, 1)))
        compose.onNodeWithContentDescription("Move undone", substring = true)
            .assertWidthIsEqualTo(1.dp)
            .assertHeightIsEqualTo(1.dp)
    }
}
