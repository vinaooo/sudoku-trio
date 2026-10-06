package io.github.vinaooo.sudokutrio

import dagger.Binds
import dagger.Module
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import io.github.vinaooo.sudokutrio.core.ads.AdBannerProvider
import io.github.vinaooo.sudokutrio.core.ads.AdsModule
import io.github.vinaooo.sudokutrio.core.ads.PlaceholderAdBanner

/** Keeps any real ads SDK out of app tests, now and once AdMob replaces the placeholder. */
@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [AdsModule::class])
interface FakeAdsModule {
    @Binds
    fun adBannerProvider(impl: PlaceholderAdBanner): AdBannerProvider
}
