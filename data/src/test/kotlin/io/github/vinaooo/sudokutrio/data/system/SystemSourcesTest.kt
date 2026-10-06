package io.github.vinaooo.sudokutrio.data.system

import io.kotest.matchers.longs.shouldBeInRange
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.Test

class SystemSourcesTest {
    @Test
    fun `seeds vary and the clock reads the system time`() {
        val seeds = RandomSeedSource()
        List(5) { seeds.nextSeed() }.toSet().size shouldNotBe 1
        val before = System.currentTimeMillis()
        SystemClock().nowMillis() shouldBeInRange before..System.currentTimeMillis()
    }
}
