package io.github.vinaooo.sudokutrio.data.local

import androidx.room.withTransaction
import io.github.vinaooo.sudokutrio.domain.model.GameMode
import io.github.vinaooo.sudokutrio.domain.model.GameStats
import io.github.vinaooo.sudokutrio.domain.model.ScoreRecord
import io.github.vinaooo.sudokutrio.domain.repository.ScoreRepository
import io.github.vinaooo.sudokutrio.domain.repository.StatsRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomScoreRepository @Inject constructor(private val dao: ScoreDao) : ScoreRepository {
    override fun observeTopScores(mode: GameMode, limit: Int): Flow<List<ScoreRecord>> =
        dao.observeTop(mode.variant.name, mode.difficulty.name, limit).map { rows -> rows.mapNotNull { it.toDomain() } }

    override suspend fun add(record: ScoreRecord) = dao.insert(record.toEntity())
}

class RoomStatsRepository @Inject constructor(private val db: SudokuTrioDatabase, private val dao: StatsDao) :
    StatsRepository {
    override fun observe(mode: GameMode): Flow<GameStats> =
        dao.observe(mode.variant.name, mode.difficulty.name).map { it.toDomain() }

    override fun observePlayedModes(): Flow<Set<GameMode>> =
        dao.observePlayedModes().map { rows -> rows.mapNotNull { gameModeOf(it.variant, it.difficulty) }.toSet() }

    override suspend fun update(mode: GameMode, transform: (GameStats) -> GameStats) = db.withTransaction {
        val current = dao.get(mode.variant.name, mode.difficulty.name).toDomain()
        dao.upsert(transform(current).toEntity(mode))
    }
}
