package io.github.vinaooo.sudokutrio.domain.model

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** The app's colors when dynamic color is off or not available; blue is Sudoku Trio's brand. */
enum class ThemeColor { GREEN, TEAL, BLUE, INDIGO, PURPLE, PINK, RED, ORANGE }

/** The hand the controls sit under. */
enum class Handedness { LEFT, RIGHT }

/** Where the board sits in the height it has, in portrait. */
enum class BoardAlignment { TOP, BOTTOM }

/** Where phone view's board sits across the room it has on a tablet. */
enum class PhoneViewSide { LEFT, CENTER, RIGHT }

data class Settings(
    /** The mode of the next new game; the game in progress keeps its own. */
    val mode: GameMode = GameMode(Variant.CLASSIC, Difficulty.EASY),
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = true,
    val themeColor: ThemeColor = ThemeColor.BLUE,
    val soundEnabled: Boolean = true,
    val hapticsEnabled: Boolean = true,
    val handedness: Handedness = Handedness.RIGHT,
    val boardAlignment: BoardAlignment = BoardAlignment.TOP,
    /** On a tablet, the board at the size a phone shows it, instead of filling the screen. */
    val phoneView: Boolean = false,
    val phoneViewSide: PhoneViewSide = PhoneViewSide.CENTER,
)
