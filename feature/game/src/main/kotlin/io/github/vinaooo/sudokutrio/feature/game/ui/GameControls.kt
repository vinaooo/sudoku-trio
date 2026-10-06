package io.github.vinaooo.sudokutrio.feature.game.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Redo
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import io.github.vinaooo.sudokutrio.core.ui.formatElapsed
import io.github.vinaooo.sudokutrio.core.ui.modeName
import io.github.vinaooo.sudokutrio.core.ui.spokenElapsed
import io.github.vinaooo.sudokutrio.domain.model.GameMode
import io.github.vinaooo.sudokutrio.domain.model.Hint
import io.github.vinaooo.sudokutrio.feature.game.GameIntent
import io.github.vinaooo.sudokutrio.feature.game.R

/**
 * The mode and the clock, read by TalkBack as one item, with the Scores and Settings buttons at the end (when given).
 * Only these: the score, mistakes and hints show at the win, so mistakes stay silent during play.
 */
@Composable
internal fun GameTopBar(
    mode: GameMode,
    elapsedSeconds: Long,
    onOpenScores: (() -> Unit)?,
    onOpenSettings: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val name = modeName(mode)
        val time = stringResource(R.string.time)
        val spoken = spokenElapsed(elapsedSeconds)
        Column(Modifier.clearAndSetSemantics { contentDescription = "$name, $time, $spoken" }) {
            Text(name, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                formatElapsed(elapsedSeconds),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.weight(1f))
        onOpenScores?.let {
            IconButton(onClick = it) { Icon(Icons.Rounded.EmojiEvents, stringResource(R.string.open_scores)) }
        }
        onOpenSettings?.let {
            IconButton(onClick = it) { Icon(Icons.Rounded.Settings, stringResource(R.string.open_settings)) }
        }
    }
}

/** The hint on show: what it is about and what the second tap does. It expands from under the top bar. */
@Composable
internal fun HintBanner(hint: Hint?, modifier: Modifier = Modifier) {
    // Keeps the last hint while it folds away.
    var shown by remember { mutableStateOf(hint) }
    if (hint != null) shown = hint
    AnimatedVisibility(
        visible = hint != null,
        modifier = modifier,
        enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(),
        exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut(),
    ) {
        shown?.let { current ->
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
            ) {
                Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Rounded.Lightbulb, contentDescription = null)
                    Column {
                        Text(hintTitle(current), style = MaterialTheme.typography.titleSmall)
                        Text(hintBody(current), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

/**
 * Undo, redo, hint and the new game menu. The hint button turns into "apply" while a hint shows. The toolbar owns
 * whether the menu is open, because it dims the game behind itself meanwhile.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun GameToolbar(
    canUndo: Boolean,
    canRedo: Boolean,
    hintShown: Boolean,
    enabled: Boolean,
    onIntent: (GameIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    var menuOpen by remember { mutableStateOf(false) }
    val dim by animateFloatAsState(
        if (menuOpen) MENU_SCRIM_ALPHA else 0f,
        MaterialTheme.motionScheme.defaultEffectsSpec(),
    )
    // While the menu is open every button but its close button leaves, only along the toolbar.
    val size = MaterialTheme.motionScheme.defaultSpatialSpec<IntSize>()
    val scale = MaterialTheme.motionScheme.defaultSpatialSpec<Float>()
    val fade = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()
    val enter = expandHorizontally(size, Alignment.CenterHorizontally) + scaleIn(scale) + fadeIn(fade)
    val exit = shrinkHorizontally(size, Alignment.CenterHorizontally) + scaleOut(scale) + fadeOut(fade)
    HorizontalFloatingToolbar(
        expanded = true,
        colors = FloatingToolbarDefaults.vibrantFloatingToolbarColors(),
        modifier = modifier.scrimBehind(MaterialTheme.colorScheme.scrim) { dim }
            .keepingEnd(vertical = false, hold = rememberMenuHold(menuOpen)),
    ) {
        AnimatedVisibility(!menuOpen, enter = enter, exit = exit) {
            IconButton(onClick = { onIntent(GameIntent.Undo) }, enabled = enabled && canUndo) {
                Icon(Icons.AutoMirrored.Rounded.Undo, stringResource(R.string.undo))
            }
        }
        AnimatedVisibility(!menuOpen, enter = enter, exit = exit) {
            IconButton(onClick = { onIntent(GameIntent.Redo) }, enabled = enabled && canRedo) {
                Icon(Icons.AutoMirrored.Rounded.Redo, stringResource(R.string.redo))
            }
        }
        AnimatedVisibility(!menuOpen, enter = enter, exit = exit) {
            IconButton(onClick = { onIntent(GameIntent.Hint) }, enabled = enabled) {
                Crossfade(hintShown, animationSpec = MaterialTheme.motionScheme.fastEffectsSpec(), label = "hint") {
                    if (it) {
                        Icon(Icons.Rounded.Check, stringResource(R.string.apply_hint))
                    } else {
                        Icon(Icons.Rounded.Lightbulb, stringResource(R.string.hint))
                    }
                }
            }
        }
        NewGameMenu(menuOpen, { menuOpen = it }, onIntent, vertical = false)
    }
}
