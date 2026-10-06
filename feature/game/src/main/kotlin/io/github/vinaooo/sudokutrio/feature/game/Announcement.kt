package io.github.vinaooo.sudokutrio.feature.game

import io.github.vinaooo.sudokutrio.domain.model.GameState
import io.github.vinaooo.sudokutrio.domain.model.Hint
import io.github.vinaooo.sudokutrio.domain.model.Move

/** What TalkBack says after an action. A wrong digit is announced like a right one: mistakes are silent. */
sealed interface Announcement {
    data class Placed(val cell: Int, val digit: Int) : Announcement

    data class Erased(val cell: Int) : Announcement

    data class Noted(val cell: Int, val digit: Int, val added: Boolean) : Announcement

    data object Undone : Announcement

    data object Redone : Announcement

    data class Hinted(val hint: Hint) : Announcement
}

/** An [announcement] numbered by [sequence], so saying the same thing twice in a row still counts as new. */
data class Announced(val announcement: Announcement, val sequence: Int)

/** The announcement for [move], played from [before] to [after]. */
internal fun announcementFor(move: Move, before: GameState, after: GameState): Announcement = when (move) {
    is Move.Place -> Announcement.Placed(move.cell, move.digit)
    is Move.Erase -> Announcement.Erased(move.cell)
    is Move.ToggleNote -> Announcement.Noted(move.cell, move.digit, added = move.digit in after.board.notes[move.cell])
    Move.RevealHint -> Announcement.Hinted(checkNotNull(after.pendingHint))
    Move.ApplyHint -> when (val hint = checkNotNull(before.pendingHint)) {
        is Hint.Placement -> Announcement.Placed(hint.cell, hint.digit)
        is Hint.WrongDigit -> Announcement.Erased(hint.cell)
    }
}
