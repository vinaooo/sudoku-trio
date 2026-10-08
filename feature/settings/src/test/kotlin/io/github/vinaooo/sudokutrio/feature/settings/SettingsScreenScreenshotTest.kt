package io.github.vinaooo.sudokutrio.feature.settings

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.vinaooo.sudokutrio.core.designsystem.theme.SudokuTrioTheme
import io.github.vinaooo.sudokutrio.domain.model.Settings
import io.github.vinaooo.vinkit.core.ThemeMode
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SettingsScreenScreenshotTest {
    @get:Rule
    val compose = createComposeRule()

    private fun capture(name: String, themeMode: ThemeMode) {
        compose.setContent {
            SudokuTrioTheme(themeMode = themeMode, dynamicColor = false) {
                SettingsScreen(Settings(themeMode = themeMode, dynamicColor = false), {}, onBack = {})
            }
        }
        compose.onRoot().captureRoboImage("src/test/screenshots/$name.png")
    }

    @Test
    @Config(qualifiers = "w411dp-h1500dp-port-xxhdpi")
    fun settings_light() = capture("settings_light", ThemeMode.LIGHT)

    @Test
    @Config(qualifiers = "w411dp-h1500dp-port-xxhdpi")
    fun settings_dark() = capture("settings_dark", ThemeMode.DARK)

    @Test
    @Config(qualifiers = "sw600dp-w1280dp-h800dp-land-xhdpi")
    fun settings_tablet() = capture("settings_tablet", ThemeMode.LIGHT)

    @Test
    @Config(qualifiers = "pt-rBR-w411dp-h1500dp-port-xxhdpi")
    fun settings_light_pt_br() = capture("settings_light_pt_br", ThemeMode.LIGHT)
}
