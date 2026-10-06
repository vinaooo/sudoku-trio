package io.github.vinaooo.sudokutrio.domain.rules

import io.github.vinaooo.sudokutrio.domain.model.Grid
import io.github.vinaooo.sudokutrio.domain.model.Puzzle
import io.github.vinaooo.sudokutrio.domain.model.Variant

/**
 * One kind of group whose digits never repeat. A variant is the set of constraints it adds up; a new variant adds
 * constraints instead of changing the rules.
 */
fun interface Constraint {
    fun units(puzzle: Puzzle): List<List<Int>>
}

object RowConstraint : Constraint {
    private val rows = Grid.CELLS.chunked(Grid.SIDE)

    override fun units(puzzle: Puzzle) = rows
}

object ColumnConstraint : Constraint {
    private val columns = (0 until Grid.SIDE).map { column -> (0 until Grid.SIDE).map { Grid.cell(it, column) } }

    override fun units(puzzle: Puzzle) = columns
}

object BoxConstraint : Constraint {
    private val boxes = Grid.CELLS.groupBy { Grid.row(it) / Grid.BOX * Grid.BOX + Grid.column(it) / Grid.BOX }
        .values.toList()

    override fun units(puzzle: Puzzle) = boxes
}

/** Sudoku X: both main diagonals. */
object DiagonalConstraint : Constraint {
    private val diagonals = listOf(
        (0 until Grid.SIDE).map { Grid.cell(it, it) },
        (0 until Grid.SIDE).map { Grid.cell(it, Grid.SIDE - 1 - it) },
    )

    override fun units(puzzle: Puzzle) = diagonals
}

/** Killer Sudoku: no digit repeats inside a cage. Its sum is checked by [ConflictFinder]. */
object CageConstraint : Constraint {
    override fun units(puzzle: Puzzle) = puzzle.cages.map { it.cells }
}

private val classic = listOf(RowConstraint, ColumnConstraint, BoxConstraint)

fun constraintsFor(variant: Variant): List<Constraint> = when (variant) {
    Variant.CLASSIC -> classic
    Variant.X -> classic + DiagonalConstraint
    Variant.KILLER -> classic + CageConstraint
}

/** Every group of a puzzle under [constraints]. */
fun List<Constraint>.units(puzzle: Puzzle): List<List<Int>> = flatMap { it.units(puzzle) }

/** The cells that share a group with [cell], [cell] itself excluded. */
fun List<List<Int>>.peersOf(cell: Int): Set<Int> = filter { cell in it }.flatten().toSet() - cell
