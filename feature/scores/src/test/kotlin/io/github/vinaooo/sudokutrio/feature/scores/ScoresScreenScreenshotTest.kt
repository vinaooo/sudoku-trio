package io.github.vinaooo.sudokutrio.feature.scores

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.vinaooo.sudokutrio.core.designsystem.theme.SudokuTrioTheme
import io.github.vinaooo.sudokutrio.domain.model.Difficulty
import io.github.vinaooo.sudokutrio.domain.model.GameMode
import io.github.vinaooo.sudokutrio.domain.model.GameStats
import io.github.vinaooo.sudokutrio.domain.model.ScoreRecord
import io.github.vinaooo.sudokutrio.domain.model.ThemeMode
import io.github.vinaooo.sudokutrio.domain.model.Variant
import java.util.TimeZone
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp-port-xxhdpi")
class ScoresScreenScreenshotTest {
    @get:Rule
    val compose = createComposeRule()

    private val modes = listOf(
        GameMode(Variant.CLASSIC, Difficulty.EASY),
        GameMode(Variant.X, Difficulty.MEDIUM),
        GameMode(Variant.KILLER, Difficulty.HARD),
    )

    /** Noon UTC on 1 January 2026 and the days after, so no time zone moves a date. */
    private fun record(points: Int, seconds: Long, mistakes: Int, hints: Int, day: Int) =
        ScoreRecord(modes[0], points, seconds, mistakes, hints, NOON + day * DAY)

    private fun capture(name: String, themeMode: ThemeMode) {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
        compose.setContent {
            SudokuTrioTheme(themeMode = themeMode, dynamicColor = false) {
                ScoresScreen(
                    ScoresUiState(
                        scores = listOf(
                            record(912, 88, 0, 0, 3),
                            record(845, 155, 0, 0, 1),
                            record(640, 160, 2, 0, 0),
                            record(395, 205, 2, 2, 5),
                        ),
                        stats = GameStats(played = 7, won = 4, currentStreak = 2, bestStreak = 3),
                        isLoading = false,
                        modes = modes,
                        mode = modes[0],
                    ),
                    onBack = {},
                )
            }
        }
        compose.onRoot().captureRoboImage("src/test/screenshots/$name.png")
    }

    @Test
    fun scores_light() = capture("scores_light", ThemeMode.LIGHT)

    @Test
    fun scores_dark() = capture("scores_dark", ThemeMode.DARK)

    private companion object {
        const val NOON = 1_767_268_800_000L
        const val DAY = 86_400_000L
    }
}
