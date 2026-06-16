package com.clearroad.app

import com.clearroad.app.domain.PreferenceMode

/**
 * Stage 32.5 — mode badge for the Home recommendation card.
 * Copy only; does not affect route selection or scoring.
 */
internal object RecommendationBadgeLayer {

    fun forMode(mode: PreferenceMode): String =
        when (mode) {
            PreferenceMode.FASTEST -> "⚡ Recommended"
            PreferenceMode.NO_TOLLS -> "💰 Best Value"
            PreferenceMode.CALM -> "🌊 Smoother trip"
        }
}
