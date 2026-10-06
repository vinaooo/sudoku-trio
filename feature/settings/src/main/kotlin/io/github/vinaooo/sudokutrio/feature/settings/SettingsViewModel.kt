package io.github.vinaooo.sudokutrio.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.vinaooo.sudokutrio.domain.model.BoardAlignment
import io.github.vinaooo.sudokutrio.domain.model.Difficulty
import io.github.vinaooo.sudokutrio.domain.model.Handedness
import io.github.vinaooo.sudokutrio.domain.model.PhoneViewSide
import io.github.vinaooo.sudokutrio.domain.model.Settings
import io.github.vinaooo.sudokutrio.domain.model.ThemeColor
import io.github.vinaooo.sudokutrio.domain.model.ThemeMode
import io.github.vinaooo.sudokutrio.domain.model.Variant
import io.github.vinaooo.sudokutrio.domain.repository.SettingsRepository
import io.github.vinaooo.sudokutrio.domain.usecase.ResumeGame
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface SettingsChange {
    fun applyTo(settings: Settings): Settings

    data class VariantChanged(val value: Variant) : SettingsChange {
        override fun applyTo(settings: Settings) = settings.copy(mode = settings.mode.copy(variant = value))
    }

    data class DifficultyChanged(val value: Difficulty) : SettingsChange {
        override fun applyTo(settings: Settings) = settings.copy(mode = settings.mode.copy(difficulty = value))
    }

    data class ThemeModeChanged(val value: ThemeMode) : SettingsChange {
        override fun applyTo(settings: Settings) = settings.copy(themeMode = value)
    }

    data class DynamicColorChanged(val value: Boolean) : SettingsChange {
        override fun applyTo(settings: Settings) = settings.copy(dynamicColor = value)
    }

    data class ThemeColorChanged(val value: ThemeColor) : SettingsChange {
        override fun applyTo(settings: Settings) = settings.copy(themeColor = value)
    }

    data class SoundChanged(val value: Boolean) : SettingsChange {
        override fun applyTo(settings: Settings) = settings.copy(soundEnabled = value)
    }

    data class HapticsChanged(val value: Boolean) : SettingsChange {
        override fun applyTo(settings: Settings) = settings.copy(hapticsEnabled = value)
    }

    data class HandednessChanged(val value: Handedness) : SettingsChange {
        override fun applyTo(settings: Settings) = settings.copy(handedness = value)
    }

    data class BoardAlignmentChanged(val value: BoardAlignment) : SettingsChange {
        override fun applyTo(settings: Settings) = settings.copy(boardAlignment = value)
    }

    data class PhoneViewChanged(val value: Boolean) : SettingsChange {
        override fun applyTo(settings: Settings) = settings.copy(phoneView = value)
    }

    data class PhoneViewSideChanged(val value: PhoneViewSide) : SettingsChange {
        override fun applyTo(settings: Settings) = settings.copy(phoneViewSide = value)
    }
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: SettingsRepository,
    private val resumeGame: ResumeGame,
) : ViewModel() {

    val settings: StateFlow<Settings> = repository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), Settings())

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
