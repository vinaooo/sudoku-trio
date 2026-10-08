package io.github.vinaooo.sudokutrio.feature.game.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Redo
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.GridOn
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.vinaooo.sudokutrio.domain.model.Hint
import io.github.vinaooo.sudokutrio.domain.model.hintsUsed
import io.github.vinaooo.sudokutrio.domain.model.mistakes
import io.github.vinaooo.sudokutrio.feature.game.GameIntent
import io.github.vinaooo.sudokutrio.feature.game.R
import io.github.vinaooo.vinkit.core.ScoreRecord
import io.github.vinaooo.vinkit.core.formatElapsed
import io.github.vinaooo.vinkit.shell.MenuOption
import io.github.vinaooo.vinkit.shell.ToolbarAction

/** The hint on show: what it is about and what the second tap does. */
@Composable
internal fun HintCard(hint: Hint, modifier: Modifier = Modifier) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        modifier = modifier,
    ) {
        Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Rounded.Lightbulb, contentDescription = null)
            Column {
                Text(hintTitle(hint), style = MaterialTheme.typography.titleSmall)
                Text(hintBody(hint), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

/** What the toolbar's buttons need to know. */
internal data class ToolbarState(
    val notesMode: Boolean,
    val canUndo: Boolean,
    val canRedo: Boolean,
    val hintShown: Boolean,
    val enabled: Boolean,
)

/** Undo, redo, notes and hint, which turns into "apply" while a hint shows. Undo and redo keep their direction. */
@Composable
internal fun toolbarActions(state: ToolbarState, onIntent: (GameIntent) -> Unit): List<ToolbarAction> = listOf(
    ToolbarAction.Button(
        Icons.AutoMirrored.Rounded.Undo,
        stringResource(R.string.undo),
        enabled = state.enabled && state.canUndo,
        keepDirection = true,
    ) { onIntent(GameIntent.Undo) },
    ToolbarAction.Button(
        Icons.AutoMirrored.Rounded.Redo,
        stringResource(R.string.redo),
        enabled = state.enabled && state.canRedo,
        keepDirection = true,
    ) { onIntent(GameIntent.Redo) },
    // Notes mode: a pencil that fills in while digits go in as pencil marks.
    ToolbarAction.Toggle(
        Icons.Outlined.Edit,
        Icons.Rounded.Edit,
        stringResource(R.string.notes),
        checked = state.notesMode,
        enabled = state.enabled,
    ) { onIntent(GameIntent.ToggleNotes) },
    if (state.hintShown) {
        ToolbarAction.Button(Icons.Rounded.Check, stringResource(R.string.apply_hint), enabled = state.enabled) {
            onIntent(GameIntent.Hint)
        }
    } else {
        ToolbarAction.Button(Icons.Rounded.Lightbulb, stringResource(R.string.hint), enabled = state.enabled) {
            onIntent(GameIntent.Hint)
        }
    },
)

/** The new game menu: a new puzzle, or this board again from its givens. */
@Composable
internal fun menuOptions(onIntent: (GameIntent) -> Unit): List<MenuOption> = listOf(
    MenuOption(Icons.Rounded.GridOn, stringResource(R.string.new_game)) { onIntent(GameIntent.NewGame) },
    MenuOption(Icons.Rounded.Refresh, stringResource(R.string.restart_board)) { onIntent(GameIntent.Restart) },
)

/** The win dialog's lines: the score, mistakes and hints show only now, so mistakes stay silent during play. */
@Composable
internal fun winLines(record: ScoreRecord): List<String> = listOf(
    stringResource(R.string.win_score, record.points),
    stringResource(R.string.win_time, formatElapsed(record.elapsedSeconds)),
    stringResource(R.string.win_mistakes, record.mistakes),
    stringResource(R.string.win_hints, record.hintsUsed),
)
