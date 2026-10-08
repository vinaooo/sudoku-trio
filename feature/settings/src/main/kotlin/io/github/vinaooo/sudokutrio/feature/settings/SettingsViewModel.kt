package io.github.vinaooo.sudokutrio.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.vinaooo.sudokutrio.domain.model.Difficulty
import io.github.vinaooo.sudokutrio.domain.model.Settings
import io.github.vinaooo.sudokutrio.domain.model.Variant
import io.github.vinaooo.sudokutrio.domain.repository.SettingsRepository
import io.github.vinaooo.sudokutrio.domain.usecase.ResumeGame
import io.github.vinaooo.vinkit.core.AppSettings
import io.github.vinaooo.vinkit.core.AppSettingsRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** A change to Sudoku Trio's own settings: a new mode starts a new game, after a confirmation if one is in progress. */
sealed interface SettingsChange {
    fun applyTo(settings: Settings): Settings

    data class VariantChanged(val value: Variant) : SettingsChange {
        override fun applyTo(settings: Settings) = settings.copy(mode = settings.mode.copy(variant = value))
    }

    data class DifficultyChanged(val value: Difficulty) : SettingsChange {
        override fun applyTo(settings: Settings) = settings.copy(mode = settings.mode.copy(difficulty = value))
    }
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: SettingsRepository,
    private val appRepository: AppSettingsRepository,
    private val resumeGame: ResumeGame,
) : ViewModel() {

    val settings: StateFlow<Settings> = repository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), Settings())

    val appSettings: StateFlow<AppSettings> = appRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), AppSettings())

    private val pending = MutableStateFlow<SettingsChange?>(null)

    /**
     * A variant or difficulty change waiting for the player to confirm abandoning the game in progress, which the
     * switch would end: "This will start a new game. The current game counts as a loss."
     */
    val pendingChange: StateFlow<SettingsChange?> = pending.asStateFlow()

    fun onChange(change: SettingsChange) {
        viewModelScope.launch {
            if (startsNewGame(change) && resumeGame()?.isInProgress == true) {
                pending.value = change
            } else {
                repository.update(change::applyTo)
            }
        }
    }

    fun onAppChange(transform: (AppSettings) -> AppSettings) {
        viewModelScope.launch { appRepository.update(transform) }
    }

    fun confirmChange() {
        val change = pending.value ?: return
        pending.value = null
        viewModelScope.launch { repository.update(change::applyTo) }
    }

    fun dismissChange() {
        pending.value = null
    }

    /** A different variant or difficulty is a different mode, which starts a new game. */
    private fun startsNewGame(change: SettingsChange): Boolean =
        change.applyTo(settings.value).mode != settings.value.mode

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
