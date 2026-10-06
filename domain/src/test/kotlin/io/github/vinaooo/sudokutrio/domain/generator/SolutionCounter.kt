package io.github.vinaooo.sudokutrio.domain.generator

import io.github.vinaooo.sudokutrio.domain.model.Grid
import io.github.vinaooo.sudokutrio.domain.model.Puzzle
import io.github.vinaooo.sudokutrio.domain.model.Variant
import io.github.vinaooo.sudokutrio.domain.rules.constraintsFor
import io.github.vinaooo.sudokutrio.domain.rules.units

/**
 * Counts a puzzle's solutions by brute force, up to a limit: an independent check that generated puzzles have
 * exactly one. Cages are pruned by their sums.
 */
class SolutionCounter private constructor(puzzle: Puzzle, variant: Variant, private val limit: Int) {
    private val cages = if (variant == Variant.KILLER) puzzle.cages else emptyList()
    private val units = constraintsFor(variant).units(cages)
    private val unitsOf = Grid.CELLS.map { cell -> units.indices.filter { cell in units[it] } }
    private val cageOf = Grid.CELLS.map { cell -> cages.indexOfFirst { cell in it.cells } }
    private val used = IntArray(units.size)
    private val cageTotals = IntArray(cages.size)
    private val cageFilled = IntArray(cages.size)
    private val values = puzzle.givens.toIntArray()
    private var found = 0

    init {
        Grid.CELLS.filter { values[it] != 0 }.forEach { set(it, values[it], 1) }
    }

    private fun set(cell: Int, digit: Int, sign: Int) {
        unitsOf[cell].forEach { used[it] = used[it] xor (1 shl digit) }
        val cage = cageOf[cell]
        if (cage >= 0) {
            cageTotals[cage] += sign * digit
            cageFilled[cage] += sign
        }
    }

    private fun fits(cell: Int, digit: Int): Boolean {
        if (unitsOf[cell].any { used[it] and (1 shl digit) != 0 }) return false
        val index = cageOf[cell]
        if (index < 0) return true
        val total = cageTotals[index] + digit
        return if (cageFilled[index] + 1 ==
            cages[index].cells.size
        ) {
            total == cages[index].sum
        } else {
            total < cages[index].sum
        }
    }

    private fun search() {
        if (found >= limit) return
        val cell = Grid.CELLS.filter { values[it] == 0 }.minByOrNull { c -> Grid.DIGITS.count { fits(c, it) } }
        if (cell == null) {
            found++
            return
        }
        Grid.DIGITS.filter { fits(cell, it) }.forEach { digit ->
            values[cell] = digit
            set(cell, digit, 1)
            search()
            set(cell, digit, -1)
            values[cell] = 0
        }
    }

    companion object {
        fun count(puzzle: Puzzle, variant: Variant, limit: Int = 2): Int =
            SolutionCounter(puzzle, variant, limit).apply { search() }.found
    }
}
