package io.github.vinaooo.sudokutrio.core.designsystem.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import io.github.vinaooo.vinkit.core.ThemeColor
import io.github.vinaooo.vinkit.core.ThemeMode
import io.github.vinaooo.vinkit.designsystem.VinkitTheme

/** vinkit's theme (blue is Sudoku Trio's brand), with the board's colors ([SudokuTrioThemeExtras]) drawn from it. */
@Composable
fun SudokuTrioTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = true,
    themeColor: ThemeColor = ThemeColor.BLUE,
    content: @Composable () -> Unit,
) {
    VinkitTheme(themeColor, themeMode, dynamicColor) {
        val colorScheme = MaterialTheme.colorScheme
        val boardColors = remember(colorScheme) { boardColorsFor(colorScheme) }
        CompositionLocalProvider(LocalBoardColors provides boardColors, content = content)
    }
}

object SudokuTrioThemeExtras {
    val boardColors: BoardColors
        @Composable
        @ReadOnlyComposable
        get() = LocalBoardColors.current
}
