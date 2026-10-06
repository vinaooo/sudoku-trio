package io.github.vinaooo.sudokutrio.feature.game.ui

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.github.vinaooo.sudokutrio.domain.model.Hint
import io.github.vinaooo.sudokutrio.domain.solver.TechniqueKind
import io.github.vinaooo.sudokutrio.feature.game.R

@get:StringRes
internal val TechniqueKind.label: Int
    get() = when (this) {
        TechniqueKind.NAKED_SINGLE -> R.string.technique_naked_single
        TechniqueKind.HIDDEN_SINGLE -> R.string.technique_hidden_single
        TechniqueKind.CAGE_COMBINATION -> R.string.technique_cage_combination
        TechniqueKind.RULE_OF_45 -> R.string.technique_rule_of_45
        TechniqueKind.LOCKED_CANDIDATES -> R.string.technique_locked_candidates
        TechniqueKind.NAKED_PAIR -> R.string.technique_naked_pair
        TechniqueKind.HIDDEN_PAIR -> R.string.technique_hidden_pair
        TechniqueKind.NAKED_TRIPLE -> R.string.technique_naked_triple
        TechniqueKind.HIDDEN_TRIPLE -> R.string.technique_hidden_triple
        TechniqueKind.X_WING -> R.string.technique_x_wing
        TechniqueKind.XY_WING -> R.string.technique_xy_wing
        TechniqueKind.SWORDFISH -> R.string.technique_swordfish
    }

/** What the hint is about: the technique that finds the digit, a wrong digit, or a cell to reveal. */
@Composable
internal fun hintTitle(hint: Hint): String = when (hint) {
    is Hint.WrongDigit -> stringResource(R.string.hint_wrong_digit)
    is Hint.Placement -> hint.technique?.let { stringResource(it.label) } ?: stringResource(R.string.hint_reveal)
}

/** What the second tap does. */
@Composable
internal fun hintBody(hint: Hint): String = when (hint) {
    is Hint.WrongDigit -> stringResource(R.string.hint_body_erase)
    is Hint.Placement -> stringResource(R.string.hint_body_fill)
}
