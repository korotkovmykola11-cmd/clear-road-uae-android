package com.clearroad.app.domain

/**
 * Stage 31.5 decision narrative — presentation-only prose from existing route metrics.
 * Does not alter scoring, selection, or recommendation calculations.
 */
internal object DecisionNarrativeSynthesis {

    fun narrative(
        mode: PreferenceMode,
        recommendedTollAed: Int,
        highConfidence: Boolean,
    ): String {
        val timingQualifier =
            if (highConfidence) "stable timing" else "current traffic timing"
        return when (mode) {
            PreferenceMode.FASTEST -> {
                val salikPhrase =
                    if (recommendedTollAed == 0) "no Salik listed" else "lower Salik impact"
                "Best time-focused pick with $timingQualifier and $salikPhrase."
            }
            PreferenceMode.NO_TOLLS ->
                if (recommendedTollAed == 0) {
                    "Lower Salik impact with $timingQualifier."
                } else {
                    "Best Salik balance with $timingQualifier."
                }
            PreferenceMode.CALM ->
                "Lower traffic delay load with $timingQualifier."
        }
    }

    fun confidenceDisplayLabel(highConfidence: Boolean): String =
        if (highConfidence) "Confidence 92%" else "Confidence 78%"
}
