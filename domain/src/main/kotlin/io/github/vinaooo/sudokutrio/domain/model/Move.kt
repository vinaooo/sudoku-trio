package io.github.vinaooo.sudokutrio.domain.model

/** Every player action. [cell] is 0–80, row by row. */
sealed interface Move {
    val cell: Int

    /** Writes [digit] in an editable cell, replacing what was there, and clears the cell's notes. */
    data class Place(override val cell: Int, val digit: Int) : Move

    /** Clears an editable cell's digit and notes. */
    data class Erase(override val cell: Int) : Move

    /** Adds or removes a pencil mark in an empty cell. */
    data class ToggleNote(override val cell: Int, val digit: Int) : Move
}
