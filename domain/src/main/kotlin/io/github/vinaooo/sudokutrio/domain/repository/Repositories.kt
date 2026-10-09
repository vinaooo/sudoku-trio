package io.github.vinaooo.sudokutrio.domain.repository

import io.github.vinaooo.sudokutrio.domain.model.Settings
import io.github.vinaooo.sudokutrio.domain.session.GameSession
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import kotlinx.coroutines.flow.Flow

interface SavedGameRepository {
    suspend fun load(): GameSession?

    suspend fun save(session: GameSession)

    suspend fun clear()
}

interface SettingsRepository {
    val settings: Flow<Settings>

    suspend fun update(transform: (Settings) -> Settings)
}

fun interface SeedSource {
    fun nextSeed(): Long
}

fun interface Clock {
    fun nowMillis(): Long

    /** The phone's local date and time now, in the time zone it is in at this moment. */
    fun now(): LocalDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(nowMillis()), ZoneId.systemDefault())
}
