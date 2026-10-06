package io.github.vinaooo.sudokutrio.domain.fake

import io.github.vinaooo.sudokutrio.domain.model.Settings
import io.github.vinaooo.sudokutrio.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow

class FakeSettingsRepository(initial: Settings = Settings()) : SettingsRepository {
    override val settings = MutableStateFlow(initial)

    override suspend fun update(transform: (Settings) -> Settings) {
        settings.value = transform(settings.value)
    }
}
