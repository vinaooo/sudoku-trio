package io.github.vinaooo.sudokutrio.domain.solver

import io.github.vinaooo.sudokutrio.domain.model.Grid

/**
 * X-wing (size 2) and swordfish (size 3): a digit whose spots in [size] rows fall in [size] columns in all must
 * take those columns there, so the columns' other cells can't hold it; and the same with rows and columns swapped.
 */
class Fish(private val size: Int, private val technique: TechniqueKind) : Technique {
    override fun find(candidates: Candidates, context: SolverContext): Deduction? =
        Grid.DIGITS.firstNotNullOfOrNull { digit ->
            find(digit, context.rows, context.columns, candidates)
                ?: find(digit, context.columns, context.rows, candidates)
        }

    private fun find(digit: Int, bases: List<List<Int>>, covers: List<List<Int>>, candidates: Candidates): Deduction? {
        val spots = bases.map { base ->
            base.indices.filter {
                candidates.isEmpty(base[it]) &&
                    candidates.has(base[it], digit)
            }
        }
        val eligible = bases.indices.filter { spots[it].size in 2..size }
        eligible.combinations(size).forEach { chosen ->
            val coverIndexes = chosen.flatMap { spots[it] }.toSet()
            if (coverIndexes.size == size) {
                val baseCells = chosen.flatMap { bases[it] }.toSet()
                val eliminations = coverIndexes.flatMap { covers[it] }.filter { it !in baseCells }
                    .map { Elimination(it, digit) }.effective(candidates)
                if (eliminations != null) {
                    val cells = chosen.flatMap { index -> spots[index].map { bases[index][it] } }
                    return Deduction.Eliminations(eliminations, technique, cells)
                }
            }
        }
        return null
    }
}

/**
 * XY-wing: a pivot with candidates {x, y} sees one pincer with {x, z} and another with {y, z}. Whichever digit the
 * pivot takes, one pincer is z, so a cell that sees both pincers can't be z.
 */
object XyWing : Technique {
    private const val PAIR = 2

    override fun find(candidates: Candidates, context: SolverContext): Deduction? {
        val pairs = Grid.CELLS.filter { candidates.isEmpty(it) && Integer.bitCount(candidates.mask(it)) == PAIR }
        return pairs.firstNotNullOfOrNull { pivot ->
            val pivotMask = candidates.mask(pivot)
            pairs.filter { context.sees(pivot, it) && Integer.bitCount(candidates.mask(it) and pivotMask) == 1 }
                .combinations(PAIR)
                .firstNotNullOfOrNull { (a, b) -> find(pivot, a, b, candidates, context) }
        }
    }

    private fun find(pivot: Int, a: Int, b: Int, candidates: Candidates, context: SolverContext): Deduction? {
        val pivotMask = candidates.mask(pivot)
        val maskA = candidates.mask(a)
        val maskB = candidates.mask(b)
        val z = maskA and maskB and pivotMask.inv()
        if (Integer.bitCount(z) != 1 || (maskA or maskB) and pivotMask != pivotMask || maskA == maskB) return null
        val digit = z.digits().single()
        return Grid.CELLS.filter { context.sees(it, a) && context.sees(it, b) }
            .map { Elimination(it, digit) }.effective(candidates)
            ?.let { Deduction.Eliminations(it, TechniqueKind.XY_WING, listOf(pivot, a, b)) }
    }
}
