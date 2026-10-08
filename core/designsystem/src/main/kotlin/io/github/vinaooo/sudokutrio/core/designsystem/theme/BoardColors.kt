package io.github.vinaooo.sudokutrio.core.designsystem.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import io.github.vinaooo.vinkit.core.ThemeColor
import io.github.vinaooo.vinkit.designsystem.paletteScheme

/**
 * The board's colors, all from the theme's roles so every palette and dynamic color work.
 * - Cells: [cell], and the highlights of the selected cell, the cells it sees, the cells with its digit and a hint.
 *   The selected cell is also outlined in [selectionBorder]: its fill is close to the same-digit one in some schemes.
 * - Digits: [given] and the player's [entered] digits, [conflict] for a repeated one, [note] for pencil marks.
 * - Lines: thin [cellLine] and thick [boxLine]; Killer's dashed [cageLine] with its [cageSum].
 */
@Immutable
data class BoardColors(
    val cell: Color,
    val selectedCell: Color,
    val peerCell: Color,
    val sameDigitCell: Color,
    val hintCell: Color,
    val conflictCell: Color,
    val given: Color,
    val entered: Color,
    val conflict: Color,
    val note: Color,
    val cellLine: Color,
    val boxLine: Color,
    val cageLine: Color,
    val cageSum: Color,
    val selectionBorder: Color,
) {
    /** Every cell background a digit or note can sit on. */
    val cellBackgrounds: List<Color> get() = listOf(cell, selectedCell, peerCell, sameDigitCell, hintCell, conflictCell)
}

internal fun boardColorsFor(scheme: ColorScheme) = BoardColors(
    cell = scheme.surfaceContainerLowest,
    selectedCell = scheme.primaryContainer,
    peerCell = scheme.surfaceContainerHigh,
    sameDigitCell = scheme.secondaryContainer,
    hintCell = scheme.tertiaryContainer,
    conflictCell = scheme.errorContainer,
    given = scheme.onSurface,
    entered = scheme.primary,
    conflict = scheme.error,
    note = scheme.onSurfaceVariant,
    cellLine = scheme.outlineVariant,
    boxLine = scheme.onSurfaceVariant,
    cageLine = scheme.tertiary,
    cageSum = scheme.onSurfaceVariant,
    selectionBorder = scheme.primary,
)

val LocalBoardColors = staticCompositionLocalOf { boardColorsFor(paletteScheme(ThemeColor.BLUE, dark = false)) }
