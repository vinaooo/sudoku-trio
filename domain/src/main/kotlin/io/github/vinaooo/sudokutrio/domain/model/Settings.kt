package io.github.vinaooo.sudokutrio.domain.model

/** Sudoku Trio's own settings; the common ones (theme, feedback, hand, layout) are vinkit's `AppSettings`. */
data class Settings(
    /** The mode of the next new game; the game in progress keeps its own. */
    val mode: GameMode = GameMode(Variant.CLASSIC, Difficulty.EASY),
    /** Games won in a row, in any mode; a counted loss resets it (the stats keep streaks per mode). */
    val winStreak: Int = 0,
)
