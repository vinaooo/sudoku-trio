package io.github.vinaooo.sudokutrio.feature.settings

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.vinaooo.sudokutrio.core.ui.label
import io.github.vinaooo.sudokutrio.domain.model.Settings
import io.github.vinaooo.sudokutrio.domain.model.Variant
import io.github.vinaooo.vinkit.core.BoardAlignment
import io.github.vinaooo.vinkit.core.Handedness
import io.github.vinaooo.vinkit.core.PhoneViewSide
import io.github.vinaooo.vinkit.core.ThemeMode
import io.github.vinaooo.vinkit.designsystem.ColorChoice

@Composable
internal fun GameSection(settings: Settings, onChange: (SettingsChange) -> Unit) {
    SectionTitle(stringResource(R.string.section_game))
    Choice(
        title = stringResource(R.string.variant),
        options = Variant.entries.map { it to it.label },
        selected = settings.mode.variant,
        onSelect = { onChange(SettingsChange.VariantChanged(it)) },
    )
    DifficultyChoice(settings.mode.difficulty) { onChange(SettingsChange.DifficultyChanged(it)) }
}

private const val TABLET_WIDTH_DP = 600

@Composable
internal fun AppearanceSection(settings: Settings, onChange: (SettingsChange) -> Unit) {
    SectionTitle(stringResource(R.string.section_appearance))
    Choice(
        title = stringResource(R.string.theme),
        options = listOf(
            ThemeMode.SYSTEM to R.string.theme_system,
            ThemeMode.LIGHT to R.string.theme_light,
            ThemeMode.DARK to R.string.theme_dark,
        ),
        selected = settings.themeMode,
        onSelect = { onChange(SettingsChange.ThemeModeChanged(it)) },
    )
    val dynamicColorAvailable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    if (dynamicColorAvailable) {
        ToggleRow(
            title = stringResource(R.string.dynamic_color),
            supporting = stringResource(R.string.dynamic_color_note),
            checked = settings.dynamicColor,
        ) { onChange(SettingsChange.DynamicColorChanged(it)) }
    }
    // Revealed from behind the switch when it's turned off; always there without dynamic color.
    AnimatedVisibility(
        visible = !dynamicColorAvailable || !settings.dynamicColor,
        enter = expandVertically(MaterialTheme.motionScheme.defaultSpatialSpec(), Alignment.Top) +
            fadeIn(MaterialTheme.motionScheme.defaultEffectsSpec()),
        exit = shrinkVertically(MaterialTheme.motionScheme.defaultSpatialSpec(), Alignment.Top) +
            fadeOut(MaterialTheme.motionScheme.defaultEffectsSpec()),
    ) {
        ColorChoice(settings.themeColor, settings.themeMode, onSelect = {
            onChange(SettingsChange.ThemeColorChanged(it))
        })
    }
    Choice(
        title = stringResource(R.string.handedness),
        options = listOf(Handedness.LEFT to R.string.hand_left, Handedness.RIGHT to R.string.hand_right),
        selected = settings.handedness,
        onSelect = { onChange(SettingsChange.HandednessChanged(it)) },
    )
    Choice(
        title = stringResource(R.string.board_alignment),
        options = listOf(
            BoardAlignment.TOP to R.string.board_top,
            BoardAlignment.BOTTOM to R.string.board_bottom,
        ),
        selected = settings.boardAlignment,
        onSelect = { onChange(SettingsChange.BoardAlignmentChanged(it)) },
    )
    PhoneViewRows(settings, onChange)
}

@Composable
private fun PhoneViewRows(settings: Settings, onChange: (SettingsChange) -> Unit) {
    // Only a tablet (Material's medium window and up) has room to spare; a phone already shows a phone's cards.
    if (LocalConfiguration.current.smallestScreenWidthDp >= TABLET_WIDTH_DP) {
        ToggleRow(
            title = stringResource(R.string.phone_view),
            supporting = stringResource(R.string.phone_view_note),
            checked = settings.phoneView,
        ) { onChange(SettingsChange.PhoneViewChanged(it)) }
        // Revealed from behind the switch above it, as if it had been tucked under it.
        AnimatedVisibility(
            visible = settings.phoneView,
            enter = expandVertically(MaterialTheme.motionScheme.defaultSpatialSpec(), Alignment.Top) +
                fadeIn(MaterialTheme.motionScheme.defaultEffectsSpec()),
            exit = shrinkVertically(MaterialTheme.motionScheme.defaultSpatialSpec(), Alignment.Top) +
                fadeOut(MaterialTheme.motionScheme.defaultEffectsSpec()),
        ) {
            Choice(
                title = stringResource(R.string.phone_view_side),
                options = listOf(
                    PhoneViewSide.LEFT to R.string.side_left,
                    PhoneViewSide.CENTER to R.string.side_center,
                    PhoneViewSide.RIGHT to R.string.side_right,
                ),
                selected = settings.phoneViewSide,
                onSelect = { onChange(SettingsChange.PhoneViewSideChanged(it)) },
            )
        }
    }
}

@Composable
internal fun FeedbackSection(settings: Settings, onChange: (SettingsChange) -> Unit) {
    SectionTitle(stringResource(R.string.section_feedback))
    ToggleRow(stringResource(R.string.sound), settings.soundEnabled) {
        onChange(SettingsChange.SoundChanged(it))
    }
    ToggleRow(stringResource(R.string.haptics), settings.hapticsEnabled) {
        onChange(SettingsChange.HapticsChanged(it))
    }
}

/**
 * The privacy policy link, which Google Play requires inside the app, and the consent form, which is offered only
 * where the law requires a way to change ad consent (GDPR, some US states).
 */
@Composable
internal fun PrivacySection(
    privacyOptionsRequired: Boolean,
    onOpenPrivacyOptions: () -> Unit,
    onOpenPrivacyPolicy: () -> Unit,
) {
    SectionTitle(stringResource(R.string.section_privacy))
    LinkRow(stringResource(R.string.privacy_policy), onClick = onOpenPrivacyPolicy)
    if (privacyOptionsRequired) {
        LinkRow(
            stringResource(R.string.privacy_options),
            supporting = stringResource(R.string.privacy_options_note),
            onClick = onOpenPrivacyOptions,
        )
    }
}

@Composable
private fun LinkRow(title: String, supporting: String? = null, onClick: () -> Unit) {
    ListItem(
        onClick = onClick,
        supportingContent = supporting?.let { { Text(it) } },
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
    ) {
        Text(title)
    }
}
