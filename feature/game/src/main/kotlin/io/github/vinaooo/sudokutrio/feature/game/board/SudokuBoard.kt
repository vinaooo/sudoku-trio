package io.github.vinaooo.sudokutrio.feature.game.board

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.vinaooo.sudokutrio.core.designsystem.theme.BoardColors
import io.github.vinaooo.sudokutrio.core.designsystem.theme.SudokuTrioThemeExtras
import io.github.vinaooo.sudokutrio.domain.model.Board
import io.github.vinaooo.sudokutrio.domain.model.Cage
import io.github.vinaooo.sudokutrio.domain.model.Grid
import io.github.vinaooo.sudokutrio.domain.model.Puzzle

/**
 * The 9×9 board, drawn on one canvas: highlighted cells, given and entered digits, pencil marks, the grid and, in
 * Killer, dashed cages with their sums; in Sudoku X, faint [diagonals]. It takes the [board] and [puzzle], never the
 * whole game state, so a clock tick doesn't redraw it. Tapping a cell selects it ([onSelect]).
 */
@Composable
fun SudokuBoard(
    board: Board,
    puzzle: Puzzle,
    highlights: List<CellHighlight>,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    diagonals: Boolean = false,
) {
    val colors = SudokuTrioThemeExtras.boardColors
    val text = rememberTextMeasurer()
    val anchors = remember(puzzle) { puzzle.cages.associateBy { it.cells.min() } }
    BoxWithConstraints(modifier) {
        BoardCanvas(board, puzzle, highlights, onSelect, diagonals, colors, text, anchors)
        CellNodes(
            board,
            puzzle,
            highlights,
            onSelect,
            BoardGeometry(constraints.maxWidth.toFloat(), constraints.maxHeight.toFloat()),
        )
    }
}

@Composable
@Suppress("LongParameterList") // The canvas draws everything the board shows.
private fun BoardCanvas(
    board: Board,
    puzzle: Puzzle,
    highlights: List<CellHighlight>,
    onSelect: (Int) -> Unit,
    diagonals: Boolean,
    colors: BoardColors,
    text: TextMeasurer,
    anchors: Map<Int, Cage>,
) {
    Canvas(
        Modifier.fillMaxSize().pointerInput(Unit) {
            detectTapGestures { offset ->
                BoardGeometry(size.width.toFloat(), size.height.toFloat()).cellAt(offset.x, offset.y)?.let(onSelect)
            }
        },
    ) {
        val geometry = BoardGeometry(size.width, size.height)
        Grid.CELLS.forEach { cell -> drawCell(cell, geometry, colors, highlights[cell]) }
        if (diagonals) drawDiagonals(geometry, colors)
        puzzle.cages.forEach { drawCage(it, geometry, colors) }
        Grid.CELLS.forEach { cell ->
            val sum = anchors[cell]?.sum
            sum?.let { drawCageSum(it, cell, geometry, colors, text) }
            val digit = board.values[cell]
            if (digit != 0) {
                val ink = when {
                    puzzle.isGiven(cell) -> colors.given
                    highlights[cell] == CellHighlight.CONFLICT -> colors.conflict
                    else -> colors.entered
                }
                drawDigit(digit, cell, Pen(geometry, text, ink), given = puzzle.isGiven(cell))
            } else {
                drawNotes(board.notes[cell], cell, Pen(geometry, text, colors.note), roomForSum = sum != null)
            }
        }
        drawGrid(geometry, colors)
        highlights.indexOf(CellHighlight.SELECTED).takeIf { it >= 0 }?.let { drawSelection(it, geometry, colors) }
    }
}

private fun DrawScope.drawCell(cell: Int, geometry: BoardGeometry, colors: BoardColors, highlight: CellHighlight) {
    val color = when (highlight) {
        CellHighlight.NONE -> colors.cell
        CellHighlight.PEER -> colors.peerCell
        CellHighlight.SAME_DIGIT -> colors.sameDigitCell
        CellHighlight.HINT -> colors.hintCell
        CellHighlight.SELECTED -> colors.selectedCell
        CellHighlight.CONFLICT -> colors.conflictCell
    }
    drawRect(color, Offset(geometry.cellLeft(cell), geometry.cellTop(cell)), Size(geometry.cellSize, geometry.cellSize))
}

/** Sudoku X's two diagonals, under the digits, faint enough not to compete with them. */
private fun DrawScope.drawDiagonals(geometry: BoardGeometry, colors: BoardColors) {
    val color = colors.cageLine.copy(alpha = DIAGONAL_ALPHA)
    val width = DIAGONAL_LINE.toPx()
    val left = geometry.left
    val right = geometry.left + geometry.side
    val bottom = geometry.top + geometry.side
    drawLine(color, Offset(left, geometry.top), Offset(right, bottom), width)
    drawLine(color, Offset(right, geometry.top), Offset(left, bottom), width)
}

private fun DrawScope.drawGrid(geometry: BoardGeometry, colors: BoardColors) {
    val thin = THIN_LINE.toPx()
    val thick = THICK_LINE.toPx()
    for (i in 0..Grid.SIDE) {
        val box = i % Grid.BOX == 0
        val at = i * geometry.cellSize
        val color = if (box) colors.boxLine else colors.cellLine
        val width = if (box) thick else thin
        drawLine(
            color,
            Offset(geometry.left + at, geometry.top),
            Offset(
                geometry.left + at,
                geometry.top + geometry.side,
            ),
            width,
        )
        drawLine(
            color,
            Offset(geometry.left, geometry.top + at),
            Offset(
                geometry.left + geometry.side,
                geometry.top + at,
            ),
            width,
        )
    }
}

