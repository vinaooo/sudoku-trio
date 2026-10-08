package io.github.vinaooo.sudokutrio.feature.game

import io.github.vinaooo.sudokutrio.domain.model.Settings
import io.github.vinaooo.sudokutrio.domain.session.GameSession
import io.github.vinaooo.vinkit.core.AppSettings
import io.github.vinaooo.vinkit.core.ScoreRecord

data class GameUiState(
    val session: GameSession? = null,
    val settings: Settings = Settings(),
    /** The settings every vinkit game has: feedback, hand, board position, phone view. */
    val appSettings: AppSettings = AppSettings(),
    /** A puzzle is being generated: the board, pad and toolbar wait, and the clock stops. */
    val loading: Boolean = true,
    val selected: Int? = null,
    /** Digits typed go in as pencil marks. */
    val notesMode: Boolean = false,
    /** The cells repeating a digit in one of their groups, or in a cage over its sum. */
    val conflicts: Set<Int> = emptySet(),
    val winRecord: ScoreRecord? = null,
    val announcement: Announced? = null,
)

sealed interface GameIntent {
    data class SelectCell(val cell: Int) : GameIntent

    /** A number pad key: writes [digit], or toggles it as a note in notes mode, in the selected cell. */
    data class Digit(val digit: Int) : GameIntent

    data object Erase : GameIntent

    data object ToggleNotes : GameIntent

    data object Undo : GameIntent

    data object Redo : GameIntent

    /** The first tap shows the next hint, the second carries it out. */
    data object Hint : GameIntent

    data object NewGame : GameIntent

    data object Restart : GameIntent

    /** Handled by the screen, which captures the board first. */

    /** The screen became visible: the clock may run. */
    data object Resume : GameIntent

    /** The screen went to the background: stop the clock and save. */
    data object Pause : GameIntent
}
