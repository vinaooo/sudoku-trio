package io.github.vinaooo.sudokutrio.feature.game.badges

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MilitaryTech
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.vinaooo.sudokutrio.domain.model.Achievement
import io.github.vinaooo.sudokutrio.domain.model.Ladder
import io.github.vinaooo.sudokutrio.feature.game.GameUiState
import io.github.vinaooo.sudokutrio.feature.game.R
import io.github.vinaooo.vinkit.achievements.Badge
import io.github.vinaooo.vinkit.achievements.BadgesScreen
import io.github.vinaooo.vinkit.achievements.R as AchievementsR
import io.github.vinaooo.vinkit.shell.NavigationAction

@Composable
fun BadgesRoute(onBack: () -> Unit, modifier: Modifier = Modifier, viewModel: BadgesViewModel = hiltViewModel()) {
    val unlocked by viewModel.unlocked.collectAsStateWithLifecycle()
    BadgesScreen(Achievement.entries.map { badge(it) }, unlocked, onBack, modifier)
}

/** A badge in vinkit's terms: stored by its name, shown in the UI language. */
@Composable
internal fun badge(achievement: Achievement): Badge {
    val ladder = achievement.ladder
    return if (ladder != null) {
        val (name, note) = LADDER_TEXT.getValue(ladder)
        Badge(
            achievement.name,
            pluralStringResource(name, achievement.count, achievement.count),
            pluralStringResource(note, achievement.count, achievement.count),
        )
    } else {
        val (name, note) = BADGE_TEXT.getValue(achievement)
        Badge(achievement.name, stringResource(name), stringResource(note))
    }
}

/** The Badges screen's button beside Scores and Settings, when there is one. */
@Composable
internal fun badgesButton(onOpenBadges: (() -> Unit)?): List<NavigationAction> {
    val label = stringResource(AchievementsR.string.vinkit_badges)
    return listOfNotNull(onOpenBadges?.let { NavigationAction(Icons.Rounded.MilitaryTech, label, it) })
}

/** Badges just unlocked, in a snackbar once no dialog or new puzzle covers the game (it would time out behind one). */
@Composable
internal fun BadgesEarned(uiState: GameUiState, snackbar: SnackbarHostState, onShown: () -> Unit) {
    val earned = uiState.earned
    val covered = uiState.winRecord != null || uiState.loading
    val text = when (earned.size) {
        0 -> null
        1 -> stringResource(AchievementsR.string.vinkit_new_badge, badge(earned.single()).name)
        else -> pluralStringResource(AchievementsR.plurals.vinkit_new_badges, earned.size, earned.size)
    }
    LaunchedEffect(text, covered) {
        // Cleared once shown: clearing first would change the key and cancel the snackbar.
        if (text != null && !covered) {
            snackbar.showSnackbar(text)
            onShown()
        }
    }
}

private val LADDER_TEXT = mapOf(
    Ladder.PLAYED to (AchievementsR.plurals.vinkit_badge_played to R.plurals.badge_played_note),
    Ladder.DAYS to (R.plurals.badge_days_name to R.plurals.badge_days_note),
    Ladder.WON to (AchievementsR.plurals.vinkit_badge_won to R.plurals.badge_won_note),
    Ladder.STREAK to (AchievementsR.plurals.vinkit_badge_streak to R.plurals.badge_streak_note),
)

private val BADGE_TEXT = mapOf(
    Achievement.WIN_CLASSIC to (R.string.badge_win_classic_name to R.string.badge_win_classic_note),
    Achievement.WIN_X to (R.string.badge_win_x_name to R.string.badge_win_x_note),
    Achievement.WIN_KILLER to (R.string.badge_win_killer_name to R.string.badge_win_killer_note),
    Achievement.WIN_TRIO to (R.string.badge_win_trio_name to R.string.badge_win_trio_note),
    Achievement.WIN_CLASSIC_EXPERT to
        (R.string.badge_win_classic_expert_name to R.string.badge_win_classic_expert_note),
    Achievement.WIN_X_EXPERT to (R.string.badge_win_x_expert_name to R.string.badge_win_x_expert_note),
    Achievement.WIN_KILLER_EXPERT to (R.string.badge_win_killer_expert_name to R.string.badge_win_killer_expert_note),
    Achievement.WIN_EVERY_MODE to (R.string.badge_win_every_mode_name to R.string.badge_win_every_mode_note),
    Achievement.KILLER_MASTER to (R.string.badge_killer_master_name to R.string.badge_killer_master_note),
    Achievement.FLAWLESS to (R.string.badge_flawless_name to R.string.badge_flawless_note),
    Achievement.FLAWLESS_EXPERT to (R.string.badge_flawless_expert_name to R.string.badge_flawless_expert_note),
    Achievement.NO_HINTS to (AchievementsR.string.vinkit_badge_no_hints to R.string.badge_no_hints_note),
    Achievement.PERFECT to (R.string.badge_perfect_name to R.string.badge_perfect_note),
    Achievement.FAST_EASY to (R.string.badge_fast_easy_name to R.string.badge_fast_easy_note),
    Achievement.FAST_MEDIUM to (R.string.badge_fast_medium_name to R.string.badge_fast_medium_note),
    Achievement.FAST_HARD to (R.string.badge_fast_hard_name to R.string.badge_fast_hard_note),
    Achievement.FAST_EXPERT to (R.string.badge_fast_expert_name to R.string.badge_fast_expert_note),
    Achievement.NIGHT_OWL to (R.string.badge_night_owl_name to R.string.badge_night_owl_note),
    Achievement.EARLY_BIRD to (R.string.badge_early_bird_name to R.string.badge_early_bird_note),
    Achievement.MARATHON to (R.string.badge_marathon_name to R.string.badge_marathon_note),
    Achievement.WEEKEND to (R.string.badge_weekend_name to R.string.badge_weekend_note),
)
