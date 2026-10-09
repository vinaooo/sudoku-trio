package io.github.vinaooo.sudokutrio.domain.model

import kotlinx.serialization.Serializable

/** The rules a Sudoku is played with: the board's extra constraints. */
enum class Variant {
    /** Rows, columns and 3×3 boxes. */
    CLASSIC,

    /** Classic plus both main diagonals. */
    X,

    /** Classic plus cages: each cage's digits add up to its sum and never repeat. */
    KILLER,
}

enum class Difficulty { EASY, MEDIUM, HARD, EXPERT }

/** The variant and difficulty a game was dealt with. It travels with the game, whatever Settings say later. */
@Serializable
data class GameMode(val variant: Variant, val difficulty: Difficulty)

/** Every mode, variant by variant from easy to expert. */
val allModes: List<GameMode> = Variant.entries.flatMap { variant -> Difficulty.entries.map { GameMode(variant, it) } }
