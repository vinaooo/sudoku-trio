package io.github.vinaooo.sudokutrio.feature.game.ui

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
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
import io.github.vinaooo.sudokutrio.feature.game.GameUiState
import io.github.vinaooo.vinkit.core.AppSettings
import io.github.vinaooo.vinkit.core.BoardAlignment
import io.github.vinaooo.vinkit.core.Handedness
import io.github.vinaooo.vinkit.core.PhoneViewSide
import io.github.vinaooo.vinkit.core.ThemeMode
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp-port-xxhdpi")
class GameScreenScreenshotTest {
    @get:Rule
    val compose = createComposeRule()

    private val engine = GameEngine()
    private val solution = (
        "249368715356971824781542639512783496467195283893426571928614357675839142134257968"
        ).map { it.digitToInt() }

    /** Row 0 in three cages; the rest with every third cell empty. */
    private val cages = listOf(Cage(15, listOf(0, 1, 2)), Cage(17, listOf(3, 4, 5)), Cage(13, listOf(6, 7, 8, 17)))

    private fun session(variant: Variant, empty: (Int) -> Boolean): GameSession {
        val puzzle = Puzzle(
            solution.mapIndexed { cell, d -> if (empty(cell)) 0 else d },
            solution,
            if (variant == Variant.KILLER) cages else emptyList(),
        )
        return GameSession(1, engine.newGame(puzzle, GameMode(variant, Difficulty.MEDIUM)).copy(elapsedSeconds = 125))
    }

    private fun GameSession.then(vararg moves: Move) = moves.fold(this) { s, m -> s.play(m, engine)!! }

    private fun capture(name: String, state: GameUiState, themeMode: ThemeMode = ThemeMode.LIGHT) {
        compose.setContent {
            SudokuTrioTheme(themeMode = themeMode, dynamicColor = false) { GameScreen(state, {}) }
        }
        compose.onRoot().captureRoboImage("src/test/screenshots/$name.png")
    }

    private fun ui(
        session: GameSession,
        selected: Int?,
        notesMode: Boolean = false,
        settings: AppSettings = AppSettings(),
    ) = GameUiState(
        appSettings = settings,
        session = session,
        loading = false,
        selected = selected,
        notesMode = notesMode,
        conflicts = ConflictFinder().conflicts(session.state),
    )

    @Test
    fun classic_light_playing() {
        val playing = session(Variant.CLASSIC) { it % 3 == 0 }
            .then(
                Move.Place(0, 2),
                Move.ToggleNote(3, 3),
                Move.ToggleNote(3, 6),
                Move.ToggleNote(3, 8),
                Move.Place(9, 4),
            )
        capture("classic_light_playing", ui(playing, selected = 0, notesMode = true))
    }

    @Test
    fun killer_dark_notes_under_sums() {
        val killer = session(Variant.KILLER) { it < 9 || it % 3 == 0 }
            .then(*(1..9).map { Move.ToggleNote(0, it) }.toTypedArray(), Move.ToggleNote(6, 1), Move.Place(4, 6))
        capture("killer_dark_notes_under_sums", ui(killer, selected = 4), ThemeMode.DARK)
    }

    @Test
    fun x_light_hint() {
        val hinted = session(Variant.X) { it % 3 == 0 }.then(Move.RevealHint)
        capture("x_light_hint", ui(hinted, selected = null))
    }

    @Test
    @Config(qualifiers = "w891dp-h411dp-land-xxhdpi")
    fun classic_dark_landscape() {
        capture("classic_dark_landscape", ui(session(Variant.CLASSIC) { it % 3 == 0 }, selected = 40), ThemeMode.DARK)
    }

    /** A short phone: no room is left under the board, so the hint banner covers its bottom rows. */
    @Test
    @Config(qualifiers = "w360dp-h640dp-port-xhdpi")
    fun classic_light_short_phone_hint() {
        val hinted = session(Variant.CLASSIC) { it == 76 }.then(Move.RevealHint)
        capture("classic_light_short_phone_hint", ui(hinted, selected = 76))
    }

    @Test
    fun classic_light_left_hand_bottom_board() {
        val left = AppSettings(handedness = Handedness.LEFT, boardAlignment = BoardAlignment.BOTTOM)
        capture(
            "classic_light_left_hand_bottom_board",
            ui(
                session(Variant.CLASSIC) {
                    it % 3 == 0
                },
                0,
                settings = left,
            ),
        )
    }

    @Test
    @Config(qualifiers = "w891dp-h411dp-land-xxhdpi")
    fun killer_light_landscape_left_hand_hint() {
        val hinted = session(Variant.KILLER) { it < 9 || it % 3 == 0 }.then(Move.RevealHint)
        capture(
            "killer_light_landscape_left_hand_hint",
            ui(hinted, selected = null, settings = AppSettings(handedness = Handedness.LEFT)),
        )
    }

    @Test
    @Config(qualifiers = "sw800dp-w800dp-h1280dp-port-xhdpi")
    fun tablet_phone_view_left() {
        val tablet = AppSettings(phoneView = true, phoneViewSide = PhoneViewSide.LEFT)
        capture("tablet_phone_view_left", ui(session(Variant.X) { it % 3 == 0 }, 40, settings = tablet))
    }

    @Test
    @Config(qualifiers = "pt-rBR-w411dp-h891dp-port-xxhdpi")
    fun x_light_hint_pt_br() {
        capture("x_light_hint_pt_br", ui(session(Variant.X) { it % 3 == 0 }.then(Move.RevealHint), selected = null))
    }
}
