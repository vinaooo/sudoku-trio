package io.github.vinaooo.sudokutrio.feature.game

import io.github.vinaooo.vinkit.core.AppSettings

enum class FeedbackEvent { MOVE, REJECTED, WIN }

/**
 * Sound and vibration. The ViewModel decides when; implementations decide how. A wrong digit sounds like a right one:
 * mistakes are silent. [FeedbackEvent.REJECTED] is for moves the rules refuse, such as typing over a given.
 */
interface GameFeedback {
    fun sound(event: FeedbackEvent)

    fun haptic(event: FeedbackEvent)
}

/** Plays [event] through the channels the player left on in [settings]. */
internal fun GameFeedback.give(event: FeedbackEvent, settings: AppSettings) {
    if (settings.soundEnabled) sound(event)
    if (settings.hapticsEnabled) haptic(event)
}
