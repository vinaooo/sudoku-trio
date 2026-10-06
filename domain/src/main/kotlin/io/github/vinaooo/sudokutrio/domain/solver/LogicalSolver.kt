package io.github.vinaooo.sudokutrio.domain.solver

import io.github.vinaooo.sudokutrio.domain.model.Difficulty
import io.github.vinaooo.sudokutrio.domain.model.Puzzle
import io.github.vinaooo.sudokutrio.domain.model.Variant

/** How far the techniques got: the board they reached and the [score], each step's [TechniqueKind.weight] added up. */
data class SolveResult(val values: List<Int>, val solved: Boolean, val score: Int)

/**
 * Solves like a person: after every step it starts again from the cheapest technique, so a puzzle's score counts
 * the techniques it really needs.
 */
class LogicalSolver(private val techniques: List<Pair<TechniqueKind, Technique>> = DEFAULT_TECHNIQUES) {

    /** Solves [puzzle] from its givens with techniques up to [maxDifficulty]. */
    fun solve(puzzle: Puzzle, variant: Variant, maxDifficulty: Difficulty = Difficulty.EXPERT): SolveResult {
        val context = SolverContext(puzzle, variant)
        val allowed = techniques.filter { (kind, _) -> kind.difficulty <= maxDifficulty }
        var candidates = Candidates.of(puzzle.givens, context)
        var score = 0
        while (!candidates.isSolved) {
            val step = allowed.firstNotNullOfOrNull { (_, technique) -> technique.find(candidates, context) } ?: break
            score += step.technique.weight
            candidates = candidates.apply(step, context)
        }
        return SolveResult(candidates.values(), candidates.isSolved, score)
    }

    /**
     * The next digit a person could write on a board with [values] (consistent with the solution). When eliminations
     * have to come first, the placement is credited to the hardest of them, whose cells are shown with the target
     * cell: that is the step the player has to see. Null when the techniques run out.
     */
    fun nextPlacement(puzzle: Puzzle, variant: Variant, values: List<Int>): Deduction.Placement? {
        val context = SolverContext(puzzle, variant)
        var candidates = Candidates.of(values, context)
        var hardest: Deduction.Eliminations? = null
        while (!candidates.isSolved) {
            val step = techniques.firstNotNullOfOrNull { (_, technique) -> technique.find(candidates, context) }
                ?: return null
            when (step) {
                is Deduction.Placement -> return hardest?.let { key ->
                    step.copy(technique = key.technique, cells = (key.cells + step.cell).distinct())
                } ?: step
                is Deduction.Eliminations -> {
                    if (hardest == null || step.technique.weight > hardest.technique.weight) hardest = step
                    candidates = candidates.eliminate(step.eliminations)
                }
            }
        }
        return null
    }

    private fun Candidates.apply(step: Deduction, context: SolverContext): Candidates = when (step) {
        is Deduction.Placement -> place(step.cell, step.digit, context)
        is Deduction.Eliminations -> eliminate(step.eliminations)
    }

    companion object {
        val DEFAULT_TECHNIQUES: List<Pair<TechniqueKind, Technique>> = listOf(
            TechniqueKind.NAKED_SINGLE to NakedSingle,
            TechniqueKind.HIDDEN_SINGLE to HiddenSingle,
            TechniqueKind.CAGE_COMBINATION to CageCombination,
            TechniqueKind.RULE_OF_45 to RuleOf45,
            TechniqueKind.LOCKED_CANDIDATES to LockedCandidates,
            TechniqueKind.NAKED_PAIR to NakedSubset(2, TechniqueKind.NAKED_PAIR),
            TechniqueKind.HIDDEN_PAIR to HiddenSubset(2, TechniqueKind.HIDDEN_PAIR),
            TechniqueKind.NAKED_TRIPLE to NakedSubset(3, TechniqueKind.NAKED_TRIPLE),
            TechniqueKind.HIDDEN_TRIPLE to HiddenSubset(3, TechniqueKind.HIDDEN_TRIPLE),
            TechniqueKind.X_WING to Fish(2, TechniqueKind.X_WING),
            TechniqueKind.XY_WING to XyWing,
            TechniqueKind.SWORDFISH to Fish(3, TechniqueKind.SWORDFISH),
        )
    }
}
