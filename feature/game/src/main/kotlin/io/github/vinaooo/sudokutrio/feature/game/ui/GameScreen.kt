package io.github.vinaooo.sudokutrio.feature.game.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
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
import io.github.vinaooo.sudokutrio.feature.game.SudokuTrioReports
import io.github.vinaooo.sudokutrio.feature.game.board.CellHighlighter
import io.github.vinaooo.sudokutrio.feature.game.board.SudokuBoard
import io.github.vinaooo.sudokutrio.feature.game.gameReport
import io.github.vinaooo.vinkit.bugreport.BugReportDialog
import io.github.vinaooo.vinkit.core.BoardAlignment
import io.github.vinaooo.vinkit.core.Handedness
import io.github.vinaooo.vinkit.core.PhoneViewSide
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
    report?.let {
        BugReportDialog(SudokuTrioReports, it.screenshot, onDone = { report = null }) {
            gameReport(uiState.settings, uiState.appSettings, uiState.session)
        }
    }
}

/**
 * Portrait: the top region (mode and clock, or the hint while one shows, so the board never moves), the board at the
 * top or bottom of its room (Settings), the number pad and the toolbar. Landscape: mode, clock and hint on one side,
 * the board centered at full height, the pad and a vertical toolbar on the preferred hand's side. A left hand mirrors
 * the pad and toolbar. On a tablet, phone view keeps the board at a phone's width on the chosen side.
 */
@Composable
private fun GameContent(
    uiState: GameUiState,
    onIntent: (GameIntent) -> Unit,
    onOpenScores: (() -> Unit)?,
    onOpenSettings: (() -> Unit)?,
) {
    val settings = uiState.appSettings
    val layout = BoardLayout(
        alignment = settings.boardAlignment,
        phoneView = settings.phoneView && LocalConfiguration.current.smallestScreenWidthDp >= TABLET_WIDTH_DP,
        side = settings.phoneViewSide,
    )
    val mirrored = settings.handedness == Handedness.LEFT
    BoxWithConstraints(Modifier.fillMaxSize().safeDrawingPadding()) {
        // Phone view keeps its column (pad under the board) in landscape too.
        if (maxWidth > maxHeight && !layout.phoneView) {
            LandscapeGame(
                uiState,
                onIntent,
                layout,
                mirrored,
                onOpenScores,
                onOpenSettings,
                large =
                maxWidth >= LARGE_LANDSCAPE,
            )
        } else {
            PortraitGame(uiState, onIntent, layout, mirrored, onOpenScores, onOpenSettings)
        }
    }
}

/** Where the board sits in its room: Settings' board position, and phone view's width and side. */
internal data class BoardLayout(val alignment: BoardAlignment, val phoneView: Boolean, val side: PhoneViewSide)

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

/** Holds the mode and clock or, in their place, the hint card: the board below never moves. */
internal val TOP_REGION = 88.dp
internal val SIDE_WIDTH = 200.dp
internal val PAD_WIDTH = 184.dp

/** On a tablet, landscape's pad has room for bigger keys. */
internal val LARGE_LANDSCAPE = 1000.dp
internal val LARGE_PAD_WIDTH = 280.dp
internal val LARGE_KEY_HEIGHT = 72.dp

/** A phone's pad width: on a tablet in portrait the pad and toolbar keep it, centered. */
internal val PORTRAIT_PAD_MAX_WIDTH = 480.dp

/** Phone view's board width: a typical modern phone's (412dp), as in Solo. */
internal val PHONE_WIDTH = 412.dp

/** From this short side (Material's medium window), the screen is a tablet's and phone view applies. */
private const val TABLET_WIDTH_DP = 600
private const val MENU_CLOSED_MILLIS = 400L

/** A bug report being written, with the board's [screenshot]. */
private class BugReport(val screenshot: ImageBitmap)
