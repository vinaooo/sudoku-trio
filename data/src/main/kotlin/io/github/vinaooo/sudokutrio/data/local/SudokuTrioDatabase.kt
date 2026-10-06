package io.github.vinaooo.sudokutrio.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * Scores and stats. Every schema change gets an `AutoMigration` (or a manual one) and a test migrating from each
 * exported schema in `data/schemas`; never a destructive fallback, which would wipe the players' scores.
 */
@Database(entities = [ScoreEntity::class, StatsEntity::class], version = 1, exportSchema = true)
abstract class SudokuTrioDatabase : RoomDatabase() {
    abstract fun scoreDao(): ScoreDao

    abstract fun statsDao(): StatsDao

    companion object {
        const val NAME = "sudokutrio.db"
    }
}
