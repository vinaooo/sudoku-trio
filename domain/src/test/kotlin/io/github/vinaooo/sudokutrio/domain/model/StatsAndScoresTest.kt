package io.github.vinaooo.sudokutrio.domain.model

import io.github.vinaooo.sudokutrio.domain.mode
import io.github.vinaooo.vinkit.core.Ranking
import io.github.vinaooo.vinkit.core.ScoreRecord
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class StatsAndScoresTest {

    @Test
    fun `every mode has its own key, and the key gives the mode back`() {
        val modes = Variant.entries.flatMap { v -> Difficulty.entries.map { GameMode(v, it) } }
        modes.map { it.key }.toSet().size shouldBe modes.size
        modes.forEach { gameModeOf(it.key) shouldBe it }
        GameMode(Variant.KILLER, Difficulty.HARD).key shouldBe "KILLER_HARD"
    }

    @Test
    fun `a key this version doesn't know has no mode`() {
        gameModeOf("SAMURAI_HARD").shouldBeNull()
        gameModeOf("CLASSIC_INSANE").shouldBeNull()
        gameModeOf("CLASSIC").shouldBeNull()
    }

    @Test
    fun `every mode ranks by the highest score`() {
        mode().ranking() shouldBe Ranking.HIGHEST_POINTS
    }

    @Test
    fun `a record without its extras reads as no mistakes and no hints`() {
        val record = ScoreRecord(mode().key, 500, 60, 0)
        record.mistakes shouldBe 0
        record.hintsUsed shouldBe 0
    }

    @Test
    fun `settings default to classic easy`() {
        Settings().mode shouldBe GameMode(Variant.CLASSIC, Difficulty.EASY)
    }
}