private fun DrawScope.drawSelection(cell: Int, geometry: BoardGeometry, colors: BoardColors) {
    val width = SELECTION_LINE.toPx()
    drawRect(
        colors.selectionBorder,
        Offset(geometry.cellLeft(cell) + width / 2, geometry.cellTop(cell) + width / 2),
        Size(geometry.cellSize - width, geometry.cellSize - width),
        style = Stroke(width),
    )
}

/** A dashed outline just inside the cage's cells, drawn only along edges that border another cage. */
private fun DrawScope.drawCage(cage: Cage, geometry: BoardGeometry, colors: BoardColors) {
    val inset = geometry.cellSize * CAGE_INSET
    val dash = PathEffect.dashPathEffect(floatArrayOf(DASH.toPx(), DASH.toPx()))
    val width = CAGE_LINE.toPx()
    cage.cells.forEach { cell ->
        val row = Grid.row(cell)
        val column = Grid.column(cell)
        val left = geometry.cellLeft(cell)
        val top = geometry.cellTop(cell)
        val right = left + geometry.cellSize
        val bottom = top + geometry.cellSize
        fun inCage(r: Int, c: Int) = r in 0 until Grid.SIDE && c in 0 until Grid.SIDE && Grid.cell(r, c) in cage.cells
        fun line(from: Offset, to: Offset) = drawLine(colors.cageLine, from, to, width, pathEffect = dash)
        // Each edge runs to the cell's corner when the cage goes on past it, so neighboring cells' dashes meet.
        val x0 = if (inCage(row, column - 1)) left else left + inset
        val x1 = if (inCage(row, column + 1)) right else right - inset
        val y0 = if (inCage(row - 1, column)) top else top + inset
        val y1 = if (inCage(row + 1, column)) bottom else bottom - inset
        if (!inCage(row - 1, column)) line(Offset(x0, top + inset), Offset(x1, top + inset))
        if (!inCage(row + 1, column)) line(Offset(x0, bottom - inset), Offset(x1, bottom - inset))
        if (!inCage(row, column - 1)) line(Offset(left + inset, y0), Offset(left + inset, y1))
        if (!inCage(row, column + 1)) line(Offset(right - inset, y0), Offset(right - inset, y1))
    }
}

private fun DrawScope.drawCageSum(
    sum: Int,
    cell: Int,
    geometry: BoardGeometry,
    colors: BoardColors,
    text: TextMeasurer,
) {
    val style = TextStyle(color = colors.cageSum, fontSize = (geometry.cellSize * SUM_SIZE).toSp())
    val inset = geometry.cellSize * CAGE_INSET
    drawText(
        text,
        sum.toString(),
        Offset(geometry.cellLeft(cell) + inset * 2, geometry.cellTop(cell) + inset),
        style = style,
    )
}

/** What text on the board is drawn with: the board's [geometry], the [text] measurer and the [ink]. */
private class Pen(val geometry: BoardGeometry, val text: TextMeasurer, val ink: Color)

private fun DrawScope.drawDigit(digit: Int, cell: Int, pen: Pen, given: Boolean) {
    val (geometry, text) = pen.geometry to pen.text
    val ink = pen.ink
    val style = TextStyle(
        color = ink,
        fontSize = (geometry.cellSize * DIGIT_SIZE).toSp(),
        fontWeight = if (given) FontWeight.Medium else FontWeight.Normal,
    )
    val layout = text.measure(digit.toString(), style)
    val x = geometry.cellLeft(cell) + (geometry.cellSize - layout.size.width) / 2
    val y = geometry.cellTop(cell) + (geometry.cellSize - layout.size.height) / 2
    drawText(layout, topLeft = Offset(x, y))
}

/**
 * Pencil marks in a 3×3 grid, 1 at the top left. In a cell carrying a cage sum, the marks shrink into the space below
 * it, so the sum and the 1 never overlap.
 */
private fun DrawScope.drawNotes(notes: Set<Int>, cell: Int, pen: Pen, roomForSum: Boolean) {
    val (geometry, text) = pen.geometry to pen.text
    val ink = pen.ink
    if (notes.isEmpty()) return
    val top = if (roomForSum) geometry.cellSize * SUM_BAND else 0f
    val slotWidth = geometry.cellSize / Grid.BOX
    val slotHeight = (geometry.cellSize - top) / Grid.BOX
    val style = TextStyle(color = ink, fontSize = (minOf(slotWidth, slotHeight) * NOTE_SIZE).toSp())
    notes.forEach { digit ->
        val layout = text.measure(digit.toString(), style)
        val slot = digit - 1
        val x = geometry.cellLeft(cell) + slot % Grid.BOX * slotWidth + (slotWidth - layout.size.width) / 2
        val y = geometry.cellTop(cell) + top + slot / Grid.BOX * slotHeight + (slotHeight - layout.size.height) / 2
        drawText(layout, topLeft = Offset(x, y))
    }
}

private val THIN_LINE: Dp = 1.dp
private val THICK_LINE: Dp = 2.dp
private val SELECTION_LINE: Dp = 2.dp
private val CAGE_LINE: Dp = 1.dp
private val DASH: Dp = 3.dp
private val DIAGONAL_LINE: Dp = 2.dp
private const val DIAGONAL_ALPHA = 0.45f
private const val CAGE_INSET = 0.07f
private const val DIGIT_SIZE = 0.6f
private const val NOTE_SIZE = 0.8f
private const val SUM_SIZE = 0.22f

/** The top part of a cell its cage sum takes. */
private const val SUM_BAND = 0.3f
