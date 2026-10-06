package io.github.vinaooo.sudokutrio.domain.model

/** Every player action. Cells are 0–80, row by row. */
sealed interface Move {
    /** Writes [digit] in an editable cell, replacing what was there, and clears the cell's notes. */
    data class Place(val cell: Int, val digit: Int) : Move

    /** Clears an editable cell's digit and notes. */
    data class Erase(val cell: Int) : Move

    /** Adds or removes a pencil mark in an empty cell. */
    data class ToggleNote(val cell: Int, val digit: Int) : Move

    /** Shows the next hint ([GameState.pendingHint]); this is when a hint is counted. */
    data object RevealHint : Move

    /** Carries out the shown hint: writes its digit, or erases the wrong one it points at. */
    data object ApplyHint : Move
}
