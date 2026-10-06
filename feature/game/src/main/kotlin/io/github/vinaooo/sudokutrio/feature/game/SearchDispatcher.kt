package io.github.vinaooo.sudokutrio.feature.game

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/** Where hint searches run, off the main thread; tests pass their own dispatcher. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class SearchDispatcher

@Module
@InstallIn(SingletonComponent::class)
internal object SearchDispatcherModule {
    @Provides
    @SearchDispatcher
    fun searchDispatcher(): CoroutineDispatcher = Dispatchers.Default
}
