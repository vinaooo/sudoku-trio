package io.github.vinaooo.sudokutrio.core.ads

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Public so app tests can swap it for fakes with `@TestInstallIn`. */
@Module
@InstallIn(SingletonComponent::class)
interface AdsModule {
    @Binds
    fun adBannerProvider(impl: PlaceholderAdBanner): AdBannerProvider
}
