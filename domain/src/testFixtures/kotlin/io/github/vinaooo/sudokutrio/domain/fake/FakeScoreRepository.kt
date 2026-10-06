package io.github.vinaooo.sudokutrio.domain.fake

import io.github.vinaooo.sudokutrio.domain.model.GameMode
import io.github.vinaooo.sudokutrio.domain.model.ScoreRecord
import io.github.vinaooo.sudokutrio.domain.repository.ScoreRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeScoreRepository(initial: List<ScoreRecord> = emptyList()) : ScoreRepository {
    val records = MutableStateFlow(initial)

    override fun observeTopScores(mode: GameMode, limit: Int): Flow<List<ScoreRecord>> = records.map { all ->
        all.filter { it.mode == mode }.sortedWith(ScoreRecord.rankingFor(mode)).take(limit)
    }

    override suspend fun add(record: ScoreRecord) {
        records.value += record
    }
}
