package io.github.vinaooo.sudokutrio.data.system

import io.github.vinaooo.sudokutrio.domain.repository.Clock
import io.github.vinaooo.sudokutrio.domain.repository.SeedSource
import javax.inject.Inject
import kotlin.random.Random

class RandomSeedSource @Inject constructor() : SeedSource {
    override fun nextSeed(): Long = Random.nextLong()
}

class SystemClock @Inject constructor() : Clock {
    override fun nowMillis(): Long = System.currentTimeMillis()
}
