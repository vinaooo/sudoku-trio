package io.github.vinaooo.sudokutrio.domain.solver

import io.github.vinaooo.sudokutrio.domain.model.Grid

/**
 * Pointing and claiming: when a digit's spots in a house all lie in another group too, that group's other cells
 * can't hold it. The house must hold every digit, so it is a 9-cell house; the other group may be any group.
 */
object LockedCandidates : Technique {
    override fun find(candidates: Candidates, context: SolverContext): Deduction? =
        context.houses.firstNotNullOfOrNull { house ->
            Grid.DIGITS.firstNotNullOfOrNull { digit -> find(house, digit, candidates, context) }
        }

    private fun find(house: List<Int>, digit: Int, candidates: Candidates, context: SolverContext): Deduction? {
        val spots = house.filter { candidates.isEmpty(it) && candidates.has(it, digit) }
        if (spots.size < 2 || house.any { candidates.value(it) == digit }) return null
        return context.units.filter { unit -> unit != house && unit.containsAll(spots) }.firstNotNullOfOrNull { unit ->
            (unit - house.toSet()).map { Elimination(it, digit) }.effective(candidates)
                ?.let { Deduction.Eliminations(it, TechniqueKind.LOCKED_CANDIDATES, spots) }
        }
    }
}
