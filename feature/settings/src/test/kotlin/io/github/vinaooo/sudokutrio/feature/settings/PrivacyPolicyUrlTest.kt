package io.github.vinaooo.sudokutrio.feature.settings

import androidx.test.core.app.ApplicationProvider
import io.kotest.matchers.shouldBe
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class PrivacyPolicyUrlTest {
    private fun url() = ApplicationProvider.getApplicationContext<android.content.Context>()
        .getString(R.string.privacy_policy_url)

    @Test
    fun `the policy opens at its English section`() {
        url() shouldBe "https://vinaooo.github.io/sudoku-trio/privacy.html#en"
    }

    @Test
    @Config(qualifiers = "pt-rBR")
    fun `and at its Brazilian Portuguese one`() {
        url() shouldBe "https://vinaooo.github.io/sudoku-trio/privacy.html#pt-br"
    }
}
