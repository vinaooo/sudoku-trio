package io.github.vinaooo.sudokutrio.domain.solver

import io.github.vinaooo.sudokutrio.domain.model.Cage
import io.github.vinaooo.sudokutrio.domain.model.Grid
import io.github.vinaooo.sudokutrio.domain.model.Puzzle
import io.github.vinaooo.sudokutrio.domain.model.Variant
import io.github.vinaooo.sudokutrio.domain.rules.CageConstraint
import io.github.vinaooo.sudokutrio.domain.rules.constraintsFor
import io.github.vinaooo.sudokutrio.domain.rules.units

/**
 * The groups a puzzle's techniques reason about, worked out once per puzzle.
 * - [units]: every group whose digits never repeat, cages included.
 * - [houses]: the 9-cell groups, which hold every digit once (rows, columns, boxes, diagonals in X). Hidden
 *   singles, hidden subsets and the rule of 45 only hold for these: a cage needn't contain every digit.
 * - [lines]: rows then columns, for fish.
 * - [innies]: Killer's rule of 45, the cells of a house left over by the cages inside it, with their known sum.
 */
class SolverContext(puzzle: Puzzle, variant: Variant) {
    val units: List<List<Int>> = constraintsFor(variant).units(puzzle)
    val houses: List<List<Int>> = constraintsFor(variant).filterNot { it == CageConstraint }.units(puzzle)
    val rows: List<List<Int>> = houses.take(Grid.SIDE)
    val columns: List<List<Int>> = houses.subList(Grid.SIDE, 2 * Grid.SIDE)
    val cages: List<Cage> = if (variant == Variant.KILLER) puzzle.cages else emptyList()
    val innies: List<Cage> = cages.takeIf { it.isNotEmpty() }?.let { innies(houses, it) }.orEmpty()
    val peers: List<IntArray> = Grid.CELLS.map { cell ->
        units.filter { cell in it }.flatten().toSortedSet().apply { remove(cell) }.toIntArray()
    }

    fun sees(a: Int, b: Int): Boolean = a != b && peers[a].binarySearch(b) >= 0

    private companion object {
        const val HOUSE_SUM = 45
        const val MAX_INNIES = 4

        /** For each house, the cells whose cage sticks out of it, when the cages wholly inside leave few of them. */
        fun innies(houses: List<List<Int>>, cages: List<Cage>): List<Cage> = houses.mapNotNull { house ->
            val inside = cages.filter { cage -> cage.cells.all { it in house } }
            val rest = house - inside.flatMap { it.cells }.toSet()
            rest.takeIf { it.size in 1..MAX_INNIES }?.let { Cage(HOUSE_SUM - inside.sumOf { it.sum }, it) }
        }
    }
}
