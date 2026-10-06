package io.github.vinaooo.sudokutrio.core.ads

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class BannerSlotTest {
    @Test
    fun `a phone in portrait gets a full-width banner up to 60dp tall`() {
        BannerSlot.of(412, landscape = false) shouldBe BannerSlot(412, 60)
        BannerSlot.of(599, landscape = false) shouldBe BannerSlot(599, 60)
    }

    @Test
    fun `landscape caps the banner at 320dp wide and 50dp tall`() {
        BannerSlot.of(891, landscape = true) shouldBe BannerSlot(320, 50)
        BannerSlot.of(300, landscape = true) shouldBe BannerSlot(300, 50)
    }

    @Test
    fun `a tablet in portrait caps the width only`() {
        BannerSlot.of(600, landscape = false) shouldBe BannerSlot(320, 60)
    }
}
