package io.github.vinaooo.sudokutrio.domain.generator

import io.github.vinaooo.sudokutrio.domain.model.Grid
import io.github.vinaooo.sudokutrio.domain.model.Variant
import io.github.vinaooo.sudokutrio.domain.rules.constraintsFor
import io.github.vinaooo.sudokutrio.domain.rules.units
import kotlin.random.Random

/** Fills an empty board at random, obeying a variant's rows, columns, boxes and diagonals (cages come later). */
object FullGridBuilder {
    fun build(variant: Variant, random: Random): List<Int> {
        val units = constraintsFor(variant).units(emptyList())
        val unitsOf = Grid.CELLS.map { cell -> units.indices.filter { cell in units[it] } }
        val used = IntArray(units.size)
        val values = IntArray(Grid.SIZE)

        fun fill(cell: Int): Boolean {
            if (cell == Grid.SIZE) return true
            val taken = unitsOf[cell].fold(0) { mask, unit -> mask or used[unit] }
            Grid.DIGITS.shuffled(random).filter { taken and (1 shl it) == 0 }.forEach { digit ->
                values[cell] = digit
                unitsOf[cell].forEach { used[it] = used[it] or (1 shl digit) }
                if (fill(cell + 1)) return true
                unitsOf[cell].forEach { used[it] = used[it] and (1 shl digit).inv() }
            }
            values[cell] = 0
            return false
        }

        check(fill(0)) { "Every variant has a full grid." }
        return values.toList()
    }
}
