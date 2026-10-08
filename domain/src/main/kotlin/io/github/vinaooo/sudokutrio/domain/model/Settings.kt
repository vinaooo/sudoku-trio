package io.github.vinaooo.sudokutrio.domain.model

import io.github.vinaooo.vinkit.core.BoardAlignment
import io.github.vinaooo.vinkit.core.Handedness
import io.github.vinaooo.vinkit.core.PhoneViewSide
import io.github.vinaooo.vinkit.core.ThemeColor
import io.github.vinaooo.vinkit.core.ThemeMode

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
