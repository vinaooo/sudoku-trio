package io.github.vinaooo.sudokutrio.domain.scoring

import io.github.vinaooo.sudokutrio.domain.model.Difficulty
import io.github.vinaooo.sudokutrio.domain.model.GameMode
import io.github.vinaooo.sudokutrio.domain.model.Variant
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class PointsScoringTest {

    @ParameterizedTest
    @CsvSource(
        "CLASSIC, EASY, 1000", "CLASSIC, MEDIUM, 2000", "CLASSIC, HARD, 4000", "CLASSIC, EXPERT, 8000",
        "X, EASY, 1200", "X, MEDIUM, 2400", "X, HARD, 4800", "X, EXPERT, 9600",
        "KILLER, EASY, 1500", "KILLER, MEDIUM, 3000", "KILLER, HARD, 6000", "KILLER, EXPERT, 12000",
    )
    fun `a game starts at the base for its difficulty times its variant's factor`(
        variant: Variant,
        difficulty: Difficulty,
        expected: Int,
    ) {
        PointsScoring.startingScore(GameMode(variant, difficulty)) shouldBe expected
    }

    @Test
    fun `seconds, mistakes and hints cost points`() {
        PointsScoring.pointsFor(ScoreEvent.TimeElapsed(7)) shouldBe -7
        PointsScoring.pointsFor(ScoreEvent.TimeElapsed(Long.MAX_VALUE)) shouldBe -Int.MAX_VALUE
        PointsScoring.pointsFor(ScoreEvent.Mistake) shouldBe -100
        PointsScoring.pointsFor(ScoreEvent.HintUsed) shouldBe -200
    }

    @Test
    fun `the score is clamped at zero and the highest ranks first`() {
        PointsScoring.bounded(-5) shouldBe 0
        PointsScoring.bounded(5) shouldBe 5
        PointsScoring.rankingOrder shouldBe RankingOrder.HIGHEST_SCORE
        scoringFor(GameMode(Variant.X, Difficulty.HARD)) shouldBe PointsScoring
    }
}
