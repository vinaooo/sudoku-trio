package io.github.vinaooo.sudokutrio.domain.model

import io.github.vinaooo.sudokutrio.domain.solver.TechniqueKind
import kotlinx.serialization.Serializable

/** What a hint shows: [cells] to highlight, and the move it suggests. */
@Serializable
sealed interface Hint {
    val cells: List<Int>

    /** A digit on the board that differs from the solution: it has to go first. */
    @Serializable
    data class WrongDigit(val cell: Int) : Hint {
        override val cells: List<Int> get() = listOf(cell)
    }

    /** The next digit to write and the [technique] that finds it; null when the solver can't, so it's revealed. */
    @Serializable
    data class Placement(val cell: Int, val digit: Int, val technique: TechniqueKind?, override val cells: List<Int>) :
        Hint
}
