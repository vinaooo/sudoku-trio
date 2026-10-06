package io.github.vinaooo.sudokutrio.domain.serialization

import io.github.vinaooo.sudokutrio.domain.SOLUTION
import io.github.vinaooo.sudokutrio.domain.model.Move
import io.github.vinaooo.sudokutrio.domain.model.Variant
import io.github.vinaooo.sudokutrio.domain.newState
import io.github.vinaooo.sudokutrio.domain.rules.GameEngine
import io.github.vinaooo.sudokutrio.domain.session.GameSession
import io.github.vinaooo.sudokutrio.domain.wrongDigit
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class GameSerializationTest {
    private val engine = GameEngine()

    @ParameterizedTest
    @EnumSource(Variant::class)
    fun `a session with notes, mistakes and undo history survives a JSON round trip`(variant: Variant) {
        val session = GameSession(7, newState(variant))
            .play(Move.ToggleNote(3, 5), engine).shouldNotBeNull()
            .play(Move.Place(0, wrongDigit(0)), engine).shouldNotBeNull()
            .play(Move.Place(6, SOLUTION[6]), engine).shouldNotBeNull()
            .undo().shouldNotBeNull()
            .tick(9, engine)
        val json = Json.encodeToString(GameSession.serializer(), session)
        Json.decodeFromString(GameSession.serializer(), json) shouldBe session
    }
}
