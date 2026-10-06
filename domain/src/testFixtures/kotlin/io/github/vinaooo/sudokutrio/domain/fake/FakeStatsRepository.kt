package io.github.vinaooo.sudokutrio.domain.fake

import io.github.vinaooo.sudokutrio.domain.model.GameMode
import io.github.vinaooo.sudokutrio.domain.model.GameStats
import io.github.vinaooo.sudokutrio.domain.repository.StatsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeStatsRepository(initial: Map<GameMode, GameStats> = emptyMap()) : StatsRepository {
    val stats = MutableStateFlow(initial)

    override fun observe(mode: GameMode): Flow<GameStats> = stats.map { it[mode] ?: GameStats() }

    override fun observePlayedModes(): Flow<Set<GameMode>> =
        stats.map { all -> all.filterValues { it.played > 0 }.keys }

    override suspend fun update(mode: GameMode, transform: (GameStats) -> GameStats) {
        stats.value += mode to transform(stats.value[mode] ?: GameStats())
    }
}
