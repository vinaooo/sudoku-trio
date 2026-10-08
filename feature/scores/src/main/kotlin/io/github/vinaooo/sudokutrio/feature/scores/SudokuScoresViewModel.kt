package io.github.vinaooo.sudokutrio.feature.scores

import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.vinaooo.sudokutrio.domain.model.Difficulty
import io.github.vinaooo.sudokutrio.domain.model.GameMode
import io.github.vinaooo.sudokutrio.domain.model.Variant
import io.github.vinaooo.sudokutrio.domain.model.gameModeOf
import io.github.vinaooo.sudokutrio.domain.model.key
import io.github.vinaooo.sudokutrio.domain.model.ranking
import io.github.vinaooo.vinkit.core.ScoreRepository
import io.github.vinaooo.vinkit.core.StatsRepository
import io.github.vinaooo.vinkit.scores.ScoresViewModel
import javax.inject.Inject

/** vinkit's Scores: a tab per variant played, a section per difficulty, each ranked by its scoring. */
@HiltViewModel
class SudokuScoresViewModel @Inject constructor(scores: ScoreRepository, stats: StatsRepository) :
    ScoresViewModel(
        scores,
        stats,
        modes.map { it.key },
        groupOf = { gameModeOf(it)!!.variant.name },
        rankingFor = { gameModeOf(it)!!.ranking() },
    )

/** Every mode, by variant then difficulty. */
private val modes = Variant.entries.flatMap { variant -> Difficulty.entries.map { GameMode(variant, it) } }
