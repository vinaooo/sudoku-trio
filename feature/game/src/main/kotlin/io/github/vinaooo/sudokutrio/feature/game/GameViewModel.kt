package io.github.vinaooo.sudokutrio.feature.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.vinaooo.sudokutrio.domain.model.Achievement
import io.github.vinaooo.sudokutrio.domain.model.GameMode
import io.github.vinaooo.sudokutrio.domain.model.Hint
import io.github.vinaooo.sudokutrio.domain.model.Move
import io.github.vinaooo.sudokutrio.domain.model.badges
import io.github.vinaooo.sudokutrio.domain.repository.SettingsRepository
import io.github.vinaooo.sudokutrio.domain.rules.ConflictFinder
import io.github.vinaooo.sudokutrio.domain.rules.GameEngine
import io.github.vinaooo.sudokutrio.domain.session.GameSession
import io.github.vinaooo.sudokutrio.domain.usecase.FinishGame
import io.github.vinaooo.sudokutrio.domain.usecase.PreparePuzzle
import io.github.vinaooo.sudokutrio.domain.usecase.PreparedPuzzle
import io.github.vinaooo.sudokutrio.domain.usecase.RecordAchievements
import io.github.vinaooo.sudokutrio.domain.usecase.RestartGame
import io.github.vinaooo.sudokutrio.domain.usecase.ResumeGame
import io.github.vinaooo.sudokutrio.domain.usecase.SaveGame
import io.github.vinaooo.sudokutrio.domain.usecase.StartNewGame
import io.github.vinaooo.vinkit.core.AchievementRepository
import io.github.vinaooo.vinkit.core.AppSettingsRepository
import io.github.vinaooo.vinkit.shell.FeedbackEvent
import io.github.vinaooo.vinkit.shell.GameFeedback
import io.github.vinaooo.vinkit.shell.Ticker
import io.github.vinaooo.vinkit.shell.give
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Suppress("LongParameterList", "TooManyFunctions") // Each collaborator is one small, separately tested responsibility.
@HiltViewModel
class GameViewModel @Inject constructor(
    private val startNewGame: StartNewGame,
    private val preparePuzzle: PreparePuzzle,
    private val restartGame: RestartGame,
    private val resumeGame: ResumeGame,
    private val saveGame: SaveGame,
    private val finishGame: FinishGame,
    private val recordAchievements: RecordAchievements,
    achievements: AchievementRepository,
    private val settingsRepository: SettingsRepository,
    private val appSettingsRepository: AppSettingsRepository,
    private val engine: GameEngine,
    private val conflicts: ConflictFinder,
    private val feedback: GameFeedback,
    @SearchDispatcher private val searchDispatcher: CoroutineDispatcher,
) : ViewModel() {

    private val state = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = state.asStateFlow()

    /** Runs while the screen is visible; it charges a second only while a game is on the board and not won. */
    private val clock = Ticker(viewModelScope, CLOCK_TICK_MILLIS) {
        // Reads the latest session on every tick, so moves between ticks are kept.
        val current = state.value
        val session = current.session?.takeUnless { current.loading || it.state.isWon } ?: return@Ticker
        state.update { it.copy(session = session.tick(session.state.elapsedSeconds + 1, engine)) }
    }
    private var generation: Job? = null

    /** The next puzzle, made in the background while this one is played, so New game starts at once. */
    private var next: Deferred<PreparedPuzzle>? = null
    private var nextMode: GameMode? = null
    private var hintSearch: Job? = null

    init {
        viewModelScope.launch {
            settingsRepository.settings.collect { settings -> state.update { it.copy(settings = settings) } }
        }
        viewModelScope.launch {
            appSettingsRepository.settings.collect { settings -> state.update { it.copy(appSettings = settings) } }
        }
        viewModelScope.launch {
            // Badges unlocked from now on are shown; the ones already earned when the screen opened are not.
            var known: Set<Achievement>? = null
            achievements.progress.map { it.badges }.distinctUntilChanged().collect { now ->
                val new = known?.let { now - it }.orEmpty()
                known = now
                if (new.isNotEmpty()) state.update { it.copy(earned = it.earned + new) }
            }
        }
        generation = viewModelScope.launch {
            val resumed = resumeGame()
            show(resumed ?: startNewGame(settingsRepository.settings.first().mode))
            prepareNext(settingsRepository.settings.first().mode)
        }
        viewModelScope.launch {
            // A mode changed in Settings (confirmed there) starts a new game in it.
            settingsRepository.settings.map { it.mode }.distinctUntilChanged().drop(1)
                .collect { mode -> newGame(mode) }
        }
    }

    fun onIntent(intent: GameIntent) {
        when (intent) {
            is GameIntent.SelectCell -> state.update { it.copy(selected = intent.cell) }
            is GameIntent.Digit -> withSelected { cell ->
                if (state.value.notesMode) Move.ToggleNote(cell, intent.digit) else Move.Place(cell, intent.digit)
            }
            GameIntent.Erase -> withSelected { Move.Erase(it) }
            GameIntent.ToggleNotes -> state.update { it.copy(notesMode = !it.notesMode) }
            GameIntent.Undo -> step(GameSession::undo, Announcement.Undone)
            GameIntent.Redo -> step(GameSession::redo, Announcement.Redone)
            GameIntent.Hint -> hint()
            GameIntent.NewGame -> newGame()
            GameIntent.Restart -> restart()
            GameIntent.BadgesShown -> state.update { it.copy(earned = emptyList()) }
            GameIntent.Resume -> clock.start()
            GameIntent.Pause -> pause()
        }
    }

    private inline fun withSelected(move: (Int) -> Move) {
        val cell = state.value.selected ?: return
        play(move(cell))
    }

    private fun play(move: Move) {
        val session = playable() ?: return
        val next = session.play(move, engine)
        if (next == null) {
            feedback.give(FeedbackEvent.REJECTED, state.value.appSettings)
            return
        }
        onPlayed(next)
        announce(announcementFor(move, session.state, next.state))
    }

    /** The session moves may be played on: none while a puzzle is generated or after the win. */
    private fun playable(): GameSession? = state.value.session?.takeUnless { state.value.loading || it.state.isWon }

    /** Shows [next], saves it and plays its feedback; a win is recorded instead. A wrong digit sounds like any move. */
    private fun onPlayed(next: GameSession) {
        hintSearch?.cancel()
        show(next)
        if (next.state.isWon) {
            feedback.give(FeedbackEvent.WIN, state.value.appSettings)
            viewModelScope.launch {
                val record = finishGame(next)
                state.update { it.copy(winRecord = record) }
            }
        } else {
            feedback.give(FeedbackEvent.MOVE, state.value.appSettings)
            viewModelScope.launch {
                saveGame(next)
                recordAchievements.played()
            }
        }
    }

    private fun step(move: (GameSession) -> GameSession?, announcement: Announcement) {
        val next = playable()?.let(move) ?: return
        onPlayed(next)
        announce(announcement)
    }

    /** First tap: find the hint off the main thread, then show it on its cell. Second tap: carry it out. */
    private fun hint() {
        val session = playable() ?: return
        if (session.state.pendingHint != null) {
            play(Move.ApplyHint)
            return
        }
        if (hintSearch?.isActive == true) return
        hintSearch = viewModelScope.launch {
            val found = withContext(searchDispatcher) { session.play(Move.RevealHint, engine) } ?: return@launch
            // Moves, undo, redo and new games cancel this search; only the clock can have ticked meanwhile, so the
            // hint goes on the latest session, charged for the seconds that passed.
            val latest = state.value.session?.takeUnless { state.value.loading } ?: return@launch
            if (latest.state.board !== session.state.board || latest.state.pendingHint != null) return@launch
            val shown = found.tick(latest.state.elapsedSeconds, engine)
            show(shown)
            val hint = checkNotNull(shown.state.pendingHint)
            state.update { it.copy(selected = hint.cell()) }
            announce(Announcement.Hinted(hint))
            saveGame(shown)
        }
    }

    /** A new game in [mode] (by default the one Settings choose), on the puzzle prepared for it if there is one. */
    private fun newGame(mode: GameMode = state.value.settings.mode) {
        val prepared = next?.takeIf { nextMode == mode }
        if (prepared == null) next?.cancel()
        next = null
        generate(nextMode = mode) { startNewGame(mode, prepared?.await()) }
    }

    private fun restart() {
        val session = state.value.session ?: return
        generate(nextMode = null) { restartGame(session) }
    }

    /**
     * Replaces the game; one at a time, so a new request cancels the one in flight. Then, for a new game, prepares
     * the one after it in [nextMode].
     */
    private fun generate(nextMode: GameMode?, game: suspend () -> GameSession) {
        generation?.cancel()
        hintSearch?.cancel()
        state.update { it.copy(loading = true) }
        generation = viewModelScope.launch {
            show(game(), fresh = true)
            nextMode?.let(::prepareNext)
        }
    }

    private fun prepareNext(mode: GameMode) {
        if (next != null && nextMode == mode) return
        next?.cancel()
        nextMode = mode
        next = viewModelScope.async { preparePuzzle(mode) }
    }

    private fun pause() {
        clock.stop()
        // A won game was already recorded and its save removed.
        state.value.session?.takeUnless { it.state.isWon || state.value.loading }
            ?.let { viewModelScope.launch { saveGame(it) } }
    }

    private fun show(session: GameSession, fresh: Boolean = false) {
        state.update {
            it.copy(
                session = session,
                loading = false,
                conflicts = conflicts.conflicts(session.state),
                selected = if (fresh) null else it.selected,
                winRecord = if (fresh) null else it.winRecord,
            )
        }
    }

    /** Numbers each announcement, so saying the same thing twice in a row is still spoken twice. */
    private fun announce(announcement: Announcement) {
        state.update { it.copy(announcement = Announced(announcement, (it.announcement?.sequence ?: 0) + 1)) }
    }

    private fun Hint.cell(): Int = when (this) {
        is Hint.Placement -> cell
        is Hint.WrongDigit -> cell
    }

    private companion object {
        const val CLOCK_TICK_MILLIS = 1_000L
    }
}
