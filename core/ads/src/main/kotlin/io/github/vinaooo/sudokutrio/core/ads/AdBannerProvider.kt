package io.github.vinaooo.sudokutrio.core.ads

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** Renders the bottom ad banner. The app depends on this abstraction, so the ad network can be swapped freely. */
interface AdBannerProvider {
    @Composable
    fun Banner(modifier: Modifier = Modifier)

    companion object {
        const val TEST_TAG = "ad_banner"
    }
}
