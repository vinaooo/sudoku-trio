package io.github.vinaooo.sudokutrio.domain.solver

import io.github.vinaooo.sudokutrio.domain.model.Grid

/** An empty cell with one candidate left. */
object NakedSingle : Technique {
    override fun find(candidates: Candidates, context: SolverContext): Deduction? = Grid.CELLS
        .firstOrNull { candidates.isEmpty(it) && Integer.bitCount(candidates.mask(it)) == 1 }
        ?.let { cell ->
            Deduction.Placement(cell, candidates.mask(cell).digits().single(), TechniqueKind.NAKED_SINGLE, listOf(cell))
        }
}

/** A digit that fits only one cell of a house. */
object HiddenSingle : Technique {
    override fun find(candidates: Candidates, context: SolverContext): Deduction? {
        context.houses.forEach { house ->
            Grid.DIGITS.forEach { digit ->
                val spots = house.filter { candidates.has(it, digit) }
                val cell = spots.singleOrNull()
                if (cell != null && candidates.isEmpty(cell)) {
                    return Deduction.Placement(cell, digit, TechniqueKind.HIDDEN_SINGLE, house)
                }
            }
        }
        return null
    }
}
