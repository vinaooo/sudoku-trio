package io.github.vinaooo.sudokutrio.feature.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.SentimentNeutral
import androidx.compose.material.icons.rounded.SentimentSatisfied
import androidx.compose.material.icons.rounded.SentimentVerySatisfied
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.github.vinaooo.sudokutrio.core.ui.label
import io.github.vinaooo.sudokutrio.domain.model.Difficulty
import io.github.vinaooo.sudokutrio.domain.model.Settings
import io.github.vinaooo.sudokutrio.domain.model.Variant
import io.github.vinaooo.vinkit.settings.Choice
import io.github.vinaooo.vinkit.settings.IconChoice
import io.github.vinaooo.vinkit.settings.IconOption

/** Sudoku Trio's section of vinkit's Settings screen: the variant and difficulty of the next game. */
@Composable
internal fun GameSection(settings: Settings, onChange: (SettingsChange) -> Unit) {
    Choice(
        title = stringResource(R.string.variant),
        options = Variant.entries.map { it to stringResource(it.label) },
        selected = settings.mode.variant,
        onSelect = { onChange(SettingsChange.VariantChanged(it)) },
    )
    // Four levels take icons, as the standard says; TalkBack reads each one's name.
    IconChoice(
        title = stringResource(R.string.difficulty),
        options = Difficulty.entries.map {
            IconOption(it, icons.getValue(it), stringResource(it.label), stringResource(descriptions.getValue(it)))
        },
        selected = settings.mode.difficulty,
        onSelect = { onChange(SettingsChange.DifficultyChanged(it)) },
    )
}

private val icons = mapOf(
    Difficulty.EASY to Icons.Rounded.SentimentVerySatisfied,
    Difficulty.MEDIUM to Icons.Rounded.SentimentSatisfied,
    Difficulty.HARD to Icons.Rounded.SentimentNeutral,
    Difficulty.EXPERT to Icons.Rounded.Psychology,
)

private val descriptions = mapOf(
    Difficulty.EASY to R.string.difficulty_easy_note,
    Difficulty.MEDIUM to R.string.difficulty_medium_note,
    Difficulty.HARD to R.string.difficulty_hard_note,
    Difficulty.EXPERT to R.string.difficulty_expert_note,
)
