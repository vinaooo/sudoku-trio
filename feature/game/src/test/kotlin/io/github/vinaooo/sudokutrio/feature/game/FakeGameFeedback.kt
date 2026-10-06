package io.github.vinaooo.sudokutrio.feature.game

class FakeGameFeedback : GameFeedback {
    val sounds = mutableListOf<FeedbackEvent>()
    val haptics = mutableListOf<FeedbackEvent>()

    override fun sound(event: FeedbackEvent) {
        sounds += event
    }

    override fun haptic(event: FeedbackEvent) {
        haptics += event
    }
}
