package io.github.vinaooo.sudokutrio.feature.game.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.roundToIntRect
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.vinaooo.sudokutrio.domain.model.Variant
import io.github.vinaooo.sudokutrio.domain.session.GameSession
import io.github.vinaooo.sudokutrio.feature.game.GameIntent
import io.github.vinaooo.sudokutrio.feature.game.GameUiState
import io.github.vinaooo.sudokutrio.feature.game.GameViewModel
import io.github.vinaooo.sudokutrio.feature.game.R
import io.github.vinaooo.sudokutrio.feature.game.board.CellHighlighter
import io.github.vinaooo.sudokutrio.feature.game.board.SudokuBoard
import io.github.vinaooo.sudokutrio.feature.game.board.completedDigits
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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

@Composable
fun GameScreen(
    uiState: GameUiState,
    onIntent: (GameIntent) -> Unit,
    modifier: Modifier = Modifier,
    onOpenScores: (() -> Unit)? = null,
    onOpenSettings: (() -> Unit)? = null,
) {
    // The screen as last drawn, for a bug report's screenshot.
    val frame = rememberGraphicsLayer()
    var report by remember { mutableStateOf<BugReport?>(null) }
    val scope = rememberCoroutineScope()
    val handle: (GameIntent) -> Unit = { intent ->
        if (intent == GameIntent.ReportBug) {
            // After the menu and its scrim have gone, so the screenshot shows the board as it was.
            scope.launch {
                delay(MENU_CLOSED_MILLIS)
                report = BugReport(frame.toImageBitmap())
            }
        } else {
            onIntent(intent)
        }
    }
    var area by remember { mutableStateOf(IntRect.Zero) }
    Surface(
        modifier = modifier.fillMaxSize()
            .onGloballyPositioned { area = it.boundsInWindow().roundToIntRect() }
            .drawWithContent {
                frame.record { this@drawWithContent.drawContent() }
                drawLayer(frame)
            },
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        // Surface stretches each direct child to its full size, so the tiny announcer sits in a Box of its own.
        Box {
            CompositionLocalProvider(LocalGameArea provides area) {
                GameContent(uiState, handle, onOpenScores, onOpenSettings)
            }
            Announcer(uiState.announcement)
        }
    }
    uiState.winRecord?.let { WinDialog(it, onNewGame = { onIntent(GameIntent.NewGame) }) }
    report?.let { BugReportDialog(uiState, it.screenshot, onDone = { report = null }) }
}

/**
 * Portrait: mode and clock, the board (with the hint below it), the number pad and the toolbar, top to bottom.
 * Landscape: the board on the left at full height, the rest in a column on its right (adaptive layouts come later).
 */
@Composable
private fun GameContent(
    uiState: GameUiState,
    onIntent: (GameIntent) -> Unit,
    onOpenScores: (() -> Unit)?,
    onOpenSettings: (() -> Unit)?,
) {
    BoxWithConstraints(Modifier.fillMaxSize().safeDrawingPadding()) {
        val landscape = maxWidth > maxHeight
        val controls: @Composable (Modifier) -> Unit = { controlsModifier ->
            Column(controlsModifier, horizontalAlignment = Alignment.CenterHorizontally) {
                GameTopBar(
                    mode = uiState.session?.state?.mode ?: uiState.settings.mode,
                    elapsedSeconds = uiState.session?.state?.elapsedSeconds ?: 0,
                    onOpenScores = onOpenScores,
                    onOpenSettings = onOpenSettings,
                )
                if (landscape) Box(Modifier.weight(1f))
            }
        }
        if (landscape) {
            Row(Modifier.fillMaxSize()) {
                BoardOrLoading(uiState, onIntent, Modifier.fillMaxHeight().aspectRatio(1f).padding(8.dp))
                Column(Modifier.width(LANDSCAPE_PANEL).fillMaxHeight()) {
                    controls(Modifier.weight(1f))
                    PadAndToolbar(uiState, onIntent)
                }
            }
        } else {
            Column(Modifier.fillMaxSize()) {
                controls(Modifier)
                BoardOrLoading(
                    uiState,
                    onIntent,
                    Modifier.fillMaxWidth().weight(1f).padding(horizontal = 8.dp, vertical = 4.dp),
                )
                PadAndToolbar(uiState, onIntent)
            }
        }
    }
}

@Composable
private fun PadAndToolbar(uiState: GameUiState, onIntent: (GameIntent) -> Unit) {
    val session = uiState.session
    val ready = session != null && !uiState.loading && !session.state.isWon
    val values = session?.state?.board?.values
    val completed = remember(values) { values?.let(::completedDigits).orEmpty() }
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        NumberPad(completed, uiState.notesMode, enabled = ready, onIntent = onIntent)
        GameToolbar(
            canUndo = session?.canUndo == true,
            canRedo = session?.canRedo == true,
            hintShown = session?.state?.pendingHint != null,
            enabled = ready,
            onIntent = onIntent,
            modifier = Modifier.padding(vertical = 12.dp),
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun BoardOrLoading(uiState: GameUiState, onIntent: (GameIntent) -> Unit, modifier: Modifier) {
    val session = uiState.session
    Box(modifier, contentAlignment = Alignment.TopCenter) {
        if (session == null || uiState.loading) {
            val loading = stringResource(R.string.generating)
            LoadingIndicator(Modifier.align(Alignment.Center).semantics { contentDescription = loading })
        } else {
            Board(session, uiState.selected, uiState.conflicts, onIntent)
            // In the room below the board, so the board never moves when a hint comes or goes.
            HintBanner(session.state.pendingHint, Modifier.align(Alignment.BottomCenter))
        }
    }
}

/** The board with its highlights, worked out only when the board, selection, hint or conflicts change. */
@Composable
private fun Board(session: GameSession, selected: Int?, conflicts: Set<Int>, onIntent: (GameIntent) -> Unit) {
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
private val LANDSCAPE_PANEL = 360.dp
private const val MENU_CLOSED_MILLIS = 400L

/** A bug report being written, with the board's [screenshot]. */
private class BugReport(val screenshot: ImageBitmap)
