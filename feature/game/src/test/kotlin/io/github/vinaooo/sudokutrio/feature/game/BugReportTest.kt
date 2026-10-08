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
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldStartWith
import java.net.URLDecoder
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
    private val info = ReportInfo(
        appVersion = "1.2.0 (140)",
        android = "16 (API 36)",
        device = "Motorola moto g",
        screen = "411x891dp, 420dpi",
        settings = Settings(mode = mode),
        session = session,
    )

    @Test
    fun `the report starts with the player's words, then the facts to reproduce it`() {
        val body = reportBody(info, "The 5 didn't go in")

        body shouldStartWith "The 5 didn't go in\n\n---\n"
        body shouldContain "App: 1.2.0 (140)"
        body shouldContain "Android: 16 (API 36)"
        body shouldContain "Device: Motorola moto g"
        body shouldContain "Settings: KILLER HARD, RIGHT hand"
        body shouldContain "Game: seed 77, KILLER HARD, 1 moves, 1 mistakes, 1 hints"
        val code = body.substringAfter("State:\n```\n").substringBefore("\n```")
        BoardCodec.decode(code) shouldBe session.state
    }

    @Test
    fun `an empty description says so, and no game means no game line`() {
        val body = reportBody(info.copy(session = null), "  ")

        body shouldStartWith "(no description)"
        body.contains("Game:") shouldBe false
    }

    @Test
    fun `the GitHub link opens a new issue on this app's repository with the title and body filled in`() {
        val url = githubIssueUrl("Cage & sum", "line 1\nline 2")

        url shouldStartWith "https://github.com/vinaooo/sudoku-trio/issues/new?title="
        URLDecoder.decode(url.substringAfter("title=").substringBefore("&body="), "UTF-8") shouldBe "Cage & sum"
        URLDecoder.decode(url.substringAfter("&body="), "UTF-8") shouldBe "line 1\nline 2"
    }

    @Test
    fun `reports go to the app's own address`() {
        REPORT_EMAIL shouldBe "vrpedrinho+trio@gmail.com"
    }
}
