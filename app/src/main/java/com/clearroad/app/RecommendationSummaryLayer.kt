package com.clearroad.app

import com.clearroad.app.domain.PreferenceMode

/**
 * Stage 32.4 — one-line human summary for the Home recommendation card.
 * Copy only; does not affect route selection or scoring.
 */
internal object RecommendationSummaryLayer {

    fun forMode(mode: PreferenceMode): String =
        when (mode) {
            PreferenceMode.FASTEST -> "Gets you there sooner."
            PreferenceMode.NO_TOLLS -> "Avoids unnecessary Salik costs."
            PreferenceMode.CALM -> "Less stop-and-go driving."
        }
}
