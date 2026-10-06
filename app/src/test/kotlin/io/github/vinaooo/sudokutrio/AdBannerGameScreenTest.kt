package io.github.vinaooo.sudokutrio

import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import io.github.vinaooo.sudokutrio.core.ads.AdBannerProvider
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The whole app, wired by Hilt with fake ads (see [FakeAdsModule]): the game screen carries the banner. */
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class)
class AdBannerGameScreenTest {
    private val hilt = HiltAndroidRule(this)
    private val compose = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val rules: TestRule = RuleChain.outerRule(hilt).around(compose)

    @Test
    fun `the game screen shows the number pad and the banner below it`() {
        compose.onNodeWithText("Notes").assertExists()
        compose.onNodeWithTag(AdBannerProvider.TEST_TAG).assertExists()
    }
}
