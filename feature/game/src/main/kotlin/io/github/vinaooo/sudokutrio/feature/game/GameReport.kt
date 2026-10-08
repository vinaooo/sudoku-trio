package io.github.vinaooo.sudokutrio.feature.game

import io.github.vinaooo.sudokutrio.domain.model.Settings
import io.github.vinaooo.sudokutrio.domain.session.BoardCodec
import io.github.vinaooo.sudokutrio.domain.session.GameSession
import io.github.vinaooo.vinkit.bugreport.GameReport
import io.github.vinaooo.vinkit.bugreport.ReportTarget
import kotlinx.serialization.json.Json

/** Where Sudoku Trio's bug reports go: the contact in the privacy policy, chosen with the user, and the repository. */
internal val SudokuTrioReports = ReportTarget("vrpedrinho+trio@gmail.com", "vinaooo/sudoku-trio")

/** What a bug report says about the game: its settings and, with a game, the game, its exact board and its file. */
internal fun gameReport(settings: Settings, session: GameSession?): GameReport = GameReport(
    details = listOfNotNull(
        with(settings) {
            "Settings: ${mode.variant} ${mode.difficulty}, $handedness hand, board $boardAlignment, " +
                "theme $themeMode, dynamic color $dynamicColor, phone view $phoneView"
        },
        session?.run {
            with(state) {
                "Game: seed $seed, ${mode.variant} ${mode.difficulty}, $moves moves, $mistakes mistakes, " +
                    "$hintsUsed hints, score $score, ${elapsedSeconds}s"
            }
        },
    ),
    state = session?.let { BoardCodec.encode(it.state) },
    files = session?.let { mapOf("game.json" to Json.encodeToString(it)) }.orEmpty(),
)
