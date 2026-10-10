package io.github.vinaooo.sudokutrio.feature.game.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.github.vinaooo.sudokutrio.domain.model.Grid
import io.github.vinaooo.sudokutrio.feature.game.Announcement
import io.github.vinaooo.sudokutrio.feature.game.R
import io.github.vinaooo.vinkit.shell.R as ShellR

@Composable
internal fun announcementText(announcement: Announcement): String = when (announcement) {
    is Announcement.Placed -> stringResource(R.string.a11y_placed, announcement.digit, cellName(announcement.cell))
    is Announcement.Erased -> stringResource(R.string.a11y_erased, cellName(announcement.cell))
    is Announcement.Noted -> stringResource(
        if (announcement.added) R.string.a11y_note_added else R.string.a11y_note_removed,
        announcement.digit,
        cellName(announcement.cell),
    )
    Announcement.Undone -> stringResource(ShellR.string.vinkit_undone)
    Announcement.Redone -> stringResource(ShellR.string.vinkit_redone)
    is Announcement.Hinted -> hintTitle(announcement.hint) + ". " + hintBody(announcement.hint)
}

/** "row 3, column 5", counted from 1. */
@Composable
internal fun cellName(cell: Int): String = stringResource(R.string.a11y_cell, Grid.row(cell) + 1, Grid.column(cell) + 1)
