package io.github.vinaooo.sudokutrio.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ScoreDao {
    @Insert
    suspend fun insert(score: ScoreEntity)

    /** A mode's best scores: the most points first, then the fastest, then the earliest (`ScoreRecord.rankingFor`). */
    @Query(
        "SELECT * FROM scores WHERE variant = :variant AND difficulty = :difficulty " +
            "ORDER BY points DESC, elapsedSeconds ASC, playedAtMillis ASC LIMIT :limit",
    )
    fun observeTop(variant: String, difficulty: String, limit: Int): Flow<List<ScoreEntity>>
}

@Dao
interface StatsDao {
    @Query("SELECT * FROM stats WHERE variant = :variant AND difficulty = :difficulty")
    fun observe(variant: String, difficulty: String): Flow<StatsEntity?>

    @Query("SELECT * FROM stats WHERE variant = :variant AND difficulty = :difficulty")
    suspend fun get(variant: String, difficulty: String): StatsEntity?

    @Query("SELECT variant, difficulty FROM stats WHERE played > 0")
    fun observePlayedModes(): Flow<List<ModeRow>>

    @Upsert
    suspend fun upsert(stats: StatsEntity)
}
