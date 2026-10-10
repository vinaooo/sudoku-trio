package io.github.vinaooo.sudokutrio.core.ui

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.github.vinaooo.sudokutrio.domain.model.Difficulty
import io.github.vinaooo.sudokutrio.domain.model.GameMode
import io.github.vinaooo.sudokutrio.domain.model.Variant
import io.github.vinaooo.vinkit.designsystem.R as DesignR

@get:StringRes
val Variant.label: Int
    get() = when (this) {
        Variant.CLASSIC -> R.string.variant_classic
        Variant.X -> R.string.variant_x
        Variant.KILLER -> R.string.variant_killer
    }

@get:StringRes
val Difficulty.label: Int
    get() = when (this) {
        Difficulty.EASY -> DesignR.string.vinkit_difficulty_easy
        Difficulty.MEDIUM -> DesignR.string.vinkit_difficulty_medium
        Difficulty.HARD -> DesignR.string.vinkit_difficulty_hard
        Difficulty.EXPERT -> R.string.difficulty_expert
    }

/** The mode's name, such as "Killer · Hard", as the game, Settings and Scores show it. */
@Composable
fun modeName(mode: GameMode): String =
    stringResource(R.string.mode_name, stringResource(mode.variant.label), stringResource(mode.difficulty.label))
