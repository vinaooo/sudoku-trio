package io.github.vinaooo.sudokutrio.domain.solver

import io.github.vinaooo.sudokutrio.domain.model.Difficulty

/**
 * The deductions the solver knows, cheapest first. [difficulty] is the easiest level that may use the technique;
 * [weight] is what each use adds to a puzzle's score, which places it within the levels.
 */
enum class TechniqueKind(val difficulty: Difficulty, val weight: Int) {
    NAKED_SINGLE(Difficulty.EASY, 1),
    HIDDEN_SINGLE(Difficulty.EASY, 1),
    CAGE_COMBINATION(Difficulty.EASY, 2),
    RULE_OF_45(Difficulty.MEDIUM, 3),
    LOCKED_CANDIDATES(Difficulty.MEDIUM, 4),
    NAKED_PAIR(Difficulty.MEDIUM, 5),
    HIDDEN_PAIR(Difficulty.MEDIUM, 6),
    NAKED_TRIPLE(Difficulty.HARD, 8),
    HIDDEN_TRIPLE(Difficulty.HARD, 9),
    X_WING(Difficulty.HARD, 10),
    XY_WING(Difficulty.EXPERT, 12),
    SWORDFISH(Difficulty.EXPERT, 14),
}

data class Elimination(val cell: Int, val digit: Int)

/** One logical step: a digit to write, or candidates to rule out. [cells] are the ones to show the player. */
sealed interface Deduction {
    val technique: TechniqueKind
    val cells: List<Int>

    data class Placement(
        val cell: Int,
        val digit: Int,
        override val technique: TechniqueKind,
        override val cells: List<Int>,
    ) : Deduction

    data class Eliminations(
        val eliminations: List<Elimination>,
        override val technique: TechniqueKind,
        override val cells: List<Int>,
    ) : Deduction {
        init {
            require(eliminations.isNotEmpty()) { "A step rules something out." }
        }
    }
}

/**
 * A way to make progress. Every technique is sound on any board consistent with its solution and never assumes the
 * solution is unique, so a full solve by techniques alone also proves the puzzle has one solution.
 */
fun interface Technique {
    /** A step that changes [candidates], or null when this technique finds none. */
    fun find(candidates: Candidates, context: SolverContext): Deduction?
}

/** The eliminations that would change [candidates], or null when none would. */
internal fun List<Elimination>.effective(candidates: Candidates): List<Elimination>? =
    filter { candidates.isEmpty(it.cell) && candidates.has(it.cell, it.digit) }.distinct().ifEmpty { null }
