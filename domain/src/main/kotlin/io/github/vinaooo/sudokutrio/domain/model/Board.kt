package io.github.vinaooo.sudokutrio.domain.model

import kotlinx.serialization.Serializable

/** What the player has written: a digit per cell (0 when empty) and the pencil marks of each cell. */
@Serializable
data class Board(val values: List<Int>, val notes: List<Set<Int>> = List(Grid.SIZE) { emptySet() }) {
    init {
        require(values.size == Grid.SIZE && notes.size == Grid.SIZE) { "A board has ${Grid.SIZE} cells." }
    }

    fun withValue(cell: Int, digit: Int): Board = copy(values = values.replaced(cell, digit))

    fun withNotes(cell: Int, digits: Set<Int>): Board = copy(notes = notes.replaced(cell, digits))

    /** Removes [digit] from the notes of every cell in [cells]. */
    fun withoutNote(cells: Collection<Int>, digit: Int): Board =
        copy(notes = notes.mapIndexed { cell, marks -> if (cell in cells) marks - digit else marks })

    companion object {
        fun from(puzzle: Puzzle): Board = Board(puzzle.givens)
    }
}

private fun <T> List<T>.replaced(index: Int, value: T): List<T> = toMutableList().also { it[index] = value }
