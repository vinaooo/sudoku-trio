package io.github.vinaooo.sudokutrio.core.ads

import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import io.github.vinaooo.sudokutrio.core.designsystem.theme.SudokuTrioTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class PlaceholderAdBannerTest {
    @get:Rule
    val compose = createComposeRule()

    private fun show() = compose.setContent { SudokuTrioTheme { PlaceholderAdBanner().Banner() } }

    @Test
    @Config(qualifiers = "w411dp-h891dp-port")
    fun `in portrait it reserves a full-width 60dp slot`() {
        show()
        compose.onNodeWithTag(AdBannerProvider.TEST_TAG).assertHeightIsEqualTo(60.dp).assertWidthIsEqualTo(411.dp)
        compose.onNodeWithText("Ad").assertExists()
    }

    @Test
    @Config(qualifiers = "w891dp-h411dp-land")
    fun `in landscape the slot is 50dp tall`() {
        show()
        compose.onNodeWithTag(AdBannerProvider.TEST_TAG).assertHeightIsEqualTo(50.dp).assertWidthIsEqualTo(891.dp)
    }
}
