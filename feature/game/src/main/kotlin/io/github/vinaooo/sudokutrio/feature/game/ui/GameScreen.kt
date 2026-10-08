package io.github.vinaooo.sudokutrio.feature.game.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.vinaooo.sudokutrio.core.ui.modeName
import io.github.vinaooo.sudokutrio.domain.model.Variant
import io.github.vinaooo.sudokutrio.domain.session.GameSession
import io.github.vinaooo.sudokutrio.feature.game.GameIntent
import io.github.vinaooo.sudokutrio.feature.game.GameUiState
import io.github.vinaooo.sudokutrio.feature.game.GameViewModel
import io.github.vinaooo.sudokutrio.feature.game.R
import io.github.vinaooo.sudokutrio.feature.game.SudokuTrioReports
import io.github.vinaooo.sudokutrio.feature.game.board.CellHighlighter
import io.github.vinaooo.sudokutrio.feature.game.board.SudokuBoard
import io.github.vinaooo.sudokutrio.feature.game.board.completedDigits
import io.github.vinaooo.sudokutrio.feature.game.gameReport
import io.github.vinaooo.vinkit.shell.FrameInfo
import io.github.vinaooo.vinkit.shell.GameFrame
import io.github.vinaooo.vinkit.shell.GameSurface
import io.github.vinaooo.vinkit.shell.GameToolbar
import io.github.vinaooo.vinkit.shell.ModeAndTime
import io.github.vinaooo.vinkit.shell.WinDialog

/** The game screen. The Scores and Settings buttons show only when their screens exist ([onOpenScores] non-null). */
@Composable
fun GameRoute(
    modifier: Modifier = Modifier,
    onOpenScores: (() -> Unit)? = null,
    onOpenSettings: (() -> Unit)? = null,
    viewModel: GameViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LifecycleResumeEffect(viewModel) {
        viewModel.onIntent(GameIntent.Resume)
        onPauseOrDispose { viewModel.onIntent(GameIntent.Pause) }
    }
    GameScreen(uiState, viewModel::onIntent, modifier, onOpenScores, onOpenSettings)
}

/**
 * vinkit's frame with Sudoku's parts: the mode and clock (or the hint in their place, so the board never moves), the
 * board, the number pad and the toolbar. A left hand mirrors the pad and toolbar.
 */
@Composable
fun GameScreen(
    uiState: GameUiState,
    onIntent: (GameIntent) -> Unit,
    modifier: Modifier = Modifier,
    onOpenScores: (() -> Unit)? = null,
    onOpenSettings: (() -> Unit)? = null,
) {
    val announced = uiState.announcement
    GameSurface(
        announcement = announced?.let { announcementText(it.announcement) },
        announcementSequence = announced?.sequence ?: 0,
        modifier = modifier,
        reportTarget = SudokuTrioReports,
        gameReport = { gameReport(uiState.settings, uiState.appSettings, uiState.session) },
    ) { reportBug ->
        val controls = remember(uiState) { uiState.controls() }
        GameFrame(
            settings = uiState.appSettings,
            info = { GameInfo(uiState, it) },
            board = { BoardOrLoading(uiState, onIntent) },
            toolbar = { frame ->
                GameToolbar(
                    actions = toolbarActions(controls.toolbar, onIntent),
                    menuOptions = menuOptions(onIntent),
                    onReportBug = reportBug,
                    vertical = frame.landscape,
                    mirrored = frame.mirrored,
                )
            },
            controls = { frame -> Pad(controls, onIntent, frame) },
            onOpenScores = onOpenScores,
            onOpenSettings = onOpenSettings,
        )
    }
    uiState.winRecord?.let { WinDialog(winLines(it), onNewGame = { onIntent(GameIntent.NewGame) }) }
}

/**
 * Portrait: the mode and clock, or the hint in their place while one shows. Landscape's side column has room for
 * both, the clock larger.
 */
