package io.github.vinaooo.sudokutrio.feature.game.board

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import io.github.vinaooo.sudokutrio.domain.model.Board
import io.github.vinaooo.sudokutrio.domain.model.Grid
import io.github.vinaooo.sudokutrio.domain.model.Puzzle
import io.github.vinaooo.sudokutrio.feature.game.R
import kotlin.math.roundToInt

/**
 * TalkBack's view of the board: one invisible node per cell over the canvas, read row by row, such as "row 3,
 * column 5, 7, given" or "row 1, column 1, empty, notes 1 4, cage of 15". A double tap selects the cell.
 */
@Composable
internal fun CellNodes(
    board: Board,
    puzzle: Puzzle,
    highlights: List<CellHighlight>,
    onSelect: (Int) -> Unit,
    geometry: BoardGeometry,
) {
    val cageOf = remember(puzzle) { puzzle.cages.flatMap { cage -> cage.cells.map { it to cage.sum } }.toMap() }
    val density = LocalDensity.current
    val size = with(density) { geometry.cellSize.toDp() }
    Grid.CELLS.forEach { cell ->
        val description = cellDescription(cell, board, puzzle, highlights[cell], cageOf[cell])
        Box(
            Modifier
                .offset { IntOffset(geometry.cellLeft(cell).roundToInt(), geometry.cellTop(cell).roundToInt()) }
                .size(size)
                .semantics {
                    contentDescription = description
                    selected = highlights[cell] == CellHighlight.SELECTED
                    onClick {
                        onSelect(cell)
                        true
                    }
                },
        )
    }
}

@Composable
private fun cellDescription(cell: Int, board: Board, puzzle: Puzzle, highlight: CellHighlight, cage: Int?): String {
    val name = stringResource(R.string.a11y_cell, Grid.row(cell) + 1, Grid.column(cell) + 1)
    val digit = board.values[cell]
    val notes = board.notes[cell].sorted()
    val parts = buildList {
        add(name)
        when {
            digit != 0 && puzzle.isGiven(cell) -> add(stringResource(R.string.a11y_cell_given, digit))
            digit != 0 -> add(digit.toString())
            notes.isNotEmpty() -> add(stringResource(R.string.a11y_cell_notes, notes.joinToString(" ")))
            else -> add(stringResource(R.string.a11y_cell_empty))
        }
        if (highlight == CellHighlight.CONFLICT) add(stringResource(R.string.a11y_cell_conflict))
        cage?.let { add(stringResource(R.string.a11y_cell_cage, it)) }
    }
    return parts.joinToString(", ")
}
