package io.github.vinaooo.sudokutrio.feature.game.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.vinaooo.sudokutrio.feature.game.GameIntent
import io.github.vinaooo.sudokutrio.feature.game.R

/**
 * The number pad, as chosen with the user: `1 2 3 4 5` / `6 7 8 9 Erase` / a full-width Notes toggle; in landscape
 * ([grid]) three columns, `1 2 3` / `4 5 6` / `7 8 9` / Notes and Erase. Erase sits on the thumb's side: the right,
 * or the left when [mirrored] (left hand). A digit already on the board nine times ([completed]) dims but stays usable.
 */
@Composable
internal fun NumberPad(
    completed: Set<Int>,
    notesMode: Boolean,
    enabled: Boolean,
    onIntent: (GameIntent) -> Unit,
    modifier: Modifier = Modifier,
    grid: Boolean = false,
    mirrored: Boolean = false,
) {
    val digit: @Composable RowScope.(Int) -> Unit = { DigitKey(it, it in completed, enabled, onIntent) }
    val erase: @Composable RowScope.() -> Unit = { EraseKey(enabled, onIntent) }
    Column(modifier.padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(KEY_GAP)) {
        if (grid) {
            for (row in 0 until GRID_ROWS) {
                Row(horizontalArrangement = Arrangement.spacedBy(KEY_GAP)) {
                    for (column in 1..GRID_COLUMNS) digit(row * GRID_COLUMNS + column)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(KEY_GAP)) {
                if (mirrored) erase()
                NotesKey(notesMode, enabled, onIntent, Modifier.weight(2f).height(KEY_HEIGHT))
                if (!mirrored) erase()
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(KEY_GAP)) { for (d in 1..5) digit(d) }
            Row(horizontalArrangement = Arrangement.spacedBy(KEY_GAP)) {
                if (mirrored) erase()
                for (d in 6..9) digit(d)
                if (!mirrored) erase()
            }
            NotesKey(notesMode, enabled, onIntent, Modifier.fillMaxWidth().height(NOTES_HEIGHT))
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun RowScope.EraseKey(enabled: Boolean, onIntent: (GameIntent) -> Unit) {
    FilledTonalButton(
        onClick = { onIntent(GameIntent.Erase) },
        enabled = enabled,
        shapes = ButtonDefaults.shapes(),
        modifier = Modifier.weight(1f).height(KEY_HEIGHT),
    ) { Icon(Icons.AutoMirrored.Rounded.Backspace, stringResource(R.string.erase)) }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun NotesKey(notesMode: Boolean, enabled: Boolean, onIntent: (GameIntent) -> Unit, modifier: Modifier) {
    ToggleButton(
        checked = notesMode,
        onCheckedChange = { onIntent(GameIntent.ToggleNotes) },
        enabled = enabled,
        modifier = modifier,
    ) {
        Icon(Icons.Rounded.Edit, contentDescription = null)
        Text(stringResource(R.string.notes), modifier = Modifier.padding(start = 8.dp))
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun RowScope.DigitKey(digit: Int, completed: Boolean, enabled: Boolean, onIntent: (GameIntent) -> Unit) {
    val description = if (completed) stringResource(R.string.digit_complete, digit) else digit.toString()
    FilledTonalButton(
        onClick = { onIntent(GameIntent.Digit(digit)) },
        enabled = enabled,
        shapes = ButtonDefaults.shapes(),
        modifier = Modifier.weight(1f).height(KEY_HEIGHT).semantics { contentDescription = description },
    ) {
        Text(
            digit.toString(),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.alpha(if (completed) COMPLETED_ALPHA else 1f),
        )
    }
}

private val KEY_GAP = 6.dp
private val KEY_HEIGHT = 56.dp
private val NOTES_HEIGHT = 48.dp
private const val GRID_ROWS = 3
private const val GRID_COLUMNS = 3

/** Material's disabled-content alpha, though the key still works. */
private const val COMPLETED_ALPHA = 0.38f
