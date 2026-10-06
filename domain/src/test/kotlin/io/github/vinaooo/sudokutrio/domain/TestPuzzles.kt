package io.github.vinaooo.sudokutrio.domain

import io.github.vinaooo.sudokutrio.domain.model.Cage
import io.github.vinaooo.sudokutrio.domain.model.Difficulty
import io.github.vinaooo.sudokutrio.domain.model.GameMode
import io.github.vinaooo.sudokutrio.domain.model.GameState
import io.github.vinaooo.sudokutrio.domain.model.Grid
import io.github.vinaooo.sudokutrio.domain.model.Puzzle
import io.github.vinaooo.sudokutrio.domain.model.Variant
import io.github.vinaooo.sudokutrio.domain.rules.GameEngine

/** A valid Sudoku X solution, so it also works for Classic and Killer. */
val SOLUTION: List<Int> = (
    "249368715" +
        "356971824" +
        "781542639" +
        "512783496" +
        "467195283" +
        "893426571" +
        "928614357" +
        "675839142" +
        "134257968"
    ).map { it.digitToInt() }

/** Killer cages: each row split into horizontal triples, summed from [SOLUTION]. */
val CAGES: List<Cage> = Grid.CELLS.chunked(Grid.BOX).map { cells -> Cage(cells.sumOf { SOLUTION[it] }, cells) }

/** The solution with [empty] cells blanked out. */
/** By default every third cell is empty: 27 cells, one solution. */
fun puzzle(empty: Collection<Int> = Grid.CELLS.filter { it % 3 == 0 }, cages: List<Cage> = emptyList()) =
    Puzzle(givens = SOLUTION.mapIndexed { cell, digit -> if (cell in empty) 0 else digit }, solution = SOLUTION, cages)

fun mode(variant: Variant = Variant.CLASSIC, difficulty: Difficulty = Difficulty.EASY) = GameMode(variant, difficulty)

fun newState(
    variant: Variant = Variant.CLASSIC,
    empty: Collection<Int> = Grid.CELLS.filter { it % 3 == 0 },
    engine: GameEngine = GameEngine(),
): GameState = engine.newGame(puzzle(empty, if (variant == Variant.KILLER) CAGES else emptyList()), mode(variant))

/** A wrong digit for [cell]: the next one after the solution's. */
fun wrongDigit(cell: Int): Int = SOLUTION[cell] % Grid.SIDE + 1
