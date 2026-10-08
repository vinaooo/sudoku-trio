package io.github.vinaooo.sudokutrio.core.designsystem.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import io.github.vinaooo.vinkit.core.ThemeColor
import io.github.vinaooo.vinkit.designsystem.paletteScheme
import io.kotest.assertions.withClue
import io.kotest.matchers.doubles.shouldBeGreaterThanOrEqual
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

/** The board stays readable in every palette, light and dark. */
class BoardColorsTest {

    private fun contrast(a: Color, b: Color): Double {
        val (light, dark) = listOf(a.luminance().toDouble(), b.luminance().toDouble()).sortedDescending()
        return (light + OFFSET) / (dark + OFFSET)
    }

    private fun schemes(color: ThemeColor) = listOf(false, true).map { dark ->
        boardColorsFor(paletteScheme(color, dark))
    }

    @ParameterizedTest
    @EnumSource(ThemeColor::class)
    fun `digits and notes reach text contrast on every cell background`(color: ThemeColor) {
        schemes(color).forEach { colors ->
            colors.cellBackgrounds.forEach { background ->
                listOf(colors.given, colors.entered, colors.note).forEach { ink ->
                    withClue("$color $ink on $background") { contrast(ink, background) shouldBeGreaterThanOrEqual TEXT }
                }
            }
            withClue("$color conflict ink") { contrast(colors.conflict, colors.cell) shouldBeGreaterThanOrEqual TEXT }
        }
    }

    @ParameterizedTest
    @EnumSource(ThemeColor::class)
    fun `box lines, cages and the selection outline stand out`(color: ThemeColor) {
        schemes(color).forEach { colors ->
            contrast(colors.boxLine, colors.cell) shouldBeGreaterThanOrEqual GRAPHIC
            contrast(colors.cageLine, colors.cell) shouldBeGreaterThanOrEqual GRAPHIC
            contrast(colors.cageSum, colors.cell) shouldBeGreaterThanOrEqual TEXT
            contrast(colors.selectionBorder, colors.selectedCell) shouldBeGreaterThanOrEqual GRAPHIC
        }
    }

    @ParameterizedTest
    @EnumSource(ThemeColor::class)
    fun `given and entered digits look different`(color: ThemeColor) {
        schemes(color).forEach { colors -> colors.given shouldNotBe colors.entered }
    }

    private companion object {
        const val OFFSET = 0.05
        const val TEXT = 4.5
        const val GRAPHIC = 3.0
    }
}
