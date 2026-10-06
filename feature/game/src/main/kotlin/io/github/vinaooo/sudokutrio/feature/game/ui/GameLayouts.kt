package io.github.vinaooo.sudokutrio.feature.game.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.vinaooo.sudokutrio.domain.model.BoardAlignment
import io.github.vinaooo.sudokutrio.domain.model.PhoneViewSide
import io.github.vinaooo.sudokutrio.feature.game.GameIntent
import io.github.vinaooo.sudokutrio.feature.game.GameUiState
import io.github.vinaooo.sudokutrio.feature.game.R
import io.github.vinaooo.sudokutrio.feature.game.board.completedDigits

@Composable
internal fun PortraitGame(
    uiState: GameUiState,
    onIntent: (GameIntent) -> Unit,
    layout: BoardLayout,
    mirrored: Boolean,
    onOpenScores: (() -> Unit)?,
    onOpenSettings: (() -> Unit)?,
) {
    val hint = uiState.session?.state?.pendingHint
    Column(Modifier.fillMaxSize()) {
        // The hint takes the mode and clock's place; Scores and Settings stay reachable beside it.
        Row(
            Modifier.fillMaxWidth().height(TOP_REGION).padding(end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AnimatedContent(
                targetState = hint,
                contentKey = { it != null },
                label = "top region",
                modifier = Modifier.weight(1f),
            ) { shown ->
                if (shown != null) {
                    HintCard(shown, Modifier.fillMaxWidth().padding(start = 12.dp, end = 4.dp))
                } else {
                    ModeAndTime(uiState.mode(), uiState.elapsedSeconds(), Modifier.padding(start = 16.dp))
                }
            }
            NavigationButtons(onOpenScores, onOpenSettings)
        }
        BoardArea(
            uiState,
            onIntent,
            layout,
            Modifier.fillMaxWidth().weight(1f).padding(horizontal = 8.dp, vertical = 4.dp),
        )
        val controls = remember(uiState) { uiState.controls() }
        NumberPad(controls.completed, uiState.notesMode, controls.toolbar.enabled, onIntent, mirrored = mirrored)
        GameToolbar(
            controls.toolbar,
            onIntent,
            Modifier.align(Alignment.CenterHorizontally).padding(vertical = 12.dp),
            mirrored = mirrored,
        )
    }
}

@Composable
internal fun LandscapeGame(
    uiState: GameUiState,
    onIntent: (GameIntent) -> Unit,
    layout: BoardLayout,
    mirrored: Boolean,
    onOpenScores: (() -> Unit)?,
    onOpenSettings: (() -> Unit)?,
) {
    val info: @Composable () -> Unit = {
        Column(
            Modifier.width(SIDE_WIDTH).fillMaxHeight().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ModeAndTime(uiState.mode(), uiState.elapsedSeconds(), large = true)
            uiState.session?.state?.pendingHint?.let { HintCard(it, Modifier.fillMaxWidth()) }
            Spacer(Modifier.weight(1f))
            Row { NavigationButtons(onOpenScores, onOpenSettings) }
        }
    }
    val controls: @Composable () -> Unit = {
        val state = remember(uiState) { uiState.controls() }
        Row(verticalAlignment = Alignment.CenterVertically) {
            val toolbar: @Composable () -> Unit = {
                GameToolbar(
                    state.toolbar,
                    onIntent,
                    Modifier.padding(horizontal = 8.dp),
                    vertical = true,
                    mirrored = mirrored,
                )
            }
            if (mirrored) toolbar()
            NumberPad(
                state.completed,
                uiState.notesMode,
                state.toolbar.enabled,
                onIntent,
                Modifier.width(PAD_WIDTH),
                grid = true,
                mirrored = mirrored,
            )
            if (!mirrored) toolbar()
        }
    }
    CenteredRow(
        modifier = Modifier.fillMaxSize(),
        start = if (mirrored) controls else info,
        center = {
            BoardArea(
                uiState,
                onIntent,
                layout.copy(alignment = BoardAlignment.TOP),
                Modifier.fillMaxSize().padding(8.dp),
                centered = true,
            )
        },
        end = if (mirrored) info else controls,
    )
}

/** What the pad and toolbar show, worked out once per state. */
private data class Controls(val completed: Set<Int>, val toolbar: ToolbarState)

private fun GameUiState.controls(): Controls {
    val session = session
    val ready = session != null && !loading && !session.state.isWon
    return Controls(
        completed = session?.state?.board?.values?.let(::completedDigits).orEmpty(),
        toolbar = ToolbarState(
            canUndo = session?.canUndo == true,
            canRedo = session?.canRedo == true,
            hintShown = session?.state?.pendingHint != null,
            enabled = ready,
        ),
    )
}

private fun GameUiState.mode() = session?.state?.mode ?: settings.mode

private fun GameUiState.elapsedSeconds() = session?.state?.elapsedSeconds ?: 0

/**
 * The board as the largest square that fits, at the top or bottom of its room (centered in landscape), or with phone
 * view at a phone's width on the chosen side; or the loading indicator while a puzzle is made.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun BoardArea(
    uiState: GameUiState,
    onIntent: (GameIntent) -> Unit,
    layout: BoardLayout,
    modifier: Modifier,
    centered: Boolean = false,
) {
    val session = uiState.session
    BoxWithConstraints(modifier) {
        if (session == null || uiState.loading) {
            val loading = stringResource(R.string.generating)
            LoadingIndicator(Modifier.align(Alignment.Center).semantics { contentDescription = loading })
        } else {
            val side = minOf(maxWidth, maxHeight, if (layout.phoneView) PHONE_WIDTH else maxWidth)
            val vertical = when {
                centered -> Alignment.CenterVertically
                layout.alignment == BoardAlignment.BOTTOM -> Alignment.Bottom
                else -> Alignment.Top
            }
            val horizontal = when {
                !layout.phoneView -> Alignment.CenterHorizontally
                layout.side == PhoneViewSide.LEFT -> Alignment.Start
                layout.side == PhoneViewSide.RIGHT -> Alignment.End
                else -> Alignment.CenterHorizontally
            }
            Box(Modifier.fillMaxSize(), contentAlignment = BiasAlignment(horizontal.bias(), vertical.bias())) {
                Box(Modifier.size(side)) { Board(session, uiState.selected, uiState.conflicts, onIntent) }
            }
        }
    }
}

private fun Alignment.Horizontal.bias(): Float = when (this) {
    Alignment.Start -> -1f
    Alignment.End -> 1f
    else -> 0f
}

private fun Alignment.Vertical.bias(): Float = when (this) {
    Alignment.Top -> -1f
    Alignment.Bottom -> 1f
    else -> 0f
}
