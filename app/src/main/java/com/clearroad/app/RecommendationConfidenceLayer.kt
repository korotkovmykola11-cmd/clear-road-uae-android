package com.clearroad.app

import com.clearroad.app.domain.PreferenceMode

/**
 * Stage 32.3 — fixed confidence copy for recommended Route Details only.
 * Wording layer; does not calculate confidence from route metrics.
 */
internal object RecommendationConfidenceLayer {

    data class ConfidenceCopy(
        val title: String,
        val body: String,
        val isHighConfidence: Boolean,
    )

    fun forMode(mode: PreferenceMode): ConfidenceCopy =
        when (mode) {
            PreferenceMode.FASTEST ->
                ConfidenceCopy(
                    title = "High confidence",
                    body = "This route is clearly faster than the available alternatives.",
                    isHighConfidence = true,
                )
            PreferenceMode.NO_TOLLS ->
                ConfidenceCopy(
                    title = "Medium confidence",
                    body =
                        "This route reduces Salik exposure, but may require a few extra minutes.",
                    isHighConfidence = false,
                )
            PreferenceMode.CALM ->
                ConfidenceCopy(
                    title = "Medium confidence",
                    body =
                        "This route minimizes additional traffic delay, even if it is not the fastest option.",
                    isHighConfidence = false,
                )
        }

    /** Stage 32.8 — one-line confidence chip for the Home recommendation card. */
    fun chipLabel(mode: PreferenceMode): String = forMode(mode).title
}
