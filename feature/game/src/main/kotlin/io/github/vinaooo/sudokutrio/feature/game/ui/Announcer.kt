package io.github.vinaooo.sudokutrio.feature.game.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.vinaooo.sudokutrio.domain.model.Grid
import io.github.vinaooo.sudokutrio.feature.game.Announced
import io.github.vinaooo.sudokutrio.feature.game.Announcement
import io.github.vinaooo.sudokutrio.feature.game.R

/**
 * Speaks each [announced] action through TalkBack, from an invisible live region: a live region is read whenever
 * its text changes. (`View.announceForAccessibility` is deprecated.) Keep it out of a `Surface`'s direct children:
 * the Surface would stretch it over the whole screen.
 */
@Composable
internal fun Announcer(announced: Announced?, modifier: Modifier = Modifier) {
    val text = announced?.let { announcementText(it.announcement) }.orEmpty()
    // Two identical announcements in a row (two undos) would leave the text unchanged and the second unspoken,
    // so every other one ends with a character that isn't read aloud.
    val spoken = if (announced != null && announced.sequence % 2 == 1) text + ZERO_WIDTH_SPACE else text
    Box(
        modifier = modifier
            .size(1.dp)
            .semantics {
                liveRegion = LiveRegionMode.Polite
                contentDescription = spoken
            },
    )
}

@Composable
internal fun announcementText(announcement: Announcement): String = when (announcement) {
    is Announcement.Placed -> stringResource(R.string.a11y_placed, announcement.digit, cellName(announcement.cell))
    is Announcement.Erased -> stringResource(R.string.a11y_erased, cellName(announcement.cell))
    is Announcement.Noted -> stringResource(
        if (announcement.added) R.string.a11y_note_added else R.string.a11y_note_removed,
        announcement.digit,
        cellName(announcement.cell),
    )
    Announcement.Undone -> stringResource(R.string.a11y_undone)
    Announcement.Redone -> stringResource(R.string.a11y_redone)
    is Announcement.Hinted -> hintTitle(announcement.hint) + ". " + hintBody(announcement.hint)
}

/** "row 3, column 5", counted from 1. */
@Composable
internal fun cellName(cell: Int): String = stringResource(R.string.a11y_cell, Grid.row(cell) + 1, Grid.column(cell) + 1)

private const val ZERO_WIDTH_SPACE = "​"
