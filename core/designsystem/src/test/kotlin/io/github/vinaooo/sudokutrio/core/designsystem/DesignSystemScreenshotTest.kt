package io.github.vinaooo.sudokutrio.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.vinaooo.sudokutrio.core.designsystem.theme.SudokuTrioTheme
import io.github.vinaooo.sudokutrio.core.designsystem.theme.SudokuTrioThemeExtras
import io.github.vinaooo.vinkit.core.ThemeMode
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

/** Every board color in use: each cell background with a given digit, an entered one and a note. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class DesignSystemScreenshotTest {
    @get:Rule
    val compose = createComposeRule()

    @Composable
    private fun Swatch(background: Color, ink: Color, text: String, border: Color) {
        Box(
            Modifier.size(40.dp).background(background).border(1.dp, border),
            contentAlignment = Alignment.Center,
        ) { Text(text, color = ink, fontSize = if (text.length > 1) 10.sp else 22.sp) }
    }

    private fun capture(name: String, themeMode: ThemeMode, dynamicColor: Boolean) {
        compose.setContent {
            SudokuTrioTheme(themeMode = themeMode, dynamicColor = dynamicColor) {
                val colors = SudokuTrioThemeExtras.boardColors
                Column(
                    Modifier.background(MaterialTheme.colorScheme.surface).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    listOf(colors.given to "5", colors.entered to "7", colors.note to "1 4").forEach { (ink, text) ->
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            colors.cellBackgrounds.forEach { background ->
                                val border = if (background ==
                                    colors.selectedCell
                                ) {
                                    colors.selectionBorder
                                } else {
                                    colors.cellLine
                                }
                                Swatch(background, ink, text, border)
                            }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Swatch(colors.conflictCell, colors.conflict, "3", colors.boxLine)
                        Swatch(colors.cell, colors.cageSum, "17", colors.cageLine)
                    }
                }
            }
        }
        compose.onRoot().captureRoboImage("src/test/screenshots/$name.png")
    }

    @Test
    fun board_light_brand() = capture("board_light_brand", ThemeMode.LIGHT, dynamicColor = false)

    @Test
    fun board_dark_brand() = capture("board_dark_brand", ThemeMode.DARK, dynamicColor = false)

    @Test
    fun board_light_dynamic() = capture("board_light_dynamic", ThemeMode.LIGHT, dynamicColor = true)

    @Test
    fun board_dark_dynamic() = capture("board_dark_dynamic", ThemeMode.DARK, dynamicColor = true)
}
