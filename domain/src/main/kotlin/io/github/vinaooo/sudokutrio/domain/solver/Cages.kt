package io.github.vinaooo.sudokutrio.domain.solver

import io.github.vinaooo.sudokutrio.domain.model.Cage

/**
 * The digits each cell of [cage] can still hold in some filling that uses distinct digits adding up to its sum,
 * as masks in the cage's cell order.
 */
internal fun cageOptions(cage: Cage, candidates: Candidates): IntArray {
    val cells = cage.cells
    val options = IntArray(cells.size)
    val chosen = IntArray(cells.size)
    fun search(index: Int, used: Int, total: Int) {
        if (total > cage.sum) return
        if (index == cells.size) {
            if (total == cage.sum) chosen.forEachIndexed { i, digit -> options[i] = options[i] or bit(digit) }
            return
        }
        candidates.mask(cells[index]).digits().filter { used and bit(it) == 0 }.forEach { digit ->
            chosen[index] = digit
            search(index + 1, used or bit(digit), total + digit)
        }
    }
    search(0, 0, 0)
    return options
}

/** Rules out the digits no filling of a cage allows. */
internal fun cageStep(cages: List<Cage>, candidates: Candidates, technique: TechniqueKind): Deduction? {
    cages.forEach { cage ->
        val options = cageOptions(cage, candidates)
        val eliminations = cage.cells.flatMapIndexed { i, cell ->
            (candidates.mask(cell) and options[i].inv()).digits().map { Elimination(cell, it) }
        }.effective(candidates)
        if (eliminations != null) return Deduction.Eliminations(eliminations, technique, cage.cells)
    }
    return null
}

/** Killer: the digits that add up to each cage's sum. */
object CageCombination : Technique {
    override fun find(candidates: Candidates, context: SolverContext) =
        cageStep(context.cages, candidates, TechniqueKind.CAGE_COMBINATION)
}

/** Killer's rule of 45: a house's cells left over by its inner cages add up to 45 minus those cages' sums. */
object RuleOf45 : Technique {
    override fun find(candidates: Candidates, context: SolverContext) =
        cageStep(context.innies, candidates, TechniqueKind.RULE_OF_45)
}
