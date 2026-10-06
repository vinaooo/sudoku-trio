package io.github.vinaooo.sudokutrio.core.ads

/**
 * The banner's room: full width on a phone in portrait; in landscape, or on a tablet (600dp+), at most 320dp wide
 * and centered. At most 60dp tall, 50dp in landscape. The slot always takes its full height, with or without an ad,
 * so the board never jumps when one loads or fails.
 */
data class BannerSlot(val adWidthDp: Int, val heightDp: Int) {
    companion object {
        const val MAX_HEIGHT_DP = 60
        const val LANDSCAPE_MAX_HEIGHT_DP = 50
        const val MAX_WIDTH_DP = 320
        const val TABLET_WIDTH_DP = 600

        fun of(availableWidthDp: Int, landscape: Boolean): BannerSlot {
            val capped = landscape || availableWidthDp >= TABLET_WIDTH_DP
            return BannerSlot(
                adWidthDp = if (capped) availableWidthDp.coerceAtMost(MAX_WIDTH_DP) else availableWidthDp,
                heightDp = if (landscape) LANDSCAPE_MAX_HEIGHT_DP else MAX_HEIGHT_DP,
            )
        }
    }
}
