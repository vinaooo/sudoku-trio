package io.github.vinaooo.sudokutrio.core.ads

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import javax.inject.Inject

/**
 * An adaptive AdMob banner in the room [BannerSlot] gives it, capped at its height. Its full-width slot always takes
 * that height, even before consent or while no ad is loaded, so the board above never jumps; a smaller ad sits
 * centered in it.
 */
class AdMobBanner @Inject constructor(private val config: AdsConfig, private val consent: AdConsent) :
    AdBannerProvider {
    @Composable
    override fun Banner(modifier: Modifier) {
        val state by consent.state.collectAsStateWithLifecycle()
        BoxWithConstraints(modifier.fillMaxWidth().testTag(AdBannerProvider.TEST_TAG)) {
            val landscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
            val slot = BannerSlot.of(maxWidth.value.toInt(), landscape)
            // An inline adaptive size takes a height cap; the anchored sizes are about 130dp tall on a phone.
            val size = remember(slot) { AdSize.getInlineAdaptiveBannerAdSize(slot.adWidthDp, slot.heightDp) }
            Surface(
                // The game's background, so the slot reads as part of the screen rather than a separate strip.
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth().height(slot.heightDp.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    // A new size (rotation, window resize) needs a new ad view sized for it.
                    if (state.canRequestAds) key(size) { BannerView(config.bannerUnitId, size, slot.adWidthDp.dp) }
                }
            }
        }
    }
}

@Composable
private fun BannerView(adUnitId: String, size: AdSize, width: Dp) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val adView = remember {
        AdView(context).apply {
            this.adUnitId = adUnitId
            setAdSize(size)
            loadAd(AdRequest.Builder().build())
        }
    }
    DisposableEffect(adView, lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> adView.resume()
                Lifecycle.Event.ON_PAUSE -> adView.pause()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            adView.destroy()
        }
    }
    AndroidView(factory = { adView }, modifier = Modifier.width(width))
}
