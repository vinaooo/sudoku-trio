package io.github.vinaooo.sudokutrio.feature.game.board

import io.kotest.matchers.floats.plusOrMinus
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class BoardGeometryTest {
    @Test
    fun `the board is the largest square that fits, centered across and at the top`() {
        val tall = BoardGeometry(width = 900f, height = 1200f)
        tall.side shouldBe 900f
        tall.left shouldBe 0f
        tall.top shouldBe 0f
        val wide = BoardGeometry(width = 1200f, height = 900f)
        wide.side shouldBe 900f
        wide.left shouldBe 150f
        wide.cellSize shouldBe (100f plusOrMinus 0.001f)
    }

    @Test
    fun `a touch maps to the cell under it, row by row`() {
        val geometry = BoardGeometry(width = 900f, height = 900f)
        geometry.cellAt(5f, 5f) shouldBe 0
        geometry.cellAt(895f, 5f) shouldBe 8
        geometry.cellAt(150f, 250f) shouldBe 19
        geometry.cellAt(899.9f, 899.9f) shouldBe 80
    }

    @Test
    fun `a touch off the board hits no cell`() {
        val geometry = BoardGeometry(width = 1200f, height = 900f)
        geometry.cellAt(100f, 100f).shouldBeNull()
        geometry.cellAt(1100f, 100f).shouldBeNull()
        geometry.cellAt(600f, 900f).shouldBeNull()
        geometry.cellAt(600f, -1f).shouldBeNull()
    }

    @Test
    fun `a cell's top-left corner`() {
        val geometry = BoardGeometry(width = 1200f, height = 900f)
        geometry.cellLeft(19) shouldBe 150f + 100f
        geometry.cellTop(19) shouldBe 200f
    }
}
