package io.github.vinaooo.sudokutrio.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** A won game in its mode's ranking. Modes are stored by their enum names. */
@Entity(tableName = "scores", indices = [Index("variant", "difficulty")])
data class ScoreEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val variant: String,
    val difficulty: String,
    val points: Int,
    val elapsedSeconds: Long,
    val mistakes: Int,
    val hintsUsed: Int,
    val playedAtMillis: Long,
)

/** One row per mode played. */
@Entity(tableName = "stats", primaryKeys = ["variant", "difficulty"])
data class StatsEntity(
    val variant: String,
    val difficulty: String,
    val played: Int,
    val won: Int,
    val currentStreak: Int,
    val bestStreak: Int,
)

/** A mode as a query returns it. */
data class ModeRow(val variant: String, val difficulty: String)
