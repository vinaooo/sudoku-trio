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
 * The number pad, as chosen with the user: `1 2 3 4 5` / `6 7 8 9 Erase` / a full-width Notes toggle. A digit already
 * on the board nine times ([completed]) dims but stays usable.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun NumberPad(
    completed: Set<Int>,
    notesMode: Boolean,
    enabled: Boolean,
    onIntent: (GameIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(KEY_GAP)) {
        Row(horizontalArrangement = Arrangement.spacedBy(KEY_GAP)) {
            for (digit in 1..5) DigitKey(digit, digit in completed, enabled, onIntent)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(KEY_GAP)) {
            for (digit in 6..9) DigitKey(digit, digit in completed, enabled, onIntent)
            FilledTonalButton(
                onClick = { onIntent(GameIntent.Erase) },
                enabled = enabled,
                shapes = androidx.compose.material3.ButtonDefaults.shapes(),
                modifier = Modifier.weight(1f).height(KEY_HEIGHT),
            ) { Icon(Icons.AutoMirrored.Rounded.Backspace, stringResource(R.string.erase)) }
        }
        ToggleButton(
            checked = notesMode,
            onCheckedChange = { onIntent(GameIntent.ToggleNotes) },
            enabled = enabled,
            modifier = Modifier.fillMaxWidth().height(NOTES_HEIGHT),
        ) {
            Icon(Icons.Rounded.Edit, contentDescription = null)
            Text(stringResource(R.string.notes), modifier = Modifier.padding(start = 8.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun RowScope.DigitKey(digit: Int, completed: Boolean, enabled: Boolean, onIntent: (GameIntent) -> Unit) {
    val description = if (completed) stringResource(R.string.digit_complete, digit) else digit.toString()
    FilledTonalButton(
        onClick = { onIntent(GameIntent.Digit(digit)) },
        enabled = enabled,
        shapes = androidx.compose.material3.ButtonDefaults.shapes(),
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

/** Material's disabled-content alpha, though the key still works. */
private const val COMPLETED_ALPHA = 0.38f
