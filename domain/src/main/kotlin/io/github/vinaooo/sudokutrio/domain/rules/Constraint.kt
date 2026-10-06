package io.github.vinaooo.sudokutrio.domain.rules

import io.github.vinaooo.sudokutrio.domain.model.Cage
import io.github.vinaooo.sudokutrio.domain.model.Grid
import io.github.vinaooo.sudokutrio.domain.model.Puzzle
import io.github.vinaooo.sudokutrio.domain.model.Variant

/**
 * One kind of group whose digits never repeat. A variant is the set of constraints it adds up; a new variant adds
 * constraints instead of changing the rules.
 */
fun interface Constraint {
    /** The groups, given the board's [cages] (Killer only). */
    fun units(cages: List<Cage>): List<List<Int>>
}

object RowConstraint : Constraint {
    private val rows = Grid.CELLS.chunked(Grid.SIDE)

    override fun units(cages: List<Cage>) = rows
}

object ColumnConstraint : Constraint {
    private val columns = (0 until Grid.SIDE).map { column -> (0 until Grid.SIDE).map { Grid.cell(it, column) } }

    override fun units(cages: List<Cage>) = columns
}

object BoxConstraint : Constraint {
    private val boxes = Grid.CELLS.groupBy { Grid.row(it) / Grid.BOX * Grid.BOX + Grid.column(it) / Grid.BOX }
        .values.toList()

    override fun units(cages: List<Cage>) = boxes
}

/** Sudoku X: both main diagonals. */
object DiagonalConstraint : Constraint {
    private val diagonals = listOf(
        (0 until Grid.SIDE).map { Grid.cell(it, it) },
        (0 until Grid.SIDE).map { Grid.cell(it, Grid.SIDE - 1 - it) },
    )

    override fun units(cages: List<Cage>) = diagonals
}

/** Killer Sudoku: no digit repeats inside a cage. Its sum is checked by [ConflictFinder]. */
object CageConstraint : Constraint {
    override fun units(cages: List<Cage>) = cages.map { it.cells }
}

private val classic = listOf(RowConstraint, ColumnConstraint, BoxConstraint)

fun constraintsFor(variant: Variant): List<Constraint> = when (variant) {
    Variant.CLASSIC -> classic
    Variant.X -> classic + DiagonalConstraint
    Variant.KILLER -> classic + CageConstraint
}

/** Every group of a puzzle under [constraints]. */
fun List<Constraint>.units(puzzle: Puzzle): List<List<Int>> = units(puzzle.cages)

fun List<Constraint>.units(cages: List<Cage>): List<List<Int>> = flatMap { it.units(cages) }

/** The cells that share a group with [cell], [cell] itself excluded. */
fun List<List<Int>>.peersOf(cell: Int): Set<Int> = filter { cell in it }.flatten().toSet() - cell
