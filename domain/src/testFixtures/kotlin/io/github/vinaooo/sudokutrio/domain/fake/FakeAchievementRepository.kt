package io.github.vinaooo.sudokutrio.domain.fake

import io.github.vinaooo.vinkit.core.AchievementProgress
import io.github.vinaooo.vinkit.core.AchievementRepository
import kotlinx.coroutines.flow.MutableStateFlow

class FakeAchievementRepository(initial: AchievementProgress = AchievementProgress()) : AchievementRepository {
    override val progress = MutableStateFlow(initial)

    override suspend fun update(transform: (AchievementProgress) -> AchievementProgress) {
        progress.value = transform(progress.value)
    }
}
