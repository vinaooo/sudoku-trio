package io.github.vinaooo.sudokutrio.domain.fake

import io.github.vinaooo.sudokutrio.domain.repository.SavedGameRepository
import io.github.vinaooo.sudokutrio.domain.session.GameSession

class FakeSavedGameRepository(var saved: GameSession? = null) : SavedGameRepository {
    var saves = 0
        private set

    override suspend fun load(): GameSession? = saved

    override suspend fun save(session: GameSession) {
        saved = session
        saves++
    }

    override suspend fun clear() {
        saved = null
    }
}