@Composable
private fun GameInfo(uiState: GameUiState, frame: FrameInfo) {
    val hint = uiState.session?.state?.pendingHint
    val modeAndTime: @Composable (Modifier) -> Unit = {
        ModeAndTime(modeName(uiState.mode()), uiState.elapsedSeconds(), it, large = frame.landscape)
    }
    if (frame.landscape) {
        Column {
            modeAndTime(Modifier)
            hint?.let { HintCard(it, Modifier.fillMaxWidth().padding(top = 12.dp)) }
        }
    } else {
        AnimatedContent(targetState = hint, contentKey = { it != null }, label = "top region") { shown ->
            if (shown != null) {
                HintCard(shown, Modifier.fillMaxWidth().padding(end = 4.dp))
            } else {
                modeAndTime(Modifier)
            }
        }
    }
}

/** The number pad: two rows in portrait, a 3-column grid in landscape, bigger on a large tablet. */
@Composable
private fun Pad(controls: Controls, onIntent: (GameIntent) -> Unit, frame: FrameInfo) {
    if (frame.landscape) {
        NumberPad(
            controls.completed,
            controls.toolbar.enabled,
            onIntent,
            Modifier.width(if (frame.large) LARGE_PAD_WIDTH else PAD_WIDTH),
            grid = true,
            keyHeight = if (frame.large) LARGE_KEY_HEIGHT else KEY_HEIGHT,
            mirrored = frame.mirrored,
        )
    } else {
        NumberPad(controls.completed, controls.toolbar.enabled, onIntent, mirrored = frame.mirrored)
    }
}

/** What the pad and toolbar show, worked out once per state. */
private data class Controls(val completed: Set<Int>, val toolbar: ToolbarState)

private fun GameUiState.controls(): Controls {
    val session = session
    val ready = session != null && !loading && !session.state.isWon
    return Controls(
        completed = session?.state?.board?.values?.let(::completedDigits).orEmpty(),
        toolbar = ToolbarState(
            notesMode = notesMode,
            canUndo = session?.canUndo == true,
            canRedo = session?.canRedo == true,
            hintShown = session?.state?.pendingHint != null,
            enabled = ready,
        ),
    )
}

private fun GameUiState.mode() = session?.state?.mode ?: settings.mode

private fun GameUiState.elapsedSeconds() = session?.state?.elapsedSeconds ?: 0

/** The board, or the loading indicator while a puzzle is made. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun BoardOrLoading(uiState: GameUiState, onIntent: (GameIntent) -> Unit) {
    val session = uiState.session
    if (session == null || uiState.loading) {
        val loading = stringResource(R.string.generating)
        Box(Modifier.fillMaxSize()) {
            LoadingIndicator(Modifier.align(Alignment.Center).semantics { contentDescription = loading })
        }
    } else {
        Board(session, uiState.selected, uiState.conflicts, onIntent)
    }
}

/** The board with its highlights, worked out only when the board, selection, hint or conflicts change. */
@Composable
internal fun Board(session: GameSession, selected: Int?, conflicts: Set<Int>, onIntent: (GameIntent) -> Unit) {
    val state = session.state
    val highlighter = remember(state.puzzle, state.mode) { CellHighlighter(state.puzzle, state.mode.variant) }
    val hintCells = state.pendingHint?.cells.orEmpty()
    val highlights = remember(highlighter, state.board, selected, hintCells, conflicts) {
        highlighter.highlights(state.board.values, selected, hintCells, conflicts)
    }
    val description = stringResource(R.string.a11y_board)
    SudokuBoard(
        board = state.board,
        puzzle = state.puzzle,
        highlights = highlights,
        onSelect = { onIntent(GameIntent.SelectCell(it)) },
        diagonals = state.mode.variant == Variant.X,
        modifier = Modifier.fillMaxSize().testTag(BOARD_TAG).semantics { contentDescription = description },
    )
}

internal const val BOARD_TAG = "board"
internal val PAD_WIDTH = 184.dp

/** On a tablet, landscape's pad has room for bigger keys. */
internal val LARGE_PAD_WIDTH = 280.dp
internal val LARGE_KEY_HEIGHT = 72.dp
