package io.github.vinaooo.sudokutrio.feature.game

import io.github.vinaooo.sudokutrio.domain.model.Difficulty
import io.github.vinaooo.sudokutrio.domain.model.GameMode
import io.github.vinaooo.sudokutrio.domain.model.Move
import io.github.vinaooo.sudokutrio.domain.model.Puzzle
import io.github.vinaooo.sudokutrio.domain.model.Settings
import io.github.vinaooo.sudokutrio.domain.model.Variant
import io.github.vinaooo.sudokutrio.domain.rules.GameEngine
import io.github.vinaooo.sudokutrio.domain.session.BoardCodec
import io.github.vinaooo.sudokutrio.domain.session.GameSession
import io.github.vinaooo.sudokutrio.domain.session.decodeSession
import io.github.vinaooo.vinkit.core.AppSettings
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class BugReportTest {
    private val engine = GameEngine()
    private val solution = (
        "249368715356971824781542639512783496467195283893426571928614357675839142134257968"
        ).map { it.digitToInt() }
    private val mode = GameMode(Variant.KILLER, Difficulty.HARD)
    private val session = GameSession(77, engine.newGame(Puzzle(solution.map { 0 }, solution), mode))
        .play(Move.Place(0, 5), engine)!!
        .play(Move.RevealHint, engine)!!
        .tick(40, engine)
    private val settings = Settings(mode = mode)

    @Test
    fun `a report holds the settings, the game, its exact board and its file`() {
        val report = gameReport(settings, AppSettings(), session)

        report.details shouldContainExactly listOf(
            "Settings: KILLER HARD, RIGHT hand, board TOP, theme SYSTEM, dynamic color true, phone view false",
            "Game: seed 77, KILLER HARD, 1 moves, 1 mistakes, 1 hints, score ${session.state.score}, " +
                "${session.state.elapsedSeconds}s",
        )
        BoardCodec.decode(report.state!!) shouldBe session.state
        decodeSession(report.files.getValue("game.json")) shouldBe session
    }

    @Test
    fun `no game means no game line, board or file`() {
        val report = gameReport(settings, AppSettings(), null)

        report.details.size shouldBe 1
        report.state shouldBe null
        report.files shouldBe emptyMap()
    }

    @Test
    fun `reports go to the app's own address and repository`() {
        SudokuTrioReports.email shouldBe "vrpedrinho+trio@gmail.com"
        SudokuTrioReports.issuesUrl shouldBe "https://github.com/vinaooo/sudoku-trio/issues/new"
    }
}
