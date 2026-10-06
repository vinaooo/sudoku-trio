package io.github.vinaooo.sudokutrio.feature.scores

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.vinaooo.sudokutrio.domain.model.GameMode
import io.github.vinaooo.sudokutrio.domain.model.GameStats
import io.github.vinaooo.sudokutrio.domain.model.ScoreRecord
import io.github.vinaooo.sudokutrio.domain.usecase.ObservePlayedModes
import io.github.vinaooo.sudokutrio.domain.usecase.ObserveStats
import io.github.vinaooo.sudokutrio.domain.usecase.ObserveTopScores
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn

data class ScoresUiState(
    val scores: List<ScoreRecord> = emptyList(),
    val stats: GameStats = GameStats(),
    val isLoading: Boolean = true,
    /** The modes played at least once, won or not, one tab each. */
    val modes: List<GameMode> = emptyList(),
    /** The mode whose ranking shows; null before anything was played. */
    val mode: GameMode? = null,
)

@HiltViewModel
class ScoresViewModel @Inject constructor(
    observeTopScores: ObserveTopScores,
    observeStats: ObserveStats,
    observePlayedModes: ObservePlayedModes,
) : ViewModel() {
    private val chosen = MutableStateFlow<GameMode?>(null)

    // The chosen tab, or the first mode played until one is chosen.
    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<ScoresUiState> =
        combine(observePlayedModes(), chosen) { played, choice ->
            val modes = played.sortedWith(compareBy({ it.variant }, { it.difficulty }))
            modes to (choice?.takeIf { it in modes } ?: modes.firstOrNull())
        }.flatMapLatest { (modes, mode) ->
            if (mode == null) {
                flowOf(ScoresUiState(isLoading = false))
            } else {
                combine(observeTopScores(mode), observeStats(mode)) { scores, stats ->
                    ScoresUiState(scores, stats, isLoading = false, modes = modes, mode = mode)
                }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), ScoresUiState())

    fun selectMode(mode: GameMode) {
        chosen.value = mode
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
