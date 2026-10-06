package io.github.vinaooo.sudokutrio.domain.solver

import io.github.vinaooo.sudokutrio.domain.model.Grid

/** Every way to pick [size] items of this list, in order. */
internal fun <T> List<T>.combinations(size: Int): List<List<T>> = when {
    size == 0 -> listOf(emptyList())
    this.size < size -> emptyList()
    else -> drop(1).combinations(size - 1).map { listOf(first()) + it } + drop(1).combinations(size)
}

/**
 * Naked pair/triple: [size] empty cells of a group whose candidates are [size] digits in all. Those digits go
 * there, so the group's other cells can't hold them. Any group of distinct digits works, cages included.
 */
class NakedSubset(private val size: Int, private val technique: TechniqueKind) : Technique {
    override fun find(candidates: Candidates, context: SolverContext): Deduction? =
        context.units.firstNotNullOfOrNull { unit ->
            val open = unit.filter { candidates.isEmpty(it) }
            open.combinations(size).firstNotNullOfOrNull { cells -> find(open, cells, candidates) }
        }

    private fun find(open: List<Int>, cells: List<Int>, candidates: Candidates): Deduction? {
        val union = cells.fold(0) { mask, cell -> mask or candidates.mask(cell) }
        if (Integer.bitCount(union) != size) return null
        return (open - cells.toSet()).flatMap { cell -> union.digits().map { Elimination(cell, it) } }
            .effective(candidates)?.let { Deduction.Eliminations(it, technique, cells) }
    }
}

/**
 * Hidden pair/triple: [size] digits of a house that fit only [size] cells in all. Those cells hold them, so they
 * can't hold anything else. Only for 9-cell houses, which must contain every digit.
 */
class HiddenSubset(private val size: Int, private val technique: TechniqueKind) : Technique {
    override fun find(candidates: Candidates, context: SolverContext): Deduction? =
        context.houses.firstNotNullOfOrNull { house ->
            val missing = Grid.DIGITS.filter { digit -> house.none { candidates.value(it) == digit } }
            missing.combinations(size).firstNotNullOfOrNull { digits -> find(house, digits, candidates) }
        }

    private fun find(house: List<Int>, digits: List<Int>, candidates: Candidates): Deduction? {
        val cells = house.filter { cell -> candidates.isEmpty(cell) && digits.any { candidates.has(cell, it) } }
        if (cells.size != size) return null
        val keep = digits.fold(0) { mask, digit -> mask or bit(digit) }
        return cells.flatMap { cell -> (candidates.mask(cell) and keep.inv()).digits().map { Elimination(cell, it) } }
            .effective(candidates)?.let { Deduction.Eliminations(it, technique, cells) }
    }
}
