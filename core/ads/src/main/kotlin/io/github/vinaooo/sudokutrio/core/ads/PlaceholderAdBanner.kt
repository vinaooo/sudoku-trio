package io.github.vinaooo.sudokutrio.core.ads

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import javax.inject.Inject

/** Stand-in until AdMob is integrated: takes the real banner's room ([BannerSlot]), so layouts are final now. */
class PlaceholderAdBanner @Inject constructor() : AdBannerProvider {
    @Composable
    override fun Banner(modifier: Modifier) {
        BoxWithConstraints(modifier.fillMaxWidth().testTag(AdBannerProvider.TEST_TAG)) {
            val landscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
            val slot = BannerSlot.of(maxWidth.value.toInt(), landscape)
            Box(Modifier.fillMaxWidth().height(slot.heightDp.dp), contentAlignment = Alignment.Center) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    modifier = Modifier.width(slot.adWidthDp.dp).fillMaxHeight(),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = stringResource(R.string.ad_placeholder),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}
