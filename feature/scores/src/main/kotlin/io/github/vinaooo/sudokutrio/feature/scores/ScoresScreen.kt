package io.github.vinaooo.sudokutrio.feature.scores

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.vinaooo.sudokutrio.core.ui.label
import io.github.vinaooo.sudokutrio.domain.model.Variant
import io.github.vinaooo.sudokutrio.domain.model.gameModeOf
import io.github.vinaooo.sudokutrio.domain.model.hintsUsed
import io.github.vinaooo.sudokutrio.domain.model.mistakes
import io.github.vinaooo.vinkit.core.ScoreRecord
import io.github.vinaooo.vinkit.scores.ScoresScreen as VinkitScoresScreen
import io.github.vinaooo.vinkit.scores.ScoresUiState

@Composable
fun ScoresRoute(onBack: () -> Unit, modifier: Modifier = Modifier, viewModel: SudokuScoresViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ScoresScreen(uiState, onBack, modifier, viewModel::selectGroup)
}

/** vinkit's Scores screen in Sudoku Trio's words: a tab per variant, a section per difficulty, mistakes and hints. */
@Composable
fun ScoresScreen(
    uiState: ScoresUiState,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    onSelectVariant: (String) -> Unit = {},
) {
    VinkitScoresScreen(
        uiState = uiState,
        onBack = onBack,
        modeName = { stringResource(gameModeOf(it)!!.difficulty.label) },
        modifier = modifier,
        groupName = { stringResource(enumValueOf<Variant>(it).label) },
        onSelectGroup = onSelectVariant,
        details = { details(it) },
    )
}

/** "No mistakes · 2 hints": Portuguese plurals would print "0 erro", so zero has its own words. */
@Composable
private fun details(record: ScoreRecord): String {
    val mistakes = if (record.mistakes == 0) {
        stringResource(R.string.no_mistakes)
    } else {
        pluralStringResource(R.plurals.mistakes, record.mistakes, record.mistakes)
    }
    val hints = if (record.hintsUsed == 0) {
        stringResource(R.string.no_hints)
    } else {
        pluralStringResource(R.plurals.hints, record.hintsUsed, record.hintsUsed)
    }
    return stringResource(R.string.score_details, mistakes, hints)
}
