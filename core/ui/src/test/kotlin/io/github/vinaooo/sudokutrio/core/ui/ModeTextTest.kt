package io.github.vinaooo.sudokutrio.core.ui

import androidx.compose.ui.test.junit4.v2.createComposeRule
import io.github.vinaooo.sudokutrio.domain.model.Difficulty
import io.github.vinaooo.sudokutrio.domain.model.GameMode
import io.github.vinaooo.sudokutrio.domain.model.Variant
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class ModeTextTest {
    @get:Rule
    val compose = createComposeRule()

    private fun name(mode: GameMode): String {
        var text = ""
        compose.setContent { text = modeName(mode) }
        compose.waitForIdle()
        return text
    }

    @Test
    fun `a mode reads as its variant and difficulty`() {
        name(GameMode(Variant.KILLER, Difficulty.HARD)) shouldBe "Killer · Hard"
    }

    @Test
    @Config(qualifiers = "pt-rBR")
    fun `in Brazilian Portuguese`() {
        name(GameMode(Variant.CLASSIC, Difficulty.EXPERT)) shouldBe "Clássico · Especialista"
    }
}
