package io.github.vinaooo.sudokutrio.debug

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import dagger.hilt.android.AndroidEntryPoint
import io.github.vinaooo.sudokutrio.MainActivity
import io.github.vinaooo.sudokutrio.domain.generator.PuzzleGenerator
import io.github.vinaooo.sudokutrio.domain.model.Difficulty
import io.github.vinaooo.sudokutrio.domain.model.GameMode
import io.github.vinaooo.sudokutrio.domain.model.Move
import io.github.vinaooo.sudokutrio.domain.model.Puzzle
import io.github.vinaooo.sudokutrio.domain.model.Variant
import io.github.vinaooo.sudokutrio.domain.repository.SavedGameRepository
import io.github.vinaooo.sudokutrio.domain.rules.GameEngine
import io.github.vinaooo.sudokutrio.domain.session.BoardCodec
import io.github.vinaooo.sudokutrio.domain.session.GameSession
import io.github.vinaooo.sudokutrio.domain.session.decodeSession
import io.github.vinaooo.sudokutrio.domain.solver.Candidates
import io.github.vinaooo.sudokutrio.domain.solver.Deduction
import io.github.vinaooo.sudokutrio.domain.solver.HiddenSingle
import io.github.vinaooo.sudokutrio.domain.solver.NakedSingle
import io.github.vinaooo.sudokutrio.domain.solver.SolverContext
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.runBlocking

/**
 * Debug builds only. Saves a test game and opens it, to try a position without playing up to it. Start it with `-S`,
 * so a running game screen can't overwrite the save; the applicationId and the package differ in case:
 * `adb shell am start -S -n io.github.vinaooo.SudokuTrio/io.github.vinaooo.sudokutrio.debug.DebugGameActivity --es game killer_near_win`
 * - `near_win` (the default), `x_near_win`, `killer_near_win`: one cell left to fill (the top-left empty one).
 * - `wrong_digit`: a wrong digit on the board, which the hint points at first.
 * - `elimination`: an expert board played with singles until only eliminations help, for the hint that names them.
 * - `killer`: a fresh Killer expert board.
 *
 * Or replay a bug report:
 * - `--es state <code>`: the code in the report's "State:" block (GitHub or email), the exact board.
 * - `--es load game.json`: the report's attached game, with its moves to undo, pushed first to the app's own folder:
 *   `adb push game.json /sdcard/Android/data/io.github.vinaooo.SudokuTrio/files/`
 */
@AndroidEntryPoint
class DebugGameActivity : ComponentActivity() {

    @Inject lateinit var savedGames: SavedGameRepository

    @Inject lateinit var generator: PuzzleGenerator

    @Inject lateinit var engine: GameEngine

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        runBlocking {
            val report = intent.getStringExtra("state")?.let { GameSession(seed = 0, state = BoardCodec.decode(it)) }
                ?: intent.getStringExtra("load")?.let {
                    decodeSession(File(getExternalFilesDir(null), it).readText())
                }
            savedGames.save(report ?: preset(intent.getStringExtra("game")))
        }
        startActivity(
            Intent(this, MainActivity::class.java).addFlags(
                Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK,
            ),
        )
        finish()
    }

    private suspend fun preset(name: String?): GameSession = when (name) {
        "x_near_win" -> nearWin(Variant.X)
        "killer_near_win" -> nearWin(Variant.KILLER)
        "wrong_digit" -> wrongDigit()
        "elimination" -> elimination()
        "killer" -> fresh(GameMode(Variant.KILLER, Difficulty.EXPERT))
        else -> nearWin(Variant.CLASSIC)
    }

    private suspend fun fresh(mode: GameMode) = GameSession(SEED, engine.newGame(generator.generate(mode, SEED), mode))

    /** Every empty cell but the first filled with its digit. */
    private suspend fun nearWin(variant: Variant): GameSession {
        var session = fresh(GameMode(variant, Difficulty.EASY))
        val puzzle = session.state.puzzle
        puzzle.givens.indices.filter { puzzle.givens[it] == 0 }.drop(1).forEach { cell ->
            session = checkNotNull(session.play(Move.Place(cell, puzzle.solution[cell]), engine))
        }
        return session
    }

    private suspend fun wrongDigit(): GameSession {
        val session = fresh(GameMode(Variant.CLASSIC, Difficulty.MEDIUM))
        val cell = session.state.puzzle.givens.indexOfFirst { it == 0 }
        val wrong = session.state.puzzle.solution[cell] % WRONG_STEP + 1
        return checkNotNull(session.play(Move.Place(cell, wrong), engine))
    }

    private suspend fun elimination(): GameSession {
        var session = fresh(GameMode(Variant.CLASSIC, Difficulty.EXPERT))
        val puzzle: Puzzle = session.state.puzzle
        val context = SolverContext(puzzle, Variant.CLASSIC)
        while (true) {
            val candidates = Candidates.of(session.state.board.values, context)
            val single = (NakedSingle.find(candidates, context) ?: HiddenSingle.find(candidates, context))
                as Deduction.Placement? ?: break
            session = checkNotNull(session.play(Move.Place(single.cell, single.digit), engine))
        }
        return session
    }

    private companion object {
        const val SEED = 5L
        const val WRONG_STEP = 9
    }
}
