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
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Redo
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalFloatingToolbar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import io.github.vinaooo.sudokutrio.core.ui.formatElapsed
import io.github.vinaooo.sudokutrio.core.ui.modeName
import io.github.vinaooo.sudokutrio.core.ui.spokenElapsed
import io.github.vinaooo.sudokutrio.domain.model.GameMode
import io.github.vinaooo.sudokutrio.domain.model.Hint
import io.github.vinaooo.sudokutrio.feature.game.GameIntent
import io.github.vinaooo.sudokutrio.feature.game.R

/**
 * The mode and the clock, read by TalkBack as one item; [large] in landscape's side column, which has the room. Only
 * these: the score, mistakes and hints show at the win, so mistakes stay silent during play.
 */
@Composable
internal fun ModeAndTime(mode: GameMode, elapsedSeconds: Long, modifier: Modifier = Modifier, large: Boolean = false) {
    val name = modeName(mode)
    val time = stringResource(R.string.time)
    val spoken = spokenElapsed(elapsedSeconds)
    val typography = MaterialTheme.typography
    Column(modifier.clearAndSetSemantics { contentDescription = "$name, $time, $spoken" }) {
        Text(
            name,
            style = if (large) typography.titleSmall else typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            formatElapsed(elapsedSeconds),
            style = if (large) typography.headlineMedium else typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
    }
}

/** The Scores and Settings buttons, each shown only when its screen exists. */
@Composable
internal fun NavigationButtons(onOpenScores: (() -> Unit)?, onOpenSettings: (() -> Unit)?) {
    onOpenScores?.let {
        IconButton(onClick = it) { Icon(Icons.Rounded.EmojiEvents, stringResource(R.string.open_scores)) }
    }
    onOpenSettings?.let {
        IconButton(onClick = it) { Icon(Icons.Rounded.Settings, stringResource(R.string.open_settings)) }
    }
}

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

/**
 * Undo, redo, hint and the new game menu, in a horizontal toolbar or, in landscape, a [vertical] one. The hint button
 * turns into "apply" while a hint shows. [mirrored] (left hand) lays it out right to left, so the menu sits at the
 * thumb's end and its pills grow the other way; the icons themselves keep their direction. The toolbar owns whether
 * the menu is open, because it dims the game behind itself meanwhile.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun GameToolbar(
    state: ToolbarState,
    onIntent: (GameIntent) -> Unit,
    modifier: Modifier = Modifier,
    vertical: Boolean = false,
    mirrored: Boolean = false,
) {
    var menuOpen by remember { mutableStateOf(false) }
    val dim by animateFloatAsState(
        if (menuOpen) MENU_SCRIM_ALPHA else 0f,
        MaterialTheme.motionScheme.defaultEffectsSpec(),
    )
    val toolbarModifier = modifier.scrimBehind(MaterialTheme.colorScheme.scrim) { dim }
        .keepingEnd(vertical = vertical, hold = rememberMenuHold(menuOpen))
    val actions: @Composable () -> Unit = { ToolbarActions(state, onIntent, vertical, menuOpen) { menuOpen = it } }
    CompositionLocalProvider(LocalLayoutDirection provides if (mirrored) LayoutDirection.Rtl else LayoutDirection.Ltr) {
        if (vertical) {
            VerticalFloatingToolbar(
                expanded = true,
                colors = FloatingToolbarDefaults.vibrantFloatingToolbarColors(),
                modifier = toolbarModifier,
            ) { actions() }
        } else {
            HorizontalFloatingToolbar(
                expanded = true,
                colors = FloatingToolbarDefaults.vibrantFloatingToolbarColors(),
                modifier = toolbarModifier,
            ) { actions() }
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

/** While the menu is open every button but its close button leaves, only along the toolbar. */
@Composable
private fun ToolbarActions(
    state: ToolbarState,
    onIntent: (GameIntent) -> Unit,
    vertical: Boolean,
    menuOpen: Boolean,
    onMenuOpenChange: (Boolean) -> Unit,
) {
    val size = MaterialTheme.motionScheme.defaultSpatialSpec<IntSize>()
    val scale = MaterialTheme.motionScheme.defaultSpatialSpec<Float>()
    val fade = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()
    val enter = if (vertical) {
        expandVertically(size, Alignment.CenterVertically)
    } else {
        expandHorizontally(size, Alignment.CenterHorizontally)
    } + scaleIn(scale) + fadeIn(fade)
    val exit = if (vertical) {
        shrinkVertically(size, Alignment.CenterVertically)
    } else {
        shrinkHorizontally(size, Alignment.CenterHorizontally)
    } + scaleOut(scale) + fadeOut(fade)
    AnimatedVisibility(!menuOpen, enter = enter, exit = exit) {
        IconButton(onClick = { onIntent(GameIntent.Undo) }, enabled = state.enabled && state.canUndo) {
            LtrIcon { Icon(Icons.AutoMirrored.Rounded.Undo, stringResource(R.string.undo)) }
        }
    }
    AnimatedVisibility(!menuOpen, enter = enter, exit = exit) {
        IconButton(onClick = { onIntent(GameIntent.Redo) }, enabled = state.enabled && state.canRedo) {
            LtrIcon { Icon(Icons.AutoMirrored.Rounded.Redo, stringResource(R.string.redo)) }
        }
    }
    AnimatedVisibility(!menuOpen, enter = enter, exit = exit) { NotesToggle(state, onIntent) }
    AnimatedVisibility(!menuOpen, enter = enter, exit = exit) {
        IconButton(onClick = { onIntent(GameIntent.Hint) }, enabled = state.enabled) {
            Crossfade(state.hintShown, animationSpec = MaterialTheme.motionScheme.fastEffectsSpec(), label = "hint") {
                if (it) {
                    Icon(Icons.Rounded.Check, stringResource(R.string.apply_hint))
                } else {
                    Icon(Icons.Rounded.Lightbulb, stringResource(R.string.hint))
                }
            }
        }
    }
    NewGameMenu(menuOpen, onMenuOpenChange, onIntent, vertical)
}

/** Notes mode: a pencil that fills in while digits go in as pencil marks. TalkBack reads it as an on/off switch. */
@Composable
private fun NotesToggle(state: ToolbarState, onIntent: (GameIntent) -> Unit) {
    IconToggleButton(
        checked = state.notesMode,
        onCheckedChange = { onIntent(GameIntent.ToggleNotes) },
        enabled = state.enabled,
        colors = IconButtonDefaults.iconToggleButtonColors(
            checkedContainerColor = MaterialTheme.colorScheme.onPrimaryContainer,
            checkedContentColor = MaterialTheme.colorScheme.primaryContainer,
        ),
    ) {
        Icon(if (state.notesMode) Icons.Rounded.Edit else Icons.Outlined.Edit, stringResource(R.string.notes))
    }
}

/** Draws [icon] left to right whatever the toolbar's direction, so undo and redo keep pointing their own way. */
@Composable
private fun LtrIcon(icon: @Composable () -> Unit) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr, content = icon)
}
