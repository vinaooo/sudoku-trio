package io.github.vinaooo.sudokutrio.data.local

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import io.github.vinaooo.sudokutrio.domain.model.Difficulty
import io.github.vinaooo.sudokutrio.domain.model.GameMode
import io.github.vinaooo.sudokutrio.domain.model.GameStats
import io.github.vinaooo.sudokutrio.domain.model.Variant
import io.kotest.matchers.shouldBe
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * A player's scores and stats survive every schema change: a database built from each exported schema, with data
 * in it, is opened by the current app, which runs the real migrations. Add a case for each new schema version.
 */
@RunWith(RobolectricTestRunner::class)
class SudokuTrioDatabaseMigrationTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val mode = GameMode(Variant.KILLER, Difficulty.HARD)

    @Test
    fun `a version 1 database opens with its scores and stats`() = runTest {
        val file = context.getDatabasePath(NAME).also { it.parentFile?.mkdirs() }
        create(version = 1, file)

        val db = Room.databaseBuilder(context, SudokuTrioDatabase::class.java, NAME).allowMainThreadQueries().build()
        try {
            val top = RoomScoreRepository(db.scoreDao()).observeTopScores(mode).first()
            top.single().points shouldBe 5400
            top.single().hintsUsed shouldBe 2
            RoomStatsRepository(db, db.statsDao()).observe(mode).first() shouldBe
                GameStats(played = 5, won = 3, currentStreak = 1, bestStreak = 2)
        } finally {
            db.close()
        }
    }

    /** The tables and identity of an exported schema, with a score and a stats row in them. */
    private fun create(version: Int, file: File) {
        val schema = Json.parseToJsonElement(File("$SCHEMAS/$version.json").readText()).jsonObject
            .getValue("database").jsonObject
        SQLiteDatabase.openOrCreateDatabase(file, null).use { db ->
            schema.getValue("entities").jsonArray.forEach { entity ->
                val table = entity.jsonObject.getValue("tableName").jsonPrimitive.content
                db.execSQL(
                    entity.jsonObject.getValue("createSql").jsonPrimitive.content.replace("\${TABLE_NAME}", table),
                )
                entity.jsonObject["indices"]?.jsonArray?.forEach { index ->
                    db.execSQL(
                        index.jsonObject.getValue("createSql").jsonPrimitive.content.replace("\${TABLE_NAME}", table),
                    )
                }
            }
            schema.getValue("setupQueries").jsonArray.forEach { db.execSQL(it.jsonPrimitive.content) }
            db.execSQL(
                "INSERT INTO scores " +
                    "(variant, difficulty, points, elapsedSeconds, mistakes, hintsUsed, playedAtMillis) " +
                    "VALUES ('KILLER', 'HARD', 5400, 300, 1, 2, 7)",
            )
            db.execSQL(
                "INSERT INTO stats (variant, difficulty, played, won, currentStreak, bestStreak) " +
                    "VALUES ('KILLER', 'HARD', 5, 3, 1, 2)",
            )
            db.version = version
        }
    }

    private companion object {
        const val NAME = "migration-test.db"
        const val SCHEMAS = "schemas/io.github.vinaooo.sudokutrio.data.local.SudokuTrioDatabase"
    }
}
