package io.github.vinaooo.sudokutrio.domain.solver

import io.github.vinaooo.sudokutrio.domain.model.Grid

/** The bit of [digit] in a candidate mask: bit 1 for digit 1 up to bit 9. */
internal fun bit(digit: Int): Int = 1 shl digit

/** The digits of a candidate mask, ascending. */
internal fun Int.digits(): List<Int> = Grid.DIGITS.filter { this and bit(it) != 0 }

internal const val ALL_DIGITS = 0x3FE

/**
 * The solver's view of a board: the placed digits and, for each empty cell, the digits it may still hold.
 * Immutable: every change returns a new [Candidates].
 */
class Candidates internal constructor(private val values: IntArray, private val masks: IntArray) {

    fun value(cell: Int): Int = values[cell]

    /** The digits an empty cell may hold, as a mask; a placed cell's own digit. */
    fun mask(cell: Int): Int = if (values[cell] != 0) bit(values[cell]) else masks[cell]

    fun has(cell: Int, digit: Int): Boolean = mask(cell) and bit(digit) != 0

    fun isEmpty(cell: Int): Boolean = values[cell] == 0

    val isSolved: Boolean get() = values.none { it == 0 }

    fun values(): List<Int> = values.toList()

    /** Writes [digit] in [cell] and removes it from the candidates of every cell that sees it. */
    fun place(cell: Int, digit: Int, context: SolverContext): Candidates {
        val newValues = values.copyOf().also { it[cell] = digit }
        val newMasks = masks.copyOf().also { it[cell] = 0 }
        context.peers[cell].forEach { newMasks[it] = newMasks[it] and bit(digit).inv() }
        return Candidates(newValues, newMasks)
    }

    fun eliminate(eliminations: List<Elimination>): Candidates {
        val newMasks = masks.copyOf()
        eliminations.forEach { newMasks[it.cell] = newMasks[it.cell] and bit(it.digit).inv() }
        return Candidates(values, newMasks)
    }

    companion object {
        /** The candidates of a board with [values] written (0 for empty): what each empty cell's peers allow. */
        fun of(values: List<Int>, context: SolverContext): Candidates {
            var candidates = Candidates(IntArray(Grid.SIZE), IntArray(Grid.SIZE) { ALL_DIGITS })
            values.forEachIndexed { cell, digit -> if (digit != 0) candidates = candidates.place(cell, digit, context) }
            return candidates
        }

        /** Exact candidates, for tests of single techniques: [masks] for empty cells, [values] for placed ones. */
        internal fun raw(values: List<Int>, masks: List<Int>) = Candidates(values.toIntArray(), masks.toIntArray())
    }
}
